package com.dismal.android.leanbacklauncher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.dismal.android.leanbacklauncher.SearchView;
import com.dismal.android.leanbacklauncher.util.Util;

/* JADX INFO: loaded from: classes.dex */
public class SearchBarView extends FrameLayout implements View.OnClickListener, SearchView {
    private final Animation mAnimBgFocus;
    private final Animation mAnimBgUnfocus;
    private final Animation mAnimGFocus;
    private final Animation mAnimGUnfocus;
    private final Animation mAnimHintFocus;
    private final Animation mAnimHintUnfocus;
    private final Animation mAnimIn;
    private final Animation mAnimMicFocus;
    private final Animation mAnimMicUnfocus;
    private final Animation mAnimOrbBreathe;
    private final Animation mAnimOrbFocus;
    private final Animation mAnimOrbUnfocus;
    private final Animation mAnimOut;
    private boolean mAnimationLock;
    private View mBarView;
    private View mBgView;
    private Context mContext;
    private View mGView;
    private View mHintView;
    private SearchView.SearchLaunchListener mListener;
    private ImageView mMicView;
    private View mOrbView;
    private View mRootView;
    private final Intent mSearchIntent;

    public SearchBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAnimIn = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_in);
        this.mAnimOut = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_out);
        this.mAnimBgFocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_bg_focus);
        this.mAnimBgUnfocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_bg_unfocus);
        this.mAnimHintFocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_hint_focus);
        this.mAnimHintUnfocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_hint_unfocus);
        this.mAnimGFocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_g_focus);
        this.mAnimGUnfocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_g_unfocus);
        this.mAnimOrbFocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_orb_focus);
        this.mAnimOrbBreathe = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_orb_breathe);
        this.mAnimOrbUnfocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_orb_unfocus);
        this.mAnimMicFocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_mic_focus);
        this.mAnimMicUnfocus = AnimationUtils.loadAnimation(context, R.anim.search_bar_anim_mic_unfocus);
        this.mSearchIntent = Util.getSearchIntent();
        this.mContext = context;
        setOnClickListener(this);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.view.View
    public void onFinishInflate() {
        this.mRootView = findViewById(R.id.search_view);
        this.mBarView = findViewById(R.id.search_bar);
        this.mBgView = findViewById(R.id.bg);
        this.mHintView = findViewById(R.id.hint);
        this.mGView = findViewById(R.id.g);
        this.mOrbView = findViewById(R.id.orb);
        this.mMicView = (ImageView) findViewById(R.id.mic);
        this.mAnimIn.setAnimationListener(new AnimateInListener(this, null));
        this.mAnimOut.setAnimationListener(new AnimateFadeListener(this.mRootView));
        this.mAnimHintUnfocus.setAnimationListener(new AnimateFadeListener(this.mHintView));
        this.mAnimGFocus.setAnimationListener(new AnimateFadeListener(this.mGView));
        this.mAnimOrbFocus.setAnimationListener(new AnimateOrbFocusListener());
        this.mAnimOrbUnfocus.setAnimationListener(new AnimateFadeListener(this.mOrbView));
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mRootView.getVisibility() == 0) {
            animateStandby();
        } else {
            animateIn();
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        if (!Util.startActivitySafely(this.mContext, this.mSearchIntent) || this.mListener == null) {
            return;
        }
        animateOut();
        this.mListener.onSearchLaunched();
    }

    @Override // com.dismal.android.leanbacklauncher.SearchView
    public void animateIn() {
        this.mAnimationLock = true;
        reset();
        this.mAnimIn.reset();
        this.mRootView.setVisibility(0);
        this.mBarView.startAnimation(this.mAnimIn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void animateStandby() {
        if (hasFocus()) {
            animateFocus(true);
        } else {
            reset();
        }
    }

    private void reset() {
        this.mHintView.setVisibility(4);
        this.mGView.setVisibility(0);
        this.mOrbView.setVisibility(4);
        this.mMicView.setImageResource(R.drawable.ic_search_mic_out_normal);
        this.mOrbView.clearAnimation();
        this.mHintView.clearAnimation();
        this.mBgView.setAnimation(this.mAnimBgUnfocus);
        this.mGView.setAnimation(this.mAnimGUnfocus);
        this.mMicView.setAnimation(this.mAnimMicUnfocus);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void animateBreathing() {
        this.mOrbView.clearAnimation();
        this.mAnimOrbBreathe.reset();
        this.mOrbView.setVisibility(0);
        this.mOrbView.startAnimation(this.mAnimOrbBreathe);
    }

    private void animateFocus(boolean gainFocus) {
        if (this.mAnimationLock) {
            return;
        }
        this.mBgView.clearAnimation();
        this.mHintView.clearAnimation();
        this.mGView.clearAnimation();
        this.mOrbView.clearAnimation();
        this.mMicView.clearAnimation();
        if (gainFocus) {
            this.mAnimBgFocus.reset();
            this.mAnimHintFocus.reset();
            this.mAnimGFocus.reset();
            this.mAnimOrbFocus.reset();
            this.mAnimMicFocus.reset();
            this.mHintView.setVisibility(0);
            this.mOrbView.setVisibility(0);
            this.mMicView.setImageResource(R.drawable.ic_search_mic_out_focused);
            this.mBgView.startAnimation(this.mAnimBgFocus);
            this.mHintView.startAnimation(this.mAnimHintFocus);
            this.mGView.startAnimation(this.mAnimGFocus);
            this.mOrbView.startAnimation(this.mAnimOrbFocus);
            this.mMicView.startAnimation(this.mAnimMicFocus);
            return;
        }
        this.mAnimBgUnfocus.reset();
        this.mAnimHintUnfocus.reset();
        this.mAnimGUnfocus.reset();
        this.mAnimOrbUnfocus.reset();
        this.mAnimMicUnfocus.reset();
        this.mGView.setVisibility(0);
        this.mMicView.setImageResource(R.drawable.ic_search_mic_out_normal);
        this.mBgView.startAnimation(this.mAnimBgUnfocus);
        this.mHintView.startAnimation(this.mAnimHintUnfocus);
        this.mGView.startAnimation(this.mAnimGUnfocus);
        this.mOrbView.startAnimation(this.mAnimOrbUnfocus);
        this.mMicView.startAnimation(this.mAnimMicUnfocus);
    }

    private void animateOut() {
        this.mBarView.clearAnimation();
        this.mAnimOut.reset();
        this.mBarView.startAnimation(this.mAnimOut);
    }

    @Override // android.view.View
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            sendAccessibilityEvent(8);
        }
        if (this.mRootView.getVisibility() != 0) {
            return;
        }
        animateFocus(gainFocus);
    }

    private class AnimateInListener implements Animation.AnimationListener {
        /* synthetic */ AnimateInListener(SearchBarView this$0, AnimateInListener animateInListener) {
            this();
        }

        private AnimateInListener() {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            SearchBarView.this.mAnimationLock = false;
            SearchBarView.this.animateStandby();
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private class AnimateOrbFocusListener implements Animation.AnimationListener {
        /* synthetic */ AnimateOrbFocusListener(SearchBarView this$0, AnimateOrbFocusListener animateOrbFocusListener) {
            this();
        }

        private AnimateOrbFocusListener() {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            if (!SearchBarView.this.hasFocus()) {
                return;
            }
            SearchBarView.this.animateBreathing();
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private class AnimateFadeListener implements Animation.AnimationListener {
        private View mView;

        public AnimateFadeListener(View v) {
            this.mView = v;
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            this.mView.setVisibility(4);
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
