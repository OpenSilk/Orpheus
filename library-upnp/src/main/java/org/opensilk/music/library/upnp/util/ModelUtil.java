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

package org.opensilk.music.library.upnp.util;

import android.net.Uri;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.upnp.provider.UpnpCDUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;

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
public class ModelUtil {

    public static Folder parseDevice(String authority, Device device) {
        final String id = device.getIdentity().getUdn().getIdentifierString();
        final String label = !StringUtils.isEmpty(device.getDetails().getFriendlyName()) ?
                device.getDetails().getFriendlyName() : device.getDisplayString();
        return Folder.builder()
                .setUri(UpnpCDUris.makeUri(authority, id, null))
                .setParentUri(LibraryUris.rootUri(authority))
                .setName(label)
                .build();
    }

    public static Folder parseFolder(String authority, String device, Container c) {
        final Folder.Builder folder = Folder.builder();
        // mandatory fields
        try {
            folder.setUri(UpnpCDUris.makeUri(authority, device, c.getId()));
            folder.setParentUri(UpnpCDUris.makeUri(authority, device, c.getParentID()));
            folder.setName(c.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // optional fields
        try {
            final int childCount = c.getChildCount();
            folder.setChildCount(childCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folder.build();
    }

    public static Artist parseArtist(String authority, String device,MusicArtist ma) {
        final Artist.Builder artist = Artist.builder();
        // mandatory fields
        try {
            artist.setUri(UpnpCDUris.makeUri(authority, device, ma.getId()));
            artist.setParentUri(UpnpCDUris.makeUri(authority, device, ma.getParentID()));
            artist.setName(ma.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return artist.build();
    }

    public static Album parseAlbum(String authority, String device,MusicAlbum ma) {
        final Album.Builder album = Album.builder();
        // mandatory fields
        try {
            album.setUri(UpnpCDUris.makeUri(authority, device, ma.getId()));
            album.setParentUri(UpnpCDUris.makeUri(authority, device, ma.getParentID()));
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
            album.setTrackCount(count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return album.build();
    }

    public static Track parseSong(String authority, String device, MusicTrack mt) {
        final Track.Builder track = Track.builder();
        Track.Res.Builder res = Track.Res.builder();
        // mandatory fields
        try {
            track.setUri(UpnpCDUris.makeUri(authority, device, mt.getId()));
            track.setName(mt.getTitle());
            final Res firstResource = mt.getFirstResource();
            res.setUri(Uri.parse(firstResource.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // optional fields
        try {
            final String album = mt.getAlbum();
            track.setAlbumName(album);
            final PersonWithRole firstArtist = mt.getFirstArtist();
            final String artist = firstArtist != null ? firstArtist.getName() : null;
            track.setArtistName(artist);
            final Res firstResource = mt.getFirstResource();
            final int duration = parseDuration(firstResource.getDuration());
            res.setDuration(duration);
            final URI artURI = mt.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
            final Uri artUri = artURI != null ? Uri.parse(artURI.toASCIIString()) : null;
            track.setArtworkUri(artUri);
            final String mimeType = firstResource.getProtocolInfo().getContentFormatMimeType().getType();
            res.setMimeType(mimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return track.addRes(res.build()).build();
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
