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

package org.opensilk.music.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;

import org.opensilk.music.ui.cards.CardSongList;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileAlbumCursorAdapter extends CursorAdapter {

    public ProfileAlbumCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.profile_list, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final CardSongList card = new CardSongList(context, CursorHelpers.makeSongFromCursor(cursor));
        TextView title = (TextView) view.findViewById(R.id.track_info);
        title.setText(card.getData().mSongName);
        View overflowButton = view.findViewById(R.id.overflow_button);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(view.getContext(), view);
                menu.inflate(card.getOverflowMenuId());
                menu.setOnMenuItemClickListener(card.getOverflowPopupMenuListener());
                menu.show();
            }
        });
    }

}
