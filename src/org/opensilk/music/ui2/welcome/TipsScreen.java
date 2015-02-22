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

package org.opensilk.music.ui2.welcome;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 2/22/15.
 */
@Layout(R.layout.welcome_tipsscreen)
@WithModule(TipsScreen.Module.class)
public class TipsScreen extends Screen {

    @dagger.Module(addsTo = LauncherActivity.Module.class, injects = TipsView.class)
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<TipsView> {

        final ActionBarOwner actionBarOwner;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner) {
            this.actionBarOwner = actionBarOwner;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            actionBarOwner.setConfig(
                    new ActionBarOwner.Config.Builder()
                            .setTitle(R.string.demo_title)
                            .build()
            );
        }

        void goBack(Context context) {
            AppFlow.get(context).goBack();
        }

    }

    public static final Creator<TipsScreen> CREATOR = new Creator<TipsScreen>() {
        @Override
        public TipsScreen createFromParcel(Parcel source) {
            TipsScreen s = new TipsScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public TipsScreen[] newArray(int size) {
            return new TipsScreen[size];
        }
    };
}
