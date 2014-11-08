#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt shell pm clear co.onemeter.oneapp
adb $opt shell rm -r /sdcard/wowtalk/.cache
