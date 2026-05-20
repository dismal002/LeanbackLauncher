package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.FadeAnimator;
import com.dismal.android.leanbacklauncher.animation.MassFadeAnimator;
import com.dismal.android.leanbacklauncher.animation.MassSlideAnimator;
import com.dismal.android.leanbacklauncher.notifications.NotificationCardView;
import com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper;

/* JADX INFO: loaded from: classes.dex */
public class LauncherReturnAnimator extends ForwardingAnimatorSet {
    public LauncherReturnAnimator(ViewGroup root, Rect epicenter, View[] headers, NotificationViewFlipper notificationsFlipper) {
        AnimatorSet.Builder builder;
        Resources res = root.getResources();
        int fadeDuration = res.getInteger(R.integer.app_launch_animation_header_fade_in_duration);
        int fadeDelay = res.getInteger(R.integer.app_launch_animation_header_fade_in_delay);
        if (root.findFocus() instanceof NotificationCardView) {
            builder = ((AnimatorSet) this.mDelegate).play(new MassSlideAnimator.Builder(root).setEpicenter(epicenter).setDirection(MassSlideAnimator.Direction.SLIDE_IN).setExclude(NotificationCardView.class).setFade(false).build());
            builder.with(new MassFadeAnimator.Builder(root).setDirection(MassFadeAnimator.Direction.FADE_IN).setTarget(NotificationCardView.class).setDuration(res.getInteger(R.integer.app_launch_animation_rec_fade_duration)).build());
        } else {
            builder = ((AnimatorSet) this.mDelegate).play(new MassSlideAnimator.Builder(root).setEpicenter(epicenter).setDirection(MassSlideAnimator.Direction.SLIDE_IN).setFade(false).build());
            if (notificationsFlipper != null && !notificationsFlipper.isRowViewVisible()) {
                Animator anim = new FadeAnimator(notificationsFlipper, FadeAnimator.Direction.FADE_IN);
                anim.setDuration(fadeDuration);
                anim.setStartDelay(fadeDelay);
                builder.with(anim);
            }
        }
        for (View view : headers) {
            Animator anim2 = new FadeAnimator(view, FadeAnimator.Direction.FADE_IN);
            anim2.setDuration(fadeDuration);
            anim2.setStartDelay(fadeDelay);
            builder.with(anim2);
        }
    }
}
