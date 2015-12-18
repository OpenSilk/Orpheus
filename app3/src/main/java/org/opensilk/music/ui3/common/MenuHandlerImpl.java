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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.client.LibraryClient;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Item;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.PlaylistManageActivity;
import org.opensilk.music.ui3.library.FoldersScreenFragment;
import org.opensilk.music.ui3.playlist.PlaylistProviderSelectScreenFragment;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

/**
 * Created by drew on 9/24/15.
 */
public abstract class MenuHandlerImpl extends MenuHandler {

    final Uri loaderUri;
    protected final ActivityResultsController activityResultsController;

    public MenuHandlerImpl(Uri loaderUri, ActivityResultsController activityResultsController) {
        this.loaderUri = loaderUri;
        this.activityResultsController = activityResultsController;
    }

    public void inflateMenu(int item, MenuInflater inflater, Menu menu) {
        inflater.inflate(item, menu);
    }

    public void inflateMenus(MenuInflater inflater, Menu menu, int... items) {
        for (int item : items) {
            inflater.inflate(item, menu);
        }
    }

    public void setNewSortOrder(BundleablePresenter presenter, String sortorder) {
        AppPreferences preferences = presenter.getSettings();
        preferences.putString(preferences.sortOrderKey(loaderUri), sortorder);
        presenter.getLoader().setSortOrder(sortorder);
        presenter.reload();
    }

    public void updateLayout(BundleablePresenter presenter, String kind) {
        AppPreferences preferences = presenter.getSettings();
        preferences.putString(preferences.layoutKey(loaderUri), kind);
        presenter.setWantsGrid(StringUtils.equals(kind, AppPreferences.GRID));
        presenter.resetRecyclerView();
    }

    public void addItemsToQueue(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getItems());
        if (toPlay.isEmpty()) {
            Timber.e("No tracks in list");
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllEnd(toPlay);
    }

    public void addSelectedItemsToQueue(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getSelectedItems());
        if (toPlay.isEmpty()) {
            Timber.e("No tracks in list");
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllEnd(toPlay);
    }

    public void playItemsNext(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getItems());
        if (toPlay.isEmpty()) {
            Timber.e("No tracks in list");
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllNext(toPlay);
    }

    public void playSelectedItemsNext(BundleablePresenter presenter) {
        List<Uri> toPlay = UtilsCommon.filterTracks(presenter.getSelectedItems());
        if (toPlay.isEmpty()) {
            Timber.e("No tracks in list");
            return; //TODO toast?
        }
        presenter.getPlaybackController().enqueueAllNext(toPlay);
    }

    //TODO somehow check authorites and allow adding to android playlists
    public void addToPlaylistFromTracksUris(Context context, List<Uri> tracksUriList) {
        if (tracksUriList == null || tracksUriList.isEmpty()) {
            return;
        }
        String firstauthority = tracksUriList.get(0).getAuthority();
        activityResultsController.startActivityForResult(
                PlaylistManageActivity.makeAddIntent(context, IndexUris.playlists(firstauthority), tracksUriList),
                ActivityRequestCodes.PLAYLIST_ADD, null);
    }

    public void addToPlaylistFromTracks(Context context, BundleablePresenter presenter) {
        List<Uri> tracks = UtilsCommon.filterTracks(presenter.getSelectedItems());
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        presenter.getFm().showDialog(PlaylistProviderSelectScreenFragment.ni(context, tracks));
    }

    public void addToQueueFromTracksUris(Context context, BundleablePresenter presenter, List<Uri> uris) {
        final PlaybackController playbackController = presenter.getPlaybackController();
        UtilsCommon.addTracksToQueue(context, uris, new Action1<List<Uri>>() {
            @Override
            public void call(List<Uri> uris) {
                playbackController.enqueueAllEnd(uris);
            }
        });
    }

    public void playFromTracksUris(Context context, BundleablePresenter presenter, List<Uri> uris) {
        final PlaybackController playbackController = presenter.getPlaybackController();
        UtilsCommon.addTracksToQueue(context, uris, new Action1<List<Uri>>() {
            @Override
            public void call(List<Uri> uris) {
                playbackController.playAll(uris, 0);
            }
        });
    }

    public void playNextFromTracksUris(Context context, BundleablePresenter presenter, List<Uri> uris) {
        final PlaybackController playbackController = presenter.getPlaybackController();
        UtilsCommon.addTracksToQueue(context, uris, new Action1<List<Uri>>() {
            @Override
            public void call(List<Uri> uris) {
                playbackController.enqueueAllNext(uris);
            }
        });
    }

    public void openFolder(BundleablePresenter presenter, Context context, Container container) {
        LibraryClient client = LibraryClient.create(context, container.getUri());
        Bundle config = client.makeCall(LibraryMethods.CONFIG, null);
        client.release();
        if (config != null) {
            presenter.getFm().replaceMainContent(FoldersScreenFragment.ni(context,
                    LibraryConfig.materialize(config), container), true);
        }
    }

    public void openParentFolder(final BundleablePresenter presenter, Context context, final Item item) {
        final Context appcontext = context.getApplicationContext();
        Observable<Container> containerObservable = TypedBundleableLoader.<Container>create(appcontext)
                .setUri(item.getParentUri())
                .setMethod(LibraryMethods.GET)
                .createObservable()
                .flatMap(new Func1<List<Container>, Observable<Container>>() {
                    @Override
                    public Observable<Container> call(List<Container> containers) {
                        return Observable.from(containers);
                    }
                })
                .first();
        Observable<LibraryConfig> configObservable = Observable.create(new Observable.OnSubscribe<LibraryConfig>() {
            @Override
            public void call(Subscriber<? super LibraryConfig> subscriber) {
                LibraryClient client = LibraryClient.create(appcontext, item.getUri());
                Bundle config = client.makeCall(LibraryMethods.CONFIG, null);
                client.release();
                if (config != null) {
                    subscriber.onNext(LibraryConfig.materialize(config));
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new NullPointerException("Null config"));
                }
            }
        });
        Subscription s = Observable.zip(containerObservable, configObservable, new Func2<Container, LibraryConfig, FoldersScreenFragment>() {
            @Override
            public FoldersScreenFragment call(Container container, LibraryConfig libraryConfig) {
                return FoldersScreenFragment.ni(appcontext, libraryConfig, container);
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<FoldersScreenFragment>() {
            @Override
            public void call(FoldersScreenFragment foldersScreenFragment) {
                presenter.getFm().replaceMainContent(foldersScreenFragment, true);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable, "openParent %s", item);
                //TODO notify
            }
        });
    }

}
