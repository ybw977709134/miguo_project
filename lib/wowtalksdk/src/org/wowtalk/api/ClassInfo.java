package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 摄像头
 * Created by zz on 03/23/15.
 */
public class ClassInfo implements Parcelable{
	public long start_day;
	public long end_day;
	public long start_time;
	public long end_time;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(start_day);
        parcel.writeLong(end_day);
        parcel.writeLong(start_time);
        parcel.writeLong(end_time);      
	}
	
	public final static Creator<ClassInfo> CREATOR = new Creator<ClassInfo>() {
		
		@Override
		public ClassInfo[] newArray(int size) {
			return new ClassInfo[size];
		}
		
		@Override
		public ClassInfo createFromParcel(Parcel parcel) {
			ClassInfo  c = new ClassInfo();
            c.start_day = parcel.readLong();
            c.end_day = parcel.readLong();
            c.start_time = parcel.readLong();
            c.end_time = parcel.readLong();
			return null;
		}
	};

}
