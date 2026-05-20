package com.dismal.android.leanbacklauncher.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import androidx.leanback.graphics.ColorFilterCache;
import androidx.leanback.graphics.ColorFilterDimmer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.dismal.android.leanbacklauncher.HomeScrollManager;
import com.dismal.android.leanbacklauncher.MainActivity;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.animation.AnimatorLifecycle;
import com.dismal.android.leanbacklauncher.notifications.NotificationRowView;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.wallpaper.AnimatedLayer;
import com.dismal.android.recline.util.BitmapMaskPostProc;
import com.dismal.android.recline.util.BitmapUtils;
import com.dismal.android.recline.util.BitmapWorkerOptions;
import com.dismal.android.recline.util.DrawableDownloader;
import com.dismal.android.recline.util.RefcountBitmapDrawable;

/* JADX INFO: loaded from: classes.dex */
public class LauncherWallpaper extends FrameLayout implements NotificationRowView.NotificationRowListener, AnimatedLayer.AnimationListener, AnimatorLifecycle.OnAnimationFinishedListener, HomeScrollManager.HomeScrollFractionListener {
    private WallpaperImage mBackground;
    private final DrawableDownloader.BitmapCallback mBitmapDownloadCallback;
    private DrawableDownloader mBitmapDownloader;
    private Drawable mBlackBg;
    private String mCurrentBackgroundUri;
    private ColorFilterDimmer mDimmer;
    private String mDownloadingUri;
    private ImageView mFadeMaskExt;
    private Handler mHandler;
    private boolean mInShyMode;
    private Bitmap mMaskBitmap;
    private AnimatedLayer mOverlay;
    private boolean mPendingChange;
    private Drawable mPendingImage;
    private String mPendingImgUri;
    private final float mScrollDarkeningAmount;
    private final float mScrollDarkeningOffset;
    private Drawable mSystemBg;
    private FadeMaskView mVideoFadeMask;
    private final int mWallpaperDelay;
    private final int mWallpaperFetchTimeout;
    private final float mWallpaperScrollScale;
    private final float mZoom;
    private final float mZoomThreshold;

    private static class PendingUpdateData {
        public Drawable image;
        public String uri;

        /* synthetic */ PendingUpdateData(PendingUpdateData pendingUpdateData) {
            this();
        }

        private PendingUpdateData() {
        }
    }

    public LauncherWallpaper(Context context) {
        this(context, null);
    }

