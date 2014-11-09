#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt install -r out/production/oneapp/oneapp.apk
