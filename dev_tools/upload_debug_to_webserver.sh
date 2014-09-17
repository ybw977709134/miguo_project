./cp_debug_to_local_webserver.sh 

SRCFILE=../out/production/yuanqutong/yuanqutong.apk
md5sum $SRCFILE
scp -i ~/.ssh/dev01_wowtalkapi.pem $SRCFILE root@dev01.wowtalkapi.com:/var/www/download/
ssh -i ~/.ssh/dev01_wowtalkapi.pem root@dev01.wowtalkapi.com "md5sum /var/www/download/yuanqutong.apk"
