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
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.android.volley.VolleyError;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkLoader.ImageContainer;
import org.opensilk.music.artwork.ArtworkLoader.ImageListener;

import java.lang.ref.WeakReference;

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

    private ArtInfo mArtInfo;

    private ArtworkType mImageType;

    /**
     * Resource ID of the image to be used as a placeholder until the network image is loaded.
     */
    private int mDefaultImageId;

    /** Local copy of the ImageLoader. */
    private ArtworkLoader mImageLoader;

    /** Current ImageContainer. (either in-flight or finished) */
    private ImageContainer mImageContainer;

    private Palette.PaletteAsyncListener mPaletteListener;

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
     * NOTE: If applicable, {@link org.opensilk.music.artwork.ArtworkImageView#setDefaultImageResId(int)} and
     * {@link org.opensilk.music.artwork.ArtworkImageView#setErrorImageResId(int)} should be called prior to calling
     * this function.
     *
     * @param url The URL that should be loaded into this ImageView.
     * @param imageLoader ImageLoader that will be used to make the request.
     */
    public void setImageInfo(ArtInfo info, ArtworkLoader imageLoader) {
        if (info == null) {
            throw new NullPointerException("ArtInfo cannot be null");
        }
        mArtInfo = info;
        mImageLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }

    /**
     * Sets maximum width in pixels we expect the image to be displayed, the imagefetcher will
     * downsample the fetched bitmap as needed to reduce size/ memory consumption.
     */
    public void setImageType(ArtworkType imageType) {
        mImageType = imageType;
    }

    /**
     * @return requested bitmap width
     */
    public ArtworkType getImageType() {
        return mImageType;
    }

    /**
     * Sets the default image resource ID to be used for this view until the attempt to load it
     * completes.
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    public void installListener(Palette.PaletteAsyncListener l) {
        mPaletteListener = l;
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
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getCacheKey() != null) {
            if (mImageContainer.getCacheKey().equals(ArtworkLoader.getCacheKey(mArtInfo, mImageType))) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
//                setDefaultImageOrNull();
                resetImage();
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mArtInfo, new ResponseListener(this, isInLayoutPass), mImageType);

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private void resetImage() {
        setImageBitmap(null);
    }

    private void setDefaultImageOrNull() {
        if (mDefaultImageId != 0) {
            setImageResource(mDefaultImageId);
            if (mPaletteListener != null) {
                mPaletteListener.onGenerated(null);
            }
        } else {
            resetImage();
        }
    }

    //@DebugLog
    public void cancelRequest() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            resetImage();
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        // clear listener ref
        mPaletteListener = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelRequest();
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    private static class ResponseListener implements ImageListener {
        private final WeakReference<ArtworkImageView> reference;
        private boolean isInLayoutPass;

        private ResponseListener(ArtworkImageView imageView, boolean isInLayoutPass) {
            this.reference = new WeakReference<>(imageView);
            this.isInLayoutPass = isInLayoutPass;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            ArtworkImageView v = reference.get();
            if (v == null) {
                Timber.w("Reference was null");
                return;
            }
            v.setDefaultImageOrNull();
        }

        @Override
        public void onResponse(final ImageContainer response, final boolean isImmediate) {
            ArtworkImageView v = reference.get();
            if (v == null) {
                Timber.w("Reference was null");
                return;
            }
            // If this was an immediate response that was delivered inside of a layout
            // pass do not set the image immediately as it will trigger a requestLayout
            // inside of a layout. Instead, defer setting the image by posting back to
            // the main thread.
            if (isImmediate && isInLayoutPass) {
                isInLayoutPass = false;
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        onResponse(response, isImmediate);
                    }
                });
                return;
            }

            if (response.getBitmap() != null) {
                if (isImmediate) {
                    v.setImageBitmap(response.getBitmap());
                } else {
                    final Drawable[] drawables = new Drawable[] {
                            v.getResources().getDrawable(v.mDefaultImageId),
                            new BitmapDrawable(v.getResources(), response.getBitmap())
                    };
                    final TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
                    transitionDrawable.setCrossFadeEnabled(true);
                    v.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(340);
                }
                if (v.mPaletteListener != null) {
                    Palette.generateAsync(response.getBitmap(), v.mPaletteListener);
                }
            } else if (v.mDefaultImageId != 0) {
                // We missed the L1 cache set the default drawable as fist
                if (isImmediate) {
                    v.setDefaultImageOrNull();
                } else {
                    v.setAlpha(0f);
                    v.animate().alpha(1.0f).setDuration(280).start();
                    v.setDefaultImageOrNull();
                }
            }
        }
    }
}
