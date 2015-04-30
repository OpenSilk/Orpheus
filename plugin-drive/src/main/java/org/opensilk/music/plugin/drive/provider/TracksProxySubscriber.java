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

import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import rx.Subscriber;
import timber.log.Timber;

/**
 * Convenience class could do away with when you feel like it :-
 *
 * Created by drew on 4/29/15.
 */
class TracksProxySubscriber extends Subscriber<Bundleable> {
    final Subscriber<? super Track> wrapped;

    public TracksProxySubscriber(Subscriber<? super Track> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void onStart() {
        super.onStart();
        wrapped.add(this);
    }

    @Override
    public void onCompleted() {
        wrapped.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        wrapped.onError(e);
    }

    @Override
    public void onNext(Bundleable bundleable) {
        if (bundleable instanceof Track) {
            wrapped.onNext((Track)bundleable);
        } else {
            Timber.w("Passed bundleable was not a track. Is the query right?");
        }
    }
}
