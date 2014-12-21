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

package org.opensilk.music.artwork;

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

import org.opensilk.common.widget.AnimatedImageView;

import java.lang.ref.WeakReference;

import rx.Subscription;

/**
 * Created by drew on 12/20/14.
 */
public class ImageContainer implements Subscription {
    final WeakReference<AnimatedImageView> imageViewWeakReference;
    final WeakReference<PaletteObserver> palleteObserverWeakReference;

    private boolean unsubscribed;

    ImageContainer(AnimatedImageView imageView, PaletteObserver paletteObserver) {
        this.imageViewWeakReference = new WeakReference<>(imageView);
        this.palleteObserverWeakReference = new WeakReference<>(paletteObserver);
        registerWithImageView();
    }

    @Override
    public void unsubscribe() {
        unsubscribed = true;
        unregisterWithImageView();
        imageViewWeakReference.clear();
        palleteObserverWeakReference.clear();
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    void registerWithImageView() {
        AnimatedImageView imageView = imageViewWeakReference.get();
        if (imageView != null) {
            imageView.addSubscription(this);
        }
    }

    void unregisterWithImageView() {
        AnimatedImageView imageView = imageViewWeakReference.get();
        if (imageView != null) {
            imageView.removeSubscription(this);
        }
    }

    void setDefaultImage() {
        if (unsubscribed) return;
        unregisterWithImageView();
        AnimatedImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return;
        imageView.setDefaultImage();
    }

    void setImageBitmap(final Bitmap bitmap, boolean shouldAnimate) {
        if (unsubscribed) return;
        unregisterWithImageView();
        AnimatedImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return;
        imageView.setImageBitmap(bitmap, shouldAnimate);
    }

    void notifyPaletteObserver(Palette palette, boolean shouldAnimate) {
        PaletteObserver po = palleteObserverWeakReference.get();
        if (po != null) {
            po.onNext(new PaletteResponse(palette, shouldAnimate));
            po.onCompleted();
        }
    }
}
