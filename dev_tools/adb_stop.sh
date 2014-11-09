#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt shell am force-stop co.onemeter.oneapp
