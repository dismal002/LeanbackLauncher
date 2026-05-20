package com.dismal.android.leanbacklauncher.util;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.dismal.android.leanbacklauncher.R;
import java.util.Calendar;
import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
public class Util {
    private Util() {
    }

    public static Intent getSearchIntent() {
        return new Intent("android.intent.action.ASSIST").addFlags(270532608);
    }

    public static int getDay(Date date) {
        if (date == null) {
            return -1;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = (cal.get(1) * 1000) + cal.get(6);
        return day;
    }

    public static Date getDate(int day) {
        if (day == -1) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(1, day / 1000);
        cal.set(6, day % 1000);
        Date date = cal.getTime();
        return date;
    }

    public static boolean isContentUri(String uriString) {
        if (TextUtils.isEmpty(uriString)) {
            return false;
        }
        return isContentUri(Uri.parse(uriString));
    }

    public static boolean isContentUri(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            return true;
        }
        return "file".equals(uri.getScheme());
    }

    public static boolean isPackagePresent(PackageManager pkgMan, String packageName) {
        try {
            if (pkgMan.getApplicationInfo(packageName, 0) == null) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isPackageOnSystem(PackageManager pkgMan, String packageName) {
        try {
            ApplicationInfo info = pkgMan.getApplicationInfo(packageName, 0);
            if (info == null) {
                return false;
            }
            if ((info.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean initialRankingApplied(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean flag = prefs.getBoolean("launcher_oob_ranking_marker", false);
        return flag;
    }

    public static void setInitialRankingAppliedFlag(Context ctx, boolean applied) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putBoolean("launcher_oob_ranking_marker", applied).commit();
    }

    public static boolean startActivitySafely(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            Log.e("LeanbackLauncher", "Could not launch intent", t);
            Toast.makeText(context, R.string.failed_launch, 0).show();
            return false;
        }
    }

    public static void startActivity(Context context, PendingIntent intent) throws IntentSender.SendIntentException {
        context.startIntentSender(intent.getIntentSender(), null, 268435456, 268435456, 0);
    }

    public static long getInstallTimeForPackage(Context context, String pkgName) {
        PackageManager pkgMan = context.getPackageManager();
        if (pkgMan != null) {
            try {
                PackageInfo info = pkgMan.getPackageInfo(pkgName, 0);
                if (info != null) {
                    return info.firstInstallTime;
                }
                return -1L;
            } catch (PackageManager.NameNotFoundException e) {
                return -1L;
            }
        }
        return -1L;
    }

    public static int getWidgetId(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt("widget_id", 0);
    }

    public static ComponentName getWidgetComponentName(Context ctx) {
        String name = PreferenceManager.getDefaultSharedPreferences(ctx).getString("widget_component_name", null);
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        ComponentName comp = ComponentName.unflattenFromString(name);
        return comp;
    }

    public static void setWidget(Context ctx, int id, ComponentName name) {
        if (id == 0 || name == null) {
            clearWidget(ctx);
        } else {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putInt("widget_id", id).putString("widget_component_name", name.flattenToString()).apply();
        }
    }

    public static void clearWidget(Context ctx) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().remove("widget_id").remove("widget_component_name").apply();
    }

    public static Bitmap getSizeCappedBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (image == null) {
            return null;
        }
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        if ((imgWidth > maxWidth || imgHeight > maxHeight) && imgWidth > 0 && imgHeight > 0) {
            float scale = Math.min(1.0f, maxHeight / imgHeight);
            if (scale < 1.0d || imgWidth > maxWidth) {
                int newWidth = (int) (imgWidth * scale);
                float deltaW = Math.max(newWidth - maxWidth, 0) / scale;
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Bitmap newImage = Bitmap.createBitmap(image, (int) (deltaW / 2.0f), 0, (int) (imgWidth - deltaW), imgHeight, matrix, true);
                if (newImage != null) {
                    return newImage;
                }
            }
        }
        return image;
    }

    public static boolean startSearchActivitySafely(Context context, Intent intent, int deviceId) {
        intent.putExtra("android.intent.extra.ASSIST_INPUT_DEVICE_ID", deviceId);
        return startActivitySafely(context, intent);
    }
}
