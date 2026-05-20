package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.util.Preconditions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class AnimatorLifecycle implements Resettable, Joinable {
    private Animator mAnimation;
    private Runnable mCallback;
    private byte mFlags;
    private OnAnimationFinishedListener mOnAnimationFinishedListener;
    public final Rect lastKnownEpicenter = new Rect();
    private final Handler mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.animation.AnimatorLifecycle.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (AnimatorLifecycle.this.isPrimed()) {
                        AnimatorLifecycle.this.start();
                    }
                    break;
            }
        }
    };
    private final ArrayList<String> mRecentAnimationDumps = new ArrayList<>();

    public interface OnAnimationFinishedListener {
        void onAnimationFinished();
    }

    public <T extends Animator & Resettable> void init(T animation, Runnable callback, byte flags) {
        if (this.mAnimation != null) {
            StringWriter buf = new StringWriter();
            PrintWriter writer = new PrintWriter(buf);
            writer.println("Called to initialize an animation that was already initialized");
            new Exception("stack trace").printStackTrace(writer);
            dump("", writer, null);
            Log.w("Animations", buf.toString());
            reset();
        }
        this.mAnimation = animation;
        this.mCallback = callback;
        this.mFlags = flags;
        setState((byte) 1);
    }

    public <T extends Animator & Resettable> void schedule() {
        Preconditions.checkState(isInitialized());
        setState((byte) 2);
    }

    public void prime() {
        Preconditions.checkState(isScheduled());
        this.mAnimation.setupStartValues();
        setState((byte) 4);
        this.mHandler.sendEmptyMessageDelayed(1, 1000L);
    }

    public void start() {
        Preconditions.checkState((isInitialized() || isScheduled()) ? true : isPrimed());
        this.mAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.animation.AnimatorLifecycle.2
            private boolean mCancelled;

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            /* JADX WARN: Multi-variable type inference failed */
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                animator.removeListener(this);
                AnimatorLifecycle.this.setState((byte) 16);
                if (AnimatorLifecycle.this.mAnimation == null) {
                    StringWriter buf = new StringWriter();
                    PrintWriter writer = new PrintWriter(buf);
                    writer.println("listener notified of animation end when mAnimation==null");
                    new Exception("stack trace").printStackTrace(writer);
                    AnimatorLifecycle.this.dump("", writer, null);
                    writer.println(animator.toString());
                    Log.w("Animations", buf.toString());
                    ((Resettable) animator).reset();
                }
                if (AnimatorLifecycle.this.mOnAnimationFinishedListener != null) {
                    AnimatorLifecycle.this.mOnAnimationFinishedListener.onAnimationFinished();
                }
                if (this.mCancelled) {
                    AnimatorLifecycle.this.reset();
                } else if (AnimatorLifecycle.this.mCallback != null) {
                    try {
                        AnimatorLifecycle.this.mCallback.run();
                    } catch (Throwable t) {
                        Log.e("Animations", "Could not execute callback", t);
                        AnimatorLifecycle.this.reset();
                    }
                }
                if ((AnimatorLifecycle.this.mFlags & 32) == 0) {
                    return;
                }
                AnimatorLifecycle.this.reset();
            }
        });
        this.mAnimation.start();
        setState((byte) 8);
        if (this.mRecentAnimationDumps == null) {
            return;
        }
        while (this.mRecentAnimationDumps.size() >= 10) {
            this.mRecentAnimationDumps.remove(9);
        }
        this.mRecentAnimationDumps.add(0, this.mAnimation.toString());
    }

    public void cancel() {
        if (!isRunning()) {
            return;
        }
        this.mAnimation.cancel();
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Resettable
    public void reset() {
        cancel();
        if (this.mAnimation != null) {
            ((Resettable) this.mAnimation).reset();
        }
        this.mFlags = (byte) 0;
        this.mAnimation = null;
        this.mCallback = null;
        this.mHandler.removeMessages(1);
    }

    public boolean isInitialized() {
        return (this.mAnimation == null || (this.mFlags & 1) == 0) ? false : true;
    }

    public boolean isScheduled() {
        return (this.mAnimation == null || (this.mFlags & 2) == 0) ? false : true;
    }

    public boolean isPrimed() {
        return (this.mAnimation == null || (this.mFlags & 4) == 0) ? false : true;
    }

    public boolean isRunning() {
        return (this.mAnimation == null || (this.mFlags & 8) == 0) ? false : true;
    }

    public boolean isFinished() {
        return (this.mAnimation == null || (this.mFlags & 16) == 0) ? false : true;
    }

    public void setOnAnimationFinishedListener(OnAnimationFinishedListener listener) {
        this.mOnAnimationFinishedListener = listener;
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void include(View target) {
        if (!(this.mAnimation instanceof Joinable)) {
            return;
        }
        ((Joinable) this.mAnimation).include(target);
    }

    @Override // com.dismal.android.leanbacklauncher.animation.Joinable
    public void exclude(View target) {
        if (!(this.mAnimation instanceof Joinable)) {
            return;
        }
        ((Joinable) this.mAnimation).exclude(target);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setState(byte state) {
        this.mFlags = (byte) (this.mFlags & (-32));
        this.mFlags = (byte) (this.mFlags | state);
        this.mHandler.removeMessages(1);
    }

    public void dump(String prefix, PrintWriter writer, ViewGroup root) {
        writer.format("%s%s State:\n", prefix, getClass().getSimpleName());
        String prefix2 = prefix + "  ";
        writer.format("%sstate: ", prefix2);
        switch (this.mFlags & 31) {
            case 1:
                writer.write("INIT");
                break;
            case 2:
                writer.write("SCHEDULED");
                break;
            case 4:
                writer.write("PRIMED");
                break;
            case 8:
                writer.write("RUNNING");
                break;
            case 16:
                writer.write("FINISHED");
                break;
            default:
                writer.write("<idle>");
                break;
        }
        writer.println();
        writer.format("%sflags: ", prefix2);
        writer.write((this.mFlags & 32) == 0 ? 46 : 82);
        writer.println();
        writer.format("%slastKnownEpicenter: %d,%d\n", prefix2, Integer.valueOf(this.lastKnownEpicenter.centerX()), Integer.valueOf(this.lastKnownEpicenter.centerY()));
        Object[] objArr = new Object[2];
        objArr[0] = prefix2;
        objArr[1] = this.mAnimation == null ? "null" : this.mAnimation.toString().replaceAll("\n", "\n" + prefix2);
        writer.format("%smAnimation: %s\n", objArr);
        writer.format("%sAnimatable Views:\n", prefix2);
        if (root != null) {
            dumpViewHierarchy(prefix2 + "  ", writer, root);
        }
        if (this.mRecentAnimationDumps == null) {
            return;
        }
        writer.format("%smRecentAnimationDumps: [\n", prefix2);
        int n = this.mRecentAnimationDumps.size();
        for (int i = 0; i < n; i++) {
            writer.format("%s    %d) %s\n", prefix2, Integer.valueOf(i), this.mRecentAnimationDumps.get(i).replaceAll("\n", "\n" + prefix2 + "    "));
        }
        writer.format("%s]\n", prefix2);
    }

    private void dumpViewHierarchy(String prefix, PrintWriter writer, View view) {
        if (view instanceof ParticipatesInLaunchAnimation) {
            writer.format("%s%s\n", prefix, toShortString(view));
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        int n = group.getChildCount();
        for (int i = 0; i < n; i++) {
            dumpViewHierarchy(prefix, writer, group.getChildAt(i));
        }
    }

    private String toShortString(View view) {
        if (view == null) {
            return "null";
        }
        return view.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(view)) + "{" + String.format("%.1f", Float.valueOf(view.getAlpha())) + " " + String.format("%.1f", Float.valueOf(view.getTranslationY())) + " " + String.format("%.1fx%.1f", Float.valueOf(view.getScaleX()), Float.valueOf(view.getScaleY())) + " " + (view.isFocused() ? 'F' : '.') + (view.isSelected() ? 'S' : '.') + "}";
    }
}
