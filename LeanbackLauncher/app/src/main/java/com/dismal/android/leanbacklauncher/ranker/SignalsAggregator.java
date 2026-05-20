package com.dismal.android.leanbacklauncher.ranker;

import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
class SignalsAggregator implements Aggregator<Signals> {
    private SumAggregator<Integer> mClicks = new SumAggregator<>();
    private SumAggregator<Integer> mImpressions = new SumAggregator<>();

    SignalsAggregator() {
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public void add(Date date, Signals value) {
        this.mClicks.add(date, Integer.valueOf(value.mClicks));
        this.mImpressions.add(date, Integer.valueOf(value.mImpressions));
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public double getAggregatedScore() {
        double impressions = this.mImpressions.getAggregatedScore();
        if (impressions > 0.0d) {
            return this.mClicks.getAggregatedScore() / impressions;
        }
        return 0.0d;
    }

    @Override // com.dismal.android.leanbacklauncher.ranker.Aggregator
    public void reset() {
        this.mClicks.reset();
        this.mImpressions.reset();
    }
}
