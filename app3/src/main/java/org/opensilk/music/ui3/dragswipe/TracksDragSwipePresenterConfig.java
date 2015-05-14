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

package org.opensilk.music.ui3.dragswipe;

import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowClickListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by drew on 5/6/15.
 */
public class TracksDragSwipePresenterConfig {
    public final TrackItemClickListener itemClickListener;
    public final OverflowClickListener overflowClickListener;
    public final ActionBarMenuConfig menuConfig;
    public final TrackDragSwipeEventListener dragSwipeEventListener;

    public TracksDragSwipePresenterConfig(
            TrackItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig,
            TrackDragSwipeEventListener dragSwipeEventListener
    ) {
        this.itemClickListener = itemClickListener;
        this.overflowClickListener = overflowClickListener;
        this.menuConfig = menuConfig;
        this.dragSwipeEventListener = dragSwipeEventListener;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        TrackItemClickListener itemClickListener;
        OverflowClickListener overflowClickListener;
        ActionBarMenuConfig menuConfig;
        TrackDragSwipeEventListener dragSwipeEventListener;

        public Builder setItemClickListener(TrackItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
            return this;
        }

        public Builder setOverflowClickListener(OverflowClickListener overflowClickListener) {
            this.overflowClickListener = overflowClickListener;
            return this;
        }

        public Builder setMenuConfig(ActionBarMenuConfig menuConfig) {
            this.menuConfig = menuConfig;
            return this;
        }

        public Builder setDragSwipeEventListener(TrackDragSwipeEventListener dragSwipeEventListener) {
            this.dragSwipeEventListener = dragSwipeEventListener;
            return this;
        }

        public TracksDragSwipePresenterConfig build() {
            return new TracksDragSwipePresenterConfig(itemClickListener,
                    overflowClickListener, menuConfig, dragSwipeEventListener);
        }
    }
}
