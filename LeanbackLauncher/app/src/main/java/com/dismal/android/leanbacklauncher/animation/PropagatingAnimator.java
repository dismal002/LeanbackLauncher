package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import com.dismal.android.leanbacklauncher.animation.PropagatingAnimator.ViewHolder;
import com.dismal.android.leanbacklauncher.util.Preconditions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public abstract class PropagatingAnimator<VH extends ViewHolder> extends ValueAnimator implements Resettable {
    private static final Propagation<?> sDefaultPropagation = new NoPropagation(null);
    private final PropagatingAnimator<VH>.PropagatingAnimatorListener mListener;
    private long mMaxStartDelay;
    private boolean mNormalized;
    private Propagation<VH> mPropagation;
    private byte mState;
    private final ArrayList<VH> mViews;

    public interface Propagation<VH extends ViewHolder> {
        long getStartDelay(VH vh);
    }

    protected abstract void onResetView(VH vh);

    protected abstract void onSetupStartValues(VH vh);

    protected abstract void onUpdateView(VH vh, float f);

    public static abstract class ViewHolder {
        long normalizedStartDelay;
        long rawStartDelay;
        protected final View view;

        protected ViewHolder(View view) {
            this.view = (View) Preconditions.checkNotNull(view);
        }
    }

    protected PropagatingAnimator() {
        this.mViews = new ArrayList<>();
        this.mListener = new PropagatingAnimatorListener(this, null);
        this.mPropagation = (Propagation<VH>) sDefaultPropagation;
        this.mState = (byte) 1;
        setFloatValues(0.0f, 1.0f);
        addListener();
    }

    protected PropagatingAnimator(int initialCapacity) {
        this();
        this.mViews.ensureCapacity(initialCapacity);
    }

    public PropagatingAnimator<VH> setPropagation(Propagation<VH> propagation) {
        this.mPropagation = (Propagation) Preconditions.checkNotNull(propagation);
        return this;
    }

    public PropagatingAnimator<VH> addView(VH vh) {
        this.mViews.add((VH) ((ViewHolder) Preconditions.checkNotNull(vh)));
        vh.rawStartDelay = getStartDelay(vh);
        this.mNormalized = false;
        if (isStarted()) {
            normalizeStartDelays();
            long childDuration = getChildAnimationDuration();
            float currentPlayTime = childDuration > 0 ? (float) (getCurrentPlayTime() - vh.normalizedStartDelay) / childDuration : 1.0f;
            if (currentPlayTime <= 0.0f) {
                onSetupStartValues(vh);
            } else {
                if (currentPlayTime > 1.0f) {
                    currentPlayTime = 1.0f;
                }
                onUpdateView(vh, getInterpolator().getInterpolation(currentPlayTime));
            }
        } else if (this.mState == 2) {
            onSetupStartValues(vh);
        }
        return this;
    }

    public VH removeView(int index) {
        VH holder = this.mViews.remove(index);
        long startDelay = holder.normalizedStartDelay;
        if (startDelay == 0 || startDelay == this.mMaxStartDelay) {
            this.mNormalized = false;
        }
        if (isStarted()) {
            if (!this.mNormalized) {
                normalizeStartDelays();
            }
            onResetView(holder);
        } else if (this.mState == 2 || this.mState == 16) {
            onResetView(holder);
        }
        return holder;
    }

    protected final void invalidateView(VH holder) {
        holder.rawStartDelay = getStartDelay(holder);
        this.mNormalized = false;
        if (!isStarted()) {
            return;
        }
        normalizeStartDelays();
    }

    public VH getView(int index) {
        return this.mViews.get(index);
    }

    public int size() {
        return this.mViews.size();
    }

    public long getChildAnimationDuration() {
        if (!this.mNormalized) {
            normalizeStartDelays();
        }
        return getDuration() - this.mMaxStartDelay;
    }

    public void reset() {
        if (this.mState == 4 || this.mState == 8) {
            StringWriter buf = new StringWriter();
            PrintWriter writer = new PrintWriter(buf);
            writer.println("Reset while started");
            new Exception("stack trace").printStackTrace(writer);
            writer.println(toString());
            Log.w("Animations", buf.toString());
            cancel();
            return;
        }
        int n = this.mViews.size();
        for (int i = 0; i < n; i++) {
            onResetView(this.mViews.get(i));
        }
        this.mState = (byte) 32;
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public PropagatingAnimator<VH> setDuration(long duration) {
        if (isStarted()) {
            throw new IllegalStateException("Can't alter the duration after start");
        }
        super.setDuration(duration);
        int n = this.mViews.size();
        for (int i = 0; i < n; i++) {
            invalidateView(this.mViews.get(i));
        }
        this.mNormalized = false;
        return this;
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public void start() {
        if (!this.mNormalized) {
            normalizeStartDelays();
        }
        setupStartValues();
        this.mState = (byte) 4;
        super.start();
    }

    @Override // android.animation.Animator
    public void setupStartValues() {
        if (this.mState == 2) {
            return;
        }
        int n = this.mViews.size();
        for (int i = 0; i < n; i++) {
            onSetupStartValues(this.mViews.get(i));
        }
        this.mState = (byte) 2;
    }

    private long getStartDelay(VH holder) {
        long startDelay = this.mPropagation.getStartDelay(holder);
        if (startDelay < 0 || startDelay >= getDuration()) {
            throw new UnsupportedOperationException(String.format("Illegal start delay returned by %s: %d", this.mPropagation, Long.valueOf(startDelay)));
        }
        return startDelay;
    }

    private void normalizeStartDelays() {
        this.mNormalized = true;
        int n = this.mViews.size();
        long minRawDelay = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            minRawDelay = Math.min(minRawDelay, this.mViews.get(i).rawStartDelay);
        }
        this.mMaxStartDelay = n == 0 ? 0L : Long.MIN_VALUE;
        for (int i2 = 0; i2 < n; i2++) {
            VH holder = this.mViews.get(i2);
            long normalizedDelay = holder.rawStartDelay - minRawDelay;
            this.mMaxStartDelay = Math.max(this.mMaxStartDelay, normalizedDelay);
            holder.normalizedStartDelay = normalizedDelay;
        }
    }

    private void addListener() {
        addListener(this.mListener);
        addUpdateListener(this.mListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeListener() {
        removeListener(this.mListener);
        removeUpdateListener(this.mListener);
    }

    @Override // android.animation.ValueAnimator
    public String toString() {
        return "PropagatingAnimator@" + Integer.toHexString(hashCode());
    }

    private final class PropagatingAnimatorListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
        /* synthetic */ PropagatingAnimatorListener(PropagatingAnimator this$0, PropagatingAnimatorListener propagatingAnimatorListener) {
            this();
        }

        private PropagatingAnimatorListener() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
            PropagatingAnimator.this.mState = (byte) 8;
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference fix 'apply assigned field type' failed
        java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$PrimitiveArg
        	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:593)
        	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
        	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
         */
        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            TimeInterpolator interpolator = PropagatingAnimator.this.getInterpolator();
            long duration = PropagatingAnimator.this.getChildAnimationDuration();
            long totalPlayTime = PropagatingAnimator.this.getCurrentPlayTime();
            int n = PropagatingAnimator.this.mViews.size();
            for (int i = 0; i < n; i++) {
                VH viewHolder = PropagatingAnimator.this.mViews.get(i);
                long playTime = totalPlayTime - viewHolder.normalizedStartDelay;
                float fraction = duration > 0 ? (float) playTime / duration : 1.0f;
                if (fraction >= 0.0f) {
                    if (fraction > 1.0f) {
                        fraction = 1.0f;
                    }
                    PropagatingAnimator.this.onUpdateView(viewHolder, interpolator.getInterpolation(fraction));
                }
            }
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            PropagatingAnimator.this.mState = (byte) 16;
            PropagatingAnimator.this.reset();
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            PropagatingAnimator.this.mState = (byte) 16;
            PropagatingAnimator.this.removeListener();
        }
    }

    private static final class NoPropagation<VH extends ViewHolder> implements Propagation<VH> {
        /* synthetic */ NoPropagation(NoPropagation noPropagation) {
            this();
        }

        private NoPropagation() {
        }

        @Override // com.dismal.android.leanbacklauncher.animation.PropagatingAnimator.Propagation
        public long getStartDelay(VH holder) {
            return 0L;
        }
    }
}
