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

package org.opensilk.music.util;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by drew on 7/11/14.
 */
public class MultipleArtworkLoaderTask extends PriorityAsyncTask<Void, Void, Set<ArtInfo>> {
    private final Context context;
    private final long[] albumIds;
    private final List<WeakReference<ArtworkImageView>> views;

    public MultipleArtworkLoaderTask(Context context, long[] albumIds, ArtworkImageView... views) {
        super();
        this.context = context;
        this.albumIds = albumIds;
        this.views = new ArrayList<>(views.length);
        for (ArtworkImageView view : views) {
            // Recycled view hack to prevent previous image from showing on slow devices
            view.setImageInfo(null, null);
            this.views.add(new WeakReference<>(view));
        }
    }

    @Override
    protected Set<ArtInfo> doInBackground(Void... params) {
        Set<ArtInfo> artInfos = new HashSet<>(albumIds.length);
        Cursor c = CursorHelpers.makeLocalAlbumsCursor(context, albumIds);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    ArtInfo info = CursorHelpers.makeArtInfoFromLocalAlbumCursor(c);
                    artInfos.add(info);
                } while (c.moveToNext() && artInfos.size() <= views.size());
            }
            c.close();
        }

        return artInfos;
    }

    @Override
    protected void onPostExecute(Set<ArtInfo> artInfos) {
        final Iterator<ArtInfo> infos = artInfos.iterator();
        final Iterator<WeakReference<ArtworkImageView>> views = this.views.iterator();
        while (infos.hasNext() && views.hasNext()) {
            ArtworkImageView view = views.next().get();
            if (view != null) {
                ArtworkManager.loadImage(infos.next(), view);
            }
        }
    }
}
