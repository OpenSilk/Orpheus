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

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Total fucking hack job i had no idea what i was doing
 * DO NOT COPY THIS CLASS
 *
 * Created by drew on 3/31/14.
 */
public class ProfileGenrePagerAdapter extends PagerAdapter {

    private static final int ALBUMS = 0;
    private static final int SONGS = 1;
    private static final int PAGE_COUNT = 2;

    private ProfileGenreFragment mFragment;
    private Genre mGenre;

    private Set<ProfileGenrePageBase> mPages = new HashSet<>(2);

    ProfileGenrePagerAdapter(ProfileGenreFragment fragment, Genre genre) {
        mFragment = fragment;
        mGenre = genre;
    }

    @Override
    public void startUpdate(ViewGroup container) {

    }

    @Override
    public void finishUpdate(ViewGroup container) {

    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final ProfileGenrePageBase obj;
        switch (position) {
            case ALBUMS:
                obj = new ProfileGenreAlbumsPage(mFragment, mGenre, container);
                break;
            case SONGS:
                obj = new ProfileGenreSongsPage(mFragment, mGenre, container);
                break;
            default:
                obj = null;
        }
        if (obj != null) {
            mPages.add(obj);
        }
        return obj;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ProfileGenrePageBase obj = (ProfileGenrePageBase) object;
        obj.finish();
        mPages.remove(object);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((ProfileGenrePageBase) object).getView() == view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case ALBUMS:
                return mFragment.getString(R.string.page_albums).toUpperCase(Locale.getDefault());
            case SONGS:
                return mFragment.getString(R.string.page_songs).toUpperCase(Locale.getDefault());
            default:
                return null;
        }
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {

    }

}
