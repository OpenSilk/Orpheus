Playback renderer api

see renderer-chromecast for example as well as
LocalRenderer in core-playback

currenly renderers must run in the same process
as the playbackservice, this means they must be
bundled with orpheus. external renderers are on
the wishlist but im not sure if android will like
having a mediasession control playback in another process.
Another idea is to package renderers as jars and
load them with the playback service. I've already
tried using the 'sharedId' thing but that is not
a viable solution as orpheus is already on the playstore
and cant have its id changed and afaik only two
apps are allowed to share the same id. So i think
the jar thing is the best bet.

TODO look into how fdroid loads the jars it downloads from repositories