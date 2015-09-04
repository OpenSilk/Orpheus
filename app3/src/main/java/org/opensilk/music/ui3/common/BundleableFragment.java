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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.mortar.ToolbarOwnerDelegate;
import org.opensilk.common.ui.mortar.ToolbarOwnerScreen;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.LauncherActivityComponent;
import org.opensilk.music.ui3.MusicActivityComponent;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 5/5/15.
 */
public abstract class BundleableFragment extends MortarFragment {

    protected LibraryConfig mLibraryConfig;
    protected LibraryInfo mLibraryInfo;
    protected String mTitle;

    Toolbar mToolbar;
    @Inject ToolbarOwner mToolbarOwner;
    ToolbarOwnerDelegate<BundleableFragment> mToolbarOwnerDelegate;

    protected static Bundle makeCommonArgsBundle(LibraryConfig config, LibraryInfo info, String title) {
        return makeCommonArgsBundle(config.dematerialize(), info, title);
    }

    protected static Bundle makeCommonArgsBundle(Bundle config, LibraryInfo info, String title) {
        Bundle b = new Bundle();
        b.putBundle("config", config);
        b.putParcelable("info", info);
        b.putString("title", title);
        return b;
    }

    protected void extractCommonArgs() {
        mLibraryConfig = LibraryConfig.materialize(getArguments().getBundle("config"));
        mLibraryInfo = getArguments().getParcelable("info");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mToolbar = ButterKnife.findById(view, R.id.screen_toolbar);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        if (mToolbar != null) {
//            mToolbarOwnerDelegate = new ToolbarOwnerDelegate<>(this, mToolbarOwner, mToolbar);
//            mToolbarOwnerDelegate.onCreate();
//            setupActionBar();
//            //Do this after so it can override home botton
//            notifyDrawerOfToolbarChange();
//        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        if (mToolbar != null) {
//            mToolbar = null;
//            mToolbarOwnerDelegate.onDestroy();
//            mToolbarOwnerDelegate = null;
//            //Do last so mToolbar will pass as null
//            notifyDrawerOfToolbarChange();
//        }
    }

    private void notifyDrawerOfToolbarChange() {
        MusicActivityComponent component = DaggerService.getDaggerComponent(getActivity());
        if (component instanceof LauncherActivityComponent) {
            LauncherActivityComponent launcherActivityComponent = (LauncherActivityComponent)component;
            DrawerOwner drawerOwner = launcherActivityComponent.drawerOwner();
            if (drawerOwner != null) {
                drawerOwner.setToolbar(mToolbar);
            }
        }
    }

    protected void setupActionBar() {
        mTitle = getArguments().getString("title");
        BundleableComponent component = DaggerService.getDaggerComponent(getScope());
        BundleablePresenter presenter = component.presenter();
        ActionBarConfig config = ActionBarConfig.builder()
                .setTitle(mTitle != null ? mTitle : "")
                .setMenuConfig(presenter.getMenuConfig()).build();
        mToolbarOwner.setConfig(config);
    }
}
