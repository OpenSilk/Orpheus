/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.mortarflow.MortarPagerAdapter;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.BaseActivity;
import org.opensilk.music.ui2.BaseMortarActivity;
import org.opensilk.music.ui2.main.Main;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import flow.Layout;

/**
 * Created by drew on 11/15/14.
 */
public class ThemePickerActivity extends BaseMortarActivity {

    public static class Blueprint extends BaseMortarActivity.Blueprint {

        public Blueprint(String scopeName) {
            super(scopeName);
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }
    }

    @dagger.Module(includes = BaseMortarActivity.Module.class, injects = ThemePickerActivity.class)
    public static class Module {

    }

    static final String EXTRA_PICKED_THEME = "picked_theme";

    @InjectView(R.id.pager) ViewPager mPager;

    Adapter mAdapter;
    OrpheusTheme mNewTheme;

    @Override
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        boolean lightTheme = !mSettings.isDarkTheme();
        setTheme(lightTheme ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_theme_picker);
        ButterKnife.inject(this);

        OrpheusTheme currentTheme = mSettings.getTheme();
        List<PageScreen> screens = new ArrayList<>(OrpheusTheme.values().length);
        for (OrpheusTheme t : OrpheusTheme.values()) {
            screens.add(new PageScreen(t));
        }
        mAdapter = new Adapter(this, screens, !mSettings.isDarkTheme());
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int i, float v, int i2) { }
            @Override public void onPageScrollStateChanged(int i) { }
            @Override public void onPageSelected(int i) {
                updateTheme(i);
            }
        });
        mPager.setCurrentItem(currentTheme.ordinal());

        setResult(RESULT_CANCELED, new Intent());

    }

    @Override
    public boolean onSupportNavigateUp() {
        onCancel();
        return true;
    }

    @OnClick(R.id.btn_negative) void onCancel() {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    @OnClick(R.id.btn_positive) void onOk() {
        setResult(RESULT_OK,
                new Intent().putExtra(EXTRA_PICKED_THEME, mNewTheme.toString())
        );
        finish();
    }

    void updateTheme(int i) {
        mNewTheme = mAdapter.getTheme(i);
    }

    @Layout(R.layout.settings_theme_picker_page)
    @WithModule(PageScreen.Module.class)
    public static class PageScreen extends Screen {
        OrpheusTheme orpheusTheme;

        PageScreen(OrpheusTheme orpheusTheme) {
            this.orpheusTheme = orpheusTheme;
        }

        @Override
        public String getName() {
            return super.getName() + orpheusTheme.toString();
        }

        @dagger.Module(addsTo = ThemePickerActivity.Module.class, includes = Main.Module.class)
        public static class Module {

        }

    }

    static class Adapter extends MortarPagerAdapter<PageScreen, View> {
        final boolean lightTHeme;

        Adapter(Context context, List<PageScreen> screens, boolean lighTHeme) {
            super(context, screens);
            this.lightTHeme = lighTHeme;
        }

        @Override
        protected Context decorateContext(Context newChildContext, int position) {
            OrpheusTheme theme = getTheme(position);
            return new ContextThemeWrapper(newChildContext, lightTHeme ? theme.light : theme.dark);
        }

        OrpheusTheme getTheme(int position) {
            return screens.get(position).orpheusTheme;
        }
    }

}
