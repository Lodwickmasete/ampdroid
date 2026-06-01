#!/bin/sh
# stop_apache.sh - Stops Apache

pkill -f "httpd"
echo "[+] Apache stopped"