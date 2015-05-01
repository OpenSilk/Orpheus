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

package org.opensilk.music.artwork;

import com.android.volley.ExecutorDelivery;

import java.util.concurrent.Executor;

import rx.Scheduler;
import rx.functions.Action0;

/**
 * Created by drew on 4/30/15.
 */
public class SchedulerResponseDelivery extends ExecutorDelivery {
    public SchedulerResponseDelivery() {
        super(new Executor() {
            @Override
            public void execute(final Runnable command) {
                final Scheduler.Worker worker = Constants.ARTWORK_SCHEDULER.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        command.run();
                        worker.unsubscribe();
                    }
                });
            }
        });
    }
}
