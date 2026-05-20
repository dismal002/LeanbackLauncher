package com.dismal.android.leanbacklauncher.ranker;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.LaunchPoint;
import com.dismal.android.leanbacklauncher.ranker.DbHelper;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.util.Util;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/* JADX INFO: loaded from: classes.dex */
public class Ranker implements DbHelper.Listener {
    static double GROUP_STARTER_SCORE;
    static double INSTALL_BONUS;
    static double OUT_OF_BOX_BONUS;
    private Context mContext;
    private DbHelper mDbHelper;
    private Comparator<LaunchPoint> mLaunchPointComparator;
    private SortingMode mSortingMode;
    private static String TAG = "LauncherRanker";
    private static boolean DEBUG = false;
    private final Object mEntitiesLock = new Object();
    private Queue<RankingListener> mListeners = new LinkedList();
    private Queue<CachedAction> mCachedActions = new LinkedList();
    private HashMap<String, Entity> mEntities = new HashMap<>();
    private Normalizer mCtrNormalizer = new Normalizer();
    private ArrayList<String> mLastNotficationRankingLogDump = new ArrayList<>();
    private ArrayList<String> mLastLaunchPointRankingLogDump = new ArrayList<>();
    private boolean mQueryingScores = true;

    public interface RankingListener {
        void onRankerReady();
    }

    private static class CachedAction {
        int action;
        String component;
        String group;
        String key;

        CachedAction(String k, String c, String g, int a) {
            this.key = k;
            this.component = c;
            this.group = g;
            this.action = a;
        }
    }

    public enum SortingMode {
        FIXED,
        RECENCY;

        /* JADX INFO: renamed from: values, reason: to resolve conflict with enum method */
        public static SortingMode[] valuesCustom() {
            return values();
        }
    }

    private class LaunchPointRecencyComparator implements Comparator<LaunchPoint> {
        /* synthetic */ LaunchPointRecencyComparator(Ranker this$0, LaunchPointRecencyComparator launchPointRecencyComparator) {
            this();
        }

        private LaunchPointRecencyComparator() {
        }

        @Override // java.util.Comparator
        public int compare(LaunchPoint lhs, LaunchPoint rhs) {
            double lhsScore = Ranker.this.getLastOpened(lhs);
            double rhsScore = Ranker.this.getLastOpened(rhs);
            if (rhsScore > lhsScore) {
                return 1;
            }
            if (rhsScore < lhsScore) {
                return -1;
            }
            return 0;
        }
    }

    private class LaunchPointInstallComparator implements Comparator<LaunchPoint> {
        /* synthetic */ LaunchPointInstallComparator(Ranker this$0, LaunchPointInstallComparator launchPointInstallComparator) {
            this();
        }

        private LaunchPointInstallComparator() {
        }

        @Override // java.util.Comparator
        public int compare(LaunchPoint lhs, LaunchPoint rhs) {
            double lhsOrder = Ranker.this.getEntityOrder(lhs);
            double rhsOrder = Ranker.this.getEntityOrder(rhs);
            double lInstallTime = lhs.getFirstInstallTime();
            double rInstallTime = rhs.getFirstInstallTime();
            if (lhsOrder != rhsOrder) {
                if (lhsOrder == 0.0d) {
                    return 1;
                }
                return (rhsOrder != 0.0d && lhsOrder > rhsOrder) ? 1 : -1;
            }
            if (lInstallTime < 0.0d && rInstallTime >= 0.0d) {
                return 1;
            }
            if (rInstallTime < 0.0d && lInstallTime >= 0.0d) {
                return -1;
            }
            if (lInstallTime != rInstallTime) {
                return lInstallTime > rInstallTime ? 1 : -1;
            }
            return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
        }
    }

    public Ranker(Context ctx, DbHelper dbHelper) {
        this.mSortingMode = SortingMode.FIXED;
        this.mContext = ctx;
        this.mDbHelper = dbHelper;
        this.mSortingMode = getSavedSortingMode();
        this.mDbHelper.getEntities(this);
        Resources res = ctx.getResources();
        TypedValue out = new TypedValue();
        res.getValue(R.dimen.entity_group_starter_score, out, true);
        GROUP_STARTER_SCORE = out.getFloat();
        res.getValue(R.dimen.install_bonus, out, true);
        INSTALL_BONUS = out.getFloat();
        res.getValue(R.dimen.out_of_box_bonus, out, true);
        OUT_OF_BOX_BONUS = out.getFloat();
    }

    public SortingMode getSortingMode() {
        return this.mSortingMode;
    }

    private SortingMode getSavedSortingMode() {
        SortingMode defaultMode = Partner.get(this.mContext).getAppSortingMode();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        if (!prefs.contains("apps_ranker_sorting_mode")) {
            return defaultMode;
        }
        SortingMode defaultMode2 = SortingMode.valueOf(prefs.getString("apps_ranker_sorting_mode", null));
        return defaultMode2;
    }

