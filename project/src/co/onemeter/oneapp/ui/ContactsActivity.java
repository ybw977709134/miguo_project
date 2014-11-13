package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.FamilyContactAdapter;
import co.onemeter.oneapp.contacts.adapter.*;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.contacts.util.ContactUtil;

import java.util.*;

public class ContactsActivity extends Activity implements OnClickListener,
					SideBar.OnTouchingLetterChangedListener{
	private static final int REQ_LAUNCH_GROUP = 123;

	/**
	 * finish invoked "getMyGroupsFromLocal()".
	 */
	private static final int HANDLER_LOCAL_GROUPS_LOADED = 1;

	/**
	 * finish invoked "getMyBuddyListFromLocal()".
	 */
	private static final int HANDLER_LOCAL_BUDDIES_LOADED = 2;

	private ImageButton btnTitleAdd;
	private TextView txtTitleCurrentGroup;
	private EditText etSearch;
    private ImageButton fieldClear;
	
	private SideBar mSideBar;

    private ListView lvFunction;
	private ListView lvContacts;
	private ListView lvContactGroup;

	private RelativeLayout mNavBar;
    private ScrollView mMainScrollView;
    private LinearLayout mMainLinearLayout;

	private LinearLayout mLocalLayout;
    private View mNewFriendsLayout;
    private LinearLayout mPublicAccount;

	private ContactListAdapter contactAdapter;
	private ContactGroupAdapter groupsAdapter;
    private FunctionAdapter fucAdapter;
	private ArrayList<GroupChatRoom> groupRooms;

	private WowTalkWebServerIF mWebif = null;
    private PrefUtil mPrefUtil;

    private MessageBox mMsgBox;

    
    private static ContactsActivity theActiveInstance;
    private BottomButtonBoard bottomBoard;

	/**
	 * time stamp, the time we read group list from local db.
	 */
	private static long mLastReadLocalGroupList = 0;

	private TextWatcher searchWacher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}
		
		@Override
		public void afterTextChanged(Editable s) {
            if(contactAdapter == null)
                return;

            if (s.length() == 0) {
                fieldClear.setVisibility(View.GONE);
                mSideBar.setVisibility(View.VISIBLE);
                mLocalLayout.setVisibility(View.GONE);
                mPublicAccount.setVisibility(View.GONE);
                contactAdapter.setPersonSource(ContactUtil.allPersons);
                contactAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(lvContacts);
            } else {
                fieldClear.setVisibility(View.VISIBLE);
                mSideBar.setVisibility(View.GONE);
                mLocalLayout.setVisibility(View.GONE);
                mPublicAccount.setVisibility(View.GONE);
                contactAdapter.getFilter().filter(s.toString(), new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int i) {
                        ListHeightUtil.setListHeight(lvContacts);
                    }
                });
            }
		}
	};

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_LOCAL_GROUPS_LOADED:
                    if (groupRooms == null || groupRooms.size() == 0) {
                        findViewById(R.id.group_indicator).setVisibility(View.GONE);
                        lvContactGroup.setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.group_indicator).setVisibility(View.VISIBLE);
                        lvContactGroup.setVisibility(View.VISIBLE);
                    }

                    groupsAdapter = new ContactGroupAdapter(ContactsActivity.this, groupRooms);
                    lvContactGroup.setAdapter(groupsAdapter);
                    ListHeightUtil.setListHeight(lvContactGroup);
                    lvContactGroup.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {
                            GroupChatRoom g = groupRooms.get(position);
                            if (g.isTemporaryGroup) {
                                MessageComposerActivity.launchToChatWithGroup(
                                        ContactsActivity.this, MessageComposerActivity.class, g);
                            } else {
                                ContactGroupInfoActivity.launchForResult(ContactsActivity.this,
                                        g.groupID, REQ_LAUNCH_GROUP);
                            }
                        }
                    });
                    break;
                case HANDLER_LOCAL_BUDDIES_LOADED:
                    try {
                        // java.lang.IllegalStateException:
                        // Observer android.widget.AdapterView$AdapterDataSetObserver@406e50e0 was not registered.
                        lvContacts.setAdapter(contactAdapter);
                        ListHeightUtil.setListHeight(lvContacts);
                        lvContacts.setOnItemClickListener(new OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View v, int position,
                                                    long id) {
                                if (contactAdapter == null)
                                    return;
                                ArrayList<Person> persons = contactAdapter.getPersonSource();
                                if (persons == null || persons.size() <= position)
                                    return;
                                if (persons.get(position).getAccountType() == Person.ACCOUNT_TYPE_PUBLIC) {
                                    Intent intent = new Intent(ContactsActivity.this, PublicAccountDetailActivity.class);
                                    intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, ContactUtil.allPersons.get(position));
                                    startActivity(intent);
                                } else {
                                    ContactInfoActivity.launch(
                                            ContactsActivity.this,
                                            persons.get(position),
                                            ContactInfoActivity.BUDDY_TYPE_IS_FRIEND);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        };
    };

	private void initView() {
		mNavBar = (RelativeLayout) findViewById(R.id.title_bar);
        mMainScrollView = (ScrollView)findViewById(R.id.main_scroll_view);
        mMainLinearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);
		btnTitleAdd = (ImageButton) findViewById(R.id.contact_add);
		txtTitleCurrentGroup = (TextView) findViewById(R.id.contact_current_group);
		etSearch = (EditText) findViewById(R.id.edt_search);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);
		mSideBar = (SideBar) findViewById(R.id.side_bar);

        lvFunction = (ListView) findViewById(R.id.function_list);
		lvContacts = (ListView) findViewById(R.id.contact_detail);
		lvContactGroup = (ListView) findViewById(R.id.group_list);
		mLocalLayout = (LinearLayout) findViewById(R.id.layout_local);
        mNewFriendsLayout =  findViewById(R.id.layout_new_friends);
        mPublicAccount = (LinearLayout) findViewById(R.id.layout_public_account);

        mNavBar.setOnClickListener(this);
		btnTitleAdd.setOnClickListener(this);
		txtTitleCurrentGroup.setOnClickListener(this);
		mSideBar.setOnTouchingLetterChangedListener(this);
		mLocalLayout.setOnClickListener(this);
        mNewFriendsLayout.setOnClickListener(this);
        mPublicAccount.setOnClickListener(this);
		etSearch.addTextChangedListener(searchWacher);
        fieldClear.setOnClickListener(this);
	}

    /**
     * Show or update function list.
     */
    public void showFunctionList() {
        HashMap<String, Integer> map;
        ArrayList<HashMap<String, Integer>> list = new ArrayList<HashMap<String, Integer>>();

        // new_friends
        //count pending requests
        ArrayList<PendingRequest> pendingRequests=new ArrayList<PendingRequest>();
        Database.open(this).fetchPendingRequest(pendingRequests);
        int pendingRequestCountIn=0;
        if(pendingRequests.size() > 0) {
            for(PendingRequest p : pendingRequests) {
                if (p.type == PendingRequest.BUDDY_IN
                        || p.type == PendingRequest.GROUP_IN
                        || p.type == PendingRequest.GROUP_ADMIN) {
                    ++pendingRequestCountIn;
                    break;
                }
            }
        }

        map = new HashMap<String, Integer>();
        map.put("image", R.drawable.new_friends);
        map.put("text", R.string.contacts_new_friends);
        map.put("badge", pendingRequestCountIn);
        list.add(map);

        fucAdapter = new FunctionAdapter(this, list);
        lvFunction.setAdapter(fucAdapter);
        ListHeightUtil.setListHeight(lvFunction);
        final List<HashMap<String, Integer>> finalList = list;
        lvFunction.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (finalList.get(position).get("text") == R.string.contacts_new_friends) {
                    Intent newFriendsIntent = new Intent(ContactsActivity.this, NewFriendsActivity.class);
                    startActivity(newFriendsIntent);
                } else if (finalList.get(position).get("text") == R.string.contacts_discussion) {
                    Intent discussionIntent = new Intent(ContactsActivity.this, ContactDiscussionActivity.class);
                    startActivity(discussionIntent);
                }
            }
        });
    }

    private void refreshGroupAndBuddy() {
        mMsgBox.showWait();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                WowTalkWebServerIF.getInstance(ContactsActivity.this)
                        .fGroupChat_GetMyGroups();
                WowTalkWebServerIF.getInstance(ContactsActivity.this)
                        .fGetBuddyList();
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                mMsgBox.dismissWait();
            }
        }.execute((Void)null);
    }
	
	private void getMyBuddyListFromServer() {
		new AsyncTask<Void, Integer, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				if (ErrorCode.OK == mWebif.fGetBuddyList()) {
                    WowMomentWebServerIF.getInstance(ContactsActivity.this)
                            .fGetMomentsOfAll(0, 10, true);
                    mPrefUtil.setContactUptodate(true);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
                mPrefUtil.setLocalContactListLastModified();
				getMyBuddyListFromLocal();
			}
		}.execute((Void)null);
	}

    //normal buddy
    private void getMyBuddyListFromLocal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                contactAdapter = new ContactListAdapter(ContactsActivity.this);
                mHandler.sendEmptyMessage(HANDLER_LOCAL_BUDDIES_LOADED);
            }
        }).start();
	}

	private void getMyGroupsFromServer() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				final int errno = WowTalkWebServerIF.getInstance(ContactsActivity.this).fGroupChat_GetMyGroups();
				if (errno == ErrorCode.OK) {
					mPrefUtil.setLocalGroupListLastModified();
//					mHandler.sendEmptyMessage(HANDLER_FINISH_GET_SERVER_GROUP);
				}
			}
			
		}).start();
	}

	private void getMyGroupsFromLocal() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                groupRooms = new ArrayList<GroupChatRoom>();
                final Database dbHelper = new Database(ContactsActivity.this);
                groupRooms = dbHelper.fetchNonTempGroupChatRooms();
                mLastReadLocalGroupList = new Date().getTime() / 1000;

                mHandler.sendEmptyMessage(HANDLER_LOCAL_GROUPS_LOADED);
            }
        }).start();
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.title_bar:
//                mMainScrollView.scrollTo(0, 0);
                mMainScrollView.smoothScrollTo(0, 0);
                break;
            case R.id.field_clear:
                etSearch.setText("");
                break;
            case R.id.contact_current_group:
                //fShowGroupMenu(v);
                break;
            case R.id.contact_add:
                EditText etSearchBar=(EditText)findViewById(R.id.edt_search);
                if(etSearchBar.hasFocus()) {
                    etSearchBar.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearchBar.getWindowToken(),0);
                }
                bottomBoard.show();
                break;
            case R.id.layout_local:
                Intent localIntent = new Intent(ContactsActivity.this, LocalContactsActivity.class);
                startActivity(localIntent);
                break;
            case R.id.layout_new_friends:
                Intent newFriendsIntent = new Intent(ContactsActivity.this, NewFriendsActivity.class);
                startActivity(newFriendsIntent);
                break;
            case R.id.layout_public_account:
                Intent publicAccountIntent = new Intent(ContactsActivity.this, PublicAccountActivity.class);
                startActivity(publicAccountIntent);
            default:
                break;
        }
	}
	
	@Override
	public void onTouchingLetterChanged(String s) {
        if(mMsgBox == null)
            mMsgBox = new MessageBox(this);
        if (s.equals("Se")) {
            mMsgBox.toast(R.string.search_toast, 0);
            mMainScrollView.smoothScrollTo(0, 0);
        } else {
            mMsgBox.toast(s, 0);

            if (contactAdapter == null)
                return;
            ArrayList<Person> persons = contactAdapter.getPersonSource();
            if (persons == null || persons.isEmpty())
                return;

            int height = 0;
            for (int i = 0; i < mMainLinearLayout.getChildCount() - 1; i++) {
                View view = mMainLinearLayout.getChildAt(i);
                if (view != null) {
                    height += view.getHeight();
                }
            }
            int numCount = 0;
            for (Person person : persons) {
                if (person.getSortKey().substring(0, 1).toUpperCase().equals(s)) {
                    mMainScrollView.smoothScrollTo(0, height);
                    break;
                }
                View view = lvContacts.getChildAt(numCount);
                if (view != null) {
                    height += view.getHeight();
                    numCount++;
                }
            }
        }
	}

    @Override
    public void onTouchCanceled() {
        if (mMsgBox != null) {
            mMsgBox.dismissToast();
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mPrefUtil = PrefUtil.getInstance(this);
		mWebif = WowTalkWebServerIF.getInstance(this);
		if(savedInstanceState != null) {
			mLastReadLocalGroupList = savedInstanceState.getLong("mLastReadLocalGroupList");
		}

        bottomBoard = new BottomButtonBoard(this,
                findViewById(R.id.contact_add));
        bottomBoard.add(getString(R.string.friends_add),
                BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent addIntent = new Intent(
                                ContactsActivity.this,
                                ContactAddActivity.class);
                        startActivity(addIntent);
                        bottomBoard.dismiss();
                    }
                });
        bottomBoard.add(getString(R.string.refresh_from_server),
                BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomBoard.dismiss();
                        refreshGroupAndBuddy();
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.close));

        initView();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putLong("mLastReadLocalGroupList", mLastReadLocalGroupList);
	}

    private IDBTableChangeListener contactBuddyObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMyBuddyListFromLocal();
                }
            });
        }
    };
    private IDBTableChangeListener contactGroupObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMyGroupsFromLocal();
                }
            });

        }
    };

    private IDBTableChangeListener pendingRequestObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showFunctionList();
                }
            });
        }
    };
	
	@Override
	protected void onResume() {
		super.onResume();
        theActiveInstance = this;
        MobclickAgent.onResume(this);

        invalidateListView();

        getMyGroupsFromLocal();
        getMyBuddyListFromLocal();
        Database.addDBTableChangeListener(Database.TBL_BUDDIES,contactBuddyObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP,contactGroupObserver);
        Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,pendingRequestObserver);
	}

    /**
     * Invalidate list view, forcing to reload it.
     *
     * Note, call me on UI thread.
     */
    public void invalidateListView() {

//        if (mLastReadLocalContactList < mPrefUtil.getLocalContactListLastModified()) {
//            getMyBuddyListFromLocal();
//        }

		if(!mPrefUtil.isContactUptodate()) {
			getMyBuddyListFromServer();
		}

		if(!mPrefUtil.isGroupUptodate()) {
			getMyGroupsFromServer();
		} else if (mLastReadLocalGroupList < mPrefUtil.getLocalGroupListLastModified()) {
//			getMyGroupsFromLocal();
		}

        showFunctionList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        theActiveInstance = null;
        MobclickAgent.onPause(this);

        Database.removeDBTableChangeListener(contactBuddyObserver);
        Database.removeDBTableChangeListener(contactGroupObserver);
        Database.removeDBTableChangeListener(pendingRequestObserver);
    }

    @Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(REQ_LAUNCH_GROUP == requestCode && RESULT_OK == resultCode) {
			// group has changed or deleted?
			Bundle b = data.getExtras();
			if(b.getBoolean(ContactGroupInfoActivity.EXTRA_GROUP_DELETED)) {
				getMyGroupsFromLocal();
			} else if(b.getBoolean(ContactGroupInfoActivity.EXTRA_PHOTO_CHANGED)) {
				// first update photo uploaded timestamp, then refresh list view. 
				String gid = b.getString(ContactGroupInfoActivity.EXTRA_GROUP_ID);
			    if(gid != null && groupsAdapter != null) {
                    for(GroupChatRoom g : groupRooms) {
                        if(gid.equals(g.groupID)) {
                            g.setPhotoUploadedTimestamp(new Date().getTime() / 1000);
                            groupsAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
			}
		}
	}

    /**
     * Get the active instance.
     * @return
     */
    public static ContactsActivity instance() {
        return theActiveInstance;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() ==   KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            handleBackEvent();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    private void handleBackEvent() {
        if (etSearch.length() != 0) {
            etSearch.setText("");
        }
    }
}
