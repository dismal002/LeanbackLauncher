package com.dismal.android.leanbacklauncher.ranker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.dismal.android.leanbacklauncher.util.Util;
import java.util.ArrayList;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public class DbHelper extends SQLiteOpenHelper {
    private Context mContext;
    private Object mLock;
    private Long mMostRecentTimeStamp;
    private BlacklistListener mPermanentBlacklistListener;
    private static String TAG = "DbHelper";
    private static boolean DEBUG = false;

    public interface BlacklistListener {
        void onEntityBlacklistReady(ArrayList<String> arrayList);

        void onEntityKeysReady(ArrayList<String> arrayList);
    }

    public interface Listener {
        void onEntitiesReady(HashMap<String, Entity> map);
    }

    public DbHelper(Context context) {
        super(context, "launcher.db", (SQLiteDatabase.CursorFactory) null, 10);
        this.mMostRecentTimeStamp = new Long(0L);
        this.mLock = new Object();
        this.mContext = context;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(" CREATE TABLE IF NOT EXISTS entity ( key TEXT  PRIMARY KEY  , notif_bonus REAL  , bonus_timestamp INTEGER  , oob_order INTEGER  , has_recs INTEGER  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS entity_scores ( key TEXT  NOT NULL  , component TEXT  , entity_score INTEGER  NOT NULL  , last_opened INTEGER  ,  PRIMARY KEY  ( key , component )  ,  FOREIGN KEY  ( key )  REFERENCES entity ( key )  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS rec_blacklist ( key TEXT  PRIMARY KEY  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS buckets ( key TEXT  NOT NULL  , group_id TEXT  NOT NULL  , last_updated INTEGER  NOT NULL  ,  PRIMARY KEY  ( key , group_id )  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS buffer_scores ( _id INTEGER  NOT NULL  , key TEXT  NOT NULL  , group_id TEXT  NOT NULL  , day INTEGER  NOT NULL  , mClicks INTEGER  , mImpressions INTEGER  ,  PRIMARY KEY  ( _id , group_id , key )  ) ");
        Util.setInitialRankingAppliedFlag(this.mContext, false);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor c;
        db.execSQL(" CREATE TABLE IF NOT EXISTS entity ( key TEXT  PRIMARY KEY  , notif_bonus REAL  , bonus_timestamp INTEGER  , oob_order INTEGER  , has_recs INTEGER  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS entity_scores ( key TEXT  NOT NULL  , component TEXT  , entity_score INTEGER  NOT NULL  , last_opened INTEGER  ,  PRIMARY KEY  ( key , component )  ,  FOREIGN KEY  ( key )  REFERENCES entity ( key )  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS rec_blacklist ( key TEXT  PRIMARY KEY  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS buckets ( key TEXT  NOT NULL  , group_id TEXT  NOT NULL  , last_updated INTEGER  NOT NULL  ,  PRIMARY KEY  ( key , group_id )  ) ");
        db.execSQL(" CREATE TABLE IF NOT EXISTS buffer_scores ( _id INTEGER  NOT NULL  , key TEXT  NOT NULL  , group_id TEXT  NOT NULL  , day INTEGER  NOT NULL  , mClicks INTEGER  , mImpressions INTEGER  ,  PRIMARY KEY  ( _id , group_id , key )  ) ");
        switch (oldVersion) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                db.execSQL(" ALTER TABLE entity ADD COLUMN has_recs INTEGER ");
                db.execSQL(" DELETE FROM rec_blacklist");
            case 8:
                db.execSQL(" DELETE FROM entity_scores");
                String[] projection = {"key", "last_opened"};
                c = db.query("entity", projection, null, null, null, null, null);
                try {
                    int keyIndex = c.getColumnIndexOrThrow("key");
                    int lastOpenedIndex = c.getColumnIndex("last_opened");
                    ContentValues cv = new ContentValues();
                    while (c.moveToNext()) {
                        String key = c.getString(keyIndex);
                        long lastOpened = lastOpenedIndex == -1 ? 0L : c.getLong(lastOpenedIndex);
                        if (!TextUtils.isEmpty(key)) {
                            cv.put("key", key);
                            cv.put("last_opened", Long.valueOf(lastOpened));
                            cv.put("entity_score", Long.valueOf(lastOpened));
                            db.insert("entity_scores", null, cv);
                        }
                        break;
                    }
                    c.close();
                    break;
                } finally {
                }
            case 9:
                String[] p = {"key", "oob_order"};
                c = db.query("entity", p, null, null, null, null, null);
                try {
                    int keyIndex2 = c.getColumnIndexOrThrow("key");
                    int oobOrder = c.getColumnIndex("oob_order");
                    ContentValues cv2 = new ContentValues();
                    while (c.moveToNext()) {
                        String key2 = c.getString(keyIndex2);
                        long order = oobOrder == -1 ? 0L : c.getLong(oobOrder);
                        if (!TextUtils.isEmpty(key2)) {
                            cv2.put("entity_score", Long.valueOf(order));
                            String[] selectionArgs = {key2};
                            db.update("entity_scores", cv2, "key = ? ", selectionArgs);
                        }
                        break;
                    }
                    return;
                } finally {
                }
            default:
                return;
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(" DROP TABLE IF EXISTS entity");
        db.execSQL(" DROP TABLE IF EXISTS entity_scores");
        db.execSQL(" DROP TABLE IF EXISTS rec_blacklist");
        db.execSQL(" DROP TABLE IF EXISTS buckets");
        db.execSQL(" DROP TABLE IF EXISTS buffer_scores");
        onCreate(db);
    }

    public void saveEntity(Entity entity) {
        if (TextUtils.isEmpty(entity.getKey())) {
            return;
        }
        new SaveEntityTask(entity).execute(new Void[0]);
    }

    public void removeEntity(String key, boolean fullRemoval) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        new RemoveEntityTask(key, fullRemoval).execute(new Void[0]);
    }

    public void removeGroupData(String key, String group) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        new RemoveGroupTask(key, group).execute(new Void[0]);
    }

    public void getEntities(Listener listener) {
        new GetEntitiesTask(listener).execute(new Void[0]);
    }

    public void getEntityKeys(BlacklistListener listener) {
        new GetEntityKeysTask(listener, false).execute(new Void[0]);
    }

    public void getEntityBlacklist(BlacklistListener listener) {
        new GetEntityKeysTask(listener, true).execute(new Void[0]);
    }

    public void saveEntityBlacklist(ArrayList<String> keys) {
        new SaveEntityBlacklistTask(keys).execute(new Void[0]);
        if (this.mPermanentBlacklistListener == null) {
            return;
        }
        this.mPermanentBlacklistListener.onEntityBlacklistReady(keys);
    }

    public void setBlacklistListener(BlacklistListener listener, boolean immediatelyFetchBlacklist) {
        if (this.mPermanentBlacklistListener == listener) {
            return;
        }
        this.mPermanentBlacklistListener = listener;
        if (!immediatelyFetchBlacklist) {
            return;
        }
        getEntityBlacklist(listener);
    }

    private class SaveEntityTask extends AsyncTask<Void, Void, Void> {
        private Entity mEntity;
        private String mKey;

        public SaveEntityTask(Entity entity) {
            this.mKey = entity.getKey();
            this.mEntity = entity;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... param) {
            int count;
            ContentValues cv = new ContentValues();
            cv.put("key", this.mKey);
            cv.put("notif_bonus", Double.valueOf(this.mEntity.getBonus()));
            cv.put("bonus_timestamp", Long.valueOf(this.mEntity.getBonusTimeStamp()));
            cv.put("has_recs", this.mEntity.hasPostedRecommendations() ? "1" : "0");
            String[] selectionArgs = {this.mKey};
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            int count2 = db.update("entity", cv, "key = ? ", selectionArgs);
            if (count2 == 0) {
                db.insert("entity", null, cv);
            }
            for (String component : this.mEntity.getEntityComponents()) {
                long timeStamp = this.mEntity.getLastOpenedTimeStamp(component);
                synchronized (DbHelper.this.mLock) {
                    if (DbHelper.this.mMostRecentTimeStamp.longValue() < timeStamp) {
                        DbHelper.this.mMostRecentTimeStamp = Long.valueOf(timeStamp);
                    }
                }
                ContentValues cv2 = new ContentValues();
                cv2.put("key", this.mKey);
                cv2.put("component", component);
                cv2.put("entity_score", Long.valueOf(this.mEntity.getOrder(component)));
                cv2.put("last_opened", Long.valueOf(this.mEntity.getLastOpenedTimeStamp(component)));
                String[] selectionArgs2 = {this.mKey};
                db = DbHelper.this.getWritableDatabase();
                if (component != null) {
                    try {
                        db.delete("entity_scores", "key = ?  AND component IS NULL ", selectionArgs2);
                        String[] selectionArgs3 = {this.mKey, component};
                        count = db.update("entity_scores", cv2, "key = ?  AND component = ? ", selectionArgs3);
                    } catch (Throwable th) {
                        String[] selectionArgs4 = {this.mKey, component};
                        db.update("entity_scores", cv2, "key = ?  AND component = ? ", selectionArgs4);
                        throw th;
                    }
                } else {
                    count = db.update("entity_scores", cv2, "key = ?  AND component IS NULL ", selectionArgs2);
                }
                if (count == 0) {
                    db.insert("entity_scores", null, cv2);
                }
            }
            ArrayList<String> groups = this.mEntity.getGroupIds();
            for (int x = 0; x < groups.size(); x++) {
                String groupId = groups.get(x);
                ContentValues cv3 = new ContentValues();
                cv3.put("key", this.mKey);
                cv3.put("group_id", groupId);
                cv3.put("last_updated", Long.valueOf(this.mEntity.getGroupTimeStamp(groupId)));
                String[] selectionArgs5 = {this.mKey, groupId};
                int count3 = db.update("buckets", cv3, "key = ?  AND group_id = ? ", selectionArgs5);
                if (count3 == 0) {
                    db.insert("buckets", null, cv3);
                }
                ActiveDayBuffer buffer = this.mEntity.getSignalsBuffer(groupId);
                if (buffer != null) {
                    int j = 0;
                    while (true) {
                        Signals value = buffer.getAt(j);
                        if (value != null) {
                            ContentValues cv4 = new ContentValues();
                            int day = buffer.getDayAt(j);
                            if (day != -1) {
                                cv4.put("_id", Integer.valueOf(j));
                                cv4.put("key", this.mKey);
                                cv4.put("group_id", groupId);
                                cv4.put("day", Integer.valueOf(day));
                                cv4.put("mClicks", Integer.valueOf(value.mClicks));
                                cv4.put("mImpressions", Integer.valueOf(value.mImpressions));
                                String[] selectionArgs6 = {this.mKey, groupId, "" + j};
                                int count4 = db.update("buffer_scores", cv4, "key = ?  AND group_id = ?  AND _id = ? ", selectionArgs6);
                                if (count4 == 0) {
                                    db.insert("buffer_scores", null, cv4);
                                }
                            }
                            j++;
                        }
                    }
                }
            }
            if (DbHelper.DEBUG) {
                Log.v(DbHelper.TAG, "Done saving " + this.mKey);
                return null;
            }
            return null;
        }
    }

    private class SaveEntityBlacklistTask extends AsyncTask<Void, Void, Void> {
        private ArrayList<String> mKeys;

        public SaveEntityBlacklistTask(ArrayList<String> keys) {
            this.mKeys = keys;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            db.beginTransaction();
            db.execSQL(" DELETE FROM rec_blacklist");
            for (String key : this.mKeys) {
                ContentValues cv = new ContentValues();
                cv.put("key", key);
                if (db.insert("rec_blacklist", null, cv) != -1 && DbHelper.DEBUG) {
                    Log.v(DbHelper.TAG, "Done saving " + key + " to blacklisted keys");
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            return null;
        }
    }

    private class RemoveEntityTask extends AsyncTask<Void, Void, Void> {
        boolean mFullRemoval;
        private String mKey;

        public RemoveEntityTask(String key, boolean fullRemoval) {
            this.mKey = key;
            this.mFullRemoval = fullRemoval;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            String[] selectionArgs = {this.mKey};
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            if (this.mFullRemoval) {
                db.delete("entity", "key = ? ", selectionArgs);
            } else {
                ContentValues cv = new ContentValues();
                cv.put("key", this.mKey);
                cv.put("notif_bonus", (Integer) 0);
                cv.put("bonus_timestamp", (Integer) 0);
                db.update("entity", cv, "key = ? ", selectionArgs);
            }
            db.delete("entity", "key = ? ", selectionArgs);
            db.delete("buckets", "key = ? ", selectionArgs);
            db.delete("buffer_scores", "key = ? ", selectionArgs);
            db.delete("rec_blacklist", "key = ? ", selectionArgs);
            if (DbHelper.DEBUG) {
                Log.v(DbHelper.TAG, "Done deleting " + this.mKey);
                return null;
            }
            return null;
        }
    }

    private class RemoveGroupTask extends AsyncTask<Void, Void, Void> {
        private String mGroup;
        private String mKey;

        public RemoveGroupTask(String entityKey, String group) {
            this.mKey = entityKey;
            this.mGroup = group;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            String[] selectionArgs = {this.mKey, this.mGroup};
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            db.delete("buckets", "key = ?  AND group_id = ? ", selectionArgs);
            String[] selectionArgs2 = {this.mKey, this.mGroup};
            db.delete("buffer_scores", "key = ?  AND group_id = ? ", selectionArgs2);
            if (DbHelper.DEBUG) {
                Log.v(DbHelper.TAG, "Done deleting Key = " + this.mKey + " , Group = " + this.mGroup);
                return null;
            }
            return null;
        }
    }

    private class GetEntitiesTask extends AsyncTask<Void, Void, HashMap<String, Entity>> {
        private Listener mListener;

        public GetEntitiesTask(Listener listener) {
            this.mListener = listener;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public HashMap<String, Entity> doInBackground(Void... params) {
            Entity entity;
            Entity entity2;
            Entity entity3;
            HashMap<String, Entity> entities = new HashMap<>();
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            Cursor c = db.query("entity", null, null, null, null, null, null);
            try {
                int keyIndex = c.getColumnIndexOrThrow("key");
                int bonusIndex = c.getColumnIndex("notif_bonus");
                int bonusTimeIndex = c.getColumnIndex("bonus_timestamp");
                int orderIndex = c.getColumnIndex("oob_order");
                int postedRecIndex = c.getColumnIndex("has_recs");
                while (c.moveToNext()) {
                    String key = c.getString(keyIndex);
                    double bonus = bonusIndex == -1 ? 0.0d : c.getDouble(bonusIndex);
                    long bonusTime = bonusTimeIndex == -1 ? 0L : c.getLong(bonusTimeIndex);
                    long initialOrder = orderIndex == -1 ? 0L : c.getLong(orderIndex);
                    boolean postedRec = postedRecIndex != -1 && c.getLong(postedRecIndex) == 1;
                    if (!TextUtils.isEmpty(key)) {
                        Entity ent = new Entity(DbHelper.this.mContext, DbHelper.this, key, initialOrder, postedRec);
                        if (bonusTime != 0 && bonus > 0.0d) {
                            ent.setBonusValues(bonus, bonusTime);
                        }
                        entities.put(key, ent);
                    }
                }
                c.close();
                c = db.query("entity_scores", null, null, null, null, null, null);
                try {
                    int keyIndex2 = c.getColumnIndexOrThrow("key");
                    int componentIndex = c.getColumnIndex("component");
                    int entityScoreIndex = c.getColumnIndex("entity_score");
                    int lastOpenedIndex = c.getColumnIndex("last_opened");
                    while (c.moveToNext()) {
                        String key2 = c.getString(keyIndex2);
                        String component = c.getString(componentIndex);
                        long entityScore = entityScoreIndex == -1 ? 0L : c.getLong(entityScoreIndex);
                        long lastOpened = lastOpenedIndex == -1 ? 0L : c.getLong(lastOpenedIndex);
                        synchronized (DbHelper.this.mLock) {
                            if (DbHelper.this.mMostRecentTimeStamp.longValue() < lastOpened) {
                                DbHelper.this.mMostRecentTimeStamp = Long.valueOf(lastOpened);
                            }
                        }
                        if (!TextUtils.isEmpty(key2) && (entity3 = entities.get(key2)) != null) {
                            entity3.setOrder(component, entityScore);
                            entity3.setLastOpenedTimeStamp(component, lastOpened);
                        }
                    }
                    c.close();
                    String[] projection = {"key", "group_id", "last_updated"};
                    c = db.query("buckets", projection, null, null, null, null, "key ASC  , last_updated ASC ");
                    try {
                        int keyIndex3 = c.getColumnIndexOrThrow("key");
                        int groupIndex = c.getColumnIndex("group_id");
                        int timeStampIndex = c.getColumnIndex("last_updated");
                        while (c.moveToNext()) {
                            String key3 = c.getString(keyIndex3);
                            String group = c.getString(groupIndex);
                            long time = c.getLong(timeStampIndex);
                            if (!TextUtils.isEmpty(key3) && (entity2 = entities.get(key3)) != null) {
                                entity2.addBucket(group, time);
                            }
                        }
                        c.close();
                        String[] projection2 = {"_id", "key", "group_id", "day", "mClicks", "mImpressions"};
                        c = db.query("buffer_scores", projection2, null, null, null, null, "key ASC  , group_id ASC  , _id ASC ");
                        try {
                            int keyIndex4 = c.getColumnIndexOrThrow("key");
                            int groupIndex2 = c.getColumnIndex("group_id");
                            int dayIndex = c.getColumnIndex("day");
                            int clicksIndex = c.getColumnIndex("mClicks");
                            int impressionsIndex = c.getColumnIndex("mImpressions");
                            while (c.moveToNext()) {
                                String key4 = c.getString(keyIndex4);
                                String group2 = c.getString(groupIndex2);
                                int day = c.getInt(dayIndex);
                                int clicks = clicksIndex == -1 ? 0 : c.getInt(clicksIndex);
                                int impressions = impressionsIndex == -1 ? 0 : c.getInt(impressionsIndex);
                                if (!TextUtils.isEmpty(key4) && day != -1 && (entity = entities.get(key4)) != null) {
                                    entity.getSignalsBuffer(group2).set(Util.getDate(day), new Signals(clicks, impressions));
                                }
                            }
                            c.close();
                            if (DbHelper.DEBUG) {
                                Log.v(DbHelper.TAG, "Done retrieving entities");
                            }
                            return entities;
                        } finally {
                        }
                    } finally {
                    }
                } finally {
                }
            } finally {
            }
        }

        @Override // android.os.AsyncTask
        public void onPostExecute(HashMap<String, Entity> entities) {
            this.mListener.onEntitiesReady(entities);
        }
    }

    private class GetEntityKeysTask extends AsyncTask<Void, Void, ArrayList<String>> {
        private BlacklistListener mBlackListListener;
        private boolean mOnlyBlacklisted;

        public GetEntityKeysTask(BlacklistListener listener, boolean onlyBlacklisted) {
            this.mOnlyBlacklisted = false;
            this.mBlackListListener = listener;
            this.mOnlyBlacklisted = onlyBlacklisted;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> keys = new ArrayList<>();
            SQLiteDatabase db = DbHelper.this.getWritableDatabase();
            String key = this.mOnlyBlacklisted ? "key" : "key";
            String table = this.mOnlyBlacklisted ? "rec_blacklist" : "entity";
            String[] projection = {key};
            String orderBy = key + " ASC ";
            Cursor c = db.query(table, projection, this.mOnlyBlacklisted ? null : "has_recs = ? ", this.mOnlyBlacklisted ? null : new String[]{"1"}, null, null, orderBy);
            try {
                int keyIndex = c.getColumnIndexOrThrow(key);
                while (c.moveToNext()) {
                    String keyValue = c.getString(keyIndex);
                    if (!TextUtils.isEmpty(keyValue)) {
                        keys.add(keyValue);
                    }
                }
                c.close();
                if (DbHelper.DEBUG) {
                    Log.v(DbHelper.TAG, "Done retrieving entity package names");
                }
                return keys;
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }

        @Override // android.os.AsyncTask
        public void onPostExecute(ArrayList<String> keys) {
            if (this.mOnlyBlacklisted) {
                this.mBlackListListener.onEntityBlacklistReady(keys);
            } else {
                this.mBlackListListener.onEntityKeysReady(keys);
            }
        }
    }

    public long getMostRecentTimeStamp() {
        long jLongValue;
        synchronized (this.mLock) {
            jLongValue = this.mMostRecentTimeStamp.longValue();
        }
        return jLongValue;
    }
}
