#!/bin/bash

omenv='dev01'
[ $# == 1 ] && omenv=$1

export OM_ENV=$omenv

echo OM_ENV: $OM_ENV 

src=./project/build/outputs/apk/project-release.apk
dest=./project/project-$omenv.apk

gradle clean \
    && gradle assembleRelease \
    && cp $src $dest \
    && echo output to $dest
