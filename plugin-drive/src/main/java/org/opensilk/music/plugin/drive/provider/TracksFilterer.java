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

package org.opensilk.music.plugin.drive.provider;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import rx.functions.Func1;

/**
 * Cant recursively search drive in one request so we get all tracks then
 * filter out the ones not in our root folder
 *
 * Created by drew on 4/29/15.
 */
class TracksFilterer implements Func1<File, Boolean> {
    final String rootFolder;

    public TracksFilterer(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    @Override
    public Boolean call(File file) {
        // I realized this isnt right.
        // TODO will have to get all files and folders check if parent of file is a child of root or one of its prodgeny
        // a sane solution i can think of now is getting all files and all folders then doing the sort
        // ourselves that will only be 2 network calls. but doesnt fit the current flow so leaving as an
        // exercise for later when someone complains about seeing tracks not in their default folder.
        List<ParentReference> parents = file.getParents();
        if (parents != null && !parents.isEmpty()) {
            for (ParentReference p : parents) {
                if (StringUtils.equalsIgnoreCase(rootFolder, p.getId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
