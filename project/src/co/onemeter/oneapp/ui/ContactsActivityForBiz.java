package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.*;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactGroupIterationAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;
import co.onemeter.oneapp.contacts.model.Person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContactsActivityForBiz extends Activity implements OnClickListener,
        SideBar.OnTouchingLetterChangedListener{
    /**
     * the groupId of latestContact, indicates the groupChatRoom is latestContact
     */
    public static final String GROUP_ID_OF_LATEST_CONTACT = "-1";

    /**
     * 每次加载的条数
     */
    private static final int LOAD_COUNT_PER_TIME = 100;
    private static final int REQUEST_CODE_LAUNCH_BUDDY_DETAIL = 1001;

    private static final int HANDLER_STOP_REFRESH = 0;
    private static final int HANDLER_GET_GROUPS_LOCAL = 1;
    private static final int HANDLER_GET_FAVORITE_GROUP = 2;
    private static final int HANDLER_GET_CURRENT_GROUP = 3;
    private static final int HANDLER_GET_FREQUENT_CONTACTS = 4;
    private static final int HANDLER_GET_MEMBERS_OF_CURRENT_GROUP = 5;
    private static final int HANDLER_GET_SEARCHED_CONTACTS = 6;

    /**
     * 最开始的没有搜索框的联系人mode（常用联系人，当前群组的联系人）
     */
    private static final int DISPLAY_MODE_CONTACTS = 1;
    /**
     * 最开始的，有搜索框的联系人mode（常用联系人，当前群组的联系人）
     */
    private static final int DISPLAY_MODE_CONTACTS_WITH_SEARCH = 2;
    /**
     * 搜索框在最上面，且下面全部被灰色背景覆盖（灰色背景下：常用联系人，当前群组的联系人）
     */
    private static final int DISPLAY_MODE_SEARCHING = 3;
    /**
     * 搜索结果mode（与搜索匹配的当前群组的联系人）
     */
    private static final int DISPLAY_MODE_SEARCH_RESULT = 4;
    /**
     * 显示群组Frame，底部灰色背景（底部的联系人能显示，但不清新）
     */
    private static final int DISPLAY_MODE_GROUP = 5;

    private RelativeLayout mTitleBar;
    private ScrollView mMainScrollView;
    private LinearLayout mMainLinearLayout;
    private ImageButton mBtnTitleGroupEnter;
    private ImageButton btnTitleRefresh;
    private TextView txtTitleCurrentGroup;
    private FrameLayout mContentLayout;
    private RelativeLayout mSearchLayout;
    //	private RelativeLayout mSearchBarLayout;
    private EditText etSearch;
    private ImageButton fieldClear;
    private Button mCancelSearchBtn;
    private SideBar mSideBar;

    private ImageView mSearchGlassImageView;
    private RelativeLayout mContactsLayout;
    private RelativeLayout mFrequentContactsArrowLayout;
    private ImageView mFrequentContactsArrow;
    private LinearLayout mFrequentContactsLayout;
    private ListView mFrequentContactsListView;
    private ListView mContactsListView;
    private TextView mLoadMoreView;
    private LinearLayout mGroupsLayout;
    private ListView mGroupsListView;
    private ListView mSearchResultContactsListView;

    /**
     * 常用联系人
     */
    private ArrayList<Person> mFrequentContacts = new ArrayList<Person>();
    private ArrayList<Person> mContactsAll = new ArrayList<Person>();
    /**
     * 当前组下的所有的成员
     */
    private ArrayList<Person> mContacts = new ArrayList<Person>();
    private ArrayList<Person> mSearchedContacts = new ArrayList<Person>();
    private ArrayList<GroupChatRoom> mAllGroupRooms = new ArrayList<GroupChatRoom>();
    private ArrayList<GroupChatRoom> mFavoriteGroupRooms = new ArrayList<GroupChatRoom>();

    private ContactListAdapter mFrequentContactsAdapter;
    private ContactListAdapter contactsAdapter;
    private ContactGroupIterationAdapter mGroupIterationAdapter;
    private ContactListAdapter mSearchResultContactsAdapter;

    private WowTalkWebServerIF mWebif = null;
    private PrefUtil mPrefUtil;
    private Database mDbHelper;
    private MessageBox mMsgBox;


    private static ContactsActivityForBiz theActiveInstance;

    /**
     * whether it's refresh
     */
    private boolean mIsRefreshing;

    private boolean mIsLoadingMore;

    /**
     * mMainScrollView滑动按下的初始位置
     */
    private float mScrollDownY;

    /**
     * 当前显示的mdoe
     */
    private int mCurrentDisplayMode;
    private GroupChatRoom mCurrentChatRoom;
    private boolean mHasFilledCurrentChatRoom;

    private TextWatcher mSearchWacher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(mSearchResultContactsAdapter == null)
                return;

            if (s.length() == 0) {
                fieldClear.setVisibility(View.GONE);
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
            } else {
                fieldClear.setVisibility(View.VISIBLE);
                mSearchResultContactsAdapter.getFilter().filter(s.toString(), new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int i) {
                        if (mCurrentDisplayMode != DISPLAY_MODE_SEARCH_RESULT) {
                            changeDisplayMode(DISPLAY_MODE_SEARCH_RESULT);
                        }
                        ListHeightUtil.setListHeight(mSearchResultContactsListView);
                    }
                });
            }
        }
    };

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d("mOnTouchListener, MotionEvent.action is " + event.getAction());

            if (mCurrentDisplayMode != DISPLAY_MODE_SEARCHING
                    && mCurrentDisplayMode != DISPLAY_MODE_SEARCH_RESULT
                    && v.getId() == R.id.edt_search
                    && MotionEvent.ACTION_DOWN == event.getAction()) {
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
            }
            return false;
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_STOP_REFRESH:
                mIsRefreshing = false;
                btnTitleRefresh.clearAnimation();
                break;
            case HANDLER_GET_GROUPS_LOCAL:
                ArrayList<GroupChatRoom> rootGroups = (ArrayList<GroupChatRoom>) msg.obj;
                if (null == mGroupIterationAdapter) {
                    mGroupIterationAdapter = new ContactGroupIterationAdapter(ContactsActivityForBiz.this, rootGroups, mSelectedGroupListener);
                    mGroupsListView.setAdapter(mGroupIterationAdapter);
                } else {
                    mGroupIterationAdapter.setRootDataSource(rootGroups);
                    mGroupIterationAdapter.notifyDataSetChanged();
                }
                break;
            case HANDLER_GET_FAVORITE_GROUP:
                GroupChatRoom rootFavoriteChatRoom = (GroupChatRoom) msg.obj;
                // 不用新创建adapter，只是修改 收藏的群组
                if (null != mGroupIterationAdapter) {
                    mGroupIterationAdapter.setRootFavoriteGroupSource(rootFavoriteChatRoom);
                    mGroupIterationAdapter.notifyDataSetChanged();
                }
                break;
            case HANDLER_GET_CURRENT_GROUP:
                setDispalyGroupName();
                break;
            case HANDLER_GET_FREQUENT_CONTACTS:
                if (mFrequentContacts.isEmpty()) {
                    mFrequentContactsLayout.setVisibility(View.GONE);
                } else {
                    mFrequentContactsLayout.setVisibility(View.VISIBLE);
                    if (null == mFrequentContactsAdapter) {
                        mFrequentContactsAdapter = new ContactListAdapter(ContactsActivityForBiz.this, mFrequentContacts);
                        mFrequentContactsAdapter.setIsShowFirstChar(false);
                        mFrequentContactsListView.setAdapter(mFrequentContactsAdapter);
                    } else {
                        mFrequentContactsAdapter.setPersonSource(mFrequentContacts);
                        mFrequentContactsAdapter.notifyDataSetChanged();
                    }
                    ListHeightUtil.setListHeight(mFrequentContactsListView);
                    mFrequentContactsListView.setOnItemClickListener(new ContactOnItemClickListener(mFrequentContactsAdapter));
                }
                break;
            case HANDLER_GET_MEMBERS_OF_CURRENT_GROUP:
                if (mContacts.isEmpty()) {
                    mContactsListView.setVisibility(View.GONE);
                } else {
                    mContactsListView.setVisibility(View.VISIBLE);
                    if (null == contactsAdapter) {
                        contactsAdapter = new ContactListAdapter(ContactsActivityForBiz.this, mContacts);
                        mContactsListView.setAdapter(contactsAdapter);
                    } else {
                        contactsAdapter.setPersonSource(mContacts);
                        contactsAdapter.notifyDataSetChanged();
                    }
                    ListHeightUtil.setListHeight(mContactsListView);
                    mContactsListView.setOnItemClickListener(new ContactOnItemClickListener(contactsAdapter));
                }
                mIsLoadingMore = false;
                setLoadMoreView(false);
                break;
            case HANDLER_GET_SEARCHED_CONTACTS:
                if (null == mSearchResultContactsAdapter) {
                    mSearchResultContactsAdapter = new ContactListAdapter(ContactsActivityForBiz.this, mSearchedContacts);
                    mSearchResultContactsAdapter.setIsShowFirstChar(false);
                    mSearchResultContactsListView.setAdapter(mSearchResultContactsAdapter);
                } else {
                    mSearchResultContactsAdapter.setPersonSource(mSearchedContacts);
                    mSearchResultContactsAdapter.notifyDataSetChanged();
                }
                mSearchResultContactsListView.setOnItemClickListener(new ContactOnItemClickListener(mSearchResultContactsAdapter));
                break;
            default:
                break;
            }
        };
    };

    /**
     * 点击了某个群组，显示此群组下的所有成员(包括子部门)
     */
    private ContactGroupIterationAdapter.GroupNameClickedListener mSelectedGroupListener
            = new ContactGroupIterationAdapter.GroupNameClickedListener() {

        @Override
        public void onGroupNameClicked(GroupChatRoom chatRoom) {
            changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
            mCurrentChatRoom = chatRoom;
            setDispalyGroupName();

            mContactsListView.setVisibility(View.GONE);
            mIsLoadingMore = true;
            setLoadMoreView(true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    getContactsListFromLocal();
                    getSearchedContacts();
                }
            }).start();

            // 从服务器获取该部门及其子部门的成员
            // 登录的刷新群组和成员完成了，也是其中一个刷新的前提条件；如果登录后的刷新未完成，此时不需要刷新
