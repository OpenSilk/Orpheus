/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.KeyEvent;

import com.andrew.apollo.utils.NavUtils;

import timber.log.Timber;


/**
 * Used to control headset playback.
 *   Single press: pause/resume
 *   Double press: next track
 *   Triple press: previous track
 *   Long press: voice search
 */
public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;

    private static final int LONG_PRESS_DELAY = 1000;
    private static final int DOUBLE_CLICK = 800;

    private static MediaHandler mHandler = new MediaHandler();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.v("Received intent: %s", intent);
        final String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            startService(context, MusicPlaybackService.CMDPAUSE);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
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
                    command = PlaybackConstants.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = PlaybackConstants.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = PlaybackConstants.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = PlaybackConstants.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = PlaybackConstants.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = PlaybackConstants.CMDPLAY;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mHandler.mDown) {
                        if (PlaybackConstants.CMDTOGGLEPAUSE.equals(command)
                                || PlaybackConstants.CMDPLAY.equals(command)) {
                            if (mHandler.mLastClickTime != 0
                                    && eventtime - mHandler.mLastClickTime > LONG_PRESS_DELAY) {
                                mHandler.acquireWakeLockAndSendMessage(context,
                                        mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context), 0);
                            }
                        }
                    } else if (event.getRepeatCount() == 0) {
                        // Only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it
                        // a command.
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (eventtime - mHandler.mLastClickTime >= DOUBLE_CLICK) {
                                mHandler.mClickCounter = 0;
                            }

                            mHandler.mClickCounter++;
                            Timber.v("Got headset click, count = %s", mHandler.mClickCounter);
                            mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                            Message msg = mHandler.obtainMessage(
                                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mHandler.mClickCounter, 0, context);

                            long delay = mHandler.mClickCounter < 3 ? DOUBLE_CLICK : 0;
                            if (mHandler.mClickCounter >= 3) {
                                mHandler.mClickCounter = 0;
                            }
                            mHandler.mLastClickTime = eventtime;
                            mHandler.acquireWakeLockAndSendMessage(context, msg, delay);
                        } else {
                            startService(context, command);
                        }
                        mHandler.mLaunched = false;
                        mHandler.mDown = true;
                    }
                } else {
                    mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mHandler.mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
                mHandler.releaseWakeLockIfHandlerIdle();
            }
        }
    }

    private static void startService(Context context, String command) {
        final Intent i = new Intent(context, PlaybackService.class);
        i.setAction(PlaybackConstants.SERVICECMD);
        i.putExtra(PlaybackConstants.CMDNAME, command);
        i.putExtra(PlaybackConstants.FROM_MEDIA_BUTTON, true);
        startWakefulService(context, i);
    }

    static class MediaHandler extends Handler {
        private WakeLock mWakeLock = null;
        private int mClickCounter = 0;
        private long mLastClickTime = 0;
        private boolean mDown = false;
        private boolean mLaunched = false;

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    Timber.v("Handling longpress timeout, launched %s", mLaunched);
                    if (!mLaunched) {
                        final Context context = (Context)msg.obj;
                        context.startActivity(NavUtils.makeLauncherIntent(context));
                        mLaunched = true;
                    }
                    break;

                case MSG_HEADSET_DOUBLE_CLICK_TIMEOUT:
                    final int clickCount = msg.arg1;
                    final String command;

                    Timber.v("Handling headset click, count = %s", clickCount);
                    switch (clickCount) {
                        case 1: command = MusicPlaybackService.CMDTOGGLEPAUSE; break;
                        case 2: command = MusicPlaybackService.CMDNEXT; break;
                        case 3: command = MusicPlaybackService.CMDPREVIOUS; break;
                        default: command = null; break;
                    }

                    if (command != null) {
                        final Context context = (Context)msg.obj;
                        startService(context, command);
                    }
                    break;
            }
            releaseWakeLockIfHandlerIdle();
        }

        private void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
            if (mWakeLock == null) {
                Context appContext = context.getApplicationContext();
                PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Apollo headset button");
                mWakeLock.setReferenceCounted(false);
            }
            Timber.v("Acquiring wake lock and sending %d", msg.what);
            // Make sure we don't indefinitely hold the wake lock under any circumstances
            mWakeLock.acquire(10000);

            sendMessageDelayed(msg, delay);
        }

        private void releaseWakeLockIfHandlerIdle() {
            if (mHandler.hasMessages(MSG_LONGPRESS_TIMEOUT)
                    || mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
                Timber.v("Handler still has messages pending, not releasing wake lock");
                return;
            }

            if (mWakeLock != null) {
                Timber.v("Releasing wake lock");
                mWakeLock.release();
                mWakeLock = null;
            }

        }
    }
}
