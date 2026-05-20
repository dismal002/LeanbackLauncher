package com.dismal.android.leanbacklauncher.apps;

import android.content.Context;
import android.graphics.Rect;
import androidx.leanback.widget.BaseCardView;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.dismal.android.leanbacklauncher.DimmableItem;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.ParticipatesInLaunchAnimation;
import com.dismal.android.leanbacklauncher.animation.ViewDimmer;
import com.dismal.android.leanbacklauncher.animation.ViewFocusAnimator;

/* JADX INFO: loaded from: classes.dex */
public class SettingsCardView extends BaseCardView implements DimmableItem, ParticipatesInLaunchAnimation {
    private int mAnimDuration;
    private ImageView mCircle;
    private ViewDimmer mDimmer;
    private ViewFocusAnimator mFocusAnimator;
    private ImageView mIcon;

    public SettingsCardView(Context context) {
        this(context, null);
    }

    public SettingsCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimDuration = context.getResources().getInteger(R.integer.item_scale_anim_duration);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mCircle = (ImageView) findViewById(R.id.selection_circle);
        this.mIcon = (ImageView) findViewById(R.id.icon);
        this.mFocusAnimator = new ViewFocusAnimator(this);
        this.mDimmer = new ViewDimmer(this);
        this.mDimmer.addDimTarget(this.mIcon);
        this.mDimmer.addDimTarget(this.mCircle);
        this.mDimmer.setDimLevelImmediate();
    }

    @Override // android.view.View
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        setSelected(gainFocus);
    }

    @Override // androidx.leanback.widget.BaseCardView, android.view.View
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (this.mCircle == null) {
            return;
        }
        this.mCircle.animate().alpha(selected ? 1.0f : 0.0f).setDuration(this.mAnimDuration).start();
    }

    @Override // com.dismal.android.leanbacklauncher.DimmableItem
    public void setDimState(boolean active, boolean immediate) {
        this.mDimmer.setDimState(active, immediate);
    }

    @Override // androidx.leanback.widget.BaseCardView, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
        this.mDimmer.setDimLevelImmediate();
        this.mFocusAnimator.setFocusImmediate(hasFocus());
    }
}