//            if (mPrefUtil.isGroupMembersUptodatePerfectly() && GlobalValue.isNeedToRefreshMembers(chatRoom.groupID)) {
//                Log.i("ContactsActivityForBiz#mSelectedGroupListener#onGroupNameClicked, need to refresh from server.");
//                getContactsListFromServer(chatRoom.groupID);
//            }
        }

        @Override
        public void onSendMsgClicked() {
        }
    };

    private class ContactOnItemClickListener implements OnItemClickListener {

        private ContactListAdapter mAdapter;

        public ContactOnItemClickListener (ContactListAdapter adapter) {
            mAdapter = adapter;
        }
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (mAdapter == null)
                return;
            ArrayList<Person> persons = mAdapter.getPersonSource();
            if (persons == null || persons.size() <= position)
                return;
            Person person = persons.get(position);
            if (null != person) {
                String myUserId = PrefUtil.getInstance(ContactsActivityForBiz.this).getUid();
                int friendType = (!TextUtils.isEmpty(myUserId) && myUserId.equals(person.getID()))
                        ? ContactInfoActivity.BUDDY_TYPE_MYSELF
                        : ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
                ContactInfoActivity.launchForResult(
                        ContactsActivityForBiz.this,
                        REQUEST_CODE_LAUNCH_BUDDY_DETAIL,
                        person,
                        friendType);
            }
        }
    }

    /**
     * 禁止mMainScrollView滑动
     */
    private OnTouchListener mForbidScrollListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };

    /**
     * 恢复mMainScrollView滑动
     */
    private OnTouchListener mRecoverScrollListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    };

    private void initView() {
        mTitleBar = (RelativeLayout) findViewById(R.id.title_bar);
        mMainScrollView = (ScrollView)findViewById(R.id.main_scroll_view);
        mMainLinearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);
        mBtnTitleGroupEnter = (ImageButton) findViewById(R.id.group_enter);
        btnTitleRefresh = (ImageButton) findViewById(R.id.contact_refresh);
        txtTitleCurrentGroup = (TextView) findViewById(R.id.contact_current_group);
        mSearchLayout = (RelativeLayout) findViewById(R.id.search_layout);
        mContentLayout = (FrameLayout) findViewById(R.id.content_layout);
