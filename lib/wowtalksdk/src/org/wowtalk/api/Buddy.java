package org.wowtalk.api;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;

public class Buddy implements IHasPhoto, Parcelable {

    // sex flag values
    public static final int SEX_NULL = 2;
    public static final int SEX_FEMALE = 1;
    public static final int SEX_MALE = 0;

    public static final int RELATIONSHIP_NONE = 0;
    public static final int RELATIONSHIP_PENDING_IN = 1;
    public static final int RELATIONSHIP_PENDING_OUT = 2;
    public static final int RELATIONSHIP_PENDING_IN_OUT
            = RELATIONSHIP_PENDING_IN | RELATIONSHIP_PENDING_OUT ;
    public static final int RELATIONSHIP_FRIEND_THERE = 4;
    public static final int RELATIONSHIP_FRIEND_HERE = 8;
    public static final int RELATIONSHIP_FRIEND_BOTH
            = RELATIONSHIP_FRIEND_THERE | RELATIONSHIP_FRIEND_HERE ;
    public static final int RELATIONSHIP_SELF = 16;
    public static final int RELATIONSHIP_CONTACT_THERE = 32;
    public static final int RELATIONSHIP_CONTACT_HERE = 64;
    public static final int RELATIONSHIP_CONTACT_BOTH
            = RELATIONSHIP_CONTACT_THERE | RELATIONSHIP_CONTACT_HERE ;
    public static final int RELATIONSHIP_UNSET = 1024;

    // user type

    /** User type: Public account. */
    public static final int ACCOUNT_TYPE_PUBLIC = 0;
    /**
     * 公司发通知的admin帐号（虚拟的buddy）
     */
    public static final int ACCOUNT_TYPE_NOTICE_MAN = 0;
    /** User type: Normal user. */
    public static final int ACCOUNT_TYPE_NORMAL = 1;
    //family user
    public static final int ACCOUNT_TYPE_FAMILY = 2;
    /** User type: Unknown(not initialized). */
    public static final int ACCOUNT_TYPE_NULL = 9;

    // A flag value indicating some fields are valid in certain context.

	public static final int FIELD_FLAG_NONE = 0;
	public static final int FIELD_FLAG_NICK = 1;
	public static final int FIELD_FLAG_STATUS = 2;
	public static final int FIELD_FLAG_BIRTHDAY = 4;
	public static final int FIELD_FLAG_SEX = 8;
	public static final int FIELD_FLAG_AREA = 16;
	/**
	 * Indicate last spot is valid: latitude, longitude, timestamp.
	 */
	public static final int FIELD_FLAG_SPOT = 32;
	/**
	 * Indicate photo and thumbnail are valid.
	 */
	public static final int FIELD_FLAG_PHOTO = 64;

	public static final int FIELD_FLAG_PRONUNCIATION = 128;
    public static final int FIELD_FLAG_JOB_TITLE = 256;
    /**
     * inter_phone
     */
    public static final int FIELD_FLAG_PHONE = 512;
    /**
     * mobile_phone
     */
    public static final int FIELD_FLAG_MOBILE = 1024;
    public static final int FIELD_FLAG_EMAIL = 2048;
    public static final int FIELD_FLAG_EMPLOYEE_ID = 4096;
    /*************************************************************************/

	/** User ID. This is a technical ID generated by server in registering. **/
	public String  userID ; 
	
	/** WowTalk id. This is user-selected ID, may be used as account name when login. **/
	public String wowtalkID;
	
	/** Hashed password **/
	public String hashedPassword;
	
	/** Plain password **/
	public String plainPassword;
	
    /**User Phone Number **/
	public String phoneNumber ; 
		
	/**User Nick Name **/
	public String nickName;

    /**display name remarked by me.*/
    public String alias;

	/**Latest Status **/
	public String status;
		
	/**Device Number **/
	public String deviceNumber;
	
	/**Client Application Version **/
	public String appVer;
	
	/**Server domain **/
	public String domain;
	
	/**last location
	 * x=longitude, y=latitude.
	 **/
	public WLocation lastLocation;
	
	/**last online time**/
	public java.util.Date lastOnline;
	
	/** 
	 * TimeStamp when this user is inserted to local db :
	 *    use to compare and list up the newly inserted friend
	 **/
	public long insertTimeStamp; 
	
	/**
     * Deprecated, use {@link #willBlockMsg} instead.
     *
	 * Flag to judge whether this user is blocked by me
     * @deprecated
	 */
	public boolean isBlocked;
	
	private int mFriendshipWithMe;
	
	private int sexFlag = SEX_NULL;
	
	/**
	 * hometown/支店(biz)
	 */
	public String area;
	
	private java.util.Date birthday;
	
