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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;

import hugo.weaving.DebugLog;

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
        final FragmentActivity activity = (FragmentActivity) context;
        final String songName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
        final long songId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        TextView title = (TextView) view.findViewById(R.id.track_info);
        title.setText(songName);
        View overflowButton = view.findViewById(R.id.overflow_button);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(view.getContext(), view);
                menu.inflate(R.menu.card_song);
                menu.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.card_menu_delete:
                                        DeleteDialog.newInstance(songName, new long[]{
                                                songId
                                        }, null).show(activity.getSupportFragmentManager(), "DeleteDialog");
                                        return true;
                                }
                                return false;
                            }
                        });
                menu.show();
            }
        });
    }

    @Override
    @DebugLog
    protected void onContentChanged() {
        super.onContentChanged();
    }
}
