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
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.opensilk.music.R;
import com.squareup.otto.Bus;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.FileItemUtil;
import org.opensilk.music.ui.cards.event.CardEvent;
import org.opensilk.music.ui.cards.event.FileItemCardClick;
import org.opensilk.music.widgets.ColorCodedThumbnail;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/15/14.
 */
public class FileItemCard extends AbsGenericCard<FileItem> {

    @InjectView(R.id.folder_thumb)
    protected ColorCodedThumbnail mThumbnail;
    @InjectView(R.id.card_overflow_button)
    protected View mOverflow;
    @InjectView(R.id.card_info2)
    protected TextView mInfo2;

    @Inject @ForFragment
    Bus mBus;

    public FileItemCard(Context context, FileItem data) {
        super(context, data, R.layout.listcard_folder_inner);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new FileItemCardClick(CardEvent.OPEN, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.getTitle());
        if (mData.getMediaType() != FileItem.MediaType.UP_DIRECTORY) {
            mOverflow.setVisibility(View.VISIBLE);
            mCardSubTitle.setVisibility(View.VISIBLE);
            mCardSubTitle.setText(FileItemUtil.prettyPrintSize(getContext(), mData.getSize(), mData.getMediaType()));
            mInfo2.setVisibility(View.VISIBLE);
            mInfo2.setText(FileItemUtil.prettyPrintDate(getContext(), mData.getDate()));
        } else {
            mOverflow.setVisibility(View.GONE);
            mCardSubTitle.setVisibility(View.GONE);
            mInfo2.setVisibility(View.GONE);
        }
        mThumbnail.init(mData.getTitle());
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        if (mData.getMediaType() != FileItem.MediaType.UP_DIRECTORY) {
            if (mData.getMediaType() == FileItem.MediaType.AUDIO) {
                m.inflate(R.menu.popup_play_next);
                m.inflate(R.menu.popup_add_to_queue);
                m.inflate(R.menu.popup_add_to_playlist);
                m.inflate(R.menu.popup_set_ringtone);
                m.inflate(R.menu.popup_delete);
            } else if (mData.getMediaType() == FileItem.MediaType.DIRECTORY) {
                m.inflate(R.menu.popup_play_all);
                m.inflate(R.menu.popup_shuffle_all);
                m.inflate(R.menu.popup_add_to_queue);
                m.inflate(R.menu.popup_add_to_playlist);
//                m.inflate(R.menu.popup_delete); //TODO
            }
            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        CardEvent event = CardEvent.valueOf(item.getItemId());
                        mBus.post(new FileItemCardClick(event, mData));
                        return true;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
        }
    }

    @Override
    protected int getListLayout() {
        return R.layout.listcard_folder_inner;
    }

    @Override
    protected int getGridLayout() {
        throw new UnsupportedOperationException("No grids for fileitemcard");
    }
}
