/**
 * Copyright 2021 json.cn
 */
package io.agora.edu.core.internal.edu.common.bean.board.sceneppt;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Auto-generated: 2021-02-16 21:7:4
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
public class TaskProgress implements Parcelable {

    private int totalPageSize;
    private int convertedPageSize;
    private int convertedPercentage;
    private List<SceneInfo> convertedFileList;
    private String currentStep;

    protected TaskProgress(Parcel in) {
        totalPageSize = in.readInt();
        convertedPageSize = in.readInt();
        convertedPercentage = in.readInt();
        convertedFileList = in.createTypedArrayList(SceneInfo.CREATOR);
        currentStep = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(totalPageSize);
        dest.writeInt(convertedPageSize);
        dest.writeInt(convertedPercentage);
        dest.writeTypedList(convertedFileList);
        dest.writeString(currentStep);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TaskProgress> CREATOR = new Creator<TaskProgress>() {
        @Override
        public TaskProgress createFromParcel(Parcel in) {
            return new TaskProgress(in);
        }

        @Override
        public TaskProgress[] newArray(int size) {
            return new TaskProgress[size];
        }
    };

    public void setTotalPageSize(int totalPageSize) {
        this.totalPageSize = totalPageSize;
    }

    public int getTotalPageSize() {
        return totalPageSize;
    }

    public void setConvertedPageSize(int convertedPageSize) {
        this.convertedPageSize = convertedPageSize;
    }

    public int getConvertedPageSize() {
        return convertedPageSize;
    }

    public void setConvertedPercentage(int convertedPercentage) {
        this.convertedPercentage = convertedPercentage;
    }

    public int getConvertedPercentage() {
        return convertedPercentage;
    }

    public void setConvertedFileList(List<SceneInfo> convertedFileList) {
        this.convertedFileList = convertedFileList;
    }

    public List<SceneInfo> getConvertedFileList() {
        return convertedFileList;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getCurrentStep() {
        return currentStep;
    }

}