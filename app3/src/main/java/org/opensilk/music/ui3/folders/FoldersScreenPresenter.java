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

package org.opensilk.music.ui3.folders;

import android.content.Context;
import android.os.Bundle;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.OverflowAction;

import javax.inject.Inject;

import mortar.ViewPresenter;

/**
 * Created by drew on 5/2/15.
 */
@ScreenScope
public class FoldersScreenPresenter extends ViewPresenter<FoldersScreenView> implements BundleablePresenter {

    final FoldersScreen screen;
    final ArtworkRequestManager requestor;
    final FragmentManagerOwner fm;

    @Inject
    public FoldersScreenPresenter(
            FoldersScreen screen,
            ArtworkRequestManager requestor,
            FragmentManagerOwner fm
    ) {
        this.screen = screen;
        this.requestor = requestor;
        this.fm = fm;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    @Override
    public void onItemClicked(Context context, Bundleable item) {
        LibraryInfo info = screen.libraryInfo.buildUpon(item.getIdentity(), item.getName());
        FoldersScreenFragment f = FoldersScreenFragment.newInstance(screen.libraryConfig, info);
        fm.replaceMainContent(f, FoldersScreenFragment.TAG+"-"+info.folderId, true);
    }

    @Override
    public void onOverflowClicked(Context context, PopupMenu m, Bundleable item) {

    }

    @Override
    public boolean onOverflowActionClicked(Context context, OverflowAction action, Bundleable item) {
        return false;
    }

    @Override
    public ArtworkRequestManager getRequestor() {
        return requestor;
    }
}
