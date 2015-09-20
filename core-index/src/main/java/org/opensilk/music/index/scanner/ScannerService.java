/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.index.scanner;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarIntentService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.database.IndexSchema;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.index.scanner.NotificationHelper.Status;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import hugo.weaving.DebugLog;
import mortar.MortarScope;
import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import timber.log.Timber;

import static android.provider.MediaStore.Audio.keyFor;
import static org.opensilk.common.core.util.Preconditions.checkNotNullOrBlank;

/**
 * Created by drew on 8/25/15.
 */
public class ScannerService extends MortarIntentService {

    @Inject LastFM mLastFM;
    @Inject IndexDatabase mIndexDatabase;
    @Inject NotificationHelper mNotifHelper;

    final AtomicInteger numTotal = new AtomicInteger(0);
    final AtomicInteger numError = new AtomicInteger(0);
    final AtomicInteger numProcessed = new AtomicInteger(0);

    Subject<Status, Status> notifSubject;//DO NOT ACCESS FROM MAIN THREAD

    public ScannerService() {
        super(ScannerService.class.getSimpleName());
    }

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        IndexComponent acc = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, ScannerComponent.FACTORY.call(acc));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ScannerComponent acc = DaggerService.getDaggerComponent(this);
        acc.inject(this);

        mNotifHelper.attachService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotifHelper.detachService(this);
    }

    void notifySuccess(Uri uri) {
        numProcessed.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void notifySkipped(Uri uri) {
        Timber.d("Skipping item already in db %s", uri);
        numProcessed.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void notifyError(Uri uri) {
        numError.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void updateNotification(Status status) {
        mNotifHelper.updateNotification(status, numProcessed.get(), numError.get(), numTotal.get());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Uri uri = intent.getData();
        if (uri == null) {
            Timber.w("ScannerService called without uri");
            return;
        }
        notifSubject = PublishSubject.create();
        notifSubject.asObservable().debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Status>() {
                    @Override
                    @DebugLog
                    public void call(Status status) {
                        updateNotification(status);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    @DebugLog
                    public void call(Throwable throwable) {
                        updateNotification(Status.COMPLETED);
                    }
                });
        notifSubject.onNext(Status.SCANNING);
        scan2(uri);
        notifSubject.onNext(Status.COMPLETED);
        notifSubject.onCompleted();
    }

    void scan2(final Uri uri) {
        Timber.i("scan2(%s)", uri);
        mIndexDatabase.addContainer(uri);
        BundleableLoader loader = new BundleableLoader(this, uri, null);
        List<Bundleable> bundleables;
        try {
            bundleables = loader.createObservable().toBlocking().first();
        } catch (RuntimeException e) {
            Timber.e(e, "scan2(%s)", uri);
            return;
        }
        for (Bundleable b : bundleables) {
            if (b instanceof Track) {
                numTotal.incrementAndGet();
                Track item = (Track) b;
                Timber.i("indexing %s, %s", item.getName(), item.getUri());
                if (!needsScan(item.getResources().get(0))) {
                    notifySkipped(item.getUri());
                    continue;
                }
                if (trackHasRequiredMeta(item)) {
                    addMetaToDb(item);
                } else {
                    Track t = extractMeta(item);
                    if (t == null) {
                        notifyError(item.getUri());
                        continue;
                    }
                    addMetaToDb(t);
                }
                notifySuccess(item.getUri());
            } else if (b instanceof Container) {
                scan2(b.getUri());
            } else {
                Timber.w("Unsupported bundleable %s at %s", b.getClass(), b.getUri());
            }
        }
    }

    static boolean trackHasRequiredMeta(Track track) {
        try {
            final String err = "Null field";
            checkNotNullOrBlank(track.getAlbumName(), err);
            checkNotNullOrBlank(track.getAlbumArtistName(), err);
            checkNotNullOrBlank(track.getArtistName(), err);
            checkNotNullOrBlank(track.getGenre(), err);
            checkNotNullOrBlank(track.getMimeType(), err);
            checkNotNullOrBlank(track.getName(), err);
        } catch (NullPointerException| IllegalArgumentException e) {
            return false;
        }
        for (Track.Res res : track.getResources()) {
            if (res.getBitrate() < 0) {
                return false;
            }
            if (res.getSize() < 0) {
                return false;
            }
        }
        return true;
    }

    Track extractMeta(Track t) {

        final Track.Res res = t.getResources().get(0);
        final Uri uri = res.getUri();
        final Map<String, String> headers = res.getHeaders();

        Track.Builder tob = t.buildUpon();
        Track.Res.Builder rob = res.buildUpon();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            if (StringUtils.startsWith(uri.getScheme(), "http")) {
                mmr.setDataSource(uri.toString(), headers);
            } else if (StringUtils.equals(uri.getScheme(), "content")) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                mmr.setDataSource(pfd.getFileDescriptor());
                pfd.close();
            } else if (StringUtils.equals(uri.getScheme(), "file")) {
                mmr.setDataSource(uri.getPath());
            }

            tob
                    .setAlbumName(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                    .setAlbumArtistName(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                    .setArtistName(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                    .setGenre(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                    .setName(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                    ;
            rob
                    .setMimeType(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
                    ;

            final String bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null) {
                try {
                    rob.setBitrate(Long.parseLong(bitrate));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_BITRATE)");
                }
            }
            final String track_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (track_num != null) {
                try {
                    if (StringUtils.contains(track_num, "/")) {
                        tob.setTrackNumber(Integer.parseInt(StringUtils.split(track_num, "/")[0]));
                    } else {
                        tob.setTrackNumber(Integer.parseInt(track_num));
                    }
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(CD_TRACK_NUMBER)");
                }
            }
            final String disc_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (disc_num != null) {
                try {
                    if (StringUtils.contains(disc_num, "/")) {
                        tob.setDiscNumber(Integer.parseInt(StringUtils.split(disc_num, "/")[0]));
                    } else {
                        tob.setDiscNumber(Integer.parseInt(disc_num));
                    }
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(DISC_NUMBER)");
                }
            }
            final String compilation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION);
            if (compilation != null) {
                try {
                    tob.setIsCompliation(Integer.parseInt(compilation) > 0);
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_COMPILATION)");
                }
            }
            final String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                try {
                    rob.setDuration(Integer.parseInt(duration));
                    tob.setDuration(Integer.parseInt(duration));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_DURATION)");
                }
            }

            tob.addRes(rob.build());

            return tob.build();
        } catch (Exception e) { //setDataSource throws runtimeException
            Timber.e(e, "extractMeta");
        } finally {
            mmr.release();
        }
        return null;
    }

    void addMetaToDb(Track track) {

        String albumArtist = resolveAlbumArtist(track);

        if (StringUtils.isEmpty(albumArtist)) {
            Timber.w("Cannot process item %s missing albumArtist", track.getUri());
            return;
        }

        long albumArtistId = checkArtist(albumArtist);
        if (albumArtistId < 0) {
            albumArtistId = lookupArtistInfo(albumArtist);
            if (albumArtistId < 0) {
                Timber.e("Unable to insert artist %s into db", albumArtist);
                return;
            }
        }

        String album = track.getAlbumName();

        if (StringUtils.isEmpty(album)) {
            Timber.w("Cannot process item %s missing album", track.getUri());
            return;
        }

        long albumId = checkAlbum(albumArtist, album);
        if (albumId < 0) {
            albumId = lookupAlbumInfo(albumArtist, album, albumArtistId);
            if (albumId < 0) {
                Timber.e("Unable to insert album %s into db", album);
                return;
            }
        }

        String artist = resolveTrackArtist(track);

        long artistId;
        if (StringUtils.isEmpty(artist)) {
            Timber.w("Cannot process item %s missing artist", track.getUri());
            return;
        } else if (StringUtils.equalsIgnoreCase(artist, albumArtist)) {
            artistId = albumArtistId;
        } else {
            artistId = checkArtist(artist);
            if (artistId < 0) {
                artistId = lookupArtistInfo(artist);
                if (artistId < 0) {
                    Timber.e("Unable to insert artist %s into db", artist);
                    return;
                }
            }
        }

        long trackId = insertTrack(track, artistId, albumId);
        if (trackId < 0) {
            Timber.e("Unable to insert track %s into db", track.getName());
            return;
        }

        long resId = insertRes(track.getResources().get(0), trackId);
        if (trackId < 0) {
            Timber.e("Unable to insert %s into db", track.getResources().get(0).getUri());
            return;
        }

        //TODO artwork

    }

    @DebugLog
    long lookupAlbumInfo(final String albumArtist, final String albumName, final long artistId) {
        Call<Album> call = mLastFM.getAlbum(albumArtist, albumName);
        Response<Album> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            return -1;
        }
        Album album = response.body();
        if (album == null) {
            Timber.w("Failed to retrieve album %s", albumName);
            return -1;
        }
        return insertAlbum(album, artistId);
    }

    @DebugLog
    long lookupArtistInfo(final String artistName) {
        Call<Artist> call = mLastFM.getArtist(artistName);
        Response<Artist> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            return -1;
        }
        Artist artist = response.body();
        if (artist == null) {
            Timber.w("Failed to retrieve artist %s", artistName);
            return -1;
        }
        return insertArtist(artist);
    }

    private static final String[] resMetaCols = new String[] {
            IndexSchema.TrackResMeta.SIZE,
            IndexSchema.TrackResMeta.LAST_MOD,
    };

    @DebugLog
    boolean needsScan(Track.Res res) {
        Cursor c = null;
        try {
            final String sel = IndexSchema.TrackResMeta.URI + "=?";
            final String[] selArgs = new String[] {res.getUri().toString()};
            c = mIndexDatabase.query(IndexSchema.TrackResMeta.TABLE, resMetaCols, sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                final long size = c.getLong(0);
                final long lastMod = c.getLong(1);
                //if size or lastmod has changed we need to rescan
                if (res.getSize() != size || res.getLastMod() != lastMod) {
                    mIndexDatabase.delete(IndexSchema.TrackResMeta.TABLE, sel, selArgs);
                    return true;
                }
                return false;
            }
            return true;
        } finally {
            closeCursor(c);
        }
    }

    private static final String[] idCols = new String[] {
            BaseColumns._ID,
    };

    static String resolveAlbumArtist(Track track) {
        String albumArtist = track.getAlbumArtistName();
        if (StringUtils.isEmpty(albumArtist)) {
            Timber.w("No albumArtist in metadata for %s", track.getUri());
            //No albumartist, we still may get away with the track artist though.
            albumArtist = track.getArtistName();
            //Artist names such as "Pretty Lights feat. Eligh" will fail so strip off the featured artist.
            if (StringUtils.containsIgnoreCase(albumArtist, "feat.")) {
                String[] strings = StringUtils.splitByWholeSeparator(albumArtist.toLowerCase(), "feat.");
                if (strings.length > 0) {
                    albumArtist = strings[0].trim();
                }
            }
        }
        return albumArtist;
    }

    static String resolveTrackArtist(Track track) {
        String artist = track.getArtistName();
        //Artist names such as "Pretty Lights feat. Eligh" will fail so strip off the album artist.
        if (StringUtils.containsIgnoreCase(artist, "feat.")) {
            String[] strings = StringUtils.splitByWholeSeparator(artist.toLowerCase(), "feat.");
            if (strings.length > 1) {
                artist = strings[2].trim();
            }
        }
        return artist;
    }

    @DebugLog
    long checkArtist(String artist) {
        long id = -1;
        Cursor c = null;
        try {
            final String sel = IndexSchema.ArtistInfo.ARTIST_KEY + "=?";
            final String[] selArgs = new String[]{keyFor(artist)};
            c = mIndexDatabase.query(IndexSchema.ArtistInfo.TABLE, idCols, sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    @DebugLog
    long insertArtist(Artist artist) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, artist.getName());
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, keyFor(artist.getName()));
        cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_SUMMARY, artist.getWikiSummary());
        cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_CONTENT, artist.getWikiText());
        cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_DATE_MOD,
                artist.getWikiLastChanged() != null ? artist.getWikiLastChanged().getTime() : null);
        cv.put(IndexSchema.ArtistMeta.ARTIST_MBID, artist.getMbid());
        return mIndexDatabase.insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @DebugLog
    long checkAlbum(String albumArtist, String album) {
        long id = -1;
        Cursor c = null;
        try {
            final String sel = IndexSchema.AlbumInfo.ALBUM_KEY + "=? AND " + IndexSchema.AlbumInfo.ARTIST_KEY + "=?";
            final String[] selArgs = new String[]{keyFor(album), keyFor(albumArtist)};
            c = mIndexDatabase.query(IndexSchema.AlbumInfo.TABLE, idCols, sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    @DebugLog
    long insertAlbum(Album album, long albumArtistId) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, album.getName());
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, keyFor(album.getName()));
        cv.put(IndexSchema.AlbumMeta.ALBUM_ARTIST_ID, albumArtistId);
        cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_SUMMARY, album.getWikiSummary());
        cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_CONTENT, album.getWikiText());
        cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_DATE_MOD,
                album.getWikiLastChanged() != null ? album.getWikiLastChanged().getTime() : null);
        cv.put(IndexSchema.AlbumMeta.ALBUM_MBID, album.getMbid());
        return mIndexDatabase.insert(IndexSchema.AlbumMeta.TABLE, null, cv,SQLiteDatabase.CONFLICT_IGNORE);
    }

    @DebugLog
    long insertTrack(Track t, long artistId, long albumId) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.TrackMeta.TRACK_NAME, t.getName());
        cv.put(IndexSchema.TrackMeta.TRACK_KEY, keyFor(t.getName()));
        cv.put(IndexSchema.TrackMeta.TRACK_NUMBER, t.getTrackNumber());
        cv.put(IndexSchema.TrackMeta.DISC_NUMBER, t.getDiscNumber());
        cv.put(IndexSchema.TrackMeta.GENRE, t.getGenre());
        cv.put(IndexSchema.TrackMeta.ARTIST_ID, artistId);
        cv.put(IndexSchema.TrackMeta.ALBUM_ID, albumId);
        return mIndexDatabase.insert(IndexSchema.TrackMeta.TABLE, null, cv,SQLiteDatabase.CONFLICT_IGNORE);
    }

    @DebugLog
    long insertRes(Track.Res res, long trackId) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.TrackResMeta.TRACK_ID, trackId);
        cv.put(IndexSchema.TrackResMeta.AUTHORITY,res.getUri().getAuthority());
        cv.put(IndexSchema.TrackResMeta.URI, res.getUri().toString());
        cv.put(IndexSchema.TrackResMeta.SIZE, res.getSize());
        cv.put(IndexSchema.TrackResMeta.MIME_TYPE, res.getMimeType());
        cv.put(IndexSchema.TrackResMeta.DATE_ADDED, System.currentTimeMillis());
        cv.put(IndexSchema.TrackResMeta.LAST_MOD, res.getLastMod());
        cv.put(IndexSchema.TrackResMeta.BITRATE, res.getBitrate());
        cv.put(IndexSchema.TrackResMeta.DURATION, res.getDuration());
        return mIndexDatabase.insert(IndexSchema.TrackResMeta.TABLE, null, cv,SQLiteDatabase.CONFLICT_IGNORE);
    }

    static void closeCursor(Cursor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

}
