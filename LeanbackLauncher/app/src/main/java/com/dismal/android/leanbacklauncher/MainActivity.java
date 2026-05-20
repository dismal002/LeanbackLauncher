package com.dismal.android.leanbacklauncher;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.dismal.android.leanbacklauncher.SearchView;
import com.dismal.android.leanbacklauncher.animation.AnimatorLifecycle;
import com.dismal.android.leanbacklauncher.animation.LauncherDismissAnimator;
import com.dismal.android.leanbacklauncher.animation.LauncherLaunchAnimator;
import com.dismal.android.leanbacklauncher.animation.LauncherReturnAnimator;
import com.dismal.android.leanbacklauncher.animation.MassSlideAnimator;
import com.dismal.android.leanbacklauncher.animation.ParticipatesInLaunchAnimation;
import com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator;
import com.dismal.android.leanbacklauncher.notifications.NotificationMonitorService;
import com.dismal.android.leanbacklauncher.notifications.NotificationRowView;
import com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper;
import com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.util.Util;
import com.dismal.android.leanbacklauncher.wallpaper.LauncherWallpaper;
import com.dismal.android.recline.util.DrawableDownloader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends Activity {
    private AppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mFadeDismissAndSummonAnimations;
    private HomeScreenAdapter mHomeAdapter;
    private int mIdlePeriod;
    private boolean mIsIdle;
    private LaunchPointListGenerator mLaunchPointListGenerator;
    private VerticalGridView mList;
    private NotificationViewFlipper mNotificationsFlipper;
    private NotificationRowView mNotificationsView;
    private Ranker mRanker;
    private int mResetPeriod;
    private HomeScrollManager mScrollManager;
    private boolean mShyMode;
    private LauncherWallpaper mWallpaper;
    private final Runnable mRefreshHomeAdapter = new Runnable() { // from class: com.dismal.android.leanbacklauncher.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.mHomeAdapter == null) {
                return;
            }
            MainActivity.this.mHomeAdapter.refreshAdapterData();
        }
    };
    private final Runnable mMoveTaskToBack = new Runnable() { // from class: com.dismal.android.leanbacklauncher.MainActivity.2
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.moveTaskToBack(true)) {
                return;
            }
            MainActivity.this.mAnimation.reset();
        }
    };
    private AnimatorLifecycle mAnimation = new AnimatorLifecycle();
    private ArrayList<IdleListener> mIdleListeners = new ArrayList<>();
    private boolean mUserInteracted = false;
    private boolean mFirstUiVisibleNotification = false;
    private boolean mSystemWallpaperCleared = false;
    private Handler mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.MainActivity.3
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                    MainActivity.this.mIsIdle = msg.what == 1;
                    for (int i = 0; i < MainActivity.this.mIdleListeners.size(); i++) {
                        ((IdleListener) MainActivity.this.mIdleListeners.get(i)).onIdleStateChange(MainActivity.this.mIsIdle);
                    }
                    break;
                case 3:
                    MainActivity.this.resetLauncherState(true);
                    break;
                case 4:
                    MainActivity.this.onNotificationRowStateUpdate(msg.arg1);
                    break;
                case 5:
                    MainActivity.this.mHomeAdapter.onUiVisible();
                    break;
                case 6:
                    MainActivity.this.addWidget();
                    break;
                case 7:
                    MainActivity.this.checkLaunchPointPositions();
                    break;
            }
        }
    };

    public interface IdleListener {
        void onIdleStateChange(boolean z);

        void onVisibilityChange(boolean z);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        Log.d("LeanbackLauncher", "onCreate started");
        super.onCreate(savedInstanceState);
        
        long t1 = System.currentTimeMillis();
        DrawableDownloader.getInstance(getApplicationContext());
        Log.d("LeanbackLauncher", "DrawableDownloader init took " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        setContentView(R.layout.activity_main);
        Log.d("LeanbackLauncher", "setContentView took " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        if (Partner.get(this).showLiveTvOnStartUp() && checkFirstRunAfterBoot()) {
            Intent tvIntent = new Intent("android.intent.action.VIEW", TvContract.buildChannelUri(0L));
            List<ResolveInfo> tvActivities = getPackageManager().queryIntentActivities(tvIntent, 1);
            if (tvActivities.size() > 0) {
                startActivity(tvIntent);
                finish();
            }
        }
        Log.d("LeanbackLauncher", "Partner & LiveTV check took " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        writeNotificationListenerSetting();
        Log.d("LeanbackLauncher", "writeNotificationListenerSetting took " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        this.mRanker = new Ranker(this, ((LauncherApplication) getApplication()).getDbHelper());
        Log.d("LeanbackLauncher", "Ranker init took " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        this.mLaunchPointListGenerator = new LaunchPointListGenerator(this);
        Log.d("LeanbackLauncher", "LaunchPointListGenerator init took " + (System.currentTimeMillis() - t1) + "ms");
        
        this.mWallpaper = (LauncherWallpaper) findViewById(R.id.background_container);
        this.mScrollManager = new HomeScrollManager();
        this.mScrollManager.addHomeScrollListener(this.mWallpaper);
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetHost = new AppWidgetHost(this, 123);
        this.mList = (VerticalGridView) findViewById(R.id.main_list_view);
        if (this.mList != null) {
            this.mList.setHasFixedSize(true);
            this.mList.setWindowAlignment(1);
            this.mList.setWindowAlignmentOffset(getResources().getDimensionPixelOffset(R.dimen.home_screen_selected_row_alignment));
            this.mList.setWindowAlignmentOffsetPercent(-1.0f);
            this.mList.setItemAlignmentOffset(0);
            this.mList.setItemAlignmentOffsetPercent(-1.0f);
            
            t1 = System.currentTimeMillis();
            this.mHomeAdapter = new HomeScreenAdapter(this, this.mScrollManager, this.mLaunchPointListGenerator, this.mRanker);
            Log.d("LeanbackLauncher", "HomeScreenAdapter init took " + (System.currentTimeMillis() - t1) + "ms");
            
            this.mList.setItemViewCacheSize(this.mHomeAdapter.getItemCount());
            this.mList.setAdapter(this.mHomeAdapter);
            this.mList.setOnChildSelectedListener(this.mHomeAdapter);
            this.mList.setAnimateChildLayout(false);
            final NotificationsAdapter recAdapter = this.mHomeAdapter.getRecommendationsAdapter();
            addIdleListener(recAdapter);
            this.mList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() { // from class: com.dismal.android.leanbacklauncher.MainActivity.4
                /* JADX WARN: Multi-variable type inference failed */
                @Override // android.view.ViewGroup.OnHierarchyChangeListener
                public void onChildViewAdded(View parent, View view) {
                    int tag = 0;
                    if (view.getTag() instanceof Integer) {
                        tag = ((Integer) view.getTag()).intValue();
                    }
                    switch (tag) {
                        case 0:
                            if (view instanceof SearchOrbView) {
                                ((SearchOrbView) view).setLaunchListener(new SearchView.SearchLaunchListener() { // from class: com.dismal.android.leanbacklauncher.MainActivity.4.2
                                    @Override // com.dismal.android.leanbacklauncher.SearchView.SearchLaunchListener
                                    public void onSearchLaunched() {
                                        MainActivity.this.setShyMode(true);
                                    }
                                });
                            }
                            MainActivity.this.addWidget();
                            break;
                        case 1:
                        case 2:
                            MainActivity.this.mNotificationsFlipper = (NotificationViewFlipper) view.findViewById(R.id.notif_view_flipper);
                            if (MainActivity.this.mNotificationsFlipper != null) {
                                if (tag == 1) {
                                    recAdapter.setNotificationRowViewFlipper(MainActivity.this.mNotificationsFlipper);
                                    MainActivity.this.mNotificationsView = (NotificationRowView) MainActivity.this.mNotificationsFlipper.findViewById(R.id.list);
                                    if (MainActivity.this.mNotificationsView != null) {
                                        MainActivity.this.mNotificationsView.setListener(MainActivity.this.mWallpaper);
                                        MainActivity.this.mNotificationsView.setGravity(48);
                                    }
                                }
                                MainActivity.this.mNotificationsFlipper.setListener(new NotificationViewFlipper.ChangeListener() { // from class: com.dismal.android.leanbacklauncher.MainActivity.4.1
                                    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper.ChangeListener
                                    public void onStateChanged(int state) {
                                        MainActivity.this.mHandler.sendMessageDelayed(MainActivity.this.mHandler.obtainMessage(4, state, 0), 500L);
                                        if (state != 2 || MainActivity.this.mFirstUiVisibleNotification) {
                                            return;
                                        }
                                        MainActivity.this.mFirstUiVisibleNotification = true;
                                        MainActivity.this.mHandler.sendEmptyMessageDelayed(5, 1500L);
                                    }
                                });
                            }
                            break;
                    }
                    if (!(view instanceof IdleListener) || MainActivity.this.mIdleListeners.contains(view)) {
                        return;
                    }
                    MainActivity.this.addIdleListener((IdleListener) view);
                }

                @Override // android.view.ViewGroup.OnHierarchyChangeListener
                public void onChildViewRemoved(View parent, View child) {
                }
            });
            this.mList.setOnScrollListener(new RecyclerView.OnScrollListener() { // from class: com.dismal.android.leanbacklauncher.MainActivity.5
                @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    MainActivity.this.mScrollManager.setScrollOffset(MainActivity.this.getCurrentScrollPos());
                }
            });
        }
        this.mShyMode = true;
        setShyMode(!this.mShyMode);
        this.mIdlePeriod = getResources().getInteger(R.integer.idle_period);
        this.mResetPeriod = getResources().getInteger(R.integer.reset_period);
        this.mFadeDismissAndSummonAnimations = getResources().getBoolean(R.bool.app_launch_animation_fade);
        this.mHomeAdapter.onInitUi();
        Log.d("LeanbackLauncher", "onCreate finished, total time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Override // android.app.Activity
    public void onDestroy() {
        Log.d("LeanbackLauncher", "onDestroy: " + (this.mHomeAdapter == null));
        super.onDestroy();
        if (this.mHomeAdapter == null) {
            return;
        }
        this.mHomeAdapter.onStopUi();
        this.mHomeAdapter.unregisterReceivers();
    }

    @Override // android.app.Activity
    public void onUserInteraction() {
        if (!hasWindowFocus()) {
            return;
        }
        this.mUserInteracted = true;
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        if (this.mIsIdle) {
            this.mHandler.sendEmptyMessage(2);
        }
        this.mHandler.sendEmptyMessageDelayed(1, this.mIdlePeriod);
        this.mHandler.sendEmptyMessageDelayed(3, this.mResetPeriod);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addIdleListener(IdleListener listener) {
        this.mIdleListeners.add(listener);
        listener.onVisibilityChange(true);
        listener.onIdleStateChange(this.mIsIdle);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.mAnimation.isRunning()) {
            this.mAnimation.cancel();
        } else {
            if (this.mAnimation.isPrimed()) {
                this.mAnimation.reset();
                return;
            }
            if (this.mAnimation.isFinished()) {
                this.mAnimation.reset();
            }
            dismissLauncher();
        }
    }

    public void onBackgroundVisibleBehindChanged(boolean visible) {
        Log.d("LeanbackLauncher", "onBackgroundVisibleBehindChanged: " + visible);
        setShyMode(!visible);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setShyMode(boolean shy) {
        if (this.mShyMode == shy) {
            return;
        }
        this.mShyMode = shy;
        if (this.mShyMode) {
            convertFromTranslucentReflection();
        } else {
            convertToTranslucentReflection();
        }
        Log.d("LeanbackLauncher", "Launcher overlay shy mode set to: " + this.mShyMode);
        this.mWallpaper.setShynessMode(this.mShyMode);
        if (!this.mShyMode || this.mNotificationsView == null) {
            return;
        }
        this.mNotificationsView.refreshSelectedBackground();
    }

    private boolean dismissLauncher() {
        if (this.mShyMode) {
            return false;
        }
        LauncherDismissAnimator animation = new LauncherDismissAnimator(this.mList, this.mFadeDismissAndSummonAnimations, this.mHomeAdapter.getRowHeaders());
        this.mAnimation.init(animation, this.mMoveTaskToBack, (byte) 0);
        this.mAnimation.start();
        return true;
    }

    @Override // android.app.Activity
    public void onNewIntent(Intent intent) {
        if (this.mAnimation.isRunning()) {
            this.mAnimation.cancel();
            return;
        }
        if (this.mAnimation.isPrimed()) {
            this.mAnimation.reset();
        }
        if (this.mAnimation.isFinished()) {
            this.mAnimation.reset();
        }
        if (hasWindowFocus()) {
            if (dismissLauncher()) {
                return;
            }
            resetLauncherState(true);
        } else {
            if (this.mAnimation.isInitialized() || this.mAnimation.isScheduled()) {
                return;
            }
            resetLauncherState(false);
            MassSlideAnimator animation = new MassSlideAnimator.Builder(this.mList).setDirection(MassSlideAnimator.Direction.SLIDE_IN).setFade(this.mFadeDismissAndSummonAnimations).build();
            this.mAnimation.init(animation, this.mRefreshHomeAdapter, (byte) 32);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetLauncherState(boolean smooth) {
        this.mScrollManager.setScrollOffset(0);
        this.mUserInteracted = false;
        this.mHomeAdapter.resetRowPositions(smooth);
        int notifIndex = this.mHomeAdapter.getRowIndex(1);
        if (notifIndex != -1 && this.mList.getSelectedPosition() != notifIndex) {
            if (smooth) {
                this.mList.setSelectedPositionSmooth(notifIndex);
            } else {
                this.mList.setSelectedPosition(notifIndex);
            }
            if (!this.mShyMode && this.mNotificationsView != null) {
                this.mNotificationsView.setIgnoreNextActivateBackgroundChange();
            }
        }
        this.mAnimation.cancel();
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.startListening();
        }
        Log.d("LeanbackLauncher", "onStart: visibleBehindState = " + isBackgroundVisibleBehindReflection());
        setShyMode(!isBackgroundVisibleBehindReflection());
        if (this.mHomeAdapter != null) {
            if (this.mFirstUiVisibleNotification) {
                this.mHomeAdapter.onUiVisible();
            }
            this.mHomeAdapter.refreshAdapterData();
        }
        if (!this.mAnimation.isInitialized()) {
            LauncherReturnAnimator animation = new LauncherReturnAnimator(this.mList, this.mAnimation.lastKnownEpicenter, this.mHomeAdapter.getRowHeaders(), this.mNotificationsFlipper);
            this.mAnimation.init(animation, this.mRefreshHomeAdapter, (byte) 32);
        }
        this.mAnimation.schedule();
    }

    @Override // android.app.Activity
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle savedInstanceState) {
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d("LeanbackLauncher", "dispatchTouchEvent: " + ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override // android.app.Activity
    protected void onResume() {
        Log.d("LeanbackLauncher", "onResume");
        super.onResume();
        setShyMode(!isBackgroundVisibleBehindReflection());
        this.mWallpaper.setShynessMode(this.mShyMode);
        this.mWallpaper.resetBackground(this.mShyMode);
        if (this.mShyMode && this.mNotificationsView != null) {
            this.mNotificationsView.refreshSelectedBackground();
        }
        this.mHandler.sendEmptyMessage(6);
        if (this.mHomeAdapter != null) {
            this.mHomeAdapter.animateSearchIn();
        }
        for (int i = 0; i < this.mIdleListeners.size(); i++) {
            this.mIdleListeners.get(i).onVisibilityChange(true);
        }
        this.mHandler.sendEmptyMessage(2);
        this.mHandler.sendEmptyMessageDelayed(1, this.mIdlePeriod);
        this.mHandler.sendEmptyMessageDelayed(7, 2000L);
        if (this.mAnimation.isFinished()) {
            this.mAnimation.reset();
        }
        if (this.mAnimation.isInitialized()) {
            this.mAnimation.reset();
        }
        if (this.mAnimation.isScheduled()) {
            primeAnimationAfterLayout();
        }
        overridePendingTransition(R.anim.home_fade_in_top, R.anim.home_fade_out_bottom);
    }

    @Override // android.app.Activity
    public void onEnterAnimationComplete() {
        if (!this.mSystemWallpaperCleared) {
            this.mSystemWallpaperCleared = true;
            clearSystemWallpaper();
        }
        if (!this.mAnimation.isScheduled() && !this.mAnimation.isPrimed()) {
            return;
        }
        this.mAnimation.start();
    }

    @Override // android.app.Activity
    protected void onPause() {
        Log.d("LeanbackLauncher", "onPause");
        super.onPause();
        this.mAnimation.cancel();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(6);
        this.mHandler.removeMessages(7);
        for (int i = 0; i < this.mIdleListeners.size(); i++) {
            this.mIdleListeners.get(i).onVisibilityChange(false);
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        Log.d("LeanbackLauncher", "onStop");
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.stopListening();
        }
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mHomeAdapter != null) {
            this.mHomeAdapter.onUiInvisible();
        }
        this.mWallpaper.resetBackground(this.mShyMode);
        super.onStop();
        setShyMode(false);
        resetRowsIfNeeded(false);
        this.mAnimation.reset();
    }

    @Override // android.app.Activity
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        this.mRanker.dump(prefix, writer);
        this.mAnimation.dump(prefix, writer, this.mList);
    }

    private void resetRowsIfNeeded(boolean force) {
        for (int i = 0; i < this.mList.getChildCount(); i++) {
            View v = this.mList.getChildAt(i);
            if (v instanceof ActiveFrame) {
                ((ActiveFrame) v).resetRowIfNeeded(force);
            }
        }
    }

    private void writeNotificationListenerSetting() {
        String listeners;
        String listeners2 = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        String component = new ComponentName(this, (Class<?>) NotificationMonitorService.class).flattenToShortString();
        String[] list = listeners2 == null ? new String[0] : listeners2.split("\\s*:\\s*");
        boolean enabled = false;
        int i = 0;
        while (true) {
            if (i >= list.length) {
                break;
            }
            if (!TextUtils.equals(list[i], component)) {
                i++;
            } else {
                enabled = true;
                break;
            }
        }
        if (enabled) {
            return;
        }
        PackageManager pm = getPackageManager();
        if (pm.checkPermission("android.permission.WRITE_SECURE_SETTINGS", getPackageName()) == -1) {
            return;
        }
        if (listeners2 == null || listeners2.length() == 0) {
            listeners = component;
        } else {
            listeners = listeners2 + ":" + component;
        }
        Settings.Secure.putString(getContentResolver(), "enabled_notification_listeners", listeners);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurrentScrollPos() {
        int pos = 0;
        int topView = -1;
        int i = 0;
        while (true) {
            if (i >= this.mList.getChildCount()) {
                break;
            }
            View v = this.mList.getChildAt(i);
            if (v == null || v.getTop() > 0) {
                i++;
            } else {
                topView = this.mList.getChildAdapterPosition(v);
                if (v.getMeasuredHeight() > 0) {
                    float fraction = Math.abs(v.getTop()) / (float) v.getMeasuredHeight();
                    pos = (int) (this.mHomeAdapter.getScrollOffset(topView) * fraction * (-1.0f));
                }
                break;
            }
        }
        for (int topView2 = topView - 1; topView2 >= 0; topView2--) {
            pos -= this.mHomeAdapter.getScrollOffset(topView2);
        }
        return pos;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNotificationRowStateUpdate(int state) {
        int searchIndex;
        if (state == 0 || state == 1) {
            if (this.mUserInteracted || (searchIndex = this.mHomeAdapter.getRowIndex(0)) == -1) {
                return;
            }
            this.mList.setSelectedPosition(searchIndex);
            View child = this.mList.getChildAt(searchIndex);
            if (child != null) {
                child.requestFocus();
            } else {
                Log.w("LeanbackLauncher", "onNotificationRowStateUpdate: child at index " + searchIndex + " is null");
            }
            return;
        }
        if (state != 2) {
            return;
        }
        int notifIndex = this.mHomeAdapter.getRowIndex(0);
        if (this.mList.getSelectedPosition() > notifIndex || this.mNotificationsView == null || this.mNotificationsView.getChildCount() <= 0) {
            return;
        }
        this.mNotificationsView.setSelectedPosition(0);
        View child = this.mNotificationsView.getChildAt(0);
        if (child != null) {
            child.requestFocus();
        } else {
            Log.w("LeanbackLauncher", "onNotificationRowStateUpdate: notification child at index 0 is null");
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean onSearchRequested() {
        setShyMode(true);
        return super.onSearchRequested();
    }

    public static final boolean isMediaKey(int keyCode) {
        switch (keyCode) {
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
                return true;
            default:
                return false;
        }
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mAnimation.isPrimed() || this.mAnimation.isRunning()) {
            switch (keyCode) {
                case 3:
                case 4:
                    return super.onKeyDown(keyCode, event);
                default:
                    return true;
            }
        }
        if (this.mShyMode || !isMediaKey(event.getKeyCode())) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!this.mShyMode && isMediaKey(event.getKeyCode())) {
            switch (keyCode) {
                case 79:
                case 85:
                case 86:
                case 127:
                    setShyMode(true);
                default:
                    return true;
            }
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addWidget() {
        boolean success = false;
        int appWidgetId = Util.getWidgetId(this);
        ComponentName appWidgetComp = Partner.get(this).getWidgetComponentName();
        LinearLayout wrapper = (LinearLayout) findViewById(R.id.widget_wrapper);
        if (wrapper == null) {
            return;
        }
        wrapper.removeAllViews();
        if (appWidgetComp != null) {
            Iterator appWidgetInfo$iterator = this.mAppWidgetManager.getInstalledProviders().iterator();
            while (true) {
                if (!appWidgetInfo$iterator.hasNext()) {
                    break;
                }
                AppWidgetProviderInfo appWidgetInfo = (AppWidgetProviderInfo) appWidgetInfo$iterator.next();
                if (appWidgetComp.equals(appWidgetInfo.provider)) {
                    success = appWidgetId != 0;
                    if (success && !appWidgetComp.equals(Util.getWidgetComponentName(this))) {
                        clearWidget(appWidgetId);
                        success = false;
                    }
                    if (!success) {
                        int width = (int) getResources().getDimension(R.dimen.widget_width);
                        int height = (int) getResources().getDimension(R.dimen.widget_height);
                        Bundle options = new Bundle();
                        options.putInt("appWidgetMinWidth", width);
                        options.putInt("appWidgetMaxWidth", width);
                        options.putInt("appWidgetMinHeight", height);
                        options.putInt("appWidgetMaxHeight", height);
                        appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                        success = this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetInfo.provider, options);
                    }
                    if (success) {
                        AppWidgetHostView hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                        hostView.setAppWidget(appWidgetId, appWidgetInfo);
                        wrapper.addView(hostView);
                        Util.setWidget(this, appWidgetId, appWidgetInfo.provider);
                    }
                }
            }
        }
        if (success) {
            return;
        }
        clearWidget(appWidgetId);
        wrapper.addView(LayoutInflater.from(this).inflate(R.layout.clock, (ViewGroup) null));
    }

    private void clearWidget(int appWidgetId) {
        if (appWidgetId != 0) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
        Util.clearWidget(this);
    }

    private void clearSystemWallpaper() {
        WallpaperManager wpManager = WallpaperManager.getInstance(this);
        try {
            wpManager.clear();
        } catch (IOException e) {
            Log.e("LeanbackLauncher", "error clearing wallpaper", e);
        }
    }

    private boolean checkFirstRunAfterBoot() {
        Intent dummyIntent = new Intent("android.intent.category.LEANBACK_LAUNCHER");
        dummyIntent.setClass(this, DummyActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0, dummyIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        boolean firstRun = intent == null;
        if (firstRun) {
            PendingIntent intent2 = PendingIntent.getActivity(this, 0, dummyIntent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmMgr = (AlarmManager) getSystemService("alarm");
            alarmMgr.set(2, SystemClock.elapsedRealtime() + 711573504, intent2);
        }
        return firstRun;
    }

    public void beginLaunchAnimation(View view, int color, Runnable onCompleteCallback) {
        if (this.mAnimation.isPrimed() || this.mAnimation.isRunning() || this.mAnimation.isFinished()) {
            return;
        }
        getBoundsOnScreen(view, this.mAnimation.lastKnownEpicenter);
        LauncherLaunchAnimator animation = new LauncherLaunchAnimator(this.mList, view, this.mAnimation.lastKnownEpicenter, (ImageView) findViewById(R.id.click_circle_layer), color, this.mHomeAdapter.getRowHeaders(), this.mNotificationsFlipper);
        this.mAnimation.init(animation, onCompleteCallback, (byte) 0);
        this.mAnimation.start();
    }

    public boolean isAnimationInProgress() {
        if (this.mAnimation.isPrimed()) {
            return true;
        }
        return this.mAnimation.isRunning();
    }

    public void includeInAnimation(View target) {
        this.mAnimation.include(target);
    }

    public void excludeFromAnimation(View target) {
        this.mAnimation.exclude(target);
    }

    public void setOnAnimationFinishedListener(AnimatorLifecycle.OnAnimationFinishedListener l) {
        this.mAnimation.setOnAnimationFinishedListener(l);
    }

    private void primeAnimationAfterLayout() {
        this.mList.getRootView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.dismal.android.leanbacklauncher.MainActivity.6
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                MainActivity.this.mList.getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!MainActivity.this.mAnimation.isScheduled()) {
                    return;
                }
                MainActivity.this.mAnimation.prime();
            }
        });
        this.mList.requestLayout();
    }

    private static void getBoundsOnScreen(View v, Rect epicenter) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        epicenter.left = location[0];
        epicenter.top = location[1];
        epicenter.right = epicenter.left + Math.round(v.getWidth() * v.getScaleX());
        epicenter.bottom = epicenter.top + Math.round(v.getHeight() * v.getScaleY());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkLaunchPointPositions() {
        if (this.mAnimation.isRunning() || !checkViewHierarchy(this.mList)) {
            return;
        }
        StringWriter buf = new StringWriter();
        buf.append((CharSequence) "Caught partially animated state; resetting...\n");
        this.mAnimation.dump("", new PrintWriter(buf), this.mList);
        Log.w("Animations", buf.toString());
        this.mAnimation.reset();
    }

    private boolean checkViewHierarchy(View view) {
        if ((view instanceof ParticipatesInLaunchAnimation) && view.getTranslationY() != 0.0f) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int n = group.getChildCount();
            for (int i = 0; i < n; i++) {
                if (checkViewHierarchy(group.getChildAt(i))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void convertFromTranslucentReflection() {
        try {
            java.lang.reflect.Method method = android.app.Activity.class.getDeclaredMethod("convertFromTranslucent");
            method.setAccessible(true);
            method.invoke(this);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void convertToTranslucentReflection() {
        try {
            for (java.lang.reflect.Method method : android.app.Activity.class.getDeclaredMethods()) {
                if (method.getName().equals("convertToTranslucent")) {
                    method.setAccessible(true);
                    if (method.getParameterTypes().length == 2) {
                        method.invoke(this, null, null);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private boolean isBackgroundVisibleBehindReflection() {
        try {
            java.lang.reflect.Method method = android.app.Activity.class.getDeclaredMethod("isBackgroundVisibleBehind");
            method.setAccessible(true);
            return (Boolean) method.invoke(this);
        } catch (Exception e) {
            return false;
        }
    }
}
