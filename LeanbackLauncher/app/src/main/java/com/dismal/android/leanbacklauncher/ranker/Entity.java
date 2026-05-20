package com.dismal.android.leanbacklauncher.ranker;

import android.content.Context;
import android.util.Log;
import com.dismal.android.leanbacklauncher.R;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
class Entity {
    public double mBonus;
    private long mBonusFadePeriod;
    private long mBonusTime;
    private LinkedHashMap<String, Bucket> mBucketList;
    private DbHelper mDbHelper;
    private boolean mHasPostedRecommendations;
    private String mKey;
    private HashMap<String, Long> mLastOpened;
    private HashMap<String, Long> mRankOrder;
    private final SignalsAggregator mSignalsAggregator;

    public Entity(Context context, DbHelper helper, String key, long lastOpenTime, long initialOrder, boolean postedRec) {
        this(context, helper, key, initialOrder, postedRec);
        setLastOpenedTimeStamp(null, lastOpenTime);
    }

    public Entity(Context context, DbHelper helper, String key, long initialOrder, boolean postedRec) {
        this(context, helper, key);
        this.mRankOrder.put(null, Long.valueOf(initialOrder));
        this.mHasPostedRecommendations = postedRec;
    }

    public Entity(Context ctx, DbHelper helper, String key) {
        this.mBucketList = new LinkedHashMap<>();
        this.mSignalsAggregator = new SignalsAggregator();
        this.mLastOpened = new HashMap<>();
        this.mRankOrder = new HashMap<>();
        this.mDbHelper = helper;
        this.mBonus = 0.0d;
        this.mBonusTime = 0L;
        this.mKey = key;
        this.mHasPostedRecommendations = false;
        this.mBonusFadePeriod = ((long) ctx.getResources().getInteger(R.integer.install_fade_period_days)) * 86400000;
        if (this.mBonusFadePeriod != 0) {
            return;
        }
        this.mBonusFadePeriod = 432000000L;
    }

    public Set<String> getEntityComponents() {
        return this.mLastOpened.keySet();
    }

    public void setLastOpenedTimeStamp(String component, long timeStamp) {
        this.mLastOpened.put(component, Long.valueOf(timeStamp));
    }

    public long getLastOpenedTimeStamp(String component) {
        Long lastOpened = this.mLastOpened.get(component);
        if (lastOpened == null && (lastOpened = this.mLastOpened.get(null)) == null) {
            lastOpened = 0L;
        }
        return lastOpened.longValue();
    }

    public long getOrder(String component) {
        Long rankOrder = this.mRankOrder.get(component);
        if (rankOrder == null && (rankOrder = this.mRankOrder.get(null)) == null) {
            rankOrder = 0L;
        }
        return rankOrder.longValue();
    }

    public void setOrder(String component, long order) {
        this.mRankOrder.put(component, Long.valueOf(order));
    }

    public boolean hasPostedRecommendations() {
        return this.mHasPostedRecommendations;
    }

    public void markPostedRecommendations() {
        this.mHasPostedRecommendations = true;
    }

    public String getKey() {
        return new String(this.mKey);
    }

    public synchronized void onAction(int actionType, String component, String group) {
        Date date = new Date();
        long time = date.getTime();
        if (this.mDbHelper.getMostRecentTimeStamp() >= time) {
            time = this.mDbHelper.getMostRecentTimeStamp() + 1;
        }
        switch (actionType) {
            case 0:
                if (getLastOpenedTimeStamp(component) == 0) {
                    addBonusValue(Ranker.INSTALL_BONUS);
                    setLastOpenedTimeStamp(component, time);
                }
                return;
            case 1:
                setLastOpenedTimeStamp(component, time);
                return;
            case 2:
            default:
                Bucket bucket = getOrAddBucket(group);
                if (bucket == null) {
                    return;
                }
                ActiveDayBuffer buffer = bucket.getBuffer();
                Signals value = buffer.get(date);
                if (value == null) {
                    value = new Signals();
                }
                switch (actionType) {
                    case 2:
                        value.mClicks++;
                        buffer.set(date, value);
                        touchBucket(group);
                        break;
                    case 4:
                        value.mImpressions++;
                        buffer.set(date, value);
                        touchBucket(group);
                        break;
                }
                return;
            case 3:
                this.mLastOpened.clear();
                this.mBonus = 0.0d;
                this.mBonusTime = 0L;
                this.mBucketList.clear();
                return;
        }
    }

