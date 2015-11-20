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

package org.opensilk.music.library.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

/**
 * Created by drew on 11/20/15.
 */
public class ResultReceiverWrapper implements Parcelable {
    public final ResultReceiver mWrapped;

    public ResultReceiverWrapper(ResultReceiver mWrapped) {
        this.mWrapped = mWrapped;
    }

    public ResultReceiver get() {
        return mWrapped;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mWrapped.writeToParcel(dest, flags);
    }

    public static final Creator<ResultReceiverWrapper> CREATOR = new Creator<ResultReceiverWrapper>() {
        @Override
        public ResultReceiverWrapper createFromParcel(Parcel source) {
            ResultReceiver wrapped = ResultReceiver.CREATOR.createFromParcel(source);
            return new ResultReceiverWrapper(wrapped);
        }

        @Override
        public ResultReceiverWrapper[] newArray(int size) {
            return new ResultReceiverWrapper[size];
        }
    };
}
