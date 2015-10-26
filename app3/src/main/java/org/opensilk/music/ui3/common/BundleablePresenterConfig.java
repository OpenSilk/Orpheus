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

import org.opensilk.bundleable.Bundleable;
import org.opensilk.music.model.sort.BaseSortOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by drew on 5/6/15.
 */
public class BundleablePresenterConfig {
    public final boolean wantsGrid;
    public final boolean wantsNumberedTracks;
    public final ItemClickListener itemClickListener;
    public final MenuHandler menuConfig;
    public final List<Bundleable> loaderSeed;
    public final String defaultSortOrder;
    public final boolean allowLongPressSelection;

    public BundleablePresenterConfig(
            boolean wantsGrid,
            boolean wantsNumberedTracks,
            ItemClickListener itemClickListener,
            MenuHandler menuConfig,
            List<Bundleable> loaderSeed,
            String defaultSortOrder,
            boolean allowLongPressSelection
    ) {
        this.wantsGrid = wantsGrid;
        this.wantsNumberedTracks = wantsNumberedTracks;
        this.itemClickListener = itemClickListener;
        this.menuConfig = menuConfig;
        this.loaderSeed = loaderSeed;
        this.defaultSortOrder = defaultSortOrder;
        this.allowLongPressSelection = allowLongPressSelection;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        boolean wantsGrid;
        boolean wantsNumberedTracks;
        ItemClickListener itemClickListener;
        MenuHandler menuConfig;
        List<Bundleable> loaderSeed = new ArrayList<>();
        String preferedSortOrder = BaseSortOrder.A_Z;
        boolean allowLongPressSelection = true;

        public Builder setWantsGrid(boolean wantsGrid) {
            this.wantsGrid = wantsGrid;
            if (wantsGrid) {
                setWantsNumberedTracks(false);
            }
            return this;
        }

        public Builder setWantsNumberedTracks(boolean wantsNumberedTracks) {
            this.wantsNumberedTracks = wantsNumberedTracks;
            if (wantsNumberedTracks) {
                setWantsGrid(false);
            }
            return this;
        }

        public Builder setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
            return this;
        }

        public Builder setMenuConfig(MenuHandler menuConfig) {
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

        public Builder setDefaultSortOrder(String sortOrder) {
            this.preferedSortOrder = sortOrder;
            return this;
        }

        public Builder setAllowLongPressSelection(boolean allowLongPressSelection) {
            this.allowLongPressSelection = allowLongPressSelection;
            return this;
        }

        public BundleablePresenterConfig build() {
            return new BundleablePresenterConfig(wantsGrid, wantsNumberedTracks,
                    itemClickListener, menuConfig,
                    loaderSeed, preferedSortOrder,allowLongPressSelection);
        }
    }
}
