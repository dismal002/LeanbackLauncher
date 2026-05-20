package com.dismal.android.leanbacklauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import androidx.leanback.widget.OnChildSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.HomeScreenRow;
import com.dismal.android.leanbacklauncher.HomeScrollManager;
import com.dismal.android.leanbacklauncher.apps.AppsAdapter;
import com.dismal.android.leanbacklauncher.apps.AppsUpdateListener;
import com.dismal.android.leanbacklauncher.apps.ConnectivityListener;
import com.dismal.android.leanbacklauncher.apps.LaunchPointListGenerator;
import com.dismal.android.leanbacklauncher.apps.SettingsAdapter;
import com.dismal.android.leanbacklauncher.inputs.InputsAdapter;
import com.dismal.android.leanbacklauncher.notifications.NotificationRowView;
import com.dismal.android.leanbacklauncher.notifications.NotificationViewFlipper;
import com.dismal.android.leanbacklauncher.notifications.NotificationsAdapter;
import com.dismal.android.leanbacklauncher.notifications.NotificationsServiceAdapter;
import com.dismal.android.leanbacklauncher.notifications.PartnerAdapter;
import com.dismal.android.leanbacklauncher.ranker.Ranker;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class HomeScreenAdapter extends RecyclerView.Adapter<HomeScreenAdapter.HomeViewHolder> implements OnChildSelectedListener, HomeScreenRow.RowChangeListener, ConnectivityListener.Listener {
    private View mActiveItem;
    private final AppsUpdateListener mAppRefresher;
    private ConnectivityListener mConnectivityListener;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private InputsAdapter mInputsAdapter;
    private final boolean mIsSearchTypeBar;
    private final LaunchPointListGenerator mLaunchPointListGenerator;
    private NotificationViewFlipper mNotifViewFlipper;
    private final Partner mPartner;
    private final PartnerAdapter mPartnerAdapter;
    private final Ranker mRanker;
    private BroadcastReceiver mReceiver;
    private final NotificationsAdapter mRecommendationsAdapter;
    private final HomeScrollManager mScrollManager;
    private SearchView mSearch;
    private final SettingsAdapter mSettingsAdapter;
    private final SparseArray<View> mHeaders = new SparseArray<>(7);
    private ArrayList<HomeScreenRow> mAllRowsList = new ArrayList<>(7);
    private ArrayList<HomeScreenRow> mVisRowsList = new ArrayList<>(7);

    static final class HomeViewHolder extends RecyclerView.ViewHolder {
        HomeViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static final class ListComparator implements Comparator<HomeScreenRow> {
        /* synthetic */ ListComparator(ListComparator listComparator) {
            this();
        }

        private ListComparator() {
        }

        @Override // java.util.Comparator
        public int compare(HomeScreenRow lhs, HomeScreenRow rhs) {
            return lhs.getPosition() - rhs.getPosition();
        }
    }

    public HomeScreenAdapter(Context context, HomeScrollManager scrollMgr, LaunchPointListGenerator launchPointListGenerator, Ranker ranker) {
        long startTime = System.currentTimeMillis();
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mScrollManager = (HomeScrollManager) Preconditions.checkNotNull(scrollMgr);
        this.mLaunchPointListGenerator = (LaunchPointListGenerator) Preconditions.checkNotNull(launchPointListGenerator);
        this.mRanker = (Ranker) Preconditions.checkNotNull(ranker);
        
        long t1 = System.currentTimeMillis();
        this.mPartner = Partner.get(this.mContext);
        Log.d("LeanbackLauncher", "HomeScreenAdapter: Partner.get took " + (System.currentTimeMillis() - t1) + "ms");
        
        this.mInflater = LayoutInflater.from(context);
        this.mAppRefresher = new AppsUpdateListener(this.mContext, this.mLaunchPointListGenerator, this.mRanker);
        this.mRecommendationsAdapter = new NotificationsAdapter(this.mContext, this.mRanker);
        this.mIsSearchTypeBar = context.getResources().getBoolean(R.bool.is_search_type_bar);
        this.mConnectivityListener = new ConnectivityListener(context, this);
        this.mSettingsAdapter = new SettingsAdapter(this.mContext, this.mLaunchPointListGenerator, this.mRanker, this.mConnectivityListener);
        this.mRecommendationsAdapter.setNotificationCountListener(this.mSettingsAdapter);
        if (this.mPartner.isRowEnabled("partner_row")) {
            this.mPartnerAdapter = new PartnerAdapter(this.mContext, this.mAppRefresher);
        } else {
            this.mPartnerAdapter = null;
        }
        setHasStableIds(true);
        
        t1 = System.currentTimeMillis();
        buildRowList();
        Log.d("LeanbackLauncher", "HomeScreenAdapter: buildRowList took " + (System.currentTimeMillis() - t1) + "ms");
        
        scrollMgr.setFullScrollThreshold(this.mContext.getResources().getDimensionPixelOffset(R.dimen.home_scroll_size_search));
        
        t1 = System.currentTimeMillis();
        this.mConnectivityListener.start();
        Log.d("LeanbackLauncher", "HomeScreenAdapter: ConnectivityListener start took " + (System.currentTimeMillis() - t1) + "ms");
        
        Log.d("LeanbackLauncher", "HomeScreenAdapter constructor finished in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void unregisterReceivers() {
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        if (this.mConnectivityListener != null) {
            this.mConnectivityListener.stop();
        }
        if (this.mAppRefresher == null) {
            return;
        }
        this.mAppRefresher.unregisterReceivers();
    }

    public void resetRowPositions(boolean smooth) {
        for (int i = 0; i < this.mAllRowsList.size(); i++) {
            if (this.mAllRowsList.get(i).getRowView() instanceof ActiveFrame) {
                ((ActiveFrame) this.mAllRowsList.get(i).getRowView()).resetScrollPosition(smooth);
            }
        }
    }

    public int getRowIndex(int rowType) {
        int index = -1;
        int size = this.mVisRowsList.size();
        for (int i = 0; i < size; i++) {
            if (this.mVisRowsList.get(i).getType() == rowType) {
                index = i;
            }
        }
        return index;
    }

    @Override // com.dismal.android.leanbacklauncher.apps.ConnectivityListener.Listener
    public void onConnectivityChange() {
        this.mSettingsAdapter.onConnectivityChange();
        if (this.mNotifViewFlipper == null) {
            return;
        }
        this.mNotifViewFlipper.refreshTimeoutScreen();
    }

    private void buildRowList() {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        Resources res = this.mContext.getResources();
        int failures = 0;
        boolean hasPartnerRow = this.mPartner.isRowEnabled("partner_row");
        int rowCount = hasPartnerRow ? 7 : 6;
        boolean hasInputsRow = this.mPartner.isRowEnabled("inputs_row");
        if (hasInputsRow) {
            List<ResolveInfo> acts = this.mContext.getPackageManager().queryIntentActivities(new Intent("android.intent.action.VIEW", TvContract.buildChannelUri(0L)), 513);
            LaunchPointListGenerator gen = this.mLaunchPointListGenerator;
            for (ResolveInfo info : acts) {
                gen.addToBlacklist(info.activityInfo.packageName, false);
            }
        } else {
            rowCount--;
        }
        String partnerFont = this.mPartner.getPartnerFontName();
        buildRow(0, 0, null, null, null, R.dimen.home_scroll_size_search, false);
        int i6 = 1 + 1;
        buildRow(1, 1, null, null, null, R.dimen.home_scroll_size_notifications, false);
        int position = this.mPartner.getRowPosition("settings_row");
        if (position == -1) {
            failures = 1;
            i = (rowCount - 1) + 0;
        } else {
            i = position + 2;
        }
        buildRow(5, i, this.mPartner.getRowTitle("settings_row", res.getString(R.string.category_label_settings)), this.mPartner.getRowIcon("settings_row"), partnerFont, R.dimen.home_scroll_size_settings, false);
        if (hasInputsRow) {
            int position2 = this.mPartner.getRowPosition("inputs_row");
            if (position2 == -1) {
                i5 = (rowCount - 1) - failures;
                failures++;
            } else {
                i5 = position2 + 2;
            }
            buildRow(6, i5, this.mPartner.getRowTitle("inputs_row", res.getString(R.string.category_label_inputs)), this.mPartner.getRowIcon("inputs_row"), partnerFont, R.dimen.home_scroll_size_inputs, true);
        }
        int position3 = this.mPartner.getRowPosition("games_row");
        if (position3 == -1) {
            i2 = (rowCount - 1) - failures;
            failures++;
        } else {
            i2 = position3 + 2;
        }
        buildRow(4, i2, this.mPartner.getRowTitle("games_row", res.getString(R.string.category_label_games)), this.mPartner.getRowIcon("games_row"), partnerFont, R.dimen.home_scroll_size_games, true);
        int position4 = this.mPartner.getRowPosition("apps_row");
        if (position4 == -1) {
            i3 = (rowCount - 1) - failures;
            failures++;
        } else {
            i3 = position4 + 2;
        }
        buildRow(3, i3, this.mPartner.getRowTitle("apps_row", res.getString(R.string.category_label_apps)), this.mPartner.getRowIcon("apps_row"), partnerFont, R.dimen.home_scroll_size_apps, true);
        if (hasPartnerRow) {
            int position5 = this.mPartner.getRowPosition("partner_row");
            if (position5 == -1) {
                int i7 = failures + 1;
                i4 = (rowCount - 1) - failures;
            } else {
                i4 = position5 + 2;
            }
            buildRow(2, i4, this.mPartner.getRowTitle("partner_row", null), this.mPartner.getRowIcon("partner_row"), partnerFont, R.dimen.home_scroll_size_partner, true);
        }
        ListComparator comp = new ListComparator(null);
        Collections.sort(this.mAllRowsList, comp);
        Collections.sort(this.mVisRowsList, comp);
    }

    private void buildRow(int type, int position, String title, Drawable icon, String font, int scrollOffsetResId, boolean hideIfEmpty) {
        HomeScreenRow row = new HomeScreenRow(type, position, hideIfEmpty);
        row.setHeaderInfo(title != null, title, icon, font);
        row.setAdapter(initAdapter(type));
        row.setViewScrollOffset(this.mContext.getResources().getDimensionPixelOffset(scrollOffsetResId));
        addRowEntry(row);
    }

    private void addRowEntry(HomeScreenRow row) {
        this.mAllRowsList.add(row);
        row.setChangeListener(this);
        if (row.getType() == 3 || row.getType() == 4 || row.getType() == 5) {
            this.mAppRefresher.addAppRow(row);
        }
        if (!row.isVisible()) {
            return;
        }
        this.mVisRowsList.add(row);
    }

    private HomeScreenRow getRowByPosition(int position) {
        for (HomeScreenRow row : this.mAllRowsList) {
            if (row.getPosition() == position) {
                return row;
            }
        }
        return null;
    }

    @Override // com.dismal.android.leanbacklauncher.HomeScreenRow.RowChangeListener
    public void onRowVisibilityChanged(int position, boolean visible) {
        if (visible) {
            int insertPoint = this.mVisRowsList.size();
            int i = 0;
            while (true) {
                if (i >= this.mVisRowsList.size()) {
                    break;
                }
                if (this.mVisRowsList.get(i).getPosition() == position) {
                    return;
                }
                if (this.mVisRowsList.get(i).getPosition() <= position) {
                    i++;
                } else {
                    insertPoint = i;
                    break;
                }
            }
            HomeScreenRow rowToInsert = getRowByPosition(position);
            if (rowToInsert != null) {
                this.mVisRowsList.add(insertPoint, rowToInsert);
                notifyItemInserted(insertPoint);
            }
            return;
        }
        int pos = -1;
        int i2 = 0;
        while (true) {
            if (i2 >= this.mVisRowsList.size()) {
                break;
            }
            if (this.mVisRowsList.get(i2).getPosition() != position) {
                i2++;
            } else {
                pos = i2;
                break;
            }
        }
        if (pos < 0) {
            return;
        }
        this.mVisRowsList.remove(pos);
        notifyItemRemoved(pos);
    }

    public void refreshAdapterData() {
        if (this.mAppRefresher != null) {
            this.mAppRefresher.refreshRows();
        }
        if (this.mInputsAdapter != null) {
            this.mInputsAdapter.refreshInputsData();
        }
        if (this.mNotifViewFlipper == null) {
            return;
        }
        this.mNotifViewFlipper.refreshTimeoutScreen();
    }

    public void animateSearchIn() {
        if (this.mSearch == null) {
            return;
        }
        this.mSearch.animateIn();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public long getItemId(int position) {
        return this.mVisRowsList.get(position).getType();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        if (position >= this.mVisRowsList.size()) {
            return -1;
        }
        return this.mVisRowsList.get(position).getPosition();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public HomeViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view;
        HomeScreenRow row = getRowByPosition(position);
        if (row == null) {
            return null;
        }
        switch (row.getType()) {
            case 0:
                if (this.mIsSearchTypeBar) {
                    view = this.mInflater.inflate(R.layout.home_search_bar, parent, false);
                } else {
                    view = this.mInflater.inflate(R.layout.home_search_orb, parent, false);
                }
                this.mHeaders.put(row.getType(), view);
                this.mSearch = (SearchView) view;
                break;
            case 1:
                view = this.mInflater.inflate(R.layout.home_notification_row, parent, false);
                NotificationRowView notifList = (NotificationRowView) view.findViewById(R.id.list);
                NotificationViewFlipper flipper = (NotificationViewFlipper) view.findViewById(R.id.notif_view_flipper);
                if (notifList != null && flipper != null) {
                    initNotificationsRows(notifList, row.getAdapter(), flipper);
                }
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                view = this.mInflater.inflate(R.layout.home_apps_row, parent, false);
                this.mHeaders.put(row.getType(), view.findViewById(R.id.header));
                if (view instanceof ActiveFrame) {
                    initAppRow((ActiveFrame) view, row);
                }
                break;
            default:
                return null;
        }
        row.setRowView(view);
        view.setTag(Integer.valueOf(row.getType()));
        return new HomeViewHolder(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(HomeViewHolder holder, int position) {
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewRecycled(HomeViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public boolean onFailedToRecycleView(HomeViewHolder holder) {
        if (holder.itemView instanceof ActiveFrame) {
            resetRowAdapter((ActiveFrame) holder.itemView);
        }
        return super.onFailedToRecycleView(holder);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mVisRowsList.size();
    }

    View[] getRowHeaders() {
        int n = this.mHeaders.size();
        View[] headers = new View[n];
        for (int i = 0; i < n; i++) {
            headers[i] = this.mHeaders.valueAt(i);
        }
        return headers;
    }

    public NotificationsAdapter getRecommendationsAdapter() {
        return this.mRecommendationsAdapter;
    }

    private void initNotificationsRows(NotificationRowView list, final RecyclerView.Adapter<?> adapter, NotificationViewFlipper flipper) {
        list.setHasFixedSize(true);
        list.setAdapter(adapter);
        this.mNotifViewFlipper = flipper;
        if (adapter.getItemCount() > 0) {
            flipper.setRowVisibility(true);
        } else {
            flipper.setRowVisibility(false);
        }
        int cardSpacing = this.mContext.getResources().getDimensionPixelOffset(R.dimen.inter_card_spacing);
        list.setItemMargin(cardSpacing);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        if (this.mReceiver != null) {
            return;
        }
        this.mReceiver = new BroadcastReceiver() { // from class: com.dismal.android.leanbacklauncher.HomeScreenAdapter.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.intent.action.USER_PRESENT") || !(adapter instanceof NotificationsServiceAdapter)) {
                    return;
                }
                ((NotificationsServiceAdapter) adapter).reregisterListener();
                HomeScreenAdapter.this.mContext.unregisterReceiver(this);
                HomeScreenAdapter.this.mReceiver = null;
            }
        };
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    private void initAppRow(ActiveFrame group, HomeScreenRow row) {
        Typeface font;
        if (group == null) {
            return;
        }
        Resources res = this.mContext.getResources();
        group.setTag(Integer.valueOf(R.integer.tag_has_header));
        ActiveItemsRowView list = (ActiveItemsRowView) group.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setAdapter(row.getAdapter());
        if (row.hasHeader()) {
            list.setContentDescription(row.getTitle());
            ((TextView) group.findViewById(R.id.title)).setText(row.getTitle());
            if (!TextUtils.isEmpty(row.getFontName()) && (font = Typeface.create(row.getFontName(), 0)) != null) {
                ((TextView) group.findViewById(R.id.title)).setTypeface(font);
            }
            Drawable icon = row.getIcon();
            ImageView iconView = (ImageView) group.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(0);
            } else {
                iconView.setVisibility(8);
            }
        }
        ViewGroup.LayoutParams lp = list.getLayoutParams();
        int cardSpacing = res.getDimensionPixelOffset(R.dimen.inter_card_spacing);
        group.setScaledWhenUnfocused(true);
        switch (row.getType()) {
            case 2:
            case 3:
            case 4:
            case 6:
                int rowHeight = (int) res.getDimension(R.dimen.banner_height);
                list.setIsNumRowsAdjustable(true);
                list.adjustNumRows(res.getInteger(R.integer.max_num_banner_rows), cardSpacing, rowHeight);
                break;
            case 5:
                lp.height = (int) res.getDimension(R.dimen.settings_row_height);
                break;
        }
        list.setItemMargin(cardSpacing);
    }

    private void resetRowAdapter(ActiveFrame group) {
        ActiveItemsRowView list = (ActiveItemsRowView) group.findViewById(R.id.list);
        list.setAdapter(null);
    }

    private RecyclerView.Adapter<?> initAdapter(int type) {
        switch (type) {
            case 1:
                RecyclerView.Adapter<?> adapter = this.mRecommendationsAdapter;
                return adapter;
            case 2:
                RecyclerView.Adapter<?> adapter2 = this.mPartnerAdapter;
                return adapter2;
            case 3:
                RecyclerView.Adapter<?> adapter3 = new AppsAdapter(this.mContext, 0, this.mLaunchPointListGenerator, this.mRanker);
                return adapter3;
            case 4:
                RecyclerView.Adapter<?> adapter4 = new AppsAdapter(this.mContext, 1, this.mLaunchPointListGenerator, this.mRanker);
                return adapter4;
            case 5:
                RecyclerView.Adapter<?> adapter5 = this.mSettingsAdapter;
                return adapter5;
            case 6:
                InputsAdapter.Configuration config = new InputsAdapter.Configuration(this.mPartner.showPhysicalTunersSeparately(), this.mPartner.disableDiconnectedInputs());
                RecyclerView.Adapter<?> adapter6 = new InputsAdapter(this.mContext, config);
                this.mInputsAdapter = (InputsAdapter) adapter6;
                return adapter6;
            default:
                return null;
        }
    }

    @Override // androidx.leanback.widget.OnChildSelectedListener
    public void onChildSelected(ViewGroup parent, View child, int position, long id) {
        if (child == this.mActiveItem) {
            return;
        }
        if (child == null) {
            this.mActiveItem.setActivated(false);
            this.mActiveItem = null;
            return;
        }
        if (this.mActiveItem != null) {
            this.mActiveItem.setActivated(false);
        }
        this.mActiveItem = child;
        if (child == null) {
            return;
        }
        this.mActiveItem.setActivated(true);
    }

    public int getScrollOffset(int index) {
        if (index >= 0 || index < this.mVisRowsList.size()) {
            return this.mVisRowsList.get(index).getRowScrollOffset();
        }
        return 0;
    }

    public void onInitUi() {
        this.mRecommendationsAdapter.onInitUi();
        if (this.mPartnerAdapter == null) {
            return;
        }
        this.mPartnerAdapter.onInitUi();
    }

    public void onUiVisible() {
        this.mRecommendationsAdapter.onUiVisible();
        if (this.mPartnerAdapter == null) {
            return;
        }
        this.mPartnerAdapter.onUiVisible();
    }

    public void onUiInvisible() {
        this.mRecommendationsAdapter.onUiInvisible();
        if (this.mPartnerAdapter == null) {
            return;
        }
        this.mPartnerAdapter.onUiInvisible();
    }

    public void onStopUi() {
        this.mRecommendationsAdapter.onStopUi();
        if (this.mPartnerAdapter == null) {
            return;
        }
        this.mPartnerAdapter.onStopUi();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewDetachedFromWindow(HomeViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
        if (!(holder.itemView instanceof HomeScrollManager.HomeScrollFractionListener)) {
            return;
        }
        this.mScrollManager.removeHomeScrollListener((HomeScrollManager.HomeScrollFractionListener) holder.itemView);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onViewAttachedToWindow(HomeViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (!(holder.itemView instanceof HomeScrollManager.HomeScrollFractionListener)) {
            return;
        }
        this.mScrollManager.addHomeScrollListener((HomeScrollManager.HomeScrollFractionListener) holder.itemView);
    }
}
