
package org.opensilk.music.ui2.core.lifecycle;

import org.opensilk.music.ui2.core.HasScope;

/**
 * Implemented by {@link android.app.Activity} instances whose pause / resume state
 * is to be shared. The activity must call {@link PauseAndResumePresenter#activityPaused()}
 * and {@link PauseAndResumePresenter#activityResumed()} at the obvious times.
 */
public interface PauseAndResumeActivity extends HasScope {
    boolean isRunning();
}
