package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import com.dismal.android.leanbacklauncher.util.Preconditions;

/* JADX INFO: loaded from: classes.dex */
final class FadeAnimator extends ValueAnimator implements Resettable {

    /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] f0com_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues;
    private final FadeAnimatorListener mListener = new FadeAnimatorListener(this, null);
    private final float mStartAlpha;
    private final View mTarget;

    /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] m51getcom_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues() {
        if (f0com_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues != null) {
            return f0com_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues;
        }
        int[] iArr = new int[Direction.valuesCustom().length];
        try {
            iArr[Direction.FADE_IN.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Direction.FADE_OUT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f0com_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues = iArr;
        return iArr;
    }

    public enum Direction {
        FADE_IN,
        FADE_OUT;

        /* JADX INFO: renamed from: values, reason: to resolve conflict with enum method */
        public static Direction[] valuesCustom() {
            return values();
        }
    }

    public FadeAnimator(View target, Direction direction) {
        float endAlpha;
        this.mTarget = (View) Preconditions.checkNotNull(target);
        switch (m51getcom_google_android_leanbacklauncher_animation_FadeAnimator$DirectionSwitchesValues()[direction.ordinal()]) {
            case 1:
                this.mStartAlpha = 0.0f;
                endAlpha = 1.0f;
                break;
            case 2:
                this.mStartAlpha = 1.0f;
                endAlpha = 0.0f;
                break;
            default:
                throw new IllegalArgumentException("Illegal direction: " + direction);
        }
        setFloatValues(this.mStartAlpha, endAlpha);
        addListener(this.mListener);
        addUpdateListener(this.mListener);
    }

    @Override // android.animation.Animator
    public void setupStartValues() {
        this.mTarget.setAlpha(this.mStartAlpha);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Resettable
    public void reset() {
        this.mTarget.setAlpha(1.0f);
    }

    @Override // android.animation.ValueAnimator
    public String toString() {
        return "FadeAnimator@" + Integer.toHexString(hashCode()) + ':' + (this.mStartAlpha == 0.0f ? "FADE_IN" : "FADE_OUT") + '{' + this.mTarget.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this.mTarget)) + '}';
    }

    private final class FadeAnimatorListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
        /* synthetic */ FadeAnimatorListener(FadeAnimator this$0, FadeAnimatorListener fadeAnimatorListener) {
            this();
        }

        private FadeAnimatorListener() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            FadeAnimator.this.mTarget.setAlpha(((Float) FadeAnimator.this.getAnimatedValue()).floatValue());
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            FadeAnimator.this.reset();
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            animation.removeListener(this);
        }
    }
}
