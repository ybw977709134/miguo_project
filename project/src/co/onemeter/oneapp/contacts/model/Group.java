package co.onemeter.oneapp.contacts.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Group implements Parcelable{
	
	private String groupId;
	
	private String groupName;
	
	private String groupPlace;
	
	private float groupDistance;
	
	public Group() {
		
	}
	
	public Group(String groupId, String groupName, String groupPlace, float groupDistance) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.groupPlace = groupPlace;
		this.groupDistance = groupDistance;
	}
	
	public void setGroupID(String id) {
		groupId = id;
	}
	
	public String getGroupID() {
		return this.groupId;
	}
	
	public void setName(String name) {
		if (name == null) {
			this.groupName = "";
		} else {
			this.groupName = name;
		}
	}
	
	public String getName() {
		if (groupName == null)
			return "";
		return groupName;
	}
	
	public void setGroupPlace(String place) {
		if (place == null) {
			groupPlace = "";
		} else {
			groupPlace = place;
		}
	}
	
	public String getGroupPlace() {
		if (groupPlace == null) {
			return "";
		}
		return groupPlace;
	}
	
	public void setGroupDistance(float distance) {
		groupDistance = distance;
	}
	
	public float getGroupDistance() {
		return groupDistance;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(groupName);
	}
	
	public static final Parcelable.Creator<Group> CREATOR = 
			new Parcelable.Creator<Group>() {

				@Override
				public Group createFromParcel(Parcel source) {
					Group g = new Group();
					g.groupName = source.readString();
					return g;
				}

				@Override
				public Group[] newArray(int size) {
					return new Group[size];
				}
			};

}
