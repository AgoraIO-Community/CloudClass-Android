/**
 * Copyright 2021 json.cn
 */
package io.agora.edu.common.bean.board.sceneppt;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Auto-generated: 2021-02-16 21:7:4
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
public class BoardCoursewareRes implements Parcelable {
    private String resourceUuid;
    private String resourceName;
    private String ext;
    private int size;
    private String url;
    private long updateTime;
    private boolean convert;
    private Conversion conversion;
    private String taskUuid;
    private String taskToken;
    private TaskProgress taskProgress;

    public BoardCoursewareRes(String resourceUuid, String resourceName, String ext, int size, String url, long updateTime, boolean convert, Conversion conversion, String taskUuid, String taskToken, TaskProgress taskProgress) {
        this.resourceUuid = resourceUuid;
        this.resourceName = resourceName;
        this.ext = ext;
        this.size = size;
        this.url = url;
        this.updateTime = updateTime;
        this.convert = convert;
        this.conversion = conversion;
        this.taskUuid = taskUuid;
        this.taskToken = taskToken;
        this.taskProgress = taskProgress;
    }

    protected BoardCoursewareRes(Parcel in) {
        resourceUuid = in.readString();
        resourceName = in.readString();
        ext = in.readString();
        size = in.readInt();
        url = in.readString();
        updateTime = in.readLong();
        convert = in.readByte() != 0;
        conversion = in.readParcelable(Conversion.class.getClassLoader());
        taskUuid = in.readString();
        taskToken = in.readString();
        taskProgress = in.readParcelable(TaskProgress.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(resourceUuid);
        dest.writeString(resourceName);
        dest.writeString(ext);
        dest.writeInt(size);
        dest.writeString(url);
        dest.writeLong(updateTime);
        dest.writeByte((byte) (convert ? 1 : 0));
        dest.writeParcelable(conversion, flags);
        dest.writeString(taskUuid);
        dest.writeString(taskToken);
        dest.writeParcelable(taskProgress, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BoardCoursewareRes> CREATOR = new Creator<BoardCoursewareRes>() {
        @Override
        public BoardCoursewareRes createFromParcel(Parcel in) {
            return new BoardCoursewareRes(in);
        }

        @Override
        public BoardCoursewareRes[] newArray(int size) {
            return new BoardCoursewareRes[size];
        }
    };

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isConvert() {
        return convert;
    }

    public void setConvert(boolean convert) {
        this.convert = convert;
    }

    public Conversion getConversion() {
        return conversion;
    }

    public void setConversion(Conversion conversion) {
        this.conversion = conversion;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getTaskToken() {
        return taskToken;
    }

    public void setTaskToken(String taskToken) {
        this.taskToken = taskToken;
    }

    public TaskProgress getTaskProgress() {
        return taskProgress;
    }

    public void setTaskProgress(TaskProgress taskProgress) {
        this.taskProgress = taskProgress;
    }
}