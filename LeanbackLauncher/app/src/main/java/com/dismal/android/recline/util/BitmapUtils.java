package com.dismal.android.recline.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

/* JADX INFO: loaded from: classes.dex */
public class BitmapUtils {
    private BitmapUtils() {
    }

    public static Bitmap applyMaskToBitmap(Resources res, Bitmap image, Bitmap mask, int width, int height, int tileMode) {
        Bitmap bitmap;
        Canvas canvas;
        if (image == null || mask == null) {
            return null;
        }
        int vwidth = image.getWidth();
        int vheight = image.getHeight();
        Rect src = new Rect();
        Rect dest = new Rect();
        if (image.isMutable() && vwidth * height == width * vheight) {
            bitmap = image;
            canvas = new Canvas(bitmap);
        } else if (vwidth * height <= width * vheight) {
            int newHeight = (vwidth * height) / width;
            bitmap = Bitmap.createBitmap(vwidth, newHeight, image.getConfig());
            canvas = new Canvas(bitmap);
            src.set(0, 0, vwidth, newHeight);
            canvas.drawBitmap(image, src, src, (Paint) null);
        } else {
            int newWidth = (width * vheight) / height;
            bitmap = Bitmap.createBitmap(newWidth, vheight, image.getConfig());
            canvas = new Canvas(bitmap);
            int left = (vwidth - newWidth) / 2;
            src.set(left, 0, left + newWidth, vheight);
            dest.set(0, 0, newWidth, vheight);
            canvas.drawBitmap(image, src, dest, (Paint) null);
        }
        if (tileMode == 1) {
            BitmapDrawable maskDrawable = new BitmapDrawable(res, mask);
            maskDrawable.setTileModeX(Shader.TileMode.REPEAT);
            maskDrawable.setTileModeY(Shader.TileMode.CLAMP);
            float scale = canvas.getHeight() / mask.getHeight();
            dest.set(0, 0, (int) (canvas.getWidth() / scale), (int) (canvas.getHeight() / scale));
            maskDrawable.setBounds(dest);
            canvas.scale(scale, scale);
            maskDrawable.draw(canvas);
        } else {
            dest.set(0, 0, canvas.getWidth(), canvas.getHeight());
            src.set(0, 0, mask.getWidth(), mask.getHeight());
            canvas.drawBitmap(mask, src, dest, (Paint) null);
        }
        return bitmap;
    }
}
