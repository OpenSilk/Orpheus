This library provides and interface for upnp ContentDirectories

The UpnpCDProvider is the entry point. When asked for folder/files/etc
it will bind to the UpnpServiceService and acquire the ControlPoint
We then make blocking calls to the remote ContentDirectoryService and parse
the objects passed back into their respective model objects.

TODOS
    Upnp is a tough thing to interact with considering its
    volatile nature, a simple network connection check is not enough
    Orpheus needs a mechanism to handle libraries that may only
    exist in very specific scenarios such as only on a particular lan
    network.