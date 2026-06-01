#!/bin/sh
echo "[+] Stopping ALL services..."

BASE="/data/data/com.lodwickmasete.php/files"

sh $BASE/scripts/stop_apache.sh
sleep 1

sh $BASE/scripts/stop_php_fpm.sh
sleep 1

sh $BASE/scripts/stop_mariadb.sh

echo "[+] ALL services stopped ✔"