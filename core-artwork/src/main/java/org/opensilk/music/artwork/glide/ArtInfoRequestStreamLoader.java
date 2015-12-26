/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.artwork.glide;

import android.content.Context;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;

import org.opensilk.common.core.util.VersionUtils;

import java.io.InputStream;

/**
 * Created by drew on 12/25/15.
 */
public class ArtInfoRequestStreamLoader implements ModelLoader<ArtInfoRequest, InputStream> {

    final Context context;
    static final boolean hasKitkat = VersionUtils.hasKitkat();

    public ArtInfoRequestStreamLoader(Context context) {
        this.context = context;
    }

    @Nullable @Override
    public LoadData<InputStream> buildLoadData(ArtInfoRequest artInfoRequest, int width, int height, Options options) {
        return new LoadData<>(new ArtInfoKey(artInfoRequest.artInfo),
                hasKitkat ? new ArtInfoRequestStreamFetcherK(context, artInfoRequest)
                        : new ArtInfoRequestStreamFetcher(context, artInfoRequest));
    }

    @Override
    public boolean handles(ArtInfoRequest artInfoRequest) {
        return true;
    }

}
