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

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/18/14.
 */
public abstract class BasePresenter extends mortar.Presenter<ProfileView> {

    final ActionBarOwner actionBarOwner;
    final ArtworkRequestManager requestor;

    Subscription loaderSubscription;

    protected BasePresenter(ActionBarOwner actionBarOwner,
                            ArtworkRequestManager requestor) {
        this.actionBarOwner = actionBarOwner;
        this.requestor = requestor;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (isSubscribed(loaderSubscription)) {
            loaderSubscription.unsubscribe();
            loaderSubscription = null;
        }
    }

    @Override
    protected MortarScope extractScope(ProfileView view) {
        return view.getScope();
    }

    abstract String getTitle(Context context);
    abstract String getSubtitle(Context context);
    abstract int getNumArtwork();
    abstract ProfileAdapter makeAdapter(Context context);
    abstract boolean isGrid();

    protected ActionBarOwner.Config getCommonConfig() {
        return new ActionBarOwner.Config.Builder()
                .setTitle(getView().isLandscape() ? getTitle(getView().getContext()) : null)
                .setSubtitle(getView().isLandscape() ? getSubtitle(getView().getContext()) : null)
                .upButtonEnabled(true)
                .setTransparentBackground(!getView().isLandscape())
                .build();
    }

    static CompositeSubscription loadMultiArtwork(ArtworkRequestManager requestor,
                                 long[] albumIds,
                                 AnimatedImageView artwork,
                                 AnimatedImageView artwork2,
                                 AnimatedImageView artwork3,
                                 AnimatedImageView artwork4) {
        return loadMultiArtwork(requestor,
                new CompositeSubscription(),
                albumIds,
                artwork,
                artwork2,
                artwork3,
                artwork4
        );
    }

    static CompositeSubscription loadMultiArtwork(ArtworkRequestManager requestor,
                                 CompositeSubscription cs,
                                 long[] albumIds,
                                 AnimatedImageView artwork,
                                 AnimatedImageView artwork2,
                                 AnimatedImageView artwork3,
                                 AnimatedImageView artwork4) {
        final int num = albumIds.length;
        if (artwork != null) {
            if (num >= 1) {
                cs.add(requestor.newAlbumRequest(artwork, null, albumIds[0], ArtworkType.LARGE));
            } else {
                artwork.setDefaultImage();
            }
        }
        if (artwork2 != null) {
            if (num >= 2) {
                cs.add(requestor.newAlbumRequest(artwork2, null, albumIds[1], ArtworkType.LARGE));
            } else {
                // never get here
                artwork2.setDefaultImage();
            }
        }
        if (artwork3 != null) {
            if (num >= 3) {
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[2], ArtworkType.LARGE));
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[1], ArtworkType.LARGE));
            } else {
                // never get here
                artwork3.setDefaultImage();
            }
        }
        if (artwork4 != null) {
            if (num >= 4) {
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[3], ArtworkType.LARGE));
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[0], ArtworkType.LARGE));
            } else {
                //never get here
                artwork4.setDefaultImage();
            }
        }
        return cs;
    }

}
