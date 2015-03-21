#!/bin/bash

omenv='dev01'
[ $# -gt 0 ] && omenv=$1

buildVer=`date +%m%d`
[ $omenv != 'product' ] && buildVer="$buildVer($omenv)"
perl -pi -e \
    "s/android:versionName=\"([0-9]+)\.([0-9]+)\.([0-9]+)\..*\"/android:versionName=\"\1.\2.\3.$buildVer\"/" \
    project/AndroidManifest.xml

export OM_ENV=$omenv 
echo OM_ENV: $OM_ENV
echo buildVer: $buildVer

src=./project/build/outputs/apk/project-release.apk
dest=./out/om_im_android_${omenv}_`date +%m%d`.apk

gradle clean \
    && gradle assembleRelease \
    && cp $src $dest \
    && echo output to $dest
