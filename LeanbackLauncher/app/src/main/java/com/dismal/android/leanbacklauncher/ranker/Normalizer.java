package com.dismal.android.leanbacklauncher.ranker;

/* JADX INFO: loaded from: classes.dex */
class Normalizer {
    private double mSum;

    public Normalizer() {
        reset();
    }

    public double getNormalizedValue(double value) {
        if (this.mSum == 0.0d) {
            return 0.0d;
        }
        double normalized = value / this.mSum;
        return normalized;
    }

    public void reset() {
        this.mSum = 0.0d;
    }
}
