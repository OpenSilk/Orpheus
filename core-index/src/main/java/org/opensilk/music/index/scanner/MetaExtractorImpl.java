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

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import java.lang.reflect.Field;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

import static org.opensilk.music.model.Metadata.*;

/**
 * Created by drew on 9/20/15.
 */
@ScannerScope
public class MetaExtractorImpl implements MetaExtractor {

    private static final boolean DUMP_META = false;
    final Context appContext;

    @Inject
    public MetaExtractorImpl(@ForApplication Context appContext) {
        this.appContext = appContext;
    }

    static int parseTrackNum(String track_num) throws NumberFormatException {
        if (StringUtils.contains(track_num, "/")) {
            return Integer.parseInt(StringUtils.split(track_num, "/")[0]);
        } else {
            return Integer.parseInt(track_num);
        }
    }

    static int parseDiskNum(String disc_num) throws  NumberFormatException {
        if (StringUtils.contains(disc_num, "/")) {
            return Integer.parseInt(StringUtils.split(disc_num, "/")[0]);
        } else {
            return Integer.parseInt(disc_num);
        }
    }

    //TODO how to handle this properly???
    static String fixLatin1(String string) {
        string = StringUtils.replace(string, "â€™", "’");
        return string;
    }

    @Override
    public @Nullable Metadata extractMetadata(Track.Res res) {

        final Uri uri = res.getUri();
        final Map<String, String> headers = res.getHeaders();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        try {
            if (StringUtils.startsWith(uri.getScheme(), "http")) {
                mmr.setDataSource(uri.toString(), headers);
            } else if (StringUtils.equals(uri.getScheme(), "content")) {
                mmr.setDataSource(appContext, uri);
            } else if (StringUtils.equals(uri.getScheme(), "file")) {
                mmr.setDataSource(uri.getPath());
            } else {
                throw new IllegalArgumentException("Unknown scheme " + uri.getScheme());
            }

            Metadata.Builder bob = Metadata.builder();

            bob.putString(KEY_ALBUM_NAME, fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)));
            bob.putString(KEY_ALBUM_ARTIST_NAME, fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)));
            bob.putString(KEY_ARTIST_NAME, fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)));
            bob.putString(KEY_GENRE_NAME, fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)));
            bob.putString(KEY_TRACK_NAME, fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)));
            bob.putString(KEY_MIME_TYPE, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));

            final String bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null) {
                try {
                    bob.putLong(KEY_BITRATE, Long.parseLong(bitrate));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_BITRATE)");
                }
            }
            final String track_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (track_num != null) {
                try {
                    bob.putInt(KEY_TRACK_NUMBER, parseTrackNum(track_num));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(CD_TRACK_NUMBER)");
                }
            }
            final String disc_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER);
            if (disc_num != null) {
                try {
                    bob.putInt(KEY_DISC_NUMBER, parseDiskNum(disc_num));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(DISC_NUMBER)");
                }
            }
            final String compilation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION);
            if (compilation != null) {
                try {
                    bob.putInt(KEY_IS_COMPILATION, Integer.parseInt(compilation));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_COMPILATION)");
                }
            }
            final String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                try {
                    bob.putLong(KEY_DURATION, Long.parseLong(duration));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_DURATION)");
                }
            }

            if(DUMP_META) {
                Field[] flds = MediaMetadataRetriever.class.getDeclaredFields();
                StringBuilder sb = new StringBuilder(100);
                for (Field f : flds) {
                    if (f.getName().startsWith("METADATA_KEY")) {
                        sb.append(f.getName()).append(": ").append(mmr.extractMetadata(f.getInt(null))).append("\n");
                    }
                }
                Timber.i(sb.toString());
            }

            return bob.build();
        } catch (Exception e) { //setDataSource throws runtimeException
            Timber.e(e, "extractMeta");
        } finally {
            mmr.release();
        }
        return null;
    }

}
