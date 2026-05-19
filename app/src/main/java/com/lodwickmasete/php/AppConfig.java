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
 *
 * Usage:
 *   AppConfig cfg = AppConfig.load(context);
 *   cfg.set("document_root", "/data/data/.../files/www");
 *   cfg.save();
 *   String root = cfg.get("document_root", "/data/data/.../files/www");
 */
public class AppConfig {

    // ── Keys ──────────────────────────────────────────────────────────────────
    public static final String KEY_DOCUMENT_ROOT   = "document_root";
    public static final String KEY_APACHE_PORT     = "apache_port";
    public static final String KEY_PHP_FPM_PORT    = "php_fpm_port";
    public static final String KEY_MYSQL_PORT      = "mysql_port";
    public static final String KEY_MYSQL_HOST      = "mysql_host";
    public static final String KEY_MEMORY_LIMIT    = "memory_limit";
    public static final String KEY_MAX_EXEC_TIME   = "max_exec_time";
    public static final String KEY_UPLOAD_MAX      = "upload_max_size";
    public static final String KEY_DISPLAY_ERRORS  = "display_errors";
    public static final String KEY_ALLOW_URL_FOPEN = "allow_url_fopen";
    public static final String KEY_OPCACHE         = "opcache_enabled";
    public static final String KEY_PHP_VERSION_IDX = "php_version_index";
    public static final String KEY_SERVER_TYPE_IDX = "server_type_index";
    public static final String KEY_PHP_MODE        = "php_mode";          // "cli" | "fpm"
    public static final String KEY_FPM_MAX_CHILDREN= "fpm_max_children";
    public static final String KEY_FPM_SOCKET      = "fpm_socket";
    public static final String KEY_ERROR_LOG       = "error_log";
    public static final String KEY_HTTPD_CONF      = "httpd_conf";
    public static final String KEY_THEME           = "theme";             // "dark"|"light"|"system"
    // FTP
    public static final String KEY_FTP_PORT        = "ftp_port";
    public static final String KEY_FTP_ROOT        = "ftp_root";
    public static final String KEY_FTP_USER        = "ftp_user";
    public static final String KEY_FTP_PASS        = "ftp_pass";
    public static final String KEY_FTP_ANON        = "ftp_anonymous";

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

    public JSONObject toJson() {
        return data;
    }

    // ── Defaults ──────────────────────────────────────────────────────────────

    private static final String DATA = "/data/data/com.lodwickmasete.php/files";

    private void applyDefaults() {
        putDefault(KEY_DOCUMENT_ROOT,   DATA + "/www");
        putDefault(KEY_APACHE_PORT,     "8080");
        putDefault(KEY_PHP_FPM_PORT,    "9000");
        putDefault(KEY_MYSQL_PORT,      "3306");
        putDefault(KEY_MYSQL_HOST,      "127.0.0.1");
        putDefault(KEY_MEMORY_LIMIT,    "128M");
        putDefault(KEY_MAX_EXEC_TIME,   "30");
        putDefault(KEY_UPLOAD_MAX,      "64M");
        putDefault(KEY_DISPLAY_ERRORS,  true);
        putDefault(KEY_ALLOW_URL_FOPEN, true);
        putDefault(KEY_OPCACHE,         false);
        putDefault(KEY_PHP_VERSION_IDX, 0);
        putDefault(KEY_SERVER_TYPE_IDX, 0);
        putDefault(KEY_PHP_MODE,        "fpm");
        putDefault(KEY_FPM_MAX_CHILDREN,"5");
        putDefault(KEY_FPM_SOCKET,      DATA + "/tmp/php-fpm.sock");
        putDefault(KEY_ERROR_LOG,       DATA + "/logs/apache2/error_log");
        putDefault(KEY_HTTPD_CONF,      DATA + "/Apache/apache2/httpd.conf");
        putDefault(KEY_THEME,           "dark");
        putDefault(KEY_FTP_PORT,        "2121");
        putDefault(KEY_FTP_ROOT,        DATA + "/www");
        putDefault(KEY_FTP_USER,        "admin");
        putDefault(KEY_FTP_PASS,        "admin");
        putDefault(KEY_FTP_ANON,        false);
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