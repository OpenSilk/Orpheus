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

package de.umass.lastfm;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created by drew on 9/16/15.
 */
public class AlbumConverter extends MusicEntryConverter<Album> {
    @Override
    public Album fromBody(ResponseBody responseBody) throws IOException {
        try {
            Result res = createResultFromInputStream(responseBody.byteStream());
            return ResponseBuilder.buildItem(res, Album.class);
        } catch (SAXException e) {
            return null;
        }
    }

    @Override
    public RequestBody toBody(Album album) {
        return null;
    }
}
