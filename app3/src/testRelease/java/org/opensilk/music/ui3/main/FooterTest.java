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

package org.opensilk.music.ui3.main;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;
import org.opensilk.music.TestApp;
import org.opensilk.music.TestData;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.TestMusicActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Created by drew on 11/16/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        application = TestApp.class,
        sdk = 21
)
public class FooterTest {

    FooterScreenView view;
    ActivityController<TestMusicActivity> activityController;
    BehaviorSubject<PlaybackStateCompat> playbackSubject;
    BehaviorSubject<List<MediaSessionCompat.QueueItem>> queueSubject;

    @org.junit.Before
    public void setup(){
        //create host activity
        activityController = Robolectric.buildActivity(TestMusicActivity.class).create();
        //mock playback contoller
        PlaybackController playbackController = DaggerService.
                <AppComponent>getDaggerComponent(RuntimeEnvironment.application).playbackController();
        playbackSubject = BehaviorSubject.create();
        when(playbackController.subscribePlayStateChanges((Action1)any()))
                .thenAnswer(new Answer<Subscription>() {
                    @Override
                    public Subscription answer(InvocationOnMock invocation) throws Throwable {
                        return playbackSubject.subscribe(invocation.getArgumentAt(0, Action1.class));
                    }
                });
        queueSubject = BehaviorSubject.create();
        when(playbackController.subscribeQueueChanges((Action1)any()))
                .thenAnswer(new Answer<Subscription>() {
                    @Override
                    public Subscription answer(InvocationOnMock invocation) throws Throwable {
                        return queueSubject.subscribe(invocation.getArgumentAt(0, Action1.class));
                    }
                });
        //initialize fragment
        FooterScreenFragment f = new FooterScreenFragment();
        activityController.get().getSupportFragmentManager().beginTransaction().replace(R.id.main, f).commit();
        activityController.get().getSupportFragmentManager().executePendingTransactions();
        activityController.start().postCreate(null).resume().visible();
        view = (FooterScreenView) f.getView();
        assertThat(view).isNotNull();
    }

    @Test
    public void testLoadQueue() {
        queueSubject.onNext(TestData.QUEUE_1);
        assertThat(view.mViewPager.getAdapter().getCount()).isEqualTo(10);
    }

}
