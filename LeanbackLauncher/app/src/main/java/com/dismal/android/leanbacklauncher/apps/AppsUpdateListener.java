package com.dismal.android.leanbacklauncher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.recyclerview.widget.RecyclerView;
import com.dismal.android.leanbacklauncher.HomeScreenRow;
import com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver;
import com.dismal.android.leanbacklauncher.notifications.BlacklistListener;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.util.Util;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class AppsUpdateListener implements PackageChangedReceiver.Listener, InstallingLaunchPointListener, BlacklistListener {
    public Context mContext;
    private BroadcastReceiver mExternalAppsUpdateReceiver;
    private final LaunchPointListGenerator mLaunchPointGen;
    private final Ranker mRanker;
    protected ArrayList<HomeScreenRow> mRows = new ArrayList<>();
    private final MarketUpdateReceiver mMarketUpdateReceiver = new MarketUpdateReceiver(this);
    private final PackageChangedReceiver mPackageChangedReceiver = new PackageChangedReceiver(this);

    public AppsUpdateListener(Context context, LaunchPointListGenerator launchPointListGenerator, Ranker ranker) {
        this.mContext = context;
        this.mRanker = ranker;
        this.mLaunchPointGen = launchPointListGenerator;
        this.mContext.registerReceiver(this.mPackageChangedReceiver, PackageChangedReceiver.getIntentFilter());
        this.mContext.registerReceiver(this.mMarketUpdateReceiver, MarketUpdateReceiver.getIntentFilter(), MarketUpdateReceiver.getBroadcastPermission(), null);
        registerExternalAppsReceiver();
    }

    public void addAppRow(HomeScreenRow row) {
        this.mRows.add(row);
        refreshRow(row);
    }

    public void unregisterReceivers() {
        this.mContext.unregisterReceiver(this.mMarketUpdateReceiver);
        this.mContext.unregisterReceiver(this.mPackageChangedReceiver);
        if (this.mExternalAppsUpdateReceiver == null) {
            return;
        }
        this.mContext.unregisterReceiver(this.mExternalAppsUpdateReceiver);
    }

    private void refreshRow(HomeScreenRow row) {
        RecyclerView.Adapter<?> adapter = row.getAdapter();
        if (!(adapter instanceof AppsAdapter)) {
            return;
        }
        ((AppsAdapter) adapter).refreshDataSetAsync();
    }

    public void refreshRows() {
        for (int i = 0; i < this.mRows.size(); i++) {
            refreshRow(this.mRows.get(i));
        }
    }

    @Override // com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver.Listener
    public void onPackageAdded(String packageName) {
        this.mRanker.onAction(packageName, 0);
        this.mLaunchPointGen.addOrUpdatePackage(packageName);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver.Listener
    public void onPackageChanged(String packageName) {
        Partner.resetIfNecessary(this.mContext, packageName);
        this.mLaunchPointGen.addOrUpdatePackage(packageName);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver.Listener
    public void onPackageFullyRemoved(String packageName) {
        this.mRanker.onAction(packageName, 3);
        Partner.resetIfNecessary(this.mContext, packageName);
        this.mLaunchPointGen.removePackage(packageName);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver.Listener
    public void onPackageRemoved(String packageName) {
        Partner.resetIfNecessary(this.mContext, packageName);
        this.mLaunchPointGen.removePackage(packageName);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.PackageChangedReceiver.Listener
    public void onPackageReplaced(String packageName) {
        Partner.resetIfNecessary(this.mContext, packageName);
        this.mLaunchPointGen.addOrUpdatePackage(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onExternalPackagesStatusChanged() {
        this.mLaunchPointGen.refreshLaunchPointList();
    }

    @Override // com.dismal.android.leanbacklauncher.apps.InstallingLaunchPointListener
    public void onInstallingLaunchPointAdded(LaunchPoint launchPoint) {
        this.mRanker.onAction(launchPoint.getPackageName(), 0);
        this.mLaunchPointGen.addOrUpdateInstallingLaunchPoint(launchPoint);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.InstallingLaunchPointListener
    public void onInstallingLaunchPointChanged(LaunchPoint launchPoint) {
        this.mLaunchPointGen.addOrUpdateInstallingLaunchPoint(launchPoint);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.InstallingLaunchPointListener
    public void onInstallingLaunchPointRemoved(LaunchPoint launchPoint, boolean success) {
        String pkgName = launchPoint.getPackageName();
        if (!success && !Util.isPackagePresent(this.mContext.getPackageManager(), pkgName)) {
            this.mRanker.onAction(pkgName, 3);
        }
        this.mLaunchPointGen.removeInstallingLaunchPoint(launchPoint, success);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.BlacklistListener
    public void onPackageBlacklisted(String pkgName) {
        this.mLaunchPointGen.addToBlacklist(pkgName);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.BlacklistListener
    public void onPackageUnblacklisted(String pkgName) {
        this.mLaunchPointGen.removeFromBlacklist(pkgName);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.BlacklistListener
    public void onPackageBlacklistUpdated(ArrayList<String> pkgNames) {
        this.mLaunchPointGen.updateBlackList(pkgNames);
    }

    private void registerExternalAppsReceiver() {
        this.mExternalAppsUpdateReceiver = new BroadcastReceiver() { // from class: com.dismal.android.leanbacklauncher.apps.AppsUpdateListener.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                AppsUpdateListener.this.onExternalPackagesStatusChanged();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mExternalAppsUpdateReceiver, filter);
    }
}
