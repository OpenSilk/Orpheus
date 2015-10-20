/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.profile;

import android.os.Bundle;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import mortar.ViewPresenter;

/**
 * Created by drew on 5/5/15.
 */
@ScreenScope
public class ProfileHeroViewPresenter extends ViewPresenter<ProfileHeroView> {

    final ArtworkRequestManager requestor;
    final List<ArtInfo> artInfos;

    @Inject
    public ProfileHeroViewPresenter(
            ArtworkRequestManager requestor,
            @Named("profile_heros") List<ArtInfo> artInfos
    ) {
        this.requestor = requestor;
        this.artInfos = artInfos;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        UtilsCommon.loadMultiArtwork(
                requestor,
                getView().mArtwork,
                getView().mArtwork2,
                getView().mArtwork3,
                getView().mArtwork4,
                artInfos
        );
    }

}
