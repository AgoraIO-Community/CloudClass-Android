/**
 * Copyright 2021 json.cn
 */
package io.agora.agoraeducore.core.internal.edu.common.bean.board.sceneppt;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Auto-generated: 2021-02-16 21:7:4
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
public class Conversion implements Parcelable {

    private String type;

    protected Conversion(Parcel in) {
        type = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Conversion> CREATOR = new Creator<Conversion>() {
        @Override
        public Conversion createFromParcel(Parcel in) {
            return new Conversion(in);
        }

        @Override
        public Conversion[] newArray(int size) {
            return new Conversion[size];
        }
    };

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}