#!/bin/bash

gradle clean
gradle assembleRelease

src=./project/build/outputs/apk/project-release.apk
dest=./project/project.apk

cp $src $dest
echo output to $dest
