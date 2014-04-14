package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.ui.home.HomeAlbumFragment;
import org.opensilk.music.ui.home.HomeArtistFragment;
import org.opensilk.music.ui.home.HomeGenreFragment;
import org.opensilk.music.ui.home.HomePlaylistFragment;
import org.opensilk.music.ui.home.HomeRecentFragment;
import org.opensilk.music.ui.home.HomeSongFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by andrew on 4/13/14.
 */
public class DragSortSwipeListPreference extends DialogPreference implements
        DragSortListView.DropListener, DragSortListView.RemoveListener {

    private static final String[] DEFAULT_LIST = {
            HomePlaylistFragment.class.getName(), HomeRecentFragment.class.getName(),
            HomeArtistFragment.class.getName(), HomeAlbumFragment.class.getName(),
            HomeSongFragment.class.getName(), HomeGenreFragment.class.getName()};

    public static final String DELIMITER = "|";

    private DragSortSwipeListAdapter mAdapter;
    private List<String> mCurrentClassList;

    public DragSortSwipeListPreference(Context context) {
        this(context, null);
    }

    public DragSortSwipeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.drag_sort_swipe_list_preference);
        setPersistent(true);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String items = listToString(mAdapter.getItems());
                if (items.length() == 0) {
                    //Error dialog here
                } else {
                    // We're only using the OnPreferenceChangeListener to restart.
                    getSharedPreferences().edit().putString(getKey(), items).apply();
                    callChangeListener(items);
                }
                dialog.dismiss();
            }
        });
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        String strItems = getSharedPreferences().getString(getKey(), "");

        if (strItems.isEmpty()) {
            mCurrentClassList = Arrays.asList(DEFAULT_LIST);
        } else {
            mCurrentClassList = listFromString(strItems);
        }

        DragSortListView listView = (DragSortListView) view.findViewById(android.R.id.list);
        mAdapter = new DragSortSwipeListAdapter(getContext(),
                R.layout.drag_sort_swipe_list_item,
                mCurrentClassList.toArray(new String[mCurrentClassList.size()]));
        listView.setAdapter(mAdapter);
        listView.setDropListener(this);
        listView.setRemoveListener(this);

        final ImageButton addButton = (ImageButton) view.findViewById(R.id.add_item);
        addButton.setImageResource(ThemeHelper.isLightTheme(getContext()) ?
                R.drawable.ic_action_add_light : R.drawable.ic_action_add_dark);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getContext(), addButton);
                for (String item : DEFAULT_LIST) {
                    if (!mAdapter.contains(item)) {
                        popupMenu.getMenu().add(getHumanReadable(item));
                    }
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        add(getClassName(item.getTitle().toString()));
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

    public void add(String item) {
        mAdapter.add(item);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        String item = mAdapter.getItem(from);
        mAdapter.remove(item);
        mAdapter.insert(item, to);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void remove(int which) {
        String item = mAdapter.getItem(which);
        mAdapter.remove(item);
        mAdapter.notifyDataSetChanged();
    }

    public class DragSortSwipeListAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private Context mContext;
        private int mLayoutRes;

        public DragSortSwipeListAdapter(Context context, int resourceId, String[] objects) {
            super(context, resourceId, new ArrayList<>(Arrays.asList(objects)));
            mContext = context;
            mLayoutRes = resourceId;
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ImageView handle;
            TextView text;

            if (row == null) {
                row = mInflater.inflate(mLayoutRes, parent, false);
                row.setTag(R.id.item_handle, row.findViewById(R.id.item_handle));
                row.setTag(R.id.item_text, row.findViewById(R.id.item_text));
            }

            handle = (ImageView) row.getTag(R.id.item_handle);
            text = (TextView) row.getTag(R.id.item_text);

            handle.setImageResource(ThemeHelper.isLightTheme(mContext)
                    ? R.drawable.ic_action_drag_light : R.drawable.ic_action_drag_dark);
            text.setText(getHumanReadable(getItem(position)));
            Log.d("DragSortList", "Item: " + getHumanReadable(getItem(position)));
            return row;
        }

        public List<String> getItems() {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < getCount() ; i++) {
                items.add(getItem(i));
            }
            return items;
        }

        public boolean contains(String compare) {
            for (String item : getItems() ) {
                if (item.equals(compare)) return true;
            }
            return false;
        }

    }

    public static List<String> listFromString(String items) {
        if (items.equals("")) {
            return null;
        }
        return new ArrayList<>(Arrays.asList(items.split("\\|")));
    }

    public static String listToString(List<String> items) {
        if(items == null || items.size() <= 0) {
            return "";
        } else {
            String s = items.get(0);
            for(int i = 1; i < items.size(); i++) {
                s += DELIMITER + items.get(i);
            }
            return s;
        }
    }

    private String getHumanReadable(String className) {
        Log.d("DragSortList", "className: " + className);
        int id = -1;
        if (className.equals(HomePlaylistFragment.class.getName())) {
            id = R.string.page_playlists;
        } else if (className.equals(HomeRecentFragment.class.getName())) {
            id = R.string.page_recent;
        } else if (className.equals(HomeArtistFragment.class.getName())) {
            id = R.string.page_artists;
        } else if (className.equals(HomeAlbumFragment.class.getName())) {
            id = R.string.page_albums;
        } else if (className.equals(HomeSongFragment.class.getName())) {
            id = R.string.page_songs;
        } else if (className.equals(HomeGenreFragment.class.getName())) {
            id = R.string.page_genres;
        } else {
            id = R.string.error;
        }

        return getContext().getResources().getString(id);
    }

    public static String getClassName(String title) {
        Log.d("DragSortList", "Title: " + title);
        if (title.equals("Playlists")) {
            return HomePlaylistFragment.class.getName();
        } else if (title.equals("Recent")) {
            return HomeRecentFragment.class.getName();
        } else if (title.equals("Artists")) {
            return HomeArtistFragment.class.getName();
        } else if (title.equals("Albums")) {
            return HomeAlbumFragment.class.getName();
        } else if (title.equals("Songs")) {
            return HomeSongFragment.class.getName();
        } else if (title.equals("Genres")) {
            return HomeGenreFragment.class.getName();
        }
        return "WTF HAPPENED HERE?!?!?!";
    }

}
