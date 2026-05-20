package com.dismal.android.leanbacklauncher.inputs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Message;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.dismal.android.leanbacklauncher.LauncherViewHolder;
import com.dismal.android.leanbacklauncher.R;
import com.dismal.android.leanbacklauncher.apps.BannerView;
import com.dismal.android.leanbacklauncher.util.Partner;
import com.dismal.android.leanbacklauncher.widget.RowViewAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class InputsAdapter extends RowViewAdapter<InputsAdapter.InputViewHolder> {
    private final InputsComparator mComp;
    private final Configuration mConfig;
    private final Handler mHandler;
    private final float mImageAlphaConnected;
    private final float mImageAlphaDisconnected;
    private final LayoutInflater mInflater;
    private final HashMap<String, TvInputEntry> mInputs;
    private final InputCallback mInputsCallback;
    private boolean mIsBundledTunerVisible;
    private final LinkedHashMap<String, TvInputInfo> mPhysicalTunerInputs;
    private final float mTextAlphaConnected;
    private final float mTextAlphaDisconnected;
    private final int mTextColorDark;
    private final int mTextColorLight;
    private final TvInputManager mTvManager;
    private Map<Integer, Integer> mTypePriorities;
    private final HashMap<String, TvInputInfo> mVirtualTunerInputs;
    private final List<TvInputEntry> mVisibleInputs;

    public static final class Configuration {
        final boolean mDisableDiconnectedInputs;
        final boolean mShowPhysicalTunersSeparately;

        public Configuration(boolean showPhysicalTunersSeparately, boolean disableDiconnectedInputs) {
            this.mShowPhysicalTunersSeparately = showPhysicalTunersSeparately;
            this.mDisableDiconnectedInputs = disableDiconnectedInputs;
        }
    }

    private class TvInputEntry {
        public Drawable mBanner;
        public final HdmiDeviceInfo mHdmiInfo;
        public final TvInputInfo mInfo;
        public final String mLabel;
        public int mNumChildren;
        public final TvInputEntry mParentEntry;
        public final String mParentLabel;
        public final int mPriority;
        public final int mSortKey;
        public int mState;
        public final int mTextColorOption;
        public final int mType;

        public TvInputEntry(String label, Drawable banner, int colorOption, int type) {
            this.mInfo = null;
            this.mHdmiInfo = null;
            this.mLabel = label;
            this.mParentLabel = null;
            this.mBanner = banner;
            this.mTextColorOption = colorOption;
            this.mType = type;
            this.mPriority = InputsAdapter.this.getPriorityForType(type);
            this.mSortKey = Integer.MAX_VALUE;
            this.mParentEntry = null;
            this.mState = 0;
        }

        public TvInputEntry(TvInputInfo info, TvInputEntry parent, int state, Context ctx) {
            this.mInfo = info;
            this.mType = this.mInfo.getType();
            if (this.mType == 1007) {
                this.mHdmiInfo = getHdmiDeviceInfoReflection(this.mInfo);
            } else {
                this.mHdmiInfo = null;
            }
            CharSequence label = info.loadCustomLabel(ctx);
            label = TextUtils.isEmpty(label) ? info.loadLabel(ctx) : label;
            if (label != null) {
                this.mLabel = label.toString();
            } else {
                this.mLabel = "";
            }
            this.mTextColorOption = this.mInfo.getServiceInfo().metaData.getInt("input_banner_label_color_option", 0);
            this.mSortKey = this.mInfo.getServiceInfo().metaData.getInt("input_sort_key", Integer.MAX_VALUE);
            this.mState = state;
                        HdmiDeviceInfo hdmiInfoTemp = getHdmiDeviceInfoReflection(info);
            if (hdmiInfoTemp != null && hdmiInfoTemp.isCecDevice()) {
                this.mPriority = InputsAdapter.this.getPriorityForType(-2);
            } else {
                this.mPriority = InputsAdapter.this.getPriorityForType(this.mType);
            }
            this.mParentEntry = parent;
            if (this.mParentEntry != null) {
                CharSequence label2 = this.mParentEntry.mInfo.loadCustomLabel(ctx);
                label2 = TextUtils.isEmpty(label2) ? this.mParentEntry.mInfo.loadLabel(ctx) : label2;
                if (label2 != null) {
                    this.mParentLabel = label2.toString();
                } else {
                    this.mParentLabel = "";
                }
            } else {
                this.mParentLabel = this.mLabel;
            }
            this.mBanner = getImageDrawable();
        }

        public boolean isBundledTuner() {
            return this.mType == -3;
        }

        public boolean isEnabled() {
            return (!isBundledTuner() && InputsAdapter.this.mConfig.mDisableDiconnectedInputs && this.mState == 2) ? false : true;
        }

        public Drawable getImageDrawable() {
            int drawableId;
            if (this.mBanner != null) {
                return this.mBanner;
            }
            if (this.mInfo != null) {
                this.mBanner = this.mInfo.loadIcon(InputsAdapter.this.mContext);
                if (this.mBanner != null) {
                    return this.mBanner;
                }
            }
            switch (this.mType) {
                case -3:
                case 0:
                    drawableId = R.drawable.ic_input_tuner;
                    break;
                case 1001:
                    drawableId = R.drawable.ic_input_composite;
                    break;
                case 1002:
                    drawableId = R.drawable.ic_input_svideo;
                    break;
                case 1003:
                    drawableId = R.drawable.ic_input_scart;
                    break;
                case 1004:
                    drawableId = R.drawable.ic_input_component;
                    break;
                case 1005:
                    drawableId = R.drawable.ic_input_vga;
                    break;
                case 1006:
                    drawableId = R.drawable.ic_input_dvi;
                    break;
                case 1007:
                    if (this.mHdmiInfo == null) {
                        drawableId = R.drawable.ic_input_hdmi;
                        break;
                    } else {
                        switch (this.mHdmiInfo.getDeviceType()) {
                            case 0:
                                drawableId = R.drawable.ic_input_livetv;
                                break;
                            case 1:
                                drawableId = R.drawable.ic_input_recording;
                                break;
                            case 2:
                            default:
                                drawableId = R.drawable.ic_input_tuner;
                                break;
                            case 3:
                                drawableId = R.drawable.ic_input_tuner;
                                break;
                            case 4:
                                drawableId = R.drawable.ic_input_playback;
                                break;
                            case 5:
                                drawableId = R.drawable.ic_input_audio;
                                break;
                        }
                    }
                    break;
                case 1008:
                    drawableId = R.drawable.ic_input_display_port;
                    break;
                default:
                    drawableId = R.drawable.ic_input_hdmi;
                    break;
            }
            Drawable drawable = ContextCompat.getDrawable(InputsAdapter.this.mContext, drawableId);
            this.mBanner = drawable;
            return drawable;
        }

        public String getLabel() {
            if (this.mHdmiInfo != null && !TextUtils.isEmpty(this.mHdmiInfo.getDisplayName())) {
                return this.mHdmiInfo.getDisplayName();
            }
            if (!TextUtils.isEmpty(this.mLabel)) {
                return this.mLabel;
            }
            if (this.mType == -3) {
                return InputsAdapter.this.mContext.getResources().getString(R.string.input_title_bundled_tuner);
            }
            return null;
        }

        public boolean isLabelDarkColor() {
            return this.mTextColorOption == 1;
        }

        public Intent getLaunchIntent() {
            if (this.mInfo != null) {
                if (this.mInfo.isPassthroughInput()) {
                    return new Intent("android.intent.action.VIEW", TvContract.buildChannelUriForPassthroughInput(this.mInfo.getId()));
                }
                return new Intent("android.intent.action.VIEW", TvContract.buildChannelsUriForInput(this.mInfo.getId()));
            }
            if (isBundledTuner()) {
                return new Intent("android.intent.action.VIEW", TvContract.buildChannelUri(0L));
            }
            return null;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof TvInputEntry)) {
                return false;
            }
            TvInputEntry obj = (TvInputEntry) o;
            if (isBundledTuner() && obj.isBundledTuner()) {
                return true;
            }
            if (this.mInfo == null || obj.mInfo == null) {
                return false;
            }
            return this.mInfo.equals(obj.mInfo);
        }
    }

    private class InputsComparator implements Comparator<TvInputEntry> {
        /* synthetic */ InputsComparator(InputsAdapter this$0, InputsComparator inputsComparator) {
            this();
        }

        private InputsComparator() {
        }

        @Override // java.util.Comparator
        public int compare(TvInputEntry lhs, TvInputEntry rhs) {
            if (rhs == null) {
                return lhs == null ? 0 : -1;
            }
            if (lhs == null) {
                return rhs == null ? 0 : 1;
            }
            if (InputsAdapter.this.mConfig.mDisableDiconnectedInputs) {
                boolean disconnectedL = lhs.mState == 2;
                boolean disconnectedR = rhs.mState == 2;
                if (disconnectedL != disconnectedR) {
                    return disconnectedL ? 1 : -1;
                }
            }
            if (lhs.mPriority != rhs.mPriority) {
                return lhs.mPriority - rhs.mPriority;
            }
            if (lhs.mType == 0 && rhs.mType == 0) {
                boolean rIsPhysical = InputsAdapter.isPhysicalTuner(InputsAdapter.this.mContext.getPackageManager(), rhs.mInfo);
                boolean lIsPhysical = InputsAdapter.isPhysicalTuner(InputsAdapter.this.mContext.getPackageManager(), lhs.mInfo);
                if (rIsPhysical != lIsPhysical) {
                    return lIsPhysical ? -1 : 1;
                }
            }
            if (lhs.mSortKey != rhs.mSortKey) {
                return rhs.mSortKey - lhs.mSortKey;
            }
            if (!TextUtils.equals(lhs.mParentLabel, rhs.mParentLabel)) {
                return lhs.mParentLabel.compareToIgnoreCase(rhs.mParentLabel);
            }
            return lhs.mLabel.compareToIgnoreCase(rhs.mLabel);
        }
    }

    public InputsAdapter(Context context, Configuration config) {
        super(context);
        this.mIsBundledTunerVisible = false;
        this.mHandler = new Handler() { // from class: com.dismal.android.leanbacklauncher.inputs.InputsAdapter.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        String id = (String) msg.obj;
                        int state = msg.arg1;
                        InputsAdapter.this.inputStateUpdated(id, state);
                        break;
                    case 2:
                        String id2 = (String) msg.obj;
                        InputsAdapter.this.inputAdded(id2, false);
                        break;
                    case 3:
                        String id3 = (String) msg.obj;
                        InputsAdapter.this.inputRemoved(id3);
                        break;
                }
            }
        };
        this.mComp = new InputsComparator(this, null);
        this.mInputs = new HashMap<>();
        this.mVisibleInputs = new ArrayList();
        this.mPhysicalTunerInputs = new LinkedHashMap<>();
        this.mVirtualTunerInputs = new HashMap<>();
        this.mConfig = config;
        this.mTvManager = (TvInputManager) context.getSystemService("tv_input");
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        setupDeviceTypePriorities();
        TypedValue out = new TypedValue();
        context.getResources().getValue(R.dimen.input_banner_connected_text_alpha, out, true);
        this.mTextAlphaConnected = out.getFloat();
        context.getResources().getValue(R.dimen.input_banner_disconnected_text_alpha, out, true);
        this.mTextAlphaDisconnected = out.getFloat();
        context.getResources().getValue(R.dimen.input_banner_connected_image_alpha, out, true);
        this.mImageAlphaConnected = out.getFloat();
        context.getResources().getValue(R.dimen.input_banner_disconnected_image_alpha, out, true);
        this.mImageAlphaDisconnected = out.getFloat();
        this.mTextColorLight = this.mContext.getResources().getColor(R.color.input_banner_label_text_color_light);
        this.mTextColorDark = this.mContext.getResources().getColor(R.color.input_banner_label_text_color_dark);
        refreshInputs();
        this.mInputsCallback = new InputCallback();
        if (this.mTvManager == null) {
            return;
        }
        this.mTvManager.registerCallback(this.mInputsCallback, this.mHandler);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public InputViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new InputViewHolder(this.mInflater.inflate(R.layout.input_banner, parent, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(InputViewHolder holder, int position) {
        if (position >= this.mVisibleInputs.size()) {
            return;
        }
        TvInputEntry input = this.mVisibleInputs.get(position);
        holder.init(input);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        int count = this.mVisibleInputs.size();
        return count;
    }

    public void refreshInputsData() {
        refreshInputs();
        notifyDataSetChanged();
    }

    private void refreshInputs() {
        List<TvInputInfo> serviceInputs;
        this.mInputs.clear();
        this.mVisibleInputs.clear();
        this.mPhysicalTunerInputs.clear();
        this.mVirtualTunerInputs.clear();
        this.mIsBundledTunerVisible = false;
        if (this.mTvManager == null || (serviceInputs = this.mTvManager.getTvInputList()) == null) {
            return;
        }
        for (int i = 0; i < serviceInputs.size(); i++) {
            TvInputInfo input = serviceInputs.get(i);
            inputAdded(input, true);
        }
        Collections.sort(this.mVisibleInputs, this.mComp);
    }

    private void hideBundledTunerInput(boolean isRefresh) {
        if (!this.mIsBundledTunerVisible) {
            return;
        }
        for (int i = this.mVisibleInputs.size() - 1; i >= 0; i--) {
            if (this.mVisibleInputs.get(i).isBundledTuner()) {
                this.mVisibleInputs.remove(i);
                if (!isRefresh) {
                    notifyItemRemoved(i);
                }
                this.mIsBundledTunerVisible = false;
            }
        }
    }

    private void showBundledTunerInput(boolean isRefresh) {
        if (this.mIsBundledTunerVisible) {
            return;
        }
        TvInputEntry bundledTuner = new TvInputEntry(Partner.get(this.mContext).getBundledTunerTitle(), Partner.get(this.mContext).getBundledTunerBanner(), Partner.get(this.mContext).getBundledTunerLabelColorOption(0), -3);
        if (isRefresh) {
            this.mVisibleInputs.add(bundledTuner);
        } else {
            notifyItemInserted(insertEntryIntoSortedList(bundledTuner, this.mVisibleInputs));
        }
        this.mIsBundledTunerVisible = true;
    }

    private void addInputEntry(TvInputInfo input, boolean isRefresh) {
        TvInputInfo parentInfo;
        try {
            int state = this.mTvManager.getInputState(input.getId());
            TvInputEntry parentEntry = null;
            if (this.mInputs.get(input.getId()) == null) {
                if (input.getParentId() != null && !isConnectedToHdmiSwitchReflection(input) && (parentInfo = this.mTvManager.getTvInputInfo(input.getParentId())) != null) {
                    parentEntry = this.mInputs.get(parentInfo.getId());
                    if (parentEntry == null) {
                        parentEntry = new TvInputEntry(parentInfo, (TvInputEntry) null, this.mTvManager.getInputState(parentInfo.getId()), this.mContext);
                        this.mInputs.put(parentInfo.getId(), parentEntry);
                    }
                    parentEntry.mNumChildren++;
                }
                TvInputEntry entry = new TvInputEntry(input, parentEntry, state, this.mContext);
                this.mInputs.put(input.getId(), entry);
                if (!entry.mInfo.isHidden(this.mContext)) {
                    if (parentEntry != null && parentEntry.mInfo.isHidden(this.mContext)) {
                        return;
                    }
                    if (isRefresh) {
                        this.mVisibleInputs.add(entry);
                        if (parentEntry == null || parentEntry.mInfo.getParentId() != null || this.mVisibleInputs.contains(parentEntry)) {
                            return;
                        }
                        this.mVisibleInputs.add(parentEntry);
                        return;
                    }
                    int i = insertEntryIntoSortedList(entry, this.mVisibleInputs);
                    notifyItemInserted(i);
                    if (parentEntry == null || parentEntry.mInfo.getParentId() != null || this.mVisibleInputs.contains(parentEntry)) {
                        return;
                    }
                    int i2 = insertEntryIntoSortedList(parentEntry, this.mVisibleInputs);
                    notifyItemInserted(i2);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e("InputsAdapter", "Failed to get state for Input, droppig entry. Id = " + input.getId());
        }
    }

    private int getIndexInVisibleList(String id) {
        for (int i = 0; i < this.mVisibleInputs.size(); i++) {
            TvInputInfo info = this.mVisibleInputs.get(i).mInfo;
            if (info != null && TextUtils.equals(info.getId(), id)) {
                return i;
            }
        }
        return -1;
    }

    private int insertEntryIntoSortedList(TvInputEntry entry, List<TvInputEntry> list) {
        int i = 0;
        while (i < list.size() && this.mComp.compare(entry, list.get(i)) >= 0) {
            i++;
        }
        if (!list.contains(entry)) {
            list.add(i, entry);
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void inputStateUpdated(String id, int state) {
        TvInputEntry entry = this.mInputs.get(id);
        if (entry == null) {
            return;
        }
        boolean wasConnected = entry.mState != 2;
        boolean isNowConnected = state != 2;
        entry.mState = state;
        int visPos = getIndexInVisibleList(id);
        if (visPos < 0) {
            return;
        }
        if (this.mConfig.mDisableDiconnectedInputs && wasConnected != isNowConnected) {
            this.mVisibleInputs.remove(visPos);
            int i = insertEntryIntoSortedList(entry, this.mVisibleInputs);
            notifyItemMoved(visPos, i);
            notifyItemChanged(i);
            return;
        }
        notifyItemChanged(visPos);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void inputAdded(String id, boolean isRefresh) {
        if (this.mTvManager == null) {
            return;
        }
        inputAdded(this.mTvManager.getTvInputInfo(id), isRefresh);
    }

    private void inputAdded(TvInputInfo info, boolean isRefresh) {
        if (info == null) {
            return;
        }
        if (info.isPassthroughInput()) {
            addInputEntry(info, isRefresh);
            return;
        }
        if (isPhysicalTuner(this.mContext.getPackageManager(), info)) {
            this.mPhysicalTunerInputs.put(info.getId(), info);
            if (this.mConfig.mShowPhysicalTunersSeparately) {
                addInputEntry(info, isRefresh);
                return;
            } else {
                showBundledTunerInput(isRefresh);
                return;
            }
        }
        this.mVirtualTunerInputs.put(info.getId(), info);
        showBundledTunerInput(isRefresh);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void inputRemoved(String id) {
        TvInputEntry entry = this.mInputs.get(id);
        if (entry != null && entry.mInfo != null && entry.mInfo.isPassthroughInput()) {
            removeEntry(id);
        } else {
            removeTuner(id);
        }
    }

    private void removeTuner(String id) {
        removeEntry(id);
        this.mVirtualTunerInputs.remove(id);
        this.mPhysicalTunerInputs.remove(id);
        if (!this.mVirtualTunerInputs.isEmpty()) {
            return;
        }
        if (!this.mPhysicalTunerInputs.isEmpty() && !this.mConfig.mShowPhysicalTunersSeparately) {
            return;
        }
        hideBundledTunerInput(false);
    }

    private void removeEntry(String id) {
        TvInputEntry entry = this.mInputs.get(id);
        if (entry == null) {
            return;
        }
        TvInputEntry parent = entry.mParentEntry;
        if (parent != null) {
            parent.mNumChildren = Math.max(0, parent.mNumChildren - 1);
        }
        this.mInputs.remove(id);
        for (int i = this.mVisibleInputs.size() - 1; i >= 0; i--) {
            if (TextUtils.equals(this.mVisibleInputs.get(i).mInfo.getId(), id)) {
                this.mVisibleInputs.remove(i);
                if (parent != null && parent.mNumChildren == 0) {
                    if ((entry.mState == 2) == (parent.mState == 2)) {
                        if (!this.mVisibleInputs.contains(parent)) {
                            this.mVisibleInputs.add(i, parent);
                            notifyItemChanged(i);
                            return;
                        } else {
                            notifyItemRemoved(i);
                            return;
                        }
                    }
                    int newPos = insertEntryIntoSortedList(parent, this.mVisibleInputs);
                    notifyItemMoved(i, newPos);
                    notifyItemChanged(newPos);
                    return;
                }
                notifyItemRemoved(i);
                return;
            }
            if (this.mVisibleInputs.get(i).mParentEntry != null && TextUtils.equals(this.mVisibleInputs.get(i).mParentEntry.mInfo.getId(), id)) {
                this.mInputs.remove(this.mVisibleInputs.get(i).mInfo.getId());
                this.mVisibleInputs.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    class InputViewHolder extends LauncherViewHolder {
        private final BannerView mBannerView;
        private boolean mEnabled;
        private final ImageView mImageView;
        private final TextView mLabelView;

        public InputViewHolder(View v) {
            super(v);
            this.mEnabled = true;
            if (v instanceof BannerView) {
                this.mBannerView = (BannerView) v;
                this.mImageView = (ImageView) v.findViewById(R.id.input_image);
                this.mLabelView = (TextView) v.findViewById(R.id.input_label);
            } else {
                this.mBannerView = null;
                this.mImageView = null;
                this.mLabelView = null;
            }
        }

        public void init(TvInputEntry entry) {
            this.itemView.setVisibility(0);
            if (entry == null) {
                return;
            }
            boolean connected = entry.isEnabled();
            this.mEnabled = connected;
            this.mBannerView.setEnabled(this.mEnabled);
            if (this.mImageView != null) {
                this.mImageView.setImageDrawable(entry.getImageDrawable());
                this.mImageView.setAlpha(connected ? InputsAdapter.this.mImageAlphaConnected : InputsAdapter.this.mImageAlphaDisconnected);
            }
            if (this.mLabelView != null) {
                this.mLabelView.setText(entry.getLabel());
                this.mLabelView.setAlpha(connected ? InputsAdapter.this.mTextAlphaConnected : InputsAdapter.this.mTextAlphaDisconnected);
                this.mBannerView.setTextViewColor(R.id.input_label, entry.isLabelDarkColor() ? InputsAdapter.this.mTextColorDark : InputsAdapter.this.mTextColorLight);
            }
            setLaunchIntent(entry.getLaunchIntent());
            setLaunchColor(InputsAdapter.this.mContext.getResources().getColor(R.color.input_banner_launch_ripple_color));
        }

        @Override // com.dismal.android.leanbacklauncher.LauncherViewHolder, android.view.View.OnClickListener
        public void onClick(View v) {
            if (!this.mEnabled) {
                return;
            }
            super.onClick(v);
        }
    }

    public class InputCallback extends TvInputManager.TvInputCallback {
        public InputCallback() {
        }

        @Override // android.media.tv.TvInputManager.TvInputCallback
        public void onInputStateChanged(String inputId, int state) {
            InputsAdapter.this.mHandler.sendMessage(InputsAdapter.this.mHandler.obtainMessage(1, state, 0, inputId));
        }

        @Override // android.media.tv.TvInputManager.TvInputCallback
        public void onInputAdded(String inputId) {
            InputsAdapter.this.mHandler.sendMessage(InputsAdapter.this.mHandler.obtainMessage(2, inputId));
        }

        @Override // android.media.tv.TvInputManager.TvInputCallback
        public void onInputRemoved(String inputId) {
            InputsAdapter.this.mHandler.sendMessage(InputsAdapter.this.mHandler.obtainMessage(3, inputId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isPhysicalTuner(PackageManager pkgMan, TvInputInfo input) {
        if (input.createSetupIntent() == null) {
            return false;
        }
        boolean mayBeTunerInput = pkgMan.checkPermission("com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA", input.getServiceInfo().packageName) == 0;
        if (mayBeTunerInput) {
            return true;
        }
        try {
            ApplicationInfo ai = pkgMan.getApplicationInfo(input.getServiceInfo().packageName, 0);
            if ((ai.flags & 129) != 0) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getPriorityForType(int type) {
        Integer priority;
        if (this.mTypePriorities != null && (priority = this.mTypePriorities.get(Integer.valueOf(type))) != null) {
            return priority.intValue();
        }
        return Integer.MAX_VALUE;
    }

    private int addToPriorityIfMissing(int key, int priority) {
        if (!this.mTypePriorities.containsKey(Integer.valueOf(key))) {
            int priority2 = priority + 1;
            this.mTypePriorities.put(Integer.valueOf(key), Integer.valueOf(priority));
            return priority2;
        }
        return priority;
    }

    private void setupDeviceTypePriorities() {
        this.mTypePriorities = Partner.get(this.mContext).getInputsOrderMap();
        int priority = this.mTypePriorities.size();
        addToPriorityIfMissing(1000, addToPriorityIfMissing(1003, addToPriorityIfMissing(1005, addToPriorityIfMissing(1008, addToPriorityIfMissing(1001, addToPriorityIfMissing(1002, addToPriorityIfMissing(1004, addToPriorityIfMissing(1006, addToPriorityIfMissing(1007, addToPriorityIfMissing(-2, addToPriorityIfMissing(0, addToPriorityIfMissing(-3, priority))))))))))));
    }

    private HdmiDeviceInfo getHdmiDeviceInfoReflection(TvInputInfo info) {
        if (info == null) return null;
        try {
            java.lang.reflect.Method method = info.getClass().getMethod("getHdmiDeviceInfo");
            return (HdmiDeviceInfo) method.invoke(info);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isConnectedToHdmiSwitchReflection(TvInputInfo info) {
        if (info == null) return false;
        try {
            java.lang.reflect.Method method = info.getClass().getMethod("isConnectedToHdmiSwitch");
            return (Boolean) method.invoke(info);
        } catch (Exception e) {
            return false;
        }
    }
}
