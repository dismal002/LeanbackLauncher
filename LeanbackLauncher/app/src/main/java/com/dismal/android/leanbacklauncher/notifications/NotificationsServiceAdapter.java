package com.dismal.android.leanbacklauncher.notifications;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.util.Log;
import com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient;
import com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public abstract class NotificationsServiceAdapter<VH extends RecyclerView.ViewHolder> extends NotificationsViewAdapter<VH> {
    protected boolean mBound;
    protected INotificationsMonitorService mBoundService;
    private ServiceConnection mConnection;
    protected NotificationsServiceAdapter<VH>.NotificationsListener mNotificationsListener;

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected abstract boolean isPartnerClient();

    protected void onConnected(ComponentName className, IBinder service) {
        try {
            if (Log.isLoggable("NotificationsAdapter", 3)) {
                Log.d("NotificationsAdapter", "Notification Client connected to Service");
            }
            this.mBoundService.registerNotificationsClient(this.mNotificationsListener, isPartnerClient());
        } catch (RemoteException e) {
        }
    }

    protected void serviceStatusChanged(boolean isReady) {
        if (!Log.isLoggable("NotificationsAdapter", 3)) {
            return;
        }
        Log.d("NotificationsAdapter", "Notification Service Status changed. Ready = " + isReady);
    }

    protected void onDisconnected(ComponentName className) {
        if (!Log.isLoggable("NotificationsAdapter", 3)) {
            return;
        }
        Log.d("NotificationsAdapter", "Notification Client disconnected from Service");
    }

    public void onInitUi() {
        Log.d("UICYCLE", "NotificationsServiceAdapter.onInitUi");
        if (Log.isLoggable("NotificationsAdapter", 3)) {
            Log.d("NotificationsAdapter", "onInitUi()");
        }
        this.mContext.bindService(new Intent("com.dismal.android.leanbacklauncher.notifications.NotificationListenerService", null, this.mContext, NotificationMonitorService.class), this.mConnection, 1);
    }

    public void reregisterListener() {
        onStopUi();
        onInitUi();
    }

    public void onStopUi() {
        Log.d("UICYCLE", "NotificationsServiceAdapter.onStopUi: " + this.mBound);
        if (Log.isLoggable("NotificationsAdapter", 3)) {
            Log.d("NotificationsAdapter", "onStopUi()");
        }
        if (!this.mBound) {
            return;
        }
        try {
            this.mBoundService.unregisterNotificationsClient(this.mNotificationsListener, isPartnerClient());
        } catch (RemoteException e) {
            Log.e("NotificationsAdapter", "Error unregistering notifications client", e);
        }
        this.mContext.unbindService(this.mConnection);
        this.mBound = false;
    }

    public NotificationsServiceAdapter(Context context) {
        super(context);
        this.mConnection = new ServiceConnection() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName className, IBinder service) {
                if (Log.isLoggable("NotificationsAdapter", 3)) {
                    Log.d("NotificationsAdapter", "Notification Service connected");
                }
                NotificationsServiceAdapter.this.mBoundService = INotificationsMonitorService.Stub.asInterface(service);
                NotificationsServiceAdapter.this.mBound = true;
                NotificationsServiceAdapter.this.onConnected(className, service);
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName className) {
                if (Log.isLoggable("NotificationsAdapter", 3)) {
                    Log.d("NotificationsAdapter", "Notification Service disconnected");
                }
                NotificationsServiceAdapter.this.mBoundService = null;
                NotificationsServiceAdapter.this.mBound = false;
                NotificationsServiceAdapter.this.onDisconnected(className);
            }
        };
        this.mNotificationsListener = new NotificationsListener();
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void updateRowVisibility() {
        if (this.mBoundService != null) {
            int count = 0;
            try {
                count = this.mBoundService.getPackageBlacklistSize();
            } catch (RemoteException e) {
                Log.e("NotificationsAdapter", "Exception when calling getPackageBlacklistSize(): " + e.toString());
            }
            setPackageBlacklistCount(count);
        } else {
            setPackageBlacklistCount(0);
        }
        super.updateRowVisibility();
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected final void onNotificationCanceled(StatusBarNotification sbn) {
        if (!this.mBound || this.mBoundService == null) {
            return;
        }
        try {
            this.mBoundService.requestNotificationCancel(sbn.getKey());
        } catch (RemoteException e) {
            Log.e("NotificationsAdapter", "Exception while cancelling notification: " + e.toString());
        }
    }

    protected void onNotificationClick(PendingIntent intent, String group) {
    }

    protected class NotificationsListener extends INotificationsMonitorClient.Stub {
        private ArrayList<StatusBarNotification> mNotifListTemp;

        protected NotificationsListener() {
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void onServiceStatusChanged(boolean isReady) {
            NotificationsServiceAdapter.this.serviceStatusChanged(isReady);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void onRecommendationPosted(StatusBarNotification sbn) {
            NotificationsServiceAdapter.this.msgRecommendationPosted(sbn);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void onRecommendationRemoved(StatusBarNotification sbn) {
            NotificationsServiceAdapter.this.msgRecommendationRemoved(sbn);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void startClientUpdate() {
            if (Log.isLoggable("NotificationsAdapter", 3)) {
                Log.d("NotificationsAdapter", "Starting Service Recommendation list update.");
            }
            this.mNotifListTemp = new ArrayList<>();
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void reportExistingRecommendation(StatusBarNotification sbn) {
            if (Log.isLoggable("NotificationsAdapter", 3)) {
                Log.d("NotificationsAdapter", "Reported existing Recommendation = " + sbn);
            }
            if (this.mNotifListTemp == null) {
                return;
            }
            this.mNotifListTemp.add(sbn);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void endClientUpdate() {
            if (this.mNotifListTemp == null) {
                return;
            }
            if (Log.isLoggable("NotificationsAdapter", 3)) {
                Log.d("NotificationsAdapter", "Service Recommendation list update Completed. Number of Recommendation = " + this.mNotifListTemp.size());
            }
            NotificationsServiceAdapter.this.msgForcedRerank(this.mNotifListTemp);
            this.mNotifListTemp = null;
        }

        public void onNotificationCountChanged(int count) {
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void onPackageBlacklistChanged(int count) {
            NotificationsServiceAdapter.this.msgOnPackageBlacklistChanged(count);
        }
    }
}
