package com.dismal.android.leanbacklauncher.ranker;

import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
interface Aggregator<T> {
    void add(Date date, T t);

    double getAggregatedScore();

    void reset();
}
