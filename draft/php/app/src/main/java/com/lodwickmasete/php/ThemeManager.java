package com.lodwickmasete.php;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

/**
 * ThemeManager — applies dark / light / system theme to activities
 * without AppCompat or DayNight themes.
 *
 * Call ThemeManager.apply(activity, mode) before setContentView().
 * Call ThemeManager.applyToView(root, mode) to re-tint a live view tree.
 *
 * Palette:
 *   Dark  — bg #0D1117, surface #161B22, card #21262D, accent #58A6FF
 *   Light — bg #F6F8FA, surface #FFFFFF, card #EAEEF2, accent #0969DA
 */
public class ThemeManager {

    public static final String MODE_DARK   = "dark";
    public static final String MODE_LIGHT  = "light";
    public static final String MODE_SYSTEM = "system";

    // ── Dark palette ──────────────────────────────────────────────────────────
    public static final int DARK_BG       = Color.parseColor("#0D1117");
    public static final int DARK_SURFACE  = Color.parseColor("#161B22");
    public static final int DARK_CARD     = Color.parseColor("#21262D");
    public static final int DARK_TEXT     = Color.parseColor("#E6EDF3");
    public static final int DARK_MUTED    = Color.parseColor("#7D8590");
    public static final int DARK_ACCENT   = Color.parseColor("#58A6FF");
    public static final int DARK_GREEN    = Color.parseColor("#3FB950");
    public static final int DARK_RED      = Color.parseColor("#F85149");
    public static final int DARK_YELLOW   = Color.parseColor("#D29922");
    public static final int DARK_CYAN     = Color.parseColor("#00E5FF");

    // ── Light palette ─────────────────────────────────────────────────────────
    public static final int LIGHT_BG      = Color.parseColor("#F6F8FA");
    public static final int LIGHT_SURFACE = Color.parseColor("#FFFFFF");
    public static final int LIGHT_CARD    = Color.parseColor("#EAEEF2");
    public static final int LIGHT_TEXT    = Color.parseColor("#1F2328");
    public static final int LIGHT_MUTED   = Color.parseColor("#57606A");
    public static final int LIGHT_ACCENT  = Color.parseColor("#0969DA");
    public static final int LIGHT_GREEN   = Color.parseColor("#1A7F37");
    public static final int LIGHT_RED     = Color.parseColor("#CF222E");
    public static final int LIGHT_YELLOW  = Color.parseColor("#9A6700");
    public static final int LIGHT_CYAN    = Color.parseColor("#0550AE");

    /** Resolved colours for the active theme — set by apply(). */
    public int bg, surface, card, text, muted, accent, green, red, yellow, terminalText;

    private String resolvedMode;

    public ThemeManager(String mode, Activity activity) {
        this.resolvedMode = resolve(mode, activity);
        init();
    }

    public boolean isDark() {
        return MODE_DARK.equals(resolvedMode);
    }

    /** Apply window background colour to the activity. */
    public void applyWindow(Activity activity) {
        Window w = activity.getWindow();
        w.getDecorView().setBackgroundColor(bg);
        // Status bar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            w.setStatusBarColor(surface);
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
            text         = DARK_TEXT;
            muted        = DARK_MUTED;
            accent       = DARK_ACCENT;
            green        = DARK_GREEN;
            red          = DARK_RED;
            yellow       = DARK_YELLOW;
            terminalText = DARK_GREEN;
        } else {
            bg           = LIGHT_BG;
            surface      = LIGHT_SURFACE;
            card         = LIGHT_CARD;
            text         = LIGHT_TEXT;
            muted        = LIGHT_MUTED;
            accent       = LIGHT_ACCENT;
            green        = LIGHT_GREEN;
            red          = LIGHT_RED;
            yellow       = LIGHT_YELLOW;
            terminalText = LIGHT_GREEN;
        }
    }
}