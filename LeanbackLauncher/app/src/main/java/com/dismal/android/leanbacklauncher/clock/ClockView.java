package com.dismal.android.leanbacklauncher.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextClock;
import java.text.SimpleDateFormat;

/* JADX INFO: loaded from: classes.dex */
public class ClockView extends TextClock {
    private BroadcastReceiver mIntentReceiver;

    public ClockView(Context context) {
        this(context, null, 0);
    }

    public ClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.dismal.android.leanbacklauncher.clock.ClockView.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.TIME_SET".equals(action) && !"android.intent.action.TIMEZONE_CHANGED".equals(action) && !"android.intent.action.LOCALE_CHANGED".equals(action)) {
                    return;
                }
                ClockView.this.updatePatterns();
            }
        };
        updatePatterns();
    }

    @Override // android.widget.TextClock, android.widget.TextView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, filter, null, null);
        updatePatterns();
    }

    @Override // android.widget.TextClock, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePatterns() {
        SimpleDateFormat format = (SimpleDateFormat) DateFormat.getTimeFormat(getContext());
        String formatString = format.toPattern().replace("a", "").trim();
        setFormat12Hour(formatString);
        setFormat24Hour(formatString);
    }
}
