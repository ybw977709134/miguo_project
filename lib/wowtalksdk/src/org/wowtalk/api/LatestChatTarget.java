package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 最近联系人，SmsActivity用来展示最新消息联系人
 * @author zjh
 *
 */
public class LatestChatTarget implements Parcelable {

    /**
     * 联系人uid／群组的id
     */
    public String targetId;
    public String sentDate;
    public boolean isGroup;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(targetId);
        dest.writeString(sentDate);
        dest.writeInt(isGroup ? 1 : 0);
    }

    public static Creator<LatestChatTarget> CREATOR = new Creator<LatestChatTarget>() {
        @Override
        public LatestChatTarget createFromParcel(Parcel s) {
            LatestChatTarget contactOrGroup = new LatestChatTarget();
            contactOrGroup.targetId = s.readString();
            contactOrGroup.sentDate = s.readString();
            contactOrGroup.isGroup = s.readInt() == 1;
            return contactOrGroup;
        }

        @Override
        public LatestChatTarget[] newArray(int size) {
            return new LatestChatTarget[size];
        }
    };
}
