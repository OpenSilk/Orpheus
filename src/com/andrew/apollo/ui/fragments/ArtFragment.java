package com.andrew.apollo.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andrew.apollo.R;

/**
 * Created by drew on 2/9/14.
 */
public class ArtFragment extends Fragment {

    // Album art
    private ImageView mAlbumArt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_albumart, container, false);
        mAlbumArt = (ImageView) v.findViewById(R.id.audio_player_album_art);
        return v;
    }

    public ImageView getArtImage() {
        return mAlbumArt;
    }

}
