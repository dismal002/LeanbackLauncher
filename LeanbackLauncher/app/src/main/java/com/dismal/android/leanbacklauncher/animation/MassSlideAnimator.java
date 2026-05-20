package com.dismal.android.leanbacklauncher.animation;

import android.animation.TimeInterpolator;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.PropagatingAnimator;
import com.dismal.android.leanbacklauncher.util.Preconditions;

/* JADX INFO: loaded from: classes.dex */
public final class MassSlideAnimator extends PropagatingAnimator<MassSlideAnimator.ViewHolder> implements Joinable {

    /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] f2com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues;
    private static final TimeInterpolator sSlideInInterpolator = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    private static final TimeInterpolator sSlideOutInterpolator = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    private final Direction mDirection;
    private final Rect mEpicenter;
    private final View mExclude;
    private final Class<?> mExcludeClass;
    private final boolean mFade;
    private final float mPropagationSpeed;
    private final ViewGroup mRoot;
    private final Class<?> mTargetClass;

    /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] m61getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues() {
        if (f2com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues != null) {
            return f2com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues;
        }
        int[] iArr = new int[Direction.valuesCustom().length];
        try {
            iArr[Direction.SLIDE_IN.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Direction.SLIDE_OUT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f2com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues = iArr;
        return iArr;
    }

    /* synthetic */ MassSlideAnimator(Builder builder, MassSlideAnimator massSlideAnimator) {
        this(builder);
    }

    public enum Direction {
        SLIDE_IN,
        SLIDE_OUT;

        /* JADX INFO: renamed from: values, reason: to resolve conflict with enum method */
        public static Direction[] valuesCustom() {
            return values();
        }
    }

    private MassSlideAnimator(Builder builder) {
        super(20);
        TypedValue propagationSpeed = new TypedValue();
        Resources res = builder.mRoot.getResources();
        res.getValue(R.dimen.slide_animator_propagation_speed, propagationSpeed, true);
        this.mRoot = builder.mRoot;
        this.mEpicenter = builder.mEpicenter;
        this.mDirection = builder.mDirection;
        this.mTargetClass = builder.mTargetClass;
        this.mExclude = builder.mExclude;
        this.mExcludeClass = builder.mExcludeClass;
        this.mFade = builder.mFade;
        this.mPropagationSpeed = propagationSpeed.getFloat();
        super.setPropagation(new SlidePropagation());
        switch (m61getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues()[this.mDirection.ordinal()]) {
            case 1:
                super.setInterpolator(sSlideInInterpolator);
                break;
            case 2:
                super.setInterpolator(sSlideOutInterpolator);
                break;
            default:
                throw new IllegalStateException("Unknown direction: " + this.mDirection);
        }
        setDuration(res.getInteger(R.integer.slide_animator_default_duration));
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public PropagatingAnimator<ViewHolder> setPropagation(PropagatingAnimator.Propagation<ViewHolder> propagation) {
        throw new UnsupportedOperationException("Propagation is immutable");
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public void setInterpolator(TimeInterpolator value) {
        throw new UnsupportedOperationException("Interpolator is immutable");
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator, android.animation.Animator
    public void setupStartValues() {
        if (size() == 0) {
            addViews(this.mRoot);
        }
        super.setupStartValues();
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator, com.dismal.android.leanbacklauncher.animation.Resettable
    public void reset() {
        super.reset();
        for (int i = size() - 1; i >= 0; i--) {
            removeView(i);
        }
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public ViewHolder removeView(int index) {
        ViewHolder holder = (ViewHolder) super.removeView(index);
        holder.removeListener();
        return holder;
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void include(View target) {
        addView(new ViewHolder(this, target, this.mRoot, this.mEpicenter, this.mDirection, null));
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
        onUpdateView(holder, 0.0f);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public void onUpdateView(ViewHolder holder, float fraction) {
        float progress = fraction * (holder.mEndY - holder.mStartY);
        holder.view.setTranslationY(holder.mStartY + progress);
        if (!this.mFade) {
            return;
        }
        float alpha = this.mDirection == Direction.SLIDE_IN ? fraction : 1.0f - fraction;
        holder.view.setAlpha(alpha);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator
    public void onResetView(ViewHolder holder) {
        holder.view.setTranslationY(0.0f);
        if (!this.mFade) {
            return;
        }
        holder.view.setAlpha(1.0f);
    }

    private void addViews(ViewGroup localRoot) {
        int n = localRoot.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = localRoot.getChildAt(i);
            if (this.mTargetClass.isInstance(child) && !isExcluded(child)) {
                addView(new ViewHolder(this, child, this.mRoot, this.mEpicenter, this.mDirection, null));
            }
            if (child instanceof ViewGroup) {
                addViews((ViewGroup) child);
            }
        }
    }

    private boolean isExcluded(View view) {
        if (view == this.mExclude) {
            return true;
        }
        if (this.mExcludeClass != null) {
            return this.mExcludeClass.isInstance(view);
        }
        return false;
    }

    @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator, android.animation.ValueAnimator
    public String toString() {
        StringBuilder buf = new StringBuilder().append("MassSlideAnimator@").append(Integer.toHexString(hashCode())).append(':').append(this.mDirection == Direction.SLIDE_IN ? "SLIDE_IN" : "SLIDE_OUT").append(':').append(this.mEpicenter.centerX()).append(',').append(this.mEpicenter.centerY()).append('{');
        int n = size();
        for (int i = 0; i < n; i++) {
            buf.append("\n    ").append(getView(i).toString().replaceAll("\n", "\n    "));
        }
        return buf.append("\n}").toString();
    }

    public static final class Builder {
        private View mExclude;
        private Class<?> mExcludeClass;
        private final ViewGroup mRoot;
        private Direction mDirection = Direction.SLIDE_OUT;
        private Rect mEpicenter = new Rect();
        private Class<?> mTargetClass = ParticipatesInLaunchAnimation.class;
        private boolean mFade = true;

        public Builder(ViewGroup root) {
            this.mRoot = (ViewGroup) Preconditions.checkNotNull(root);
        }

        public Builder setDirection(Direction direction) {
            this.mDirection = (Direction) Preconditions.checkNotNull(direction);
            return this;
        }

        public Builder setEpicenter(Rect epicenter) {
            this.mEpicenter = (Rect) Preconditions.checkNotNull(epicenter);
            return this;
        }

        public Builder setExclude(View exclude) {
            this.mExclude = (View) Preconditions.checkNotNull(exclude);
            return this;
        }

        public Builder setExclude(Class<?> excludeClass) {
            this.mExcludeClass = excludeClass;
            return this;
        }

        public Builder setFade(boolean fade) {
            this.mFade = fade;
            return this;
        }

        public MassSlideAnimator build() {
            return new MassSlideAnimator(this, null);
        }
    }

    final class ViewHolder extends PropagatingAnimator.ViewHolder {

        /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues, reason: not valid java name */
        private /* synthetic */ int[] f3com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues;
        final int[] mCenter;
        final float mEndY;
        private final View.OnLayoutChangeListener mListener;
        final int mSide;
        final float mStartY;

        /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues, reason: not valid java name */
        private /* synthetic */ int[] m69getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues() {
            if (f3com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues != null) {
                return f3com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues;
            }
            int[] iArr = new int[Direction.valuesCustom().length];
            try {
                iArr[Direction.SLIDE_IN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Direction.SLIDE_OUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            f3com_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues = iArr;
            return iArr;
        }

        /* synthetic */ ViewHolder(MassSlideAnimator this$0, View view, ViewGroup root, Rect epicenter, Direction direction, ViewHolder viewHolder) {
            this(view, root, epicenter, direction);
        }

        private ViewHolder(View view, ViewGroup root, Rect epicenter, Direction direction) {
            super(view);
            this.mCenter = new int[2];
            this.mListener = new View.OnLayoutChangeListener() { // from class: com.dismal.android.leanbacklauncher.animation.MassSlideAnimator.ViewHolder.1
                @Override // android.view.View.OnLayoutChangeListener
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    float restoreTranslationY = v.getTranslationY();
                    v.setTranslationY(0.0f);
                    ViewHolder.this.recordMCenter(v);
                    v.setTranslationY(restoreTranslationY);
                    MassSlideAnimator.this.invalidateView(ViewHolder.this);
                }
            };
            recordMCenter(view);
            this.mSide = this.mCenter[1] > epicenter.centerY() ? 2 : 1;
            switch (m69getcom_google_android_leanbacklauncher_animation_MassSlideAnimator$DirectionSwitchesValues()[direction.ordinal()]) {
                case 1:
                    this.mStartY = getEndY(root);
                    this.mEndY = 0.0f;
                    break;
                case 2:
                    this.mStartY = 0.0f;
                    this.mEndY = getEndY(root);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal direction: " + direction);
            }
            addListener();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordMCenter(View view) {
            view.getLocationOnScreen(this.mCenter);
            int[] iArr = this.mCenter;
            iArr[0] = iArr[0] + (view.getWidth() / 2);
            int[] iArr2 = this.mCenter;
            iArr2[1] = iArr2[1] + (view.getHeight() / 2);
        }

        private void addListener() {
            this.view.addOnLayoutChangeListener(this.mListener);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void removeListener() {
            this.view.removeOnLayoutChangeListener(this.mListener);
        }

        private float getScaleFactor(ViewGroup root) {
            float scale = 1.0f;
            for (View v = this.view; v != null && v != root; v = (View) v.getParent()) {
                scale *= v.getScaleY();
            }
            return scale;
        }

        private float getEndY(ViewGroup root) {
            float scaleFactor = getScaleFactor(root);
            switch (this.mSide) {
                case 1:
                    return (-root.getHeight()) / scaleFactor;
                case 2:
                    return root.getHeight() / scaleFactor;
                default:
                    throw new IllegalArgumentException("Illegal side: " + this.mSide);
            }
        }

        public String toString() {
            return this.view.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this.view)) + ':' + this.mCenter[0] + ',' + this.mCenter[1] + ':' + Math.round(this.mStartY) + ".." + Math.round(this.mEndY);
        }
    }

    private final class SlidePropagation implements PropagatingAnimator.Propagation<ViewHolder> {
        private final int mWindowHeight;

        SlidePropagation() {
            Rect windowInsets = new Rect();
            MassSlideAnimator.this.mRoot.getWindowVisibleDisplayFrame(windowInsets);
            this.mWindowHeight = windowInsets.height();
        }

        @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator.Propagation
        public long getStartDelay(ViewHolder holder) {
            float distance = getDistance(holder);
            float distanceFraction = distance / this.mWindowHeight;
            long duration = MassSlideAnimator.this.getDuration();
            if (duration < 0) {
                duration = 300;
            }
            return Math.round((duration / MassSlideAnimator.this.mPropagationSpeed) * distanceFraction);
        }

        private int getDistance(ViewHolder holder) {
            int targetX = holder.mCenter[0];
            int targetY = holder.mCenter[1];
            int epicenterX = MassSlideAnimator.this.mEpicenter.centerX();
            switch (holder.mSide) {
                case 1:
                    return (this.mWindowHeight - targetY) + Math.abs(epicenterX - targetX);
                case 2:
                    return Math.abs(epicenterX - targetX) + targetY;
                default:
                    throw new IllegalArgumentException("Unsupported side: " + holder.mSide);
            }
        }

        public String toString() {
            return getClass().getSimpleName() + '[' + MassSlideAnimator.this.mDirection.name() + '@' + MassSlideAnimator.this.mEpicenter.centerX() + ',' + MassSlideAnimator.this.mEpicenter.centerY() + ']';
        }
    }
}
