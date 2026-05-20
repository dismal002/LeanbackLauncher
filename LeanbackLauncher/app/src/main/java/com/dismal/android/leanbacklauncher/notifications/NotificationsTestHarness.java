package com.dismal.android.leanbacklauncher.notifications;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public class NotificationsTestHarness {
    private static final Random mRandomGenerator = new Random();
    private ArrayList<StatusBarNotification> mSynthetic;

    public void setSynthetic(ArrayList<StatusBarNotification> synthetic) {
        this.mSynthetic = new ArrayList<>();
        for (StatusBarNotification i : synthetic) {
            this.mSynthetic.add(i);
        }
    }

    public static final String toString(StatusBarNotification sbn) {
        CharSequence title = "***";
        Notification notif = sbn.getNotification();
        if (notif != null) {
            title = (CharSequence) sbn.getNotification().extras.get("android.title");
            if (title == null) {
                title = "***";
            }
            if (title.length() > 50) {
                title = title.subSequence(0, 50);
            }
        }
        return title.toString();
    }
}
