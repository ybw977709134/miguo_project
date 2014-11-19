package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * 活动
 */
public class WEvent implements Parcelable, IHasMultimedia, IHasReview {
    /** 多媒体附件的上传目录。*/
    public final static String MEDIA_FILE_REMOTE_DIR = "eventfile";

    public String id;
    public String owner_uid;
    /** 主办方 */
    public String host;
    public String title;
    public String description;
    public int event_type;
    public int joinedMemberCount=0;
    public int capacity = 0;
    public int possibleJoinedMemberCount=0;
    public String contactEmail;
    public String address;
    public float latitude = 0;
    public float longitude = 0;
    public String tag;
    /** category name, not category literal for human reading. */
    public String category;
    public String event_start_date;
    public String event_dead_line;
    public int membership;
    public String timeStamp;
    public String thumbNail;



    public Date startTime;
    public Date endTime;
    public Date createdTime;

    public int costGolds = 0;

    public ArrayList<WFile> multimedias = null;
    public ArrayList<Review> reviews = null;

    public final static int EVENT_TYPE_SIMPLE=0;
    public final static int EVENT_TYPE_WITH_DETAIL=1;

    public final static int MEMBER_SHIP_NOT_JOIN=0;
    public final static int MEMBER_SHIP_JOINED =1;
    
    public final static int PRIVACY_LEVEL_UNDEFINED = -1;
    /**
     * Everyone can see and join this.
     */
    public final static int PRIVACY_LEVEL_OPEN = 0;
    /**
     * Everyone can see this, approval required to join in.
     */
    public final static int PRIVACY_LEVEL_APPROVAL = 1;
    /**
     * Only those been required can see this.
     */
    public final static int PRIVACY_LEVEL_INVITE = 2;
    /**
     * Nobody can see this.
     */
    public final static int PRIVACY_LEVEL_PRIVATE = 3;
    
	public int privacy_level = PRIVACY_LEVEL_UNDEFINED;
	
	public final static int USERTYPE_ALL = 511;
	
	public int target_user_type = USERTYPE_ALL;
	
	public boolean allowReview = true;

    public boolean isOfficial = false;
    
    /**
     * need to submit a work.
     */
    public boolean needWork = false;

	/**
	 * member count.
	 */
	public int size;


    public WEvent() {
    	
    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(address);
		dest.writeInt(allowReview ? 1 : 0);
		dest.writeInt(capacity);
		dest.writeInt(costGolds);
		dest.writeLong(createdTime == null ? 0 : createdTime.getTime());
		dest.writeString(description);
		dest.writeString(id);
		dest.writeInt(isOfficial ? 1 : 0);
		dest.writeFloat(latitude);
		dest.writeFloat(longitude);
		dest.writeInt(membership);
		dest.writeTypedList(multimedias == null ? new ArrayList<WFile>() : multimedias);
		dest.writeInt(needWork ? 1 : 0);
		dest.writeString(owner_uid);
        dest.writeString(host);
		dest.writeInt(privacy_level);
		dest.writeTypedList(reviews == null ? new ArrayList<Review>() : reviews);
		dest.writeInt(size);
		dest.writeLong(startTime == null ? 0 : startTime.getTime());
        dest.writeLong(endTime == null ? 0 : endTime.getTime());
		dest.writeInt(target_user_type);
		dest.writeString(title);
        dest.writeString(contactEmail);
        dest.writeInt(joinedMemberCount);
        dest.writeInt(possibleJoinedMemberCount);
        dest.writeString(event_start_date);
        dest.writeString(event_dead_line);
        dest.writeString(timeStamp);
        dest.writeString(thumbNail);
        dest.writeInt(event_type);
        dest.writeString(tag);
        dest.writeString(category);
	}

	public static Parcelable.Creator<WEvent> CREATOR
	= new Parcelable.Creator<WEvent>() {

		@Override
		public WEvent createFromParcel(Parcel source) {
			WEvent a = new WEvent();
			a.address = source.readString();
			a.allowReview = source.readInt() == 1;
			a.capacity = source.readInt();
			a.costGolds = source.readInt();
			a.createdTime = new Date(source.readLong());
			a.description = source.readString();
			a.id = source.readString();
			a.isOfficial = source.readInt() == 1;
			a.latitude = source.readFloat();
			a.longitude = source.readFloat();
			a.membership = source.readInt();
			a.multimedias = new ArrayList<WFile>();
			source.readTypedList(a.multimedias, WFile.CREATOR);
			a.needWork = source.readInt() == 1;
			a.owner_uid = source.readString();
            a.host = source.readString();
			a.privacy_level = source.readInt();
			a.reviews = new ArrayList<Review>();
			source.readTypedList(a.reviews, Review.CREATOR);
			a.size = source.readInt();
			a.startTime = new Date(source.readLong());
            a.endTime = new Date(source.readLong());
			a.target_user_type = source.readInt();
			a.title = source.readString();
            a.contactEmail=source.readString();
            a.joinedMemberCount=source.readInt();
            a.possibleJoinedMemberCount=source.readInt();
            a.event_start_date=source.readString();
            a.event_dead_line=source.readString();
            a.timeStamp=source.readString();
            a.thumbNail=source.readString();
            a.event_type=source.readInt();
            a.tag=source.readString();
            a.category=source.readString();
			return a;
		}

		@Override
		public WEvent[] newArray(int size) {
			return new WEvent[size];
		}
	};

    @Override
    public String getMediaDataTableName() {
        return "event_media";
    }

    @Override
    public String getMediaDataTablePrimaryKeyName() {
        return "event_id";
    }

    @Override
    public String getMediaDataTablePrimaryKeyValue() {
        return id;
    }
    @Override
    public int getMediaCount() {
        return multimedias == null ? 0 : multimedias.size();
    }

    @Override
    public Iterator<WFile> getMediaIterator() {
        return multimedias == null ? null : multimedias.iterator();
    }

    @Override
    public void addMedia(WFile media) {
        if (multimedias == null)
            multimedias = new ArrayList<WFile>();
        multimedias.add(media);
    }

    @Override
    public void clearMedia() {
        if (multimedias != null && !multimedias.isEmpty())
            multimedias.clear();
    }

    @Override
    public String getReviewDataTableName() {
        return "event_review";
    }

    @Override
    public String getReviewDataTablePrimaryKeyName() {
        return "event_id";
    }

    @Override
    public String getReviewDataTablePrimaryKeyValue() {
        return id;
    }

    @Override
    public int getReviewsCount() {
        return reviews == null ? 0 : reviews.size();
    }

    @Override
    public Iterator<Review> getReviewIterator() {
        return reviews == null ? null : reviews.iterator();
    }

    @Override
    public void addReview(Review review) {
        if (reviews == null)
            reviews = new ArrayList<Review>();
        reviews.add(review);
    }

    @Override
    public void clearReviews() {
        if (reviews != null && !reviews.isEmpty())
            reviews.clear();
    }
}
