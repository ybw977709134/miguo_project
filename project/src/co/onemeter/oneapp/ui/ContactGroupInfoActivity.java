package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.*;
import org.wowtalk.ui.msg.MessageComposerActivityBase;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.GroupMembersGridAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContactGroupInfoActivity extends Activity implements OnClickListener,
        GroupMembersGridAdapter.OnBuddyClickListener {
	public final static String EXTRA_GROUP_DELETED = "group_deleted";
	public final static String EXTRA_PHOTO_CHANGED = "photo_changed";

    public final static String EXTRA_GROUP_ID = "group_id";
	public final static String EXTRA_ACTIVITY_RESULT = "activity_result";

    public final static int PHOTO_THUMBNAIL_WIDTH = 200;
    public final static int PHOTO_THUMBNAIL_HEIGHT = 200;
    // resize photo to VGA size before sending
    public final static int PHOTO_SEND_WIDTH = 600;
    public final static int PHOTO_SEND_HEIGHT = 600;


	private static final int REQ_PICK_BUDDYS = 123;
	private static final int REQ_PICK_PHOTO = 124;
    private static final int REQ_EDIT_INFO = 125;
    private static final int REQ_MANAGE_MEMBER = 126;
    private static final int REQ_DEAL_JOIN_GROUP = 127;
	private static final String GROUP_ID = "group_id";

    private boolean mProfileUpdated = false;

	private ImageView imgPhoto;
	private ImageButton btnTitleBack;
	private ImageButton btnTitleSetting;
    private ImageButton btnTitleMore;
	private TextView txtGroupName;
	private TextView txtGroupID;
	private Button btnGroupChat;
    private LinearLayout layoutExtra;
    private ImageView imgDiv;
    private ImageView imgDiv2;
    private LinearLayout layoutPlace;
    private LinearLayout layoutDistance;
    private LinearLayout layoutCategory;
	private TextView txtPlace;
	private TextView txtDistance;
    private TextView txtCategory;
	private TextView txtGroupIntroduce;
	private TextView txtMembersCount;
	private TextView txtEventsCount;
	private YQGridView gridMembers;
    private YQGridView gridPendingMembers;
    private View vgPendingMembers;

	private ArrayList<GroupMember> groupMembers;
    private ArrayList<GroupMember> mPendingMembers;
    private ArrayList<PendingRequest> mCurrentPendingRequests = new ArrayList<PendingRequest>();
	private GroupMembersGridAdapter memberAdapter;
	private GroupChatRoom groupRoom;

    private MessageBox mMsgBox;
//    private BottomButtonBoard bottomBoard;

	private Database mDbHelper = null;
	private WowTalkWebServerIF mWebif = null;
	private MediaInputHelper mMediaInput = null;
	private PrefUtil mPrefUtil;

	private int myLevel = GroupMember.LEVEL_DEFAULT;

	private Intent mActivityResult = new Intent();

	// show a dummy buddy at the end of group members list
	private boolean mShowDummyBuddy = true;

    private void getMembersFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mWebif.fGroupChat_GetMembersIteration(groupRoom.groupID);
            }
        }).start();
    }

	private void initView() {
		imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleSetting = (ImageButton) findViewById(R.id.title_setting);
        btnTitleMore = (ImageButton) findViewById(R.id.title_more);

		txtGroupName = (TextView) findViewById(R.id.group_name_text);
		txtGroupID = (TextView) findViewById(R.id.group_id_text);
		btnGroupChat = (Button) findViewById(R.id.group_chat_button);

        layoutExtra = (LinearLayout) findViewById(R.id.layout_extra);
        imgDiv = (ImageView) findViewById(R.id.img_div);
        imgDiv2 = (ImageView) findViewById(R.id.img_div2);
        layoutPlace = (LinearLayout) findViewById(R.id.layout_place);
        layoutDistance = (LinearLayout) findViewById(R.id.layout_distance);
        layoutCategory = (LinearLayout) findViewById(R.id.layout_category);
		txtPlace = (TextView) findViewById(R.id.place_text);
		txtDistance = (TextView) findViewById(R.id.distance_text);
        txtCategory = (TextView) findViewById(R.id.category_text);
		txtGroupIntroduce = (TextView) findViewById(R.id.group_introduce);
		txtMembersCount = (TextView) findViewById(R.id.group_members_count_text);
		txtEventsCount = (TextView) findViewById(R.id.group_events_count_text);
		gridMembers = (YQGridView) findViewById(R.id.grid_members);
        gridPendingMembers = (YQGridView)findViewById(R.id.grid_pending_members);
        vgPendingMembers = findViewById(R.id.vg_pending_members);

		btnTitleBack.setOnClickListener(this);
		btnTitleSetting.setOnClickListener(this);
        btnTitleMore.setOnClickListener(this);
		btnGroupChat.setOnClickListener(this);
		findViewById(R.id.img_thumbnail_mask).setOnClickListener(this);
		findViewById(R.id.message_history_layout).setOnClickListener(this);
        vgPendingMembers.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.title_setting:
		    showPopupMenu();
			break;
        case R.id.title_more:
            showPopupMenu();
            break;
        case R.id.message_history_layout:
            Intent intent = new Intent(this, MessageHistoryActivity.class);
            intent.putExtra(MessageComposerActivityBase.KEY_TARGET_IS_NORMAL_GROUP, true);
            intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID, groupRoom.groupID);
            intent.putExtra(MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME, groupRoom.groupNameOriginal);
            startActivity(intent);
            break;
		case R.id.group_chat_button:
			MessageComposerActivity.launchToChatWithGroup(this, MessageComposerActivity.class, groupRoom);
			break;
		case R.id.img_thumbnail_mask:
            ImageViewActivity.launch(this, groupRoom);
			break;
		default:
			break;
		}
	}

    private ArrayList<GroupMember> sortMembersInGroup(ArrayList<GroupMember> members) {
        ArrayList<GroupMember> creator = new ArrayList<GroupMember>();
        ArrayList<GroupMember> admin = new ArrayList<GroupMember>();
        ArrayList<GroupMember> member = new ArrayList<GroupMember>();
        for (GroupMember g : members) {
            if (g.getLevel() == GroupMember.LEVEL_CREATOR) {
                creator.add(g);
            } else if (g.getLevel() == GroupMember.LEVEL_ADMIN) {
                admin.add(g);
            } else if (g.getLevel() == GroupMember.LEVEL_DEFAULT) {
                member.add(g);
            }
        }
        ArrayList<GroupMember> list = new ArrayList<GroupMember>();
        list.addAll(creator);
        list.addAll(admin);
        list.addAll(member);
        return list;
    }

    private void quitGroup() {
        new AsyncTask<Void, Integer, Void>() {
            int ok;
            @Override
            protected Void doInBackground(Void... arg0) {
                ok = mWebif.fGroupChat_LeaveGroup(groupRoom.groupID);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (ok == ErrorCode.OK) {
//                    mDbHelper.deleteGroupChatRoom(groupRoom);
                    mActivityResult.putExtra(EXTRA_GROUP_DELETED, true);
                    setResult(RESULT_OK, mActivityResult);
                    finish();
                }
            }
        }.execute((Void)null);
    }

    private void disbandGroup() {
        new AsyncTask<Void, Integer, Void>() {
            int ok;
            @Override
            protected Void doInBackground(Void... arg0) {
                ok = mWebif.fGroupChat_Disband(groupRoom.groupID);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (ok == ErrorCode.OK) {
//                    mDbHelper.deleteGroupChatRoom(groupRoom);
                    mDbHelper.deleteBuddyFromGroupChatRoom(groupRoom.groupID, mPrefUtil.getUid());
                    mDbHelper.deleteMyselfFlagFromGroupChatRoom(groupRoom.groupID);
                    mActivityResult.putExtra(EXTRA_GROUP_DELETED, true);
                    setResult(RESULT_OK, mActivityResult);
                    finish();
                }
            }
        }.execute((Void)null);
    }

    private void editGroupInfo() {
        Intent intent = new Intent(this, CreateGroupActivity.class);
        intent.putExtra("group", groupRoom);
        startActivityForResult(intent, REQ_EDIT_INFO);
    }

    private void manageGroupMember() {
        Intent intent = new Intent(this, GroupMemberManageActivity.class);
        ArrayList<Buddy> rooms = new ArrayList<Buddy>();
        rooms.addAll(groupMembers);
        if (mShowDummyBuddy) {
            rooms.remove(rooms.size() - 1);
            rooms.remove(rooms.size() - 1);
        }
        intent.putExtra("group", groupRoom);
        intent.putExtra("pending_requests", mCurrentPendingRequests);
        intent.putExtra("members", rooms);
        intent.putExtra("mylevel", myLevel);
        startActivityForResult(intent, REQ_MANAGE_MEMBER);
    }

    private void addDummyItems() {
        GroupMember b = new GroupMember();
        b.setLevel(GroupMember.LEVEL_ADD);
        groupMembers.add(b);
        b = new GroupMember();
        b.setLevel(GroupMember.LEVEL_DELETE);
        groupMembers.add(b);
        mShowDummyBuddy = true;
    }

