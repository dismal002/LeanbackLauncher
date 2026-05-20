package com.dismal.android.leanbacklauncher.widget;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.dismal.android.leanbacklauncher.MainActivity;
import com.dismal.android.leanbacklauncher.util.Preconditions;

/* JADX INFO: loaded from: classes.dex */
public abstract class RowViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final MainActivity mContext;

    protected RowViewAdapter(Context context) {
        this.mContext = (MainActivity) Preconditions.checkNotNull((MainActivity) context);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewDetachedFromWindow(VH holder) {
        super.onViewDetachedFromWindow(holder);
        this.mContext.excludeFromAnimation(holder.itemView);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewAttachedToWindow(final VH holder) {
        super.onViewAttachedToWindow(holder);
        if (!this.mContext.isAnimationInProgress()) {
            return;
        }
        holder.itemView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.dismal.android.leanbacklauncher.widget.RowViewAdapter.1
            @Override // android.view.View.OnLayoutChangeListener
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                RowViewAdapter.this.mContext.includeInAnimation(holder.itemView);
            }
        });
    }
}
