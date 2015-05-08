
package org.opensilk.common.ui.mortar;

/**
 * <p>Implemented by objects that need to know when the {@link android.app.Activity} pauses
 * and resumes. Sign up for service via {@link PauseAndResumeRegistrar#register(PausesAndResumes)}.
 *
 * <p>Registered objects will also be subscribed to the {@link com.squareup.otto.OttoBus}
 * only while the activity is running.
 */
public interface PausesAndResumes {
    void onResume();

    void onPause();
}
