Base package for all libraries

A library is a ContentProvider with
the LibraryProvider#ACTION_FILTER declared to mark it as
an Orpheus library. All libraries must extend from LibraryProvider
and override at a minimum listObjs(), getObj(), and listRoots()...

There is much more to be said here. Will update once api is more finalized
email me if you have questions
