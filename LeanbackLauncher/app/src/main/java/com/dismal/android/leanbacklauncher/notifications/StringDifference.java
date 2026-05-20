package com.dismal.android.leanbacklauncher.notifications;

import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.lang.reflect.Array;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class StringDifference {

    /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] f6com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;

    public static class ExtractDeleteAndUpdateResult {
        ArrayList<EditItem> mItems;
        int mRemainingEditItems;
    }

    /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
    private static /* synthetic */ int[] m168getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues() {
        if (f6com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues != null) {
            return f6com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;
        }
        int[] iArr = new int[EditItem.Op.valuesCustom().length];
        try {
            iArr[EditItem.Op.DELETE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[EditItem.Op.INSERT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[EditItem.Op.MOVE.ordinal()] = 5;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[EditItem.Op.SUB.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[EditItem.Op.UPDATE.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        f6com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues = iArr;
        return iArr;
    }

    private StringDifference() {
    }

    public static final class EditItem {

        /* JADX INFO: renamed from: -com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
        private static /* synthetic */ int[] f7com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;
        int mDstIndex;
        StatusBarNotification mItem;
        Op mOp;
        int mSrcIndex;

        /* JADX INFO: renamed from: -getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues, reason: not valid java name */
        private static /* synthetic */ int[] m169getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues() {
            if (f7com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues != null) {
                return f7com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues;
            }
            int[] iArr = new int[Op.valuesCustom().length];
            try {
                iArr[Op.DELETE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Op.INSERT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Op.MOVE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Op.SUB.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Op.UPDATE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            f7com_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues = iArr;
            return iArr;
        }

        public enum Op {
            INSERT,
            DELETE,
            MOVE,
            SUB,
            UPDATE;

            /* JADX INFO: renamed from: values, reason: to resolve conflict with enum method */
            public static Op[] valuesCustom() {
                return values();
            }
        }

        public String toString() {
            switch (m169getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues()[this.mOp.ordinal()]) {
                case 1:
                    String s = "Delete " + this.mSrcIndex;
                    return s;
                case 2:
                    String s2 = "Insert " + this.mSrcIndex + "  " + NotificationsTestHarness.toString(this.mItem);
                    return s2;
                case 3:
                    String s3 = "Move " + this.mSrcIndex + " " + this.mDstIndex;
                    return s3;
                case 4:
                    String s4 = "Sub " + this.mSrcIndex + " " + NotificationsTestHarness.toString(this.mItem);
                    return s4;
                case 5:
                    String s5 = "Update " + this.mSrcIndex + " " + NotificationsTestHarness.toString(this.mItem);
                    return s5;
                default:
                    return "?";
            }
        }
    }

    public static ArrayList<EditItem> calculateStringAlignment(ArrayList<StatusBarNotification> src, ArrayList<StatusBarNotification> dst) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance(src, dst);
        ArrayList<EditItem> editItems = levenshteinDistance.getChangeList();
        int itemNdx = 0;
        int itemDim = editItems.size();
        int srcNdx = 0;
        int srcDim = src.size();
        int dstNdx = 0;
        int dstDim = dst.size();
        int cumulativeAdjustment = 0;
        while (itemNdx < itemDim && srcNdx < srcDim && dstNdx < dstDim) {
            EditItem item = editItems.get(itemNdx);
            int itemSrcNdx = item.mSrcIndex;
            while (srcNdx < srcDim && dstNdx < dstDim && dstNdx + cumulativeAdjustment < itemSrcNdx) {
                StatusBarNotification srcSbn = src.get(srcNdx);
                StatusBarNotification dstSbn = dst.get(dstNdx);
                if (srcSbn.getPostTime() != dstSbn.getPostTime()) {
                    EditItem updItem = new EditItem();
                    updItem.mOp = EditItem.Op.UPDATE;
                    updItem.mSrcIndex = dstNdx + cumulativeAdjustment;
                    updItem.mItem = srcSbn;
                    editItems.add(itemNdx, updItem);
                    itemNdx++;
                    itemDim++;
                }
                srcNdx++;
                dstNdx++;
            }
            switch (m168getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues()[item.mOp.ordinal()]) {
                case 1:
                    cumulativeAdjustment--;
                    dstNdx++;
                    break;
                case 2:
                    cumulativeAdjustment++;
                    srcNdx++;
                    break;
                case 3:
                    srcNdx++;
                    dstNdx++;
                    break;
            }
            itemNdx++;
        }
        while (srcNdx < srcDim && dstNdx < dstDim) {
            StatusBarNotification srcSbn2 = src.get(srcNdx);
            StatusBarNotification dstSbn2 = dst.get(dstNdx);
            if (srcSbn2.getPostTime() != dstSbn2.getPostTime()) {
                EditItem updItem2 = new EditItem();
                updItem2.mOp = EditItem.Op.UPDATE;
                updItem2.mSrcIndex = dstNdx + cumulativeAdjustment;
                updItem2.mItem = srcSbn2;
                editItems.add(updItem2);
            }
            srcNdx++;
            dstNdx++;
        }
        return editItems;
    }

    public static ExtractDeleteAndUpdateResult extractDeleteAndUpdateItems(ArrayList<EditItem> editItems, ArrayList<StatusBarNotification> targetList) {
        ExtractDeleteAndUpdateResult extractDeleteAndUpdateResult = new ExtractDeleteAndUpdateResult();
        extractDeleteAndUpdateResult.mItems = new ArrayList<>();
        int cumulativeAdjustment = 0;
        int n = editItems.size();
        for (int i = 0; i < n; i++) {
            EditItem item = editItems.get(i);
            switch (m168getcom_google_android_leanbacklauncher_notifications_StringDifference$EditItem$OpSwitchesValues()[item.mOp.ordinal()]) {
                case 1:
                    item.mSrcIndex += cumulativeAdjustment;
                    extractDeleteAndUpdateResult.mItems.add(item);
                    break;
                case 2:
                    cumulativeAdjustment--;
                    extractDeleteAndUpdateResult.mRemainingEditItems++;
                    break;
                case 3:
                case 4:
                    item.mSrcIndex += cumulativeAdjustment;
                    if (item.mSrcIndex < 0) {
                        Log.d("CMD", "StringDifference::extractDeleteAndUpdateItems  fail su " + item.mSrcIndex);
                    } else {
                        StatusBarNotification targetItem = targetList.get(item.mSrcIndex);
                        if (NotificationUtils.isUpdate(item.mItem, targetItem)) {
                            item.mOp = EditItem.Op.DELETE;
                            cumulativeAdjustment--;
                        }
                        extractDeleteAndUpdateResult.mItems.add(item);
                        extractDeleteAndUpdateResult.mRemainingEditItems++;
                    }
                    break;
            }
        }
        return extractDeleteAndUpdateResult;
    }

    private static class LevenshteinDistance {
        private int[][] D;
        private ArrayList<StatusBarNotification> mPendingList;
        private final int mPendingListDim;
        private final int mVisibleListDim;

        LevenshteinDistance(ArrayList<StatusBarNotification> willView, ArrayList<StatusBarNotification> nowView) {
            int min;
            this.mPendingListDim = willView.size();
            this.mVisibleListDim = nowView.size();
            this.mPendingList = willView;
            this.D = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, this.mPendingListDim + 1, this.mVisibleListDim + 1);
            for (int i = 1; i <= this.mPendingListDim; i++) {
                this.D[i][0] = 536870912 | i;
            }
            for (int i2 = 1; i2 <= this.mVisibleListDim; i2++) {
                this.D[0][i2] = 1073741824 | i2;
            }
            for (int i3 = 1; i3 <= this.mPendingListDim; i3++) {
                for (int j = 1; j <= this.mVisibleListDim; j++) {
                    if (NotificationUtils.equals(willView.get(i3 - 1), nowView.get(j - 1))) {
                        min = (this.D[i3 - 1][j - 1] & 536870911) | Integer.MIN_VALUE;
                    } else {
                        int del = (this.D[i3 - 1][j] & 536870911) + 1;
                        int ins = (this.D[i3][j - 1] & 536870911) + 1;
                        int sub = (this.D[i3 - 1][j - 1] & 536870911) + 2;
                        if (del < ins) {
                            if (del <= sub) {
                                min = del | 1073741824;
                            } else {
                                min = sub | 1610612736;
                            }
                        } else if (ins <= sub) {
                            min = ins | 536870912;
                        } else {
                            min = sub | 1610612736;
                        }
                    }
                    this.D[i3][j] = min;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ArrayList<EditItem> getChangeList() {
            int ndx;
            int dir;
            int dim = this.mPendingListDim + this.mVisibleListDim + 1;
            int[] changeList = new int[dim];
            int i = this.mPendingListDim;
            int j = this.mVisibleListDim;
            int ndx2 = 0;
            while (true) {
                if (i != 0 || j != 0) {
                    switch (this.D[i][j] & (-536870912)) {
                        case Integer.MIN_VALUE:
                            i--;
                            j--;
                            ndx = ndx2;
                            break;
                        case 536870912:
                        case 1073741824:
                        case 1610612736:
                            int ins = i > 0 ? this.D[i - 1][j] : Integer.MAX_VALUE;
                            int del = j > 0 ? this.D[i][j - 1] : Integer.MAX_VALUE;
                            int sub = (i <= 0 || j <= 0) ? Integer.MAX_VALUE : this.D[i - 1][j - 1];
                            if (del < ins) {
                                if (del <= sub) {
                                    dir = 1073741824;
                                } else {
                                    dir = 1610612736;
                                }
                            } else if (ins <= sub) {
                                dir = 536870912;
                            } else {
                                dir = 1610612736;
                            }
                            int change = 0;
                            switch (dir) {
                                case 536870912:
                                    change = (i - 1) | 536870912;
                                    i--;
                                    break;
                                case 1073741824:
                                    change = i | 1073741824;
                                    j--;
                                    break;
                                case 1610612736:
                                    change = (i - 1) | 1610612736;
                                    i--;
                                    j--;
                                    break;
                            }
                            ndx = ndx2 + 1;
                            changeList[ndx2] = change;
                            break;
                        default:
                            ndx = ndx2;
                            break;
                    }
                    ndx2 = ndx;
                } else {
                    int i2 = 0;
                    for (int j2 = ndx2 - 1; i2 < j2; j2--) {
                        int t = changeList[i2];
                        changeList[i2] = changeList[j2];
                        changeList[j2] = t;
                        i2++;
                    }
                    ArrayList<EditItem> changeItems = new ArrayList<>();
                    for (int k = 0; k < ndx2; k++) {
                        EditItem changeItem = new EditItem();
                        int change2 = changeList[k];
                        int op = change2 & (-536870912);
                        switch (op) {
                            case 536870912:
                                changeItem.mOp = EditItem.Op.INSERT;
                                break;
                            case 1073741824:
                                changeItem.mOp = EditItem.Op.DELETE;
                                break;
                            case 1610612736:
                                changeItem.mOp = EditItem.Op.SUB;
                                break;
                        }
                        changeItem.mSrcIndex = 536870911 & change2;
                        if (op == 536870912 || op == 1610612736) {
                            changeItem.mItem = this.mPendingList.get(536870911 & change2);
                        }
                        changeItems.add(changeItem);
                    }
                    return changeItems;
                }
            }
        }
    }
}
