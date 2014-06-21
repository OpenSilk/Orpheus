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

package org.opensilk.music.artwork;

import android.app.PendingIntent;
import android.os.ParcelFileDescriptor;

/**
 * Created by drew on 3/23/14.
 */
public interface ArtworkService {

    public ParcelFileDescriptor getArtwork(long id);
    public ParcelFileDescriptor getArtworkThumbnail(long id);

    public ParcelFileDescriptor getArtwork(String artistName, String albumName);
    public ParcelFileDescriptor getArtworkThumbnail(String artistName, String albumName);

    public void clearCache();

    public void scheduleCacheClear();
    public void cancelCacheClear();

}
