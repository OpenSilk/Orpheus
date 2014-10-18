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
import android.view.View;
import android.widget.TextView;

import org.opensilk.music.R;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.FileItemUtil;
import org.opensilk.music.widgets.ColorCodedThumbnail;

import butterknife.InjectView;

/**
 * Created by drew on 7/14/14.
 */
public class FolderPickerCard extends AbsBasicCard<FileItem> {

    @InjectView(R.id.folder_thumb)
    protected ColorCodedThumbnail mThumbnail;
    @InjectView(R.id.card_info2)
    protected TextView mInfo2;

    public FolderPickerCard(Context context, FileItem data) {
        super(context, data, R.layout.listcard_folderpicker_inner);
    }

    @Override
    protected void init() {
        setOnClickListener((OnCardClickListener) getContext());
        if (mData.getMediaType() != FileItem.MediaType.UP_DIRECTORY) {
            setOnLongClickListener((OnLongCardClickListener) getContext());
        }
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.getTitle());
        if (mData.getMediaType() != FileItem.MediaType.UP_DIRECTORY) {
            mCardSubTitle.setVisibility(View.VISIBLE);
            mCardSubTitle.setText(FileItemUtil.prettyPrintSize(getContext(), mData.getSize(), mData.getMediaType()));
            mInfo2.setVisibility(View.VISIBLE);
            mInfo2.setText(FileItemUtil.prettyPrintDate(getContext(), mData.getDate()));
        } else {
            mCardSubTitle.setVisibility(View.GONE);
            mInfo2.setVisibility(View.GONE);
        }
        mThumbnail.init(mData.getTitle());
    }

    @Override
    protected int getListLayout() {
        return R.layout.listcard_folderpicker_inner;
    }

    @Override
    protected int getGridLayout() {
        throw new UnsupportedOperationException("Can't do grids");
    }

}
