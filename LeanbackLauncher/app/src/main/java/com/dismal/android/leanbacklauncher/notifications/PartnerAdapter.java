package com.dismal.android.leanbacklauncher.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.AppsAdapter;
import com.dismal.android.leanbacklauncher.core.LaunchException;
import com.dismal.android.leanbacklauncher.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public class PartnerAdapter extends NotificationsServiceAdapter<PartnerAdapter.PartnerBannerViewHolder> {
    private final NotifComparator mComparator;
    private final BlacklistListener mListener;

    private class NotifComparator implements Comparator<StatusBarNotification> {
        /* synthetic */ NotifComparator(PartnerAdapter this$0, NotifComparator notifComparator) {
            this();
        }

        private NotifComparator() {
        }

        public int compare(double lhsSortKey, double rhsSortKey) {
            double sort = lhsSortKey - rhsSortKey;
            if (sort > 0.0d) {
                return 1;
            }
            return sort < 0.0d ? -1 : 0;
        }

        @Override // java.util.Comparator
        public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
            return compare(PartnerAdapter.this.getSortKey(lhs), PartnerAdapter.this.getSortKey(rhs));
        }
    }

    public PartnerAdapter(Context context, BlacklistListener listener) {
        super(context);
        this.mComparator = new NotifComparator(this, null);
        this.mListener = listener;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        return 2;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public PartnerBannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View newView = LayoutInflater.from(this.mContext).inflate(R.layout.app_banner, parent, false);
        return new PartnerBannerViewHolder(newView);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(PartnerBannerViewHolder appHolder, int position) {
        if (position >= getItemCount()) {
            return;
        }
        Notification notif = getNotification(position).getNotification();
        appHolder.init(notif.extras.getString("android.title"), new BitmapDrawable(this.mContext.getResources(), notif.largeIcon), notif.contentIntent, notif.color);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void insertNotification(StatusBarNotification sbn, int oldNotifPosition) {
        int position = this.mMasterList.size();
        double sortKey = getSortKey(sbn);
        int i = 0;
        while (true) {
            if (i >= this.mMasterList.size()) {
                break;
            }
            if (this.mComparator.compare(sortKey, getSortKey(this.mMasterList.get(i))) >= 0) {
                i++;
            } else {
                position = i;
                break;
            }
        }
        this.mMasterList.add(position, sbn);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void onRecommendationPosted(StatusBarNotification sbn) {
        String pkgName = sbn.getNotification().extras.getString("com.dismal.android.leanbacklauncher.replacespackage");
        if (TextUtils.isEmpty(pkgName) || this.mListener == null) {
            return;
        }
        this.mListener.onPackageBlacklisted(pkgName);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void onRecommendationUpdated(StatusBarNotification old, StatusBarNotification sbn) {
        String oldPkgName = old.getNotification().extras.getString("com.dismal.android.leanbacklauncher.replacespackage");
        String pkgName = sbn.getNotification().extras.getString("com.dismal.android.leanbacklauncher.replacespackage");
        if (TextUtils.equals(oldPkgName, pkgName) || this.mListener == null) {
            return;
        }
        if (!TextUtils.isEmpty(oldPkgName)) {
            this.mListener.onPackageUnblacklisted(oldPkgName);
        }
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        this.mListener.onPackageBlacklisted(pkgName);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void onRecommendationRemoved(StatusBarNotification sbn) {
        String pkgName = sbn.getNotification().extras.getString("com.dismal.android.leanbacklauncher.replacespackage");
        if (TextUtils.isEmpty(pkgName) || this.mListener == null) {
            return;
        }
        this.mListener.onPackageUnblacklisted(pkgName);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected void onUpdateRecommendationsList(ArrayList<StatusBarNotification> newList) {
        Collections.sort(this.mMasterList, this.mComparator);
        if (this.mListener == null) {
            return;
        }
        ArrayList<String> pkgNames = new ArrayList<>();
        for (int i = 0; i < this.mMasterList.size(); i++) {
            String pkgName = this.mMasterList.get(i).getNotification().extras.getString("com.dismal.android.leanbacklauncher.replacespackage");
            if (!TextUtils.isEmpty(pkgName)) {
                pkgNames.add(pkgName);
            }
        }
        this.mListener.onPackageBlacklistUpdated(pkgNames);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter, com.dismal.android.leanbacklauncher.notifications.NotificationsViewAdapter
    protected boolean isPartnerClient() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public double getSortKey(StatusBarNotification sbn) {
        try {
            double value = Double.valueOf(sbn.getNotification().getSortKey()).doubleValue();
            return value;
        } catch (NullPointerException e) {
            return -1.0d;
        } catch (NumberFormatException e2) {
            return -1.0d;
        }
    }

    static final class PartnerBannerViewHolder extends AppsAdapter.AppBannerViewHolder {
        private PendingIntent mIntent;

        public PartnerBannerViewHolder(View v) {
            super(v, null);
        }

        public void init(String title, Drawable banner, PendingIntent intent, int launchColor) {
            super.init(title, banner, launchColor);
            this.mIntent = intent;
        }

        @Override // com.dismal.android.leanbacklauncher.LauncherViewHolder
        protected void performLaunch() {
            if (this.mIntent != null) {
                try {
                    Util.startActivity(this.mCtx, this.mIntent);
                    return;
                } catch (Throwable t) {
                    throw new LaunchException("Could not launch partner intent", t);
                }
            }
            throw new LaunchException("No partner intent to launch: " + getPackageName());
        }
    }
}
