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

package com.andrew.apollo.menu;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew.apollo.MusicPlaybackService;
import org.opensilk.music.R;
import com.triggertrap.seekarc.SeekArc;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.GraphHolder;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Created by drew on 8/30/14.
 */
public class SleepTimerDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimerSetDialog(getActivity());
    }

    public class TimerSetDialog extends AlertDialog {

        protected TimerSetDialog(Context context) {
            super(context);
        }

        public static final String PREF_PREVIOUS_TIMER_VALUE = "last_sleep_timer_value";

        @InjectView(R.id.timer_set) SeekArc mTimerSet;
        @InjectView(R.id.timer_display) TextView mTimerDisplay;
        @InjectView(R.id.timer_button_bar) ViewGroup mTimerButtonBar;

        AppPreferences mSettings;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            mSettings = GraphHolder.get(getContext()).getObj(AppPreferences.class);
            View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_timer, null);
            setView(v);
            setButton(BUTTON_POSITIVE, getContext().getResources().getString(android.R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final int min = mTimerSet.getProgress();
                    // save new value
                    mSettings.putInt(PREF_PREVIOUS_TIMER_VALUE, min);
                    // build intent
                    PendingIntent pi = PendingIntent.getService(getContext(), 0, makeTimerIntent(), PendingIntent.FLAG_CANCEL_CURRENT);
                    // set alarm
                    AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + min * 60 * 1000, pi);
                    // notify
                    Timber.i("Sleep timer set for %d minutes from now", min);
                    Toast.makeText(getContext(), getContext().getResources().getString(R.string.msg_sleep_timer_set, min), Toast.LENGTH_SHORT).show();
                }
            });
            setButton(BUTTON_NEGATIVE, getContext().getResources().getString(android.R.string.cancel), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            setTitle(R.string.menu_sleep_timer);
            super.onCreate(savedInstanceState);
            ButterKnife.inject(this);
            final int prevMin = mSettings.getInt(PREF_PREVIOUS_TIMER_VALUE, 30);
            mTimerSet.setProgress(prevMin);
            mTimerSet.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekArc, int i, boolean b) {
                    mTimerDisplay.setText(String.valueOf(i)+" min");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekArc) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekArc) {

                }
            });
            mTimerDisplay.setText(prevMin+" min");
            final PendingIntent previous = PendingIntent.getService(getContext(), 0, makeTimerIntent(), PendingIntent.FLAG_NO_CREATE);
            if (previous != null) {
                mTimerButtonBar.setVisibility(View.VISIBLE);
            }
        }

        @OnClick(R.id.timer_cancel_previous)
        protected void cancelPrevious() {
            final PendingIntent previous = PendingIntent.getService(getContext(), 0, makeTimerIntent(), PendingIntent.FLAG_NO_CREATE);
            if (previous != null) {
                AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                am.cancel(previous);
                previous.cancel();
            }
            mTimerButtonBar.setVisibility(View.GONE);
        }

        private Intent makeTimerIntent() {
            return new Intent(getContext(), MusicPlaybackService.class)
                    .setAction(MusicPlaybackService.STOP_ACTION);
        }
    }
}
