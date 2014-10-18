package org.opensilk.music.ui.settings;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by andrew on 3/1/14.
 */
public abstract class SettingsFragment extends PreferenceFragment {

    protected String mTitle;
    protected int mIconRes;

    protected PreferenceScreen mPrefSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString("title");
            mIconRes = getArguments().getInt("dark_icon");
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTitle != null) {
            ActionBarActivity activity = (ActionBarActivity) getActivity();
            activity.getSupportActionBar().setTitle(mTitle);
        }
    }

    protected String getTitle() { return mTitle; }
    protected int getIconRes() { return mIconRes; }

}
