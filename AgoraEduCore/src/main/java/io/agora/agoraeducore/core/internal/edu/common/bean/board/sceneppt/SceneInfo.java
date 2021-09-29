/**
 * Copyright 2021 json.cn
 */
package io.agora.agoraeducore.core.internal.edu.common.bean.board.sceneppt;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Auto-generated: 2021-02-16 22:52:16
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
public class SceneInfo implements Parcelable {

    private int componentCount;
    private Ppt ppt;
    private String name;

    public SceneInfo(int componentCount, Ppt ppt, String name) {
        this.componentCount = componentCount;
        this.ppt = ppt;
        this.name = name;
    }

    protected SceneInfo(Parcel in) {
        componentCount = in.readInt();
        ppt = in.readParcelable(Ppt.class.getClassLoader());
        name = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(componentCount);
        dest.writeParcelable(ppt, flags);
        dest.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SceneInfo> CREATOR = new Creator<SceneInfo>() {
        @Override
        public SceneInfo createFromParcel(Parcel in) {
            return new SceneInfo(in);
        }

        @Override
        public SceneInfo[] newArray(int size) {
            return new SceneInfo[size];
        }
    };

    public void setComponentCount(int componentCount) {
        this.componentCount = componentCount;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public void setPpt(Ppt ppt) {
        this.ppt = ppt;
    }

    public Ppt getPpt() {
        return ppt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}