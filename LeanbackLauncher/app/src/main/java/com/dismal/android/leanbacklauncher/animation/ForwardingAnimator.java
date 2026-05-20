package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.util.ArrayMap;
import android.view.View;
import com.dismal.android.leanbacklauncher.util.Preconditions;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public abstract class ForwardingAnimator<T extends Animator> extends Animator implements Resettable, Joinable {
    protected final T mDelegate;
    private ArrayMap<Animator.AnimatorListener, Animator.AnimatorListener> mListeners;
    private ArrayMap<Animator.AnimatorPauseListener, Animator.AnimatorPauseListener> mPauseListeners;

    public ForwardingAnimator(T delegate) {
        this.mDelegate = (T) Preconditions.checkNotNull(delegate);
    }

    public void reset() {
        if (!(this.mDelegate instanceof Resettable)) {
            return;
        }
        ((Resettable) this.mDelegate).reset();
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void include(View target) {
        if (!(this.mDelegate instanceof Joinable)) {
            return;
        }
        ((Joinable) this.mDelegate).include(target);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void exclude(View target) {
        if (!(this.mDelegate instanceof Joinable)) {
            return;
        }
        ((Joinable) this.mDelegate).exclude(target);
    }

    @Override // android.animation.Animator
    public void start() {
        this.mDelegate.start();
    }

    @Override // android.animation.Animator
    public void cancel() {
        this.mDelegate.cancel();
    }

    @Override // android.animation.Animator
    public void end() {
        this.mDelegate.end();
    }

    @Override // android.animation.Animator
    public void pause() {
        this.mDelegate.pause();
    }

    @Override // android.animation.Animator
    public void resume() {
        this.mDelegate.resume();
    }

    @Override // android.animation.Animator
    public boolean isPaused() {
        return this.mDelegate.isPaused();
    }

    @Override // android.animation.Animator
    public long getStartDelay() {
        return this.mDelegate.getStartDelay();
    }

    @Override // android.animation.Animator
    public void setStartDelay(long startDelay) {
        this.mDelegate.setStartDelay(startDelay);
    }

    @Override // android.animation.Animator
    public long getDuration() {
        return this.mDelegate.getDuration();
    }

    @Override // android.animation.Animator
    public Animator setDuration(long duration) {
        this.mDelegate.setDuration(duration);
        return this;
    }

    @Override // android.animation.Animator
    public TimeInterpolator getInterpolator() {
        return this.mDelegate.getInterpolator();
    }

    @Override // android.animation.Animator
    public void setInterpolator(TimeInterpolator value) {
        this.mDelegate.setInterpolator(value);
    }

    @Override // android.animation.Animator
    public void addListener(Animator.AnimatorListener listener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayMap<>();
        }
        if (this.mListeners.containsKey(listener)) {
            return;
        }
        Animator.AnimatorListener proxy = new ProxyingAnimatorListener(listener, this);
        this.mListeners.put(listener, proxy);
        this.mDelegate.addListener(proxy);
    }

    @Override // android.animation.Animator
    public void removeListener(Animator.AnimatorListener listener) {
        if (this.mListeners == null) {
            return;
        }
        Animator.AnimatorListener proxy = this.mListeners.remove(listener);
        if (proxy != null) {
            this.mDelegate.removeListener(proxy);
        }
        if (!this.mListeners.isEmpty()) {
            return;
        }
        this.mListeners = null;
    }

    @Override // android.animation.Animator
    public ArrayList<Animator.AnimatorListener> getListeners() {
        if (this.mListeners == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.mListeners.keySet());
    }

    @Override // android.animation.Animator
    public void addPauseListener(Animator.AnimatorPauseListener listener) {
        if (this.mPauseListeners == null) {
            this.mPauseListeners = new ArrayMap<>();
        }
        if (this.mPauseListeners.containsKey(listener)) {
            return;
        }
        Animator.AnimatorPauseListener proxy = new ProxyingAnimatorPauseListener(listener, this);
        this.mPauseListeners.put(listener, proxy);
        this.mDelegate.addPauseListener(proxy);
    }

    @Override // android.animation.Animator
    public void removePauseListener(Animator.AnimatorPauseListener listener) {
        if (this.mPauseListeners == null) {
            return;
        }
        Animator.AnimatorPauseListener proxy = this.mPauseListeners.remove(listener);
        if (proxy != null) {
            this.mDelegate.removePauseListener(proxy);
        }
        if (!this.mPauseListeners.isEmpty()) {
            return;
        }
        this.mPauseListeners = null;
    }

    @Override // android.animation.Animator
    public void removeAllListeners() {
        this.mDelegate.removeAllListeners();
        this.mListeners = null;
        this.mPauseListeners = null;
    }

    @Override // android.animation.Animator
    public boolean isRunning() {
        return this.mDelegate.isRunning();
    }

    @Override // android.animation.Animator
    public boolean isStarted() {
        return this.mDelegate.isStarted();
    }

    @Override // android.animation.Animator
    public void setupEndValues() {
        this.mDelegate.setupEndValues();
    }

    @Override // android.animation.Animator
    public void setupStartValues() {
        this.mDelegate.setupStartValues();
    }

    @Override // android.animation.Animator
    public void setTarget(Object target) {
        this.mDelegate.setTarget(target);
    }

    public String toString() {
        return "ForwardingAnimator@" + Integer.toHexString(hashCode()) + '{' + this.mDelegate.toString() + '}';
    }

    private static final class ProxyingAnimatorListener implements Animator.AnimatorListener {
        private final Animator.AnimatorListener mDelegate;
        private final Animator mProxyAnimator;

        public ProxyingAnimatorListener(Animator.AnimatorListener delegate, Animator proxyAnimator) {
            this.mDelegate = delegate;
            this.mProxyAnimator = proxyAnimator;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator unused) {
            this.mDelegate.onAnimationStart(this.mProxyAnimator);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator unused) {
            this.mDelegate.onAnimationEnd(this.mProxyAnimator);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator unused) {
            this.mDelegate.onAnimationCancel(this.mProxyAnimator);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator unused) {
            this.mDelegate.onAnimationRepeat(this.mProxyAnimator);
        }
    }

    private static final class ProxyingAnimatorPauseListener implements Animator.AnimatorPauseListener {
        private final Animator.AnimatorPauseListener mDelegate;
        private final Animator mProxyAnimator;

        public ProxyingAnimatorPauseListener(Animator.AnimatorPauseListener delegate, Animator proxyAnimator) {
            this.mDelegate = delegate;
            this.mProxyAnimator = proxyAnimator;
        }

        @Override // android.animation.Animator.AnimatorPauseListener
        public void onAnimationPause(Animator unused) {
            this.mDelegate.onAnimationPause(this.mProxyAnimator);
        }

        @Override // android.animation.Animator.AnimatorPauseListener
        public void onAnimationResume(Animator unused) {
            this.mDelegate.onAnimationResume(this.mProxyAnimator);
        }
    }
}
