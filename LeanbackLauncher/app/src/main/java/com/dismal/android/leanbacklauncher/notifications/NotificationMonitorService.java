package com.dismal.android.leanbacklauncher.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RemoteController;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import com.dismal.android.leanbacklauncher.LauncherApplication;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService;
import com.dismal.android.leanbacklauncher.ranker.DbHelper;
import com.dismal.android.leanbacklauncher.util.Util;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class NotificationMonitorService extends NotificationListenerService implements RemoteController.OnClientUpdateListener, DbHelper.BlacklistListener {
    private int mBannerMaxHeight;
    private int mBannerMaxWidth;
    private boolean mBlackListReady;
    private int mCardMaxHeight;
    private int mCardMaxWidth;
    private DbHelper mDbHelper;
    private boolean mFullyConnected;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mInitialFetchDone;
    private boolean mInitialFetchPending;
    private Looper mLooper;
    private int mMaxRecsPerApp;
    private IRemoteControlListener mMediaListener;
    private int mNonRecommendationsCount;
    private float mNowPlayingDefaultDarkening;
    private RemoteController mRemoteController;
    private final RemoteCallbackList<INotificationsMonitorClient> mPartnerClients = new RemoteCallbackList<>();
    private final RemoteCallbackList<INotificationsMonitorClient> mClients = new RemoteCallbackList<>();
    private final ArrayList<StatusBarNotification> mNotifList = new ArrayList<>();
    private final RecommendationComparator mRecSorter = new RecommendationComparator(this, null);
    private HashSet<String> mBlacklistedPackages = new HashSet<>();
    private INotificationsMonitorService.Stub mServiceStub = new INotificationsMonitorService.Stub() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationMonitorService.1
        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public synchronized void registerNotificationsClient(INotificationsMonitorClient client, boolean isPartnerClient) throws RemoteException {
            if (Log.isLoggable("NotifMonitorService", 3)) {
                Log.d("NotifMonitorService", "registerNotificationsClient: " + client);
            }
            if (client != null) {
                if (isPartnerClient) {
                    NotificationMonitorService.this.mPartnerClients.register(client);
                } else {
                    NotificationMonitorService.this.mClients.register(client);
                }
                if (NotificationMonitorService.this.mFullyConnected) {
                    client.onServiceStatusChanged(true);
                }
                if (NotificationMonitorService.this.mInitialFetchDone) {
                    NotificationMonitorService.this.updateClientLocked(client, isPartnerClient);
                }
            }
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public synchronized void unregisterNotificationsClient(INotificationsMonitorClient client, boolean isPartnerClient) throws RemoteException {
            if (Log.isLoggable("NotifMonitorService", 3)) {
                Log.d("NotifMonitorService", "unregisterNotificationsClient: " + client);
            }
            if (client != null) {
                if (isPartnerClient) {
                    NotificationMonitorService.this.mPartnerClients.unregister(client);
                } else {
                    NotificationMonitorService.this.mClients.unregister(client);
                }
            }
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public void requestNotificationCancel(String key) throws RemoteException {
            NotificationMonitorService.this.cancelNotification(key);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public synchronized void setRemoteControlListener(IRemoteControlListener listener) throws RemoteException {
            if (Log.isLoggable("NotifMonitorService", 3)) {
                Log.d("NotifMonitorService", "setRemoteControlListener: " + listener);
            }
            if (listener != null) {
                NotificationMonitorService.this.mMediaListener = listener;
                if (NotificationMonitorService.this.mInitialFetchDone) {
                    NotificationMonitorService.this.registerRemoteController();
                }
            } else {
                NotificationMonitorService.this.mMediaListener = null;
                NotificationMonitorService.this.unregisterRemoteController();
            }
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public Bitmap getImageForNotification(String key) {
            return NotificationMonitorService.this.getRecomendationImage(key);
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorService
        public int getPackageBlacklistSize() {
            return NotificationMonitorService.this.mBlacklistedPackages.size();
        }
    };

    private class RecommendationComparator implements Comparator<StatusBarNotification> {
        /* synthetic */ RecommendationComparator(NotificationMonitorService this$0, RecommendationComparator recommendationComparator) {
            this();
        }

        private RecommendationComparator() {
        }

        @Override // java.util.Comparator
        public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
            int res = lhs.getPackageName().compareToIgnoreCase(rhs.getPackageName());
            if (res != 0) {
                return res;
            }
            if (lhs.getPostTime() > rhs.getPostTime()) {
                return -1;
            }
            if (lhs.getPostTime() < rhs.getPostTime()) {
                return 1;
            }
            return res;
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onCreate");
        }
        this.mCardMaxWidth = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_max_width);
        this.mCardMaxHeight = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_height);
        this.mBannerMaxWidth = getResources().getDimensionPixelOffset(R.dimen.banner_width);
        this.mBannerMaxHeight = getResources().getDimensionPixelOffset(R.dimen.banner_height);
        this.mHandlerThread = new HandlerThread("NotificationMonitorService");
        this.mHandlerThread.start();
        this.mLooper = this.mHandlerThread.getLooper();
        this.mHandler = new ServiceHandler(this, this.mLooper, null);
        this.mDbHelper = ((LauncherApplication) getApplication()).getDbHelper();
        TypedValue out = new TypedValue();
        getResources().getValue(R.dimen.now_playing_icon_color_darkening, out, true);
        this.mNowPlayingDefaultDarkening = out.getFloat();
        this.mMaxRecsPerApp = getResources().getInteger(R.integer.max_recommendations_per_app);
    }

    @Override // android.service.notification.NotificationListenerService, android.app.Service
    public void onDestroy() {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onDestroy");
        }
        this.mHandlerThread.quit();
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.DbHelper.BlacklistListener
    public void onEntityKeysReady(ArrayList<String> keys) {
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.DbHelper.BlacklistListener
    public void onEntityBlacklistReady(ArrayList<String> packageNames) {
        boolean fetch = !this.mInitialFetchDone;
        this.mBlackListReady = true;
        if (!fetch) {
            fetch = packageNames.size() != this.mBlacklistedPackages.size();
            if (!fetch) {
                int size = packageNames.size();
                for (int i = 0; i < size && !fetch; i++) {
                    fetch = !this.mBlacklistedPackages.contains(packageNames.get(i));
                }
            }
        }
        if (fetch) {
            this.mBlacklistedPackages.clear();
            this.mBlacklistedPackages.addAll(packageNames);
            this.mHandler.sendEmptyMessage(3);
        }
        int count = this.mClients.beginBroadcast();
        int listSize = this.mBlacklistedPackages.size();
        for (int i2 = 0; i2 < count; i2++) {
            try {
                ((INotificationsMonitorClient) this.mClients.getBroadcastItem(i2)).onPackageBlacklistChanged(listSize);
            } catch (RemoteException e) {
                Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
            }
        }
        this.mClients.finishBroadcast();
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerConnected() {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "Listener fully connected to Notification Manager");
        }
        this.mFullyConnected = true;
        if (!this.mInitialFetchPending) {
            this.mInitialFetchPending = true;
            this.mInitialFetchDone = false;
        }
        if (!this.mBlackListReady) {
            this.mDbHelper.setBlacklistListener(this, this.mInitialFetchDone ? false : true);
        } else {
            if (Log.isLoggable("NotifMonitorService", 3)) {
                Log.d("NotifMonitorService", "BlackList already initialized, triggering Initial Fetch");
            }
            this.mHandler.sendEmptyMessage(3);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            requestInterruptionFilter(4);
        } else if (Build.VERSION.SDK_INT >= 21) {
            requestListenerHints(1);
        }
        setOnNotificationPostedTrimReflection(1);
        notifyServiceStatusChange(true);
    }

    @Override // android.service.notification.NotificationListenerService, android.app.Service
    public IBinder onBind(Intent intent) {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onBind: " + intent);
        }
        if (!TextUtils.isEmpty(intent.getAction())) {
            if (intent.getAction().equals("android.service.notification.NotificationListenerService")) {
                if (Log.isLoggable("NotifMonitorService", 3)) {
                    Log.d("NotifMonitorService", "Bound to Notification Manager");
                }
                this.mFullyConnected = false;
                return super.onBind(intent);
            }
            if (intent.getAction().equals("com.dismal.android.leanbacklauncher.notifications.NotificationListenerService")) {
                if (Log.isLoggable("NotifMonitorService", 3)) {
                    Log.d("NotifMonitorService", "Connected to Service Client");
                }
                return this.mServiceStub;
            }
            return null;
        }
        return null;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        if (intent.getAction().equals("android.service.notification.NotificationListenerService")) {
            this.mInitialFetchPending = false;
            this.mInitialFetchDone = false;
            this.mFullyConnected = false;
            notifyServiceStatusChange(false);
        }
        return super.onUnbind(intent);
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationPosted(StatusBarNotification sbn) {
        this.mHandler.obtainMessage(1, sbn).sendToTarget();
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationRemoved(StatusBarNotification sbn) {
        this.mHandler.obtainMessage(2, sbn).sendToTarget();
    }

    @Override // android.media.RemoteController.OnClientUpdateListener
    public void onClientChange(boolean clearing) {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onClientChange: " + clearing);
        }
        if (this.mMediaListener == null) {
            return;
        }
        try {
            this.mMediaListener.onClientChanged(clearing);
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    @Override // android.media.RemoteController.OnClientUpdateListener
    public void onClientPlaybackStateUpdate(int state) {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onClientPlaybackStateUpdate = " + state);
        }
        if (this.mMediaListener == null) {
            return;
        }
        try {
            this.mMediaListener.onClientPlaybackStateUpdate(state, 0L, -1L);
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    @Override // android.media.RemoteController.OnClientUpdateListener
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "onClientPlaybackStateUpdate = " + state + " , currentPosMs = " + currentPosMs);
        }
        if (this.mMediaListener == null) {
            return;
        }
        try {
            this.mMediaListener.onClientPlaybackStateUpdate(state, stateChangeTimeMs, currentPosMs);
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    @Override // android.media.RemoteController.OnClientUpdateListener
    public void onClientTransportControlUpdate(int transportControlFlags) {
    }

    @Override // android.media.RemoteController.OnClientUpdateListener
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
        if (this.mMediaListener == null) {
            return;
        }
        try {
            NowPlayingCardData data = new NowPlayingCardData();
            data.playerPackage = getRccPackageName(this.mRemoteController);
            data.title = metadataEditor.getString(7, getString(R.string.unknown_title));
            String fallbackArtist = getApplicationLabel(data.playerPackage);
            if (TextUtils.isEmpty(fallbackArtist)) {
                fallbackArtist = getString(R.string.unknown_artist);
            }
            data.artist = metadataEditor.getString(2, fallbackArtist);
            data.albumArtist = metadataEditor.getString(13, getString(R.string.unknown_album_artist));
            data.albumTitle = metadataEditor.getString(1, getString(R.string.unknown_album));
            data.year = metadataEditor.getLong(8, -1L);
            data.trackNumber = metadataEditor.getLong(0, -1L);
            data.duration = metadataEditor.getLong(9, -1L);
            data.artwork = getResizedRecommendationBitmap(metadataEditor.getBitmap(100, null), false, false);
            data.launchColor = getColor(data.playerPackage);
            data.playerPackage = getRccPackageName(this.mRemoteController);
            data.pIntent = getPendingIntent((MediaSessionManager) getSystemService("media_session"), data.playerPackage);
            if (data.artwork == null) {
                data.artwork = generateArtwork(data.playerPackage);
            }
            this.mMediaListener.onMediaDataUpdated(data);
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    private String getApplicationLabel(String packageName) {
        try {
            PackageManager pkgMan = getPackageManager();
            return pkgMan.getApplicationLabel(pkgMan.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private Bitmap generateArtwork(String playerPackage) {
        int appColor = getColor(playerPackage);
        int color = Color.rgb((int) (Color.red(appColor) * this.mNowPlayingDefaultDarkening), (int) (Color.green(appColor) * this.mNowPlayingDefaultDarkening), (int) (Color.blue(appColor) * this.mNowPlayingDefaultDarkening));
        Drawable playIcon = getResources().getDrawable(R.drawable.ic_now_playing_default);
        int height = playIcon.getIntrinsicHeight();
        int width = playIcon.getIntrinsicWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(color);
        Canvas canvas = new Canvas(bmp);
        playIcon.setBounds(new Rect(0, 0, width, height));
        playIcon.draw(canvas);
        return bmp;
    }

    private int getColor(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return getResources().getColor(R.color.notif_background_color);
        }
        PackageManager pkgMan = getPackageManager();
        Intent intent = pkgMan.getLeanbackLaunchIntentForPackage(packageName);
        if (intent == null) {
            return getResources().getColor(R.color.notif_background_color);
        }
        ResolveInfo info = pkgMan.resolveActivity(intent, 0);
        if (info == null) {
            return getResources().getColor(R.color.notif_background_color);
        }
        int defaultColor = getResources().getColor(R.color.notif_background_color);
        try {
            Context ctx = createPackageContext(packageName, 0);
            Resources.Theme theme = ctx.getTheme();
            theme.applyStyle(info.activityInfo.getThemeResource(), true);
            int[] values = {android.R.attr.colorPrimary};
            TypedArray a = theme.obtainStyledAttributes(values);
            int color = a.getColor(0, defaultColor);
            a.recycle();
            return color;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return defaultColor;
        }
    }

    private PendingIntent getPendingIntent(MediaSessionManager sessionManager, String fallBackPackageName) {
        if (sessionManager == null) {
            return getPendingIntentFallback(fallBackPackageName);
        }
        List<MediaController> controllers = sessionManager.getActiveSessions(null);
        MediaController controller = null;
        int i = 0;
        while (true) {
            if (i >= controllers.size()) {
                break;
            }
            MediaController temp = controllers.get(i);
            if ((temp.getFlags() & 2) == 0) {
                i++;
            } else {
                controller = temp;
                break;
            }
        }
        if (controller == null) {
            return getPendingIntentFallback(fallBackPackageName);
        }
        PendingIntent pIntent = controller.getSessionActivity();
        if (pIntent == null) {
            return getPendingIntentFallback(controller.getPackageName());
        }
        return pIntent;
    }

    private PendingIntent getPendingIntentFallback(String packageName) {
        PackageManager pkgMan = getPackageManager();
        Intent lbIntent = pkgMan.getLeanbackLaunchIntentForPackage(packageName);
        if (lbIntent == null) {
            return null;
        }
        ComponentName componentName = lbIntent.getComponent();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(componentName);
        intent.addFlags(270532608);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private String getRccPackageName(RemoteController controller) {
        try {
            Class<?> rcClass = Class.forName(controller.getClass().getName());
            Method method = rcClass.getMethod("getRemoteControlClientPackageName", new Class[0]);
            if (method != null) {
                method.setAccessible(true);
                Object res = method.invoke(controller, new Object[0]);
                if (res instanceof String) {
                    return (String) res;
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e("NotifMonitorService", "Failed to get package name from RCC. Exception: " + e);
        } catch (IllegalAccessException e2) {
            Log.e("NotifMonitorService", "Failed to get package name from RCC. Exception: " + e2);
        } catch (IllegalArgumentException e3) {
            Log.e("NotifMonitorService", "Failed to get package name from RCC. Exception: " + e3);
        } catch (NoSuchMethodException e4) {
            Log.e("NotifMonitorService", "Failed to get package name from RCC. Exception: " + e4);
        } catch (InvocationTargetException e5) {
            Log.e("NotifMonitorService", "Failed to get package name from RCC. Exception: " + e5);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchExistingNotificationsLocked() {
        if (Log.isLoggable("NotifMonitorService", 3)) {
            Log.d("NotifMonitorService", "Executing initial Fetch of notifications");
        }
        this.mNotifList.clear();
        this.mNonRecommendationsCount = 0;
        for (StatusBarNotification sbn : getActiveNotificationsReflection(1)) {
            if (!shouldBeIgnored(sbn)) {
                removeRemoteViewsIfPresent(sbn);
                if (isRecommendation(sbn)) {
                    processRecommendationImage(sbn);
                } else {
                    this.mNonRecommendationsCount++;
                }
                this.mNotifList.add(sbn);
            }
        }
        if (this.mMaxRecsPerApp > 0) {
            sortAndTrimRecommendations(this.mMaxRecsPerApp, false);
        }
        if (!Log.isLoggable("NotifMonitorService", 3)) {
            return;
        }
        Log.d("NotifMonitorService", "Initial fetch total = " + this.mNotifList.size());
    }

    private void removeRemoteViewsIfPresent(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        if (notif == null) {
            return;
        }
        notif.contentView = null;
        notif.bigContentView = null;
    }

    private class ServiceHandler extends Handler {
        /* synthetic */ ServiceHandler(NotificationMonitorService this$0, Looper looper, ServiceHandler serviceHandler) {
            this(looper);
        }

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public synchronized void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NotificationMonitorService.this.reportNotificationChangeLocked((StatusBarNotification) msg.obj, true);
                    break;
                case 2:
                    NotificationMonitorService.this.reportNotificationChangeLocked((StatusBarNotification) msg.obj, false);
                    break;
                case 3:
                    NotificationMonitorService.this.mHandler.removeMessages(3);
                    NotificationMonitorService.this.fetchExistingNotificationsLocked();
                    NotificationMonitorService.this.updateAllClientsLocked(NotificationMonitorService.this.mClients, false);
                    NotificationMonitorService.this.updateAllClientsLocked(NotificationMonitorService.this.mPartnerClients, true);
                    if (NotificationMonitorService.this.mMediaListener != null) {
                        NotificationMonitorService.this.registerRemoteController();
                    }
                    NotificationMonitorService.this.mInitialFetchDone = true;
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportNotificationChangeLocked(StatusBarNotification sbn, boolean added) {
        reportNotificationChangeLocked(sbn, added, false);
    }

    private void reportNotificationChangeLocked(StatusBarNotification sbn, boolean added, boolean force) {
        boolean found = false;
        if (shouldBeIgnored(sbn) && !force) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= this.mNotifList.size()) {
                break;
            }
            if (!NotificationUtils.equals(this.mNotifList.get(i), sbn)) {
                i++;
            } else {
                found = true;
                if (added) {
                    removeRemoteViewsIfPresent(sbn);
                    processRecommendationImage(sbn);
                    this.mNotifList.set(i, sbn);
                } else {
                    this.mNotifList.remove(i);
                }
            }
        }
        if (added && !found) {
            removeRemoteViewsIfPresent(sbn);
            processRecommendationImage(sbn);
            this.mNotifList.add(sbn);
        }
        if (isRecommendation(sbn)) {
            RemoteCallbackList<INotificationsMonitorClient> clients = isInPartnerRow(sbn) ? this.mPartnerClients : this.mClients;
            int count = clients.beginBroadcast();
            for (int i2 = 0; i2 < count; i2++) {
                try {
                    INotificationsMonitorClient client = (INotificationsMonitorClient) clients.getBroadcastItem(i2);
                    if (added) {
                        client.onRecommendationPosted(sbn);
                    } else {
                        client.onRecommendationRemoved(sbn);
                    }
                } catch (RemoteException e) {
                    Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
                }
            }
            clients.finishBroadcast();
        } else if (added && !found) {
            this.mNonRecommendationsCount++;
            reportNonRecommendationChangeLocked();
        } else if (!added) {
            this.mNonRecommendationsCount = Math.max(0, this.mNonRecommendationsCount - 1);
            reportNonRecommendationChangeLocked();
        }
        if (!added || found || this.mMaxRecsPerApp <= 0) {
            return;
        }
        sortAndTrimRecommendations(this.mMaxRecsPerApp, true);
    }

    private void reportNonRecommendationChangeLocked() {
        int count = this.mClients.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                ((INotificationsMonitorClient) this.mClients.getBroadcastItem(i)).onNotificationCountChanged(this.mNonRecommendationsCount);
            } catch (RemoteException e) {
                Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
            }
        }
        this.mClients.finishBroadcast();
    }

    private boolean isInPartnerRow(StatusBarNotification sbn) {
        if (Util.isPackageOnSystem(getPackageManager(), sbn.getPackageName())) {
            return "partner_row_entry".equals(sbn.getNotification().getGroup());
        }
        return false;
    }

    private boolean isRecommendation(StatusBarNotification sbn) {
        return TextUtils.equals(sbn.getNotification().category, "recommendation");
    }

    private boolean shouldBeIgnored(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        if (!isRecommendation(sbn) || notif.contentIntent == null) {
            return true;
        }
        return this.mBlacklistedPackages.contains(sbn.getPackageName()) && !isInPartnerRow(sbn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAllClientsLocked(RemoteCallbackList<INotificationsMonitorClient> clients, boolean isPartner) {
        int count = clients.beginBroadcast();
        for (int i = 0; i < count; i++) {
            updateClientLocked((INotificationsMonitorClient) clients.getBroadcastItem(i), isPartner);
        }
        clients.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateClientLocked(INotificationsMonitorClient client, boolean isPartner) {
        if (client == null) {
            return;
        }
        try {
            client.startClientUpdate();
            for (int i = 0; i < this.mNotifList.size(); i++) {
                StatusBarNotification sbn = this.mNotifList.get(i);
                if (isRecommendation(sbn) && isPartner == isInPartnerRow(sbn)) {
                    client.reportExistingRecommendation(sbn);
                }
            }
            client.endClientUpdate();
            client.onNotificationCountChanged(this.mNonRecommendationsCount);
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerRemoteController() {
        if (this.mRemoteController != null) {
            return;
        }
        this.mRemoteController = new RemoteController(this, this);
        if (this.mRemoteController == null) {
            return;
        }
        AudioManager audioMgr = (AudioManager) getSystemService("audio");
        if (!audioMgr.registerRemoteController(this.mRemoteController)) {
            Log.e("NotifMonitorService", "Failed to register RemoteController with Audio Manager.");
            this.mRemoteController = null;
        } else {
            int maxWidth = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_max_width);
            int height = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_height);
            this.mRemoteController.setArtworkConfiguration(maxWidth, height);
            this.mRemoteController.setSynchronizationMode(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterRemoteController() {
        if (this.mRemoteController == null) {
            return;
        }
        ((AudioManager) getSystemService("audio")).unregisterRemoteController(this.mRemoteController);
        this.mRemoteController = null;
    }

    private void notifyServiceStatusChange(boolean isReady) {
        try {
            int count = this.mClients.beginBroadcast();
            for (int i = 0; i < count; i++) {
                ((INotificationsMonitorClient) this.mClients.getBroadcastItem(i)).onServiceStatusChanged(isReady);
            }
            this.mClients.finishBroadcast();
            int count2 = this.mPartnerClients.beginBroadcast();
            for (int i2 = 0; i2 < count2; i2++) {
                ((INotificationsMonitorClient) this.mPartnerClients.getBroadcastItem(i2)).onServiceStatusChanged(isReady);
            }
            this.mPartnerClients.finishBroadcast();
        } catch (RemoteException e) {
            Log.e("NotifMonitorService", "RemoteException:" + e.getMessage());
        }
    }

    protected Bitmap getRecomendationImage(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        String[] keySet = {key};
        StatusBarNotification[] ret = null;
        if (this.mFullyConnected) {
            try {
                ret = getActiveNotificationsReflection(keySet, 0);
            } catch (SecurityException e) {
                Log.d("NotifMonitorService", "Exception fetching activeRecommendations: " + e.toString());
            }
        } else {
            Log.d("NotifMonitorService", "Image Request with DISCONNECTED service, Ignoring request.");
        }
        if (ret == null || ret.length <= 0) {
            return null;
        }
        boolean partner = isInPartnerRow(ret[0]);
        return getResizedRecommendationBitmap(ret[0].getNotification().largeIcon, partner, false);
    }

    protected void processRecommendationImage(StatusBarNotification sbn) {
        if (!isRecommendation(sbn)) {
            return;
        }
        Notification notif = sbn.getNotification();
        boolean partner = isInPartnerRow(sbn);
        Bitmap img = notif.largeIcon != null ? notif.largeIcon : getRecomendationImage(sbn.getKey());
        notif.extras.remove("android.largeIcon");
        if (img == null) {
            return;
        }
        if (!partner) {
            Pair<Integer, Integer> dim = getResizedCardDimmensions(img.getWidth(), img.getHeight());
            if (dim != null) {
                notif.extras.putInt("notif_img_width", ((Integer) dim.first).intValue());
                notif.extras.putInt("notif_img_height", ((Integer) dim.second).intValue());
            }
            notif.largeIcon = getResizedRecommendationBitmap(img, false, true);
            return;
        }
        notif.largeIcon = getResizedRecommendationBitmap(img, true, false);
    }

    private Bitmap getResizedRecommendationBitmap(Bitmap image, boolean isBanner, boolean lowRes) {
        int maxWidth;
        int maxHeight;
        if (isBanner) {
            maxWidth = this.mBannerMaxWidth;
        } else {
            maxWidth = (int) ((!lowRes ? 1.0f : 0.1f) * this.mCardMaxWidth);
        }
        if (isBanner) {
            maxHeight = this.mBannerMaxHeight;
        } else {
            maxHeight = (int) (this.mCardMaxHeight * (lowRes ? 0.1f : 1.0f));
        }
        return Util.getSizeCappedBitmap(image, maxWidth, maxHeight);
    }

    private Pair<Integer, Integer> getResizedCardDimmensions(int imgWidth, int imgHeight) {
        if (imgWidth <= this.mCardMaxWidth && imgHeight <= this.mCardMaxHeight) {
            return new Pair<>(Integer.valueOf(imgWidth), Integer.valueOf(imgHeight));
        }
        if (imgWidth > 0 && imgHeight > 0) {
            float scale = Math.min(1.0f, this.mCardMaxHeight / imgHeight);
            if (scale < 1.0d || imgWidth > this.mCardMaxWidth) {
                int newWidth = (int) Math.min(imgWidth * scale, this.mCardMaxWidth);
                int newHeight = (int) (imgHeight * scale);
                return new Pair<>(Integer.valueOf(newWidth), Integer.valueOf(newHeight));
            }
        }
        return new Pair<>(Integer.valueOf(imgWidth), Integer.valueOf(imgHeight));
    }

    private void sortAndTrimRecommendations(int maxPerApp, boolean reportRemovals) {
        Collections.sort(this.mNotifList, this.mRecSorter);
        String pkg = null;
        int count = 0;
        int i = 0;
        while (i < this.mNotifList.size()) {
            StatusBarNotification notif = this.mNotifList.get(i);
            if (!isInPartnerRow(notif)) {
                if (!TextUtils.equals(pkg, notif.getPackageName())) {
                    pkg = notif.getPackageName();
                    count = 1;
                    i++;
                } else if (count < maxPerApp) {
                    count++;
                    i++;
                } else {
                    cancelNotification(notif.getKey());
                    this.mNotifList.remove(i);
                    if (reportRemovals) {
                        reportNotificationChangeLocked(notif, false);
                    }
                }
            } else {
                i++;
            }
        }
    }

    private void setOnNotificationPostedTrimReflection(int trim) {
        try {
            java.lang.reflect.Method method = getClass().getMethod("setOnNotificationPostedTrim", int.class);
            method.invoke(this, trim);
        } catch (Exception e) {
            // Ignore
        }
    }

    private StatusBarNotification[] getActiveNotificationsReflection(int trim) {
        try {
            java.lang.reflect.Method method = getClass().getMethod("getActiveNotifications", int.class);
            return (StatusBarNotification[]) method.invoke(this, trim);
        } catch (Exception e) {
            return getActiveNotifications();
        }
    }

    private StatusBarNotification[] getActiveNotificationsReflection(String[] keys, int trim) {
        try {
            java.lang.reflect.Method method = getClass().getMethod("getActiveNotifications", String[].class, int.class);
            return (StatusBarNotification[]) method.invoke(this, keys, trim);
        } catch (Exception e) {
            return getActiveNotifications(keys);
        }
    }
}
