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
 * Bundled installer: master.zip is expected in assets/master.zip.
 * Net installer:     master.zip is downloaded from download_url.
 *
 * Both paths converge at STEP_WELCOME after the zip is extracted.
 *
 * Component detection uses folder/file existence:
 *   Apache    → files/Apache/
 *   PHP-FPM   → files/fpm/
 *   MariaDB   → files/db/
 *   phpMyAdmin→ files/phpmyadmin/
 */
public class SetupActivity extends Activity {

    // ─── Steps ────────────────────────────────────────────────────────────────
    private static final int STEP_PREPARE  = 0;   // download (net) OR extract bundled zip
    private static final int STEP_WELCOME  = 1;
    private static final int STEP_COMPONENTS = 2;
    private static final int STEP_REVIEW   = 3;

    // ─── Component IDs ────────────────────────────────────────────────────────
    private static final String COMP_APACHE = "Apache";
    private static final String COMP_FPM    = "fpm";
    private static final String COMP_MYSQL  = "db";
    private static final String COMP_PMA    = "phpmyadmin";
    private static final String COMP_FTP    = "ftp";
    private static final String COMP_WP     = "wordpress";

    private static final String MASTER_ZIP  = "master.zip";
    private static final String LIB_ZIP     = "lib.zip";

    // ─── UI ──────────────────────────────────────────────────────────────────
    private LinearLayout contentArea;
    private TextView btnBack, btnNext, btnInstall, btnCancel;
    private TextView step1Ind, step2Ind, step3Ind;

    // ─── State ───────────────────────────────────────────────────────────────
    private int     currentStep;
    private boolean isNetInstaller = false;
    private String  downloadUrl    = "";
    private boolean assetsReady    = false;   // master zip extracted

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

        readSetupJson();
        initViews();

