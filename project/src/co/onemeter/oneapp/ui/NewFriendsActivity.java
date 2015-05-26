package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-2
 * Time: PM7:38
 */
public class NewFriendsActivity extends Activity implements AdapterView.OnItemClickListener {
    private class NewFriendAdapter extends BaseAdapter {
        private Context mContext;

        public NewFriendAdapter(Context context) {
            mContext = context;    
        }       

        @Override
        public int getCount() {
            return pendings.size();
        }

        @Override
        public Object getItem(int position) {
            return pendings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final PendingRequest p = pendings.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_new_friend, parent, false);
            }
            RoundedImageView imgPhoto = (RoundedImageView) convertView.findViewById(R.id.img_photo);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            TextView txtExtra = (TextView) convertView.findViewById(R.id.txt_extra);
            Button btnAdd = (Button) convertView.findViewById(R.id.btn_add);
            Button btnIgnore = (Button) convertView.findViewById(R.id.btn_ignore);
            TextView txtStatus = (TextView) convertView.findViewById(R.id.txt_status);
            if (p.type == PendingRequest.BUDDY_IN) {
                txtStatus.setVisibility(View.GONE);
                btnAdd.setVisibility(View.VISIBLE);
                btnIgnore.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(p.msg)) {
                    txtExtra.setText(p.msg);
                } else {
                    txtExtra.setText(String.format(getString(R.string.contacts_new_friends_buddyin), p.nickname));
                }
                btnAdd.setText(mContext.getResources().getString(R.string.newfriends_accept));
                txtName.setText(p.nickname);
                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isNetworkAvailable()) {
                            mMsgBox.toast(R.string.network_connection_unavailable);
                            return;
                        }
                        addFriend_async(p);
                    }
                });
                btnIgnore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isNetworkAvailable()) {
                            mMsgBox.toast(R.string.network_connection_unavailable);
                            return;
                        }
                        ignoreBuddyRequest_async(p);
                    }
                });
            } else if (p.type == PendingRequest.BUDDY_OUT) {
                txtStatus.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.GONE);
                btnIgnore.setVisibility(View.GONE);
                txtExtra.setText(String.format(getString(R.string.contacts_new_friends_buddyout), p.nickname));
                txtStatus.setText(mContext.getResources().getString(R.string.contacts_newfriend_wait_for_cofirm));
                txtName.setText(p.nickname);
                
            }
            // 某群邀请我加入
            else if (p.type == PendingRequest.GROUP_IN) {
                txtStatus.setVisibility(View.GONE);
                btnAdd.setVisibility(View.VISIBLE);
                btnAdd.setText(mContext.getResources().getString(R.string.contacts_new_friends_join_group));
                txtName.setText(p.group_name);
                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            }
            // 我请求加入某群
            else if (p.type == PendingRequest.GROUP_OUT) {
                txtStatus.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.GONE);
                btnIgnore.setVisibility(View.GONE);
                txtExtra.setText(String.format(getString(R.string.contacts_new_friends_groupout), p.group_name));
                txtStatus.setText(mContext.getResources().getString(R.string.contacts_newfriend_wait_for_cofirm));
                txtName.setText(p.group_name);
            }
            //  有人请求加入我的群
            else if (p.type == PendingRequest.GROUP_ADMIN) {
                txtStatus.setVisibility(View.VISIBLE);
                txtName.setText(p.group_name);
                txtStatus.setText(String.format(getString(R.string.contacts_new_friends_groupadmin), p.nickname, p.group_name));
                if (TextUtils.isEmpty(p.msg)) {
                    txtExtra.setVisibility(View.GONE);
                } else {
                    txtExtra.setText(p.msg);
                    txtExtra.setVisibility(View.VISIBLE);
                }

                btnAdd.setVisibility(View.VISIBLE);
                btnIgnore.setVisibility(View.VISIBLE);

                btnAdd.setText(mContext.getResources().getString(R.string.add));
                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isNetworkAvailable()) {
                            mMsgBox.toast(R.string.network_connection_unavailable);
                            return;
                        }
                        acceptBuddyToJoinGroup_async(pendings.get(position));
                    }
                });

                btnIgnore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isNetworkAvailable()) {
                            mMsgBox.toast(R.string.network_connection_unavailable);
                            return;
                        }
                        rejectBuddyToJoinGroup_async(pendings.get(position));
                    }
                });
            } else {

            }

            // 头像
            if (p.type == PendingRequest.BUDDY_IN || p.type == PendingRequest.BUDDY_OUT) {
                Buddy b = new Buddy();
                b.userID = p.uid;
                b.photoUploadedTimeStamp = p.buddy_photo_timestamp;
                PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_avatar_90, b, true);
            } else if (p.type == PendingRequest.GROUP_IN || p.type == PendingRequest.GROUP_OUT
                    || p.type == PendingRequest.GROUP_ADMIN) {
                GroupChatRoom g = new GroupChatRoom();
                g.groupID = p.group_id;
                g.setPhotoUploadedTimestamp(p.group_photo_timestamp);
                PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_group_avatar_90, g, true);

            }
            return convertView;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != cm) {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ImageButton btnTitleBack;

    private ListView mListView;
    private MessageBox mMsgBox;
    private NewFriendAdapter friendAdapter;
    private ArrayList<PendingRequest> pendings;

    private WowTalkWebServerIF mWeb;
    private Database dbHelper;
    private PrefUtil mPrefUtil;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friends);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mPrefUtil = PrefUtil.getInstance(this);
        dbHelper = new Database(this);
