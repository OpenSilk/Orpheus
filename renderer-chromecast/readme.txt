a playback renderer for google/chrome cast

GMS services are required.

Local files are served via a jetty server

Remote files that require auth (really any track resource with headers) are proxied
through the jetty server

Artwork is served via the jetty server. Artwork is pulled from the artwork provider
via the PlaybackServiceAccessor

TODOS:
    loadNext will simply enqueue the track without verifying it is playable
    then when we try to play unsupported media the renderer goes into error state
    we need to be able to detect this.
