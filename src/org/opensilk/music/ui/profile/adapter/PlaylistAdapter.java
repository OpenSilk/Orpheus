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

package org.opensilk.music.ui.profile.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowHandlers;

import butterknife.ButterKnife;

/**
 * Created by drew on 2/18/14.
 */
public class PlaylistAdapter extends SongCollectionAdapter {

    private final long playlistId;

    public PlaylistAdapter(Context context,
                           OverflowHandlers.LocalSongs overflowHandler,
                           ArtworkRequestManager requestor,
                           long playlistId) {
        super(context, overflowHandler, requestor, false);
        this.playlistId = playlistId;
    }

    @Override
    public View newView(Context context, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.gallery_list_item_dragsort, parent, false);
        v.setTag(new ViewHolder(v));
        if (playlistId == -2) ButterKnife.findById(v, R.id.drag_handle).setVisibility(View.GONE);
        return v;
    }

    @Override
    protected void onPostPopulateOverflow(PopupMenu m) {
        m.getMenu().removeItem(R.id.popup_delete);
    }

}
