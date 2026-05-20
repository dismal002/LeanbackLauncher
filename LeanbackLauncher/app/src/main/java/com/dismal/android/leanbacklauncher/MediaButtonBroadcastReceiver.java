package com.dismal.android.leanbacklauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class MediaButtonBroadcastReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Log.i("LeanbackLauncher", "Received media button event in LeanbackLauncher, ignoring.");
        abortBroadcast();
    }
}
