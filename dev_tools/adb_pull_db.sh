#!/bin/bash

if [ $# == 0 ]; then
    echo -e "Usage:\n\t$0 <uid> [<device id>]"
    exit
fi

if [ $# == 2 ]; then
  opt="-s $2"
fi

db=wowtalkdb_$1
adb $opt pull /data/data/co.onemeter.oneapp/databases/$db && sqlite3 $db
