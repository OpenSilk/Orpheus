## Orpheus, the offspring of Apollo

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
