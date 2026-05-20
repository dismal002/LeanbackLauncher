package com.dismal.android.leanbacklauncher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.dismal.android.leanbacklauncher.R;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public class MarketUpdateReceiver extends BroadcastReceiver {
    private final HashMap<String, LaunchPoint> mInstallLaunchPoints = new HashMap<>();
    private final InstallingLaunchPointListener mListener;

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.launcher.action.ACTION_PACKAGE_ENQUEUED");
        filter.addAction("com.android.launcher.action.ACTION_PACKAGE_DOWNLOADING");
        filter.addAction("com.android.launcher.action.ACTION_PACKAGE_INSTALLING");
        filter.addAction("com.android.launcher.action.ACTION_PACKAGE_DEQUEUED");
        filter.addDataScheme("package");
        return filter;
    }

    public static String getBroadcastPermission() {
        return "android.permission.INSTALL_PACKAGES";
    }

    public MarketUpdateReceiver(InstallingLaunchPointListener listener) {
        this.mListener = listener;
    }

    private boolean isNonLeanbackApp(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(pkgName, 1);
            Intent lbIntent = pm.getLeanbackLaunchIntentForPackage(pkgName);
            return lbIntent == null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String pkgName;
        Uri uri = intent.getData();
        if (uri == null || (pkgName = uri.getSchemeSpecificPart()) == null || isNonLeanbackApp(context, pkgName)) {
            return;
        }
        String action = intent.getAction();
        LaunchPoint installLaunchPoint = this.mInstallLaunchPoints.get(pkgName);
        if (installLaunchPoint == null && !intent.getBooleanExtra("user_initiated", false)) {
            return;
        }
        boolean wasAdded = false;
        if (installLaunchPoint == null) {
            wasAdded = true;
            installLaunchPoint = createInstallLaunchPoint(context, pkgName, intent);
            this.mInstallLaunchPoints.put(pkgName, installLaunchPoint);
        }
        boolean success = false;
        if ("com.android.launcher.action.ACTION_PACKAGE_ENQUEUED".equals(action)) {
            installLaunchPoint.setInstallProgressPercent(-1);
            String reason = "install";
            if (intent.hasExtra("reason")) {
                reason = intent.getStringExtra("reason");
            }
            if ("install".equals(reason)) {
                installLaunchPoint.setInstallStateStringResourceId(R.string.install_pending);
            } else if ("update".equals(reason)) {
                installLaunchPoint.setInstallStateStringResourceId(R.string.update_pending);
            } else if ("restore".equals(reason)) {
                installLaunchPoint.setInstallStateStringResourceId(R.string.restore_pending);
            } else {
                installLaunchPoint.setInstallStateStringResourceId(R.string.unknown_state);
            }
            boolean userInitiated = intent.getBooleanExtra("user_initiated", false);
            Log.d("MarketUpdateReceiver", "market has promised to " + reason + ": " + pkgName + " user initiated: " + userInitiated);
        } else if ("com.android.launcher.action.ACTION_PACKAGE_DOWNLOADING".equals(action)) {
            int progress = intent.getIntExtra("progress", 0);
            installLaunchPoint.setInstallProgressPercent(progress);
            installLaunchPoint.setInstallStateStringResourceId(R.string.downloading);
            Log.d("MarketUpdateReceiver", "market is downloading (" + progress + "%): " + pkgName);
        } else if ("com.android.launcher.action.ACTION_PACKAGE_INSTALLING".equals(action)) {
            installLaunchPoint.setInstallProgressPercent(-1);
            installLaunchPoint.setInstallStateStringResourceId(R.string.installing);
            Log.d("MarketUpdateReceiver", "market is installing: " + pkgName);
        } else if ("com.android.launcher.action.ACTION_PACKAGE_DEQUEUED".equals(action)) {
            this.mInstallLaunchPoints.remove(pkgName);
            installLaunchPoint.setInstallStateStringResourceId(0);
            installLaunchPoint.setInstallProgressPercent(-1);
            success = intent.getBooleanExtra("com.android.launcher.action.INSTALL_COMPLETED", false);
            if (success) {
                Log.d("MarketUpdateReceiver", "market has installed: " + pkgName);
            } else {
                Log.d("MarketUpdateReceiver", "market has decided not to install: " + pkgName);
            }
        } else {
            installLaunchPoint.setInstallProgressPercent(-1);
            installLaunchPoint.setInstallStateStringResourceId(R.string.unknown_state);
            Log.d("MarketUpdateReceiver", "unknown message " + action);
        }
        if ("com.android.launcher.action.ACTION_PACKAGE_DEQUEUED".equals(action)) {
            this.mListener.onInstallingLaunchPointRemoved(installLaunchPoint, success);
        } else if (wasAdded) {
            this.mListener.onInstallingLaunchPointAdded(installLaunchPoint);
        } else {
            this.mListener.onInstallingLaunchPointChanged(installLaunchPoint);
        }
    }

    private LaunchPoint createInstallLaunchPoint(Context context, String pkgName, Intent intent) {
        String appName = intent.getStringExtra("app_name");
        String appIconUri = intent.getStringExtra("app_icon");
        boolean appIsGame = intent.getBooleanExtra("app_is_game", false);
        Intent appDetailsIntent = (Intent) intent.getParcelableExtra("app_detailIntent");
        return new LaunchPoint(context, appName, appIconUri, pkgName, appDetailsIntent, appIsGame, this.mListener);
    }
}
