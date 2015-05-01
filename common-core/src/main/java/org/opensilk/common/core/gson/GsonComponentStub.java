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

package org.opensilk.common.core.gson;

import com.google.gson.Gson;

/**
 * Some libraries need gson. But we want the app to decide where
 * gson is comming from. This allows libraries to satisfy their dependency
 * chains without knowing where gson is coming from.
 *
 * The root component must extend this and provide gson in its @Singleton scope
 *
 * Created by drew on 5/1/15.
 */
public interface GsonComponentStub {
    Gson gson();
}
