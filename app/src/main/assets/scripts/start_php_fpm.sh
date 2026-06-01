#!/bin/sh
# start_php_fpm.sh - Starts PHP-FPM with configurable environment variables

# Use environment variables or defaults
FPM_PORT="${FPM_PORT:-9000}"
FPM_MAX_CHILDREN="${FPM_MAX_CHILDREN:-5}"
FPM_SOCKET="${FPM_SOCKET:-/data/data/com.lodwickmasete.php/files/tmp/php-fpm.sock}"
PHP_MEMORY_LIMIT="${PHP_MEMORY_LIMIT:-128M}"
PHP_MAX_EXEC_TIME="${PHP_MAX_EXEC_TIME:-30}"
PHP_UPLOAD_MAX="${PHP_UPLOAD_MAX:-64M}"
PHP_DISPLAY_ERRORS="${PHP_DISPLAY_ERRORS:-On}"
PHP_ALLOW_URL_FOPEN="${PHP_ALLOW_URL_FOPEN:-On}"
PHP_OPCACHE="${PHP_OPCACHE:-0}"

PHP_DISABLE_FUNCTIONS="${PHP_DISABLE_FUNCTIONS:-}"


# Paths
PHP_FPM_BIN="/data/data/com.lodwickmasete.php/files/fpm/bin/php-fpm"
PHP_INI="/data/data/com.lodwickmasete.php/files/php.ini"
FPM_CONF="/data/data/com.lodwickmasete.php/files/fpm/php-fpm.conf"
WWW_CONF="/data/data/com.lodwickmasete.php/files/fpm/php-fpm.d/www.conf"

# Set library path
export LD_LIBRARY_PATH="/data/data/com.lodwickmasete.php/files/lib/common:/data/data/com.lodwickmasete.php/files/lib/php:/data/data/com.lodwickmasete.php/files/lib/php-fpm"

# Make sure binary is executable
chmod +x "$PHP_FPM_BIN"

# Create temp php.ini with custom settings
TEMP_PHP_INI="${PHP_INI}.tmp"
cat > "$TEMP_PHP_INI" << EOF
memory_limit = $PHP_MEMORY_LIMIT
max_execution_time = $PHP_MAX_EXEC_TIME
upload_max_filesize = $PHP_UPLOAD_MAX
post_max_size = $PHP_UPLOAD_MAX
display_errors = $PHP_DISPLAY_ERRORS
allow_url_fopen = $PHP_ALLOW_URL_FOPEN

disable_functions = $PHP_DISABLE_FUNCTIONS



opcache.enable = $PHP_OPCACHE
opcache.lockfile_path = "/data/data/com.lodwickmasete.php/files/tmp"
error_log = "/data/data/com.lodwickmasete.php/files/logs/php_errors.log"
session.save_path = "/data/data/com.lodwickmasete.php/files/tmp"
upload_tmp_dir = "/data/data/com.lodwickmasete.php/files/tmp"
extension_dir = "/data/data/com.lodwickmasete.php/files/lib/php-fpm"
EOF


# Create temp www.conf with custom port/socket
mkdir -p "$(dirname "$WWW_CONF")"
TEMP_WWW_CONF="${WWW_CONF}.tmp"
cat > "$TEMP_WWW_CONF" << EOF
[www]

#env[HOME] = /data/data/com.lodwickmasete.php/files

listen = ${FPM_SOCKET}

listen.mode = 0660

pm = dynamic
pm.max_children = ${FPM_MAX_CHILDREN}
pm.start_servers = 2
pm.min_spare_servers = 1
pm.max_spare_servers = 3

pm.status_path = /status
ping.path = /ping
ping.response = pong

request_terminate_timeout = ${PHP_MAX_EXEC_TIME}
EOF

# Start PHP-FPM with temp configs
echo "[+] Starting PHP-FPM on socket: $FPM_SOCKET"
echo "[+] Max children: $FPM_MAX_CHILDREN"
"$PHP_FPM_BIN" \
  -y "$FPM_CONF" \
  -c "$TEMP_PHP_INI" \
  -p /data/data/com.lodwickmasete.php/files/fpm &

PHP_PID=$!

sleep 2
echo "[+] PHP-FPM started with PID $PHP_PID"