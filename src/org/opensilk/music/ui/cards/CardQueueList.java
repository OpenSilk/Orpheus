package org.opensilk.music.ui.cards;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.widgets.PlayingIndicator;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 2/13/14.
 */
public class CardQueueList extends CardBaseListNoHeader<LocalSong> {

    protected PlayingIndicator mPlayingIndicatior;

    public CardQueueList(Context context, LocalSong data) {
        this(context, data, R.layout.card_list_inner_layout_queue);
    }

    public CardQueueList(Context context, LocalSong data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        mTitle = mData.name;
        mSubTitle = mData.artistName;
        mExtraText = MusicUtils.makeTimeString(getContext(),mData.duration);
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                // When selecting a track from the queue, just jump there instead of
                // reloading the queue. This is both faster, and prevents accidentally
                // dropping out of party shuffle.
                MusicUtils.setQueuePosition(Integer.valueOf(getId()));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        mPlayingIndicatior = (PlayingIndicator) view.findViewById(R.id.playing_indicator_record);
        maybeStartPlayingIndicator();
    }

    @Override
    protected void loadThumbnail(ArtworkImageView view) {
        ArtworkManager.loadAlbumImage(mData.artistName, mData.albumName, null, view);
    }

    @Override
    public int getOverflowMenuId() {
        return R.menu.card_queue;
    }

    @Override
    public PopupMenu.OnMenuItemClickListener getOverflowPopupMenuListener() {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.card_menu_play_next:
//                        NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
//                                .makeQueueCursor(getContext());
//                        queue.removeItem(Integer.valueOf(getId()));
//                        queue.close();
//                        MusicUtils.playNextOLD(new long[] {
//                                mData.mSongId
//                        });
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(new long[]{
                                mData.songId
                        }).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_remove_queue:
//                        MusicUtils.removeTrackOLD(mData.mSongId);
                        break;
                    case R.id.card_menu_more_by:
                        NavUtils.openArtistProfile(getContext(), MusicUtils.makeArtist(getContext(), mData.artistName));
                        break;
                    case R.id.card_menu_set_ringtone:
                        MusicUtils.setRingtone(getContext(), mData.songId);
                        break;
                    case R.id.card_menu_delete:
                        final String song = mData.name;
                        DeleteDialog.newInstance(song, new long[]{
                                mData.songId
                        }, null).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
                return false;
            }
        };
    }

    /**
     * Conditionally starts playing indicator animation
     */
    protected void maybeStartPlayingIndicator() {
        if (mPlayingIndicatior != null) {
            // Always set gone. else recycled views might end up with it showing
            if (mPlayingIndicatior.isAnimating()) {
                mPlayingIndicatior.stopAnimating(); //stopAnimating sets GONE
            } else {
                mPlayingIndicatior.setVisibility(View.GONE);
            }
//            if (mData.mSongId == MusicUtils.getCurrentAudioId()) {
//                if (MusicUtils.isPlaying()) {
//                    mPlayingIndicatior.startAnimating();
//                } else {
//                    mPlayingIndicatior.setVisibility(View.VISIBLE);
//                }
//            }
        }
    }

}
