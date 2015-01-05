package co.onemeter.oneapp.contacts.adapter;

import java.util.List;

import org.wowtalk.api.GroupChatRoom;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactDiscussionAdapter extends BaseAdapter {

    private Context mContext;
    private List<GroupChatRoom> mGroupRooms;

    public ContactDiscussionAdapter(Context context, List<GroupChatRoom> groupRooms) {
        mContext = context;
        this.mGroupRooms = groupRooms;
    }

    @Override
    public int getCount() {
        return mGroupRooms.size();
    }

    @Override
    public Object getItem(int position) {
        return mGroupRooms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setDataSource(List<GroupChatRoom> group) {
        this.mGroupRooms = group;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        GroupChatRoom group = mGroupRooms.get(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_discussion_biz, null);
            holder.imgThumbnail = (ImageView) convertView.findViewById(R.id.group_photo);
            holder.groupName = (TextView) convertView.findViewById(R.id.group_name);
            holder.memberCounts = (TextView) convertView.findViewById(R.id.member_counts);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.imgThumbnail.setBackgroundDrawable(null);
        PhotoDisplayHelper.displayPhoto(mContext, holder.imgThumbnail, R.drawable.default_group_avatar_90, group, true);

        holder.groupName.setText(group.groupNameOriginal);
        holder.memberCounts.setText(String.format(mContext.getString(R.string.contacts_discussion_member_counts), group.memberCount));
        if (group.isTemporaryGroup) {
            holder.memberCounts.setVisibility(View.VISIBLE);
        } else {
            holder.memberCounts.setVisibility(View.GONE);
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView imgThumbnail;
        TextView groupName;
        TextView memberCounts;
    }
}
