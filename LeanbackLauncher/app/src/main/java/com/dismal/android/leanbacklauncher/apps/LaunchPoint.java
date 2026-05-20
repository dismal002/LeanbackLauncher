package com.dismal.android.leanbacklauncher.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.util.Util;
import com.dismal.android.recline.util.BitmapWorkerOptions;
import com.dismal.android.recline.util.DrawableDownloader;
import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
public class LaunchPoint {
    protected String mAppTitle;
    protected Drawable mBannerDrawable;
    private final DrawableDownloader.BitmapCallback mBitmapCallback;
    protected String mComponentName;
    protected String mContentDescription;
    protected boolean mHasBanner;
    protected Drawable mIconDrawable;
    private int mInstallProgressPercent;
    private int mInstallStateStringResourceId;
    protected boolean mIsGame;
    protected boolean mIsInitialInstall;
    protected int mLaunchColor;
    protected Intent mLaunchIntent;
    private InstallingLaunchPointListener mListener;
    protected long mPackageInstallTime;
    protected String mPackageName;
    protected int mPriority;
    protected int mSettingsType;

    public LaunchPoint(Context context, String appTitle, Drawable iconDrawable, Intent launchIntent, int launchColor) {
        this.mBitmapCallback = new DrawableDownloader.BitmapCallback() { // from class: com.dismal.android.leanbacklauncher.apps.LaunchPoint.1
            @Override // com.dismal.android.recline.util.CachedTaskPool.TaskCompleteCallback
            public void onCompleted(Drawable drawable) {
                if (drawable == null) {
                    return;
                }
                LaunchPoint.this.mIconDrawable = drawable;
                LaunchPoint.this.mListener.onInstallingLaunchPointChanged(LaunchPoint.this);
            }
        };
        clear();
        this.mAppTitle = appTitle;
        this.mIconDrawable = iconDrawable;
        this.mLaunchColor = launchColor;
        if (launchIntent == null) {
            return;
        }
        this.mLaunchIntent = launchIntent.addFlags(270532608);
        if (this.mLaunchIntent.getComponent() == null) {
            return;
        }
        this.mComponentName = this.mLaunchIntent.getComponent().flattenToString();
        this.mPackageName = this.mLaunchIntent.getComponent().getPackageName();
        this.mPackageInstallTime = Util.getInstallTimeForPackage(context, this.mPackageName);
    }

    public LaunchPoint(Context context, String appTitle, String iconUrl, String pkgName, Intent launchIntent, boolean isGame, InstallingLaunchPointListener listener) {
        this(context, appTitle, (Drawable) null, launchIntent, context.getResources().getColor(R.color.app_launch_ripple_default_color));
        this.mPackageName = pkgName;
        this.mIsGame = isGame;
        this.mListener = listener;
        this.mIsInitialInstall = true;
        Date date = new Date();
        this.mPackageInstallTime = date.getTime();
        if (TextUtils.isEmpty(iconUrl)) {
            return;
        }
        int maxIconSize = context.getResources().getDimensionPixelSize(R.dimen.banner_icon_size);
        BitmapWorkerOptions bitmapWorkerOptions = new BitmapWorkerOptions.Builder(context).resource(Uri.parse(iconUrl)).width(maxIconSize).height(maxIconSize).cacheFlag(1).build();
        DrawableDownloader.getInstance(context).getBitmap(bitmapWorkerOptions, this.mBitmapCallback);
    }

    public LaunchPoint(Context ctx, PackageManager pm, ResolveInfo info) {
        this.mBitmapCallback = new DrawableDownloader.BitmapCallback() { // from class: com.dismal.android.leanbacklauncher.apps.LaunchPoint.1
            @Override // com.dismal.android.recline.util.CachedTaskPool.TaskCompleteCallback
            public void onCompleted(Drawable drawable) {
                if (drawable == null) {
                    return;
                }
                LaunchPoint.this.mIconDrawable = drawable;
                LaunchPoint.this.mListener.onInstallingLaunchPointChanged(LaunchPoint.this);
            }
        };
        set(ctx, pm, info);
    }