	/** 
	 * TimeStamp when this user photo is update:
	 *    use to compare and fetch the newly update profile thumbnail or photo
	 **/
	public long photoUploadedTimeStamp; 

	 /**
	 * buddy has a new thumbnail photo
	 *    need to download it now
	 */
    public boolean needToDownloadThumbnail;
    
    /**
	 * buddy has a new profile photo
	 *    need to download it now
	 */
    public boolean needToDownloadPhoto;

	/**
	 * File path where the thumbnail photo is stored in local
	 */
	public String pathOfThumbNail;//ローカル
	/**
	 * File path where the profile photo is stored in local
	 */
	public String pathOfPhoto;  //ローカル
	
	/**
	 * Flag that help to indicate buddy 
	 *    that might have account deleted but we still have some information in local db
	 */
	public boolean mayNotExist;
	
	private int accountType;
	
	/**
	 * The interpreting of this property is context-dependent.
	 */
	public int tag;
    private String email;
    /**
     * Used for sorting and filtering.
     */
    public String sortKey;

    /**
     * Block new messages from this buddy?
     */
    public boolean willBlockMsg = false;

    /**
     * Block notification of new messages from this buddy?
     */
    public boolean willBlockNotification = false;
    /**
     * Is hidden from contacts list view?
     */
    public boolean hidden = false;
    /**
     * Starred, a.k.a, has been added to favorites
     */
    public boolean favorite = false;

    /**
     * 常用联系人
     */
    public boolean isFrequent = false;

    /**
     * 发音
     */
    public String pronunciation;

    /**
     * 部门名称
     */
    public String deptName;
    /**
     * 职位
     */
    public String jobTitle;

    /**
     * 手机
     */
    public String mobile;

    /**
     * 工号
     */
    public String employeeId;

    /**
	 * Constructor  
	 *   default value will be set
	 */
	public Buddy(){
        initFields();
    }

    public Buddy(String uid) {
        initFields();
        userID = uid;
    }

    @Override public boolean equals(Object o) {
        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(o instanceof Buddy)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        Buddy lhs = (Buddy) o;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        return userID.equals(lhs.userID);
    }

    private void initFields() {
        userID ="";
        phoneNumber = "";
        nickName = "";
        nickName = "";
        status = "";
        status ="";
        deviceNumber = "";
        deviceNumber ="";
        appVer = "";
        appVer = "";
        isBlocked = false;
        photoUploadedTimeStamp = -1;
        insertTimeStamp = -1;  // to be set after reading db
        pathOfPhoto = "";  // to be set after reading db
        pathOfThumbNail = ""; // to be set after reading db
        needToDownloadPhoto = true;  // to be set after comparing photoUploadedTimeStamp
        needToDownloadThumbnail = true;// to be set after comparing photoUploadedTimeStamp
        mayNotExist = false;
        sortKey = "";
        lastLocation = null;
        lastOnline = null;
        domain = "";
        tag = 0;
        sexFlag = SEX_NULL;
        accountType = ACCOUNT_TYPE_NULL;
        mFriendshipWithMe = RELATIONSHIP_UNSET;
    }

    /**
	 * Please call this when you have downloaded and stored the thumbnail 
	 * @param path
	 */
	public void setThumbnailPath(String path){
		this.pathOfThumbNail = path;
		this.needToDownloadThumbnail =false;
	}
	
	/**
	 * Please call this when you have downloaded and stored the photo 
	 * @param path
	 */
	public void setPhotoPath(String path){
		this.pathOfPhoto = path;
		this.needToDownloadPhoto =false;
	}

	@Override
	public String getGUID() {
		return userID;
	}

	@Override
	public long getPhotoUploadedTimestamp() {
		return photoUploadedTimeStamp;
	}

    @Override
    public boolean isHybrid() {
        return false;
    }

    @Override
    public ArrayList<IHasPhoto> getHybrid(Context context) {
        return null;
    }

