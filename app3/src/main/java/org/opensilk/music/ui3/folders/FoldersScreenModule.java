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

package org.opensilk.music.ui3.folders;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ActionBarOwner;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.FolderSortOrder;
import org.opensilk.music.library.sort.FolderTrackSortOrder;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func0;
import rx.functions.Func2;

/**
 * Created by drew on 5/2/15.
 */
@Module
public class FoldersScreenModule {
    final FoldersScreen screen;

    public FoldersScreenModule(FoldersScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return LibraryUris.folders(screen.libraryConfig.authority,
                screen.libraryInfo.libraryId, screen.libraryInfo.folderId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.FOLDER_SORT_ORDER), FolderTrackSortOrder.A_Z);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig
    ) {

        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setOverflowClickListener(overflowClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                if (item instanceof Folder) {
                    LibraryInfo info = screen.libraryInfo.buildUpon(item.getIdentity(), item.getName());
                    FoldersScreenFragment f = FoldersScreenFragment.ni(context, screen.libraryConfig, info);
                    presenter.getFm().replaceMainContent(f, true);
                } else if (item instanceof Track) {
                    BundleableComponent component = DaggerService.getDaggerComponent(context);
                    PlaybackController playbackController = component.playbackController();
                    List<Bundleable> adaperItems = presenter.getItems();
                    List<Uri> toPlay = new ArrayList<>(adaperItems.size());
                    for (Bundleable b : adaperItems) {
                        if (b instanceof Track) {
                            toPlay.add(LibraryUris.track(screen.libraryConfig.authority, screen.libraryInfo.libraryId, b.getIdentity()));
                        }
                    }
                    //lazy way to find its new pos in case there were folders before it in the adapteritems
                    int pos = toPlay.indexOf(LibraryUris.track(screen.libraryConfig.authority, screen.libraryInfo.libraryId, item.getIdentity()));
                    playbackController.playAll(toPlay, pos);
                }

            }
        };
    }

    // Albums, Artists, Folders
    static final int[] MENUS_COLLECTION = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
    };
    static final int[] MENUS_SONG = new int[] {
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
    };

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener() {
        return new OverflowClickListener() {

            @Override
            public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
                if (item instanceof Track) {
                    for (int ii : MENUS_SONG) {
                        m.inflate(ii);
                    }
                } else {
                    for (int ii : MENUS_COLLECTION) {
                        m.inflate(ii);
                    }
                }
            }

            @Override
            public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
                BundleableComponent component = DaggerService.getDaggerComponent(context);
                PlaybackController playbackController = component.playbackController();
                AppPreferences appPreferences = component.appPreferences();
                final String auth = screen.libraryConfig.authority;
                final String lib = screen.libraryInfo.libraryId;
                switch (action) {
                    case PLAY_ALL: {
                        if (item instanceof Folder) {
                            playbackController.playTracksFrom(
                                    LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                                    0,
                                    provideLoaderSortOrder(appPreferences)
                            );
                        } else {
                            return false;
                        }
                        return true;
                    }
                    case SHUFFLE_ALL: {
                        if (item instanceof Folder) {
                            playbackController.shuffleTracksFrom(
                                    LibraryUris.folderTracks(auth, lib, item.getIdentity())
                            );
                        } else {
                            return false;
                        }
                        return true;
                    }
                    case PLAY_NEXT: {
                        if (item instanceof Track) {
                            playbackController.enqueueAllNext(Collections.singletonList(
                                    LibraryUris.track(auth, lib, item.getIdentity())
                            ));
                        } else if (item instanceof Folder) {
                            playbackController.enqueueTracksNextFrom(
                                    LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                                    provideLoaderSortOrder(appPreferences)
                            );
                        } else {
                            return false;
                        }
                        return true;
                    }
                    case ADD_TO_QUEUE: {
                        if (item instanceof Track) {
                            playbackController.addAllToQueue(Collections.singletonList(
                                    LibraryUris.track(auth, lib, item.getIdentity())
                            ));
                        } else if (item instanceof Folder) {
                            playbackController.addTracksToQueueFrom(
                                    LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                                    provideLoaderSortOrder(appPreferences)
                            );
                        } else {
                            return false;
                        }
                        return true;
                    }
                    default:
                        return false;
                }
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig() {

        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        // Common items
        if (screen.libraryInfo.folderId != null) {
            builder.withMenus(ActionBarMenuConfig.toObject(MENUS_COLLECTION));
        }

        //TODO sortby

        // device selection
        String selectName = screen.libraryConfig.getMeta(LibraryConfig.META_MENU_NAME_PICKER);
        if (!TextUtils.isEmpty(selectName)) {
            builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
                    0, R.id.menu_change_source, 99, selectName, -1));
        } else {
            builder.withMenu(R.menu.library_change_source);
        }

        // library settings
        if (screen.libraryConfig.hasAbility(LibraryCapability.SETTINGS)) {
            String settingsName = screen.libraryConfig.getMeta(LibraryConfig.META_MENU_NAME_SETTINGS);
            if (!TextUtils.isEmpty(settingsName)) {
                builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
                        0, R.id.menu_library_settings, 100, settingsName, -1));
            } else {
                builder.withMenu(R.menu.library_settings);
            }
        }

        Func2<Context, Integer, Boolean> handler = new Func2<Context, Integer, Boolean>() {
            @Override
            public Boolean call(Context context, Integer integer) {
                MusicActivityComponent component = DaggerService.getDaggerComponent(context);
                AppPreferences appPreferences = component.appPreferences();
                ActivityResultsController activityResultsController = component.activityResultsController();
                PlaybackController playbackController = component.playbackController();
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component1 = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component1.presenter();
                final String auth = screen.libraryConfig.authority;
                final String lib = screen.libraryInfo.libraryId;
                final String id = screen.libraryInfo.folderId;
                switch (integer) {
                    case R.id.menu_change_source:
                        appPreferences.removeDefaultLibraryInfo(screen.libraryConfig);
                        //TODO
                        return true;
                    case R.id.menu_library_settings:
                        Intent intent = new Intent()
                                .setComponent(screen.libraryConfig.<ComponentName>getMeta(LibraryConfig.META_SETTINGS_COMPONENT))
                                .putExtra(LibraryConstants.EXTRA_LIBRARY_ID, screen.libraryInfo.libraryId)
                                .putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, screen.libraryInfo);
                        activityResultsController.startActivityForResult(intent, 2012, null); //TODO
                        return true;
                    case R.id.popup_play_all:
                        playbackController.playTracksFrom(
                                LibraryUris.folderTracks(auth, lib, id),
                                0,
                                provideLoaderSortOrder(appPreferences)
                        );
                        return true;
                    case R.id.popup_shuffle_all:
                        playbackController.shuffleTracksFrom(
                                LibraryUris.folderTracks(auth, lib, id)
                        );
                        return true;
                    case R.id.popup_play_next:
                        playbackController.enqueueTracksNextFrom(
                                LibraryUris.folderTracks(auth, lib, id),
                                provideLoaderSortOrder(appPreferences)
                        );
                        return true;
                    case R.id.popup_add_to_queue:
                        playbackController.addTracksToQueueFrom(
                                LibraryUris.folderTracks(auth, lib, id),
                                provideLoaderSortOrder(appPreferences)
                        );
                        return true;
                    default:
                        return false;
                }
            }
        };

        return builder.setActionHandler(handler).build();
    }
}
