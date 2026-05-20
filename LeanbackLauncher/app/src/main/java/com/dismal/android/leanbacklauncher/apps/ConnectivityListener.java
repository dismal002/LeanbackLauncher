package com.dismal.android.leanbacklauncher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ConnectivityListener {
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private boolean mIsRegistered;
    private final Listener mListener;
    private final BroadcastReceiver mReceiver;
    private final TelephonyManager mTelephonyManager;
    private final WifiManager mWifiManager;
    private ConnectivityStatus mConnectivityStatus = new ConnectivityStatus();
    private final LeanbackLauncherPhoneStateListener mPhoneStateListener = new LeanbackLauncherPhoneStateListener(this);
    private final IntentFilter mFilter = new IntentFilter();

    public interface Listener {
        void onConnectivityChange();
    }

    public class ConnectivityStatus {
        public String mMobileNetworkName;
        public int mMobileSignalStrength;
        public int mNetworkType;
        public int mWifiSignalStrength;
        public String mWifiSsid;

        public ConnectivityStatus() {
        }
    }

    private static class LeanbackLauncherPhoneStateListener extends PhoneStateListener {
        private WeakReference<ConnectivityListener> mListener;

        public LeanbackLauncherPhoneStateListener(ConnectivityListener listener) {
            this.mListener = new WeakReference<>(listener);
        }

        @Override // android.telephony.PhoneStateListener
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            int level = 0;
            ConnectivityListener listener = this.mListener.get();
            if (listener == null) {
                return;
            }
            try {
                level = ((Integer) Class.forName("android.telephony.SignalStrength").getMethod("getLevel", new Class[0]).invoke(signalStrength, new Object[0])).intValue();
            } catch (Exception e) {
                Log.e("ConnectivityListener", e.toString());
            }
            listener.mConnectivityStatus.mMobileSignalStrength = level;
        }
    }

    public ConnectivityListener(Context context, Listener listener) {
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mListener = listener;
        this.mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
        this.mReceiver = new BroadcastReceiver() { // from class: com.dismal.android.leanbacklauncher.apps.ConnectivityListener.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                Log.d("ConnectivityListener", "Connectivity change!");
                String intentAction = intent.getAction();
                int connectionStatus = intent.getIntExtra("inetCondition", -551);
                ConnectivityManager cm = (ConnectivityManager) context2.getSystemService("connectivity");
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info == null || !info.isAvailable() || !info.isConnected()) {
                    ConnectivityListener.writeConnectivity(context2, false);
                }
                if (intentAction.equals("android.net.conn.INET_CONDITION_ACTION") || intentAction.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    ConnectivityListener.writeConnectivity(context2, connectionStatus > 50);
                }
                ConnectivityListener.this.updateConnectivityStatus();
                ConnectivityListener.this.mListener.onConnectivityChange();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void writeConnectivity(Context context, boolean inetConnected) {
        context.getSharedPreferences("inet-prefs", 0).edit().putBoolean("inetCondition", inetConnected).apply();
    }

    public static boolean readConnectivity(Context context) {
        return context.getSharedPreferences("inet-prefs", 0).getBoolean("inetCondition", true);
    }

    public void start() {
        if (this.mIsRegistered) {
            return;
        }
        updateConnectivityStatus();
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 256);
        this.mIsRegistered = true;
    }

    public void stop() {
        if (!this.mIsRegistered) {
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        this.mIsRegistered = false;
    }

    public ConnectivityStatus getConnectivityStatus() {
        return this.mConnectivityStatus;
    }

    private void setNoConnection() {
        this.mConnectivityStatus.mNetworkType = 1;
        this.mConnectivityStatus.mWifiSsid = null;
        this.mConnectivityStatus.mWifiSignalStrength = 0;
    }

    private boolean isSecureWifi(WifiInfo wifiInfo) {
        if (wifiInfo == null) {
            return false;
        }
        int networkId = wifiInfo.getNetworkId();
        List<WifiConfiguration> configuredNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration configuredNetwork : configuredNetworks) {
                if (configuredNetwork.networkId == networkId) {
                    if (configuredNetwork.allowedKeyManagement.get(1) || configuredNetwork.allowedKeyManagement.get(2)) {
                        return true;
                    }
                    return configuredNetwork.allowedKeyManagement.get(3);
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConnectivityStatus() {
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            setNoConnection();
        }
        boolean isConnected = readConnectivity(this.mContext);
        switch (networkInfo.getType()) {
            case 0:
                if (!isConnected) {
                    this.mConnectivityStatus.mNetworkType = 15;
                } else {
                    this.mConnectivityStatus.mNetworkType = 13;
                }
                String operator = null;
                if (this.mTelephonyManager != null && (operator = this.mTelephonyManager.getNetworkOperatorName()) != null) {
                    operator = removeDoubleQuotes(operator);
                }
                if (operator != null) {
                    this.mConnectivityStatus.mMobileNetworkName = operator;
                } else {
                    this.mConnectivityStatus.mMobileNetworkName = "";
                }
                this.mConnectivityStatus.mWifiSsid = null;
                this.mConnectivityStatus.mWifiSignalStrength = 0;
                break;
            case 1:
                WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
                if (isSecureWifi(wifiInfo)) {
                    this.mConnectivityStatus.mNetworkType = 5;
                } else {
                    this.mConnectivityStatus.mNetworkType = 3;
                }
                if (!isConnected) {
                    this.mConnectivityStatus.mNetworkType = 9;
                }
                String ssid = null;
                if (wifiInfo != null && (ssid = wifiInfo.getSSID()) != null) {
                    ssid = removeDoubleQuotes(ssid);
                }
                if (ssid != null) {
                    this.mConnectivityStatus.mWifiSsid = ssid;
                } else {
                    this.mConnectivityStatus.mWifiSsid = "";
                }
                if (wifiInfo != null) {
                    this.mConnectivityStatus.mWifiSignalStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
                } else {
                    this.mConnectivityStatus.mWifiSignalStrength = 0;
                }
                this.mConnectivityStatus.mMobileNetworkName = null;
                this.mConnectivityStatus.mMobileSignalStrength = 0;
                break;
            case 9:
                if (!isConnected) {
                    this.mConnectivityStatus.mNetworkType = 11;
                } else {
                    this.mConnectivityStatus.mNetworkType = 7;
                }
                this.mConnectivityStatus.mWifiSsid = null;
                this.mConnectivityStatus.mWifiSignalStrength = 0;
                this.mConnectivityStatus.mMobileNetworkName = null;
                this.mConnectivityStatus.mMobileSignalStrength = 0;
                break;
            default:
                setNoConnection();
                break;
        }
    }

    public static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
