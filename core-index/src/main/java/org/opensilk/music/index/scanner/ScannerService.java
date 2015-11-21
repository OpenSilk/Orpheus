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

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarIntentService;
import org.opensilk.common.core.util.ConnectionUtils;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.database.TreeNode;
import org.opensilk.music.index.provider.LastFMHelper;
import org.opensilk.music.index.scanner.NotificationHelper.Status;
import org.opensilk.music.library.client.BundleableLoader;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

import static org.opensilk.music.model.Metadata.KEY_ALBUM_ARTIST_NAME;
import static org.opensilk.music.model.Metadata.KEY_ARTIST_NAME;

/**
 * Created by drew on 8/25/15.
 */
public class ScannerService extends MortarIntentService {

    public static final String ACTION_RESCAN = "rescan";
    public static final String ACTION_CONNECTION_RESTORED = "connection_restored";
    public static final String EXTRA_AUTHORITY = "authority";
    public static final String EXTRA_LIBRARY_EXTRAS = "libraryextras";

    @Inject IndexDatabase mIndexDatabase;
    @Inject NotificationHelper mNotifHelper;
    @Inject MetaExtractor mMetaExtractor;

    final AtomicInteger numTotal = new AtomicInteger(0);
    final AtomicInteger numError = new AtomicInteger(0);
    final AtomicInteger numProcessed = new AtomicInteger(0);
    final AtomicReference<Status> status = new AtomicReference<>();

    public ScannerService() {
        super(ScannerService.class.getSimpleName());
    }

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        IndexComponent acc = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, ScannerComponent.FACTORY.call(acc, this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ScannerComponent acc = DaggerService.getDaggerComponent(this);
        acc.inject(this);
    }

