package org.opensilk.music.ui.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import org.opensilk.music.R;

import org.opensilk.music.ui2.BaseActivity;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.ui2.BaseActivityModule;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.ObjectGraph;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsActivity extends BaseActivity implements DaggerInjector {

    @dagger.Module(includes = BaseActivityModule.class, injects = SettingsActivity.class)
    public static class Module {
    }

    ObjectGraph mGraph;

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        mGraph = ((DaggerInjector) getApplication()).getObjectGraph().plus(new Module());
        inject(this);

        boolean lightTheme = !mSettings.isDarkTheme();
        setTheme(lightTheme ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.blank_framelayout_toolbar);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);

        if (savedInstanceState == null) {
            //Load the main fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new SettingsMainFragment())
                    .commit();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGraph = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;//dont want mediarouter
    }

    @Override
    public void onBackPressed() {
        // we use system fragment manager with PreferenceFragments
        if (getFragmentManager().popBackStackImmediate()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void inject(Object obj) {
        mGraph.inject(obj);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mGraph;
    }
}
