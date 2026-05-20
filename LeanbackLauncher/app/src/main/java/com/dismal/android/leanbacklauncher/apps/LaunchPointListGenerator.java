package com.dismal.android.leanbacklauncher.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.dismal.android.leanbacklauncher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/* JADX INFO: loaded from: classes.dex */
public class LaunchPointListGenerator {
    private static final String[] sSpecialSettingsActions = {"android.settings.WIFI_SETTINGS"};
    private final Context mContext;
    private ArrayList<LaunchPoint> mSettingsLaunchPoints;
    private boolean mIsReady = false;
    private boolean mShouldNotify = false;
    private final Queue<CachedAction> mCachedActions = new LinkedList();
    private final List<Listener> mListeners = new LinkedList();
    private final List<LaunchPoint> mAllLaunchPoints = new LinkedList();
    private final List<LaunchPoint> mInstallingLaunchPoints = new LinkedList();
    private HashMap<String, Integer> mUpdatableBlacklist = new HashMap<>();
    private HashMap<String, Integer> mNonUpdatableBlacklist = new HashMap<>();
    private final Object mLock = new Object();

    public interface Listener {
        void onLaunchPointListGeneratorReady();

        void onLaunchPointsAddedOrUpdated(ArrayList<LaunchPoint> arrayList);

        void onLaunchPointsRemoved(ArrayList<LaunchPoint> arrayList);

        void onSettingsChanged();
    }

    private class CachedAction {
        int mAction;
        LaunchPoint mLaunchPoint;
        String mPkgName;
        boolean mSuccess;
        boolean mUpdatable;

        CachedAction(int action, String pkgName) {
            this.mSuccess = false;
            this.mUpdatable = true;
            this.mAction = action;
            this.mPkgName = pkgName;
        }

        CachedAction(LaunchPointListGenerator this$0, int action, String pkgName, boolean updatable) {
            this(action, pkgName);
            this.mUpdatable = updatable;
        }

        CachedAction(int action, LaunchPoint launchPoint) {
            this.mSuccess = false;
            this.mUpdatable = true;
            this.mAction = action;
            this.mLaunchPoint = launchPoint;
        }

        CachedAction(LaunchPointListGenerator this$0, int action, LaunchPoint launchPoint, boolean success) {
            this(action, launchPoint);
            this.mSuccess = success;
        }

        public void apply() {
            switch (this.mAction) {
                case 0:
                    LaunchPointListGenerator.this.addOrUpdatePackage(this.mPkgName);
                    break;
                case 1:
                    LaunchPointListGenerator.this.removePackage(this.mPkgName);
                    break;
                case 2:
                    LaunchPointListGenerator.this.addToBlacklist(this.mPkgName, this.mUpdatable);
                    break;
                case 3:
                    LaunchPointListGenerator.this.removeFromBlacklist(this.mPkgName, this.mUpdatable);
                    break;
                case 4:
                    LaunchPointListGenerator.this.addOrUpdateInstallingLaunchPoint(this.mLaunchPoint);
                    break;
                case 5:
                    LaunchPointListGenerator.this.removeInstallingLaunchPoint(this.mLaunchPoint, this.mSuccess);
                    break;
            }
        }
    }

    public LaunchPointListGenerator(Context ctx) {
        this.mContext = ctx;
        createLaunchPointList();
    }

    public void registerChangeListener(Listener listener) {
        if (this.mListeners.contains(listener)) {
            return;
        }
        this.mListeners.add(listener);
    }

