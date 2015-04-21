package org.opensilk.music.ui.settings;

import android.app.Activity;
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

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.iab.core.DonateManager;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsMainFragment extends Fragment {

    @dagger.Module(addsTo = SettingsActivity.Module.class, injects = SettingsMainFragment.class)
    public static class Module {

    }

    @Inject DonateManager mDonateManager;

    GridView mGridView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity).getObjectGraph().plus(new Module()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mGridView = (GridView) inflater.inflate(R.layout.settings_gridview, container, false);
        final SettingsAdapter adapter = new SettingsAdapter();

        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Holder holder = adapter.getItem(position);
                if ("donate".equals(holder.className)) {
                    mDonateManager.launchDonateActivity(getActivity());
                } else {
                    Fragment frag = Fragment.instantiate(getActivity(), holder.className, holder.getArguments());
                    getFragmentManager().beginTransaction()
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .replace(R.id.main, frag, holder.className)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        return mGridView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        activity.setTitle(R.string.settings_title);
    }

    static class SettingsAdapter extends BaseAdapter {

        private List<Holder> mFragments = new ArrayList<Holder>();

        public SettingsAdapter() {
            mFragments.add(new Holder(SettingsInterfaceFragment.class.getName(),
                    R.string.settings_ui_category,
                    R.drawable.ic_phone_android_grey600_48dp));
            mFragments.add(new Holder(SettingsDataFragment.class.getName(),
                    R.string.settings_data_category,
                    R.drawable.ic_data_usage_grey600_48dp));
            mFragments.add(new Holder(SettingsAudioFragment.class.getName(),
                    R.string.settings_audio_category,
                    R.drawable.ic_tune_grey600_48dp));
            mFragments.add(new Holder(SettingsPluginFragment.class.getName(),
                    R.string.settings_plugin_category,
                    R.drawable.ic_extension_grey600_48dp));
            // XXX add new items above this one.
            mFragments.add(new Holder("donate", //XXX hack
                    R.string.settings_donate_category,
                    R.drawable.ic_attach_money_grey600_48dp));
            mFragments.add(new Holder(SettingsAboutFragment.class.getName(),
                    R.string.settings_about_category,
                    R.drawable.ic_info_outline_grey600_48dp));
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Holder getItem(int pos) {
            return mFragments.get(pos);
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
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_grid_item, parent, false);
                v.setTag(R.id.grid_item_text, v.findViewById(R.id.grid_item_text));
            }
            title = (TextView) v.getTag(R.id.grid_item_text);
            title.setText(parent.getContext().getString(getItem(pos).title));
            title.setCompoundDrawablesWithIntrinsicBounds(getItem(pos).iconRes, 0, 0, 0);
            return v;
        }

    }

    static class Holder {
        String className;
        int title;
        int iconRes;

        Holder(String className, int title, int iconRes) {
            this.className = className;
            this.title = title;
            this.iconRes = iconRes;
        }

        Bundle getArguments() {
            Bundle b = new Bundle();
            b.putInt("title", title);
            b.putInt("icon", iconRes);
            return b;
        }
    }
}
