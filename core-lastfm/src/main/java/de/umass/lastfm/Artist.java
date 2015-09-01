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

import java.util.ArrayList;
import java.util.Collection;

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
