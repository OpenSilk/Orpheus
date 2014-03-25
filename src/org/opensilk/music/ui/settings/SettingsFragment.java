package org.opensilk.music.ui.settings;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

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
            mIconRes = getArguments().getInt("icon");
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTitle != null && mIconRes != 0) {
            getActivity().getActionBar().setTitle(mTitle);
            getActivity().getActionBar().setIcon(mIconRes);
        }
    }

    protected String getTitle() { return mTitle; }
    protected int getIconRes() { return mIconRes; }

}
