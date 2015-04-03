#!/bin/bash

omenv='dev01'
[ $# -gt 0 ] && omenv=$1

# increase version code
curr_ver_code=`grep android:versionCode project/AndroidManifest.xml |grep -oP '[0-9]+'`
curr_ver_code=$((curr_ver_code + 1))
perl -pi -e \
    "s/android:versionCode=\"([0-9]+)\"/android:versionCode=\"$curr_ver_code\"/" \
    project/AndroidManifest.xml

# update build version name
buildVer=`date +%m%d`
[ $omenv != 'product' ] && buildVer="$buildVer($omenv)"
perl -pi -e \
    "s/android:versionName=\"([0-9]+)\.([0-9]+)\.([0-9]+)\..*\"/android:versionName=\"\1.\2.\3.$buildVer\"/" \
    project/AndroidManifest.xml
curr_ver_name=`grep android:versionName project/AndroidManifest.xml |awk -F '"' '{print $2}'`

export OM_ENV=$omenv 
echo OM_ENV: $OM_ENV
echo version $curr_ver_code:$curr_ver_name

src=./project/build/outputs/apk/project-release.apk
dest=./out/om_im_android_${omenv}_`date +%m%d`.apk

gradle assembleRelease \
    && cp $src $dest \
    && echo output to $dest

#
# update server side upgrade configuration.
#
if [ $? -eq 0 ] && [ $omenv == 'dev01' ]
then
    ver_xml=`mktemp`
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
    echo "        <link>http://dev01-websrv.onemeter.co/download/`basename $dest`</link>" >> $ver_xml
    echo "        <change_log>" >> $ver_xml

    git log --pretty='format:          <li>%h %s</li>' --no-merges origin/dev_as..HEAD >> $ver_xml
    echo '' >> $ver_xml

    echo "        </change_log>" >> $ver_xml

    echo '      </android>
          <ios>
            <ver_code>200</ver_code>
            <ver_name>2.0.0.1</ver_name>
            <md5sum>b037bfb6e71b5908a18ad24ea92e17ff</md5sum>
              <change_log>
                <li>测试一下更新</li>
            </change_log>
          </ios>
        </check_for_updates>
      </body>
    </Smartphone>' >> $ver_xml

    scp $dest dev01:/var/www/newapi/download/
    scp $ver_xml dev01:/var/www/newapi/download/ver.xml
    rm -f $ver_xml
fi
