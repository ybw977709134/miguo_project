package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.util.ArrayList;

public class ContactGroupAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<GroupChatRoom> groupRooms;
	
	public ContactGroupAdapter(Context context, ArrayList<GroupChatRoom> groupRooms) {
		mContext = context;
		this.groupRooms = groupRooms;
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
	
	public void setDataSource(ArrayList<GroupChatRoom> group) {
		this.groupRooms = group;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		GroupChatRoom group = groupRooms.get(position);
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_contact, null);
			holder.imgThumbnail = (ImageView) convertView.findViewById(R.id.contact_photo);
			holder.txtName = (TextView) convertView.findViewById(R.id.contact_name);
			holder.txtIntroduce = (TextView) convertView.findViewById(R.id.contact_state);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.imgThumbnail.setBackgroundDrawable(null);
		PhotoDisplayHelper.displayPhoto(mContext, holder.imgThumbnail, R.drawable.default_group_avatar_90, group, true);
		
		holder.txtName.setText(group.groupNameOriginal);
		holder.txtIntroduce.setText(group.groupStatus);
		return convertView;
	}
	
	static class ViewHolder {
		ImageView imgThumbnail;
		TextView txtName;
		TextView txtIntroduce;
	}

}
