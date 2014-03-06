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
package org.opensilk.music.ui.cards;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.utils.ApolloUtils;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;

/**
 * Created by drew on 2/11/14.
 */
public abstract class CardBaseThumb<D> extends Card {

    protected D mData;

    public CardBaseThumb(Context context, D data) {
        this(context, data, -1); // -1 will cause super to skip
    }

    public CardBaseThumb(Context context, D data, int innerLayout) {
        super(context, innerLayout);
        mData = data;
        init();
    }

    protected void init() {
        initContent();
        initHeader();
        initThumbnail();
    }

    /**
     * Setup content view
     */
    protected abstract void initContent();

    /**
     * Setup header view
     */
    protected abstract void initHeader();

    protected void initThumbnail() {
        ImageFetcherCardThumbnail cardThumbnail = new ImageFetcherCardThumbnail(mContext);
        cardThumbnail.setExternalUsage(true);
        addCardThumbnail(cardThumbnail);
    }

    public D getData() {
        return mData;
    }


    protected int mThumbnailWidth = 0;
    protected int mThumbnailHeight = 0;

    public void setThumbSize(int width, int height) {
        mThumbnailWidth = width;
        mThumbnailHeight = height;
    }

    /**
     * call appropriate method in ImageFetcher to load image
     * @param fetcher
     * @param view
     */
    protected abstract void loadThumbnail(ImageFetcher fetcher, ImageView view);

    /**
     * Wrapper class to allow dynamic loading of images from the ImageFetcher Instance
     */
    protected class ImageFetcherCardThumbnail extends CardThumbnail {

        public ImageFetcherCardThumbnail(Context context) {
            super(context);
        }

        @Override
        public void setupInnerViewElements(ViewGroup parent, View viewImage) {
            if (mThumbnailWidth > 0) {
                viewImage.getLayoutParams().width = mThumbnailWidth;
            }
            if (mThumbnailHeight > 0) {
                viewImage.getLayoutParams().height = mThumbnailHeight;
            }
            /*
             * If your cardthumbnail uses external library you have to provide how to load the image.
             * If your cardthumbnail doesn't use an external library it will use a built-in method
             */
            final ImageFetcher imageFetcher = ApolloUtils.getImageFetcher((FragmentActivity) getContext());
            loadThumbnail(imageFetcher, (ImageView) viewImage);
        }
    }
}
