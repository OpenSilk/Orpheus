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
package org.opensilk.music.artwork;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.android.volley.VolleyError;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.MusicApp;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkLoader.ImageContainer;
import org.opensilk.music.artwork.ArtworkLoader.ImageListener;

import timber.log.Timber;

/**
 * Handles fetching an image from a URL as well as the life-cycle of the
 * associated request.
 *
 * Modified form volleys NetworkImageView to fetch album/artist images
 */
public class ArtworkImageView extends ImageView {
    private static final String TAG = ArtworkImageView.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    // information for current request
    private ArtInfo mArtInfo;
    // image type to pass to ArtworkLoader
    private ArtworkType mArtworkType;
    // Resource ID of the image to be used as a placeholder until the network image is loaded.
    private int mDefaultImageId = -1;
    // Local copy of the ArtworkLoader.
    private ArtworkLoader mArtworkLoader;
    // Current ImageContainer. (either in-flight or finished)
    private ImageContainer mImageContainer;
    // palette
    private Palette.PaletteAsyncListener mPaletteListener;
    private AsyncTask mPaletteTask;

    public ArtworkImageView(Context context) {
        this(context, null);
    }

    public ArtworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArtworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that calling this will
     * immediately either set the cached image (if available) or the default image specified by
     * {@link org.opensilk.music.artwork.ArtworkImageView#setDefaultImageResId(int)} on the view.
     *
     * NOTE: If applicable, {@link org.opensilk.music.artwork.ArtworkImageView#setDefaultImageResId(int)}
     * should be called prior to calling this function.
     */
    public void setImageInfo(ArtInfo info, ArtworkLoader imageLoader) {
        mArtInfo = info;
        mArtworkLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }

    /**
     * Sets maximum width in pixels we expect the image to be displayed, the imagefetcher will
     * downsample the fetched bitmap as needed to reduce size/ memory consumption.
     */
    public void setImageType(ArtworkType imageType) {
        mArtworkType = imageType;
    }

    /**
     * @return requested bitmap size
     */
    public ArtworkType getArtworkType() {
        return mArtworkType;
    }

    /**
     * Sets the default image resource ID to be used for this view until the attempt to load it
     * completes.
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the Palette listener to be called when image is loaded
     */
    public void setPaletteListener(Palette.PaletteAsyncListener l) {
        if (!MusicApp.sIsLowEndHardware) {
            cancelPaletteTask();
            mPaletteListener = l;
        }
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null
                && getLayoutParams().height == LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == LayoutParams.WRAP_CONTENT;
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (mArtInfo == null //If artinfo is null or all fields are null
                || (TextUtils.isEmpty(mArtInfo.artistName)
                    && TextUtils.isEmpty(mArtInfo.albumName)
                    && mArtInfo.artworkUri == null)) {
            Timber.i("Nothing to load. Setting default image");
            cancelRequest();
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getCacheKey() != null) {
            if (mImageContainer.getCacheKey().equals(ArtworkLoader.getCacheKey(mArtInfo, mArtworkType))) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                cancelRequest();
                resetImage();
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network and update the ImageContainer to be the new bitmap container.
        mImageContainer = mArtworkLoader.get(mArtInfo, new ResponseListener(isInLayoutPass), mArtworkType);
    }

    private void resetImage() {
        setImageDrawable(null);
    }

    public void setDefaultImageOrNull() {
        if (mDefaultImageId != -1) {
            setImageResource(mDefaultImageId);
        } else {
            resetImage();
        }
    }

    public void cancelRequest() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            Timber.i("Canceling old Request %s", mArtInfo);
            mImageContainer.cancelRequest();
            // clear out the image from the view.
            resetImage();
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        cancelPaletteTask();
    }

    public void cancelPaletteTask() {
        if (mPaletteTask != null) {
            Timber.i("Canceling old Palette task");
            mPaletteTask.cancel(true);
            mPaletteTask = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelRequest();
        // clear listener ref
        mPaletteListener = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    private class ResponseListener implements ImageListener {
        private boolean isInLayoutPass;
        private boolean isDefaultImageSet;

        private ResponseListener(boolean isInLayoutPass) {
            this.isInLayoutPass = isInLayoutPass;
            this.isDefaultImageSet = false;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            setDefaultImageOrNull();
        }

        @Override
        public void onResponse(final ImageContainer response, final boolean isImmediate) {
            // If this was an immediate response that was delivered inside of a layout
            // pass do not set the image immediately as it will trigger a requestLayout
            // inside of a layout. Instead, defer setting the image by posting back to
            // the main thread.
            if (isImmediate && isInLayoutPass) {
                isInLayoutPass = false;
                post(new Runnable() {
                    @Override
                    public void run() {
                        onResponse(response, isImmediate);
                    }
                });
                return;
            }

            if (response.getBitmap() != null) {
                if (isImmediate) {
                    setImageBitmap(response.getBitmap());
                } else if (isDefaultImageSet) {
                    // we found the image in l2 cache or online, animate the switch from default
                    final TransitionDrawable transition = new TransitionDrawable(new Drawable[] {
                            // we arent immediate so we assume, weve already been called
                            // with a null bitmap and the default image is loaded
                            getResources().getDrawable(mDefaultImageId),
                            new BitmapDrawable(getResources(), response.getBitmap())
                    });
                    transition.setCrossFadeEnabled(true);
                    transition.startTransition(300);
                    setImageDrawable(transition);
                } else {
                    // we found the image but the default image was never set
                    Timber.w("Something weird happened! We forgot to set the default image first.");
                    setAlpha(0f);
                    animate().alpha(1.0f).setDuration(200).start();
                    setImageBitmap(response.getBitmap());
                }
                if (mPaletteListener != null) {
                    mPaletteTask = Palette.generateAsync(response.getBitmap(), mPaletteListener);
                    // release our listener ref
                    mPaletteListener = null;
                }
            } else if (mDefaultImageId != -1) {
                if (isImmediate) {
                    // We missed the L1 cache set the default drawable
                    setDefaultImageOrNull();
                    isDefaultImageSet = true;
                } else if (!isDefaultImageSet) {
                    // we couldnt find the image, fade in default drawable
                    setAlpha(0f);
                    animate().alpha(1.0f).setDuration(200).start();
                    setDefaultImageOrNull();
                    isDefaultImageSet = true;
                }
            }
        }
    }
}
