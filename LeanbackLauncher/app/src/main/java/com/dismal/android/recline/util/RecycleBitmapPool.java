package com.dismal.android.recline.util;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class RecycleBitmapPool {
    private static Method sGetAllocationByteCount;
    private final SparseArray<ArrayList<SoftReference<Bitmap>>> mRecycled8888 = new SparseArray<>();

    static {
        try {
            sGetAllocationByteCount = Bitmap.class.getMethod("getAllocationByteCount", new Class[0]);
        } catch (NoSuchMethodException e) {
        }
    }

    public static int getSize(Bitmap bitmap) {
        if (sGetAllocationByteCount != null) {
            try {
                return ((Integer) sGetAllocationByteCount.invoke(bitmap, new Object[0])).intValue();
            } catch (IllegalAccessException e) {
                Log.e("RecycleBitmapPool", "getAllocationByteCount() failed", e);
                sGetAllocationByteCount = null;
                return bitmap.getByteCount();
            } catch (IllegalArgumentException e2) {
                Log.e("RecycleBitmapPool", "getAllocationByteCount() failed", e2);
                sGetAllocationByteCount = null;
                return bitmap.getByteCount();
            } catch (InvocationTargetException e3) {
                Log.e("RecycleBitmapPool", "getAllocationByteCount() failed", e3);
                sGetAllocationByteCount = null;
                return bitmap.getByteCount();
            }
        }
        return bitmap.getByteCount();
    }

    private static int getSize(int width, int height) {
        if (width >= 2048 || height >= 2048) {
            return 0;
        }
        return width * height * 4;
    }

    public void addRecycledBitmap(Bitmap bitmap) {
        int key;
        if (bitmap.isRecycled()) {
            return;
        }
        Bitmap.Config config = bitmap.getConfig();
        if (config != Bitmap.Config.ARGB_8888 || (key = getSize(bitmap)) == 0) {
            return;
        }
        synchronized (this.mRecycled8888) {
            ArrayList<SoftReference<Bitmap>> list = this.mRecycled8888.get(key);
            if (list == null) {
                list = new ArrayList<>();
                this.mRecycled8888.put(key, list);
            }
            list.add(new SoftReference<>(bitmap));
        }
    }

    public Bitmap getRecycledBitmap(int width, int height) {
        int key = getSize(width, height);
        if (key == 0) {
            return null;
        }
        synchronized (this.mRecycled8888) {
            Bitmap bitmap = getRecycledBitmap(this.mRecycled8888.get(key));
            if (sGetAllocationByteCount == null || bitmap != null) {
                return bitmap;
            }
            return null;
        }
    }

    private static Bitmap getRecycledBitmap(ArrayList<SoftReference<Bitmap>> list) {
        if (list != null && !list.isEmpty()) {
            while (!list.isEmpty()) {
                SoftReference<Bitmap> ref = list.remove(list.size() - 1);
                Bitmap bitmap = ref.get();
                if (bitmap != null && !bitmap.isRecycled()) {
                    return bitmap;
                }
            }
        }
        return null;
    }
}
