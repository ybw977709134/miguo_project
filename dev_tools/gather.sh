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
pick_yuan="git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`"
echo $pick_yuan
sh -c "$cmd"
echo

dst_branch=dev
src_branch=origin/dev_hutianfeng
echo $src_branch
cmd="git log $log_opt --no-merges $chkpnt_dev..$dst_branch $src_branch"
pick_hu="git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`"
echo $pick_hu
sh -c "$cmd"
echo

dst_branch=dev
src_branch=origin/dev_zhangzheng
echo $src_branch
cmd="git log $log_opt --no-merges $chkpnt_dev..$dst_branch $src_branch"
pick_zhang="git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`"
echo $pick_zhang
sh -c "$cmd"
echo

echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo \# for dev_as
echo git checkout dev_as \\
echo \&\& git merge origin/dev_yuanlei \\
echo \&\& $pick_hu \\
echo \&\& $pick_zhang 

echo ''
echo \# for dev
echo git checkout dev \\
echo \&\& git merge origin/dev_hutianfeng origin/dev_zhangzheng \\
echo \&\& $pick_yuan

echo ''
echo \# check
echo git diff dev_as dev project/src
