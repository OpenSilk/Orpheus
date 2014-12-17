/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.dream.views;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opensilk.common.rx.RxUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.widgets.AudioVisualizationView;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import mortar.Mortar;
import mortar.MortarScope;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by drew on 12/16/14.
 */
public class VisualizerWave extends RelativeLayout implements IDreamView {

    @Inject ArtworkRequestManager mRequestor;
    @Inject DreamPresenter mPresenter;

    @InjectView(R.id.dream_visualizer) AudioVisualizationView mWaveView;
    @InjectView(R.id.album_art) AnimatedImageView mArtwork;
    @InjectView(R.id.track_name) TextView mTrack;
    @InjectView(R.id.artist_name) TextView mArtist;

    Visualizer mVisualizer;

    public VisualizerWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mWaveView.setWaveColor(Color.WHITE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        RxUtils.observeOnMain(mPresenter.connection.getAudioSessionId())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        initVisualizer(integer);
                    }
                });
        if (!isInEditMode()) mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
        destroyVisualizer();
    }

    void initVisualizer(int id) {
        if (id != AudioEffect.ERROR_BAD_VALUE) {
            try {
                destroyVisualizer();
                mVisualizer = new Visualizer(id);
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                mVisualizer.setDataCaptureListener(new VisualizerListener(),
                        Math.min(Visualizer.getMaxCaptureRate()/2, 7000), true, false);
                mVisualizer.setEnabled(true);
            } catch (RuntimeException e) {
                mWaveView.setVisibility(GONE);
            }
        }
    }

    void destroyVisualizer() {
        if (mVisualizer == null) return;
        mVisualizer.release();
        mVisualizer = null;
    }

    @Override
    public void updatePlaystate(boolean playing) {

    }

    @Override
    public void updateShuffleState(int mode) {

    }

    @Override
    public void updateRepeatState(int mode) {

    }

    @Override
    public void updateTrack(String name) {
        mTrack.setText(name);
    }

    @Override
    public void updateArtist(String name) {
        mArtist.setText(name);
    }

    @Override
    public void updateAlbum(String name) {

    }

    @Override
    public void updateArtwork(ArtInfo artInfo) {
        mRequestor.newAlbumRequest(mArtwork, null, artInfo, ArtworkType.LARGE);
    }

    @Override
    public MortarScope getScope() {
        return Mortar.getScope(getContext());
    }

    class VisualizerListener implements Visualizer.OnDataCaptureListener {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            mWaveView.updateVisualizer(waveform);
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

        }
    }

}
