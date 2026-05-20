package com.dismal.android.recline.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.LruCache;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes.dex */
public class CachedTaskPool {
    private static CachedTaskPool sInstance;
    private Context mContext;
    private final LruCache<String, CacheItem> mMemoryCache;
    private static final Executor DOWNLOADER_THREAD_POOL_EXECUTOR = Executors.newFixedThreadPool(5);
    private static final Executor LOCAL_DOWNLOADER_THREAD_POOL_EXECUTOR = Executors.newFixedThreadPool(1);
    private static final Object sInstanceLock = new Object();

    public static abstract class CacheItem<T> {
        protected int mByteCount;
        protected ArrayList<T> mObjects = new ArrayList<>(3);

        public abstract void clear();

        public abstract T getItem(CachedTaskPool cachedTaskPool, TaskOption taskOption);
    }

    public interface TaskBuilder<T> {
        AsyncTask<TaskOption, Void, T> buildTask(TaskOption taskOption, TaskCompleteCallback<T> taskCompleteCallback);
    }

    public static abstract class TaskCompleteCallback<T> {
        SoftReference<AsyncTask<TaskOption, Void, T>> mTask;

        public abstract void onCompleted(T t);
    }

    public interface TaskOption {
        String getCacheKey();

        boolean isLocal();
    }

    public static final CachedTaskPool getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sInstanceLock) {
                if (sInstance == null) {
                    sInstance = new CachedTaskPool(context);
                }
            }
        }
        return sInstance;
    }

    private CachedTaskPool(Context context) {
        this.mContext = context.getApplicationContext();
        int memClass = ((ActivityManager) context.getSystemService("activity")).getMemoryClass() / 4;
        int cacheSize = 1048576 * (memClass > 32 ? 32 : memClass);
        this.mMemoryCache = new LruCache<String, CacheItem>(cacheSize) { // from class: com.dismal.android.recline.util.CachedTaskPool.1
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public int sizeOf(String key, CacheItem item) {
                return item.mByteCount;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public void entryRemoved(boolean evicted, String key, CacheItem oldValue, CacheItem newValue) {
                if (!evicted) {
                    return;
                }
                oldValue.clear();
            }
        };
    }

    public Object getObjectFromMemCache(TaskOption options) {
        CacheItem item = this.mMemoryCache.get(options.getCacheKey());
        if (item != null) {
            return item.getItem(this, options);
        }
        return null;
    }

    public void execute(TaskOption options, TaskCompleteCallback callback, TaskBuilder taskBuilder) {
        cancelTask(callback);
        Object object = getObjectFromMemCache(options);
        if (object != null) {
            callback.onCompleted(object);
            return;
        }
        AsyncTask<TaskOption, Void, Object> task = taskBuilder.buildTask(options, callback);
        callback.mTask = new SoftReference<>(task);
        scheduleTask(task, options);
    }

    public static void scheduleTask(AsyncTask<TaskOption, Void, Object> task, TaskOption options) {
        if (options.isLocal()) {
            task.executeOnExecutor(LOCAL_DOWNLOADER_THREAD_POOL_EXECUTOR, options);
        } else {
            task.executeOnExecutor(DOWNLOADER_THREAD_POOL_EXECUTOR, options);
        }
    }

    public boolean cancelTask(TaskCompleteCallback key) {
        AsyncTask task;
        if (key.mTask != null && (task = (AsyncTask) key.mTask.get()) != null) {
            return task.cancel(true);
        }
        return false;
    }

    public Context getContext() {
        return this.mContext;
    }

    public LruCache<String, CacheItem> getMemCache() {
        return this.mMemoryCache;
    }
}
