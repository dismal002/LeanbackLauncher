package com.dismal.android.leanbacklauncher.notifications;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public interface IRemoteControlListener extends IInterface {
    void onClientChanged(boolean z) throws RemoteException;

    void onClientPlaybackStateUpdate(int i, long j, long j2) throws RemoteException;

    void onMediaDataUpdated(NowPlayingCardData nowPlayingCardData) throws RemoteException;

    public static abstract class Stub extends Binder implements IRemoteControlListener {
        public Stub() {
            attachInterface(this, "com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
        }

        public static IRemoteControlListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
            if (iin != null && (iin instanceof IRemoteControlListener)) {
                return (IRemoteControlListener) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            NowPlayingCardData nowPlayingCardDataCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    boolean _arg0 = data.readInt() != 0;
                    onClientChanged(_arg0);
                    return true;
                case 2:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    if (data.readInt() != 0) {
                        nowPlayingCardDataCreateFromParcel = NowPlayingCardData.CREATOR.createFromParcel(data);
                    } else {
                        nowPlayingCardDataCreateFromParcel = null;
                    }
                    onMediaDataUpdated(nowPlayingCardDataCreateFromParcel);
                    return true;
                case 3:
                    data.enforceInterface("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    int _arg02 = data.readInt();
                    long _arg1 = data.readLong();
                    long _arg2 = data.readLong();
                    onClientPlaybackStateUpdate(_arg02, _arg1, _arg2);
                    return true;
                case 1598968902:
                    reply.writeString("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IRemoteControlListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
            public void onClientChanged(boolean clearing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    _data.writeInt(clearing ? 1 : 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
            public void onMediaDataUpdated(NowPlayingCardData mediaData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    if (mediaData != null) {
                        _data.writeInt(1);
                        mediaData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
            public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener");
                    _data.writeInt(state);
                    _data.writeLong(stateChangeTimeMs);
                    _data.writeLong(currentPosMs);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
