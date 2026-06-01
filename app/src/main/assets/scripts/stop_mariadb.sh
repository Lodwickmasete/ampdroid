#!/bin/sh
# stop_mariadb.sh - Stops MariaDB inside your APK

echo "[+] Stopping MariaDB..."

# Paths
PID_FILE="/data/data/com.lodwickmasete.php/files/db/data/mariadb.pid"
SOCKET_FILE="/data/data/com.lodwickmasete.php/files/db/data/mysql.sock"
MARIADB_BIN="/data/data/com.lodwickmasete.php/files/db/bin/mariadbd"

# 1️⃣ Try clean shutdown using mysqladmin if available
if command -v mysqladmin >/dev/null 2>&1; then
  mysqladmin --socket="$SOCKET_FILE" shutdown 2>/dev/null
fi

# 2️⃣ If PID file exists, kill using PID
if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    echo "[+] Sent TERM to MariaDB (PID: $PID)"
  fi
fi

# 3️⃣ Force kill if still running
pkill -f mariadbd

# 4️⃣ Cleanup PID and socket files
rm -f "$PID_FILE"
rm -f "$SOCKET_FILE"

echo "[+] MariaDB stopped"