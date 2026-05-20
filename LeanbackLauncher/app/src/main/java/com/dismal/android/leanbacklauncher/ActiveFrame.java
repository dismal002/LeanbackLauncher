package com.dismal.android.leanbacklauncher;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.ActiveItemsRowView;
import com.dismal.android.leanbacklauncher.HomeScrollManager;
import com.dismal.android.leanbacklauncher.animation.ViewDimmer;
import com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper;

/* JADX INFO: loaded from: classes.dex */
public class ActiveFrame extends LinearLayout implements HomeScrollManager.HomeScrollFractionListener, ActiveItemsRowView.RowCountChangeListener {
    protected boolean mActivated;
    protected float mActiveTextMargin;
    private int mAnimDuration;
    protected float mBottomPadding;
    protected float mCardSpacing;
    private ViewDimmer mDimmer;
    private float mDownscaleFactor;
    private RowExpandAnimation mExpandAnim;
    private float mExpanded;
    protected View mHeader;
    protected float mHeaderHeight;
    protected ActiveItemsRowView mRow;
    private int mRowAlign;
    protected float mRowMinSpacing;
    protected float mRowPadding;
    private boolean mScalesWhenUnfocused;

    public ActiveFrame(Context context) {
        this(context, null);
    }

    public ActiveFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActiveFrame(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mActivated = false;
        this.mScalesWhenUnfocused = false;
        this.mExpanded = 1.0f;
        this.mRowAlign = 0;
        setDescendantFocusability(262144);
        this.mHeaderHeight = getResources().getDimension(R.dimen.header_height);
        this.mActiveTextMargin = getResources().getDimension(R.dimen.header_text_active_margin_extra);
        this.mAnimDuration = getResources().getInteger(R.integer.item_scale_anim_duration);
        this.mBottomPadding = getResources().getDimension(R.dimen.group_vertical_spacing);
        this.mRowMinSpacing = getResources().getDimension(R.dimen.inter_card_spacing);
        this.mRowPadding = getResources().getDimension(R.dimen.row_padding);
        this.mCardSpacing = getResources().getDimension(R.dimen.card_spacing);
        TypedValue out = new TypedValue();
        getResources().getValue(R.dimen.inactive_banner_scale_down_ammount, out, true);
        this.mDownscaleFactor = out.getFloat();
        if (this.mDownscaleFactor < 0.0f || this.mDownscaleFactor >= 1.0f) {
            this.mDownscaleFactor = 0.0f;
        }
        this.mRowMinSpacing *= 1.0f - this.mDownscaleFactor;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        int count = getChildCount();
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            View view = getChildAt(i);
            if (view instanceof ActiveItemsRowView) {
                this.mRow = (ActiveItemsRowView) view;
                break;
            } else if (!(view instanceof NotificationViewFlipper)) {
                i++;
            } else {
                this.mRow = ((NotificationViewFlipper) view).getNotificationRow();
                break;
            }
        }
        if (this.mRow != null) {
            this.mRow.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.dismal.android.leanbacklauncher.ActiveFrame.1
                @Override // android.view.View.OnLayoutChangeListener
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    ActiveFrame.this.updateRow(left, right);
                }
            });
        }
        this.mHeader = findViewById(R.id.header);
        if (this.mHeader == null) {
            return;
        }
        this.mDimmer = new ViewDimmer(this);
        TextView title = (TextView) this.mHeader.findViewById(R.id.title);
        if (title != null) {
            this.mDimmer.addDimTarget(title);
        }
        ImageView icon = (ImageView) this.mHeader.findViewById(R.id.icon);
        if (icon != null) {
            this.mDimmer.addDimTarget(icon);
        }
        this.mDimmer.setDimState(this.mActivated, true);
    }

    @Override // android.view.View
    public void setActivated(boolean activated) {
        this.mActivated = activated;
        if (this.mDimmer != null) {
            this.mDimmer.setDimState(activated, false);
        }
        if (this.mRow == null) {
            return;
        }
        this.mRow.setActivated(activated);
        if (!this.mScalesWhenUnfocused) {
            return;
        }
        setRowState(activated, hasWindowFocus());
    }

    public void setScaledWhenUnfocused(boolean scalingEnabled) {
        this.mScalesWhenUnfocused = scalingEnabled;
        if (this.mScalesWhenUnfocused) {
            setRowState(this.mActivated, false);
        } else {
            setRowState(true, false);
        }
    }

    @Override // com.dismal.android.leanbacklauncher.HomeScrollManager.HomeScrollFractionListener
    public void onScrollPositionChanged(int position, float fractionFromTop) {
        if (this.mHeader != null) {
            ViewGroup.LayoutParams lp = this.mHeader.getLayoutParams();
            int height = (int) (this.mHeaderHeight * fractionFromTop);
            this.mHeader.setAlpha(fractionFromTop);
            if (lp.height != height) {
                lp.height = height;
                this.mHeader.setLayoutParams(lp);
            }
        }
        int padding = Math.round(((this.mBottomPadding - this.mRowMinSpacing) * fractionFromTop) + this.mRowMinSpacing);
        setPadding(0, 0, 0, padding);
    }

    @Override // com.dismal.android.leanbacklauncher.ActiveItemsRowView.RowCountChangeListener
    public void onRowCountChanged() {
        setExpandedFraction(this.mExpanded);
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mRow == null || this.mRow.getScaleY() >= 1.0f) {
            return;
        }
        int heightDelta = (int) (this.mRow.getMeasuredHeight() * (1.0f - this.mRow.getScaleY()));
        int height = getMeasuredHeight() - heightDelta;
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    public void resetScrollPosition(boolean smooth) {
        if (this.mRow == null || this.mRow.getSelectedPosition() == 0) {
            return;
        }
        if (smooth) {
            this.mRow.setSelectedPositionSmooth(0);
        } else {
            this.mRow.setSelectedPosition(0);
        }
    }

    public void resetRowIfNeeded(boolean force) {
        this.mRow.resetRowIfNeeded(force);
    }

    private void setRowState(boolean expanded, boolean animate) {
        if (this.mExpandAnim != null) {
            this.mExpandAnim.cancel();
            this.mExpandAnim = null;
        }
        if (!animate || !isAttachedToWindow() || getVisibility() != 0) {
            if (expanded) {
                setExpandedFraction(1.0f);
                return;
            } else {
                setExpandedFraction(0.0f);
                return;
            }
        }
        this.mExpandAnim = new RowExpandAnimation(this.mExpanded, expanded ? 1.0f : 0.0f);
        startAnimation(this.mExpandAnim);
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        adjustRowDimensions(w);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setExpandedFraction(float fraction) {
        this.mExpanded = fraction;
        adjustRowDimensions(getMeasuredWidth());
        if (this.mHeader == null || !(this.mHeader.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mHeader.getLayoutParams();
        int margin = (int) (this.mActiveTextMargin * fraction);
        if (lp.bottomMargin == margin) {
            return;
        }
        lp.bottomMargin = margin;
        this.mHeader.setLayoutParams(lp);
    }

    private void adjustRowDimensions(int frameWidth) {
        if (this.mRow == null) {
            return;
        }
        boolean isScaled = this.mExpanded < 1.0f;
        int rowLength = (int) (frameWidth / (isScaled ? 1.0f - this.mDownscaleFactor : 1.0f));
        ViewGroup.LayoutParams p = this.mRow.getLayoutParams();
        if (p.width != rowLength) {
            p.width = rowLength;
            this.mRow.setLayoutParams(p);
        } else {
            updateRow(this.mRow.getLeft(), this.mRow.getRight());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRow(int left, int right) {
        float deltaStart;
        float deltaEnd;
        RecyclerView.ViewHolder holder;
        boolean isScaled = this.mExpanded < 1.0f;
        float scale = 1.0f - ((1.0f - this.mExpanded) * this.mDownscaleFactor);
        float unfocusedScale = 1.0f - this.mDownscaleFactor;
        boolean useRtl = getLayoutDirection() == 1;
        if (this.mRow == null) {
            return;
        }
        int rowLength = right - left;
        int deltaW = rowLength - getMeasuredWidth();
        float usableSpace = rowLength - (this.mRowPadding * 2.0f);
        int itemCount = this.mRow.getAdapter().getItemCount();
        int selected = this.mRow.getSelectedPosition();
        int numRows = this.mRow.getNumRows();
        if (numRows <= 0) {
            numRows = 1;
        }
        int numCol = (int) Math.ceil(itemCount / numRows);
        int selectedCol = (int) Math.floor(this.mRow.getSelectedPosition() / numRows);
        View selectedView = null;
        if (itemCount > 0 && selected >= 0 && (holder = this.mRow.findViewHolderForAdapterPosition(selected)) != null) {
            selectedView = holder.itemView;
        }
        if (selectedView != null) {
            float viewLength = selectedView.getMeasuredWidth();
            float totalLength = (itemCount * viewLength) + (this.mCardSpacing * (itemCount - 1));
            float distFromStart = ((this.mCardSpacing + viewLength) * selectedCol) + (0.5f * viewLength);
            float distFromEnd = ((this.mCardSpacing + viewLength) * ((numCol - selectedCol) - 1)) + (0.5f * viewLength);
            if (totalLength < getMeasuredWidth() - (this.mRowPadding * 2.0f) || distFromStart <= getMeasuredWidth() / 2) {
                this.mRowAlign = 0;
            } else if (distFromEnd < getMeasuredWidth() / 2) {
                this.mRowAlign = 2;
            } else {
                this.mRowAlign = 1;
            }
            float selectCtr = selectedView.getX() + (viewLength / 2.0f);
            if (isScaled) {
                if (this.mRowAlign == 0) {
                    this.mRow.setPivotX(useRtl ? rowLength - this.mRowPadding : this.mRowPadding);
                    this.mRow.setTranslationX(0.0f);
                } else if (this.mRowAlign == 1) {
                    this.mRow.setPivotX(useRtl ? rowLength - selectCtr : selectCtr);
                    float deltaStart2 = (distFromStart * unfocusedScale) - ((getMeasuredWidth() / 2.0f) - this.mRowPadding);
                    if (deltaStart2 > 0.0f) {
                        deltaStart = 0.0f;
                    } else {
                        deltaStart = deltaStart2 * (1.0f - this.mExpanded);
                    }
                    float deltaEnd2 = (distFromEnd * unfocusedScale) - ((getMeasuredWidth() / 2.0f) - this.mRowPadding);
                    if (deltaEnd2 > 0.0f) {
                        deltaEnd = 0.0f;
                    } else {
                        deltaEnd = deltaEnd2 * (1.0f - this.mExpanded);
                    }
                    float centerOffset = 0.0f;
                    if (deltaStart < 0.0f) {
                        centerOffset = -deltaStart;
                    } else if (deltaEnd < 0.0f) {
                        centerOffset = deltaEnd;
                    }
                    this.mRow.setTranslationX((useRtl ? -1 : 1) * (((getMeasuredWidth() / 2.0f) - selectCtr) - centerOffset));
                } else if (totalLength <= usableSpace) {
                    float deltaX = ((getMeasuredWidth() - (this.mRowPadding * 2.0f)) - totalLength) * (useRtl ? -1 : 1);
                    this.mRow.setPivotX(useRtl ? rowLength - this.mRowPadding : this.mRowPadding);
                    this.mRow.setTranslationX(this.mExpanded * deltaX);
                } else {
                    this.mRow.setPivotX(useRtl ? this.mRowPadding : rowLength - this.mRowPadding);
                    this.mRow.setTranslationX((useRtl ? 1 : -1) * deltaW);
                }
            } else {
                this.mRow.setPivotX(useRtl ? rowLength - this.mRowPadding : this.mRowPadding);
                this.mRow.setTranslationX(0.0f);
            }
        } else {
            this.mRowAlign = 0;
            this.mRow.setPivotX(useRtl ? rowLength - this.mRowPadding : this.mRowPadding);
            this.mRow.setTranslationX(0.0f);
        }
        this.mRow.setPivotY(0.0f);
        this.mRow.setScaleX(scale);
        this.mRow.setScaleY(scale);
    }

    private class RowExpandAnimation extends Animation {
        private float mDelta;
        private float mStartValue;

        public RowExpandAnimation(float start, float end) {
            this.mStartValue = start;
            this.mDelta = end - start;
            setDuration(ActiveFrame.this.mAnimDuration);
            Interpolator inter = AnimationUtils.loadInterpolator(ActiveFrame.this.getContext(), android.R.interpolator.fast_out_slow_in);
            setInterpolator(inter);
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            ActiveFrame.this.setExpandedFraction(this.mStartValue + (this.mDelta * interpolatedTime));
        }
    }
}