//		mSearchBarLayout = (RelativeLayout) findViewById(R.id.search_bar);
        etSearch = (EditText) findViewById(R.id.edt_search);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);
        mCancelSearchBtn = (Button) findViewById(R.id.cancel_btn);
        mSideBar = (SideBar) findViewById(R.id.side_bar);

        mSearchGlassImageView = (ImageView) findViewById(R.id.search_glass_img);
        mContactsLayout = (RelativeLayout) findViewById(R.id.layout_contacts);
        mFrequentContactsArrowLayout = (RelativeLayout) findViewById(R.id.frequent_contacts_arrow_layout);
        mFrequentContactsArrow = (ImageView) findViewById(R.id.frequent_contacts_arrow);
        mFrequentContactsLayout = (LinearLayout) findViewById(R.id.layout_frequent_contacts);
        mFrequentContactsListView = (ListView) findViewById(R.id.frequent_contacts);
        mContactsListView = (ListView) findViewById(R.id.contacts_list);
        mLoadMoreView = (TextView) findViewById(R.id.text);
        mGroupsLayout = (LinearLayout) findViewById(R.id.layout_groups);
        mGroupsListView = (ListView) findViewById(R.id.groups_iteration);
        mSearchResultContactsListView = (ListView) findViewById(R.id.layout_searched_contacts);

        mTitleBar.setOnClickListener(this);
        mBtnTitleGroupEnter.setOnClickListener(this);
        btnTitleRefresh.setOnClickListener(this);
        etSearch.addTextChangedListener(mSearchWacher);
        etSearch.setOnTouchListener(mOnTouchListener);
        fieldClear.setOnClickListener(this);
        mCancelSearchBtn.setOnClickListener(this);
        mSearchGlassImageView.setOnClickListener(this);
        mFrequentContactsArrowLayout.setOnClickListener(this);
        mFrequentContactsArrow.setOnClickListener(this);
        mGroupsLayout.setOnClickListener(this);
        mLoadMoreView.setOnClickListener(this);

        findViewById(R.id.title_txt_layout).setOnClickListener(this);

//		mSearchBarLayout.setOnClickListener(this);
        mSideBar.setOnTouchingLetterChangedListener(this);
//		mMainScrollView.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN){
//                    mScrollDownY = event.getY();
//                } else if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
//                    // TODO 此处判断条件， MotionEvent.ACTION_MOVE是否需要？
//
//                    android.util.Log.i("zjh", "scrollY=" + mMainScrollView.getScrollY() + ", newY="+ event.getY() + ", oldY=" + mScrollDownY);
//                    // 监听ScrollView的滚动事件
//                    if (mMainScrollView.getScrollY() == 0 && event.getY() - mScrollDownY > 50
//                            && mTitleBar.getVisibility() == View.VISIBLE) {
//                        changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
//                    }
//                }
//                return false;
//            }
//        });
    }

