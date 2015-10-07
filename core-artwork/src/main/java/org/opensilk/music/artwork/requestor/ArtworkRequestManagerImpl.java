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

package org.opensilk.music.artwork.requestor;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.glide.PaletteSwatchType;
import org.opensilk.common.glide.Paletteable;
import org.opensilk.common.glide.PalettizedBitmap;
import org.opensilk.common.glide.PalettizedBitmapTarget;
import org.opensilk.common.glide.PalettizedBitmapTransitionOptions;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.artwork.R;
import org.opensilk.music.model.ArtInfo;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by drew on 10/21/14.
 */
@Singleton
public class ArtworkRequestManagerImpl implements ArtworkRequestManager {

    final Context mContext;
    final String mAuthority;

    @Inject
    public ArtworkRequestManagerImpl(
            @ForApplication Context mContext,
            @Named("artworkauthority") String authority
    ) {
        this.mContext = mContext;
        this.mAuthority = authority;
    }

    @Override
    public Subscription newRequest(AnimatedImageView imageView, final PaletteObserver paletteObserver,
                                   ArtInfo artInfo, ArtworkType artworkType) {
        final Palette.PaletteAsyncListener listener = paletteObserver != null ? new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                paletteObserver.onNext(new PaletteResponse(palette, true));
            }
        } : null;
        final PalettizedBitmapTarget target = newRequest(imageView, listener, artInfo.asUri(mAuthority));
        return Subscriptions.empty();
    }

    public PalettizedBitmapTarget newRequest(final ImageView imageView, Palette.PaletteAsyncListener listener, Uri uri) {
        PalettizedBitmapTarget.Builder bob = PalettizedBitmapTarget.builder().from(imageView);
        if (listener != null) {
            bob.intoCallBack(listener);
        }
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop(imageView.getContext())
                .placeholder(R.drawable.default_artwork)
                ;
        return Glide.with(imageView.getContext())
                .as(PalettizedBitmap.class)
                .apply(options)
                .transition(PalettizedBitmapTransitionOptions.withCrossFade())
                .load(uri)
                .into(bob.build());
    }

    public void newRequest(ArtInfo artInfo, ImageView imageView, @Nullable Paletteable paletteable, @Nullable Bundle extras) {
        newRequest(artInfo.asUri(mAuthority), imageView, paletteable, extras);
    }

    public void newRequest(Uri uri, ImageView imageView, @Nullable Paletteable paletteable, @Nullable Bundle extras) {
        PalettizedBitmapTarget.Builder bob = PalettizedBitmapTarget.builder().from(imageView);
        if (paletteable != null && extras != null) {
            PaletteSwatchType type = PaletteSwatchType.valueOf(BundleHelper.getString(extras));
            PaletteSwatchType fallbackType = PaletteSwatchType.valueOf(BundleHelper.getString2(extras));
            bob.using(type, fallbackType).intoPalettable(paletteable);
        }
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.default_artwork)
                ;
        if (BundleHelper.getInt(extras) == 1) {
            options.circleCrop(imageView.getContext());
        } else {
            options.centerCrop(imageView.getContext());
        }
        Glide.with(imageView.getContext())
                .as(PalettizedBitmap.class)
                .apply(options)
                .transition(PalettizedBitmapTransitionOptions.withCrossFade())
                .load(uri)
                .into(bob.build());
    }

}
