package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import com.dismal.android.leanbacklauncher.R;

/* JADX INFO: loaded from: classes.dex */
public class ViewFocusAnimator implements View.OnFocusChangeListener {
    private final int mAnimDuration;
    ObjectAnimator mFocusAnimation;
    private final Interpolator mFocusInterpolator;
    private float mFocusProgress;
    private final float mSelectedScaleDelta;
    private final float mSelectedZDelta;
    private View mTargetView;
    private final float mUnselectedScale;
    private final float mUnselectedZ;

    public ViewFocusAnimator(View view) {
        this.mTargetView = view;
        Resources res = view.getResources();
        this.mTargetView.setOnFocusChangeListener(this);
        TypedValue out = new TypedValue();
        res.getValue(R.dimen.unselected_scale, out, true);
        this.mUnselectedScale = out.getFloat();
        this.mSelectedScaleDelta = res.getFraction(R.fraction.lb_focus_zoom_factor_medium, 1, 1) - this.mUnselectedScale;
        this.mUnselectedZ = res.getDimensionPixelOffset(R.dimen.unselected_item_z);
        this.mSelectedZDelta = res.getDimensionPixelOffset(R.dimen.selected_item_z_delta);
        this.mAnimDuration = res.getInteger(R.integer.item_scale_anim_duration);
        this.mFocusInterpolator = new AccelerateDecelerateInterpolator();
        this.mFocusAnimation = ObjectAnimator.ofFloat(this, "focusProgress", 0.0f);
        this.mFocusAnimation.setDuration(this.mAnimDuration);
        this.mFocusAnimation.setInterpolator(this.mFocusInterpolator);
        setFocusProgress(0.0f);
        this.mFocusAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.animation.ViewFocusAnimator.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                ViewFocusAnimator.this.mTargetView.setHasTransientState(true);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                ViewFocusAnimator.this.mTargetView.setHasTransientState(false);
            }
        });
    }

    public void setFocusProgress(float level) {
        this.mFocusProgress = level;
        float scale = this.mUnselectedScale + (this.mSelectedScaleDelta * level);
        float z = this.mUnselectedZ + (this.mSelectedZDelta * level);
        this.mTargetView.setScaleX(scale);
        this.mTargetView.setScaleY(scale);
        this.mTargetView.setZ(z);
    }

    public float getFocusProgress() {
        return this.mFocusProgress;
    }

    public void animateFocus(boolean focused) {
        if (this.mFocusAnimation.isStarted()) {
            this.mFocusAnimation.cancel();
        }
        float target = focused ? 1.0f : 0.0f;
        if (getFocusProgress() == target) {
            return;
        }
        this.mFocusAnimation.setFloatValues(getFocusProgress(), target);
        this.mFocusAnimation.start();
    }

    public void setFocusImmediate(boolean focused) {
        if (this.mFocusAnimation.isStarted()) {
            this.mFocusAnimation.cancel();
        }
        float target = focused ? 1.0f : 0.0f;
        setFocusProgress(target);
    }

    @Override // android.view.View.OnFocusChangeListener
    public void onFocusChange(View v, boolean hasFocus) {
        if (v != this.mTargetView) {
            return;
        }
        ViewGroup.LayoutParams lp = this.mTargetView.getLayoutParams();
        int width = lp.width;
        int height = lp.height;
        if (width < 0 && height < 0) {
            this.mTargetView.measure(0, 0);
            this.mTargetView.getMeasuredHeight();
        }
        if (this.mTargetView.isAttachedToWindow() && this.mTargetView.hasWindowFocus() && this.mTargetView.getVisibility() == 0) {
            animateFocus(hasFocus);
        } else {
            setFocusImmediate(hasFocus);
        }
    }
}
