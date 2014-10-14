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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.opensilk.music.ui2.core.CanShowScreen;
import org.opensilk.music.ui2.core.ScreenConductor;

import javax.inject.Inject;

import flow.Flow;
import mortar.Blueprint;
import mortar.Mortar;

/**
 * Created by drew on 10/13/14.
 */
public class MainView extends FrameLayout implements CanShowScreen<Blueprint> {

    @Inject
    God.Presenter presenter;

    final ScreenConductor<Blueprint> screenConductor;

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        screenConductor = new ScreenConductor<>(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    public void showScreen(Blueprint screen, Flow.Direction direction) {
        screenConductor.showScreen(screen, direction);
    }

    public Flow getFlow() {
        return presenter.getFlow();
    }

}
