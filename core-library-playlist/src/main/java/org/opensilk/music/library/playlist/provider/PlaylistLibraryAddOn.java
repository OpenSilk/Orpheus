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

package org.opensilk.music.library.playlist.provider;

import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.library.playlist.PlaylistExtras;
import org.opensilk.music.library.playlist.PlaylistOperationListener;
import org.opensilk.music.library.playlist.internal.PlaylistIntegerResult;
import org.opensilk.music.library.playlist.internal.PlaylistPlaylistResult;
import org.opensilk.music.library.playlist.internal.PlaylistUriResult;
import org.opensilk.music.library.playlist.provider.PlaylistMethods;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Playlist;

import java.util.List;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 12/10/15.
 */
public class PlaylistLibraryAddOn {

    final Scheduler playlistScheduler;
    final Handler handler;

    public PlaylistLibraryAddOn(Scheduler playlistScheduler, Handler handler) {
        this.playlistScheduler = playlistScheduler;
        this.handler = handler;
    }

    public Reply handleCall(String method, String arg, Bundle extras) {

        final PlaylistExtras.Builder ok = PlaylistExtras.b();
        ok.putOk(true);

        if (extras != null) {
            extras.setClassLoader(getClass().getClassLoader());
        }

        if (method == null) method = "_";
        switch (method) {
            case PlaylistMethods.CREATE: {
                final String name = PlaylistExtras.getName(extras);
                if (StringUtils.isEmpty(name)) {
                    return new Reply(ok.putOk(false).putError("No name in extras").get());
                }
                final ResultReceiver resultReceiver = PlaylistExtras.getResultReceiver(extras);
                final PlaylistUriResult resultHandler = new PlaylistUriResult(resultReceiver);
                final Bundle sanitizedExtras = PlaylistExtras.sanitize(extras);
                final Scheduler.Worker worker = playlistScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        handler.createPlaylist(name, resultHandler, sanitizedExtras);
                        worker.unsubscribe();
                    }
                });
                return new Reply(ok.get());
            }
            case PlaylistMethods.ADD_TO: {
                final Uri plist = PlaylistExtras.getUri(extras);
                final List<Uri> list = PlaylistExtras.getUriList(extras);
                if (plist == null || list == null || list.isEmpty()) {
                    return new Reply(ok.putOk(false).putError("No playlist uri or uri list").get());
                }
                final ResultReceiver resultReceiver = PlaylistExtras.getResultReceiver(extras);
                final PlaylistPlaylistResult resultHandler = new PlaylistPlaylistResult(resultReceiver);
                final Bundle sanitizedExtras = PlaylistExtras.sanitize(extras);
                final Scheduler.Worker worker = playlistScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        handler.addToPlaylist(plist, list, resultHandler, sanitizedExtras);
                        worker.unsubscribe();
                    }
                });
                return new Reply(ok.get());
            }
            case PlaylistMethods.REMOVE_FROM: {
                final Uri plist = PlaylistExtras.getUri(extras);
                final List<Uri> list = PlaylistExtras.getUriList(extras);
                if (plist == null || list == null || list.isEmpty()) {
                    ok.putOk(false).putError("No playlist uri or uri list");
                }
                final ResultReceiver resultReceiver = PlaylistExtras.getResultReceiver(extras);
                final PlaylistPlaylistResult resultHandler = new PlaylistPlaylistResult(resultReceiver);
                final Bundle sanitizedExtras = PlaylistExtras.sanitize(extras);
                final Scheduler.Worker worker = playlistScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        handler.removeFromPlaylist(plist, list, resultHandler, sanitizedExtras);
                        worker.unsubscribe();
                    }
                });
                return new Reply(ok.get());
            }
            case PlaylistMethods.UPDATE: {
                final Uri plist = PlaylistExtras.getUri(extras);
                final List<Uri> list = PlaylistExtras.getUriList(extras);
                if (plist == null || list == null || list.isEmpty()) {
                    return new Reply(ok.putOk(false).putError("No playlist uri or uri list").get());
                }
                final ResultReceiver resultReceiver = PlaylistExtras.getResultReceiver(extras);
                final PlaylistPlaylistResult resultHandler = new PlaylistPlaylistResult(resultReceiver);
                final Bundle sanitizedExtras = PlaylistExtras.sanitize(extras);
                final Scheduler.Worker worker = playlistScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        handler.updatePlaylist(plist, list, resultHandler, sanitizedExtras);
                        worker.unsubscribe();
                    }
                });
                return new Reply(ok.get());
            }
            case PlaylistMethods.DELETE: {
                final List<Uri> list = PlaylistExtras.getUriList(extras);
                if (list == null || list.isEmpty()) {
                    return new Reply(ok.putOk(false).putError("No uri list").get());
                }
                final ResultReceiver resultReceiver = PlaylistExtras.getResultReceiver(extras);
                final PlaylistIntegerResult resultHandler = new PlaylistIntegerResult(resultReceiver);
                final Bundle sanitizedExtras = PlaylistExtras.sanitize(extras);
                final Scheduler.Worker worker = playlistScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        handler.deletePlaylists(list, resultHandler, sanitizedExtras);
                        worker.unsubscribe();
                    }
                });
                return new Reply(ok.get());
            }
            default:
                return new Reply();
        }
    }

    public interface Handler {
        void createPlaylist(String name, PlaylistOperationListener<Uri> resultListener, Bundle extras);
        void addToPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras);
        void removeFromPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras);
        void updatePlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras);
        void deletePlaylists(List<Uri> playlists, PlaylistOperationListener<Integer> resultListener, Bundle extras);
    }

    public static class Reply {
        private final boolean handled;
        private final Bundle reply;

        private Reply() {
            this.handled = false;
            this.reply = null;
        }

        private Reply(Bundle reply) {
            this.handled = true;
            this.reply = reply;
        }

        public boolean isHandled() {
            return handled;
        }

        public Bundle getReply() {
            return reply;
        }
    }

}
