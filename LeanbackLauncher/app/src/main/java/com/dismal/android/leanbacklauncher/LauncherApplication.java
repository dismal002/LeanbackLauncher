package com.dismal.android.leanbacklauncher;

import android.app.Application;
import android.util.Log;
import com.dismal.android.leanbacklauncher.ranker.DbHelper;

/* JADX INFO: loaded from: classes.dex */
public class LauncherApplication extends Application {
    private DbHelper mDbHelper;

    @Override
    public void onCreate() {
        long startTime = System.currentTimeMillis();
        super.onCreate();
        Log.d("LeanbackLauncher", "LauncherApplication.onCreate took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public synchronized DbHelper getDbHelper() {
        if (this.mDbHelper == null) {
            this.mDbHelper = new DbHelper(this);
        }
        return this.mDbHelper;
    }
}
