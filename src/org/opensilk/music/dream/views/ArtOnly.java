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
import android.widget.RelativeLayout;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;

import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.widgets.FullScreenArtworkImageView;

/**
 * Created by drew on 4/13/14.
 */
public class ArtOnly extends RelativeLayout implements MusicStateListener {

    protected FullScreenArtworkImageView mArtwork;

    public ArtOnly(Context context) {
        super(context);
    }

    public ArtOnly(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArtOnly(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mArtwork = (FullScreenArtworkImageView) findViewById(R.id.album_art);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateInfo();
    }

    @Override
    public void restartLoader() {

    }

    @Override
    public void onMetaChanged() {
        updateInfo();
    }

    @Override
    public void onPlaystateChanged() {

    }

    @Override
    public void onQueueChanged() {

    }

    @Override
    public void onPlaybackModeChanged() {

    }

    protected void updateInfo() {
        ArtworkManager.loadCurrentArtwork(mArtwork);
    }
}
