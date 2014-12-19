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
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 4/13/14.
 */
public class ArtOnly extends RelativeLayout implements IDreamView {

    @Inject ArtworkRequestManager mRequestor;
    @Inject DreamPresenter mPresenter;

    @InjectView(R.id.album_art) protected AnimatedImageView mArtwork;

    public ArtOnly(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        if (!isInEditMode()) mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    public void updatePlaystate(boolean playing) {

    }

    public void updateShuffleState(int mode) {

    }

    public void updateRepeatState(int mode) {

    }

    public void updateTrack(String name) {

    }

    public void updateArtist(String name) {

    }

    public void updateAlbum(String name) {

    }

    public void updateArtwork(ArtInfo artInfo) {
        mRequestor.newAlbumRequest(mArtwork, null, artInfo, ArtworkType.LARGE);
    }

    @Override
    public MortarScope getScope() {
        return Mortar.getScope(getContext());
    }
}