    public LauncherWallpaper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LauncherWallpaper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInShyMode = true;
        this.mPendingChange = false;
        this.mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.wallpaper.LauncherWallpaper.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        String uri = (String) msg.obj;
                        LauncherWallpaper.this.setBackgroundImage(uri);
                        break;
                    case 2:
                        PendingUpdateData args = (PendingUpdateData) msg.obj;
                        LauncherWallpaper.this.setOverlayBackground(args.image, args.uri);
                        break;
                    case 3:
                        LauncherWallpaper.this.mBitmapDownloader.cancelDownload(LauncherWallpaper.this.mBitmapDownloadCallback);
                        Log.w("LauncherWallpaper", "TIMEOUT fetching wallpeper image: " + LauncherWallpaper.this.mDownloadingUri);
                        LauncherWallpaper.this.mDownloadingUri = null;
                        LauncherWallpaper.this.setOverlayBackground(null, null);
                        break;
                }
            }
        };
        this.mBitmapDownloadCallback = new DrawableDownloader.BitmapCallback() { // from class: com.dismal.android.leanbacklauncher.wallpaper.LauncherWallpaper.2
            @Override // com.dismal.android.recline.util.CachedTaskPool.TaskCompleteCallback
            public void onCompleted(Drawable bitmap) {
                LauncherWallpaper.this.mHandler.removeMessages(3);
                if (bitmap == null) {
                    LauncherWallpaper.this.setOverlayBackground(null, null);
                } else {
                    LauncherWallpaper.this.setOverlayBackground(bitmap, LauncherWallpaper.this.mDownloadingUri);
                }
                LauncherWallpaper.this.releaseDrawable(bitmap);
                LauncherWallpaper.this.mDownloadingUri = null;
            }
        };
        this.mBitmapDownloader = DrawableDownloader.getInstance(getContext());
        this.mScrollDarkeningOffset = getContext().getResources().getDimensionPixelOffset(R.dimen.home_scroll_size_search);
        TypedValue out = new TypedValue();
        getResources().getValue(R.dimen.wallpaper_scroll_darkening_amount, out, true);
        this.mScrollDarkeningAmount = out.getFloat();
        getResources().getValue(R.dimen.wallpaper_to_launcher_scroll_scale, out, true);
        this.mWallpaperScrollScale = out.getFloat();
        getResources().getValue(R.dimen.wallpaper_zoom_amount, out, true);
        this.mZoom = out.getFloat();
        getResources().getValue(R.dimen.wallpaper_zoom_to_darkening_scale, out, true);
        float zoomToDarkeningScale = out.getFloat();
        this.mZoomThreshold = this.mScrollDarkeningOffset / zoomToDarkeningScale;
        ColorFilterCache cache = ColorFilterCache.getColorFilterCache(getResources().getColor(R.color.launcher_background_color));
        this.mDimmer = ColorFilterDimmer.create(cache, 0.0f, this.mScrollDarkeningAmount);
        this.mWallpaperDelay = getResources().getInteger(R.integer.wallpaper_update_delay);
        this.mWallpaperFetchTimeout = getResources().getInteger(R.integer.wallpaper_fetch_timeout);
        Partner partner = Partner.get(context);
        this.mSystemBg = partner.getSystemBackground();
        if (this.mSystemBg == null) {
            this.mSystemBg = getResources().getDrawable(R.drawable.bg_default, null);
        }
        this.mBlackBg = new ColorDrawable(-16777216);
        this.mMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_protection);
        this.mBitmapDownloader.registerPostProc(R.drawable.bg_protection, new BitmapMaskPostProc(context.getResources(), this.mMaskBitmap, this.mSystemBg.getIntrinsicWidth(), this.mSystemBg.getIntrinsicHeight(), 1));
        if (!(this.mSystemBg instanceof BitmapDrawable)) {
            this.mSystemBg = null;
            Log.e("LauncherWallpaper", "Invalid Drawable provided for the background. mSystemBg = " + this.mSystemBg);
        } else {
            this.mSystemBg = new BitmapDrawable(getResources(), BitmapUtils.applyMaskToBitmap(context.getResources(), ((BitmapDrawable) this.mSystemBg).getBitmap(), this.mMaskBitmap, this.mSystemBg.getIntrinsicWidth(), this.mSystemBg.getIntrinsicHeight(), 1));
        }
    }

    public void resetBackground(boolean visible) {
        this.mHandler.removeCallbacksAndMessages(null);
        this.mOverlay.cancelAnimation();
        this.mOverlay.setVisibility(8);
        this.mOverlay.setImageDrawable(null);
        if (!visible) {
            this.mBackground.setImageDrawable(null);
            this.mBackground.setVisibility(8);
        } else {
            this.mBackground.setImageDrawable(this.mSystemBg);
            this.mBackground.setVisibility(0);
        }
        this.mCurrentBackgroundUri = null;
    }

    public void setShynessMode(boolean shyMode) {
        if (!this.mInShyMode && shyMode) {
            this.mBackground.setImageDrawable(this.mBlackBg);
            this.mBackground.setVisibility(0);
            this.mCurrentBackgroundUri = "temp_background";
        }
        this.mInShyMode = shyMode;
        setBackgroundScrimVisibility(this.mInShyMode ? false : true);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, null));
    }

    public void setOverlayBackground(Drawable drawable, String uri) {
        if (drawable != null) {
            Log.v("LauncherWallpaper", "setOverlayBackground: " + ((RefcountBitmapDrawable) drawable).getBitmap());
        }
        MainActivity activity = (MainActivity) getContext();
        if (this.mOverlay.isAnimating() || activity.isAnimationInProgress()) {
            releaseDrawable(this.mPendingImage);
            this.mPendingChange = true;
            this.mPendingImage = addRef(drawable);
            this.mPendingImgUri = uri;
            return;
        }
        if (uri == null && this.mCurrentBackgroundUri == null && (this.mInShyMode || this.mBackground.getVisibility() != 0)) {
            return;
        }
        this.mCurrentBackgroundUri = uri;
        if (drawable != null) {
            if (PixelFormat.formatHasAlpha(drawable.getOpacity())) {
                drawable = overlayDrawables(this.mBlackBg, drawable);
            }
            this.mOverlay.animateIn(drawable);
        } else {
            Drawable old = this.mBackground.getDrawable();
            if (this.mInShyMode) {
                this.mBackground.setImageDrawable(this.mSystemBg);
            } else {
                this.mBackground.setVisibility(8);
            }
            this.mOverlay.animateOut(old);
        }
    }

    private Drawable overlayDrawables(Drawable imageToUnderlay, Drawable imageToOverlay) {
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{imageToUnderlay, imageToOverlay});
        return layerDrawable;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBackground = (WallpaperImage) findViewById(R.id.launcher_background);
        this.mOverlay = (AnimatedLayer) findViewById(R.id.animation_layer);
        this.mVideoFadeMask = (FadeMaskView) findViewById(R.id.video_fade_mask);
        this.mFadeMaskExt = (ImageView) findViewById(R.id.fade_mask_extension);
        if (this.mVideoFadeMask != null) {
            Bitmap videoMask = BitmapFactory.decodeResource(getResources(), R.drawable.bg_protection_video);
            this.mVideoFadeMask.setBitmap(videoMask);
        }
        this.mOverlay.setAnimationListener(this);
        ((MainActivity) getContext()).setOnAnimationFinishedListener(this);
    }

    @Override // com.dismal.android.leanbacklauncher.notifications.NotificationRowView.NotificationRowListener
    public void onBackgroundImageChanged(String imageUri, boolean notifActive) {
        if (!this.mInShyMode) {
            return;
        }
        if (imageUri != null && (TextUtils.equals(imageUri, this.mPendingImgUri) || TextUtils.equals(imageUri, this.mDownloadingUri))) {
            return;
        }
        this.mHandler.removeCallbacksAndMessages(null);
        cancelPendingUpdate();
        this.mBitmapDownloader.cancelDownload(this.mBitmapDownloadCallback);
        this.mDownloadingUri = null;
        if (TextUtils.equals(imageUri, this.mCurrentBackgroundUri)) {
            return;
        }
        if (!notifActive && imageUri == null && !TextUtils.equals(this.mCurrentBackgroundUri, "temp_background")) {
            return;
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, imageUri), this.mWallpaperDelay);
    }

    private void cancelPendingUpdate() {
        if (this.mPendingChange) {
            this.mPendingChange = false;
            releaseDrawable(this.mPendingImage);
            this.mPendingImage = null;
        }
        this.mPendingImgUri = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBackgroundImage(String imageUri) {
        this.mBitmapDownloader.cancelDownload(this.mBitmapDownloadCallback);
        this.mDownloadingUri = null;
        if (imageUri != null) {
            fetchWallpaperImage(imageUri);
        } else {
            setOverlayBackground(null, null);
        }
    }

    private void fetchWallpaperImage(String uri) {
        this.mHandler.removeMessages(3);
        BitmapWorkerOptions options = new BitmapWorkerOptions.Builder(getContext().getApplicationContext()).resource(Uri.parse(uri)).cacheFlag(1).postProcId(R.drawable.bg_protection).build();
        this.mDownloadingUri = uri;
        this.mBitmapDownloader.getBitmap(options, this.mBitmapDownloadCallback);
        this.mHandler.sendEmptyMessageDelayed(3, this.mWallpaperFetchTimeout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseDrawable(Drawable drawable) {
        if (!(drawable instanceof RefcountBitmapDrawable)) {
            return;
        }
        ((RefcountBitmapDrawable) drawable).getRefcountObject().releaseRef();
    }

    private Drawable addRef(Drawable drawable) {
        if (drawable instanceof RefcountBitmapDrawable) {
            ((RefcountBitmapDrawable) drawable).getRefcountObject().addRef();
        }
        return drawable;
    }

    @Override // com.dismal.android.leanbacklauncher.animation.AnimatorLifecycle.OnAnimationFinishedListener
    public void onAnimationFinished() {
        applyPendingUpdateIfNecessary();
    }

    @Override // com.dismal.android.leanbacklauncher.wallpaper.AnimatedLayer.AnimationListener
    public void animationDone(boolean visible) {
        if (visible) {
            this.mBackground.setImageDrawable(this.mOverlay.getDrawable());
            this.mBackground.setVisibility(0);
            this.mOverlay.setVisibility(8);
        } else {
            this.mOverlay.setImageDrawable(null);
        }
        applyPendingUpdateIfNecessary();
    }

    private void applyPendingUpdateIfNecessary() {
        PendingUpdateData pendingUpdateData = null;
        if (!this.mPendingChange) {
            return;
        }
        PendingUpdateData args = new PendingUpdateData(pendingUpdateData);
        args.image = this.mPendingImage;
        args.uri = this.mPendingImgUri;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, args));
        this.mPendingImage = null;
        this.mPendingChange = false;
        this.mPendingImgUri = null;
    }

    @Override // com.dismal.android.leanbacklauncher.HomeScrollManager.HomeScrollFractionListener
    public void onScrollPositionChanged(int position, float fractionFromTop) {
        int newPos = Math.round(position / this.mWallpaperScrollScale);
        this.mBackground.setY(newPos);
        this.mOverlay.setY(newPos);
        this.mVideoFadeMask.setY(newPos);
        this.mFadeMaskExt.setY(getMeasuredHeight() + newPos);
        this.mFadeMaskExt.setVisibility(newPos >= 0 ? 8 : 0);
        float darkeningFraction = 1.0f - Math.min(1.0f, Math.abs(position) / this.mScrollDarkeningOffset);
        float dimLevel = darkeningFraction * this.mScrollDarkeningAmount;
        float zoomFraction = 1.0f - Math.min(1.0f, Math.abs(position) / this.mZoomThreshold);
        float zoomLevel = this.mZoom * zoomFraction;
        this.mBackground.setZoomLevel(zoomLevel);
        this.mOverlay.setZoomLevel(zoomLevel);
        this.mDimmer.setActiveLevel(dimLevel);
        this.mBackground.setColorFilter(this.mDimmer.getColorFilter());
        this.mOverlay.setColorFilter(this.mDimmer.getColorFilter());
    }

    private void setBackgroundScrimVisibility(boolean visible) {
        this.mVideoFadeMask.setVisibility(visible ? 0 : 8);
    }
}
