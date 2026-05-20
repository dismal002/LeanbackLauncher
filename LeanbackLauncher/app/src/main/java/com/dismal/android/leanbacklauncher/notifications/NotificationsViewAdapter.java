package com.dismal.android.leanbacklauncher.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.util.Log;
import com.dismal.android.leanbacklauncher.MainActivity;
import com.dismal.android.leanbacklauncher.notifications.StringDifference;
import com.dismal.android.leanbacklauncher.util.Preconditions;
import com.dismal.android.leanbacklauncher.widget.RowViewAdapter;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/* JADX INFO: loaded from: classes.dex */
public abstract class NotificationsViewAdapter<VH extends RecyclerView.ViewHolder> extends RowViewAdapter<VH> implements MainActivity.IdleListener {

    /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] f5com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;
    private ArrayList<StatusBarNotification> mCmdMasterList;
    private final Handler mHandler;
    protected final ArrayList<StatusBarNotification> mMasterList;
    private final LinkedHashSet<StatusBarNotification> mNotifToBeRemoved;
    private NotificationsTestHarness mNotificationsTestHarness;
    private NotificationsViewAdapter<VH>.PrioritizeRowUpdateState mPrioritizeRowUpdateState;
    private final Handler mRowUpdateStateTick;
    private final Runnable mStateTickRunnable;
    private final ArrayList<StatusBarNotification> mSyncedList;
    private int mUiState;
    private boolean mUpdateUiWhenVisible;
    private NotificationViewFlipper mViewFlipper;

    /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] m155getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues() {
        if (f5com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues != null) {
            return f5com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;
        }
        int[] iArr = new int[StringDifference.EditItem.Op.valuesCustom().length];
        try {
            iArr[StringDifference.EditItem.Op.DELETE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[StringDifference.EditItem.Op.INSERT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[StringDifference.EditItem.Op.MOVE.ordinal()] = 5;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[StringDifference.EditItem.Op.SUB.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[StringDifference.EditItem.Op.UPDATE.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        f5com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues = iArr;
        return iArr;
    }

    protected abstract void insertNotification(StatusBarNotification statusBarNotification, int i);

    protected abstract boolean isPartnerClient();

    protected abstract void onNotificationCanceled(StatusBarNotification statusBarNotification);

    private final class PrioritizeRowUpdateState {
        private int mAccumulatedChanges;
        private boolean mIsIdle;

        /* synthetic */ PrioritizeRowUpdateState(NotificationsViewAdapter this$0, PrioritizeRowUpdateState prioritizeRowUpdateState) {
            this();
        }

        private PrioritizeRowUpdateState() {
            this.mAccumulatedChanges = 0;
            this.mIsIdle = false;
        }

        private void scheduleUpdateTickIfNeeded() {
            NotificationsViewAdapter.this.mHandler.removeCallbacks(NotificationsViewAdapter.this.mStateTickRunnable);
            if (this.mAccumulatedChanges <= 0) {
                return;
            }
            NotificationsViewAdapter.this.mHandler.postDelayed(NotificationsViewAdapter.this.mStateTickRunnable, (300000 / this.mAccumulatedChanges) + 300000);
        }

        private void unScheduleUpdateTick() {
            NotificationsViewAdapter.this.mHandler.removeCallbacks(NotificationsViewAdapter.this.mStateTickRunnable);
        }

        private void postDeletesAndSubstitutes() {
            ArrayList<StatusBarNotification> masterList = NotificationsViewAdapter.this.mMasterList;
            ArrayList<StringDifference.EditItem> editItems = StringDifference.calculateStringAlignment(masterList, NotificationsViewAdapter.this.mSyncedList);
            StringDifference.ExtractDeleteAndUpdateResult extractDeleteAndUpdateResult = StringDifference.extractDeleteAndUpdateItems(editItems, NotificationsViewAdapter.this.mSyncedList);
            NotificationsViewAdapter.this.applyEditList(extractDeleteAndUpdateResult.mItems, NotificationsViewAdapter.this.mSyncedList);
            this.mAccumulatedChanges = extractDeleteAndUpdateResult.mRemainingEditItems;
            NotificationsViewAdapter.this.updateRowVisibility();
        }

        private void postOneUpdate() {
            ArrayList<StatusBarNotification> masterList = NotificationsViewAdapter.this.mMasterList;
            ArrayList<StringDifference.EditItem> editItems = StringDifference.calculateStringAlignment(masterList, NotificationsViewAdapter.this.mSyncedList);
            if (editItems.size() > 0) {
                ArrayList<StringDifference.EditItem> items = new ArrayList<>();
                items.add(editItems.get(0));
                NotificationsViewAdapter.this.applyEditList(items, NotificationsViewAdapter.this.mSyncedList);
                this.mAccumulatedChanges = editItems.size() - 1;
            } else {
                this.mAccumulatedChanges = 0;
            }
            NotificationsViewAdapter.this.updateRowVisibility();
        }

        public void updateTick() {
            postOneUpdate();
            if (this.mAccumulatedChanges <= 0) {
                return;
            }
            scheduleUpdateTickIfNeeded();
        }

        void onUiVisible() {
            NotificationsViewAdapter.this.postAllRowUpdates();
            this.mAccumulatedChanges = 0;
        }

        void onUiInVisible() {
            unScheduleUpdateTick();
        }

        void onNewRowChange() {
            if (NotificationsViewAdapter.this.mUiState == 1) {
                if (!this.mIsIdle) {
                    postDeletesAndSubstitutes();
                    scheduleUpdateTickIfNeeded();
                    return;
                } else {
                    NotificationsViewAdapter.this.postAllRowUpdates();
                    this.mAccumulatedChanges = 0;
                    return;
                }
            }
            this.mAccumulatedChanges++;
        }

        void onIdleStateChange(boolean isIdle) {
            this.mIsIdle = isIdle;
            if (!isIdle) {
                return;
            }
            NotificationsViewAdapter.this.postAllRowUpdates();
            this.mAccumulatedChanges = 0;
            unScheduleUpdateTick();
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    /* JADX WARN: Multi-variable type inference failed */
    NotificationsViewAdapter(Context context) {
        super(context);
        
        this.mUiState = 0;
        this.mHandler = new NotificationsHandler(this, null);
        this.mNotifToBeRemoved = new LinkedHashSet<>();
        this.mUpdateUiWhenVisible = false;
        this.mSyncedList = new ArrayList<>();
        this.mMasterList = new ArrayList<>();
        this.mRowUpdateStateTick = new Handler();
        this.mStateTickRunnable = new Runnable() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter.1
            @Override // java.lang.Runnable
            public void run() {
                NotificationsViewAdapter.this.mPrioritizeRowUpdateState.updateTick();
            }
        };
        if (isPartnerClient()) {
            return;
        }
        this.mPrioritizeRowUpdateState = new PrioritizeRowUpdateState();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public final int getItemCount() {
        return this.mSyncedList.size() + getNonNotifItemCount();
    }

    public void onIdleStateChange(boolean isIdle) {
        if (isPartnerClient()) {
            return;
        }
        this.mPrioritizeRowUpdateState.onIdleStateChange(isIdle);
    }

    public void onVisibilityChange(boolean isVisible) {
    }

    protected final StatusBarNotification getNotification(int position) {
        return this.mSyncedList.get(position - getNonNotifItemCount());
    }

    private final int indexOfMasterNotification(StatusBarNotification sbn) {
        for (int i = 0; i < this.mMasterList.size(); i++) {
            StatusBarNotification notif = this.mMasterList.get(i);
            if (NotificationUtils.equals(sbn, notif)) {
                return i;
            }
        }
        return -1;
    }

    protected void onUpdateRecommendationsList(ArrayList<StatusBarNotification> sbn) {
    }

    protected final void purgeDismissedNotifications() {
        for (StatusBarNotification sbn : this.mNotifToBeRemoved) {
            removeRecommendation(sbn);
            onNotificationCanceled(sbn);
        }
        this.mNotifToBeRemoved.clear();
    }

    protected final void dismissNotification(StatusBarNotification sbn) {
        this.mNotifToBeRemoved.add(sbn);
    }

    private class NotificationsHandler extends Handler {
        /* synthetic */ NotificationsHandler(NotificationsViewAdapter this$0, NotificationsHandler notificationsHandler) {
            this();
        }

        private NotificationsHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NotificationsViewAdapter.this.recommendationPosted((StatusBarNotification) Preconditions.checkNotNull((StatusBarNotification) msg.obj));
                    break;
                case 2:
                    NotificationsViewAdapter.this.removeRecommendation((StatusBarNotification) msg.obj);
                    break;
                case 3:
                    NotificationsViewAdapter.this.setPackageBlacklistCount(msg.arg1);
                    break;
                case 4:
                    ArrayList<StatusBarNotification> newList = (ArrayList) Preconditions.checkNotNull((ArrayList) msg.obj);
                    NotificationsViewAdapter.this.updateRecommendationsList(newList);
                    break;
            }
        }
    }

    protected final void msgRecommendationPosted(StatusBarNotification sbn) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, sbn));
    }

    protected final void msgRecommendationRemoved(StatusBarNotification sbn) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, sbn));
    }

    protected final void msgForcedRerank(ArrayList<StatusBarNotification> list) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, list));
    }

    protected final void msgOnPackageBlacklistChanged(int newCount) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, newCount, 0));
    }

    protected void onRecommendationRemoved(StatusBarNotification sbn) {
    }

    protected void onRecommendationPosted(StatusBarNotification sbn) {
    }

    protected void onRecommendationUpdated(StatusBarNotification old, StatusBarNotification sbn) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recommendationPosted(StatusBarNotification sbn) {
        int index = indexOfMasterNotification(sbn);
        if (index != -1) {
            StatusBarNotification old = this.mMasterList.remove(index);
            insertNotification(sbn, index);
            onRecommendationUpdated(old, sbn);
            if (Log.isLoggable("ViewAdapter", 3)) {
                int nonNotifItemCount = getNonNotifItemCount();
                Log.d("ViewAdapter", "Recommendation Updated position " + (index + nonNotifItemCount) + " : " + sbn);
            }
        } else {
            insertNotification(sbn, -1);
            onRecommendationPosted(sbn);
            if (Log.isLoggable("ViewAdapter", 3)) {
                Log.d("ViewAdapter", "Recommendation Posted : " + sbn);
            }
        }
        masterListHasChanged();
    }

    public final void setNotificationRowViewFlipper(NotificationViewFlipper flipper) {
        this.mViewFlipper = (NotificationViewFlipper) Preconditions.checkNotNull(flipper);
    }

    protected void setPackageBlacklistCount(int count) {
        if (this.mViewFlipper == null) {
            return;
        }
        this.mViewFlipper.setHasDisabledRecommendations(count > 0);
    }

    protected void updateRowVisibility() {
        if (this.mViewFlipper == null) {
            return;
        }
        this.mViewFlipper.setRowVisibility(getItemCount() > 0);
    }

    public final void onUiVisible() {
        if (Log.isLoggable("ViewAdapter", 3)) {
            Log.d("ViewAdapter", "onUiVisible()");
        }
        if (this.mUiState == 0 && this.mNotificationsTestHarness != null) {
            this.mNotificationsTestHarness.setSynthetic(this.mMasterList);
        }
        if (this.mUiState == 0) {
        }
        this.mUiState = 1;
        if (isPartnerClient()) {
            postAllRowUpdates();
        } else {
            this.mPrioritizeRowUpdateState.onUiVisible();
        }
    }

    public final void onUiInvisible() {
        if (Log.isLoggable("ViewAdapter", 3)) {
            Log.d("ViewAdapter", "onUiInvisible()");
        }
        this.mUiState = 2;
        purgeDismissedNotifications();
        if (isPartnerClient()) {
            return;
        }
        this.mPrioritizeRowUpdateState.onUiInVisible();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyEditList(ArrayList<StringDifference.EditItem> editList, ArrayList<StatusBarNotification> target) {
        int offset = getNonNotifItemCount();
        for (StringDifference.EditItem i : editList) {
            int n = i.mSrcIndex;
            switch (m155getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues()[i.mOp.ordinal()]) {
                case 1:
                    if (n >= target.size()) {
                        Log.d("CMD", "NotificationsViewAdapter::applyEditList  fail d");
                        return;
                    } else {
                        target.remove(n);
                        super.notifyItemRemoved(n + offset);
                    }
                    break;
                case 2:
                    if (n > target.size()) {
                        Log.d("CMD", "NotificationsViewAdapter::applyEditList  fail i");
                        return;
                    } else {
                        target.add(n, i.mItem);
                        super.notifyItemInserted(n + offset);
                    }
                    break;
                case 3:
                case 4:
                    if (n >= target.size()) {
                        Log.d("CMD", "NotificationsViewAdapter::applyEditList  fail su");
                        return;
                    } else {
                        target.set(n, i.mItem);
                        super.notifyItemChanged(n + offset);
                    }
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void postAllRowUpdates() {
        ArrayList<StringDifference.EditItem> editItems = StringDifference.calculateStringAlignment(this.mMasterList, this.mSyncedList);
        applyEditList(editItems, this.mSyncedList);
        updateRowVisibility();
    }

    private final void masterListHasChanged() {
        if (isPartnerClient()) {
            switch (this.mUiState) {
                case 0:
                    postAllRowUpdates();
                    break;
                case 1:
                    postAllRowUpdates();
                    break;
            }
        }
        switch (this.mUiState) {
            case 0:
                postAllRowUpdates();
                break;
            case 1:
            case 2:
                this.mPrioritizeRowUpdateState.onNewRowChange();
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeRecommendation(StatusBarNotification sbn) {
        int index = indexOfMasterNotification(sbn);
        if (index == -1) {
            return;
        }
        this.mMasterList.remove(index);
        onRecommendationRemoved(sbn);
        int nonNotifItemCount = getNonNotifItemCount();
        if (Log.isLoggable("ViewAdapter", 3)) {
            Log.d("ViewAdapter", "Recommendation Removed from postition" + (index + nonNotifItemCount) + ": " + sbn);
        }
        masterListHasChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateRecommendationsList(ArrayList<StatusBarNotification> newList) {
        if (newList != this.mMasterList) {
            this.mMasterList.clear();
            this.mMasterList.addAll(newList);
        }
        onUpdateRecommendationsList(newList);
        masterListHasChanged();
    }

    protected int getNonNotifItemCount() {
        return 0;
    }

    protected void notifyNonNotifItemChanged(int position) {
        super.notifyItemChanged(position);
    }

    protected void notifyNonNotifItemRemoved(int position) {
        super.notifyItemRemoved(position);
    }

    protected void notifyNonNotifItemInserted(int position) {
        super.notifyItemInserted(position);
    }
}
