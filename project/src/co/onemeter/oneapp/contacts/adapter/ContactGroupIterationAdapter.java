package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WebServerIF;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ContactGroupIterationAdapter extends BaseAdapter {

    /**
     * the groupId of root_favorite_group, indicates the groupChatRoom is the root of favorite_group
     */
    public static final String GROUP_ID_ROOT = "0";
    /**
     * 缩进的最大层数
     */
    private static final int MAX_INDENT_LEVEL = 2;

	private Context mContext;
	private GroupNameClickedListener mGroupClickedListener;
	private ArrayList<GroupChatRoom> mRootGroups;
	private ArrayList<GroupChatRoom> mShowGroupRooms;
	/**
	 * 当前展现的level，value为对应的level群组的数量 
	 */
	private HashMap<Integer, Integer> mShowLevelMap;
	private int mMaxShowLevel = 0;

    private boolean showGroupNameOnly=false;
    private boolean withSelectionInd=false;
    private ArrayList<String> selectedGroupIds;

	public ContactGroupIterationAdapter(Context context, ArrayList<GroupChatRoom> rootGroupRooms, GroupNameClickedListener selectedGroupListener) {
		mContext = context;
		mGroupClickedListener = selectedGroupListener;
		mRootGroups = rootGroupRooms;
        for (GroupChatRoom rootRoom : mRootGroups) {
            rootRoom.isShow = true;
        }
		mShowGroupRooms = new ArrayList<GroupChatRoom>();
		mShowLevelMap = new HashMap<Integer, Integer>();
		reloadShowGroups();
	}

    public void setShowGroupNameOnly(boolean b,boolean selectInd,ArrayList<String> groupIds) {
        showGroupNameOnly=b;
        withSelectionInd=selectInd;
        selectedGroupIds=groupIds;
    }

    private void reloadShowGroups() {
	    mShowGroupRooms.clear();
        for (GroupChatRoom rootGroup : mRootGroups) {
            // rootGroup.level 默认值为0
            addShowLevleMap(rootGroup.level);
            fillShowGroups(rootGroup);
        }
    }

	private void fillShowGroups(GroupChatRoom parentGroup) {
        if (parentGroup.isShow) {
            mShowGroupRooms.add(parentGroup);
            if (null != parentGroup.childGroups && !parentGroup.childGroups.isEmpty()) {
                for (GroupChatRoom childGroup : parentGroup.childGroups) {
                    fillShowGroups(childGroup);
                }
            }
        }
    }

	@Override
	public int getCount() {
		return mShowGroupRooms.size();
	}

	@Override
	public Object getItem(int position) {
		return mShowGroupRooms.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * 重新计算mRootGroups 及 用于展示的 mShowGroupRooms
	 * @param group
	 */
	public void setRootDataSource(ArrayList<GroupChatRoom> group) {
        mShowLevelMap.clear();
        mRootGroups = group;
        for (GroupChatRoom rootRoom : mRootGroups) {
            rootRoom.isShow = true;
        }
        reloadShowGroups();
	}

	/**
	 * 重新设置favorite groups
	 * @param newRootFavoriteRoom
	 */
	public void setRootFavoriteGroupSource(GroupChatRoom newRootFavoriteRoom) {
	    GroupChatRoom oldRootFavoriteRoom = mRootGroups.get(0);
	    if (null != oldRootFavoriteRoom && GROUP_ID_ROOT.equals(oldRootFavoriteRoom.groupID)) {
	        boolean isExpand = oldRootFavoriteRoom.isExpand;
	        mRootGroups.remove(0);
	        newRootFavoriteRoom.isShow = true;
	        newRootFavoriteRoom.isExpand = isExpand;
	        // 原先展开，判断新的里面是否存在子元素：存在，继续展开，且子元素可见；否则，收缩自己。
	        if (isExpand) {
	            if (null != newRootFavoriteRoom.childGroups && !newRootFavoriteRoom.childGroups.isEmpty()) {
	                for (GroupChatRoom favoriteRoom : newRootFavoriteRoom.childGroups) {
	                    favoriteRoom.isShow = true;
	                    favoriteRoom.indentLevel = 1;
                    }
	            } else {
	                newRootFavoriteRoom.isExpand = false;
                }
	        }
	        mRootGroups.add(0, newRootFavoriteRoom);
	        reloadShowGroups();
	    }
	}

    /**
     * 关闭展开的群组，如果不关闭，下次打开群组列表时，会有之前依次展开的状态的短暂停留
     */
    public void collapseGroups() {
        if (null != mShowGroupRooms) {
            mShowGroupRooms.clear();
        }
    }

	@SuppressWarnings("deprecation")
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder tempHolder = null;
		final GroupChatRoom group = mShowGroupRooms.get(position);
		if (convertView == null) {
		    tempHolder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_group_iteration, null);
			tempHolder.imgArrowLayout = (RelativeLayout) convertView.findViewById(R.id.img_arrow_layout);
			tempHolder.imgArrow = (ImageButton) convertView.findViewById(R.id.img_arrow);
			tempHolder.groupNameText = (TextView) convertView.findViewById(R.id.group_name);
			tempHolder.imgFavorite = (ImageButton) convertView.findViewById(R.id.img_favorite);
			tempHolder.imgActionLayout = (RelativeLayout) convertView.findViewById(R.id.img_action_layout);
			tempHolder.imgAction = (ImageButton) convertView.findViewById(R.id.img_action);
			tempHolder.dividerAfter = (ImageView) convertView.findViewById(R.id.divider_after);
			convertView.setTag(tempHolder);
		} else {
		    tempHolder = (ViewHolder) convertView.getTag();
		}
		// 匿名内部类使用，需要final
		final ViewHolder holder = tempHolder;

        // 缩进
        setIndentLevel(group);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(DensityUtil.dip2px(mContext, 40), RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (null == group.childGroups || group.childGroups.isEmpty()) {
            params.setMargins(DensityUtil.dip2px(mContext, (group.indentLevel - 1) * 43), 0, 0, 0);
            holder.imgArrowLayout.setLayoutParams(params);
            holder.imgArrowLayout.setVisibility(View.INVISIBLE);
        } else {
            params.setMargins(DensityUtil.dip2px(mContext, group.indentLevel * 43), 0, 0, 0);
            holder.imgArrowLayout.setLayoutParams(params);
            holder.imgArrowLayout.setVisibility(View.VISIBLE);
        }
        holder.imgArrow.setBackgroundResource(group.isExpand ? R.drawable.group_arrow_down : R.drawable.group_arrow_right);
        holder.imgArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                expandOrCollapse(position);
            }
        });

        holder.groupNameText.setText(group.groupNameOriginal);
        holder.groupNameText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击的收藏的群组，则不跳转，无响应事件
                if (GROUP_ID_ROOT.equals(group.groupID)) {
                    return;
                }
                // 点击群组名称，跳转至联系人界面
                mGroupClickedListener.onGroupNameClicked(group);
            }
        });

        holder.imgFavorite.setVisibility(group.isRootGroup() ? View.INVISIBLE : View.VISIBLE);
        holder.imgFavorite.setBackgroundResource(group.isFavorite ? R.drawable.group_bookmark_a : R.drawable.group_bookmark);
        holder.imgFavorite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 设置favorite，且需要变更favorite节点群组的内容
                // 可能是添加，也可能是取消
                // 此处先设置isFavorite，如果上传服务器失败后，再在回调中设回来
                group.isFavorite = !group.isFavorite;
                holder.imgFavorite.setBackgroundResource(group.isFavorite ? R.drawable.group_bookmark_a : R.drawable.group_bookmark);

                // 可能是点的all节点下的群组；也可能是点击的favorite节点下的群组
                // all节点下的不需要特殊处理
                // favorite节点下的，需要同时更新all下对应群组的isFavorite属性
                // 且对应的group不一定处于show状态，可能不存在与mShowGroupRooms中，需要更新mRootGroups
                // 需要此处对adapter内容进行修改，如果直接监听数据库，则会重新获取所有的group，会是一个全新的未展开的根节点
                if (GROUP_ID_ROOT.equals(group.parentGroupId)) {
                    // 过滤mShowGroupRooms
                    for (GroupChatRoom tempChatRoom : mShowGroupRooms) {
                        // group自己包含在mShowGroupRooms里，所以需要过滤掉自己
                        if (tempChatRoom != group && null != tempChatRoom && group.groupID.equals(tempChatRoom.groupID)) {
                            tempChatRoom.isFavorite = group.isFavorite;
                            holder.imgFavorite.setBackgroundResource(tempChatRoom.isFavorite ? R.drawable.group_bookmark_a : R.drawable.group_bookmark);
                            break;
                        }
                    }

                    // 过滤mRootGroups
                    for (GroupChatRoom rootRoom : mRootGroups) {
                        // 过滤非favorite节点下的群组
                        if (!GROUP_ID_ROOT.equals(rootRoom.groupID)) {
                            setFavoriteInRootGroupRoom(group.isFavorite, group.groupID, rootRoom);
                        }
                    }
                }

                // 上报服务器
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... params) {
                        return WebServerIF.getInstance(mContext).updateGroupFavorite(group.groupID, group.isFavorite);
                    }

                    protected void onPostExecute(Integer result) {
                        Log.d("ContactGroupIterationAdapter#getView, updateGroupFavorite() resultCode is " + result);
                        if (ErrorCode.OK != result) {
                            // 如果上传服务器失败后，则设置回原来的状态
                            group.isFavorite = !group.isFavorite;
                            holder.imgFavorite.setBackgroundResource(group.isFavorite ? R.drawable.group_bookmark_a : R.drawable.group_bookmark);
                        }
                    };

                }.execute((Void)null);
            }
        });

        if (GROUP_ID_ROOT.equals(group.groupID)) {
//            holder.imgActionLayout.setVisibility(View.VISIBLE);
            holder.imgAction.setVisibility(View.VISIBLE);
            holder.imgAction.setBackgroundResource(R.drawable.group_sort);
//            holder.dividerAfter.setVisibility(View.VISIBLE);
        } else {
//            holder.imgActionLayout.setVisibility(group.isMeBelongs ? View.VISIBLE : View.GONE);
            holder.imgAction.setVisibility(group.isMeBelongs ? View.VISIBLE : View.INVISIBLE);
            holder.imgAction.setBackgroundResource(R.drawable.group_send);
//            holder.dividerAfter.setVisibility(group.isMeBelongs ? View.VISIBLE : View.GONE);
        }
