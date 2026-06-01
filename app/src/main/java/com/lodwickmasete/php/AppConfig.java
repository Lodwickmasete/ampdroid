package com.lodwickmasete.php;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * AppConfig — persists all user settings as a JSON file at
 * getFilesDir()/app_config.json so no setup is lost across restarts.
 */
public class AppConfig {

    // ── Web Server ────────────────────────────────────────────────────────────
    public static final String KEY_DOCUMENT_ROOT     = "document_root";
    public static final String KEY_APACHE_PORT       = "apache_port";
    public static final String KEY_SERVER_TYPE_IDX   = "server_type_index";   // 0=Apache,1=Nginx,2=Lighttpd
    public static final String KEY_SERVER_NAME       = "server_name";          // e.g. localhost
    public static final String KEY_SERVER_ADMIN      = "server_admin";
    public static final String KEY_AUTOSTART_SERVER  = "autostart_server";
    public static final String KEY_DIRECTORY_LISTING = "directory_listing";
    public static final String KEY_KEEP_ALIVE        = "keep_alive";
    public static final String KEY_KEEP_ALIVE_TIMEOUT= "keep_alive_timeout";   // seconds
    public static final String KEY_MAX_CONNECTIONS   = "max_connections";
    public static final String KEY_HTTPD_CONF        = "httpd_conf";
    public static final String KEY_SSL_ENABLED       = "ssl_enabled";
    public static final String KEY_SSL_PORT          = "ssl_port";
    public static final String KEY_SSL_CERT          = "ssl_cert";
    public static final String KEY_SSL_KEY           = "ssl_key";

    // ── PHP ───────────────────────────────────────────────────────────────────
    public static final String KEY_PHP_VERSION_IDX   = "php_version_index";    // 0=8.3,1=8.2,2=8.1,3=7.4
    public static final String KEY_PHP_MODE          = "php_mode";             // "cli"|"fpm"
    public static final String KEY_PHP_FPM_PORT      = "php_fpm_port";
    public static final String KEY_FPM_MAX_CHILDREN  = "fpm_max_children";
    public static final String KEY_FPM_SOCKET        = "fpm_socket";
    public static final String KEY_MEMORY_LIMIT      = "memory_limit";         // e.g. 128M
    public static final String KEY_MAX_EXEC_TIME     = "max_exec_time";        // seconds
    public static final String KEY_UPLOAD_MAX        = "upload_max_size";
    public static final String KEY_POST_MAX_SIZE     = "post_max_size";
    public static final String KEY_DISPLAY_ERRORS    = "display_errors";
    public static final String KEY_ERROR_REPORTING   = "error_reporting";       // "E_ALL"|"E_ALL & ~E_NOTICE"|"0"
    public static final String KEY_ALLOW_URL_FOPEN   = "allow_url_fopen";
    public static final String KEY_OPCACHE           = "opcache_enabled";
    public static final String KEY_OPCACHE_MEMORY    = "opcache_memory_mb";    // MB
    public static final String KEY_SHORT_OPEN_TAG    = "short_open_tag";
    public static final String KEY_TIMEZONE          = "timezone";              // e.g. UTC
    public static final String KEY_SESSION_SAVE_PATH = "session_save_path";
    public static final String KEY_DISABLE_FUNCTIONS = "disable_functions";    // comma-separated

    // ── MySQL / MariaDB ───────────────────────────────────────────────────────
    public static final String KEY_MYSQL_PORT        = "mysql_port";
    public static final String KEY_MYSQL_HOST        = "mysql_host";
    public static final String KEY_MYSQL_ROOT_PASS   = "mysql_root_password";
    public static final String KEY_MYSQL_DATA_DIR    = "mysql_data_dir";
    public static final String KEY_MYSQL_MAX_CONN    = "mysql_max_connections";
    public static final String KEY_MYSQL_CHARSET     = "mysql_charset";        // utf8mb4
    public static final String KEY_MYSQL_AUTOSTART   = "mysql_autostart";
    public static final String KEY_MYSQL_INNODB_SIZE = "mysql_innodb_buffer_mb"; // MB
    public static final String KEY_PHPMYADMIN        = "phpmyadmin_enabled";

