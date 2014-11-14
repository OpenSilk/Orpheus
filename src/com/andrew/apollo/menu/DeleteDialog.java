/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.andrew.apollo.Config;

import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import com.andrew.apollo.utils.MusicUtils;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Alert dialog used to delete tracks.
 * <p>
 * TODO: Remove albums from the recents list upon deletion.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class DeleteDialog extends DialogFragment {

    /**
     * The item(s) to delete
     */
    private long[] mItemList;

    /**
     * @param title The title of the artist, album, or song to delete
     * @param items The item(s) to delete
     * @return A new instance of the dialog
     */
    public static DeleteDialog newInstance(final String title, final long[] items) {
        final DeleteDialog frag = new DeleteDialog();
        final Bundle args = new Bundle();
        args.putString(Config.NAME, title);
        args.putLongArray("items", items);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final String delete = getString(R.string.context_menu_delete);
        final Bundle arguments = getArguments();
        // Get the track(s) to delete
        mItemList = arguments.getLongArray("items");
        // Get the dialog title
        final String title = arguments.getString(Config.NAME);
        final String dialogTitle = getString(R.string.delete_dialog_title, title);
        // Build the dialog
        return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setMessage(R.string.cannot_be_undone)
                .setPositiveButton(delete, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        // Delete the selected item(s)
                        Observable.create(new Observable.OnSubscribe<CharSequence>() {
                            @Override
                            public void call(Subscriber<? super CharSequence> subscriber) {
                                CharSequence msg = MusicUtils.deleteTracks(getActivity(), mItemList);
                                if (!TextUtils.isEmpty(msg)) {
                                    subscriber.onNext(msg);
                                }
                                subscriber.onCompleted();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SimpleObserver<CharSequence>() {
                            @Override
                            public void onNext(CharSequence charSequence) {
                                Toast.makeText(getActivity(), charSequence, Toast.LENGTH_SHORT).show();
                            }
                        });
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
