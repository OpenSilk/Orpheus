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

package org.opensilk.music.ui.widget.timer;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.widget.RxTextView;

import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.service.IntentHelper;

import butterknife.ButterKnife;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Created by drew on 12/1/15.
 */
public class TimerDialog extends DialogFragment {

    int mValue;
    Subscription tvUpdater;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mValue = savedInstanceState.getInt("val");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.menu_sleep_timer);
        View v = ViewUtils.inflate(getActivity(), R.layout.timer_seekbar, null);
        b.setView(v);
        CircularSeekBar sb = ButterKnife.findById(v, R.id.seekbar);
        sb.setOnSeekBarChangeListener(mListener);
        if (mValue > 0) {
            sb.setProgress(mValue);
        } else {
            mValue = sb.getProgress();
        }
        TextView tv = ButterKnife.findById(v, R.id.timer_display);
        tvUpdater = updaterSubject.map(new Func1<Integer, String>() {
            @Override
            public String call(Integer integer) {
                return getActivity().getResources().getQuantityString(R.plurals.Nminutes, integer, integer);
            }
        }).subscribe(RxTextView.text(tv));
        updaterSubject.onNext(mValue);
        b.setPositiveButton(android.R.string.ok, mOkListener);
        b.setNeutralButton(R.string.cancel_current, mCancelCurrentListener);
        b.setNegativeButton(android.R.string.cancel, null);
        return b.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RxUtils.unsubscribe(tvUpdater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("val", mValue);
    }

    PendingIntent getPendingIntent() {
        Intent i = new Intent(PlaybackConstants.PAUSE_ACTION).setComponent(IntentHelper.getComponent(getActivity()));
        return PendingIntent.getService(getActivity(), 192, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    long getAlarmTime() {
        long now = SystemClock.elapsedRealtime();
        long offset = mValue * 60000;
        Timber.i("Scheduling sleep at %d, %d min from now", now + offset, mValue);
        return now + offset;
    }

    final DialogInterface.OnClickListener mOkListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mValue <= 0) {
                return;
            }
            AlarmManager manager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, getAlarmTime(), getPendingIntent());
            String msg = getActivity().getString(R.string.msg_sleep_timer_set, mValue);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
    };

    final DialogInterface.OnClickListener mCancelCurrentListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AlarmManager manager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
            manager.cancel(getPendingIntent());
            Toast.makeText(getActivity(), R.string.msg_sleep_timer_canceled, Toast.LENGTH_SHORT).show();
        }
    };

    final BehaviorSubject<Integer> updaterSubject = BehaviorSubject.create();

    final CircularSeekBar.OnCircularSeekBarChangeListener mListener = new CircularSeekBar.OnCircularSeekBarChangeListener() {
        @Override
        public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
            mValue = progress;
            updaterSubject.onNext(progress);
        }

        @Override
        public void onStopTrackingTouch(CircularSeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(CircularSeekBar seekBar) {

        }
    };
}
