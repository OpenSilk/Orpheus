/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.nowplaying;

import android.content.Context;
import android.os.Parcel;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;

import java.util.Iterator;

import flow.Backstack;
import flow.Flow;
import flow.Layout;

/**
 * Created by drew on 11/17/14.
 */
@Layout(R.layout.now_playing)
@WithModule(NowPlayingScreenModule.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class NowPlayingScreen extends Screen {

    public static void toggleNowPlaying(Context context) {
        if (context == null) return;
        Flow flow = AppFlow.get(context);
        if (flow == null) return;
        if (flow.getBackstack().current().getScreen() instanceof NowPlayingScreen) {
            flow.goBack();
        } else {
            Iterator<Backstack.Entry> ii = flow.getBackstack().reverseIterator();
            while (ii.hasNext()) {
                if (ii.next().getScreen() instanceof NowPlayingScreen) {
                    flow.resetTo(new NowPlayingScreen());
                    return;
                }
            }
            flow.goTo(new NowPlayingScreen());
        }
    }

    public static final Creator<NowPlayingScreen> CREATOR = new Creator<NowPlayingScreen>() {
        @Override
        public NowPlayingScreen createFromParcel(Parcel source) {
            NowPlayingScreen s = new NowPlayingScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public NowPlayingScreen[] newArray(int size) {
            return new NowPlayingScreen[size];
        }
    };

}
