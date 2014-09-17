package org.wowtalk.api;

import android.os.Parcel;
import junit.framework.Assert;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-7
 * Time: PM1:13
 * To change this template use File | Settings | File Templates.
 */
public class GroupMember extends Buddy {
    // user level values
    public static final int LEVEL_CREATOR = 0;
    public static final int LEVEL_ADMIN = 1;
    public static final int LEVEL_DEFAULT = 9;
    public static final int LEVEL_PENDING = 10;
    /**
     * This value is for UI only, and does not exist in db.
     */
    public static final int LEVEL_DUMMY = 1008;
    /**
     * This value is for UI to show add item, add does not exist in db.
     */
    public static final int LEVEL_ADD = 1009;
    /**
     * This value is for UI to show delete item, and does not exist in db.
     */
    public static final int LEVEL_DELETE = 1010;

    /**
     * not set.
     */
    public static final int LEVEL_NULL = 1009;

    public String groupID;

    private int level;

    /** The message sent when requesting to join in. */
    public String message;

    public GroupMember() {
        initFields();
    }

    public GroupMember(String uid) {
        super(uid);
        initFields();
    }

    public GroupMember(String uid, String groupID) {
        super(uid);
        this.groupID = groupID;
    }

    private void initFields() {
        level = LEVEL_NULL;
    }

    /**
     * user's level in Group or Event.
     */
    public int getLevel() {
        Assert.assertTrue(level == LEVEL_DEFAULT
                || level == LEVEL_PENDING
                || level == LEVEL_ADMIN
                || level == LEVEL_CREATOR
                || level == LEVEL_DUMMY
                || level == LEVEL_ADD
                || level == LEVEL_DELETE
                || level == LEVEL_NULL);
        return level;
    }

    public void setLevel(int level) {
        if (level == LEVEL_CREATOR
                || level == LEVEL_DEFAULT
                || level == LEVEL_PENDING
                || level == LEVEL_ADMIN
                || level == LEVEL_ADD
                || level == LEVEL_DELETE
                || level == LEVEL_DUMMY) {
            this.level = level;
        } else {
            this.level = LEVEL_NULL;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(groupID);
        dest.writeInt(getLevel());
        dest.writeString(message);
    }

    public static void loadParcel(Parcel s, GroupMember gm) {
        gm.groupID = s.readString();
        gm.setLevel(s.readInt());
        gm.message = s.readString();
    }

    public static Creator<GroupMember> CREATOR = new Creator<GroupMember>() {

        @Override
        public GroupMember createFromParcel(Parcel parcel) {
            GroupMember gm = new GroupMember();
            Buddy.loadParcel(parcel, gm);
            GroupMember.loadParcel(parcel, gm);
            return gm;
        }

        @Override
        public GroupMember[] newArray(int i) {
            return new GroupMember[i];
        }
    };

    public String toString() {
        return groupID + "_" + userID + "_" + photoUploadedTimeStamp + ".";
    };
}
