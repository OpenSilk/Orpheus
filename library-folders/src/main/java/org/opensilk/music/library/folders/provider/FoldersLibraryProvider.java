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

package org.opensilk.music.library.folders.provider;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.library.folders.BuildConfig;
import org.opensilk.music.library.folders.StorageLookup;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import rx.Subscriber;

import static org.opensilk.music.library.folders.util.FileUtil.PRIMARY_STORAGE_DIR;
import static org.opensilk.music.library.folders.util.FileUtil.SECONDARY_STORAGE_DIR;
import static org.opensilk.music.library.folders.util.FileUtil.SECONDARY_STORAGE_ID;
import static org.opensilk.music.library.folders.util.FileUtil.isAudio;
import static org.opensilk.music.library.folders.util.FileUtil.makeFolder;
import static org.opensilk.music.library.folders.util.FileUtil.makeSong;

/**
 * Created by drew on 4/28/15.
 */
public class FoldersLibraryProvider extends LibraryProvider {

    @Inject StorageLookup mStorageLookup;

    @Override
    public boolean onCreate() {
        return super.onCreate();
    }

    @Override
    protected String getAuthority() {
        return BuildConfig.DEBUG ? "files.debug" : "files";
    }

    @Override
    protected void getFoldersTracks(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        super.getFoldersTracks(library, identity, subscriber, args);
    }

    @Override
    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        super.queryTracks(library, subscriber, args);
    }

    @Override
    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        super.getTrack(library, identity, subscriber, args);
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
            if (isAudio(getContext(), f)) {
                songs.add(makeSong(getContext(), base, f));
            }
        }
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
