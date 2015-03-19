#!/bin/bash
# Archive APK and proguard mapping files.

build=$1
echo cp project/project.apk out/om_im_android_$build.apk
echo cp project/proguard_logs/mapping.txt out/om_im_android_${build}_mapping.txt
cp project/project.apk out/om_im_android_$build.apk
cp project/proguard_logs/mapping.txt out/om_im_android_${build}_mapping.txt
