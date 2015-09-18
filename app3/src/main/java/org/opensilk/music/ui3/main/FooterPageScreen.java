/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.main;

import android.content.res.Resources;
import android.support.v4.media.session.MediaSessionCompat;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.R;

import mortar.MortarScope;

/**
 * Created by drew on 9/17/15.
 */
@Layout(R.layout.screen_footer_page)
@WithComponentFactory(FooterPageScreen.Factory.class)
public class FooterPageScreen extends Screen {

    final MediaSessionCompat.QueueItem queueItem;

    public FooterPageScreen(MediaSessionCompat.QueueItem queueItem) {
        this.queueItem = queueItem;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + queueItem.getQueueId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FooterPageScreen that = (FooterPageScreen) o;
        return queueItem.getQueueId() == that.queueItem.getQueueId();
    }

    @Override
    public int hashCode() {
        long qId = queueItem.getQueueId();
        return 31 * (int) (qId ^ (qId >>> 32));
    }

    public static class Factory extends ComponentFactory<FooterPageScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, FooterPageScreen screen) {
            FooterScreenComponent acc = DaggerService.getDaggerComponent(parentScope);
            return FooterPageScreenComponent.FACTORY.call(acc, screen);
        }
    }
}
