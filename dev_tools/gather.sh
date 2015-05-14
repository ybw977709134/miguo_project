#!/bin/bash
# 收集各开发分支上的新内容
# 
# 执行之前请：
# git fetch

function gitlog {
    author=$1
    src_branch=$2

    echo by $author:

    range="--since=`date -d '1 days ago' +%Y-%m-%d`"
    log_opt='--pretty="%h | %ar | %s"'

    cmd="git log $log_opt --author $author --no-merges $range $src_branch"
    pick_dev_as="git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`"
    sh -c "$cmd"
    echo '=>' $pick_dev_as
    echo '=>' git merge $src_branch
    echo
}

gitlog hutianfeng origin/dev_as
gitlog zhangzheng origin/dev_zhangzheng

