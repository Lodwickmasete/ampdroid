#!/bin/sh
# start_apache.sh - Starts Apache with configurable environment variables

# Apache paths
APACHE_BIN="${APACHE_BIN:-/data/data/com.lodwickmasete.php/files/Apache/bin/httpd}"
APACHE_CONF="${APACHE_CONF:-/data/data/com.lodwickmasete.php/files/Apache/apache2/httpd.conf}"
APACHE_LOGS="/data/data/com.lodwickmasete.php/files/logs/apache2"

# Web config
APACHE_DOCROOT="${APACHE_DOCROOT:-/data/data/com.lodwickmasete.php/files/www}"
APACHE_PORT="${APACHE_PORT:-8080}"

# PHP-FPM
FPM_PORT="${FPM_PORT:-9000}"

# HTTPS / SSL
SSL_ENABLED="${SSL_ENABLED:-false}"
SSL_PORT="${SSL_PORT:-8443}"
SSL_CERT="${SSL_CERT:-/data/data/com.lodwickmasete.php/files/ssl/server.crt}"
SSL_KEY="${SSL_KEY:-/data/data/com.lodwickmasete.php/files/ssl/server.key}"

# Export variables
export APACHE_DOCROOT
export APACHE_PORT
export FPM_PORT

export SSL_ENABLED
export SSL_PORT
export SSL_CERT
export SSL_KEY

# Library paths
COMMON_LIB="/data/data/com.lodwickmasete.php/files/lib/common"
HTTPD_LIB="/data/data/com.lodwickmasete.php/files/lib/httpd"

export LD_LIBRARY_PATH="$COMMON_LIB:$HTTPD_LIB"

# Check Apache binary
if [ ! -f "$APACHE_BIN" ]; then
    echo "[!] Apache binary not found at $APACHE_BIN"
    exit 1
fi

# Make executable
chmod +x "$APACHE_BIN"

# Ensure logs directory exists
mkdir -p "$APACHE_LOGS"

# Ensure document root exists
if [ ! -d "$APACHE_DOCROOT" ]; then
    echo "[!] Creating document root: $APACHE_DOCROOT"
    mkdir -p "$APACHE_DOCROOT"
fi

# HTTPS info
if [ "$SSL_ENABLED" = "true" ]; then
    echo "[+] SSL enabled"
    echo "[+] SSL Port: $SSL_PORT"
    echo "[+] SSL Cert: $SSL_CERT"
    echo "[+] SSL Key: $SSL_KEY"

    "$APACHE_BIN" -D SSL_ENABLED -f "$APACHE_CONF" &
else
    echo "[+] SSL disabled"

    "$APACHE_BIN" -f "$APACHE_CONF" &
fi

# Save PID
APACHE_PID=$!

echo "[+] Apache started with PID $APACHE_PID"
echo "[+] HTTP Port: $APACHE_PORT"

if [ "$SSL_ENABLED" = "true" ]; then
    echo "[+] HTTPS Port: $SSL_PORT"
fi

echo "[+] Document root: $APACHE_DOCROOT"
echo "[+] Logs directory: $APACHE_LOGS"