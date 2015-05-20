package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 布置作业对应的多媒体文件
 * Created by hutianfeng on 15-5-20.
 */
public class HomeWorkMultimedia implements Parcelable{

    public int multimedia_content_id;
    public int moment_id;//该多媒体关联的homeworkmoment
    public String multimedia_content_type;//多媒体类型
    public String multimedia_content_path;//多媒体文件路径
    public int duration;
    public String multimedia_thumbnail_path;//多媒体缩略路径


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(multimedia_content_id);
        dest.writeInt(moment_id);
        dest.writeString(multimedia_content_type);
        dest.writeString(multimedia_content_path);
        dest.writeInt(duration);
        dest.writeString(multimedia_thumbnail_path);
    }


    public final static Creator<HomeWorkMultimedia> CREATOR = new Creator<HomeWorkMultimedia>() {
        @Override
        public HomeWorkMultimedia createFromParcel(Parcel source) {
            HomeWorkMultimedia l =new HomeWorkMultimedia();
            l.multimedia_content_id = source.readInt();
            l.moment_id = source.readInt();
            l.multimedia_content_type = source.readString();
            l.multimedia_content_path = source.readString();
            l.duration = source.readInt();
            l.multimedia_thumbnail_path = source.readString();
            return l;
        }

        @Override
        public HomeWorkMultimedia[] newArray(int size) {
            return new HomeWorkMultimedia[size];
        }
    };
}