//    private void getContactsListFromServer(final String groupID) {
//        new AsyncTask<Void, Integer, Integer>() {
//            @Override
//            protected Integer doInBackground(Void... params) {
//                return mWebif.fGroupChat_GetMembersIteration(groupID);
//            }
//
//            @Override
//            protected void onPostExecute(Integer result) {
//                Log.i("ContactsActivityForBiz#getContactsListFromServer by groupId(" + groupID + "), resultCode is " + result);
//                // 服务器数据加载完成后，还是原先请求的时候的群组，则刷新界面；否则不做处理
//                if (result == ErrorCode.OK) {
//                    GlobalValue.setDontNeedToRefreshMembers(groupID);
//                    if (groupID.equals(mCurrentChatRoom.groupID)) {
//                        // 此处需要刷新收藏的联系人，因为可能原先收藏的某些联系人没有下载到本地
//                        getFrequentContacts();
//                        getContactsListFromLocal();
//                        getSearchedContacts();
//                    }
//                }
//            }
//        }.execute((Void)null);
//    }

    /**
     * 根据当前群组，查找成员，包括子部门的成员，迭代
     */
    private void getContactsListFromLocal() {
        // 当前群组为空，说明未下载到群组，属于异常情况。。。
        if (null == mCurrentChatRoom) {
            Log.e("ContactsActivityForBiz#getContactsListFromLocal, the current dept is null.");
            return;
        }
        ArrayList<GroupMember> members = new ArrayList<GroupMember>();
        // 获取当前群组下的所有成员，包括子部门
        // 此时返回的members可能存在重复的成员（同时属于多个部门）
//        mDbHelper.fetchGroupMembersIteration(mCurrentChatRoom, members);
        members = mDbHelper.fetchGroupMembers(mCurrentChatRoom.groupID);

        // 过滤重复的成员
//        ArrayList<String> addedIds = new ArrayList<String>();
//        mContactsAll = new ArrayList<Person>();
//        for (GroupMember groupMember : members) {
//            if (!addedIds.contains(groupMember.userID)) {
//                mContactsAll.add(Person.fromBuddy(groupMember));
//                addedIds.add(groupMember.userID);
//            }
//        }
        mContactsAll = new ArrayList<Person>();
        for (GroupMember groupMember : members) {
            mContactsAll.add(Person.fromBuddy(groupMember));
        }

        int endIndex = LOAD_COUNT_PER_TIME >= mContactsAll.size()
                ? mContactsAll.size() : LOAD_COUNT_PER_TIME;
        List<Person> subList = mContactsAll.subList(0, endIndex);
        mContacts = new ArrayList<Person>();
        mContacts.addAll(subList);
        mHandler.sendEmptyMessage(HANDLER_GET_MEMBERS_OF_CURRENT_GROUP);
    }

    private void loadMorePersons() {
        if (mIsLoadingMore || mIsRefreshing) {
            return;
        }
        mIsLoadingMore = true;
        setLoadMoreView(false);
        int start = mContacts.size();
        int end = start + LOAD_COUNT_PER_TIME >= mContactsAll.size()
                ? mContactsAll.size() : start + LOAD_COUNT_PER_TIME;
        List<Person> subList = mContactsAll.subList(start, end);
        mContacts.addAll(subList);
        mHandler.sendEmptyMessage(HANDLER_GET_MEMBERS_OF_CURRENT_GROUP);
    }

    /**
     * 第一次进入页面加载时，需要显示“正在加载”
     * @param isInit
     */
    private void setLoadMoreView(boolean isInit) {
        if (isInit || mContacts.size() < mContactsAll.size()) {
            mLoadMoreView.setVisibility(View.VISIBLE);
        } else {
            mLoadMoreView.setVisibility(View.GONE);
        }
        mLoadMoreView.setText(mIsLoadingMore ? R.string.loading_more_in_progress : R.string.load_more);
    }


    private void getSearchedContacts() {
        mSearchedContacts = new ArrayList<Person>();
        mSearchedContacts.addAll(mFrequentContacts);
        HashSet<String> searchedContactIds = new HashSet<String>();
        for (Person frequentPerson : mFrequentContacts) {
            searchedContactIds.add(frequentPerson.getID());
        }
        // 合并frequent 和 contacts
        for (Person person : mContacts) {
            if (!searchedContactIds.contains(person.getID())) {
                mSearchedContacts.add(person);
                searchedContactIds.add(person.getID());
            }
        }

        mHandler.sendEmptyMessage(HANDLER_GET_SEARCHED_CONTACTS);
    }

    private void getMyGroupsFromServer() {
        final WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(ContactsActivityForBiz.this);
        new AsyncTask<Void, Void, Integer> () {
            @Override
            protected Integer doInBackground(Void... params) {
                return webIF.getGroupsByCompanyId(PrefUtil.getInstance(ContactsActivityForBiz.this).getCompanyId());
            }

            protected void onPostExecute(Integer result) {
                Log.i("ContactsActivityForBiz#getMyGroupsFromServer, resultCode is " + result);
                // 重新获取群组，会重新刷新群组列表，导致展开的状态不正确，等待下一次再打开时显示
                if (result == ErrorCode.OK/* && mCurrentDisplayMode == DISPLAY_MODE_GROUP*/) {
                    GlobalValue.setNeedToRefreshAllGroups(false);
//                    getMyGroupsFromLocal();
                    // mPrefUtil.setLocalGroupListLastModified();
                    // mHandler.sendEmptyMessage(HANDLER_FINISH_GET_SERVER_GROUP);

                    // 判断是否有没有成员的群组，如果有，说明是新下载下来的群组，需要下载其成员，来判断自己是否属于该群组
                    final ArrayList<String> newGroupIds = mDbHelper.fetchAllGroupChatRoomsWithoutMembers();
                    if (!newGroupIds.isEmpty()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (String newGroupId : newGroupIds) {
                                    if (ErrorCode.OK == webIF.fGroupChat_GetMembersIteration(newGroupId)) {
                                        GlobalValue.setDontNeedToRefreshMembers(newGroupId);
                                    }
                                }
                            }
                        }).start();
                    }
                }
            };
        }.execute((Void)null);
    }

    /**
     * 获取所有群组，树形结构(根节点：收藏群组，一般群组)
     * @param isRefreshUI 子线程中不能刷新UI
     */
    private void getMyGroupsFromLocal(boolean isRefreshUI) {
        mFavoriteGroupRooms = mDbHelper.fetchFavoriteGroupChatRooms();
        mAllGroupRooms = mDbHelper.fetchNonTempGroupChatRooms(true);
        // 处理 mGroupRooms，为childGroups赋值
        ArrayList<GroupChatRoom> rootGroups = new ArrayList<GroupChatRoom>();
        rootGroups.add(treeFavoriteGroups(getString(R.string.contactsforbiz_root_favorite_group_name),mFavoriteGroupRooms));
        rootGroups.addAll(treeAllGroupRooms(ContactsActivityForBiz.this, mAllGroupRooms));

        if (isRefreshUI) {
            Message msg = mHandler.obtainMessage();
            msg.what = HANDLER_GET_GROUPS_LOCAL;
            msg.obj = rootGroups;
            mHandler.sendMessage(msg);
        }
    }

    /**
     * 构造收藏的群组树
     * @return
     */
    public static GroupChatRoom treeFavoriteGroups(String fgname,ArrayList<GroupChatRoom> rooms) {
        // 构造最外层的收藏的根群组
//        String rootRoomName = getString(R.string.contactsforbiz_root_favorite_group_name);
        GroupChatRoom rootChatRoom = new GroupChatRoom(ContactGroupIterationAdapter.GROUP_ID_ROOT,
                fgname, fgname, "", 0, 0, false);
        rootChatRoom.isEditable = false;
        rootChatRoom.parentGroupId = null;
        rootChatRoom.childGroups = new ArrayList<GroupChatRoom>();
        // 将收藏的群组的parentGroupId设置为构造的根群组的id
        for (GroupChatRoom favoriteGroup : rooms) {
            favoriteGroup.parentGroupId = ContactGroupIterationAdapter.GROUP_ID_ROOT;
            rootChatRoom.childGroups.add(favoriteGroup);
        }
        return rootChatRoom;
    }

    /**
     * 利用GroupChatRoom的parentGrupId属性，构造GroupChatRoom的树形结构
     */
    public static ArrayList<GroupChatRoom> treeAllGroupRooms(Context context, ArrayList<GroupChatRoom> mAllGroupRooms) {
        ArrayList<GroupChatRoom> rootGroups = new ArrayList<GroupChatRoom>();
        GroupChatRoom outerGroup = null;
        GroupChatRoom innerGroup = null;
        for (int i = 0; i < mAllGroupRooms.size(); i++) {
            outerGroup = mAllGroupRooms.get(i);
            if (null == outerGroup.childGroups) {
                outerGroup.childGroups = new ArrayList<GroupChatRoom>();
            }
            if (!outerGroup.isEditable && TextUtils.isEmpty(outerGroup.parentGroupId)) {
                outerGroup.groupNameOriginal = context.getString(R.string.contactsforbiz_root_group_name_display);
                rootGroups.add(outerGroup);
            }
            for (int j = i + 1; j < mAllGroupRooms.size(); j++) {
                innerGroup = mAllGroupRooms.get(j);
                // outerGroup可能是innerGroup的父节点；也可能是innerGroup的子节点
                if (!TextUtils.isEmpty(innerGroup.parentGroupId)
                        && innerGroup.parentGroupId.equals(outerGroup.groupID)) {
                    outerGroup.childGroups.add(innerGroup);
                } else if (!TextUtils.isEmpty(outerGroup.parentGroupId)
                        && outerGroup.parentGroupId.equals(innerGroup.groupID)) {
                    if (null == innerGroup.childGroups) {
                        innerGroup.childGroups = new ArrayList<GroupChatRoom>();
                    }
                    innerGroup.childGroups.add(outerGroup);
                }
            }
        }
        return rootGroups;
    }

