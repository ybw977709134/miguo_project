package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一个学生在某节课上的某个方面的表现。
 * Created by pzy on 12/21/14.
 */
public class LessonPerformance implements Parcelable {
    public int lesson_id;
    /** 学生的 uid */
    public String student_id;
    /** 哪方面的表现？*/
    public int property_id;
    public int property_value;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(lesson_id);
        parcel.writeString(student_id);
        parcel.writeInt(property_id);
        parcel.writeInt(property_value);
    }

    public final static Creator<LessonPerformance> CREATOR = new Creator<LessonPerformance>() {
        @Override
        public LessonPerformance createFromParcel(Parcel parcel) {
            LessonPerformance l = new LessonPerformance();
            l.lesson_id = parcel.readInt();
            l.student_id = parcel.readString();
            l.property_id = parcel.readInt();
            l.property_value = parcel.readInt();
            return l;
        }

        @Override
        public LessonPerformance[] newArray(int i) {
            return new LessonPerformance[i];
        }
    };
}
