package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.FadeAnimator;
import com.dismal.android.leanbacklauncher.animation.MassSlideAnimator;

/* JADX INFO: loaded from: classes.dex */
public class LauncherDismissAnimator extends ForwardingAnimatorSet {
    public LauncherDismissAnimator(ViewGroup root, boolean fade, View[] headers) {
        AnimatorSet.Builder builder = ((AnimatorSet) this.mDelegate).play(new MassSlideAnimator.Builder(root).setDirection(MassSlideAnimator.Direction.SLIDE_OUT).setFade(fade).build());
        Resources res = root.getResources();
        int fadeDuration = res.getInteger(R.integer.app_launch_animation_header_fade_out_duration);
        int fadeDelay = res.getInteger(R.integer.app_launch_animation_header_fade_out_delay);
        for (View view : headers) {
            Animator anim = new FadeAnimator(view, FadeAnimator.Direction.FADE_OUT);
            anim.setDuration(fadeDuration);
            anim.setStartDelay(fadeDelay);
            builder.with(anim);
        }
    }
}