//    private void deleteDummyItems() {
//        groupMembers.remove(groupMembers.size() - 1);
//        groupMembers.remove(groupMembers.size() - 1);
//        mShowDummyBuddy = false;
//    }

    private void showPopupMenu() {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, findViewById(R.id.layout));
        if (myLevel == GroupMember.LEVEL_CREATOR) {
            bottomBoard.add(getString(R.string.group_disband), BottomButtonBoard.BUTTON_RED, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    disbandGroup();
                }
            });
            bottomBoard.add(getString(R.string.group_edit_info), BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    editGroupInfo();
                }
            });
            String text;
            mCurrentPendingRequests.clear();
            mDbHelper.fetchPendingsByGroupId(groupRoom.groupID, mCurrentPendingRequests);
            if (mCurrentPendingRequests.isEmpty()) {
                text = getString(R.string.group_member_manage).substring(0, getString(R.string.group_member_manage).length() - 4);
            } else {
                text = String.format(getString(R.string.group_member_manage), mCurrentPendingRequests.size());
            }
            bottomBoard.add(text,
                    BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    manageGroupMember();
                    bottomBoard.dismiss();
                }
            });
        } else if (myLevel == GroupMember.LEVEL_ADMIN) {
            bottomBoard.add(getString(R.string.group_quit), BottomButtonBoard.BUTTON_RED, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    quitGroup();
                }
            });
            bottomBoard.add(getString(R.string.group_edit_info), BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    editGroupInfo();
                }
            });
            String text;
            mCurrentPendingRequests.clear();
            mDbHelper.fetchPendingsByGroupId(groupRoom.groupID, mCurrentPendingRequests);
            if (mCurrentPendingRequests.isEmpty()) {
                text = getString(R.string.group_apply).substring(0, getString(R.string.group_apply).length() - 4);
            } else {
                text = String.format(getString(R.string.group_apply), mCurrentPendingRequests.size());
            }
            bottomBoard.add(text, BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    // Deal the request of joining the group.
                    dealJoinGroupReq(mCurrentPendingRequests);
                }
            });
        } else {
            bottomBoard.add(getString(R.string.group_quit), BottomButtonBoard.BUTTON_RED, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    quitGroup();
                }
            });
        }
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }

    /**
     * Deal the request of joining the group.
     * @param currentReqs
     */
	protected void dealJoinGroupReq(List<PendingRequest> currentReqs) {
	    if (currentReqs.size() == 1) {
	        ApplyInfoActivity.launch(ContactGroupInfoActivity.this, currentReqs.get(0), REQ_DEAL_JOIN_GROUP);
	    } else {
	        Intent dealReqIntent = new Intent(ContactGroupInfoActivity.this, GroupJoinRequestActivity.class);
	        dealReqIntent.putExtra(GroupJoinRequestActivity.GROUP_ID, groupRoom.groupID);
	        startActivityForResult(dealReqIntent, REQ_DEAL_JOIN_GROUP);
	    }
	}

    public static void launchForResult(Activity context, String groupID, int requestCode) {
		Intent i = new Intent(context, ContactGroupInfoActivity.class);
		i.putExtra(GROUP_ID, groupID);
		context.startActivityForResult(i, requestCode);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_group_infos);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        String groupID = getIntent().getStringExtra(GROUP_ID);

		if(savedInstanceState != null) {
			mActivityResult = savedInstanceState.getParcelable(EXTRA_ACTIVITY_RESULT);
            mMediaInput = savedInstanceState.getParcelable("mMediaInput");
        } else {
			mActivityResult = new Intent();
        }

        mMsgBox = new MessageBox(this);
        mPrefUtil = PrefUtil.getInstance(this);
		mDbHelper = new Database(this);
		mWebif = WowTalkWebServerIF.getInstance(this);

		groupRoom = mDbHelper.fetchGroupChatRoom(groupID);
        if (groupRoom != null) {
            mActivityResult.putExtra(EXTRA_GROUP_ID, groupRoom.groupID);
            initView();

            /* 理论上软件安装后只需要调用一次 getMembersFromServer()，
             * 因为以后若有成员增减都会有消息通知。
             *
             * 不过现在无法分辨这两种情况：
             * A，成员数为0；
             * B，软件刚刚安装；
             */
            setDisplayOfGroup();
        } else {
            Log.e("ContractGroupInfoActivity.onCreate(), load data faliure: groupId = " + groupID);
            btnTitleBack = (ImageButton) findViewById(R.id.title_back);
            btnTitleBack.setOnClickListener(this);
            mMsgBox.show(null, getString(R.string.contactgroupinfo_load_failure));
        }

	}

    private void setDisplayOfGroup() {
        groupRoom = mDbHelper.fetchGroupChatRoom(groupRoom.groupID); // reload from db


//        if(true || groupMembers.size() <= (mShowDummyBuddy ? 2 : 0)) // there's a dummy buddy at the end
        getMembersFromServer();

        if (null != groupRoom && !groupRoom.isEditable && TextUtils.isEmpty(groupRoom.parentGroupId)) {
            groupRoom.groupNameOriginal = getString(R.string.contactsforbiz_root_group_name_display);
        }

        txtGroupName.setText(groupRoom.groupNameOriginal);
        txtGroupID.setText(String.format(getString(R.string.contact_info_group_short_id), groupRoom.shortGroupID));
        // There're 3 layout rows: place, distance, category.
        int totalLayoutRows = 3;
        int emptyLayoutRows = 0;
        if (groupRoom.location == null || groupRoom.location.x == 0 || groupRoom.location.y == 0) {
            layoutDistance.setVisibility(View.GONE);
            imgDiv.setVisibility(View.GONE);
            emptyLayoutRows++;
        } else {
            layoutDistance.setVisibility(View.GONE);
            float [] distance = new float[1];
            Location.distanceBetween(0, 0, 0, 0, distance);
            txtDistance.setText(String.format("%fKM", distance[0]));
            emptyLayoutRows++;
        }
        if (groupRoom.place == null || groupRoom.place.equals("")) {
            layoutPlace.setVisibility(View.GONE);
            imgDiv.setVisibility(View.GONE);
            emptyLayoutRows++;
        } else {
            layoutPlace.setVisibility(View.VISIBLE);
            txtPlace.setText(groupRoom.place);
        }
        if (groupRoom.category == null || groupRoom.category.equals("")) {
            layoutCategory.setVisibility(View.GONE);
            emptyLayoutRows++;
        } else {
            layoutCategory.setVisibility(View.VISIBLE);
            String[] categoryMsg = getResources().getStringArray(R.array.group_category_msg);
            String[] category = getResources().getStringArray(R.array.group_category);
            for (int i = 0; i < categoryMsg.length; i++) {
                if (groupRoom.category.equals(categoryMsg[i])) {
                    txtCategory.setText(category[i]);
                    break;
                }
            }
        }
        if(layoutPlace.getVisibility() == View.GONE || layoutDistance.getVisibility() == View.GONE) {
            imgDiv.setVisibility(View.GONE);
        } else {
            imgDiv.setVisibility(View.VISIBLE);
        }
        if (layoutDistance.getVisibility() == View.GONE || layoutCategory.getVisibility() == View.GONE) {
            imgDiv2.setVisibility(View.GONE);
        } else {
            imgDiv2.setVisibility(View.VISIBLE);
        }
        if (layoutDistance.getVisibility() == View.GONE && layoutPlace.getVisibility() == View.VISIBLE
                && layoutCategory.getVisibility() == View.VISIBLE) {
            imgDiv.setVisibility(View.VISIBLE);
            imgDiv2.setVisibility(View.GONE);
        }
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            if (emptyLayoutRows == totalLayoutRows) {
                layoutExtra.setVisibility(View.GONE);
            } else {
                int eachLineHeight = getResources().getDimensionPixelOffset(
                        R.dimen.section_row_height);
                int rows = totalLayoutRows - emptyLayoutRows;
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutExtra
                        .getLayoutParams();
                params.height = eachLineHeight * rows;
                layoutExtra.setLayoutParams(params);
                layoutExtra.setVisibility(View.VISIBLE);
            }
        } else if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
            layoutExtra.setVisibility(View.GONE);
        }
        txtGroupIntroduce.setText((groupRoom.groupStatus == null || groupRoom.groupStatus.equals("")) ?
                getResources().getString(R.string.contacts_group_no_introduce) :
                groupRoom.groupStatus);
        txtEventsCount.setText(String.format(getResources().getString(R.string.group_events), "0"));

        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_group_avatar_90, groupRoom, true);
    }

	@Override
	protected void onSaveInstanceState(Bundle data) {
        super.onSaveInstanceState(data);
		data.putParcelable(EXTRA_ACTIVITY_RESULT, mActivityResult);
        if(mMediaInput != null) {
            data.putParcelable("mMediaInput", mMediaInput);
        }
	}

    private IDBTableChangeListener buddiesObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshMemberGrid();
                }
            });
        }
    };

    private IDBTableChangeListener groupMembersObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshMemberGrid();
                }
            });
        }
    };

    private IDBTableChangeListener mPendingRequestObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTitleSettingImage();
                }
            });
        }
    };

    /**
     * 设置右上角的菜单按钮是否要有新请求的小红点
     */
    private void setTitleSettingImage() {
        if (GlobalValue.RELEASE_AS_WOWCITY && (GroupMember.LEVEL_CREATOR == myLevel || GroupMember.LEVEL_ADMIN == myLevel)) {
            int pendingCounts = Database.open(ContactGroupInfoActivity.this).fetchPendingCountsByGroupId(groupRoom.groupID);
            Log.i("ContactGroupInfoActivity, mPendingRequestObserver, the counts of pendings is " + pendingCounts);
            if (pendingCounts == 0) {
                btnTitleSetting.setImageResource(R.drawable.nav_settings_selector);
            } else {
                btnTitleSetting.setImageResource(R.drawable.nav_settings_pending_selector);
            }
        }
    }

	@Override
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);

        refreshMemberGrid();
        setTitleSettingImage();
        Database.addDBTableChangeListener(Database.TBL_BUDDIES,buddiesObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP_MEMBER,groupMembersObserver);
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,mPendingRequestObserver);
        }
	}

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);

        Database.removeDBTableChangeListener(buddiesObserver);
        Database.removeDBTableChangeListener(groupMembersObserver);
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            Database.removeDBTableChangeListener(mPendingRequestObserver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mProfileUpdated) {
            new AsyncTask<Void, Integer, Void> () {
                @Override
                protected Void doInBackground(Void... params) {
                    ServerHelper.notifyGroupProfileChanged(ContactGroupInfoActivity.this, groupRoom.groupID);
                    return null;
                }
            }.execute((Void) null);
            mProfileUpdated = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(REQ_PICK_BUDDYS == requestCode && RESULT_OK == resultCode) {
            // do nothing
            // Database.TBL_GROUP_MEMBER <==> groupMembersObserver
        } else if (REQ_PICK_PHOTO == requestCode && RESULT_OK == resultCode) {
            String[] path = new String[2];
            if(mMediaInput != null && mMediaInput.handleImageResult(this, data,
                    PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                    PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                    path)) {

                displayPhoto(path[0]);

                mMsgBox.showWait();

				new AsyncTask<String, Integer, Void> (){

					boolean ok = true;
					String filePath, thumbPath;

					@Override
					protected Void doInBackground(String... arg0) {
						filePath = arg0[0];
						thumbPath = arg0[1];

						mWebif.fPostGroupPhoto(groupRoom.groupID, filePath,
								new NetworkIFDelegate(){

							@Override
							public void didFailNetworkIFCommunication(int arg0,
									byte[] arg1) {
								ok = false;
							}

							@Override
							public void didFinishNetworkIFCommunication(int arg0,
									byte[] arg1) {
							}

							@Override
							public void setProgress(int arg0, int arg1) {
							}

						}, false, 0);

						mWebif.fPostGroupPhoto(groupRoom.groupID, thumbPath,
								new NetworkIFDelegate(){

							@Override
							public void didFailNetworkIFCommunication(int arg0,
									byte[] arg1) {
								ok = false;
							}

							@Override
							public void didFinishNetworkIFCommunication(int arg0,
									byte[] arg1) {
							}

							@Override
							public void setProgress(int arg0, int arg1) {
							}

						}, true, 0);

						return null;
					}

					@Override
					protected void onPostExecute(Void v) {
                        mMsgBox.dismissWait();
						mMsgBox.toast(ok ? R.string.done : R.string.operation_failed);
						if(ok) {
							groupRoom.setPhotoUploadedTimestamp(new Date().getTime() / 1000);
							mDbHelper.updateGroupChatRoom(groupRoom);

                            PhotoDisplayHelper.locallyCopy(ContactGroupInfoActivity.this,
                                    groupRoom.groupID, filePath, false);
                            PhotoDisplayHelper.locallyCopy(ContactGroupInfoActivity.this,
                                    groupRoom.groupID, thumbPath, true);

							PhotoDisplayHelper.displayPhoto(ContactGroupInfoActivity.this,
                                    imgPhoto, R.drawable.default_avatar_90, groupRoom, true);

							mActivityResult.putExtra(EXTRA_PHOTO_CHANGED, true);
							ContactGroupInfoActivity.this.setResult(RESULT_OK, mActivityResult);
                            mProfileUpdated = true;
						}
					}

				}.execute(path);
			}
		} else if (requestCode == REQ_EDIT_INFO) {
            if (resultCode == RESULT_OK) {
                setDisplayOfGroup();
                PrefUtil.getInstance(this).setLocalGroupListLastModified();
            }
        } else if ((requestCode == REQ_MANAGE_MEMBER || requestCode == REQ_DEAL_JOIN_GROUP) && resultCode == RESULT_OK) {
//            refreshMemberGrid();
        }
	}

