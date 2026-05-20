package com.dismal.android.leanbacklauncher.wallpaper;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.dismal.android.leanbacklauncher.R;

/* JADX INFO: loaded from: classes.dex */
public class AnimatedLayer extends WallpaperImage {
    private final Animator mFadeInAnim;
    private final Animator mFadeOutAnim;
    private AnimationListener mListener;
    private Animator mRunningAnimation;

    public interface AnimationListener {
        void animationDone(boolean z);
    }

    public AnimatedLayer(Context context) {
        this(context, null);
    }

    public AnimatedLayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void setAnimationListener(AnimationListener listener) {
        this.mListener = listener;
    }

    public AnimatedLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFadeInAnim = AnimatorInflater.loadAnimator(context, R.anim.wallpaper_fade_in);
        this.mFadeOutAnim = AnimatorInflater.loadAnimator(context, R.anim.wallpaper_fade_out);
        this.mFadeInAnim.setTarget(this);
        this.mFadeOutAnim.setTarget(this);
        this.mFadeInAnim.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.wallpaper.AnimatedLayer.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (AnimatedLayer.this.mRunningAnimation == AnimatedLayer.this.mFadeInAnim) {
                    AnimatedLayer.this.mRunningAnimation = null;
                }
                if (AnimatedLayer.this.mListener == null) {
                    return;
                }
                AnimatedLayer.this.mListener.animationDone(true);
            }
        });
        this.mFadeOutAnim.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.wallpaper.AnimatedLayer.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (AnimatedLayer.this.mRunningAnimation == AnimatedLayer.this.mFadeOutAnim) {
                    AnimatedLayer.this.mRunningAnimation = null;
                }
                AnimatedLayer.this.setVisibility(8);
                if (AnimatedLayer.this.mListener == null) {
                    return;
                }
                AnimatedLayer.this.mListener.animationDone(false);
            }
        });
    }

    public boolean isAnimating() {
        if (this.mRunningAnimation != null) {
            return this.mRunningAnimation.isRunning();
        }
        return false;
    }

    public void cancelAnimation() {
        if (!isAnimating()) {
            return;
        }
        this.mRunningAnimation.cancel();
    }

    public void animateIn(Drawable image) {
        cancelAnimation();
        setVisibility(0);
        setImageDrawable(image);
        this.mRunningAnimation = this.mFadeInAnim;
        this.mFadeInAnim.start();
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setPivotX(0.0f);
        setPivotY(getMeasuredHeight() / 2);
    }

    public void animateOut(Drawable image) {
        cancelAnimation();
        setVisibility(0);
        setImageDrawable(image);
        this.mRunningAnimation = this.mFadeOutAnim;
        this.mFadeOutAnim.start();
    }
}
