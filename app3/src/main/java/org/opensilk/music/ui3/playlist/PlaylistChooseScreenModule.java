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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.DialogFactory;
import org.opensilk.common.ui.mortar.DialogPresenter;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.sort.PlaylistSortOrder;
import org.opensilk.music.ui3.ProfileActivity;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.profile.playlist.PlaylistDetailsScreen;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by drew on 10/24/15.
 */
@Module
public class PlaylistChooseScreenModule {
    final PlaylistChooseScreen screen;

    public PlaylistChooseScreenModule(PlaylistChooseScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.playlists(authority);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener(
            final DialogPresenter dialogPresenter, final ActivityResultsController activityResultsController) {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(final BundleablePresenter presenter, final Context context, final Model item) {
                final Subscription s = presenter.getIndexClient().addToPlaylist(item.getUri(), screen.tracksUris)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Integer>() {
                            @Override
                            public void onCompleted() {
                                Intent intent = new Intent().putExtra("plist", ((Playlist)item).toBundle());
                                activityResultsController.setResultAndFinish(Activity.RESULT_OK, intent);
                            }

                            @Override
                            public void onError(Throwable e) {
                                dialogPresenter.showDialog(new DialogFactory() {
                                    @Override
                                    public Dialog call(Context context) {
                                        return new AlertDialog.Builder(context)
                                                .setMessage(R.string.err_generic)
                                                .create();
                                    }
                                });
                            }

                            @Override
                            public void onNext(Integer integer) {
                                //pass
                            }
                        });
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri) {
        return new MenuHandlerImpl(loaderUri) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.playlist_sort_by
                );
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, PlaylistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, PlaylistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_date_added:
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                return false;
            }
        };
    }

    @Provides @Named("fabaction")
    Action1<ViewClickEvent> provideFabClickAction(
            final BundleablePresenter presenter, final DialogPresenter dialogPresenter) {
        return new Action1<ViewClickEvent>() {
            @Override
            @DebugLog
            public void call(ViewClickEvent viewClickEvent) {
                dialogPresenter.showDialog(new DialogFactory() {
                    @Override
                    public Dialog call(Context context) {
                        AlertDialog.Builder b = new AlertDialog.Builder(context)
                                .setTitle(R.string.create_playlist_prompt);
                        final EditText editText = new EditText(context);
                        editText.setSingleLine(true);
                        editText.setInputType(editText.getInputType()
                                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        b.setView(editText)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        presenter.getIndexClient().createPlaylist(editText.getText().toString());
                                        dialog.dismiss();
                                    }
                                });
                        return b.create();
                    }
                });
            }
        };
    }
}
