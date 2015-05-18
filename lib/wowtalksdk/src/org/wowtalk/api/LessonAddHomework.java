package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 布置作业
 * Created by zz on 05/18/15.
 */
public class LessonAddHomework implements Parcelable {
    public int lesson_id;
    /** 作业id */
    public int homework_id;
    /** 内容详情保存在动态表中。 */
    public int moment_id;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(lesson_id);
        parcel.writeInt(homework_id);
        parcel.writeInt(moment_id);
    }

    public final static Creator<LessonAddHomework> CREATOR = new Creator<LessonAddHomework>() {
        @Override
        public LessonAddHomework createFromParcel(Parcel parcel) {
            LessonAddHomework l = new LessonAddHomework();
            l.lesson_id = parcel.readInt();
            l.homework_id = parcel.readInt();
            l.moment_id = parcel.readInt();
            return l;
        }

        @Override
        public LessonAddHomework[] newArray(int i) {
            return new LessonAddHomework[i];
        }
    };
}
