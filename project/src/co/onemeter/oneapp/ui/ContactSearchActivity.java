package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.BuddySearchItemAdapter;
import co.onemeter.oneapp.adapter.GroupSearchAdapter;
import co.onemeter.oneapp.contacts.model.Person;

import java.util.ArrayList;

public class ContactSearchActivity extends Activity implements OnClickListener {

//    public static final int REQ_PUBLIC_ACCOUNT = 999;

//    public static final int REQ_NORMAL_ACCOUNT = 998;

//    public static final String EXTRA_SEARCH =  "extra_search";

	private static final int GET_BUDDY_BY_WOWTALK_ID = 1000;
	private static final int GET_GROUP_BY_NAME = 1001;
	private int searchKind = GET_BUDDY_BY_WOWTALK_ID;
	
	private ImageButton btnTitleBack;
//    private LinearLayout mLayoutButton;
	private Button btnSearchGroup;
	private Button btnSearchPerson;
//    private TextView txtPublicSearch;
	private Button btnSearch;
	
//	private Button btnAdd;
	
//	private RelativeLayout mPersonLayout;
//	private RelativeLayout mGroupLayout;
	
//	private FrameLayout framePhoto;
	private EditText edtSearchContent;
    private ImageButton fieldClear;
//	private ImageView imgThumbNail;
//	private TextView txtPersonName;
    private LinearLayout searchResultLayout;
	private TextView txtResultCount;
    private ListView lvBuddy;
	private ListView lvGroups;

    private BuddySearchItemAdapter buddyAdapter;
	private GroupSearchAdapter groupAdapter;

    private String buddySearchContent="";
    private String groupSearchContent="";

    private ArrayList<Buddy> searchedBuddyList=new ArrayList<Buddy>();
	private ArrayList<GroupChatRoom> searchedGroupRoomList=new ArrayList<GroupChatRoom>();
	
	private boolean mAllowAdd = true;
//	private int mFriendType = ContactInfoActivity.BUDDY_TYPE_UNKNOWN;

//	private Buddy buddy;
	
	private WowTalkWebServerIF mWebif = null;
    private PrefUtil mPrefUtil;
	private Database mDbHelper = null;
    private MessageBox mMsgBox;

//	private int searchType;

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {
                fieldClear.setVisibility(View.GONE);
            } else {
                fieldClear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            String curSearchTxt=edtSearchContent.getText().toString();

            if (searchKind == GET_BUDDY_BY_WOWTALK_ID) {
                if(!buddySearchContent.equals(curSearchTxt)) {
                    if (buddyAdapter != null) {
                        buddyAdapter.clear();
                    } else {
                        searchedBuddyList.clear();
                    }
                }
                buddySearchContent=edtSearchContent.getText().toString();
            } else {
                if(!groupSearchContent.equals(curSearchTxt)) {
                    if (groupAdapter != null) {
                        groupAdapter.clear();
                    } else {
                        searchedGroupRoomList.clear();
                    }
                }
                groupSearchContent=edtSearchContent.getText().toString();
            }

            setSearchResultStatus();
        }
    };

	private void searchGroup() {
		final String keyword = edtSearchContent.getText().toString().trim();
		if(keyword.equals(""))
			return;

        mMsgBox.showWait();

		new AsyncTask<Void, Integer, Void>() {

            ArrayList<GroupChatRoom> results = new ArrayList<GroupChatRoom>();

			@Override
			protected Void doInBackground(Void... params) {
//				GroupChatRoom groupWithThatShortID = null;
                if (groupAdapter != null)
                    groupAdapter.clear();
                else
                    searchedGroupRoomList.clear();
//				if(groupWithThatShortID != null) {
//                    searchedGroupRoomList.add(groupWithThatShortID);
//				}
				mWebif.fGroupChat_Search(keyword, results);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
                mMsgBox.dismissWait();
                if (groupAdapter != null) {
                    groupAdapter.addAll(results);
                    setSearchResultStatus();
                } else {
                    searchedGroupRoomList.addAll(results);
                    fShowGroupResult();
                }
			}
			
		}.execute((Void)null);
	}
	
	private void searchBuddy() {
		final String wowtalkId = edtSearchContent.getText().toString();

        mMsgBox.showWait();

		new AsyncTask<Void, Integer, Void>() {
			int errno = ErrorCode.OK;
            ArrayList<Buddy> results = new ArrayList<Buddy>();

			@Override
			protected Void doInBackground(Void... arg0) {
//				buddy = new Buddy();
                if (buddyAdapter != null)
                    buddyAdapter.clear();
                else
                    searchedBuddyList.clear();

				errno = mWebif.fSearchBuddy(wowtalkId,Buddy.ACCOUNT_TYPE_NORMAL, results);

//                mAllowAdd = errno == ErrorCode.OK
//                        && !Utils.isNullOrEmpty(buddy.userID)
//                        && 0 == (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)
//                        && !buddy.userID.equals(mWebif.fGetMyUserIDFromLocal());
//
//                if (!TextUtils.isEmpty(buddy.userID)) {
//                    if (buddy.userID.equals(mWebif.fGetMyUserIDFromLocal())) {
//                        mFriendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
//                    } else if (errno == ErrorCode.OK) {
//                        mFriendType = (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE))
//                                ? ContactInfoActivity.BUDDY_TYPE_IS_FRIEND
//                                        : ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
//                    }
//                }
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
                mMsgBox.dismissWait();

                if (buddyAdapter != null) {
                    buddyAdapter.addAll(results);
                    setSearchResultStatus();
                } else {
                    searchedBuddyList.addAll(results);
                    fShowBuddyResult();
                }
				if (errno == ErrorCode.USER_NOT_EXISTS) {
                    mMsgBox.toast(R.string.login_user_not_exists);
				}
			}
			
		}.execute((Void)null);
	}

