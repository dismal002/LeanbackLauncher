package com.dismal.android.leanbacklauncher.apps;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.LauncherViewHolder;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import com.dismal.android.leanbacklauncher.util.Lists;
import com.dismal.android.leanbacklauncher.widget.RowViewAdapter;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AppsAdapter extends RowViewAdapter<AppsAdapter.AppViewHolder> implements Ranker.RankingListener, LaunchPointListGenerator.Listener {
    protected final int mAppType;
    protected boolean mFlaggedForResort;
    protected final LayoutInflater mInflater;
    private LaunchPointListGenerator mLaunchPointGen;
    protected ArrayList<LaunchPoint> mLaunchPoints;
    private Handler mNotifyHandler;
    final Ranker mRanker;

    public AppsAdapter(Context context, int appType, LaunchPointListGenerator launchPointListGenerator, Ranker ranker) {
        super(context);
        this.mNotifyHandler = new Handler();
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mLaunchPoints = new ArrayList<>();
        this.mRanker = ranker;
        this.mAppType = appType;
        this.mFlaggedForResort = false;
        this.mLaunchPointGen = launchPointListGenerator;
        this.mLaunchPointGen.registerChangeListener(this);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        if (position >= this.mLaunchPoints.size()) {
            Log.e("AppsAdapter", "getItemViewType with out of bounds index = " + position);
            return this.mAppType != 2 ? 0 : 2;
        }
        LaunchPoint launchPoint = this.mLaunchPoints.get(position);
        if (this.mAppType == 2) {
            return 2;
        }
        return launchPoint.hasBanner() ? 0 : 1;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0:
                return new AppBannerViewHolder(this.mInflater.inflate(R.layout.app_banner, parent, false), this);
            case 1:
                return new AppFallbackViewHolder(this.mInflater.inflate(R.layout.app_fallback_banner, parent, false), this);
            case 2:
                return new SettingViewHolder(this.mInflater.inflate(R.layout.setting, parent, false), this);
            default:
                return null;
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(AppViewHolder holder, int position) {
        if (position >= this.mLaunchPoints.size()) {
            return;
        }
        LaunchPoint launchPoint = this.mLaunchPoints.get(position);
        holder.init(launchPoint);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mLaunchPoints.size();
    }

    public boolean sortItemsIfNeeded(boolean force) {
        boolean z = !this.mFlaggedForResort ? force : true;
        this.mFlaggedForResort = false;
        if (z) {
            sortLaunchPoints(this.mLaunchPoints);
            notifyDataSetChanged();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static class AppViewHolder extends LauncherViewHolder {
        private final AppsAdapter mAdapter;
        private String mComponentName;
        private String mPackageName;

        AppViewHolder(View v, AppsAdapter adapter) {
            super(v);
            this.mAdapter = adapter;
        }

        public void init(LaunchPoint launchPoint) {
            this.itemView.setVisibility(0);
            if (launchPoint == null) {
                return;
            }
            this.mPackageName = launchPoint.getPackageName();
            this.mComponentName = launchPoint.getComponentName();
            setLaunchIntent(launchPoint.getLaunchIntent());
            setLaunchColor(launchPoint.getLaunchColor());
        }

        public void init(String packageName, String componentName, Intent launchIntent, int launchColor) {
            this.mPackageName = packageName;
            this.mComponentName = componentName;
            setLaunchIntent(launchIntent);
            setLaunchColor(launchColor);
        }

        protected final String getPackageName() {
            return this.mPackageName;
        }

        @Override // com.dismal.android.leanbacklauncher.LauncherViewHolder
        protected void onLaunchSucceeded() {
            this.mAdapter.mRanker.onAction(this.mPackageName, this.mComponentName, 1);
            if (this.mAdapter == null || this.mAdapter.mRanker.getSortingMode() != Ranker.SortingMode.RECENCY) {
                return;
            }
            this.mAdapter.mFlaggedForResort = true;
        }
    }

    public static class AppBannerViewHolder extends AppViewHolder {
        private final ImageView mBannerView;
        private final InstallStateOverlayHelper mOverlayHelper;

        @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter.AppViewHolder
        public /* bridge */ /* synthetic */ void init(String packageName, String componentName, Intent launchIntent, int launchColor) {
            super.init(packageName, componentName, launchIntent, launchColor);
        }

        public AppBannerViewHolder(View v, AppsAdapter adapter) {
            super(v, adapter);
            this.mOverlayHelper = new InstallStateOverlayHelper(v);
            if (v != null) {
                this.mBannerView = (ImageView) v.findViewById(R.id.app_banner);
            } else {
                this.mBannerView = null;
            }
        }

        @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter.AppViewHolder
        public void init(LaunchPoint launchPoint) {
            super.init(launchPoint);
            if (launchPoint == null || this.mBannerView == null) {
                return;
            }
            this.mBannerView.setContentDescription(launchPoint.getTitle());
            this.mBannerView.setImageDrawable(launchPoint.getBannerDrawable());
            if (launchPoint.isInstalling()) {
                this.mOverlayHelper.initOverlay(launchPoint);
            } else {
                this.mOverlayHelper.hideOverlay();
            }
        }

        public void init(String title, Drawable banner, int launchColor) {
            super.init(null, null, null, launchColor);
            if (this.mBannerView == null) {
                return;
            }
            this.mBannerView.setImageDrawable(banner);
            this.mBannerView.setContentDescription(title);
        }
    }

    private static final class AppFallbackViewHolder extends AppViewHolder {
        private final ImageView mIconView;
        private final TextView mLabelView;
        private final InstallStateOverlayHelper mOverlayHelper;

        public AppFallbackViewHolder(View v, AppsAdapter adapter) {
            super(v, adapter);
            this.mOverlayHelper = new InstallStateOverlayHelper(v);
            if (v != null) {
                this.mIconView = (ImageView) v.findViewById(R.id.banner_icon);
                this.mLabelView = (TextView) v.findViewById(R.id.banner_label);
            } else {
                this.mIconView = null;
                this.mLabelView = null;
            }
        }

        @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter.AppViewHolder
        public void init(LaunchPoint launchPoint) {
            super.init(launchPoint);
            if (launchPoint == null) {
                return;
            }
            Drawable icon = launchPoint.getIconDrawable();
            if (this.mIconView != null) {
                this.mIconView.setImageDrawable(icon);
            }
            if (this.mLabelView != null) {
                this.mLabelView.setText(launchPoint.getTitle());
            }
            if (launchPoint.isInstalling()) {
                this.mOverlayHelper.initOverlay(launchPoint);
            } else {
                this.mOverlayHelper.hideOverlay();
            }
        }
    }

    private static class InstallStateOverlayHelper {
        private final View mOverlayView;
        private final ProgressBar mProgressBar;
        private final TextView mProgressView;
        private final TextView mStateView;

        public InstallStateOverlayHelper(View v) {
            if (v != null) {
                this.mOverlayView = v.findViewById(R.id.install_state_overlay);
                this.mStateView = (TextView) v.findViewById(R.id.banner_install_state);
                this.mProgressView = (TextView) v.findViewById(R.id.banner_install_progress);
                this.mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
                return;
            }
            this.mOverlayView = null;
            this.mStateView = null;
            this.mProgressView = null;
            this.mProgressBar = null;
        }

        public void initOverlay(LaunchPoint launchPoint) {
            if (this.mStateView != null) {
                this.mStateView.setText(launchPoint.getInstallStateString(this.mStateView.getContext()));
            }
            if (this.mProgressView != null) {
                this.mProgressView.setText(launchPoint.getInstallProgressString(this.mProgressView.getContext()));
            }
            if (this.mProgressBar != null) {
                int progressPercent = launchPoint.getInstallProgressPercent();
                if (progressPercent == -1) {
                    this.mProgressBar.setIndeterminate(true);
                } else {
                    this.mProgressBar.setProgress(progressPercent);
                    this.mProgressBar.setIndeterminate(false);
                }
            }
            if (this.mOverlayView == null) {
                return;
            }
            this.mOverlayView.setVisibility(0);
        }

        public void hideOverlay() {
            if (this.mOverlayView == null) {
                return;
            }
            this.mOverlayView.setVisibility(8);
        }
    }

    private static final class SettingViewHolder extends AppViewHolder {
        private final ImageView mIconView;
        private final TextView mLabelView;
        private final View mMainView;

        public SettingViewHolder(View v, AppsAdapter adapter) {
            super(v, adapter);
            Log.d("AppsAdapter", "Created Settings View Holder v = " + v);
            if (v != null) {
                this.mMainView = v.findViewById(R.id.main);
                this.mIconView = (ImageView) v.findViewById(R.id.icon);
                this.mLabelView = (TextView) v.findViewById(R.id.label);
                Log.d("AppsAdapter", "   mMainView  = " + this.mMainView);
                Log.d("AppsAdapter", "   mIconView  = " + this.mIconView);
                Log.d("AppsAdapter", "   mLabelView = " + this.mLabelView);
                return;
            }
            this.mMainView = null;
            this.mIconView = null;
            this.mLabelView = null;
        }

        @Override // com.dismal.android.leanbacklauncher.apps.AppsAdapter.AppViewHolder
        public void init(LaunchPoint launchPoint) {
            super.init(launchPoint);
            if (launchPoint == null) {
                return;
            }
            if (this.mIconView != null) {
                this.mIconView.setImageDrawable(launchPoint.getIconDrawable());
            }
            if (this.mLabelView != null) {
                this.mLabelView.setText(launchPoint.getTitle());
            }
            this.mMainView.setContentDescription(launchPoint.getContentDescription());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayList<LaunchPoint> getRefreshedLaunchPointList() {
        ArrayList<LaunchPoint> launchPoints;
        if (this.mAppType == 0) {
            launchPoints = this.mLaunchPointGen.getNonGameLaunchPoints();
        } else if (this.mAppType == 1) {
            launchPoints = this.mLaunchPointGen.getGameLaunchPoints();
        } else if (this.mAppType == 2) {
            launchPoints = this.mLaunchPointGen.getSettingsLaunchPoints(true);
        } else {
            launchPoints = this.mLaunchPointGen.getAllLaunchPoints();
        }
        if (launchPoints == null) {
            launchPoints = new ArrayList<>();
        }
        sortLaunchPoints(launchPoints);
        
        if (this.mAppType == 0) {
            launchPoints.add(createAllAppsLaunchPoint());
        }
        
        return launchPoints;
    }

    private LaunchPoint createAllAppsLaunchPoint() {
        Intent intent = new Intent(this.mContext, com.marshmallow.launcher.Launcher.class);
        intent.putExtra("SHOW_ALL_APPS", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        Drawable icon = this.mContext.getDrawable(R.drawable.ic_allapps);
        int color = this.mContext.getResources().getColor(R.color.app_launch_ripple_default_color);

        return new LaunchPoint(this.mContext, "All Apps", icon, intent, color);
    }

    protected void onPostRefresh() {
    }

    protected void sortLaunchPoints(ArrayList<LaunchPoint> launchPoints) {
        if (this.mAppType == 2) {
            return;
        }
        this.mRanker.rankLaunchPoints(launchPoints, this);
    }

    @Override // com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onLaunchPointsAddedOrUpdated(final ArrayList<LaunchPoint> launchPoints) {
        this.mNotifyHandler.post(new Runnable() { // from class: com.dismal.android.leanbacklauncher.apps.AppsAdapter.1
            @Override // java.lang.Runnable
            public void run() {
                boolean isGame = AppsAdapter.this.mAppType == 1;
                for (int i = launchPoints.size() - 1; i >= 0; i--) {
                    boolean gameMatch = ((LaunchPoint) launchPoints.get(i)).isGame() == isGame;
                    boolean found = false;
                    int j = AppsAdapter.this.mLaunchPoints.size() - 1;
                    while (true) {
                        if (j < 0) {
                            break;
                        }
                        if (AppsAdapter.this.mLaunchPoints.get(j).equals(launchPoints.get(i))) {
                            if (gameMatch) {
                                AppsAdapter.this.mLaunchPoints.set(j, (LaunchPoint) launchPoints.get(i));
                                AppsAdapter.this.notifyItemChanged(j);
                            } else {
                                AppsAdapter.this.mLaunchPoints.remove(j);
                                AppsAdapter.this.notifyItemRemoved(j);
                            }
                            found = true;
                        } else {
                            j--;
                        }
                    }
                    if (!found && gameMatch) {
                        int pos = AppsAdapter.this.mRanker.insertLaunchPoint(AppsAdapter.this.mLaunchPoints, (LaunchPoint) launchPoints.get(i));
                        AppsAdapter.this.notifyItemInserted(pos);
                    }
                }
            }
        });
    }

    @Override // com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onLaunchPointsRemoved(final ArrayList<LaunchPoint> launchPoints) {
        this.mNotifyHandler.post(new Runnable() { // from class: com.dismal.android.leanbacklauncher.apps.AppsAdapter.2
            @Override // java.lang.Runnable
            public void run() {
                for (int j = AppsAdapter.this.mLaunchPoints.size() - 1; j >= 0; j--) {
                    int i = launchPoints.size() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        if (AppsAdapter.this.mLaunchPoints.get(j).equals(launchPoints.get(i))) {
                            ((LaunchPoint) launchPoints.get(i)).cancelPendingOperations(AppsAdapter.this.mContext);
                            AppsAdapter.this.mLaunchPoints.remove(j);
                            AppsAdapter.this.notifyItemRemoved(j);
                            break;
                        }
                        i--;
                    }
                }
            }
        });
    }

    @Override // com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onLaunchPointListGeneratorReady() {
        if (!this.mRanker.isReady()) {
            return;
        }
        refreshDataSetAsync();
    }

    @Override // com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator.Listener
    public void onSettingsChanged() {
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Ranker.RankingListener
    public void onRankerReady() {
        if (!this.mLaunchPointGen.isReady()) {
            return;
        }
        refreshDataSetAsync();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewRecycled(AppViewHolder holder) {
        holder.itemView.setVisibility(0);
    }

    public void refreshDataSetAsync() {
        new RefreshTask(this, null).execute(new Void[0]);
    }

    private class RefreshTask extends AsyncTask<Void, Void, ArrayList<LaunchPoint>> {

        /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues, reason: not valid java name */
        private /* synthetic */ int[] f4com_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues;

        /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues, reason: not valid java name */
        private /* synthetic */ int[] m79getcom_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues() {
            if (f4com_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues != null) {
                return f4com_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues;
            }
            int[] iArr = new int[Lists.Change.Type.valuesCustom().length];
            try {
                iArr[Lists.Change.Type.INSERTION.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Lists.Change.Type.REMOVAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            f4com_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues = iArr;
            return iArr;
        }

        /* synthetic */ RefreshTask(AppsAdapter this$0, RefreshTask refreshTask) {
            this();
        }

        private RefreshTask() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public ArrayList<LaunchPoint> doInBackground(Void... params) {
            return AppsAdapter.this.getRefreshedLaunchPointList();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(ArrayList<LaunchPoint> launchPoints) {
            List<Lists.Change> changes = Lists.getChanges(AppsAdapter.this.mLaunchPoints, launchPoints, AppsAdapter.this.mRanker.getLaunchPointComparator());
            AppsAdapter.this.mLaunchPoints = launchPoints;
            AppsAdapter.this.onPostRefresh();
            for (Lists.Change change : changes) {
                switch (m79getcom_google_android_leanbacklauncher_util_Lists$Change$TypeSwitchesValues()[change.type.ordinal()]) {
                    case 1:
                        AppsAdapter.this.notifyItemRangeInserted(change.index, change.count);
                        break;
                    case 2:
                        AppsAdapter.this.notifyItemRangeRemoved(change.index, change.count);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported change type: " + change.type);
                }
            }
        }
    }
}