    public void addOrUpdatePackage(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(0, pkgName));
                return;
            }
            synchronized (this.mLock) {
                ArrayList<LaunchPoint> removedLaunchPoints = new ArrayList<>();
                getLaunchPoints(this.mInstallingLaunchPoints, removedLaunchPoints, pkgName, true);
                getLaunchPoints(this.mAllLaunchPoints, removedLaunchPoints, pkgName, true);
                ArrayList<LaunchPoint> launchPoints = createLaunchPoints(pkgName, removedLaunchPoints);
                if (!launchPoints.isEmpty()) {
                    this.mAllLaunchPoints.addAll(launchPoints);
                    if (!isBlacklisted(pkgName) && this.mShouldNotify) {
                        for (Listener cl : this.mListeners) {
                            cl.onLaunchPointsAddedOrUpdated(launchPoints);
                        }
                    }
                }
                if (!removedLaunchPoints.isEmpty() && !isBlacklisted(pkgName) && this.mShouldNotify) {
                    for (Listener cl2 : this.mListeners) {
                        cl2.onLaunchPointsRemoved(removedLaunchPoints);
                    }
                }
                if (packageHasSettingsEntry(pkgName)) {
                    for (Listener cl3 : this.mListeners) {
                        cl3.onSettingsChanged();
                    }
                }
            }
        }
    }

    public void removePackage(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(1, pkgName));
                return;
            }
            synchronized (this.mLock) {
                ArrayList<LaunchPoint> removedLaunchPoints = new ArrayList<>();
                getLaunchPoints(this.mInstallingLaunchPoints, removedLaunchPoints, pkgName, true);
                getLaunchPoints(this.mAllLaunchPoints, removedLaunchPoints, pkgName, true);
                if (!removedLaunchPoints.isEmpty() && !isBlacklisted(pkgName) && this.mShouldNotify) {
                    for (Listener cl : this.mListeners) {
                        cl.onLaunchPointsRemoved(removedLaunchPoints);
                    }
                }
                if (packageHasSettingsEntry(pkgName)) {
                    for (Listener cl2 : this.mListeners) {
                        cl2.onSettingsChanged();
                    }
                }
            }
        }
    }

    public void updateBlackList(ArrayList<String> pkgNames) {
        HashMap<String, Integer> newBlacklist = new HashMap<>();
        for (int i = 0; i < pkgNames.size(); i++) {
            String pkgName = pkgNames.get(i);
            Integer occurrences = newBlacklist.get(pkgName);
            if (occurrences == null) {
                occurrences = 0;
            }
            int iIntValue = occurrences.intValue() + 1;
            Integer.valueOf(iIntValue);
            newBlacklist.put(pkgName, Integer.valueOf(iIntValue));
        }
        synchronized (this.mLock) {
            List<String> toBeRemoved = new LinkedList<>();
            for (String key : this.mUpdatableBlacklist.keySet()) {
                if (!newBlacklist.containsKey(key)) {
                    toBeRemoved.add(key);
                }
            }
            Iterator<String> itt = toBeRemoved.iterator();
            while (itt.hasNext()) {
                removeFromBlacklist(itt.next(), true, true);
            }
            for (String key2 : newBlacklist.keySet()) {
                if (!this.mUpdatableBlacklist.containsKey(key2)) {
                    addToBlacklist(key2);
                }
            }
            this.mUpdatableBlacklist = newBlacklist;
        }
    }

    public boolean addToBlacklist(String pkgName) {
        return addToBlacklist(pkgName, true);
    }

    public boolean addToBlacklist(String pkgName, boolean updatable) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(this, 2, pkgName, updatable));
                return false;
            }
            boolean added = false;
            synchronized (this.mLock) {
                HashMap<String, Integer> blacklist = updatable ? this.mUpdatableBlacklist : this.mNonUpdatableBlacklist;
                HashMap<String, Integer> otherBlacklist = updatable ? this.mNonUpdatableBlacklist : this.mUpdatableBlacklist;
                Integer occurrences = blacklist.get(pkgName);
                Integer otherOccurrences = otherBlacklist.get(pkgName);
                if (occurrences == null || occurrences.intValue() <= 0) {
                    occurrences = 0;
                    if (otherOccurrences == null || otherOccurrences.intValue() <= 0) {
                        added = true;
                        ArrayList<LaunchPoint> blacklistedLaunchPoints = new ArrayList<>();
                        getLaunchPoints(this.mInstallingLaunchPoints, blacklistedLaunchPoints, pkgName, false);
                        getLaunchPoints(this.mAllLaunchPoints, blacklistedLaunchPoints, pkgName, false);
                        if (!blacklistedLaunchPoints.isEmpty() && this.mShouldNotify) {
                            for (Listener cl : this.mListeners) {
                                cl.onLaunchPointsRemoved(blacklistedLaunchPoints);
                            }
                        }
                    }
                }
                int iIntValue = occurrences.intValue() + 1;
                Integer.valueOf(iIntValue);
                blacklist.put(pkgName, Integer.valueOf(iIntValue));
            }
            return added;
        }
    }

    public boolean removeFromBlacklist(String pkgName) {
        return removeFromBlacklist(pkgName, false, true);
    }

    public boolean removeFromBlacklist(String pkgName, boolean updatable) {
        return removeFromBlacklist(pkgName, false, updatable);
    }

    private boolean removeFromBlacklist(String pkgName, boolean force, boolean updatable) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(this, 3, pkgName, updatable));
                return false;
            }
            boolean removed = false;
            synchronized (this.mLock) {
                HashMap<String, Integer> blacklist = updatable ? this.mUpdatableBlacklist : this.mNonUpdatableBlacklist;
                HashMap<String, Integer> otherBlacklist = updatable ? this.mNonUpdatableBlacklist : this.mUpdatableBlacklist;
                Integer occurrences = blacklist.get(pkgName);
                Integer otherOccurrences = otherBlacklist.get(pkgName);
                if (occurrences != null) {
                    Integer occurrences2 = Integer.valueOf(occurrences.intValue() - 1);
                    if (occurrences2.intValue() <= 0 || force) {
                        blacklist.remove(pkgName);
                        if (otherOccurrences == null) {
                            removed = true;
                            ArrayList<LaunchPoint> blacklistedLaunchPoints = new ArrayList<>();
                            getLaunchPoints(this.mInstallingLaunchPoints, blacklistedLaunchPoints, pkgName, false);
                            getLaunchPoints(this.mAllLaunchPoints, blacklistedLaunchPoints, pkgName, false);
                            if (!blacklistedLaunchPoints.isEmpty() && this.mShouldNotify) {
                                for (Listener cl : this.mListeners) {
                                    cl.onLaunchPointsAddedOrUpdated(blacklistedLaunchPoints);
                                }
                            }
                        }
                    } else {
                        blacklist.put(pkgName, occurrences2);
                    }
                }
            }
            return removed;
        }
    }

    public void addOrUpdateInstallingLaunchPoint(LaunchPoint launchPoint) {
        if (launchPoint == null) {
            return;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(4, launchPoint));
                return;
            }
            String pkgName = launchPoint.getPackageName();
            ArrayList<LaunchPoint> launchPoints = new ArrayList<>();
            synchronized (this.mLock) {
                getLaunchPoints(this.mInstallingLaunchPoints, launchPoints, pkgName, true);
                getLaunchPoints(this.mAllLaunchPoints, launchPoints, pkgName, true);
                for (int i = 0; i < launchPoints.size(); i++) {
                    launchPoints.get(i).setInstallationState(launchPoint);
                }
                if (launchPoints.isEmpty()) {
                    launchPoints.add(launchPoint);
                }
                this.mInstallingLaunchPoints.addAll(launchPoints);
                if (!isBlacklisted(pkgName) && this.mShouldNotify) {
                    for (Listener cl : this.mListeners) {
                        cl.onLaunchPointsAddedOrUpdated(launchPoints);
                    }
                }
            }
        }
    }

    public void removeInstallingLaunchPoint(LaunchPoint launchPoint, boolean success) {
        if (launchPoint == null) {
            return;
        }
        synchronized (this.mCachedActions) {
            if (!this.mIsReady) {
                this.mCachedActions.add(new CachedAction(this, 5, launchPoint, success));
            } else {
                if (success) {
                    return;
                }
                addOrUpdatePackage(launchPoint.getPackageName());
            }
        }
    }

    private ArrayList<LaunchPoint> getLaunchPoints(List<LaunchPoint> parentList, ArrayList<LaunchPoint> found, String pkgName, boolean remove) {
        if (found == null) {
            found = new ArrayList<>();
        }
        Iterator<LaunchPoint> itt = parentList.iterator();
        while (itt.hasNext()) {
            LaunchPoint lp = itt.next();
            if (TextUtils.equals(pkgName, lp.getPackageName())) {
                found.add(lp);
                if (remove) {
                    itt.remove();
                }
            }
        }
        return found;
    }

    public ArrayList<LaunchPoint> getGameLaunchPoints() {
        return getLaunchPoints(false, true);
    }

    public ArrayList<LaunchPoint> getNonGameLaunchPoints() {
        return getLaunchPoints(true, false);
    }

    public ArrayList<LaunchPoint> getAllLaunchPoints() {
        return getLaunchPoints(true, true);
    }

    private ArrayList<LaunchPoint> getLaunchPoints(boolean nonGames, boolean games) {
        ArrayList<LaunchPoint> launchPoints = new ArrayList<>();
        synchronized (this.mLock) {
            getLaunchPointsLocked(this.mInstallingLaunchPoints, launchPoints, nonGames, games);
            getLaunchPointsLocked(this.mAllLaunchPoints, launchPoints, nonGames, games);
        }
        return launchPoints;
    }

    private void getLaunchPointsLocked(List<LaunchPoint> parentList, List<LaunchPoint> childList, boolean nonGames, boolean games) {
        boolean z = nonGames ? games : false;
        for (LaunchPoint lp : parentList) {
            if (!isBlacklisted(lp.getPackageName()) && (games == lp.isGame() || z)) {
                childList.add(lp);
            }
        }
    }

    public ArrayList<LaunchPoint> getSettingsLaunchPoints(boolean force) {
        if (force || this.mSettingsLaunchPoints == null) {
            this.mSettingsLaunchPoints = createSettingsList();
        }
        return (ArrayList) this.mSettingsLaunchPoints.clone();
    }

    private void createLaunchPointList() {
        new CreateLaunchPointListTask(this, null).execute(new Void[0]);
    }

    public void refreshLaunchPointList() {
        synchronized (this.mCachedActions) {
            this.mIsReady = false;
            this.mShouldNotify = false;
        }
        new CreateLaunchPointListTask(this, null).execute(new Void[0]);
    }

    private class CreateLaunchPointListTask extends AsyncTask<Void, Void, List<LaunchPoint>> {
        /* synthetic */ CreateLaunchPointListTask(LaunchPointListGenerator this$0, CreateLaunchPointListTask createLaunchPointListTask) {
            this();
        }

        private CreateLaunchPointListTask() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public List<LaunchPoint> doInBackground(Void... params) {
            long startTime = System.currentTimeMillis();
            String category = LaunchPointListGenerator.this.mContext.getString(R.string.launcher_category);
            Intent mainIntent = new Intent("android.intent.action.MAIN");
            mainIntent.addCategory(category);
            List<LaunchPoint> launcherItems = new LinkedList<>();
            PackageManager pkgMan = LaunchPointListGenerator.this.mContext.getPackageManager();
            List<ResolveInfo> rawLaunchPoints = pkgMan.queryIntentActivities(mainIntent, 129);
            int size = rawLaunchPoints.size();
            for (int ptr = 0; ptr < size; ptr++) {
                ResolveInfo info = rawLaunchPoints.get(ptr);
                if (info.activityInfo != null) {
                    launcherItems.add(new LaunchPoint(LaunchPointListGenerator.this.mContext, pkgMan, info));
                }
            }
            Log.d("LeanbackLauncher", "CreateLaunchPointListTask: doInBackground read " + size + " launch points in " + (System.currentTimeMillis() - startTime) + "ms");
            return launcherItems;
        }

        @Override // android.os.AsyncTask
        public void onPostExecute(List<LaunchPoint> launcherItems) {
            synchronized (LaunchPointListGenerator.this.mLock) {
                LaunchPointListGenerator.this.mAllLaunchPoints.clear();
                LaunchPointListGenerator.this.mAllLaunchPoints.addAll(launcherItems);
            }
            synchronized (LaunchPointListGenerator.this.mCachedActions) {
                LaunchPointListGenerator.this.mIsReady = true;
                while (!LaunchPointListGenerator.this.mCachedActions.isEmpty()) {
                    ((CachedAction) LaunchPointListGenerator.this.mCachedActions.remove()).apply();
                }
                LaunchPointListGenerator.this.mShouldNotify = true;
                Iterator<Listener> itt = LaunchPointListGenerator.this.mListeners.iterator();
                while (itt.hasNext()) {
                    itt.next().onLaunchPointListGeneratorReady();
                }
            }
        }
    }

    public boolean isReady() {
        boolean z;
        synchronized (this.mCachedActions) {
            z = this.mIsReady;
        }
        return z;
    }

    private ArrayList<LaunchPoint> createLaunchPoints(String pkgName, ArrayList<LaunchPoint> reusable) {
        if (reusable == null) {
            reusable = new ArrayList<>();
        }
        String category = this.mContext.getString(R.string.launcher_category);
        Intent mainIntent = new Intent("android.intent.action.MAIN");
        mainIntent.setPackage(pkgName).addCategory(category);
        ArrayList<LaunchPoint> launchPoints = new ArrayList<>();
        PackageManager pkgMan = this.mContext.getPackageManager();
        List<ResolveInfo> rawLaunchPoints = pkgMan.queryIntentActivities(mainIntent, 129);
        Iterator<ResolveInfo> rawItt = rawLaunchPoints.iterator();
        while (rawItt.hasNext()) {
            ResolveInfo info = rawItt.next();
            if (info.activityInfo != null) {
                Iterator<LaunchPoint> reusableItt = reusable.iterator();
                while (reusableItt.hasNext()) {
                    LaunchPoint reusableLp = reusableItt.next();
                    if (reusableLp.isInitialInstall() || reusableLp.equals(info)) {
                        launchPoints.add(reusableLp.set(this.mContext, pkgMan, info));
                        reusableItt.remove();
                        rawItt.remove();
                        break;
                    }
                }
            }
        }
        Iterator<ResolveInfo> rawItt2 = rawLaunchPoints.iterator();
        Iterator<LaunchPoint> reusableItt2 = reusable.iterator();
        while (rawItt2.hasNext() && reusableItt2.hasNext()) {
            launchPoints.add(reusableItt2.next().set(this.mContext, pkgMan, rawItt2.next()));
            reusableItt2.remove();
        }
        while (rawItt2.hasNext()) {
            launchPoints.add(new LaunchPoint(this.mContext, pkgMan, rawItt2.next()));
        }
        return launchPoints;
    }

    private ArrayList<LaunchPoint> createSettingsList() {
        Intent mainIntent = new Intent("android.intent.action.MAIN");
        mainIntent.addCategory("android.intent.category.LEANBACK_SETTINGS");
        ArrayList<LaunchPoint> settingsItems = new ArrayList<>();
        PackageManager pkgMan = this.mContext.getPackageManager();
        List<ResolveInfo> rawLaunchPoints = pkgMan.queryIntentActivities(mainIntent, 129);
        HashMap<ComponentName, Integer> specialEntries = new HashMap<>();
        for (int i = 0; i < sSpecialSettingsActions.length; i++) {
            specialEntries.put(getComponentNameForSettingsActivity(sSpecialSettingsActions[i]), Integer.valueOf(i));
        }
        int size = rawLaunchPoints.size();
        for (int ptr = 0; ptr < size; ptr++) {
            ResolveInfo info = rawLaunchPoints.get(ptr);
            boolean system = (info.activityInfo.applicationInfo.flags & 1) != 0;
            ComponentName comp = getComponentName(info);
            int type = -1;
            if (specialEntries.containsKey(comp)) {
                type = specialEntries.get(comp).intValue();
            }
            if (info.activityInfo != null && system) {
                LaunchPoint lp = new LaunchPoint(this.mContext, pkgMan, info, false, type);
                lp.addLaunchIntentFlags(32768);
                settingsItems.add(lp);
            }
        }
        return settingsItems;
    }

    public boolean packageHasSettingsEntry(String packageName) {
        if (this.mSettingsLaunchPoints != null) {
            for (int i = 0; i < this.mSettingsLaunchPoints.size(); i++) {
                if (TextUtils.equals(this.mSettingsLaunchPoints.get(i).getPackageName(), packageName)) {
                    return true;
                }
            }
        }
        Intent mainIntent = new Intent("android.intent.action.MAIN");
        mainIntent.addCategory("android.intent.category.LEANBACK_SETTINGS");
        PackageManager pkgMan = this.mContext.getPackageManager();
        List<ResolveInfo> rawLaunchPoints = pkgMan.queryIntentActivities(mainIntent, 129);
        int size = rawLaunchPoints.size();
        for (int ptr = 0; ptr < size; ptr++) {
            ResolveInfo info = rawLaunchPoints.get(ptr);
            boolean system = (info.activityInfo.applicationInfo.flags & 1) != 0;
            if (info.activityInfo != null && system && TextUtils.equals(info.activityInfo.applicationInfo.packageName, packageName)) {
                return true;
            }
        }
        return false;
    }

    private ComponentName getComponentName(ResolveInfo info) {
        if (info == null) {
            return null;
        }
        return new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
    }

    private ComponentName getComponentNameForSettingsActivity(String action) {
        Intent mainIntent = new Intent(action);
        mainIntent.addCategory("android.intent.category.LEANBACK_SETTINGS");
        PackageManager pkgMan = this.mContext.getPackageManager();
        List<ResolveInfo> launchPoints = pkgMan.queryIntentActivities(mainIntent, 129);
        if (launchPoints.size() > 0) {
            int size = launchPoints.size();
            for (int ptr = 0; ptr < size; ptr++) {
                ResolveInfo info = launchPoints.get(ptr);
                boolean system = (info.activityInfo.applicationInfo.flags & 1) != 0;
                if (info.activityInfo != null && system) {
                    return getComponentName(info);
                }
            }
        }
        return null;
    }

    private boolean isBlacklisted(String pkgName) {
        if (this.mUpdatableBlacklist.containsKey(pkgName)) {
            return true;
        }
        return this.mNonUpdatableBlacklist.containsKey(pkgName);
    }
}
