package org.opensilk.music.ui2;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.main.DrawerPresenter;
import org.opensilk.music.ui2.main.DrawerView;
import org.opensilk.music.ui2.main.God;
import org.opensilk.music.ui2.main.NavScreen;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import flow.Backstack;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;


public class GodActivity extends ActionBarActivity implements
        Flow.Listener,
        DrawerPresenter.View {

    @Inject @Named("activity")
    Bus mBus;
    @Inject
    God.Presenter mGodPresenter;
    @Inject
    DrawerPresenter mDrawerPresenter;

    @InjectView(R.id.drawer_layout) @Optional
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container)
    ViewGroup mNavContainer;
    @InjectView(R.id.main)
    ViewGroup mMainContainer;

    MortarActivityScope mActivityScope;

    Flow mFlow;
    ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getInstance(this).getPanelTheme());
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new God());
        mActivityScope.onCreate(savedInstanceState);
        Mortar.inject(this, this);

        mBus.register(this);
        mGodPresenter.takeView(this);
        mDrawerPresenter.takeView(this);

        mFlow = mGodPresenter.getFlow();

        setContentView(R.layout.activity_god);
        ButterKnife.inject(this);

        setupDrawer();
        setupNavigation();

        showScreen((Blueprint) mFlow.getBackstack().current().getScreen(), null);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBus != null) mBus.unregister(this);
        if (mGodPresenter != null) mGodPresenter.dropView(this);
        if (mDrawerPresenter != null) mDrawerPresenter.dropView(this);

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
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isDrawerOpen()) {
//            showGlobalContextActionBar();
            return false;
        } else {
//            restoreActionBar();
            getMenuInflater().inflate(R.menu.sleep_timer, menu);
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
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

    @Subscribe
    public void onStartActivityForResultEvent(StartActivityForResult req) {
        req.intent.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, ThemeHelper.isLightTheme(this));
        startActivityForResult(req.intent, req.code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
            case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                mBus.post(new ActivityResult(data, requestCode, resultCode));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //Flow
    @Override
    public void go(Backstack nextBackstack, Flow.Direction direction, Flow.Callback callback) {
        Blueprint newScreen = (Blueprint) nextBackstack.current().getScreen();
        showScreen(newScreen, direction);
        callback.onComplete();
    }

    public void showScreen(Blueprint screen, Flow.Direction direction) {
        Timber.v("showScreen()");

        MortarScope newChildScope = mActivityScope.requireChild(screen);

        View oldChild = mMainContainer.getChildAt(0);
        View newChild;

        if (oldChild != null) {
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
                // If it's already showing, short circuit.
                Timber.v("Short circuit");
                return;
            }

            mActivityScope.destroyChild(oldChildScope);
        }

        // Create the new child.
        Context childContext = newChildScope.createContext(this);
        newChild = Layouts.createView(childContext, screen);

//        setAnimation(direction, oldChild, newChild);

        // Out with the old, in with the new.
        if (oldChild != null) mMainContainer.removeView(oldChild);
        mMainContainer.addView(newChild);
    }

    /*
     * HasScope
     */

    public MortarScope getScope() {
        return mActivityScope;
    }

    /*
     * DrawerPresenter.View
     */

    @Override
    public void openDrawer() {
        if (!isDrawerOpen()) mDrawerLayout.openDrawer(mNavContainer);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) mDrawerLayout.closeDrawer(mNavContainer);
    }

    @Override
    public void disableDrawer(boolean hideIndicator) {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(!hideIndicator);
        closeDrawer();
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mNavContainer);
    }

    @Override
    public void enableDrawer() {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(true);
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mNavContainer);
    }

    // Drawer Helpers

    private boolean isDrawerOpen() {
        return mNavContainer != null && mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mNavContainer);
    }

    private void setupDrawer() {
        if (mDrawerLayout == null) return;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
    }

    private void setupNavigation() {
        Blueprint navScreen = new NavScreen();
        MortarScope newChildScope = mActivityScope.requireChild(navScreen);
        View newChild = Layouts.createView(newChildScope.createContext(this), navScreen);
        mNavContainer.addView(newChild);
    }

}
