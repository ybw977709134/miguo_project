package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 学生完成作业的结果
 * Created by hanson on 15-5-20.
 */
public class HomeWorkResult implements Parcelable{

    public int id;
    public String student_id;
    public int homework_id;
    public int moment_id;
    public HomeWorkMoment stuMoment;
    public HomeWorkReview homeWorkReview;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(student_id);
        dest.writeInt(homework_id);
        dest.writeInt(moment_id);
        dest.writeParcelable(stuMoment,flags);
        dest.writeParcelable(homeWorkReview,flags);


    }

    public final static Creator<HomeWorkResult> CREATOR = new Creator<HomeWorkResult>() {
        @Override
        public HomeWorkResult createFromParcel(Parcel source) {
            HomeWorkResult l = new HomeWorkResult();
            l.id = source.readInt();
            l.student_id = source.readString();
            l.homework_id = source.readInt();
            l.moment_id = source.readInt();
            l.stuMoment = source.readParcelable(HomeWorkMoment.class.getClassLoader());
            l.homeWorkReview = source.readParcelable(HomeWorkReview.class.getClassLoader());
            return l;
        }

        @Override
        public HomeWorkResult[] newArray(int size) {
            return new HomeWorkResult[size];
        }
    };
}
