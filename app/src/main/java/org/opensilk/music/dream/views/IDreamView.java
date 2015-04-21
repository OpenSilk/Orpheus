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

package org.opensilk.music.dream.views;

import org.opensilk.common.mortar.HasScope;
import org.opensilk.music.api.meta.ArtInfo;

/**
 * Created by drew on 4/22/14.
 */
public interface IDreamView extends HasScope {
    void updatePlaystate(boolean playing);
    void updateShuffleState(int mode);
    void updateRepeatState(int mode);
    void updateTrack(String name);
    void updateArtist(String name);
    void updateAlbum(String name);
    void updateArtwork(ArtInfo artInfo);
}
