package com.dismal.android.leanbacklauncher.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class Partner {
    private static final Map<String, Integer> sDefaultPrioritiesMap = new HashMap();
    private static final Object sLock;
    private static Partner sPartner;
    private static boolean sSearched;
    private final String mPackageName;
    private final String mReceiverName;
    private final Resources mResources;
    private HashMap<String, Integer> mRowPositions = new HashMap<>();
    private boolean mRowDataReady = false;

    static {
        sDefaultPrioritiesMap.put("input_type_combined_tuners", -3);
        sDefaultPrioritiesMap.put("input_type_tuner", 0);
        sDefaultPrioritiesMap.put("input_type_cec_logical", -2);
        sDefaultPrioritiesMap.put("input_type_hdmi", 1007);
        sDefaultPrioritiesMap.put("input_type_dvi", 1006);
        sDefaultPrioritiesMap.put("input_type_component", 1004);
        sDefaultPrioritiesMap.put("input_type_svideo", 1002);
        sDefaultPrioritiesMap.put("input_type_composite", 1001);
        sDefaultPrioritiesMap.put("input_type_displayport", 1008);
        sDefaultPrioritiesMap.put("input_type_vga", 1005);
        sDefaultPrioritiesMap.put("input_type_scart", 1003);
        sDefaultPrioritiesMap.put("input_type_other", 1000);
        sSearched = false;
        sLock = new Object();
    }

    public static Partner get(Context context) {
        long startTime = System.currentTimeMillis();
        PackageManager pm = context.getPackageManager();
        synchronized (sLock) {
            if (!sSearched) {
                long t1 = System.currentTimeMillis();
                ResolveInfo info = getPartnerResolveInfo(pm, null);
                Log.d("LeanbackLauncher", "Partner: getPartnerResolveInfo took " + (System.currentTimeMillis() - t1) + "ms");
                if (info != null) {
                    String packageName = info.activityInfo.packageName;
                    String receiverName = info.activityInfo.name;
                    try {
                        Resources res = pm.getResourcesForApplication(packageName);
                        sPartner = new Partner(packageName, receiverName, res);
                        sPartner.sendInitBroadcast(context);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w("Partner", "Failed to find resources for " + packageName);
                    }
                }
                sSearched = true;
                if (sPartner == null) {
                    sPartner = new Partner(null, null, null);
                }
            }
        }
        Log.d("LeanbackLauncher", "Partner.get total took " + (System.currentTimeMillis() - startTime) + "ms");
        return sPartner;
    }

    public static void resetIfNecessary(Context context, String packageName) {
        synchronized (sLock) {
            if (sPartner != null && !TextUtils.isEmpty(packageName) && packageName.equals(sPartner.mPackageName)) {
                sSearched = false;
                sPartner = null;
                get(context);
            }
        }
    }

    private Partner(String packageName, String receiverName, Resources res) {
        this.mPackageName = packageName;
        this.mReceiverName = receiverName;
        this.mResources = res;
    }

    private void sendInitBroadcast(Context context) {
        if (TextUtils.isEmpty(this.mPackageName) || TextUtils.isEmpty(this.mReceiverName)) {
            return;
        }
        Intent intent = new Intent("com.dismal.android.leanbacklauncher.action.PARTNER_CUSTOMIZATION");
        ComponentName componentName = new ComponentName(this.mPackageName, this.mReceiverName);
        intent.setComponent(componentName);
        intent.setFlags(268435456);
        int cutoff = context.getResources().getInteger(R.integer.two_row_cut_off);
        intent.putExtra("com.dismal.android.leanbacklauncher.extra.ROW_WRAPPING_CUTOFF", cutoff);
        context.sendBroadcast(intent);
    }

    public Drawable getSystemBackground() {
        int nameResId;
        int wallpaperResId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (nameResId = this.mResources.getIdentifier("partner_wallpaper", "string", this.mPackageName)) == 0) {
            return null;
        }
        String name = this.mResources.getString(nameResId);
        if (TextUtils.isEmpty(name) || (wallpaperResId = this.mResources.getIdentifier(name, "drawable", this.mPackageName)) == 0) {
            return null;
        }
        Drawable wallpaper = this.mResources.getDrawable(wallpaperResId, null);
        return wallpaper;
    }

    public Ranker.SortingMode getAppSortingMode() {
        Ranker.SortingMode sortingMode = Ranker.SortingMode.FIXED;
        if (this.mResources != null && !TextUtils.isEmpty(this.mPackageName)) {
            int nameResId = this.mResources.getIdentifier("partner_app_sorting_mode", "string", this.mPackageName);
            if (nameResId != 0) {
                return Ranker.SortingMode.valueOf(this.mResources.getString(nameResId).toUpperCase());
            }
            boolean hasPartnerRow = isRowEnabled("partner_row");
            return hasPartnerRow ? Ranker.SortingMode.RECENCY : Ranker.SortingMode.FIXED;
        }
        return sortingMode;
    }

    public Drawable getCustomSearchIcon() {
        int nameResId;
        int iconResId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (nameResId = this.mResources.getIdentifier("partner_search_icon", "string", this.mPackageName)) == 0) {
            return null;
        }
        String name = this.mResources.getString(nameResId);
        if (TextUtils.isEmpty(name) || (iconResId = this.mResources.getIdentifier(name, "drawable", this.mPackageName)) == 0) {
            return null;
        }
        Drawable icon = this.mResources.getDrawable(iconResId, null);
        return icon;
    }

    public ComponentName getWidgetComponentName() {
        int nameResId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (nameResId = this.mResources.getIdentifier("partner_widget_provider_component_name", "string", this.mPackageName)) == 0) {
            return null;
        }
        ComponentName comp = ComponentName.unflattenFromString(this.mResources.getString(nameResId));
        return comp;
    }

    public String getPartnerFontName() {
        int nameResId;
        if (this.mResources != null && !TextUtils.isEmpty(this.mPackageName) && (nameResId = this.mResources.getIdentifier("partner_font", "string", this.mPackageName)) != 0) {
            String name = this.mResources.getString(nameResId);
            if (!TextUtils.isEmpty(name)) {
                return name;
            }
        }
        return null;
    }

    public boolean isRowEnabled(String row) {
        return getRowPosition(row) != -1;
    }

    public int getRowPosition(String row) {
        if (!this.mRowDataReady) {
            fetchRowsData();
        }
        if (!this.mRowDataReady) {
            return -1;
        }
        int position = this.mRowPositions.get(row.trim().toLowerCase()).intValue();
        return position;
    }

    public String getRowTitle(String row, String defaultValue) {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier(row + "_title", "string", this.mPackageName)) == 0) {
            return defaultValue;
        }
        String title = this.mResources.getString(resId);
        return title;
    }

    public Drawable getRowIcon(String row) {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier(row + "_icon", "drawable", this.mPackageName)) == 0) {
            return null;
        }
        Drawable icon = this.mResources.getDrawable(resId, null);
        return icon;
    }

    public String[] getOutOfBoxOrder() {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier("partner_out_of_box_order", "array", this.mPackageName)) == 0) {
            return null;
        }
        String[] order = this.mResources.getStringArray(resId);
        return order;
    }

    public boolean showLiveTvOnStartUp() {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier("partner_show_live_tv_on_start_up", "bool", this.mPackageName)) == 0) {
            return false;
        }
        boolean showLiveTvOnStartUp = this.mResources.getBoolean(resId);
        return showLiveTvOnStartUp;
    }

    public Map<Integer, Integer> getInputsOrderMap() {
        HashMap<Integer, Integer> map = new HashMap<>();
        if (this.mResources != null && !TextUtils.isEmpty(this.mPackageName)) {
            String[] inputsArray = null;
            int resId = this.mResources.getIdentifier("home_screen_inputs_ordering", "array", this.mPackageName);
            if (resId != 0) {
                inputsArray = this.mResources.getStringArray(resId);
            }
            if (inputsArray != null) {
                int priority = 0;
                for (String str : inputsArray) {
                    Integer type = sDefaultPrioritiesMap.get(str);
                    if (type != null) {
                        map.put(type, Integer.valueOf(priority));
                        priority++;
                    }
                }
            }
        }
        return map;
    }

    public boolean showPhysicalTunersSeparately() {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier("show_physical_tuners_separately", "bool", this.mPackageName)) == 0) {
            return false;
        }
        boolean showPhysicalTunersSeparately = this.mResources.getBoolean(resId);
        return showPhysicalTunersSeparately;
    }

    public boolean disableDiconnectedInputs() {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier("disable_disconnected_inputs", "bool", this.mPackageName)) == 0) {
            return true;
        }
        boolean disableDiconnectedInputs = this.mResources.getBoolean(resId);
        return disableDiconnectedInputs;
    }

    public String getBundledTunerTitle() {
        int nameResId;
        if (this.mResources != null && !TextUtils.isEmpty(this.mPackageName) && (nameResId = this.mResources.getIdentifier("bundled_tuner_title", "string", this.mPackageName)) != 0) {
            String name = this.mResources.getString(nameResId);
            if (!TextUtils.isEmpty(name)) {
                return name;
            }
        }
        return null;
    }

    public Drawable getBundledTunerBanner() {
        int resId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (resId = this.mResources.getIdentifier("bundled_tuner_banner", "drawable", this.mPackageName)) == 0) {
            return null;
        }
        Drawable banner = this.mResources.getDrawable(resId, null);
        return banner;
    }

    public int getBundledTunerLabelColorOption(int defaultColor) {
        int nameResId;
        if (this.mResources == null || TextUtils.isEmpty(this.mPackageName) || (nameResId = this.mResources.getIdentifier("bundled_tuner_label_color_option", "integer", this.mPackageName)) == 0) {
            return defaultColor;
        }
        int color = this.mResources.getInteger(nameResId);
        return color;
    }

    private void fetchRowsData() {
        int resId;
        String[] rowsArray = null;
        if (this.mResources != null && !TextUtils.isEmpty(this.mPackageName) && (resId = this.mResources.getIdentifier("home_screen_row_ordering", "array", this.mPackageName)) != 0) {
            rowsArray = this.mResources.getStringArray(resId);
        }
        if (rowsArray == null) {
            return;
        }
        this.mRowPositions.clear();
        this.mRowPositions.put("partner_row", -1);
        this.mRowPositions.put("apps_row", -1);
        this.mRowPositions.put("games_row", -1);
        this.mRowPositions.put("inputs_row", -1);
        this.mRowPositions.put("settings_row", -1);
        int position = 0;
        for (int i = 0; i < rowsArray.length; i++) {
            Integer prev = this.mRowPositions.get(rowsArray[i]);
            if (prev.intValue() == -1) {
                this.mRowPositions.put(rowsArray[i], Integer.valueOf(position));
                position++;
            }
        }
        this.mRowDataReady = true;
    }

    private static ResolveInfo getPartnerResolveInfo(PackageManager pm, ComponentName name) {
        Intent intent = new Intent("com.dismal.android.leanbacklauncher.action.PARTNER_CUSTOMIZATION");
        if (name != null) {
            intent.setPackage(name.getPackageName());
        }
        for (ResolveInfo info : pm.queryBroadcastReceivers(intent, 0)) {
            if (isSystemApp(info)) {
                return info;
            }
        }
        return null;
    }

    private static boolean isSystemApp(ResolveInfo info) {
        return (info.activityInfo == null || (info.activityInfo.applicationInfo.flags & 1) == 0) ? false : true;
    }
}
