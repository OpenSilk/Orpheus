/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.upnp.util;

import android.net.Uri;
import android.text.TextUtils;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/18/14.
 */
public class Helpers {

    public static Folder parseFolder(Container c) {
        final Folder.Builder folder = new Folder.Builder();
        // mandatory fields
        try {
            folder.setIdentity(c.getId());
            folder.setName(c.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // optional fields
        try {
            final String parentId = c.getParentID();
            folder.setParentIdentity(parentId);
            final int childCount = c.getChildCount();
            folder.setChildCount(childCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folder.build();
    }

    public static Artist parseArtist(MusicArtist ma) {
        final Artist.Builder artist = new Artist.Builder();
        // mandatory fields
        try {
            artist.setIdentity(ma.getId());
            artist.setName(ma.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return artist.build();
    }

    public static Album parseAlbum(MusicAlbum ma) {
        final Album.Builder album = new Album.Builder();
        // mandatory fields
        try {
            album.setIdentity(ma.getId());
            album.setName(ma.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // optional fields
        try {
            final PersonWithRole firstArtist = ma.getFirstArtist();
            final String artist = firstArtist != null ? firstArtist.getName() : null;
            album.setArtistName(artist);
            final URI artURI = ma.getFirstAlbumArtURI();
            final Uri artUri = artURI != null ? Uri.parse(artURI.toASCIIString()) : null;
            album.setArtworkUri(artUri);
            //final String date = ma.getDate();
            final int count = ma.getChildCount();
            album.setSongCount(count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return album.build();
    }

    public static Song parseSong(MusicTrack mt) {
        final Song.Builder song = new Song.Builder();
        // mandatory fields
        try {
            song.setIdentity(mt.getId());
            song.setName(mt.getTitle());
            final Res firstResource = mt.getFirstResource();
            song.setDataUri(Uri.parse(firstResource.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // optional fields
        try {
            final String album = mt.getAlbum();
            song.setAlbumName(album);
            final PersonWithRole firstArtist = mt.getFirstArtist();
            final String artist = firstArtist != null ? firstArtist.getName() : null;
            song.setArtistName(artist);
            final Res firstResource = mt.getFirstResource();
            final int duration = parseDuration(firstResource.getDuration());
            song.setDuration(duration);
            final URI artURI = mt.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
            final Uri artUri = artURI != null ? Uri.parse(artURI.toASCIIString()) : null;
            song.setArtworkUri(artUri);
            final String mimeType = firstResource.getProtocolInfo().getContentFormatMimeType().getType();
            song.setMimeType(mimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return song.build();
    }

    public static int parseDuration(String dur) {
        if (TextUtils.isEmpty(dur)) return 0;
        String[] strings = dur.split(":");
        if (strings.length != 3) return 0;
        try {
            int sec = 0;
            if (!TextUtils.isEmpty(strings[0])) {
                sec += TimeUnit.SECONDS.convert(Integer.decode(strings[0]), TimeUnit.HOURS);
            }
            sec += TimeUnit.SECONDS.convert(Integer.decode(strings[1]), TimeUnit.MINUTES);
            sec += TimeUnit.SECONDS.convert(Integer.decode(strings[2].substring(0, 2)), TimeUnit.SECONDS);
            return sec;
        } catch (NumberFormatException e) {
            return 0;
        }

    }

}
