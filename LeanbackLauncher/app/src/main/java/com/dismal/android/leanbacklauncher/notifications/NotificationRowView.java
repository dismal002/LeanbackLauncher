package com.dismal.android.leanbacklauncher.notifications;

import android.content.Context;
import androidx.leanback.widget.OnChildSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.ActiveItemsRowView;

/* JADX INFO: loaded from: classes.dex */
public class NotificationRowView extends ActiveItemsRowView {
    private boolean mIgnoreActivateForBckChange;
    private String mLastReportedBackground;
    private NotificationRowListener mNotifListener;

    public interface NotificationRowListener {
        void onBackgroundImageChanged(String str, boolean z);
    }

    public NotificationRowView(Context context) {
        this(context, null, 0);
    }

    public NotificationRowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLastReportedBackground = null;
        this.mIgnoreActivateForBckChange = false;
        setOnChildSelectedListener(new OnChildSelectedListener() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationRowView.1
            @Override // androidx.leanback.widget.OnChildSelectedListener
            public void onChildSelected(ViewGroup parent, View child, int position, long id) {
                NotificationRowView.super.onChildSelected(parent, child, position, id);
                NotificationRowView.this.updateLauncherBackground(0);
            }
        });
    }

    public void setListener(NotificationRowListener listener) {
        this.mNotifListener = listener;
    }

    public void refreshSelectedBackground() {
        updateLauncherBackground(1);
    }

    public void setIgnoreNextActivateBackgroundChange() {
        if (this.mActiveRow) {
            return;
        }
        this.mIgnoreActivateForBckChange = true;
    }

    @Override // com.dismal.android.leanbacklauncher.ActiveItemsRowView, android.view.View
    public void setActivated(boolean activated) {
        if (this.mActiveRow != activated) {
            if (!this.mIgnoreActivateForBckChange) {
                updateLauncherBackground(0);
            } else {
                updateLauncherBackground(2);
                this.mIgnoreActivateForBckChange = false;
            }
        }
        super.setActivated(activated);
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.ViewParent
    public void requestChildFocus(View child, View focused) {
        if (!this.mActiveRow) {
            ((NotificationCardView) child).setSelectedAnimationDelayed(false);
        }
        super.requestChildFocus(child, focused);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLauncherBackground(int type) {
        RecyclerView.ViewHolder holder;
        View child;
        NotificationCardView card;
        String backgroundUri = null;
        if (this.mNotifListener != null && ((this.mActiveRow || type == 1) && getChildCount() > 0 && (holder = findViewHolderForLayoutPosition(getSelectedPosition())) != null && (child = holder.itemView) != null && (card = (NotificationCardView) child) != null)) {
            backgroundUri = card.getWallpaperUri();
        }
        if (type == 2) {
            this.mLastReportedBackground = backgroundUri;
            return;
        }
        if (type == 1 || this.mLastReportedBackground != backgroundUri) {
            this.mLastReportedBackground = backgroundUri;
            this.mNotifListener.onBackgroundImageChanged(this.mLastReportedBackground, this.mActiveRow);
        } else {
            if (this.mLastReportedBackground != null || backgroundUri != null) {
                return;
            }
            this.mNotifListener.onBackgroundImageChanged(this.mLastReportedBackground, this.mActiveRow);
        }
    }
}