//    protected void getGroupsAndMembersFormServer() {
//        mIsRefreshing = true;
//        long startLoadGroupsTime = System.currentTimeMillis();
//        Log.i("begin load groups and group members");
//        WowTalkWebServerIF.getInstance(ContactsActivityForBiz.this).fGroupChat_GetMyGroups();
//        mPrefUtil.setGroupUptodate(true);
//        long finishLoadGroupsTime = System.currentTimeMillis();
//        Log.i("Finished load groups coasts " + (finishLoadGroupsTime - startLoadGroupsTime));
//
//        mAllGroupRooms = mDbHelper.fetchNonTempGroupChatRooms();
//        mLastReadLocalGroupList = new Date().getTime() / 1000;
//        mHandler.sendEmptyMessage(HANDLER_LOAD_GROUPS_FINISHED);
//
//        List<GroupChatRoom> tempGroupRooms = new ArrayList<GroupChatRoom>();
//        tempGroupRooms.addAll(mAllGroupRooms);
//        WowTalkWebServerIF wif = WowTalkWebServerIF.getInstance(ContactsActivityForBiz.this);
//        for (GroupChatRoom groupChatRoom : tempGroupRooms) {
//            Map<String, Object> resultMap = wif.fGroupChat_GetMembers(groupChatRoom.groupID);
//            ArrayList<GroupMember> members = (ArrayList<GroupMember>) resultMap.get("data");
//            if(null != members) {
//                ArrayList<Buddy> buddies = new ArrayList<Buddy>();
//                for (GroupMember groupMember : members) {
//                    groupMember.setFriendshipWithMe(GroupMember.RELATIONSHIP_FRIEND_HERE);
//                    buddies.add(groupMember);
//                }
//                mDbHelper.storeBuddies(buddies);
//            }
//        }
//        long finishLoadMembersTime = System.currentTimeMillis();
//        Log.i("Finished load groupMembers costs " + (finishLoadMembersTime - finishLoadGroupsTime));
//        mHandler.sendEmptyMessage(HANDLER_LOAD_MEMBERS_FINISHED);
//        mIsRefreshing = false;
//    }

    /**
     * 切换至指定mode显示
     * @param mode DISPLAY_MODE_CONTACTS, DISPLAY_MODE_CONTACTS_WITH_SEARCH,
     *              DISPLAY_MODE_SEARCHING, DISPLAY_MODE_SEARCH_RESULT, DISPLAY_MODE_GROUP
     */
    private void changeDisplayMode(int mode) {
        if (mCurrentDisplayMode == DISPLAY_MODE_GROUP && null != mGroupIterationAdapter) {
            mGroupIterationAdapter.collapseGroups();
            mGroupIterationAdapter.notifyDataSetChanged();
        }
        switch (mode) {
            case DISPLAY_MODE_CONTACTS:
                etSearch.setText("");
                mTitleBar.setVisibility(View.VISIBLE);
                mSearchLayout.setVisibility(View.GONE);
                mContactsLayout.setVisibility(View.VISIBLE);
                mSearchResultContactsListView.setVisibility(View.GONE);
                mSearchGlassImageView.setVisibility(View.GONE);
                mGroupsLayout.setVisibility(View.GONE);
                mBtnTitleGroupEnter.setImageResource(R.drawable.nav_group_list_selector);
                etSearch.clearFocus();
                mMainScrollView.setOnTouchListener(mRecoverScrollListener);
                mSideBar.setVisibility(View.VISIBLE);
                break;
            case DISPLAY_MODE_CONTACTS_WITH_SEARCH:
                etSearch.setText("");
                //            if (mCurrentDisplayMode == DISPLAY_MODE_SEARCHING || mCurrentDisplayMode == DISPLAY_MODE_SEARCH_RESULT) {
                //                Animation animation = AnimationUtils.loadAnimation(ContactsActivityForBiz.this, R.anim.show_title_bar);
                //                mContentLayout.startAnimation(animation);
                //                mTitleBar.setAnimation(animation);
                //            }
                mTitleBar.setVisibility(View.VISIBLE);
                mSearchLayout.setVisibility(View.VISIBLE);
                //            mSearchLayout.requestFocus();
                mCancelSearchBtn.setVisibility(View.GONE);
                mContactsLayout.setVisibility(View.VISIBLE);
                mSearchResultContactsListView.setVisibility(View.GONE);
                mSearchGlassImageView.setVisibility(View.GONE);
                mGroupsLayout.setVisibility(View.GONE);
                mBtnTitleGroupEnter
                        .setImageResource(R.drawable.nav_group_list_selector);
                etSearch.clearFocus();
                mMainScrollView.setOnTouchListener(mRecoverScrollListener);
                mSideBar.setVisibility(View.VISIBLE);
                break;
            case DISPLAY_MODE_SEARCHING:
                // TODO animation
//            Animation animation = AnimationUtils.loadAnimation(ContactsActivityForBiz.this, R.anim.hide_title_bar);
//            mContentLayout.startAnimation(animation);
//            mTitleBar.setAnimation(animation);

                mTitleBar.setVisibility(View.GONE);
                mSearchLayout.setVisibility(View.VISIBLE);
                mCancelSearchBtn.setVisibility(View.VISIBLE);
                mContactsLayout.setVisibility(View.VISIBLE);
                mSearchResultContactsListView.setVisibility(View.GONE);
                mSearchGlassImageView.setVisibility(View.VISIBLE);
                mGroupsLayout.setVisibility(View.GONE);
                mMainScrollView.setOnTouchListener(mForbidScrollListener);
                mSideBar.setVisibility(View.VISIBLE);
                break;
            case DISPLAY_MODE_SEARCH_RESULT:
                mTitleBar.setVisibility(View.GONE);
                mSearchLayout.setVisibility(View.VISIBLE);
                mCancelSearchBtn.setVisibility(View.VISIBLE);
                mContactsLayout.setVisibility(View.GONE);
                mSearchResultContactsListView.setVisibility(View.VISIBLE);
                mSearchGlassImageView.setVisibility(View.GONE);
                mGroupsLayout.setVisibility(View.GONE);
                mMainScrollView.setOnTouchListener(mRecoverScrollListener);
                mSideBar.setVisibility(View.GONE);
                break;
            case DISPLAY_MODE_GROUP:
                mTitleBar.setVisibility(View.VISIBLE);
                mSearchLayout.setVisibility(View.GONE);
                mContactsLayout.setVisibility(View.VISIBLE);
                mSearchResultContactsListView.setVisibility(View.GONE);
                mSearchGlassImageView.setVisibility(View.GONE);
                mGroupsLayout.setVisibility(View.VISIBLE);
                mBtnTitleGroupEnter.setImageResource(R.drawable.nav_up_selector);
                mMainScrollView.setOnTouchListener(mRecoverScrollListener);
                mSideBar.setVisibility(View.GONE);
                break;
            default:
                break;
        }

        mCurrentDisplayMode = mode;
    }

    /**
     * 设置当前显示的群组名称，首先获取当前群组
     */
    private void setDispalyGroupName() {
        if (null != mCurrentChatRoom) {
            // 根部门时，显示"所有成员"
            if (!mCurrentChatRoom.isEditable && TextUtils.isEmpty(mCurrentChatRoom.parentGroupId)) {
                txtTitleCurrentGroup.setText(R.string.contactsforbiz_root_group_name_display);
            } else {
                txtTitleCurrentGroup.setText(mCurrentChatRoom.groupNameOriginal);
            }
            // 保存最新显示的group
            mPrefUtil.setDisplayGroupId(mCurrentChatRoom.groupID);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar:
                mMainScrollView.smoothScrollTo(0, 0);
                break;
            case R.id.title_txt_layout:
            case R.id.group_enter:
                // 显示群组Frame
                if (mCurrentDisplayMode != DISPLAY_MODE_GROUP) {
                    changeDisplayMode(DISPLAY_MODE_GROUP);
                    getMyGroupsFromLocal(true);
                    // 登录的刷新群组和成员完成了，也是其中一个刷新的前提条件；如果登录后的刷新未完成，此时不需要刷新
                    if (mPrefUtil.isGroupMembersUptodatePerfectly() && GlobalValue.isNeedToRefreshAllGroups()) {
                        Log.i("ContactsActivityForBiz#onClick(group_enter), need to refresh from server.");
                        getMyGroupsFromServer();
                    }
                } else {
                    changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
                }
                break;
//            case R.id.edt_search:
//                Toast.makeText(ContactsActivityForBiz.this, "edt search click", Toast.LENGTH_SHORT).show();
//                if (mCurrentDisplayMode != DISPLAY_MODE_SEARCHING) {
//                    changeDisplayMode(DISPLAY_MODE_SEARCHING);
//                }
//            case R.id.search_bar:
//                changeDisplayMode(DISPLAY_MODE_SEARCHING);
//                break;
            case R.id.field_clear:
                etSearch.setText("");
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
                break;
            // 取消搜索，获取点击覆盖的mSearchGlassImageView
            case R.id.cancel_btn:
            case R.id.search_glass_img:
            case R.id.layout_groups:
                // 如果软键盘仍存在，需要手动关闭
                InputMethodManager inputManager = ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE));
                inputManager.hideSoftInputFromWindow(
                        etSearch.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                if (etSearch.length() != 0) {
                    etSearch.setText("");
                }
                changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
                break;
            case R.id.contact_refresh:
                if (!mIsRefreshing && !mIsLoadingMore) {
                    EditText etSearchBar=(EditText)findViewById(R.id.edt_search);
                    if(etSearchBar.hasFocus()) {
                        etSearchBar.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(etSearchBar.getWindowToken(),0);
                    }
                    refreshCompanyStructure();
//                    if (mCurrentDisplayMode == DISPLAY_MODE_GROUP) {
//                        refreshGroupsAndMembersFromServer();
//                    } else if (null != mCurrentChatRoom) {
//                        refreshMembersFromServer(mCurrentChatRoom.groupID);
//                    }
                }
                break;
            case R.id.frequent_contacts_arrow_layout:
            case R.id.frequent_contacts_arrow:
                if (mFrequentContactsListView.getVisibility() == View.VISIBLE) {
                    // 收缩
                    mFrequentContactsArrow.setImageResource(R.drawable.contact_arrow_down);
                    mFrequentContactsListView.setVisibility(View.GONE);
                } else {
                    // 展开
                    mFrequentContactsArrow.setImageResource(R.drawable.contact_arrow_up);
                    mFrequentContactsListView.setVisibility(View.VISIBLE);
                    ListHeightUtil.setListHeight(mFrequentContactsListView);
                }
                break;
            case R.id.text:
                loadMorePersons();
                break;
            default:
                break;
        }
    }

