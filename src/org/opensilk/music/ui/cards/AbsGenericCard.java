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
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/30/14.
 */
public abstract class AbsGenericCard<D> extends AbsBasicCard<D> {

    public AbsGenericCard(Context context, D data, int innerLayout) {
        super(context, data, innerLayout);
    }

    protected abstract void onCreatePopupMenu(PopupMenu m);

    @OnClick(R.id.card_overflow_button)
    public void onOverflowClicked(View v) {
        PopupMenu m = new PopupMenu(getContext(), v);
        onCreatePopupMenu(m);
        m.show();
    }

}
