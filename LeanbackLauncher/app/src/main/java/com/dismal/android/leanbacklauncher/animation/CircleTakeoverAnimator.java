package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.ImageView;

/* JADX INFO: loaded from: classes.dex */
final class CircleTakeoverAnimator extends ForwardingAnimator<Animator> {
    private final ImageView mCircleLayerView;
    private boolean mFinished;

    public CircleTakeoverAnimator(View target, ImageView circleLayerView, int color) {
        super(getDelegate(target, circleLayerView, color));
        this.mCircleLayerView = circleLayerView;
        this.mDelegate.addListener(new CircleTakeoverAnimatorListener(this, null));
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, com.dismal.android.leanbacklauncher.animation.Resettable
    public void reset() {
        this.mCircleLayerView.setVisibility(4);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, android.animation.Animator
    public void pause() {
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, android.animation.Animator
    public void resume() {
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, android.animation.Animator
    public boolean isStarted() {
        if (this.mFinished) {
            return false;
        }
        return super.isStarted();
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, android.animation.Animator
    public void addPauseListener(Animator.AnimatorPauseListener listener) {
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, android.animation.Animator
    public void removePauseListener(Animator.AnimatorPauseListener listener) {
    }

    private static Animator getDelegate(View target, ImageView circleLayerView, int color) {
        Point screenSize = getScreenSize(circleLayerView);
        int displayWidth = screenSize.x;
        int displayHeight = screenSize.y;
        int[] pos = new int[2];
        target.getLocationInWindow(pos);
        float scale = target.getScaleX();
        int x = (int) (pos[0] + ((target.getMeasuredWidth() * scale) / 2.0f));
        int y = (int) (pos[1] + ((target.getMeasuredHeight() * scale) / 2.0f));
        int w = displayWidth - x;
        int h = displayHeight - y;
        int r = (int) Math.ceil(Math.sqrt((x * x) + (y * y)));
        int r2 = (int) Math.max((int) Math.max((int) Math.max(r, Math.ceil(Math.sqrt((w * w) + (y * y)))), Math.ceil(Math.sqrt((w * w) + (h * h)))), Math.ceil(Math.sqrt((x * x) + (h * h))));
        circleLayerView.setBackgroundColor((-16777216) | color);
        circleLayerView.setAlpha(1.0f);
        return ViewAnimationUtils.createCircularReveal(circleLayerView, x, y, 0.0f, r2);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator
    public String toString() {
        return "CircleTakeoverAnimator@" + Integer.toHexString(hashCode());
    }

    private static Point getScreenSize(View v) {
        WindowManager wm = (WindowManager) v.getContext().getSystemService("window");
        Display display = wm.getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        return screenSize;
    }

    private final class CircleTakeoverAnimatorListener extends AnimatorListenerAdapter {
        /* synthetic */ CircleTakeoverAnimatorListener(CircleTakeoverAnimator this$0, CircleTakeoverAnimatorListener circleTakeoverAnimatorListener) {
            this();
        }

        private CircleTakeoverAnimatorListener() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            CircleTakeoverAnimator.this.mCircleLayerView.setVisibility(0);
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            CircleTakeoverAnimator.this.reset();
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            animator.removeListener(this);
            CircleTakeoverAnimator.this.mFinished = true;
        }
    }
}
