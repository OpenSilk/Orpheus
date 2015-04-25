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

package com.andrew.apollo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Created by drew on 4/25/15.
 */
public class MediaButtonHandler extends Handler {
    private static final int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;
    private static final int DOUBLE_CLICK = 300;
    private int mClickCounter = 0;
    private long mLastClickTime = 0;

    private final WeakReference<MusicPlaybackService> mService;

    public MediaButtonHandler(MusicPlaybackService mService) {
        this.mService = new WeakReference<MusicPlaybackService>(mService);
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_HEADSET_DOUBLE_CLICK_TIMEOUT:
                final int clickCount = msg.arg1;
                final String command;

                Timber.v("Handling headset click, count = " + clickCount);
                switch (clickCount) {
                    case 1: command = MusicPlaybackService.CMDTOGGLEPAUSE; break;
                    case 2: command = MusicPlaybackService.CMDNEXT; break;
                    case 3: command = MusicPlaybackService.CMDPREVIOUS; break;
                    default: command = null; break;
                }

                if (command != null) {
                    sendCommand(command);
                }
                break;
        }
    }

    public void resetCounter() {
        mClickCounter = 0;
    }

    public void processIntent(Intent intent) {
        final String intentAction = intent.getAction();
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            final KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            final int keycode = event.getKeyCode();
            final int action = event.getAction();
            final long eventtime = event.getEventTime();

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    Timber.v( "Keycode=MEDIA_STOP");
                    command = MusicPlaybackService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    Timber.v("Keycode=" + (keycode==KeyEvent.KEYCODE_HEADSETHOOK ? "HEADSETHOOK" : "PLAY_PAUSE"));
                    command = MusicPlaybackService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    Timber.v("Keycode=MEDIA_NEXT");
                    command = MusicPlaybackService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    Timber.v("Keycode=MEDIA_PREVIOUS");
                    command = MusicPlaybackService.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    Timber.v("Keycode=MEDIA_PAUSE");
                    command = MusicPlaybackService.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    Timber.v("Keycode=MEDIA_PLAY");
                    command = MusicPlaybackService.CMDPLAY;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) {
                        // Only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it
                        // a command.
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (eventtime - mLastClickTime >= DOUBLE_CLICK) {
                                mClickCounter = 0;
                            }

                            mClickCounter++;
                            Timber.v("Got headset click, count = " + mClickCounter);
                            removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                            Message msg = obtainMessage(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0);

                            long delay = mClickCounter < 3 ? DOUBLE_CLICK : 0;
                            if (mClickCounter >= 3) {
                                mClickCounter = 0;
                            }
                            mLastClickTime = eventtime;
                            acquireWakeLockAndSendMessage(msg, delay);
                        } else {
                            sendCommand(command);
                        }
                    }
                }
            }
        }
    }

    private void sendCommand(String command) {
        final Intent i = new Intent();
        i.setAction(MusicPlaybackService.SERVICECMD);
        i.putExtra(MusicPlaybackService.CMDNAME, command);
        MusicPlaybackService s = mService.get();
        if (s != null) {
            s.postCommandIntent(i);
        }
    }

    private void acquireWakeLockAndSendMessage(Message msg, long delay) {
        Timber.v("Acquiring wake lock and sending " + msg.what);
        // Make sure we don't indefinitely hold the wake lock under any circumstances
        MusicPlaybackService s = mService.get();
        if (s != null) {
            s.acquireWakeLock(10000);
            sendMessageDelayed(msg, delay);
        }
    }

}
