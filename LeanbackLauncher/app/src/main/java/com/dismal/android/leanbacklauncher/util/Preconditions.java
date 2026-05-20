package com.dismal.android.leanbacklauncher.util;

/* JADX INFO: loaded from: classes.dex */
public final class Preconditions {
    private Preconditions() {
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static void checkState(boolean expression) {
        if (expression) {
        } else {
            throw new IllegalStateException();
        }
    }

    public static void checkArgument(boolean expression) {
        if (expression) {
        } else {
            throw new IllegalArgumentException();
        }
    }
}
