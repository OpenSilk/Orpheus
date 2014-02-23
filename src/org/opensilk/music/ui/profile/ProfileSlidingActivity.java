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

import android.os.Bundle;
import android.provider.MediaStore;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;

import org.opensilk.music.ui.activities.BaseSlidingActivity;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileSlidingActivity extends BaseSlidingActivity {

    /**
     * The Bundle to pass into the Fragments
     */
    private Bundle mArguments;

    /**
     * MIME type of the profile
     */
    private String mType;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the Bundle
        mArguments = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        // Get the MIME type
        mType = mArguments.getString(Config.MIME_TYPE);
        // Load the appropriate fragment
        if (savedInstanceState == null) {
            if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mType)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, AlbumFragment.newInstance(mArguments)).commit();
            } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mType)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, ArtistFragment.newInstance(mArguments)).commit();
            }
        }
    }
}
