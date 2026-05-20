package com.dismal.android.recline.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/* JADX INFO: loaded from: classes.dex */
public class RefcountBitmapDrawable extends BitmapDrawable {
    private RefcountObject<Bitmap> mRefcountObject;

    public RefcountBitmapDrawable(Resources res, RefcountObject<Bitmap> bitmap) {
        super(res, bitmap.getObject());
        this.mRefcountObject = bitmap;
    }

    public RefcountBitmapDrawable(Resources res, RefcountBitmapDrawable drawable) {
        this(res, drawable.getRefcountObject());
    }

    public RefcountObject<Bitmap> getRefcountObject() {
        return this.mRefcountObject;
    }
}
