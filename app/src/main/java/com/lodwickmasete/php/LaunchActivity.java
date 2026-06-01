package com.lodwickmasete.php;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/**
 * LaunchActivity — splash/gate screen.
 *
 * Flow:
 *   1. Check if all required components are installed.
 *   2a. All good → go straight to MainActivity.
 *   2b. Something missing → show warning dialog with options:
 *         [RUN SETUP]  →  SetupActivity
 *         [CONTINUE]   →  MainActivity (with instability warning)
 *       A "don't show again" checkbox suppresses the dialog on future launches
 *       (user can always reach Setup via the sidebar in MainActivity).
 */
public class LaunchActivity extends Activity {

    private static final String PREFS_NAME      = "launch_prefs";
    private static final String KEY_SKIP_CHECK  = "skip_install_check";

    // Required component folders (matches SetupActivity constants)
    private static final String[] REQUIRED = { "Apache", "fpm" };
    // Optional but checked for a "partial" warning
    private static final String[] OPTIONAL = { "db", "phpmyadmin" };

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Quick check
        InstallState state = checkInstallation();

        if (state == InstallState.ALL_GOOD) {
            // Everything present — go straight to main
            goMain();
            return;
        }

        if (state == InstallState.PARTIAL && prefs.getBoolean(KEY_SKIP_CHECK, false)) {
            // User said "don't ask again" for partial installs — just warn via Toast and continue
            goMain();
            return;
        }

