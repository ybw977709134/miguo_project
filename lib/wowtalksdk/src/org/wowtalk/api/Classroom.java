package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 物理教室
 * Created by zz on 03/03/15.
 */
public class Classroom implements Parcelable{
	public int lesson_id;
	public int room_id;
	public String room_name;
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(lesson_id);
        parcel.writeInt(room_id);
        parcel.writeString(room_name);
	}
	
	public final static Creator<Classroom> CREATOR = new Creator<Classroom>() {
		
		@Override
		public Classroom[] newArray(int size) {
			return new Classroom[size];
		}
		
		@Override
		public Classroom createFromParcel(Parcel parcel) {
			Classroom  c = new Classroom();
            c.lesson_id = parcel.readInt();
            c.room_id = parcel.readInt();
            c.room_name = parcel.readString();
			return null;
		}
	};

}
