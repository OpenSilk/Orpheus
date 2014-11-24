package org.opensilk.music.ui.settings;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsMainFragment extends Fragment {

    GridView mGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mGridView = (GridView) inflater.inflate(R.layout.settings_gridview, container, false);
        final SettingsAdapter adapter = new SettingsAdapter(getActivity());

        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SettingsFragment frag = (SettingsFragment) adapter.getItem(position);
                String tag = adapter.mFragments.get(position).className;
                getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.main, frag, tag)
                    .addToBackStack(null)
                    .commit();
            }
        });

        return mGridView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        activity.setTitle(R.string.settings_title);

        final Intent intent = getActivity().getIntent();
        if (intent != null && intent.getAction() != null) {
            if ("open_donate".equals(intent.getAction())) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGridView.getOnItemClickListener().onItemClick(null, null, mGridView.getAdapter().getCount()-2, 0);
                        getActivity().setIntent(intent.setAction(null));
                    }
                });
            }
        }
    }

    private static class SettingsAdapter extends BaseAdapter {

        private List<Holder> mFragments = new ArrayList<Holder>();
        private LayoutInflater mInflater;
        private Context mContext;

        public SettingsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFragments.add(new Holder(SettingsInterfaceFragment.class.getName(),
                    mContext.getString(R.string.settings_ui_category),
                    R.drawable.ic_settings_interface_light,
                    R.drawable.ic_settings_interface_dark));
            mFragments.add(new Holder(SettingsDataFragment.class.getName(),
                    mContext.getString(R.string.settings_data_category),
                    R.drawable.ic_settings_data_light,
                    R.drawable.ic_settings_data_dark));
            mFragments.add(new Holder(SettingsAudioFragment.class.getName(),
                    mContext.getString(R.string.settings_audio_category),
                    R.drawable.ic_settings_audio_light,
                    R.drawable.ic_settings_audio_dark));
            mFragments.add(new Holder(SettingsPluginFragment.class.getName(),
                    mContext.getString(R.string.settings_plugin_category),
                    R.drawable.ic_action_puzzle_black,
                    R.drawable.ic_action_puzzle_white));
            // Must be last, add new items above this one.
            mFragments.add(new Holder(SettingsDonateFragment.class.getName(),
                    mContext.getString(R.string.settings_donate_category),
                    R.drawable.ic_settings_donate_light,
                    R.drawable.ic_settings_donate_dark));
            mFragments.add(new Holder(SettingsAboutFragment.class.getName(),
                    mContext.getString(R.string.settings_about_category),
                    R.drawable.ic_settings_about_light,
                    R.drawable.ic_settings_about_dark));
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
            boolean light = ThemeUtils.isLightTheme(mContext);
            if (v == null) {
                v = mInflater.inflate(R.layout.settings_grid_item, parent, false);
//                v.setBackgroundColor(light ? Color.WHITE : Color.BLACK);
                v.setTag(R.id.grid_item_icon, v.findViewById(R.id.grid_item_icon));
                v.setTag(R.id.grid_item_text, v.findViewById(R.id.grid_item_text));
            }

            icon = (ImageView) v.getTag(R.id.grid_item_icon);
            title = (TextView) v.getTag(R.id.grid_item_text);

            int iconRes = light ? mFragments.get(pos).lightIconRes :
                    mFragments.get(pos).darkIconRes;
            icon.setImageResource(iconRes);
            title.setText(mFragments.get(pos).title);
            return v;
        }

        private static class Holder {
            String className;
            String title;
            int lightIconRes;
            int darkIconRes;

            Holder(String className, String title, int lightIconRes, int darkIconRes) {
                this.className = className;
                this.title = title;
                this.lightIconRes = lightIconRes;
                this.darkIconRes = darkIconRes;
            }

            Bundle getArguments() {
                Bundle b = new Bundle();
                b.putString("title", title);
                b.putInt("light_icon", lightIconRes);
                b.putInt("dark_icon", darkIconRes);
                return b;
            }
        }
    }
}
