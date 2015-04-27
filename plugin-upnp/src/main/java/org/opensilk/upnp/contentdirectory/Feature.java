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

package org.opensilk.upnp.contentdirectory;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DescMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 6/18/14.
 */
public class Feature {

    public static final String SEC_BASICVIEW = "samsung.com_BASICVIEW";

    private String name;
    private int version;
    private final List<DescMeta> meta;

    public Feature() {
        meta = new ArrayList<>();
    }

    public void addMeta(DescMeta descMeta) {
        meta.add(descMeta);
    }

    public String getContainerIdOf(DIDLObject.Class clazz) {
        for (DescMeta m : meta) {
            if (clazz.getValue().equals(m.getType())) {
                return m.getId();
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "(" + name + "):"+version;
    }

}