//    private void searchPublicAccount() {
//        final String wowtalkId = edtSearchContent.getText().toString();
//
//        new AsyncTask<Void, Integer, Void>() {
//            int errno = ErrorCode.OK;
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                buddy = new Buddy();
//                errno = mWebif.fGetBuddyByWowtalkId(wowtalkId, buddy);
//
//                if (errno == ErrorCode.OK && !Utils.isNullOrEmpty(buddy.userID)
//                        && (buddy.getFriendShipWithMe() == Buddy.RELATIONSHIP_FRIEND_BOTH
//                        || buddy.userID.equals(mWebif.fGetMyUserIDFromLocal()))) {
//                    mAllowAdd = false;
//                }
//                return null;  //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            protected void onPostExecute(Void v) {
//                if (errno == ErrorCode.OK) {
//                    fShowBuddyResult();
//                } else if (errno == ErrorCode.USER_NOT_EXISTS) {
//                    MessageBox msgBox = new MessageBox(ContactSearchActivity.this);
//                    msgBox.toast("user not exists!");
//                }
//            }
//        }.execute((Void)null);
//    }

	private void fShowGroupResult() {
//		mPersonLayout.setVisibility(View.GONE);
//		mGroupLayout.setVisibility(View.VISIBLE);
		final String strContent = edtSearchContent.getText().toString().trim();
//		txtResultCount.setText(String.format(getResources().getString(R.string.search_result_count), groupRooms.size()));
		groupAdapter = new GroupSearchAdapter(ContactSearchActivity.this, searchedGroupRoomList, strContent);
		lvGroups.setAdapter(groupAdapter);
        setSearchResultStatus();
	}
	
	private void fShowBuddyResult() {
//		mPersonLayout.setVisibility(View.VISIBLE);
//		mGroupLayout.setVisibility(View.GONE);
//		imgThumbNail.setBackgroundDrawable(null);
//        if (null == buddy || Utils.isNullOrEmpty(buddy.userID)) {
//            imgThumbNail.setVisibility(View.GONE);
//            btnAdd.setVisibility(View.GONE);
//            framePhoto.setVisibility(View.GONE);
//            txtPersonName.setText(R.string.contacts_not_found);
//        } else {
//            framePhoto.setVisibility(View.VISIBLE);
//            imgThumbNail.setVisibility(View.VISIBLE);
//            PhotoDisplayHelper.displayPhoto(this, imgThumbNail, R.drawable.default_avatar_90, buddy, true);
//            txtPersonName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
//            btnAdd.setVisibility(mAllowAdd ? View.VISIBLE : View.INVISIBLE);
//        }
        lvBuddy.setAdapter(buddyAdapter = new BuddySearchItemAdapter(this,searchedBuddyList,edtSearchContent.getText().toString().trim(),mMsgBox));
        setSearchResultStatus();
	}
	
	private void fStartSearch() {
        if(TextUtils.isEmpty(edtSearchContent.getText().toString())) {
            mMsgBox.toast(getString(R.string.alert_search_target_empty));
            return;
        }

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(edtSearchContent.getWindowToken(), 0);
//        if (searchType == REQ_NORMAL_ACCOUNT) {
//            if (searchKind == GET_BUDDY_BY_WOWTALK_ID) {
//                searchBuddy();
//            } else {
//                searchGroup();
//            }
//        } else {
//            searchPublicAccount();
//        }
        if (searchKind == GET_BUDDY_BY_WOWTALK_ID) {
            searchBuddy();
        } else {
            searchGroup();
        }
	}

    private void setSearchResultStatus() {
        if (searchKind == GET_BUDDY_BY_WOWTALK_ID) {
            txtResultCount.setText(String.format(getResources().getString(R.string.search_result_count), searchedBuddyList.size()));
            lvBuddy.setVisibility(View.VISIBLE);
            lvGroups.setVisibility(View.GONE);
            if(searchedBuddyList.size() <= 0) {
                searchResultLayout.setVisibility(View.GONE);
            } else {
                searchResultLayout.setVisibility(View.VISIBLE);
            }
        } else {
            txtResultCount.setText(String.format(getResources().getString(R.string.search_result_count), searchedGroupRoomList.size()));
            lvBuddy.setVisibility(View.GONE);
            lvGroups.setVisibility(View.VISIBLE);
            if(searchedGroupRoomList.size() <= 0) {
                searchResultLayout.setVisibility(View.GONE);
            } else {
                searchResultLayout.setVisibility(View.VISIBLE);
            }
        }
    }
	
	private void setTitleMode() {
//        edtSearchContent.setText("");
		if (searchKind == GET_BUDDY_BY_WOWTALK_ID) {
            edtSearchContent.setText(buddySearchContent);

			btnSearchPerson.setBackgroundResource(R.drawable.tab_button_left_a);
			btnSearchGroup.setBackgroundResource(R.drawable.tab_button_right);
			btnSearchPerson.setTextColor(getResources().getColor(R.color.white));
			btnSearchGroup.setTextColor(getResources().getColor(R.color.login_input_font));
//			mPersonLayout.setVisibility(View.GONE);
//			mGroupLayout.setVisibility(View.GONE);

			edtSearchContent.setHint(R.string.contacts_search_buddy_hint);
		} else {
            edtSearchContent.setText(groupSearchContent);
			btnSearchPerson.setBackgroundResource(R.drawable.tab_button_left);
			btnSearchGroup.setBackgroundResource(R.drawable.tab_button_right_a);
			btnSearchPerson.setTextColor(getResources().getColor(R.color.login_input_font));
			btnSearchGroup.setTextColor(getResources().getColor(R.color.white));
//			mPersonLayout.setVisibility(View.GONE);
//			mGroupLayout.setVisibility(View.GONE);

			edtSearchContent.setHint(R.string.contacts_search_group_hint);
		}
        setSearchResultStatus();
	}
	
	private void initView() {
//		mPersonLayout = (RelativeLayout) findViewById(R.id.personLayout);
//		mGroupLayout = (RelativeLayout) findViewById(R.id.groupLayout);
//		framePhoto = (FrameLayout) findViewById(R.id.photo_frame);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
//        mLayoutButton = (LinearLayout) findViewById(R.id.layout_btn);
		btnSearchGroup = (Button) findViewById(R.id.search_group);
		btnSearchPerson = (Button) findViewById(R.id.search_person);
//        txtPublicSearch = (TextView) findViewById(R.id.public_search);
		btnSearch = (Button) findViewById(R.id.btn_search);
		edtSearchContent = (EditText) findViewById(R.id.edt_search);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);
