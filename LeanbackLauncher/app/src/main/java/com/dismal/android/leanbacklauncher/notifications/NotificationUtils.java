package com.dismal.android.leanbacklauncher.notifications;

import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

/* JADX INFO: loaded from: classes.dex */
class NotificationUtils {
    private NotificationUtils() {
    }

    public static boolean equals(StatusBarNotification left, StatusBarNotification right) {
        return (left == null || right == null) ? left == right : TextUtils.equals(left.getPackageName(), right.getPackageName()) && left.getId() == right.getId() && TextUtils.equals(left.getTag(), right.getTag());
    }

    public static boolean isUpdate(StatusBarNotification left, StatusBarNotification right) {
        return left.getId() != right.getId();
    }
}
