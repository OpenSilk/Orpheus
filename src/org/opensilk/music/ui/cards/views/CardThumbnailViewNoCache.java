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

package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;

import it.gmariotti.cardslib.library.view.component.CardThumbnailView;

/**
 * Created by drew on 2/16/14.
 */
public class CardThumbnailViewNoCache extends CardThumbnailView {

    public CardThumbnailViewNoCache(Context context) {
        super(context);
    }

    public CardThumbnailViewNoCache(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardThumbnailViewNoCache(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Same as super but without the cardlib cache since apollo has its own.
     */
    @Override
    protected void initView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInternalOuterView = inflater.inflate(card_thumbnail_layout_resourceID,this,true);

        //Get ImageVIew
        mImageView= (ImageView) findViewById(R.id.card_thumbnail_image);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Cancel any pending work
        try {
            ImageFetcher.cancelWork(mImageView);
        } catch (Exception ignored) {} //Just dont explode we don't really care if this fails
    }
}
