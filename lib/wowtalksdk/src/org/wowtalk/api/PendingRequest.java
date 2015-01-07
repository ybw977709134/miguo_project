package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Pending request types.
 */
public class PendingRequest implements Parcelable {
    public static final int INVALID_TYPE_VALUE = -1;

    /** type: 有人请求加我为朋友 */
    public static final int BUDDY_IN = 0;
    /** type: 我请求加某人为朋友 */
    public static final int BUDDY_OUT = 1;
    /** type: 某群邀请我加入 */
    public static final int GROUP_IN = 2;
    /** type: 我请求加入某群 */
    public static final int GROUP_OUT = 3;
    /** type: 有人请求加入我的群 */
    public static final int GROUP_ADMIN = 4;

    /** id in local db. */
    public int id = -1;

    public int type = INVALID_TYPE_VALUE;

    /** 此值的解释取决于 type，基本上它是指事件中除我以外的那个人。*/
    public String uid;
    public String nickname;
    public long buddy_photo_timestamp;
    /** 此值可能无意义，取决于 type。*/
    public String group_id;
    public String group_name;
    public long group_photo_timestamp;
    /** 发送请求时附带的文本消息。*/
    public String msg;

    public PendingRequest() {
    }

    public PendingRequest(int type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(buddy_photo_timestamp);
        parcel.writeString(group_id);
        parcel.writeString(group_name);
        parcel.writeLong(group_photo_timestamp);
        parcel.writeInt(id);
        parcel.writeString(msg);
        parcel.writeString(nickname);
        parcel.writeInt(type);
        parcel.writeString(uid);
    }

    public static Creator<PendingRequest> CREATOR = new Creator<PendingRequest>() {
        @Override
        public PendingRequest createFromParcel(Parcel parcel) {
            PendingRequest p = new PendingRequest();
            p.buddy_photo_timestamp = parcel.readLong();
            p.group_id = parcel.readString();
            p.group_name = parcel.readString();
            p.group_photo_timestamp = parcel.readLong();
            p.id = parcel.readInt();
            p.msg = parcel.readString();
            p.nickname = parcel.readString();
            p.type = parcel.readInt();
            p.uid = parcel.readString();
            return p;
        }

        @Override
        public PendingRequest[] newArray(int i) {
            return new PendingRequest[i];
        }
    };
}
