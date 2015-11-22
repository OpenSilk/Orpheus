## Orpheus: offspring of Apollo, music player from hell

There's kinda a lot going on here. The main app is in app3, with libraries in core-*, common-*, library-*,
plugin-*, there are some other 'dead' modules here as well that will be removed someday.

The primary focus for Orpheus is to build a nice core app that can be extended through
plugins. External libraries are supported. Pluggable renderers are supported but must be bundled
with the app, external renderers are still in the works.

### Building

OpenSilk projects are managed by repo

    mkdir OpenSilk
    repo init -u https://github.com/OpenSilk/repo-manifest
    repo sync
    cd Orpheus
    ./gradlew app3:assembleDebug

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
