#!/bin/bash
# 收集各开发分支上的新内容

# determine check point
chkpnt_dev_as=`git log --oneline -n1 dev_as | col.sh 1`
chkpnt_dev=`git log --oneline -n1 dev | col.sh 1`
echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo check point of branch dev_as: $chkpnt_dev_as
echo check point of branch dev: $chkpnt_dev
echo

dst_branch=dev_as
src_branch=origin/dev_yuanlei
echo $src_branch
cmd="git log --oneline --no-merges $chkpnt_dev_as..$dst_branch $src_branch"
echo git cherry-pick `$cmd | col.sh 1`
$cmd
echo

dst_branch=dev
src_branch=origin/dev_hutianfeng
echo $src_branch
cmd="git log --oneline --no-merges $chkpnt_dev..$dst_branch $src_branch"
echo git cherry-pick `$cmd | col.sh 1`
$cmd
echo

dst_branch=dev
src_branch=origin/dev_zhangzheng
echo $src_branch
cmd="git log --oneline --no-merges $chkpnt_dev..$dst_branch $src_branch"
echo git cherry-pick `$cmd | col.sh 1`
$cmd
echo
