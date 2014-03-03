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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Bean that contains artist information.<br/> This class contains static methods that executes API methods relating to artists.<br/> Method
 * names are equivalent to the last.fm API method names.
 *
 * @author Janni Kovacs
 */
public class Artist extends MusicEntry {

    static final ItemFactory<Artist> FACTORY = new ArtistFactory();

    private Collection<Artist> similar = new ArrayList<Artist>();

    protected Artist(String name, String url) {
        super(name, url);
    }

    protected Artist(String name, String url, String mbid) {
        super(name, url, mbid);
    }

    /**
     * Returns a list of similar <code>Artist</code>s. Note that this method does not retrieve this list from the server but instead returns
     * the result of an <code>artist.getInfo</code> call.<br/> If you need to retrieve similar artists to a specified artist use the {@link
     * #getSimilar(String, String)} method.
     *
     * @return list of similar artists
     * @see #getSimilar(String, String)
     * @see #getSimilar(String, int, String)
     */
    public Collection<Artist> getSimilar() {
        return similar;
    }

    /**
     * Retrieves detailed artist info for the given artist or mbid entry.
     *
     * @param artistOrMbid Name of the artist or an mbid
     * @param apiKey The API key
     * @return detailed artist info
     */
    public static Artist getInfo(Context context, String artistOrMbid) {
        return getInfo(context, artistOrMbid, Locale.getDefault());
    }

    /**
     * Retrieves detailed artist info for the given artist or mbid entry.
     *
     * @param artistOrMbid Name of the artist or an mbid
     * @param locale The language to fetch info in, or <code>null</code>
     * @param username The username for the context of the request, or <code>null</code>. If supplied, the user's playcount for this artist is
     * included in the response
     * @param apiKey The API key
     * @return detailed artist info
     */
    public static Artist getInfo(Context context, String artistOrMbid, Locale locale) {
        Map<String, String> params = new HashMap<String, String>();
        if (StringUtilities.isMbid(artistOrMbid)) {
            params.put("mbid", artistOrMbid);
        } else {
            params.put("artist", artistOrMbid);
        }
        if (locale != null && locale.getLanguage().length() != 0) {
            params.put("lang", locale.getLanguage());
        }
        Result result;
        try {
            result = Caller.getInstance(context).call("artist.getInfo", params);
        } catch (CallException ignored) {
            return null;
        }
        return ResponseBuilder.buildItem(result, Artist.class);
    }

    /**
     * Calls {@link #getSimilar(String, int, String)} with the default limit of 100.
     *
     * @param artist Artist's name
     * @param apiKey The API key
     * @return similar artists
     * @see #getSimilar(String, int, String)
     */
    public static Collection<Artist> getSimilar(Context context, String artist) {
        return getSimilar(context, artist, 100);
    }

    /**
     * Returns <code>limit</code> similar artists to the given one.
     *
     * @param artist Artist's name
     * @param limit Number of maximum results
     * @param apiKey The API key
     * @return similar artists
     */
    public static Collection<Artist> getSimilar(Context context, String artist, int limit) {
        Result result;
        try {
            result = Caller.getInstance(context).call("artist.getSimilar", "artist", artist, "limit", String.valueOf(limit));
        } catch (CallException ignored) {
            return null;
        }
        return ResponseBuilder.buildCollection(result, Artist.class);
    }

    /**
     * Searches for an artist and returns a <code>Collection</code> of possible matches.
     *
     * @param name The artist name to look up
     * @param apiKey The API key
     * @return a list of possible matches
     */
    public static Collection<Artist> search(Context context, String name) {
        Result result;
        try {
            result = Caller.getInstance(context).call("artist.search", "artist", name);
        } catch (CallException ignored) {
            return null;
        }
        Collection<DomElement> children = result.getContentElement().getChild("artistmatches").getChildren("artist");
        List<Artist> list = new ArrayList<Artist>(children.size());
        for (DomElement c : children) {
            list.add(FACTORY.createItemFromElement(c));
        }
        return list;
    }

    /**
     * Use the last.fm corrections data to check whether the supplied artist has a correction to a canonical artist. This method returns a new
     * {@link Artist} object containing the corrected data, or <code>null</code> if the supplied Artist was not found.
     *
     * @param artist The artist name to correct
     * @param apiKey A Last.fm API key
     * @return a new {@link Artist}, or <code>null</code>
     */
    public static Artist getCorrection(Context context, String artist) {
        Result result;
        try {
            result = Caller.getInstance(context).call("artist.getCorrection", "artist", artist);
        } catch (CallException ignored) {
            return null;
        }
        if (!result.isSuccessful())
            return null;
        DomElement correctionElement = result.getContentElement().getChild("correction");
        if (correctionElement == null)
            return new Artist(artist, null);
        DomElement artistElem = correctionElement.getChild("artist");
        return FACTORY.createItemFromElement(artistElem);
    }

    private static class ArtistFactory implements ItemFactory<Artist> {
        public Artist createItemFromElement(DomElement element) {
            Artist artist = new Artist(null, null);
            MusicEntry.loadStandardInfo(artist, element);
            // similar artists
            DomElement similar = element.getChild("similar");
            if (similar != null) {
                Collection<DomElement> children = similar.getChildren("artist");
                for (DomElement child : children) {
                    artist.similar.add(createItemFromElement(child));
                }
            }
            return artist;
        }
    }
}
