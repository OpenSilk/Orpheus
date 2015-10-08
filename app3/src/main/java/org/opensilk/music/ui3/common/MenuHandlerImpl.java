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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackList;

import java.util.List;

/**
 * Created by drew on 9/24/15.
 */
public abstract class MenuHandlerImpl implements MenuHandler {

    final Uri loaderUri;

    public MenuHandlerImpl(Uri loaderUri) {
        this.loaderUri = loaderUri;
    }

    public void inflateMenu(int item, MenuInflater inflater, Menu menu) {
        inflater.inflate(item, menu);
    }

    public void inflateMenus(MenuInflater inflater, Menu menu, int... items) {
        for (int item : items) {
            inflater.inflate(item, menu);
        }
    }

    public void setNewSortOrder(BundleablePresenter presenter, String sortorder) {
        AppPreferences preferences = presenter.getSettings();
        preferences.putString(preferences.sortOrderKey(loaderUri), sortorder);
        presenter.getLoader().setSortOrder(sortorder);
        presenter.reload();
    }

    public void updateLayout(BundleablePresenter presenter, String kind) {
        AppPreferences preferences = presenter.getSettings();
        preferences.putString(preferences.layoutKey(loaderUri), kind);
        presenter.setWantsGrid(StringUtils.equals(kind, AppPreferences.GRID));
        presenter.resetRecyclerView();
    }

    public void addItemsToQueue(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllEnd(toPlay);
    }

    public void addSelectedItemsToQueue(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getSelectedItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllEnd(toPlay);
    }

    public void playItemsNext(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllNext(toPlay);
    }

    public void playSelectedItemsNext(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getSelectedItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllNext(toPlay);
    }

}
