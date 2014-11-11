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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.RelativeLayout;

import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
public class PluginView extends RelativeLayout {

    @Inject PluginScreen.Presenter presenter;
    @InjectView(R.id.btn_chooselibrary) Button chooser;

    AlertDialog upgradeAlert;

    public PluginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            presenter.takeView(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    protected void onDetachedFromWindow() {
        Timber.v("onDetachedFromWindow");
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void showLanding() {
        chooser.setVisibility(VISIBLE);
    }

    @OnClick(R.id.btn_chooselibrary)
    public void chooseLibrary() {
        presenter.openPicker();
        chooser.setVisibility(GONE);
    }

    void showUpgradeAlert(String packagename) {
        upgradeAlert = new AlertDialog.Builder(getContext())
                .setTitle()
    }

}