//		txtPersonName = (TextView) findViewById(R.id.person_name_text);
		txtResultCount = (TextView) findViewById(R.id.txt_count);
//		btnAdd = (Button) findViewById(R.id.btn_add);
//		imgThumbNail = (ImageView) findViewById(R.id.person_photo);
        searchResultLayout=(LinearLayout) findViewById(R.id.search_result_layout);
        lvBuddy=(ListView) findViewById(R.id.list_buddy_find);
		lvGroups = (ListView) findViewById(R.id.list_group_find);

//        if (searchType == REQ_NORMAL_ACCOUNT) {
//            mLayoutButton.setVisibility(View.VISIBLE);
//            txtPublicSearch.setVisibility(View.GONE);
//        } else {
//            mLayoutButton.setVisibility(View.GONE);
//            txtPublicSearch.setVisibility(View.VISIBLE);
//        }
        edtSearchContent.addTextChangedListener(textWatcher);
        fieldClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                edtSearchContent.setText("");
            }
        });
        edtSearchContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    fStartSearch();
                    return true;
                }
                return false;
            }
        });

//		framePhoto.setOnClickListener(this);
		btnTitleBack.setOnClickListener(this);
		btnSearchGroup.setOnClickListener(this);
		btnSearchPerson.setOnClickListener(this);
		btnSearch.setOnClickListener(this);
