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

package com.andrew.apollo.model;

import java.util.Arrays;

/**
 * Created by drew on 7/10/14.
 */
public class LocalSongGroup {

    public final String name;
    public final long[] songIds;
    public final long[] albumIds;

    public LocalSongGroup(String name, long[] songIds, long[] albumIds) {
        this.name = name;
        this.songIds = songIds;
        this.albumIds = albumIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalSongGroup)) return false;

        LocalSongGroup that = (LocalSongGroup) o;

        if (!Arrays.equals(albumIds, that.albumIds)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (!Arrays.equals(songIds, that.songIds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (songIds != null ? Arrays.hashCode(songIds) : 0);
        result = 31 * result + (albumIds != null ? Arrays.hashCode(albumIds) : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
