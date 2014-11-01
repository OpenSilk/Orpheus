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

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import javax.inject.Inject;

import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;
import timber.log.Timber;

import static android.view.animation.AnimationUtils.loadAnimation;

/**
 * Created by drew on 10/6/14.
 */
public class PluginView extends LinearLayout {

    @Inject
    PluginScreen.Presenter presenter;

    public PluginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

}