//		btnAdd.setOnClickListener(this);
		
		lvGroups.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, final int position,
					long id) {
				if(searchedGroupRoomList == null || position < 0 || position >= searchedGroupRoomList.size())
					return;
				final GroupChatRoom g = searchedGroupRoomList.get(position);
				if(g == null)
					return;

                // judge whether the owner belongs to the group.
                mMsgBox.showWait();
                final String groupID = searchedGroupRoomList.get(position).groupID;
                final String memberId = mPrefUtil.getUid();
                new AsyncTask<Void, Void, Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        boolean isBelongsToGroup = mWebif.fIsBelongsToGroup(memberId, groupID);
                        Log.d("search acitivity, the member " + memberId + " belongs to the group " + groupID + "(" + isBelongsToGroup + ")");
                        return isBelongsToGroup;
                    }

                    protected void onPostExecute(Boolean isBelongsToGroup) {
                        mMsgBox.dismissWait();
                        final BottomButtonBoard bottomBtnBoard = new BottomButtonBoard(ContactSearchActivity.this, findViewById(R.id.layout));
                        String menuItem = null;
                        OnClickListener listener = null;
                        if (isBelongsToGroup){
                            menuItem = getString(R.string.group_chat);
                            listener = new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    MessageComposerActivity.launchToChatWithGroup(ContactSearchActivity.this, MessageComposerActivity.class, groupID);
                                }
                            };
                        } else {
                            menuItem = getString(R.string.group_sure_to_join);
                            listener = new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    bottomBtnBoard.dismiss();
                                    showInputMessageDlg(g);
                                }
                            };
                        }

                        bottomBtnBoard.add(menuItem, BottomButtonBoard.BUTTON_BLUE, listener);
                        bottomBtnBoard.addCancelBtn(getString(R.string.cancel));
                        bottomBtnBoard.show();
                    };
                }.execute((Void)null);
            }
        });

        lvBuddy.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int mFriendType = ContactInfoActivity.BUDDY_TYPE_UNKNOWN;
                Buddy buddy=searchedBuddyList.get(i);

                if (!TextUtils.isEmpty(buddy.userID)) {
                    if (buddy.userID.equals(mPrefUtil.getUid())) {
                        mFriendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
                    } else {
                        mFriendType = (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE))
                                ? ContactInfoActivity.BUDDY_TYPE_IS_FRIEND
                                : ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
                    }
                }
                ContactInfoActivity.launch(ContactSearchActivity.this,
                        Person.fromBuddy(buddy),
                        mFriendType);
            }
        });
    }

    private void showInputMessageDlg(final GroupChatRoom g) {
        final EditText edtText = new EditText(ContactSearchActivity.this);
        new AlertDialog.Builder(ContactSearchActivity.this)
                .setTitle(getString(R.string.group_join_authentication_msg))
                .setView(edtText)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMsgBox.showWait();
                        new AsyncTask<GroupChatRoom, Integer, Void>(){
                            int errno = ErrorCode.OK;
                            PendingRequest pr;

                            @Override
                            protected Void doInBackground(GroupChatRoom... arg0) {
                                GroupChatRoom g = arg0[0];

                                errno = mWebif.fGroupChat_AskForJoining(g.groupID, edtText.getText().toString());
                                if (ErrorCode.OK == errno) {
                                    pr = new PendingRequest();
                                    pr.group_id = g.groupID;
                                    pr.group_name = g.getDisplayName();
                                    pr.group_photo_timestamp = g.getPhotoUploadedTimestamp();
                                    pr.uid = mPrefUtil.getUid();
                                    pr.nickname = mPrefUtil.getMyNickName();
                                    pr.buddy_photo_timestamp = mPrefUtil.getMyPhotoUploadedTimestamp();
                                    pr.type = PendingRequest.GROUP_OUT;
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void v) {
                                mMsgBox.dismissWait();
                                if(errno == ErrorCode.OK) {
                                    mMsgBox.toast(R.string.done);
                                    mDbHelper.storePendingRequest(pr);
                                } else {
                                    mMsgBox.show(null, getString(R.string.operation_failed));
                                }
                            }
                        }.execute(g);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

    }

	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
//		case R.id.photo_frame:
//            if (null != buddy && !Utils.isNullOrEmpty(buddy.userID)) {
//                if (buddy.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
//                    Intent intent = new Intent(ContactSearchActivity.this, PublicAccountDetailActivity.class);
//                    intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, Person.fromBuddy(buddy));
//                    startActivity(intent);
//                } else {
//                    ContactInfoActivity.launch(ContactSearchActivity.this,
//                            Person.fromBuddy(buddy),
//                            mFriendType);
//                }
//            }
//			break;
		case R.id.search_group:
			searchKind = GET_GROUP_BY_NAME;
			setTitleMode();
			break;
		case R.id.search_person:
			searchKind = GET_BUDDY_BY_WOWTALK_ID;
			setTitleMode();
			break;
		case R.id.btn_search:
			fStartSearch();
			break;
//		case R.id.btn_add:
//            onAddFriendPressed();
//            break;
		default:
			break;
		}
	}

