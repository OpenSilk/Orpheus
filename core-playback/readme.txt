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
    Playback is handled by pluggable renderers see the core-playback-renderer
    module.

MediaSession:
    Playback was built around the mediasession introduced in android-L. Wrappers
    and switchers and a whole lot of misdirection was added to support Kitkat and
    prior. Sorry if this place is now a total cluster f***.

TODOS:
    tests