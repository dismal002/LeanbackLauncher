package com.dismal.android.leanbacklauncher.notifications;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.LauncherViewHolder;
import com.dismal.android.leanbacklauncher.MainActivity;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.core.LaunchException;
import com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import com.dismal.android.leanbacklauncher.util.Util;
import java.util.ArrayList;
import java.util.LinkedList;

/* JADX INFO: loaded from: classes.dex */
public class NotificationsAdapter extends NotificationsServiceAdapter<NotificationsAdapter.NotifViewHolder> implements MainActivity.IdleListener, Ranker.RankingListener {
    private final CardUpdateController mCardUpdateController;
    private boolean mHasNowPlayingCard;
    private final int mImpressionDelay;
    private Handler mImpressionHandler;
    private boolean mIsIdle;
    private RemoteControlListener mMediaListener;
    private int mNotifCount;
    private NotificationCountListener mNotifCountListener;
    private NowPlayingCardData mNowPlayingData;
    private long mNowPlayingPosMs;
    private long mNowPlayingPosUpdateMs;
    private int mNowPlayingState;
    private final Ranker mRanker;
    private final RecommendationsHander mRecommendationsHandler;

    public interface NotificationCountListener {
        void onNotificationCountUpdated(int i);
    }

    private static class CardUpdateController {
        private final LinkedList<NotifViewHolder> mWaitingConnectionNofification = new LinkedList<>();
        private boolean mIsConnected = false;

        public void onDisconnected() {
            this.mIsConnected = false;
        }

        public void onServiceStatusCahanged(boolean isReady) {
            synchronized (this) {
                this.mIsConnected = isReady;
                if (isReady) {
                    for (NotifViewHolder notifViewHolder : this.mWaitingConnectionNofification) {
                        if (notifViewHolder.mQueuedState == 1) {
                            notifViewHolder.executeImageTask();
                            notifViewHolder.mQueuedState = 0;
                        } else {
                            notifViewHolder.mQueuedState = 3;
                        }
                    }
                    this.mWaitingConnectionNofification.clear();
                }
            }
        }

        public boolean queueImageFetchIfDisconnected(NotifViewHolder notifViewHolder) {
            synchronized (this) {
                if (this.mIsConnected) {
                    return false;
                }
                if (notifViewHolder.mQueuedState == 0) {
                    this.mWaitingConnectionNofification.add(notifViewHolder);
                    notifViewHolder.mQueuedState = 1;
                }
                return true;
            }
        }

        public void onViewAttachedToWindow(NotifViewHolder notifViewHolder) {
            synchronized (this) {
                if (!this.mIsConnected) {
                    if (notifViewHolder.mQueuedState == 2) {
                        notifViewHolder.mQueuedState = 1;
                    }
                } else if (notifViewHolder.mQueuedState == 3) {
                    notifViewHolder.executeImageTask();
                    notifViewHolder.mQueuedState = 0;
                }
            }
        }

        public void onViewDetachedFromWindow(NotifViewHolder notifViewHolder) {
            synchronized (this) {
                if (!this.mIsConnected && notifViewHolder.mQueuedState == 1) {
                    notifViewHolder.mQueuedState = 2;
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public NotificationsAdapter(Context context, Ranker ranker) {
        super(context);
        this.mRecommendationsHandler = new RecommendationsHander(this, null);
        this.mCardUpdateController = new CardUpdateController();
        this.mImpressionHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what != 11 || NotificationsAdapter.this.mIsIdle || !(msg.obj instanceof NotifViewHolder)) {
                    return;
                }
                PendingIntent intent = ((NotifViewHolder) msg.obj).getPendingIntent();
                String group = ((NotifViewHolder) msg.obj).getGroup();
                NotificationsAdapter.this.mRanker.onAction(intent, group, 4);
            }
        };
        this.mNotificationsListener = new RecommendationsListener();
        this.mRanker = ranker;
        this.mMediaListener = new RemoteControlListener();
        this.mImpressionDelay = context.getResources().getInteger(R.integer.impression_delay);
    }

    private class RecommendationsHander extends Handler {
        /* synthetic */ RecommendationsHander(NotificationsAdapter this$0, RecommendationsHander recommendationsHander) {
            this();
        }

