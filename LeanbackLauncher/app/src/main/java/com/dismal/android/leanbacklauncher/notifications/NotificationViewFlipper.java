package com.dismal.android.leanbacklauncher.notifications;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.ConnectivityListener;

/* JADX INFO: loaded from: classes.dex */
public class NotificationViewFlipper extends ViewFlipper {
    private boolean mActivated;
    private Context mContextRef;
    private TextView mErrorMessageText;
    private final String mErrorNoConnection;
    private final String mErrorNoRecs;
    private final String mErrorRecsDisabled;
    private Handler mHandler;
    private boolean mHasDisabledRecs;
    private ChangeListener mListener;
    private int mPreparingDelay;
    private int mPreparingTimeout;
    private NotificationRowView mRow;
    private int mShowDelay;

    public interface ChangeListener {
        void onStateChanged(int i);
    }

    public NotificationViewFlipper(Context context) {
        this(context, null);
    }

    public NotificationViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        NotificationViewFlipper.this.mRow.setSelectedPosition(0);
                        NotificationViewFlipper.this.mHandler.removeCallbacksAndMessages(null);
                        NotificationViewFlipper.this.setDisplayedChild(0);
                        if (NotificationViewFlipper.this.mListener != null) {
                            NotificationViewFlipper.this.mListener.onStateChanged(2);
                        }
                        break;
                    case 1:
                        NotificationViewFlipper.this.setDisplayedChild(2);
                        NotificationViewFlipper.this.mHandler.sendEmptyMessageDelayed(2, NotificationViewFlipper.this.mPreparingTimeout);
                        if (NotificationViewFlipper.this.mListener != null) {
                            NotificationViewFlipper.this.mListener.onStateChanged(1);
                        }
                        break;
                    case 2:
                        NotificationViewFlipper.this.updateTimeoutMessage();
                        NotificationViewFlipper.this.setDisplayedChild(3);
                        break;
                }
            }
        };
        this.mActivated = false;
        this.mContextRef = context;
        Resources res = context.getResources();
        this.mShowDelay = res.getInteger(R.integer.notif_row_reveal_row_delay);
        this.mPreparingDelay = res.getInteger(R.integer.notif_row_preparing_screen_show_delay);
        this.mPreparingTimeout = res.getInteger(R.integer.notif_row_preparing_screen_timeout);
        this.mErrorRecsDisabled = res.getString(R.string.recommendation_row_empty_message_recs_disabled);
        this.mErrorNoRecs = res.getString(R.string.recommendation_row_empty_message_no_recs);
        this.mErrorNoConnection = res.getString(R.string.recommendation_row_empty_message_no_connection);
    }

    public void setListener(ChangeListener listener) {
        this.mListener = listener;
    }

    public boolean isRowViewVisible() {
        return getDisplayedChild() == 0;
    }

    public void setHasDisabledRecommendations(boolean hasDisabledRecs) {
        this.mHasDisabledRecs = hasDisabledRecs;
        refreshTimeoutScreen();
    }

    public void setRowVisibility(boolean visible) {
        this.mHandler.removeMessages(0);
        if (isRowViewVisible() == visible) {
            return;
        }
        boolean closeToBoot = SystemClock.elapsedRealtime() < 60000;
        if (visible) {
            if (1 == getDisplayedChild()) {
                if (!closeToBoot) {
                    this.mHandler.removeCallbacksAndMessages(null);
                    setDisplayedChild(0);
                    if (this.mListener == null) {
                        return;
                    }
                    this.mListener.onStateChanged(2);
                    return;
                }
                this.mHandler.sendEmptyMessageDelayed(0, this.mShowDelay);
                return;
            }
            if (3 == getDisplayedChild()) {
                setDisplayedChild(2);
            }
            this.mHandler.sendEmptyMessageDelayed(0, this.mShowDelay);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessageDelayed(2, this.mPreparingTimeout);
            return;
        }
        setDisplayedChild(1);
        this.mHandler.sendEmptyMessageDelayed(1, this.mPreparingDelay);
        if (this.mListener == null) {
            return;
        }
        this.mListener.onStateChanged(0);
    }

    public NotificationRowView getNotificationRow() {
        return this.mRow;
    }

    @Override // android.view.View
    public void setActivated(boolean activated) {
        if (this.mActivated == activated) {
            return;
        }
        this.mActivated = activated;
        if (this.mRow == null) {
            return;
        }
        this.mRow.setActivated(activated);
    }

    @Override // android.widget.ViewFlipper, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        this.mHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        View v = getChildAt(0);
        this.mErrorMessageText = (TextView) findViewById(R.id.text_error_message);
        if (v instanceof NotificationRowView) {
            this.mRow = (NotificationRowView) v;
        }
        setDisplayedChild(0);
        setRowVisibility(false);
    }

    @Override // android.widget.ViewAnimator
    public void setDisplayedChild(int whichChild) {
        if (getDisplayedChild() == whichChild) {
            return;
        }
        super.setDisplayedChild(whichChild);
    }

    public void refreshTimeoutScreen() {
        if (getDisplayedChild() != 3) {
            return;
        }
        updateTimeoutMessage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTimeoutMessage() {
        if (!ConnectivityListener.readConnectivity(this.mContextRef)) {
            this.mErrorMessageText.setText(this.mErrorNoConnection);
        } else if (this.mHasDisabledRecs) {
            this.mErrorMessageText.setText(this.mErrorRecsDisabled);
        } else {
            this.mErrorMessageText.setText(this.mErrorNoRecs);
        }
    }
}
