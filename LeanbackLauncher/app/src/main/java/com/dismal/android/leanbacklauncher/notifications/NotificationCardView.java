package com.dismal.android.leanbacklauncher.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import androidx.leanback.widget.BaseCardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.DimmableItem;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.ParticipatesInLaunchAnimation;
import com.dismal.android.leanbacklauncher.animation.ViewDimmer;
import com.dismal.android.leanbacklauncher.animation.ViewFocusAnimator;
import com.dismal.android.leanbacklauncher.util.Util;

/* JADX INFO: loaded from: classes.dex */
public class NotificationCardView extends BaseCardView implements DimmableItem, ParticipatesInLaunchAnimation {
    protected boolean mAutoDismiss;
    protected ImageView mBadgeImage;
    protected float mBadgeImageSelectedAlpha;
    protected float mBadgeImageUnselectedAlpha;
    protected final int mBadgeSize;
    protected int mCardWidth;
    protected PendingIntent mClickedIntent;
    protected int mColor;
    protected TextView mContentView;
    protected ViewDimmer mDimmer;
    protected final int mFocusAnimDuration;
    private ViewFocusAnimator mFocusAnimator;
    protected final int mImageHeight;
    protected final int mImageMaxWidth;
    protected final int mImageMinWidth;
    protected ImageView mImageView;
    protected View mInfoArea;
    protected ColorDrawable mInfoBackground;
    protected final int mMaxAllowedArea;
    private ObjectAnimator mMetaAnim;
    private int mMetaClosedHeight;
    private float mMetaOpenFraction;
    protected View mMetadataArea;
    protected final float mMinAspectRatio;
    protected String mNotifKey;
    protected ProgressBar mProgBar;
    protected String mRecGroup;
    protected View mSourceArea;
    protected TextView mSourceNameView;
    protected TextView mTitleView;
    private String mWallpaperUri;

    public NotificationCardView(Context context) {
        this(context, null);
    }

