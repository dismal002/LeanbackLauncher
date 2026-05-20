package com.dismal.android.leanbacklauncher.notifications;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.dismal.android.leanbacklauncher.R;

/* JADX INFO: loaded from: classes.dex */
public class NowPlayingCardView extends NotificationCardView {
    private boolean animationStarted;
    private Handler mHandler;
    private ImageView mImage1;
    private ImageView mImage2;
    private ImageView mImage3;
    private NowPlayingCardData mMediaData;
    private ImageView mPauseImage;
    private long mPlayerPosMs;
    private int mPlayerState;
    private AnimatorSet mStartFadeAnimation;
    private AnimatorSet mStopFadeAnimation;
    private AnimatorSet mThreeBarAnimator;
    private long mTimeUpdateMs;
    private final int mUpdateInterval;

    static class DropListener implements Animator.AnimatorListener {
        private View mView;

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            setDropScale(this.mView);
            animation.removeListener(this);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }

        static void setDropScale(View view) {
            view.setScaleY(0.083333336f);
        }
    }

    protected Drawable getBarDrawable(Resources resources) {
        return resources.getDrawable(R.drawable.leanback_card_now_playing_bar);
    }

    private void setUpThreeBarAnimators() {
        Context context = getContext();
        this.mThreeBarAnimator = new AnimatorSet();
        Drawable barDrawable = getBarDrawable(context.getResources());
        int pivotY = barDrawable.getIntrinsicHeight();
        this.mImage1 = (ImageView) findViewById(R.id.bar1);
        this.mImage1.setPivotY(pivotY);
        DropListener.setDropScale(this.mImage1);
        this.mImage2 = (ImageView) findViewById(R.id.bar2);
        this.mImage2.setPivotY(pivotY);
        DropListener.setDropScale(this.mImage2);
        this.mImage3 = (ImageView) findViewById(R.id.bar3);
        this.mImage3.setPivotY(pivotY);
        DropListener.setDropScale(this.mImage3);
        ObjectAnimator bar1Animator = ObjectAnimator.ofFloat(this.mImage1, "scaleY", 0.41666666f, 0.25f, 0.41666666f, 0.5833333f, 0.75f, 0.8333333f, 0.9166667f, 1.0f, 0.9166667f, 1.0f, 0.8333333f, 0.6666667f, 0.5f, 0.33333334f, 0.16666667f, 0.33333334f, 0.5f, 0.5833333f, 0.75f, 0.9166667f, 0.75f, 0.5833333f, 0.41666666f, 0.25f, 0.41666666f, 0.6666667f, 0.41666666f, 0.25f, 0.33333334f, 0.41666666f);
        bar1Animator.setRepeatCount(-1);
        bar1Animator.setDuration(2320L);
        bar1Animator.setInterpolator(new LinearInterpolator());
        ObjectAnimator bar2Animator = ObjectAnimator.ofFloat(this.mImage2, "scaleY", 1.0f, 0.9166667f, 0.8333333f, 0.9166667f, 1.0f, 0.9166667f, 0.75f, 0.5833333f, 0.75f, 0.9166667f, 1.0f, 0.8333333f, 0.6666667f, 0.8333333f, 1.0f, 0.9166667f, 0.75f, 0.41666666f, 0.25f, 0.41666666f, 0.6666667f, 0.8333333f, 1.0f, 0.8333333f, 0.75f, 0.6666667f, 1.0f);
        bar2Animator.setRepeatCount(-1);
        bar2Animator.setDuration(2080L);
        bar2Animator.setInterpolator(new LinearInterpolator());
        ObjectAnimator bar3Animator = ObjectAnimator.ofFloat(this.mImage3, "scaleY", 0.6666667f, 0.75f, 0.8333333f, 1.0f, 0.9166667f, 0.75f, 0.5833333f, 0.41666666f, 0.5833333f, 0.6666667f, 0.75f, 1.0f, 0.9166667f, 1.0f, 0.75f, 0.5833333f, 0.75f, 0.9166667f, 1.0f, 0.8333333f, 0.6666667f, 0.75f, 0.5833333f, 0.41666666f, 0.25f, 0.6666667f);
        bar3Animator.setRepeatCount(-1);
        bar3Animator.setDuration(2000L);
        bar3Animator.setInterpolator(new LinearInterpolator());
        this.mThreeBarAnimator.playTogether(bar1Animator, bar2Animator, bar3Animator);
        findViewById(R.id.scrim).setVisibility(0);
    }

    private void setUpFadeAnimators() {
        Context context = getContext();
        LinearLayout threeBarLayout = (LinearLayout) findViewById(R.id.three_bars);
        this.mStopFadeAnimation = new AnimatorSet();
        this.mStartFadeAnimation = new AnimatorSet();
        Animator threeBarFadeInAnimator = AnimatorInflater.loadAnimator(context, R.anim.now_playing_bars_fade_in);
        Animator threeBarFadeOutAnimator = AnimatorInflater.loadAnimator(context, R.anim.now_playing_bars_fade_out);
        Animator pauseFadeInAnim = AnimatorInflater.loadAnimator(context, R.anim.now_playing_pause_fade_in);
        Animator pauseFadeOutAnim = AnimatorInflater.loadAnimator(context, R.anim.now_playing_pause_fade_out);
        this.mPauseImage = (ImageView) findViewById(R.id.pause_icon);
        this.mPauseImage.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_now_playing_paused));
        pauseFadeInAnim.setTarget(this.mPauseImage);
        pauseFadeInAnim.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                NowPlayingCardView.this.mPauseImage.setVisibility(0);
            }
        });
        pauseFadeOutAnim.setTarget(this.mPauseImage);
        pauseFadeOutAnim.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardView.3
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                NowPlayingCardView.this.mPauseImage.setVisibility(0);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                NowPlayingCardView.this.mPauseImage.setVisibility(8);
            }
        });
        threeBarFadeInAnimator.setTarget(threeBarLayout);
        threeBarFadeInAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardView.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                NowPlayingCardView.this.mThreeBarAnimator.start();
                NowPlayingCardView.this.setBarImageVisibility(0);
            }
        });
        threeBarFadeOutAnimator.setTarget(threeBarLayout);
        threeBarFadeOutAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardView.5
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                NowPlayingCardView.this.setBarImageVisibility(0);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                NowPlayingCardView.this.setBarImageVisibility(8);
                NowPlayingCardView.this.mThreeBarAnimator.end();
            }
        });
        this.mStartFadeAnimation.playSequentially(pauseFadeOutAnim, threeBarFadeInAnimator);
        this.mStopFadeAnimation.playSequentially(threeBarFadeOutAnimator, pauseFadeInAnim);
    }

    private void startAnimation() {
        if (this.animationStarted) {
            return;
        }
        this.mStartFadeAnimation.start();
        this.animationStarted = true;
    }

    private void stopAnimation() {
        if (!this.animationStarted) {
            return;
        }
        this.mStopFadeAnimation.start();
        this.animationStarted = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBarImageVisibility(int visibility) {
        this.mImage1.setVisibility(visibility);
        this.mImage2.setVisibility(visibility);
        this.mImage3.setVisibility(visibility);
    }

    public NowPlayingCardView(Context context) {
        this(context, null);
    }

    public NowPlayingCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.animationStarted = false;
        this.mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardView.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        NowPlayingCardView.this.updatePlayProgress();
                        NowPlayingCardView.this.mHandler.sendEmptyMessageDelayed(1, NowPlayingCardView.this.mUpdateInterval);
                        break;
                }
            }
        };
        this.mUpdateInterval = getResources().getInteger(R.integer.now_playing_card_update_interval_ms);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationCardView
    public void setNotificationContent(StatusBarNotification sbn, boolean updateImage) {
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationCardView, android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mPlayerState != 3) {
            return;
        }
        this.mHandler.removeMessages(1);
        updatePlayProgress();
        this.mHandler.sendEmptyMessageDelayed(1, this.mUpdateInterval);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationCardView, androidx.leanback.widget.BaseCardView, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSelfUpdate();
    }

    public void setPlayerState(int state, long currentPosMs, long stateChangeTimeMs) {
        this.mPlayerState = state;
        this.mPlayerPosMs = currentPosMs;
        this.mTimeUpdateMs = stateChangeTimeMs;
        this.mHandler.removeMessages(1);
        if (this.mPlayerState == 9) {
            setProgressShown(false);
            return;
        }
        if (this.mPlayerState == 1) {
            stopAnimation();
            setSourceName(getResources().getString(R.string.now_playing_card_stopped));
            setContentText(getResources().getString(R.string.now_playing_card_stopped));
        } else if (this.mPlayerState == 2) {
            stopAnimation();
            setSourceName(getResources().getString(R.string.now_playing_card_paused));
            setContentText(getResources().getString(R.string.now_playing_card_paused));
        } else {
            startAnimation();
            setSourceName(getResources().getString(R.string.now_playing_card_playing));
            setContentText(getResources().getString(R.string.now_playing_card_playing));
        }
        updatePlayProgress();
        if (this.mPlayerState != 3 || !isAttachedToWindow()) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(1, this.mUpdateInterval);
    }

    public void setNowPlayingContent(NowPlayingCardData mediaData) {
        Drawable image;
        this.mMediaData = mediaData;
        if (mediaData != null) {
            if (mediaData.artwork != null) {
                image = new BitmapDrawable(getContext().getResources(), mediaData.artwork);
            } else {
                image = getContext().getResources().getDrawable(R.drawable.ic_now_playing_default);
            }
            if (image != null) {
                setDimensions(image.getIntrinsicWidth(), image.getIntrinsicHeight());
            }
            setMainImage(image);
            setWallpaperUri(null);
            setTitleText(mediaData.title);
            if (this.mPlayerState == 1) {
                setSourceName(getResources().getString(R.string.now_playing_card_stopped));
                setContentText(getResources().getString(R.string.now_playing_card_stopped));
            } else if (this.mPlayerState == 2) {
                setSourceName(getResources().getString(R.string.now_playing_card_paused));
                setContentText(getResources().getString(R.string.now_playing_card_paused));
            } else {
                setSourceName(getResources().getString(R.string.now_playing_card_playing));
                setContentText(getResources().getString(R.string.now_playing_card_playing));
            }
            this.mColor = mediaData.launchColor;
            if (this.mColor != 0) {
                this.mInfoBackground.setColor(this.mColor);
            } else {
                this.mInfoBackground.setColor(getResources().getColor(R.color.notif_background_color));
            }
            Drawable badgeIcon = null;
            if (mediaData.playerPackage != null) {
                try {
                    badgeIcon = getContext().getPackageManager().getApplicationIcon(mediaData.playerPackage);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("NowPlayingCardView", "Couldn't get remote control client package icon", e);
                }
                setClickedIntent(this.mMediaData.pIntent);
            }
            setBadgeImage(badgeIcon);
            return;
        }
        setMainImage(null);
        setTitleText("");
        setContentText("");
        setSourceName("");
        setBadgeImage(null);
        setWallpaperUri(null);
        setProgressShown(false);
        setClickedIntent(null);
    }

    public void stopSelfUpdate() {
        this.mHandler.removeCallbacksAndMessages(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayProgress() {
        if (this.mMediaData != null && this.mPlayerPosMs >= 0 && this.mMediaData.duration > 0 && this.mPlayerPosMs <= this.mMediaData.duration) {
            long current = this.mPlayerPosMs;
            if (this.mPlayerState == 3) {
                current += SystemClock.elapsedRealtime() - this.mTimeUpdateMs;
            }
            float progress = Math.min(1.0f, current / this.mMediaData.duration);
            setProgressShown(true);
            setProgress(100, (int) (100.0f * progress));
            return;
        }
        setProgressShown(false);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationCardView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpThreeBarAnimators();
        setUpFadeAnimators();
        this.mDimmer.addDimTarget((ImageView) findViewById(R.id.bar1));
        this.mDimmer.addDimTarget((ImageView) findViewById(R.id.bar2));
        this.mDimmer.addDimTarget((ImageView) findViewById(R.id.bar3));
        this.mDimmer.addDimTarget((ImageView) findViewById(R.id.pause_icon));
    }
}
