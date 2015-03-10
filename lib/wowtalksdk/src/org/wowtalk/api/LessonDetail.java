package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一节课的所有信息。
 * Created by zz on 03/10/15.
 */
public class LessonDetail implements Parcelable {
    public int lesson_id;
    /** 所在的课堂的 {@link org.wowtalk.api.GroupChatRoom#groupID}. */
    public String class_id;
    public String title;
    /** unix timestamp in sec */
    public long start_date;
    /** unix timestamp in sec */
    public long end_date;
    public int live;
    
    public int room_id;
	public String school_id;
	public String room_name;
	public String room_num;
	public int students;
	public int is_camera;
	public int camaras;
	public int is_multimedia;
	
	public int camera_id;
	public String camera_name;
	public String mac;
	public String httpURL;
	public String order_id;
	public String im_id;
	public String create_timestamp;
	public int status;

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
        
        parcel.writeInt(room_id);
        parcel.writeString(school_id);
        parcel.writeString(room_name);
        parcel.writeString(room_num);
        parcel.writeInt(students);
        parcel.writeInt(is_camera);
        parcel.writeInt(camaras);
        parcel.writeInt(is_multimedia);
        
        parcel.writeInt(camera_id);
        parcel.writeString(camera_name);
        parcel.writeString(mac);
        parcel.writeString(httpURL);
        parcel.writeString(order_id);
        parcel.writeString(im_id);
        parcel.writeString(create_timestamp);
        parcel.writeInt(status);
        
        
    }

    public final static Creator<LessonDetail> CREATOR = new Creator<LessonDetail>() {
        @Override
        public LessonDetail createFromParcel(Parcel parcel) {
            LessonDetail l = new LessonDetail();
            l.lesson_id = parcel.readInt();
            l.class_id = parcel.readString();
            l.title = parcel.readString();
            l.start_date = parcel.readLong();
            l.end_date = parcel.readLong();
            
            l.room_id = parcel.readInt();
            l.school_id = parcel.readString();
            l.room_name = parcel.readString();
            l.room_num = parcel.readString();
            l.students = parcel.readInt();
            l.is_camera = parcel.readInt();
            l.camaras = parcel.readInt();
            l.is_multimedia = parcel.readInt();
            
            l.camera_id = parcel.readInt();
            l.camera_name = parcel.readString();
            l.mac = parcel.readString();
            l.httpURL = parcel.readString();
            l.order_id = parcel.readString();
            l.im_id = parcel.readString();
            l.create_timestamp = parcel.readString();
            l.status = parcel.readInt();
            return l;
        }

        @Override
        public LessonDetail[] newArray(int i) {
            return new LessonDetail[i];
        }
    };
}
