package com.dismal.android.recline.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class CachedInputStream extends FilterInputStream {
    private ArrayList<byte[]> mBufs;
    private int mCount;
    private int mMarkLimit;
    private int mMarkPos;
    private int mOverrideMarkLimit;
    private int mPos;
    private byte[] tmp;

    public CachedInputStream(InputStream in) {
        super(in);
        this.mBufs = new ArrayList<>();
        this.mPos = 0;
        this.mCount = 0;
        this.mMarkPos = -1;
        this.tmp = new byte[1];
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public boolean markSupported() {
        return true;
    }

    public void setOverrideMarkLimit(int overrideMarkLimit) {
        this.mOverrideMarkLimit = overrideMarkLimit;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public void mark(int readlimit) {
        int chunks;
        if (readlimit < this.mOverrideMarkLimit) {
            readlimit = this.mOverrideMarkLimit;
        }
        if (this.mMarkPos >= 0 && (chunks = this.mPos / 16384) > 0) {
            int removedBytes = chunks * 16384;
            List<byte[]> subList = this.mBufs.subList(0, chunks);
            releaseChunks(subList);
            subList.clear();
            this.mPos -= removedBytes;
            this.mCount -= removedBytes;
        }
        this.mMarkPos = this.mPos;
        this.mMarkLimit = readlimit;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public void reset() throws IOException {
        if (this.mMarkPos < 0) {
            throw new IOException("mark has been invalidated");
        }
        this.mPos = this.mMarkPos;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        int r = read(this.tmp, 0, 1);
        if (r <= 0) {
            return -1;
        }
        return this.tmp[0] & 255;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        if (this.in != null) {
            this.in.close();
            this.in = null;
        }
        releaseChunks(this.mBufs);
    }

    private static void releaseChunks(List<byte[]> bufs) {
        ByteArrayPool.get16KBPool().releaseChunks(bufs);
    }

    private byte[] allocateChunk() {
        return ByteArrayPool.get16KBPool().allocateChunk();
    }

    private boolean invalidate() {
        if (this.mCount - this.mMarkPos <= this.mMarkLimit) {
            return false;
        }
        this.mMarkPos = -1;
        this.mCount = 0;
        this.mPos = 0;
        releaseChunks(this.mBufs);
        this.mBufs.clear();
        return true;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] buffer, int offset, int count) throws IOException {
        if (this.in == null) {
            throw streamClosed();
        }
        if (this.mMarkPos == -1) {
            return this.in.read(buffer, offset, count);
        }
        if (count == 0) {
            return 0;
        }
        int copied = copyMarkedBuffer(buffer, offset, count);
        int count2 = count - copied;
        int offset2 = offset + copied;
        int totalReads = copied;
        while (true) {
            if (count2 > 0) {
                if (this.mPos == this.mBufs.size() * 16384) {
                    this.mBufs.add(allocateChunk());
                }
                int currentBuf = this.mPos / 16384;
                int indexInBuf = this.mPos - (currentBuf * 16384);
                byte[] buf = this.mBufs.get(currentBuf);
                int end = (currentBuf + 1) * 16384;
                int leftInBuffer = end - this.mPos;
                int toRead = count2 > leftInBuffer ? leftInBuffer : count2;
                int reads = this.in.read(buf, indexInBuf, toRead);
                if (reads <= 0) {
                    break;
                }
                System.arraycopy(buf, indexInBuf, buffer, offset2, reads);
                this.mPos += reads;
                this.mCount += reads;
                totalReads += reads;
                offset2 += reads;
                count2 -= reads;
                if (invalidate()) {
                    int reads2 = this.in.read(buffer, offset2, count2);
                    if (reads2 > 0) {
                        totalReads += reads2;
                    }
                }
            } else {
                break;
            }
        }
        if (totalReads == 0) {
            return -1;
        }
        return totalReads;
    }

    private int copyMarkedBuffer(byte[] buffer, int offset, int read) {
        int totalRead = 0;
        while (read > 0 && this.mPos < this.mCount) {
            int currentBuf = this.mPos / 16384;
            int indexInBuf = this.mPos - (currentBuf * 16384);
            byte[] buf = this.mBufs.get(currentBuf);
            int end = (currentBuf + 1) * 16384;
            if (end > this.mCount) {
                end = this.mCount;
            }
            int leftInBuffer = end - this.mPos;
            int toRead = read > leftInBuffer ? leftInBuffer : read;
            System.arraycopy(buf, indexInBuf, buffer, offset, toRead);
            offset += toRead;
            read -= toRead;
            totalRead += toRead;
            this.mPos += toRead;
        }
        return totalRead;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        if (this.in == null) {
            throw streamClosed();
        }
        return (this.mCount - this.mPos) + this.in.available();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public long skip(long byteCount) throws IOException {
        if (this.in == null) {
            throw streamClosed();
        }
        if (this.mMarkPos < 0) {
            return this.in.skip(byteCount);
        }
        long totalSkip = this.mCount - this.mPos;
        if (totalSkip > byteCount) {
            totalSkip = byteCount;
        }
        this.mPos = (int) (((long) this.mPos) + totalSkip);
        long byteCount2 = byteCount - totalSkip;
        while (byteCount2 > 0) {
            if (this.mPos == this.mBufs.size() * 16384) {
                this.mBufs.add(allocateChunk());
            }
            int currentBuf = this.mPos / 16384;
            int indexInBuf = this.mPos - (currentBuf * 16384);
            byte[] buf = this.mBufs.get(currentBuf);
            int end = (currentBuf + 1) * 16384;
            int leftInBuffer = end - this.mPos;
            int toRead = (int) (byteCount2 > ((long) leftInBuffer) ? leftInBuffer : byteCount2);
            int reads = this.in.read(buf, indexInBuf, toRead);
            if (reads > 0) {
                this.mPos += reads;
                this.mCount += reads;
                byteCount2 -= (long) reads;
                totalSkip += (long) reads;
                if (invalidate()) {
                    if (byteCount2 > 0) {
                        return totalSkip + this.in.skip(byteCount2);
                    }
                    return totalSkip;
                }
            } else {
                return totalSkip;
            }
        }
        return totalSkip;
    }

    private static IOException streamClosed() {
        return new IOException("stream closed");
    }
}
