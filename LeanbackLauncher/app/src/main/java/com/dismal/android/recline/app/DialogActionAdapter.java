package com.dismal.android.recline.app;

import com.dismal.android.leanbacklauncher.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.dismal.android.recline.app.DialogFragment;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class DialogActionAdapter extends RecyclerView.Adapter {
    private final ActionOnFocusAnimator mActionOnFocusAnimator;
    private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
    private final List<DialogFragment.Action> mActions;
    private LayoutInflater mInflater;
    private DialogFragment.Action.Listener mListener;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() { // from class: com.dismal.android.recline.app.DialogActionAdapter.1
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (v == null || v.getWindowToken() == null || DialogActionAdapter.this.mListener == null) {
                return;
            }
            DialogActionAdapter.this.mListener.onActionClicked(((ActionViewHolder) v.getTag(R.id.action_title)).getAction());
        }
    };

    public DialogActionAdapter(DialogFragment.Action.Listener listener, DialogFragment.Action.OnFocusListener onFocusListener, List<DialogFragment.Action> actions) {
        this.mListener = listener;
        this.mActions = new ArrayList(actions);
        this.mActionOnKeyPressAnimator = new ActionOnKeyPressAnimator(listener, this.mActions);
        this.mActionOnFocusAnimator = new ActionOnFocusAnimator(onFocusListener);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (this.mInflater == null) {
            this.mInflater = (LayoutInflater) parent.getContext().getSystemService("layout_inflater");
        }
        View v = this.mInflater.inflate(R.layout.lb_dialog_action_list_item, parent, false);
        v.setTag(R.layout.lb_dialog_action_list_item, parent);
        return new ActionViewHolder(v, this.mActionOnKeyPressAnimator, this.mActionOnFocusAnimator, this.mOnClickListener);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
        ActionViewHolder holder = (ActionViewHolder) baseHolder;
        if (position >= this.mActions.size()) {
            return;
        }
        holder.init(this.mActions.get(position));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mActions.size();
    }

    public void setActions(ArrayList<DialogFragment.Action> actions) {
        this.mActionOnFocusAnimator.unFocus(null);
        this.mActions.clear();
        this.mActions.addAll(actions);
        notifyDataSetChanged();
    }

    private static class ActionViewHolder extends RecyclerView.ViewHolder {
        private DialogFragment.Action mAction;
        private final ActionOnFocusAnimator mActionOnFocusAnimator;
        private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
        private final View.OnClickListener mViewOnClickListener;

        public ActionViewHolder(View v, ActionOnKeyPressAnimator actionOnKeyPressAnimator, ActionOnFocusAnimator actionOnFocusAnimator, View.OnClickListener viewOnClickListener) {
            super(v);
            this.mActionOnKeyPressAnimator = actionOnKeyPressAnimator;
            this.mActionOnFocusAnimator = actionOnFocusAnimator;
            this.mViewOnClickListener = viewOnClickListener;
        }

        public DialogFragment.Action getAction() {
            return this.mAction;
        }

        public void init(DialogFragment.Action action) {
            this.mAction = action;
            TextView title = (TextView) this.itemView.findViewById(R.id.action_title);
            TextView description = (TextView) this.itemView.findViewById(R.id.action_description);
            description.setText(action.getDescription());
            description.setVisibility(TextUtils.isEmpty(action.getDescription()) ? 8 : 0);
            title.setText(action.getTitle());
            ImageView checkmarkView = (ImageView) this.itemView.findViewById(R.id.action_checkmark);
            checkmarkView.setVisibility(action.isChecked() ? 0 : 4);
            ImageView indicatorView = (ImageView) this.itemView.findViewById(R.id.action_icon);
            View content = this.itemView.findViewById(R.id.action_content);
            ViewGroup.LayoutParams contentLp = content.getLayoutParams();
            if (setIndicator(indicatorView, action)) {
                contentLp.width = this.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.lb_action_text_width);
            } else {
                contentLp.width = this.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.lb_action_text_width_no_icon);
            }
            content.setLayoutParams(contentLp);
            ImageView chevronView = (ImageView) this.itemView.findViewById(R.id.action_next_chevron);
            chevronView.setVisibility(action.hasNext() ? 0 : 4);
            Resources res = this.itemView.getContext().getResources();
            if (action.hasMultilineDescription()) {
                title.setMaxLines(res.getInteger(R.integer.lb_dialog_action_title_max_lines));
                description.setMaxHeight(getDescriptionMaxHeight(this.itemView.getContext(), title));
            } else {
                title.setMaxLines(res.getInteger(R.integer.lb_dialog_action_title_min_lines));
                description.setMaxLines(res.getInteger(R.integer.lb_dialog_action_description_min_lines));
            }
            this.itemView.setTag(R.id.action_title, this);
            this.itemView.setOnKeyListener(this.mActionOnKeyPressAnimator);
            this.itemView.setOnClickListener(this.mViewOnClickListener);
            this.itemView.setOnFocusChangeListener(this.mActionOnFocusAnimator);
            this.mActionOnFocusAnimator.unFocus(this.itemView);
        }

        private boolean setIndicator(ImageView indicatorView, DialogFragment.Action action) {
            Context context = indicatorView.getContext();
            Drawable indicator = action.getIndicator(context);
            if (indicator != null) {
                indicatorView.setImageDrawable(indicator);
                indicatorView.setVisibility(0);
                return true;
            }
            Uri iconUri = action.getIconUri();
            if (iconUri != null) {
                indicatorView.setVisibility(4);
                return true;
            }
            indicatorView.setVisibility(8);
            return false;
        }

        private int getDescriptionMaxHeight(Context context, TextView title) {
            Resources res = context.getResources();
            float verticalPadding = res.getDimension(R.dimen.lb_dialog_list_item_vertical_padding);
            int titleMaxLines = res.getInteger(R.integer.lb_dialog_action_title_max_lines);
            int displayHeight = ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getHeight();
            return (int) ((displayHeight - (2.0f * verticalPadding)) - ((titleMaxLines * 2) * title.getLineHeight()));
        }
    }

    private static class ActionOnFocusAnimator implements View.OnFocusChangeListener {
        private int mAnimationDuration;
        private float mDisabledChevronAlpha;
        private float mDisabledDescriptionAlpha;
        private float mDisabledTitleAlpha;
        private DialogFragment.Action.OnFocusListener mOnFocusListener;
        private boolean mResourcesSet;
        private float mSelectedChevronAlpha;
        private float mSelectedDescriptionAlpha;
        private float mSelectedTitleAlpha;
        private View mSelectedView;
        private float mUnselectedAlpha;
        private float mUnselectedDescriptionAlpha;

        ActionOnFocusAnimator(DialogFragment.Action.OnFocusListener onFocusListener) {
            this.mOnFocusListener = onFocusListener;
        }

        public void unFocus(View v) {
            if (v == null) {
                v = this.mSelectedView;
            }
            changeFocus(v, false, false);
        }

        @Override // android.view.View.OnFocusChangeListener
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                this.mSelectedView = v;
                changeFocus(v, true, true);
                if (this.mOnFocusListener == null) {
                    return;
                }
                this.mOnFocusListener.onActionFocused(((ActionViewHolder) v.getTag(R.id.action_title)).getAction());
                return;
            }
            if (this.mSelectedView == v) {
                this.mSelectedView = null;
            }
            changeFocus(v, false, true);
        }

        private void changeFocus(View v, boolean hasFocus, boolean shouldAnimate) {
            float descriptionAlpha;
            if (v == null) {
                return;
            }
            if (!this.mResourcesSet) {
                this.mResourcesSet = true;
                Resources res = v.getContext().getResources();
                this.mAnimationDuration = res.getInteger(R.integer.lb_dialog_animation_duration);
                this.mUnselectedAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_unselected_text_alpha)).floatValue();
                this.mSelectedTitleAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_selected_title_text_alpha)).floatValue();
                this.mDisabledTitleAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_disabled_title_text_alpha)).floatValue();
                this.mSelectedDescriptionAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_selected_description_text_alpha)).floatValue();
                this.mUnselectedDescriptionAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_unselected_description_text_alpha)).floatValue();
                this.mDisabledDescriptionAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_disabled_description_text_alpha)).floatValue();
                this.mSelectedChevronAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_selected_chevron_background_alpha)).floatValue();
                this.mDisabledChevronAlpha = Float.valueOf(res.getString(R.string.lb_dialog_list_item_disabled_chevron_background_alpha)).floatValue();
            }
            DialogFragment.Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();
            float titleAlpha = (!action.isEnabled() || action.infoOnly()) ? this.mDisabledTitleAlpha : hasFocus ? this.mSelectedTitleAlpha : this.mUnselectedAlpha;
            if (!hasFocus || action.infoOnly()) {
                descriptionAlpha = this.mUnselectedDescriptionAlpha;
            } else {
                descriptionAlpha = action.isEnabled() ? this.mSelectedDescriptionAlpha : this.mDisabledDescriptionAlpha;
            }
            float chevronAlpha = (!action.hasNext() || action.infoOnly()) ? 0.0f : action.isEnabled() ? this.mSelectedChevronAlpha : this.mDisabledChevronAlpha;
            TextView title = (TextView) v.findViewById(R.id.action_title);
            setAlpha(title, shouldAnimate, titleAlpha);
            TextView description = (TextView) v.findViewById(R.id.action_description);
            setAlpha(description, shouldAnimate, descriptionAlpha);
            ImageView checkmark = (ImageView) v.findViewById(R.id.action_checkmark);
            setAlpha(checkmark, shouldAnimate, titleAlpha);
            ImageView icon = (ImageView) v.findViewById(R.id.action_icon);
            setAlpha(icon, shouldAnimate, titleAlpha);
            ImageView chevron = (ImageView) v.findViewById(R.id.action_next_chevron);
            setAlpha(chevron, shouldAnimate, chevronAlpha);
        }

        private void setAlpha(View view, boolean shouldAnimate, float alpha) {
            if (shouldAnimate) {
                view.animate().alpha(alpha).setDuration(this.mAnimationDuration).setInterpolator(new DecelerateInterpolator(2.0f)).start();
            } else {
                view.setAlpha(alpha);
            }
        }
    }

    private static class ActionOnKeyPressAnimator implements View.OnKeyListener {
        private final List<DialogFragment.Action> mActions;
        private boolean mKeyPressed = false;
        private DialogFragment.Action.Listener mListener;

        public ActionOnKeyPressAnimator(DialogFragment.Action.Listener listener, List<DialogFragment.Action> actions) {
            this.mListener = listener;
            this.mActions = actions;
        }

        private void playSound(Context context, int soundEffect) {
            AudioManager manager = (AudioManager) context.getSystemService("audio");
            manager.playSoundEffect(soundEffect);
        }

        @Override // android.view.View.OnKeyListener
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (v == null) {
                return false;
            }
            DialogFragment.Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();
            switch (keyCode) {
                case 23:
                case 66:
                case 99:
                case 100:
                case 160:
                    if (!action.isEnabled() || action.infoOnly()) {
                        if (!v.isSoundEffectsEnabled() || event.getAction() == 0) {
                        }
                    } else {
                        switch (event.getAction()) {
                            case 0:
                                if (!this.mKeyPressed) {
                                    this.mKeyPressed = true;
                                    if (v.isSoundEffectsEnabled()) {
                                        playSound(v.getContext(), 0);
                                    }
                                    prepareAndAnimateView(v, 1.0f, 0.2f, 100, 0, null, this.mKeyPressed);
                                }
                                break;
                            case 1:
                                if (this.mKeyPressed) {
                                    this.mKeyPressed = false;
                                    prepareAndAnimateView(v, 0.2f, 1.0f, 100, 0, null, this.mKeyPressed);
                                }
                                break;
                        }
                    }
                    break;
            }
            return false;
        }

        private void prepareAndAnimateView(final View v, float initAlpha, float destAlpha, int duration, int delay, Interpolator interpolator, final boolean pressed) {
            if (v == null || v.getWindowToken() == null) {
                return;
            }
            final DialogFragment.Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();
            if (!pressed) {
                fadeCheckmarks(v, action, duration, delay, interpolator);
            }
            v.setAlpha(initAlpha);
            v.setLayerType(2, null);
            v.buildLayer();
            v.animate().alpha(destAlpha).setDuration(duration).setStartDelay(delay);
            if (interpolator != null) {
                v.animate().setInterpolator(interpolator);
            }
            v.animate().setListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.recline.app.DialogActionAdapter.ActionOnKeyPressAnimator.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    v.setLayerType(0, null);
                    if (pressed || ActionOnKeyPressAnimator.this.mListener == null) {
                        return;
                    }
                    ActionOnKeyPressAnimator.this.mListener.onActionClicked(action);
                }
            });
            v.animate().start();
        }

        private void fadeCheckmarks(View v, DialogFragment.Action action, int duration, int delay, Interpolator interpolator) {
            int actionCheckSetId = action.getCheckSetId();
            if (actionCheckSetId == 0) {
                return;
            }
            ViewGroup parent = (ViewGroup) v.getTag(R.layout.lb_dialog_action_list_item);
            int size = this.mActions.size();
            for (int i = 0; i < size; i++) {
                DialogFragment.Action a = this.mActions.get(i);
                if (a != action && a.getCheckSetId() == actionCheckSetId && a.isChecked()) {
                    a.setChecked(false);
                    View viewToAnimateOut = parent.getChildAt(i);
                    if (viewToAnimateOut != null) {
                        final View checkView = viewToAnimateOut.findViewById(R.id.action_checkmark);
                        checkView.animate().alpha(0.0f).setDuration(duration).setStartDelay(delay);
                        if (interpolator != null) {
                            checkView.animate().setInterpolator(interpolator);
                        }
                        checkView.animate().setListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.recline.app.DialogActionAdapter.ActionOnKeyPressAnimator.2
                            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                            public void onAnimationEnd(Animator animation) {
                                checkView.setVisibility(4);
                            }
                        });
                    }
                }
            }
            if (action.isChecked()) {
                return;
            }
            action.setChecked(true);
            View checkView2 = v.findViewById(R.id.action_checkmark);
            checkView2.setVisibility(0);
            checkView2.setAlpha(0.0f);
            checkView2.animate().alpha(1.0f).setDuration(duration).setStartDelay(delay);
            if (interpolator != null) {
                checkView2.animate().setInterpolator(interpolator);
            }
            checkView2.animate().setListener(null);
        }
    }
}
