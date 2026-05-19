package com.lodwickmasete.php;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * SetupActivity — multi-step wizard for installing / uninstalling server components.
 *
 * setup.json (bundled in assets) controls behaviour:
 * {
 *   "net_installer": true,           // true  → download master zip first
 *   "download_url": "https://…",    // master zip URL (net installer only)
 *   "version": "1.0"
 * }
 *
 * Component detection uses folder/file existence:
 *   Apache    → files/Apache/
 *   PHP-FPM   → files/fpm/
 *   MariaDB   → files/db/
 *   phpMyAdmin→ files/phpmyadmin/
 *   FTP       → files/ftp/          (placeholder — not yet implemented)
 *   WordPress → files/wordpress/    (placeholder)
 *
 * The activity never auto-finishes; the user must press CANCEL / back.
 */
public class SetupActivity extends Activity {

    // ─── Steps ────────────────────────────────────────────────────────────────
    private static final int STEP_DOWNLOAD  = 0;   // net installer only
    private static final int STEP_WELCOME   = 1;
    private static final int STEP_COMPONENTS= 2;
    private static final int STEP_REVIEW    = 3;

    // ─── Component IDs (also used as sub-dir names) ───────────────────────────
    private static final String COMP_APACHE  = "Apache";
    private static final String COMP_FPM     = "fpm";
    private static final String COMP_MYSQL   = "db";
    private static final String COMP_PMA     = "phpmyadmin";
    private static final String COMP_FTP     = "ftp";
    private static final String COMP_WP      = "wordpress";


    private static final String LIB_ZIP = "lib.zip";

    // ─── UI ──────────────────────────────────────────────────────────────────
    private LinearLayout contentArea;
    private TextView btnBack, btnNext, btnInstall, btnCancel;
    private TextView step1Ind, step2Ind, step3Ind;

    // ─── State ───────────────────────────────────────────────────────────────
    private int currentStep;
    private boolean isNetInstaller  = false;
    private String  downloadUrl     = "";
    private boolean masterZipReady  = false;   // download complete

    // user choices (checked = want installed; will be compared to current state)
    private boolean wantApache = true;
    private boolean wantFpm    = true;
    private boolean wantMysql  = true;
    private boolean wantPma    = true;
    private boolean wantFtp    = false;
    private boolean wantWp     = false;

    private SharedPreferences prefs;
    private AppConfig appConfig;

    // ─── onCreate ────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        prefs     = getSharedPreferences("setup", MODE_PRIVATE);
        appConfig = AppConfig.load(this);

        readSetupJson();   // detect net vs bundled
        initViews();

