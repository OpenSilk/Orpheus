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

package org.opensilk.music.ui.settings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/6/14.
 */
public class ThanksDialogFragment extends DialogFragment {

    @Override
    //@DebugLog
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ThanksDialog(getActivity());
    }

    private class ThanksDialog extends Dialog {

        private ArrayAdapter mAdapter;
        private ListView mListView;

        public ThanksDialog(Context context) {
            super(context);
        }

        public ThanksDialog(Context context, int theme) {
            super(context, theme);
        }

        protected ThanksDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        @Override
        //@DebugLog
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.settings_thanks_listview);
            setTitle(R.string.settings_special_thanks);
            List<Thankee> thankees = new ArrayList<Thankee>(3);
            int lastFmRes = ThemeHelper.isLightTheme(getContext()) ? R.drawable.lastfm_logo_light
                    : R.drawable.lastfm_logo_dark;
            thankees.add(new Thankee(null, getResources().getDrawable(lastFmRes),
                    getString(R.string.settings_about_thanks_lfm_desc),
                    getString(R.string.settings_about_thanks_lfm_url)));
            thankees.add(new Thankee(getString(R.string.settings_about_thanks_caa), null,
                    getString(R.string.settings_about_thanks_caa_desc),
                    getString(R.string.settings_about_thanks_caa_url)));
            thankees.add(new Thankee(getString(R.string.settings_about_thanks_an), null,
                    getString(R.string.settings_about_thanks_an_desc), null));
            thankees.add(new Thankee(getString(R.string.settings_about_thanks_ac), null,
                    getString(R.string.settings_about_thanks_ac_desc), null));
            mAdapter = new ThanksAdapter(getContext(), thankees);
            mListView = (ListView) findViewById(android.R.id.list);
            mListView.setAdapter(mAdapter);
        }
    }

    private class ThanksAdapter extends ArrayAdapter<Thankee> {

        public ThanksAdapter(Context context, List<Thankee> Thankees) {
            super(context, 0, Thankees);
        }

        @Override
        //@DebugLog
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.settings_thanks_list_item, parent, false);
            }

            final Thankee t = getItem(position);

            TextView name = (TextView) v.findViewById(R.id.name);
            ImageView logo = (ImageView) v.findViewById(R.id.logo);
            TextView desc = (TextView) v.findViewById(R.id.desc);

            if (t.logo != null) {
                logo.setImageDrawable(t.logo);
                name.setVisibility(View.GONE);
                logo.setVisibility(View.VISIBLE);
            } else {
                name.setText(t.name);
                name.setVisibility(View.VISIBLE);
                logo.setVisibility(View.GONE);
            }

            desc.setText(t.desc);

            v.setClickable(true);
            if (t.url != null) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(t.url)));
                    }
                });
            }

            return v;
        }
    }

    private class Thankee {
        String name;
        Drawable logo;
        String desc;
        String url;

        Thankee(String name, Drawable logo, String desc, String url) {
            this.name= name;
            this.logo = logo;
            this.desc = desc;
            this.url = url;
        }

    }

}
