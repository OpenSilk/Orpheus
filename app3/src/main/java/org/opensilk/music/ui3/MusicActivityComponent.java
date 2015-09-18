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

package org.opensilk.music.ui3;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.mortarfragment.MortarFragmentActivityComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.main.MainPresenter;

import javax.inject.Named;

/**
 * Requires ActivityResultsOwnerModule
 *
 * Created by drew on 5/1/15.
 */
public interface MusicActivityComponent extends MortarFragmentActivityComponent {
    @ForApplication Context appContext();
    AppPreferences appPreferences();
    ActivityResultsController activityResultsController();
    ArtworkRequestManager artworkRequestor();
    PlaybackController playbackController();
    MainPresenter mainPresenter();
    ToolbarOwner toolbarOwner();
    @Named("IndexProviderAuthority") String indexProviderAuthority();
    IndexClient indexClient();
}
