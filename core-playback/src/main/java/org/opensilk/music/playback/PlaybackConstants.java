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

package org.opensilk.music.playback;

/**
 * Created by drew on 5/6/15.
 */
public interface PlaybackConstants {
    String TAG = "MusicPlaybackService";
    boolean D = BuildConfig.DEBUG;

    String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;

    /**
     * Indicates that the music has paused or resumed
     */
    String PLAYSTATE_CHANGED = APOLLO_PACKAGE_NAME+".playstatechanged";

    /**
     * Indicates that music playback position within
     * a title was changed
     */
    String POSITION_CHANGED = APOLLO_PACKAGE_NAME+".positionchanged";

    /**
     * Indicates the meta data has changed in some way, like a track change
     */
    String META_CHANGED = APOLLO_PACKAGE_NAME+".metachanged";

    /**
     * Indicates the queue has been updated
     */
    String QUEUE_CHANGED = APOLLO_PACKAGE_NAME+".queuechanged";

    /**
     * Indicates the repeat mode chaned
     */
    String REPEATMODE_CHANGED = APOLLO_PACKAGE_NAME+".repeatmodechanged";

    /**
     * Indicates the shuffle mode chaned
     */
    String SHUFFLEMODE_CHANGED = APOLLO_PACKAGE_NAME+".shufflemodechanged";

    /**
     * For backwards compatibility reasons, also provide sticky
     * broadcasts under the music package
     */
    String MUSIC_PACKAGE_NAME = "com.android.music";

    /**
     * Called to indicate a general service commmand. Used in
     * {@link MediaButtonIntentReceiver}
     */
    String SERVICECMD = APOLLO_PACKAGE_NAME+".musicservicecommand";

    String EXTERNAL_SERVICECMD = MUSIC_PACKAGE_NAME+".musicservicecommand";

    /**
     * Called to go toggle between pausing and playing the music
     */
    String TOGGLEPAUSE_ACTION = APOLLO_PACKAGE_NAME+".togglepause";

    /**
     * Called to go to pause the playback
     */
    String PAUSE_ACTION = APOLLO_PACKAGE_NAME+".pause";

    /**
     * Called to go to stop the playback
     */
    String STOP_ACTION = APOLLO_PACKAGE_NAME+".stop";

    /**
     * Called to go to the previous track
     */
    String PREVIOUS_ACTION = APOLLO_PACKAGE_NAME+".previous";

    /**
     * Called to go to the next track
     */
    String NEXT_ACTION = APOLLO_PACKAGE_NAME+".next";

    /**
     * Called to change the repeat mode
     */
    String REPEAT_ACTION = APOLLO_PACKAGE_NAME+".repeat";

    /**
     * Called to change the shuffle mode
     */
    String SHUFFLE_ACTION = APOLLO_PACKAGE_NAME+".shuffle";

    /**
     * Called to update the service about the foreground state of Apollo's activities
     */
    String FOREGROUND_STATE_CHANGED = APOLLO_PACKAGE_NAME+".fgstatechanged";

    String NOW_IN_FOREGROUND = "nowinforeground";

    String FROM_MEDIA_BUTTON = "frommediabutton";

    /**
     * Used to easily notify a list that it should refresh. i.e. A playlist
     * changes
     */
    String REFRESH = APOLLO_PACKAGE_NAME+".refresh";

    /**
     * Used by the alarm intent to shutdown the service after being idle
     */
    String SHUTDOWN = APOLLO_PACKAGE_NAME+".shutdown";

    /**
     * Called to update the remote control client
     */
    String UPDATE_LOCKSCREEN = APOLLO_PACKAGE_NAME+".updatelockscreen";

    String CMDNAME = "command";

    String CMDTOGGLEPAUSE = "togglepause";

    String CMDSTOP = "stop";

    String CMDPAUSE = "pause";

    String CMDPLAY = "play";

    String CMDPREVIOUS = "previous";

    String CMDNEXT = "next";

    String CMDNOTIF = "buttonId";

    int IDCOLIDX = 0;

    /**
     * Moves a list to the front of the queue
     */
    int ENQUEUE_NOW = 1;

    /**
     * Moves a list to the next position in the queue
     */
    int ENQUEUE_NEXT = 2;

    /**
     * Moves a list to the last position in the queue
     */
    int ENQUEUE_LAST = 3;

    /**
     * Shuffles no songs, turns shuffling off
     */
    int SHUFFLE_NONE = 0;

    /**
     * Shuffles all songs
     */
    int SHUFFLE_NORMAL = 1;

    /**
     * Party shuffle
     */
    int SHUFFLE_AUTO = 2;

    /**
     * Turns repeat off
     */
    int REPEAT_NONE = 0;

    /**
     * Repeats the current track in a list
     */
    int REPEAT_CURRENT = 1;

    /**
     * Repeats all the tracks in a list
     */
    int REPEAT_ALL = 2;


    /**
     * Idle time before stopping the foreground notfication (1 minute)
     */
    int IDLE_DELAY = 60000 * 6;

    /**
     * Song play time used as threshold for rewinding to the beginning of the
     * track instead of skipping to the previous track when getting the PREVIOUS
     * command
     */
    long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;

    /**
     * The max size allowed for the track history
     */
    int MAX_HISTORY_SIZE = 100;

    String BOOL_ARG = "bool_arg";
    //Has int extra
    String FOCUSCHANGE = APOLLO_PACKAGE_NAME + ".cmd.focuschange";

    interface CMD {
        String CYCLE_REPEAT = "cmd.cyclerepeat";
        String SHUFFLE_QUEUE = "cmd.shufflequeue";
        String ENQUEUE  = "cmd.enqueue";
        String REMOVE_QUEUE_ITEM = "cmd.removequeueitem";
        String REMOVE_QUEUE_ITEM_AT = "cmd.removequeueitemat";
        String CLEAR_QUEUE = "cmd.clearqueue";
        String MOVE_QUEUE_ITEM_TO = "cmd.movequeueitemto";
        String ENQUEUE_TRACKS_FROM = "cmd.enqueuetracksfrom";
        String PLAY_ALL = "cmd.playall";
        String PLAY_TRACKS_FROM = "cmd.playtracksfrom";
        String TOGGLE_PLAYBACK = "cmd.toggleplayback";
    }

    interface EVENT {
        String QUEUE_SHUFFLED = "event.queueshuffled";
    }

    interface EXTRA {
        String DURATION = "duration";
    }

    interface META {
        String TRACK_ARTWORK_URI = "meta.trackartworkuri";
    }

}
