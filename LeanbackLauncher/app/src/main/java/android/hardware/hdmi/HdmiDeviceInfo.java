package android.hardware.hdmi;

import android.os.Parcel;
import android.os.Parcelable;

public class HdmiDeviceInfo implements Parcelable {
    public static final Parcelable.Creator<HdmiDeviceInfo> CREATOR = new Parcelable.Creator<HdmiDeviceInfo>() {
        @Override
        public HdmiDeviceInfo createFromParcel(Parcel source) {
            return new HdmiDeviceInfo();
        }

        @Override
        public HdmiDeviceInfo[] newArray(int size) {
            return new HdmiDeviceInfo[size];
        }
    };

    public int getDeviceType() {
        return 0;
    }

    public String getDisplayName() {
        return null;
    }

    public boolean isCecDevice() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
