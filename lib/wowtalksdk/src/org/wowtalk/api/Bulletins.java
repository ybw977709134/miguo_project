package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

import org.wowtalk.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Bulletin 
 */
public class Bulletins implements Parcelable{
	public int bulletin_id;
	public String class_id;
	public String moment_id;
	public String uid;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(bulletin_id);
        parcel.writeString(class_id);
        parcel.writeString(moment_id);
        parcel.writeString(uid);
        
	}
	
	public final static Creator<Bulletins> CREATOR = new Creator<Bulletins>() {
		
		@Override
		public Bulletins[] newArray(int size) {
			return new Bulletins[size];
		}
		
		@Override
		public Bulletins createFromParcel(Parcel parcel) {
			Bulletins  b = new Bulletins();
            b.bulletin_id = parcel.readInt();
            b.class_id = parcel.readString();
            b.moment_id = parcel.readString();
            b.uid = parcel.readString();    
			return null;
		}
	};

}