#!/bin/bash

/usr/bin/supervisord -c /etc/supervisord.conf
sleep 5

mysql < myCDN.sql
rm -rf myCDN.sql

cd /opt/app
su -s /bin/sh <<EOF
php -S 0.0.0.0:8080
EOF
