package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.GraphHolder;
import org.opensilk.music.ui.home.MusicFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andrew on 4/13/14.
 */
public class DragSortSwipeListPreference extends DialogPreference implements
        DragSortListView.DropListener, DragSortListView.RemoveListener {

    private DragSortSwipeListAdapter mAdapter;
    private ArrayList<MusicFragment> mCurrentClassList;
    private AppPreferences mSettings;

    public DragSortSwipeListPreference(Context context) {
        this(context, null);
    }

    public DragSortSwipeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.drag_sort_swipe_list_preference);
        // TODO real inject
        mSettings = GraphHolder.get(context).getObj(AppPreferences.class);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Adapter operates on the underlying list
                if (mCurrentClassList.size() < 1) {
                    //Error dialog here
                } else {
                    mSettings.setHomePages(mCurrentClassList);
                    // We're only using the OnPreferenceChangeListener to restart.
                    callChangeListener(null);
//                    Log.d("TAG", mAdapter.getItems().toString());
//                    Log.d("TAG", mCurrentClassList.toString());
                }
                dialog.dismiss();
            }
        });
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        List<MusicFragment> savedPages = mSettings.getHomePages();
        if (savedPages == null) {
            savedPages = Arrays.asList(MusicFragment.values());
        }
        mCurrentClassList = new ArrayList<>(savedPages);

        DragSortListView listView = (DragSortListView) view.findViewById(android.R.id.list);
        mAdapter = new DragSortSwipeListAdapter(getContext(), mCurrentClassList);
        listView.setAdapter(mAdapter);
        listView.setDropListener(this);
        listView.setRemoveListener(this);

        final Button addButton = (Button) view.findViewById(R.id.add_item);
        Drawable icon = getContext().getResources().getDrawable(ThemeHelper.isLightTheme(getContext()) ?
                R.drawable.ic_action_add_light : R.drawable.ic_action_add_dark);
        addButton.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getContext(), addButton);
                final List<MusicFragment> pages = Lists.newArrayList();
                for (MusicFragment item : MusicFragment.values()) {
                    if (!mCurrentClassList.contains(item)) {
                        pages.add(item);
                        popupMenu.getMenu().add(Menu.NONE, pages.size()-1, Menu.NONE, item.getTitleResource());
                    }
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        add(pages.get(item.getItemId()));
                        return false;
                    }
                });
                popupMenu.show();
            }
        });

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }

    public void add(MusicFragment item) {
        mAdapter.add(item);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        MusicFragment item = mAdapter.getItem(from);
        mAdapter.remove(item);
        mAdapter.insert(item, to);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void remove(int which) {
        MusicFragment item = mAdapter.getItem(which);
        mAdapter.remove(item);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * List adapter
     */
    public static class DragSortSwipeListAdapter extends ArrayAdapter<MusicFragment> {

        private LayoutInflater mInflater;

        public DragSortSwipeListAdapter(Context context, List<MusicFragment> objects) {
            super(context, -1, objects);
            mInflater = LayoutInflater.from(getContext());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                row = mInflater.inflate(R.layout.drag_sort_swipe_list_item, parent, false);
                ImageView handle = (ImageView) row.findViewById(R.id.item_handle);
                TextView text = (TextView) row.findViewById(R.id.item_text);
                row.setTag(new ViewHolder(handle, text));
            }

            ViewHolder holder = (ViewHolder) row.getTag();
            if (holder != null) {
                holder.handle.setImageResource(ThemeHelper.isLightTheme(getContext())
                        ? R.drawable.ic_action_drag_light : R.drawable.ic_action_drag_dark);

                holder.text.setText(getContext().getString(getItem(position).getTitleResource()));
            }
            return row;
        }

        private static class ViewHolder {
            private ImageView handle;
            private TextView text;
            private ViewHolder(ImageView handle, TextView text) {
                this.handle = handle;
                this.text = text;
            }
        }
    }

}
