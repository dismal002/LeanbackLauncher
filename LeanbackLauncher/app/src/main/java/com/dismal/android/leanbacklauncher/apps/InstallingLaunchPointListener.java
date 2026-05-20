package com.dismal.android.leanbacklauncher.apps;

/* JADX INFO: loaded from: classes.dex */
public interface InstallingLaunchPointListener {
    void onInstallingLaunchPointAdded(LaunchPoint launchPoint);

    void onInstallingLaunchPointChanged(LaunchPoint launchPoint);

    void onInstallingLaunchPointRemoved(LaunchPoint launchPoint, boolean z);
}
