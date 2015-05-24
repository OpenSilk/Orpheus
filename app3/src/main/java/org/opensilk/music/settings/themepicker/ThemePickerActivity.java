/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.settings.themepicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui3.MusicActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import mortar.MortarScope;

/**
 * Created by drew on 11/15/14.
 */
public class ThemePickerActivity extends MusicActivity {

    public static final String EXTRA_PICKED_THEME = "picked_theme";

    @Inject AppPreferences mSettings;

    @InjectView(R.id.pager) ViewPager mPager;

    ThemePickerPagerAdapter mAdapter;
    OrpheusTheme mNewTheme;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                ThemePickerActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.settings_theme_picker);
        ButterKnife.inject(this);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
        boolean lightTheme = !preferences.isDarkTheme();
        setTheme(lightTheme ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);
    }

    @Override
    protected void performInjection() {
        ThemePickerActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OrpheusTheme currentTheme = mSettings.getTheme();
        List<ThemePickerPageScreen> screens = new ArrayList<>(OrpheusTheme.values().length);
        for (OrpheusTheme t : OrpheusTheme.values()) {
            screens.add(new ThemePickerPageScreen(t));
        }
        mAdapter = new ThemePickerPagerAdapter(this, screens, !mSettings.isDarkTheme());
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
        if (mNewTheme == null) {
            onCancel();
            return;
        }
        setResult(RESULT_OK,
                new Intent().putExtra(EXTRA_PICKED_THEME, mNewTheme.toString())
        );
        finish();
    }

    void updateTheme(int i) {
        mNewTheme = mAdapter.getTheme(i);
    }

}