    public synchronized Bucket addBucket(String group, long timeStamp) {
        String group2 = safeGroupId(group);
        if (this.mBucketList.containsKey(group2)) {
            Log.e("Entity", "Entity.addBucket: Got duplicated Group ID: " + group2);
            Bucket bucket = this.mBucketList.get(group2);
            bucket.setTimestamp(timeStamp);
            this.mBucketList.remove(group2);
            this.mBucketList.put(group2, bucket);
            return bucket;
        }
        if (this.mBucketList.size() >= 100) {
            String removedGroup = this.mBucketList.keySet().iterator().next();
            this.mBucketList.remove(removedGroup);
            if (this.mDbHelper != null) {
                this.mDbHelper.removeGroupData(this.mKey, removedGroup);
            }
        }
        Bucket bucket2 = new Bucket(timeStamp);
        this.mBucketList.put(group2, bucket2);
        return bucket2;
    }

    public synchronized ArrayList<String> getGroupIds() {
        ArrayList<String> groups;
        String groupId;
        groups = new ArrayList<>();
        Iterator<String> keyIterator = this.mBucketList.keySet().iterator();
        while (keyIterator.hasNext() && (groupId = keyIterator.next()) != null) {
            groups.add(groupId);
        }
        return groups;
    }

    public synchronized long getGroupTimeStamp(String group) {
        Bucket bucket = this.mBucketList.get(safeGroupId(group));
        if (bucket != null) {
            return bucket.getTimestamp();
        }
        return 0L;
    }

    public synchronized ActiveDayBuffer getSignalsBuffer(String group) {
        Bucket bucket = this.mBucketList.get(safeGroupId(group));
        if (bucket == null) {
            return null;
        }
        return bucket.getBuffer();
    }

    public synchronized void setBonusValues(double bonus, long timeStamp) {
        double bonusAge = timeStamp - System.currentTimeMillis();
        if (bonusAge >= this.mBonusFadePeriod) {
            this.mBonus = 0.0d;
            this.mBonusTime = 0L;
        } else {
            this.mBonus = bonus;
            this.mBonusTime = timeStamp;
        }
    }

    public double getBonus() {
        return this.mBonus;
    }

    public long getBonusTimeStamp() {
        return this.mBonusTime;
    }

    public double getAmortizedBonus() {
        if (this.mBonusTime == 0 && this.mBonus == 0.0d) {
            return 0.0d;
        }
        double timeDiff = System.currentTimeMillis() - this.mBonusTime;
        double factor = 1.0d - (timeDiff / this.mBonusFadePeriod);
        if (factor < 0.0d) {
            return 0.0d;
        }
        return this.mBonus * factor;
    }

    private void addBonusValue(double newBonus) {
        this.mBonus = getAmortizedBonus() + newBonus;
        this.mBonusTime = System.currentTimeMillis();
    }

    public synchronized double getCtr(Normalizer ctrNormalizer, String group) {
        double ctr;
        ctr = 0.0d;
        Bucket bucket = this.mBucketList.get(safeGroupId(group));
        if (bucket != null) {
            ActiveDayBuffer buffer = bucket.getBuffer();
            double aggregatedCtr = buffer.getAggregatedScore(this.mSignalsAggregator);
            if (aggregatedCtr != -1.0d) {
                ctr = ctrNormalizer.getNormalizedValue(aggregatedCtr);
            }
        }
        return ctr;
    }

    public synchronized double getNotificationScore(Normalizer ctrNormalizer, String group, double score, double ctr) {
        double v;
        if (ctr == -1.0d) {
            ctr = getCtr(ctrNormalizer, group);
            v = getAmortizedBonus() + ctr + score;
        } else {
            v = getAmortizedBonus() + ctr + score;
        }
        return ((1.0d / (Math.exp(-v) + 1.0d)) - 0.5d) * 2.0d;
    }

    private String safeGroupId(String id) {
        return id == null ? new String("") : id;
    }

    private Bucket getOrAddBucket(String group) {
        String group2 = safeGroupId(group);
        Bucket bucket = this.mBucketList.get(group2);
        if (bucket == null) {
            return addBucket(group2, System.currentTimeMillis());
        }
        return bucket;
    }

    private void touchBucket(String group) {
        Bucket bucket = this.mBucketList.remove(group);
        if (bucket == null) {
            return;
        }
        bucket.updateTimestamp();
        this.mBucketList.put(group, bucket);
    }
}
