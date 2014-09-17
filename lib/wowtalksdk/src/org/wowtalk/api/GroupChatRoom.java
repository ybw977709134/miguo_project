package org.wowtalk.api;

import android.content.Context;
import android.graphics.PointF;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class GroupChatRoom extends TreeNode implements IHasPhoto{
	/*************************************************************************/
	/**
	 * A flag value indicating some fields are valid in certain context.
	 */
	public final static int FIELD_FLAG_NAME_ORIGINAL = 1;
	public final static int FIELD_FLAG_LOCATION = 2;
	public final static int FIELD_FLAG_STATUS = 4;
	
	/*************************************************************************/
	/**
	 *　Group ID : e.g. "28885bfa-8f64-4b22-9183-dcf965755ce9"
	 */
	public String groupID = ""; 
	/**
	 * Short group ID, e.g., "12030382910"
	 */
	public String shortGroupID = "";

    /**
     * Category name, eg, family, friends.
     */
    public String category = "";

	/**
	 *  Group name that has been set when created
	 */
	public String groupNameOriginal = ""; 
	
	/**
	 *  Group name that has been set in local
	 */
	public String groupNameLocal =""; 
	
	/**
	 * x = longitude;
	 * y = latitude;
	 */
	public PointF location;

    /**
     * Place name, or geo address.
     */
    public String place;

	/**
	 * Latest status of group
	 */
	public String groupStatus = ""; 

    public String description = "";

	/**
	 * Max number of group
	 */
	public int maxNumber = 0;  
	

	/**
	 * Member count
	 */
	public int memberCount = 0;
	
	/**
	 * True when you want to invite 3rd people in your chat
	 */
	public boolean isTemporaryGroup=false;

	/**
	 * if true, display as the name; otherwise, display it as the members and ","
	 */
	public boolean isGroupNameChanged;

	/**
	 * is me belong to the group chat room
	 */
	public boolean isMeBelongs;

	private long photoUploadedTimestamp = 0;

    /**
     * Custom my nickname in the scope of this group?
     */
    public String myNickHere = null;


	/**
	 * Member list
	 */
	public ArrayList<Buddy> memberList =null;

	/**
	 * child groupChatRooms
	 */
	public ArrayList<GroupChatRoom> childGroups = null;
	public String parentGroupId;
	public boolean isEditable;
	/**
	 * 排序的权重，同一level中比较
	 */
	public int weight;
	/**
	 * 组织架构中的层级
	 */
	public int level;
	/**
	 * 收藏的群组
	 */
	public boolean isFavorite;
	public int favoriteLevel;

    public boolean willBlockMsg = false;
    public boolean willBlockNotification = false;

    public GroupChatRoom(){
		
	}
	
	public GroupChatRoom(String groupID,String groupNameOriginal,String groupNameLocal,String groupStatus, int maxNumber, int memberCount,boolean isTemporaryGroup){
		this.groupID = groupID;
		this.groupNameOriginal = groupNameOriginal;
		this.groupNameLocal = groupNameLocal;
		this.groupStatus = groupStatus;
		this.maxNumber = maxNumber;
		this.memberCount = memberCount;
		this.isTemporaryGroup=isTemporaryGroup;
	}

    /**
     * 从editable 和 parentGroupId 可以看出这个组在公司架构里的位置
     * editable = false && parent_id=NULL的是公司的根节点
     * @return
     */
    public boolean isRootGroup() {
        return !isEditable && TextUtils.isEmpty(parentGroupId);
    }

	public void addMember(Buddy buddy){
		if(buddy==null) return;
		if(memberList==null){
			memberList = new ArrayList<Buddy>();
		}
		memberList.add(buddy);
	}

	@Override
	public String getGUID() {
		return groupID;
	}

	@Override
	public long getPhotoUploadedTimestamp() {
		return photoUploadedTimestamp;
	}

    @Override
    public boolean isHybrid() {
        return isTemporaryGroup;
    }

    /**
     * 必要时会执行网络操作，因此请勿在UI线程调用。
     * @param context
     * @return
     */
    @Override
    public ArrayList<IHasPhoto> getHybrid(Context context) {
        if (!isHybrid()) return null;

        Database db = Database.open(context);

        ArrayList<IHasPhoto> result = new ArrayList<IHasPhoto>();
        ArrayList<GroupMember> buddies = db.fetchGroupMembers(groupID);

        // 为空的情况不应该会发生
        if (null == buddies || buddies.isEmpty()) {
            Log.w("GroupChatRoom", "GroupChatRoom#getHybrid(), the buddies is null of group(" + groupID + ")");
//            Map<String, Object> resultMap = mWeb.fGroupChat_GetMembers(groupID);
//            if (ErrorCode.OK == (Integer)resultMap.get("code")) {
//                buddies = db.fetchGroupMembers(groupID);
//            }
        }

        if (null != buddies && !buddies.isEmpty()) {
            String myUid = PrefUtil.getInstance(context).getUid();
            for(GroupMember gm : buddies) {
                if (myUid.equals(gm.userID))
                    continue;
                result.add(gm);
            }
        }

        return result;
    }

    @Override
	public void setPhotoUploadedTimestamp(long value) {
		photoUploadedTimestamp = value;
	}
	
	/**
	 * Get display name.
	 * @return local name if exists, otherwise original name.
	 */
	public String getDisplayName() {
		if(groupNameLocal == null || groupNameLocal.equals(""))
			return groupNameOriginal;
		return groupNameLocal;
	}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel d, int flags) {
        d.writeString(category);
        d.writeString(groupID);
        d.writeString(groupNameLocal);
        d.writeString(groupNameOriginal);
        d.writeString(groupStatus);
        d.writeInt(isTemporaryGroup ? 1 : 0);
        d.writeInt(location == null ? 0 : 1);
        if(location != null) {
            d.writeFloat(location.x);
            d.writeFloat(location.y);
        }
        d.writeInt(maxNumber);
        d.writeInt(memberCount);
        d.writeTypedList(memberList);
        d.writeTypedList(childGroups);
        d.writeInt(isEditable ? 1 : 0);
        d.writeString(parentGroupId);
        d.writeString(myNickHere);
        d.writeLong(photoUploadedTimestamp);
        d.writeString(place);
        d.writeString(shortGroupID);
        d.writeInt(willBlockMsg ? 1 : 0);
        d.writeInt(willBlockNotification ? 1 : 0);
    }

    public static Creator<GroupChatRoom> CREATOR = new Creator<GroupChatRoom>() {
        @Override
        public GroupChatRoom createFromParcel(Parcel s) {
            GroupChatRoom g = new GroupChatRoom();
            g.category = s.readString();
            g.groupID = s.readString();
            g.groupNameLocal = s.readString();
            g.groupNameOriginal = s.readString();
            g.groupStatus = s.readString();
            g.isTemporaryGroup = 1 == s.readInt();
            if(1 == s.readInt()) {
                g.location = new PointF();
                g.location.x = s.readFloat();
                g.location.y = s.readFloat();
            }
            g.maxNumber = s.readInt();
            g.memberCount = s.readInt();
            g.memberList = new ArrayList<Buddy>();
            g.childGroups = new ArrayList<GroupChatRoom>();
            g.isEditable = 1 == s.readInt();
            g.parentGroupId = s.readString();
            s.readTypedList(g.memberList, Buddy.CREATOR);
            g.myNickHere = s.readString();
            g.photoUploadedTimestamp = s.readLong();
            g.place = s.readString();
            g.shortGroupID = s.readString();
            g.willBlockMsg = 1 == s.readInt();
            g.willBlockNotification = 1 == s.readInt();
            return g;
        }

        @Override
        public GroupChatRoom[] newArray(int size) {
            return new GroupChatRoom[size];
        }
    };

    /**
     * To be useful as cache key used by {@link ImageWorker}, the string value
     * of this object should be defined properly.
     */
    @Override
    public String toString() {
        return "Group " + groupID;
    }
}
