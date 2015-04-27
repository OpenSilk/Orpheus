/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.folders;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.RemoteLibraryService;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.plugin.folders.ui.StorageLocationPicker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import static org.opensilk.music.plugin.folders.util.FileUtil.*;

/**
 * Created by drew on 11/13/14.
 */
public class FolderLibraryService extends RemoteLibraryService {

    static class CallbackWrapper extends Subscriber<List<Bundle>> {

        final Result callback;

        CallbackWrapper(Result callback) {
            super();
            this.callback = callback;
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            try {
                if (e instanceof ParcelableException) {
                    callback.onError((ParcelableException) e);
                } else {
                    callback.onError(new ParcelableException(e));
                }
            } catch (RemoteException e2) {
                unsubscribe();
            }

        }

        @Override
        public void onNext(List<Bundle> bundles) {
            try {
                callback.onNext(bundles, null);
            } catch (RemoteException e) {
                unsubscribe();
            }
        }

    }

    @Override
    protected PluginConfig getConfig() {
        return new PluginConfig.Builder()
                .setPickerComponent(new ComponentName(this, StorageLocationPicker.class), getString(R.string.folders_picker_title))
                .build();
    }

    @Override
    protected void browseFolders(@NonNull final String libraryIdentity, @Nullable final String folderIdentity,
                                 int maxResults, @Nullable Bundle paginationBundle, @NonNull Result callback) {
        Observable.create(new Observable.OnSubscribe<List<Bundle>>() {
            @Override
            public void call(Subscriber<? super List<Bundle>> subscriber) {
                //Blatantly ignore maxresults, all in one go for us
                List<Bundle> list = doListing(libraryIdentity, folderIdentity, false);
                subscriber.onNext(list);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).subscribe(new CallbackWrapper(callback));
    }

    @Override
    protected void listSongsInFolder(@NonNull final String libraryIdentity, @Nullable final String folderIdentity,
                                     int maxResults, @Nullable Bundle paginationBundle, @NonNull Result callback) {
        Observable.create(new Observable.OnSubscribe<List<Bundle>>() {
            @Override
            public void call(Subscriber<? super List<Bundle>> subscriber) {
                //Blatantly ignore maxresults, all in one go for us
                List<Bundle> list = doListing(libraryIdentity, folderIdentity, true);
                subscriber.onNext(list);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).subscribe(new CallbackWrapper(callback));
    }

    @Override
    protected void search(@NonNull String libraryIdentity, @NonNull String query,
                          int maxResults, @Nullable Bundle paginationBundle, @NonNull Result callback) {
        throw new UnsupportedOperationException("Folders does not support search");
    }


    List<Bundle> doListing(String libraryIdentity, String folderIdentity, boolean songsOnly) {
        final File base = SECONDARY_STORAGE_ID.equals(libraryIdentity) ? SECONDARY_STORAGE_DIR : PRIMARY_STORAGE_DIR;
        final File rootDir = TextUtils.isEmpty(folderIdentity) ? base : new File(base, folderIdentity);
        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
            return Collections.emptyList();
        }
        File[] dirList = rootDir.listFiles();
        List<Folder> folders = new ArrayList<>(dirList.length);
        List<Song> songs = new ArrayList<>(dirList.length);
        for (File f : dirList) {
            if (!f.canRead()) continue;
            if (f.getName().startsWith(".")) continue;
            if (!songsOnly && f.isDirectory()) {
                folders.add(makeFolder(base, f));
            }
            if (isAudio(this, f)) {
                songs.add(makeSong(this, base, f));
            }
        }
        Collections.sort(folders, new Comparator<Folder>() {
            @Override
            public int compare(Folder lhs, Folder rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        List<Bundle> bundles = new ArrayList<>(folders.size() + songs.size());
        if (!songsOnly) {
            for (Folder f : folders) {
                bundles.add(f.toBundle());
            }
        }
        for (Song s : songs) {
            bundles.add(s.toBundle());
        }
        return bundles;
    }

}
