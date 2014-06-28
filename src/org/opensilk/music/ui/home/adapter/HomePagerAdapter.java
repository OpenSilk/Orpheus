/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.home.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.andrew.apollo.utils.Lists;

import org.opensilk.music.ui.home.MusicFragment;

import java.util.List;
import java.util.Locale;

/**
 * Created by drew on 6/28/14.
 */
public class HomePagerAdapter extends FragmentPagerAdapter {
    private final Context mContext;
    private final List<FragmentHolder> mHolderList = Lists.newArrayList();

    public HomePagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        FragmentHolder holder = mHolderList.get(position);
        return Fragment.instantiate(mContext, holder.fragment.getFragmentClass().getName(), holder.params);
    }

    @Override
    public int getCount() {
        return mHolderList.size();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        final int id = mHolderList.get(position).fragment.getTitleResource();
        return mContext.getString(id).toUpperCase(Locale.getDefault());
    }

    public void add(final MusicFragment fragment, final Bundle params) {
        mHolderList.add(new FragmentHolder(fragment, params));
    }

    /**
     * A private class with information about fragment initialization
     */
    private final static class FragmentHolder {
        MusicFragment fragment;
        Bundle params;
        private FragmentHolder(MusicFragment fragment, Bundle params) {
            this.fragment = fragment;
            this.params = params;
        }
    }

}
