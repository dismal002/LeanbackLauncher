package com.dismal.android.recline.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import com.dismal.android.recline.util.CachedTaskPool;

/* JADX INFO: loaded from: classes.dex */
public class BitmapWorkerOptions implements CachedTaskPool.TaskOption {
    private Bitmap.Config mBitmapConfig;
    private int mCacheFlag;
    private Context mContext;
    private int mHeight;
    private Intent.ShortcutIconResource mIconResource;
    private String mKey;
    private int mPostProcId;
    private Uri mResourceUri;
    private int mWidth;

    /* synthetic */ BitmapWorkerOptions(BitmapWorkerOptions bitmapWorkerOptions) {
        this();
    }

    public static class Builder {
        private Context mContext;
        private String mPackageName;
        private String mResourceName;
        private Uri mResourceUri;
        private int mWidth = 2048;
        private int mHeight = 2048;
        private int mCacheFlag = 0;
        private Bitmap.Config mBitmapConfig = null;
        private int mPostProcId = -1;

        public Builder(Context context) {
            this.mContext = context.getApplicationContext();
        }

        public BitmapWorkerOptions build() {
            BitmapWorkerOptions options = new BitmapWorkerOptions(null);
            if (!TextUtils.isEmpty(this.mPackageName)) {
                options.mIconResource = new Intent.ShortcutIconResource();
                options.mIconResource.packageName = this.mPackageName;
                options.mIconResource.resourceName = this.mResourceName;
            }
            int largestDim = Math.max(this.mWidth, this.mHeight);
            if (largestDim > 2048) {
                double scale = 2048.0d / ((double) largestDim);
                this.mWidth = (int) (((double) this.mWidth) * scale);
                this.mHeight = (int) (((double) this.mHeight) * scale);
            }
            options.mResourceUri = this.mResourceUri;
            options.mWidth = this.mWidth;
            options.mHeight = this.mHeight;
            options.mContext = this.mContext;
            options.mCacheFlag = this.mCacheFlag;
            options.mBitmapConfig = this.mBitmapConfig;
            options.mPostProcId = this.mPostProcId;
            if (options.mIconResource == null && options.mResourceUri == null) {
                throw new RuntimeException("Both Icon and ResourceUri are null");
            }
            return options;
        }

        public Builder resource(Uri resourceUri) {
            this.mResourceUri = resourceUri;
            return this;
        }

        public Builder width(int width) {
            if (width > 0) {
                this.mWidth = width;
                return this;
            }
            throw new IllegalArgumentException("Can't set width to " + width);
        }

        public Builder height(int height) {
            if (height > 0) {
                this.mHeight = height;
                return this;
            }
            throw new IllegalArgumentException("Can't set height to " + height);
        }

        public Builder cacheFlag(int flag) {
            this.mCacheFlag = flag;
            return this;
        }

        public Builder postProcId(int postProcId) {
            this.mPostProcId = postProcId;
            return this;
        }
    }

    private BitmapWorkerOptions() {
    }

    public Intent.ShortcutIconResource getIconResource() {
        return this.mIconResource;
    }

    public Uri getResourceUri() {
        return this.mResourceUri;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public Context getContext() {
        return this.mContext;
    }

    public boolean isFromResource() {
        if (getIconResource() != null || UriUtils.isAndroidResourceUri(getResourceUri())) {
            return true;
        }
        return UriUtils.isShortcutIconResourceUri(getResourceUri());
    }

    public boolean isMemCacheEnabled() {
        return (this.mCacheFlag & 1) == 0;
    }

    public Bitmap.Config getBitmapConfig() {
        return this.mBitmapConfig;
    }

    public int getPostProcId() {
        return this.mPostProcId;
    }

    @Override // com.dismal.android.recline.util.CachedTaskPool.TaskOption
    public String getCacheKey() {
        String string;
        if (this.mKey == null) {
            if (this.mIconResource != null) {
                string = this.mIconResource.packageName + "/" + this.mIconResource.resourceName;
            } else {
                string = this.mResourceUri.toString();
            }
            this.mKey = string;
            if (this.mPostProcId != 0) {
                this.mKey += " postproc:" + this.mPostProcId;
            }
        }
        return this.mKey;
    }

    @Override // com.dismal.android.recline.util.CachedTaskPool.TaskOption
    public boolean isLocal() {
        return isFromResource();
    }

    public String toString() {
        if (this.mIconResource == null) {
            return "URI: " + this.mResourceUri;
        }
        return "PackageName: " + this.mIconResource.packageName + " Resource: " + this.mIconResource + " URI: " + this.mResourceUri;
    }
}
