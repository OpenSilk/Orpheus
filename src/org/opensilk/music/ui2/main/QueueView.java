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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import com.mobeta.android.dslv.DragSortListView;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 10/15/14.
 */
public class QueueView extends DragSortListView {

    @Inject
    QueueScreen.Presenter presenter;

    public QueueView(Context context, AttributeSet attrs) {
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
        setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, generateList()));
    }

    String[] generateList() {
        String[] lst = new String[50];
        for (int ii=0; ii<50; ii++) {
            lst[ii] = "Song " + ii;
        }
        return lst;
    }

}
