package io.agora.online.impl.whiteboard.bean;

import android.os.Parcel;
import android.os.Parcelable;

public enum AgoraBoardFitMode implements Parcelable {
    Auto,
    Retain;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public static final Creator<AgoraBoardFitMode> CREATOR = new Creator<AgoraBoardFitMode>() {
        @Override
        public AgoraBoardFitMode createFromParcel(Parcel in) {
            return AgoraBoardFitMode.values()[in.readInt()];
        }

        @Override
        public AgoraBoardFitMode[] newArray(int size) {
            return new AgoraBoardFitMode[size];
        }
    };
}