//        pendings = new ArrayList<PendingRequest>();
//        ArrayList<PendingRequest> pendingRequests = new ArrayList<PendingRequest>();
//        dbHelper.fetchPendingRequest(pendingRequests);
//        for (PendingRequest p : pendingRequests) {
//            if (p.type == PendingRequest.BUDDY_IN) {
//                pendings.add(p);
//            }
//        }
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        mListView = (ListView)findViewById(R.id.list);
        mWeb = WowTalkWebServerIF.getInstance(this);
        mMsgBox = new MessageBox(this);

//        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

//        friendAdapter = new NewFriendAdapter(this);
//        mListView.setAdapter(friendAdapter);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteDlg(pendings.get(position));
                return true;
            }
        });
    }
    
    /**
     * @author hutianfeng
     * @date 2014-12-5 11:51
     * 为“+”号图片添加事件监听,点击跳转到添加新朋友界面
     * @param view
     */
    public void clickButton(View view){
    	switch (view.getId()) {
		case R.id.title_newfriend_add:
			Intent intent = new Intent();
			intent.setClass(NewFriendsActivity.this, ContactAddActivity.class);
			startActivity(intent);
			break;
		case R.id.textView_buddy_back:
			finish();
			break;

		default:
			break;
		}
    }

    private void showDeleteDlg(final PendingRequest p) {
        new AlertDialog.Builder(this).setTitle(p.nickname)
                .setIcon(android.R.drawable.ic_menu_more)
                .setItems(new String[]{getResources().getString(R.string.contacts_local_delete)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (p.type == PendingRequest.BUDDY_IN) {
                                    rejectBuddyToJoinGroup_async(p);
                                }
                            }
                        })
                .show();
    }

    private void addFriend_async(final PendingRequest p) {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mWeb.fAddBuddy(p.uid);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    dbHelper.deletePendingRequest(p.id);
                    //fAddBuddy will call storeNewBuddyWithUpdate
//                    dbHelper.storeNewBuddyWithUpdate(makeBuddyFromPendingRequest(p));
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                    mPrefUtil.setLocalContactListLastModified();

                    MessageComposerActivity.launchToChatWithBuddy(
                            NewFriendsActivity.this,
                            MessageComposerActivity.class,
                            p.uid,
                            getString(R.string.msg_friend_request_is_passed));

                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        });
    }

