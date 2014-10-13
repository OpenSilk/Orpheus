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

package org.opensilk.music.ui3;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;
import com.squareup.otto.Bus;

import org.opensilk.music.ui3.theme.Themer;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.support.ScopedDaggerActionBarActivity;

import javax.inject.Named;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/12/14.
 */
public class LauncherActivity extends ScopedDaggerActionBarActivity {

    @dagger.Module (
            injects = LauncherActivity.class,
            library = true,
            complete = false
    )
    public static class Module {
        @Provides @Singleton @ForActivity
        public Bus provideEventBus() {
            return new Bus("activity");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getInstance(this).getPanelTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProgressBar progress = ButterKnife.findById(this, R.id.progress);
        progress.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_purple), PorterDuff.Mode.SRC_IN);
        progress.setProgress(500);
        SeekBar seek = ButterKnife.findById(this, R.id.seek);

        Themer.themeSeekBar(seek);
//        seek.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_purple), PorterDuff.Mode.SRC_IN);
        seek.setProgress(500);
//        seek.getThumb().setColorFilter(getResources().getColor(android.R.color.holo_purple), PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
            new Module(),
        };
    }

}
