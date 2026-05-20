package com.dismal.android.leanbacklauncher.apps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.DimmableItem;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.ParticipatesInLaunchAnimation;
import com.dismal.android.leanbacklauncher.animation.ViewDimmer;
import com.dismal.android.leanbacklauncher.animation.ViewFocusAnimator;

/* JADX INFO: loaded from: classes.dex */
public class BannerView extends FrameLayout implements DimmableItem, ParticipatesInLaunchAnimation {
    private ViewDimmer mDimmer;
    private ViewFocusAnimator mFocusAnimator;

    public BannerView(Context context) {
        this(context, null);
    }

    public BannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFocusAnimator = new ViewFocusAnimator(this);
        setClipToOutline(true);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDimmer = new ViewDimmer(this);
        View bannerView = findViewById(R.id.app_banner);
        if (bannerView instanceof ImageView) {
            this.mDimmer.addDimTarget((ImageView) bannerView);
        } else {
            View bannerView2 = findViewById(R.id.banner_icon);
            if (bannerView2 instanceof ImageView) {
                this.mDimmer.addDimTarget((ImageView) bannerView2);
            }
            View bannerLabel = findViewById(R.id.banner_label);
            if (bannerLabel instanceof TextView) {
                this.mDimmer.addDimTarget((TextView) bannerLabel);
            }
            View bannerView3 = findViewById(R.id.input_image);
            if (bannerView3 instanceof ImageView) {
                this.mDimmer.addDimTarget((ImageView) bannerView3);
            }
            View bannerLabel2 = findViewById(R.id.input_label);
            if (bannerLabel2 instanceof TextView) {
                this.mDimmer.addDimTarget((TextView) bannerLabel2);
            }
            if (getBackground() != null) {
                this.mDimmer.addDimTarget(getBackground());
            }
        }
        this.mDimmer.setDimLevelImmediate();
    }

    public void setTextViewColor(int viewId, int color) {
        View view = findViewById(viewId);
        if (!(view instanceof TextView) || this.mDimmer == null) {
            return;
        }
        TextView textView = (TextView) view;
        this.mDimmer.setTargetTextColor(textView, color);
    }

    @Override // com.dismal.android.leanbacklauncher.DimmableItem
    public void setDimState(boolean active, boolean immediate) {
        this.mDimmer.setDimState(active, immediate);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
        this.mDimmer.setDimLevelImmediate();
        this.mFocusAnimator.setFocusImmediate(hasFocus());
    }
}