    // ── FTP ───────────────────────────────────────────────────────────────────
    public static final String KEY_FTP_ENABLED       = "ftp_enabled";
    public static final String KEY_FTP_PORT          = "ftp_port";
    public static final String KEY_FTP_ROOT          = "ftp_root";
    public static final String KEY_FTP_USER          = "ftp_user";
    public static final String KEY_FTP_PASS          = "ftp_pass";
    public static final String KEY_FTP_ANON          = "ftp_anonymous";
    public static final String KEY_FTP_PASSIVE       = "ftp_passive_mode";
    public static final String KEY_FTP_PASSIVE_PORTS = "ftp_passive_ports";    // e.g. 40000-40100
    public static final String KEY_FTP_MAX_CLIENTS   = "ftp_max_clients";

    // ── Logging ───────────────────────────────────────────────────────────────
    public static final String KEY_ERROR_LOG         = "error_log";
    public static final String KEY_ACCESS_LOG        = "access_log";
    public static final String KEY_LOG_LEVEL         = "log_level";            // "warn"|"error"|"debug"|"info"
    public static final String KEY_LOG_ROTATION      = "log_rotation";         // "daily"|"weekly"|"size"
    public static final String KEY_LOG_MAX_SIZE_MB   = "log_max_size_mb";
    public static final String KEY_LOG_KEEP_DAYS     = "log_keep_days";

    // ── Security ─────────────────────────────────────────────────────────────
    public static final String KEY_HTACCESS          = "htaccess_enabled";
    public static final String KEY_HOTLINK_PROTECT   = "hotlink_protection";
    public static final String KEY_CSRF_HEADER       = "csrf_header";
    public static final String KEY_BLOCK_XMLRPC      = "block_xmlrpc";
    public static final String KEY_RATE_LIMIT        = "rate_limit_enabled";
    public static final String KEY_RATE_LIMIT_REQ    = "rate_limit_requests";  // per minute
    public static final String KEY_CORS_ENABLED      = "cors_enabled";
    public static final String KEY_CORS_ORIGINS      = "cors_origins";         // * or domain list
    public static final String KEY_HIDE_SERVER_TOKEN = "hide_server_tokens";

    // ── App / UI ──────────────────────────────────────────────────────────────
    public static final String KEY_THEME             = "theme";                // "dark"|"light"|"system"
    public static final String KEY_CONFIRM_STOP      = "confirm_stop_server";
    public static final String KEY_NOTIFY_START      = "notify_on_start";
    public static final String KEY_NOTIFY_CRASH      = "notify_on_crash";
    public static final String KEY_WAKELOCK          = "wakelock_enabled";
    public static final String KEY_STATS_INTERVAL    = "stats_refresh_interval"; // seconds
    public static final String KEY_TERMINAL_LINES    = "terminal_max_lines";
    public static final String KEY_FONT_SIZE         = "editor_font_size";

    // ── Backup ───────────────────────────────────────────────────────────────
    public static final String KEY_BACKUP_ENABLED    = "backup_enabled";
    public static final String KEY_BACKUP_DIR        = "backup_dir";
    public static final String KEY_BACKUP_SCHEDULE   = "backup_schedule";      // "daily"|"weekly"|"manual"
    public static final String KEY_BACKUP_KEEP       = "backup_keep_count";
    public static final String KEY_BACKUP_INCLUDE_DB = "backup_include_db";
    public static final String KEY_BACKUP_INCLUDE_WWW= "backup_include_www";

    // ── Cron ─────────────────────────────────────────────────────────────────
    public static final String KEY_CRON_ENABLED      = "cron_enabled";
    public static final String KEY_CRON_LOG          = "cron_log";

    private static final String CONFIG_FILE = "app_config.json";

    private final File configFile;
    private JSONObject data;

