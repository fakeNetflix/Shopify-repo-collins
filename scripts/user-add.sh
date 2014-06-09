#!/bin/bash

read -p "Enter a username in google auth domain: " username
echo -n "Enter a password: "
read -s password
echo
read -p "Enter permission groups (comma delimited): " groups
if [ -z "$groups" ]; then
  groups="none"
fi
hash=`echo -n "$password" | openssl dgst -binary -sha1 | openssl base64`
echo "Enter db password for collins user:"
mysql -u collins -p collins --execute="insert into users (username, password, salt, roles, type) values('$username','{SHA}$hash','','$groups','google');"
