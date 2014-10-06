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

package org.opensilk.music.ui2.folder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.opensilk.filebrowser.FileItem;

import javax.inject.Inject;

import butterknife.ButterKnife;
import flow.Flow;
import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class FolderView extends ListView {

    @Inject
    FolderScreen.Presenter presenter;

    public FolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void setup() {
        Adapter adapter = new Adapter(getContext());
        setAdapter(adapter);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                presenter.go(getAdapter().getItem(position));
            }
        });
    }

    @Override
    public Adapter getAdapter() {
        return (Adapter) super.getAdapter();
    }

    public static class Adapter extends ArrayAdapter<FileItem> {

        public Adapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
        }
    }

}
