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

import java.util.Locale;

/**
 * Created by andrew on 3/1/14.
 */
public class ThemeListPreference extends ListPreference {

    public ThemeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        int index = 1;

        ListAdapter listAdapter = new ThemeListAdapter(getContext(), -1, getEntries());

        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    public class ThemeListAdapter extends ArrayAdapter<CharSequence> {

        private LayoutInflater mInflater;
        private Context mContext;
        private CharSequence[] mThemes;

        public ThemeListAdapter(Context context, int resourceId, CharSequence[] objects) {
            super(context, resourceId, objects);
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
            mThemes = objects;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ImageView icon;
            ImageView check;
            TextView name;


            if (row == null) {
                row = mInflater.inflate(R.layout.settings_theme_list_row, parent, false);
                row.setTag(R.id.theme_preview_icon, row.findViewById(R.id.theme_preview_icon));
                row.setTag(R.id.theme_check_icon, row.findViewById(R.id.theme_check_icon));
                row.setTag(R.id.theme_name, row.findViewById(R.id.theme_name));
            }

            icon = (ImageView) row.getTag(R.id.theme_preview_icon);
            check = (ImageView) row.getTag(R.id.theme_check_icon);
            name = (TextView) row.getTag(R.id.theme_name);

            name.setText(mThemes[position]);
            icon.setImageDrawable(new ColorDrawable(ThemeHelper.getInstance(getContext())
                    .getThemeColor(ThemeStyle.valueOf(mThemes[position].toString().replaceAll(" ","")
                            .toUpperCase(Locale.US)))));

            String currentTheme = ThemeHelper.getInstance(mContext).getThemeName();
            if (currentTheme.equalsIgnoreCase(mThemes[position].toString().replaceAll(" ",""))) {
                check.setVisibility(View.VISIBLE);
            } else {
                check.setVisibility(View.INVISIBLE);
            }

            return row;
        }

    }

}
