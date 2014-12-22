package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一道家庭作业。
 * Created by pzy on 12/21/14.
 */
public class LessonHomework implements Parcelable {
    public int lesson_id;
    public int homework_id;
    public String title;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(lesson_id);
        parcel.writeInt(homework_id);
        parcel.writeString(title);
    }

    public final static Creator<LessonHomework> CREATOR = new Creator<LessonHomework>() {
        @Override
        public LessonHomework createFromParcel(Parcel parcel) {
            LessonHomework l = new LessonHomework();
            l.lesson_id = parcel.readInt();
            l.homework_id = parcel.readInt();
            l.title = parcel.readString();
            return l;
        }

        @Override
        public LessonHomework[] newArray(int i) {
            return new LessonHomework[i];
        }
    };
}
