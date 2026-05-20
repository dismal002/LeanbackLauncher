package com.dismal.android.leanbacklauncher;

import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class HomeScrollManager {
    private float mFractionFromTop;
    private ArrayList<HomeScrollFractionListener> mScrollListeners = new ArrayList<>();
    private int mScrollPosition;
    private int mScrollThreshold;

    public interface HomeScrollFractionListener {
        void onScrollPositionChanged(int i, float f);
    }

    public void addHomeScrollListener(HomeScrollFractionListener listener) {
        if (this.mScrollListeners.contains(listener)) {
            return;
        }
        this.mScrollListeners.add(listener);
        listener.onScrollPositionChanged(this.mScrollPosition, this.mFractionFromTop);
    }

    public void setFullScrollThreshold(int threshold) {
        this.mScrollThreshold = threshold;
    }

    public void removeHomeScrollListener(HomeScrollFractionListener listener) {
        for (int i = 0; i < this.mScrollListeners.size(); i++) {
            if (this.mScrollListeners.get(i) == listener) {
                this.mScrollListeners.remove(i);
                return;
            }
        }
    }

    public void setScrollOffset(int position) {
        if (this.mScrollPosition == position) {
            return;
        }
        this.mScrollPosition = position;
        if (this.mScrollThreshold > 0) {
            this.mFractionFromTop = Math.max(0.0f, Math.min(1.0f, Math.abs(this.mScrollPosition / this.mScrollThreshold)));
        } else {
            this.mFractionFromTop = 0.0f;
        }
        updateListeners();
    }

    private void updateListeners() {
        for (int i = 0; i < this.mScrollListeners.size(); i++) {
            this.mScrollListeners.get(i).onScrollPositionChanged(this.mScrollPosition, this.mFractionFromTop);
        }
    }
}