    public NotificationCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mImageMinWidth = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_min_width);
        this.mImageMaxWidth = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_max_width);
        this.mImageHeight = getResources().getDimensionPixelOffset(R.dimen.notif_card_img_height);
        this.mBadgeSize = getResources().getDimensionPixelOffset(R.dimen.notif_card_extra_badge_size);
        this.mFocusAnimDuration = getResources().getInteger(R.integer.notif_card_metadata_animation_duration);
        this.mMetaClosedHeight = getResources().getDimensionPixelOffset(R.dimen.notif_card_info_unfocused_height);
        TypedValue out = new TypedValue();
        getResources().getValue(R.dimen.notif_card_default_aspect_ratio, out, true);
        this.mMinAspectRatio = out.getFloat();
        getResources().getValue(R.dimen.badge_icon_selected_alpha, out, true);
        this.mBadgeImageSelectedAlpha = out.getFloat();
        getResources().getValue(R.dimen.badge_icon_unselected_alpha, out, true);
        this.mBadgeImageUnselectedAlpha = out.getFloat();
        getResources().getValue(R.dimen.notif_card_max_area_allowed_excess, out, true);
        float allowedExcess = out.getFloat();
        this.mMaxAllowedArea = (int) (Math.pow(this.mImageHeight, 2.0d) * ((double) this.mMinAspectRatio) * ((double) allowedExcess));
        this.mFocusAnimator = new ViewFocusAnimator(this);
        this.mMetaOpenFraction = 0.0f;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mImageView = (ImageView) findViewById(R.id.art_work);
        this.mMetadataArea = findViewById(R.id.metadata);
        this.mInfoArea = findViewById(R.id.info_field);
        this.mSourceArea = findViewById(R.id.source_field);
        this.mTitleView = (TextView) findViewById(R.id.title_text);
        this.mContentView = (TextView) findViewById(R.id.content_text);
        this.mSourceNameView = (TextView) findViewById(R.id.source_name);
        this.mBadgeImage = (ImageView) findViewById(R.id.extra_badge);
        this.mProgBar = (ProgressBar) findViewById(R.id.progress_bar);
        Drawable cardBkg = getBackground();
        this.mColor = getResources().getColor(R.color.notif_background_color);
        this.mInfoBackground = new ColorDrawable(this.mColor);
        this.mInfoArea.setBackground(this.mInfoBackground);
        this.mDimmer = new ViewDimmer(this);
        this.mDimmer.addDimTarget(this.mImageView);
        this.mDimmer.addDimTarget(this.mTitleView);
        this.mDimmer.addDimTarget(this.mContentView);
        this.mDimmer.addDimTarget(this.mSourceNameView);
        this.mDimmer.addDimTarget(cardBkg);
        this.mDimmer.addDesatDimTarget(this.mBadgeImage);
        this.mDimmer.addDimTarget(this.mInfoBackground);
        this.mDimmer.addDimTarget(this.mProgBar.getProgressDrawable());
        this.mDimmer.setDimLevelImmediate();
        setClipToOutline(true);
    }

    public void setMainImage(Drawable image) {
        if (this.mImageView == null) {
            return;
        }
        this.mImageView.setImageDrawable(image);
    }

    public void setTitleText(CharSequence text) {
        if (this.mTitleView == null) {
            return;
        }
        this.mTitleView.setText(text);
        this.mTitleView.requestLayout();
    }

    public void setContentText(CharSequence text) {
        if (this.mContentView == null) {
            return;
        }
        this.mContentView.setText(text);
        this.mContentView.requestLayout();
    }

    public void setSourceName(CharSequence text) {
        if (this.mSourceNameView == null) {
            return;
        }
        this.mSourceNameView.setText(text);
    }

    public void setBadgeImage(Drawable image) {
        if (this.mBadgeImage == null) {
            return;
        }
        if (image != null) {
            image.mutate();
            this.mBadgeImage.setImageDrawable(image);
            this.mBadgeImage.setVisibility(0);
            return;
        }
        this.mBadgeImage.setVisibility(8);
    }

    private Drawable getResizedBitmapDrawable(Drawable image, int width, int height) {
        if (!(image instanceof BitmapDrawable)) {
            return image;
        }
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    public void setWallpaperUri(String uri) {
        if (Util.isContentUri(uri)) {
            this.mWallpaperUri = uri;
        } else {
            Log.w("NotificationCardView", "Invalid Content URI provided for recommendation background: " + uri);
            this.mWallpaperUri = null;
        }
    }

    public String getWallpaperUri() {
        return this.mWallpaperUri;
    }

    public void setProgressShown(boolean shown) {
        if (this.mProgBar == null) {
            return;
        }
        this.mProgBar.setVisibility(shown ? 0 : 8);
    }

    public void setProgress(int max, int progress) {
        if (this.mProgBar == null) {
            return;
        }
        this.mProgBar.setMax(max);
        this.mProgBar.setProgress(progress);
    }

    private static void setViewWidth(View v, int width) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        p.width = width;
        v.setLayoutParams(p);
    }

    private static void setViewHeight(View v, int height) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        p.height = height;
        v.setLayoutParams(p);
    }

    public void setClickedIntent(PendingIntent intent) {
        this.mClickedIntent = intent;
    }

    public PendingIntent getClickedIntent() {
        return this.mClickedIntent;
    }

    public void setNotificationKey(String key) {
        this.mNotifKey = key;
    }

    public void setAutoDismiss(boolean auto) {
        this.mAutoDismiss = auto;
    }

    public boolean isAutoDismiss() {
        return this.mAutoDismiss;
    }

    public void setRecommendationGroup(String tag) {
        this.mRecGroup = tag;
    }

    public String getRecommendationGroup() {
        return this.mRecGroup;
    }

    public int getColor() {
        return this.mColor;
    }

    @Override // com.dismal.android.leanbacklauncher.DimmableItem
    public void setDimState(boolean active, boolean immediate) {
        this.mDimmer.setDimState(active, immediate);
    }

    @Override // androidx.leanback.widget.BaseCardView, android.view.View
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setMetaDataState(selected, hasWindowFocus());
    }

    public void setNotificationContent(StatusBarNotification sbn, boolean updateImage) {
        Notification notif = sbn.getNotification();
        if (notif == null) {
            return;
        }
        CharSequence title = (CharSequence) notif.extras.get("android.title");
        CharSequence content = (CharSequence) notif.extras.get("android.text");
        CharSequence label = (CharSequence) notif.extras.get("android.infoText");
        if (updateImage) {
            setNotificationImage(sbn);
        }
        setTitleText(title);
        setContentText(content);
        PackageManager pm = getContext().getPackageManager();
        if (TextUtils.isEmpty(label)) {
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(sbn.getPackageName(), 0));
            } catch (PackageManager.NameNotFoundException e) {
                label = null;
            }
        }
        setSourceName(label);
        if (notif.extras != null) {
            String uri = notif.extras.getString("android.backgroundImageUri");
            setWallpaperUri(uri);
        } else {
            setWallpaperUri(null);
        }
        if (notif.color != 0) {
            this.mColor = notif.color;
            this.mInfoBackground.setColor(this.mColor);
        } else {
            this.mColor = getResources().getColor(R.color.notif_background_color);
            this.mInfoBackground.setColor(this.mColor);
        }
        setClickedIntent(notif.contentIntent);
        setRecommendationGroup(notif.getGroup());
        setNotificationKey(sbn.getKey());
        setAutoDismiss((notif.flags & 16) != 0);
        boolean progVisible = false;
        int max = 0;
        int prog = 0;
        if (notif.extras.containsKey("android.progress") && (!notif.extras.containsKey("android.progressIndeterminate") || !notif.extras.getBoolean("android.progressIndeterminate"))) {
            max = notif.extras.getInt("android.progressMax");
            prog = notif.extras.getInt("android.progress");
            if (max > 0) {
                progVisible = true;
            }
        }
        if (this.mProgBar == null) {
            return;
        }
        if (progVisible) {
            this.mProgBar.setVisibility(0);
            this.mProgBar.setMax(max);
            this.mProgBar.setProgress(prog);
            return;
        }
        this.mProgBar.setVisibility(8);
    }

    protected void setNotificationImage(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        try {
            Resources res = getContext().getPackageManager().getResourcesForApplication(sbn.getPackageName());
            Drawable image = null;
            int width = -1;
            int height = -1;
            if (notif.largeIcon != null) {
                image = new BitmapDrawable(res, notif.largeIcon);
                width = image.getIntrinsicWidth();
                height = image.getIntrinsicHeight();
            }
            if (sbn.getNotification().extras != null) {
                width = sbn.getNotification().extras.getInt("notif_img_width", -1);
                height = sbn.getNotification().extras.getInt("notif_img_height", -1);
            }
            this.mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            setDimensions(width, height);
            setMainImage(image);
            setBadgeImage(getResizedBitmapDrawable(res.getDrawable(notif.icon, null), this.mBadgeSize, this.mBadgeSize));
        } catch (PackageManager.NameNotFoundException e) {
        } catch (Resources.NotFoundException e2) {
        }
    }

    protected void setDimensions(int imgWidth, int imgHeight) {
        int cardWidth;
        if (imgWidth <= 0 || imgHeight <= 0) {
            cardWidth = this.mImageMinWidth;
        } else {
            float scale = imgHeight / this.mImageHeight;
            cardWidth = (int) (imgWidth / scale);
            if (cardWidth > this.mImageMaxWidth) {
                cardWidth = this.mImageMaxWidth;
            }
            if (cardWidth < this.mImageMinWidth) {
                cardWidth = this.mImageMinWidth;
            }
        }
        this.mCardWidth = cardWidth;
        setViewWidth(this.mImageView, cardWidth);
        setViewHeight(this.mImageView, this.mImageHeight);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetCardState();
    }

    @Override // androidx.leanback.widget.BaseCardView, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetCardState();
    }

    public void resetCardState() {
        boolean focus = hasFocus();
        super.setSelected(focus);
        if (this.mMetaAnim != null) {
            this.mMetaAnim.cancel();
            this.mMetaAnim = null;
        }
        clearAnimation();
        this.mDimmer.setDimLevelImmediate();
        this.mFocusAnimator.setFocusImmediate(focus);
        setMetaOpenFraction(focus ? 1.0f : 0.0f);
    }

    public void setMetaDataState(boolean expanded, boolean animate) {
        this.mInfoArea.setVisibility(expanded ? 0 : 8);
        if (this.mMetaAnim != null) {
            this.mMetaAnim.cancel();
            this.mMetaAnim = null;
        }
        if (!animate || !isAttachedToWindow() || getVisibility() != 0) {
            if (expanded) {
                setMetaOpenFraction(1.0f);
                return;
            } else {
                setMetaOpenFraction(0.0f);
                return;
            }
        }
        float[] fArr = new float[2];
        fArr[0] = this.mMetaOpenFraction;
        fArr[1] = expanded ? 1.0f : 0.0f;
        this.mMetaAnim = ObjectAnimator.ofFloat(this, "metaOpenFraction", fArr);
        this.mMetaAnim.setDuration(this.mFocusAnimDuration);
        this.mMetaAnim.addListener(new AnimatorListenerAdapter() { // from class: com.dismal.android.leanbacklauncher.notifications.NotificationCardView.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                NotificationCardView.this.setHasTransientState(true);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                NotificationCardView.this.setHasTransientState(false);
            }
        });
        this.mMetaAnim.start();
    }

    public float getMetaOpenFraction() {
        return this.mMetaOpenFraction;
    }

    public void setMetaOpenFraction(float fract) {
        if (this.mMetadataArea == null) {
            return;
        }
        this.mMetaOpenFraction = fract;
        this.mInfoArea.requestLayout();
        this.mInfoArea.measure(View.MeasureSpec.makeMeasureSpec(this.mCardWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
        int metaOpenHeight = this.mInfoArea.getMeasuredHeight();
        int delta = Math.max(0, metaOpenHeight - this.mMetaClosedHeight);
        int height = this.mMetaClosedHeight + Math.round(this.mMetaOpenFraction * delta);
        setViewHeight(this.mMetadataArea, height);
        this.mInfoArea.getBackground().setAlpha((int) ((this.mMetaOpenFraction * 255.0f) + 0.5f));
        this.mTitleView.setAlpha(this.mMetaOpenFraction);
        this.mContentView.setAlpha(this.mMetaOpenFraction);
        this.mSourceNameView.setAlpha(1.0f - this.mMetaOpenFraction);
        this.mInfoArea.setVisibility(this.mMetaOpenFraction <= 0.0f ? 8 : 0);
        float badgeAlpha = ((this.mBadgeImageSelectedAlpha - this.mBadgeImageUnselectedAlpha) * fract) + this.mBadgeImageUnselectedAlpha;
        this.mBadgeImage.setImageAlpha((int) (badgeAlpha * 255.0f));
        requestLayout();
    }

    @Override // android.view.View
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        setSelected(gainFocus);
    }
}
