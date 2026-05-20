package com.dismal.android.recline.util;

import com.dismal.android.leanbacklauncher.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.util.SparseArray;
import android.widget.ImageView;
import com.dismal.android.recline.util.CachedTaskPool;
import java.lang.ref.SoftReference;

/* JADX INFO: loaded from: classes.dex */
public class DrawableDownloader {
    private static DrawableDownloader sInstance;
    private static final Object sInstanceLock = new Object();
    private SparseArray<PostProc<Bitmap>> mPostProcs = new SparseArray<>();
    private RecycleBitmapPool mRecycledBitmaps = new RecycleBitmapPool();
    private CachedTaskPool mTaskPool;

    public static abstract class BitmapCallback extends CachedTaskPool.TaskCompleteCallback<Drawable> {
    }

    private static class BitmapItem extends CachedTaskPool.CacheItem<BitmapDrawable> {
        int mOriginalHeight;
        int mOriginalWidth;

        public BitmapItem(int originalWidth, int originalHeight) {
            this.mOriginalWidth = originalWidth;
            this.mOriginalHeight = originalHeight;
        }

        BitmapDrawable findDrawable(BitmapWorkerOptions options) {
            int c = this.mObjects.size();
            for (int i = 0; i < c; i++) {
                BitmapDrawable d = (BitmapDrawable) this.mObjects.get(i);
                if (d.getIntrinsicWidth() == this.mOriginalWidth && d.getIntrinsicHeight() == this.mOriginalHeight) {
                    return d;
                }
                if (options.getHeight() != 2048) {
                    if (options.getHeight() <= d.getIntrinsicHeight()) {
                        return d;
                    }
                } else if (options.getWidth() != 2048 && options.getWidth() <= d.getIntrinsicWidth()) {
                    return d;
                }
            }
            return null;
        }

        /* JADX WARN: Type inference incomplete: some casts might be missing */
        public void addItem(BitmapDrawable bitmapDrawable) {
            int i = 0;
            int size = this.mObjects.size();
            while (i < size && ((BitmapDrawable) this.mObjects.get(i)).getIntrinsicHeight() >= bitmapDrawable.getIntrinsicHeight()) {
                i++;
            }
            this.mObjects.add(i, bitmapDrawable);
            this.mByteCount += RecycleBitmapPool.getSize(bitmapDrawable.getBitmap());
        }

        @Override // com.dismal.android.recline.util.CachedTaskPool.CacheItem
        public void clear() {
            int c = this.mObjects.size();
            for (int i = 0; i < c; i++) {
                BitmapDrawable d = (BitmapDrawable) this.mObjects.get(i);
                if (d instanceof RefcountBitmapDrawable) {
                    ((RefcountBitmapDrawable) d).getRefcountObject().releaseRef();
                }
            }
            this.mObjects.clear();
            this.mByteCount = 0;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.dismal.android.recline.util.CachedTaskPool.CacheItem
        public BitmapDrawable getItem(CachedTaskPool downloader, CachedTaskPool.TaskOption option) {
            BitmapDrawable drawable = findDrawable((BitmapWorkerOptions) option);
            return (BitmapDrawable) DrawableDownloader.createRefCopy(downloader.getContext(), drawable);
        }
    }

    static class CallbackDrawableLoader extends DrawableLoader {
        BitmapCallback mBitmapCallback;

        CallbackDrawableLoader(BitmapCallback callback, RecycleBitmapPool recycledBitmapPool, PostProc<Bitmap> postProc) {
            super(null, recycledBitmapPool, postProc);
            this.mBitmapCallback = callback;
        }

        @Override // com.dismal.android.recline.util.DrawableLoader, android.os.AsyncTask
        protected void onCancelled(Object result) {
            this.mBitmapCallback = null;
            super.onCancelled(result);
        }
    }

