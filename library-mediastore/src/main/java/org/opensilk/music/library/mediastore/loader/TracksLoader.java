/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.library.mediastore.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.StorageLookup;
import org.opensilk.music.library.mediastore.util.FilesHelper;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.Track;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

/**
 * Created by drew on 2/21/14.
 */
@LoaderScope
public class TracksLoader extends RxCursorLoader<Track> {

    final String authority;
    final StorageLookup storageLookup;
    final List<StorageLookup.StorageVolume> volumes;

    @Inject
    public TracksLoader(
            @ForApplication Context context,
            @Named("foldersLibraryAuthority") String authority,
            StorageLookup storageLookup
    ) {
        super(context);
        this.authority = authority;
        this.storageLookup = storageLookup;
        this.volumes = storageLookup.getStorageVolumes();
        setUri(Uris.EXTERNAL_MEDIASTORE_MEDIA);
        setProjection(Projections.AUDIO_FILE);
        setSelection(Selections.LOCAL_SONG);
        setSelectionArgs(SelectionArgs.LOCAL_SONG);
        // need set sortorder
    }

    @Override
    protected Track makeFromCursor(Cursor c) throws Exception {
        File f = new File(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
        StorageLookup.StorageVolume storageVolume = FilesHelper.guessStorageVolume(volumes, f.getAbsolutePath());
        if (storageVolume == null) {
            Timber.e("Unable to locate volume for %s", f.getAbsolutePath());
            throw new IllegalArgumentException("Unable to locate storage volume for " + f.getAbsolutePath());
        }
        Track.Builder tb = FilesHelper.makeTrackFromCursor(authority, storageVolume, f, c);
        return tb.build();
    }

}
