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

package com.andrew.apollo;

import android.os.Handler;

/**
 * Created by drew on 7/4/14.
 */
public interface IMusicPlayer {

    public long seek(long position);
    public long position();
    public long duration();
    public void play();
    public void pause();
    public boolean canGoNext();
    public boolean isInitialized();
    public void stop(boolean goToIdle);
    public void setNextDataSource(long songId);
    public void setDataSource(long songId, String path);
    // note make isInitialized() return false if cant open
    public void setDataSource(String path);
    public long seekAndPlay(long position);
    public boolean canGoPrev();
    public void setNextDataSource(String path);
    public void setHandler(Handler handler);
    public void setVolume(float volume);
}
