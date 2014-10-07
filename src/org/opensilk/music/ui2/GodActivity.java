package org.opensilk.music.ui2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.ui2.main.ActionBarPresenter;
import org.opensilk.music.ui2.main.DrawerPresenter;
import org.opensilk.music.ui2.main.GodScreen;
import org.opensilk.music.ui2.main.DrawerView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Flow;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;


public class GodActivity extends ActionBarActivity implements ActionBarPresenter.Owner {

    @InjectView(R.id.drawer_layout)
    DrawerView mDrawerView;

    protected MortarActivityScope mActivityScope;

    Flow mFlow;
    ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new GodScreen());
        mActivityScope.onCreate(savedInstanceState);
//        Mortar.inject(this, this);

        setContentView(R.layout.activity_god);
        ButterKnife.inject(this);

        mFlow = mDrawerView.getFlow();

        doDrawerSetup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            Timber.d("Destroying Activity scope");
            MortarScope parentScope = Mortar.getScope(getApplication());
            parentScope.destroyChild(mActivityScope);
            mActivityScope = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActivityScope.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerView.isDrawerOpen()) {
//            restoreActionBar();
            getMenuInflater().inflate(R.menu.sleep_timer, menu);
            return super.onCreateOptionsMenu(menu);
        } else {
//            showGlobalContextActionBar();
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_sleep_timer:
                NavUtils.openSleepTimerDialog(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mFlow.goBack()) return;
        super.onBackPressed();
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mActivityScope;
        }
        return super.getSystemService(name);
    }

    private void doDrawerSetup() {

    }

    @Override
    public MortarScope getScope() {
        return null;
    }
}
