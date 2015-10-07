package org.opensilk.common.glide;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.transition.NoTransition;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.request.transition.ViewAnimationFactory;

/**
 * A factory class that produces a new {@link Transition} that varies depending on whether or not
 * the drawable was loaded from the memory cache and whether or not the drawable is the first image
 * to be put on the target.
 *
 * <p> Resources are usually loaded from the memory cache just before the user can see the view, for
 * example when the user changes screens or scrolls back and forth in a list. In those cases the
 * user typically does not expect to see a transition. As a result, when the resource is loaded from
 * the memory cache this factory produces an {@link NoTransition}. </p>
 */
public class PalettizedBitmapTransitionFactory implements TransitionFactory<PalettizedBitmap> {
    private static final int DEFAULT_DURATION_MS = 300;
    private final ViewAnimationFactory<PalettizedBitmap> viewAnimationFactory;
    private final int duration;
    private PalettizedBitmapTransition firstResourceTransition;
    private PalettizedBitmapTransition secondResourceTransition;

    public PalettizedBitmapTransitionFactory() {
        this(DEFAULT_DURATION_MS);
    }

    public PalettizedBitmapTransitionFactory(int duration) {
        this(new ViewAnimationFactory<PalettizedBitmap>(makeDefaultAnimation(duration)), duration);
    }

    public PalettizedBitmapTransitionFactory(int defaultAnimationId, int duration) {
        this(new ViewAnimationFactory<PalettizedBitmap>(defaultAnimationId), duration);
    }

    public PalettizedBitmapTransitionFactory(Animation defaultAnimation, int duration) {
        this(new ViewAnimationFactory<PalettizedBitmap>(defaultAnimation), duration);
    }

    PalettizedBitmapTransitionFactory(ViewAnimationFactory<PalettizedBitmap> viewAnimationFactory, int duration) {
        this.viewAnimationFactory = viewAnimationFactory;
        this.duration = duration;
    }

    @Override
    public Transition<PalettizedBitmap> build(DataSource dataSource, boolean isFirstResource) {
        if (dataSource == DataSource.MEMORY_CACHE) {
            return NoTransition.get();
        } else if (isFirstResource) {
            return getFirstResourceTransition(dataSource);
        } else {
            return getSecondResourceTransition(dataSource);
        }
    }

    private Transition<PalettizedBitmap> getFirstResourceTransition(DataSource dataSource) {
        if (firstResourceTransition == null) {
            Transition<PalettizedBitmap> defaultAnimation =
                    viewAnimationFactory.build(dataSource, true /*isFirstResource*/);
            firstResourceTransition = new PalettizedBitmapTransition(defaultAnimation, duration);
        }
        return firstResourceTransition;
    }

    private Transition<PalettizedBitmap> getSecondResourceTransition(DataSource dataSource) {
        if (secondResourceTransition == null) {
            Transition<PalettizedBitmap> defaultAnimation =
                    viewAnimationFactory.build(dataSource, false /*isFirstResource*/);
            secondResourceTransition = new PalettizedBitmapTransition(defaultAnimation, duration);
        }
        return secondResourceTransition;
    }

    private static Animation makeDefaultAnimation(int duration) {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(duration);
        return animation;
    }

}
