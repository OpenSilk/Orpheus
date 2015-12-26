/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui3.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.index.model.BioContent;
import org.opensilk.music.index.model.SimilarArtist;
import org.opensilk.music.model.Model;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/18/15.
 */
public class BioScreenViewAdapter extends RecyclerListAdapter<Object, BioScreenViewAdapter.ViewHolder>
        implements ItemClickSupport.OnItemClickListener {

    final ArtworkRequestManager requestor;

    public BioScreenViewAdapter(
            ArtworkRequestManager requestor
    ) {
        this.requestor = requestor;
    }

    public void onModels(List<Model> models) {
        if (models.isEmpty()) {
            clear();
        } else {
            replaceAll(models);
            if (getItem(0) instanceof BioContent) {
                addItem(0, Header.CONTENT);
            }
            for (int ii=0; ii<getItemCount(); ii++) {
                if (getItem(ii) instanceof SimilarArtist) {
                    addItem(ii, Header.SIMILAR);
                    break;
                }
            }
        }
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        String url = null;
        Object m = getItem(position);
        if (m instanceof BioContent) {
            url = ((BioContent)m).getUrl();
        } else if (m instanceof SimilarArtist) {
            url = ((SimilarArtist)m).getUrl();
        }
        if (!StringUtils.isEmpty(url)) {
            Intent i = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(url))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    ;
            try {
                recyclerView.getContext().startActivity(i);
            } catch (ActivityNotFoundException e) {
                //pass
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(this);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.removeFrom(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.screen_bio_item_summary:
                return new BioContentVH(inflate(parent, viewType));
            case R.layout.screen_bio_item_artist:
                return new SimmilarArtistVH(inflate(parent, viewType));
            case R.layout.screen_bio_header:
                return new HeaderVH(inflate(parent, viewType));
        }
        throw new IllegalArgumentException("unknown layout id " + viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Object m = getItem(position);
        switch (holder.getKind()) {
            case CONTENT:
                ((BioContentVH) holder).bind((BioContent) m);
                break;
            case SIMILARARTIST:
                ((SimmilarArtistVH) holder).bind((SimilarArtist)m, requestor);
                break;
            case HEADER:
                ((HeaderVH) holder).bind((Header)m);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object m = getItem(position);
        if (m instanceof BioContent) {
            return R.layout.screen_bio_item_summary;
        } else if (m instanceof SimilarArtist) {
            return R.layout.screen_bio_item_artist;
        } else if (m instanceof Header) {
            return R.layout.screen_bio_header;
        }
        throw new IllegalArgumentException("Unknown model " + m.getClass());
    }

    static class Header {
        static final Header CONTENT = new Header(R.string.bio_biography_title);
        static final Header SIMILAR = new Header(R.string.bio_similar_artists_title);
        final int res;
        public Header(int res) {
            this.res = res;
        }
    }

    private enum Kind {
        CONTENT,
        SIMILARARTIST,
        HEADER,
    }

    public abstract static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        abstract Kind getKind();
    }

    public static class BioContentVH extends ViewHolder {
        @InjectView(R.id.bio_content) TextView content;

        public BioContentVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        public void bind(BioContent bio) {
            this.content.setText(Html.fromHtml(bio.getContent()));
        }

        @Override
        Kind getKind() {
            return Kind.CONTENT;
        }
    }

    public static class SimmilarArtistVH extends ViewHolder {
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.artist_name) TextView title;

        public SimmilarArtistVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        public void bind(SimilarArtist artist, ArtworkRequestManager requestor) {
            this.title.setText(artist.getName());
            if (!StringUtils.isEmpty(artist.getImageUrl())) {
                requestor.newRequest(Uri.parse(artist.getImageUrl()),
                        this.artwork, null);
            } else {
                Resources resources = itemView.getResources();
                LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, artist.getName());
                drawable.setIsCircular(false);
                this.artwork.setImageDrawable(drawable);
            }
        }

        @Override
        Kind getKind() {
            return Kind.SIMILARARTIST;
        }
    }

    public static class HeaderVH extends ViewHolder {
        public HeaderVH(View itemView) {
            super(itemView);
        }

        void bind(Header header) {
            ((TextView) itemView).setText(header.res);
        }

        @Override
        Kind getKind() {
            return Kind.HEADER;
        }
    }
}