        private RecommendationsHander() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 6:
                    NotificationsAdapter.this.remoteControllerClientChanged(msg.arg1 != 0);
                    break;
                case 7:
                    if (msg.obj != null) {
                        NotificationsAdapter.this.remoteControllerMediaDataUpdated((NowPlayingCardData) msg.obj);
                    }
                    break;
                case 8:
                    if (msg.obj != null) {
                        long[] values = (long[]) msg.obj;
                        NotificationsAdapter.this.remoteControllerClientPlaybackStateUpdate((int) values[0], values[1], values[2]);
                    }
                    break;
                case 9:
                    NotificationsAdapter.this.updateNowPlayingCard();
                    break;
                case 10:
                    if (NotificationsAdapter.this.mHasNowPlayingCard) {
                        NotificationsAdapter.this.mHasNowPlayingCard = false;
                        NotificationsAdapter.this.notifyNonNotifItemRemoved(0);
                    }
                    break;
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter, com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected boolean isPartnerClient() {
        return false;
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    protected void onConnected(ComponentName className, IBinder service) {
        try {
            if (this.mHasNowPlayingCard) {
                this.mRecommendationsHandler.sendEmptyMessageDelayed(10, 1500L);
            }
            super.onConnected(className, service);
            this.mBoundService.setRemoteControlListener(this.mMediaListener);
        } catch (RemoteException e) {
        }
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    protected void serviceStatusChanged(boolean isReady) {
        super.serviceStatusChanged(isReady);
        this.mCardUpdateController.onServiceStatusCahanged(isReady);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    protected void onDisconnected(ComponentName className) {
        this.mCardUpdateController.onDisconnected();
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    public void onInitUi() {
        Log.d("UICYCLE", "NotificationsAdapter.onInitUi");
        super.onInitUi();
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    public void onStopUi() {
        Log.d("UICYCLE", "NotificationsAdapter.onStopUi: " + this.mBound);
        this.mCardUpdateController.onDisconnected();
        if (this.mBound) {
            try {
                super.onStopUi();
                this.mBoundService.setRemoteControlListener(null);
            } catch (RemoteException e) {
            }
        }
        if (!this.mHasNowPlayingCard) {
            return;
        }
        notifyNonNotifItemChanged(0);
    }

    public void setNotificationCountListener(NotificationCountListener listener) {
        this.mNotifCountListener = listener;
        if (this.mNotifCountListener == null) {
            return;
        }
        this.mNotifCountListener.onNotificationCountUpdated(this.mNotifCount);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        return (this.mHasNowPlayingCard && position == 0) ? 1 : 0;
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected int getNonNotifItemCount() {
        if (!this.mHasNowPlayingCard) {
            return 0;
        }
        return 1;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public NotifViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        switch (viewType) {
            case 0:
                View newView = inflater.inflate(R.layout.notification_card, parent, false);
                return new NotifViewHolder(newView);
            case 1:
                View newView2 = inflater.inflate(R.layout.now_playing_card, parent, false);
                return new NowPlayingViewHolder(newView2);
            default:
                Log.e("RecommendationsAdapter", "Invalid view type = " + viewType);
                return null;
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(NotifViewHolder holder, int position) {
        if (position >= getItemCount()) {
        }
        int type = getItemViewType(position);
        switch (type) {
            case 0:
                holder.init(getNotification(position));
                break;
            case 1:
                if (holder instanceof NowPlayingViewHolder) {
                    ((NowPlayingViewHolder) holder).init(this.mNowPlayingData);
                }
                break;
            default:
                Log.e("RecommendationsAdapter", "Invalid view type = " + type);
                break;
        }
    }

    @Override // com.dismal.android.leanbacklauncher.widget.RowViewAdapter, androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewAttachedToWindow(NotifViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Message msg = new Message();
        msg.what = 11;
        msg.obj = holder;
        this.mImpressionHandler.sendMessageDelayed(msg, this.mImpressionDelay);
        this.mCardUpdateController.onViewAttachedToWindow(holder);
    }

    @Override // com.dismal.android.leanbacklauncher.widget.RowViewAdapter, androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewDetachedFromWindow(NotifViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        this.mImpressionHandler.removeMessages(11, holder);
        this.mCardUpdateController.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter
    protected void onNotificationClick(PendingIntent intent, String group) {
        super.onNotificationClick(intent, group);
        this.mRanker.onAction(intent, group, 2);
    }

    private class NowPlayingViewHolder extends NotifViewHolder {
        NowPlayingCardView mNowPlayingCard;

        public NowPlayingViewHolder(View v) {
            super(v);
            this.mNowPlayingCard = (NowPlayingCardView) v;
        }

        public void init(NowPlayingCardData mediaData) {
            this.itemView.setVisibility(0);
            if (this.mNowPlayingCard == null) {
                return;
            }
            this.mNowPlayingCard.setNowPlayingContent(mediaData);
            if (NotificationsAdapter.this.mBound) {
                this.mNowPlayingCard.setPlayerState(NotificationsAdapter.this.mNowPlayingState, NotificationsAdapter.this.mNowPlayingPosMs, NotificationsAdapter.this.mNowPlayingPosUpdateMs);
            } else {
                this.mNowPlayingCard.stopSelfUpdate();
            }
            setLaunchColor(this.mNowPlayingCard.getColor());
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter.NotifViewHolder
        public PendingIntent getPendingIntent() {
            return this.mNowPlayingCard.getClickedIntent();
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter.NotifViewHolder
        protected void onNotificationClick(PendingIntent intent) {
        }
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Ranker.RankingListener
    public void onRankerReady() {
        msgForcedRerank(this.mMasterList);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void insertNotification(StatusBarNotification sbn, int oldNotifPosition) {
        this.mRanker.insertNotification(sbn, this.mMasterList, oldNotifPosition, this);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void onUpdateRecommendationsList(ArrayList<StatusBarNotification> newList) {
        this.mRanker.rankNotifications(this.mMasterList, this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNowPlayingCard() {
        if (!this.mHasNowPlayingCard) {
            this.mHasNowPlayingCard = true;
            notifyNonNotifItemInserted(0);
        } else {
            notifyNonNotifItemChanged(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void remoteControllerClientChanged(boolean clearing) {
        if (Log.isLoggable("RecommendationsAdapter", 3)) {
            Log.d("RecommendationsAdapter", "remoteControllerClientChanged. Clearing= " + clearing);
        }
        this.mRecommendationsHandler.removeMessages(10);
        this.mRecommendationsHandler.removeMessages(9);
        if (!this.mHasNowPlayingCard || !clearing) {
            return;
        }
        this.mHasNowPlayingCard = false;
        notifyNonNotifItemRemoved(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void remoteControllerMediaDataUpdated(NowPlayingCardData mediaData) {
        if (Log.isLoggable("RecommendationsAdapter", 3)) {
            Log.d("RecommendationsAdapter", "remoteControllerMediaDataUpdated. mediaData= " + mediaData);
        }
        this.mNowPlayingData = mediaData;
        this.mRecommendationsHandler.removeMessages(9);
        this.mRecommendationsHandler.sendEmptyMessageDelayed(9, 300L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void remoteControllerClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs) {
        if (Log.isLoggable("RecommendationsAdapter", 3)) {
            Log.d("RecommendationsAdapter", "remoteControllerClientPlaybackStateUpdate. state= " + state);
        }
        this.mNowPlayingState = state;
        this.mNowPlayingPosMs = currentPosMs;
        this.mNowPlayingPosUpdateMs = stateChangeTimeMs;
        if (!this.mHasNowPlayingCard) {
            return;
        }
        notifyNonNotifItemChanged(0);
    }

    private class RemoteControlListener extends IRemoteControlListener.Stub {
        /* synthetic */ RemoteControlListener(NotificationsAdapter this$0, RemoteControlListener remoteControlListener) {
            this();
        }

        private RemoteControlListener() {
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
        public void onClientChanged(boolean clearing) {
            NotificationsAdapter.this.mRecommendationsHandler.sendMessage(NotificationsAdapter.this.mRecommendationsHandler.obtainMessage(6, clearing ? 1 : 0, 0));
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
        public void onMediaDataUpdated(NowPlayingCardData mediaData) {
            NotificationsAdapter.this.mRecommendationsHandler.sendMessage(NotificationsAdapter.this.mRecommendationsHandler.obtainMessage(7, mediaData));
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.IRemoteControlListener
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs) {
            long[] stateValues = {state, stateChangeTimeMs, currentPosMs};
            NotificationsAdapter.this.mRecommendationsHandler.sendMessage(NotificationsAdapter.this.mRecommendationsHandler.obtainMessage(8, stateValues));
        }
    }

    protected class RecommendationsListener extends NotificationsServiceAdapter<NotificationsAdapter.NotifViewHolder>.NotificationsListener {
        protected RecommendationsListener() {
            super();
        }

        @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter.NotificationsListener, com.dismal.android.leanbacklauncher.notifications.INotificationsMonitorClient
        public void onNotificationCountChanged(int count) {
            NotificationsAdapter.this.mNotifCount = count;
            if (Log.isLoggable("RecommendationsAdapter", 3)) {
                Log.d("RecommendationsAdapter", "Notification Count updated = " + count);
            }
            if (NotificationsAdapter.this.mNotifCountListener == null) {
                return;
            }
            NotificationsAdapter.this.mNotifCountListener.onNotificationCountUpdated(NotificationsAdapter.this.mNotifCount);
        }
    }

    class NotifViewHolder extends LauncherViewHolder {
        FetchImageTask mImageTask;
        NotificationCardView mNotificationCard;
        int mQueuedState;
        StatusBarNotification mSbn;

        public NotifViewHolder(View v) {
            super(v);
            this.mQueuedState = 0;
            if (!(v instanceof NotificationCardView)) {
                return;
            }
            this.mNotificationCard = (NotificationCardView) v;
        }

        void init(StatusBarNotification sbn) {
            this.itemView.setVisibility(0);
            if (this.mNotificationCard == null) {
                return;
            }
            boolean refreshSameContent = NotificationUtils.equals(sbn, this.mSbn);
            this.mNotificationCard.setNotificationContent(sbn, !refreshSameContent);
            this.mNotificationCard.resetCardState();
            this.mSbn = sbn;
            setLaunchColor(this.mNotificationCard.getColor());
            this.mQueuedState = 0;
            if (NotificationsAdapter.this.mCardUpdateController.queueImageFetchIfDisconnected(this)) {
                return;
            }
            executeImageTask();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void executeImageTask() {
            FetchImageTask fetchImageTask = null;
            if (this.mImageTask != null && !this.mImageTask.isCancelled()) {
                this.mImageTask.cancel(true);
            }
            this.mImageTask = new FetchImageTask(this, fetchImageTask);
            this.mImageTask.execute(this.mSbn.getKey());
        }

        public PendingIntent getPendingIntent() {
            return this.mNotificationCard.getClickedIntent();
        }

        public String getGroup() {
            return this.mNotificationCard.getRecommendationGroup();
        }

        @Override // com.dismal.android.leanbacklauncher.LauncherViewHolder
        protected void performLaunch() {
            PendingIntent intent = getPendingIntent();
            if (intent != null) {
                try {
                    Util.startActivity(NotificationsAdapter.this.mContext, intent);
                    onLaunchSucceeded();
                    onNotificationClick(intent);
                    return;
                } catch (Throwable t) {
                    throw new LaunchException("Could not launch notification intent", t);
                }
            }
            throw new LaunchException("No notification intent to launch: " + this.mSbn);
        }

        protected void onNotificationClick(PendingIntent intent) {
            NotificationsAdapter.this.onNotificationClick(intent, this.mNotificationCard.getRecommendationGroup());
            if (!this.mNotificationCard.isAutoDismiss()) {
                return;
            }
            NotificationsAdapter.this.dismissNotification(this.mSbn);
        }

        private class FetchImageTask extends AsyncTask<String, Void, Bitmap> {
            /* synthetic */ FetchImageTask(NotifViewHolder this$1, FetchImageTask fetchImageTask) {
                this();
            }

            private FetchImageTask() {
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public Bitmap doInBackground(String... params) {
                String notifKey = params[0];
                if (!NotificationsAdapter.this.mCardUpdateController.queueImageFetchIfDisconnected(NotifViewHolder.this)) {
                    try {
                        Bitmap img = NotificationsAdapter.this.mBoundService.getImageForNotification(notifKey);
                        if (img == null) {
                            NotificationsAdapter.this.mCardUpdateController.queueImageFetchIfDisconnected(NotifViewHolder.this);
                        }
                        return img;
                    } catch (RemoteException e) {
                        Log.e("RecommendationsAdapter", "Exception while fetching card image: " + e.toString());
                    }
                }
                return null;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public void onPostExecute(Bitmap image) {
                if (isCancelled()) {
                    return;
                }
                if (image != null) {
                    NotifViewHolder.this.mNotificationCard.setMainImage(new BitmapDrawable(NotificationsAdapter.this.mContext.getResources(), image));
                }
                NotifViewHolder.this.mImageTask = null;
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter, com.dismal.android.leanbacklauncher.MainActivity.IdleListener
    public void onIdleStateChange(boolean isIdle) {
        this.mIsIdle = isIdle;
        super.onIdleStateChange(isIdle);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter, com.dismal.android.leanbacklauncher.MainActivity.IdleListener
    public void onVisibilityChange(boolean isVisible) {
        this.mIsIdle = !isVisible;
        super.onVisibilityChange(isVisible);
    }
}
