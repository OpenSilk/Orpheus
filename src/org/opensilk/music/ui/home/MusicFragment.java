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

package org.opensilk.music.ui.home;

import android.support.v4.app.Fragment;

import com.andrew.apollo.R;

/**
 * Created by drew on 6/28/14.
 */
public enum MusicFragment {
    /**
     * The playlist fragment
     */
    PLAYLIST(PlaylistFragment.class, R.string.page_playlists),
    /**
     * The recent fragment
     */
//    RECENT(HomeRecentFragment.class, R.string.page_recent),
    /**
     * The artist fragment
     */
    ARTIST(ArtistFragment.class, R.string.page_artists),
    /**
     * The album fragment
     */
    ALBUM(AlbumFragment.class, R.string.page_albums),
    /**
     * The song fragment
     */
    SONG(SongFragment.class, R.string.page_songs),
    /**
     * The genre fragment
     */
    GENRE(GenreFragment.class, R.string.page_genres);

    private Class<? extends Fragment> mFragmentClass;
    private int mTitleResource;

    /**
     * Constructor of <code>MusicFragments</code>
     *
     * @param fragmentClass The fragment class
     */
    private MusicFragment(final Class<? extends Fragment> fragmentClass,
                          final int titleResource) {
        mFragmentClass = fragmentClass;
        mTitleResource = titleResource;
    }

    /**
     * Method that returns the fragment class.
     *
     * @return Class<? extends Fragment> The fragment class.
     */
    public Class<? extends Fragment> getFragmentClass() {
        return mFragmentClass;
    }

    public int getTitleResource() {
        return mTitleResource;
    }
}
