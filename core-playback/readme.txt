Houses the playback package

Orpheus has two hearts, playback is one of them (the other
being the index) playback cannot function without the index
and the index has no purpose without playback.

This package is divided into two processes, the primary
process is ':service' it houses the PlaybackService.
the PlaybackController is the other and is an addon to the
':ui' process.

The glue holding playback together is the MediaSession. Controlled
and managed my the PlaybackService. The PlaybackService itself
is the conductor, managing the interactions between external input
(Mediasessions TransportControls), the PlaybackQueue, and Playback
(the Playback class) itself.

PlaybackQueue:
    The queue acts on Track uris passed in from the ui process
    and manages the list of uris acting as the queue as well as
    a secondary list of QueueItems which is passed to the mediaSession
    (by the playbackService) whenever it changes. It also holds the
    shulffle and repeat states. The queue operates asynchronously via
    a set of callbacks, so when items are added or removed or we progress
    to the next song, we send that information to the queue and the queue
    will answer at some later time that it has processde the changes,
    we are then free to query for the current/next items in the list
    and send them to Playback.

Playback:
    Its operation may change after writing this as it is brand new
    at this time (pulled from the MediaBrowser sample then mangled beyond recognition).
    Simply it manages two instances of IMediaPlayer. The IMediaPlayer is injected
    via a factory when we call the load* methods. This allows uri specific
    players (ie drm enabled player) as well as remote players.
    It also operates asynchronously and is not thread safe, all callbacks and method
    access must occur on the PlaybackService's handler thread. Which means when
    MediaPlayers are created they must create a handler using Looper.myLooper()
    and post callbacks to it.

IMediaPlayer:
    Api may change, depending on needs of playback
    Currenly only one implementation the DefaultMediaPlayer which is simply
    a wrapper for Androids MediaPlayer. Further implementations to come
    including ExoPlayer and Chromecast.
    This should (and will) become an aidl interface to allow communication
    with remote players provided by plugins, however Playback will need to be made
    thread safe at that point.

TODOS:
    remove player/mediaplayer packages
    remove AudioManagerHelper and find another way to propagate audioSessionIds
    remove PlaybackPreferences its function is now provided by index
    test test test This is all brand new code and needs extensive tests written