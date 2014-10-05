package org.opensilk.music.ui2;

import android.app.Activity;
import android.os.Bundle;

import com.andrew.apollo.R;

import org.opensilk.music.ui2.main.DrawerView;
import org.opensilk.music.ui2.main.GodScreen;
import org.opensilk.music.ui2.main.GodView;

import butterknife.ButterKnife;
import flow.Flow;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;


public class GodActivity extends Activity {

    DrawerView drawerView;

    Flow mainFlow;

    protected MortarActivityScope mActivityScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new GodScreen());
        mActivityScope.onCreate(savedInstanceState);
//        Mortar.inject(this, this);

        setContentView(R.layout.activity_main);

        GodView godView = ButterKnife.findById(this, R.id.drawer_layout);
        mainFlow = godView.getFlow();

        drawerView = ButterKnife.findById(this, R.id.drawer_list);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
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
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mActivityScope;
        }
        return super.getSystemService(name);
    }

    public Flow getFlow() {
        return mainFlow;
    }

}
