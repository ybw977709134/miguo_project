package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一节课。
 * Created by pzy on 12/21/14.
 */
public class Lesson implements Parcelable {
    public int lesson_id;
    /** 所在的课堂的 {@link org.wowtalk.api.GroupChatRoom#groupID}. */
    public String class_id;
    public String title;
    /** unix timestamp in sec */
    public long start_date;
    /** unix timestamp in sec */
    public long end_date;
    public int live;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(lesson_id);
        parcel.writeString(class_id);
        parcel.writeString(title);
        parcel.writeLong(start_date);
        parcel.writeLong(end_date);
        parcel.writeInt(live);
    }

    public final static Creator<Lesson> CREATOR = new Creator<Lesson>() {
        @Override
        public Lesson createFromParcel(Parcel parcel) {
            Lesson l = new Lesson();
            l.lesson_id = parcel.readInt();
            l.class_id = parcel.readString();
            l.title = parcel.readString();
            l.start_date = parcel.readLong();
            l.end_date = parcel.readLong();
            l.live = parcel.readInt();
            return l;
        }

        @Override
        public Lesson[] newArray(int i) {
            return new Lesson[i];
        }
    };
}
