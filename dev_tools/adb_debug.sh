#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt shell am start -D -n "co.onemeter.oneapp/co.onemeter.oneapp.YuanquActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
