package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.msg.MessageComposerActivityBase;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;


import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

public class GroupChatInfoActivity extends Activity implements OnClickListener{
	private class MemberAdapter extends BaseAdapter {
		public MemberAdapter() {
			
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			final GroupMember member = groupMembers.get(position);
            OnClickListener listener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userID = PrefUtil.getInstance(GroupChatInfoActivity.this).getUid();
                    if (_isDeleteMode) {
                        if (groupMembers.get(position).userID.equals(userID))
                            return;
                        deleteBuddyFromGroup(groupMembers.get(position));
                    } else {
                        if (_isCreator) {
                            if (position == groupMembers.size() - 1) {
                                // There are only creator and add/delete buttons.
                                if (groupMembers.size() == 3) {
                                    mMsgBox.toast(R.string.group_delete_only_creator);
                                    return;
                                }
                                _isDeleteMode = true;
                                deleteGridLast();
                                memberAdapter.notifyDataSetChanged();
                            } else if (position == groupMembers.size() - 2) {
                                Intent addIntent = new Intent();
                                addIntent.setClass(GroupChatInfoActivity.this, MultiSelectActivity.class);
                                ThemeHelper.putExtraCurrThemeResId(addIntent, GroupChatInfoActivity.this);
                                String[] memberIds = getCurrentMemberIds();
                                addIntent.putExtra("currentMemberIds", memberIds);
                                addIntent.putExtra("group_id", groupId);
                                startActivityForResult(addIntent, REQ_ADD_MEMBER);
                            } else {
                                GroupMember member = groupMembers.get(position);
                                if (null != member && !TextUtils.isEmpty(member.userID)) {
                                    int friendType = ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
                                    if (member.userID.equals(userID)) {
                                        friendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
                                    } else if (0 != (member.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                                        friendType = ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
                                    }
                                    ContactInfoActivity.launch(GroupChatInfoActivity.this,
                                            Person.fromBuddy(member),
                                            friendType);
                                }
                            }
                        } else {
                            if (position == groupMembers.size() - 1) {
                                Intent addIntent = new Intent();
                                addIntent.setClass(GroupChatInfoActivity.this, MultiSelectActivity.class);
                                ThemeHelper.putExtraCurrThemeResId(addIntent, GroupChatInfoActivity.this);
                                String[] memberIds = getCurrentMemberIds();
                                addIntent.putExtra("currentMemberIds", memberIds);
                                addIntent.putExtra("group_id", groupId);
                                startActivityForResult(addIntent, REQ_ADD_MEMBER);
                            } else {
                                GroupMember member = groupMembers.get(position);
                                if (null != member && !TextUtils.isEmpty(member.userID)) {
                                    int friendType = ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
                                    if (member.userID.equals(userID)) {
                                        friendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
                                    } else if (0 != (member.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                                        friendType = ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
                                    }
                                    ContactInfoActivity.launch(GroupChatInfoActivity.this,
                                            Person.fromBuddy(member),
                                            friendType);
                                }
                            }
                        }
                    }
                }
            };
			if (convertView == null) {
				convertView = LayoutInflater.from(GroupChatInfoActivity.this).inflate(R.layout.listitem_groupchat_member,
                        parent, false);
			}
			ImageView imgPhoto = (ImageView) convertView.findViewById(R.id.img_photo);
			ImageView imgDelete = (ImageView) convertView.findViewById(R.id.img_delete);
			TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
			TextView adminView = (TextView) convertView.findViewById(R.id.admin_view);
            imgPhoto.setOnClickListener(listener);
			if (_isDeleteMode) {
				imgDelete.setVisibility(View.VISIBLE);
				Buddy buddy = groupMembers.get(position);
				PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
				txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
                if (groupMembers.get(position).userID.equals(PrefUtil.getInstance(GroupChatInfoActivity.this).getUid())) {
                    imgDelete.setVisibility(View.GONE);
                }
			} else {
                if (_isCreator) {
                    if (position < groupMembers.size() - 2) {
                        if(member.getLevel() == GroupMember.LEVEL_CREATOR){
                        	adminView.setVisibility(View.VISIBLE);
                        }else if(member.getLevel() == GroupMember.LEVEL_DEFAULT){
                        	adminView.setVisibility(View.GONE);
                        }
                        Buddy buddy = groupMembers.get(position);
                        PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
                        txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);   
                    } else if (position == groupMembers.size() - 2 && position!=0) {
                        if(member.getLevel() == GroupMember.LEVEL_CREATOR){
                        	adminView.setVisibility(View.VISIBLE);
                        }else if(member.getLevel() == GroupMember.LEVEL_DEFAULT){
                        	adminView.setVisibility(View.GONE);
                        }
                        PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.chat_invite, new Buddy(), true);
                        txtName.setText(getResources().getString(R.string.add));
                    } else if (position == groupMembers.size() - 1 && position!=0) {
                        if(member.getLevel() == GroupMember.LEVEL_CREATOR){
                        	adminView.setVisibility(View.VISIBLE);
                        }else if(member.getLevel() == GroupMember.LEVEL_DEFAULT){
                        	adminView.setVisibility(View.GONE);
                        }
                        PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.chat_remove, new Buddy(), true);
                        txtName.setText(getResources().getString(R.string.contacts_local_delete));
                    }
                    imgDelete.setVisibility(View.GONE);
                } else {
                    if (position < groupMembers.size() - 1) {
                        Buddy buddy = groupMembers.get(position);
                        PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
                        txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
                    } else {
                        PhotoDisplayHelper.displayPhoto(GroupChatInfoActivity.this, imgPhoto, R.drawable.chat_invite, new Buddy(), true);
                        txtName.setText(getResources().getString(R.string.add));
                    }
                    imgDelete.setVisibility(View.GONE);
                }
			}
			return convertView;
		}
		
	}
	private static final int MSG_GET_GROUPCHAT_MEMBER_SUCCESS = 1000;
	private static final int REQ_ADD_MEMBER = 123;
	private static final int REQ_MODIFY_GROUP_NAME = 124;
	private ImageButton btnTitleBack;
	private TextView txtMemberCount;
	private TextView txtSeeAll;
	private TextView txtSave;
	private TextView txtSetBg;
	private RelativeLayout mGroupNameLayout;
	private TextView mGroupNameText;
	private RelativeLayout mMsgHistoryLayout;
	private TextView txtClear;
	private Button btnQuit;
	private Button btnView;
	private YQGridView gridMembers;
    private MessageBox mMsgBox;

	private String groupId;
	private String mGroupName;
	private boolean mIsGroupNameChanged;
	private boolean _isDeleteMode = false;
    private boolean _isCreator = false;
	private Database dbHelper;
    private WowTalkWebServerIF mWeb;
	private MemberAdapter memberAdapter;
	private ArrayList<GroupMember> groupMembers = new ArrayList<GroupMember>();
	private boolean mIsClearedChatHistory;
	private int myLevel = GroupMember.LEVEL_DEFAULT;
	private Database mDbHelper = null;
	private GroupChatRoom groupRoom;
	private PrefUtil mPrefUtil;
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtMemberCount = (TextView) findViewById(R.id.txt_members_count);
		txtSeeAll = (TextView) findViewById(R.id.txt_seeall);
		txtSave = (TextView) findViewById(R.id.txt_save);
		txtSetBg = (TextView) findViewById(R.id.txt_setbg);
		mGroupNameLayout = (RelativeLayout) findViewById(R.id.group_name_layout);
		mGroupNameText = (TextView) findViewById(R.id.txt_group_name);
		if (mIsGroupNameChanged && !TextUtils.isEmpty(mGroupName)){
		    mGroupNameText.setText(mGroupName);
		}
		mMsgHistoryLayout = (RelativeLayout) findViewById(R.id.message_history_layout);
		txtClear = (TextView) findViewById(R.id.txt_clear);
		btnQuit = (Button) findViewById(R.id.btn_quit);
		btnView = (Button) findViewById(R.id.btn_view);
		gridMembers = (YQGridView) findViewById(R.id.grid_members);
		memberAdapter = new MemberAdapter();
        gridMembers.setAdapter(memberAdapter);
        
        mDbHelper = new Database(this);
        mPrefUtil = PrefUtil.getInstance(this);
        groupRoom = mDbHelper.fetchGroupChatRoom(groupId);

		btnTitleBack.setOnClickListener(this);
		mGroupNameLayout.setOnClickListener(this);
		mMsgHistoryLayout.setOnClickListener(this);
        txtClear.setOnClickListener(this);
        btnQuit.setOnClickListener(this);
        btnView.setOnClickListener(this);