    public void onAction(String packageName, int actionType) {
        onAction(packageName, null, null, actionType);
    }

    public void onAction(String packageName, String component, int actionType) {
        onAction(packageName, component, null, actionType);
    }

    public void onAction(PendingIntent intent, String group, int actionType) {
        onAction(getPackageName(intent), null, group, actionType);
    }

    public void onAction(String key, String component, String group, int actionType) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        synchronized (this.mCachedActions) {
            if (this.mQueryingScores) {
                if (DEBUG || Log.isLoggable(TAG, 2)) {
                    Log.d(TAG, "Scores not ready, caching this action");
                }
                this.mCachedActions.add(new CachedAction(key, component, group, actionType));
                return;
            }
            if (DEBUG || Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "action: " + actionType + " for " + key + " - group = " + group);
            }
            synchronized (this.mEntitiesLock) {
                Entity entity = this.mEntities.get(key);
                if (actionType == 3) {
                    if (entity != null) {
                        if (entity.getOrder(component) != 0) {
                            entity.onAction(actionType, component, null);
                            this.mDbHelper.removeEntity(key, false);
                        } else {
                            this.mEntities.remove(key);
                            this.mDbHelper.removeEntity(key, true);
                        }
                    }
                } else {
                    if (entity == null) {
                        entity = new Entity(this.mContext, this.mDbHelper, key);
                        this.mEntities.put(key, entity);
                    }
                    entity.onAction(actionType, component, group);
                    this.mDbHelper.saveEntity(entity);
                }
            }
        }
    }

    public void rankNotifications(ArrayList<StatusBarNotification> notifications, RankingListener listener) {
        if (registerListenerIfNecessary(listener)) {
            return;
        }
        if (DEBUG || Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Executing Ranking of existing Recommendations.");
        }
        ArrayList<StatusBarNotification> unorderedNotifications = new ArrayList<>(notifications);
        notifications.clear();
        for (StatusBarNotification i : unorderedNotifications) {
            insertNotification(i, notifications, -1, listener);
        }
        for (int i2 = 0; i2 < notifications.size(); i2++) {
            StatusBarNotification notification = notifications.get(i2);
            double rawScore = getNotificationScore(notification);
            double score = getCachedScore(notification);
            this.mLastNotficationRankingLogDump.add("[" + String.format("%2d", Integer.valueOf(i2)) + "]" + notification.getPackageName() + ", Tag = " + notification.getTag() + ", Id = " + notification.getId() + ", Group = " + notification.getNotification().getGroup() + ", RawScore = " + rawScore + ", SCORE: " + String.format("%.6f", Double.valueOf(score)));
        }
    }

    private final double getAdjustedScore(double score, int groupCount) {
        return score / ((double) (groupCount + 1));
    }

    public void insertNotification(StatusBarNotification sbn, ArrayList<StatusBarNotification> notifications, int oldNotifPosition, RankingListener listener) {
        if (registerListenerIfNecessary(listener)) {
            return;
        }
        if (isValidIndex(oldNotifPosition, notifications)) {
            cacheCtr(sbn, getCachedCtr(notifications.get(oldNotifPosition)));
        } else {
            synchronized (this.mEntitiesLock) {
                Entity entity = this.mEntities.get(sbn.getPackageName());
                if (entity != null && !entity.hasPostedRecommendations()) {
                    entity.markPostedRecommendations();
                    this.mDbHelper.saveEntity(entity);
                }
            }
        }
        String group = sbn.getNotification().getGroup();
        double score = getNotificationScore(sbn);
        int notificationsSize = notifications.size();
        int higherIndex = -1;
        int smallerIndex = notificationsSize;
        int index = 0;
        int groupCount = 0;
        while (true) {
            if (index >= notificationsSize) {
                break;
            }
            StatusBarNotification n = notifications.get(index);
            if (TextUtils.equals(n.getNotification().getGroup(), group)) {
                if (getNotificationScore(n) < score) {
                    smallerIndex = index;
                    break;
                } else {
                    higherIndex = index;
                    groupCount++;
                }
            }
            index++;
        }
        double adjustedScore = getAdjustedScore(score, groupCount);
        cacheScore(sbn, adjustedScore);
        int i = higherIndex + 1;
        int index2 = higherIndex + 1;
        while (index2 < smallerIndex && getCachedScore(notifications.get(index2)) >= adjustedScore) {
            index2++;
        }
        notifications.add(index2, sbn);
        int smallerIndex2 = smallerIndex + 1;
        while (true) {
            groupCount++;
            if (smallerIndex2 > notificationsSize) {
                return;
            }
            StatusBarNotification n2 = notifications.get(smallerIndex2);
            double lia = getAdjustedScore(getNotificationScore(n2), groupCount);
            cacheScore(n2, lia);
            boolean sameGroup = false;
            int index3 = smallerIndex2 + 1;
            while (true) {
                if (index3 > notificationsSize) {
                    break;
                }
                StatusBarNotification t = notifications.get(index3);
                if (!TextUtils.equals(t.getNotification().getGroup(), group)) {
                    if (getCachedScore(t) < lia) {
                        break;
                    } else {
                        index3++;
                    }
                } else {
                    sameGroup = true;
                    break;
                }
            }
            if (index3 > notificationsSize) {
                return;
            }
            if (!sameGroup) {
                notifications.remove(smallerIndex2);
                notifications.add(index3 - 1, n2);
                index3++;
                while (index3 <= notificationsSize && !TextUtils.equals(notifications.get(index3).getNotification().getGroup(), group)) {
                    index3++;
                }
            }
            smallerIndex2 = index3;
        }
    }

    public boolean rankLaunchPoints(ArrayList<LaunchPoint> launchPoints, RankingListener listener) {
        if (registerListenerIfNecessary(listener)) {
            return false;
        }
        if (DEBUG || Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "refreshing Launchpoint ranking");
        }
        synchronized (this.mEntitiesLock) {
            Comparator<LaunchPoint> comp = getLaunchPointComparator();
            Collections.sort(launchPoints, comp);
            this.mLastLaunchPointRankingLogDump.clear();
            this.mLastLaunchPointRankingLogDump.add("Last Launchpoint Ranking Ordering: " + new Date().toString());
            for (LaunchPoint lp : launchPoints) {
                Entity entity = this.mEntities.get(lp.getPackageName());
                if (entity != null) {
                    this.mLastLaunchPointRankingLogDump.add(lp.getTitle() + " | Last Opened " + entity.getOrder(lp.getComponentName()));
                }
            }
        }
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public Comparator<LaunchPoint> getLaunchPointComparator() {
        if (this.mLaunchPointComparator == null) {
            this.mLaunchPointComparator = this.mSortingMode == SortingMode.RECENCY ? new LaunchPointRecencyComparator() : new LaunchPointInstallComparator();
        }
        return this.mLaunchPointComparator;
    }

    public int insertLaunchPoint(ArrayList<LaunchPoint> launchPoints, LaunchPoint newLp) {
        if (DEBUG || Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Inserting new LaunchPoint");
        }
        if (registerListenerIfNecessary(null)) {
            int pos = launchPoints.size();
            launchPoints.add(newLp);
            return pos;
        }
        int pos2 = 0;
        Comparator<LaunchPoint> comp = getLaunchPointComparator();
        while (pos2 < launchPoints.size() && comp.compare(newLp, launchPoints.get(pos2)) >= 0) {
            pos2++;
        }
        launchPoints.add(pos2, newLp);
        return pos2;
    }

    private double getRawScore(Notification notification) {
        try {
            double score = Math.max(0.0d, Math.min(1.0d, Double.parseDouble(notification.getSortKey())));
            return score;
        } catch (Exception e) {
            double score2 = ((double) (notification.priority + 2)) / 4.0d;
            return score2;
        }
    }

    private String getPackageName(PendingIntent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getCreatorPackage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public double getLastOpened(LaunchPoint lp) {
        Entity entity;
        synchronized (this.mEntitiesLock) {
            entity = this.mEntities.get(lp.getPackageName());
        }
        if (entity == null) {
            return -100.0d;
        }
        double value = entity.getLastOpenedTimeStamp(lp.getComponentName());
        return value;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public double getEntityOrder(LaunchPoint lp) {
        Entity entity;
        synchronized (this.mEntitiesLock) {
            entity = this.mEntities.get(lp.getPackageName());
        }
        if (entity == null) {
            return 0.0d;
        }
        double value = entity.getOrder(lp.getComponentName());
        return value;
    }

    private double getNotificationScore(StatusBarNotification sbn) {
        Entity entity;
        if (sbn == null) {
            return -100.0d;
        }
        String packageName = sbn.getPackageName();
        Notification notif = sbn.getNotification();
        if (notif == null || TextUtils.isEmpty(packageName)) {
            return -100.0d;
        }
        synchronized (this.mEntitiesLock) {
            entity = this.mEntities.get(packageName);
        }
        if (entity == null) {
            return -100.0d;
        }
        double ctr = getCachedCtr(sbn);
        if (ctr == -1.0d) {
            ctr = entity.getCtr(this.mCtrNormalizer, notif.getGroup());
            cacheCtr(sbn, ctr);
        }
        double value = entity.getNotificationScore(this.mCtrNormalizer, notif.getGroup(), getRawScore(notif), ctr);
        return value;
    }

    private double getCachedCtr(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        if (extras != null && extras.containsKey("cached_ctr")) {
            return extras.getDouble("cached_ctr");
        }
        return -1.0d;
    }

    private void cacheCtr(StatusBarNotification sbn, double ctr) {
        Bundle extras = sbn.getNotification().extras;
        if (extras == null) {
            extras = new Bundle();
            sbn.getNotification().extras = extras;
        }
        extras.putDouble("cached_ctr", ctr);
    }

    private double getCachedScore(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        if (extras != null && extras.containsKey("cached_score")) {
            return extras.getDouble("cached_score");
        }
        return -1.0d;
    }

    private void cacheScore(StatusBarNotification sbn, double score) {
        Bundle extras = sbn.getNotification().extras;
        if (extras == null) {
            extras = new Bundle();
            sbn.getNotification().extras = extras;
        }
        extras.putDouble("cached_score", score);
    }

    private boolean registerListenerIfNecessary(RankingListener listener) {
        boolean mustRegister;
        synchronized (this.mCachedActions) {
            mustRegister = this.mQueryingScores;
            if (mustRegister) {
                if (DEBUG || Log.isLoggable(TAG, 2)) {
                    Log.d(TAG, "Entities not ready");
                }
                if (listener != null) {
                    this.mListeners.add(listener);
                }
            }
        }
        return mustRegister;
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.DbHelper.Listener
    public void onEntitiesReady(HashMap<String, Entity> entities) {
        synchronized (this.mEntitiesLock) {
            this.mEntities = entities;
        }
        synchronized (this.mCachedActions) {
            this.mQueryingScores = false;
            if (DEBUG || Log.isLoggable(TAG, 2)) {
                Log.d(TAG, "Scores retrieved, playing back " + this.mCachedActions.size() + " actions");
            }
            while (!this.mCachedActions.isEmpty()) {
                CachedAction action = this.mCachedActions.remove();
                onAction(action.key, action.component, action.group, action.action);
            }
            if (!Util.initialRankingApplied(this.mContext)) {
                String[] outOfBoxOrder = this.mContext.getResources().getStringArray(R.array.out_of_box_order);
                String[] partnerOutOfBoxOrder = Partner.get(this.mContext).getOutOfBoxOrder();
                int partnerLength = partnerOutOfBoxOrder != null ? partnerOutOfBoxOrder.length : 0;
                int defaultLength = outOfBoxOrder != null ? outOfBoxOrder.length : 0;
                int totalOrderings = defaultLength + partnerLength;
                if (partnerOutOfBoxOrder != null) {
                    applyOutOfBoxOrdering(partnerOutOfBoxOrder, 0, totalOrderings);
                }
                if (outOfBoxOrder != null) {
                    applyOutOfBoxOrdering(outOfBoxOrder, partnerLength, totalOrderings);
                }
                Util.setInitialRankingAppliedFlag(this.mContext, true);
            }
        }
        while (!this.mListeners.isEmpty()) {
            this.mListeners.remove().onRankerReady();
        }
    }

    public boolean isReady() {
        boolean z;
        synchronized (this.mCachedActions) {
            z = !this.mQueryingScores;
        }
        return z;
    }

    private void applyOutOfBoxOrdering(String[] order, int offsetEntities, int totalEntities) {
        if (order == null || order.length == 0 || offsetEntities < 0 || totalEntities < order.length + offsetEntities) {
            return;
        }
        int entitiesBelow = (totalEntities - offsetEntities) - order.length;
        double bonusSum = ((double) totalEntities) * 0.5d * ((double) (totalEntities + 1));
        int size = order.length;
        for (int i = 0; i < size; i++) {
            String key = order[(size - i) - 1];
            if (!this.mEntities.containsKey(key)) {
                int initialOrder = (entitiesBelow + size) - i;
                int score = entitiesBelow + i + 1;
                Entity e = new Entity(this.mContext, this.mDbHelper, key, score, initialOrder, false);
                double bonus = OUT_OF_BOX_BONUS * (((double) score) / bonusSum);
                e.setBonusValues(bonus, new Date().getTime());
                this.mEntities.put(key, e);
                this.mDbHelper.saveEntity(e);
            }
        }
    }

    private boolean isValidIndex(int index, ArrayList<?> array) {
        return index >= 0 && index < array.size();
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.println(prefix + "==========================");
        for (String lpLine : this.mLastLaunchPointRankingLogDump) {
            writer.println(prefix + " " + lpLine);
        }
        writer.println(prefix + "==========================");
        for (String notifLine : this.mLastNotficationRankingLogDump) {
            writer.println(prefix + " " + notifLine);
        }
        writer.println(prefix + "==========================");
    }
}
