package com.dismal.android.leanbacklauncher.notifications;

import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public interface BlacklistListener {
    void onPackageBlacklistUpdated(ArrayList<String> arrayList);

    void onPackageBlacklisted(String str);

    void onPackageUnblacklisted(String str);
}
