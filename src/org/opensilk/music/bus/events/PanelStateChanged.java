package org.opensilk.music.bus.events;

/**
 * Created by drew on 4/25/14.
 */
public class PanelStateChanged {

    public enum Action {
        USER_EXPAND,
        USER_COLLAPSE,
        SYSTEM_EXPAND,
        SYSTEM_COLLAPSE
    }

    private Action action;

    public PanelStateChanged(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}
