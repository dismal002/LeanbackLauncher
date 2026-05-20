package com.dismal.android.recline.util;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ByteArrayPool {
    private static final ByteArrayPool sChunk16K = new ByteArrayPool(16384, 8);
    private final ArrayList<byte[]> mCachedBuf;
    private final int mChunkSize;
    private final int mMaxNum;

    private ByteArrayPool(int chunkSize, int maxNum) {
        this.mChunkSize = chunkSize;
        this.mMaxNum = maxNum;
        this.mCachedBuf = new ArrayList<>(this.mMaxNum);
    }

    public static ByteArrayPool get16KBPool() {
        return sChunk16K;
    }

    public byte[] allocateChunk() {
        synchronized (this.mCachedBuf) {
            int size = this.mCachedBuf.size();
            if (size > 0) {
                return this.mCachedBuf.remove(size - 1);
            }
            return new byte[this.mChunkSize];
        }
    }

    public void releaseChunk(byte[] buf) {
        if (buf == null || buf.length != this.mChunkSize) {
            return;
        }
        synchronized (this.mCachedBuf) {
            if (this.mCachedBuf.size() < this.mMaxNum) {
                this.mCachedBuf.add(buf);
            }
        }
    }

    public void releaseChunks(List<byte[]> bufs) {
        synchronized (this.mCachedBuf) {
            int c = bufs.size();
            for (int i = 0; i < c; i++) {
                if (this.mCachedBuf.size() == this.mMaxNum) {
                    break;
                }
                byte[] buf = bufs.get(i);
                if (buf != null && buf.length == this.mChunkSize) {
                    this.mCachedBuf.add(bufs.get(i));
                }
            }
        }
    }
}
