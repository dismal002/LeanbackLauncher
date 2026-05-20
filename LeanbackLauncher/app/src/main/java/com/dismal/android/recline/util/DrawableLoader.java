package com.dismal.android.recline.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import com.dismal.android.recline.util.CachedTaskPool;
import com.dismal.android.recline.util.RefcountObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/* JADX INFO: loaded from: classes.dex */
class DrawableLoader extends AsyncTask<CachedTaskPool.TaskOption, Void, Object> {
    private WeakReference<ImageView> mImageView;
    private int mOriginalHeight;
    private int mOriginalWidth;
    private PostProc<Bitmap> mPostProc;
    private RecycleBitmapPool mRecycledBitmaps;
    private RefcountObject.RefcountListener mRefcountListener = new RefcountObject.RefcountListener() { // from class: com.dismal.android.recline.util.DrawableLoader.1
        @Override // com.dismal.android.recline.util.RefcountObject.RefcountListener
        public void onRefcountZero(RefcountObject object) {
            DrawableLoader.this.mRecycledBitmaps.addRecycledBitmap((Bitmap) object.getObject());
        }
    };

    DrawableLoader(ImageView imageView, RecycleBitmapPool recycledBitmapPool, PostProc<Bitmap> postProc) {
        this.mImageView = new WeakReference<>(imageView);
        this.mRecycledBitmaps = recycledBitmapPool;
        this.mPostProc = postProc;
    }

    public int getOriginalWidth() {
        return this.mOriginalWidth;
    }

