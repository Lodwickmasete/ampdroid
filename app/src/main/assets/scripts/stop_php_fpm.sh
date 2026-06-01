#!/bin/sh
# stop_php_fpm.sh - Stops PHP-FPM

pkill -f "php-fpm"
echo "[+] PHP-FPM stopped"