    public LaunchPoint(Context ctx, PackageManager pm, ResolveInfo info, boolean useBanner, int settingsType) {
        this.mBitmapCallback = new DrawableDownloader.BitmapCallback() { // from class: com.dismal.android.leanbacklauncher.apps.LaunchPoint.1
            @Override // com.dismal.android.recline.util.CachedTaskPool.TaskCompleteCallback
            public void onCompleted(Drawable drawable) {
                if (drawable == null) {
                    return;
                }
                LaunchPoint.this.mIconDrawable = drawable;
                LaunchPoint.this.mListener.onInstallingLaunchPointChanged(LaunchPoint.this);
            }
        };
        set(ctx, pm, info, useBanner);
        this.mSettingsType = settingsType;
    }

    public LaunchPoint set(Context ctx, PackageManager pm, ResolveInfo info) {
        return set(ctx, pm, info, true);
    }

    public LaunchPoint set(Context ctx, PackageManager pm, ResolveInfo info, boolean useBanner) {
        boolean z;
        clear();
        this.mAppTitle = info.loadLabel(pm).toString();
        this.mLaunchIntent = getLaunchIntent(info);
        if (this.mLaunchIntent.getComponent() != null) {
            this.mComponentName = this.mLaunchIntent.getComponent().flattenToString();
            this.mPackageName = this.mLaunchIntent.getComponent().getPackageName();
        }
        Resources res = ctx.getResources();
        int maxWidth = res.getDimensionPixelOffset(R.dimen.max_banner_image_width);
        int maxHeight = res.getDimensionPixelOffset(R.dimen.max_banner_image_height);
        ActivityInfo actInfo = info.activityInfo;
        if (actInfo != null) {
            if (useBanner) {
                this.mBannerDrawable = actInfo.loadBanner(pm);
                if (this.mBannerDrawable instanceof BitmapDrawable) {
                    BitmapDrawable bmDrawable = (BitmapDrawable) this.mBannerDrawable;
                    this.mBannerDrawable = new BitmapDrawable(res, Util.getSizeCappedBitmap(bmDrawable.getBitmap(), maxWidth, maxHeight));
                }
            }
            if ((actInfo.applicationInfo.flags & 33554432) == 0) {
                z = actInfo.applicationInfo.metaData != null ? actInfo.applicationInfo.metaData.getBoolean("isGame", false) : false;
            } else {
                z = true;
            }
            this.mIsGame = z;
            if (this.mBannerDrawable != null) {
                this.mHasBanner = true;
            } else {
                if (useBanner) {
                    this.mBannerDrawable = actInfo.loadLogo(pm);
                    if (this.mBannerDrawable instanceof BitmapDrawable) {
                        BitmapDrawable bmDrawable2 = (BitmapDrawable) this.mBannerDrawable;
                        this.mBannerDrawable = new BitmapDrawable(res, Util.getSizeCappedBitmap(bmDrawable2.getBitmap(), maxWidth, maxHeight));
                    }
                }
                if (this.mBannerDrawable != null) {
                    this.mHasBanner = true;
                } else {
                    this.mHasBanner = false;
                    this.mIconDrawable = info.loadIcon(pm);
                }
            }
        }
        this.mPriority = info.priority;
        this.mLaunchColor = getColor(ctx, info);
        this.mPackageInstallTime = Util.getInstallTimeForPackage(ctx, this.mPackageName);
        return this;
    }

    public void addLaunchIntentFlags(int flags) {
        if (this.mLaunchIntent == null) {
            return;
        }
        this.mLaunchIntent.addFlags(flags);
    }

    private void clear() {
        this.mInstallProgressPercent = -1;
        this.mInstallStateStringResourceId = 0;
        this.mComponentName = null;
        this.mPackageName = null;
        this.mBannerDrawable = null;
        this.mAppTitle = null;
        this.mContentDescription = null;
        this.mIconDrawable = null;
        this.mLaunchColor = 0;
        this.mLaunchIntent = null;
        this.mHasBanner = false;
        this.mPriority = -1;
        this.mSettingsType = -1;
        this.mIsGame = false;
        this.mIsInitialInstall = false;
        this.mListener = null;
        this.mPackageInstallTime = -1L;
    }

