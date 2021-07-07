package io.agora.record;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class ReplayRes {

    public int count;
    public int nextId;
    public int total;
    public List<RecordDetail> list;

    @IntDef({Status.RECORDING, Status.FINISHED, Status.DOWNLOADING, Status.CONVERTING, Status.UPLOADING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
        int RECORDING = 1;
        int FINISHED = 2;
        int DOWNLOADING = 3;
        int CONVERTING = 4;
        int UPLOADING = 5;
    }

    public static class RecordDetail implements Parcelable, Cloneable {
        public String roomUuid;
        public String recordId;
        /**
         * 录制用户id
         */
        public String recordUuid;
        public String boardId;
        public String boardToken;
        @Status
        public int status;
        public long startTime;
        public long endTime;
        public String url;

        public boolean isFinished() {
            return status == Status.FINISHED;
        }

        @NonNull
        @Override
        protected RecordDetail clone() throws CloneNotSupportedException {
            return (RecordDetail) super.clone();
        }

        public static Creator<RecordDetail> getCREATOR() {
            return CREATOR;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.roomUuid);
            dest.writeString(this.recordId);
            dest.writeString(this.recordUuid);
            dest.writeString(this.boardId);
            dest.writeString(this.boardToken);
            dest.writeInt(this.status);
            dest.writeLong(this.startTime);
            dest.writeLong(this.endTime);
            dest.writeString(this.url);
        }

        protected RecordDetail(Parcel in) {
            this.roomUuid = in.readString();
            this.recordId = in.readString();
            this.recordUuid = in.readString();
            this.boardId = in.readString();
            this.boardToken = in.readString();
            this.status = in.readInt();
            this.startTime = in.readLong();
            this.endTime = in.readLong();
            this.url = in.readString();
        }

        public static final Creator<RecordDetail> CREATOR = new Creator<RecordDetail>() {
            @Override
            public RecordDetail createFromParcel(Parcel source) {
                return new RecordDetail(source);
            }

            @Override
            public RecordDetail[] newArray(int size) {
                return new RecordDetail[size];
            }
        };
    }

}
