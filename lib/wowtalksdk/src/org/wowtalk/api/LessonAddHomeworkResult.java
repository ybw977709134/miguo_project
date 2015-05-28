package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 布置作业
 * Created by zz on 05/28/15.
 */
public class LessonAddHomeworkResult implements Parcelable {
    public int homeworkResult_id;
    /** 作业id */
    public int homework_id;
    /** 内容详情保存在动态表中。 */
    public int moment_id;
    public String student_id;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(homeworkResult_id);
        parcel.writeInt(homework_id);
        parcel.writeInt(moment_id);
        parcel.writeString(student_id);
    }

    public final static Creator<LessonAddHomeworkResult> CREATOR = new Creator<LessonAddHomeworkResult>() {
        @Override
        public LessonAddHomeworkResult createFromParcel(Parcel parcel) {
            LessonAddHomeworkResult l = new LessonAddHomeworkResult();
            l.homeworkResult_id = parcel.readInt();
            l.homework_id = parcel.readInt();
            l.moment_id = parcel.readInt();
            l.student_id = parcel.readString();
            return l;
        }

        @Override
        public LessonAddHomeworkResult[] newArray(int i) {
            return new LessonAddHomeworkResult[i];
        }
    };
}
