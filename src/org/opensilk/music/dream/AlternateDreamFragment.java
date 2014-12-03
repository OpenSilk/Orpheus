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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.service.dreams.DreamService;

import org.opensilk.common.util.VersionUtils;
import org.opensilk.music.R;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 4/4/14.
 */
public class AlternateDreamFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = AlternateDreamFragment.class.getSimpleName();

    @Inject DreamPrefs dreamPrefs;

    ComponentName activeDream;
    List<DreamInfo> dreamInfos;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Mortar.inject(activity, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blank_prefscreen);
        // Pull saved dream
        activeDream = dreamPrefs.getAltDreamComponent();
        // Pull list of available dreams
        dreamInfos = getDreamInfos();
        // Add all available dreams to preference screen
        // yea its hackish but much less typing compared to a list view
        for (DreamInfo info : dreamInfos) {
            DreamPreference p = new DreamPreference(getActivity(), info);
            p.setOnPreferenceChangeListener(this);
            getPreferenceScreen().addPreference(p);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (VersionUtils.hasLollipop() && dreamInfos.isEmpty()) {
            dreamPrefs.removeAltDreamComponent();
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dream_settings_alt_dream_l_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dream_settings_alt_dream_warning)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isChecked = ((Boolean) newValue);
        for (int ii=0; ii<getPreferenceScreen().getPreferenceCount(); ii++) {
            DreamPreference dreamPreference = (DreamPreference) getPreferenceScreen().getPreference(ii);
            if (preference == dreamPreference) {
                // Mark dreaminfo as active
                dreamPreference.dreamInfo.isActive = isChecked;
                // Store the new dream component
                if (isChecked) {
                    dreamPrefs.saveAltDreamComponent(dreamPreference.dreamInfo.componentName);
                } else {
                    dreamPrefs.removeAltDreamComponent();
                }
                // update active dream
                activeDream = dreamPreference.dreamInfo.componentName;
            } else {
                // Blanket deselect for all other dreams
                dreamPreference.setChecked(false);
            }
        }
        return true;
    }

    /**
     * Pulls list of dream infos from installed packages
     * from AOSP Settings#DreamBackend.java
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public List<DreamInfo> getDreamInfos() {
        PackageManager pm = getActivity().getPackageManager();
        Intent dreamIntent = new Intent(DreamService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(dreamIntent, PackageManager.GET_META_DATA);
        List<DreamInfo> dreamInfos = new ArrayList<DreamInfo>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null)
                continue;
            if (VersionUtils.hasLollipop()
                    && "android.permission.BIND_DREAM_SERVICE".equals(resolveInfo.serviceInfo.permission)) {
                Timber.w("Ignoring protected daydream %s", getDreamComponentName(resolveInfo).flattenToString());
                continue;
            }
            DreamInfo dreamInfo = new DreamInfo();
            dreamInfo.caption = resolveInfo.loadLabel(pm);
            dreamInfo.icon = resolveInfo.loadIcon(pm);
            dreamInfo.componentName = getDreamComponentName(resolveInfo);
            dreamInfo.isActive = dreamInfo.componentName.equals(activeDream);
            // Dont add ourselves
            if (new ComponentName(getActivity().getPackageName(),
                    DayDreamService.class.getName()).equals(dreamInfo.componentName))
                continue;
            dreamInfos.add(dreamInfo);
        }
        return dreamInfos;
    }

    /**
     * Resolves dream component info
     * from AOSP Settings#DreamBackend.java
     * @param resolveInfo
     * @return
     */
    private static ComponentName getDreamComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null)
            return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    /**
     * from AOSP Settings#DreamBackend.java
     */
    public static class DreamInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        public ComponentName componentName;
        public ComponentName settingsComponentName;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(DreamInfo.class.getSimpleName());
            sb.append('[').append(caption);
            if (isActive)
                sb.append(",active");
            sb.append(',').append(componentName);
            if (settingsComponentName != null)
                sb.append("settings=").append(settingsComponentName);
            return sb.append(']').toString();
        }
    }

    /**
     * Dream preference
     */
    public static class DreamPreference extends CheckBoxPreference {
        DreamInfo dreamInfo;
        public DreamPreference(Context context, DreamInfo info) {
            super(context);
            dreamInfo = info;
            setTitle(info.caption);
            setIcon(info.icon);
            setChecked(info.isActive);
        }
    }

}
