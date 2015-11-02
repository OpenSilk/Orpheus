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

package org.opensilk.music.index.database;

import android.net.Uri;

import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 11/1/15.
 */
public class TreeNode {
    public final Uri self;
    public final Uri parent;
    public final List<TreeNode> children = new ArrayList<>();
    public final List<Track> tracks = new ArrayList<>();

    public TreeNode(Uri self, Uri parent) {
        this.self = self;
        this.parent = parent;
    }
}
