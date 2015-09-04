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

package org.opensilk.music.ui3.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.ActivityResultsListener;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.albums.AlbumsScreenFragment;
import org.opensilk.music.ui3.artists.ArtistsScreenFragment;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.folders.FoldersScreenFragment;
import org.opensilk.music.ui3.gallery.GalleryPage;
import org.opensilk.music.ui3.gallery.GalleryScreenFragment;
import org.opensilk.music.ui3.genres.GenresScreenFragment;
import org.opensilk.music.ui3.library.LandingScreenViewAdapter.ViewItem;
import org.opensilk.music.ui3.playlists.PlaylistsScreenFragment;
import org.opensilk.music.ui3.tracks.TracksScreenFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 5/1/15.
 */
@ScreenScope
public class LandingScreenPresenter extends ViewPresenter<LandingScreenView> implements ActivityResultsListener {

    final AppPreferences settings;
    final LandingScreen screen;
    final ActivityResultsController activityResultsController;
    final FragmentManagerOwner fm;
    final Context appContext;
    final ToolbarOwner toolbarOwner;

    LibraryInfo currentSelection;
    int lastval;

    @Inject
    public LandingScreenPresenter(
            AppPreferences settings,
            LandingScreen screen,
            ActivityResultsController activityResultsController,
            FragmentManagerOwner fm,
            @ForApplication Context appContext,
            ToolbarOwner toolbarOwner
    ) {
        this.settings = settings;
        this.screen = screen;
        this.activityResultsController = activityResultsController;
        this.fm = fm;
        this.appContext = appContext;
        this.toolbarOwner = toolbarOwner;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        activityResultsController.register(scope, this);
    }