    private AppConfig(File file, JSONObject data) {
        this.configFile = file;
        this.data = data;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AppConfig load(Context context) {
        File file = new File(context.getFilesDir(), CONFIG_FILE);
        JSONObject obj = new JSONObject();

        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                obj = new JSONObject(sb.toString());
            } catch (Exception ignored) {}
        }

        AppConfig cfg = new AppConfig(file, obj);
        cfg.applyDefaults();
        return cfg;
    }

    // ── Read / Write ──────────────────────────────────────────────────────────

    public String get(String key, String defaultValue) {
        return data.optString(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (data.has(key)) {
            try { return data.getBoolean(key); } catch (JSONException ignored) {}
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return data.optInt(key, defaultValue);
    }

    public void set(String key, String value) {
        try { data.put(key, value); } catch (JSONException ignored) {}
    }

    public void set(String key, boolean value) {
        try { data.put(key, value); } catch (JSONException ignored) {}
    }

    public void set(String key, int value) {
        try { data.put(key, value); } catch (JSONException ignored) {}
    }

    public boolean save() {
        try {
            FileWriter fw = new FileWriter(configFile);
            fw.write(data.toString(2));
            fw.close();
            return true;
        } catch (IOException | JSONException e) {
            return false;
        }
    }

    public JSONObject toJson() { return data; }

    // ── Defaults ──────────────────────────────────────────────────────────────

    private static final String DATA = "/data/data/com.lodwickmasete.php/files";

    private void applyDefaults() {
        // Web server
        putDefault(KEY_DOCUMENT_ROOT,      DATA + "/www");
        putDefault(KEY_APACHE_PORT,        "8080");
        putDefault(KEY_SERVER_TYPE_IDX,    0);
        putDefault(KEY_SERVER_NAME,        "localhost");
        putDefault(KEY_SERVER_ADMIN,       "admin@localhost");
        putDefault(KEY_AUTOSTART_SERVER,   false);
        putDefault(KEY_DIRECTORY_LISTING,  false);
        putDefault(KEY_KEEP_ALIVE,         true);
        putDefault(KEY_KEEP_ALIVE_TIMEOUT, "5");
        putDefault(KEY_MAX_CONNECTIONS,    "150");
        putDefault(KEY_HTTPD_CONF,         DATA + "/Apache/apache2/httpd.conf");
        putDefault(KEY_SSL_ENABLED,        false);
        putDefault(KEY_SSL_PORT,           "8443");
        putDefault(KEY_SSL_CERT,           DATA + "/ssl/server.crt");
        putDefault(KEY_SSL_KEY,            DATA + "/ssl/server.key");

        // PHP
        putDefault(KEY_PHP_VERSION_IDX,    0);
        putDefault(KEY_PHP_MODE,           "fpm");
        putDefault(KEY_PHP_FPM_PORT,       "9000");
        putDefault(KEY_FPM_MAX_CHILDREN,   "5");
        putDefault(KEY_FPM_SOCKET,         DATA + "/tmp/php-fpm.sock");
        putDefault(KEY_MEMORY_LIMIT,       "128M");
        putDefault(KEY_MAX_EXEC_TIME,      "30");
        putDefault(KEY_UPLOAD_MAX,         "64M");
        putDefault(KEY_POST_MAX_SIZE,      "64M");
        putDefault(KEY_DISPLAY_ERRORS,     true);
        putDefault(KEY_ERROR_REPORTING,    "E_ALL");
        putDefault(KEY_ALLOW_URL_FOPEN,    true);
        putDefault(KEY_OPCACHE,            false);
        putDefault(KEY_OPCACHE_MEMORY,     "64");
        putDefault(KEY_SHORT_OPEN_TAG,     false);
        putDefault(KEY_TIMEZONE,           "UTC");
        putDefault(KEY_SESSION_SAVE_PATH,  DATA + "/tmp/sessions");
        putDefault(KEY_DISABLE_FUNCTIONS,  "exec,shell_exec,system,passthru,proc_open,popen");

        // MySQL
        putDefault(KEY_MYSQL_PORT,         "3306");
        putDefault(KEY_MYSQL_HOST,         "127.0.0.1");
        putDefault(KEY_MYSQL_ROOT_PASS,    "");
        putDefault(KEY_MYSQL_DATA_DIR,     DATA + "/mysql/data");
        putDefault(KEY_MYSQL_MAX_CONN,     "100");
        putDefault(KEY_MYSQL_CHARSET,      "utf8mb4");
        putDefault(KEY_MYSQL_AUTOSTART,    false);
        putDefault(KEY_MYSQL_INNODB_SIZE,  "64");
        putDefault(KEY_PHPMYADMIN,         false);

        // FTP
        putDefault(KEY_FTP_ENABLED,        false);
        putDefault(KEY_FTP_PORT,           "2121");
        putDefault(KEY_FTP_ROOT,           DATA + "/www");
        putDefault(KEY_FTP_USER,           "admin");
        putDefault(KEY_FTP_PASS,           "admin");
        putDefault(KEY_FTP_ANON,           false);
        putDefault(KEY_FTP_PASSIVE,        true);
        putDefault(KEY_FTP_PASSIVE_PORTS,  "40000-40100");
        putDefault(KEY_FTP_MAX_CLIENTS,    "10");

        // Logging
        putDefault(KEY_ERROR_LOG,          DATA + "/logs/apache2/error_log");
        putDefault(KEY_ACCESS_LOG,         DATA + "/logs/apache2/access_log");
        putDefault(KEY_LOG_LEVEL,          "warn");
        putDefault(KEY_LOG_ROTATION,       "daily");
        putDefault(KEY_LOG_MAX_SIZE_MB,    "10");
        putDefault(KEY_LOG_KEEP_DAYS,      "7");

        // Security
        putDefault(KEY_HTACCESS,           true);
        putDefault(KEY_HOTLINK_PROTECT,    false);
        putDefault(KEY_CSRF_HEADER,        false);
        putDefault(KEY_BLOCK_XMLRPC,       true);
        putDefault(KEY_RATE_LIMIT,         false);
        putDefault(KEY_RATE_LIMIT_REQ,     "100");
        putDefault(KEY_CORS_ENABLED,       false);
        putDefault(KEY_CORS_ORIGINS,       "*");
        putDefault(KEY_HIDE_SERVER_TOKEN,  true);

        // App/UI
        putDefault(KEY_THEME,              "dark");
        putDefault(KEY_CONFIRM_STOP,       true);
        putDefault(KEY_NOTIFY_START,       true);
        putDefault(KEY_NOTIFY_CRASH,       true);
        putDefault(KEY_WAKELOCK,           true);
        putDefault(KEY_STATS_INTERVAL,     "3");
        putDefault(KEY_TERMINAL_LINES,     "500");
        putDefault(KEY_FONT_SIZE,          "13");

        // Backup
        putDefault(KEY_BACKUP_ENABLED,     false);
        putDefault(KEY_BACKUP_DIR,         DATA + "/backups");
        putDefault(KEY_BACKUP_SCHEDULE,    "daily");
        putDefault(KEY_BACKUP_KEEP,        "5");
        putDefault(KEY_BACKUP_INCLUDE_DB,  true);
        putDefault(KEY_BACKUP_INCLUDE_WWW, true);

        // Cron
        putDefault(KEY_CRON_ENABLED,       false);
        putDefault(KEY_CRON_LOG,           DATA + "/logs/cron.log");
    }

    private void putDefault(String key, String val) {
        if (!data.has(key)) try { data.put(key, val); } catch (JSONException ignored) {}
    }
    private void putDefault(String key, boolean val) {
        if (!data.has(key)) try { data.put(key, val); } catch (JSONException ignored) {}
    }
    private void putDefault(String key, int val) {
        if (!data.has(key)) try { data.put(key, val); } catch (JSONException ignored) {}
    }
}