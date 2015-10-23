This library provides tracks stored on the devices sdcard.

The provider of interest is the FoldersLibraryProvider. It works
by querying some hidden apis via the StorageLookup class. And provides
storages as roots ie Internal(emulated)storage, each sdcard, and even
usb drives.

Below the roots are the actual files and directories. For directories
we walk the actual file system and use the directories relative path
(relative to base path of storage volume ie /storage/sdcard1, etc) as
its id. For files (tracks) we scan the directories for files then filter out the
audio/music files by asking the mediastore what type it is and falling back
to guessing based on file extension. For files in the mediastore we use the
mediastore _id column as its id, ones that aren't in the mediastore we use
relative path (like directories). We convert files to tracks by asking the
mediastore for metadata and fallback to basic info we can get from the File object.

TODOS
    Use mediastore ids for everything, this will make uris more stable
    Add missing files and directories to the mediastore
