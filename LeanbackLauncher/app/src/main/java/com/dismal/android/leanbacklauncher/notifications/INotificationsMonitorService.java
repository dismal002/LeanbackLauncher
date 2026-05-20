package com.dismal.android.leanbacklauncher.notifications;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient;
import com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener;

/* JADX INFO: loaded from: classes.dex */
public interface INotificationsMonitorService extends IInterface {
    Bitmap getImageForNotification(String str) throws RemoteException;

    int getPackageBlacklistSize() throws RemoteException;

    void registerNotificationsClient(INotificationsMonitorClient iNotificationsMonitorClient, boolean z) throws RemoteException;

    void requestNotificationCancel(String str) throws RemoteException;

    void setRemoteControlListener(IRemoteControlListener iRemoteControlListener) throws RemoteException;

    void unregisterNotificationsClient(INotificationsMonitorClient iNotificationsMonitorClient, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements INotificationsMonitorService {
        public Stub() {
            attachInterface(this, "com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
        }

        public static INotificationsMonitorService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
            if (iin != null && (iin instanceof INotificationsMonitorService)) {
                return (INotificationsMonitorService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    INotificationsMonitorClient _arg0 = INotificationsMonitorClient.Stub.asInterface(data.readStrongBinder());
                    boolean _arg1 = data.readInt() != 0;
                    registerNotificationsClient(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    INotificationsMonitorClient _arg02 = INotificationsMonitorClient.Stub.asInterface(data.readStrongBinder());
                    boolean _arg12 = data.readInt() != 0;
                    unregisterNotificationsClient(_arg02, _arg12);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    IRemoteControlListener _arg03 = IRemoteControlListener.Stub.asInterface(data.readStrongBinder());
                    setRemoteControlListener(_arg03);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    String _arg04 = data.readString();
                    requestNotificationCancel(_arg04);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    String _arg05 = data.readString();
                    Bitmap _result = getImageForNotification(_arg05);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    int _result2 = getPackageBlacklistSize();
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 1598968902:
                    reply.writeString("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INotificationsMonitorService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public void registerNotificationsClient(INotificationsMonitorClient client, boolean isPartnerClient) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(isPartnerClient ? 1 : 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public void unregisterNotificationsClient(INotificationsMonitorClient client, boolean isPartnerClient) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(isPartnerClient ? 1 : 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public void setRemoteControlListener(IRemoteControlListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public void requestNotificationCancel(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    _data.writeString(key);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public Bitmap getImageForNotification(String key) throws RemoteException {
                Bitmap bitmap;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    _data.writeString(key);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bitmap = (Bitmap) Bitmap.CREATOR.createFromParcel(_reply);
                    } else {
                        bitmap = null;
                    }
                    return bitmap;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
            public int getPackageBlacklistSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService");
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