//    private void refreshGroupsAndMembersFromServer() {
//        final WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(ContactsActivityForBiz.this);
//        mIsRefreshing = true;
//        Animation animation = AnimationUtils.loadAnimation(ContactsActivityForBiz.this, R.anim.refresh);
//        btnTitleRefresh.startAnimation(animation);
////        mMsgBox.showWait();
//        new AsyncTask<Void, Void, Void> () {
//            @Override
//            protected Void doInBackground(Void... params) {
//                // 群组
//                Log.i("ContactsActivityForBiz#refreshGroupsAndMembersFromServer, downloading all groups for biz");
//                if (ErrorCode.OK == webIF.getGroupsByCompanyId(mPrefUtil.getCompanyId())) {
//                    Log.i("LoginActivity#getAllGroupsAndMembersFromServer, finish downloading all groups for biz");
//                    mPrefUtil.setLocalGroupListLastModified();
//                    mPrefUtil.setGroupUptodate(true);
//                }
//
//                // 收藏的群组和常用联系人
//                Log.i("ContactsActivityForBiz#refreshGroupsAndMembersFromServer, downloading favorite contacts and groups for biz");
//                if (ErrorCode.OK == webIF.getFavoriteContactsAndGroups()) {
//                    Log.i("LoginActivity#getAllGroupsAndMembersFromServer, finish downloading favorite contacts and groups for biz");
//                    mPrefUtil.setFavoritesUptodate(true);
//                }
//
//                // 公司用来发通知的buddy信息,其wowtalk_id格式为companyId_companyId
//                Log.i("ContactsActivityForBiz#refreshGroupsAndMembersFromServer, downloading notice_buddy for biz");
//                String companyId = mPrefUtil.getCompanyId();
//                webIF.fGetBuddyByWowtalkId(companyId + "_" + companyId, new Buddy());
//                Log.i("LoginActivity#getAllGroupsAndMembersFromServer, finish downloading notice_buddy for biz");
//
//                // 群组成员
//                Log.i("ContactsActivityForBiz#refreshGroupsAndMembersFromServer, downloading all group members for biz");
//                ArrayList<GroupChatRoom> groupChatRooms = mDbHelper.fetchNonTempGroupChatRooms(true);
//                for (GroupChatRoom groupChatRoom : groupChatRooms) {
//                    if (ErrorCode.OK == webIF.fGroupChat_GetMembersIteration(groupChatRoom.groupID)) {
//                        GlobalValue.setDontNeedToRefreshMembers(groupChatRoom.groupID);
//                    }
//                }
//                Log.i("ContactsActivityForBiz#refreshGroupsAndMembersFromServer, Finish downloading all group members for biz");
//
//                getMyGroupsFromLocal(true);
//                return null;
//            }
//
//            protected void onPostExecute(Void result) {
////                mMsgBox.dismissWait();
//                btnTitleRefresh.clearAnimation();
//                mIsRefreshing = false;
//            };
//        }.execute((Void)null);
//    }

    private void refreshCompanyStructure() {
        mIsRefreshing = true;
        Animation animation = AnimationUtils.loadAnimation(ContactsActivityForBiz.this, R.anim.refresh);
        btnTitleRefresh.startAnimation(animation);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                int errnoGroups = mWebif.getGroupsByCompanyId(mPrefUtil.getCompanyId());
                int errnoMembers = mWebif.getCompanyStructure(mPrefUtil.getCompanyId());
                if (errnoGroups == ErrorCode.OK && errnoMembers == ErrorCode.OK) {
                    getMyGroupsFromLocal(true);
                    getCurrentChatGroup();
                    getFrequentContacts();
                    getContactsListFromLocal();
                    getSearchedContacts();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                btnTitleRefresh.clearAnimation();
                mIsRefreshing = false;
            };
        }.execute((Void)null);
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        if (mCurrentDisplayMode == DISPLAY_MODE_SEARCHING) {
            return;
        }
        if(mMsgBox == null)
            mMsgBox = new MessageBox(this);
        if (s.equals("Se")) {
            mMsgBox.toast(R.string.search_toast, 0);
            mMainScrollView.smoothScrollTo(0, 0);
        } else {
            mMsgBox.toast(s, 0);

            if (contactsAdapter == null)
                return;
            ArrayList<Person> persons = contactsAdapter.getPersonSource();
            if (persons == null || persons.isEmpty())
                return;

            // TODO 此处计算height，需要根据xml中的布局来调整，修改布局文件后，此处也需要修改
            int height = 0;
            // search_layout
            View heightView = mMainLinearLayout.getChildAt(0);
            if (heightView != null) {
                height += heightView.getHeight();
            }
            // layout_contacts
            heightView = mMainLinearLayout.getChildAt(1);
            if (heightView instanceof RelativeLayout) {
                RelativeLayout relativeLayout = (RelativeLayout) heightView;
                // layout_frequent_contacts
                heightView = relativeLayout.getChildAt(0);
                if (null != heightView) {
                    height += heightView.getHeight();
                }
            }

            int numCount = 0;
            for (Person person : persons) {
                if (person.getSortKey().substring(0, 1).toUpperCase().equals(s)) {
                    mMainScrollView.smoothScrollTo(0, height);
                    break;
                }
                View view = mContactsListView.getChildAt(numCount);
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
        setContentView(R.layout.contacts_biz);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mDbHelper = new Database(ContactsActivityForBiz.this);
        mWebif = WowTalkWebServerIF.getInstance(this);

        initView();

        mFrequentContactsLayout.setVisibility(View.GONE);
        mContactsListView.setVisibility(View.GONE);
        mIsLoadingMore = true;
        setLoadMoreView(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 以下几个方法中调用的集合，不能使用clear()方法清空数据，需要重新new，
                // 因为这是新起线程，和UI线程不同步，导致UI刷新错误(数组越界)
                getCurrentChatGroup();
                getFrequentContacts();
                getContactsListFromLocal();
                getSearchedContacts();
            }
        }).start();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_SWITCH_ACCOUNT,mAccountSwitchObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FAVORITE_GROUP, mFavoriteGroupsObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FAVORITE_CONTACT, mFavoriteContactsObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FINISH_LOAD_MEMBERS, mFinishDownloadMembersObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_SORT_KEY_UPDATED, mSortKeyUpdatedObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_MY_INFO_UPDATED, mMyInfoUpdatedObserver);
