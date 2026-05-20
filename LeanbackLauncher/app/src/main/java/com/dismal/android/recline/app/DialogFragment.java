package com.dismal.android.recline.app;

import com.dismal.android.leanbacklauncher.R;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.leanback.widget.VerticalGridView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class DialogFragment extends Fragment {
    private ArrayList<Action> mActions;
    private DialogActionAdapter mAdapter;
    private String mBreadcrumb;
    private String mDescription;
    private boolean mEntryTransitionEnabled;
    private boolean mEntryTransitionPerformed;
    private Bitmap mIconBitmap;
    private int mIconPadding;
    private int mIconResourceId;
    private Uri mIconUri;
    private boolean mIntroAnimationInProgress;
    private VerticalGridView mListView;
    private Action.Listener mListener;
    private String mName;
    private String mTitle;
    private int mIconBackgroundColor = 0;
    private int mSelectedIndex = -1;

    public static class Builder {
        private ArrayList<Action> mActions;
        private String mContentBreadcrumb;
        private String mContentDescription;
        private String mContentTitle;
        private Bitmap mIconBitmap;
        private int mIconPadding;
        private int mIconResourceId;
        private Uri mIconUri;
        private String mName;
        private int mSelectedIndex;
        private int mIconBackgroundColor = 0;
        private boolean mEntryTransitionEnabled = true;

        public DialogFragment build() {
            DialogFragment fragment = new DialogFragment();
            Bundle args = new Bundle();
            args.putString("title", this.mContentTitle);
            args.putString("breadcrumb", this.mContentBreadcrumb);
            args.putString("description", this.mContentDescription);
            args.putInt("iconResourceId", this.mIconResourceId);
            args.putParcelable("iconUri", this.mIconUri);
            args.putParcelable("iconBitmap", this.mIconBitmap);
            args.putInt("iconBackground", this.mIconBackgroundColor);
            args.putInt("iconPadding", this.mIconPadding);
            args.putParcelableArrayList("actions", this.mActions);
            args.putString("name", this.mName);
            args.putInt("selectedIndex", this.mSelectedIndex);
            args.putBoolean("entryTransitionEnabled", this.mEntryTransitionEnabled);
            fragment.setArguments(args);
            return fragment;
        }

        public Builder title(String title) {
            this.mContentTitle = title;
            return this;
        }

        public Builder breadcrumb(String breadcrumb) {
            this.mContentBreadcrumb = breadcrumb;
            return this;
        }

        public Builder description(String description) {
            this.mContentDescription = description;
            return this;
        }

        public Builder iconResourceId(int iconResourceId) {
            this.mIconResourceId = iconResourceId;
            return this;
        }

        public Builder iconBackgroundColor(int iconBackgroundColor) {
            this.mIconBackgroundColor = iconBackgroundColor;
            return this;
        }

        public Builder actions(ArrayList<Action> actions) {
            this.mActions = actions;
            return this;
        }
    }

    public static void add(FragmentManager fm, DialogFragment f) {
        add(fm, f, android.R.id.content);
    }

    public static void add(FragmentManager fm, DialogFragment f, int id) {
        boolean hasDialog = getCurrentDialogFragment(fm) != null;
        FragmentTransaction ft = fm.beginTransaction();
        if (hasDialog) {
            ft.setCustomAnimations(1, 2, 3, 4);
            ft.addToBackStack(null);
        }
        ft.replace(id, f, "leanBackDialogFragment").commit();
    }

    public static DialogFragment getCurrentDialogFragment(FragmentManager fm) {
        Fragment f = fm.findFragmentByTag("leanBackDialogFragment");
        if (f instanceof DialogFragment) {
            return (DialogFragment) f;
        }
        return null;
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        Log.v("DialogFragment", "onCreate");
        super.onCreate(savedInstanceState);
        Bundle state = savedInstanceState != null ? savedInstanceState : getArguments();
        if (this.mTitle == null) {
            this.mTitle = state.getString("title");
        }
        if (this.mBreadcrumb == null) {
            this.mBreadcrumb = state.getString("breadcrumb");
        }
        if (this.mDescription == null) {
            this.mDescription = state.getString("description");
        }
        if (this.mIconResourceId == 0) {
            this.mIconResourceId = state.getInt("iconResourceId", 0);
        }
        if (this.mIconUri == null) {
            this.mIconUri = (Uri) state.getParcelable("iconUri");
        }
        if (this.mIconBitmap == null) {
            this.mIconBitmap = (Bitmap) state.getParcelable("iconBitmap");
        }
        if (this.mIconBackgroundColor == 0) {
            this.mIconBackgroundColor = state.getInt("iconBackground", 0);
        }
        if (this.mIconPadding == 0) {
            this.mIconPadding = state.getInt("iconPadding", 0);
        }
        if (this.mActions == null) {
            this.mActions = state.getParcelableArrayList("actions");
        }
        if (this.mName == null) {
            this.mName = state.getString("name");
        }
        if (this.mSelectedIndex == -1) {
            this.mSelectedIndex = state.getInt("selectedIndex", -1);
        }
        this.mEntryTransitionEnabled = state.getBoolean("entryTransitionEnabled", true);
        this.mEntryTransitionPerformed = state.getBoolean("entryTransitionPerformed", false);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lb_dialog_fragment, container, false);
        View contentContainer = v.findViewById(R.id.content_fragment);
        View content = inflater.inflate(getContentLayoutId(), container, false);
        ((ViewGroup) contentContainer).addView(content);
        setContentView(content);
        v.setTag(R.id.content_fragment, content);
        View actionContainer = v.findViewById(R.id.action_fragment);
        View action = inflater.inflate(R.layout.lb_dialog_action_list, container, false);
        ((ViewGroup) actionContainer).addView(action);
        setActionView(action);
        v.setTag(R.id.action_fragment, action);
        final View selectorView = action.findViewById(R.id.selector);
        if (selectorView != null) {
            this.mListView.getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() { // from class: com.dismal.android.recline.app.DialogFragment.1
                private boolean mChildFocused;

                @Override // android.view.ViewTreeObserver.OnGlobalFocusChangeListener
                public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                    View focusedChild = DialogFragment.this.mListView.getFocusedChild();
                    if (focusedChild == null) {
                        selectorView.setVisibility(4);
                        this.mChildFocused = false;
                    } else {
                        if (this.mChildFocused) {
                            return;
                        }
                        this.mChildFocused = true;
                        selectorView.setVisibility(0);
                        if (DialogFragment.this.isIntroAnimationInProgress()) {
                            return;
                        }
                        DialogFragment.this.onIntroAnimationFinished();
                    }
                }
            });
        }
        return v;
    }

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", this.mTitle);
        outState.putString("breadcrumb", this.mBreadcrumb);
        outState.putString("description", this.mDescription);
        outState.putInt("iconResourceId", this.mIconResourceId);
        outState.putParcelable("iconUri", this.mIconUri);
        outState.putParcelable("iconBitmap", this.mIconBitmap);
        outState.putInt("iconBackground", this.mIconBackgroundColor);
        outState.putInt("iconPadding", this.mIconPadding);
        outState.putParcelableArrayList("actions", this.mActions);
        outState.putInt("selectedIndex", this.mListView != null ? getSelectedItemPosition() : this.mSelectedIndex);
        outState.putString("name", this.mName);
        outState.putBoolean("entryTransitionPerformed", this.mEntryTransitionPerformed);
    }

    @Override // android.app.Fragment
    public void onStart() {
        super.onStart();
        if (!isEntryTransitionEnabled() || this.mEntryTransitionPerformed) {
            return;
        }
        this.mEntryTransitionPerformed = true;
        performEntryTransition();
    }

    @Override // android.app.Fragment
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        View dialogView = getView();
        View contentView = (View) dialogView.getTag(R.id.content_fragment);
        View actionView = (View) dialogView.getTag(R.id.action_fragment);
        View actionContainerView = dialogView.findViewById(R.id.action_fragment);
        View listView = (View) actionView.getTag(R.id.list);
        View selectorView = (View) actionView.getTag(R.id.selector);
        ArrayList<Animator> animators = new ArrayList<>();
        addContentViewAnimations(contentView, nextAnim, animators);
        switch (nextAnim) {
            case 1:
                animators.add(createSlideInFromEndAnimator(listView));
                animators.add(createSlideInFromEndAnimator(selectorView));
                break;
            case 2:
                animators.add(createSlideOutToStartAnimator(listView));
                animators.add(createSlideOutToStartAnimator(selectorView));
                animators.add(createFadeOutAnimator(actionContainerView));
                break;
            case 3:
                animators.add(createSlideInFromStartAnimator(listView));
                animators.add(createSlideInFromStartAnimator(selectorView));
                break;
            case 4:
                animators.add(createSlideOutToEndAnimator(listView));
                animators.add(createSlideOutToEndAnimator(selectorView));
                animators.add(createFadeOutAnimator(actionContainerView));
                break;
            default:
                return super.onCreateAnimator(transit, enter, nextAnim);
        }
        this.mEntryTransitionPerformed = true;
        return createDummyAnimator(dialogView, animators);
    }

    protected boolean isEntryTransitionEnabled() {
        return this.mEntryTransitionEnabled;
    }

    protected void addContentViewAnimations(View contentView, int nextAnim, ArrayList<Animator> animators) {
        View titleView = (View) contentView.getTag(R.id.title);
        View breadcrumbView = (View) contentView.getTag(R.id.breadcrumb);
        View descriptionView = (View) contentView.getTag(R.id.description);
        View iconView = (View) contentView.getTag(R.id.icon);
        switch (nextAnim) {
            case 1:
                animators.add(createSlideInFromEndAnimator(titleView));
                animators.add(createSlideInFromEndAnimator(breadcrumbView));
                animators.add(createSlideInFromEndAnimator(descriptionView));
                animators.add(createSlideInFromEndAnimator(iconView));
                break;
            case 2:
                animators.add(createSlideOutToStartAnimator(titleView));
                animators.add(createSlideOutToStartAnimator(breadcrumbView));
                animators.add(createSlideOutToStartAnimator(descriptionView));
                animators.add(createSlideOutToStartAnimator(iconView));
                break;
            case 3:
                animators.add(createSlideInFromStartAnimator(titleView));
                animators.add(createSlideInFromStartAnimator(breadcrumbView));
                animators.add(createSlideInFromStartAnimator(descriptionView));
                animators.add(createSlideInFromStartAnimator(iconView));
                break;
            case 4:
                animators.add(createSlideOutToEndAnimator(titleView));
                animators.add(createSlideOutToEndAnimator(breadcrumbView));
                animators.add(createSlideOutToEndAnimator(descriptionView));
                animators.add(createSlideOutToEndAnimator(iconView));
                break;
        }
    }

    public ArrayList<Action> getActions() {
        return this.mActions;
    }

    public void setActions(ArrayList<Action> actions) {
        this.mActions = actions;
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.setActions(this.mActions);
    }

    public int getSelectedItemPosition() {
        return this.mListView.indexOfChild(this.mListView.getFocusedChild());
    }

    public void onIntroAnimationFinished() {
        this.mIntroAnimationInProgress = false;
        View focusedChild = this.mListView.getFocusedChild();
        if (focusedChild == null) {
            return;
        }
        View actionView = (View) getView().getTag(R.id.action_fragment);
        int height = focusedChild.getHeight();
        View selectorView = actionView.findViewById(R.id.selector);
        ViewGroup.LayoutParams lp = selectorView.getLayoutParams();
        lp.height = height;
        selectorView.setLayoutParams(lp);
        selectorView.setAlpha(1.0f);
    }

    public boolean isIntroAnimationInProgress() {
        return this.mIntroAnimationInProgress;
    }

    protected int getContentLayoutId() {
        return R.layout.lb_dialog_content;
    }

    protected void setContentView(View content) {
        TextView titleView = (TextView) content.findViewById(R.id.title);
        TextView breadcrumbView = (TextView) content.findViewById(R.id.breadcrumb);
        TextView descriptionView = (TextView) content.findViewById(R.id.description);
        titleView.setText(this.mTitle);
        breadcrumbView.setText(this.mBreadcrumb);
        descriptionView.setText(this.mDescription);
        ImageView iconImageView = (ImageView) content.findViewById(R.id.icon);
        if (this.mIconBackgroundColor != 0) {
            iconImageView.setBackgroundColor(this.mIconBackgroundColor);
        }
        iconImageView.setPadding(this.mIconPadding, this.mIconPadding, this.mIconPadding, this.mIconPadding);
        if (this.mIconResourceId != 0) {
            iconImageView.setImageResource(this.mIconResourceId);
            updateViewSize(iconImageView);
        } else if (this.mIconBitmap != null) {
            iconImageView.setImageBitmap(this.mIconBitmap);
            updateViewSize(iconImageView);
        } else if (this.mIconUri != null) {
            iconImageView.setVisibility(4);
        } else {
            iconImageView.setVisibility(8);
        }
        content.setTag(R.id.title, titleView);
        content.setTag(R.id.breadcrumb, breadcrumbView);
        content.setTag(R.id.description, descriptionView);
        content.setTag(R.id.icon, iconImageView);
    }

    private void setActionView(View action) {
        this.mAdapter = new DialogActionAdapter(new Action.Listener() { // from class: com.dismal.android.recline.app.DialogFragment.2
            @Override // com.dismal.android.recline.app.DialogFragment.Action.Listener
            public void onActionClicked(Action action2) {
                if (!action2.isEnabled() || action2.infoOnly()) {
                    return;
                }
                if (DialogFragment.this.mListener != null) {
                    DialogFragment.this.mListener.onActionClicked(action2);
                } else {
                    if (!(DialogFragment.this.getActivity() instanceof Action.Listener)) {
                        return;
                    }
                    Action.Listener listener = (Action.Listener) DialogFragment.this.getActivity();
                    listener.onActionClicked(action2);
                }
            }
        }, new Action.OnFocusListener() { // from class: com.dismal.android.recline.app.DialogFragment.3
            @Override // com.dismal.android.recline.app.DialogFragment.Action.OnFocusListener
            public void onActionFocused(Action action2) {
                if (!(DialogFragment.this.getActivity() instanceof Action.OnFocusListener)) {
                    return;
                }
                Action.OnFocusListener listener = (Action.OnFocusListener) DialogFragment.this.getActivity();
                listener.onActionFocused(action2);
            }
        }, this.mActions);
        if (action instanceof VerticalGridView) {
            this.mListView = (VerticalGridView) action;
        } else {
            this.mListView = (VerticalGridView) action.findViewById(R.id.list);
            if (this.mListView == null) {
                throw new IllegalStateException("No ListView exists.");
            }
            this.mListView.setWindowAlignmentOffset(0);
            this.mListView.setWindowAlignmentOffsetPercent(50.0f);
            this.mListView.setWindowAlignment(0);
            View selectorView = action.findViewById(R.id.selector);
            if (selectorView != null) {
                this.mListView.setOnScrollListener(new SelectorAnimator(selectorView, this.mListView));
            }
        }
        this.mListView.requestFocusFromTouch();
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setSelectedPosition((this.mSelectedIndex < 0 || this.mSelectedIndex >= this.mActions.size()) ? getFirstCheckedAction() : this.mSelectedIndex);
        action.setTag(R.id.list, this.mListView);
        action.setTag(R.id.selector, action.findViewById(R.id.selector));
    }

    private int getFirstCheckedAction() {
        int size = this.mActions.size();
        for (int i = 0; i < size; i++) {
            if (this.mActions.get(i).isChecked()) {
                return i;
            }
        }
        return 0;
    }

    private void updateViewSize(ImageView iconView) {
        int intrinsicWidth = iconView.getDrawable().getIntrinsicWidth();
        ViewGroup.LayoutParams lp = iconView.getLayoutParams();
        if (intrinsicWidth > 0) {
            lp.height = (lp.width * iconView.getDrawable().getIntrinsicHeight()) / intrinsicWidth;
        } else {
            lp.height = lp.width;
        }
    }

    private void performEntryTransition() {
        final View dialogView = getView();
        final View contentView = (View) dialogView.getTag(R.id.content_fragment);
        final View actionContainerView = dialogView.findViewById(R.id.action_fragment);
        this.mIntroAnimationInProgress = true;
        getActivity().overridePendingTransition(0, R.anim.lb_dialog_fade_out);
        int bgColor = contentView.getContext().getResources().getColor(R.color.lb_dialog_activity_background);
        final ColorDrawable bgDrawable = new ColorDrawable();
        bgDrawable.setColor(bgColor);
        bgDrawable.setAlpha(0);
        dialogView.setBackground(bgDrawable);
        dialogView.setVisibility(4);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.dismal.android.recline.app.DialogFragment.4
            Runnable mEntryAnimationRunnable;

            {
                final View view = dialogView;
                final ColorDrawable colorDrawable = bgDrawable;
                final View view2 = contentView;
                final View view3 = actionContainerView;
                this.mEntryAnimationRunnable = new Runnable() { // from class: com.dismal.android.recline.app.DialogFragment.4.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (!DialogFragment.this.isAdded()) {
                            return;
                        }
                        view.setVisibility(0);
                        ObjectAnimator oa = ObjectAnimator.ofInt(colorDrawable, "alpha", 255);
                        oa.setDuration(250L);
                        oa.setStartDelay(120L);
                        oa.setInterpolator(new DecelerateInterpolator(1.0f));
                        oa.start();
                        DialogFragment.this.animateInContentView(view2);
                        DialogFragment.this.animateInActionView(view3);
                    }
                };
            }

            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                contentView.postOnAnimationDelayed(this.mEntryAnimationRunnable, 550L);
            }
        });
    }

    protected void animateInContentView(View contentView) {
        boolean isRtl = ViewCompat.getLayoutDirection(contentView) == 1;
        int startDist = isRtl ? 120 : -120;
        prepareAndAnimateView((View) contentView.getTag(R.id.title), startDist, false);
        prepareAndAnimateView((View) contentView.getTag(R.id.breadcrumb), startDist, false);
        prepareAndAnimateView((View) contentView.getTag(R.id.description), startDist, false);
        prepareAndAnimateView((View) contentView.getTag(R.id.icon), startDist, false);
    }

    protected void animateInActionView(View actionView) {
        boolean isRtl = ViewCompat.getLayoutDirection(actionView) == 1;
        int endDist = isRtl ? -actionView.getMeasuredWidth() : actionView.getMeasuredWidth();
        prepareAndAnimateView(actionView, endDist, true);
    }

    protected void prepareAndAnimateView(final View v, float initTransX, final boolean notifyAnimationFinished) {
        v.setLayerType(2, null);
        v.buildLayer();
        v.setAlpha(0.0f);
        v.setTranslationX(initTransX);
        v.animate().alpha(1.0f).translationX(0.0f).setDuration(250L).setStartDelay(120L);
        v.animate().setInterpolator(new DecelerateInterpolator(1.0f));
        v.animate().setListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.recline.app.DialogFragment.5
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(0, null);
                if (!notifyAnimationFinished) {
                    return;
                }
                DialogFragment.this.onIntroAnimationFinished();
            }
        });
        v.animate().start();
    }

    private Animator createDummyAnimator(View v, ArrayList<Animator> animators) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        return new UntargetableAnimatorSet(animatorSet);
    }

    protected Animator createSlideOutToStartAnimator(View v) {
        return createTranslateAlphaAnimator(v, R.anim.lb_dialog_slide_out_to_start);
    }

    protected Animator createSlideInFromEndAnimator(View v) {
        return createTranslateAlphaAnimator(v, R.anim.lb_dialog_slide_in_from_end);
    }

    protected Animator createSlideInFromStartAnimator(View v) {
        return createTranslateAlphaAnimator(v, R.anim.lb_dialog_slide_in_from_start);
    }

    protected Animator createSlideOutToEndAnimator(View v) {
        return createTranslateAlphaAnimator(v, R.anim.lb_dialog_slide_out_to_end);
    }

    private Animator createFadeOutAnimator(View v) {
        return createAlphaAnimator(v, 1.0f, 0.0f);
    }

    private Animator createTranslateAlphaAnimator(View v, int animatorResourceId) {
        Animator animator = AnimatorInflater.loadAnimator(v.getContext(), animatorResourceId);
        animator.setTarget(v);
        return animator;
    }

    private Animator createAlphaAnimator(View v, float fromAlpha, float toAlpha) {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", fromAlpha, toAlpha);
        alphaAnimator.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        return alphaAnimator;
    }

    private static class SelectorAnimator extends RecyclerView.OnScrollListener {
        private final int mAnimationDuration;
        private volatile boolean mFadedOut = true;
        private final ViewGroup mParentView;
        private final View mSelectorView;

        SelectorAnimator(View selectorView, ViewGroup parentView) {
            this.mSelectorView = selectorView;
            this.mParentView = parentView;
            this.mAnimationDuration = selectorView.getContext().getResources().getInteger(R.integer.lb_dialog_animation_duration);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState != 0) {
                this.mSelectorView.animate().alpha(0.0f).setDuration(this.mAnimationDuration).setInterpolator(new DecelerateInterpolator(2.0f)).setListener(new Listener(true)).start();
                return;
            }
            int selectorHeight = this.mSelectorView.getHeight();
            if (selectorHeight == 0) {
                ViewGroup.LayoutParams lp = this.mSelectorView.getLayoutParams();
                selectorHeight = this.mSelectorView.getContext().getResources().getDimensionPixelSize(R.dimen.lb_action_fragment_selector_min_height);
                lp.height = selectorHeight;
                this.mSelectorView.setLayoutParams(lp);
            }
            View focusedChild = this.mParentView.getFocusedChild();
            if (focusedChild != null) {
                float scaleY = focusedChild.getHeight() / selectorHeight;
                ViewPropertyAnimator animation = this.mSelectorView.animate().alpha(1.0f).setListener(new Listener(false)).setDuration(this.mAnimationDuration).setInterpolator(new DecelerateInterpolator(2.0f));
                if (this.mFadedOut) {
                    this.mSelectorView.setScaleY(scaleY);
                } else {
                    animation.scaleY(scaleY);
                }
                animation.start();
            }
        }

        private class Listener implements Animator.AnimatorListener {
            private boolean mCanceled;
            private boolean mFadingOut;

            public Listener(boolean fadingOut) {
                this.mFadingOut = fadingOut;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (this.mFadingOut) {
                    return;
                }
                SelectorAnimator.this.mFadedOut = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (this.mCanceled || !this.mFadingOut) {
                    return;
                }
                SelectorAnimator.this.mFadedOut = true;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animation) {
                this.mCanceled = true;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animation) {
            }
        }
    }

    private static class UntargetableAnimatorSet extends Animator {
        private final AnimatorSet mAnimatorSet;

        UntargetableAnimatorSet(AnimatorSet animatorSet) {
            this.mAnimatorSet = animatorSet;
        }

        @Override // android.animation.Animator
        public void addListener(Animator.AnimatorListener listener) {
            this.mAnimatorSet.addListener(listener);
        }

        @Override // android.animation.Animator
        public void cancel() {
            this.mAnimatorSet.cancel();
        }

        @Override // android.animation.Animator
        public Animator clone() {
            return this.mAnimatorSet.clone();
        }

        @Override // android.animation.Animator
        public void end() {
            this.mAnimatorSet.end();
        }

        @Override // android.animation.Animator
        public long getDuration() {
            return this.mAnimatorSet.getDuration();
        }

        @Override // android.animation.Animator
        public ArrayList<Animator.AnimatorListener> getListeners() {
            return this.mAnimatorSet.getListeners();
        }

        @Override // android.animation.Animator
        public long getStartDelay() {
            return this.mAnimatorSet.getStartDelay();
        }

        @Override // android.animation.Animator
        public boolean isRunning() {
            return this.mAnimatorSet.isRunning();
        }

        @Override // android.animation.Animator
        public boolean isStarted() {
            return this.mAnimatorSet.isStarted();
        }

        @Override // android.animation.Animator
        public void removeAllListeners() {
            this.mAnimatorSet.removeAllListeners();
        }

        @Override // android.animation.Animator
        public void removeListener(Animator.AnimatorListener listener) {
            this.mAnimatorSet.removeListener(listener);
        }

        @Override // android.animation.Animator
        public Animator setDuration(long duration) {
            return this.mAnimatorSet.setDuration(duration);
        }

        @Override // android.animation.Animator
        public void setInterpolator(TimeInterpolator value) {
            this.mAnimatorSet.setInterpolator(value);
        }

        @Override // android.animation.Animator
        public void setStartDelay(long startDelay) {
            this.mAnimatorSet.setStartDelay(startDelay);
        }

        @Override // android.animation.Animator
        public void setTarget(Object target) {
        }

        @Override // android.animation.Animator
        public void setupEndValues() {
            this.mAnimatorSet.setupEndValues();
        }

        @Override // android.animation.Animator
        public void setupStartValues() {
            this.mAnimatorSet.setupStartValues();
        }

        @Override // android.animation.Animator
        public void start() {
            this.mAnimatorSet.start();
        }
    }

    public static class Action implements Parcelable {
        public static Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>() { // from class: com.dismal.android.recline.app.DialogFragment.Action.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Action createFromParcel(Parcel source) {
                return new Builder().key(source.readString()).title(source.readString()).description(source.readString()).intent((Intent) source.readParcelable(Intent.class.getClassLoader())).resourcePackageName(source.readString()).drawableResource(source.readInt()).iconUri((Uri) source.readParcelable(Uri.class.getClassLoader())).checked(source.readInt() != 0).multilineDescription(source.readInt() != 0).checkSetId(source.readInt()).build();
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Action[] newArray(int size) {
                return new Action[size];
            }
        };
        private int mCheckSetId;
        private boolean mChecked;
        private String mDescription;
        private int mDrawableResource;
        private boolean mEnabled;
        private boolean mHasNext;
        private Uri mIconUri;
        private boolean mInfoOnly;
        private Intent mIntent;
        private String mKey;
        private boolean mMultilineDescription;
        private String mResourcePackageName;
        private String mTitle;

        public interface Listener {
            void onActionClicked(Action action);
        }

        public interface OnFocusListener {
            void onActionFocused(Action action);
        }

        /* synthetic */ Action(Action action) {
            this();
        }

        public static class Builder {
            private boolean mChecked;
            private String mDescription;
            private boolean mHasNext;
            private Uri mIconUri;
            private boolean mInfoOnly;
            private Intent mIntent;
            private String mKey;
            private boolean mMultilineDescription;
            private String mResourcePackageName;
            private String mTitle;
            private int mDrawableResource = 0;
            private int mCheckSetId = 0;
            private boolean mEnabled = true;

            public Action build() {
                Action action = new Action(null);
                action.mKey = this.mKey;
                action.mTitle = this.mTitle;
                action.mDescription = this.mDescription;
                action.mIntent = this.mIntent;
                action.mResourcePackageName = this.mResourcePackageName;
                action.mDrawableResource = this.mDrawableResource;
                action.mIconUri = this.mIconUri;
                action.mChecked = this.mChecked;
                action.mMultilineDescription = this.mMultilineDescription;
                action.mHasNext = this.mHasNext;
                action.mInfoOnly = this.mInfoOnly;
                action.mCheckSetId = this.mCheckSetId;
                action.mEnabled = this.mEnabled;
                return action;
            }

            public Builder key(String key) {
                this.mKey = key;
                return this;
            }

            public Builder title(String title) {
                this.mTitle = title;
                return this;
            }

            public Builder description(String description) {
                this.mDescription = description;
                return this;
            }

            public Builder intent(Intent intent) {
                this.mIntent = intent;
                return this;
            }

            public Builder resourcePackageName(String resourcePackageName) {
                this.mResourcePackageName = resourcePackageName;
                return this;
            }

            public Builder drawableResource(int drawableResource) {
                this.mDrawableResource = drawableResource;
                return this;
            }

            public Builder iconUri(Uri iconUri) {
                this.mIconUri = iconUri;
                return this;
            }

            public Builder checked(boolean checked) {
                this.mChecked = checked;
                return this;
            }

            public Builder multilineDescription(boolean multilineDescription) {
                this.mMultilineDescription = multilineDescription;
                return this;
            }

            public Builder checkSetId(int checkSetId) {
                this.mCheckSetId = checkSetId;
                return this;
            }
        }

        private Action() {
        }

        public String getKey() {
            return this.mKey;
        }

        public String getTitle() {
            return this.mTitle;
        }

        public String getDescription() {
            return this.mDescription;
        }

        public boolean isChecked() {
            return this.mChecked;
        }

        public Uri getIconUri() {
            return this.mIconUri;
        }

        public int getCheckSetId() {
            return this.mCheckSetId;
        }

        public boolean hasMultilineDescription() {
            return this.mMultilineDescription;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }

        public void setChecked(boolean checked) {
            this.mChecked = checked;
        }

        public boolean hasNext() {
            return this.mHasNext;
        }

        public boolean infoOnly() {
            return this.mInfoOnly;
        }

        public Drawable getIndicator(Context context) {
            if (this.mDrawableResource == 0) {
                return null;
            }
            if (this.mResourcePackageName == null) {
                return context.getResources().getDrawable(this.mDrawableResource);
            }
            try {
                Context packageContext = context.createPackageContext(this.mResourcePackageName, 0);
                Drawable icon = packageContext.getResources().getDrawable(this.mDrawableResource);
                return icon;
            } catch (PackageManager.NameNotFoundException e) {
                if (!Log.isLoggable("Action", 5)) {
                    return null;
                }
                Log.w("Action", "No icon for this action.");
                return null;
            } catch (Resources.NotFoundException e2) {
                if (!Log.isLoggable("Action", 5)) {
                    return null;
                }
                Log.w("Action", "No icon for this action.");
                return null;
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mKey);
            dest.writeString(this.mTitle);
            dest.writeString(this.mDescription);
            dest.writeParcelable(this.mIntent, flags);
            dest.writeString(this.mResourcePackageName);
            dest.writeInt(this.mDrawableResource);
            dest.writeParcelable(this.mIconUri, flags);
            dest.writeInt(this.mChecked ? 1 : 0);
            dest.writeInt(this.mMultilineDescription ? 1 : 0);
            dest.writeInt(this.mCheckSetId);
        }
    }
}
