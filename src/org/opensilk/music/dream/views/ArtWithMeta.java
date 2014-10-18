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
import com.andrew.apollo.utils.MusicUtils;

/**
 * Created by drew on 4/13/14.
 */
public class ArtWithMeta extends ArtOnly {

    protected TextView mTrackTitle;
    protected TextView mArtistName;
    protected TextView mAlbumName;

    public ArtWithMeta(Context context) {
        super(context);
    }

    public ArtWithMeta(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArtWithMeta(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTrackTitle = (TextView) findViewById(R.id.track_title);
        mArtistName = (TextView) findViewById(R.id.artist_name);
        mAlbumName = (TextView) findViewById(R.id.album_name);
//        update();
    }

    @Override
    public void update() {
        super.update();
        mTrackTitle.setText(MusicUtils.getTrackName());
        mArtistName.setText(MusicUtils.getArtistName());
        mAlbumName.setText(MusicUtils.getAlbumName());
    }
}
