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

package org.opensilk.music.ui.cards;

import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.squareup.otto.Bus;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.FileItemUtil;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.ui.cards.event.CardEvent;
import org.opensilk.music.ui.cards.event.FolderCardClick;
import org.opensilk.music.widgets.ColorCodedThumbnail;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class FolderCard extends AbsBundleableCard<Folder> {

    @Inject @ForFragment
    Bus mBus; //Injected by adapter

    @InjectView(R.id.folder_thumb)
    ColorCodedThumbnail mThumbnail;
    @InjectView(R.id.card_info2)
    TextView mInfo2;

    public FolderCard(Context context, Folder data) {
        this(context, data, R.layout.listcard_folder_inner);
    }

    public FolderCard(Context context, Folder data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new FolderCardClick(CardEvent.OPEN, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        if (mData.childCount > 0) {
            mCardSubTitle.setVisibility(View.VISIBLE);
            mCardSubTitle.setText(FileItemUtil.prettyPrintSize(getContext(), mData.childCount, FileItem.MediaType.DIRECTORY));
        } else {
            mCardSubTitle.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mData.date)) {
            mInfo2.setVisibility(View.VISIBLE);
            mInfo2.setText(mData.date);
            if (mCardSubTitle.getVisibility() == View.GONE) {
                mCardSubTitle.setVisibility(View.VISIBLE);
                mCardSubTitle.setText(" ");
            }
        } else {
            mInfo2.setVisibility(View.GONE);
        }
        mThumbnail.init(mData.name);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    CardEvent event = CardEvent.valueOf(item.getItemId());
                    mBus.post(new FolderCardClick(event, mData));
                    return true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        return R.layout.listcard_folder_inner;
    }

    @Override
    protected int getGridLayout() {
        throw new UnsupportedOperationException("Can't do grids");
    }
}
