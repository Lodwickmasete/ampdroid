#!/bin/sh
# start_mariadb.sh - Starts MariaDB with configurable environment variables

# Use environment variables or defaults
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"

# Paths
MARIADB_BIN="/data/data/com.lodwickmasete.php/files/db/bin/mariadbd"
DATA_DIR="/data/data/com.lodwickmasete.php/files/db/data"
SOCKET_PATH="$DATA_DIR/mysql.sock"
PID_FILE="$DATA_DIR/mariadb.pid"
LOG_FILE="$DATA_DIR/mariadb.log"

# Set library path
export LD_LIBRARY_PATH="/data/data/com.lodwickmasete.php/files/lib/common:/data/data/com.lodwickmasete.php/files/lib/mysql:/data/data/com.lodwickmasete.php/files/lib/php:/data/data/com.lodwickmasete.php/files/lib/php-fpm"

# Ensure data directory exists
mkdir -p "$DATA_DIR"

# Initialize database if not already initialized
#if [ ! -f "$DATA_DIR/mysql/user.MYD" ] && [ ! -f "$DATA_DIR/mysql/user.ibd" ]; then
#    echo "[+] Initializing MariaDB database..."
#    /data/data/com.lodwickmasete.php/files/db/bin/mariadb-install-db \
#        --datadir="$DATA_DIR" \
#        --auth-root-authentication-method=normal
#fi

# Make sure binary is executable
chmod +x "$MARIADB_BIN"

# Start MariaDB
echo "[+] Starting MariaDB on port $MYSQL_PORT..."
"$MARIADB_BIN" \
  --datadir="$DATA_DIR" \
  --port="$MYSQL_PORT" \
  --bind-address="$MYSQL_HOST" \
  --pid-file="$PID_FILE" \
  --socket="$SOCKET_PATH" \
  --log-error="$LOG_FILE" \
  --skip-networking=0 &

DB_PID=$!

echo "[+] MariaDB started with PID $DB_PID on port $MYSQL_PORT"