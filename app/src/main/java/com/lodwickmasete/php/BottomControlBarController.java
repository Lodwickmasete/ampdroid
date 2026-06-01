package com.lodwickmasete.php;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class BottomControlBarController {

    // ── Visibility states ──────────────────────────────────────────────────
    public static final int STATE_EXPANDED  = 0;   // all rows shown
    public static final int STATE_PARTIAL   = 1;   // partial rows shown (per settings)
    public static final int STATE_COLLAPSED = 2;   // only handle shown

    // ── Partial-row bitmask flags ──────────────────────────────────────────
    public static final int ROW_START_STOP  = 1;   // Start All / Stop All
    public static final int ROW_SERVICES    = 2;   // PHP / Apache / Database
    public static final int ROW_TOOLS       = 4;   // Terminal / Logs / Restart
    public static final int ROW_EXTRA       = 8;   // Config …
    public static final int ROW_SMALL_GRID  = 16;  // Files / Upload / Browser / Api

    // Default: show Start/Stop row when partially collapsed
    private int partialVisibilityMask = ROW_START_STOP;

    private final Activity activity;
    private final View controlBar;

    // Handle (always visible)
    private LinearLayout controlBarHeader;
    private ImageView chevronIcon;

    // Content rows
    private View rowStartStop;
    private View rowServices;
    private View rowTools;
    private View rowExtra;
    private View rowSmallGrid;

    private int currentState = STATE_EXPANDED;

    // Callbacks for MainActivity wiring
    private ButtonCallback buttonCallback;

    public interface ButtonCallback {
        // Called on normal click
        void onButtonClick(int buttonId);
        // Called on long click (return true to consume)
        boolean onButtonLongClick(int buttonId);
    }

    public BottomControlBarController(Activity activity, View controlBarView) {
        this.activity = activity;
        this.controlBar = controlBarView;
        init();
    }

    // ── Init ───────────────────────────────────────────────────────────────

    private void init() {
        controlBarHeader = (LinearLayout) controlBar.findViewById(R("controlBarHeader", "id"));
        chevronIcon      = (ImageView)    controlBar.findViewById(R("chevronIcon",      "id"));

        rowStartStop = controlBar.findViewById(R("rowStartStop", "id"));
        rowServices  = controlBar.findViewById(R("rowServices",  "id"));
        rowTools     = controlBar.findViewById(R("rowTools",     "id"));
        rowExtra     = controlBar.findViewById(R("rowExtra",     "id"));
        rowSmallGrid = controlBar.findViewById(R("rowSmallGrid", "id"));

        // Handle click → cycle through states
        if (controlBarHeader != null) {
            controlBarHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cycleState();
                }
            });
        }

        wireButtons();
        applyState(STATE_PARTIAL, false);
    }

    // ── State cycling ──────────────────────────────────────────────────────

    /**
     * Cycles: EXPANDED → PARTIAL → COLLAPSED → EXPANDED
     * Skips PARTIAL when partialVisibilityMask == 0 (show nothing partial).
     */
    public void cycleState() {
        int next;
        if (currentState == STATE_EXPANDED) {
            next = (partialVisibilityMask != 0) ? STATE_PARTIAL : STATE_COLLAPSED;
        } else if (currentState == STATE_PARTIAL) {
            next = STATE_EXPANDED;  
        } else {
            next = STATE_COLLAPSED;
        }
        applyState(next, true);
    }

    public void setState(int state) {
        applyState(state, true);
    }

    private void applyState(int state, boolean animate) {
        currentState = state;
        updateChevron();

        if (state == STATE_EXPANDED) {
            setRowVisibility(rowStartStop, true,  animate);
            setRowVisibility(rowServices,  true,  animate);
            setRowVisibility(rowTools,     true,  animate);
            setRowVisibility(rowExtra,     true,  animate);
            setRowVisibility(rowSmallGrid, true,  animate);
        } else if (state == STATE_PARTIAL) {
            setRowVisibility(rowStartStop, (partialVisibilityMask & ROW_START_STOP) != 0, animate);
            setRowVisibility(rowServices,  (partialVisibilityMask & ROW_SERVICES)   != 0, animate);
            setRowVisibility(rowTools,     (partialVisibilityMask & ROW_TOOLS)       != 0, animate);
            setRowVisibility(rowExtra,     (partialVisibilityMask & ROW_EXTRA)       != 0, animate);
            setRowVisibility(rowSmallGrid, (partialVisibilityMask & ROW_SMALL_GRID)  != 0, animate);
        } else { // COLLAPSED
            setRowVisibility(rowStartStop, false, animate);
            setRowVisibility(rowServices,  false, animate);
            setRowVisibility(rowTools,     false, animate);
            setRowVisibility(rowExtra,     false, animate);
            setRowVisibility(rowSmallGrid, false, animate);
        }
    }

    private void setRowVisibility(final View row, boolean visible, boolean animate) {
        if (row == null) return;
        if (visible) {
            row.setVisibility(View.VISIBLE);
            if (animate) {
                Animation slideDown = new TranslateAnimation(0, 0, -row.getHeight(), 0);
                slideDown.setDuration(220);
                row.startAnimation(slideDown);
            }
        } else {
            if (animate) {
                Animation slideUp = new TranslateAnimation(0, 0, 0, -row.getHeight());
                slideUp.setDuration(180);
                slideUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation a) {}
                    @Override public void onAnimationEnd(Animation a) {
                        row.setVisibility(View.GONE);
                        row.clearAnimation();
                    }
                    @Override public void onAnimationRepeat(Animation a) {}
                });
                row.startAnimation(slideUp);
            } else {
                row.setVisibility(View.GONE);
            }
        }
    }

    // ── Chevron ────────────────────────────────────────────────────────────

    private void updateChevron() {
        if (chevronIcon == null) return;
        // Expanded → chevron points DOWN (bar can be shrunk)
        // Partial / Collapsed → chevron points UP (bar can be expanded)
        boolean pointingDown = (currentState == STATE_EXPANDED);
        chevronIcon.setRotation(pointingDown ? 180f : 0f);
    }

    // ── Partial-visibility settings ────────────────────────────────────────

    /**
     * Set which rows remain visible in STATE_PARTIAL.
     * Use bitmask of ROW_* constants.
     * Pass 0 to skip the partial state entirely (go straight to collapsed).
     */
    public void setPartialVisibilityMask(int mask) {
        this.partialVisibilityMask = mask;
        if (currentState == STATE_PARTIAL) {
            applyState(STATE_PARTIAL, false);
        }
    }

    public int getPartialVisibilityMask() {
        return partialVisibilityMask;
    }

    // ── Button wiring ──────────────────────────────────────────────────────

    private void wireButtons() {
        wireButton(R("btnStartAll",    "id"));
        wireButton(R("btnStopAll",     "id"));
        wireButton(R("btnTogglePhp",   "id"));
        wireButton(R("btnToggleApache","id"));
        wireButton(R("btnToggleDb",    "id"));
        wireButton(R("btnTerminal",    "id"));
     //   wireButton(R("btnLogs",        "id"));
     //   wireButton(R("btnRestart",     "id"));
        wireButton(R("btnConfig",      "id"));
        wireButton(R("btnFiles",       "id"));
       wireButton(R("btnUpload",      "id"));
    /*    wireButton(R("btnBrowser",     "id"));
        wireButton(R("btnApi",         "id"));*/
    }

    private void wireButton(final int id) {
        View btn = controlBar.findViewById(id);
        if (btn == null) return;

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonCallback != null) buttonCallback.onButtonClick(id);
            }
        });

        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                startLongPressAnimation(v);
                if (buttonCallback != null) return buttonCallback.onButtonLongClick(id);
                return true;
            }
        });
    }

    // ── Long-press "activating" pulse animation ────────────────────────────

    /**
     * Pulses the button background between its current color and a bright
     * cyan/teal accent — giving the "turning on, waiting…" vibe.
     * Call stopLongPressAnimation(btn) when the action resolves.
     */
    public void startLongPressAnimation(final View btn) {
        if (btn == null) return;

        // Derive base color from current background
        int baseColor = getViewBgColor(btn);
        int pulseColor = Color.parseColor("#00BFAE"); // cyan-teal accent

        ValueAnimator animator = ValueAnimator.ofObject(
                new ArgbEvaluator(), baseColor, pulseColor);
        animator.setDuration(500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                btn.setBackgroundColor((int) va.getAnimatedValue());
            }
        });
        animator.start();

        // Store animator on the view so we can cancel it later.
        // Plain setTag(value) is safe — no resource ID needed.
        btn.setTag(animator);
    }

    /**
     * Stop the pulse and restore a specific background color.
     */
    public void stopLongPressAnimation(View btn, int restoreColor) {
        if (btn == null) return;
        Object tag = btn.getTag();
        if (tag instanceof ValueAnimator) {
            ((ValueAnimator) tag).cancel();
            btn.setTag(null);
        }
        btn.setBackgroundColor(restoreColor);
    }

    /**
     * Convenience: stop animation on any button by resource id.
     */
    public void stopLongPressAnimation(int buttonId, int restoreColor) {
        stopLongPressAnimation(controlBar.findViewById(buttonId), restoreColor);
    }

    // ── Service-state visual update ────────────────────────────────────────

    /**
     * Toggle the visual "on/off" state of a service button.
     * active=true  → green tint (#238636)
     * active=false → default dark (#30363D)
     */
    public void setServiceActive(int buttonId, boolean active) {
        View btn = controlBar.findViewById(buttonId);
        if (btn == null) return;
        btn.setBackgroundColor(Color.parseColor(active ? "#238636" : "#30363D"));
    }

    // ── Public getters ─────────────────────────────────────────────────────

    public int getCurrentState() { return currentState; }
    public View getControlBar()  { return controlBar; }
    public boolean isExpanded()  { return currentState == STATE_EXPANDED; }

    public void setButtonCallback(ButtonCallback cb) {
        this.buttonCallback = cb;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private int R(String name, String type) {
        return activity.getResources().getIdentifier(name, type, activity.getPackageName());
    }

    private int getViewBgColor(View v) {
        Drawable bg = v.getBackground();
        if (bg instanceof ColorDrawable) return ((ColorDrawable) bg).getColor();
        return Color.parseColor("#30363D"); // fallback dark
    }
}