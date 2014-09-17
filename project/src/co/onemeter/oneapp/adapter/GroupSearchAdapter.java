package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

public class GroupSearchAdapter extends BaseAdapter {

	private ArrayList<GroupChatRoom> groupRooms;
	private Context context;
	private String strContent;
	
	public GroupSearchAdapter(Context context, ArrayList<GroupChatRoom> groupRooms, String strContent) {
		this.context = context;
		this.groupRooms = groupRooms;
		this.strContent = strContent;
	}
	@Override
	public int getCount() {
		return groupRooms.size();
	}

	@Override
	public Object getItem(int position) {
		return groupRooms.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(context).inflate(R.layout.listitem_groupsearch, null);
			holder.imgThumbnail = (ImageView) convertView.findViewById(R.id.img_thumbnail);
			holder.txtName = (TextView) convertView.findViewById(R.id.txt_group_name);
			holder.txtID = (TextView) convertView.findViewById(R.id.txt_group_id);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		String groupName = groupRooms.get(position).groupNameOriginal;
		SpannableStringBuilder ssb = new SpannableStringBuilder(groupName);
		int start = 0;
		int end = 0;
		for (int i = 0; i < groupName.length(); i++) {
            if (String.valueOf(groupName.charAt(i)).equalsIgnoreCase(String.valueOf(strContent.charAt(0)))) {
				start = i;
				end = i + strContent.length();
				break;
			}
		}
		ssb.setSpan(new ForegroundColorSpan(
                context.getResources().getColor(R.color.group_search_blue)),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		holder.txtName.setText(ssb);
		holder.txtID.setText(String.format(context.getResources().getString(
                R.string.contact_info_group_short_id),
                groupRooms.get(position).shortGroupID));
        PhotoDisplayHelper.displayPhoto(context, holder.imgThumbnail,
                R.drawable.default_avatar_90, groupRooms.get(position), true);
		return convertView;
	}
	
	static class ViewHolder {
		ImageView imgThumbnail;
		TextView txtName;
		TextView txtID;
	}

}
