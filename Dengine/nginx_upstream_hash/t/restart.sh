#!/bin/bash

pkill nginx
cp nginx.conf ./nginx/conf/
./nginx/sbin/nginx

killall memcached
memcached -d -l 127.0.0.1 -p 11211
memcached -d -l 127.0.0.1 -p 11212
memcached -d -l 127.0.0.1 -p 11213
memcached -d -l 127.0.0.1 -p 11214
memcached -d -l 127.0.0.1 -p 11215
