#!/bin/bash
# 收集各开发分支上的新内容

log_opt='--pretty="%h |%ar|%an|%s"'

# determine check point
chkpnt_dev_as=`sh -c "git log $log_opt -n1 dev_as" | col.sh 1`
chkpnt_dev=`sh -c "git log $log_opt -n1 dev" | col.sh 1`

echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo check point of branch dev_as: $chkpnt_dev_as
echo check point of branch dev: $chkpnt_dev
echo

dst_branch=dev_as
src_branch=origin/dev_yuanlei
echo $src_branch
cmd="git log $log_opt --no-merges $chkpnt_dev_as..$dst_branch $src_branch"
echo git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`
sh -c "$cmd"
echo

dst_branch=dev
src_branch=origin/dev_hutianfeng
echo $src_branch
cmd="git log $log_opt --no-merges $chkpnt_dev..$dst_branch $src_branch"
echo git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`
sh -c "$cmd"
echo

dst_branch=dev
src_branch=origin/dev_zhangzheng
echo $src_branch
cmd="git log $log_opt --no-merges $chkpnt_dev..$dst_branch $src_branch"
echo git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`
sh -c "$cmd"
echo
