## Orpheus, the offspring of Apollo

Orpheus is in the middle of a ground up rewrite for 3.0. If your stumbling upon here you will likely
want to checkout the latest tag. Though there is still a buildable version 2 in the `app` module.
With dependencies in `api`, `common`, and `iab*` module. They are there for reference and will be removed
once all applicable code has been copied.
Orpheus 3 is in `app3` with dependencies in the `core-*`, `common-*`, `library-*`, and `plugin-*` modules.
Its also noteworthy that app3 will only work on L+ until the support lib is fixed or I make a custom
MediaSessionCompat

### Building

OpenSilk projects are managed by repo

    mkdir OpenSilk
    repo init -u https://github.com/OpenSilk/Manifest
    repo sync
    cd Orpheus
    ./gradlew assembleDebug

### Contributing

OpenSilk projects are managed with gerrit at `review.opensilk.org`

    cd Orpheus
    repo start my-branch .
    #make changes
    git commit -a -m 'stuffs'
    repo upload .

### License

GPLv3 `http://www.gnu.org/licenses/gpl.txt`

Portions under Apache2 where specified
