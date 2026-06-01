package com.lodwickmasete.php;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable drawer navigation component with collapsible menu items and swipe gestures.
 * 
 * Usage:
 * 
 * // In Any activity's onCreate after setContentView:
 * DrawerNavigation drawer = new DrawerNavigation(this);
 * drawer.setMenuItems(new String[]{"Home", "Settings", "About"}, new String[]{"🏠", "⚙️", "ℹ️"});
 * drawer.setOnMenuItemSelectedListener(new DrawerNavigation.OnMenuItemSelectedListener() { ... });
 * drawer.openDrawer();
 * drawer.closeDrawer();
 */
public class DrawerNavigation {

    // ─── Callback Interface ─────────────────────────────────────────────────
    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int position, String title);
        void onSubMenuItemSelected(int parentPosition, int childPosition, String title);
    }

    // ─── Menu Item Classes ──────────────────────────────────────────────────
    public static class MenuItem {
        String title;
        String icon;
        boolean isCollapsible;
        boolean isExpanded;
        List<MenuItem> subItems;
        View itemView;
        LinearLayout subItemsContainer;
        
        public MenuItem(String title, String icon) {
            this.title = title;
            this.icon = icon;
            this.isCollapsible = false;
            this.isExpanded = false;
            this.subItems = new ArrayList<>();
        }
        
        public void addSubItem(String title, String icon) {
            subItems.add(new MenuItem(title, icon));
        }
        
        public List<MenuItem> getSubItems() {
            return subItems;
        }
    }

    // ─── Configuration ──────────────────────────────────────────────────────
    private Activity activity;
    private FrameLayout rootContainer;
    private LinearLayout drawerPanel;
    private View drawerScrim;
    private LinearLayout navList;
    private boolean isDrawerOpen = false;
    private OnMenuItemSelectedListener listener;
    private GestureDetector gestureDetector;
    private float startX = 0;
    private boolean isDragging = false;
    
    // Menu items
    private List<MenuItem> menuItems = new ArrayList<>();
    private int currentSelectedPosition = -1;
    private int currentSelectedSubPosition = -1;
    
    // Theme colors
    private int drawerBackgroundColor = Color.parseColor("#1E1E1E");
    private int headerBackgroundColor = Color.parseColor("#2D2D2D");
    private int itemActiveColor = Color.parseColor("#3D3D3D");
    private int subItemColor = Color.parseColor("#252525");
    private int textColor = Color.WHITE;
    private int accentColor = Color.parseColor("#4CAF50");
    private int mutedColor = Color.parseColor("#888888");
    private int dividerColor = Color.parseColor("#404040");
    
    private int drawerWidthDp = 280;
    
    // Click listeners
    private View.OnClickListener scrimClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeDrawer();
        }
    };
    
    // ─── Constructor ────────────────────────────────────────────────────────
    
    public DrawerNavigation(Activity activity) {
        this.activity = activity;
        setupDrawer();
        setupGestureDetector();
    }
    
    // ─── Public Methods ─────────────────────────────────────────────────────
    
    /**
     * Set menu items with titles and icons
     */
    public void setMenuItems(String[] titles, String[] icons) {
        menuItems.clear();
        for (int i = 0; i < titles.length; i++) {
            String icon = (icons != null && i < icons.length) ? icons[i] : "•";
            menuItems.add(new MenuItem(titles[i], icon));
        }
        buildMenuItems();
    }
    
    /**
     * Set menu items with titles only
     */
    public void setMenuItems(String[] titles) {
        String[] icons = new String[titles.length];
        for (int i = 0; i < titles.length; i++) {
            icons[i] = "•";
        }
        setMenuItems(titles, icons);
    }
    
    /**
     * Add a collapsible menu item with sub-items
     */
    public MenuItem addCollapsibleItem(String title, String icon) {
        MenuItem item = new MenuItem(title, icon);
        item.isCollapsible = true;
        menuItems.add(item);
        buildMenuItems();
        return item;
    }
    
    /**
     * Set the listener for menu item selections
     */
    public void setOnMenuItemSelectedListener(OnMenuItemSelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Set custom colors for the drawer
     */
    public void setColors(int drawerBg, int headerBg, int itemActive, int subItemBg,
                          int textColor, int accentColor, int mutedColor, int dividerColor) {
        this.drawerBackgroundColor = drawerBg;
        this.headerBackgroundColor = headerBg;
        this.itemActiveColor = itemActive;
        this.subItemColor = subItemBg;
        this.textColor = textColor;
        this.accentColor = accentColor;
        this.mutedColor = mutedColor;
        this.dividerColor = dividerColor;
        applyTheme();
    }
    
    /**
     * Set drawer width in dp (default 280)
     */
    public void setDrawerWidth(int widthDp) {
        this.drawerWidthDp = widthDp;
        updateDrawerWidth();
    }
    
    /**
     * Open the drawer with animation
     */
    public void openDrawer() {
        if (!isDrawerOpen) {
            isDrawerOpen = true;
            drawerScrim.setVisibility(View.VISIBLE);
            drawerScrim.setAlpha(0f);
            drawerScrim.animate().alpha(1f).setDuration(240);
            
            drawerPanel.animate()
                .translationX(0)
                .setDuration(240)
                .start();
        }
    }
    
    /**
     * Close the drawer with animation
     */
    public void closeDrawer() {
        if (isDrawerOpen) {
            isDrawerOpen = false;
            drawerScrim.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        drawerScrim.setVisibility(View.GONE);
                    }
                });
            
            drawerPanel.animate()
                .translationX(-getDrawerWidthPx())
                .setDuration(200)
                .start();
        }
    }
    
    /**
     * Toggle drawer open/close
     */
    public void toggleDrawer() {
        if (isDrawerOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }
    
    /**
     * Check if drawer is open
     */
    public boolean isDrawerOpen() {
        return isDrawerOpen;
    }
    
    /**
     * Set currently selected menu item by position
     */
    public void setSelectedItem(int position) {
        this.currentSelectedPosition = position;
        this.currentSelectedSubPosition = -1;
        highlightSelectedItem();
    }
    
    /**
     * Get the drawer panel view
     */
    public LinearLayout getDrawerPanel() {
        return drawerPanel;
    }
    
    /**
     * Add a custom header view
     */
    public void addHeaderView(View headerView) {
        if (drawerPanel != null && headerView != null) {
            drawerPanel.addView(headerView, 0);
        }
    }
    
    /**
     * Add a custom footer view
     */
    public void addFooterView(View footerView) {
        if (drawerPanel != null && footerView != null) {
            drawerPanel.addView(footerView);
        }
    }
    
    /**
     * Handle back button press
     */
    public boolean onBackPressed() {
        if (isDrawerOpen) {
            closeDrawer();
            return true;
        }
        return false;
    }
    
    /**
     * Pass touch events to the drawer for swipe gestures
     * Call this in any activity's dispatchTouchEvent or onTouchEvent
     */
    public void onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                isDragging = false;
                break;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - startX;
                
                if (!isDragging && Math.abs(deltaX) > dp(10)) {
                    isDragging = true;
                }
                
                if (isDragging) {
                    float newX = drawerPanel.getTranslationX() + deltaX;
                    if (newX > 0 && newX <= getDrawerWidthPx()) {
                        drawerPanel.setTranslationX(newX);
                        float alpha = newX / getDrawerWidthPx();
                        drawerScrim.setVisibility(View.VISIBLE);
                        drawerScrim.setAlpha(alpha);
                    } else if (newX < 0) {
                        drawerPanel.setTranslationX(0);
                    } else if (newX > getDrawerWidthPx()) {
                        drawerPanel.setTranslationX(getDrawerWidthPx());
                    }
                    startX = event.getRawX();
                }
                break;
                
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    float currentX = drawerPanel.getTranslationX();
                    if (currentX > getDrawerWidthPx() / 2) {
                        openDrawer();
                    } else {
                        closeDrawer();
                    }
                    isDragging = false;
                }
                break;
        }
    }
    
    // ─── Private Setup Methods ──────────────────────────────────────────────
    
    private void setupDrawer() {
        FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);
        
        // can be Wraped content in FrameLayout if needed
        if (!(root.getChildAt(0) instanceof FrameLayout)) {
            View content = root.getChildAt(0);
            root.removeView(content);
            
            rootContainer = new FrameLayout(activity);
            rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rootContainer.addView(content);
            root.addView(rootContainer);
        } else {
            rootContainer = (FrameLayout) root.getChildAt(0);
        }
        
        // Ensure root container can handle touch events
        rootContainer.setClickable(true);
        rootContainer.setFocusable(true);
        
        // 1) Scrim (dark overlay) - initially hidden
        drawerScrim = new View(activity);
        drawerScrim.setBackgroundColor(Color.parseColor("#99000000"));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setAlpha(0f);
        drawerScrim.setOnClickListener(scrimClickListener);
        
        FrameLayout.LayoutParams scrimLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootContainer.addView(drawerScrim, scrimLp);
        
        // 2) Drawer panel - positioned off-screen initially
        drawerPanel = buildDrawer();
        FrameLayout.LayoutParams drawerLp = new FrameLayout.LayoutParams(
            getDrawerWidthPx(), ViewGroup.LayoutParams.MATCH_PARENT);
        drawerLp.gravity = Gravity.START;
        drawerPanel.setTranslationX(-getDrawerWidthPx());
        rootContainer.addView(drawerPanel, drawerLp);
        
        // Make drawer panel receive touch events
        drawerPanel.setClickable(true);
    }
    
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > dp(100) && Math.abs(velocityX) > 1000) {
                    if (diffX > 0 && !isDrawerOpen) {
                        openDrawer();
                    } else if (diffX < 0 && isDrawerOpen) {
                        closeDrawer();
                    }
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    return true;
                }
                return false;
            }
        });
    }
    
    private LinearLayout buildDrawer() {
        LinearLayout drawer = new LinearLayout(activity);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(drawerBackgroundColor);
        
        // Scrollable area for menu items
        ScrollView navScroll = new ScrollView(activity);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        navScroll.setLayoutParams(navLp);
        navScroll.setVerticalScrollBarEnabled(true);
        
        navList = new LinearLayout(activity);
        navList.setOrientation(LinearLayout.VERTICAL);
        navList.setPadding(dp(0), dp(8), dp(0), dp(8));
        navScroll.addView(navList);
        drawer.addView(navScroll);
        
        return drawer;
    }
    
    private void buildMenuItems() {
        if (navList == null) return;
        
        navList.removeAllViews();
        
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);
            View itemView = buildNavItem(item, i);
            navList.addView(itemView);
            
            // Add divider
            if (i < menuItems.size() - 1) {
                navList.addView(makeDivider());
            }
        }
    }
    
    private View buildNavItem(final MenuItem item, final int index) {
        if (item.isCollapsible) {
            return buildCollapsibleItem(item, index);
        } else {
            return buildRegularItem(item, index);
        }
    }
    
    private View buildRegularItem(final MenuItem item, final int index) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        boolean active = (index == currentSelectedPosition && currentSelectedSubPosition == -1);
        row.setBackgroundColor(active ? itemActiveColor : Color.TRANSPARENT);
        
        TextView icon = new TextView(activity);
        icon.setText(item.icon);
        icon.setTextSize(18);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
            dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        icon.setLayoutParams(iconLp);
        
        TextView label = new TextView(activity);
        label.setText(item.title);
        label.setTextColor(active ? accentColor : textColor);
        label.setTextSize(14);
        label.setTypeface(Typeface.MONOSPACE, active ? Typeface.BOLD : Typeface.NORMAL);
        
        row.addView(icon);
        row.addView(label);
        
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedItem(index);
                closeDrawer();
                if (listener != null) {
                    listener.onMenuItemSelected(index, item.title);
                }
            }
        });
        
        item.itemView = row;
        return row;
    }
    
    private View buildCollapsibleItem(final MenuItem item, final int index) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        
        // Header row
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(20), dp(14), dp(20), dp(14));
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView icon = new TextView(activity);
        icon.setText(item.icon);
        icon.setTextSize(18);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
            dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        icon.setLayoutParams(iconLp);
        
        TextView label = new TextView(activity);
        label.setText(item.title);
        label.setTextColor(textColor);
        label.setTextSize(14);
        label.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        final TextView arrow = new TextView(activity);
        arrow.setText(item.isExpanded ? "▼" : "▶");
        arrow.setTextColor(mutedColor);
        arrow.setTextSize(12);
        
        header.addView(icon);
        header.addView(label);
        header.addView(arrow);
        
        // Sub-items container
        final LinearLayout subContainer = new LinearLayout(activity);
        subContainer.setOrientation(LinearLayout.VERTICAL);
        subContainer.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
        subContainer.setBackgroundColor(subItemColor);
        subContainer.setPadding(dp(16), dp(4), dp(0), dp(4));
        
        // Build sub-items
        for (int i = 0; i < item.subItems.size(); i++) {
            final MenuItem subItem = item.subItems.get(i);
            final int subIndex = i;
            
            LinearLayout subRow = new LinearLayout(activity);
            subRow.setOrientation(LinearLayout.HORIZONTAL);
            subRow.setPadding(dp(52), dp(12), dp(20), dp(12));
            subRow.setGravity(Gravity.CENTER_VERTICAL);
            
            boolean active = (index == currentSelectedPosition && subIndex == currentSelectedSubPosition);
            subRow.setBackgroundColor(active ? itemActiveColor : Color.TRANSPARENT);
            
            TextView subIcon = new TextView(activity);
            subIcon.setText(subItem.icon);
            subIcon.setTextSize(14);
            LinearLayout.LayoutParams subIconLp = new LinearLayout.LayoutParams(
                dp(24), ViewGroup.LayoutParams.WRAP_CONTENT);
            subIcon.setLayoutParams(subIconLp);
            
            TextView subLabel = new TextView(activity);
            subLabel.setText(subItem.title);
            subLabel.setTextColor(active ? accentColor : mutedColor);
            subLabel.setTextSize(13);
            
            subRow.addView(subIcon);
            subRow.addView(subLabel);
            
            subRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentSelectedPosition = index;
                    currentSelectedSubPosition = subIndex;
                    highlightSelectedItem();
                    closeDrawer();
                    if (listener != null) {
                        listener.onSubMenuItemSelected(index, subIndex, subItem.title);
                    }
                }
            });
            
            subContainer.addView(subRow);
            
            if (i < item.subItems.size() - 1) {
                View subDivider = new View(activity);
                subDivider.setBackgroundColor(dividerColor);
                subDivider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
                subContainer.addView(subDivider);
            }
        }
        
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.isExpanded = !item.isExpanded;
                arrow.setText(item.isExpanded ? "▼" : "▶");
                subContainer.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
            }
        });
        
        container.addView(header);
        container.addView(subContainer);
        
        item.itemView = container;
        item.subItemsContainer = subContainer;
        
        return container;
    }
    
    private void highlightSelectedItem() {
        if (navList == null) return;
        
        int menuIndex = 0;
        for (int i = 0; i < navList.getChildCount(); i++) {
            View child = navList.getChildAt(i);
            
            // Skip dividers
            if (child.getTag() != null && child.getTag().equals("divider")) {
                continue;
            }
            
            if (menuIndex < menuItems.size()) {
                MenuItem item = menuItems.get(menuIndex);
                
                if (item.isCollapsible) {
                    // Handle collapsible item
                    if (menuIndex == currentSelectedPosition && currentSelectedSubPosition == -1) {
                        // Parent item selected
                        if (item.itemView instanceof LinearLayout) {
                            LinearLayout container = (LinearLayout) item.itemView;
                            if (container.getChildAt(0) instanceof LinearLayout) {
                                LinearLayout header = (LinearLayout) container.getChildAt(0);
                                header.setBackgroundColor(itemActiveColor);
                                for (int j = 0; j < header.getChildCount(); j++) {
                                    View childView = header.getChildAt(j);
                                    if (childView instanceof TextView) {
                                        TextView tv = (TextView) childView;
                                        if (tv.getText().toString().equals(item.title)) {
                                            tv.setTextColor(accentColor);
                                            tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Reset parent
                        if (item.itemView instanceof LinearLayout) {
                            LinearLayout container = (LinearLayout) item.itemView;
                            if (container.getChildAt(0) instanceof LinearLayout) {
                                LinearLayout header = (LinearLayout) container.getChildAt(0);
                                header.setBackgroundColor(Color.TRANSPARENT);
                                for (int j = 0; j < header.getChildCount(); j++) {
                                    View childView = header.getChildAt(j);
                                    if (childView instanceof TextView) {
                                        TextView tv = (TextView) childView;
                                        if (tv.getText().toString().equals(item.title)) {
                                            tv.setTextColor(textColor);
                                            tv.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Handle regular item
                    if (item.itemView instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) item.itemView;
                        boolean isSelected = (menuIndex == currentSelectedPosition && currentSelectedSubPosition == -1);
                        row.setBackgroundColor(isSelected ? itemActiveColor : Color.TRANSPARENT);
                        
                        for (int j = 0; j < row.getChildCount(); j++) {
                            View innerChild = row.getChildAt(j);
                            if (innerChild instanceof TextView) {
                                TextView tv = (TextView) innerChild;
                                if (tv.getText().toString().equals(item.title)) {
                                    tv.setTextColor(isSelected ? accentColor : textColor);
                                    tv.setTypeface(Typeface.MONOSPACE, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                                    break;
                                }
                            }
                        }
                    }
                }
                menuIndex++;
            }
        }
    }
    
    private void applyTheme() {
        if (drawerPanel != null) {
            drawerPanel.setBackgroundColor(drawerBackgroundColor);
        }
        buildMenuItems();
    }
    
    private void updateDrawerWidth() {
        if (drawerPanel != null) {
            ViewGroup.LayoutParams params = drawerPanel.getLayoutParams();
            params.width = getDrawerWidthPx();
            drawerPanel.setLayoutParams(params);
            if (!isDrawerOpen) {
                drawerPanel.setTranslationX(-getDrawerWidthPx());
            }
        }
    }
    
    private View makeDivider() {
        View d = new View(activity);
        d.setBackgroundColor(dividerColor);
        d.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        d.setTag("divider");
        return d;
    }
    
    private int getDrawerWidthPx() {
        return dp(drawerWidthDp);
    }
    
    private int dp(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}