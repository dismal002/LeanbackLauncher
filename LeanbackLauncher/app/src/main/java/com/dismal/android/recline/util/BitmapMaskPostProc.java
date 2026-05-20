package com.dismal.android.recline.util;

import android.content.res.Resources;
import android.graphics.Bitmap;

/* JADX INFO: loaded from: classes.dex */
public class BitmapMaskPostProc implements PostProc<Bitmap> {
    protected int mHeight;
    protected final Bitmap mMask;
    protected Resources mResources;
    protected int mTileMode;
    protected int mWidth;

    public BitmapMaskPostProc(Resources res, Bitmap mask, int width, int height, int tileMode) {
        this.mResources = res;
        this.mMask = mask;
        this.mWidth = width;
        this.mHeight = height;
        this.mTileMode = tileMode;
    }

    @Override // com.dismal.android.recline.util.PostProc
    public Bitmap postProcess(Bitmap bitmap) {
        return BitmapUtils.applyMaskToBitmap(this.mResources, bitmap, this.mMask, this.mWidth, this.mHeight, this.mTileMode);
    }
}
