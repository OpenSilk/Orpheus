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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarOwner;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.music.R;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;

/**
 * Created by drew on 11/21/14.
 */
public class ProfileLandscapeView extends RelativeLayout {

    @Inject @Named("profile_heros") Boolean wantMultiHeros;
    @Inject @Named("profile_title") String mTitleText;
    @Inject @Named("profile_subtitle") String mSubTitleText;
    @Inject ActionBarOwner mActionBarOwner;

    boolean mLightTheme;

    public ProfileLandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ProfileComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
        mLightTheme = ThemeUtils.isLightTheme(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(getContext()).inflate(
                wantMultiHeros ? R.layout.profile_hero4 : R.layout.profile_hero,
                ButterKnife.<ViewGroup>findById(this, R.id.hero_holder),
                true
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ActionBarConfig c = mActionBarOwner.getConfig().buildUpon()
                .clearTitle()
                .setTitle(mTitleText)
                .setSubtitle(mSubTitleText)
                .build();
        mActionBarOwner.setConfig(c);
    }
}
