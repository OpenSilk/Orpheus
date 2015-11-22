This library provides and interface for upnp ContentDirectories

The UpnpCDProvider is the entry point. When asked for folder/files/etc
it will bind to the UpnpServiceService and acquire the ControlPoint
then query the cds

TODOS
    settings to override isAvailable
    settings to only scan select known wifi networks