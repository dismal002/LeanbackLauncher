package com.dismal.android.leanbacklauncher;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import com.dismal.android.leanbacklauncher.MainActivity;
import com.dismal.android.leanbacklauncher.SearchView;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.util.Util;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public class SearchOrbView extends FrameLayout implements View.OnFocusChangeListener, SearchView, MainActivity.IdleListener {
    private Context mContext;
    private int mCurrentIndex;
    private final int mFocusedColor;
    private final String mFocusedText;
    private Handler mHandler;
    private final int mHintTextStartMarginFocused;
    private final int mHintTextStartMarginUnfocused;
    private final int mIdleTextFlipDelay;
    private final boolean mIsHintFlippingAllowed;
    private final int mLaunchFadeDuration;
    private SearchView.SearchLaunchListener mListener;
    private androidx.leanback.widget.SearchOrbView mOrbView;
    private final String mSearchHintText;
    private final Intent mSearchIntent;
    private Runnable mSwitchRunnable;
    private TextSwitcher mSwitcher;
    private final String[] mTextToShow;
    private final int mUnfocusedColor;
    private View mWidgetView;

    public SearchOrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentIndex = 0;
        this.mHandler = new Handler();
        this.mSearchIntent = Util.getSearchIntent();
        this.mContext = context;
        Resources res = context.getResources();
        this.mTextToShow = res.getStringArray(R.array.search_orb_text_to_show);
        this.mIdleTextFlipDelay = res.getInteger(R.integer.search_orb_idle_hint_flip_delay);
        this.mLaunchFadeDuration = res.getInteger(R.integer.search_orb_text_fade_duration);
        this.mSearchHintText = context.getString(R.string.search_hint_text) + " ";
        this.mFocusedText = context.getString(R.string.focused_search_hint_text);
        this.mFocusedColor = res.getColor(R.color.search_orb_focused_hint_color);
        this.mUnfocusedColor = res.getColor(R.color.search_orb_unfocused_hint_color);
        this.mIsHintFlippingAllowed = res.getBoolean(R.bool.is_hint_flipping_allowed);
        this.mHintTextStartMarginFocused = res.getDimensionPixelSize(R.dimen.search_bar_hint_text_margin_start_focused);
        this.mHintTextStartMarginUnfocused = res.getDimensionPixelSize(R.dimen.search_bar_hint_text_margin_start_unfocused);
    }

    @Override // android.view.View
    public void onFinishInflate() {
        this.mWidgetView = findViewById(R.id.widget_wrapper);
        this.mOrbView = (androidx.leanback.widget.SearchOrbView) findViewById(R.id.orb);
        this.mOrbView.setOnFocusChangeListener(this);
        Partner partner = Partner.get(this.mContext);
        Drawable partnerSearchIcon = partner.getCustomSearchIcon();
        if (partnerSearchIcon != null) {
            this.mOrbView.setOrbIcon(partnerSearchIcon);
        }
        initTextSwitcher(getContext());
    }

    private void initTextSwitcher(final Context context) {
        this.mSwitcher = (TextSwitcher) findViewById(R.id.text_switcher);
        this.mSwitcher.setAnimateFirstView(false);
        this.mSwitcher.setFactory(new ViewSwitcher.ViewFactory() { // from class: com.dismal.android.leanbacklauncher.SearchOrbView.1
            LayoutInflater inflater;

            {
                this.inflater = (LayoutInflater) context.getSystemService("layout_inflater");
            }

            @Override // android.widget.ViewSwitcher.ViewFactory
            public View makeView() {
                return this.inflater.inflate(R.layout.search_orb_text_hint, (ViewGroup) SearchOrbView.this, false);
            }
        });
        this.mSwitchRunnable = new Runnable() { // from class: com.dismal.android.leanbacklauncher.SearchOrbView.2
            @Override // java.lang.Runnable
            public void run() {
                int old = SearchOrbView.this.mCurrentIndex;
                SearchOrbView.this.mCurrentIndex = new Random().nextInt(SearchOrbView.this.mTextToShow.length);
                if (old == SearchOrbView.this.mCurrentIndex) {
                    SearchOrbView.this.mCurrentIndex = (SearchOrbView.this.mCurrentIndex + 1) % SearchOrbView.this.mTextToShow.length;
                }
                SearchOrbView.this.configSwitcher(true, false, false);
                SearchOrbView.this.mSwitcher.setText(SearchOrbView.this.mTextToShow[SearchOrbView.this.mCurrentIndex] + " ");
                SearchOrbView.this.mHandler.postDelayed(this, SearchOrbView.this.mIdleTextFlipDelay);
            }
        };
        reset();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void configSwitcher(boolean isFlipping, boolean gainFocus, boolean focusChange) {
        int i;
        int i2;
        View v = isFlipping ? this.mSwitcher.getNextView() : this.mSwitcher.getCurrentView();
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            textView.setTextColor(gainFocus ? this.mFocusedColor : this.mUnfocusedColor);
            textView.setTypeface(null, gainFocus ? 0 : 2);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
            params.setMarginStart(gainFocus ? this.mHintTextStartMarginFocused : this.mHintTextStartMarginUnfocused);
            textView.setLayoutParams(params);
        }
        TextSwitcher textSwitcher = this.mSwitcher;
        Context context = this.mContext;
        if (focusChange) {
            i = R.anim.slide_in_left;
        } else {
            i = R.anim.slide_in_bottom;
        }
        textSwitcher.setInAnimation(context, i);
        TextSwitcher textSwitcher2 = this.mSwitcher;
        Context context2 = this.mContext;
        if (focusChange) {
            i2 = R.anim.slide_out_right;
        } else {
            i2 = R.anim.slide_out_top;
        }
        textSwitcher2.setOutAnimation(context2, i2);
    }

    public void reset() {
        this.mHandler.removeCallbacks(this.mSwitchRunnable);
        this.mSwitcher.reset();
        boolean focused = this.mOrbView.hasFocus();
        this.mCurrentIndex = focused ? -2 : -1;
        this.mSwitcher.setText(focused ? this.mFocusedText : this.mSearchHintText);
        configSwitcher(false, focused, false);
    }

    @Override // com.dismal.android.leanbacklauncher.MainActivity.IdleListener
    public void onIdleStateChange(boolean isIdle) {
        if (!this.mIsHintFlippingAllowed) {
            return;
        }
        this.mHandler.removeCallbacks(this.mSwitchRunnable);
        if (!isIdle || !isAttachedToWindow() || this.mOrbView.hasFocus()) {
            return;
        }
        this.mHandler.post(this.mSwitchRunnable);
    }

    @Override // com.dismal.android.leanbacklauncher.MainActivity.IdleListener
    public void onVisibilityChange(boolean isVisible) {
        if (!isVisible) {
            reset();
        }
        this.mOrbView.enableOrbColorAnimation(isVisible ? this.mOrbView.hasFocus() : false);
    }

    private void setVisibile(boolean visible) {
        animateVisibility(this.mWidgetView, visible);
        animateVisibility(this.mSwitcher, visible);
    }

    private boolean isVisible() {
        return this.mSwitcher.getVisibility() == 0 && this.mSwitcher.getAlpha() > 0.0f;
    }

    private void animateVisibility(View view, boolean visible) {
        view.clearAnimation();
        view.animate().alpha(visible ? 1.0f : 0.0f).setDuration(this.mLaunchFadeDuration).start();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setVisibile(true);
    }

    public void setLaunchListener(SearchView.SearchLaunchListener listener) {
        this.mListener = listener;
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 1 && event.getKeyCode() == 23 && Util.startSearchActivitySafely(this.mContext, this.mSearchIntent, event.getDeviceId()) && this.mListener != null) {
            animateOut();
            this.mListener.onSearchLaunched();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override // com.dismal.android.leanbacklauncher.SearchView
    public void animateIn() {
        setVisibile(true);
    }

    private void animateOut() {
        setVisibile(false);
        reset();
    }

    @Override // android.view.View
    public void setActivated(boolean active) {
    }

    @Override // android.view.View.OnFocusChangeListener
    public void onFocusChange(View v, boolean hasFocus) {
        if (!isVisible()) {
            return;
        }
        this.mHandler.removeCallbacks(this.mSwitchRunnable);
        int old = this.mCurrentIndex;
        this.mCurrentIndex = hasFocus ? -2 : -1;
        if (old == this.mCurrentIndex) {
            return;
        }
        configSwitcher(true, hasFocus, true);
        this.mSwitcher.setText(hasFocus ? this.mFocusedText : this.mSearchHintText);
    }
}
