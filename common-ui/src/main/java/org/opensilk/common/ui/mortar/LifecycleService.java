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

package org.opensilk.common.ui.mortar;

import android.content.Context;
import android.view.View;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewAttachEvent;

import mortar.MortarScope;
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.functions.Func2;

import static java.lang.String.format;

/**
 * Simplified version of trellos RxLifecycle (https://github.com/trello/RxLifecycle)
 * Allows presenters to bind observables to parent lifecycle without knowing who
 * controls the lifecycle
 *
 * Created by drew on 10/12/15.
 */
public class LifecycleService {
    public static final String LIFECYCLE_SERVICE = LifecycleService.class.getName();

    /**
     * @throws IllegalArgumentException if there is no LifecycleService attached to this scope
     * @return The Lifecycle Observable associated with this context
     */
    @SuppressWarnings("unchecked") //
    public static Observable<Lifecycle> getLifecycle(Context context) {
        //noinspection ResourceType
        return (Observable<Lifecycle>) context.getSystemService(LIFECYCLE_SERVICE);
    }


    /**
     * @throws IllegalArgumentException if there is no LifecycleService attached to this scope
     * @return The Lifecycle Observable associated with this scope
     */
    @SuppressWarnings("unchecked") //
    public static Observable<Lifecycle> getLifecycle(MortarScope scope) {
        if (scope.hasService(LIFECYCLE_SERVICE)) {
            return (Observable<Lifecycle>) scope.getService(LIFECYCLE_SERVICE);
        }
        throw new IllegalArgumentException(format("No lifecycle service found in scope %s", scope.getName()));
    }

    /**
     * Binds the given source to a Lifecycle.
     * <p>
     * When the lifecycle event occurs, the source will cease to emit any notifications.
     * <p>
     * Use with {@link Observable#compose(Observable.Transformer)}:
     * {@code source.compose(RxLifecycle.bindUntilEvent(lifecycle, FragmentEvent.STOP)).subscribe()}
     *
     * @param lifecycle the Fragment lifecycle sequence
     * @param event the event which should conclude notifications from the source
     * @return a reusable {@link Observable.Transformer} that unsubscribes the source at the specified event
     */
    public static <T> Observable.Transformer<? super T, ? extends T> bindUntilLifecycleEvent(
            final Observable<Lifecycle> lifecycle, final Lifecycle event) {
        return bindUntilEvent(lifecycle, event);
    }

    public static <T> Observable.Transformer<? super T, ? extends T> bindUntilLifecycleEvent(
            final MortarScope scope, final Lifecycle event) {
        return bindUntilEvent(getLifecycle(scope), event);
    }

    public static <T> Observable.Transformer<? super T, ? extends T> bindUntilLifecycleEvent(
            final Context context, final Lifecycle event) {
        return bindUntilEvent(getLifecycle(context), event);
    }

    private static <T, R> Observable.Transformer<? super T, ? extends T> bindUntilEvent(
            final Observable<R> lifecycle, final R event) {
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be given");
        }
        else if (event == null) {
            throw new IllegalArgumentException("Event must be given");
        }

        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> source) {
                return source.takeUntil(
                        lifecycle.takeFirst(new Func1<R, Boolean>() {
                            @Override
                            public Boolean call(R lifecycleEvent) {
                                return lifecycleEvent == event;
                            }
                        })
                );
            }
        };
    }

    /**
     * Binds the given source to a Lifecycle.
     * <p>
     * Use with {@link Observable#compose(Observable.Transformer)}:
     * {@code source.compose(RxLifecycle.bindActivity(lifecycle)).subscribe()}
     * <p>
     * This helper automatically determines (based on the lifecycle sequence itself) when the source
     * should stop emitting items. In the case that the lifecycle sequence is in the
     * creation phase (START, RESUME) it will choose the equivalent destructive phase (PAUSE,
     * STOP). If used in the destructive phase, the notifications will cease at the next event;
     * for example, if used in PAUSE, it will unsubscribe in STOP.
     *
     * @param lifecycle the lifecycle sequence of an Activity
     * * @return a reusable {@link Observable.Transformer} that unsubscribes the source during the Activity lifecycle
     */
    public static <T> Observable.Transformer<? super T, ? extends T> bindLifecycle(Observable<Lifecycle> lifecycle) {
        return bind(lifecycle, LIFECYCLE);
    }

    public static <T> Observable.Transformer<? super T, ? extends T> bindLifecycle(MortarScope scope) {
        return bind(getLifecycle(scope), LIFECYCLE);
    }

    public static <T> Observable.Transformer<? super T, ? extends T> bindLifecycle(Context context) {
        return bind(getLifecycle(context), LIFECYCLE);
    }

    private static <T, R> Observable.Transformer<? super T, ? extends T> bind(Observable<R> lifecycle,
                                                                              final Func1<R, R> correspondingEvents) {
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be given");
        }

        // Make sure we're truly comparing a single stream to itself
        final Observable<R> sharedLifecycle = lifecycle.share();

        // Keep emitting from source until the corresponding event occurs in the lifecycle
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> source) {
                return source.takeUntil(
                        Observable.combineLatest(
                                sharedLifecycle.take(1).map(correspondingEvents),
                                sharedLifecycle.skip(1),
                                new Func2<R, R, Boolean>() {
                                    @Override
                                    public Boolean call(R bindUntilEvent, R lifecycleEvent) {
                                        return lifecycleEvent == bindUntilEvent;
                                    }
                                })
                                .onErrorReturn(RESUME_FUNCTION)
                                .takeFirst(SHOULD_COMPLETE)
                );
            }
        };
    }

    private static final Func1<Throwable, Boolean> RESUME_FUNCTION = new Func1<Throwable, Boolean>() {
        @Override
        public Boolean call(Throwable throwable) {
            if (throwable instanceof OutsideLifecycleException) {
                return true;
            }

            Exceptions.propagate(throwable);
            return false;
        }
    };

    private static final Func1<Boolean, Boolean> SHOULD_COMPLETE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean shouldComplete) {
            return shouldComplete;
        }
    };

    // Figures out which corresponding next lifecycle event in which to unsubscribe
    private static final Func1<Lifecycle, Lifecycle> LIFECYCLE =
            new Func1<Lifecycle, Lifecycle>() {
                @Override
                public Lifecycle call(Lifecycle lastEvent) {
                    switch (lastEvent) {
                        case START:
                            return Lifecycle.STOP;
                        case RESUME:
                            return Lifecycle.PAUSE;
                        case PAUSE:
                            return Lifecycle.STOP;
                        case STOP:
                            throw new OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.");
                        default:
                            throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
                    }
                }
            };

    private static class OutsideLifecycleException extends IllegalStateException {
        public OutsideLifecycleException(String detailMessage) {
            super(detailMessage);
        }
    }

}
