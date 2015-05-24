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

package org.opensilk.music.settings;

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

import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.music.R;

import java.util.ArrayList;
import java.util.List;

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
            int lastFmRes = ThemeUtils.isLightTheme(getContext())
                    ? R.drawable.lastfm_logo_light : R.drawable.lastfm_logo_dark;
            thankees.add(new Thankee(null, getResources().getDrawable(lastFmRes),
                    getString(R.string.settings_thanks_dialog_lfm_desc),
                    getString(R.string.settings_thanks_dialog_lfm_url)));
            thankees.add(new Thankee(getString(R.string.settings_thanks_dialog_caa), null,
                    getString(R.string.settings_thanks_dialog_caa_desc),
                    getString(R.string.settings_thanks_dialog_caa_url)));
            thankees.add(new Thankee(getString(R.string.settings_thanks_dialog_an), null,
                    getString(R.string.settings_thanks_dialog_an_desc), null));
            thankees.add(new Thankee(getString(R.string.settings_thanks_dialog_mc), null,
                    getString(R.string.settings_thanks_dialog_mc_desc),
                    getString(R.string.settings_thanks_dialog_mc_url)));
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