        // Always start at STEP_PREPARE so the user sees the prepare screen
        // (download OR bundled-extract). If assets are already ready (from a
        // previous run) we skip straight to STEP_WELCOME.
        if (assetsAlreadyExtracted()) {
            assetsReady = true;
            currentStep = STEP_WELCOME;
        } else {
            currentStep = STEP_PREPARE;
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
            isNetInstaller = false;
        }
    }

    /**
     * Assets are considered already extracted when the temp-unpack sentinel
     * file exists OR when all four base component dirs are present.
     */
    private boolean assetsAlreadyExtracted() {
        File sentinel = new File(getFilesDir(), ".assets_ready");
        if (sentinel.exists()) return true;
        // Alternatively, trust that at least Apache is present
        return new File(getFilesDir(), COMP_APACHE).isDirectory();
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
            @Override public void onClick(View v) {
                if (allSelectedAlreadyInstalled()) {
                    goToMainActivity();
                } else {
                    startInstallation();
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    // ─── Navigation ──────────────────────────────────────────────────────────
    private void navigateBack() {
        if (currentStep == STEP_WELCOME) return;
        if (currentStep == STEP_COMPONENTS) currentStep = STEP_WELCOME;
        else if (currentStep == STEP_REVIEW)  currentStep = STEP_COMPONENTS;
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
            case STEP_PREPARE:    buildPrepareStep();    break;
            case STEP_WELCOME:    buildWelcomeStep();    break;
            case STEP_COMPONENTS: buildComponentsStep(); break;
            case STEP_REVIEW:     buildReviewStep();     break;
        }
    }

    private void updateButtons(int step) {
        if (step == STEP_PREPARE) {
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
            if (allSelectedAlreadyInstalled()) {
                btnInstall.setText("FINISH");
                btnInstall.setBackgroundResource(R.drawable.button_state);
            } else {
                btnInstall.setText("INSTALL");
                btnInstall.setBackgroundResource(R.drawable.button_success);
            }
        }
    }

    /** Returns true when every wanted component is already on disk. */
    private boolean allSelectedAlreadyInstalled() {
        if (wantApache && !isInstalled(COMP_APACHE)) return false;
        if (wantFpm    && !isInstalled(COMP_FPM))    return false;
        if (wantMysql  && !isInstalled(COMP_MYSQL))  return false;
        if (wantPma    && !isInstalled(COMP_PMA))    return false;
        return true;
    }

    private void updateIndicators(int step) {
        // STEP_PREPARE has no indicator; steps 1-3 map to WELCOME, COMPONENTS, REVIEW
        int active = step; // STEP_PREPARE(0) → nothing lit; otherwise matches
        applyIndicator(step1Ind, active >= STEP_WELCOME);
        applyIndicator(step2Ind, active >= STEP_COMPONENTS);
        applyIndicator(step3Ind, active >= STEP_REVIEW);
    }

    private void applyIndicator(TextView tv, boolean on) {
        tv.setBackgroundResource(on ? R.drawable.button_state : R.drawable.button_secondary);
        tv.setTextColor(getResources().getColor(on ? android.R.color.white : R.color.text_secondary));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 0 — Prepare assets (download OR extract bundled master.zip)
    // ═════════════════════════════════════════════════════════════════════════
    private android.widget.ProgressBar prepareProgress;
    private TextView prepareStatus;

    private void buildPrepareStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        if (isNetInstaller) {
            layout.addView(createSectionTitle("\u2193 DOWNLOAD ASSETS"));
        } else {
            layout.addView(createSectionTitle("\ud83d\udce6 PREPARE BUNDLED ASSETS"));
        }

        prepareStatus = new TextView(this);
        if (isNetInstaller) {
            prepareStatus.setText("Assets will be downloaded from GitHub.\nThis may take a few minutes on a slow connection.");
        } else {
            prepareStatus.setText("Assets are bundled inside this APK.\nThey will be extracted to app storage.");
        }
        prepareStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        prepareStatus.setTextSize(10);
        prepareStatus.setTypeface(Typeface.MONOSPACE);
        prepareStatus.setBackgroundResource(R.drawable.button_secondary);
        prepareStatus.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.setMargins(0, 0, 0, 16);
        prepareStatus.setLayoutParams(statusLp);
        layout.addView(prepareStatus);

        if (isNetInstaller) {
            // Show URL
            layout.addView(createLabel("SOURCE URL"));
            TextView urlVal = new TextView(this);
            urlVal.setText(downloadUrl.isEmpty() ? "(no URL in setup.json)" : downloadUrl);
            urlVal.setTextColor(getResources().getColor(R.color.accent));
            urlVal.setTextSize(9);
            urlVal.setTypeface(Typeface.MONOSPACE);
            urlVal.setPadding(0, 4, 0, 16);
            layout.addView(urlVal);
        }

        // Progress bar
        prepareProgress = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        prepareProgress.setMax(100);
        prepareProgress.setProgress(0);
        prepareProgress.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24));
        layout.addView(prepareProgress);

        // Action button
        final TextView btnAction = new TextView(this);
        btnAction.setText(isNetInstaller ? "START DOWNLOAD" : "EXTRACT ASSETS");
        btnAction.setTextColor(getResources().getColor(android.R.color.white));
        btnAction.setTextSize(12);
        btnAction.setTypeface(Typeface.MONOSPACE);
        btnAction.setGravity(android.view.Gravity.CENTER);
        btnAction.setBackgroundResource(R.drawable.button_state);
        btnAction.setPadding(20, 14, 20, 14);
        btnAction.setClickable(true);
        btnAction.setFocusable(true);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 24, 0, 0);
        btnAction.setLayoutParams(btnLp);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                btnAction.setEnabled(false);
                btnAction.setText(isNetInstaller ? "DOWNLOADING..." : "EXTRACTING...");
                if (isNetInstaller) {
                    downloadAndExtractMaster();
                } else {
                    extractBundledMaster();
                }
            }
        });
        layout.addView(btnAction);

        contentArea.addView(layout);
    }

    // ─── Net installer: download then extract ─────────────────────────────────
    private void downloadAndExtractMaster() {
        final String url = downloadUrl;
        if (url.isEmpty()) {
            setPrepareStatus("Error: no download URL configured in setup.json");
            return;
        }

        new Thread(new Runnable() {
            @Override public void run() {
                InputStream in = null;
                FileOutputStream out = null;

                try {
                    File dest = new File(getFilesDir(), MASTER_ZIP);

                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(120000);
                    conn.connect();

                    final long total = conn.getContentLengthLong();
                    in  = conn.getInputStream();
                    out = new FileOutputStream(dest);

                    byte[] buf = new byte[8192];
                    int n;
                    long downloaded = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (total > 0) {
                            final int pct = (int) (downloaded * 100 / total);
                            setPrepareProgress(pct / 2); // first 50 = download
                        }
                    }
                    out.flush();
                    setPrepareProgress(50);
                    setPrepareStatus("Download complete. Extracting...");

                    extractMasterZipFromFile(dest);
                    dest.delete();

                    onAssetsReady();

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setPrepareStatus("Download failed: " + e.getMessage() +
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

    // ─── Bundled installer: copy from assets then extract ─────────────────────
    private void extractBundledMaster() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    setPrepareStatus("Copying master.zip from APK assets...");

                    // master.zip must exist in assets/
                    if (!assetExists(MASTER_ZIP)) {
                        setPrepareStatus("Error: master.zip not found in APK assets.");
                        return;
                    }

                    File dest = new File(getFilesDir(), MASTER_ZIP);
                    copyAssetToFileWithProgress(MASTER_ZIP, dest);

                    setPrepareProgress(50);
                    setPrepareStatus("Extraction in progress...");

                    extractMasterZipFromFile(dest);
                    dest.delete();

                    onAssetsReady();

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setPrepareStatus("Extraction failed: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Shared: extract the master zip (already on disk) to filesDir.
     * The master zip layout mirrors the net-installer zip exactly.
     * Progress is mapped to 50–100%.
     */
    private void extractMasterZipFromFile(File masterZip) throws IOException {
        ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(masterZip));
        ZipEntry entry;
        byte[] buf = new byte[8192];

        // Count entries for progress
        // (We can't easily count without a second pass, so just pulse progress)
        int count = 0;
        while ((entry = zis.getNextEntry()) != null) {
            File target = new File(getFilesDir(), entry.getName());
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
            count++;
            // Smooth progress from 50→95
            final int pct = Math.min(95, 50 + count);
            setPrepareProgress(pct);
        }
        zis.close();

        // Write sentinel so we don't re-extract next launch
        new File(getFilesDir(), ".assets_ready").createNewFile();
        setPrepareProgress(100);
    }

    private void onAssetsReady() {
        assetsReady = true;
        runOnUiThread(new Runnable() {
            @Override public void run() {
                setPrepareStatus("Assets ready!");
                currentStep = STEP_WELCOME;
                showStep(currentStep);
            }
        });
    }

    private void setPrepareProgress(final int pct) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (prepareProgress != null) prepareProgress.setProgress(pct);
            }
        });
    }

    private void setPrepareStatus(final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (prepareStatus != null) prepareStatus.setText(msg);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 1 — Welcome / info
    // ═════════════════════════════════════════════════════════════════════════
    private void buildWelcomeStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createSectionTitle("\u2665 AMPDROID SERVER SETUP"));

        StringBuilder info = new StringBuilder();
        info.append("Storage Required: ~").append(calculateTotalWantedSize()).append(" MB\n\n");
        info.append("Components available:\n");
        info.append("  \u2022 Apache HTTP Server\n");
        info.append("  \u2022 PHP-FPM Interpreter\n");
        info.append("  \u2022 MariaDB Database\n");
        info.append("  \u2022 phpMyAdmin (optional)\n");
        info.append("\nAlready installed components will be shown\n");
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

        layout.addView(createLabel("INSTALLATION PATH"));

        EditText txtPath = new EditText(this);
        txtPath.setText(getFilesDir().getAbsolutePath());
        txtPath.setTextColor(getResources().getColor(R.color.text_primary));
        txtPath.setTextSize(10);
        txtPath.setTypeface(Typeface.MONOSPACE);
        txtPath.setBackgroundResource(R.drawable.button_secondary);
        txtPath.setPadding(12, 10, 12, 10);
        txtPath.setEnabled(false);
        layout.addView(txtPath);

        File filesDir = getFilesDir();
        long freeMb = filesDir.getFreeSpace() / (1024 * 1024);
        TextView spaceInfo = new TextView(this);
        spaceInfo.setText("Free space: " + freeMb + " MB");
        spaceInfo.setTextColor(freeMb > 300
                ? getResources().getColor(R.color.status_green)
                : getResources().getColor(R.color.warning));
        spaceInfo.setTextSize(10);
        spaceInfo.setTypeface(Typeface.MONOSPACE);
        spaceInfo.setPadding(0, 8, 0, 0);
        layout.addView(spaceInfo);

        TextView badge = new TextView(this);
        badge.setText(isNetInstaller
                ? "\u2601  NET INSTALLER  —  assets fetched from GitHub"
                : "\ud83d\udce6  BUNDLED INSTALLER  —  assets included in APK");
        badge.setTextColor(getResources().getColor(isNetInstaller ? R.color.accent : R.color.status_green));
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setBackgroundResource(R.drawable.button_secondary);
        badge.setPadding(12, 10, 12, 10);
        badge.setLayoutParams(matchWrap(0, 20, 0, 0));
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

        layout.addView(buildComponentRow(COMP_APACHE,
                "Apache HTTP Server", "Web server — required for PHP execution",
                wantApache, true));

        layout.addView(buildComponentRow(COMP_FPM,
                "PHP-FPM Interpreter", "PHP FastCGI process manager — required",
                wantFpm, true));

        layout.addView(buildComponentRow(COMP_MYSQL,
                "MariaDB Database", "MySQL-compatible database server",
                wantMysql, false));

        layout.addView(buildComponentRow(COMP_PMA,
                "phpMyAdmin", "Web-based database management UI",
                wantPma, false));

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

    private LinearLayout buildComponentRow(final String comp, String title,
                                           String description, boolean wanted,
                                           final boolean required) {
        final boolean installed = isInstalled(comp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(R.drawable.button_secondary);
        container.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8);
        container.setLayoutParams(lp);

        final CheckBox cb = new CheckBox(this);
        cb.setChecked(installed || wanted);
        if (required || installed) cb.setEnabled(false);
        cb.setButtonTintList(getColorStateList(R.color.accent));

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

        final TextView statusTag = new TextView(this);
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
            final LinearLayout rowRef   = container;
            final TextView statusTagRef = statusTag;
            final CheckBox cbRef        = cb;
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
        b.setMessage("This will delete all files in " + getFilesDir() + "/" + comp + ".\n\nAre you sure?");
        b.setPositiveButton("REMOVE", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                deleteRecursive(new File(getFilesDir(), comp));
                new File(getFilesDir(), comp + ".zip").delete();
                cb.setChecked(false);
                cb.setEnabled(true);
                statusTag.setText("NOT INSTALLED");
                statusTag.setTextColor(getResources().getColor(R.color.text_secondary));
                if (row.getChildCount() > 0) {
                    View last = row.getChildAt(row.getChildCount() - 1);
                    if (last instanceof TextView
                            && "REMOVE".equals(((TextView) last).getText().toString())) {
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
    // STEP 3 — Review & install / finish
    // ═════════════════════════════════════════════════════════════════════════
    private void buildReviewStep() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        boolean allDone = allSelectedAlreadyInstalled();
        layout.addView(createSectionTitle(allDone ? "\u2713 ALL COMPONENTS READY" : "\u2713 REVIEW & INSTALL"));

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
        addSummaryRow(card, "Apache:",         formatAction(COMP_APACHE, wantApache));
        addSummaryRow(card, "PHP-FPM:",        formatAction(COMP_FPM,    wantFpm));
        addSummaryRow(card, "MariaDB:",        formatAction(COMP_MYSQL,  wantMysql));
        addSummaryRow(card, "phpMyAdmin:",     formatAction(COMP_PMA,    wantPma));
        addSummaryRow(card, "Total size:",     calculateTotalWantedSize() + " MB");
        layout.addView(card);

        TextView footer = new TextView(this);
        if (allDone) {
            footer.setText("\u2705 Everything is installed. Tap FINISH to continue.");
            footer.setTextColor(getResources().getColor(R.color.status_green));
        } else {
            footer.setText("\u26A0\uFE0F Installation may take 5\u201330 seconds.\nDo not close the app during this process.");
            footer.setTextColor(getResources().getColor(R.color.warning));
        }
        footer.setTextSize(9);
        footer.setTypeface(Typeface.MONOSPACE);
        footer.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.addView(footer);

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
            @Override public void run() { performInstallation(pd); }
        }).start();
    }

    private void performInstallation(final ProgressDialog pd) {
        try {
            // lib.zip
            updateProgress(pd, 0, 100, "Extracting core libraries...");
            extractLibZip(pd);

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
                final String  comp      = entry[0];
                final boolean want      = Boolean.parseBoolean(entry[1]);
                final boolean alreadyIn = isInstalled(comp);

                updateProgress(pd, 20 + (done * 70 / total), total, "Processing " + comp + "...");

                if (want && !alreadyIn) {
                    boolean placeholder = comp.equals(COMP_FTP) || comp.equals(COMP_WP);
                    if (!placeholder) installComponent(comp, pd);
                    prefs.edit().putBoolean(comp + "_installed", true).apply();
                } else if (!want && alreadyIn) {
                    deleteRecursive(new File(getFilesDir(), comp));
                    new File(getFilesDir(), comp + ".zip").delete();
                    prefs.edit().remove(comp + "_installed").apply();
                }
                done++;
            }

            appConfig.save();

            runOnUiThread(new Runnable() {
                @Override public void run() {
                    pd.dismiss();
                    Toast.makeText(SetupActivity.this,
                            "Done! Components updated.", Toast.LENGTH_LONG).show();
                    currentStep = STEP_REVIEW;
                    showStep(currentStep);   // re-draw; button becomes FINISH if all done
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

    private void extractLibZip(final ProgressDialog pd) throws IOException {
        File libDir = new File(getFilesDir(), "lib");
        if (libDir.exists() && libDir.isDirectory()) {
            updateProgressMsg(pd, "Native libraries already present...");
            return;
        }

        File libZipInFiles = new File(getFilesDir(), LIB_ZIP);
        if (libZipInFiles.exists()) {
            updateProgressMsg(pd, "Extracting native libraries...");
            extractZipTo(libZipInFiles, getFilesDir());
            libZipInFiles.delete();
            return;
        }

        if (assetExists(LIB_ZIP)) {
            File tmp = new File(getFilesDir(), LIB_ZIP);
            updateProgressMsg(pd, "Copying native libraries from assets...");
            copyAssetToFile(LIB_ZIP, tmp);
            updateProgressMsg(pd, "Extracting native libraries...");
            extractZipTo(tmp, getFilesDir());
            tmp.delete();
            return;
        }

        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(SetupActivity.this,
                        "Warning: lib.zip not found — native libraries may be missing",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Install one component. By the time we get here the master zip has already
     * been extracted to filesDir (during STEP_PREPARE), so the component folder
     * should already be present. This method is a safety net for cases where
     * a component zip is stored alongside the folder.
     */
    private void installComponent(final String comp, final ProgressDialog pd) throws Exception {
        File destDir    = getFilesDir();
        File targetDir  = new File(destDir, comp);
        File zipInFiles = new File(destDir, comp + ".zip");
        File zipAlt     = new File(destDir, "assets_" + comp + ".zip");

        if (targetDir.exists() && targetDir.isDirectory()) return;

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

        if (assetExists(comp + ".zip")) {
            updateProgressMsg(pd, "Copying " + comp + ".zip from assets...");
            copyAssetToFile(comp + ".zip", zipInFiles);
            extractZipTo(zipInFiles, destDir);
            zipInFiles.delete();
            return;
        }

        if (assetExists("assets_" + comp + ".zip")) {
            updateProgressMsg(pd, "Copying assets_" + comp + ".zip from assets...");
            copyAssetToFile("assets_" + comp + ".zip", zipAlt);
            extractZipTo(zipAlt, destDir);
            zipAlt.delete();
            return;
        }

        String[] assetFiles = getAssets().list(comp);
        if (assetFiles != null && assetFiles.length > 0) {
            updateProgressMsg(pd, "Copying " + comp + " from assets...");
            copyAssetDirToFile(comp, targetDir);
            return;
        }

        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(SetupActivity.this,
                        "Warning: source not found for " + comp + ", skipped.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgress(final ProgressDialog pd, final int done,
                                final int total, final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                pd.setProgress(total > 0 ? (done * 100 / total) : 0);
                if (msg != null) pd.setMessage(msg);
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

    // ─── Asset helpers ────────────────────────────────────────────────────────

    private boolean assetExists(String name) {
        try {
            InputStream is = getAssets().open(name);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void copyAssetToFile(String assetName, File dest) throws IOException {
        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    /**
     * Same as copyAssetToFile but posts progress to the prepare progress bar
     * (0→50%) so the user sees something happening for large bundled zips.
     */
    private void copyAssetToFileWithProgress(String assetName, File dest) throws IOException {
        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int n;
        long written = 0;
        // We don't know the total size from AssetManager, so just pulse every 512 KB
        int pulse = 0;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            written += n;
            pulse++;
            if (pulse % 64 == 0) {
                // slowly advance 0→48
                final int pct = (int) Math.min(48, written / (1024 * 20));
                setPrepareProgress(pct);
            }
        }
        in.close();
        out.close();
    }

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

    // ─── Zip extraction ───────────────────────────────────────────────────────

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

    // ─── File removal ─────────────────────────────────────────────────────────

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }

    // ─── Component detection ──────────────────────────────────────────────────

    private boolean isInstalled(String comp) {
        File dir = new File(getFilesDir(), comp);
        return dir.exists() && dir.isDirectory();
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private boolean validateStep1() {
        long freeBytes    = getFilesDir().getFreeSpace();
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

    // ─── Size estimate ────────────────────────────────────────────────────────

    private int calculateTotalWantedSize() {
        int size = 78; // lib
        if (wantApache) size += 28;
        if (wantFpm)    size += 23;
        if (wantMysql)  size += 37;
        if (wantPma)    size += 55;
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