    public int getOriginalHeight() {
        return this.mOriginalHeight;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // android.os.AsyncTask
    public Drawable doInBackground(CachedTaskPool.TaskOption... params) {
        return retrieveDrawable((BitmapWorkerOptions) params[0]);
    }

    protected Drawable retrieveDrawable(BitmapWorkerOptions workerOptions) {
        try {
            if (workerOptions.getIconResource() != null) {
                return getBitmapFromResource(workerOptions.getIconResource(), workerOptions);
            }
            if (workerOptions.getResourceUri() != null) {
                if (UriUtils.isAndroidResourceUri(workerOptions.getResourceUri()) || UriUtils.isShortcutIconResourceUri(workerOptions.getResourceUri())) {
                    return getBitmapFromResource(UriUtils.getIconResource(workerOptions.getResourceUri()), workerOptions);
                }
                if (UriUtils.isWebUri(workerOptions.getResourceUri())) {
                    return getBitmapFromHttp(workerOptions);
                }
                if (UriUtils.isContentUri(workerOptions.getResourceUri())) {
                    return getBitmapFromContent(workerOptions);
                }
                Log.e("DrawableLoader", "Error loading bitmap - unknown resource URI! " + workerOptions.getResourceUri());
            } else {
                Log.e("DrawableLoader", "Error loading bitmap - no source!");
            }
            return null;
        } catch (IOException e) {
            Log.e("DrawableLoader", "Error loading url " + workerOptions.getResourceUri(), e);
            return null;
        } catch (RuntimeException e2) {
            Log.e("DrawableLoader", "Critical Error loading url " + workerOptions.getResourceUri(), e2);
            return null;
        }
    }

    @Override // android.os.AsyncTask
    protected void onPostExecute(Object bitmap) {
        ImageView imageView;
        if (this.mImageView == null || (imageView = this.mImageView.get()) == null) {
            return;
        }
        imageView.setImageDrawable((Drawable) bitmap);
    }

    @Override // android.os.AsyncTask
    protected void onCancelled(Object result) {
        if (!(result instanceof RefcountBitmapDrawable)) {
            return;
        }
        RefcountBitmapDrawable d = (RefcountBitmapDrawable) result;
        d.getRefcountObject().releaseRef();
    }

    private Drawable getBitmapFromResource(Intent.ShortcutIconResource iconResource, BitmapWorkerOptions outputOptions) throws IOException {
        String str = iconResource.packageName;
        String str2 = iconResource.resourceName;
        try {
            Object drawable = loadDrawable(outputOptions.getContext(), iconResource);
            if (drawable instanceof InputStream) {
                return decodeBitmap((InputStream) drawable, outputOptions);
            }
            if (drawable instanceof Drawable) {
                Drawable d = (Drawable) drawable;
                this.mOriginalWidth = d.getIntrinsicWidth();
                this.mOriginalHeight = d.getIntrinsicHeight();
                return d;
            }
            Log.w("DrawableLoader", "getBitmapFromResource failed, unrecognized resource: " + drawable);
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("DrawableLoader", "Could not load package: " + iconResource.packageName + "! NameNotFound");
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.w("DrawableLoader", "Could not load resource: " + iconResource.resourceName + "! NotFound");
            return null;
        }
    }

    private Drawable decodeBitmap(InputStream in, BitmapWorkerOptions options) throws IOException {
        CachedInputStream bufferedStream = null;
        BitmapFactory.Options bitmapOptions = null;
        try {
            bufferedStream = new CachedInputStream(in);
            bufferedStream.setOverrideMarkLimit(Integer.MAX_VALUE);
            bitmapOptions = new BitmapFactory.Options();
            
            bitmapOptions.inJustDecodeBounds = true;
            if (options.getBitmapConfig() != null) {
                bitmapOptions.inPreferredConfig = options.getBitmapConfig();
            }
            bitmapOptions.inTempStorage = ByteArrayPool.get16KBPool().allocateChunk();
            bufferedStream.mark(Integer.MAX_VALUE);
            BitmapFactory.decodeStream(bufferedStream, null, bitmapOptions);
            this.mOriginalWidth = bitmapOptions.outWidth;
            this.mOriginalHeight = bitmapOptions.outHeight;
            int heightScale = 1;
            int height = options.getHeight();
            if (height > 0) {
                heightScale = bitmapOptions.outHeight / height;
            }
            int widthScale = 1;
            int width = options.getWidth();
            if (width > 0) {
                widthScale = bitmapOptions.outWidth / width;
            }
            int scale2 = heightScale > widthScale ? heightScale : widthScale;
            int scale;
            if (scale2 <= 1) {
                scale = 1;
            } else {
                int shift = 0;
                do {
                    scale2 >>= 1;
                    shift++;
                } while (scale2 != 0);
                scale = 1 << (shift - 1);
            }
            bufferedStream.reset();
            bufferedStream.setOverrideMarkLimit(0);
            Bitmap bitmap;
            try {
                bitmapOptions.inJustDecodeBounds = false;
                bitmapOptions.inSampleSize = scale;
                bitmapOptions.inMutable = true;
                bitmapOptions.inBitmap = this.mRecycledBitmaps.getRecycledBitmap(this.mOriginalWidth / scale, this.mOriginalHeight / scale);
                bitmap = BitmapFactory.decodeStream(bufferedStream, null, bitmapOptions);
            } catch (RuntimeException ex) {
                Log.e("DrawableLoader", "RuntimeException" + ex + ", trying decodeStream again");
                bufferedStream.reset();
                bufferedStream.setOverrideMarkLimit(0);
                bitmapOptions.inBitmap = null;
                bitmap = BitmapFactory.decodeStream(bufferedStream, null, bitmapOptions);
            }
            if (bitmap == null) {
                Log.v("DrawableLoader", "bitmap was null");
                return null;
            }
            if (this.mPostProc != null) {
                bitmap = this.mPostProc.postProcess(bitmap);
            }
            RefcountObject<Bitmap> object = new RefcountObject<>(bitmap);
            object.addRef();
            object.setRefcountListener(this.mRefcountListener);
            return new RefcountBitmapDrawable(options.getContext().getResources(), object);
        } finally {
            if (bitmapOptions != null && bitmapOptions.inTempStorage != null) {
                ByteArrayPool.get16KBPool().releaseChunk(bitmapOptions.inTempStorage);
            }
            if (bufferedStream != null) {
                try {
                    bufferedStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private Drawable getBitmapFromHttp(BitmapWorkerOptions options) throws IOException {
        URL url = new URL(options.getResourceUri().toString());
        try {
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            InputStream in = connection.getInputStream();
            return decodeBitmap(in, options);
        } catch (SocketTimeoutException e) {
            Log.e("DrawableLoader", "loading " + url + " timed out");
            return null;
        }
    }

    private Drawable getBitmapFromContent(BitmapWorkerOptions options) throws IOException {
        Uri resourceUri = options.getResourceUri();
        if (resourceUri != null) {
            try {
                InputStream bitmapStream = options.getContext().getContentResolver().openInputStream(resourceUri);
                if (bitmapStream != null) {
                    return decodeBitmap(bitmapStream, options);
                }
                Log.w("DrawableLoader", "Content provider returned a null InputStream when trying to open resource.");
                return null;
            } catch (FileNotFoundException e) {
                Log.e("DrawableLoader", "FileNotFoundException during openInputStream for uri: " + resourceUri.toString());
                return null;
            }
        }
        Log.w("DrawableLoader", "Get null resourceUri from BitmapWorkerOptions.");
        return null;
    }

    private static Object loadDrawable(Context context, Intent.ShortcutIconResource r) throws PackageManager.NameNotFoundException {
        Resources resources = context.getPackageManager().getResourcesForApplication(r.packageName);
        if (resources == null) {
            return null;
        }
        int id = resources.getIdentifier(r.resourceName, null, null);
        if (id == 0) {
            Log.e("DrawableLoader", "Couldn't get resource " + r.resourceName + " in resources of " + r.packageName);
            return null;
        }
        TypedValue value = new TypedValue();
        resources.getValue(id, value, true);
        if ((value.type == 3 && value.string.toString().endsWith(".xml")) || (value.type >= 28 && value.type <= 31)) {
            return resources.getDrawable(id);
        }
        return resources.openRawResource(id, value);
    }
}
