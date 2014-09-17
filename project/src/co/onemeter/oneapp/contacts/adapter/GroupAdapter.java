package co.onemeter.oneapp.contacts.adapter;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Group;
import co.onemeter.oneapp.contacts.util.ContactUtil;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GroupAdapter extends BaseAdapter {
	
	private Context context;
	private String _currentGroupName;
	
	public GroupAdapter(Context context) {
		this.context = context;
		_currentGroupName = "";
		ContactUtil.fFetchAllGroups();
	}

	@Override
	public int getCount() {
		return ContactUtil.allGroups.size();
	}

	@Override
	public Object getItem(int position) {
		return ContactUtil.allGroups.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setCurrentGroupName(String currentGroupName) {
		_currentGroupName = currentGroupName;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View lView = null;
		if (convertView != null) {
			lView = convertView;
		} else {
			lView = LayoutInflater.from(context).inflate(R.layout.listitem_popup, null);
		}
		Group group = ContactUtil.allGroups.get(position);
		TextView txtGroupName = (TextView) lView.findViewById(R.id.group_name);
		txtGroupName.setText(group.getName());
		if (_currentGroupName.equals(group.getName())) {
			txtGroupName.setTextColor(Color.parseColor("#999999"));
		} else {
			txtGroupName.setTextColor(context.getResources().getColor(R.color.blue));
		}
		return lView;
	}

}
