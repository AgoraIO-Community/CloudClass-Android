/**
  * Copyright 2021 json.cn 
  */
package io.agora.edu.core.internal.edu.common.bean.board.sceneppt;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Auto-generated: 2021-02-16 22:52:16
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
public class Ppt implements Parcelable {

    private String src;
    private double width;
    private double height;

    public Ppt(String src, double width, double height) {
        this.src = src;
        this.width = width;
        this.height = height;
    }

    protected Ppt(Parcel in) {
        src = in.readString();
        width = in.readDouble();
        height = in.readDouble();
    }

    public static final Creator<Ppt> CREATOR = new Creator<Ppt>() {
        @Override
        public Ppt createFromParcel(Parcel in) {
            return new Ppt(in);
        }

        @Override
        public Ppt[] newArray(int size) {
            return new Ppt[size];
        }
    };

    public void setSrc(String src) {
         this.src = src;
     }
     public String getSrc() {
         return src;
     }

    public void setWidth(double width) {
         this.width = width;
     }
     public double getWidth() {
         return width;
     }

    public void setHeight(double height) {
         this.height = height;
     }
     public double getHeight() {
         return height;
     }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(src);
        dest.writeDouble(width);
        dest.writeDouble(height);
    }
}