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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.proj.AlbumProj;
import org.opensilk.music.library.util.CursorUtil;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;

/**
 * Created by drew on 5/2/15.
 */
public class ArtInfoForAlbumLoader extends RxCursorLoader<ArtInfo> {

    public ArtInfoForAlbumLoader(Context context, Uri uri) {
        super(context);
        setUri(uri);
        setProjection(AlbumProj.ALL);
    }

    @Override
    protected ArtInfo makeFromCursor(Cursor c) throws Exception {
        Album album = CursorUtil.fromAlbumCursor(c);
        return Utils.makeBestfitArtInfo(album.artistName, null, album.name, album.artworkUri);
    }
}
