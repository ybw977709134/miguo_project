package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 学生完成作业的结果
 * Created by hanson on 15-5-20.
 */
public class HomeWorkResult implements Parcelable{


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public final static Creator<HomeWorkResult> CREATOR = new Creator<HomeWorkResult>() {
        @Override
        public HomeWorkResult createFromParcel(Parcel source) {
            return null;
        }

        @Override
        public HomeWorkResult[] newArray(int size) {
            return new HomeWorkResult[0];
        }
    };
}