    public LaunchPoint setInstallationState(LaunchPoint launchPoint) {
        this.mInstallProgressPercent = launchPoint.getInstallProgressPercent();
        this.mInstallStateStringResourceId = launchPoint.getInstallStateStringResId();
        return this;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        String pkgName = null;
        String compName = null;
        if (other instanceof LaunchPoint) {
            pkgName = ((LaunchPoint) other).getPackageName();
            compName = ((LaunchPoint) other).getComponentName();
        } else if (other instanceof ResolveInfo) {
            Intent intent = getLaunchIntent((ResolveInfo) other);
            if (intent.getComponent() != null) {
                pkgName = this.mLaunchIntent.getComponent().getPackageName();
                compName = this.mLaunchIntent.getComponent().flattenToString();
            }
        }
        if (!TextUtils.equals(this.mPackageName, pkgName)) {
            return false;
        }
        return TextUtils.equals(this.mComponentName, compName);
    }

    private static Intent getLaunchIntent(ResolveInfo info) {
        String packageName = info.activityInfo.applicationInfo.packageName;
        ComponentName componentName = new ComponentName(packageName, info.activityInfo.name);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(componentName);
        intent.addFlags(270532608);
        return intent;
    }

    private static int getColor(Context myContext, ResolveInfo info) {
        String packageName = info.activityInfo.applicationInfo.packageName;
        try {
            Context ctx = myContext.createPackageContext(packageName, 0);
            Resources.Theme theme = ctx.getTheme();
            theme.applyStyle(info.activityInfo.getThemeResource(), true);
            int[] values = {android.R.attr.colorPrimary};
            TypedArray a = theme.obtainStyledAttributes(values);
            int color = a.getColor(0, 0);
            a.recycle();
            return color;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return myContext.getResources().getColor(R.color.app_launch_ripple_default_color);
        }
    }

    public String getComponentName() {
        return this.mComponentName;
    }

    public String getTitle() {
        return this.mAppTitle;
    }

    public void setContentDescription(String desc) {
        this.mContentDescription = desc;
    }

    public String getContentDescription() {
        return this.mContentDescription;
    }

    public boolean hasBanner() {
        return this.mHasBanner;
    }

    public Intent getLaunchIntent() {
        return this.mLaunchIntent;
    }

    public Drawable getBannerDrawable() {
        return this.mBannerDrawable;
    }

    public Drawable getIconDrawable() {
        return this.mIconDrawable;
    }

    public boolean isGame() {
        return this.mIsGame;
    }

    public void setIconDrawable(Drawable drawable) {
        this.mIconDrawable = drawable;
    }

    public void setTitle(String title) {
        this.mAppTitle = title;
    }

    public int getLaunchColor() {
        return this.mLaunchColor;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public int getSettingsType() {
        return this.mSettingsType;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getInstallProgressString(Context context) {
        return this.mInstallProgressPercent == -1 ? "" : context.getString(R.string.progress_percent, Integer.valueOf(this.mInstallProgressPercent));
    }

    public int getInstallProgressPercent() {
        return this.mInstallProgressPercent;
    }

    public String getInstallStateString(Context context) {
        return context.getString(this.mInstallStateStringResourceId);
    }

    public int getInstallStateStringResId() {
        return this.mInstallStateStringResourceId;
    }

    public void setInstallProgressPercent(int progressPercent) {
        this.mInstallProgressPercent = progressPercent;
    }

    public void setInstallStateStringResourceId(int stateStringResourceId) {
        this.mInstallStateStringResourceId = stateStringResourceId;
    }

    public boolean isInstalling() {
        return this.mInstallStateStringResourceId != 0;
    }

    public boolean isInitialInstall() {
        return this.mIsInitialInstall;
    }

    public long getFirstInstallTime() {
        return this.mPackageInstallTime;
    }

    public String toString() {
        return this.mAppTitle + " [" + this.mPackageName + "]";
    }

    public void cancelPendingOperations(Context context) {
        DrawableDownloader.getInstance(context).cancelDownload(this.mBitmapCallback);
    }
}
