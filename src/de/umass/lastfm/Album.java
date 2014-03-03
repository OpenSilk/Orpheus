/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm;

import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Wrapper class for Album related API calls and Album Bean.
 *
 * @author Janni Kovacs
 */
public class Album extends MusicEntry {

    static final ItemFactory<Album> FACTORY = new AlbumFactory();

    private static final DateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateFormat RELEASE_DATE_FORMAT_2 = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z",
            Locale.ENGLISH); /* only used in User.getNewReleases() */
    
    private String artist;
    private Date releaseDate;

    private Album(String name, String url, String artist) {
        super(name, url);
        this.artist = artist;
    }

    private Album(String name, String url, String mbid, String artist) {
        super(name, url, mbid);
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    /**
     * Get the metadata for an album on Last.fm using the album name or a musicbrainz id.
     * See playlist.fetch on how to get the album playlist.
     *
     * @param artist Artist's name
     * @param albumOrMbid Album name or MBID
     * @param apiKey The API key
     * @return Album metadata
     */
    public static Album getInfo(Context context, String artist, String albumOrMbid) {
        Map<String, String> params = new HashMap<String, String>();
        if (StringUtilities.isMbid(albumOrMbid)) {
            params.put("mbid", albumOrMbid);
        } else {
            params.put("artist", artist);
            params.put("album", albumOrMbid);
        }
        Result result;
        try {
            result = Caller.getInstance(context).call("album.getInfo", params);
        } catch (CallException ignored) {
            return null;
        }
        return ResponseBuilder.buildItem(result, Album.class);
    }

    /**
     * Search for an album by name. Returns album matches sorted by relevance.
     *
     * @param album The album name in question.
     * @param apiKey A Last.fm API key.
     * @return a Collection of matches
     */
    public static Collection<Album> search(Context context, String album, String apiKey) {
        Result result;
        try {
            result = Caller.getInstance(context).call("album.search", apiKey, "album", album);
        } catch (CallException ignored) {
            return null;
        }
        DomElement matches = result.getContentElement().getChild("albummatches");
        Collection<DomElement> children = matches.getChildren("album");
        Collection<Album> albums = new ArrayList<Album>(children.size());
        for (DomElement element : children) {
            albums.add(FACTORY.createItemFromElement(element));
        }
        return albums;
    }

    private static class AlbumFactory implements ItemFactory<Album> {
        public Album createItemFromElement(DomElement element) {
            Album album = new Album(null, null, null);
            MusicEntry.loadStandardInfo(album, element);
            if (element.hasChild("artist")) {
                album.artist = element.getChild("artist").getChildText("name");
                if (album.artist == null)
                    album.artist = element.getChildText("artist");
            }
            if (element.hasChild("releasedate")) {
                try {
                    album.releaseDate = RELEASE_DATE_FORMAT.parse(element.getChildText("releasedate"));
                } catch (ParseException e) {
                    // uh oh
                }
            }
            String releaseDateAttribute = element.getAttribute("releasedate");
            if (releaseDateAttribute != null) {
                try {
                    album.releaseDate = RELEASE_DATE_FORMAT_2.parse(releaseDateAttribute);
                } catch (ParseException e) {
                    // uh oh
                }
            }
            return album;
        }
    }
}
