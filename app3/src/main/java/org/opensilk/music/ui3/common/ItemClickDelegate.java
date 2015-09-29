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

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class ItemClickDelegate {

    final PlaybackController playbackController;

    @Inject
    public ItemClickDelegate(
            PlaybackController playbackController
    ) {
        this.playbackController = playbackController;
    }

    public void playAllItems(Context context, List<Bundleable> adapterItems, Bundleable clickedItem) {
        if (adapterItems == null || adapterItems.isEmpty()) {
            return;
        }

        List<Uri> toPlay = new ArrayList<>(adapterItems.size());
        for (Bundleable b : adapterItems) {
            if (b instanceof Track) {
                toPlay.add(((Track) b).getUri());
            }
        }
        if (toPlay.isEmpty()) {
            return;//TODO toast?
        }
        //lazy way to find its new pos in case there were folders before it in the adapteritems
        int pos = toPlay.indexOf(((Model) clickedItem).getUri());

        playbackController.playAll(toPlay, pos);
    }

}
