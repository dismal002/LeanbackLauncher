package com.dismal.android.leanbacklauncher;

import android.content.Context;
import android.content.res.Resources;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.dismal.android.leanbacklauncher.apps.AppsAdapter;

/* JADX INFO: loaded from: classes.dex */
public class ActiveItemsRowView extends HorizontalGridView implements OnChildSelectedListener, ViewGroup.OnHierarchyChangeListener {
    protected boolean mActiveRow;
    private int mCardSpacing;
    RecyclerView.AdapterDataObserver mChangeObserver;
    private View mCurView;
    private boolean mIsAdjustable;
    private RowCountChangeListener mListener;
    private int mNumRows;
    private int mRowHeight;

    public interface RowCountChangeListener {
        void onRowCountChanged();
    }

    public ActiveItemsRowView(Context context) {
        this(context, null, 0);
    }

    public ActiveItemsRowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActiveItemsRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChangeObserver = new RecyclerView.AdapterDataObserver() { // from class: com.dismal.android.leanbacklauncher.ActiveItemsRowView.1
            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onChanged() {
                ActiveItemsRowView.this.adjustNumRows();
            }

            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onItemRangeInserted(int positionStart, int itemCount) {
                ActiveItemsRowView.this.adjustNumRows();
            }

            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                ActiveItemsRowView.this.adjustNumRows();
            }
        };
        setChildrenDrawingOrderEnabled(true);
        setOnChildSelectedListener(this);
        setAnimateChildLayout(true);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterAdapterDataObserver(this.mChangeObserver);
        }
        super.setAdapter(adapter);
        if (adapter == null) {
            return;
        }
        adapter.registerAdapterDataObserver(this.mChangeObserver);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.view.ViewGroup
    public void addView(View view, int index, ViewGroup.LayoutParams lp) {
        super.addView(view, index, lp);
        view.setActivated(this.mActiveRow);
        if (view instanceof DimmableItem) {
            ((DimmableItem) view).setDimState(this.mActiveRow, false);
        }
        view.setZ(getResources().getDimensionPixelOffset(R.dimen.unselected_item_z));
    }

    public int getNumRows() {
        return this.mNumRows;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.view.View
    public void setActivated(boolean activated) {
        if (this.mActiveRow == activated) {
            return;
        }
        this.mActiveRow = activated;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                childAt.setActivated(activated);
                if (childAt instanceof DimmableItem) {
                    ((DimmableItem) childAt).setDimState(activated, false);
                }
            }
        }
    }

    @Override // androidx.leanback.widget.OnChildSelectedListener
    public void onChildSelected(ViewGroup parent, View child, int position, long id) {
        if (child != this.mCurView) {
            this.mCurView = child;
        }
        postInvalidateDelayed(50L);
    }

    public void setIsNumRowsAdjustable(boolean isAdjustable) {
        this.mIsAdjustable = isAdjustable;
        setOnHierarchyChangeListener(isAdjustable ? this : null);
    }

    public void adjustNumRows(int numRows, int cardSpacing, int rowHeight) {
        if (!this.mIsAdjustable || this.mNumRows == numRows) {
            return;
        }
        this.mNumRows = numRows;
        this.mCardSpacing = cardSpacing;
        this.mRowHeight = rowHeight;
        post(new Runnable() { // from class: com.dismal.android.leanbacklauncher.ActiveItemsRowView.2
            @Override // java.lang.Runnable
            public void run() {
                ViewGroup.LayoutParams lp = ActiveItemsRowView.this.getLayoutParams();
                int padding = ActiveItemsRowView.this.getPaddingTop() + ActiveItemsRowView.this.getPaddingBottom();
                lp.height = (ActiveItemsRowView.this.mNumRows * ActiveItemsRowView.this.mRowHeight) + ((ActiveItemsRowView.this.mNumRows - 1) * ActiveItemsRowView.this.mCardSpacing) + padding;
                ActiveItemsRowView.this.setNumRows(ActiveItemsRowView.this.mNumRows);
                ActiveItemsRowView.this.setRowHeight(ActiveItemsRowView.this.mRowHeight);
                if (ActiveItemsRowView.this.mListener == null) {
                    return;
                }
                ActiveItemsRowView.this.mListener.onRowCountChanged();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void adjustNumRows() {
        int integer;
        Resources res = getResources();
        if (getAdapter().getItemCount() >= res.getInteger(R.integer.two_row_cut_off)) {
            integer = res.getInteger(R.integer.max_num_banner_rows);
        } else {
            integer = res.getInteger(R.integer.min_num_banner_rows);
        }
        adjustNumRows(integer, this.mCardSpacing, this.mRowHeight);
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    @Override // android.view.ViewGroup.OnHierarchyChangeListener
    public void onChildViewAdded(View parent, View child) {
        adjustNumRows();
    }

    @Override // android.view.ViewGroup.OnHierarchyChangeListener
    public void onChildViewRemoved(View parent, View child) {
        adjustNumRows();
    }

    public void resetRowIfNeeded(boolean force) {
        if (!(getAdapter() instanceof AppsAdapter)) {
            return;
        }
        AppsAdapter adapter = (AppsAdapter) getAdapter();
        if (!adapter.sortItemsIfNeeded(force)) {
            return;
        }
        setSelectedPosition(0);
    }
}
