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

package org.opensilk.music.ui3.playlist;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarDialogFragment;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.ui3.PlaylistManageActivity;
import org.opensilk.music.ui3.common.ActivityRequestCodes;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 12/17/15.
 */
public class PlaylistProviderSelectScreenFragment extends MortarDialogFragment {
    static final String NAME = PlaylistProviderSelectScreenFragment.class.getName();

    public static PlaylistProviderSelectScreenFragment ni(Context context, List<Uri> tracks) {
        return factory(context, NAME, BundleHelper.b().putList(tracks).get());
    }

    @Override
    protected Screen newScreen() {
        List<Uri> tracks = BundleHelper.getList(getArguments());
        return new PlaylistProviderSelectScreen(tracks);
    }

    @Inject @Named("IndexProviderAuthority") String mIndexAuthority;
    @Inject @Named("foldersLibraryAuthority") String mFoldersAuthority;
    @Inject ActivityResultsController mActivityResultsController;

    boolean mAllLocal = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaylistProviderSelectScreenComponent cmp = DaggerService.getDaggerComponent(getScope());
        cmp.inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.select_playlist_provider);
        b.setItems(getItems(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mAllLocal) {
                    if (which == 0) {
                        openIndexProviderChooser();
                    } else {
                        openFoldersProviderChooser();
                    }
                } else {
                    openIndexProviderChooser();
                }
            }
        });
        b.setNegativeButton(android.R.string.cancel, null);
        return b.create();
    }

    String[] getItems() {
        List<Uri> tracks = ((PlaylistProviderSelectScreen) getScreen()).tracks;
        for (Uri uri : tracks) {
            if (!StringUtils.equals(uri.getAuthority(), mFoldersAuthority)) {
                mAllLocal = false;
                break;
            }
        }
        if (mAllLocal) {
            return new String[] {
                    getActivity().getString(R.string.orpheus_playlists),
                    getActivity().getString(R.string.folders_playlists),
            };
        } else {
            return new String[] {
                    getActivity().getString(R.string.orpheus_playlists),
            };
        }
    }

    void openIndexProviderChooser() {
        List<Uri> tracks = ((PlaylistProviderSelectScreen) getScreen()).tracks;
        mActivityResultsController.startActivityForResult(
                PlaylistManageActivity.makeAddIntent2(getActivity(), IndexUris.playlists(mIndexAuthority), tracks),
                ActivityRequestCodes.PLAYLIST_ADD, null);
    }

    void openFoldersProviderChooser() {
        List<Uri> tracks = ((PlaylistProviderSelectScreen) getScreen()).tracks;
        mActivityResultsController.startActivityForResult(
                PlaylistManageActivity.makeAddIntent2(getActivity(), FoldersUris.playlists(mFoldersAuthority), tracks),
                ActivityRequestCodes.PLAYLIST_ADD, null);
    }

}
