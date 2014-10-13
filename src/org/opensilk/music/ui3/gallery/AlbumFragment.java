/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui3.gallery;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.andrew.apollo.R;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.ui3.core.ListGridFragment;
import org.opensilk.silkdagger.DaggerInjector;

import javax.inject.Inject;

import dagger.Module;

/**
 * Created by drew on 10/12/14.
 */
public class AlbumFragment extends ListGridFragment {

    @dagger.Module(
            addsTo = PagerFragment.Module.class,
            injects = AlbumFragment.class
    )
    public static class Module {

    }

    @Inject
    AppPreferences mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getString(R.string.empty_music));
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    boolean wantGridView() {
        return true;
    }

    @Override
    public int getListViewLayout() {
        return wantGridView() ? R.layout.card_staggeredgridview : R.layout.card_listview;
    }

    @Override
    public int getEmptyViewLayout() {
        return R.layout.list_empty_view;
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new Module(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) getParentFragment();
    }
}
