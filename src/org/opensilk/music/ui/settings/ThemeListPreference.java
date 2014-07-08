package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;
import com.andrew.apollo.utils.ThemeStyle;

/**
 * Created by andrew on 3/1/14.
 */
public class ThemeListPreference extends ListPreference {

    public ThemeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        ListAdapter listAdapter = new ThemeListAdapter(getContext(), zip(getEntries(), getEntryValues()));
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    /**
     * Zips up entry values and entry names
     * @param entries
     * @param values
     * @return
     */
    public static EntryHolder[] zip(CharSequence[] entries, CharSequence[] values) {
        if (entries.length != values.length) {
            return null;
        }
        EntryHolder[] em = new EntryHolder[entries.length];
        for (int ii=0; ii<entries.length; ii++) {
            em[ii] = new EntryHolder(entries[ii], values[ii]);
        }
        return em;
    }

    /**
     * Class to pair entry values with entry display names
     */
    public static class EntryHolder {
        public CharSequence entry;
        public CharSequence value;
        public EntryHolder(CharSequence entry, CharSequence value) {
            this.entry = entry;
            this.value = value;
        }
    }

    /**
     * List adapter
     */
    public static class ThemeListAdapter extends ArrayAdapter<EntryHolder> {

        private ThemeHelper mThemeHelper;
        private String mCurrentTheme;
        private LayoutInflater mInflater;

        public ThemeListAdapter(Context context, EntryHolder[] objects) {
            super(context, -1, objects);
            mThemeHelper = ThemeHelper.getInstance(getContext());
            mCurrentTheme = mThemeHelper.getThemeName().replace("DARK", "");
            mInflater = LayoutInflater.from(getContext());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(R.layout.settings_theme_list_row, parent, false);
                ImageView icon = (ImageView) row.findViewById(R.id.theme_preview_icon);
                TextView name = (TextView) row.findViewById(R.id.theme_name);
                ImageView check = (ImageView) row.findViewById(R.id.theme_check_icon);
                row.setTag(new ViewHolder(icon, name, check));
            }

            ViewHolder holder = (ViewHolder) row.getTag();
            if (holder != null) {
                int themeColor = mThemeHelper.getThemePrimaryColor(ThemeStyle.valueOf(getItem(position).value.toString()));
                holder.icon.setImageDrawable(new ColorDrawable(themeColor));

                holder.name.setText(getItem(position).entry);

                holder.check.setImageResource(ThemeHelper.isLightTheme(getContext())
                        ? R.drawable.ic_action_tick_black : R.drawable.ic_action_tick_white);
                holder.check.setVisibility(mCurrentTheme.equals(getItem(position).value) ? View.VISIBLE : View.INVISIBLE);
            }
            return row;
        }

        private static class ViewHolder {
            private ImageView icon;
            private TextView name;
            private ImageView check;
            private ViewHolder(ImageView icon, TextView name, ImageView check) {
                this.icon = icon;
                this.name = name;
                this.check = check;
            }
        }

    }

}