//    private void onAddFriendPressed() {
//        if(!mAllowAdd) {
//            mMsgBox.show(null, getString(R.string.you_are_already_friends));
//        } else {
//            mMsgBox.showWait();
//
//            new AsyncTask<Void, Void, Integer>() {
//                @Override
//                protected Integer doInBackground(Void... params) {
//                    int errno = WowTalkWebServerIF.getInstance(ContactSearchActivity.this).fAddBuddy(buddy.userID);
//                    return errno;
//                }
//                @Override
//                protected void onPostExecute(Integer s) {
//                    mMsgBox.dismissWait();
//                    if(s == ErrorCode.OK) {
//                        mAllowAdd = false;
//                        fShowBuddyResult(); // update UI
//                        mDbHelper.fetchBuddyDetail(buddy);
//                        if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
//                            mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
//                        } else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
//                            mMsgBox.show(null, getString(R.string.contacts_add_buddy_pending_out));
//                        }
//                    } else if (s == ErrorCode.ERR_OPERATION_DENIED){
//                        mMsgBox.show(null, getString(R.string.contactinfo_add_friend_denied));
//                    } else {
//                        mMsgBox.show(null, getString(R.string.operation_failed));
//                    }
//                }
//            }.execute((Void)null);
//        }
//    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_search);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

//        searchType = getIntent().getIntExtra(EXTRA_SEARCH, REQ_NORMAL_ACCOUNT);
		initView();
		setTitleMode();

        mMsgBox = new MessageBox(this);
		mWebif = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
		mDbHelper = new Database(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);
	}

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

//    public static void launchPublicSearch(Activity activity) {
//        Intent intent = new Intent(activity, ContactSearchActivity.class);
//        intent.putExtra(EXTRA_SEARCH, REQ_PUBLIC_ACCOUNT);
//        activity.startActivity(intent);
//    }
//
//
//    public static void launchBuddySearch(Activity activity) {
//        Intent intent = new Intent(activity, ContactSearchActivity.class);
//        intent.putExtra(EXTRA_SEARCH, REQ_NORMAL_ACCOUNT);
//        activity.startActivity(intent);
//    }

}
