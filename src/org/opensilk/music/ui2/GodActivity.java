package org.opensilk.music.ui2;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.main.DrawerView;
import org.opensilk.music.ui2.main.God;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Backstack;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;


public class GodActivity extends ActionBarActivity implements
        Flow.Listener {

    @Inject @Named("activity")
    Bus mBus;
    @Inject
    God.Presenter mGodPresenter;

    @InjectView(R.id.drawer_layout)
    DrawerView mDrawerView;

    MortarActivityScope mActivityScope;

    Flow mFlow;
    ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new God());
        mActivityScope.onCreate(savedInstanceState);
        Mortar.inject(this, this);

        mBus.register(this);
        mGodPresenter.takeView(this);

        mFlow = mGodPresenter.getFlow();

        setContentView(R.layout.activity_god);
        ButterKnife.inject(this);

        showScreen((Blueprint) mFlow.getBackstack().current().getScreen(), null);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBus.unregister(this);
        if (mGodPresenter != null) mGodPresenter.dropView(this);

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
        ViewGroup container = mDrawerView.getMainView();

        MortarScope myScope = mActivityScope;
        MortarScope newChildScope = myScope.requireChild(screen);

        View oldChild = mDrawerView.getMainView().getChildAt(0);
        View newChild;

        if (oldChild != null) {
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
                // If it's already showing, short circuit.
                Timber.v("Short circuit");
                return;
            }

            myScope.destroyChild(oldChildScope);
        }

        // Create the new child.
        Context childContext = newChildScope.createContext(this);
        newChild = Layouts.createView(childContext, screen);

//        setAnimation(direction, oldChild, newChild);

        // Out with the old, in with the new.
        if (oldChild != null) container.removeView(oldChild);
        container.addView(newChild);
    }

    //God
    public MortarScope getScope() {
        return mActivityScope;
    }

}
