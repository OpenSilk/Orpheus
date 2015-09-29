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

package org.opensilk.music.ui3.nowplaying;

import android.support.v4.media.session.MediaSessionCompat;

import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.model.ArtInfo;

/**
 * Created by drew on 5/15/15.
 */
public class QueueScreenItem {
    final long queueId;
    final String mediaId;
    final String title;
    final String subtitle;
    final ArtInfo artInfo;

    public QueueScreenItem(long queueId, String mediaId, CharSequence title, CharSequence subtitle, ArtInfo artInfo) {
        this.queueId = queueId;
        this.mediaId = mediaId;
        this.title = title != null ? title.toString() : null;
        this.subtitle = subtitle != null ? subtitle.toString() : null;
        this.artInfo = artInfo;
    }

    public static QueueScreenItem fromQueueItem(MediaSessionCompat.QueueItem queueItem) {
        return new QueueScreenItem(
                queueItem.getQueueId(),
                queueItem.getDescription().getMediaId(),
                queueItem.getDescription().getTitle(),
                queueItem.getDescription().getSubtitle(),
                BundleHelper.<ArtInfo>getParcelable(queueItem.getDescription().getExtras())
        );
    }

    public long getQueueId() {
        return queueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueScreenItem that = (QueueScreenItem) o;
        if (queueId != that.queueId) return false;
        if (mediaId != null ? !mediaId.equals(that.mediaId) : that.mediaId != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (subtitle != null ? !subtitle.equals(that.subtitle) : that.subtitle != null)
            return false;
        return !(artInfo != null ? !artInfo.equals(that.artInfo) : that.artInfo != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (queueId ^ (queueId >>> 32));
        result = 31 * result + (mediaId != null ? mediaId.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
        result = 31 * result + (artInfo != null ? artInfo.hashCode() : 0);
        return result;
    }
}
