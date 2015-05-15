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

package org.opensilk.music.ui3.delete;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.R;
import org.opensilk.music.library.internal.DeleteSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.internal.ResultReceiver;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackCollection;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 5/14/15.
 */
@ScreenScope
public class DeleteScreenPresenter extends ViewPresenter<DeleteScreenView> {

    final Context appContext;
    final DeleteScreen screen;
    final DeleteScreenFragmentPresenter fragmentPresenter;

    final String authority;
    final String libraryId;

    boolean inprogress = false;
    boolean complete = false;
    boolean success = false;
    String error;
    Subscription loaderSubscription;

    @Inject
    public DeleteScreenPresenter(
            @ForApplication Context appContext,
            DeleteScreen screen,
            DeleteScreenFragmentPresenter fragmentPresenter
    ) {
        this.appContext = appContext;
        this.screen = screen;
        this.fragmentPresenter = fragmentPresenter;
        this.authority = screen.libraryConfig.authority;
        this.libraryId = screen.libraryInfo.libraryId;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (screen.bundleable != null) {
            getView().setTitle(screen.bundleable.getName());
        } else if (screen.what != null) {
            getView().setTitle(screen.what);
        } //else todo
        if (complete) {
            if (success) {
                getView().showSuccess();
            } else {
                getView().showError(error);
            }
        } else if (inprogress) {
            getView().gotoLoading();
        }
    }

    void dismissSelf() {
        fragmentPresenter.dismiss();
    }

    void notifySuccess() {
        complete = true;
        success = true;
        if (hasView()) {
            getView().showSuccess();
        }
    }

    void notifyFailure(LibraryException e) {
        complete = true;
        success = false;
        if (e != null && e.getCause() != null) {
            error = e.getCause().getMessage();
        }
        if (hasView()) {
            getView().showError(error);
        }
    }

    void doDelete() {
        inprogress = true;
        if (screen.trackUris != null) {
            deleteTracks(screen.trackUris);
        } else if (screen.bundleable != null) {
            Bundleable b = screen.bundleable;
            if (b instanceof Album) {
                deleteAlbum((Album)b);
            } else if (b instanceof Artist) {
                deleteArtist((Artist)b);
            } else if (b instanceof Folder) {
                deleteFolder((Folder)b);
            } else if (b instanceof Playlist) {
                deletePlaylist((Playlist) b);
            } else if (b instanceof Track) {
                deleteTrack((Track)b);
            } // else TODO
        } // else TODO
    }

    void deleteAlbum(Album album) {
        Uri uri = LibraryUris.albumTracks(authority, libraryId, album.identity);
        deleteTracks(uri);
    }

    void deleteArtist(Artist artist) {
        Uri uri = LibraryUris.artistTracks(authority, libraryId, artist.identity);
        deleteTracks(uri);
    }

    void deleteFolder(Folder folder) {
        Uri uri = LibraryUris.folders(authority, libraryId, folder.identity);
        delete(uri);
    }

    void deletePlaylist(Playlist playlist) {
        Uri uri = LibraryUris.playlist(authority, libraryId, playlist.identity);
        delete(uri);
    }

    void deleteTrack(Track track) {
        Uri uri =LibraryUris.track(authority, libraryId, track.identity);
        deleteTracks(Collections.singletonList(uri));
    }

    void delete(Uri uri) {
        Bundle extras = LibraryExtras.b()
                .putUri(uri)
                .putResultReceiver(makeResultReceiver())
                .get();
        appContext.getContentResolver().call(uri, LibraryMethods.DELETE, null, extras);
    }

    void deleteTracks(Uri uri) {
        if (isSubscribed(loaderSubscription)) {
            return;
        }
        loaderSubscription = new BundleableLoader(appContext, uri, null).createObservable()
                .flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
                    @Override
                    public Observable<Bundleable> call(List<Bundleable> bundleables) {
                        return Observable.from(bundleables);
                    }
                })
                .collect(new ArrayList<Uri>(), new Action2<ArrayList<Uri>, Bundleable>() {
                    @Override
                    public void call(ArrayList<Uri> uris, Bundleable track) {
                        Uri uri = LibraryUris.track(authority, libraryId, track.getIdentity());
                        uris.add(uri);
                    }
                })
                .subscribe(new Action1<ArrayList<Uri>>() {
                    @Override
                    public void call(ArrayList<Uri> uris) {
                        deleteTracks(uris);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        notifyFailure(new LibraryException(throwable));
                    }
                });
    }

    void deleteTracks(List<Uri> trackUris) {
        final Uri uri = LibraryUris.tracks(authority, libraryId);
        Bundle extras = LibraryExtras.b()
                .putUri(uri)
                .putResultReceiver(makeResultReceiver())
                .putUriList(trackUris)
                .get();
        Bundle ok = appContext.getContentResolver().call(uri, LibraryMethods.DELETE, null, extras);
        if (ok == null || !LibraryExtras.getOk(ok)) {
            notifyFailure(LibraryExtras.getCause(ok));
        }
    }

    ResultReceiver makeResultReceiver() {
        return new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == DeleteSubscriber.RESULT) {
                    if (LibraryExtras.getOk(resultData)) {
                        notifySuccess();
                    } else {
                        notifyFailure(null);
                    }
                } else if (resultCode == DeleteSubscriber.ERROR) {
                    notifyFailure(LibraryExtras.getCause(resultData));
                }
            }
        };
    }
}
