#!/bin/bash
# 收集各开发分支上的新内容
# 
# 执行之前请：
# git checkout dev_as
# git pull

log_opt='--pretty="%h |%ar|%an|%s"'

# determine check point
chkpnt_dev_as=`sh -c "git log $log_opt -n1 dev_as" | col.sh 1`
chkpnt_dev=`sh -c "git log $log_opt -n1 dev" | col.sh 1`

echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo check point of branch dev_as: $chkpnt_dev_as
echo check point of branch dev: $chkpnt_dev
echo

src_branch=dev_as
range="--since=`date -d '1 days ago' +%Y-%m-%d`"
echo $src_branch
cmd="git log $log_opt --no-merges $range $src_branch"
pick_dev_as="git cherry-pick `sh -c "$cmd" | col.sh 1 | tac`"
echo $pick_dev_as
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
echo \&\& $pick_zhang 

echo ''
echo \# for dev
echo git checkout dev \\
echo \&\& git merge origin/dev_zhangzheng \\
echo \&\& $pick_dev_as

echo ''
echo \# check
echo git diff dev_as dev project/src
