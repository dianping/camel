#!/bin/bash
set -e

if [ `pgrep nginx | wc -l` != 0 ]; then
	echo "reload tengine config"
	sudo -u root /usr/local/nginx/sbin/nginx -s reload || { echo "fail to reload tengine, exit code is $?"; exit 1; }
else
	echo "start tengine"
	sudo -u root /usr/local/nginx/sbin/nginx -s start || { echo "fail to start tengine, exit code is $?"; exit 1; }
fi