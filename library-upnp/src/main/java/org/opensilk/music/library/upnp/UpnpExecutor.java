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

package org.opensilk.music.library.upnp;

import org.seamless.util.Exceptions;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Created by drew on 10/23/15.
 */
public class UpnpExecutor {

    private static Logger log = Logger.getLogger(UpnpExecutor.class.getName());
    private static final int threadPoolSize = (Runtime.getRuntime().availableProcessors() * 2) + 1;

    public static class ClingExecutor extends ThreadPoolExecutor {

        public ClingExecutor() {
            this(new ClingThreadFactory(),
                    new ThreadPoolExecutor.DiscardPolicy() {
                        // The pool is unbounded but rejections will happen during shutdown
                        @Override
                        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                            // Log and discard
                            log.info("Thread pool rejected execution of " + runnable.getClass());
                            super.rejectedExecution(runnable, threadPoolExecutor);
                        }
                    }
            );
        }

        public ClingExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
            super(0,
                    threadPoolSize,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    threadFactory,
                    rejectedHandler
            );
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (throwable != null) {
                Throwable cause = Exceptions.unwrap(throwable);
                if (cause instanceof InterruptedException) {
                    // Ignore this, might happen when we shutdownNow() the executor. We can't
                    // log at this point as the logging system might be stopped already (e.g.
                    // if it's a CDI component).
                    return;
                }
                // Log only
                log.warning("Thread terminated " + runnable + " abruptly with exception: " + throwable);
                log.warning("Root cause: " + cause);
            }
        }
    }

    // Executors.DefaultThreadFactory is package visibility (...no touching, you unworthy JDK user!)
    public static class ClingThreadFactory implements ThreadFactory {

        protected final ThreadGroup group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String namePrefix = "cling-";

        public ClingThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(
                    group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0
            );
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }
}