//		gridMembers.setOnItemClickListener(new OnItemClickListener() {
//
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
//					long id) {
//                String userID = WowTalkWebServerIF.getInstance(GroupChatInfoActivity.this).fGetMyUserIDFromLocal();
//                if (groupMembers.get(position).userID.equals(userID))
//                    return;
//				if (_isDeleteMode) {
//                    deleteBuddyFromGroup(groupMembers.get(position));
//				} else {
//                    if (_isCreator) {
//                        if (position == groupMembers.size() - 1) {
//                            _isDeleteMode = true;
//                            deleteGridLast();
//                            memberAdapter.notifyDataSetChanged();
//                        } else if (position == groupMembers.size() - 2) {
//                            Intent addIntent = new Intent(GroupChatInfoActivity.this, MultiSelectActivity.class);
//                            String[] memberIds = getCurrentMemberIds();
//                            addIntent.putExtra("currentMemberIds", memberIds);
//                            startActivityForResult(addIntent, REQ_ADD_MEMBER);
//                        }
//                    } else {
//                        if (position == groupMembers.size() - 1) {
//                            Intent addIntent = new Intent(GroupChatInfoActivity.this, MultiSelectActivity.class);
//                            String[] memberIds = getCurrentMemberIds();
//                            addIntent.putExtra("currentMemberIds", memberIds);
//                            startActivityForResult(addIntent, REQ_ADD_MEMBER);
//                        }
//                    }
//				}
//			}
//		});
        mMsgHistoryLayout.setVisibility(View.GONE);
        if (isCreatedByMe()) {
            btnQuit.setVisibility(View.GONE);
            btnView.setVisibility(View.VISIBLE);
            mGroupNameLayout.setVisibility(View.VISIBLE);
        } else {
            btnQuit.setVisibility(View.VISIBLE);
            btnView.setVisibility(View.GONE);
            mGroupNameLayout.setVisibility(View.GONE);
        }
        
	}

    private String[] getCurrentMemberIds() {
        String[] memberIds = new String[groupMembers.size()];
        GroupMember tempMember = null;
        for (int i = 0; i < groupMembers.size(); i++) {
            tempMember = groupMembers.get(i);
            memberIds[i] = tempMember.userID;
        }
        return memberIds;
    }

	private void setGridLast() {
        String strUserId = PrefUtil.getInstance(this).getUid();
        _isCreator = false;
        for (GroupMember g : groupMembers) {
            if (g.getLevel() == GroupMember.LEVEL_CREATOR) {
                if (g.userID.equals(strUserId)) {
                    _isCreator = true;
                }
            }
        }
		GroupMember buddy = new GroupMember();
		groupMembers.add(buddy);
        if (_isCreator) {
            groupMembers.add(buddy);
        }
	}
	
	private void deleteGridLast() {
		groupMembers.remove(groupMembers.size() - 1);
		groupMembers.remove(groupMembers.size() - 1);
	}
	
	private void deleteBuddyFromGroup(final Buddy buddy) {
        mMsgBox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(GroupChatInfoActivity.this)
                        .fGroupChat_RemoveMember(groupId, buddy.userID);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
                    dbHelper.deleteBuddyFromGroupChatRoom(groupId, buddy.userID);
                    groupMembers.remove(buddy);
                    // There are no add/delete buddy.
                    txtMemberCount.setText(String.format(getResources().getString(R.string.groupchat_members),
                            String.valueOf(groupMembers.size())));
                }
                _isDeleteMode = false;
                setGridLast();
                gridMembers.setAdapter(memberAdapter);
            }

        });
	}
	
	private void getGroupChatMemberFromServer() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return (Integer) WowTalkWebServerIF.getInstance(GroupChatInfoActivity.this)
                        .fGroupChat_GetMembers(groupId).get("code");
            }

            @Override
            protected void onPostExecute(Integer result) {
                groupMembers = dbHelper.fetchGroupMembers(groupId);
                groupMembers = sortMembersInGroup(groupMembers);
                setMyLevel();
                txtMemberCount.setText(String.format(getResources().getString(R.string.groupchat_members),
                        String.valueOf(groupMembers.size())));
                if (!_isDeleteMode) {
                    setGridLast();
                }
                memberAdapter.notifyDataSetChanged();
            }
        });
	}
	
	private void getGroupChatMemberFromLocal() {
        groupMembers = dbHelper.fetchGroupMembers(groupId);
        groupMembers = sortMembersInGroup(groupMembers);
        setMyLevel();
        txtMemberCount.setText(String.format(getResources().getString(R.string.groupchat_members),
                String.valueOf(groupMembers.size())));
        setGridLast();
        memberAdapter.notifyDataSetChanged();
	}

    private boolean isCreatedByMe() {
        ArrayList<GroupMember> members = dbHelper.fetchGroupMembers(groupId);
        String userId = PrefUtil.getInstance(this).getUid();
        for (GroupMember g : members) {
            if (g.getLevel() == GroupMember.LEVEL_CREATOR) {
                if (userId.equals(g.userID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void quitTempGroup() {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(GroupChatInfoActivity.this)
                        .fGroupChat_LeaveGroup(groupId);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
//                    dbHelper.deleteChatMessageWithUser(groupId);
//                    dbHelper.deleteGroupChatRoomWithID(groupId);
                    PickTempGroupActivity.instance().finish();
                    MultiSelectActivity.instance().finish();
                    MessageComposerActivity.instance().finish(); // XXX null pointer exception!
                    finish();
                }
            }
        });
    }

    private void disbandTempGroup() {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(GroupChatInfoActivity.this)
                        .fGroupChat_Disband(groupId);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
//                    dbHelper.deleteChatMessageWithUser(groupId);
//                    dbHelper.deleteGroupChatRoomWithID(groupId);
                    String myUid = PrefUtil.getInstance(GroupChatInfoActivity.this).getUid();
                    dbHelper.deleteBuddyFromGroupChatRoom(groupId, myUid);
                    dbHelper.deleteMyselfFlagFromGroupChatRoom(groupId);
                    dbHelper.deleteChatMessageWithUser(groupId);
                    dbHelper.deleteLatestChatTarget(groupId);
                    PickTempGroupActivity.instance().finish();
                    MultiSelectActivity.instance().finish();
                    MessageComposerActivity.instance().finish();
                    finish();
                }
            }
        });
    }

	@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                Intent data = new Intent();
                data.putExtra(MessageComposerActivity.IS_CLEAR_CHAT_HISTORY, mIsClearedChatHistory);
                setResult(RESULT_OK, data);
                finish();
                break;
            case R.id.group_name_layout:
            	if(myLevel == GroupMember.LEVEL_CREATOR){
            		gotoSetGroupName();
            	}else{
            		mMsgBox.toast("您没有权限修改群组名");
            	}
                
                break;
            case R.id.message_history_layout:
                Intent intent = new Intent(GroupChatInfoActivity.this, MessageHistoryActivity.class);
                intent.putExtra(MessageComposerActivityBase.KEY_TARGET_IS_TMP_GROUP, true);
                intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID, groupId);
                intent.putExtra(MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME, mGroupName);
                startActivity(intent);
                break;
            case R.id.txt_clear:
                deleteChatMsg();
                break;
            case R.id.btn_quit:
    			Builder builder = new AlertDialog.Builder(GroupChatInfoActivity.this);
    			builder.setTitle("提示");
    			builder.setMessage("你确定要退出吗?");
    			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface arg0, int arg1) {
                        dbHelper.deleteChatMessageWithUser(groupId);
                        dbHelper.deleteLatestChatTarget(groupId);
    	                quitTempGroup();
    				}
    			});
    			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {			
    				@Override
    				public void onClick(DialogInterface arg0, int arg1) {
    					
    				}
    			});
    			
    			builder.create().show();
                break;
            case R.id.btn_view:
    			Builder builder_btn_view = new AlertDialog.Builder(GroupChatInfoActivity.this);
    			builder_btn_view.setTitle("提示");
    			builder_btn_view.setMessage("你确定要退出吗?");
    			builder_btn_view.setPositiveButton("确定", new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface arg0, int arg1) {
                        dbHelper.deleteChatMessageWithUser(groupId);
                        dbHelper.deleteLatestChatTarget(groupId);
                        disbandTempGroup();
    				}
    			});
    			builder_btn_view.setNegativeButton("取消", new DialogInterface.OnClickListener() {			
    				@Override
    				public void onClick(DialogInterface arg0, int arg1) {
    					
    				}
    			});
    			
    			builder_btn_view.create().show();
                break;
            default:
                break;
        }
    }

	private void deleteChatMsg() {
	    final BottomButtonBoard board = new BottomButtonBoard(GroupChatInfoActivity.this, findViewById(R.id.layout));
	    board.add(getResources().getString(R.string.clear_chat_msg_ok), BottomButtonBoard.BUTTON_RED, new OnClickListener() {
            @Override
            public void onClick(View v) {
                board.dismiss();
                mIsClearedChatHistory = true;
                dbHelper.deleteChatMessageWithUser(groupId);
                mMsgBox.toast(R.string.groupchatinfo_clear_chat_msg_toast);
            }
        });
        board.addCancelBtn(getString(R.string.clear_chat_msg_cancel));
        board.show();
    }

    private void gotoSetGroupName() {
	    Intent intent = new Intent(GroupChatInfoActivity.this, ModifyTempGroupChatNameActivity.class);
	    intent.putExtra(ModifyTempGroupChatNameActivity.EXTRA_GROUP_ID, groupId);
	    if (null == mGroupName || !mIsGroupNameChanged){
	        mGroupName = "";
	    }
	    intent.putExtra(ModifyTempGroupChatNameActivity.EXTRA_GROUP_NAME, mGroupName);
	    startActivityForResult(intent, REQ_MODIFY_GROUP_NAME);
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK && _isDeleteMode) {
            _isDeleteMode = false;
            setGridLast();
            memberAdapter.notifyDataSetChanged();
            return true;
        }
	    return super.onKeyDown(keyCode, event);
	}

	private final static String GROUP_ID = "group_id";
	
	public static void launchForResult(Activity context, String groupID, int requestCode) {
		Intent i = new Intent(context, GroupChatInfoActivity.class);
		i.putExtra(GROUP_ID, groupID);
		context.startActivityForResult(i, requestCode);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupchat_info);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mWeb = WowTalkWebServerIF.getInstance(this);
		groupId = getIntent().getStringExtra(GROUP_ID); 
		if (groupId == null)
			return;
		dbHelper = new Database(GroupChatInfoActivity.this);
		GroupChatRoom chatRoom = dbHelper.fetchGroupChatRoom(groupId);
		if (null != chatRoom){
		    mGroupName = chatRoom.groupNameOriginal;
		    mIsGroupNameChanged = chatRoom.isGroupNameChanged;
		}
		initView();
		getGroupChatMemberFromLocal();
		getGroupChatMemberFromServer();
		//refreshMemberGrid();
	}

    @Override
    protected void onResume() {
        super.onResume();
     // refreshMemberGrid();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_MEMBER && resultCode == Activity.RESULT_OK) {
            getGroupChatMemberFromLocal();
        } else if (requestCode == REQ_MODIFY_GROUP_NAME && resultCode == Activity.RESULT_OK) {
            mIsGroupNameChanged = true;
            mGroupName = data.getStringExtra(ModifyTempGroupChatNameActivity.EXTRA_GROUP_NAME);
            mGroupNameText.setText(mGroupName);
        }
    }
    
    private ArrayList<GroupMember> sortMembersInGroup(ArrayList<GroupMember> members) {
        ArrayList<GroupMember> creator = new ArrayList<GroupMember>();
        ArrayList<GroupMember> member = new ArrayList<GroupMember>();
        for (GroupMember g : members) {
            if (g.getLevel() == GroupMember.LEVEL_CREATOR) {
                creator.add(g);
            }else if (g.getLevel() == GroupMember.LEVEL_DEFAULT) {
                member.add(g);
            }
        }
        ArrayList<GroupMember> list = new ArrayList<GroupMember>();
        list.addAll(creator);
        list.addAll(member);
        return list;
    }
    
    private void setMyLevel(){
    	myLevel = GroupMember.LEVEL_DEFAULT;
		String myUid = mPrefUtil.getUid();
		for(GroupMember b : groupMembers) {
			if(b != null && b.userID != null && b.userID.equals(myUid)) {
				myLevel = b.getLevel();
				Log.i("myLevel : " + myLevel+"");
				break;
			}
		}
    }
    
    /*
    private void refreshMemberGrid() {
		groupMembers = mDbHelper.fetchGroupMembers(groupRoom.groupID);
        groupMembers = sortMembersInGroup(groupMembers);
		Log.i("group member count : " + groupMembers.size());

		// determine my level
		myLevel = GroupMember.LEVEL_DEFAULT;
		String myUid = mPrefUtil.getUid();
		for(GroupMember b : groupMembers) {
			if(b != null && b.userID != null && b.userID.equals(myUid)) {
				myLevel = b.getLevel();
				Log.i("myLevel : " + myLevel+"");
				break;
			}
		}
		memberAdapter = new MemberAdapter();
		gridMembers.setAdapter(memberAdapter);
	}
     */
}