//    private Buddy makeBuddyFromPendingRequest(PendingRequest p) {
//        Buddy b = new Buddy();
//        b.userID = p.uid;
//        b.nickName = p.nickname;
//        b.setPhotoUploadedTimestamp(p.buddy_photo_timestamp);
//        return b;
//    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        String strUserId = PrefUtil.getInstance(NewFriendsActivity.this).getUid();
        final PendingRequest p = pendings.get(position);
        if (p.type == PendingRequest.BUDDY_IN || p.type == PendingRequest.BUDDY_OUT) {
            ContactInfoActivity.launch(this, p, ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND);
        } else if (p.type == PendingRequest.GROUP_IN) {
            ContactGroupInfoActivity.launchForResult(this, p.group_id, 0);
        } else if (p.type == PendingRequest.GROUP_ADMIN) {
            ApplyInfoActivity.launch(NewFriendsActivity.this,
                    pendings.get(position), 0);
        } else if (p.type == PendingRequest.GROUP_OUT && !p.uid.equals(strUserId)) {
            ApplyInfoActivity.launch(NewFriendsActivity.this, pendings.get(position), 0);
        }
    }

    private void rejectBuddyToJoinGroup_async(final PendingRequest p) {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<PendingRequest, Void, Integer>() {
            @Override
            protected Integer doInBackground(PendingRequest... params) {
                PendingRequest p = params[0];
                return mWeb.fGroupChat_Reject(p.group_id, p.uid);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (errno == ErrorCode.OK) {
                    dbHelper.deletePendingRequest(p.id);
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                } else if (errno == ErrorCode.PENDING_REQUEST_ALREADY_PROCESSED) {
                    mMsgBox.toast(R.string.group_pending_request_already_processed);
                    dbHelper.deletePendingRequest(p.id);
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        }, p);
    }

    private void acceptBuddyToJoinGroup_async(final PendingRequest p) {
        final Buddy b = new Buddy();
        b.userID = p.uid;
        List<Buddy> list = new ArrayList<Buddy>();
        list.add(b);
        final List<Buddy> finalList = list;
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<PendingRequest, Void, Integer>() {
            @Override
            protected Integer doInBackground(PendingRequest... params) {
                return mWeb.fGroupChat_AddMembers(p.group_id, finalList, true);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    dbHelper.deletePendingRequest(p.id);
                    GroupMember groupMember = new GroupMember(p.uid, p.group_id);
                    groupMember.setLevel(GroupMember.LEVEL_DEFAULT);
                    dbHelper.addNewBuddyToGroupChatRoomByID(groupMember, false);
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                } else if (result == ErrorCode.PENDING_REQUEST_ALREADY_PROCESSED) {
                    mMsgBox.toast(R.string.group_pending_request_already_processed);
                    dbHelper.deletePendingRequest(p.id);
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        }, p);
    }

    private void ignoreBuddyRequest_async(final PendingRequest p) {

        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<PendingRequest, Void, Integer>() {
            @Override
            protected Integer doInBackground(PendingRequest... buddies) {
                return mWeb.fIgnoreBuddyRequest(p.uid);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == errno) {
                    dbHelper.deletePendingRequest(p.id);
//                    pendings.remove(p);
//                    friendAdapter.notifyDataSetChanged();
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        }, p);
    }

    /**
     * Get pending in requests from local db.
     * <p>Count of unhandled pending-in requests will be outputted to GlobalValue.unreadPendingInRequests.</p>
     *
     * @param ctx
     * @return PendingRequest list.
     */
    public static ArrayList<PendingRequest> getPendingRequestsFromLocal(Context ctx) {
        ArrayList<PendingRequest> pendings = new ArrayList<PendingRequest>();
        Database.open(ctx).fetchPendingRequest(pendings);
        return pendings;
    }

    private IDBTableChangeListener pendingsObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadLocalPendingRequests();
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // clear notification for this activity, if any
        NotificationManager notiManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.cancel(GlobalValue.NOTIFICATION_FOR_PENDINGREQUESTS);

        // 本来想仅当收到新请求的通知后才下载请求列表，不过由于新请求通知并不足够可靠，还是每次进来都下载了。
        pullPendingRequests_async();

        Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,pendingsObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 用户可能在此页面处理了一些好友请求，那么离开此页面时，适宜刷新一下计数器。
//        getPendingRequestsFromLocal(this);

        Database.removeDBTableChangeListener(pendingsObserver);
    }

    private void loadLocalPendingRequests() {
//      pendings = getPendingRequestsFromLocal(this);
      pendings = new ArrayList<PendingRequest>();
      ArrayList<PendingRequest> pendingRequests = new ArrayList<PendingRequest>();
      dbHelper.fetchPendingRequest(pendingRequests);
      for (PendingRequest p : pendingRequests) {
          if (p.type != PendingRequest.GROUP_OUT && p.type != PendingRequest.BUDDY_OUT ) {
              pendings.add(p);
          }
      }
        friendAdapter = new NewFriendAdapter(this);
        mListView.setAdapter(friendAdapter);

        Log.i("pendigns count "+pendings.size());
        RelativeLayout rlNoFriendsBg=(RelativeLayout)findViewById(R.id.no_new_friends_bg);
        if(pendings.size() == 0) {
            rlNoFriendsBg.setVisibility(View.VISIBLE);
        } else {
            rlNoFriendsBg.setVisibility(View.GONE);
        }
    }

    private void pullPendingRequests_async() {
        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(NewFriendsActivity.this)
                        .fGetPendingRequests();
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                loadLocalPendingRequests();
            }
        });
    }

}
