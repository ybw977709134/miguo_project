package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactGroupAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;
import co.onemeter.oneapp.contacts.adapter.FunctionAdapter;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import co.onemeter.oneapp.utils.WebServerEventPoller;
import co.onemeter.utils.AsyncTaskExecutor;


import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 显示我的联系人。
 */
public class ContactsFragment extends Fragment implements OnClickListener,
        SideBar.OnTouchingLetterChangedListener,
        BottomButtonBoard.OptionsMenuProvider {
	private static final int REQ_LAUNCH_GROUP = 123;

	/**
	 * finish invoked "getMyGroupsFromLocal()".
	 */
	private static final int HANDLER_LOCAL_GROUPS_LOADED = 1;

	/**
	 * finish invoked "getMyBuddyListFromLocal()".
	 */
	private static final int HANDLER_LOCAL_BUDDIES_LOADED = 2;

    private View contentView;

	private EditText etSearch;
    private ImageButton fieldClear;
	
	private SideBar mSideBar;

    private ListView lvFunction;
	private ListView lvContacts;
	private ListView lvContactGroup;
	
	private TextView group_indicator;

    private ScrollView mMainScrollView;
    private LinearLayout mMainLinearLayout;

	private ContactListAdapter contactAdapter;
	private ContactGroupAdapter groupsAdapter;
    private FunctionAdapter fucAdapter;
	private ArrayList<GroupChatRoom> groupRooms;

	private WowTalkWebServerIF mWebif = null;
    private PrefUtil mPrefUtil;

    private MessageBox mMsgBox;

    private static ContactsFragment theActiveInstance;

	/**
	 * time stamp, the time we read group list from local db.
	 */
	private static long mLastReadLocalGroupList = 0;
	private int errno = 0;

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
                contactAdapter.setPersonSource(ContactUtil.allPersons);
                contactAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(lvContacts);
                lvContactGroup.setVisibility(View.VISIBLE);
                lvFunction.setVisibility(View.VISIBLE);
                group_indicator.setVisibility(View.VISIBLE);
            } else {
                fieldClear.setVisibility(View.VISIBLE);
                mSideBar.setVisibility(View.GONE);
                lvContactGroup.setVisibility(View.GONE);
                lvFunction.setVisibility(View.GONE);
                group_indicator.setVisibility(View.GONE);
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
                case HANDLER_LOCAL_GROUPS_LOADED://群组关系

                    if (getActivity() == null)
                        break;

                    if (groupRooms == null || groupRooms.size() == 0) {
                    	group_indicator.setVisibility(View.GONE);
                        lvContactGroup.setVisibility(View.GONE);
                    } else {
                    	group_indicator.setVisibility(View.VISIBLE);
                        lvContactGroup.setVisibility(View.VISIBLE);
                    }

                    groupsAdapter = new ContactGroupAdapter(getActivity(), groupRooms);
                    lvContactGroup.setAdapter(groupsAdapter);
                    ListHeightUtil.setListHeight(lvContactGroup);
                    lvContactGroup.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {
                            GroupChatRoom g = groupRooms.get(position);
                            if (g.isTemporaryGroup) {
                                MessageComposerActivity.launchToChatWithGroup(
                                        getActivity(), MessageComposerActivity.class, g);
                            } else {
                                ContactGroupInfoActivity.launchForResult(getActivity(),
                                        g.groupID, REQ_LAUNCH_GROUP);
                            }
                        }
                    });
                    break;
                case HANDLER_LOCAL_BUDDIES_LOADED://小伙伴，好友
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
                                if (persons.get(position).getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {//跳转到公共账号
                                    Intent intent = new Intent(getActivity(), PublicAccountDetailActivity.class);
                                    intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, ContactUtil.allPersons.get(position));
                                    startActivity(intent);
                                } else {
                                    ContactInfoActivity.launch(//跳转到好友的详情页
                                            getActivity(),
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
        }
    };

    private void initView() {
        mMainScrollView = (ScrollView)contentView.findViewById(R.id.main_scroll_view);
        mMainLinearLayout = (LinearLayout) contentView.findViewById(R.id.main_linear_layout);
		etSearch = (EditText) contentView.findViewById(R.id.edt_search);
        fieldClear = (ImageButton) contentView.findViewById(R.id.field_clear);
		mSideBar = (SideBar) contentView.findViewById(R.id.side_bar);

        lvFunction = (ListView) contentView.findViewById(R.id.function_list);
		lvContacts = (ListView) contentView.findViewById(R.id.contact_detail);
		lvContactGroup = (ListView) contentView.findViewById(R.id.group_list);
		
		group_indicator = (TextView) contentView.findViewById(R.id.group_indicator);

		mSideBar.setOnTouchingLetterChangedListener(this);
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
        Database.open(getActivity()).fetchPendingRequest(pendingRequests);
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
        map.put("image", R.drawable.icon_contact_add);
        map.put("text", R.string.contacts_new_friends);
        map.put("badge", pendingRequestCountIn);
        list.add(map);

        map = new HashMap<String, Integer>();
        map.put("image", R.drawable.official_icon);
        map.put("text", R.string.offical_account);
        map.put("badge", 0);
        list.add(map);

        fucAdapter = new FunctionAdapter(getActivity(), list);
        lvFunction.setAdapter(fucAdapter);
        ListHeightUtil.setListHeight(lvFunction);
        final List<HashMap<String, Integer>> finalList = list;
        lvFunction.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (finalList.get(position).get("text") == R.string.contacts_new_friends) {
                    Intent newFriendsIntent = new Intent(getActivity(), NewFriendsActivity.class);
                    startActivity(newFriendsIntent);
                } else if (finalList.get(position).get("text") == R.string.contacts_discussion) {
                    Intent discussionIntent = new Intent(getActivity(), ContactDiscussionActivity.class);
                    startActivity(discussionIntent);
                } else if (finalList.get(position).get("text") == R.string.offical_account) {
                    Intent discussionIntent = new Intent(getActivity(), PublicAccountActivity.class);
                    startActivity(discussionIntent);
                }
            }
        });
    }

    public void refresh() {
        if(mMsgBox == null)
            mMsgBox = new MessageBox(getActivity());

        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
            	WowTalkWebServerIF.getInstance(getActivity())
                        .fGroupChat_GetMyGroups();
            	errno = WowTalkWebServerIF.getInstance(getActivity())
                        .fGetBuddyList();
                WowTalkWebServerIF.getInstance(getActivity())
                        .fGetPendingRequests();
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (errno == ErrorCode.OK) {

                } else {
                	mMsgBox.toast(R.string.timeout_contacts_message);
                }
            }
        });
    }

    //normal buddy
    private void getMyBuddyListFromLocal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                contactAdapter = new ContactListAdapter(getActivity());
                mHandler.sendEmptyMessage(HANDLER_LOCAL_BUDDIES_LOADED);
            }
        }).start();
	}

    private void getMyGroupsFromLocal() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                groupRooms = new ArrayList<GroupChatRoom>();
                final Database dbHelper = new Database(getActivity());
                groupRooms = dbHelper.fetchNonTempGroupChatRooms();
                mLastReadLocalGroupList = new Date().getTime() / 1000;

                mHandler.sendEmptyMessage(HANDLER_LOCAL_GROUPS_LOADED);
            }
        }).start();
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.field_clear:
                etSearch.setText("");
                break;
            default:
                break;
        }
	}

    public void gotoTop() {
        mMainScrollView.smoothScrollTo(0, 0);
    }

    @Override
	public void onTouchingLetterChanged(String s) {
        if(mMsgBox == null)
            mMsgBox = new MessageBox(getActivity());
        if (s.equals("Se")) {
            mMsgBox.toast(R.string.search_toast, 0);
            gotoTop();
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
		contentView = inflater.inflate(R.layout.fragment_contacts, container, false);

        mMsgBox = new MessageBox(getActivity());
        mPrefUtil = PrefUtil.getInstance(getActivity());
		mWebif = WowTalkWebServerIF.getInstance(getActivity());
		if(savedInstanceState != null) {
			mLastReadLocalGroupList = savedInstanceState.getLong("mLastReadLocalGroupList");
		}

        initView();
        return contentView;
	}
	
	@Override
    public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putLong("mLastReadLocalGroupList", mLastReadLocalGroupList);
	}

    private IDBTableChangeListener contactBuddyObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMyBuddyListFromLocal();
                }
            });
        }
    };
    private IDBTableChangeListener contactGroupObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getMyGroupsFromLocal();
                }
            });

        }
    };

    private IDBTableChangeListener pendingRequestObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showFunctionList();
                }
            });
        }
    };
	
	@Override
    public void onResume() {
		super.onResume();
        theActiveInstance = this;

        getMyGroupsFromLocal();
        getMyBuddyListFromLocal();
        showFunctionList();

        Database.addDBTableChangeListener(Database.TBL_BUDDIES,contactBuddyObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP,contactGroupObserver);
        Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,pendingRequestObserver);

        // 如果有发出的好友请求，则总是频繁刷新好友列表，因为我们不知道对方什么时候同意，
        // 为了让用户被同意后即时看到新好友，只能如此。
        if (new Database(getActivity()).hasPendingOutBuddies()) {
            WebServerEventPoller.instance(getActivity()).invokeImmediately(
                    WebServerEventPoller.TASK_ID_REFRESH_BUDDIES);
        }
	}

    @Override
    public void onPause() {
        super.onPause();
        theActiveInstance = null;

        Database.removeDBTableChangeListener(contactBuddyObserver);
        Database.removeDBTableChangeListener(contactGroupObserver);
        Database.removeDBTableChangeListener(pendingRequestObserver);
    } 

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(REQ_LAUNCH_GROUP == requestCode && Activity.RESULT_OK == resultCode) {
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
    public static ContactsFragment instance() {
        return theActiveInstance;
    }

    public boolean handleBackPress() {
        if(etSearch.hasFocus()) {
            etSearch.clearFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(),0);
        }

        if (etSearch.length() != 0) {
            etSearch.setText("");
            return true;
        }

        return false;
    }

    @Override
    public String[] getOptionsMenuItems(Context context) {
        return new String[] {
                context.getString(R.string.friends_add),
                context.getString(R.string.refresh_from_server) };
    }

    @Override
    public int[] getOptionsMenuItemIcons(Context context) {
        return new int[] { R.drawable.nav_refresh_selector, R.drawable.sel_topbar_add_contact};
    }

    @Override
    public boolean onOptionsItemSelected(int position) {
        switch (position) {
            case 1:
                Intent addIntent = new Intent(
                        getActivity(),
                        ContactAddActivity.class);
                startActivity(addIntent);
                return true;
            case 0:
                refresh();
                return true;
            default:
                return false;
        }
    }
}