//        Database.addDBTableChangeListener(Database.TBL_BUDDY_DETAIL, mBuddyDetailObserver);
    }

    private IDBTableChangeListener mAccountSwitchObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {

            mDbHelper = new Database(ContactsActivityForBiz.this);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mDbHelper = new Database(ContactsActivityForBiz.this);
                    getCurrentChatGroup();
                    getFrequentContacts();
                    getContactsListFromLocal();
                    getSearchedContacts();
                }
            }).start();
        }
    };

    private IDBTableChangeListener mFavoriteGroupsObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            mFavoriteGroupRooms.clear();
            mFavoriteGroupRooms = mDbHelper.fetchFavoriteGroupChatRooms();
            GroupChatRoom rootFavoriteChatRoom = treeFavoriteGroups(getString(R.string.contactsforbiz_root_favorite_group_name),mFavoriteGroupRooms);
            Message message = mHandler.obtainMessage(HANDLER_GET_FAVORITE_GROUP);
            message.obj = rootFavoriteChatRoom;
            mHandler.sendMessage(message);
        }
    };

    private IDBTableChangeListener mFavoriteContactsObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            getFrequentContacts();
        }
    };

    private IDBTableChangeListener mFinishDownloadMembersObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            Log.i("ContactsActivityForBiz#mFinishDownloadMembersObserver, finish loaded...");
            mHandler.sendEmptyMessage(HANDLER_STOP_REFRESH);
            getCurrentChatGroup();
            getFrequentContacts();
            getContactsListFromLocal();
            getSearchedContacts();
        }
    };

    /**
     * 改变语言环境，重新给sort_key赋值完成后，更新联系人列表
     */
    private IDBTableChangeListener mSortKeyUpdatedObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            Log.i("ContactsActivityForBiz#mSortKeyUpdatedObserver...");
            getContactsListFromLocal();
        }
    };

    private IDBTableChangeListener mMyInfoUpdatedObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            Log.i("ContactsActivityForBiz#mMyInfoUpdatedObserver...");
            android.util.Log.e("zjh", "ContactsActivityForBiz#mMyInfoUpdatedObserver...");
            // 只会出现在联系人及搜索结果中
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Buddy buddy = new Buddy(mPrefUtil.getUid());
                    buddy = mDbHelper.fetchBuddyDetail(buddy);
                    if (null == buddy) {
                        return;
                    }

                    Person newPerson = Person.fromBuddy(buddy);
                    // 联系人
                    int index = mContacts.indexOf(newPerson);
                    if (index != -1) {
                        Person person = mContacts.get(index);
                        person.setWithBuddy(buddy);
                        mHandler.sendEmptyMessage(HANDLER_GET_MEMBERS_OF_CURRENT_GROUP);
                    }
                    // 搜索结果
                    index = mSearchedContacts.indexOf(newPerson);
                    if (index != -1) {
                        Person person = mSearchedContacts.get(index);
                        person.setWithBuddy(buddy);
                        mHandler.sendEmptyMessage(HANDLER_GET_SEARCHED_CONTACTS);
                    }
                }
            }).start();
        }
    };

