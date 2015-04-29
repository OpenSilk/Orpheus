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

package org.opensilk.music.plugin.drive;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.plugin.drive.util.DriveHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 4/28/15.
 */
@Singleton
public class SessionFactory {

    public static class Session {

        final GoogleAccountCredential credential;
        final Drive drive;

        public Session (GoogleAccountCredential credential, Drive drive) {
            this.credential = credential;
            this.drive = drive;
        }

        public Drive getDrive() {
            return drive;
        }

        public GoogleAccountCredential getCredential() {
            return credential;
        }
    }

    private static final String APP_NAME = BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME;
    private final Map<String, Session> sSessions = Collections.synchronizedMap(new HashMap<String, Session>());

    private final Context context;

    @Inject
    public SessionFactory(@ForApplication Context context) {
        this.context = context;
    }

    public Session getSession(String accountName) {
        Session s = sSessions.get(accountName);
        if (s != null) {
            return s;
        }
        return createSession(accountName);
    }

    private Session createSession(String accountName) {
        final GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)).setSelectedAccountName(accountName);
        final Drive drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                AndroidJsonFactory.getDefaultInstance(), credential).setApplicationName(APP_NAME).build();
        final Session holder = new Session(credential, drive);
        sSessions.put(accountName, holder);
        return holder;
    }
}
