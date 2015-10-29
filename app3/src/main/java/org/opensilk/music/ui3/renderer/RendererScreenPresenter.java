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

package org.opensilk.music.ui3.renderer;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.playback.renderer.client.RendererInfo;
import org.opensilk.music.playback.renderer.client.RendererPluginLoader;
import org.opensilk.music.ui3.common.ActivityRequestCodes;

import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.functions.Action1;

/**
 * Created by drew on 10/27/15.
 */
@ScreenScope
public class RendererScreenPresenter extends ViewPresenter<RendererScreenView>
        implements ItemClickSupport.OnItemClickListener {

    final RendererPluginLoader loader;
    final PlaybackController controller;
    final ActivityResultsController activityResultsController;
    final FragmentManagerOwner fragmentManagerOwner;
    final RendererScreen screen;

    @Inject
    public RendererScreenPresenter(
            RendererPluginLoader loader,
            PlaybackController controller,
            ActivityResultsController activityResultsController,
            FragmentManagerOwner fragmentManagerOwner,
            RendererScreen screen
    ) {
        this.loader = loader;
        this.controller = controller;
        this.activityResultsController = activityResultsController;
        this.fragmentManagerOwner = fragmentManagerOwner;
        this.screen = screen;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        controller.getCurrentRenderer(new Action1<ComponentName>() {
            @Override
            public void call(ComponentName componentName) {
                final List<RendererInfo> infos = loader.getPluginInfos();
                for (RendererInfo info : infos) {
                    if (componentName == null) {
                        //local is null
                        info.setActive(info.getComponentName() == null);
                    } else {
                        info.setActive(componentName.equals(info.getComponentName()));
                    }
                }
                if (hasView()) {
                    getView().getAdapter().replaceAll(infos);
                }
            }
        });
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        if (hasView()) {
            RendererInfo info = getView().getAdapter().getItem(position);
            if (info.hasActivityComponent()) {
                activityResultsController.startActivityForResult(
                        new Intent().setComponent(info.getActivityComponent()),
                        ActivityRequestCodes.RENDERER_PICKER, null);
            } else {
                controller.switchToNewRenderer(info.getComponentName());
            }
            fragmentManagerOwner.dismissDialog(screen.getName());
        }
    }
}
