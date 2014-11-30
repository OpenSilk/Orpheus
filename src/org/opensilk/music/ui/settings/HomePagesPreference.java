package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;
import com.andrew.apollo.utils.Lists;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.ui2.gallery.GalleryPage;
import org.opensilk.music.ui2.gallery.GalleryView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by andrew on 4/13/14.
 */
public class HomePagesPreference extends DialogPreference implements
        DragSortListView.DropListener, DragSortListView.RemoveListener {

    @dagger.Module(addsTo = SettingsActivity.Module.class, injects = HomePagesPreference.class)
    public static class Module {

    }

    @Inject AppPreferences mSettings;

    private DragSortSwipeListAdapter mAdapter;
    private ArrayList<GalleryPage> mCurrentClassList;

    public HomePagesPreference(Context context) {
        this(context, null);
    }

    public HomePagesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.settings_homepages_preference);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        ((DaggerInjector) getContext()).getObjectGraph().plus(new Module()).inject(this);
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
                    mSettings.saveGalleryPages(mCurrentClassList);
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

        List<GalleryPage> savedPages = mSettings.getGalleryPages();
        if (savedPages == null) {
            savedPages = Arrays.asList(GalleryPage.values());
        }
        mCurrentClassList = new ArrayList<>(savedPages);

        DragSortListView listView = (DragSortListView) view.findViewById(android.R.id.list);
        mAdapter = new DragSortSwipeListAdapter(getContext(), mCurrentClassList);
        listView.setAdapter(mAdapter);
        listView.setDropListener(this);
        listView.setRemoveListener(this);

        final Button addButton = (Button) view.findViewById(R.id.add_item);
        final int icon = ThemeUtils.isLightTheme(getContext()) ? R.drawable.ic_action_add_light : R.drawable.ic_action_add_dark;
        addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                final List<GalleryPage> pages = Lists.newArrayList();
                for (GalleryPage item : GalleryPage.values()) {
                    if (!mCurrentClassList.contains(item)) {
                        pages.add(item);
                        popupMenu.getMenu().add(Menu.NONE, pages.size()-1, Menu.NONE,
                                GalleryView.getGalleryPageTitleResource(item.screen));
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

    public void add(GalleryPage item) {
        mAdapter.add(item);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        GalleryPage item = mAdapter.getItem(from);
        mAdapter.remove(item);
        mAdapter.insert(item, to);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void remove(int which) {
        GalleryPage item = mAdapter.getItem(which);
        mAdapter.remove(item);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * List adapter
     */
    public static class DragSortSwipeListAdapter extends ArrayAdapter<GalleryPage> {

        private LayoutInflater mInflater;

        public DragSortSwipeListAdapter(Context context, List<GalleryPage> objects) {
            super(context, -1, objects);
            mInflater = LayoutInflater.from(getContext());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                row = mInflater.inflate(R.layout.settings_homepages_preference_item, parent, false);
                ImageView handle = (ImageView) row.findViewById(R.id.item_handle);
                TextView text = (TextView) row.findViewById(R.id.item_text);
                row.setTag(new ViewHolder(handle, text));
            }

            ViewHolder holder = (ViewHolder) row.getTag();
            if (holder != null) {
                holder.handle.setImageResource(ThemeUtils.isLightTheme(getContext())
                        ? R.drawable.ic_action_drag_light : R.drawable.ic_action_drag_dark);

                holder.text.setText(getContext().getString(
                        GalleryView.getGalleryPageTitleResource(getItem(position).screen)));
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
