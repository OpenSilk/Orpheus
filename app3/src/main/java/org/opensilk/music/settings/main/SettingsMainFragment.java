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

package org.opensilk.music.settings.main;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.music.R;
import org.opensilk.music.ui3.common.RecyclerAdapterItemClickDelegate;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsMainFragment extends Fragment implements RecyclerAdapterItemClickDelegate.ItemClickListener {

    @InjectView(R.id.recyclerview) RecyclerView mList;
    SettingsMainRecyclerAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        mAdapter = new SettingsMainRecyclerAdapter();
        mAdapter.setClickListener(this);
        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mList.setHasFixedSize(true);
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
        mAdapter = null;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.settings_title);
    }

    @Override
    public void onItemClicked(Context context, int pos) {
        SettingsMainItem item = mAdapter.getItem(pos);
        if ("donate".equals(item.className)) {
//            mDonateManager.launchDonateActivity(getActivity());
        } else {
            Fragment frag = Fragment.instantiate(getActivity(), item.className, item.getArguments());
            getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.main, frag, item.className)
                    .addToBackStack(null)
                    .commit();
        }
    }

}
