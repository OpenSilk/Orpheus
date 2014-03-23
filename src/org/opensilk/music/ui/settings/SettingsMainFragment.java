package org.opensilk.music.ui.settings;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsMainFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_gridview, container, false);

        GridView grid = (GridView) v.findViewById(R.id.settings_grid);
        final SettingsAdapter adapter = new SettingsAdapter(getActivity());

        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SettingsFragment frag = (SettingsFragment) adapter.getItem(position);
                getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.settings_content, frag)
                    .addToBackStack(frag.getTitle())
                    .commit();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.settings_title);
        getActivity().getActionBar().setIcon(R.drawable.ic_settings);
    }

    private class SettingsAdapter extends BaseAdapter {

        private List<Holder> mFragments = new ArrayList<Holder>();
        private LayoutInflater mInflater;
        private Context mContext;

        public SettingsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFragments.add(new Holder(SettingsInterfaceFragment.class.getName(),
                    getString(R.string.settings_ui_category),
                    R.drawable.ic_settings_interface));
            mFragments.add(new Holder(SettingsDataFragment.class.getName(),
                    getString(R.string.settings_data_category),
                    R.drawable.ic_settings_data));
            mFragments.add(new Holder(SettingsAboutFragment.class.getName(),
                    getString(R.string.settings_about_category),
                    R.drawable.ic_settings_about));
            mFragments.add(new Holder(SettingsAudioFragment.class.getName(),
                    getString(R.string.settings_audio_title),
                    R.drawable.ic_settings_audio));
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Object getItem(int pos) {
            return Fragment.instantiate(mContext, mFragments.get(pos).className, mFragments.get(pos).getArguments());
        }

        @Override
        public long getItemId(int pos) {
            return 0;
        }

        @Override
        public View getView(int pos, View view, ViewGroup parent) {
            View v = view;
            ImageView icon;
            TextView title;

            if (v == null) {
                v = mInflater.inflate(R.layout.settings_grid_item, parent, false);
                v.setTag(R.id.grid_item_icon, v.findViewById(R.id.grid_item_icon));
                v.setTag(R.id.grid_item_text, v.findViewById(R.id.grid_item_text));
            }

            icon = (ImageView) v.getTag(R.id.grid_item_icon);
            title = (TextView) v.getTag(R.id.grid_item_text);

            icon.setImageResource(mFragments.get(pos).iconRes);
            title.setText(mFragments.get(pos).title);
            return v;
        }

        private class Holder {
            String className;
            String title;
            int iconRes;

            Holder(String className, String title, int iconRes) {
                this.className = className;
                this.title = title;
                this.iconRes = iconRes;
            }

            Bundle getArguments() {
                Bundle b = new Bundle();
                b.putString("title", title);
                b.putInt("icon", iconRes);
                return b;
            }
        }
    }
}
