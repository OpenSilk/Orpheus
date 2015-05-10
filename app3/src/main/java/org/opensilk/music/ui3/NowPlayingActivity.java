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

package org.opensilk.music.ui3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.common.ui.mortar.PauseAndResumeActivity;
import org.opensilk.common.ui.mortar.PauseAndResumePresenter;
import org.opensilk.common.ui.mortarfragment.MortarFragmentActivity;
import org.opensilk.music.AppComponent;
import org.opensilk.music.R;

import javax.inject.Inject;

import mortar.MortarScope;

/**
 * Created by drew on 5/9/15.
 */
public class NowPlayingActivity extends MortarFragmentActivity {

    public static void startSelf(Context context) {
        Intent i = new Intent(context, NowPlayingActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                NowPlayingActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void performInjection() {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;//unsuported
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nowplaying);
    }
}