//    private IDBTableChangeListener mBuddyDetailObserver = new IDBTableChangeListener() {
//        public void onDBTableChanged(String tableName) {
//
//            mDbHelper = new Database(ContactsActivityForBiz.this);
//            getFrequentContacts();
//            getContactsListFromLocal();
//            getSearchedContacts();
//        }
//    };

    @Override
    protected void onResume() {
        super.onResume();
        theActiveInstance = this;
        MobclickAgent.onResume(this);

        mDbHelper = new Database(this);
        // onResume时，显示初始的联系人mode
        changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);

        if (!mPrefUtil.isGroupMembersUptodatePerfectly()) {
            mIsRefreshing = true;
            Animation animation = AnimationUtils.loadAnimation(ContactsActivityForBiz.this, R.anim.refresh);
            btnTitleRefresh.startAnimation(animation);
            // 重启子线程下载部门／成员，会判断是否有线程正在下载
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StartActivity.downloadContactsAndGroups(ContactsActivityForBiz.this, null);
                }
            }).start();
        } else {
            mIsRefreshing = false;
            btnTitleRefresh.clearAnimation();
        }
    }

    /**
     * 获取mCurrentChatRoom
     * <p>每次重新启动时，会调用到此方法内的实现(null == mCurrentChatRoom)
     * <p>也可能是后台切前台，重新下载群组，但该群组被移除，需要重新赋值mCurrentChatRoom
     */
    private void getCurrentChatGroup() {
        String currentGroupId = mPrefUtil.getDisplayGroupId();
        if (TextUtils.isEmpty(currentGroupId)) {
            // 此处获取群组，为了给mCurrentChatRoom组织公司组织架构
            getMyGroupsFromLocal(false);
            mCurrentChatRoom = mDbHelper.fetchRootGroupChatRoom();
        } else {
            // 没有保存group_id，或没有找到对应群组：获取root chat_room
            mCurrentChatRoom = mDbHelper.fetchGroupChatRoom(currentGroupId);
            if (null == mCurrentChatRoom) {
                getMyGroupsFromLocal(false);
                mCurrentChatRoom = mDbHelper.fetchRootGroupChatRoom();
            }
        }
        // 组织mCurrentChatRoom的组织结构
        if (null != mCurrentChatRoom && !TextUtils.isEmpty(mCurrentChatRoom.groupID)) {
            for (GroupChatRoom groupChatRoom : mAllGroupRooms) {
                fillCurrentChatGroup(groupChatRoom);
            }
        }

        mHandler.sendEmptyMessage(HANDLER_GET_CURRENT_GROUP);
    }

    /**
     * 填充mCurrentChatRoom的组织架构
     * @param groupChatRoom
     */
    private void fillCurrentChatGroup(GroupChatRoom groupChatRoom) {
        if (mHasFilledCurrentChatRoom) {
            return;
        }
        if (mCurrentChatRoom.groupID.equals(groupChatRoom.groupID)) {
            mCurrentChatRoom = groupChatRoom;
            mHasFilledCurrentChatRoom = true;
            return;
        }
        if (null != groupChatRoom.childGroups && !groupChatRoom.childGroups.isEmpty()) {
            for (GroupChatRoom parentChatRoom : groupChatRoom.childGroups) {
                fillCurrentChatGroup(parentChatRoom);
            }
        }
    }

    /**
     * 获得常用联系人，没有常用联系人时，隐藏此标题和 listview
     */
    private void getFrequentContacts() {
        ArrayList<Buddy> frequentBuddies = mDbHelper.fetchFrequentContacts();
        mFrequentContacts = new ArrayList<Person>();
        for (Buddy buddy : frequentBuddies) {
            mFrequentContacts.add(Person.fromBuddy(buddy));
        }

        mHandler.sendEmptyMessage(HANDLER_GET_FREQUENT_CONTACTS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        theActiveInstance = null;
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Database.removeDBTableChangeListener(mAccountSwitchObserver);
        Database.removeDBTableChangeListener(mFavoriteGroupsObserver);
        Database.removeDBTableChangeListener(mFavoriteContactsObserver);
        Database.removeDBTableChangeListener(mFinishDownloadMembersObserver);
        Database.removeDBTableChangeListener(mSortKeyUpdatedObserver);
        Database.removeDBTableChangeListener(mMyInfoUpdatedObserver);
//        Database.removeDBTableChangeListener(mBuddyDetailObserver);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LAUNCH_BUDDY_DETAIL && resultCode == RESULT_OK) {
            final String buddyId = data.getStringExtra("uid");
            boolean isBuddyDetailChanged = data.getBooleanExtra("is_changed", false);
            Log.d("ContactsActivityForBiz#onActivityResult, buddyId" + buddyId + ", isChanged " + isBuddyDetailChanged);
            if (!isBuddyDetailChanged) {
                return;
            }
            // 更新此buddyId对应的联系人信息
            if (!TextUtils.isEmpty(buddyId)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 获取联系人的新信息
                        Buddy buddy = new Buddy(buddyId);
                        buddy = mDbHelper.fetchBuddyDetail(buddy);
                        if (null == buddy) {
                            return;
                        }

                        Person newPerson = Person.fromBuddy(buddy);
                        // 常用联系人
                        int index = mFrequentContacts.indexOf(newPerson);
                        if (index != -1) {
                            Person person = mFrequentContacts.get(index);
                            person.setWithBuddy(buddy);
                            mHandler.sendEmptyMessage(HANDLER_GET_FREQUENT_CONTACTS);
                        }
                        // 联系人
                        index = mContacts.indexOf(newPerson);
                        if (index != -1) {
                            Person person = mContacts.get(index);
                            person.setWithBuddy(buddy);
                            mHandler.sendEmptyMessage(HANDLER_GET_MEMBERS_OF_CURRENT_GROUP);
                        }
                        // 搜索结果
                        index = mSearchedContacts.indexOf(newPerson);
                        if (index != -1) {
                            Person person = mSearchedContacts.get(index);
                            person.setWithBuddy(buddy);
                            mHandler.sendEmptyMessage(HANDLER_GET_SEARCHED_CONTACTS);
                        }
                    }
                }).start();
            }
        }
    }

    /**
     * Get the active instance.
     * @return
     */
    public static ContactsActivityForBiz instance() {
        return theActiveInstance;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() ==   KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (mCurrentDisplayMode == DISPLAY_MODE_CONTACTS_WITH_SEARCH) {
                handleBackEvent();
            } else {
                if (etSearch.length() != 0) {
                    etSearch.setText("");
                }
                changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
            }
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
