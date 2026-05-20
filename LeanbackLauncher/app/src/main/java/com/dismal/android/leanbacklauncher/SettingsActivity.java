package com.dismal.android.leanbacklauncher;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import com.dismal.android.leanbacklauncher.ranker.DbHelper;
import com.dismal.android.recline.app.DialogFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/* JADX INFO: loaded from: classes.dex */
public class SettingsActivity extends Activity implements DialogFragment.Action.Listener, DbHelper.BlacklistListener {
    private String mClickedActionTitle;
    private DbHelper mDbHelper;
    private ArrayList<String> mPackageNames = null;
    private HashSet<String> mBlacklistedPackageNames = null;
    private boolean mSaved = true;

    private class ActionComparator implements Comparator<DialogFragment.Action> {
        /* synthetic */ ActionComparator(SettingsActivity this$0, ActionComparator actionComparator) {
            this();
        }

        private ActionComparator() {
        }

        @Override // java.util.Comparator
        public int compare(DialogFragment.Action lhs, DialogFragment.Action rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return compare(lhs.getTitle(), rhs.getTitle());
        }

        private int compare(String lhs, String rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.settings_dialog_bg_protection));
        this.mDbHelper = ((LauncherApplication) getApplication()).getDbHelper();
        this.mDbHelper.getEntityKeys(this);
        this.mDbHelper.getEntityBlacklist(this);
    }

    @Override // android.app.Activity
    public void onPause() {
        saveBlacklistIfNecessary();
        super.onPause();
    }

    @Override // android.app.Activity
    public void onStop() {
        super.onStop();
        finish();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        super.onBackPressed();
        if (!saveBlacklistIfNecessary()) {
            return;
        }
        DialogFragment frag = DialogFragment.getCurrentDialogFragment(getFragmentManager());
        ArrayList<DialogFragment.Action> actions = frag.getActions();
        int size = actions.size();
        for (int a = 0; a < size; a++) {
            if (TextUtils.equals(actions.get(a).getKey(), "recs")) {
                frag.setActions(getBaseActions());
                return;
            }
        }
    }

    @Override // com.dismal.android.recline.app.DialogFragment.Action.Listener
    public void onActionClicked(DialogFragment.Action action) {
        String key = action.getKey();
        if ("recs".equals(action.getKey())) {
            showBlacklistDialog();
            return;
        }
        action.setChecked(!action.isChecked());
        this.mSaved = false;
        if (action.isChecked()) {
            this.mBlacklistedPackageNames.remove(key);
        } else {
            this.mBlacklistedPackageNames.add(key);
        }
        DialogFragment fragment = DialogFragment.getCurrentDialogFragment(getFragmentManager());
        if (fragment == null) {
            return;
        }
        fragment.setActions(fragment.getActions());
    }

    private void attemptToShowBaseDialog() {
        if (this.mPackageNames == null || this.mBlacklistedPackageNames == null) {
            return;
        }
        String title = getString(R.string.settings_dialog_title);
        ArrayList<DialogFragment.Action> actions = getBaseActions();
        DialogFragment.add(getFragmentManager(), buildFragment(title, R.drawable.ic_settings_home, new DialogFragment.Builder().actions(actions)));
    }

    private void showBlacklistDialog() {
        CharSequence appTitle;
        int resId;
        PackageManager pkgMan = getPackageManager();
        String title = getString(R.string.recommendation_blacklist_content_title);
        String description = getString(R.string.recommendation_blacklist_content_description);
        ArrayList<DialogFragment.Action> actions = new ArrayList<>(this.mPackageNames.size());
        for (String key : this.mPackageNames) {
            try {
                ApplicationInfo info = pkgMan.getApplicationInfo(key, 0);
                appTitle = pkgMan.getApplicationLabel(info);
                resId = info.banner;
                if (resId == 0) {
                    resId = info.icon;
                }
            } catch (PackageManager.NameNotFoundException e) {
                appTitle = null;
                resId = 0;
            }
            if (!TextUtils.isEmpty(appTitle)) {
                actions.add(new DialogFragment.Action.Builder().title(appTitle.toString()).resourcePackageName(key).drawableResource(resId).key(key).checked(!this.mBlacklistedPackageNames.contains(key)).build());
            }
        }
        Collections.sort(actions, new ActionComparator(this, null));
        DialogFragment.add(getFragmentManager(), buildFragment(title, R.drawable.ic_settings_home, new DialogFragment.Builder().description(description).actions(actions)));
    }

    private DialogFragment buildFragment(String title, int iconResourceId, DialogFragment.Builder builder) {
        if (!TextUtils.isEmpty(this.mClickedActionTitle)) {
            builder.breadcrumb(this.mClickedActionTitle);
        }
        if (!TextUtils.isEmpty(title)) {
            builder.title(title);
            this.mClickedActionTitle = title;
        }
        if (iconResourceId != 0) {
            builder.iconResourceId(iconResourceId);
            builder.iconBackgroundColor(getResources().getColor(R.color.settings_icon_background));
        }
        return builder.build();
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.DbHelper.BlacklistListener
    public void onEntityKeysReady(ArrayList<String> keys) {
        if (keys == null) {
            return;
        }
        this.mPackageNames = new ArrayList<>(keys);
        attemptToShowBaseDialog();
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.DbHelper.BlacklistListener
    public void onEntityBlacklistReady(ArrayList<String> keys) {
        if (keys == null) {
            return;
        }
        this.mBlacklistedPackageNames = new HashSet<>(keys);
        attemptToShowBaseDialog();
    }

    private boolean saveBlacklistIfNecessary() {
        if (!this.mSaved && this.mBlacklistedPackageNames != null) {
            this.mSaved = true;
            this.mDbHelper.saveEntityBlacklist(new ArrayList<>(this.mBlacklistedPackageNames));
            return true;
        }
        return false;
    }

    private ArrayList<DialogFragment.Action> getBaseActions() {
        ArrayList<DialogFragment.Action> actions = new ArrayList<>(1);
        actions.add(new DialogFragment.Action.Builder().title(getString(R.string.recommendation_blacklist_action_title)).description(getResources().getQuantityString(R.plurals.recommendation_blacklist_action_description, this.mBlacklistedPackageNames.size(), Integer.valueOf(this.mBlacklistedPackageNames.size()))).key("recs").build());
        return actions;
    }
}
