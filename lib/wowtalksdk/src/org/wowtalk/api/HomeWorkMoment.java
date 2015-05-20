package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * homework中对应的moment
 * Created by hutianfeng on 15-5-20.
 */
public class HomeWorkMoment implements Parcelable{

    public int moment_id;
    public String owner_id;
    public long insert_timestamp;
    public int insert_latitude;
    public int insert_longitude;
    public String text_content;
    public int privacy_level;
    public int allow_review;
    public int tag;
    public int deadline;
    public int delete;
    public List<HomeWorkMultimedia> homeWorkMultimedias = null;





    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(moment_id);
        dest.writeString(owner_id);
        dest.writeLong(insert_timestamp);

        dest.writeInt(insert_latitude);
        dest.writeInt(insert_longitude);
        dest.writeString(text_content);
        dest.writeInt(privacy_level);
        dest.writeInt(allow_review);
        dest.writeInt(tag);
        dest.writeInt(deadline);
        dest.writeInt(delete);
        dest.writeTypedList(homeWorkMultimedias);

    }

    public final static Creator<HomeWorkMoment> CREATOR = new Creator<HomeWorkMoment>() {
        @Override
        public HomeWorkMoment createFromParcel(Parcel source) {
            HomeWorkMoment l = new HomeWorkMoment();
            l.moment_id = source.readInt();
            l.owner_id = source.readString();
            l.insert_timestamp = source.readLong();
            l.insert_latitude = source.readInt();
            l.insert_longitude = source.readInt();
            l.text_content = source.readString();
            l.privacy_level = source.readInt();
            l.allow_review = source.readInt();
            l.tag = source.readInt();
            l.deadline = source.readInt();
            l.delete = source.readInt();
            l.homeWorkMultimedias = new ArrayList<HomeWorkMultimedia>();
            source.readTypedList(l.homeWorkMultimedias,HomeWorkMultimedia.CREATOR);
            return l;
        }

        @Override
        public HomeWorkMoment[] newArray(int size) {
            return new HomeWorkMoment[size];
        }
    };
}
