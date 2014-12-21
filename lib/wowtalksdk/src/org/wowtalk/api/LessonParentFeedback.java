package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 家长对某节课的意见。
 * Created by pzy on 12/21/14.
 */
public class LessonParentFeedback implements Parcelable {
    public int lesson_id;
    /** 学生的 uid */
    public String student_id;
    /** 内容详情保存在动态表中。 */
    public int moment_id;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(lesson_id);
        parcel.writeString(student_id);
        parcel.writeInt(moment_id);
    }

    public final static Creator<LessonParentFeedback> CREATOR = new Creator<LessonParentFeedback>() {
        @Override
        public LessonParentFeedback createFromParcel(Parcel parcel) {
            LessonParentFeedback l = new LessonParentFeedback();
            l.lesson_id = parcel.readInt();
            l.student_id = parcel.readString();
            l.moment_id = parcel.readInt();
            return l;
        }

        @Override
        public LessonParentFeedback[] newArray(int i) {
            return new LessonParentFeedback[i];
        }
    };
}
