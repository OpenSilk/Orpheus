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

package org.opensilk.music.dream;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import org.opensilk.common.util.VersionUtils;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;
import org.opensilk.music.dream.views.ArtOnly;
import org.opensilk.music.dream.views.ArtWithControls;
import org.opensilk.music.dream.views.ArtWithMeta;
import org.opensilk.music.MusicServiceConnection;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import mortar.Mortar;
import mortar.MortarActivityScope;

/**
 * Created by drew on 4/4/14.
 */
public class DreamSettings extends PreferenceActivity {

    public static class BluePrint implements mortar.Blueprint {
        @Override
        public String getMortarScopeName() {
            return getClass().getName();
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }
    }

    @dagger.Module(
            addsTo = AppModule.class,
            injects = {
                    DreamSettings.class,
                    AlternateDreamFragment.class,
                    ChooserFragment.class,
                    ArtOnly.class,
                    ArtWithControls.class,
                    ArtWithMeta.class,
            }
    )
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return new EventBus();
        }
    }

    static final String[] VALID_FRAGMENTS;

    static {
        VALID_FRAGMENTS = new String[] {
                AlternateDreamFragment.class.getName(),
                ChooserFragment.class.getName(),
                ConfigurationFragment.class.getName(),
        };
    }

    @Inject MusicServiceConnection mServiceConnection;

    MortarActivityScope mMortarScope;

    @Override @SuppressWarnings("AppCompatMethod")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMortarScope = Mortar.requireActivityScope(Mortar.getScope(getApplication()), new BluePrint());
        mMortarScope.onCreate(savedInstanceState);
        Mortar.inject(this, this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (VersionUtils.hasLollipop()) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                // Make it look like lollipop
                actionBar.setIcon(R.drawable.ic_action_arrow_left_white);
                actionBar.setHomeButtonEnabled(true);
            }
        }

        mServiceConnection.bind();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mServiceConnection.unbind();

        if (mMortarScope != null) {
            Mortar.getScope(getApplication()).destroyChild(mMortarScope);
            mMortarScope = null;
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mMortarScope;
        }
        return super.getSystemService(name);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.dream_settings, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (String frag : VALID_FRAGMENTS) {
            if (frag.equals(fragmentName)) return true;
        }
        return false;
    }

}
