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

import org.opensilk.bundleable.Bundleable;
import org.opensilk.music.model.Model;

import java.util.List;

/**
 * Created by drew on 10/7/15.
 */
public class PlayAllItemClickListener implements ItemClickListener {

    @Override
    public void onItemClicked(BundleablePresenter presenter, Context context, Model item) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        //find its new pos in case there were folders before it in the adapteritems
        int pos = toPlay.indexOf(item.getUri());
        presenter.getPlaybackController().playAll(toPlay, pos);
    }

}
