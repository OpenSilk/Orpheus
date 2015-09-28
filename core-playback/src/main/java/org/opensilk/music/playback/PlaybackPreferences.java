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

package org.opensilk.music.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.opensilk.common.core.app.PreferencesWrapper;
import org.opensilk.common.core.dagger2.ForApplication;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 5/7/15.
 */
@Singleton
public class PlaybackPreferences extends PreferencesWrapper {
    public static final String NAME = "Service";

    private static final String VERSION = "schemaversion";
    private static int myVersion = 300;

    public static final String CURRENT_POS = "curpos";
    public static final String SEEK_POS = "seekpos";
    public static final String QUEUE = "queue";

    final Context context;
    final Gson gson;

    @Inject
    public PlaybackPreferences(
            @ForApplication Context context,
            Gson gson
    ) {
        this.context = context;
        this.gson = gson;
        checkVersion();
    }

    @Override
    protected SharedPreferences getPrefs() {
        return context.getSharedPreferences(NAME, Context.MODE_MULTI_PROCESS);
    }

    void checkVersion() {
        int ver = getInt(VERSION, 0);
        if (ver < 2) {
            ver = myVersion;//New install
        }
        SharedPreferences.Editor editor = getPrefs().edit();
        if (ver < myVersion) {
            editor
                    .remove("cardid")
                    .remove(QUEUE)
                    .remove(CURRENT_POS)
                    .remove(SEEK_POS)
                    .remove("repeatmode")
                    .remove("shufflemode")
                    .remove("history")
                    .remove("autohistory");
        }
        editor.putInt(VERSION, myVersion).commit();
    }

    public List<Uri> getQueue() {
        String q = getString(QUEUE, null);
        if (q == null) {
            return Collections.emptyList();
        } else {
            try {
                return gson.fromJson(q, new TypeToken<List<Uri>>(){}.getType());
            } catch (Exception e) {
                remove(QUEUE);
                return Collections.emptyList();
            }
        }
    }

    public void saveQueue(List<Uri> queue) {
        if (queue.isEmpty()) {
            remove(QUEUE);
        } else {
            try {
                String j = gson.toJson(queue, new TypeToken<List<Uri>>(){}.getType());
                putString(QUEUE, j);
            } catch (Exception e) {
                remove(QUEUE);
            }
        }
    }
}
