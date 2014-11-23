package co.onemeter.oneapp.contacts.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.IHasPhoto;
import org.wowtalk.api.Utils;
import co.onemeter.oneapp.ui.ContactInfoActivity;
import co.onemeter.oneapp.ui.StartActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class Person implements IHasPhoto, Parcelable{
	public static final int SEX_MAIL = 0;

	public static final int SEX_FEMAIL = 1;

	public static final int SEX_UNKNOWN = 2;

	public HashMap<String, String> map = new HashMap<String, String>();
	
	private String ID;
	
	private String sortKey;
	
	private String name;
	
	private String noteName;
	
	private int sexFlag = 0;

    private int accountType = Buddy.ACCOUNT_TYPE_NULL;
	
	private long localPersonPhotoID;
	
	private long localContactID;
	
	private String birthday;
	
	private String phoneticName;
	
	private String wowtalkId;
	
	private String personState;
	
	private String globalPhonenumber;
	
	private String emailAddress;
	
	private String rigion;
	
	private ArrayList<String[]> Phones;
	
	private boolean selected = false;

    private boolean showInContact = true;

    private boolean autoAcceptMsg = false;
	
	public long photoUploadedTimestamp = 0;

    public int buddyType= ContactInfoActivity.BUDDY_TYPE_UNKNOWN;

	public static Person fromBuddy(Buddy buddy) {
		Person person = new Person();
        fetchPersonInfoFromBuddy(person,buddy);
		return person;
	}

    private static void fetchPersonInfoFromBuddy(Person person,Buddy buddy) {
        person.ID = buddy.userID;
        if (!TextUtils.isEmpty(buddy.nickName)) {
//            person.sortKey = buddy.nickName.substring(0, 1);
            person.name = buddy.nickName;
        } else {
            person.name = buddy.wowtalkID;
        }
//        person.sortKey=person.name;
        if (TextUtils.isEmpty(buddy.sortKey)) {
            person.sortKey = Utils.makeSortKey(StartActivity.instance(), person.name);
        } else {
            person.sortKey = buddy.sortKey;
        }
        if (buddy.getSexFlag() == Buddy.SEX_MALE) {
            person.sexFlag = SEX_MAIL;
        } else if (buddy.getSexFlag() == Buddy.SEX_FEMALE) {
            person.sexFlag = SEX_FEMAIL;
        } else {
            person.sexFlag = SEX_UNKNOWN;
        }
		person.accountType = buddy.getAccountType();

        person.wowtalkId = buddy.wowtalkID;
        person.rigion = buddy.area;
        person.emailAddress = buddy.getEmail();
        person.personState = buddy.status;
        person.globalPhonenumber = buddy.phoneNumber;
        person.selected = false;
        person.photoUploadedTimestamp = buddy.photoUploadedTimeStamp;
    }

    public void setWithBuddy(Buddy buddy) {
        fetchPersonInfoFromBuddy(this,buddy);
    }

    public Buddy toBuddy() {
        Buddy b = new Buddy();
        b.userID = ID;
        b.wowtalkID = wowtalkId;
        b.nickName = name;
        b.setSexFlag(sexFlag);
        b.status = personState;
        b.area = rigion;
        b.setEmail(emailAddress);
        b.phoneNumber = globalPhonenumber;
        b.photoUploadedTimeStamp = photoUploadedTimestamp;
		b.setAccountType(accountType);
        return b;
    }

    public void fillBuddy(Buddy b) {
        b.userID = ID;
        b.wowtalkID = wowtalkId;
        b.nickName = name;
        b.setSexFlag(sexFlag);
        b.status = personState;
        b.area = rigion;
        b.setEmail(emailAddress);
        b.phoneNumber = globalPhonenumber;
        b.photoUploadedTimeStamp = photoUploadedTimestamp;
    }

	public String getID() {
        if (ID == null)
            return "";
		return ID;
	}
	
	public void setID(String personID) {
		ID = personID;
	}
	
    public String getSortKey() {
//        return PinyinHelper.instance().getPinyin(StartActivity.instance(), sortKey, false);
        return sortKey;
    }

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public String getName() {
		if (name == null)
			return "";
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getWowTalkId() {
		return wowtalkId;
	}
	
	public void setWowTalkId(String wowtalkId) {
		this.wowtalkId = wowtalkId;
	}
	
	public void setSexFlag(int sex) {
		this.sexFlag = sex;
	}
	
	public int getSexFlag() {
		return this.sexFlag;
	}

    public void setAccountType(int type) {
        this.accountType = type;
    }

    public int getAccountType() {
        return this.accountType;
    }
	
	public void setLocalPersonPhotoID(long photoId) {
		this.localPersonPhotoID = photoId;
	}
	
	public long getLocalPersonPhotoID() {
		return localPersonPhotoID;
	}
	
	public void setLocalContactID(long contactId) {
		this.localContactID = contactId;
	}
	
	public long getLocalContactID() {
		return localContactID;
	}

	public String getPhoneticName() {
		return phoneticName;
	}

	public void setPhoneticName(String phoneticName) {
		this.phoneticName = phoneticName;
	}
	
	public String getPersonState() {
		if (personState == null)
			return "";
		return personState;
	}
	
	public void setPersonState(String personState) {
		if (personState == null) {
			this.personState = "";
		} else {
			this.personState = personState;
		}
	}
	
	public String getGlobalPhoneNumber() {
		if (globalPhonenumber == null) {
			return "";
		}
		return globalPhonenumber;
	}
	
	public void setGlobalPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			globalPhonenumber = "";
		} else {
			globalPhonenumber = phoneNumber;
		}
	}
	
	public String getEmailAddress() {
		if (emailAddress == null) {
			return "";
		}
		return emailAddress;
	}
	
	public void setEmailAddress(String email) {
		if (email == null) {
			emailAddress = "";
		} else {
			emailAddress = email;
		}
	}
	
	public ArrayList<String[]> getPhones() {
        if (Phones == null)
            Phones = new ArrayList<String[]>();
		return Phones;
	}
	
	public void setPhones(ArrayList<String[]> phones) {
        if (phones == null) {
            Phones = new ArrayList<String[]>();
        } else {
            Phones = phones;
        }
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean isSelected) {
		selected = isSelected;
	}
	
	public void setRigion(String rigion) {
		this.rigion = rigion;
	}
	
	public String getRigion() {
		if (rigion == null) {
			rigion = "";
		}
		return rigion;
	}

    public void setShowInContact(boolean showInContact) {
        this.showInContact = showInContact;
    }

    public boolean getShowInContact() {
        return this.showInContact;
    }

    public void setAutoAcceptMsg(boolean autoAcceptMsg) {
        this.autoAcceptMsg = autoAcceptMsg;
    }

    public boolean getAutoAcceptMsg() {
        return this.autoAcceptMsg;
    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// dest.writeMap(map);
        dest.writeBooleanArray(new boolean[]{autoAcceptMsg, showInContact});
		dest.writeInt(sexFlag);
        dest.writeInt(accountType);
		dest.writeLong(localPersonPhotoID);
        dest.writeLong(photoUploadedTimestamp);
		dest.writeLong(localContactID);
		dest.writeString(ID);
		dest.writeString(sortKey);
		dest.writeString(name);
		dest.writeString(wowtalkId);
		dest.writeString(phoneticName);
		dest.writeString(personState);
		dest.writeString(globalPhonenumber);
		dest.writeString(emailAddress);
		dest.writeString(rigion);
	}

    @Override
    public boolean equals(Object o) {
        // 根据id判断两个Person对象是否相等，在联系人列表界面使用来比较某个id对应的person是否存在list中
        if (o != null && o instanceof Person) {
            Person person = (Person) o;
            if (null == this.ID) {
                return null == person.ID;
            } else {
                return this.ID.equals(person.ID);
            }
        }
        return super.equals(o);
    }

    /**
     * Return a lower-case string for filtering.
     * @return
     */
    public String toStringForFiltering() {
        return (name + " " + wowtalkId + " " + globalPhonenumber).toLowerCase();
    }
	
	public static final Parcelable.Creator<Person> CREATOR = 
			new Parcelable.Creator<Person>() {

				@Override
				public Person createFromParcel(Parcel source) {
					Person p = new Person();
					// p.map = source.readHashMap(HashMap.class.getClassLoader());
                    boolean[] b = new boolean[2];
                    source.readBooleanArray(b);
                    p.autoAcceptMsg = b[0];
                    p.showInContact = b[1];
					p.sexFlag = source.readInt();
                    p.accountType = source.readInt();
					p.localPersonPhotoID = source.readLong();
                    p.photoUploadedTimestamp = source.readLong();
					p.localContactID = source.readLong();
					p.ID = source.readString();
					p.sortKey = source.readString();
					p.name = source.readString();
					p.wowtalkId = source.readString();
					p.phoneticName = source.readString();
					p.personState = source.readString();
					p.globalPhonenumber = source.readString();
					p.emailAddress = source.readString();
					p.rigion = source.readString();
					return p;
				}

				@Override
				public Person[] newArray(int size) {
					return new Person[size];
				}
			};

    @Override
    public String getGUID() {
        return ID;
    }

    @Override
    public void setPhotoUploadedTimestamp(long value) {
        photoUploadedTimestamp = value;
    }

    @Override
    public long getPhotoUploadedTimestamp() {
        return photoUploadedTimestamp;
    }

    @Override
    public boolean isHybrid() {
        return false;
    }

    @Override
    public ArrayList<IHasPhoto> getHybrid(Context context) {
        return null;
    }

}
