package co.onemeter.oneapp.contacts.model;

import android.content.Context;
import android.os.Parcel;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.IHasPhoto;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * 树形通讯录的节点，它封装了一个 {@link org.wowtalk.api.GroupChatRoom} 或者 {@link Buddy}.
 *
 * Created by pzy on 11/30/14.
 */
public class ContactTreeNode implements IHasPhoto {
    private GroupChatRoom parent;
    private GroupChatRoom group;
    private Buddy buddy;
    public List<ContactTreeNode> children;

    public ContactTreeNode(GroupChatRoom group, GroupChatRoom parent) {
        if (group == null)
            throw new InvalidParameterException("group is null.");

        this.group = group;
        this.parent = parent;

        initChildren(group);
    }

    private void initChildren(GroupChatRoom group) {
        int numSubGroups = group.childGroups == null ? 0 : group.childGroups.size();
        int numBuddies = group.memberList == null ? 0 : group.memberList.size();
        children = new ArrayList<ContactTreeNode>(numBuddies + numSubGroups);
        if (numSubGroups > 0) {
            for (GroupChatRoom subGroup : group.childGroups) {
                children.add(new ContactTreeNode(subGroup, group));
            }
        }
        if (numBuddies > 0) {
            for (Buddy b : group.memberList) {
                children.add(new ContactTreeNode(b, group));
            }
        }
    }

    public ContactTreeNode(Buddy buddy, GroupChatRoom parent) {
        if (buddy == null)
            throw new InvalidParameterException("buddy is null.");
        this.buddy = buddy;
        this.parent = parent;
    }

    public boolean isExpandable() {
        return children != null && !children.isEmpty();
    }

    public int getIndentLevel() {
        if (group != null)
            return group.level;
        else if (parent != null)
            return parent.level + 1;
        return 0;
    }

    public boolean isExpanded() {
        return group != null && group.isExpand;
    }

    public void toggleExpandingState() {
        if (group != null)
            group.isExpand = !group.isExpand;
    }

    public String name() {
        if (group != null)
            return group.getDisplayName();
        if (buddy != null)
            return buddy.alias;
        return "?";
    }

    public boolean isGroup() {
        return group != null;
    }

    @Override
    public int getAccountType() {
        if (group != null)
            return Buddy.ACCOUNT_TYPE_GROUP;
        if (buddy != null)
            return buddy.getAccountType();
        return Buddy.ACCOUNT_TYPE_NULL;
    }

    @Override
    public String getGUID() {
        if (group != null)
            return group.getGUID();
        if (buddy != null)
            return buddy.getGUID();
        return null;
    }

    @Override
    public void setPhotoUploadedTimestamp(long value) {
        if (group != null)
            group.setPhotoUploadedTimestamp(value);
        if (buddy != null)
            buddy.setPhotoUploadedTimestamp(value);
    }

    @Override
    public long getPhotoUploadedTimestamp() {
        if (group != null)
            return group.getPhotoUploadedTimestamp();
        if (buddy != null)
            return buddy.getPhotoUploadedTimestamp();
        return 0;
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(parent, flags);
        parcel.writeParcelable(group, flags);
        parcel.writeParcelable(buddy, flags);
    }

    public static final Creator<ContactTreeNode> CREATOR =
            new Creator<ContactTreeNode>() {
                @Override
                public ContactTreeNode createFromParcel(Parcel parcel) {
                    GroupChatRoom parent = parcel.readParcelable(GroupChatRoom.class.getClassLoader());
                    GroupChatRoom group = parcel.readParcelable(GroupChatRoom.class.getClassLoader());
                    if (group != null)
                        return new ContactTreeNode(group, parent);

                    Buddy buddy = parcel.readParcelable(Buddy.class.getClassLoader());
                    if (buddy != null)
                        return new ContactTreeNode(buddy, parent);

                    return null;
                }

                @Override
                public ContactTreeNode[] newArray(int n) {
                    return new ContactTreeNode[n];
                }
            };
}
