package com.dismal.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.R;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ViewDimmer {
    private static ColorFilter[] sFilters;
    private static ColorFilter[] sFiltersDesat;
    private final float mActiveDimLevel;
    private List<ImageView> mDesatImageViews;
    private final int mDimAnimDuration;
    ObjectAnimator mDimAnimation;
    private float mDimLevel;
    private boolean mDimmed;
    private List<Drawable> mDrawables;
    private List<ImageView> mImageViews;
    private final float mInactiveDimLevel;
    private List<Integer> mOriginalTextColors;
    private View mTargetView;
    private List<TextView> mTextViews;

    public ViewDimmer(View view) {
        this.mTargetView = view;
        if (sFilters == null || sFiltersDesat == null) {
            sFilters = new ColorFilter[256];
            sFiltersDesat = new ColorFilter[256];
            ColorMatrix desat = new ColorMatrix();
            desat.setSaturation(0.0f);
            for (int i = 0; i <= 255; i++) {
                float dimVal = 1.0f - (i / 255.0f);
                ColorMatrix dimMatrix = new ColorMatrix();
                dimMatrix.setScale(dimVal, dimVal, dimVal, 1.0f);
                sFilters[i] = new ColorMatrixColorFilter(dimMatrix);
                ColorMatrix dimDesatMatrix = new ColorMatrix();
                dimDesatMatrix.setScale(dimVal, dimVal, dimVal, 1.0f);
                dimDesatMatrix.postConcat(desat);
                sFiltersDesat[i] = new ColorMatrixColorFilter(dimDesatMatrix);
            }
        }
        TypedValue out = new TypedValue();
        this.mTargetView.getResources().getValue(R.dimen.launcher_active_dim_level, out, true);
        this.mActiveDimLevel = out.getFloat();
        this.mTargetView.getResources().getValue(R.dimen.launcher_inactive_dim_level, out, true);
        this.mInactiveDimLevel = out.getFloat();
        this.mDimAnimDuration = this.mTargetView.getResources().getInteger(R.integer.item_dim_anim_duration);
        this.mDimAnimation = ObjectAnimator.ofFloat(this, "dimLevel", this.mInactiveDimLevel);
        this.mDimAnimation.setDuration(this.mDimAnimDuration);
        this.mDimAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.animation.ViewDimmer.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                ViewDimmer.this.mTargetView.setHasTransientState(true);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                ViewDimmer.this.mTargetView.setHasTransientState(false);
            }
        });
    }

    static int getDimmedColor(int color, float level) {
        float factor = 1.0f - level;
        return Color.argb(Color.alpha(color), (int) (Color.red(color) * factor), (int) (Color.green(color) * factor), (int) (Color.blue(color) * factor));
    }

    public void setDimLevel(float level) {
        this.mDimLevel = level;
        ColorFilter filter = null;
        ColorFilter desatFilter = null;
        if ((this.mImageViews != null || this.mDrawables != null) && this.mDimLevel > 0.0f && this.mDimLevel <= 1.0f) {
            int filterIndex = (int) (255.0f * level);
            filter = sFilters[filterIndex];
        }
        if (this.mDesatImageViews != null && this.mDimLevel >= 0.0f && this.mDimLevel <= 1.0f) {
            int filterIndex2 = (int) (255.0f * level);
            desatFilter = sFiltersDesat[filterIndex2];
        }
        if (this.mImageViews != null) {
            int size = this.mImageViews.size();
            for (int i = 0; i < size; i++) {
                this.mImageViews.get(i).setColorFilter(filter);
            }
        }
        if (this.mDesatImageViews != null) {
            int size2 = this.mDesatImageViews.size();
            for (int i2 = 0; i2 < size2; i2++) {
                this.mDesatImageViews.get(i2).setColorFilter(desatFilter);
            }
        }
        if (this.mDrawables != null) {
            int size3 = this.mDrawables.size();
            for (int i3 = 0; i3 < size3; i3++) {
                this.mDrawables.get(i3).setColorFilter(filter);
            }
        }
        if (this.mTextViews == null) {
            return;
        }
        int size4 = this.mTextViews.size();
        for (int i4 = 0; i4 < size4; i4++) {
            this.mTextViews.get(i4).setTextColor(getDimmedColor(this.mOriginalTextColors.get(i4).intValue(), level));
        }
    }

    public float getDimLevel() {
        return this.mDimLevel;
    }

    public void animateDim(boolean active) {
        if (this.mDimAnimation.isStarted()) {
            this.mDimAnimation.cancel();
        }
        float end = active ? this.mActiveDimLevel : this.mInactiveDimLevel;
        if (getDimLevel() == end) {
            return;
        }
        this.mDimAnimation.setFloatValues(getDimLevel(), end);
        this.mDimAnimation.start();
    }

    public void setDimLevelImmediate(boolean active) {
        if (this.mDimAnimation.isStarted()) {
            this.mDimAnimation.cancel();
        }
        float level = active ? this.mActiveDimLevel : this.mInactiveDimLevel;
        setDimLevel(level);
    }

    public void setDimLevelImmediate() {
        setDimLevelImmediate(this.mDimmed);
    }

    public void setDimState(boolean active, boolean immediate) {
        if (!immediate) {
            animateDim(active);
        } else {
            setDimLevelImmediate(active);
        }
        this.mDimmed = active;
    }

    public void addDimTarget(ImageView view) {
        if (this.mImageViews == null) {
            this.mImageViews = new ArrayList(4);
        }
        this.mImageViews.add(view);
    }

    public void addDesatDimTarget(ImageView view) {
        if (this.mDesatImageViews == null) {
            this.mDesatImageViews = new ArrayList(4);
        }
        this.mDesatImageViews.add(view);
    }

    public void addDimTarget(TextView view) {
        if (this.mTextViews == null) {
            this.mTextViews = new ArrayList(4);
        }
        if (this.mOriginalTextColors == null) {
            this.mOriginalTextColors = new ArrayList(4);
        }
        this.mTextViews.add(view);
        this.mOriginalTextColors.add(Integer.valueOf(view.getCurrentTextColor()));
    }

    public void setTargetTextColor(TextView view, int newColor) {
        int index;
        if (this.mTextViews == null || this.mOriginalTextColors == null || (index = this.mTextViews.indexOf(view)) < 0) {
            return;
        }
        this.mOriginalTextColors.set(index, Integer.valueOf(newColor));
        view.setTextColor(getDimmedColor(newColor, this.mDimLevel));
    }

    public void addDimTarget(Drawable drawable) {
        if (this.mDrawables == null) {
            this.mDrawables = new ArrayList(4);
        }
        this.mDrawables.add(drawable);
    }
}
