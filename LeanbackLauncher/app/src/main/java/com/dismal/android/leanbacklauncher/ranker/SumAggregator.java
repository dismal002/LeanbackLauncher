package com.dismal.android.leanbacklauncher.ranker;

import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
class SumAggregator<T> implements Aggregator<T> {
    private double mSum = 0.0d;

    SumAggregator() {
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public void add(Date date, T t) {
        if (t instanceof Integer) {
            this.mSum += (double) ((Integer) t).intValue();
        } else if (t instanceof Long) {
            this.mSum += ((Long) t).longValue();
        } else {
            if (!(t instanceof Double)) {
                return;
            }
            this.mSum += ((Double) t).doubleValue();
        }
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public double getAggregatedScore() {
        return this.mSum;
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public void reset() {
        this.mSum = 0.0d;
    }
}
