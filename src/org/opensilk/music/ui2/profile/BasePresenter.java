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

import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import mortar.ViewPresenter;
import rx.Subscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/18/14.
 */
public abstract class BasePresenter extends ViewPresenter<ProfileView> {

    final ActionBarOwner actionBarOwner;
    final ArtworkRequestManager requestor;

    Subscription loaderSubscription;

    protected BasePresenter(ActionBarOwner actionBarOwner,
                            ArtworkRequestManager requestor) {
        this.actionBarOwner = actionBarOwner;
        this.requestor = requestor;
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (isSubscribed(loaderSubscription)) {
            loaderSubscription.unsubscribe();
            loaderSubscription = null;
        }
    }

    abstract String getTitle(Context context);
    abstract String getSubtitle(Context context);
    abstract int getNumArtwork();

    void loadMultiArtwork(long[] albumIds) {
        final int num = getNumArtwork();
        if (getView().mArtwork != null) {
            if (num >= 1) {
                requestor.newAlbumRequest(getView().mArtwork, null, albumIds[0], ArtworkType.LARGE);
            } else {
                getView().mArtwork.setDefaultImage();
            }
        }
        if (getView().mArtwork2 != null) {
            if (num >= 2) {
                requestor.newAlbumRequest(getView().mArtwork2, null, albumIds[1], ArtworkType.LARGE);
            } else {
                // never get here
                getView().mArtwork2.setDefaultImage();
            }
        }
        if (getView().mArtwork3 != null) {
            if (num >= 3) {
                requestor.newAlbumRequest(getView().mArtwork3, null, albumIds[2], ArtworkType.LARGE);
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                requestor.newAlbumRequest(getView().mArtwork3, null, albumIds[1], ArtworkType.LARGE);
            } else {
                // never get here
                getView().mArtwork3.setDefaultImage();
            }
        }
        if (getView().mArtwork4 != null) {
            if (num >= 4) {
                requestor.newAlbumRequest(getView().mArtwork4, null, albumIds[3], ArtworkType.LARGE);
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                requestor.newAlbumRequest(getView().mArtwork4, null, albumIds[0], ArtworkType.LARGE);
            } else {
                //never get here
                getView().mArtwork4.setDefaultImage();
            }
        }
    }

}