    @Override
    @DebugLog
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(getClass().getClassLoader());
            currentSelection = savedInstanceState.getParcelable("selection");
            lastval = savedInstanceState.getInt("lastval");
        }
        if (currentSelection == null) {
            currentSelection = settings.getLibraryInfo(screen.libraryConfig, AppPreferences.DEFAULT_LIBRARY);
        }
        if (currentSelection == null) {
            lastval = 0;
            Intent i = new Intent().setComponent(screen.libraryConfig.pickerComponent);
            activityResultsController.startActivityForResult(i, ActivityRequestCodes.LIBRARY_PICKER, null);
        } else {
            createCategories();
        }
        updateActionBar();
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        outState.putParcelable("selection", currentSelection);
        outState.putInt("lastval", lastval);
    }

    void createCategories() {
        if (screen.libraryConfig.hasAbility(LibraryCapability.GALLERY)) {
            openGallery(false);
            return;
        }

        List<ViewItem> items = new ArrayList<>();
        if (screen.libraryConfig.hasAbility(LibraryCapability.FOLDERSTRACKS)) {
            items.add(ViewItem.FOLDERS);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.ALBUMS)) {
            items.add(ViewItem.ALBUMS);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.ARTISTS)) {
            items.add(ViewItem.ARTISTS);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.GENRES)) {
            items.add(ViewItem.GENRES);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.PLAYLISTS)) {
            items.add(ViewItem.PLAYLISTS);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.TRACKS)) {
            items.add(ViewItem.TRACKS);
        }

        if (items.size() == 1) {
            openScreen(items.get(0), false);
//        } else if (lastval > 0) {
//            for (ViewItem item : items) {
//                if (item.val == lastval) {
//                    openScreen(item, true);
//                    break;
//                }
//            }
        } else if (hasView()) {
            getView().getAdapter().replaceAll(items);
            getView().setListShown(true, true);
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ActivityRequestCodes.LIBRARY_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    currentSelection = data.getParcelableExtra(LibraryConstants.EXTRA_LIBRARY_INFO);
                    if (currentSelection == null) {
                        Timber.e("Library chooser should set EXTRA_LIBRARY_INFO");
                        String id = data.getStringExtra(LibraryConstants.EXTRA_LIBRARY_ID);
                        if (TextUtils.isEmpty(id)) {
                            Timber.e("Library chooser must set EXTRA_LIBRARY_ID");
                            //TODO show error
                            return true;
                        }
                        currentSelection = new LibraryInfo(id, null, null, null);
                    }
                    if (currentSelection.folderId != null || currentSelection.folderName != null) {
                        Timber.w("Please stop setting folderId and folderName in the returned LibraryInfo");
                        currentSelection = currentSelection.buildUpon(null, null);
                    }
                    settings.setLibraryInfo(screen.libraryConfig, AppPreferences.DEFAULT_LIBRARY, currentSelection);
                    updateActionBar();
                    createCategories();
                } else {
                    Timber.e("Activity returned bad result");
                    //TODO show error
                }
                return true;
            default:
                return false;
        }
    }

    void onItemClicked(Context context, ViewItem item) {
        openScreen(item, true);
    }

    void openScreen(ViewItem item, boolean bs) {
        lastval = item.val;
        if (item.val == ViewItem.FOLDERS.val) {
            openFolders(bs);
        } else if (item.val == ViewItem.ALBUMS.val) {
            openAlbums(bs);
        } else if (item.val == ViewItem.ARTISTS.val) {
            openArtists(bs);
        } else if (item.val == ViewItem.GENRES.val) {
            openGenres(bs);
        } else if (item.val == ViewItem.PLAYLISTS.val) {
            openPlaylists(bs);
        } else if (item.val == ViewItem.TRACKS.val) {
            openTracks(bs);
        }
    }

    void openFolders(boolean bs) {
        FoldersScreenFragment f = FoldersScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openAlbums(boolean bs) {
        AlbumsScreenFragment f = AlbumsScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openArtists(boolean bs) {
        ArtistsScreenFragment f = ArtistsScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openGenres(boolean bs) {
        GenresScreenFragment f = GenresScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openPlaylists(boolean bs) {
        PlaylistsScreenFragment f = PlaylistsScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openTracks(boolean bs) {
        TracksScreenFragment f = TracksScreenFragment.ni(appContext, screen.libraryConfig, currentSelection);
        fm.replaceMainContent(f, bs);
    }

    void openGallery(boolean bs) {
        List<GalleryPage> pages = new ArrayList<>();
        if (screen.libraryConfig.hasAbility(LibraryCapability.PLAYLISTS)) {
            pages.add(GalleryPage.PLAYLIST);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.ARTISTS)) {
            pages.add(GalleryPage.ARTIST);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.ALBUMS)) {
            pages.add(GalleryPage.ALBUM);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.GENRES)) {
            pages.add(GalleryPage.GENRE);
        }
        if (screen.libraryConfig.hasAbility(LibraryCapability.TRACKS)) {
            pages.add(GalleryPage.SONG);
        }
        GalleryScreenFragment f = GalleryScreenFragment.ni(appContext, screen.libraryConfig, currentSelection, pages);
        fm.replaceMainContent(f, bs);
    }

    boolean isGalleryEligible() {
//        boolean no = screen.libraryConfig.hasAbility(LibraryCapability.FOLDERSTRACKS);
//        if (no) {
//            return false;
//        }
        boolean yes = screen.libraryConfig.hasAbility(LibraryCapability.ALBUMS);
        yes |= screen.libraryConfig.hasAbility(LibraryCapability.ARTISTS);
        yes |= screen.libraryConfig.hasAbility(LibraryCapability.GENRES);
        yes |= screen.libraryConfig.hasAbility(LibraryCapability.PLAYLISTS);
        yes |= screen.libraryConfig.hasAbility(LibraryCapability.TRACKS);
        return yes;
    }

    void updateActionBar() {
        ActionBarConfig.Builder builder = ActionBarConfig.builder()
                .setTitle(screen.libraryConfig.label);
        if (currentSelection != null) {
            ActionBarMenuConfig.Builder menuBuilder = ActionBarMenuConfig.builder();
            ActionBarMenuConfigWrapper wrapper = new ActionBarMenuConfigWrapper(
                    screen.libraryConfig, currentSelection, fm, settings, activityResultsController);
            wrapper.applyCommonItems(menuBuilder);
            menuBuilder.setActionHandler(wrapper.getDelegateHandler(null));
            builder.setMenuConfig(menuBuilder.build());
        }
        toolbarOwner.setConfig(builder.build());
    }

}
