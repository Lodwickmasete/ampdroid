package com.lodwickmasete.php;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────────────────────────────────
    private static final int REQUEST_STORAGE_PERM = 1001;
    private static final int REQUEST_DIR_SELECT   = 1002;
    private static final int REQUEST_FILE_SELECT  = 1003;

    private static final String DATA_DIR = "/data/data/com.lodwickmasete.php/files";

    // Script names (must exist in assets/scripts/)
    private static final String SCRIPT_START_ALL    = "start_all.sh";
    private static final String SCRIPT_STOP_ALL     = "stop_all.sh";
    private static final String SCRIPT_START_APACHE = "start_apache.sh";
    private static final String SCRIPT_STOP_APACHE  = "stop_apache.sh";
    private static final String SCRIPT_START_FPM    = "start_php_fpm.sh";
    private static final String SCRIPT_STOP_FPM     = "stop_php_fpm.sh";
    private static final String SCRIPT_START_DB     = "start_mariadb.sh";
    private static final String SCRIPT_STOP_DB      = "stop_mariadb.sh";

    // ─────────────────────────────────────────────────────────────────────────
    //  Core helpers
    // ─────────────────────────────────────────────────────────────────────────
    private AppConfig     config;
    private ScriptRunner  scriptRunner;
    private ThemeManager  theme;

    // ─────────────────────────────────────────────────────────────────────────
    //  Service state flags
    // ─────────────────────────────────────────────────────────────────────────
    private boolean phpRunning    = false;
    private boolean apacheRunning = false;
    private boolean dbRunning     = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — NAVBAR
    // ─────────────────────────────────────────────────────────────────────────
    private TextView txtServerStatus;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — QUICK ACTION BAR (service-specific)
    // ─────────────────────────────────────────────────────────────────────────
    private TextView btnStartAll;
    private TextView btnTogglePhp;
    private TextView btnToggleApache;
    private TextView btnToggleDb;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — SERVER CONFIGURATION section
    // ─────────────────────────────────────────────────────────────────────────
    private Spinner   spinnerPhpVersion;
    private Spinner   spinnerServerType;
    private RadioGroup radioGroupPhpMode;
    private RadioButton radioCli, radioFpm;
    private LinearLayout layoutFpmSettings;
    private EditText  txtFpmPort, txtFpmMaxChildren, txtFpmSocket;
    private EditText  txtMemoryLimit, txtMaxExecTime, txtUploadMaxSize;
    private EditText  txtPort, txtDocumentRoot, txtHttpdConf, txtErrorLog;
    private CheckBox  chkDisplayErrors, chkAllowUrlFopen, chkOpcache;
    private TextView  btnSelectDocRoot, btnSelectHttpdConf;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — DATABASE section
    // ─────────────────────────────────────────────────────────────────────────
    private TextView txtMysqlStatus;
    private EditText txtMysqlHost, txtMysqlPort;
    private TextView btnStartMySQL, btnStopMySQL;
    private TextView btnInstallPhpMyAdmin, btnOpenPhpMyAdmin;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — FTP section
    // ─────────────────────────────────────────────────────────────────────────
    private EditText txtFtpPort, txtFtpRoot, txtFtpUser, txtFtpPass;
    private CheckBox chkFtpAnonymous;
    private TextView btnStartFtp, btnStopFtp;
    private TextView txtFtpStatus;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — ASSET MANAGEMENT section
    // ─────────────────────────────────────────────────────────────────────────
    private EditText  txtZipUrl, txtZipPath;
    private ProgressBar progressDownload;
    private TextView  btnDownloadZip, btnExtractZip, btnCopyAssets;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — PHP CODE EDITOR shortcut
    // ─────────────────────────────────────────────────────────────────────────
    private TextView btnOpenEditor;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — TERMINAL
    // ─────────────────────────────────────────────────────────────────────────
    private TextView  txtTerminal;
    private ScrollView scrollViewTerminal;
    private EditText  txtCommand;
    private TextView  btnExecute;
    private TextView  btnClearLog, btnExportLog, btnCopyLog;
    private CheckBox  chkAutoScroll;
    private LinearLayout layoutTerminalContainer; // for show/hide

    // Quick command chips
    private TextView btnChipPhpV, btnChipPhpInfo, btnChipFpmStatus,
                     btnChipNetstat, btnChipLs, btnChipErrorLog;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — COLLAPSIBLE SECTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private LinearLayout configContent, databaseContent, assetsContent,
                          ftpContent, editorContent;
    private TextView txtCollapseConfig, txtCollapseDatabase,
                      txtCollapseAssets, txtCollapseFtp, txtCollapseEditor;
    private boolean configExpanded   = false;
    private boolean databaseExpanded = false;
    private boolean assetsExpanded   = false;
    private boolean ftpExpanded      = false;
    private boolean editorExpanded   = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — THEME TOGGLE
    // ─────────────────────────────────────────────────────────────────────────
    private TextView btnThemeDark, btnThemeLight, btnThemeSystem;

    // ─────────────────────────────────────────────────────────────────────────
    //  Views — CONFIG ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private TextView btnSaveConfig, btnLoadConfig;

    // ─────────────────────────────────────────────────────────────────────────
    //  Misc
    // ─────────────────────────────────────────────────────────────────────────
    private StringBuilder logBuilder = new StringBuilder();
    private EditText currentEditText; // for dir/file picker callbacks

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        config       = AppConfig.load(this);
        theme        = new ThemeManager(config.get(AppConfig.KEY_THEME, "dark"), this);
        scriptRunner = new ScriptRunner(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        theme.applyWindow(this);
        scriptRunner.installAllScripts();

        initViews();
        setupCollapsibles();
        setupSpinners();
        setupPhpModeToggle();
        setupListeners();
        loadConfigToUi();
        requestStoragePermission();
        checkAssets();

        appendTerminal("PHP Server Manager v3.0 — ready");
        appendTerminal("Scripts: " + new File(getFilesDir(), "scripts").getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        scriptRunner.stopAll();
        super.onDestroy();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        // Navbar
        txtServerStatus        = findViewById(R.id.txtServerStatus);

        // Quick action bar
        btnStartAll            = findViewById(R.id.btnStartAll);
        btnTogglePhp           = findViewById(R.id.btnTogglePhp);
        btnToggleApache        = findViewById(R.id.btnToggleApache);
        btnToggleDb            = findViewById(R.id.btnToggleDb);

        // Config section
        spinnerPhpVersion      = findViewById(R.id.spinnerPhpVersion);
        spinnerServerType      = findViewById(R.id.spinnerServerType);
        radioGroupPhpMode      = findViewById(R.id.radioGroupPhpMode);
        radioCli               = findViewById(R.id.radioCli);
        radioFpm               = findViewById(R.id.radioFpm);
        layoutFpmSettings      = findViewById(R.id.layoutFpmSettings);
        txtFpmPort             = findViewById(R.id.txtFpmPort);
        txtFpmMaxChildren      = findViewById(R.id.txtFpmMaxChildren);
        txtFpmSocket           = findViewById(R.id.txtFpmSocket);
        txtMemoryLimit         = findViewById(R.id.txtMemoryLimit);
        txtMaxExecTime         = findViewById(R.id.txtMaxExecTime);
        txtUploadMaxSize       = findViewById(R.id.txtUploadMaxSize);
        txtPort                = findViewById(R.id.txtPort);
        txtDocumentRoot        = findViewById(R.id.txtDocumentRoot);
        txtHttpdConf           = findViewById(R.id.txtHttpdConf);
        txtErrorLog            = findViewById(R.id.txtErrorLog);
        chkDisplayErrors       = findViewById(R.id.chkDisplayErrors);
        chkAllowUrlFopen       = findViewById(R.id.chkAllowUrlFopen);
        chkOpcache             = findViewById(R.id.chkOpcache);
        btnSelectDocRoot       = findViewById(R.id.btnSelectDocRoot);
        btnSelectHttpdConf     = findViewById(R.id.btnSelectHttpdConf);

        // Database section
        txtMysqlStatus         = findViewById(R.id.txtMysqlStatus);
        txtMysqlHost           = findViewById(R.id.txtMysqlHost);
        txtMysqlPort           = findViewById(R.id.txtMysqlPort);
        btnStartMySQL          = findViewById(R.id.btnStartMySQL);
        btnStopMySQL           = findViewById(R.id.btnStopMySQL);
        btnInstallPhpMyAdmin   = findViewById(R.id.btnInstallPhpMyAdmin);
        btnOpenPhpMyAdmin      = findViewById(R.id.btnOpenPhpMyAdmin);

        // FTP section
        txtFtpPort             = findViewById(R.id.txtFtpPort);
        txtFtpRoot             = findViewById(R.id.txtFtpRoot);
        txtFtpUser             = findViewById(R.id.txtFtpUser);
        txtFtpPass             = findViewById(R.id.txtFtpPass);
        chkFtpAnonymous        = findViewById(R.id.chkFtpAnonymous);
        btnStartFtp            = findViewById(R.id.btnStartFtp);
        btnStopFtp             = findViewById(R.id.btnStopFtp);
        txtFtpStatus           = findViewById(R.id.txtFtpStatus);

        // Assets section
        txtZipUrl              = findViewById(R.id.txtZipUrl);
        txtZipPath             = findViewById(R.id.txtZipPath);
        progressDownload       = findViewById(R.id.progressDownload);
        btnDownloadZip         = findViewById(R.id.btnDownloadZip);
        btnExtractZip          = findViewById(R.id.btnExtractZip);
        btnCopyAssets          = findViewById(R.id.btnCopyAssets);

        // Editor shortcut
        btnOpenEditor          = findViewById(R.id.btnOpenEditor);

        // Terminal
        txtTerminal            = findViewById(R.id.txtTerminal);
        scrollViewTerminal     = findViewById(R.id.scrollView);
        txtCommand             = findViewById(R.id.txtCommand);
        btnExecute             = findViewById(R.id.btnExecute);
        btnClearLog            = findViewById(R.id.btnClearLog);
        btnExportLog           = findViewById(R.id.btnLog);
        btnCopyLog             = findViewById(R.id.btnCopyLog);
        chkAutoScroll          = findViewById(R.id.chkAutoScroll);
        layoutTerminalContainer= findViewById(R.id.layoutTerminalContainer);

        // Quick chips
        btnChipPhpV            = findViewById(R.id.btnChipPhpV);
        btnChipPhpInfo         = findViewById(R.id.btnChipPhpInfo);
        btnChipFpmStatus       = findViewById(R.id.btnChipFpmStatus);
        btnChipNetstat         = findViewById(R.id.btnChipNetstat);
        btnChipLs              = findViewById(R.id.btnChipLs);
        btnChipErrorLog        = findViewById(R.id.btnChipErrorLog);

        // Collapsible containers
        configContent          = findViewById(R.id.configContent);
        databaseContent        = findViewById(R.id.databaseContent);
        assetsContent          = findViewById(R.id.assetsContent);
        ftpContent             = findViewById(R.id.ftpContent);

        txtCollapseConfig      = findViewById(R.id.txtCollapseIcon);
        txtCollapseDatabase    = findViewById(R.id.txtDatabaseCollapseIcon);
        txtCollapseAssets      = findViewById(R.id.txtAssetsCollapseIcon);
        txtCollapseFtp         = findViewById(R.id.txtFtpCollapseIcon);

        // Theme toggles
        btnThemeDark           = findViewById(R.id.btnThemeDark);
        btnThemeLight          = findViewById(R.id.btnThemeLight);
        btnThemeSystem         = findViewById(R.id.btnThemeSystem);

        // Config actions
        btnSaveConfig          = findViewById(R.id.btnSaveConfig);
        btnLoadConfig          = findViewById(R.id.btnLoadConfig);

        // Settings button → EditorActivity
        TextView btnSettings   = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
            }
        });

        txtTerminal.setMovementMethod(new ScrollingMovementMethod());
        txtTerminal.setTextIsSelectable(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Collapsible Sections
    // ─────────────────────────────────────────────────────────────────────────

    private void setupCollapsibles() {
        // All sections start collapsed
        collapseAll();

        LinearLayout headerConfig   = findViewById(R.id.headerConfig);
        LinearLayout headerDatabase = findViewById(R.id.headerDatabase);
        LinearLayout headerAssets   = findViewById(R.id.headerAssets);
        LinearLayout headerFtp      = findViewById(R.id.headerFtp);

        headerConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleSection(configContent, txtCollapseConfig, configExpanded = !configExpanded); }
        });
        headerDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleSection(databaseContent, txtCollapseDatabase, databaseExpanded = !databaseExpanded); }
        });
        headerAssets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleSection(assetsContent, txtCollapseAssets, assetsExpanded = !assetsExpanded); }
        });
        headerFtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleSection(ftpContent, txtCollapseFtp, ftpExpanded = !ftpExpanded); }
        });
    }

    private void collapseAll() {
        configContent.setVisibility(View.GONE);
        databaseContent.setVisibility(View.GONE);
        assetsContent.setVisibility(View.GONE);
        ftpContent.setVisibility(View.GONE);
    }

    private void toggleSection(LinearLayout content, TextView icon, boolean expanded) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        icon.setText(expanded ? "▼" : "▶");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Spinners
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSpinners() {
        String[] phpVersions = {"PHP 8.5.1 (fpm-fcgi)"};
        ArrayAdapter<String> phpAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phpVersions);
        phpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhpVersion.setAdapter(phpAdapter);

        String[] serverTypes = {"Apache HTTP Server"};
        ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serverTypes);
        serverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServerType.setAdapter(serverAdapter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PHP Mode Toggle (CLI ↔ FPM)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupPhpModeToggle() {
        radioGroupPhpMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean fpmSelected = (checkedId == R.id.radioFpm);
                layoutFpmSettings.setVisibility(fpmSelected ? View.VISIBLE : View.GONE);
            }
        });
        // Apply saved mode
        String mode = config.get(AppConfig.KEY_PHP_MODE, "fpm");
        if ("fpm".equals(mode)) {
            radioFpm.setChecked(true);
            layoutFpmSettings.setVisibility(View.VISIBLE);
        } else {
            radioCli.setChecked(true);
            layoutFpmSettings.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Event Listeners
    // ─────────────────────────────────────────────────────────────────────────

    private void setupListeners() {

        // ── Quick Action Bar ────────────────────────────────────────────────
        btnStartAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startAll(); }
        });

        btnTogglePhp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phpRunning) stopService(SCRIPT_STOP_FPM, "PHP-FPM", false, true, false);
                else startPhpFpm();
            }
        });

        btnToggleApache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (apacheRunning) stopService(SCRIPT_STOP_APACHE, "Apache", true, false, false);
                else startApache();
            }
        });

        btnToggleDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dbRunning) stopService(SCRIPT_STOP_DB, "MariaDB", false, false, true);
                else startDatabase();
            }
        });

        // ── Database ────────────────────────────────────────────────────────
        btnStartMySQL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startDatabase(); }
        });

        btnStopMySQL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stopService(SCRIPT_STOP_DB, "MariaDB", false, false, true); }
        });

        btnInstallPhpMyAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { installPhpMyAdmin(); }
        });

        btnOpenPhpMyAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openPhpMyAdmin(); }
        });

        // ── FTP ─────────────────────────────────────────────────────────────
        btnStartFtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startFtp(); }
        });

        btnStopFtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stopFtp(); }
        });

        // ── Assets ──────────────────────────────────────────────────────────
        btnCopyAssets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { copyAssetsFromExternalStorage(); }
        });

        btnDownloadZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { downloadAndExtractZip(); }
        });

        btnExtractZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { extractZipFile(); }
        });

        // ── Browse buttons ──────────────────────────────────────────────────
        btnSelectDocRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectDirectory(txtDocumentRoot); }
        });

        btnSelectHttpdConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectFile(txtHttpdConf); }
        });

        // ── Editor shortcut ──────────────────────────────────────────────────
        btnOpenEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
            }
        });

        // ── Config actions ──────────────────────────────────────────────────
        btnSaveConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveConfigFromUi(); }
        });

        btnLoadConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { loadConfigToUi(); }
        });

        // ── Terminal ────────────────────────────────────────────────────────
        btnExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { executeCommand(txtCommand.getText().toString()); }
        });

        txtCommand.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    executeCommand(txtCommand.getText().toString());
                    return true;
                }
                return false;
            }
        });

        btnClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logBuilder.setLength(0);
                txtTerminal.setText("");
                appendTerminal("Terminal cleared.");
            }
        });

        btnExportLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { exportLog(); }
        });

        btnCopyLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { copyLogToClipboard(); }
        });

        // ── Terminal show/hide ───────────────────────────────────────────────
        TextView btnToggleTerminal = findViewById(R.id.btnToggleTerminal);
        if (btnToggleTerminal != null) {
            btnToggleTerminal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutTerminalContainer.getVisibility() == View.VISIBLE) {
                        layoutTerminalContainer.setVisibility(View.GONE);
                        ((TextView) v).setText("▸ SHOW TERMINAL");
                    } else {
                        layoutTerminalContainer.setVisibility(View.VISIBLE);
                        ((TextView) v).setText("▾ HIDE TERMINAL");
                    }
                }
            });
        }

        // ── Quick chips ──────────────────────────────────────────────────────
        btnChipPhpV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { executeCommand("php -v"); }
        });
        btnChipPhpInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("php -r \"phpinfo();\" 2>&1 | head -30");
            }
        });
        btnChipFpmStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("ps aux | grep php-fpm | grep -v grep");
            }
        });
        btnChipNetstat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("cat /proc/net/tcp 2>/dev/null || netstat -tulpn 2>&1 | head -20");
            }
        });
        btnChipLs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("ls -la " + config.get(AppConfig.KEY_DOCUMENT_ROOT, DATA_DIR + "/www"));
            }
        });
        btnChipErrorLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("tail -30 " + config.get(AppConfig.KEY_ERROR_LOG, DATA_DIR + "/logs/apache2/error.log"));
            }
        });

        // ── Theme buttons ─────────────────────────────────────────────────────
        btnThemeDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTheme("dark"); }
        });
        btnThemeLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTheme("light"); }
        });
        btnThemeSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTheme("system"); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Service Management (via ScriptRunner)
    // ─────────────────────────────────────────────────────────────────────────

    /** Build env vars from current UI / config to pass to shell scripts. */
    private Map<String, String> buildEnv() {
        Map<String, String> env = new HashMap<>();
        // Apache / common
        env.put("APACHE_DOCROOT",    txtDocumentRoot.getText().toString().trim());
        env.put("APACHE_PORT",       txtPort.getText().toString().trim());
        env.put("APACHE_CONF",       txtHttpdConf.getText().toString().trim());
        // PHP-FPM
        env.put("FPM_PORT",          txtFpmPort != null ? txtFpmPort.getText().toString().trim() : "9000");
        env.put("FPM_MAX_CHILDREN",  txtFpmMaxChildren != null ? txtFpmMaxChildren.getText().toString().trim() : "5");
        env.put("FPM_SOCKET",        txtFpmSocket != null ? txtFpmSocket.getText().toString().trim() : "");
        // PHP settings
        env.put("PHP_MEMORY_LIMIT",  txtMemoryLimit.getText().toString().trim());
        env.put("PHP_MAX_EXEC_TIME", txtMaxExecTime != null ? txtMaxExecTime.getText().toString().trim() : "30");
        env.put("PHP_UPLOAD_MAX",    txtUploadMaxSize != null ? txtUploadMaxSize.getText().toString().trim() : "64M");
        env.put("PHP_DISPLAY_ERRORS",chkDisplayErrors.isChecked() ? "On" : "Off");
        env.put("PHP_ALLOW_URL_FOPEN",chkAllowUrlFopen.isChecked() ? "On" : "Off");
        env.put("PHP_OPCACHE",       chkOpcache != null && chkOpcache.isChecked() ? "1" : "0");
        // MariaDB
        env.put("MYSQL_PORT",        txtMysqlPort.getText().toString().trim());
        env.put("MYSQL_HOST",        txtMysqlHost.getText().toString().trim());
        // FTP
        if (txtFtpPort != null) env.put("FTP_PORT", txtFtpPort.getText().toString().trim());
        if (txtFtpRoot != null) env.put("FTP_ROOT", txtFtpRoot.getText().toString().trim());
        if (txtFtpUser != null) env.put("FTP_USER", txtFtpUser.getText().toString().trim());
        if (txtFtpPass != null) env.put("FTP_PASS", txtFtpPass.getText().toString().trim());
        return env;
    }

    private void startAll() {
        appendTerminal("[+] Starting all services...");
        Map<String, String> env = buildEnv();
        scriptRunner.run(SCRIPT_START_ALL, env, new ScriptRunner.OutputListener() {
            @Override public void onLine(String line) { appendTerminal(line); }
            @Override public void onDone(int code) {
                phpRunning = apacheRunning = dbRunning = (code == 0);
                updateAllStatusViews();
                appendTerminal(code == 0 ? "[+] All services started." : "[!] start_all.sh exited with code " + code);
            }
            @Override public void onError(String msg) { appendTerminal("[!] " + msg); }
        });
    }

    private void startApache() {
        appendTerminal("[+] Starting Apache...");
        Map<String, String> env = buildEnv();
        scriptRunner.run(SCRIPT_START_APACHE, env, new ScriptRunner.OutputListener() {
            @Override public void onLine(String line) { appendTerminal(line); }
            @Override public void onDone(int code) {
                apacheRunning = (code == 0);
                updateApacheStatusView();
                appendTerminal(code == 0 ? "[+] Apache started." : "[!] Apache exit code: " + code);
            }
            @Override public void onError(String msg) { appendTerminal("[!] Apache: " + msg); }
        });
        // optimistic — Apache forks and exits 0 quickly
        apacheRunning = true;
        updateApacheStatusView();
    }

    private void startPhpFpm() {
        appendTerminal("[+] Starting PHP-FPM...");
        Map<String, String> env = buildEnv();
        scriptRunner.run(SCRIPT_START_FPM, env, new ScriptRunner.OutputListener() {
            @Override public void onLine(String line) { appendTerminal(line); }
            @Override public void onDone(int code) {
                phpRunning = (code == 0);
                updatePhpStatusView();
                appendTerminal(code == 0 ? "[+] PHP-FPM started." : "[!] PHP-FPM exit code: " + code);
            }
            @Override public void onError(String msg) { appendTerminal("[!] PHP-FPM: " + msg); }
        });
        phpRunning = true;
        updatePhpStatusView();
    }

    private void startDatabase() {
        appendTerminal("[+] Starting MariaDB...");
        Map<String, String> env = buildEnv();
        scriptRunner.run(SCRIPT_START_DB, env, new ScriptRunner.OutputListener() {
            @Override public void onLine(String line) { appendTerminal(line); }
            @Override public void onDone(int code) {
                dbRunning = (code == 0);
                updateDbStatusView();
                appendTerminal(code == 0 ? "[+] MariaDB started." : "[!] MariaDB exit code: " + code);
            }
            @Override public void onError(String msg) { appendTerminal("[!] MariaDB: " + msg); }
        });
        dbRunning = true;
        updateDbStatusView();
    }

    private void stopService(String stopScript, final String name,
                              final boolean isApache, final boolean isPhp, final boolean isDb) {
        appendTerminal("[+] Stopping " + name + "...");
        scriptRunner.run(stopScript, buildEnv(), new ScriptRunner.OutputListener() {
            @Override public void onLine(String line) { appendTerminal(line); }
            @Override public void onDone(int code) {
                if (isApache) { apacheRunning = false; updateApacheStatusView(); }
                if (isPhp)    { phpRunning    = false; updatePhpStatusView(); }
                if (isDb)     { dbRunning     = false; updateDbStatusView(); }
                appendTerminal("[+] " + name + " stopped.");
            }
            @Override public void onError(String msg) { appendTerminal("[!] " + msg); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Status View Updates
    // ─────────────────────────────────────────────────────────────────────────

    private void updateAllStatusViews() {
        updatePhpStatusView();
        updateApacheStatusView();
        updateDbStatusView();
        updateMasterStatus();
    }

    private void updateMasterStatus() {
        boolean anyRunning = phpRunning || apacheRunning || dbRunning;
        txtServerStatus.setText(anyRunning ? "● ONLINE" : "● OFFLINE");
        txtServerStatus.setTextColor(anyRunning
                ? getResources().getColor(android.R.color.holo_green_dark)
                : getResources().getColor(android.R.color.holo_red_dark));
    }

    private void updatePhpStatusView() {
        btnTogglePhp.setText(phpRunning ? "■ PHP-FPM" : "▶ PHP-FPM");
        int color = phpRunning
                ? getResources().getColor(android.R.color.holo_green_dark)
                : 0xFF586069;
        btnTogglePhp.setBackgroundColor(color);
        updateMasterStatus();
    }

    private void updateApacheStatusView() {
        btnToggleApache.setText(apacheRunning ? "■ Apache" : "▶ Apache");
        int color = apacheRunning
                ? 0xFF1F6FEB
                : 0xFF586069;
        btnToggleApache.setBackgroundColor(color);
        updateMasterStatus();
    }

    private void updateDbStatusView() {
        btnToggleDb.setText(dbRunning ? "■ Database" : "▶ Database");
        int color = dbRunning
                ? 0xFF9E6A03
                : 0xFF586069;
        btnToggleDb.setBackgroundColor(color);

        if (txtMysqlStatus != null) {
            txtMysqlStatus.setText(dbRunning ? "● ONLINE" : "● OFFLINE");
            txtMysqlStatus.setTextColor(dbRunning
                    ? getResources().getColor(android.R.color.holo_green_dark)
                    : getResources().getColor(android.R.color.holo_red_dark));
        }
        updateMasterStatus();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FTP (stub — wire to actual FTP script when ready)
    // ─────────────────────────────────────────────────────────────────────────

    private void startFtp() {
        saveConfigFromUi(); // persist FTP settings first
        appendTerminal("[+] FTP server start — wire start_ftp.sh when ready");
        Toast.makeText(this, "FTP: stub — add start_ftp.sh to assets/scripts", Toast.LENGTH_LONG).show();
        if (txtFtpStatus != null) {
            txtFtpStatus.setText("● RUNNING");
            txtFtpStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void stopFtp() {
        appendTerminal("[+] FTP server stop — stub");
        if (txtFtpStatus != null) {
            txtFtpStatus.setText("● STOPPED");
            txtFtpStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  phpMyAdmin
    // ─────────────────────────────────────────────────────────────────────────

    private void installPhpMyAdmin() {
        appendTerminal("[+] phpMyAdmin — download it from https://www.phpmyadmin.net/");
        appendTerminal("[+] Extract into: " + config.get(AppConfig.KEY_DOCUMENT_ROOT, DATA_DIR + "/www") + "/phpmyadmin/");
        Toast.makeText(this, "Download phpMyAdmin and extract to htdocs/phpmyadmin", Toast.LENGTH_LONG).show();
    }

    private void openPhpMyAdmin() {
        String port = txtPort.getText().toString().trim();
        String url  = "http://127.0.0.1:" + port + "/phpmyadmin";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        appendTerminal("[+] Opening phpMyAdmin: " + url);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Config — save / load
    // ─────────────────────────────────────────────────────────────────────────

    private void saveConfigFromUi() {
        config.set(AppConfig.KEY_DOCUMENT_ROOT,   txtDocumentRoot.getText().toString());
        config.set(AppConfig.KEY_APACHE_PORT,      txtPort.getText().toString());
        config.set(AppConfig.KEY_PHP_FPM_PORT,     txtFpmPort != null ? txtFpmPort.getText().toString() : "9000");
        config.set(AppConfig.KEY_MYSQL_PORT,       txtMysqlPort.getText().toString());
        config.set(AppConfig.KEY_MYSQL_HOST,       txtMysqlHost.getText().toString());
        config.set(AppConfig.KEY_MEMORY_LIMIT,     txtMemoryLimit.getText().toString());
        config.set(AppConfig.KEY_MAX_EXEC_TIME,    txtMaxExecTime != null ? txtMaxExecTime.getText().toString() : "30");
        config.set(AppConfig.KEY_UPLOAD_MAX,       txtUploadMaxSize != null ? txtUploadMaxSize.getText().toString() : "64M");
        config.set(AppConfig.KEY_DISPLAY_ERRORS,   chkDisplayErrors.isChecked());
        config.set(AppConfig.KEY_ALLOW_URL_FOPEN,  chkAllowUrlFopen.isChecked());
        config.set(AppConfig.KEY_OPCACHE,          chkOpcache != null && chkOpcache.isChecked());
        config.set(AppConfig.KEY_PHP_VERSION_IDX,  spinnerPhpVersion.getSelectedItemPosition());
        config.set(AppConfig.KEY_SERVER_TYPE_IDX,  spinnerServerType.getSelectedItemPosition());
        config.set(AppConfig.KEY_PHP_MODE,         radioFpm != null && radioFpm.isChecked() ? "fpm" : "cli");
        config.set(AppConfig.KEY_FPM_MAX_CHILDREN, txtFpmMaxChildren != null ? txtFpmMaxChildren.getText().toString() : "5");
        config.set(AppConfig.KEY_FPM_SOCKET,       txtFpmSocket != null ? txtFpmSocket.getText().toString() : "");
        config.set(AppConfig.KEY_ERROR_LOG,        txtErrorLog.getText().toString());
        config.set(AppConfig.KEY_HTTPD_CONF,       txtHttpdConf.getText().toString());
        // FTP
        if (txtFtpPort != null) config.set(AppConfig.KEY_FTP_PORT, txtFtpPort.getText().toString());
        if (txtFtpRoot != null) config.set(AppConfig.KEY_FTP_ROOT, txtFtpRoot.getText().toString());
        if (txtFtpUser != null) config.set(AppConfig.KEY_FTP_USER, txtFtpUser.getText().toString());
        if (txtFtpPass != null) config.set(AppConfig.KEY_FTP_PASS, txtFtpPass.getText().toString());
        if (chkFtpAnonymous != null) config.set(AppConfig.KEY_FTP_ANON, chkFtpAnonymous.isChecked());

        boolean ok = config.save();
        appendTerminal(ok ? "[+] Config saved to app_config.json" : "[!] Failed to save config");
        Toast.makeText(this, ok ? "Config saved" : "Save failed", Toast.LENGTH_SHORT).show();

        savePhpIni();
    }

    private void loadConfigToUi() {
        txtDocumentRoot.setText(config.get(AppConfig.KEY_DOCUMENT_ROOT, DATA_DIR + "/www"));
        txtPort.setText(config.get(AppConfig.KEY_APACHE_PORT, "8080"));
        if (txtFpmPort != null) txtFpmPort.setText(config.get(AppConfig.KEY_PHP_FPM_PORT, "9000"));
        txtMysqlPort.setText(config.get(AppConfig.KEY_MYSQL_PORT, "3307"));
        txtMysqlHost.setText(config.get(AppConfig.KEY_MYSQL_HOST, "127.0.0.1"));
        txtMemoryLimit.setText(config.get(AppConfig.KEY_MEMORY_LIMIT, "128M"));
        if (txtMaxExecTime != null) txtMaxExecTime.setText(config.get(AppConfig.KEY_MAX_EXEC_TIME, "30"));
        if (txtUploadMaxSize != null) txtUploadMaxSize.setText(config.get(AppConfig.KEY_UPLOAD_MAX, "64M"));
        chkDisplayErrors.setChecked(config.getBoolean(AppConfig.KEY_DISPLAY_ERRORS, true));
        chkAllowUrlFopen.setChecked(config.getBoolean(AppConfig.KEY_ALLOW_URL_FOPEN, true));
        if (chkOpcache != null) chkOpcache.setChecked(config.getBoolean(AppConfig.KEY_OPCACHE, false));
        spinnerPhpVersion.setSelection(config.getInt(AppConfig.KEY_PHP_VERSION_IDX, 0));
        spinnerServerType.setSelection(config.getInt(AppConfig.KEY_SERVER_TYPE_IDX, 0));
        if (txtFpmMaxChildren != null) txtFpmMaxChildren.setText(config.get(AppConfig.KEY_FPM_MAX_CHILDREN, "5"));
        if (txtFpmSocket != null) txtFpmSocket.setText(config.get(AppConfig.KEY_FPM_SOCKET, ""));
        txtErrorLog.setText(config.get(AppConfig.KEY_ERROR_LOG, DATA_DIR + "/logs/apache2/error.log"));
        txtHttpdConf.setText(config.get(AppConfig.KEY_HTTPD_CONF, DATA_DIR + "/Apache/apache2/httpd.conf"));
        // PHP mode
        String phpMode = config.get(AppConfig.KEY_PHP_MODE, "fpm");
        if (radioFpm != null && "fpm".equals(phpMode)) radioFpm.setChecked(true);
        else if (radioCli != null) radioCli.setChecked(true);
        // FTP
        if (txtFtpPort != null) txtFtpPort.setText(config.get(AppConfig.KEY_FTP_PORT, "2121"));
        if (txtFtpRoot != null) txtFtpRoot.setText(config.get(AppConfig.KEY_FTP_ROOT, DATA_DIR + "/www"));
        if (txtFtpUser != null) txtFtpUser.setText(config.get(AppConfig.KEY_FTP_USER, "admin"));
        if (txtFtpPass != null) txtFtpPass.setText(config.get(AppConfig.KEY_FTP_PASS, "admin"));
        if (chkFtpAnonymous != null) chkFtpAnonymous.setChecked(config.getBoolean(AppConfig.KEY_FTP_ANON, false));

        appendTerminal("[+] Config loaded from app_config.json");
    }

    private void savePhpIni() {
        try {
            File phpIni = new File(DATA_DIR, "php_custom.ini");
            StringBuilder ini = new StringBuilder();
            ini.append("memory_limit = ").append(txtMemoryLimit.getText()).append("\n");
            ini.append("max_execution_time = ").append(txtMaxExecTime != null ? txtMaxExecTime.getText() : "30").append("\n");
            ini.append("upload_max_filesize = ").append(txtUploadMaxSize != null ? txtUploadMaxSize.getText() : "64M").append("\n");
            ini.append("post_max_size = ").append(txtUploadMaxSize != null ? txtUploadMaxSize.getText() : "64M").append("\n");
            ini.append("display_errors = ").append(chkDisplayErrors.isChecked() ? "On" : "Off").append("\n");
            ini.append("allow_url_fopen = ").append(chkAllowUrlFopen.isChecked() ? "On" : "Off").append("\n");
            if (chkOpcache != null) {
                ini.append("opcache.enable = ").append(chkOpcache.isChecked() ? "1" : "0").append("\n");
            }
            ini.append("error_log = ").append(txtErrorLog.getText()).append("\n");

            FileOutputStream fos = new FileOutputStream(phpIni);
            fos.write(ini.toString().getBytes());
            fos.close();
            appendTerminal("[+] php_custom.ini updated");
        } catch (Exception e) {
            appendTerminal("[!] Failed to write php_custom.ini: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Theme Switching
    // ─────────────────────────────────────────────────────────────────────────

    private void switchTheme(String mode) {
        config.set(AppConfig.KEY_THEME, mode);
        config.save();
        // Restart activity to apply new theme
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Terminal
    // ─────────────────────────────────────────────────────────────────────────

    private void appendTerminal(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logBuilder.append(text).append("\n");
                txtTerminal.append(text + "\n");
                if (chkAutoScroll != null && chkAutoScroll.isChecked() && scrollViewTerminal != null) {
                    scrollViewTerminal.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollViewTerminal.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private void exportLog() {
        try {
            File logFile = new File(Environment.getExternalStorageDirectory(), "php_server_log.txt");
            FileOutputStream fos = new FileOutputStream(logFile);
            fos.write(logBuilder.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Log exported to: " + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            appendTerminal("[+] Log exported: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            appendTerminal("[!] Export failed: " + e.getMessage());
        }
    }

    private void copyLogToClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("terminal_log", logBuilder.toString());
        cm.setPrimaryClip(clip);
        Toast.makeText(this, "Terminal log copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Shell Command Execution
    // ─────────────────────────────────────────────────────────────────────────

    private void executeCommand(final String command) {
        if (command == null || command.trim().isEmpty()) {
            appendTerminal("No command entered");
            return;
        }
        appendTerminal("\n$ " + command);
        txtCommand.setText("");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File phpBinary = new File(DATA_DIR, "bin/php");
                    String ldPath  = DATA_DIR + "/lib/common:" + DATA_DIR + "/lib/php";
                    File tmpDir    = new File(DATA_DIR, "tmp");
                    if (!tmpDir.exists()) tmpDir.mkdirs();

                    String finalCmd = command;
                    if (command.startsWith("php") && phpBinary.exists()) {
                        finalCmd = command.replaceFirst("^php", phpBinary.getAbsolutePath());
                    }

                    ProcessBuilder pb = new ProcessBuilder("sh", "-c", finalCmd);
                    pb.environment().put("LD_LIBRARY_PATH", ldPath);
                    pb.environment().put("TMPDIR", tmpDir.getAbsolutePath());
                    pb.environment().put("TEMP", tmpDir.getAbsolutePath());
                    pb.environment().put("TMP",  tmpDir.getAbsolutePath());
                    pb.directory(getFilesDir());
                    pb.redirectErrorStream(true);

                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean hasOutput = false;
                    while ((line = reader.readLine()) != null) {
                        final String l = line;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { appendTerminal(l); }
                        });
                        hasOutput = true;
                    }

                    final int code = process.waitFor();
                    final boolean ho = hasOutput;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!ho) appendTerminal("[exit " + code + "]");
                        }
                    });
                } catch (Exception e) {
                    appendTerminal("[!] " + e.getMessage());
                }
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Asset Management
    // ─────────────────────────────────────────────────────────────────────────

    private void copyAssetsFromExternalStorage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File src  = new File("/storage/emulated/0/assets/");
                    File dest = getFilesDir();

                    if (!src.exists()) {
                        appendTerminal("[!] /storage/emulated/0/assets/ not found");
                        return;
                    }
                    appendTerminal("[+] Copying from " + src + " → " + dest);
                    copyDirectory(src, dest);
                    setPermissions(dest);
                    appendTerminal("[+] Copy complete");
                } catch (Exception e) {
                    appendTerminal("[!] Copy error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void downloadAndExtractZip() {
        final String url      = txtZipUrl.getText().toString().trim();
        final String destPath = txtZipPath.getText().toString().trim();
        if (url.isEmpty() || destPath.isEmpty()) {
            appendTerminal("[!] Enter URL and destination path");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() { progressDownload.setVisibility(View.VISIBLE); progressDownload.setProgress(0); }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendTerminal("[+] Downloading: " + url);
                    File dest = new File(destPath);
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(60000);
                    final long total = conn.getContentLength();
                    InputStream in  = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(dest);
                    byte[] buf = new byte[8192];
                    int n; long downloaded = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (total > 0) {
                            final int pct = (int)(downloaded * 100 / total);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { progressDownload.setProgress(pct); }
                            });
                        }
                    }
                    out.close(); in.close();
                    appendTerminal("[+] Download complete: " + dest.getAbsolutePath());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { progressDownload.setProgress(100); }
                    });
                    extractZipFile();
                } catch (Exception e) {
                    appendTerminal("[!] Download error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { progressDownload.setVisibility(View.GONE); }
                    });
                }
            }
        }).start();
    }

    private void extractZipFile() {
        final String zipPath = txtZipPath.getText().toString().trim();
        if (zipPath.isEmpty()) { appendTerminal("[!] Enter ZIP path"); return; }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File zipFile = new File(zipPath);
                    if (!zipFile.exists()) { appendTerminal("[!] ZIP not found: " + zipPath); return; }
                    appendTerminal("[+] Extracting: " + zipPath);
                    File out = getFilesDir();
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            File f = new File(out, entry.getName());
                            if (entry.isDirectory()) { f.mkdirs(); }
                            else {
                                if (f.getParentFile() != null) f.getParentFile().mkdirs();
                                FileOutputStream fos = new FileOutputStream(f);
                                byte[] buf = new byte[8192]; int n;
                                while ((n = zis.read(buf)) != -1) fos.write(buf, 0, n);
                                fos.close();
                            }
                            zis.closeEntry();
                            appendTerminal("[+] " + entry.getName());
                        }
                    }
                    setPermissions(out);
                    appendTerminal("[+] Extraction complete");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { progressDownload.setVisibility(View.GONE); }
                    });
                } catch (Exception e) {
                    appendTerminal("[!] Extract error: " + e.getMessage());
                }
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  File / Directory Picker
    // ─────────────────────────────────────────────────────────────────────────

    private void selectDirectory(EditText target) {
        currentEditText = target;
        startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_DIR_SELECT);
    }

    private void selectFile(EditText target) {
        currentEditText = target;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_FILE_SELECT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && currentEditText != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.getPath();
                // Strip SAF prefix like /tree/primary: → /storage/emulated/0/
                if (path != null && path.contains(":")) {
                    path = path.replace("/tree/primary:", "/storage/emulated/0/");
                    path = path.replace("/document/primary:", "/storage/emulated/0/");
                }
                currentEditText.setText(path);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Permissions
    // ─────────────────────────────────────────────────────────────────────────

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                     Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERM);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQUEST_STORAGE_PERM) {
            boolean granted = results.length > 0
                    && results[0] == PackageManager.PERMISSION_GRANTED;
            appendTerminal(granted ? "[+] Storage permission granted"
                                   : "[!] Storage permission denied");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private void checkAssets() {
        File www = new File(getFilesDir(), "www");
        if (www.exists()) {
            appendTerminal("[+] www/ found: " + www.getAbsolutePath());
        } else {
            appendTerminal("[!] www/ not found — copy or extract assets first");
        }
    }

    private void copyDirectory(File src, File dst) throws IOException {
        if (!dst.exists()) dst.mkdirs();
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            File d = new File(dst, f.getName());
            if (f.isDirectory()) copyDirectory(f, d);
            else if (!d.exists()) copyFile(f, d);
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        if (dst.getParentFile() != null) dst.getParentFile().mkdirs();
        InputStream in   = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        out.flush(); out.close(); in.close();
    }

    private void setPermissions(File dir) {
        File php   = new File(dir, "bin/php");
        File httpd = new File(dir, "bin/httpd");
        File fpm   = new File(dir, "bin/php-fpm");
        File db    = new File(dir, "db/bin/mariadbd");
        for (File b : new File[]{php, httpd, fpm, db}) {
            if (b.exists()) {
                b.setExecutable(true, false);
                appendTerminal("[+] chmod +x " + b.getName());
            }
        }
        setLibPermissions(new File(dir, "lib"));
        setLibPermissions(new File(dir, "scripts"));
    }

    private void setLibPermissions(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) { f.setExecutable(true, false); f.setReadable(true, false); }
            else if (f.isDirectory()) setLibPermissions(f);
        }
    }
}