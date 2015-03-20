#!/bin/bash

gradle clean
gradle assembleRelease

src=./project/build/outputs/apk/project-release-unsigned.apk
dest=./project/project.apk

jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
    -keystore doc/release.keystore -storepass qwerty \
    $src onemeter

jarsigner -verify -verbose -certs $src

if [ -f $dest ];
then
    rm -f $dest
fi
$ANDROID_HOME/build-tools/21.1.2/zipalign -v 4 $src $dest

echo output to $dest
