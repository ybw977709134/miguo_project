#!/bin/bash
# 复制 om_im_ui repo 中的一个图片资源到 Android 资源目录中。

if [[ $# -lt 1 ]]; then
    echo Usage: $0 src [dest]
    echo -e '\tsrc,dest\tfilename without ext.'
    exit
fi

from=$1
if [ $# -lt 2 ]; then
    to=$from
else
    to=$2
fi

projectdir=project/
#projectdir=lib/message-input/

for dpi in mdpi hdpi xhdpi
do
    echo cp -f "../om_im_ui/client/slice/Android/$dpi/$from.png" $projectdir/res/drawable-$dpi/$to.png
    cp -f "../om_im_ui/client/slice/Android/$dpi/$from.png" $projectdir/res/drawable-$dpi/$to.png
done

