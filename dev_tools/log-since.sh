#!/bin/bash
# $1 = range start

git log --pretty='format:%h (%an) %s' --no-merges $1..HEAD