    @Override
	public void setPhotoUploadedTimestamp(long value) {
		photoUploadedTimeStamp = value;
	}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appVer);
        dest.writeString(area);
        dest.writeLong(getBirthday() != null ? getBirthday().getTime() : 0);
        dest.writeString(deviceNumber);
        dest.writeString(domain);
        dest.writeString(getEmail());
        dest.writeString(hashedPassword);
        dest.writeLong(insertTimeStamp);
        dest.writeInt(isBlocked ? 1 : 0);
        dest.writeInt(hidden ? 1 : 0);
        dest.writeInt(mFriendshipWithMe);
        dest.writeInt(lastLocation == null ? 0 : 1);
        if(lastLocation != null) {
            dest.writeDouble(lastLocation.latitude);
            dest.writeDouble(lastLocation.longitude);
        }
        dest.writeLong(lastOnline == null ? 0 : lastOnline.getTime());
        dest.writeInt(mayNotExist ? 1 : 0);
        dest.writeInt(needToDownloadPhoto ? 1 : 0);
        dest.writeInt(needToDownloadThumbnail ? 1 : 0);
        dest.writeString(nickName);
        dest.writeString(pathOfPhoto);
        dest.writeString(pathOfThumbNail);
        dest.writeString(phoneNumber);
        dest.writeLong(photoUploadedTimeStamp);
        dest.writeString(plainPassword);
        dest.writeString(alias);
        dest.writeInt(getSexFlag());
        dest.writeInt(favorite ? 1 : 0);
        dest.writeString(status);
        dest.writeInt(tag);
        dest.writeInt(getAccountType());
        dest.writeString(userID);
        dest.writeInt(willBlockMsg ? 1 : 0);
        dest.writeInt(willBlockNotification ? 1 : 0);
        dest.writeString(wowtalkID);
    }

    public static void loadParcel(Parcel s, Buddy b) {
        b.appVer = s.readString();
        b.area = s.readString();
        Long dob = s.readLong();
        if(dob != 0)
            b.setBirthday(new Date(dob));
        b.deviceNumber = s.readString();
        b.domain = s.readString();
        b.setEmail(s.readString());
        b.hashedPassword = s.readString();
        b.insertTimeStamp = s.readLong();
        b.isBlocked = s.readInt() == 1;
        b.hidden = s.readInt() == 1;
        b.mFriendshipWithMe = s.readInt();
        if(s.readInt() == 1) {
            b.lastLocation = new WLocation();
            b.lastLocation.longitude = s.readDouble();
            b.lastLocation.latitude = s.readDouble();
        }
        Long tm = s.readLong();
        if(tm != null)
            b.lastOnline = new Date(tm);
        b.mayNotExist = 1 == s.readInt();
        b.needToDownloadPhoto = 1 == s.readInt();
        b.needToDownloadThumbnail = 1 == s.readInt();
        b.nickName = s.readString();
        b.pathOfPhoto = s.readString();
        b.pathOfThumbNail = s.readString();
        b.phoneNumber = s.readString();
        b.photoUploadedTimeStamp = s.readLong();
        b.plainPassword = s.readString();
        b.alias = s.readString();
        b.setSexFlag(s.readInt());
        b.favorite = s.readInt() == 1;
        b.status = s.readString();
        b.tag = s.readInt();
        b.setAccountType(s.readInt());
        b.userID = s.readString();
        b.willBlockMsg = s.readInt() == 1;
        b.willBlockNotification = s.readInt() == 1;
        b.wowtalkID = s.readString();
    }

    public static Creator<Buddy> CREATOR = new Creator<Buddy>() {
        @Override
        public Buddy createFromParcel(Parcel s) {
            Buddy b = new Buddy();
            loadParcel(s, b);
            return b;
        }

        @Override
        public Buddy[] newArray(int size) {
            return new Buddy[size];
        }
    };

    /**
     * Gender/sex.
     */
    public int getSexFlag() {
        return sexFlag;
    }

    public void setSexFlag(int sexFlag) {
        this.sexFlag = sexFlag;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    /**
     * User type.
     */
    public int getAccountType() {
//        Assert.assertTrue(accountType == ACCOUNT_TYPE_NORMAL
//                || accountType == ACCOUNT_TYPE_PUBLIC
//                || accountType == ACCOUNT_TYPE_NULL);
        return accountType;
    }

    public void setAccountType(int accountType) {
        if (accountType == ACCOUNT_TYPE_NORMAL
                || accountType == ACCOUNT_TYPE_PUBLIC
                || accountType == ACCOUNT_TYPE_FAMILY) {
            this.accountType = accountType;
        } else {
            this.accountType = ACCOUNT_TYPE_NULL;
        }
    }

    /**
     * Email address.
     */
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Buddy.FRIENDSHIP_* constants.
     */
    public int getFriendShipWithMe() {
        return mFriendshipWithMe;
    }

    /**
     * @param relationship Buddy.FRIENDSHIP_* constants.
     */
    public void setFriendshipWithMe(int relationship) {
        this.mFriendshipWithMe = relationship;
    }

    @Override
    public String toString() {
        String at;
        switch (accountType) {
            case ACCOUNT_TYPE_NULL:
                at = "ACCOUNT_TYPE_NULL";
                break;
            case ACCOUNT_TYPE_NORMAL:
                at = "ACCOUNT_TYPE_NORMAL";
                break;
            case ACCOUNT_TYPE_PUBLIC:
                at = "ACCOUNT_TYPE_PUBLIC";
                break;
            default:
                at = "?";
                break;
        }

        return String.format("%s %s, accountType=%s, friendship=%d",
                userID, nickName, at, mFriendshipWithMe);
    }
}
