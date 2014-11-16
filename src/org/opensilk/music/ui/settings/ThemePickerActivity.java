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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.BaseActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by drew on 11/15/14.
 */
public class ThemePickerActivity extends BaseActivity {

    @dagger.Module(includes = BaseActivity.Module.class, injects = ThemePickerActivity.class)
    public static class Module {

    }

    static int[] THEMES_LIGHT = new int[] {
            R.style.Theme_Light,
            R.style.Theme_Light_RedYellow,
            R.style.Theme_Light_RedBlue,
    };

    static int[] THEMES_DARK = new int[] {
            R.style.Theme_Dark,
            R.style.Theme_Dark_RedYellow,
            R.style.Theme_Dark_RedBlue,
    };

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.pager) ViewPager mPager;
    @InjectView(R.id.faux_fab) ImageButton mFab;

    ChooserPagerAdapter mAdapter;

    boolean mLightTheme;
    OrpheusTheme mNewTheme;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        mLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        setTheme(mLightTheme ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);
        ((DaggerInjector) getApplication()).getObjectGraph().plus(new Module()).inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_theme_picker);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAdapter = new ChooserPagerAdapter(this, mLightTheme);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int i, float v, int i2) { }
            @Override public void onPageScrollStateChanged(int i) { }
            @Override public void onPageSelected(int i) {
                updateTheme(i);
            }
        });

        setResult(RESULT_CANCELED, new Intent());

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @OnClick(R.id.btn_negative) void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @OnClick(R.id.btn_positive) void onOk() {
        setResult(RESULT_OK);
        finish();
    }

    void updateTheme(int i) {
        PageHolder h = mAdapter.getPage(i);
        mNewTheme = h.orpheusTheme;
        mToolbar.setBackgroundColor(h.primaryColor);
        ShapeDrawable bg = new ShapeDrawable(new OvalShape());
        bg.getPaint().setColor(h.accentColor);
        mFab.setBackgroundDrawable(bg);
    }

    public static int resolveAttr(Resources.Theme theme, int attr) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attr, outValue, true);
        return outValue.data;
    }

    static class ChooserPagerAdapter extends PagerAdapter {

        final Context mContext;
        final LayoutInflater mInflater;
        final boolean mLightTheme;
        final SparseArrayCompat<PageHolder> mPages = new SparseArrayCompat<>();

        ChooserPagerAdapter(Context context, boolean lightTheme) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mLightTheme = lightTheme;
        }

        PageHolder getPage(int position) {
            return mPages.get(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            OrpheusTheme orpheusTheme = OrpheusTheme.values()[position];
            View v = mInflater.inflate(R.layout.settings_theme_picker_page, container, false);
            PageHolder h = new PageHolder(v, orpheusTheme, mLightTheme);
            mPages.put(position, h);
            container.addView(v);
            return h;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(((PageHolder) object).itemView);
            mPages.remove(position);
        }

        @Override
        public int getCount() {
            return OrpheusTheme.values().length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((PageHolder) object).itemView;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }
    }

    static class PageHolder {
        final View itemView;
        @InjectView(R.id.primary) View primary;
        @InjectView(R.id.secondary) View secondary;
        final OrpheusTheme orpheusTheme;
        final int primaryColor;
        final int primaryColorDark;
        final int accentColor;

        PageHolder(View itemView, OrpheusTheme orpheusTheme, boolean lightTheme) {
            this.itemView = itemView;
            this.orpheusTheme = orpheusTheme;
            // create new theme
            Resources.Theme t = itemView.getResources().newTheme();
            t.applyStyle((lightTheme ? orpheusTheme.light : orpheusTheme.dark), true);
            // resolve the colors for nev theme
            primaryColor = resolveAttr(t, R.attr.colorPrimary);
            primaryColorDark = resolveAttr(t, R.attr.colorPrimaryDark);
            accentColor = resolveAttr(t, R.attr.colorAccent);
            ButterKnife.inject(this, itemView);
            // update the views with the theme colors
            primary.setBackgroundColor(primaryColor);
            secondary.setBackgroundColor(accentColor);
        }
    }

}
