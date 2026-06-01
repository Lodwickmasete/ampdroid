#!/bin/sh
echo "[+] Starting ALL services..."

BASE="/data/data/com.lodwickmasete.php/files"

sh $BASE/scripts/start_mariadb.sh
sleep 2

sh $BASE/scripts/start_php_fpm.sh
sleep 2

sh $BASE/scripts/start_apache.sh

echo "[+] ALL services started ✔"