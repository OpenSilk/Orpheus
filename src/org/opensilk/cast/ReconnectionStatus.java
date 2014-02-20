package org.opensilk.cast;

/**
 * Enumerates various stages during a session recovery
 */
public class ReconnectionStatus {
    public static final int STARTED = 1;
    public static final int IN_PROGRESS = 2;
    public static final int FINALIZE = 4;
    public static final int INACTIVE = 8;
}
