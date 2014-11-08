#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

adb $opt uninstall co.onemeter.oneapp
