/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.library;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.andrew.apollo.meta.LibraryInfo;

import org.opensilk.music.ui.library.BackgroundFetcherFragment.Action;

import static org.opensilk.music.ui.library.BackgroundFetcherFragment.ARG_ACTION;

/**
 * Created by drew on 6/26/14.
 */
public class FetchingProgressFragment extends DialogFragment implements BackgroundFetcherFragment.CompleteListener {

    public static String FRAGMENT_TAG = "fetchingprogress";

    private LibraryInfo mLibraryInfo;
    private Action mAction;

    public static FetchingProgressFragment newInstance(LibraryInfo libraryInfo, Action action) {
        FetchingProgressFragment f = new FetchingProgressFragment();
        Bundle b = new Bundle(2);
        b.putParcelable(LibraryFragment.ARG_LIBRARY_INFO, libraryInfo);
        b.putString(ARG_ACTION, action.toString());
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLibraryInfo = getArguments().getParcelable(LibraryFragment.ARG_LIBRARY_INFO);
        mAction = Action.valueOf(getArguments().getString(ARG_ACTION));

        setStyle(STYLE_NO_TITLE, 0);

        if (savedInstanceState == null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(BackgroundFetcherFragment.newInstance(mLibraryInfo, mAction, FRAGMENT_TAG), "bgfetcher")
                    .commit();
        }

        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.fetching_song_list));
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    @Override
    public void onComplete(CharSequence toastString) {
        Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    @Override
    public void onMessageUpdated(CharSequence message) {
        if (getDialog() != null) {
            ((ProgressDialog) getDialog()).setMessage(message);
        }
    }
}
