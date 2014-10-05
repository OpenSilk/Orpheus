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

import org.opensilk.filebrowser.FileBrowserArgs;
import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.MediaProviderUtil;
import org.opensilk.filebrowser.WrappedAsyncTaskLoader;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Created by drew on 10/5/14.
 */
public class FileItemLoader implements AsyncLoader<FileItem> {

    final Context context;
    final String directory;
    LoaderTask<FileItem> task;
    LoaderCallback<FileItem> callback;

    final static Set<Integer> MEDIA_TYPES = new HashSet<>();
    static {
        MEDIA_TYPES.add(FileItem.MediaType.AUDIO);
        MEDIA_TYPES.add(FileItem.MediaType.DIRECTORY);
    }

    @Inject
    public FileItemLoader(@ForApplication Context context, String directory) {
        this.context = context;
        this.directory = directory.endsWith("/") ? directory.substring(0, directory.length()-1) : directory;
    }

    @Override
    public void loadAsync(LoaderCallback<FileItem> callback) {
        this.callback = callback;
        if (task == null) {
            task = new LoaderTask<FileItem>(context, this) {
                @Override
                protected List<FileItem> doInBackground(Object... params) {
                    final String directory = (String) params[0];
                    return MediaProviderUtil.ls(context, directory, MEDIA_TYPES);
                }
            };
            task.execute(directory);
        }
    }

    @Override
    public void onLoadComplete(List<FileItem> items) {
        if (callback != null) {
            callback.onLoadComplete(items);
            callback = null;
        }
        task = null;
    }

}
