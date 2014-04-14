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

import android.app.Fragment;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 4/13/14.
 */
public class ChooserFragment extends Fragment implements
        View.OnClickListener,
        ViewPager.OnPageChangeListener {

    protected ViewPager mViewPager;
    protected ChooserPagerAdapter mPagerAdapter;
    protected ImageView mCheckMark;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPagerAdapter = new ChooserPagerAdapter(getActivity().getLayoutInflater());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.daydream_pager, container, false);
        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setAdapter(mPagerAdapter);
        mCheckMark = (ImageView) v.findViewById(R.id.checkmark);
        mCheckMark.setOnClickListener(this);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        mViewPager.setCurrentItem(DreamPrefs.getDreamLayout(getActivity()));
        // Work around for fist item selected
        onPageSelected(mViewPager.getCurrentItem());
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
        if (position == DreamPrefs.getDreamLayout(getActivity())) {
            setCheckSelected();
        } else {
            setCheckUnSelected();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //pass
    }

    /*
     * OnClickListener
     */

    @Override
    public void onClick(View v) {
        if (v == mCheckMark) {
            DreamPrefs.saveDreamLayout(getActivity(), mViewPager.getCurrentItem());
            setCheckSelected();
        }
    }

    protected void setCheckSelected() {
        mCheckMark.setImageDrawable(ThemeHelper.themeDrawable(getActivity(),
                R.drawable.ic_action_tick_white,
                getResources().getColor(android.R.color.holo_blue_light)));
    }

    protected void setCheckUnSelected() {
        mCheckMark.setImageResource(R.drawable.ic_action_tick_white);
    }

    /**
     * PagerAdapter, very simple just inflates the views into the container
     */
    private static class ChooserPagerAdapter extends PagerAdapter {

        private final LayoutInflater mInflater;
        private static final int[] LAYOUTS;

        static {
            LAYOUTS = new int[] {
                    //Order must match DreamPrefs.DreamLayout
                    R.layout.daydream_art_only,
                    R.layout.daydream_art_meta,
                    R.layout.daydream_art_controls,
            };
        }

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
