#!/bin/bash
# $1 = range start

if [ $# == 1 ]
then
    range="$1..HEAD"
else
    range="--since=`date -d '1 days ago' +%Y-%m-%d`"
fi

git log --pretty='format:%h (%an) %s' --no-merges $range

