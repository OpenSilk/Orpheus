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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.RelativeLayout;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.music.R;
import org.opensilk.music.api.PluginConfig;

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
        ButterKnife.inject(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        Timber.v("onDetachedFromWindow");
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void showLanding() {
        try {
            String name = presenter.pluginConfig.getMeta(PluginConfig.META_MENU_NAME_PICKER);
            if (!TextUtils.isEmpty(name)) chooser.setText(name);
        } catch (NullPointerException ignored) {/*pluginConfig shouldn't be null but shit happens*/}
        chooser.setVisibility(VISIBLE);
    }

    @OnClick(R.id.btn_chooselibrary)
    public void chooseLibrary() {
        presenter.openPicker();
        chooser.setVisibility(GONE);
    }

    void showUpgradeAlert(final String pluginName, final String packagename) {
        upgradeAlert = new AlertDialog.Builder(getContext())
                .setTitle(R.string.msg_newer_plugin_needed)
                .setMessage(getResources().getString(R.string.msg_newer_plugin_message, pluginName))
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppFlow.loadInitialScreen(getContext());
                    }
                })
                .setPositiveButton(R.string.common_google_play_services_update_button,//Piggyback
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                Uri.parse(getResources().getString(R.string.playstore_package_url, packagename))));
                    }
                })
            .show();
    }

}