    public static final DrawableDownloader getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sInstanceLock) {
                if (sInstance == null) {
                    sInstance = new DrawableDownloader(context);
                }
            }
        }
        return sInstance;
    }

    private DrawableDownloader(Context context) {
        this.mTaskPool = CachedTaskPool.getInstance(context);
    }

    public void registerPostProc(int postProcId, PostProc<Bitmap> postProc) {
        this.mPostProcs.append(postProcId, postProc);
    }

    public PostProc<Bitmap> getPostProc(int postProcId) {
        return this.mPostProcs.get(postProcId);
    }

    public void getBitmap(final BitmapWorkerOptions options, BitmapCallback callback) {
        this.mTaskPool.execute(options, callback, new CachedTaskPool.TaskBuilder() { // from class: com.dismal.android.recline.util.DrawableDownloader.3
            @Override // com.dismal.android.recline.util.CachedTaskPool.TaskBuilder
            public AsyncTask<CachedTaskPool.TaskOption, Void, Object> buildTask(CachedTaskPool.TaskOption option, CachedTaskPool.TaskCompleteCallback callback2) {
                return new CallbackDrawableLoader((BitmapCallback) callback2, DrawableDownloader.this.mRecycledBitmaps, DrawableDownloader.this.getPostProc(options.getPostProcId())) { // from class: com.dismal.android.recline.util.DrawableDownloader.3.1
                    @Override // com.dismal.android.recline.util.DrawableLoader
                    public Drawable doInBackground(CachedTaskPool.TaskOption... params) {
                        Drawable bitmap = super.doInBackground(params);
                        BitmapWorkerOptions options2 = (BitmapWorkerOptions) params[0];
                        if (bitmap != null) {
                            DrawableDownloader.this.addBitmapToMemoryCache(options2, bitmap, this);
                        }
                        return bitmap;
                    }

                    @Override // com.dismal.android.recline.util.DrawableLoader, android.os.AsyncTask
                    protected void onPostExecute(Object bitmap) {
                        this.mBitmapCallback.onCompleted((Drawable) bitmap);
                        this.mBitmapCallback = null;
                    }
                };
            }
        });
    }

    public boolean cancelDownload(Object key) {
        DrawableLoader task = null;
        if (key instanceof ImageView) {
            ImageView imageView = (ImageView) key;
            SoftReference<DrawableLoader> softReference = (SoftReference) imageView.getTag(R.id.lb_image_download_task_tag);
            if (softReference != null) {
                DrawableLoader task2 = softReference.get();
                task = task2;
                softReference.clear();
            }
            if (task != null) {
                return task.cancel(true);
            }
            return false;
        }
        if (key instanceof BitmapCallback) {
            return this.mTaskPool.cancelTask((BitmapCallback) key);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addBitmapToMemoryCache(BitmapWorkerOptions key, Drawable bitmap, DrawableLoader loader) {
        if (!key.isMemCacheEnabled() || !(bitmap instanceof BitmapDrawable)) {
            return;
        }
        String cacheKey = key.getCacheKey();
        LruCache<String, CachedTaskPool.CacheItem> memCache = this.mTaskPool.getMemCache();
        BitmapItem bitmapItem = (BitmapItem) memCache.get(cacheKey);
        if (bitmapItem != null) {
            memCache.remove(cacheKey);
        } else {
            bitmapItem = new BitmapItem(loader.getOriginalWidth(), loader.getOriginalHeight());
        }
        if (bitmap instanceof RefcountBitmapDrawable) {
            RefcountBitmapDrawable refcountDrawable = (RefcountBitmapDrawable) bitmap;
            refcountDrawable.getRefcountObject().addRef();
        }
        bitmapItem.addItem((BitmapDrawable) bitmap);
        memCache.put(cacheKey, bitmapItem);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Drawable createRefCopy(Context context, Drawable d) {
        if (d == null) {
            return null;
        }
        if (d instanceof RefcountBitmapDrawable) {
            RefcountBitmapDrawable refcountDrawable = (RefcountBitmapDrawable) d;
            refcountDrawable.getRefcountObject().addRef();
            return new RefcountBitmapDrawable(context.getResources(), refcountDrawable);
        }
        return d;
    }
}
