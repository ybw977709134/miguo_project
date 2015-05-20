package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 老师针对学生的答题情况做的评价回复
 * Created by hanson on 15-5-20.
 */
public class HomeWorkReview implements Parcelable{

    public int id;
    public int homeworkresult_id;
    public int rank1;
    public int rank2;
    public int rank3;
    public String text;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(homeworkresult_id);
        dest.writeInt(rank1);
        dest.writeInt(rank2);
        dest.writeInt(rank3);
        dest.writeString(text);

    }

    public final static Creator<HomeWorkReview> CREATOR = new Creator<HomeWorkReview>() {
        @Override
        public HomeWorkReview createFromParcel(Parcel source) {
            HomeWorkReview l = new HomeWorkReview();
            l.id =  source.readInt();
            l.homeworkresult_id =  source.readInt();
            l.rank1 =  source.readInt();
            l.rank2 =  source.readInt();
            l.rank3 =  source.readInt();
            l.text =  source.readString();
            return l;
        }

        @Override
        public HomeWorkReview[] newArray(int size) {
            return new HomeWorkReview[0];
        }
    };
}
