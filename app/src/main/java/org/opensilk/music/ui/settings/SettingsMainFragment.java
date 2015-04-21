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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                    R.drawable.ic_phone_android_grey600_48dp));
            mFragments.add(new Holder(SettingsDataFragment.class.getName(),
                    mContext.getString(R.string.settings_data_category),
                    R.drawable.ic_data_usage_grey600_48dp));
            mFragments.add(new Holder(SettingsAudioFragment.class.getName(),
                    mContext.getString(R.string.settings_audio_category),
                    R.drawable.ic_tune_grey600_48dp));
            mFragments.add(new Holder(SettingsPluginFragment.class.getName(),
                    mContext.getString(R.string.settings_plugin_category),
                    R.drawable.ic_extension_grey600_48dp));
            // XXX add new items above this one.
            mFragments.add(new Holder(SettingsDonateFragment.class.getName(),
                    mContext.getString(R.string.settings_donate_category),
                    R.drawable.ic_attach_money_grey600_48dp));
            mFragments.add(new Holder(SettingsAboutFragment.class.getName(),
                    mContext.getString(R.string.settings_about_category),
                    R.drawable.ic_info_outline_grey600_48dp));
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
            TextView title;
            if (v == null) {
                v = mInflater.inflate(R.layout.settings_grid_item, parent, false);
                v.setTag(R.id.grid_item_text, v.findViewById(R.id.grid_item_text));
            }
            title = (TextView) v.getTag(R.id.grid_item_text);
            title.setText(mFragments.get(pos).title);
            int iconRes = mFragments.get(pos).iconRes;
            title.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            return v;
        }

        private static class Holder {
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
