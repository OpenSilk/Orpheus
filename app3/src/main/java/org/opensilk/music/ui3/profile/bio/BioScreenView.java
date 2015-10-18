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

package org.opensilk.music.ui3.profile.bio;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.Model;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 10/18/15.
 */
public class BioScreenView extends CoordinatorLayout {

    @Inject BioScreenPresenter presenter;
    @Inject ToolbarOwner toolbarOwner;
    @Inject ArtworkRequestManager requestor;

    @InjectView(R.id.recyclerview) RecyclerView recyclerView;
    @InjectView(R.id.toolbar) Toolbar toolbar;
    @InjectView(R.id.loading_progress) ProgressBar progressBar;

    public BioScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            BioScreenComponent cmp = DaggerService.getDaggerComponent(getContext());
            cmp.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (!isInEditMode()) {
            presenter.takeView(this);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            toolbarOwner.attachToolbar(toolbar);
            toolbarOwner.setConfig(presenter.getActionBarConfig());
            presenter.takeView(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        toolbarOwner.detachToolbar(toolbar);
        presenter.dropView(this);
    }

    @DebugLog
    public void onModels(List<Model> model) {
        BioScreenViewAdapter adapter = new BioScreenViewAdapter(requestor);
        adapter.onModels(model);
        recyclerView.setAdapter(adapter);
        progressBar.setVisibility(GONE);
    }
}
