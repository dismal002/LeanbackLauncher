package com.dismal.android.leanbacklauncher.wallpaper;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.dismal.android.recline.util.RefcountBitmapDrawable;

/* JADX INFO: loaded from: classes.dex */
public class WallpaperImage extends ImageView {
    protected float mZoom;

    public WallpaperImage(Context context) {
        this(context, null);
    }

    public WallpaperImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mZoom = 0.0f;
    }

    @Override // android.widget.ImageView
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        setScaleMatrix(getDrawable());
        return changed;
    }

    @Override // android.widget.ImageView
    public void setImageDrawable(Drawable drawable) {
        Drawable previousDrawable = getDrawable();
        addRef(drawable);
        super.setImageDrawable(drawable);
        releaseRef(previousDrawable);
    }

    private static void releaseRef(Drawable drawable) {
        if (!(drawable instanceof RefcountBitmapDrawable)) {
            return;
        }
        ((RefcountBitmapDrawable) drawable).getRefcountObject().releaseRef();
    }

    private static void addRef(Drawable drawable) {
        if (!(drawable instanceof RefcountBitmapDrawable)) {
            return;
        }
        ((RefcountBitmapDrawable) drawable).getRefcountObject().addRef();
    }

    public void setZoomLevel(float zoom) {
        this.mZoom = zoom;
        setScaleMatrix(getDrawable());
        invalidate();
    }

    private void setScaleMatrix(Drawable drawable) {
        float scale;
        float dx;
        if (drawable == null) {
            return;
        }
        Matrix matrix = getImageMatrix();
        int vwidth = (getWidth() - getPaddingLeft()) - getPaddingRight();
        int vheight = (getHeight() - getPaddingTop()) - getPaddingBottom();
        int dwidth = drawable.getIntrinsicWidth();
        int dheight = drawable.getIntrinsicHeight();
        if (dwidth * vheight > vwidth * dheight) {
            scale = (vheight / dheight) * (this.mZoom + 1.0f);
            dx = (vwidth - (dwidth * scale)) * 0.5f;
        } else {
            scale = (vwidth / dwidth) * (this.mZoom + 1.0f);
            dx = vwidth * this.mZoom * 0.5f * (-1.0f);
        }
        float dy = vheight * this.mZoom * 0.5f * (-1.0f);
        matrix.setScale(scale, scale);
        matrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        setImageMatrix(matrix);
    }
}
