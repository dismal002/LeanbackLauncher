package com.dismal.android.leanbacklauncher;

import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class HomeScreenRow extends RecyclerView.AdapterDataObserver {
    private RecyclerView.Adapter<?> mAdapter;
    private String mFontName;
    private boolean mHasHeader;
    private final boolean mHideIfEmpty;
    private final int mHomeScreenPosition;
    private Drawable mIcon;
    private RowChangeListener mListener;
    private int mRowScrollOffset;
    private String mTitle;
    private final int mType;
    private boolean mVisible;
    private View mRowView = null;

    public interface RowChangeListener {
        void onRowVisibilityChanged(int i, boolean z);
    }

    HomeScreenRow(int type, int position, boolean hideIfEmpty) {
        this.mType = type;
        this.mHomeScreenPosition = position;
        this.mHideIfEmpty = hideIfEmpty;
        this.mVisible = isVisible();
    }

    public int getType() {
        return this.mType;
    }

    public int getPosition() {
        return this.mHomeScreenPosition;
    }

    public void setHeaderInfo(boolean hasHeader, String title, Drawable icon, String fontName) {
        this.mHasHeader = hasHeader;
        if (hasHeader) {
            this.mTitle = title;
            this.mFontName = fontName;
            this.mIcon = icon;
            return;
        }
        this.mTitle = null;
    }

    public boolean hasHeader() {
        return this.mHasHeader;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getFontName() {
        return this.mFontName;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        if (adapter == null) {
            return;
        }
        if (this.mHideIfEmpty && this.mAdapter != null) {
            this.mAdapter.unregisterAdapterDataObserver(this);
        }
        this.mAdapter = adapter;
        this.mVisible = isVisible();
        if (!this.mHideIfEmpty || this.mAdapter == null) {
            return;
        }
        this.mAdapter.registerAdapterDataObserver(this);
    }

    public RecyclerView.Adapter<?> getAdapter() {
        return this.mAdapter;
    }

    public void setViewScrollOffset(int size) {
        this.mRowScrollOffset = size;
    }

    public int getRowScrollOffset() {
        return this.mRowScrollOffset;
    }

    public void setChangeListener(RowChangeListener listener) {
        this.mListener = listener;
    }

    public View getRowView() {
        return this.mRowView;
    }

    public void setRowView(View view) {
        this.mRowView = view;
    }

    public boolean isVisible() {
        if (this.mHideIfEmpty) {
            return this.mAdapter != null && this.mAdapter.getItemCount() > 0;
        }
        return true;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        onChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
    public void onItemRangeChanged(int positionStart, int itemCount) {
        onChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
    public void onItemRangeInserted(int positionStart, int itemCount) {
        onChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
    public void onChanged() {
        if (this.mVisible == isVisible()) {
            return;
        }
        this.mVisible = !this.mVisible;
        if (this.mListener == null) {
            return;
        }
        this.mListener.onRowVisibilityChanged(this.mHomeScreenPosition, this.mVisible);
    }
}
