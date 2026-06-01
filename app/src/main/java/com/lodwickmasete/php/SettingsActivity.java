package com.lodwickmasete.php;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * SettingsActivity — full settings screen.
 *
 * Sections (matching drawer nav):
 *   0  Web Server      — ports, document root, server type, SSL, keep-alive
 *   1  PHP             — version, mode, FPM, memory, errors, opcache, timezone
 *   2  MySQL           — port, host, root password, innodb, charset, phpMyAdmin
 *   3  FTP             — enable, port, user/pass, passive, anonymous
 *   4  Logging         — log paths, level, rotation
 *   5  Security        — .htaccess, hotlink, rate limit, CORS, hide tokens
 *   6  Backup          — schedule, keep count, include db/www
 *   7  App             — theme, notifications, wakelock, stats interval, font size
 *
 * No AppCompat / AndroidX / lambdas. Anonymous inner classes only.
 because im on sketchware
 */
public class SettingsActivity extends Activity {

    // ─── State ────────────────────────────────────────────────────────────────
    private AppConfig cfg;
    private ThemeManager tm;
    private boolean drawerOpen = false;
    private int currentSection = 0;

    // ─── Root views ───────────────────────────────────────────────────────────
    private LinearLayout drawerPanel;
    private LinearLayout contentArea;
    private View drawerScrim;
    private TextView toolbarTitle;

    // ─── Section names ────────────────────────────────────────────────────────
    private static final String[] SECTIONS = {
        "Web Server", "PHP", "MySQL", "FTP",
        "Logging", "Security", "Backup", "App"
    };
    private static final String[] ICONS = {
        "🌐", "🐘", "🗄️", "📂", "📋", "🔒", "💾", "⚙️"
/*TODO:ADD ICONS*/
    //first make it exist,make it beatiful later (:
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cfg = AppConfig.load(this);
        tm  = new ThemeManager(cfg.get(AppConfig.KEY_THEME, "dark"), this);
        tm.applyWindow(this);

        View root = buildRoot();
        setContentView(root);

        showSection(0);
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ROOT LAYOUT  (FrameLayout built in code — FrameLayout not easy in code,
    //  so we use a RelativeLayout-like trick: outermost is a LinearLayout set
    //  to MATCH/MATCH with the content behind, and the drawer overlaid.)
    //
    //  Actual structure:
    //    FrameLayout (root)
    //      LinearLayout (main column: toolbar + scrollview + save bar)
    //      View (scrim)
    //      LinearLayout (drawer)
    // ═════════════════════════════════════════════════════════════════════════

    private View buildRoot() {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setBackgroundColor(tm.bg);

        //  Main column
        LinearLayout main = buildMainColumn();
        android.widget.FrameLayout.LayoutParams mainLp =
                new android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frame.addView(main, mainLp);

        //  Scrim
        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.parseColor("#99000000"));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { closeDrawer(); }
        });
        android.widget.FrameLayout.LayoutParams scrimLp =
                new android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frame.addView(drawerScrim, scrimLp);

        // Drawer
        drawerPanel = buildDrawer();
        int drawerWidthPx = dp(280);
        android.widget.FrameLayout.LayoutParams drawerLp =
                new android.widget.FrameLayout.LayoutParams(drawerWidthPx,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        drawerLp.gravity = Gravity.START;
        drawerPanel.setTranslationX(-drawerWidthPx);
        frame.addView(drawerPanel, drawerLp);

        return frame;
    }

    // ─── Main column ─────────────────────────────────────────────────────────

    private LinearLayout buildMainColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(tm.bg);
        col.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        col.addView(buildToolbar());
        col.addView(buildScrollArea());
        col.addView(buildSaveBar());

