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

package org.opensilk.music.plugin.drive.util;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.opensilk.music.plugin.drive.BuildConfig;
import org.opensilk.common.core.dagger2.ForApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 6/15/14.
 */
@Singleton
public class DriveHelperImpl implements DriveHelper {

    static class Holder implements Session {

        final GoogleAccountCredential credential;
        final Drive drive;

        Holder(GoogleAccountCredential credential, Drive drive) {
            this.credential = credential;
            this.drive = drive;
        }

        @Override
        public Drive getDrive() {
            return drive;
        }

        @Override
        public GoogleAccountCredential getCredential() {
            return credential;
        }
    }

    private static final String APP_NAME = BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME;
    private final Map<String, Holder> SESSIONS = new HashMap<>();

    private final Context context;

    @Inject
    public DriveHelperImpl(@ForApplication Context context) {
        this.context = context;
    }

    @Override
    public Session getSession(String accountName) {
        if (SESSIONS.containsKey(accountName)) return SESSIONS.get(accountName);
        return createSession(accountName);
    }

    @Override
    public void destroy() {
        SESSIONS.clear();
    }

    private Holder createSession(String accountName) {
        final GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)).setSelectedAccountName(accountName);
        final Drive drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                AndroidJsonFactory.getDefaultInstance(), credential).setApplicationName(APP_NAME).build();
        final Holder holder = new Holder(credential, drive);
        SESSIONS.put(accountName, holder);
        return holder;
    }

}
