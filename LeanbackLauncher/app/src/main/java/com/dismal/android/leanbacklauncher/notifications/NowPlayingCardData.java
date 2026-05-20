package com.dismal.android.leanbacklauncher.notifications;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes.dex */
public class NowPlayingCardData implements Parcelable {
    public static final Parcelable.Creator<NowPlayingCardData> CREATOR = new Parcelable.Creator<NowPlayingCardData>() { // from class: com.dismal.android.leanbacklauncher.notifications.NowPlayingCardData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NowPlayingCardData createFromParcel(Parcel in) {
            return new NowPlayingCardData(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NowPlayingCardData[] newArray(int size) {
            return new NowPlayingCardData[size];
        }
    };
    public String albumArtist;
    public String albumTitle;
    public String artist;
    public Bitmap artwork;
    public long duration;
    public int launchColor;
    public PendingIntent pIntent;
    public String playerPackage;
    public String title;
    public long trackNumber;
    public long year;

    public NowPlayingCardData() {
    }

    public NowPlayingCardData(Parcel in) {
        readFromParcel(in);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.title);
        out.writeString(this.artist);
        out.writeString(this.albumArtist);
        out.writeString(this.albumTitle);
        out.writeLong(this.year);
        out.writeLong(this.trackNumber);
        out.writeLong(this.duration);
        out.writeString(this.playerPackage);
        out.writeParcelable(this.artwork, 0);
    }

    public void readFromParcel(Parcel in) {
        this.title = in.readString();
        this.artist = in.readString();
        this.albumArtist = in.readString();
        this.albumTitle = in.readString();
        this.year = in.readLong();
        this.trackNumber = in.readLong();
        this.duration = in.readLong();
        this.playerPackage = in.readString();
        this.artwork = (Bitmap) in.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
