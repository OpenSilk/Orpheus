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

package org.opensilk.music.dream.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.opensilk.music.R;

import butterknife.InjectView;

/**
 * Created by drew on 4/13/14.
 */
public class ArtWithMeta extends ArtOnly {

    @InjectView(R.id.track_title) TextView mTrackTitle;
    @InjectView(R.id.artist_name) TextView mArtistName;
    @InjectView(R.id.album_name) TextView mAlbumName;

    public ArtWithMeta(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void updateTrack(String name) {
        mTrackTitle.setText(name);
    }

    @Override
    public void updateArtist(String name) {
        mArtistName.setText(name);
    }

    @Override
    public void updateAlbum(String name) {
        mAlbumName.setText(name);
    }

}
