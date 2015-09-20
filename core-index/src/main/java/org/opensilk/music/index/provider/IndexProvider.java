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

package org.opensilk.music.index.provider;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.scanner.ScannerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

import static org.opensilk.music.index.provider.IndexUris.M_ALBUMS;
import static org.opensilk.music.index.provider.IndexUris.M_ARTISTS;
import static org.opensilk.music.index.provider.IndexUris.M_TRACKS;
import static org.opensilk.music.index.provider.IndexUris.details;
import static org.opensilk.music.index.provider.IndexUris.makeMatcher;

/**
 * Created by drew on 7/11/15.
 */
public class IndexProvider extends LibraryProvider {
    static final boolean TESTING = false; //for when the tester app doesnt use mortar

    @Inject @Named("IndexProviderAuthority") String mRealAuthority;
    @Inject IndexDatabase mDataBase;

    private UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        final IndexComponent acc;
        if (TESTING) {
            Timber.plant(new Timber.DebugTree());
            acc = IndexComponent.FACTORY.call(getContext());
        } else {
            acc = DaggerService.getDaggerComponent(getContext());
        }
        IndexProviderComponent.FACTORY.call(acc).inject(this);
        super.onCreate();
        //override authority to avoid discover-ability
        mAuthority = mRealAuthority;
        mUriMatcher = makeMatcher(mAuthority);
        return true;
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLabel("index") //Not used but can't be null.
                .build();
    }

    @Override
    protected Bundle callCustom(String method, String arg, Bundle extras) {
        LibraryExtras.Builder reply = LibraryExtras.b();
        switch (method) {
            case Methods.IS_INDEXED: {
                Container c = LibraryExtras.getBundleable(extras);
                long id = mDataBase.hasContainer(c.getUri());
                return reply.putOk((id > 0)).get();
            }
            case Methods.ADD: {
                Intent i = new Intent(getContext(), ScannerService.class)
                        .putExtra(LibraryExtras.INTENT_KEY, extras);
                getContext().startService(i);
                return reply.putOk(true).get();
            }
            case Methods.REMOVE: {
                Container c = LibraryExtras.getBundleable(extras);
                int numremoved = mDataBase.removeContainer(c.getUri());
                return reply.putOk(numremoved > 0).get();
            }
            default: {
                return super.callCustom(method, arg, extras);
            }
        }
    }

    @Override
    protected void listObjsInternal(Uri uri, final IBinder binder, Bundle args) {
        switch (mUriMatcher.match(uri)) {
            case M_ALBUMS: {
                final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                final List<Album> lst = mDataBase.getAlbums(LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_ARTISTS: {
                final BundleableSubscriber<Artist> subscriber = new BundleableSubscriber<>(binder);
                final List<Artist> lst = mDataBase.getArtists(LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_TRACKS: {
                final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                final List<Track> lst = mDataBase.getTracks(LibraryExtras.getSortOrder(args), false);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            default: {
                final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
                subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                        new IllegalArgumentException("Unknown uri: " + uri.toString())));
            }
        }
    }

}
