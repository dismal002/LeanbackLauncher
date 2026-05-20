package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public abstract class ForwardingAnimatorSet extends ForwardingAnimator<AnimatorSet> {
    protected ForwardingAnimatorSet() {
        super(new AnimatorSet());
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, com.dismal.android.leanbacklauncher.animation.Resettable
    public void reset() {
        for (Cloneable cloneable : ((AnimatorSet) this.mDelegate).getChildAnimations()) {
            if (cloneable instanceof Resettable) {
                ((Resettable) cloneable).reset();
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, com.dismal.android.leanbacklauncher.animation.Joinable
    public void include(View target) {
        for (Cloneable cloneable : ((AnimatorSet) this.mDelegate).getChildAnimations()) {
            if (cloneable instanceof Joinable) {
                ((Joinable) cloneable).include(target);
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator, com.dismal.android.leanbacklauncher.animation.Joinable
    public void exclude(View target) {
        for (Cloneable cloneable : ((AnimatorSet) this.mDelegate).getChildAnimations()) {
            if (cloneable instanceof Joinable) {
                ((Joinable) cloneable).exclude(target);
            }
        }
    }

    @Override // com.dismal.android.leanbacklauncher.animation.ForwardingAnimator
    public String toString() {
        StringBuilder buf = new StringBuilder().append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).append('{');
        for (Animator animation : ((AnimatorSet) this.mDelegate).getChildAnimations()) {
            buf.append("\n    ").append(animation.toString().replaceAll("\n", "\n    "));
        }
        return buf.append("\n}").toString();
    }
}
