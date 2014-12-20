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

package org.opensilk.music.dream;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.FloatingActionButtonCheckable;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.BroadcastObservables;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import mortar.Mortar;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by drew on 4/13/14.
 */
public class ChooserFragment extends Fragment implements
        ViewPager.OnPageChangeListener {

    static final int[] LAYOUTS;

    static {
        LAYOUTS = new int[] {
                //Order must match DreamPrefs.DreamLayout
                R.layout.daydream_art_only,
                R.layout.daydream_art_meta,
                R.layout.daydream_art_controls,
                R.layout.daydream_visualization,
        };
    }

    @Inject DreamPrefs mDreamPrefs;
    @Inject MusicServiceConnection mMusicService;

    @InjectView(R.id.pager) ViewPager mViewPager;
    @InjectView(R.id.checkmark) ImageView mCheckMark;
    @InjectView(R.id.floating_action_button) FloatingActionButtonCheckable mFab;

    ChooserPagerAdapter mPagerAdapter;
    Subscription playPauseSubscripton;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Mortar.inject(activity, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPagerAdapter = new ChooserPagerAdapter(getActivity().getLayoutInflater());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.daydream_pager, container, false);
        ButterKnife.inject(this, v);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setAdapter(mPagerAdapter);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        playPauseSubscripton = BroadcastObservables
                .playStateChanged(getActivity().getApplicationContext())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        mFab.setChecked(aBoolean);
                    }
                });
        mViewPager.setCurrentItem(mDreamPrefs.getDreamLayout());
        // Work around for fist item selected
        onPageSelected(mViewPager.getCurrentItem());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (playPauseSubscripton != null) {
            playPauseSubscripton.unsubscribe();
        }
    }

    @OnClick(R.id.checkmark)
    void selectDream() {
        mDreamPrefs.saveDreamLayout(mViewPager.getCurrentItem());
        setCheckSelected();
    }

    void setCheckSelected() {
        mCheckMark.setImageDrawable(
                ThemeUtils.colorizeBitmapDrawableCopy(
                        getActivity(),
                        R.drawable.ic_action_tick_white,
                        getResources().getColor(android.R.color.holo_blue_light)
                )
        );
    }

    void setCheckUnSelected() {
        mCheckMark.setImageResource(R.drawable.ic_action_tick_white);
    }

    @OnClick(R.id.floating_action_button)
    void playOrPause() {
        mMusicService.playOrPause();
    }

    /*
     * OnPageChangeListener
     */

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //pass
    }

    @Override
    public void onPageSelected(int position) {
        if (position == mDreamPrefs.getDreamLayout()) {
            setCheckSelected();
        } else {
            setCheckUnSelected();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //pass
    }

    /**
     * PagerAdapter, very simple just inflates the views into the container
     */
    static class ChooserPagerAdapter extends PagerAdapter {

        private final LayoutInflater mInflater;

        private ChooserPagerAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Extra layout to center our views in
            ViewGroup c = (ViewGroup) mInflater.inflate(R.layout.daydream_pager_container, container, false);
            mInflater.inflate(LAYOUTS[position], c, true);
            container.addView(c);
            return c;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return LAYOUTS.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }
    }
}
