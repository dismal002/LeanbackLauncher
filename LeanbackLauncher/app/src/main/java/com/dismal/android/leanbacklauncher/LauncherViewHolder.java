package com.dismal.android.leanbacklauncher;

import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;
import com.dismal.android.leanbacklauncher.core.LaunchException;

/* JADX INFO: loaded from: classes.dex */
public abstract class LauncherViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    protected final Context mCtx;
    private int mLaunchColor;
    private Intent mLaunchIntent;

    protected LauncherViewHolder(View v) {
        super(v);
        if (v != null) {
            this.mCtx = v.getContext();
            v.setOnClickListener(this);
        } else {
            this.mCtx = null;
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        if (v == null || v != this.itemView) {
            return;
        }
        ((MainActivity) this.mCtx).beginLaunchAnimation(v, this.mLaunchColor, new Runnable() { // from class: com.dismal.android.leanbacklauncher.LauncherViewHolder.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    LauncherViewHolder.this.performLaunch();
                } catch (LaunchException e) {
                    Toast.makeText(LauncherViewHolder.this.mCtx, R.string.failed_launch, 0).show();
                    throw new RuntimeException("Could not perform launch", e);
                }
            }
        });
    }

    protected final void setLaunchColor(int launchColor) {
        this.mLaunchColor = launchColor;
    }

    protected final void setLaunchIntent(Intent launchIntent) {
        this.mLaunchIntent = launchIntent;
    }

    protected void performLaunch() {
        try {
            this.mCtx.startActivity(this.mLaunchIntent);
            onLaunchSucceeded();
        } catch (Throwable t) {
            throw new LaunchException("Failed to launch intent: " + this.mLaunchIntent, t);
        }
    }

    protected void onLaunchSucceeded() {
    }
}