//        holder.imgAction.setBackgroundResource(ContactsActivityForBiz.GROUP_ID_ROOT.equals(group.groupID)
//                ? R.drawable.group_sort : R.drawable.group_send);
        holder.imgAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GROUP_ID_ROOT.equals(group.groupID)) {
                    // 排序，跳转排序页面
                    GroupChatRoom rootFavoriteGroup = mRootGroups.get(0);
                    // 收藏的群组数超过一个才能进入收藏界面
                    if (null != rootFavoriteGroup.childGroups && rootFavoriteGroup.childGroups.size() > 1) {
                        Intent intent = new Intent(mContext, FavoriteGroupsSortActivity.class);
                        mContext.startActivity(intent);
                    }
                } else {
                    // 聊天
                    MessageComposerActivity.launchToChatWithGroupForBizDept(mContext, MessageComposerActivity.class, group);
                }
            }
        });

        if(showGroupNameOnly) {
            tempHolder.imgFavorite.setVisibility(View.GONE);

            if(!withSelectionInd) {
                tempHolder.imgActionLayout.setVisibility(View.GONE);
                tempHolder.dividerAfter.setVisibility(View.GONE);
            } else {
                if(selectedGroupIds.contains(group.groupID)) {
                    tempHolder.imgAction.setBackgroundResource(R.drawable.photo_selected);
                } else {
                    tempHolder.imgAction.setBackgroundResource(R.drawable.photo_unselected);
                }
                holder.imgAction.setVisibility(View.VISIBLE);
                holder.imgAction.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.groupNameText.performClick();
                    }
                });
            }
        }

        return convertView;
    }

	/**
	 * 设置group的缩进格数indentLevel
	 * @param group
	 */
    private void setIndentLevel(GroupChatRoom group) {
        // f(group.level,group.indentLevel,mMaxShowLevel) 之间的某个关系
        if (GROUP_ID_ROOT.equals(group.groupID)) {
            group.indentLevel = 0;
        } else if (GROUP_ID_ROOT.equals(group.parentGroupId)) {
            group.indentLevel = 1;
        } else {
            if (mMaxShowLevel <= MAX_INDENT_LEVEL) {
                group.indentLevel = group.level;
            } else {
                group.indentLevel = group.level - mMaxShowLevel + MAX_INDENT_LEVEL;
                group.indentLevel = group.indentLevel > 0 ? group.indentLevel : 0;
            }
        }
    }

    /**
	 * 点击favorite节点下的群组时，需要将其对应的all节点的群组的is_favorite状态修改
	 * @param isFavorite
	 * @param targetGroupId
	 * @param parentGroupChatRoom
	 */
	private void setFavoriteInRootGroupRoom(boolean isFavorite, String targetGroupId, GroupChatRoom parentGroupChatRoom) {
	    if (null != parentGroupChatRoom) {
	        if (targetGroupId.equals(parentGroupChatRoom.groupID)) {
	            // 只会有一个与targetGroupId相同，可以直接return；但是由于此方法为迭代，其他迭代路径无法return
	            parentGroupChatRoom.isFavorite = isFavorite;
	            return;
	        }
	        if (null != parentGroupChatRoom.childGroups && !parentGroupChatRoom.childGroups.isEmpty()) {
	            for (GroupChatRoom childChatRoom : parentGroupChatRoom.childGroups) {
                    setFavoriteInRootGroupRoom(isFavorite, targetGroupId, childChatRoom);
                }
	        }
	    }
	}

	/**
	 * 展开或收缩group
	 * @param position
	 */
	public void expandOrCollapse(int position) {
        GroupChatRoom group = mShowGroupRooms.get(position);
        if (null != group.childGroups && !group.childGroups.isEmpty()) {
            if (group.isExpand) {
                // 收缩子组
                collapseGroupsIteration(group);
                group.isShow = true;
                addShowLevleMap(group.level);
            } else {
                expandChildGroups(group);
            }
        }
        // 计算mMaxShowLevel
        setMaxShowLevel();
        mShowGroupRooms.clear();
        reloadShowGroups();
        notifyDataSetChanged();
    }

	/**
	 * 计算mMaxShowLevel
	 */
	private void setMaxShowLevel() {
	    mMaxShowLevel = 0;
	    Set<Integer> levels = mShowLevelMap.keySet();
	    for (Integer level : levels) {
	        mMaxShowLevel = mMaxShowLevel > level ? mMaxShowLevel : level;
        }
    }

    /**
	 * 展开group.
	 * @param group
	 */
    private void expandChildGroups(GroupChatRoom group) {
        if (group.isRootGroup()) {
            group.level = 0;
        }
        group.isExpand = true;
        group.isShow = true;
        // 展开的group，必然有子群组，否则没有展开的入口按钮
        if (null != group.childGroups && !group.childGroups.isEmpty()) {
            // 当点击的当前group的indentLevel为2时(即第三层)，
            // 就必须前移 父group及本身同级(此时其他的group的indentLevel最大为2)。
            if (group.indentLevel == MAX_INDENT_LEVEL) {
                // TODO 未完成
//                forwardMoveGroups(group);
            }
            for (GroupChatRoom tempGroup : group.childGroups) {
                // TODO 判断是否到最后
                tempGroup.level = group.level + 1;
                tempGroup.indentLevel = group.indentLevel + 1;
                tempGroup.isExpand = false;
                tempGroup.isShow = true;
                addShowLevleMap(tempGroup.level);
            }
        }
    }

    /**
     * 收缩group
     * @param group
     */
    private void collapseGroupsIteration(GroupChatRoom group) {
        group.isExpand = false;
        group.isShow = false;
        removeShowLevleMap(group.level);
        if (null == group.childGroups || group.childGroups.isEmpty()) {
            // 最后的叶子节点
        } else {
            for (GroupChatRoom tempGroup : group.childGroups) {
                collapseGroupsIteration(tempGroup);
            }
        }
    }

    /**
     * 增加show的level，用于计算mMaxShowLevel
     * @param level
     */
    public void addShowLevleMap(int level) {
        if (mShowLevelMap.containsKey(level)) {
            mShowLevelMap.put(level, mShowLevelMap.get(level) + 1);
        } else {
            mShowLevelMap.put(level, 1);
        }
    }

    /**
     * 移除show的level，用于计算mMaxShowLevel
     * @param level
     */
    public void removeShowLevleMap(int level) {
        Integer levelCounts = mShowLevelMap.get(level);
        if (null == levelCounts || levelCounts == 1) {
            mShowLevelMap.remove(level);
        } else {
            mShowLevelMap.put(level, levelCounts - 1);
        }
  }

	static class ViewHolder {
	    RelativeLayout imgArrowLayout;
	    ImageButton imgArrow;
		TextView groupNameText;
		RelativeLayout imgActionLayout;
		ImageButton imgAction;
		ImageView dividerAfter;
		ImageButton imgFavorite;
	}

	/**
	 * 点击群组名称，进入群组成员列表界面的回调接口
	 */
	public interface GroupNameClickedListener {
	    void onGroupNameClicked(GroupChatRoom chatRoom);
	    void onSendMsgClicked();
	}
}
