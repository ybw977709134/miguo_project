package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

public class Review implements Parcelable {
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_LIKE = 0;
	public static final int TYPE_TEXT = 1;
	
	public String id;
	public String uid;
    /**
     * Moment or WEvent Id
     */
    public String hostId;

	/**
	 * TYPE_* constants.
	 */
	public int type;
	public String text;
	public String nickname;
    public String replyToReviewId;
    public String replyToUid;
    public String replyToNickname;
    public long timestamp;
    /**
     * Has been read?
     */
    public boolean read = true;

    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(uid);
		dest.writeString(nickname);
		dest.writeInt(type);
		dest.writeString(text);
        dest.writeString(replyToUid);
        dest.writeString(replyToNickname);
        dest.writeString(replyToReviewId);
        dest.writeInt(read ? 1 : 0);
	}
	
	public static Parcelable.Creator<Review> CREATOR = 
			new Parcelable.Creator<Review>() {

				@Override
				public Review createFromParcel(Parcel source) {
					Review r = new Review();
					r.id = source.readString();
					r.uid = source.readString();
					r.nickname = source.readString();
					r.type = source.readInt();
					r.text = source.readString();
                    r.replyToUid = source.readString();
                    r.replyToNickname = source.readString();
                    r.replyToReviewId = source.readString();
                    r.read = 1 == source.readInt();
					return r;
				}

				@Override
				public Review[] newArray(int size) {
					return new Review[size];
				}
			};

}
