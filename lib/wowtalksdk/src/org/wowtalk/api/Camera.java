package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 摄像头
 * Created by zz on 05/03/15.
 */
public class Camera implements Parcelable{
	public int id;
	public String camera_name;
	public String mac;
	public String httpURL;
	public String order_id;
	public String im_id;
	public String school_id;
	public String create_timestamp;
	public int status;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(camera_name);
        parcel.writeString(mac);
        parcel.writeString(httpURL);
        parcel.writeString(order_id);
        parcel.writeString(im_id);
        parcel.writeString(school_id);
        parcel.writeString(create_timestamp);
        parcel.writeInt(status);
        
	}
	
	public final static Creator<Camera> CREATOR = new Creator<Camera>() {
		
		@Override
		public Camera[] newArray(int size) {
			return new Camera[size];
		}
		
		@Override
		public Camera createFromParcel(Parcel parcel) {
			Camera  c = new Camera();
            c.id = parcel.readInt();
            c.camera_name = parcel.readString();
            c.mac = parcel.readString();
            c.httpURL = parcel.readString();
            c.order_id = parcel.readString();
            c.im_id = parcel.readString();
            c.school_id = parcel.readString();
            c.create_timestamp = parcel.readString();
            c.status = parcel.readInt();
			return null;
		}
	};

}
