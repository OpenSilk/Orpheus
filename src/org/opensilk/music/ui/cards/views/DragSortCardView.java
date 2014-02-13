package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;

import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by drew on 2/13/14.
 */
public class DragSortCardView extends CardView {
    public DragSortCardView(Context context) {
        super(context);
    }

    public DragSortCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragSortCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void setupListeners() {
        // Disable this... all i know is it was fucking up the dragsortlist
        // consequence is card isnt clickable so revisit later //TODO
        //super.setupListeners();
    }
}
