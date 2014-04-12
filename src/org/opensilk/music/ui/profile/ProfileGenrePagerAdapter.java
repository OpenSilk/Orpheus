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

package org.opensilk.music.ui.profile;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.andrew.apollo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * Created by drew on 3/31/14.
 */
public class ProfileGenrePagerAdapter extends FragmentPagerAdapter {

    private static final int ALBUMS = 0;
    private static final int SONGS = 1;
    private static final int PAGE_COUNT = 2;

    private final List<ProfileGenrePageBase> mPages;
    private final String[] mTitles;

    public ProfileGenrePagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mPages = new ArrayList<>(2);
        mTitles = new String[] {
                context.getString(R.string.page_albums).toUpperCase(Locale.getDefault()),
                context.getString(R.string.page_songs).toUpperCase(Locale.getDefault())
        };
    }

    @Override
    public Fragment getItem(int position) {
        ProfileGenrePageBase f = null;
        if (ALBUMS == position) {
            f = ProfileGenreAlbumsPage.newInstance();
        } else if (SONGS == position) {
            f = ProfileGenreSongsPage.newInstance();
        }
        if (f != null) {
            mPages.add(position, f);
        }
        return f;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }
}
