package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一道家庭作业。
 * Created by zz on 05/14/14.
 */
public class HomeworkState implements Parcelable {
    public String student_uid;
    public String student_name;
    public int state;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(student_uid);
        parcel.writeString(student_name);
        parcel.writeInt(state);
    }

    public final static Creator<HomeworkState> CREATOR = new Creator<HomeworkState>() {
        @Override
        public HomeworkState createFromParcel(Parcel parcel) {
            HomeworkState h = new HomeworkState();
            h.student_uid = parcel.readString();
            h.student_name = parcel.readString();
            h.state = parcel.readInt();
            return h;
        }

        @Override
        public HomeworkState[] newArray(int i) {
            return new HomeworkState[i];
        }
    };
}
