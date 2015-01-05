package co.onemeter.oneapp.adapter;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupMember;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * A adapter for displaying group members in a grid.
 *
 * User: pan
 * Date: 4/18/13
 * Time: 7:49 PM
 */
public class GroupMembersGridAdapter extends BaseAdapter {

    public interface OnBuddyClickListener {
        public void onBuddyClick(GroupMember buddy);
    }

    private Activity mContext;
    private ArrayList<GroupMember> groupMembers;
    private int myLevel;
    private OnBuddyClickListener mListener;
    private LayoutInflater inflater;
    private Database mDbHelper;
    private boolean showDummyItems;

    public GroupMembersGridAdapter (
            Activity context,
            OnBuddyClickListener listener,
            ArrayList<GroupMember> members,
            int level,
            boolean isShowDummyItems) {
        mContext = context;
        groupMembers = members;
        mListener = listener;
        mDbHelper = new Database(context);
        myLevel = level;
        showDummyItems = isShowDummyItems;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return groupMembers.size();
    }

    @Override
    public Object getItem(int position) {
        return groupMembers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View lView;
        final GroupMember buddy = groupMembers.get(position);
        if (convertView != null) {
            lView = convertView;
        } else {
            lView = inflater.inflate(R.layout.listitem_group_member, null);
        }
        ImageView imgThumbnail = (ImageView) lView.findViewById(R.id.img_thumbnail);
        TextView adminView = (TextView) lView.findViewById(R.id.admin_view);
        TextView txtName = (TextView) lView.findViewById(R.id.txt_name);
        ImageView imgDel = (ImageView) lView.findViewById(R.id.img_del);
        if ((myLevel == GroupMember.LEVEL_CREATOR || myLevel == GroupMember.LEVEL_ADMIN)) {
            if (showDummyItems) {
                if (position == groupMembers.size() - 1) {
                    adminView.setVisibility(View.GONE);
                    imgThumbnail.setImageResource(R.drawable.remove_member);
                    txtName.setText(mContext.getResources().getString(R.string.contacts_local_delete));
                } else if (position == groupMembers.size() - 2) {
                    adminView.setVisibility(View.GONE);
                    imgThumbnail.setImageResource(R.drawable.add_member);
                    txtName.setText(mContext.getResources().getString(R.string.invite));
                } else {
                    if (buddy.getLevel() == GroupMember.LEVEL_CREATOR) {
                        adminView.setVisibility(View.VISIBLE);
                        adminView.setText(mContext.getResources().getString(R.string.group_creator));
                        adminView.setBackgroundResource(R.drawable.group_admin_bg1);
                    } else if(buddy.getLevel() == GroupMember.LEVEL_ADMIN) {
                        adminView.setVisibility(View.VISIBLE);
                        adminView.setBackgroundResource(R.drawable.group_admin_bg2);
                    } else {
                        adminView.setVisibility(View.GONE);
                    }
                    Buddy b2 = mDbHelper.buddyWithUserID(buddy.userID);
                    if(b2 != null)
                        buddy.photoUploadedTimeStamp = b2.photoUploadedTimeStamp;
                    imgThumbnail.setBackgroundDrawable(null);
                    PhotoDisplayHelper.displayPhoto(mContext, imgThumbnail, R.drawable.default_avatar_90, buddy, true);

                    txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
                }
            } else {
                imgDel.setVisibility(View.VISIBLE);
                if (buddy.getLevel() == GroupMember.LEVEL_CREATOR) {
                    adminView.setVisibility(View.VISIBLE);
                    adminView.setText(mContext.getResources().getString(R.string.group_creator));
                    adminView.setBackgroundResource(R.drawable.group_admin_bg1);
                    imgDel.setVisibility(View.GONE);
                } else if(buddy.getLevel() == GroupMember.LEVEL_ADMIN) {
                    if (myLevel != GroupMember.LEVEL_CREATOR) {
                        imgDel.setVisibility(View.GONE);
                    }
                    adminView.setVisibility(View.VISIBLE);
                    adminView.setBackgroundResource(R.drawable.group_admin_bg2);
                } else {
                    adminView.setVisibility(View.GONE);
                }
                Buddy b2 = mDbHelper.buddyWithUserID(buddy.userID);
                if(b2 != null)
                    buddy.photoUploadedTimeStamp = b2.photoUploadedTimeStamp;
                imgThumbnail.setBackgroundDrawable(null);
                PhotoDisplayHelper.displayPhoto(mContext, imgThumbnail, R.drawable.default_avatar_90, buddy, true);

                txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);

            }
        } else {
            imgDel.setVisibility(View.GONE);
            if (showDummyItems) {
                if (position == groupMembers.size() - 1) {
                    adminView.setVisibility(View.GONE);
                    imgThumbnail.setImageResource(R.drawable.add_member);
                    txtName.setText(mContext.getResources().getString(R.string.invite));
                } else {
                    if (buddy.getLevel() == GroupMember.LEVEL_CREATOR) {
                        adminView.setVisibility(View.VISIBLE);
                        adminView.setText(mContext.getResources().getString(R.string.group_creator));
                        adminView.setBackgroundResource(R.drawable.group_admin_bg1);
                    } else if(buddy.getLevel() == GroupMember.LEVEL_ADMIN) {
                        adminView.setVisibility(View.VISIBLE);
                        adminView.setBackgroundResource(R.drawable.group_admin_bg2);
                    } else {
                        adminView.setVisibility(View.GONE);
                    }
                    Buddy b2 = mDbHelper.buddyWithUserID(buddy.userID);
                    if(b2 != null)
                        buddy.photoUploadedTimeStamp = b2.photoUploadedTimeStamp;
                    imgThumbnail.setBackgroundDrawable(null);
                    PhotoDisplayHelper.displayPhoto(mContext, imgThumbnail, R.drawable.default_avatar_90, buddy, true);

                    txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
                }

            } else {
                if (buddy.getLevel() == GroupMember.LEVEL_CREATOR) {
                    adminView.setVisibility(View.VISIBLE);
                    adminView.setText(mContext.getResources().getString(R.string.group_creator));
                    adminView.setBackgroundResource(R.drawable.group_admin_bg1);
                } else if(buddy.getLevel() == GroupMember.LEVEL_ADMIN) {
                    adminView.setVisibility(View.VISIBLE);
                    adminView.setBackgroundResource(R.drawable.group_admin_bg2);
                } else {
                    adminView.setVisibility(View.GONE);
                }
                Buddy b2 = mDbHelper.buddyWithUserID(buddy.userID);
                if(b2 != null)
                    buddy.photoUploadedTimeStamp = b2.photoUploadedTimeStamp;
                imgThumbnail.setBackgroundDrawable(null);
                PhotoDisplayHelper.displayPhoto(mContext, imgThumbnail, R.drawable.default_avatar_90, buddy, true);

                txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
            }
        }

        imgThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onBuddyClick(buddy);
                }
            }
        });

        return lView;
    }
}
