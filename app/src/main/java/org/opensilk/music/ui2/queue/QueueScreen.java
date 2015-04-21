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

package org.opensilk.music.ui2.queue;

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
 * Created by drew on 10/15/14.
 */
@Layout(R.layout.queue)
@WithModule(QueueScreenModule.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class QueueScreen extends Screen {

    public static void toggleQueue(Context context) {
        if (context == null) return;
        Flow flow = AppFlow.get(context);
        if (flow == null) return;
        if (flow.getBackstack().current().getScreen() instanceof QueueScreen) {
            flow.goBack();
        } else {
            Iterator<Backstack.Entry> ii = flow.getBackstack().reverseIterator();
            while (ii.hasNext()) {
                if (ii.next().getScreen() instanceof QueueScreen) {
                    flow.resetTo(new QueueScreen());
                    return;
                }
            }
            flow.goTo(new QueueScreen());
        }
    }

    public static final Creator<QueueScreen> CREATOR = new Creator<QueueScreen>() {
        @Override
        public QueueScreen createFromParcel(Parcel source) {
            QueueScreen s = new QueueScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public QueueScreen[] newArray(int size) {
            return new QueueScreen[size];
        }
    };

}
