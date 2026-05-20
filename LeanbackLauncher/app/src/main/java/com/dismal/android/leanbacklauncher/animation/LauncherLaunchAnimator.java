package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.FadeAnimator;
import com.dismal.android.leanbacklauncher.animation.MassFadeAnimator;
import com.dismal.android.leanbacklauncher.animation.MassSlideAnimator;
import com.dismal.android.leanbacklauncher.notifications.NotificationCardView;
import com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper;

/* JADX INFO: loaded from: classes.dex */
public final class LauncherLaunchAnimator extends ForwardingAnimatorSet {
    public LauncherLaunchAnimator(ViewGroup root, View cause, Rect epicenter, ImageView circleLayerView, int color, View[] headers, NotificationViewFlipper notificationsFlipper) {
        Resources res = root.getResources();
        int fadeDuration = res.getInteger(R.integer.app_launch_animation_header_fade_out_duration);
        int fadeDelay = res.getInteger(R.integer.app_launch_animation_header_fade_out_delay);
        Animator anim = new CircleTakeoverAnimator(cause, circleLayerView, color);
        anim.setDuration(res.getInteger(R.integer.app_launch_animation_explode_duration));
        AnimatorSet.Builder builder = ((AnimatorSet) this.mDelegate).play(anim);
        if (cause instanceof NotificationCardView) {
            builder.with(new MassFadeAnimator.Builder(root).setDirection(MassFadeAnimator.Direction.FADE_OUT).setTarget(NotificationCardView.class).setDuration(res.getInteger(R.integer.app_launch_animation_rec_fade_duration)).build());
            builder.with(new MassSlideAnimator.Builder(root).setEpicenter(epicenter).setExclude(cause).setExclude(NotificationCardView.class).setFade(false).build());
        } else {
            Animator anim2 = new FadeAnimator(cause, FadeAnimator.Direction.FADE_OUT);
            anim2.setDuration(res.getInteger(R.integer.app_launch_animation_target_fade_duration));
            anim2.setStartDelay(res.getInteger(R.integer.app_launch_animation_target_fade_delay));
            builder.with(anim2);
            builder.with(new MassSlideAnimator.Builder(root).setEpicenter(epicenter).setExclude(cause).setFade(false).build());
            if (notificationsFlipper != null && !notificationsFlipper.isRowViewVisible()) {
                Animator anim3 = new FadeAnimator(notificationsFlipper, FadeAnimator.Direction.FADE_OUT);
                anim3.setDuration(fadeDuration);
                anim3.setStartDelay(fadeDelay);
                builder.with(anim3);
            }
        }
        for (View view : headers) {
            Animator anim4 = new FadeAnimator(view, FadeAnimator.Direction.FADE_OUT);
            anim4.setDuration(fadeDuration);
            anim4.setStartDelay(fadeDelay);
            builder.with(anim4);
        }
    }
}
