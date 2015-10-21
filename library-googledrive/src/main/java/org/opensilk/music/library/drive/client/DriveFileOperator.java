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

package org.opensilk.music.library.drive.client;

import com.google.api.services.drive.model.File;

import org.opensilk.music.model.Model;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by drew on 10/21/15.
 */
public class DriveFileOperator implements Observable.Operator<File, Model> {
    @Override
    public Subscriber<? super Model> call(Subscriber<? super File> subscriber) {
        return null;
    }
}
