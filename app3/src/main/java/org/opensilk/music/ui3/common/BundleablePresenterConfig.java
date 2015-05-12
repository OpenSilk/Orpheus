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

import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by drew on 5/6/15.
 */
public class BundleablePresenterConfig {
    public final boolean wantsGrid;
    public final ItemClickListener itemClickListener;
    public final OverflowClickListener overflowClickListener;
    public final ActionBarMenuConfig menuConfig;
    public final List<Bundleable> loaderSeed;

    public BundleablePresenterConfig(
            boolean wantsGrid,
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig,
            List<Bundleable> loaderSeed
    ) {
        this.wantsGrid = wantsGrid;
        this.itemClickListener = itemClickListener;
        this.overflowClickListener = overflowClickListener;
        this.menuConfig = menuConfig;
        this.loaderSeed = loaderSeed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        boolean wantsGrid;
        ItemClickListener itemClickListener;
        OverflowClickListener overflowClickListener;
        ActionBarMenuConfig menuConfig;
        List<Bundleable> loaderSeed = new ArrayList<>();

        public Builder setWantsGrid(boolean wantsGrid) {
            this.wantsGrid = wantsGrid;
            return this;
        }

        public Builder setItemClickListener(ItemClickListener itemClickListener) {
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

        public Builder addLoaderSeed(Bundleable bundleable) {
            loaderSeed.add(bundleable);
            return this;
        }

        public Builder addLoaderSeed(Collection<Bundleable> bundleables) {
            loaderSeed.addAll(bundleables);
            return this;
        }


        public BundleablePresenterConfig build() {
            return new BundleablePresenterConfig(wantsGrid, itemClickListener, overflowClickListener, menuConfig, loaderSeed);
        }
    }
}
