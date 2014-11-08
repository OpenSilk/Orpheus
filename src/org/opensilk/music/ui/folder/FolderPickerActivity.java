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

package org.opensilk.music.ui.folder;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.music.ui.activities.ActivityModule;
import org.opensilk.music.ui.activities.BaseDialogActivity;
import org.opensilk.music.ui.cards.FolderPickerCard;
import org.opensilk.common.dagger.qualifier.ForActivity;

import javax.inject.Inject;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/13/14.
 */
public class FolderPickerActivity extends BaseDialogActivity implements Card.OnCardClickListener, Card.OnLongCardClickListener {

    public static final String EXTRA_DIR = "start_dir";
    public static final String SDCARD_ROOT;
    static {
        SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED, getIntent());

        // non traditional up nav, we just want to go back
//        mActionBarHelper.enableHomeAsUp(R.drawable.blank, R.drawable.ic_action_arrow_left_white);
        final String action = getIntent().getStringExtra(EXTRA_DIR);
        mActionBarHelper.setTitle(makeTitle(action));
        mActionBarHelper.setSubTitle(makeSubtitle(action));

        if (savedInstanceState == null) {
            FolderPickerFragment f = FolderPickerFragment.newInstance(action);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, f, "folders")
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
        };
    }

    @Override
    public void onClick(Card card, View view) {
        boolean addToBackstack = true;
        final FolderPickerCard c = (FolderPickerCard) card;
        final String path = c.getData().getPath();
        final String title = c.getData().getTitle();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(makeTitle(path));
        actionBar.setSubtitle(makeSubtitle(path));
        if (c.getData().getMediaType() == FileItem.MediaType.UP_DIRECTORY) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                return;
            } else {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                addToBackstack = false;
            }
        }
        FolderPickerFragment f = FolderPickerFragment.newInstance(path);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main, f, title);
        if (addToBackstack) {
            ft.addToBackStack(title);
        }
        ft.commit();
    }

    @Override
    public boolean onLongClick(Card card, View view) {
        Intent i = new Intent().putExtra(EXTRA_DIR, ((FolderPickerCard) card).getData().getPath());
        setResult(RESULT_OK, i);
        finish();
        return true;
    }

    protected static String makeSubtitle(String path) {
        if (TextUtils.isEmpty(path)) {
            path = SDCARD_ROOT;
        }
        if (path.startsWith(SDCARD_ROOT)) {
            path = path.replace(SDCARD_ROOT, "SDCARD");
        }
        return path;
    }

    protected static String makeTitle(String path) {
        if (TextUtils.isEmpty(path)) {
            path = SDCARD_ROOT;
        }
        if (TextUtils.equals(path, SDCARD_ROOT)) {
            return "SDCARD";
        } else if (path.contains("/") && !path.endsWith("/")) {
            return path.substring(path.lastIndexOf("/")+1);
        } else {
            return path;
        }
    }

}
