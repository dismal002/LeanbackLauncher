package com.dismal.android.leanbacklauncher.notifications;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;

/* JADX INFO: loaded from: classes.dex */
public interface INotificationsMonitorClient extends IInterface {
    void endClientUpdate() throws RemoteException;

    void onNotificationCountChanged(int i) throws RemoteException;

    void onPackageBlacklistChanged(int i) throws RemoteException;

    void onRecommendationPosted(StatusBarNotification statusBarNotification) throws RemoteException;

    void onRecommendationRemoved(StatusBarNotification statusBarNotification) throws RemoteException;

    void onServiceStatusChanged(boolean z) throws RemoteException;

    void reportExistingRecommendation(StatusBarNotification statusBarNotification) throws RemoteException;

    void startClientUpdate() throws RemoteException;

    public static abstract class Stub extends Binder implements INotificationsMonitorClient {
        public Stub() {
            attachInterface(this, "com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
        }

        public static INotificationsMonitorClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
            if (iin != null && (iin instanceof INotificationsMonitorClient)) {
                return (INotificationsMonitorClient) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            StatusBarNotification statusBarNotification;
            StatusBarNotification statusBarNotification2;
            StatusBarNotification statusBarNotification3;
            switch (code) {
                case 1:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    boolean _arg0 = data.readInt() != 0;
                    onServiceStatusChanged(_arg0);
                    return true;
                case 2:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (data.readInt() != 0) {
                        statusBarNotification3 = (StatusBarNotification) StatusBarNotification.CREATOR.createFromParcel(data);
                    } else {
                        statusBarNotification3 = null;
                    }
                    onRecommendationPosted(statusBarNotification3);
                    return true;
                case 3:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (data.readInt() != 0) {
                        statusBarNotification2 = (StatusBarNotification) StatusBarNotification.CREATOR.createFromParcel(data);
                    } else {
                        statusBarNotification2 = null;
                    }
                    onRecommendationRemoved(statusBarNotification2);
                    return true;
                case 4:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    int _arg02 = data.readInt();
                    onNotificationCountChanged(_arg02);
                    return true;
                case 5:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    startClientUpdate();
                    return true;
                case 6:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (data.readInt() != 0) {
                        statusBarNotification = (StatusBarNotification) StatusBarNotification.CREATOR.createFromParcel(data);
                    } else {
                        statusBarNotification = null;
                    }
                    reportExistingRecommendation(statusBarNotification);
                    return true;
                case 7:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    endClientUpdate();
                    return true;
                case 8:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    int _arg03 = data.readInt();
                    onPackageBlacklistChanged(_arg03);
                    return true;
                case 1598968902:
                    reply.writeString("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INotificationsMonitorClient {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void onServiceStatusChanged(boolean isReady) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    _data.writeInt(isReady ? 1 : 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void onRecommendationPosted(StatusBarNotification sbn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (sbn != null) {
                        _data.writeInt(1);
                        sbn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void onRecommendationRemoved(StatusBarNotification sbn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (sbn != null) {
                        _data.writeInt(1);
                        sbn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void onNotificationCountChanged(int count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    _data.writeInt(count);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void startClientUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void reportExistingRecommendation(StatusBarNotification sbn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    if (sbn != null) {
                        _data.writeInt(1);
                        sbn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void endClientUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
            public void onPackageBlacklistChanged(int count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient");
                    _data.writeInt(count);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
