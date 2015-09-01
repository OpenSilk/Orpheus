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


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