        if (isNetInstaller && !masterZipReady) {
            currentStep = STEP_DOWNLOAD;
        } else {
            currentStep = STEP_WELCOME;
        }
        showStep(currentStep);
    }

    // ─── setup.json ──────────────────────────────────────────────────────────
    private void readSetupJson() {
        try {
            InputStream is = getAssets().open("setup.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject j = new JSONObject(sb.toString());
            isNetInstaller = j.optBoolean("net_installer", false);
            downloadUrl    = j.optString("download_url", "");
        } catch (Exception e) {
            // no setup.json → treat as bundled
            isNetInstaller = false;
        }
    }

    // ─── initViews ───────────────────────────────────────────────────────────
    private void initViews() {
        contentArea = (LinearLayout) findViewById(R.id.contentArea);
        btnBack     = (TextView)     findViewById(R.id.btnBack);
        btnNext     = (TextView)     findViewById(R.id.btnNext);
        btnInstall  = (TextView)     findViewById(R.id.btnInstall);
        btnCancel   = (TextView)     findViewById(R.id.btnCancel);
        step1Ind    = (TextView)     findViewById(R.id.step1Indicator);
        step2Ind    = (TextView)     findViewById(R.id.step2Indicator);
        step3Ind    = (TextView)     findViewById(R.id.step3Indicator);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { navigateBack(); }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { navigateNext(); }
        });
        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startInstallation(); }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    // ─── Navigation ──────────────────────────────────────────────────────────
    private void navigateBack() {
        if (currentStep == STEP_WELCOME) return;          // nothing before welcome
        if (currentStep == STEP_COMPONENTS) {
            currentStep = STEP_WELCOME;
        } else if (currentStep == STEP_REVIEW) {
            currentStep = STEP_COMPONENTS;
        }
        showStep(currentStep);
    }

    private void navigateNext() {
        if (currentStep == STEP_WELCOME) {
            if (!validateStep1()) return;
            currentStep = STEP_COMPONENTS;
        } else if (currentStep == STEP_COMPONENTS) {
            if (!validateStep2()) return;
            currentStep = STEP_REVIEW;
        }
        showStep(currentStep);
    }

    // ─── showStep ─────────────────────────────────────────────────────────────
    private void showStep(int step) {
        contentArea.removeAllViews();
        updateIndicators(step);
        updateButtons(step);

        switch (step) {
            case STEP_DOWNLOAD:  buildDownloadStep();    break;
            case STEP_WELCOME:   buildWelcomeStep();     break;
            case STEP_COMPONENTS:buildComponentsStep();  break;
            case STEP_REVIEW:    buildReviewStep();      break;
        }
    }

    private void updateButtons(int step) {
        if (step == STEP_DOWNLOAD) {
            btnBack.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
            btnInstall.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);
        } else if (step == STEP_WELCOME) {
            btnBack.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
            btnInstall.setVisibility(View.GONE);
            btnNext.setText("NEXT \u2192");
        } else if (step == STEP_COMPONENTS) {
            btnBack.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);
            btnInstall.setVisibility(View.GONE);
            btnNext.setText("NEXT \u2192");
        } else if (step == STEP_REVIEW) {
            btnBack.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
            btnInstall.setVisibility(View.VISIBLE);
        }
    }

    private void updateIndicators(int step) {
        // step numbers for indicator: WELCOME=1, COMPONENTS=2, REVIEW=3
        int vis = step == STEP_DOWNLOAD ? 0 : step; // DOWNLOAD shows no indicator highlight
        step1Ind.setBackgroundResource(vis >= STEP_WELCOME   ? R.drawable.button_state : R.drawable.button_secondary);
        step2Ind.setBackgroundResource(vis >= STEP_COMPONENTS? R.drawable.button_state : R.drawable.button_secondary);
        step3Ind.setBackgroundResource(vis >= STEP_REVIEW    ? R.drawable.button_state : R.drawable.button_secondary);
        step1Ind.setTextColor(getResources().getColor(vis >= STEP_WELCOME    ? android.R.color.white : R.color.text_secondary));
        step2Ind.setTextColor(getResources().getColor(vis >= STEP_COMPONENTS ? android.R.color.white : R.color.text_secondary));
        step3Ind.setTextColor(getResources().getColor(vis >= STEP_REVIEW     ? android.R.color.white : R.color.text_secondary));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 0 — Download master zip (net installer only)
    // ═════════════════════════════════════════════════════════════════════════
    private android.widget.ProgressBar downloadProgress;
    private TextView downloadStatus;

    private void buildDownloadStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, 0);

        layout.addView(createSectionTitle("\u2193 DOWNLOAD ASSETS"));

        downloadStatus = new TextView(this);
        downloadStatus.setText("Assets will be downloaded from GitHub.\nThis may take a few minutes on a slow connection.");
        downloadStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        downloadStatus.setTextSize(10);
        downloadStatus.setTypeface(Typeface.MONOSPACE);
        downloadStatus.setBackgroundResource(R.drawable.button_secondary);
        downloadStatus.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        downloadStatus.setLayoutParams(lp);
        layout.addView(downloadStatus);

        // URL display
        TextView urlLabel = createLabel("SOURCE URL");
        layout.addView(urlLabel);
        TextView urlVal = new TextView(this);
        urlVal.setText(downloadUrl.isEmpty() ? "(no URL in setup.json)" : downloadUrl);
        urlVal.setTextColor(getResources().getColor(R.color.accent));
        urlVal.setTextSize(9);
        urlVal.setTypeface(Typeface.MONOSPACE);
        urlVal.setPadding(0, 4, 0, 16);
        layout.addView(urlVal);

        // Progress bar
        downloadProgress = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        downloadProgress.setMax(100);
        downloadProgress.setProgress(0);
        downloadProgress.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24));
        layout.addView(downloadProgress);

        // Start button
        TextView btnDownload = new TextView(this);
        btnDownload.setText("START DOWNLOAD");
        btnDownload.setTextColor(getResources().getColor(android.R.color.white));
        btnDownload.setTextSize(12);
        btnDownload.setTypeface(Typeface.MONOSPACE);
        btnDownload.setGravity(android.view.Gravity.CENTER);
        btnDownload.setBackgroundResource(R.drawable.button_state);
        btnDownload.setPadding(20, 14, 20, 14);
        btnDownload.setClickable(true);
        btnDownload.setFocusable(true);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 24, 0, 0);
        btnDownload.setLayoutParams(btnLp);
        final TextView btnRef = btnDownload;
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                btnRef.setEnabled(false);
                btnRef.setText("DOWNLOADING...");
                downloadMasterZip();
            }
        });
        layout.addView(btnDownload);

        contentArea.addView(layout);
    }

    private void downloadMasterZip() {
        final String url = downloadUrl;
        if (url.isEmpty()) {
            setDownloadStatus("Error: no download URL configured in setup.json");
            return;
        }

        new Thread(new Runnable() {
            @Override public void run() {
                InputStream in = null;
                FileOutputStream out = null;
                File dest = null;

                try {
                    File appDir = getFilesDir();
                    String fileName = "assets_master.zip";
                    if (url.contains("/")) {
                        String s = url.substring(url.lastIndexOf("/") + 1);
                        if (!s.isEmpty()) fileName = s;
                    }
                    dest = new File(appDir, fileName);

                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(120000);
                    conn.connect();

                    final long total = conn.getContentLengthLong();
                    in = conn.getInputStream();
                    out = new FileOutputStream(dest);

                    byte[] buf = new byte[8192];
                    int n;
                    long downloaded = 0;

                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (total > 0) {
                            final int pct = (int) (downloaded * 100 / total);
                            setDownloadProgress(pct);
                        }
                    }
                    out.flush();
                    setDownloadProgress(100);
                    setDownloadStatus("Download complete. Extracting master archive...");

                    // Extract the master zip — it contains component zips + folders
                    extractZipTo(dest, getFilesDir());

                    // Clean up the downloaded zip
                    dest.delete();

                    masterZipReady = true;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setDownloadStatus("Assets ready. Proceeding to setup...");
                            currentStep = STEP_WELCOME;
                            showStep(currentStep);
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setDownloadStatus("Download failed: " + e.getMessage() +
                                    "\n\nCheck your internet connection and try again.");
                        }
                    });
                } finally {
                    try { if (in  != null) in.close();  } catch (Exception ignored) {}
                    try { if (out != null) out.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    private void setDownloadProgress(final int pct) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (downloadProgress != null) downloadProgress.setProgress(pct);
            }
        });
    }

    private void setDownloadStatus(final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (downloadStatus != null) downloadStatus.setText(msg);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 1 — Welcome / path
    // ═════════════════════════════════════════════════════════════════════════
    private void buildWelcomeStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createSectionTitle("\u2665 AMPDROID SERVER SETUP"));

        // Info card
        StringBuilder info = new StringBuilder();
        info.append("Storage Required: ~").append(calculateTotalWantedSize()).append(" MB\n\n");
        info.append("Components available:\n");
        info.append("  \u2022 Apache HTTP Server\n");
        info.append("  \u2022 PHP-FPM Interpreter\n");
        info.append("  \u2022 MariaDB Database\n");
        info.append("  \u2022 phpMyAdmin (optional)\n");
        info.append("  \u2022 FTP Server (optional)\n");
        info.append("  \u2022 WordPress (placeholder)\n\n");
        info.append("Already installed components will be shown\n");
        info.append("on the next screen. You may install or remove\n");
        info.append("individual components at any time.");

        TextView infoCard = new TextView(this);
        infoCard.setText(info.toString());
        infoCard.setTextColor(getResources().getColor(R.color.text_secondary));
        infoCard.setTextSize(10);
        infoCard.setTypeface(Typeface.MONOSPACE);
        infoCard.setBackgroundResource(R.drawable.button_secondary);
        infoCard.setPadding(16, 16, 16, 16);
        infoCard.setLayoutParams(matchWrap(0, 0, 0, 12));
        layout.addView(infoCard);

        // Install path (read-only display)
        layout.addView(createLabel("INSTALLATION PATH"));

        LinearLayout pathRow = new LinearLayout(this);
        pathRow.setOrientation(LinearLayout.HORIZONTAL);

        final EditText txtPath = new EditText(this);
        txtPath.setText(getFilesDir().getAbsolutePath());
        txtPath.setTextColor(getResources().getColor(R.color.text_primary));
        txtPath.setTextSize(10);
        txtPath.setTypeface(Typeface.MONOSPACE);
        txtPath.setBackgroundResource(R.drawable.button_secondary);
        txtPath.setPadding(12, 10, 12, 10);
        txtPath.setEnabled(false);
        LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        txtPath.setLayoutParams(pathLp);
        pathRow.addView(txtPath);

        layout.addView(pathRow);

        // Free space
        File filesDir = getFilesDir();
        long freeBytes = filesDir.getFreeSpace();
        long freeMb = freeBytes / (1024 * 1024);

        TextView spaceInfo = new TextView(this);
        spaceInfo.setText("Free space: " + freeMb + " MB");
        spaceInfo.setTextColor(freeMb > 300
                ? getResources().getColor(R.color.status_green)
                : getResources().getColor(R.color.warning));
        spaceInfo.setTextSize(10);
        spaceInfo.setTypeface(Typeface.MONOSPACE);
        spaceInfo.setPadding(0, 8, 0, 0);
        layout.addView(spaceInfo);

        // Installer type badge
        TextView badge = new TextView(this);
        badge.setText(isNetInstaller
                ? "\u2601  NET INSTALLER  —  assets fetched from GitHub"
                : "\ud83d\udce6  BUNDLED INSTALLER  —  assets included in APK");
        badge.setTextColor(getResources().getColor(isNetInstaller ? R.color.accent : R.color.status_green));
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setBackgroundResource(R.drawable.button_secondary);
        badge.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams badgeLp = matchWrap(0, 20, 0, 0);
        badge.setLayoutParams(badgeLp);
        layout.addView(badge);

        contentArea.addView(layout);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 2 — Component selection
    // ═════════════════════════════════════════════════════════════════════════
    private CheckBox chkApache, chkFpm, chkMysql, chkPma, chkFtp, chkWp;

    private void buildComponentsStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createSectionTitle("\ud83d\udd27 SELECT COMPONENTS"));

        // Helper: show installed state in description
        layout.addView(buildComponentRow(COMP_APACHE,
                "Apache HTTP Server",
                "Web server — required for PHP execution",
                wantApache, true /* required */));

        layout.addView(buildComponentRow(COMP_FPM,
                "PHP-FPM Interpreter",
                "PHP FastCGI process manager — required",
                wantFpm, true));

        layout.addView(buildComponentRow(COMP_MYSQL,
                "MariaDB Database",
                "MySQL-compatible database server",
                wantMysql, false));

        layout.addView(buildComponentRow(COMP_PMA,
                "phpMyAdmin",
                "Web-based database management UI",
                wantPma, false));

        layout.addView(buildComponentRow(COMP_FTP,
                "FTP Server",
                "File transfer protocol server (placeholder)",
                wantFtp, false));

        layout.addView(buildComponentRow(COMP_WP,
                "WordPress",
                "WordPress CMS (placeholder — not yet available)",
                wantWp, false));

        // Size summary
        TextView sizeInfo = new TextView(this);
        sizeInfo.setText("Estimated install size: ~" + calculateTotalWantedSize() + " MB");
        sizeInfo.setTextColor(getResources().getColor(R.color.text_primary));
        sizeInfo.setTextSize(10);
        sizeInfo.setTypeface(Typeface.MONOSPACE);
        sizeInfo.setPadding(0, 16, 0, 0);
        sizeInfo.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.addView(sizeInfo);

        contentArea.addView(layout);
    }

    /**
     * Builds one row in the component list.
     * The checkbox is pre-ticked based on:
     *   - wantXxx (user's remembered choice) if NOT yet installed
     *   - isInstalled(comp)                  overrides to checked + green label
     * Required components (Apache, PHP) are locked to checked.
     */
    private LinearLayout buildComponentRow(final String comp, String title,
                                           String description, boolean wanted,
                                           final boolean required) {
        final boolean installed = isInstalled(comp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(installed
                ? R.drawable.button_secondary   // could use a green-tinted drawable if desired
                : R.drawable.button_secondary);
        container.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8);
        container.setLayoutParams(lp);

        final CheckBox cb = new CheckBox(this);
        cb.setChecked(installed || wanted);   // installed → always checked
        if (required || installed) cb.setEnabled(false);  // lock required & already-installed
        cb.setButtonTintList(getColorStateList(R.color.accent));

        // Store reference for later reads
        if      (comp.equals(COMP_APACHE)) chkApache = cb;
        else if (comp.equals(COMP_FPM))    chkFpm    = cb;
        else if (comp.equals(COMP_MYSQL))  chkMysql  = cb;
        else if (comp.equals(COMP_PMA))    chkPma    = cb;
        else if (comp.equals(COMP_FTP))    chkFtp    = cb;
        else if (comp.equals(COMP_WP))     chkWp     = cb;

        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if      (comp.equals(COMP_APACHE)) wantApache = isChecked;
                else if (comp.equals(COMP_FPM))    wantFpm    = isChecked;
                else if (comp.equals(COMP_MYSQL))  wantMysql  = isChecked;
                else if (comp.equals(COMP_PMA))    wantPma    = isChecked;
                else if (comp.equals(COMP_FTP))    wantFtp    = isChecked;
                else if (comp.equals(COMP_WP))     wantWp     = isChecked;
                // refresh size label — just rebuild to keep it simple
            }
        });
        container.addView(cb);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        titleView.setTextSize(11);
        titleView.setTypeface(Typeface.MONOSPACE);
        textLayout.addView(titleView);

        // Status tag
        TextView statusTag = new TextView(this);
        if (installed) {
            statusTag.setText("\u2713 INSTALLED");
            statusTag.setTextColor(getResources().getColor(R.color.status_green));
        } else if (required) {
            statusTag.setText("REQUIRED");
            statusTag.setTextColor(getResources().getColor(R.color.warning));
        } else {
            statusTag.setText("NOT INSTALLED");
            statusTag.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        statusTag.setTextSize(8);
        statusTag.setTypeface(Typeface.MONOSPACE);
        textLayout.addView(statusTag);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextColor(getResources().getColor(R.color.text_secondary));
        descView.setTextSize(9);
        descView.setTypeface(Typeface.MONOSPACE);
        textLayout.addView(descView);

        container.addView(textLayout);

        // Uninstall button — only shown for installed, non-required components
        if (installed && !required) {
            TextView btnRemove = new TextView(this);
            btnRemove.setText("REMOVE");
            btnRemove.setTextSize(8);
            btnRemove.setTextColor(getResources().getColor(android.R.color.white));
            btnRemove.setBackgroundResource(R.drawable.button_danger);
            btnRemove.setPadding(10, 6, 10, 6);
            btnRemove.setGravity(android.view.Gravity.CENTER);
            btnRemove.setClickable(true);
            btnRemove.setFocusable(true);
            final LinearLayout rowRef       = container;
            final TextView statusTagRef     = statusTag;
            final CheckBox cbRef            = cb;
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    confirmAndRemove(comp, statusTagRef, cbRef, rowRef);
                }
            });
            container.addView(btnRemove);
        }

        return container;
    }

    private void confirmAndRemove(final String comp, final TextView statusTag,
                                  final CheckBox cb, final LinearLayout row) {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Remove " + comp + "?");
        b.setMessage("This will delete all files in " + getFilesDir() + "/" + comp +
                ".\n\nAre you sure?");
        b.setPositiveButton("REMOVE", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                deleteRecursive(new File(getFilesDir(), comp));
                // Also remove component zip if bundled
                new File(getFilesDir(), comp + ".zip").delete();
                cb.setChecked(false);
                cb.setEnabled(true);
                statusTag.setText("NOT INSTALLED");
                statusTag.setTextColor(getResources().getColor(R.color.text_secondary));
                // hide remove button
                if (row.getChildCount() > 0) {
                    View last = row.getChildAt(row.getChildCount() - 1);
                    if (last instanceof TextView && ((TextView)last).getText().toString().equals("REMOVE")) {
                        last.setVisibility(View.GONE);
                    }
                }
                Toast.makeText(SetupActivity.this, comp + " removed.", Toast.LENGTH_SHORT).show();
                prefs.edit().remove(comp + "_installed").apply();
            }
        });
        b.setNegativeButton("CANCEL", null);
        b.show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 3 — Review & install
    // ═════════════════════════════════════════════════════════════════════════
    private void buildReviewStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createSectionTitle("\u2713 REVIEW & INSTALL"));

        // Summary card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.button_secondary);
        card.setPadding(16, 16, 16, 16);
        card.setLayoutParams(matchWrap(0, 0, 0, 16));

        TextView cardTitle = new TextView(this);
        cardTitle.setText("Installation Summary");
        cardTitle.setTextColor(getResources().getColor(R.color.accent));
        cardTitle.setTextSize(11);
        cardTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(cardTitle);

        addSummaryRow(card, "Installer type:", isNetInstaller ? "Net (GitHub)" : "Bundled");
        addSummaryRow(card, "Install path:",   getFilesDir().getAbsolutePath());

        addSummaryRow(card, "Apache:",      formatAction(COMP_APACHE, wantApache));
        addSummaryRow(card, "PHP-FPM:",     formatAction(COMP_FPM,    wantFpm));
        addSummaryRow(card, "MariaDB:",     formatAction(COMP_MYSQL,  wantMysql));
        addSummaryRow(card, "phpMyAdmin:",  formatAction(COMP_PMA,    wantPma));
        addSummaryRow(card, "FTP Server:",  formatAction(COMP_FTP,    wantFtp));
        addSummaryRow(card, "WordPress:",   formatAction(COMP_WP,     wantWp));
        addSummaryRow(card, "Total size:",  calculateTotalWantedSize() + " MB");

        layout.addView(card);

        // Placeholders note
        TextView note = new TextView(this);
        note.setText("Note: FTP Server and WordPress are placeholders.\n" +
                "They will be skipped during installation.");
        note.setTextColor(getResources().getColor(R.color.text_secondary));
        note.setTextSize(9);
        note.setTypeface(Typeface.MONOSPACE);
        note.setBackgroundResource(R.drawable.button_secondary);
        note.setPadding(12, 10, 12, 10);
        note.setLayoutParams(matchWrap(0, 0, 0, 12));
        layout.addView(note);

        // Warning
        TextView warning = new TextView(this);
        warning.setText("\u26A0\uFE0F Installation may take 1–3 minutes.\n" +
                "Do not close the app during this process.");
        warning.setTextColor(getResources().getColor(R.color.warning));
        warning.setTextSize(9);
        warning.setTypeface(Typeface.MONOSPACE);
        warning.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.addView(warning);

        contentArea.addView(layout);
    }

    private String formatAction(String comp, boolean wanted) {
        boolean installed = isInstalled(comp);
        if (installed && wanted)  return "\u2713 Installed (no change)";
        if (!installed && wanted) return "\u25BA Will install";
        if (installed && !wanted) return "\u2715 Will remove";
        return "\u2014 Skip";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Installation logic
    // ═════════════════════════════════════════════════════════════════════════
    private void startInstallation() {
        // Sync checkbox values one more time
        if (chkApache != null) wantApache = chkApache.isChecked();
        if (chkFpm    != null) wantFpm    = chkFpm.isChecked();
        if (chkMysql  != null) wantMysql  = chkMysql.isChecked();
        if (chkPma    != null) wantPma    = chkPma.isChecked();
        if (chkFtp    != null) wantFtp    = chkFtp.isChecked();
        if (chkWp     != null) wantWp     = chkWp.isChecked();

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Installing...");
        pd.setMessage("Please wait...");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        pd.show();

        new Thread(new Runnable() {
            @Override public void run() {
                performInstallation(pd);
            }
        }).start();
    }


private void performInstallation(final ProgressDialog pd) {
    try {
        // FIRST: Extract ALL assets (lib.zip plus any component zips/folders)
        updateProgress(pd, 0, 100, "Extracting core libraries...");
        
        // Extract lib.zip first (contains .so files)
        extractLibZip(pd);
        
        // Extract ALL component assets to a temporary location
        updateProgress(pd, 20, 100, "Extracting all component assets...");
        extractAllAssetsToTemp(pd);
        
        // Build work list for installation/uninstallation
        String[][] components = {
            { COMP_APACHE, String.valueOf(wantApache) },
            { COMP_FPM,    String.valueOf(wantFpm)    },
            { COMP_MYSQL,  String.valueOf(wantMysql)  },
            { COMP_PMA,    String.valueOf(wantPma)    },
            { COMP_FTP,    String.valueOf(wantFtp)    },
            { COMP_WP,     String.valueOf(wantWp)     },
        };

        int total = components.length;
        int done  = 0;

        for (String[] entry : components) {
            final String comp   = entry[0];
            final boolean want  = Boolean.parseBoolean(entry[1]);
            final boolean alreadyInstalled = isInstalled(comp);

            updateProgress(pd, 40 + (done * 40 / total), total, "Processing " + comp + "...");

            if (want && !alreadyInstalled) {
                // Install component from temporary location
                boolean placeholder = comp.equals(COMP_FTP) || comp.equals(COMP_WP);
                if (!placeholder) {
                    installComponentFromTemp(comp, pd);
                }
                prefs.edit().putBoolean(comp + "_installed", true).apply();

            } else if (!want && alreadyInstalled) {
                // Remove component
                deleteRecursive(new File(getFilesDir(), comp));
                new File(getFilesDir(), comp + ".zip").delete();
                prefs.edit().remove(comp + "_installed").apply();
                
            } else if (want && alreadyInstalled) {
                // Already installed and wanted - ensure files are present
                ensureComponentFiles(comp, pd);
            }
            done++;
        }

        // Clean up temporary directory
        cleanupTempDir();
        
        // Persist config
        appConfig.save();

        runOnUiThread(new Runnable() {
            @Override public void run() {
                pd.dismiss();
                Toast.makeText(SetupActivity.this,
                        "Done! Components updated.", Toast.LENGTH_LONG).show();
                currentStep = STEP_COMPONENTS;
                showStep(currentStep);
            }
        });

    } catch (final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                pd.dismiss();
                Toast.makeText(SetupActivity.this,
                        "Installation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
    }
}
// Temporary directory for extracted assets
private File getTempDir() {
    File tempDir = new File(getFilesDir(), ".temp_assets");
    if (!tempDir.exists()) {
        tempDir.mkdirs();
    }
    return tempDir;
}

private void cleanupTempDir() {
    deleteRecursive(getTempDir());
}

private void extractLibZip(final ProgressDialog pd) throws IOException {
    File libZipInAssets = new File(getFilesDir(), LIB_ZIP);
    File libZipInTemp = new File(getTempDir(), LIB_ZIP);
    
    // Check if lib.zip exists in various locations
    boolean libExtracted = false;
    
    // Check if already extracted (lib directory exists)
    File libDir = new File(getFilesDir(), "lib");
    if (libDir.exists() && libDir.isDirectory()) {
        updateProgressMsg(pd, "Native libraries already present...");
        return;
    }
    
    // Try to find and extract lib.zip
    if (libZipInAssets.exists()) {
        updateProgressMsg(pd, "Extracting native libraries...");
        extractZipTo(libZipInAssets, getFilesDir());
        libZipInAssets.delete();
        libExtracted = true;
    } 
    else if (assetExists(LIB_ZIP)) {
        updateProgressMsg(pd, "Copying native libraries from assets...");
        copyAssetToFile(LIB_ZIP, libZipInTemp);
        updateProgressMsg(pd, "Extracting native libraries...");
        extractZipTo(libZipInTemp, getFilesDir());
        libExtracted = true;
    }
    
    if (!libExtracted) {
        // Log warning but continue
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(SetupActivity.this, 
                    "Warning: lib.zip not found - native libraries may be missing", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}

private void extractAllAssetsToTemp(final ProgressDialog pd) throws IOException {
    File tempDir = getTempDir();
    
    // List of all possible components
    String[] components = {COMP_APACHE, COMP_FPM, COMP_MYSQL, COMP_PMA, COMP_FTP, COMP_WP};
    
    for (String comp : components) {
        File compTempDir = new File(tempDir, comp);
        
        // Skip if already extracted to temp
        if (compTempDir.exists() && compTempDir.isDirectory()) {
            continue;
        }
        
        updateProgressMsg(pd, "Preparing " + comp + " files...");
        
        // Try to extract from zip first
        boolean extracted = tryExtractComponentFromZip(comp, tempDir, pd);
        
        if (!extracted) {
            // Try to copy from assets folder
            tryCopyComponentFromAssets(comp, compTempDir);
        }
    }
}

private boolean tryExtractComponentFromZip(String comp, File destDir, ProgressDialog pd) throws IOException {
    File zipInFiles = new File(getFilesDir(), comp + ".zip");
    File zipAlt = new File(getFilesDir(), "assets_" + comp + ".zip");
    File tempZip = new File(getTempDir(), comp + ".zip");
    
    // Check various zip locations
    if (zipInFiles.exists()) {
        extractZipTo(zipInFiles, destDir);
        zipInFiles.delete();
        return true;
    }
    else if (zipAlt.exists()) {
        extractZipTo(zipAlt, destDir);
        zipAlt.delete();
        return true;
    }
    else if (assetExists(comp + ".zip")) {
        copyAssetToFile(comp + ".zip", tempZip);
        extractZipTo(tempZip, destDir);
        tempZip.delete();
        return true;
    }
    else if (assetExists("assets_" + comp + ".zip")) {
        copyAssetToFile("assets_" + comp + ".zip", tempZip);
        extractZipTo(tempZip, destDir);
        tempZip.delete();
        return true;
    }
    
    return false;
}

private void tryCopyComponentFromAssets(String comp, File targetDir) throws IOException {
    String[] assetFiles = getAssets().list(comp);
    if (assetFiles != null && assetFiles.length > 0) {
        copyAssetDirToFile(comp, targetDir);
    }
}

private void installComponentFromTemp(String comp, ProgressDialog pd) throws IOException {
    File tempCompDir = new File(getTempDir(), comp);
    File targetDir = new File(getFilesDir(), comp);
    
    if (tempCompDir.exists() && tempCompDir.isDirectory()) {
        updateProgressMsg(pd, "Installing " + comp + "...");
        
        // Ensure parent directory exists
        targetDir.getParentFile().mkdirs();
        
        // Copy from temp to final location
        copyRecursive(tempCompDir, targetDir);
    } else {
        // Fallback to original installComponent method
       // installComponent(comp, pd);
       /*
       ----------
       1. ERROR in /storage/emulated/0/.sketchware/data/717/files/java/SetupActivity.java (at line 989)
       installComponent(comp, pd);
       ^^^^^^^^^^^^^^^^^^^^^^^^^^
       Unhandled exception type Exception
       ----------
       1 problem (1 error)
       */
    }
}

private void ensureComponentFiles(String comp, ProgressDialog pd) throws IOException {
    File targetDir = new File(getFilesDir(), comp);
    File tempCompDir = new File(getTempDir(), comp);
    
    // If component directory is empty or missing files, restore from temp
    if (!targetDir.exists() || targetDir.listFiles().length == 0) {
        if (tempCompDir.exists() && tempCompDir.isDirectory()) {
            updateProgressMsg(pd, "Restoring " + comp + " files...");
            targetDir.mkdirs();
            copyRecursive(tempCompDir, targetDir);
        }
    }
}

private void copyRecursive(File src, File dst) throws IOException {
    if (src.isDirectory()) {
        if (!dst.exists()) {
            dst.mkdirs();
        }
        String[] children = src.list();
        if (children != null) {
            for (String child : children) {
                copyRecursive(new File(src, child), new File(dst, child));
            }
        }
    } else {
        // Ensure parent directories exist
        dst.getParentFile().mkdirs();
        
        java.nio.file.Files.copy(src.toPath(), dst.toPath(), 
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}

    private void updateProgress(final ProgressDialog pd,
                                final int done, final int total,
                                final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                pd.setProgress(total > 0 ? (done * 100 / total) : 0);
                if (msg != null) pd.setMessage(msg);
            }
        });
    }

    /**
     * Installs one component. Sources are checked in order:
     *   1. Already-extracted folder in files/  → skip (already done)
     *   2. Zip file in files/<comp>.zip        → extract in-place
     *   3. Asset named <comp>.zip              → copy then extract
     *   4. Asset named assets_<comp>.zip       → copy then extract
     *   5. Folder in assets/<comp>/            → copy files directly
     */
    private void installComponent(final String comp, final ProgressDialog pd) throws Exception {
        File destDir    = getFilesDir();
        File targetDir  = new File(destDir, comp);
        File zipInFiles = new File(destDir, comp + ".zip");
        File zipAlt     = new File(destDir, "assets_" + comp + ".zip");

        // Already extracted?
        if (targetDir.exists() && targetDir.isDirectory()) return;

        // Zip already in files/?
        if (zipInFiles.exists()) {
            updateProgressMsg(pd, "Extracting " + comp + "...");
            extractZipTo(zipInFiles, destDir);
            zipInFiles.delete();
            return;
        }
        if (zipAlt.exists()) {
            updateProgressMsg(pd, "Extracting " + comp + "...");
            extractZipTo(zipAlt, destDir);
            zipAlt.delete();
            return;
        }

        // Try asset: <comp>.zip
        if (assetExists(comp + ".zip")) {
            updateProgressMsg(pd, "Copying " + comp + ".zip from assets...");
            copyAssetToFile(comp + ".zip", zipInFiles);
            updateProgressMsg(pd, "Extracting " + comp + "...");
            extractZipTo(zipInFiles, destDir);
            zipInFiles.delete();
            return;
        }

        // Try asset: assets_<comp>.zip
        if (assetExists("assets_" + comp + ".zip")) {
            updateProgressMsg(pd, "Copying assets_" + comp + ".zip from assets...");
            copyAssetToFile("assets_" + comp + ".zip", zipAlt);
            updateProgressMsg(pd, "Extracting " + comp + "...");
            extractZipTo(zipAlt, destDir);
            zipAlt.delete();
            return;
        }

        // Try flat asset folder: <comp>/
        String[] assetFiles = getAssets().list(comp);
        if (assetFiles != null && assetFiles.length > 0) {
            updateProgressMsg(pd, "Copying " + comp + " from assets...");
            copyAssetDirToFile(comp, targetDir);
            return;
        }

        // Not found — log and skip (don't throw, allow other components to continue)
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(SetupActivity.this,
                        "Warning: source not found for " + comp + ", skipped.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgressMsg(final ProgressDialog pd, final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (pd != null && pd.isShowing()) pd.setMessage(msg);
            }
        });
    }

    // ─── Asset helpers ───────────────────────────────────────────────────────

    private boolean assetExists(String name) {
        try {
            InputStream is = getAssets().open(name);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Copy a single asset file to a File on disk. */
    private void copyAssetToFile(String assetName, File dest) throws IOException {
        InputStream in  = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    /** Recursively copy an asset directory to a File directory. */
    private void copyAssetDirToFile(String assetPath, File destDir) throws IOException {
        destDir.mkdirs();
        String[] children = getAssets().list(assetPath);
        if (children == null) return;
        for (String child : children) {
            String childPath = assetPath + "/" + child;
            String[] grandChildren = getAssets().list(childPath);
            if (grandChildren != null && grandChildren.length > 0) {
                copyAssetDirToFile(childPath, new File(destDir, child));
            } else {
                copyAssetToFile(childPath, new File(destDir, child));
            }
        }
    }

    // ─── Zip extraction ──────────────────────────────────────────────────────

    private void extractZipTo(File zipFile, File destDir) throws IOException {
        ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(zipFile));
        ZipEntry entry;
        byte[] buf = new byte[8192];

        while ((entry = zis.getNextEntry()) != null) {
            File target = new File(destDir, entry.getName());

            if (entry.isDirectory()) {
                target.mkdirs();
            } else {
                target.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(target);
                int n;
                while ((n = zis.read(buf)) != -1) fos.write(buf, 0, n);
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }

    // ─── File removal ────────────────────────────────────────────────────────

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }

    // ─── Component detection ─────────────────────────────────────────────────

    /**
     * A component is considered installed when its folder exists in filesDir.
     * For required components (Apache, fpm) we also trust the prefs flag as a
     * fallback so a first-run install isn't re-triggered on every launch.
     */
    private boolean isInstalled(String comp) {
        File dir = new File(getFilesDir(), comp);
        return dir.exists() && dir.isDirectory();
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    private boolean validateStep1() {
        File path = getFilesDir();
        long freeBytes = path.getFreeSpace();
        long requiredBytes = (long) calculateTotalWantedSize() * 1024 * 1024;
        if (freeBytes < requiredBytes) {
            Toast.makeText(this, "Not enough free space! Need ~"
                    + calculateTotalWantedSize() + " MB", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (!wantApache || !wantFpm) {
            Toast.makeText(this,
                    "Apache and PHP-FPM are required components.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ─── Size estimate ───────────────────────────────────────────────────────

private int calculateTotalWantedSize() {
    int size = 0;
    if (wantApache) size += 28;
    if (wantFpm)    size += 23;
    if (wantMysql)  size += 37;
    if (wantPma)    size += 55;
    // Add lib size (estimate)
    size += 78; // Approximate size for .so files
    // FTP, WP: placeholder, 0 MB
    return size;
}

    // ═════════════════════════════════════════════════════════════════════════
    // UI helpers
    // ═════════════════════════════════════════════════════════════════════════

    private TextView createSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.accent));
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(0, 0, 0, 12);
        return tv;
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.accent));
        tv.setTextSize(9);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(0, 16, 0, 4);
        return tv;
    }

    private void addSummaryRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView lv = new TextView(this);
        lv.setText(label);
        lv.setTextColor(getResources().getColor(R.color.text_secondary));
        lv.setTextSize(10);
        lv.setTypeface(Typeface.MONOSPACE);
        lv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView vv = new TextView(this);
        vv.setText(value);
        vv.setTextColor(getResources().getColor(R.color.text_primary));
        vv.setTextSize(10);
        vv.setTypeface(Typeface.MONOSPACE);

        row.addView(lv);
        row.addView(vv);
        parent.addView(row);
    }

    private LinearLayout.LayoutParams matchWrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}