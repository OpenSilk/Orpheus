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

package org.opensilk.music.ui.library.card;

import android.content.Context;
import android.view.View;

import com.squareup.otto.Bus;

import org.opensilk.music.api.model.Folder;
import org.opensilk.music.ui.library.event.FolderCardClick;
import org.opensilk.silkdagger.IDaggerActivity;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class FolderListCard extends AbsListCard<Folder> {

    @Inject @ForActivity
    Bus mActivityBus;

    public FolderListCard(Context context, Folder data) {
        super(context, data);
        ((IDaggerActivity) context).inject(this);
    }

    public FolderListCard(Context context, Folder data, int innerLayout) {
        super(context, data, innerLayout);
        ((IDaggerActivity) context).inject(this);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mActivityBus.post(new FolderCardClick(mData.identity));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setVisibility(View.GONE);
    }
}
