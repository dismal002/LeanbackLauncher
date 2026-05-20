package com.dismal.android.leanbacklauncher.animation;

import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.animation.PropagatingAnimator;
import com.dismal.android.leanbacklauncher.util.Preconditions;

/* JADX INFO: loaded from: classes.dex */
public final class MassFadeAnimator extends PropagatingAnimator<MassFadeAnimator.ViewHolder> implements Joinable {

    /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] f1com_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues;
    private final Direction mDirection;
    private final float mEndAlpha;
    private final ViewGroup mRoot;
    private final float mStartAlpha;
    private final Class<?> mTargetClass;

    public interface Participant {
    }

    /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] m52getcom_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues() {
        if (f1com_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues != null) {
            return f1com_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues;
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
        f1com_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues = iArr;
        return iArr;
    }

    /* synthetic */ MassFadeAnimator(Builder builder, MassFadeAnimator massFadeAnimator) {
        this(builder);
    }

    public enum Direction {
        FADE_IN,
        FADE_OUT;

        /* JADX INFO: renamed from: values, reason: to resolve conflict with enum method */
        public static Direction[] valuesCustom() {
            return values();
        }
    }

    private MassFadeAnimator(Builder builder) {
        super(10);
        this.mRoot = builder.mRoot;
        this.mDirection = builder.mDirection;
        this.mTargetClass = builder.mTargetClass;
        switch (m52getcom_google_android_leanbacklauncher_animation_MassFadeAnimator$DirectionSwitchesValues()[this.mDirection.ordinal()]) {
            case 1:
                this.mStartAlpha = 0.0f;
                this.mEndAlpha = 1.0f;
                break;
            case 2:
                this.mStartAlpha = 1.0f;
                this.mEndAlpha = 0.0f;
                break;
            default:
                throw new IllegalStateException("Unknown direction: " + this.mDirection);
        }
        if (builder.mDuration <= 0) {
            return;
        }
        setDuration(builder.mDuration);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator, android.animation.Animator
    public void setupStartValues() {
        if (size() == 0) {
            addViews(this.mRoot);
        }
        super.setupStartValues();
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void include(View target) {
        addView(new ViewHolder(target));
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void exclude(View target) {
        int n = size();
        for (int i = 0; i < n; i++) {
            ViewHolder holder = getView(i);
            if (holder.view == target) {
                removeView(i);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public void onSetupStartValues(ViewHolder holder) {
        holder.view.setAlpha(this.mStartAlpha);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public void onUpdateView(ViewHolder holder, float fraction) {
        float alpha = this.mStartAlpha + ((this.mEndAlpha - this.mStartAlpha) * fraction);
        holder.view.setAlpha(alpha);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public void onResetView(ViewHolder holder) {
        holder.view.setAlpha(1.0f);
    }

    private void addViews(ViewGroup localRoot) {
        int n = localRoot.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = localRoot.getChildAt(i);
            if (this.mTargetClass.isInstance(child)) {
                addView(new ViewHolder(child));
            }
            if (child instanceof ViewGroup) {
                addViews((ViewGroup) child);
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator, android.animation.ValueAnimator
    public String toString() {
        StringBuilder buf = new StringBuilder().append("MassFadeAnimator@").append(Integer.toHexString(hashCode())).append(':').append(this.mDirection == Direction.FADE_IN ? "FADE_IN" : "FADE_OUT").append('{');
        int n = size();
        for (int i = 0; i < n; i++) {
            buf.append("\n    ").append(getView(i).toString().replaceAll("\n", "\n    "));
        }
        return buf.append("\n}").toString();
    }

    public static final class Builder {
        private final ViewGroup mRoot;
        private Direction mDirection = Direction.FADE_OUT;
        private Class<?> mTargetClass = Participant.class;
        private long mDuration = -1;

        public Builder(ViewGroup root) {
            this.mRoot = (ViewGroup) Preconditions.checkNotNull(root);
        }

        public Builder setDirection(Direction direction) {
            this.mDirection = (Direction) Preconditions.checkNotNull(direction);
            return this;
        }

        public Builder setTarget(Class<?> targetClass) {
            this.mTargetClass = (Class) Preconditions.checkNotNull(targetClass);
            return this;
        }

        public Builder setDuration(long duration) {
            Preconditions.checkArgument(duration > 0);
            this.mDuration = duration;
            return this;
        }

        public MassFadeAnimator build() {
            return new MassFadeAnimator(this, null);
        }
    }

    static final class ViewHolder extends PropagatingAnimator.ViewHolder {
        ViewHolder(View view) {
            super(view);
        }

        public String toString() {
            return this.view.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this.view));
        }
    }
}
