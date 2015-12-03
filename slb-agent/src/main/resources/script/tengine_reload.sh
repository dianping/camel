#!/bin/bash
set -e

if [ `pgrep nginx | wc -l` != 0 ]; then
	echo "reload tengine config"
	sudo -u root /etc/init.d/nginx reload || { echo "fail to reload tengine, exit code is $?"; exit 1; }
else
	echo "start tengine"
	sudo -u root /etc/init.d/nginx start || { echo "fail to start tengine, exit code is $?"; exit 1; }
fi