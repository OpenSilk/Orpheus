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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.android.volley.VolleyError;

import org.opensilk.music.artwork.ArtworkLoader.ImageContainer;
import org.opensilk.music.artwork.ArtworkLoader.ImageListener;

import hugo.weaving.DebugLog;

/**
 * Handles fetching an image from a URL as well as the life-cycle of the
 * associated request.
 *
 * Modified form volleys NetworkImageView to fetch album/artist images
 */
public class ArtworkImageView extends ImageView {

    private String mArtistName;

    private String mAlbumName;

    /** Maximum width of image for downsampling downloaded bitmap */
    private int mRequestedWidthPx = 0;

    /** Array used in transition drawable */
    private Drawable[] mDrawables = new Drawable[2];

    /**
     * Resource ID of the image to be used as a placeholder until the network image is loaded.
     */
    private int mDefaultImageId;

    /**
     * Resource ID of the image to be used if the network response fails.
     */
    private int mErrorImageId;

    /** Local copy of the ImageLoader. */
    private ArtworkLoader mImageLoader;

    /** Current ImageContainer. (either in-flight or finished) */
    private ImageContainer mImageContainer;

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
    public void setImageInfo(String artistName, String albumName, ArtworkLoader imageLoader) {
        mArtistName = artistName;
        mAlbumName = albumName;
        mImageLoader = imageLoader;
        // reinitialize the first layer
        mDrawables[0] = new ColorDrawable(getResources().getColor(R.color.transparent));
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }

    /**
     * Sets maximum width in pixels we expect the image to be displayed, the imagefetcher will
     * downsample the fetched bitmap as needed to reduce size/ memory consumption.
     */
    public void setRequestedWidth(int maxWidth) {
        mRequestedWidthPx = maxWidth;
    }

    /**
     * @return requested bitmap width
     */
    public int getRequestedWidth() {
        return mRequestedWidthPx;
    }

    /**
     * Sets the default image resource ID to be used for this view until the attempt to load it
     * completes.
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event that the image
     * requested fails to load.
     */
    public void setErrorImageResId(int errorImage) {
        mErrorImageId = errorImage;
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    @DebugLog
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
        if (TextUtils.isEmpty(mArtistName)) { //Artist name is always not null
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getCacheKey() != null) {
            if (mImageContainer.getCacheKey().equals(ArtworkLoader.getCacheKey(mArtistName, mAlbumName, mRequestedWidthPx, 0))) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mArtistName, mAlbumName,
                new ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mErrorImageId != 0) {
                            setImageResource(mErrorImageId);
                        }
                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        // If this was an immediate response that was delivered inside of a layout
                        // pass do not set the image immediately as it will trigger a requestLayout
                        // inside of a layout. Instead, defer setting the image by posting back to
                        // the main thread.
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    onResponse(response, false);
                                }
                            });
                            return;
                        }

                        if (response.getBitmap() != null) {
                            mDrawables[1] = new BitmapDrawable(getResources(), response.getBitmap());
                            final TransitionDrawable result = new TransitionDrawable(mDrawables);
                            result.setCrossFadeEnabled(true);
                            result.startTransition(200);
                            setImageDrawable(result);
//                            setImageBitmap(response.getBitmap());
                        } else if (mDefaultImageId != 0) {
                            // We missed the L1 cache set the default drawable as fist
                            // layer of transition drawable for smoother effect
                            mDrawables[0] = getResources().getDrawable(mDefaultImageId);
                            // Fade in the default image
                            Drawable[] drawables = new Drawable[2];
                            drawables[0] = new ColorDrawable(getResources().getColor(R.color.transparent));
                            drawables[1] = mDrawables[0];
                            TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
                            transitionDrawable.setCrossFadeEnabled(true);
                            transitionDrawable.startTransition(200);
                            setImageDrawable(transitionDrawable);
//                            setImageResource(mDefaultImageId);
                        }
                    }
                }, mRequestedWidthPx, 0);

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private void setDefaultImageOrNull() {
//        if(mDefaultImageId != 0) {
//            setImageResource(mDefaultImageId);
//        }
//        else {
            setImageBitmap(null);
//        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
