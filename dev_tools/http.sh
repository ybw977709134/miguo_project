#!/bin/bash
curl http://websrv.onemeter.co/om_im_api.php -d "$1" 2>/dev/null
curl http://dev01-websrv.onemeter.co/om_im_api.php -d "$1" 2>/dev/null
curl http://localhost/om_im_api.php -d "$1" 2>/dev/null
