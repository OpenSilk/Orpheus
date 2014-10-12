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

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.andrew.apollo.R;

import org.opensilk.music.api.model.spi.Bundleable;

import java.util.Collection;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.Mortar;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryView extends ListView {

    @Inject
    LibraryScreen.Presenter presenter;

    public LibraryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void setup() {
        //Causes fucked up problems when fast scrolling and the loader simlutaneously adds new items
//        addFooterView(LayoutInflater.from(getContext()).inflate(R.layout.list_footer, null));
        setAdapter(new LibraryAdapter(getContext(), presenter));
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                presenter.go(getAdapter().getItem(position));
            }
        });
    }

    @Override
    public LibraryAdapter getAdapter() {
        if (super.getAdapter() instanceof HeaderViewListAdapter) {
            return (LibraryAdapter) ((HeaderViewListAdapter)super.getAdapter()).getWrappedAdapter();
        }
        return (LibraryAdapter) super.getAdapter();
    }

}