    @Override
    @DebugLog
    protected void onHandleIntent(Intent intent) {
        if (!ConnectionUtils.hasInternetConnection(this)) {
            mNotifHelper.showNoConnection();
            return;
        }
        status.set(Status.SCANNING);
        final Subscription notifSubs = Observable.interval(5, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        mNotifHelper.updateNotification(true);
                    }
                });
        if (ACTION_RESCAN.equals(intent.getAction())) {
            if (intent.hasExtra(EXTRA_LIBRARY_EXTRAS)) {
                Bundleable b = LibraryExtras.getBundleable(intent.getBundleExtra(EXTRA_LIBRARY_EXTRAS));
                if (b != null && (b instanceof Container)) {
                    Container c = (Container)b;
                    scan(c.getUri(), c.getParentUri());
                }
            } else {
                String authority = intent.getStringExtra(EXTRA_AUTHORITY);
                List<Pair<Uri, Uri>> topLevel = mIndexDatabase.findTopLevelContainers(authority);
                for (Pair<Uri,Uri> p : topLevel) {
                    scan(p.first, p.second);
                }
            }
        } else {
            Bundle extras = intent.getBundleExtra(EXTRA_LIBRARY_EXTRAS);
            if (extras != null) {
                Container container = LibraryExtras.getBundleable(extras);
                if (container != null) {
                    scan(container.getUri(), container.getParentUri());
                } else {
                    Timber.e("No container in extras");
                }
            } else {
                Timber.e("No extras in intent");
            }
        }
        notifSubs.unsubscribe();
        status.set(Status.COMPLETED);
        mNotifHelper.updateNotification(false);
    }

    void notifySuccess(Uri uri) {
        Timber.v("Indexed %s", uri);
        numProcessed.incrementAndGet();
    }

    void notifySkipped(Uri uri) {
        Timber.d("Skipping item already in db %s", uri);
        numProcessed.incrementAndGet();
    }

    void notifyError(Uri uri) {
        Timber.w("An error occured while proccessing %s", uri);
        numError.incrementAndGet();
    }

    void scan(Uri uri, Uri parentUri) {
        Timber.i("scan(%s)", uri);
        final TreeNode currentTree = mIndexDatabase.buildTree(uri, parentUri);
        final TreeNode newTree = scanChildren(uri, parentUri);
        indexTree(newTree);
        removeDifference(currentTree, newTree);
        mIndexDatabase.notifyObservers();
    }

    private TreeNode scanChildren(Uri uri, Uri parentUri) {
        Timber.d("scanChildren(%s)", uri);
        List<Bundleable> bundleableList = getChildren(uri);
        TreeNode node = new TreeNode(uri, parentUri);
        for (Bundleable b : bundleableList) {
            if (b instanceof Track) {
                node.tracks.add((Track)b);
            } else if (b instanceof Container) {
                Container c = (Container)b;
                node.children.add(scanChildren(c.getUri(), c.getParentUri()));
            } else {
                Timber.w("Passed an unsupported bundle to scanner class=%s", b.getClass());
            }
        }
        return node;
    }

    protected List<Bundleable> getChildren(Uri uri) {
        final List<Bundleable> bundleableList =
                Collections.synchronizedList(new ArrayList<Bundleable>());
        final CountDownLatch latch = new CountDownLatch(1);
        BundleableLoader loader = BundleableLoader.create(this)
                .setMethod(LibraryMethods.SCAN)
                .setUri(uri);
        loader.createObservable()
                .subscribe(new Subscriber<List<Bundleable>>() {
                    @Override public void onCompleted() {
                        latch.countDown();
                    }
                    @Override public void onError(Throwable e) {
                        latch.countDown();
                    }
                    @Override public void onNext(List<Bundleable> bundleables) {
                        bundleableList.addAll(bundleables);
                    }
                });
        while (true) {
            try {
                latch.await(); break;
            } catch (InterruptedException ignored) {}
        }
        return bundleableList;
    }

    private void indexTree(TreeNode tree) {
        Timber.i("indexTree(%s)", tree.self);
        mIndexDatabase.insertContainer(tree.self, tree.parent);
        //first extract metadata from all tracks in container
        List<Pair<Track,Metadata>> trackMeta = new ArrayList<>(tree.tracks.size());
        for (Track item : tree.tracks) {
            numTotal.incrementAndGet();
            if (mIndexDatabase.trackNeedsScan(item)) {
                Track.Res res = item.getResources().get(0);
                final Metadata meta = mMetaExtractor.extractMetadata(res);
                if (meta != null) {
                    trackMeta.add(Pair.create(item, meta));
                } else {
                    notifyError(item.getUri());
                }
            } else {
                notifySkipped(item.getUri());
            }
        }
        //Second fixup any descrepancies with albumartist/trackartist
        Set<String> albumArtists = new HashSet<>();
        Set<String> artists = new HashSet<>();
        for (Pair<Track,Metadata> pair : trackMeta) {
            String albumArtist = pair.second.getString(KEY_ALBUM_ARTIST_NAME);
            if (!StringUtils.isEmpty(albumArtist)) {
                albumArtists.add(albumArtist);
            }
            String artist = pair.second.getString(KEY_ARTIST_NAME);
            if (!StringUtils.isEmpty(artist)) {
                //strip of any featured artists
                artists.add(LastFMHelper.resolveAlbumArtistFromTrackArtist(artist));
            }
        }
        //not all the tracks had album artist set,
        //but we only have one artist, so use that as album artist for everyone
        if (albumArtists.size() != trackMeta.size() && artists.size() == 1) {
            final String artist = artists.toArray(new String[1])[0]; //FIXME ugly
            final List<Pair<Track,Metadata>> oldTrackMeta = new ArrayList<>();
            oldTrackMeta.addAll(trackMeta);
            trackMeta.clear();
            for (Pair<Track,Metadata> pair : oldTrackMeta) {
                Metadata m = pair.second.buildUpon()
                        .putString(KEY_ALBUM_ARTIST_NAME, artist)
                        .build();
                trackMeta.add(Pair.create(pair.first, m));
            }
        }
        //add everyone to the db
        for (Pair<Track,Metadata> pair : trackMeta) {
            final Track track = pair.first;
            final Metadata meta = pair.second;
            final boolean success =
                    mIndexDatabase.insertTrack(track, meta) > 0;
            if (success) {
                notifySuccess(track.getUri());
            } else {
                notifyError(track.getUri());
            }
        }
        //continue walking the tree
        for (TreeNode node : tree.children) {
            indexTree(node);
        }
    }

    void removeDifference(TreeNode currentTree, TreeNode newTree) {
        if (!currentTree.self.equals(newTree.self)) {
            Timber.e("Mismatched trees not continuing %s != %s", currentTree.self, newTree.self);
            return;
        }
        //first remove all the tracks not in the current
        for (Track currentTrack : currentTree.tracks) {
            if (currentTrack == null) {
                Timber.e("Null track in currentTree");
                continue;
            }
            boolean found = false;
            for (Track newTrack : newTree.tracks) {
                if (newTrack == null) {
                    Timber.e("Null track in newTree");
                    continue;
                }
                if (currentTrack.getUri().equals(newTrack.getUri())
                        && currentTrack.getParentUri().equals(newTrack.getParentUri())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Timber.d("Removing stale track %s", currentTrack.getUri());
                mIndexDatabase.removeTrack(currentTrack.getUri(), currentTrack.getParentUri());
            }
        }
        //now walk down the tree removing any containers not in current
        for (TreeNode currentNode : currentTree.children) {
            boolean found = false;
            for (TreeNode newNode : newTree.children) {
                if (currentNode.self.equals(newNode.self) &&
                        currentNode.parent.equals(newNode.parent)) {
                    found = true;
                    //walk down this node and remove anything not present
                    removeDifference(currentNode, newNode);
                    break;
                }
            }
            if (!found) {
                Timber.d("Removing stale container %s", currentNode.self);
                mIndexDatabase.removeContainer(currentNode.self);
            }
        }
    }

}
