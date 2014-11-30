package org.opensilk.music.ui.settings;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;

import org.opensilk.music.R;

/**
 * Created by andrew on 3/1/14.
 */
public abstract class SettingsFragment extends PreferenceFragment {

    protected String mTitle;

    protected PreferenceScreen mPrefSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString("title");
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTitle != null) {
            getActivity().setTitle(mTitle);
        }
    }

    protected String getTitle() { return mTitle; }
}