//    private void refreshGroupInfo() {
//        new AsyncTask<Void, Void, Integer>() {
//            @Override
//            protected Integer doInBackground(Void... params) {
//                return mWebif.fGroupChat_GetGroupDetail(groupRoom.groupID);
//            }
//
//            @Override
//            protected void onPostExecute(Integer result) {
//                if (ErrorCode.OK == result) {
//                    groupRoom = mDbHelper.fetchGroupChatRoom(groupRoom.groupID);
//                    setDisplayOfGroup();
//                }
//            }
//        }.execute((Void)null);
//    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && !mShowDummyBuddy
                && (myLevel == GroupMember.LEVEL_ADMIN || myLevel == GroupMember.LEVEL_CREATOR)) {
            refreshMemberGrid();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

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
				break;
			}
		}

        txtMembersCount.setText(String.format(
                getResources().getString(R.string.group_members),
                String.valueOf(groupMembers.size())));

		if(GroupMember.LEVEL_ADMIN == myLevel || GroupMember.LEVEL_CREATOR == myLevel) {
//			Buddy buddy = new Buddy();
//            buddy.setLevel(GroupMember.LEVEL_DUMMY);
//            buddy.setLevel(GroupMember.LEVEL_ADD);
//			groupMembers.add(buddy);
//            buddy.setLevel(GroupMember.LEVEL_DELETE);
//            groupMembers.add(buddy);
            addDummyItems();
			mShowDummyBuddy = true;
		} else {
//            GroupMember b = new GroupMember();
//            b.setLevel(GroupMember.LEVEL_ADD);
//            groupMembers.add(b);
			mShowDummyBuddy = false;
		}
		memberAdapter = new GroupMembersGridAdapter(this, this, groupMembers, myLevel, mShowDummyBuddy);
		gridMembers.setAdapter(memberAdapter);

        if (GroupMember.LEVEL_ADMIN == myLevel || GroupMember.LEVEL_CREATOR == myLevel) {
//            downloadAndDisplayPendingMembers_async(); // function moved to NewFriendsActivity
        }

        if (GlobalValue.RELEASE_AS_WOWCITY) {
            if (GroupMember.LEVEL_ADMIN == myLevel) {
                btnTitleMore.setVisibility(View.INVISIBLE);
                btnTitleSetting.setVisibility(View.VISIBLE);
            } else if (GroupMember.LEVEL_CREATOR == myLevel) {
                btnTitleMore.setVisibility(View.INVISIBLE);
                btnTitleSetting.setVisibility(View.VISIBLE);
            } else if (GroupMember.LEVEL_DEFAULT == myLevel) {
                btnTitleMore.setVisibility(View.VISIBLE);
                btnTitleSetting.setVisibility(View.INVISIBLE);
            }
        } else if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
            btnTitleMore.setVisibility(View.INVISIBLE);
            btnTitleSetting.setVisibility(View.INVISIBLE);
        }
	}

    // download & show pending members
