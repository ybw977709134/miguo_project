#!/bin/bash

if [ $# == 1 ]; then
  opt="-s $1"
fi

db=wowtalkdb_8a0dc925-b22a-4d6b-8b9f-ecc7159dda14
adb $opt pull /data/data/co.onemeter.oneapp/databases/$db && sqlite3 $db
