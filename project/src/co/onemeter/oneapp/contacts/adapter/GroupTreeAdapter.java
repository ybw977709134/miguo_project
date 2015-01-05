package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.ui.MessageComposerActivity;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 树形通讯录的适配器。
 *
 * Created by pzy on 11/30/14.
 */
public class GroupTreeAdapter extends BaseAdapter {
	private final static int VIEW_TYPE_GROUP_SCHOOL = 0;
	private final static int VIEW_TYPE_GROUP_CLASSROOM = 1;
	private final static int VIEW_TYPE_BUDDY = 2;
	private final static int VIEW_TYPE_COUNT = 3;

	private Context context;
	private ArrayList<ContactTreeNode> items;

	public GroupTreeAdapter(Context context, Collection<GroupChatRoom> topLevelGroups) {
		this.context = context;
		items = new ArrayList<ContactTreeNode>(countRecursive(topLevelGroups));
		for (GroupChatRoom g : topLevelGroups) {
			addRecursive(items, g, null);
		}
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return VIEW_TYPE_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		ContactTreeNode node = items.get(position);
		if (node.isGroup()) {
			if (node.getIndentLevel() == 0)
				return VIEW_TYPE_GROUP_SCHOOL;
			else
				return VIEW_TYPE_GROUP_CLASSROOM;
		} else {
			return VIEW_TYPE_BUDDY;
		}
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ContactTreeNode node = items.get(position);

		View lView;
		int viewType = getItemViewType(position);
		if (convertView != null) {
			lView = convertView;
		} else {
			switch (viewType) {
				case VIEW_TYPE_GROUP_SCHOOL:
					lView = LayoutInflater.from(context).inflate(R.layout.listitem_school, null);
					break;
				case VIEW_TYPE_GROUP_CLASSROOM:
					lView = LayoutInflater.from(context).inflate(R.layout.listitem_classroom, null);
					break;
				case VIEW_TYPE_BUDDY:
				default:
					lView = LayoutInflater.from(context).inflate(R.layout.listitem_contact, null);
					break;
			}
		}

		switch (viewType) {
			case VIEW_TYPE_GROUP_SCHOOL:
				setupSchoolItemView(position, node, lView);
				break;
			case VIEW_TYPE_GROUP_CLASSROOM:
				setupClassRoomItemView(position, node, lView);
				break;
			case VIEW_TYPE_BUDDY:
			default:
				setupBuddyItemView(node, lView);
				break;
		}

		return lView;
	}

	private void setupBuddyItemView(ContactTreeNode node, View view) {
		AQuery q = new AQuery(view);

		q.find(R.id.contact_name).text(node.name());
		q.find(R.id.contact_state).invisible();
		PhotoDisplayHelper.displayPhoto(context, q.find(R.id.contact_photo).getImageView(),
				R.drawable.default_avatar_90, node, true);
		if(node.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			q.find(R.id.imageView_tag_stu_tea).visible();
		}else{
			q.find(R.id.imageView_tag_stu_tea).gone();
		}

		indent(node, view);
	}

	private void setupSchoolItemView(final int position, final ContactTreeNode node, View view) {
		setupGroupItemView(position, node, view);
	}

	private void setupClassRoomItemView(final int position, final ContactTreeNode node, View view) {
		setupGroupItemView(position, node, view);

		new AQuery(view).find(R.id.btn_chat).clicked(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MessageComposerActivity.launchToChatWithGroup(context, MessageComposerActivity.class, node.getGUID());
			}
		});
	}

	private void setupGroupItemView(final int position, final ContactTreeNode node, View view) {
		TextView txtGroupName = (TextView) view.findViewById(R.id.group_name);
		txtGroupName.setText(node.name());
		txtGroupName.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                node.isExpanded() ? R.drawable.tree_arrow_down : R.drawable.tree_arrow_right,
                0);

		int paddingDp = 10;
		int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) paddingDp, context.getResources().getDisplayMetrics());
		txtGroupName.setCompoundDrawablePadding(paddingPx);

		indent(node, view);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleTreeNode(position, node);
			}
		});
	}

	private void indent(ContactTreeNode node, View view) {
		int indentDp = 40;
		int indentPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) indentDp, context.getResources().getDisplayMetrics());
		view.setPadding(
				node.getIndentLevel() * indentPx + view.getPaddingRight(),
				view.getPaddingTop(),
				view.getPaddingRight(),
				view.getPaddingBottom());
	}

	private void toggleTreeNode(int position, ContactTreeNode node) {
		if (node.isExpanded()) {
			// collapse
			int i = position + 1;
			while (i < items.size() && items.get(i).getIndentLevel() > node.getIndentLevel()) {
				items.remove(i);
			}
		} else {
			// expand
			if (node.isExpandable()) {
				items.addAll(position + 1, node.children);
			}
		}
		node.toggleExpandingState();
		notifyDataSetChanged();
	}

	private void addRecursive(ArrayList<ContactTreeNode> items, GroupChatRoom g, GroupChatRoom parent) {
		items.add(new ContactTreeNode(g, parent));
		if (g.isExpand && g.childGroups != null && !g.childGroups.isEmpty()) {
			for (GroupChatRoom c : g.childGroups) {
				addRecursive(items, c, g);
			}
		}
	}

	private int countRecursive(Collection<GroupChatRoom> items) {
		if (items == null || items.isEmpty()) {
			return 0;
		}
		int cnt = items.size();
		for (GroupChatRoom g : items) {
				cnt += countRecursive(g.childGroups);
		}
		return cnt;
	}
}
