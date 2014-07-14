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
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 4/15/14.
 */
public class CardShuffle extends Card {

    @InjectView(R.id.shuffle_thumbnail)
    ImageView mShuffleImage;
    @InjectView(R.id.card_title)
    TextView mCardTitle;

    public CardShuffle(Context context) {
        super(context, R.layout.listcard_shuffle_inner);
        init();
    }

    private void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                ApolloUtils.execute(false, new CommandRunner(getContext(),
                        new Command() {
                            @Override
                            public CharSequence execute() {
                                MusicUtils.shuffleAll(getContext());
                                return null;
                            }
                        }));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        ButterKnife.inject(this, view);
        mCardTitle.setText(getContext().getString(R.string.menu_shuffle));
        final int accentColor = ThemeHelper.getAccentColor(getContext());
        mShuffleImage.setImageDrawable(ThemeHelper.themeDrawable(getContext(),
                R.drawable.ic_action_playback_shuffle_white, accentColor));
    }

}
