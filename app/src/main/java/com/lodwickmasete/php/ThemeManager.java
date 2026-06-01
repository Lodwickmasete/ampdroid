package com.lodwickmasete.php;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * ThemeManager — applies dark / light / system theme without AppCompat.
 *
 * Usage:
 *   ThemeManager tm = new ThemeManager(cfg.get(KEY_THEME,"dark"), activity);
 *   tm.applyWindow(activity);
 *   tm.applyToView(rootView);
 *
 * Tag-based theming: set android:tag on any View to one of these strings
 * so applyToView() knows how to colour it:
 *
 *   "bg"       — main background colour
 *   "surface"  — header / footer panels
 *   "card"     — card / section containers
 *   "accent"   — accent-coloured text
 *   "muted"    — secondary / hint text
 *   "text"     — primary text
 *   "green"    — success text
 *   "red"      — danger / error text
 *   "yellow"   — warning text
 *   "divider"  — thin divider lines
 *   "input"    — EditText background
 */
public class ThemeManager {

    public static final String MODE_DARK   = "dark";
    public static final String MODE_LIGHT  = "light";
    public static final String MODE_SYSTEM = "system";

    // ── Dark palette ──────────────────────────────────────────────────────────
    public static final int DARK_BG       = Color.parseColor("#0D1117");
    public static final int DARK_SURFACE  = Color.parseColor("#161B22");
    public static final int DARK_CARD     = Color.parseColor("#21262D");
    public static final int DARK_DIVIDER  = Color.parseColor("#30363D");
    public static final int DARK_TEXT     = Color.parseColor("#E6EDF3");
    public static final int DARK_MUTED    = Color.parseColor("#7D8590");
    public static final int DARK_ACCENT   = Color.parseColor("#58A6FF");
    public static final int DARK_GREEN    = Color.parseColor("#3FB950");
    public static final int DARK_RED      = Color.parseColor("#F85149");
    public static final int DARK_YELLOW   = Color.parseColor("#D29922");
    public static final int DARK_CYAN     = Color.parseColor("#00E5FF");
    public static final int DARK_INPUT    = Color.parseColor("#0D1117");

    // ── Light palette ─────────────────────────────────────────────────────────
    public static final int LIGHT_BG      = Color.parseColor("#F6F8FA");
    public static final int LIGHT_SURFACE = Color.parseColor("#FFFFFF");
    public static final int LIGHT_CARD    = Color.parseColor("#EAEEF2");
    public static final int LIGHT_DIVIDER = Color.parseColor("#D0D7DE");
    public static final int LIGHT_TEXT    = Color.parseColor("#1F2328");
    public static final int LIGHT_MUTED   = Color.parseColor("#57606A");
    public static final int LIGHT_ACCENT  = Color.parseColor("#0969DA");
    public static final int LIGHT_GREEN   = Color.parseColor("#1A7F37");
    public static final int LIGHT_RED     = Color.parseColor("#CF222E");
    public static final int LIGHT_YELLOW  = Color.parseColor("#9A6700");
    public static final int LIGHT_CYAN    = Color.parseColor("#0550AE");
    public static final int LIGHT_INPUT   = Color.parseColor("#FFFFFF");

    /** Resolved colours for the active theme. */
    public int bg, surface, card, divider, text, muted, accent, green, red, yellow, cyan, input, terminalText;

    private final String resolvedMode;

    public ThemeManager(String mode, Activity activity) {
        this.resolvedMode = resolve(mode, activity);
        init();
    }

    public boolean isDark() { return MODE_DARK.equals(resolvedMode); }

    /** Apply window background and status bar colour. */
    public void applyWindow(Activity activity) {
        Window w = activity.getWindow();
        w.getDecorView().setBackgroundColor(bg);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            w.setStatusBarColor(surface);
        }
    }

    /**
     * Walk the entire view tree and apply colours based on each view's tag.
     * Call after setContentView() and inflate.
     */
    public void applyToView(View root) {
        if (root == null) return;
        applyOne(root);
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToView(vg.getChildAt(i));
            }
        }
    }

    private void applyOne(View v) {
        Object tag = v.getTag();
        if (tag == null) return;
        String t = tag.toString();

        if ("bg".equals(t)) {
            v.setBackgroundColor(bg);
        } else if ("surface".equals(t)) {
            v.setBackgroundColor(surface);
        } else if ("card".equals(t)) {
            v.setBackgroundColor(card);
        } else if ("divider".equals(t)) {
            v.setBackgroundColor(divider);
        } else if ("input".equals(t)) {
            v.setBackgroundColor(input);
            if (v instanceof EditText) {
                ((EditText) v).setTextColor(text);
                ((EditText) v).setHintTextColor(muted);
            }
        } else if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if ("text".equals(t))   tv.setTextColor(text);
            else if ("accent".equals(t))  tv.setTextColor(accent);
            else if ("muted".equals(t))   tv.setTextColor(muted);
            else if ("green".equals(t))   tv.setTextColor(green);
            else if ("red".equals(t))     tv.setTextColor(red);
            else if ("yellow".equals(t))  tv.setTextColor(yellow);
            else if ("cyan".equals(t))    tv.setTextColor(cyan);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolve(String mode, Activity activity) {
        if (MODE_SYSTEM.equals(mode)) {
            int uiMode = activity.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            return (uiMode == Configuration.UI_MODE_NIGHT_YES) ? MODE_DARK : MODE_LIGHT;
        }
        return MODE_LIGHT.equals(mode) ? MODE_LIGHT : MODE_DARK;
    }

    private void init() {
        if (isDark()) {
            bg           = DARK_BG;
            surface      = DARK_SURFACE;
            card         = DARK_CARD;
            divider      = DARK_DIVIDER;
            text         = DARK_TEXT;
            muted        = DARK_MUTED;
            accent       = DARK_ACCENT;
            green        = DARK_GREEN;
            red          = DARK_RED;
            yellow       = DARK_YELLOW;
            cyan         = DARK_CYAN;
            input        = DARK_INPUT;
            terminalText = DARK_GREEN;
        } else {
            bg           = LIGHT_BG;
            surface      = LIGHT_SURFACE;
            card         = LIGHT_CARD;
            divider      = LIGHT_DIVIDER;
            text         = LIGHT_TEXT;
            muted        = LIGHT_MUTED;
            accent       = LIGHT_ACCENT;
            green        = LIGHT_GREEN;
            red          = LIGHT_RED;
            yellow       = LIGHT_YELLOW;
            cyan         = LIGHT_CYAN;
            input        = LIGHT_INPUT;
            terminalText = LIGHT_GREEN;
        }
    }
}