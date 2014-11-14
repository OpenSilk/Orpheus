/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.cast.dialogs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.MediaRouteControllerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opensilk.music.AppModule;
import org.opensilk.music.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.silkdagger.DaggerInjector;

import javax.inject.Inject;

/**
 * A custom {@link MediaRouteControllerDialog} that provides an album art, a play/pause button and
 * the ability to take user to the target activity when the album art is tapped.
 */
public class StyledMediaRouteControllerDialog extends MediaRouteControllerDialog {

    @dagger.Module(addsTo = AppModule.class, injects = StyledMediaRouteControllerDialog.class)
    public static class Module {

    }

    private static final String TAG = StyledMediaRouteControllerDialog.class.getSimpleName();

//    private ThumbnailArtworkImageView mIcon;
    private ImageView mPausePlay;
    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mEmptyText;
    private ProgressBar mLoading;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private View mIconContainer;

    private boolean mIsRemotePlayback;

    @Inject ArtworkRequestManager mRequestor;

    public StyledMediaRouteControllerDialog(Context context) {
        this(context, ThemeHelper.isLightTheme(context) ? R.style.CastDialogLight : R.style.CastDialogDark);
        ((DaggerInjector) context.getApplicationContext()).getObjectGraph().plus(new Module()).inject(this);
    }

    /**
     * Creates a new VideoMediaRouteControllerDialog in the given context.
     */
    public StyledMediaRouteControllerDialog(Context context, int theme) {
        super(context, theme);
        mIsRemotePlayback = MusicUtils.isRemotePlayback();
        if (ThemeHelper.isLightTheme(context)) {
            mPauseDrawable = context.getResources().getDrawable(R.drawable.ic_action_playback_pause_black);
            mPlayDrawable = context.getResources().getDrawable(R.drawable.ic_action_playback_play_black);
        } else {
            mPauseDrawable = ThemeHelper.themeDrawable(context, R.drawable.ic_action_playback_pause_black,
                    getContext().getResources().getColor(android.R.color.white));
            mPlayDrawable = ThemeHelper.themeDrawable(context, R.drawable.ic_action_playback_play_black,
                    getContext().getResources().getColor(android.R.color.white));
        }

    }

    /*
     * Hides/show the icon and metadata and play/pause if there is no media
     */
    private void hideControls(boolean hide, int resId) {
        int visibility = hide ? View.GONE : View.VISIBLE;
//        mIcon.setVisibility(visibility);
        mIconContainer.setVisibility(visibility);
        mPausePlay.setVisibility(visibility);
        mTitle.setVisibility(visibility);
        mSubTitle.setVisibility(visibility);
        mEmptyText.setText(resId == 0 ? R.string.no_media_info : resId);
        mEmptyText.setVisibility(hide ? View.VISIBLE : View.GONE);
    }

    private void updateMetadata() {
        mTitle.setText(MusicUtils.getTrackName());
        mSubTitle.setText(MusicUtils.getArtistName());
//        ArtworkManager.loadCurrentArtwork(mIcon);
    }

    private void updatePlayPauseState() {
        if (null != mPausePlay) {
            if (MusicUtils.isPlaying()) {
                mPausePlay.setVisibility(View.VISIBLE);
                mPausePlay.setImageDrawable(mPauseDrawable);
            } else {
                mPausePlay.setVisibility(View.VISIBLE);
                mPausePlay.setImageDrawable(mPlayDrawable);
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * Initializes this dialog's set of playback buttons and adds click listeners.
     */
    @Override
    public View onCreateMediaControlView(Bundle savedInstanceState) {
        mIsRemotePlayback = MusicUtils.isRemotePlayback();
        if (mIsRemotePlayback) {
            LayoutInflater inflater = getLayoutInflater();
            View controls = inflater.inflate(R.layout.cast_mediarouter_controller_controls, null);
            loadViews(controls);
            updatePlayPauseState();
            updateMetadata();
            setupCallbacks();
            return controls;
        }
        setVolumeControlEnabled(true);
        return super.onCreateMediaControlView(savedInstanceState);
    }

    private void setupCallbacks() {
        mPausePlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                MusicUtils.playOrPause();
                updatePlayPauseState();
            }
        });
    }

    private void loadViews(View controls) {
//        mIcon = (ThumbnailArtworkImageView) controls.findViewById(R.id.iconView);
        mIconContainer = controls.findViewById(R.id.iconContainer);
        mPausePlay = (ImageView) controls.findViewById(R.id.playPauseView);
        mTitle = (TextView) controls.findViewById(R.id.titleView);
        mSubTitle = (TextView) controls.findViewById(R.id.subTitleView);
        mLoading = (ProgressBar) controls.findViewById(R.id.loadingView);
        mEmptyText = (TextView) controls.findViewById(R.id.emptyView);
    }

}