        return col;
    }

    private View buildToolbar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(tm.surface);
        bar.setPadding(dp(4), dp(12), dp(16), dp(12));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        // Hamburger button
        TextView burger = new TextView(this);
        burger.setText("☰");
        burger.setTextSize(22);
        burger.setTextColor(tm.text);
        burger.setPadding(dp(12), dp(4), dp(12), dp(4));
        burger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleDrawer(); }
        });

        // Title
        toolbarTitle = new TextView(this);
        toolbarTitle.setText("⚙️  Settings");
        toolbarTitle.setTextColor(tm.text);
        toolbarTitle.setTextSize(16);
        toolbarTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.setMargins(dp(8), 0, 0, 0);
        toolbarTitle.setLayoutParams(titleLp);

        bar.addView(burger);
        bar.addView(toolbarTitle);
        return bar;
    }

    private View buildScrollArea() {
        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(lp);
        sv.setBackgroundColor(tm.bg);

        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(dp(12), dp(12), dp(12), dp(12));
        sv.addView(contentArea);

        return sv;
    }

    private View buildSaveBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(tm.surface);
        bar.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Discard
        TextView discard = new TextView(this);
        discard.setText("DISCARD");
        discard.setTextColor(tm.red);
        discard.setTextSize(12);
        discard.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        discard.setGravity(Gravity.CENTER);
        discard.setBackgroundResource(R.drawable.button_secondary);
        discard.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams discardLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        discard.setLayoutParams(discardLp);
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg = AppConfig.load(SettingsActivity.this);
                showSection(currentSection);
                toast("Changes discarded");
            }
        });

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));

        // Save
        TextView save = new TextView(this);
        save.setText("SAVE");
        save.setTextColor(Color.WHITE);
        save.setTextSize(12);
        save.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        save.setGravity(Gravity.CENTER);
        save.setBackgroundResource(R.drawable.button_success);
        save.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        save.setLayoutParams(saveLp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cfg.save()) {
                    toast("Settings saved");
                    // Re-apply theme in case it changed
                    tm = new ThemeManager(cfg.get(AppConfig.KEY_THEME, "dark"), SettingsActivity.this);
                } else {
                    toast("Save failed — check storage permissions");
                }
            }
        });

        bar.addView(discard);
        bar.addView(gap);
        bar.addView(save);
        return bar;
    }

    // ─── Drawer ───────────────────────────────────────────────────────────────

    private LinearLayout buildDrawer() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(tm.surface);

        // Drawer header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(tm.card);
        header.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("AmpDroid");
        title.setTextColor(tm.text);
        title.setTextSize(18);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText("Settings");
        sub.setTextColor(tm.accent);
        sub.setTextSize(12);
        sub.setTypeface(Typeface.MONOSPACE);

        header.addView(title);
        header.addView(sub);
        drawer.addView(header);

        // Divider
        drawer.addView(makeDivider());

        // Nav items
        ScrollView navScroll = new ScrollView(this);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        navScroll.setLayoutParams(navLp);

        LinearLayout navList = new LinearLayout(this);
        navList.setOrientation(LinearLayout.VERTICAL);
        navList.setPadding(0, dp(8), 0, dp(8));

        for (int i = 0; i < SECTIONS.length; i++) {
            navList.addView(buildNavItem(i));
        }
        navScroll.addView(navList);
        drawer.addView(navScroll);

        // Version footer
        drawer.addView(makeDivider());
        TextView ver = new TextView(this);
        ver.setText("v1.0.0  •  AmpDroid");
        ver.setTextColor(tm.muted);
        ver.setTextSize(10);
        ver.setTypeface(Typeface.MONOSPACE);
        ver.setPadding(dp(20), dp(14), dp(20), dp(14));
        drawer.addView(ver);

        return drawer;
    }

    private View buildNavItem(final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        row.setGravity(Gravity.CENTER_VERTICAL);

        boolean active = (index == currentSection);
        row.setBackgroundColor(active ? tm.card : Color.TRANSPARENT);

        TextView icon = new TextView(this);
        icon.setText(ICONS[index]);
        icon.setTextSize(18);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        icon.setLayoutParams(iconLp);

        TextView label = new TextView(this);
        label.setText(SECTIONS[index]);
        label.setTextColor(active ? tm.accent : tm.text);
        label.setTextSize(14);
        label.setTypeface(Typeface.MONOSPACE, active ? Typeface.BOLD : Typeface.NORMAL);

        row.addView(icon);
        row.addView(label);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(index);
                closeDrawer();
            }
        });

        return row;
    }

    // ─── Drawer animation ─────────────────────────────────────────────────────

    private void toggleDrawer() {
        if (drawerOpen) closeDrawer(); else openDrawer();
    }

    private void openDrawer() {
        drawerOpen = true;
        drawerScrim.setVisibility(View.VISIBLE);
        drawerPanel.animate().translationX(0).setDuration(240).start();
    }

    private void closeDrawer() {
        drawerOpen = false;
        int w = drawerPanel.getWidth();
        if (w == 0) w = dp(280);
        drawerPanel.animate().translationX(-w).setDuration(200).start();
        drawerScrim.setVisibility(View.GONE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SECTION RENDERING
    // ═════════════════════════════════════════════════════════════════════════

    private void showSection(int index) {
        currentSection = index;
        toolbarTitle.setText(ICONS[index] + "  " + SECTIONS[index]);
        contentArea.removeAllViews();

        switch (index) {
            case 0: buildWebServerSection(); break;
            case 1: buildPhpSection();       break;
            case 2: buildMysqlSection();     break;
            case 3: buildFtpSection();       break;
            case 4: buildLoggingSection();   break;
            case 5: buildSecuritySection();  break;
            case 6: buildBackupSection();    break;
            case 7: buildAppSection();       break;
        }
    }

    // ─── 0  Web Server ────────────────────────────────────────────────────────

    private void buildWebServerSection() {
        addSectionHeader("Server Type");
        addSpinner("Server Engine", "server_type_index",
                new String[]{"Apache 2.4", "Nginx", "Lighttpd"},
                AppConfig.KEY_SERVER_TYPE_IDX);

        addSectionHeader("General");
        addTextField("Document Root",   "Path served to browsers",
                AppConfig.KEY_DOCUMENT_ROOT,   cfg.get(AppConfig.KEY_DOCUMENT_ROOT, ""));
        addTextField("Apache Port",     "HTTP listen port (default 8080)",
                AppConfig.KEY_APACHE_PORT,     cfg.get(AppConfig.KEY_APACHE_PORT, "8080"));
        addTextField("Server Name",     "Hostname / virtual host",
                AppConfig.KEY_SERVER_NAME,     cfg.get(AppConfig.KEY_SERVER_NAME, "localhost"));
        addTextField("Server Admin",    "Admin email in error pages",
                AppConfig.KEY_SERVER_ADMIN,    cfg.get(AppConfig.KEY_SERVER_ADMIN, "admin@localhost"));
        addTextField("httpd.conf Path", "Full path to httpd.conf",
                AppConfig.KEY_HTTPD_CONF,      cfg.get(AppConfig.KEY_HTTPD_CONF, ""));
        addTextField("Max Connections", "MaxRequestWorkers / worker_connections",
                AppConfig.KEY_MAX_CONNECTIONS, cfg.get(AppConfig.KEY_MAX_CONNECTIONS, "150"));
        addToggle("Auto-start on launch", AppConfig.KEY_AUTOSTART_SERVER,
                cfg.getBoolean(AppConfig.KEY_AUTOSTART_SERVER, false));
        addToggle("Directory Listing",    AppConfig.KEY_DIRECTORY_LISTING,
                cfg.getBoolean(AppConfig.KEY_DIRECTORY_LISTING, false));

        addSectionHeader("Keep-Alive");
        addToggle("Keep-Alive",           AppConfig.KEY_KEEP_ALIVE,
                cfg.getBoolean(AppConfig.KEY_KEEP_ALIVE, true));
        addTextField("Keep-Alive Timeout", "Seconds",
                AppConfig.KEY_KEEP_ALIVE_TIMEOUT, cfg.get(AppConfig.KEY_KEEP_ALIVE_TIMEOUT, "5"));

        addSectionHeader("SSL / TLS");
        addToggle("Enable SSL",           AppConfig.KEY_SSL_ENABLED,
                cfg.getBoolean(AppConfig.KEY_SSL_ENABLED, false));
        addTextField("SSL Port",          "HTTPS listen port (default 8443)",
                AppConfig.KEY_SSL_PORT,        cfg.get(AppConfig.KEY_SSL_PORT, "8443"));
        addTextField("Certificate Path",  "Path to server.crt",
                AppConfig.KEY_SSL_CERT,        cfg.get(AppConfig.KEY_SSL_CERT, ""));
        addTextField("Private Key Path",  "Path to server.key",
                AppConfig.KEY_SSL_KEY,         cfg.get(AppConfig.KEY_SSL_KEY, ""));
    }

    // ─── 1  PHP ───────────────────────────────────────────────────────────────

    private void buildPhpSection() {
        addSectionHeader("Runtime");
        addSpinner("PHP Version", "php_version_index",
                new String[]{"PHP 8.3", "PHP 8.2", "PHP 8.1", "PHP 7.4"},
                AppConfig.KEY_PHP_VERSION_IDX);
        addSpinner("Execution Mode", "php_mode_spinner",
                new String[]{"PHP-FPM", "CLI"},
                AppConfig.KEY_PHP_MODE, new String[]{"fpm", "cli"});

        addSectionHeader("PHP-FPM");
        addTextField("FPM Port",          "FastCGI listen port",
                AppConfig.KEY_PHP_FPM_PORT,    cfg.get(AppConfig.KEY_PHP_FPM_PORT, "9000"));
        addTextField("Max Children",      "pm.max_children",
                AppConfig.KEY_FPM_MAX_CHILDREN,cfg.get(AppConfig.KEY_FPM_MAX_CHILDREN, "5"));
        addTextField("FPM Socket Path",   "Unix socket path",
                AppConfig.KEY_FPM_SOCKET,      cfg.get(AppConfig.KEY_FPM_SOCKET, ""));

        addSectionHeader("Limits");
        addTextField("Memory Limit",      "e.g. 128M",
                AppConfig.KEY_MEMORY_LIMIT,    cfg.get(AppConfig.KEY_MEMORY_LIMIT, "128M"));
        addTextField("Max Execution Time","Seconds (0 = unlimited)",
                AppConfig.KEY_MAX_EXEC_TIME,   cfg.get(AppConfig.KEY_MAX_EXEC_TIME, "30"));
        addTextField("Upload Max Size",   "e.g. 64M",
                AppConfig.KEY_UPLOAD_MAX,      cfg.get(AppConfig.KEY_UPLOAD_MAX, "64M"));
        addTextField("Post Max Size",     "e.g. 64M",
                AppConfig.KEY_POST_MAX_SIZE,   cfg.get(AppConfig.KEY_POST_MAX_SIZE, "64M"));

        addSectionHeader("Error Handling");
        addToggle("Display Errors",       AppConfig.KEY_DISPLAY_ERRORS,
                cfg.getBoolean(AppConfig.KEY_DISPLAY_ERRORS, true));
        addSpinner("Error Reporting", "error_reporting",
                new String[]{"E_ALL", "E_ALL & ~E_NOTICE", "E_ALL & ~E_DEPRECATED", "None (0)"},
                AppConfig.KEY_ERROR_REPORTING,
                new String[]{"E_ALL", "E_ALL & ~E_NOTICE", "E_ALL & ~E_DEPRECATED", "0"});

        addSectionHeader("Extensions & Features");
        addToggle("allow_url_fopen",      AppConfig.KEY_ALLOW_URL_FOPEN,
                cfg.getBoolean(AppConfig.KEY_ALLOW_URL_FOPEN, true));
        addToggle("Short Open Tags",      AppConfig.KEY_SHORT_OPEN_TAG,
                cfg.getBoolean(AppConfig.KEY_SHORT_OPEN_TAG, false));
        addToggle("OPcache",              AppConfig.KEY_OPCACHE,
                cfg.getBoolean(AppConfig.KEY_OPCACHE, false));
        addTextField("OPcache Memory (MB)","Memory for OPcache",
                AppConfig.KEY_OPCACHE_MEMORY,  cfg.get(AppConfig.KEY_OPCACHE_MEMORY, "64"));

        addSectionHeader("Locale & Paths");
        addTextField("Timezone",          "e.g. UTC, Africa/Harare",
                AppConfig.KEY_TIMEZONE,        cfg.get(AppConfig.KEY_TIMEZONE, "UTC"));
        addTextField("Session Save Path", "Directory for session files",
                AppConfig.KEY_SESSION_SAVE_PATH, cfg.get(AppConfig.KEY_SESSION_SAVE_PATH, ""));
        addTextField("Disabled Functions","Comma-separated function names",
                AppConfig.KEY_DISABLE_FUNCTIONS, cfg.get(AppConfig.KEY_DISABLE_FUNCTIONS, ""));
    }

    // ─── 2  MySQL ─────────────────────────────────────────────────────────────

    private void buildMysqlSection() {
        addSectionHeader("Connection");
        addTextField("Host",              "MySQL bind address",
                AppConfig.KEY_MYSQL_HOST,      cfg.get(AppConfig.KEY_MYSQL_HOST, "127.0.0.1"));
        addTextField("Port",              "Default 3306",
                AppConfig.KEY_MYSQL_PORT,      cfg.get(AppConfig.KEY_MYSQL_PORT, "3306"));
        addPasswordField("Root Password", "Leave blank for no password",
                AppConfig.KEY_MYSQL_ROOT_PASS, cfg.get(AppConfig.KEY_MYSQL_ROOT_PASS, ""));

        addSectionHeader("Storage");
        addTextField("Data Directory",    "MySQL data dir path",
                AppConfig.KEY_MYSQL_DATA_DIR,  cfg.get(AppConfig.KEY_MYSQL_DATA_DIR, ""));
        addTextField("InnoDB Buffer (MB)","innodb_buffer_pool_size in MB",
                AppConfig.KEY_MYSQL_INNODB_SIZE, cfg.get(AppConfig.KEY_MYSQL_INNODB_SIZE, "64"));

        addSectionHeader("Performance");
        addTextField("Max Connections",   "max_connections",
                AppConfig.KEY_MYSQL_MAX_CONN,  cfg.get(AppConfig.KEY_MYSQL_MAX_CONN, "100"));
        addSpinner("Default Charset", "mysql_charset",
                new String[]{"utf8mb4", "utf8", "latin1"},
                AppConfig.KEY_MYSQL_CHARSET,
                new String[]{"utf8mb4", "utf8", "latin1"});

        addSectionHeader("Features");
        addToggle("Auto-start MySQL",     AppConfig.KEY_MYSQL_AUTOSTART,
                cfg.getBoolean(AppConfig.KEY_MYSQL_AUTOSTART, false));
        addToggle("Enable phpMyAdmin",    AppConfig.KEY_PHPMYADMIN,
                cfg.getBoolean(AppConfig.KEY_PHPMYADMIN, false));
    }

    // ─── 3  FTP ───────────────────────────────────────────────────────────────

    private void buildFtpSection() {
        addSectionHeader("FTP Server");
        addToggle("Enable FTP",           AppConfig.KEY_FTP_ENABLED,
                cfg.getBoolean(AppConfig.KEY_FTP_ENABLED, false));
        addTextField("Port",              "Default 2121 (avoid 21 on Android)",
                AppConfig.KEY_FTP_PORT,        cfg.get(AppConfig.KEY_FTP_PORT, "2121"));
        addTextField("Root Directory",    "FTP base path",
                AppConfig.KEY_FTP_ROOT,        cfg.get(AppConfig.KEY_FTP_ROOT, ""));
        addTextField("Max Clients",       "Simultaneous connections",
                AppConfig.KEY_FTP_MAX_CLIENTS, cfg.get(AppConfig.KEY_FTP_MAX_CLIENTS, "10"));

        addSectionHeader("Authentication");
        addTextField("Username",          "FTP login username",
                AppConfig.KEY_FTP_USER,        cfg.get(AppConfig.KEY_FTP_USER, "admin"));
        addPasswordField("Password",      "FTP login password",
                AppConfig.KEY_FTP_PASS,        cfg.get(AppConfig.KEY_FTP_PASS, ""));
        addToggle("Allow Anonymous",      AppConfig.KEY_FTP_ANON,
                cfg.getBoolean(AppConfig.KEY_FTP_ANON, false));

        addSectionHeader("Passive Mode");
        addToggle("Passive Mode (PASV)",  AppConfig.KEY_FTP_PASSIVE,
                cfg.getBoolean(AppConfig.KEY_FTP_PASSIVE, true));
        addTextField("Passive Port Range","e.g. 40000-40100",
                AppConfig.KEY_FTP_PASSIVE_PORTS, cfg.get(AppConfig.KEY_FTP_PASSIVE_PORTS, "40000-40100"));
    }

    // ─── 4  Logging ───────────────────────────────────────────────────────────

    private void buildLoggingSection() {
        addSectionHeader("Log Paths");
        addTextField("Error Log",         "Apache / Nginx error log path",
                AppConfig.KEY_ERROR_LOG,       cfg.get(AppConfig.KEY_ERROR_LOG, ""));
        addTextField("Access Log",        "Apache / Nginx access log path",
                AppConfig.KEY_ACCESS_LOG,      cfg.get(AppConfig.KEY_ACCESS_LOG, ""));
        addTextField("Cron Log",          "Cron job output path",
                AppConfig.KEY_CRON_LOG,        cfg.get(AppConfig.KEY_CRON_LOG, ""));

        addSectionHeader("Log Level");
        addSpinner("Log Level", "log_level",
                new String[]{"warn", "error", "info", "debug"},
                AppConfig.KEY_LOG_LEVEL,
                new String[]{"warn", "error", "info", "debug"});

        addSectionHeader("Rotation");
        addSpinner("Rotation Strategy", "log_rotation",
                new String[]{"Daily", "Weekly", "By Size"},
                AppConfig.KEY_LOG_ROTATION,
                new String[]{"daily", "weekly", "size"});
        addTextField("Max Log Size (MB)", "Rotate when log exceeds this size",
                AppConfig.KEY_LOG_MAX_SIZE_MB, cfg.get(AppConfig.KEY_LOG_MAX_SIZE_MB, "10"));
        addTextField("Keep (days)",       "Delete logs older than N days",
                AppConfig.KEY_LOG_KEEP_DAYS,   cfg.get(AppConfig.KEY_LOG_KEEP_DAYS, "7"));
    }

    // ─── 5  Security ──────────────────────────────────────────────────────────

    private void buildSecuritySection() {
        addSectionHeader("Access Control");
        addToggle(".htaccess Support",    AppConfig.KEY_HTACCESS,
                cfg.getBoolean(AppConfig.KEY_HTACCESS, true));
        addToggle("Hide Server Tokens",   AppConfig.KEY_HIDE_SERVER_TOKEN,
                cfg.getBoolean(AppConfig.KEY_HIDE_SERVER_TOKEN, true));
        addToggle("Block XML-RPC",        AppConfig.KEY_BLOCK_XMLRPC,
                cfg.getBoolean(AppConfig.KEY_BLOCK_XMLRPC, true));
        addToggle("Hotlink Protection",   AppConfig.KEY_HOTLINK_PROTECT,
                cfg.getBoolean(AppConfig.KEY_HOTLINK_PROTECT, false));

        addSectionHeader("Rate Limiting");
        addToggle("Enable Rate Limit",    AppConfig.KEY_RATE_LIMIT,
                cfg.getBoolean(AppConfig.KEY_RATE_LIMIT, false));
        addTextField("Max Requests / min","Per-IP request cap per minute",
                AppConfig.KEY_RATE_LIMIT_REQ,  cfg.get(AppConfig.KEY_RATE_LIMIT_REQ, "100"));

        addSectionHeader("CORS");
        addToggle("Enable CORS Headers",  AppConfig.KEY_CORS_ENABLED,
                cfg.getBoolean(AppConfig.KEY_CORS_ENABLED, false));
        addTextField("Allowed Origins",   "* or https://example.com",
                AppConfig.KEY_CORS_ORIGINS,    cfg.get(AppConfig.KEY_CORS_ORIGINS, "*"));

        addSectionHeader("Headers");
        addToggle("CSRF Origin Header",   AppConfig.KEY_CSRF_HEADER,
                cfg.getBoolean(AppConfig.KEY_CSRF_HEADER, false));
    }

    // ─── 6  Backup ────────────────────────────────────────────────────────────

    private void buildBackupSection() {
        addSectionHeader("Backup");
        addToggle("Enable Auto-Backup",   AppConfig.KEY_BACKUP_ENABLED,
                cfg.getBoolean(AppConfig.KEY_BACKUP_ENABLED, false));
        addTextField("Backup Directory",  "Where to store .tar.gz archives",
                AppConfig.KEY_BACKUP_DIR,      cfg.get(AppConfig.KEY_BACKUP_DIR, ""));

        addSectionHeader("Schedule");
        addSpinner("Backup Schedule", "backup_schedule",
                new String[]{"Daily", "Weekly", "Manual"},
                AppConfig.KEY_BACKUP_SCHEDULE,
                new String[]{"daily", "weekly", "manual"});
        addTextField("Keep (count)",      "Number of backups to keep",
                AppConfig.KEY_BACKUP_KEEP,     cfg.get(AppConfig.KEY_BACKUP_KEEP, "5"));

        addSectionHeader("What to Back Up");
        addToggle("Include MySQL Databases", AppConfig.KEY_BACKUP_INCLUDE_DB,
                cfg.getBoolean(AppConfig.KEY_BACKUP_INCLUDE_DB, true));
        addToggle("Include www Directory",   AppConfig.KEY_BACKUP_INCLUDE_WWW,
                cfg.getBoolean(AppConfig.KEY_BACKUP_INCLUDE_WWW, true));

        addSectionHeader("Cron");
        addToggle("Enable Cron Daemon",   AppConfig.KEY_CRON_ENABLED,
                cfg.getBoolean(AppConfig.KEY_CRON_ENABLED, false));

        // Manual backup button
        addDangerButton("Run Backup Now", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("Backup queued — check cron log");
            }
        });
    }

    // ─── 7  App ───────────────────────────────────────────────────────────────

    private void buildAppSection() {
        addSectionHeader("Appearance");
        addSpinner("Theme", "theme",
                new String[]{"Dark", "Light", "System"},
                AppConfig.KEY_THEME,
                new String[]{"dark", "light", "system"});
        addTextField("Editor Font Size",  "pt — used in terminal / editor",
                AppConfig.KEY_FONT_SIZE,       cfg.get(AppConfig.KEY_FONT_SIZE, "13"));

        addSectionHeader("Behaviour");
        addToggle("Confirm Before Stop",  AppConfig.KEY_CONFIRM_STOP,
                cfg.getBoolean(AppConfig.KEY_CONFIRM_STOP, true));
        addToggle("Wake Lock (keep awake)",AppConfig.KEY_WAKELOCK,
                cfg.getBoolean(AppConfig.KEY_WAKELOCK, true));
        addTextField("Stats Refresh (sec)","How often live stats update",
                AppConfig.KEY_STATS_INTERVAL,  cfg.get(AppConfig.KEY_STATS_INTERVAL, "3"));
        addTextField("Terminal Max Lines","Buffer size for terminal panel",
                AppConfig.KEY_TERMINAL_LINES,  cfg.get(AppConfig.KEY_TERMINAL_LINES, "500"));

        addSectionHeader("Notifications");
        addToggle("Notify on Server Start",AppConfig.KEY_NOTIFY_START,
                cfg.getBoolean(AppConfig.KEY_NOTIFY_START, true));
        addToggle("Notify on Crash",       AppConfig.KEY_NOTIFY_CRASH,
                cfg.getBoolean(AppConfig.KEY_NOTIFY_CRASH, true));

        addSectionHeader("Danger Zone");
        addDangerButton("Reset All Settings to Defaults", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Reset Settings")
                    .setMessage("This will overwrite all settings with defaults. Continue?")
                    .setPositiveButton("RESET", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            // Delete config file so AppConfig recreates defaults
                            java.io.File f = new java.io.File(
                                    getFilesDir(), "app_config.json");
                            f.delete();
                            cfg = AppConfig.load(SettingsActivity.this);
                            cfg.save();
                            showSection(currentSection);
                            toast("Settings reset to defaults");
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  WIDGET BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Bold label row above a section. */
    private void addSectionHeader(String title) {
        // top spacing
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(12)));
        contentArea.addView(spacer);

        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase());
        tv.setTextColor(tm.accent);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setPadding(dp(4), dp(4), dp(4), dp(6));
        tv.setLetterSpacing(0.15f);
        contentArea.addView(tv);

        contentArea.addView(makeDivider());
    }

    /** Single-line text input row in a card. */
    private void addTextField(String label, String hint, final String key, String value) {
        LinearLayout card = makeCard();
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(tm.text);
        lbl.setTextSize(13);
        lbl.setTypeface(Typeface.MONOSPACE);

        EditText et = new EditText(this);
        et.setText(value);
        et.setHint(hint);
        et.setTextColor(tm.text);
        et.setHintTextColor(tm.muted);
        et.setTextSize(12);
        et.setTypeface(Typeface.MONOSPACE);
        et.setBackgroundColor(tm.bg);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, dp(6), 0, 0);
        et.setLayoutParams(etLp);
        et.setSingleLine(true);

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    cfg.set(key, ((EditText) v).getText().toString().trim());
                }
            }
        });

        card.addView(lbl);
        card.addView(et);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(4), 0, 0);
        contentArea.addView(card, cardLp);
    }

    /** Password field — shows dots, with show/hide toggle. */
    private void addPasswordField(String label, String hint, final String key, String value) {
        LinearLayout card = makeCard();
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(tm.text);
        lbl.setTextSize(13);
        lbl.setTypeface(Typeface.MONOSPACE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(rowLp);
        row.setGravity(Gravity.CENTER_VERTICAL);

        final EditText et = new EditText(this);
        et.setText(value);
        et.setHint(hint);
        et.setTextColor(tm.text);
        et.setHintTextColor(tm.muted);
        et.setTextSize(12);
        et.setTypeface(Typeface.MONOSPACE);
        et.setBackgroundColor(tm.bg);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setSingleLine(true);
        row.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView toggle = new TextView(this);
        toggle.setText("👁");
        toggle.setTextSize(16);
        toggle.setPadding(dp(10), dp(4), dp(4), dp(4));
        toggle.setOnClickListener(new View.OnClickListener() {
            boolean visible = false;
            @Override
            public void onClick(View v) {
                visible = !visible;
                if (visible) {
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                et.setSelection(et.getText().length());
            }
        });
        row.addView(toggle);

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) cfg.set(key, et.getText().toString());
            }
        });

        card.addView(lbl);
        card.addView(row);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(4), 0, 0);
        contentArea.addView(card, cardLp);
    }

    /**
     * Toggle row (on/off) — stores boolean.
     * Taps flip state immediately and write to cfg.
     */
    private void addToggle(String label, final String key, boolean currentValue) {
        LinearLayout card = makeCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(tm.text);
        lbl.setTextSize(13);
        lbl.setTypeface(Typeface.MONOSPACE);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView pill = new TextView(this);
        pill.setText(currentValue ? "ON" : "OFF");
        pill.setTextColor(Color.WHITE);
        pill.setTextSize(10);
        pill.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(14), dp(5), dp(14), dp(5));
        pill.setBackgroundColor(currentValue ? tm.green : tm.muted);
        final boolean[] state = {currentValue};

        pill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state[0] = !state[0];
                cfg.set(key, state[0]);
                pill.setText(state[0] ? "ON" : "OFF");
                pill.setBackgroundColor(state[0] ? tm.green : tm.muted);
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { pill.performClick(); }
        });

        card.addView(lbl);
        card.addView(pill);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(4), 0, 0);
        contentArea.addView(card, cardLp);
    }

    /**
     * Spinner row using AlertDialog list.
     * Stores integer index.
     */
    private void addSpinner(String label, String tag,
                            final String[] labels, final String key) {
        final int cur = cfg.getInt(key, 0);
        addSpinnerImpl(label, labels, labels, cur, new SpinnerCallback() {
            @Override public void onSelected(int idx, String val) { cfg.set(key, idx); }
        });
    }

    /**
     * Spinner row using AlertDialog list.
     * Stores string value from values[].
     */
    private void addSpinner(String label, String tag,
                            final String[] labels, final String key, final String[] values) {
        String curVal = cfg.get(key, values[0]);
        int curIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(curVal)) { curIdx = i; break; }
        }
        final int startIdx = curIdx;
        addSpinnerImpl(label, labels, values, startIdx, new SpinnerCallback() {
            @Override public void onSelected(int idx, String val) { cfg.set(key, val); }
        });
    }

    private interface SpinnerCallback { void onSelected(int idx, String val); }

    private void addSpinnerImpl(final String label, final String[] labels,
                                 final String[] values, final int curIdx,
                                 final SpinnerCallback cb) {
        LinearLayout card = makeCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(tm.text);
        lbl.setTextSize(13);
        lbl.setTypeface(Typeface.MONOSPACE);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView val = new TextView(this);
        val.setText(labels[curIdx] + "  ▾");
        val.setTextColor(tm.accent);
        val.setTextSize(12);
        val.setTypeface(Typeface.MONOSPACE);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(label)
                    .setItems(labels, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            val.setText(labels[which] + "  ▾");
                            cb.onSelected(which, values[which]);
                        }
                    })
                    .show();
            }
        };
        card.setOnClickListener(click);
        val.setOnClickListener(click);

        card.addView(lbl);
        card.addView(val);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(4), 0, 0);
        contentArea.addView(card, cardLp);
    }

    /** Red button for destructive actions. */
    private void addDangerButton(String text, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12);
        btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(R.drawable.button_danger);
        btn.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(12), 0, 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        contentArea.addView(btn);
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundColor(tm.card);
        // rounded background not available without drawable resource,
        // so we use a simple flat card matching the design language
        return card;
    }

    private View makeDivider() {
        View d = new View(this);
        d.setBackgroundColor(tm.divider);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return d;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}