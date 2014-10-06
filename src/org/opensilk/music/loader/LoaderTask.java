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

package org.opensilk.music.loader;

import android.content.Context;

import org.opensilk.music.util.PriorityAsyncTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by drew on 10/5/14.
 */
public abstract class LoaderTask<T> extends PriorityAsyncTask<Object, Void, List<T>> {

    final Context context;
    final Set<AsyncLoader.Callback<T>> callbacks;

    public LoaderTask(Context context, AsyncLoader.Callback<T> callback) {
        super();
        this.context = context;
        callbacks = new HashSet<>();
        callbacks.add(callback);
    }

    public void addListener(AsyncLoader.Callback<T> callback) {
        callbacks.add(callback);
    }

    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    @Override
    protected void onPostExecute(List<T> etems) {
        for (AsyncLoader.Callback<T> callback : callbacks) {
            callback.onDataFetched(etems);
        }
    }

}
