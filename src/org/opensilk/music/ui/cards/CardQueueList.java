package org.opensilk.music.ui.cards;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.activities.BaseSlidingActivity;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/13/14.
 */
public class CardQueueList extends CardBaseList<Song> {
    public CardQueueList(Context context, Song data) {
        this(context, data, R.layout.card_list_thumb_inner_layout);
    }

    public CardQueueList(Context context, Song data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        mTitle = mData.mSongName;
        mSubTitle = mData.mArtistName;
        mExtraText = MusicUtils.makeTimeString(getContext(),mData.mDuration);
    }

    @Override
    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        fetcher.loadAlbumImage(mData.mArtistName, mData.mAlbumName, mData.mAlbumId, view);
    }

    @Override
    protected int getHeaderMenuId() {
        return R.menu.card_queue;
    }

    @DebugLog
    @Override
    public boolean shouldStartAnimating(long trackId) {
        return trackId == mData.mSongId;
    }

    @Override
    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        // When selecting a track from the queue, just jump there instead of
                        // reloading the queue. This is both faster, and prevents accidentally
                        // dropping out of party shuffle.
                        MusicUtils.setQueuePosition(Integer.valueOf(getId()));
                        break;
                    case R.id.card_menu_play_next:
                        NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                                .makeQueueCursor(getContext());
                        queue.removeItem(Integer.valueOf(getId()));
                        queue.close();
                        MusicUtils.playNext(new long[] {
                                mData.mSongId
                        });
                        ((BaseSlidingActivity) getContext()).refreshQueue();
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(new long[]{
                                mData.mSongId
                        }).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_remove_queue:
                        MusicUtils.removeTrack(mData.mSongId);
                        ((BaseSlidingActivity) getContext()).refreshQueue();
                        break;
                    case R.id.card_menu_more_by:
                        NavUtils.openArtistProfile(getContext(), mData.mArtistName);
                        break;
                    case R.id.card_menu_set_ringtone:
                        MusicUtils.setRingtone(getContext(), mData.mSongId);
                        break;
                    case R.id.card_menu_delete:
                        final String song = mData.mSongName;
                        DeleteDialog.newInstance(song, new long[]{
                                mData.mSongId
                        }, null).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
            }
        };
    }

}