        // Show the check screen
        buildCheckScreen(state);
    }

    // ─── Installation check ───────────────────────────────────────────────────

    private enum InstallState {
        ALL_GOOD,   // required + all optional present
        PARTIAL,    // required present but some optional missing
        MISSING     // one or more required components absent
    }

    private InstallState checkInstallation() {
        File base = getFilesDir();

        for (String comp : REQUIRED) {
            File dir = new File(base, comp);
            if (!dir.exists() || !dir.isDirectory()) {
                return InstallState.MISSING;
            }
        }

        for (String comp : OPTIONAL) {
            File dir = new File(base, comp);
            if (!dir.exists() || !dir.isDirectory()) {
                return InstallState.PARTIAL;
            }
        }

        return InstallState.ALL_GOOD;
    }

    // ─── Build screen ─────────────────────────────────────────────────────────

    private void buildCheckScreen(final InstallState state) {
        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D1117);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        setContentView(root);

        // ── Header ──
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(0xFF161B22);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(24), dp(36), dp(24), dp(36));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        header.setLayoutParams(headerLp);

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDC18"); // elephant
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        header.addView(icon);

        TextView appName = new TextView(this);
        appName.setText("AmpDroid");
        appName.setTextColor(0xFFE6EDF3);
        appName.setTextSize(22);
        appName.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, dp(8), 0, 0);
        header.addView(appName);

        TextView subtitle = new TextView(this);
        subtitle.setText("Server Manager");
        subtitle.setTextColor(0xFF58A6FF);
        subtitle.setTextSize(12);
        subtitle.setTypeface(Typeface.MONOSPACE);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // ── Card ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.button_secondary);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(20), dp(28), dp(20), 0);
        card.setLayoutParams(cardLp);

        // Status icon + title row
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, 0, 0, dp(12));

        TextView statusIcon = new TextView(this);
        statusIcon.setText(state == InstallState.MISSING ? "\u26A0\uFE0F" : "\u2139\uFE0F");
        statusIcon.setTextSize(22);
        statusIcon.setPadding(0, 0, dp(10), 0);
        titleRow.addView(statusIcon);

        TextView cardTitle = new TextView(this);
        cardTitle.setText(state == InstallState.MISSING
                ? "Setup Required"
                : "Incomplete Installation");
        cardTitle.setTextColor(state == InstallState.MISSING ? 0xFFF85149 : 0xFFD29922);
        cardTitle.setTextSize(15);
        cardTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        titleRow.addView(cardTitle);
        card.addView(titleRow);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(0xFF30363D);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(0, 0, 0, dp(14));
        divider.setLayoutParams(divLp);
        card.addView(divider);

        // Component status list
        card.addView(sectionLabel("COMPONENT STATUS"));
        card.addView(componentRow("Apache HTTP Server", "Apache",   true));
        card.addView(componentRow("PHP-FPM Interpreter","fpm",      true));
        card.addView(componentRow("MariaDB Database",   "db",       false));
        card.addView(componentRow("phpMyAdmin",         "phpmyadmin",false));

        // Warning text
        TextView warn = new TextView(this);
        if (state == InstallState.MISSING) {
            warn.setText("\nRequired server components are not installed. "
                    + "The app will not function correctly without them.\n\n"
                    + "Run the Setup Wizard to get started.");
        } else {
            warn.setText("\nSome optional components are missing. "
                    + "Certain features (e.g. database management) may not work.\n\n"
                    + "You can install them later via Setup in the sidebar.");
        }
        warn.setTextColor(0xFF8B949E);
        warn.setTextSize(10);
        warn.setTypeface(Typeface.MONOSPACE);
        card.addView(warn);

        root.addView(card);

        // ── "Don't ask again" checkbox (only for partial; missing always needs setup) ──
        final CheckBox chkSkip = new CheckBox(this);
        if (state == InstallState.PARTIAL) {
            chkSkip.setText("Don't show this again");
            chkSkip.setTextColor(0xFF8B949E);
            chkSkip.setTextSize(11);
            chkSkip.setTypeface(Typeface.MONOSPACE);
            chkSkip.setChecked(false);
            chkSkip.setButtonTintList(getColorStateList(R.color.accent));
            LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            chkLp.setMargins(dp(20), dp(18), dp(20), 0);
            chkSkip.setLayoutParams(chkLp);
            chkSkip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                    prefs.edit().putBoolean(KEY_SKIP_CHECK, checked).apply();
                }
            });
            root.addView(chkSkip);
        }

        // ── Buttons ──
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowLp.setMargins(dp(20), dp(20), dp(20), dp(28));
        btnRow.setLayoutParams(btnRowLp);

        if (state == InstallState.MISSING) {
            // Primary: Setup (full width)
            TextView btnSetup = makeButton("RUN SETUP \u2192", 0xFFE6EDF3, R.drawable.button_state);
            btnSetup.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            btnSetup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { goSetup(); }
            });
            btnRow.addView(btnSetup);

            // Secondary: Continue anyway
            TextView btnContinue = makeButton("CONTINUE ANYWAY", 0xFF8B949E, R.drawable.button_secondary);
            LinearLayout.LayoutParams contLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            contLp.setMargins(dp(10), 0, 0, 0);
            btnContinue.setLayoutParams(contLp);
            btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { goMain(); }
            });
            btnRow.addView(btnContinue);

        } else {
            // Partial: primary is Continue, secondary is Setup
            TextView btnContinue = makeButton("CONTINUE", 0xFFE6EDF3, R.drawable.button_state);
            btnContinue.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { goMain(); }
            });
            btnRow.addView(btnContinue);

            TextView btnSetup = makeButton("OPEN SETUP", 0xFF58A6FF, R.drawable.button_secondary);
            LinearLayout.LayoutParams setupLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            setupLp.setMargins(dp(10), 0, 0, 0);
            btnSetup.setLayoutParams(setupLp);
            btnSetup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { goSetup(); }
            });
            btnRow.addView(btnSetup);
        }

        root.addView(btnRow);

        // ── Footer note ──
        TextView footerNote = new TextView(this);
        footerNote.setText("You can always run Setup from the sidebar in the main app.");
        footerNote.setTextColor(0xFF484F58);
        footerNote.setTextSize(9);
        footerNote.setTypeface(Typeface.MONOSPACE);
        footerNote.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        footerLp.setMargins(dp(20), 0, dp(20), dp(20));
        footerNote.setLayoutParams(footerLp);
        root.addView(footerNote);
    }

    // ─── Component status row ─────────────────────────────────────────────────

    private LinearLayout componentRow(String label, String dirName, boolean required) {
        boolean installed = new File(getFilesDir(), dirName).isDirectory();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);

        // Bullet / tick
        TextView bullet = new TextView(this);
        bullet.setText(installed ? "\u2713" : "\u2715");
        bullet.setTextColor(installed ? 0xFF3FB950 : (required ? 0xFFF85149 : 0xFF8B949E));
        bullet.setTextSize(12);
        bullet.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bullet.setPadding(0, 0, dp(8), 0);
        row.addView(bullet);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(installed ? 0xFFE6EDF3 : 0xFF8B949E);
        name.setTextSize(10);
        name.setTypeface(Typeface.MONOSPACE);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(name);

        TextView tag = new TextView(this);
        if (installed) {
            tag.setText("OK");
            tag.setTextColor(0xFF3FB950);
        } else if (required) {
            tag.setText("MISSING");
            tag.setTextColor(0xFFF85149);
        } else {
            tag.setText("NOT INSTALLED");
            tag.setTextColor(0xFF8B949E);
        }
        tag.setTextSize(8);
        tag.setTypeface(Typeface.MONOSPACE);
        row.addView(tag);

        return row;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF58A6FF);
        tv.setTextSize(8);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    // ─── Button factory ───────────────────────────────────────────────────────

    private TextView makeButton(String text, int textColor, int bgRes) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(bgRes);
        tv.setPadding(dp(12), dp(14), dp(12), dp(14));
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void goSetup() {
        startActivity(new Intent(this, SetupActivity.class));
        finish();
    }

    // ─── Util ─────────────────────────────────────────────────────────────────

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}