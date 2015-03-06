package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 物理教室
 * Created by zz on 03/03/15.
 */
public class Classroom implements Parcelable{
	public int id;
	public String school_id;
	public String room_name;
	public String room_num;
	public int students;
	public int is_camera;
	public int camaras;
	public int is_multimedia;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(school_id);
        parcel.writeString(room_name);
        parcel.writeString(room_num);
        parcel.writeInt(students);
        parcel.writeInt(is_camera);
        parcel.writeInt(camaras);
        parcel.writeInt(is_multimedia);
        
	}
	
	public final static Creator<Classroom> CREATOR = new Creator<Classroom>() {
		
		@Override
		public Classroom[] newArray(int size) {
			return new Classroom[size];
		}
		
		@Override
		public Classroom createFromParcel(Parcel parcel) {
			Classroom  c = new Classroom();
            c.id = parcel.readInt();
            c.school_id = parcel.readString();
            c.room_name = parcel.readString();
            c.room_num = parcel.readString();
            c.students = parcel.readInt();
            c.is_camera = parcel.readInt();
            c.camaras = parcel.readInt();
            c.is_multimedia = parcel.readInt();
			return null;
		}
	};

}
