#!/bin/bash
# usage
#   sh dev_tools/build_with_gradle.sh [omenv]
# where omenv = dev01 | product

omenv='dev01'
[ $# -gt 0 ] && omenv=$1

# increase version code
curr_ver_code=`grep android:versionCode project/AndroidManifest.xml |grep -oP '[0-9]+'`
curr_ver_code=$((curr_ver_code + 1))
perl -pi -e \
    "s/android:versionCode=\"([0-9]+)\"/android:versionCode=\"$curr_ver_code\"/" \
    project/AndroidManifest.xml

# update build version name
# version name := major.minor.ver_code.date
buildVer=`date +%m%d`
[ $omenv != 'product' ] && buildVer="$buildVer($omenv)"
perl -pi -e \
    "s/android:versionName=\"([0-9]+)\.([0-9]+)\.([0-9]+)\..*\"/android:versionName=\"\1.\2.\3.$buildVer\/$curr_ver_code\"/" \
    project/AndroidManifest.xml
curr_ver_name=`grep android:versionName project/AndroidManifest.xml |awk -F '"' '{print $2}'`

export OM_ENV=$omenv 
echo OM_ENV: $OM_ENV
echo version $curr_ver_code '=>' $curr_ver_name

src=./project/build/outputs/apk/project-release.apk
dest=./out/om_im_android_${omenv}_`date +%m%d`_${curr_ver_code}.apk

gradle clean \
    && gradle assembleRelease \
    && cp $src $dest \
    && echo output to $dest

# archive proguard mapping file (the path is configured in proguard.cfg)
if [ -f project/classes-processed.map ]
then
    cp project/classes-processed.map ${dest%.apk}.map
fi

#
# prepare for publishing
#
if [ $? -eq 0 ]
then
    if [ $omenv == 'product' ]; then
        host='websrv.onemeter.co'
    else
        host='dev01-websrv.onemeter.co'
    fi

    ver_xml=`mktemp`
    old_ver_xml=`mktemp`

    echo download current upgrading configuration
    scp root@$host:/var/www/newapi/download/ver.xml $old_ver_xml

    echo '<?xml version="1.0" encoding="UTF-8"?>
<Smartphone xmlns="https://www.onemeter.co">
  <header>
    <err_no>0</err_no>
    <s_version>
      <server_version>1000</server_version>
    </s_version>
    <user_id>-1</user_id>
  </header>
  <body>
    <check_for_updates>
      <android>' > $ver_xml

    echo "        <ver_code>$curr_ver_code</ver_code>" >> $ver_xml
    echo "        <ver_name>$curr_ver_name</ver_name>" >> $ver_xml
    echo "        <md5sum>`md5sum $dest|awk '{print $1}'`</md5sum>" >> $ver_xml
    echo "        <link>http://$host/download/`basename $dest`</link>" >> $ver_xml
    echo "        <change_log>" >> $ver_xml

    git log --pretty='format:          <li><![CDATA[%h %s]]></li>' --no-merges \
        --since "`date -d '1 days ago' +%Y-%m-%d`" >> $ver_xml
    echo '' >> $ver_xml

    echo "        </change_log>" >> $ver_xml

    echo '      </android>' >> $ver_xml
    sed -e '/<ios>/,/<\/ios>/!d' $old_ver_xml >> $ver_xml
    echo '  </check_for_updates>
  </body>
</Smartphone>' >> $ver_xml

    echo '--------------------------------------'
    cat $ver_xml
    echo '--------------------------------------'
    echo updated upgrading configuration written to $ver_xml

    if [ $omenv == 'dev01' ]
    then
        scp $dest root@$host:/var/www/newapi/download/
        scp $ver_xml root@$host:/var/www/newapi/download/ver.xml
        rm -f $old_ver_xml
    else
        echo ''
        echo you may want to:
        echo scp $dest root@$host:/var/www/newapi/download/
        echo scp $ver_xml root@$host:/var/www/newapi/download/ver.xml
    fi
fi