//    private void downloadAndDisplayPendingMembers_async() {
//        mPendingMembers = new ArrayList<GroupMember>();
//        new AsyncTask<Void, Void, Integer>() {
//            ArrayList<GroupMember> lst = new ArrayList<GroupMember>();
//
//            @Override
//            protected Integer doInBackground(Void... voids) {
//                return mWebif.fGroupChat_GetPendingMembers(groupRoom.groupID, lst);
//            }
//
//            @Override
//            protected void onPostExecute(Integer errno) {
//                if (errno == ErrorCode.OK) {
//                    mPendingMembers = lst;
//                    if (mPendingMembers.isEmpty()) {
//                        vgPendingMembers.setVisibility(View.GONE);
//                    } else {
//                        gridPendingMembers.setAdapter(
//                                new GroupMembersGridAdapter(
//                                        ContactGroupInfoActivity.this,
//                                        ContactGroupInfoActivity.this,
//                                        mPendingMembers, GroupMember.LEVEL_DEFAULT, false));
//                        vgPendingMembers.setVisibility(View.VISIBLE);
//                    }
//                    vgPendingMembers.setVisibility(View.GONE);
//                }
//            }
//        }.execute((Void)null);
//    }

    private void displayPhoto(String path) {
        if(path != null && new File(path).exists()) {
            try {
                imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgPhoto.setImageDrawable(new BitmapDrawable(this.getResources(), path));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBuddyClick(final GroupMember buddy) {

        if (buddy.userID.equals(mPrefUtil.getUid()))
            return;

//        if (buddy.getLevel() == GroupMember.LEVEL_DUMMY) {
//            Intent intent = new Intent(this, MultiSelectActivity.class);
//            startActivityForResult(intent, REQ_PICK_BUDDYS);
//            return;
//        }

        if (buddy.getLevel() == GroupMember.LEVEL_ADD) {
            ArrayList<GroupMember> members = mDbHelper.fetchGroupMembers(groupRoom.groupID);
            String[] membersArray = new String[members.size()];
            for (int i = 0; i < members.size(); i++) {
                membersArray[i] = members.get(i).userID;
            }
            Intent addIntent = new Intent();
            addIntent.setClass(this, MultiSelectActivityForBiz.class);
            addIntent.putExtra("isOnlyShowContacts", true);
            addIntent.putExtra("currentMemberIds", membersArray);
            addIntent.putExtra("group_id", groupRoom.groupID);
            startActivityForResult(addIntent, REQ_PICK_BUDDYS);
            return;
        }

        if (buddy.getLevel() == GroupMember.LEVEL_DELETE) {
            // If there is only the creater exists, it will not work.
            // the size 3 means the creator,LEVEL_ADD and LEVEL_DELETE
            if ((groupMembers.size() == 3) && (groupMembers.get(0).getLevel() == GroupMember.LEVEL_CREATOR)){
                mMsgBox.toast(R.string.group_delete_only_creator);
                return;
            }

            mShowDummyBuddy = false;
            groupMembers.remove(groupMembers.size() - 1);
            groupMembers.remove(groupMembers.size() - 1);
            memberAdapter = new GroupMembersGridAdapter(this, this, groupMembers, myLevel, false);
            gridMembers.setAdapter(memberAdapter);
            return;
        }

        if (buddy.getLevel() == GroupMember.LEVEL_DEFAULT && !mShowDummyBuddy) {
            if (myLevel == GroupMember.LEVEL_CREATOR || myLevel == GroupMember.LEVEL_ADMIN) {
                kickoutMember_asyc(buddy);
                return;
            }
        } else if (buddy.getLevel() == GroupMember.LEVEL_ADMIN && !mShowDummyBuddy) {
            if (myLevel == GroupMember.LEVEL_CREATOR) {
                kickoutMember_asyc(buddy);
                return;
            }
        }

        // reuse string resource id as action id.
        final int[] actions = new int[10];
        int n = 0;

        actions[n++] = R.string.view_info;

        if (buddy.getLevel() == GroupMember.LEVEL_DEFAULT
                && myLevel == GroupMember.LEVEL_CREATOR) {
            actions[n++] = R.string.group_set_as_admin;
        }

        if (buddy.getLevel() == GroupMember.LEVEL_DEFAULT
                && (myLevel == GroupMember.LEVEL_ADMIN || myLevel == GroupMember.LEVEL_CREATOR)) {
            actions[n++] = R.string.group_kickout;
        }

        if (buddy.getLevel() == GroupMember.LEVEL_ADMIN
                && myLevel == GroupMember.LEVEL_CREATOR) {
            actions[n++] = R.string.groupmembermanage_cancel_admin;
            actions[n++] = R.string.group_kickout;
        }

        if (buddy.getLevel() == GroupMember.LEVEL_PENDING
                && (myLevel == GroupMember.LEVEL_ADMIN || myLevel == GroupMember.LEVEL_CREATOR)) {
            actions[n++] = R.string.group_accept_member;
            actions[n++] = R.string.group_reject_member;
        }

        String[] items = new String[n];
        final int[] btnStyles = new int[n];
        OnClickListener[] listeners = new OnClickListener[n];
        final BottomButtonBoard bottomBtnBoard = new BottomButtonBoard(this, findViewById(R.id.layout));
        for (int i = 0; i < n; ++i) {
            final int index = i;
            items[i] = getString(actions[i]);
            switch (actions[i]) {
                case R.string.view_info:
                case R.string.group_accept_member:
                case R.string.group_set_as_admin:
                case R.string.groupmembermanage_cancel_admin:
                    btnStyles[i] = BottomButtonBoard.BUTTON_BLUE;
                    break;
                case R.string.group_reject_member:
                case R.string.group_kickout:
                    btnStyles[i] = BottomButtonBoard.BUTTON_RED;
                    break;
            }

            listeners[i] = new OnClickListener() {

                @Override
                public void onClick(View v) {
                    bottomBtnBoard.dismiss();
                    switch (actions[index]) {
                        case R.string.view_info:
                            ContactInfoActivity.launch(ContactGroupInfoActivity.this,
                                    buddy.userID,
                                    ContactInfoActivity.BUDDY_TYPE_UNKNOWN);
                            break;
                        case R.string.group_accept_member:
                            acceptMember_asyc(buddy);
                            break;
                        case R.string.group_reject_member:
                            mMsgBox.toast(R.string.not_implemented);
                            break;
                        case R.string.group_set_as_admin:
                            // Set admin.
                            changeBuddyLevel(groupRoom.groupID, buddy.userID, GroupMember.LEVEL_ADMIN);
                            break;
                        case R.string.groupmembermanage_cancel_admin:
                            // cancel admin.
                            changeBuddyLevel(groupRoom.groupID, buddy.userID, GroupMember.LEVEL_DEFAULT);
                            break;
                        case R.string.group_kickout:
                            kickoutMember_asyc(buddy);
                            break;
                    }
                }
            };
        }

        bottomBtnBoard.add(items, btnStyles, listeners);
        bottomBtnBoard.addCancelBtn(getString(R.string.cancel));
        bottomBtnBoard.show();
    }

    protected void changeBuddyLevel(final String groupID, final String userID, final int levelAdmin) {
        mMsgBox.showWait();
        new AsyncTask<Void, Integer, Integer>(){

            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WowTalkWebServerIF.getInstance(ContactGroupInfoActivity.this).fGroupChat_SetMemberLevel(groupID, userID, levelAdmin);
                return resultCode;
            }

            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                if (result == ErrorCode.OK) {
                    mDbHelper.updateGroupMemberLevel(groupID, userID, levelAdmin);
//                    refreshMemberGrid();
                } else {
                    mMsgBox.toast(R.string.operation_failed);
                }
            };
        }.execute((Void)null);
    }

    private void acceptMember_asyc(final Buddy buddy) {
        mMsgBox.showWait();

        new AsyncTask<Buddy, Void, Integer>() {
            @Override
            protected Integer doInBackground(Buddy... buddies) {
                if (buddies[0] == null || Utils.isNullOrEmpty(buddies[0].userID))
                    return ErrorCode.INVALID_ARGUMENT;
                ArrayList<Buddy> b = new ArrayList<Buddy>();
                b.add(buddies[0]);
                int errno = mWebif.fGroupChat_AddMembers(groupRoom.groupID, b, true);
                if (errno == ErrorCode.OK) {
                    for(Buddy buddy : b) {
                        mDbHelper.addNewBuddyToGroupChatRoomByID(
                                new GroupMember(buddy.userID, groupRoom.groupID), false);
                    }
                    mWebif.fGroupChat_GetMembers(groupRoom.groupID);
                }
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
//                if (errno == ErrorCode.OK) {
//                    refreshMemberGrid();
//                }
            }
        }.execute(buddy);
    }

    private void kickoutMember_asyc(final Buddy buddy) {
        mMsgBox.showWait();

        new AsyncTask<Buddy, Void, Integer>() {
            @Override
            protected Integer doInBackground(Buddy... buddies) {
                if (buddies[0] == null || Utils.isNullOrEmpty(buddies[0].userID))
                    return ErrorCode.INVALID_ARGUMENT;
                int errno = mWebif.fGroupChat_RemoveMember(
                        groupRoom.groupID, buddies[0].userID);
                if (errno == ErrorCode.OK) {
                    mDbHelper.deleteBuddyFromGroupChatRoom(groupRoom.groupID, buddies[0].userID);
                    mWebif.fGroupChat_GetMembers(groupRoom.groupID);
                }
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
//                if (errno == ErrorCode.OK) {
//                    refreshMemberGrid();
//                }
                mShowDummyBuddy = true;
            }
        }.execute(buddy);
    }
}
