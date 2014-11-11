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

package org.opensilk.music.ui2.library;

import android.widget.PopupMenu;

import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandler;

import rx.functions.Func0;

/**
 * Created by drew on 11/10/14.
 */
public class LibraryOverflowHandlers {

    public static class Bundleables implements OverflowHandler<Bundleable> {

        // Albums, Artists, Folders
        public static int[] MENUS_COLLECTION = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
        };

        public static int[] MENUS_SONG = new int[] {
                R.menu.popup_play_next,
                R.menu.popup_add_to_queue,
        };

        final LibraryScreen.Presenter presenter;

        public Bundleables(LibraryScreen.Presenter presenter) {
            this.presenter = presenter;
        }

        @Override
        public void populateMenu(PopupMenu m, Bundleable item) {
            if (item instanceof Song) {
                for (int ii : MENUS_SONG) {
                    m.inflate(ii);
                }
            } else {
                for (int ii : MENUS_COLLECTION) {
                    m.inflate(ii);
                }
            }

        }

        @Override
        public boolean handleClick(OverflowAction action, Bundleable item) {
            switch (action) {
                case PLAY_ALL:
                case SHUFFLE_ALL:
                case ADD_TO_QUEUE:
                    if (item instanceof  Song) {
                        final Song song = (Song) item;
                        presenter.musicService.enqueueEnd(new Func0<Song[]>() {
                            @Override
                            public Song[] call() {
                                return new Song[]{song};
                            }
                        });
                    } else {
                        presenter.startWork(action, item);
                    }
                    return true;
                case PLAY_NEXT:
                    if (item instanceof Song) {
                        final Song song = (Song) item;
                        presenter.musicService.enqueueNext(new Func0<Song[]>() {
                            @Override
                            public Song[] call() {
                                return new Song[]{song};
                            }
                        });
                        return true;
                    } else { //unsupported
                        return false;
                    }
                default:
                    return false;
            }
        }
    }

}
