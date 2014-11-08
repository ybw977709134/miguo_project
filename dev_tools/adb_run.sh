#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt shell am start -n co.onemeter.oneapp/co.onemeter.oneapp.YuanquActivity
