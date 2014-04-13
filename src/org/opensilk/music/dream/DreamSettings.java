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

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.util.ConfigHelper;

import java.util.List;

/**
 * Created by drew on 4/4/14.
 */
public class DreamSettings extends PreferenceActivity {

    static final String[] VALID_FRAGMENTS;

    static {
        VALID_FRAGMENTS = new String[] {
                AlternateDreamFragment.class.getName(),
                ChooserFragment.class.getName(),
                ConfigurationFragment.class.getName(),
        };
    }

    private MusicUtils.ServiceToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mToken = MusicUtils.bindToService(this, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicUtils.unbindFromService(mToken);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.dream_settings, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (String frag : VALID_FRAGMENTS) {
            if (frag.equals(fragmentName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMultiPane() {
        return ConfigHelper.isXLargeScreen(getResources());
    }
}
