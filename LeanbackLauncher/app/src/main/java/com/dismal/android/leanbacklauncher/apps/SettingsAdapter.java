package com.dismal.android.leanbacklauncher.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.ConnectivityListener;
import com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public class SettingsAdapter extends AppsAdapter implements NotificationsAdapter.NotificationCountListener {
    private ConnectivityListener mConnectivityListener;
    private final Handler mHandler;
    private Resources mNetResources;
    private boolean mNetResourcesSet;

    private class SettingsComparator implements Comparator<LaunchPoint> {
        /* synthetic */ SettingsComparator(SettingsAdapter this$0, SettingsComparator settingsComparator) {
            this();
        }

        private SettingsComparator() {
        }

        @Override // java.util.Comparator
        public int compare(LaunchPoint lhs, LaunchPoint rhs) {
            if (lhs.getPriority() == rhs.getPriority()) {
                if (lhs.getTitle() == null) {
                    return -1;
                }
                if (rhs.getTitle() == null) {
                    return 1;
                }
                return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
            }
            return rhs.getPriority() - lhs.getPriority();
        }
    }

    public SettingsAdapter(Context context, LaunchPointListGenerator launchPointListGenerator, Ranker ranker, ConnectivityListener listener) {
        super(context, 2, launchPointListGenerator, ranker);
        this.mNetResourcesSet = false;
        this.mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.apps.SettingsAdapter.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        int index = SettingsAdapter.this.updateNetwork();
                        if (index >= 0) {
                            SettingsAdapter.this.notifyItemChanged(index);
                        }
                        break;
                }
            }
        };
        this.mConnectivityListener = listener;
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter
    protected void onPostRefresh() {
        this.mNetResourcesSet = false;
        updateNetwork();
    }

    public void onConnectivityChange() {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter.NotificationCountListener
    public void onNotificationCountUpdated(int count) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int updateNetwork() {
        for (int i = 0; i < this.mLaunchPoints.size(); i++) {
            LaunchPoint launchPoint = this.mLaunchPoints.get(i);
            if (launchPoint.getSettingsType() == 0) {
                setNetwork(this.mContext.getResources(), launchPoint);
                return i;
            }
        }
        return -1;
    }

    private LaunchPoint setNetwork(Resources res, LaunchPoint launchPoint) {
        if (!this.mNetResourcesSet) {
            setNetworkResources(launchPoint);
        }
        ConnectivityListener.ConnectivityStatus connectivityStatus = this.mConnectivityListener.getConnectivityStatus();
        int titleId = 0;
        String title = null;
        boolean hasNetworkName = false;
        launchPoint.setIconDrawable(null);
        switch (connectivityStatus.mNetworkType) {
            case 1:
                titleId = R.string.settings_network;
                launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_disconnected"));
                break;
            case 3:
            case 5:
                title = connectivityStatus.mWifiSsid;
                hasNetworkName = true;
                int signal = connectivityStatus.mWifiSignalStrength;
                switch (signal) {
                    case 0:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_0"));
                        break;
                    case 1:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_1"));
                        break;
                    case 2:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_2"));
                        break;
                    case 3:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_3"));
                        break;
                    case 4:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_4"));
                        break;
                }
                break;
            case 7:
                launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_ethernet"));
                titleId = R.string.settings_network;
                break;
            case 9:
                title = connectivityStatus.mWifiSsid;
                hasNetworkName = true;
                launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_no_internet"));
                break;
            case 11:
                titleId = R.string.settings_network;
                launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_ethernet_no_internet"));
                break;
            case 13:
                title = connectivityStatus.mMobileNetworkName;
                hasNetworkName = true;
                int signal2 = connectivityStatus.mMobileSignalStrength;
                switch (signal2) {
                    case 0:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_0"));
                        break;
                    case 1:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_1"));
                        break;
                    case 2:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_2"));
                        break;
                    case 3:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_3"));
                        break;
                    case 4:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_4"));
                        break;
                }
                break;
            case 15:
                title = connectivityStatus.mMobileNetworkName;
                hasNetworkName = true;
                int signal3 = connectivityStatus.mMobileSignalStrength;
                switch (signal3) {
                    case 0:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_no_internet_0"));
                        break;
                    case 1:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_no_internet_1"));
                        break;
                    case 2:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_no_internet_2"));
                        break;
                    case 3:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_no_internet_3"));
                        break;
                    case 4:
                        launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_cellular_no_internet_4"));
                        break;
                }
                break;
        }
        if (launchPoint.getIconDrawable() == null) {
            launchPoint.setIconDrawable(getNetResourceDrawable(launchPoint, "network_state_wifi_0"));
        }
        if (titleId != 0) {
            title = res.getString(titleId);
        }
        if (title == null) {
            title = res.getString(R.string.settings_network);
        }
        launchPoint.setTitle(title);
        launchPoint.setContentDescription(hasNetworkName ? res.getString(R.string.settings_network) : null);
        return launchPoint;
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter, com.dismal.android.leanbacklauncher.ranker.Ranker.RankingListener
    public void onRankerReady() {
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter
    protected void sortLaunchPoints(ArrayList<LaunchPoint> launchPoints) {
        Collections.sort(launchPoints, new SettingsComparator(this, null));
    }

    private void setNetworkResources(LaunchPoint lp) {
        String packageName = lp.getLaunchIntent().getComponent().getPackageName();
        try {
            this.mNetResources = this.mContext.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        this.mNetResourcesSet = true;
    }

    private Drawable getNetResourceDrawable(LaunchPoint launchPoint, String resName) {
        Drawable ret;
        int resId = 0;
        if (this.mNetResources != null) {
            resId = this.mNetResources.getIdentifier(resName, "drawable", launchPoint.getPackageName());
        }
        if (resId != 0 && (ret = this.mNetResources.getDrawable(resId, null)) != null) {
            return ret;
        }
        Resources launcherRes = this.mContext.getResources();
        String launcherPackageName = this.mContext.getPackageName();
        int resId2 = launcherRes.getIdentifier(resName, "drawable", launcherPackageName);
        return launcherRes.getDrawable(resId2, null);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter, com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onSettingsChanged() {
        refreshDataSetAsync();
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter, com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onLaunchPointsAddedOrUpdated(ArrayList<LaunchPoint> launchPoints) {
    }

    @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter, com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onLaunchPointsRemoved(ArrayList<LaunchPoint> launchPoints) {
    }
}
