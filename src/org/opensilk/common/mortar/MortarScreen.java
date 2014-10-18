
package org.opensilk.common.mortar;

import mortar.Blueprint;
import mortar.MortarScope;

/**
 * To be implemented by every screen in a screen-style mortar app. The use of this interface allows
 * the {@link com.example.mortar.mortarscreen} package to have no direct ties to Flow.
 */
public interface MortarScreen {
    /**
     * The name to use for this screen's {@link MortarScope}. (This name will be returned
     * by the generated {@link Blueprint#getMortarScopeName()} method.)
     */
    String getName();
}
