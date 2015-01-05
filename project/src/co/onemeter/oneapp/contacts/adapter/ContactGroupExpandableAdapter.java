package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.*;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;

import java.util.ArrayList;
import java.util.List;

public class ContactGroupExpandableAdapter extends BaseExpandableListAdapter {

    /**
     * the groupId of latestContact, indicates the groupChatRoom is latestContact
     */
    public static final String GROUP_ID_OF_LATEST_CONTACT = "-1";
    private List<GroupChatRoom> mParentGroups;
    private List<List> mChildGroupMembers;

    private Context mContext;
    private PhotoDisplayHelper mPhotoDisplayHelper;
    private Database mDbHelper;
    private boolean mIsSelectMode;

    public ContactGroupExpandableAdapter(Context context, List<GroupChatRoom> parentGroups, List<List> childGroupMembers) {
        this(context, parentGroups, childGroupMembers, false);
    }

    public ContactGroupExpandableAdapter(Context context, List<GroupChatRoom> parentGroups, List<List> childGroupMembers, boolean isSelectMode) {
        mParentGroups = parentGroups;
        mChildGroupMembers = childGroupMembers;
        mContext = context;
        mIsSelectMode = isSelectMode;
        mPhotoDisplayHelper = new PhotoDisplayHelper(context);
        mDbHelper = new Database(context);
    }

    public List<GroupChatRoom> getGroupRooms() {
        return mParentGroups;
    }

    /**
     * get child Members
     * @param groupPosition
     * @return 此方法是在多人会话选择时被调用，此时没有最近联系人，因此返回的是Person的List集合
     */
    public List getChildMembers(int groupPosition) {
        return mChildGroupMembers.get(groupPosition);
    }

    public List<List> getChildMembers() {
        return mChildGroupMembers;
    }

    public void setChildMembers(List<List> childGroupMembers) {
        mChildGroupMembers = childGroupMembers;
    }

    public void setDatas(List<GroupChatRoom> parentGroups, List<List> childGroupMembers) {
        mParentGroups = parentGroups;
        mChildGroupMembers = childGroupMembers;
    }

    @Override
    public int getGroupCount() {
        return mParentGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildGroupMembers.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mParentGroups.get(groupPosition);
    }

    /**
     * 此方法在多人会话选择时被调用时，没有最近联系人，因此返回的是Person；
     * 在联系人界面被调用时，返回的是Person/GroupChatRoom
     */
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildGroupMembers.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {

        GroupChatRoom group = mParentGroups.get(groupPosition);
        ViewHolderParent holder = null;
        if (convertView == null) {
            holder = new ViewHolderParent();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_group_parent, null);
            holder.groupName = (TextView) convertView.findViewById(R.id.group_name);
            holder.groupCounts = (TextView) convertView.findViewById(R.id.group_counts);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderParent) convertView.getTag();
        }

        if (GROUP_ID_OF_LATEST_CONTACT.equals(group.groupID)) {
            holder.groupName.setText(mContext.getString(R.string.contacts_latest_contacts));
        } else {
            holder.groupName.setText(group.groupNameOriginal);
        }
        holder.groupCounts.setText(String.valueOf(group.memberCount));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {

        boolean isContactGroup = false;
        Person person = null;
        Buddy buddy = null;
        GroupChatRoom groupRoom = null;
        Object contactObj = mChildGroupMembers.get(groupPosition).get(childPosition);
        if (contactObj instanceof Person) {
            isContactGroup = false;
            person = (Person) contactObj;
            buddy = person.toBuddy();
        } else if (contactObj instanceof GroupChatRoom) {
            isContactGroup = true;
            groupRoom = (GroupChatRoom) contactObj;
        } else {
            // it shouldn't be true
            return convertView;
        }
        ViewHolderChildren holder = null;
        if (convertView == null) {
            holder = new ViewHolderChildren();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_contact, null);
            holder.txtContactFirstChar = (TextView) convertView.findViewById(R.id.contact_first_char);
            holder.imgDivider = (ImageView) convertView.findViewById(R.id.divider_view);
            holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
            holder.imgSelected = (ImageView) convertView.findViewById(R.id.img_selected);
            holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
            holder.txtContactState = (TextView) convertView.findViewById(R.id.contact_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderChildren) convertView.getTag();
        }
        holder.txtContactFirstChar.setVisibility(View.GONE);
        holder.imgDivider.setVisibility(View.GONE);

        holder.imgPhoto.setBackgroundDrawable(null);
        if (null != person && person.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
            PhotoDisplayHelper.displayPhoto(mContext, holder.imgPhoto,
                    R.drawable.default_official_avatar_90, buddy, true);
        } else {
            if (isContactGroup) {
                // 最近联系人中，获得的临时会话组的属性中没有头像属性，所以不能和buddy做相同处理
                // 此处参考 smsActivity中的临时会话头像处理
                mPhotoDisplayHelper.setLoadingImage(R.drawable.default_group_avatar_90);
                if (null != groupRoom) {
                    mPhotoDisplayHelper.loadImage(groupRoom, holder.imgPhoto);
                } else {
                    holder.imgPhoto.setImageResource(R.drawable.default_group_avatar_90);
                }
            } else {
                PhotoDisplayHelper.displayPhoto(mContext, holder.imgPhoto,
                        R.drawable.default_avatar_90, buddy, true);
            }
        }

        // mIsSelectMode 为 true时， 在多人会话选择界面，此时没有最近联系人， child 都是 Person(isContactGroup = false)
        if (mIsSelectMode) {
            holder.imgSelected.setVisibility(View.VISIBLE);
            holder.imgSelected.setBackgroundResource(person.isSelected() ? R.drawable.list_selected : R.drawable.list_unselected);
        } else {
            holder.imgSelected.setVisibility(View.GONE);
        }

        holder.txtContactName.setText(isContactGroup ? groupRoom.groupNameOriginal : person.getName());
        //if is temp group, set contact as all members' name if the display_name hasn't been set;
        // otherwise, set as the display_name
        if(isContactGroup && null != groupRoom && groupRoom.isTemporaryGroup && !groupRoom.isGroupNameChanged) {
            ArrayList<GroupMember> groupMembers = mDbHelper.fetchGroupMembers(groupRoom.groupID);
            PrefUtil prefUtil = PrefUtil.getInstance(mContext);
            String myUid = prefUtil.getUid();
            String myNickname = prefUtil.getMyNickName();
            StringBuilder sb = new StringBuilder();
            if (!groupMembers.isEmpty()) {
                for (int i = 0; i < groupMembers.size(); ++i) {
                    GroupMember aMember = groupMembers.get(i);
                    if (!TextUtils.isEmpty(myUid) && myUid.equals(aMember.userID)) {
                        continue;
                    }
                    sb.append(TextUtils.isEmpty(aMember.alias) ? aMember.nickName : aMember.alias);
                    sb.append(" , ");
                }
            }
            sb.append(myNickname);
            holder.txtContactName.setText(sb.toString());
        }

        holder.txtContactState.setText(isContactGroup ? groupRoom.groupStatus : person.getPersonState());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    static class ViewHolderParent {
        TextView groupName;
        TextView groupCounts;
    }

    static class ViewHolderChildren {
        ImageView expandablePortrait;
        TextView txtContactFirstChar;
        ImageView imgDivider;
        ImageView imgSelected;
        ImageView imgPhoto;
        TextView txtContactName;
        TextView txtContactState;
    }
}
