package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactGroupIterationAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.SideBar.OnTouchingLetterChangedListener;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.msg.RoundedImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MultiSelectActivityForBiz extends Activity implements OnClickListener, OnTouchingLetterChangedListener {
	private class SelectedAdapter extends BaseAdapter {

		private ArrayList<Person> adapterSelectedPerson;

		public SelectedAdapter(ArrayList<Person> selectedPerson) {
			this.adapterSelectedPerson = selectedPerson;
		}

		@Override
		public int getCount() {
			return adapterSelectedPerson.size();
		}

		@Override
		public Object getItem(int position) {
			return adapterSelectedPerson.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View lView;
			if (convertView == null) {
				lView = LayoutInflater.from(MultiSelectActivityForBiz.this).inflate(R.layout.listitem_group_selected, null);
			} else {
				lView = convertView;
			}
			final RoundedImageView imgThumbnail = (RoundedImageView) lView.findViewById(R.id.img_thumbnail);
            imgThumbnail.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position < adapterSelectedPerson.size()) {
                        Person selectedPerson = adapterSelectedPerson.get(position);

                        // can't cancel_select the original mermbers in selectedPersons, if it's from group_chat_room
                        if (mInitMemberIds.contains(selectedPerson.getID())) {
                            return;
                        }

                        // 改变 常用联系人／联系人列表，在改变selected属性之前
                        changeImageSelected(mContactsListView, mContacts, selectedPerson);
                        changeImageSelected(mFrequentContactsListView, mFrequentContacts, selectedPerson);
                        // 搜索模式时，改变搜索结果
                        if (mCurrentDisplayMode == DISPLAY_MODE_SEARCH_RESULT) {
                            changeImageSelected(mSearchedContactsListView, mSearchedContactsAdapter.getPersonSource(), selectedPerson);
                        }

                        selectedPerson.setSelected(false);
                        adapterSelectedPerson.remove(position);
                        mSelectedPersonIds.remove(selectedPerson.getID());

                        // refresh view
                        refreshView(selectedPerson.getID(), false);
                    }
                }
            });
			if (position < adapterSelectedPerson.size()) {
                Person p = adapterSelectedPerson.get(position);
                if(!TextUtils.isEmpty(p.getID())) {
                    Buddy buddy = new Buddy();
                    buddy.userID = adapterSelectedPerson.get(position).getID();
                    buddy.photoUploadedTimeStamp = adapterSelectedPerson.get(position).photoUploadedTimestamp;
                    PhotoDisplayHelper.displayPhoto(MultiSelectActivityForBiz.this, imgThumbnail, R.drawable.default_avatar_90, buddy, true);
                } else {
                    // local (non-wowtalk) user?
                    // TODO display avatar & nick
                    if (p.getLocalPersonPhotoID() > 0) {
                        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, p.getLocalContactID());
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(MultiSelectActivityForBiz.this.getContentResolver(), uri);
                        imgThumbnail.setImageBitmap(BitmapFactory.decodeStream(input));
                    } else {
                        PhotoDisplayHelper.displayPhoto(MultiSelectActivityForBiz.this, imgThumbnail, R.drawable.default_avatar_90, null, true);
                    }
                }
            }
			return lView;
		}

	}

	public static final String INTENT_GROUP_CHAT = "intent_group_chat";
	public static final String INTENT_NEW_GROUP_CHAT = "intent_new_group_chat";
	public static final String INTENT_ADD_GROUP_CHAT = "intent_add_group_chat";

	/**
     * 最开始的没有搜索框的联系人mode（常用联系人，当前群组的联系人）
     */
    public static final int DISPLAY_MODE_CONTACTS = 1;
    /**
     * 最开始的，有搜索框的联系人mode（常用联系人，当前群组的联系人）
     */
    public static final int DISPLAY_MODE_CONTACTS_WITH_SEARCH = 2;
    /**
     * 搜索框在最上面，且下面全部被灰色背景覆盖（灰色背景下：常用联系人，当前群组的联系人）
     */
    public static final int DISPLAY_MODE_SEARCHING = 3;
    /**
     * 搜索结果mode（与搜索匹配的当前群组的联系人）
     */
    public static final int DISPLAY_MODE_SEARCH_RESULT = 4;
    /**
     * 显示群组Frame，底部灰色背景（底部的联系人能显示，但不清新）
     */
    public static final int DISPLAY_MODE_GROUP = 5;

    private static final int REQ_SELECT_LOCAL_CONTACTS = 123;

    /**
     * 每次加载的条数
     */
    private static final int LOAD_COUNT_PER_TIME = 100;

    private static final int HANDLER_DISMISS_WAIT = 0;
    private static final int HANDLER_DISPLAY_NAME = 1;
    private static final int HANDLER_FREQUENT_CONTACTS = 2;
    private static final int HANDLER_CONTACTS_LIST = 3;
    private static final int HANDLER_SEARCHED_CONTACTS = 4;

    private static MultiSelectActivityForBiz instance;

    private RelativeLayout mTitleBar;
    private ImageButton mGroupEnterBtn;
	private ImageButton btnTitleBack;
	private TextView mTitleText;
	private ScrollView mMainScrollView;
	private RelativeLayout mMainRelativeLayout;
	private LinearLayout mFrequentContactsLayout;
	private RelativeLayout mFrequentContactsSubtitleLayout;
	private ImageView mFrequentContactsArrow;
	private ListView mFrequentContactsListView;
	private RelativeLayout mContactsLayout;
//	private View mFilterView;
	private EditText edtSearch;
	private ImageButton mClearButton;
	private Button mCancelButton;
	private View btnSelectGroup;
	private ImageView mDividerImage;
	private ListView mContactsListView;
	private ListView mSearchedContactsListView;
	private ImageView mSearchGlassImg;
	private LinearLayout mGroupsLayout;
	private ListView mGroupsListView;
	private SideBar mSideBar;
	private TextView mLoadMoreView;
	private GridView gridSelected;
	private Button btnOK;

	private ArrayList<Person> mSelectedPerson = new ArrayList<Person>();
	private HashSet<String> mSelectedPersonIds = new HashSet<String>();
	private List<String> mInitMemberIds = new ArrayList<String>();
	private ArrayList<Person> mFrequentContacts = new ArrayList<Person>();
    private ArrayList<Person> mContactsAll = new ArrayList<Person>();
	private ArrayList<Person> mContacts = new ArrayList<Person>();
	private ArrayList<Person> mSearchedContacts = new ArrayList<Person>();
	private ArrayList<GroupChatRoom> mAllGroupRooms = new ArrayList<GroupChatRoom>();
	private ArrayList<GroupChatRoom> mFavoriteGroupRooms = new ArrayList<GroupChatRoom>();

	private ContactListAdapter mFrequentContactsAdapter;
	private ContactListAdapter mContactsAdapter;
	private ContactGroupIterationAdapter mGroupIterationAdapter;
	private ContactListAdapter mSearchedContactsAdapter;
	private SelectedAdapter selectedAdapter;


	/**
	 * whether should display the enter of chatRooms at the head of it.
	 */
	private boolean mIsOnlyShowContacts;

    /**
     * the original group_id:
     * if it is empty, so create a new temp group chatroom;
     * otherwise, add members to the group whose group_id is same with it.
     */
    private String mOriginalGid;

	/**
	 * the original person id if this multi-select is from the single-chat
	 */
	private String mInitPersonIdFromSingle;
	private String mMyOwnId;
	private int mCurrentDisplayMode;
	private GroupChatRoom mCurrentChatRoom;
	private boolean mHasFilledCurrentChatRoom;
    private boolean mIsLoadingMore;

    private MessageBox mMsgBox;
    private Database mDbHelper;
    private PrefUtil mPref;

    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
            case HANDLER_DISMISS_WAIT:
                mMsgBox.dismissWait();
                break;
            case HANDLER_DISPLAY_NAME:
                setDispalyGroupName();
                break;
            case HANDLER_FREQUENT_CONTACTS:
                if (mFrequentContacts.isEmpty()) {
                    mFrequentContactsLayout.setVisibility(View.GONE);
                } else {
                    mFrequentContactsLayout.setVisibility(View.VISIBLE);
                }
                if (null == mFrequentContactsAdapter) {
                    mFrequentContactsAdapter = new ContactListAdapter(MultiSelectActivityForBiz.this, mFrequentContacts);
                    mFrequentContactsAdapter.setIsShowFirstChar(false);
                    mFrequentContactsAdapter.setDefaultSelected(true);
                    mFrequentContactsListView.setAdapter(mFrequentContactsAdapter);
                } else {
                    mFrequentContactsAdapter.setPersonSource(mFrequentContacts);
                    mFrequentContactsAdapter.notifyDataSetChanged();
                }
                ListHeightUtil.setListHeight(mFrequentContactsListView);
                mFrequentContactsListView.setOnItemClickListener(new ContactOnItemClickListener(mFrequentContactsAdapter));
                break;
            case HANDLER_CONTACTS_LIST:
                if (mContacts.isEmpty()) {
                    mContactsListView.setVisibility(View.GONE);
                } else {
                    mContactsListView.setVisibility(View.VISIBLE);
                }
                if (null == mContactsAdapter) {
                    mContactsAdapter = new ContactListAdapter(MultiSelectActivityForBiz.this, mContacts);
                    mContactsAdapter.setDefaultSelected(true);
                    mContactsListView.setAdapter(mContactsAdapter);
                } else {
                    mContactsAdapter.setPersonSource(mContacts);
                    mContactsAdapter.notifyDataSetChanged();
                }
                ListHeightUtil.setListHeight(mContactsListView);
                mContactsListView.setOnItemClickListener(new ContactOnItemClickListener(mContactsAdapter));
                mIsLoadingMore = false;
                setLoadMoreView(false);
                break;
            case HANDLER_SEARCHED_CONTACTS:
                if (null == mSearchedContactsAdapter) {
                    mSearchedContactsAdapter = new ContactListAdapter(MultiSelectActivityForBiz.this, mSearchedContacts);
                    mSearchedContactsAdapter.setIsShowFirstChar(false);
                    mSearchedContactsAdapter.setDefaultSelected(true);
                    mSearchedContactsListView.setAdapter(mSearchedContactsAdapter);
                } else {
                    mSearchedContactsAdapter.setPersonSource(mSearchedContacts);
                    mSearchedContactsAdapter.notifyDataSetChanged();
                }
                mSearchedContactsListView.setOnItemClickListener(new ContactOnItemClickListener(mSearchedContactsAdapter));
                break;
            default:
                break;
            }
        };
    };

	private TextWatcher mSearchWacher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(mContactsAdapter == null)
                return;

            if (s.length() == 0) {
                mClearButton.setVisibility(View.GONE);
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
            } else {
                mClearButton.setVisibility(View.VISIBLE);
                mSearchedContactsAdapter.getFilter().filter(s.toString(), new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int i) {
                        if (mCurrentDisplayMode != DISPLAY_MODE_SEARCH_RESULT) {
                            changeDisplayMode(DISPLAY_MODE_SEARCH_RESULT);
                        }
                        ListHeightUtil.setListHeight(mSearchedContactsListView);
                    }
                });
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mCurrentDisplayMode != DISPLAY_MODE_SEARCHING
                    && mCurrentDisplayMode != DISPLAY_MODE_SEARCH_RESULT
                    && v.getId() == R.id.edt_search
                    && MotionEvent.ACTION_DOWN == event.getAction()) {
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
            }
            return false;
        }
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
//            if (mPref.isGroupMembersUptodatePerfectly() && GlobalValue.isNeedToRefreshMembers(chatRoom.groupID)) {
//                Log.i("MultiSelectActivityForBiz#mSelectedGroupListener#onGroupNameClicked, need to refresh from server.");
//                getContactsListFromServer(chatRoom.groupID);
//            }
        }

        @Override
        public void onSendMsgClicked() {
            finish();
        }
    };

    private IDBTableChangeListener mLoadMembersAfterLoginObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i("MultiSelectActivityForBiz#mLoadMembersAfterLoginObserver, finish loaded...");
                    mHandler.sendEmptyMessage(HANDLER_DISMISS_WAIT);
                    getCurrentChatGroup();
                    getFrequentContacts();
                    getLocalNormalContacts();
                    getContactsListFromLocal();
                    getSearchedContacts();
                }
            }).start();
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
                // 不能取消原来群组中的成员
                if (mInitMemberIds.contains(person.getID())) {
                    return;
                }
                if (person.isSelected()) {
                    mSelectedPersonIds.remove(person.getID());
                    for (Iterator<Person> iterator = mSelectedPerson.iterator(); iterator.hasNext();) {
                        Person selectedPerson = iterator.next();
                        if (person.getID().equals(selectedPerson.getID())) {
                            iterator.remove();
                            break;
                        }
                    }
                } else {
                    mSelectedPersonIds.add(person.getID());
                    mSelectedPerson.add(person);
                }
                // 改变当前选中的item的勾选状态
                ImageView imageView = (ImageView) view.findViewById(R.id.img_selected);
                imageView.setBackgroundResource(!person.isSelected() ? R.drawable.list_selected : R.drawable.list_unselected);
                // 改变与当前选中的item代表同一个成员的item的勾选状态(常用联系人／联系人列表)
                if (mAdapter == mFrequentContactsAdapter) {
                    changeImageSelected(mContactsListView, mContacts, person);
                } else if (mAdapter == mContactsAdapter) {
                    changeImageSelected(mFrequentContactsListView, mFrequentContacts, person);
                } else if (mAdapter == mSearchedContactsAdapter) {
                    // 搜索结果中改变时，需要改变 常用联系人／联系人列表
                    changeImageSelected(mContactsListView, mContacts, person);
                    changeImageSelected(mFrequentContactsListView, mFrequentContacts, person);
                }
                refreshView(person.getID(), !person.isSelected());
            }
        }
    }

    /**
     * 改变listview与person对应item的勾选状态
     * @param listView
     * @param datas
     * @param person
     */
    private void changeImageSelected(ListView listView, List<Person> datas, Person person) {
        int index = datas.indexOf(person);
        if (index >= 0) {
            // 此处是由于ListView被包含在ScrollView中，所以才能这样处理
            View childView = listView.getChildAt(index).findViewById(R.id.img_selected);
            if (childView instanceof ImageView) {
                ((ImageView)childView).setBackgroundResource(
                        !person.isSelected() ? R.drawable.list_selected : R.drawable.list_unselected);
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

    public static MultiSelectActivityForBiz instance() {
        if (instance == null)
            return new MultiSelectActivityForBiz();
        return instance;
    }

    private void initView() {
	    mTitleBar = (RelativeLayout) findViewById(R.id.title_bar);
	    mGroupEnterBtn = (ImageButton) findViewById(R.id.group_enter);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		mTitleText = (TextView) findViewById(R.id.title_text);
		mMainScrollView = (ScrollView)findViewById(R.id.main_scroll_view);
		mMainRelativeLayout = (RelativeLayout)findViewById(R.id.main_relative_layout);
		btnSelectGroup = findViewById(R.id.txt_group);
		mDividerImage = (ImageView) findViewById(R.id.divider_image);
		mFrequentContactsLayout = (LinearLayout) findViewById(R.id.layout_frequent_contacts);
		mFrequentContactsSubtitleLayout = (RelativeLayout) findViewById(R.id.frequent_contacts_subtitle_layout);
		mFrequentContactsArrow = (ImageView) findViewById(R.id.frequent_contacts_arrow);
		mFrequentContactsListView = (ListView) findViewById(R.id.frequent_contacts);
		mContactsLayout = (RelativeLayout) findViewById(R.id.contacts_layout);
		mContactsListView = (ListView) findViewById(R.id.contacts_list_view);
		mSearchedContactsListView = (ListView) findViewById(R.id.searched_contacts_list_view);
		mSearchGlassImg = (ImageView) findViewById(R.id.search_glass_img);
		mGroupsLayout = (LinearLayout) findViewById(R.id.layout_groups);
		mGroupsListView = (ListView) findViewById(R.id.groups_iteration);
		mSideBar = (SideBar) findViewById(R.id.side_bar);
        mLoadMoreView = (TextView) findViewById(R.id.text);

		gridSelected = (GridView) findViewById(R.id.grid_selected);
		btnOK = (Button) findViewById(R.id.btn_ok);
		btnOK.setText(String.format(getResources().getString(R.string.ok_with_num), mSelectedPerson.size()));
		btnOK.setEnabled(false);

        edtSearch = (EditText) findViewById(R.id.edt_search);
        mClearButton = (ImageButton) findViewById(R.id.field_clear);
        mCancelButton = (Button) findViewById(R.id.cancel_btn);

        if (mIsOnlyShowContacts) {
            btnSelectGroup.setVisibility(View.GONE);
            mDividerImage.setVisibility(View.GONE);
        }

        mGroupEnterBtn.setOnClickListener(this);
		btnTitleBack.setOnClickListener(this);
		mClearButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
		mSearchGlassImg.setOnClickListener(this);
		mGroupsLayout.setOnClickListener(this);
		mFrequentContactsSubtitleLayout.setOnClickListener(this);
		mFrequentContactsArrow.setOnClickListener(this);
        mLoadMoreView.setOnClickListener(this);
		btnSelectGroup.setOnClickListener(this);
		mSideBar.setOnTouchingLetterChangedListener(this);
		btnOK.setOnClickListener(this);
		edtSearch.addTextChangedListener(mSearchWacher);
		edtSearch.setOnTouchListener(mOnTouchListener);
	}

	/**
	 *
	 * @param mode
	 */
	private void changeDisplayMode(int mode) {
	    switch (mode) {
        case DISPLAY_MODE_CONTACTS:
//            etSearch.setText("");
//            mTitleBar.setVisibility(View.VISIBLE);
//            mSearchLayout.setVisibility(View.GONE);
//            mContactsLayout.setVisibility(View.VISIBLE);
//            mSearchResultContactsListView.setVisibility(View.GONE);
//            mSearchGlassImageView.setVisibility(View.GONE);
//            mGroupsLayout.setVisibility(View.GONE);
//            mBtnTitleGroupEnter.setImageResource(R.drawable.nav_group_list_selector);
//            etSearch.clearFocus();
//            mMainScrollView.setOnTouchListener(mRecoverScrollListener);
//            mSideBar.setVisibility(View.VISIBLE);
            break;
        case DISPLAY_MODE_CONTACTS_WITH_SEARCH:
            edtSearch.setText("");
            //            if (mCurrentDisplayMode == DISPLAY_MODE_SEARCHING || mCurrentDisplayMode == DISPLAY_MODE_SEARCH_RESULT) {
            //                Animation animation = AnimationUtils.loadAnimation(this, R.anim.show_title_bar);
            //                mContentLayout.startAnimation(animation);
            //                mTitleBar.setAnimation(animation);
            //            }
            mTitleBar.setVisibility(View.VISIBLE);
//            mSearchLayout.setVisibility(View.VISIBLE);
//            //            mSearchLayout.requestFocus();
            mCancelButton.setVisibility(View.GONE);
            mContactsLayout.setVisibility(View.VISIBLE);
            mSearchedContactsListView.setVisibility(View.GONE);
            mSearchGlassImg.setVisibility(View.GONE);
            mGroupsLayout.setVisibility(View.GONE);
            mGroupEnterBtn.setImageResource(R.drawable.nav_group_list_selector);
            edtSearch.clearFocus();
            mMainScrollView.setOnTouchListener(mRecoverScrollListener);
            mSideBar.setVisibility(View.VISIBLE);
            break;
        case DISPLAY_MODE_SEARCHING:
            // TODO animation
//            Animation animation = AnimationUtils.loadAnimation(this, R.anim.hide_title_bar);
//            mContentLayout.startAnimation(animation);
//            mTitleBar.setAnimation(animation);

            mTitleBar.setVisibility(View.GONE);
//            mSearchLayout.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mContactsLayout.setVisibility(View.VISIBLE);
            mSearchedContactsListView.setVisibility(View.GONE);
            mSearchGlassImg.setVisibility(View.VISIBLE);
            mGroupsLayout.setVisibility(View.GONE);
            mMainScrollView.setOnTouchListener(mForbidScrollListener);
            mSideBar.setVisibility(View.VISIBLE);
            break;
        case DISPLAY_MODE_SEARCH_RESULT:
            mTitleBar.setVisibility(View.GONE);
//            mSearchLayout.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mContactsLayout.setVisibility(View.GONE);
            mSearchedContactsListView.setVisibility(View.VISIBLE);
            mSearchGlassImg.setVisibility(View.GONE);
            mGroupsLayout.setVisibility(View.GONE);
            mMainScrollView.setOnTouchListener(mRecoverScrollListener);
            mSideBar.setVisibility(View.GONE);
            break;
        case DISPLAY_MODE_GROUP:
            mTitleBar.setVisibility(View.VISIBLE);
//            mSearchLayout.setVisibility(View.GONE);
            mContactsLayout.setVisibility(View.VISIBLE);
            mSearchedContactsListView.setVisibility(View.GONE);
            mSearchGlassImg.setVisibility(View.GONE);
            mGroupsLayout.setVisibility(View.VISIBLE);
            mGroupEnterBtn.setImageResource(R.drawable.nav_up_selector);
            mMainScrollView.setOnTouchListener(mRecoverScrollListener);
            mSideBar.setVisibility(View.GONE);
            break;
        default:
            break;
        }

        mCurrentDisplayMode = mode;
	}

	private void setGridWidth() {
        int width = getResources().getDimensionPixelSize(R.dimen.selected_grid_width);
        int space = getResources().getDimensionPixelSize(R.dimen.selected_grid_space);
		gridSelected.setColumnWidth(width);
		gridSelected.setNumColumns(mSelectedPerson.size());
		LayoutParams params = gridSelected.getLayoutParams();
		// the width of grid should contains the leftmost and rightmost space.(whose space is equal to space * 2)
        params.width = mSelectedPerson.size() * width + (mSelectedPerson.size() - 1) * space + space * 2;
		gridSelected.setLayoutParams(params);
	}

    private void setData() {
        if (!mPref.isGroupMembersUptodatePerfectly()) {
            mMsgBox.showWait();
        }
        mFrequentContactsLayout.setVisibility(View.GONE);
        mContactsListView.setVisibility(View.GONE);
        mIsLoadingMore = true;
        setLoadMoreView(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                getCurrentChatGroup();
                getFrequentContacts();
                getLocalNormalContacts();
                getContactsListFromLocal();
                getSearchedContacts();
            }
        }).start();
        selectedAdapter = new SelectedAdapter(mSelectedPerson);
        gridSelected.setAdapter(selectedAdapter);
        setGridWidth();
	}

    /**
     * 获取mCurrentChatRoom
     */
    private void getCurrentChatGroup() {
        if (null != mCurrentChatRoom && mPref.getSecurityLevel() == Constants.SECURITY_LEVEL_HIGH) {
            // 判断此群组是否还存在，可能是后台切前台，重新下载群组，但该群组在服务器被删除
            mCurrentChatRoom = mDbHelper.fetchGroupChatRoom(mCurrentChatRoom.groupID);
        }

        if (null == mCurrentChatRoom) {
            // 此处获取群组，为了给mCurrentChatRoom组织公司组织架构
            getMyGroupsFromLocal();
            String currentGroupId = mPref.getDisplayGroupId();
            if (!TextUtils.isEmpty(currentGroupId)) {
                mCurrentChatRoom = mDbHelper.fetchGroupChatRoom(currentGroupId);
            }
            // 没有保存group_id，或没有找到对应群组：获取root chat_room
            if (null == mCurrentChatRoom) {
                mCurrentChatRoom = mDbHelper.fetchRootGroupChatRoom();
            }

            // 组织mCurrentChatRoom的组织结构
            if (null != mCurrentChatRoom && !TextUtils.isEmpty(mCurrentChatRoom.groupID)) {
                for (GroupChatRoom groupChatRoom : mAllGroupRooms) {
                    fillCurrentChatGroup(groupChatRoom);
                }
            }
        }

        mHandler.sendEmptyMessage(HANDLER_DISPLAY_NAME);
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
        mFrequentContacts.clear();
        Person tempPerson = null;
        for (Buddy buddy : frequentBuddies) {
            tempPerson = Person.fromBuddy(buddy);
            if (mSelectedPersonIds.contains(buddy.userID)) {
                tempPerson.setSelected(true);
            }
            mFrequentContacts.add(tempPerson);
        }

        mHandler.sendEmptyMessage(HANDLER_FREQUENT_CONTACTS);
    }

    private void notifyAllPersonSelectedChanged() {
        // frequent contacts
        for (Person person : mFrequentContacts) {
            person.setSelected(mSelectedPersonIds.contains(person.getID()));
        }
        mFrequentContactsAdapter.notifyDataSetChanged();
        ListHeightUtil.setListHeight(mFrequentContactsListView);

        // contacts
        for (Person person : mContacts) {
            person.setSelected(mSelectedPersonIds.contains(person.getID()));
        }
        mContactsAdapter.notifyDataSetChanged();
        ListHeightUtil.setListHeight(mContactsListView);

        // mSearchedContacts 由mFrequentContacts 和 mContacts组合而来，因此不需要重新设置person.setSelected()
        mSearchedContactsAdapter.notifyDataSetChanged();
        ListHeightUtil.setListHeight(mSearchedContactsListView);

        selectedAdapter.notifyDataSetChanged();
    }

    private void notifyPersonSelectedChanged(String personId, boolean isSelected) {
        // frequent contacts
        for (Person person : mFrequentContacts) {
            if (personId.equals(person.getID())) {
                person.setSelected(isSelected);
                break;
            }
        }
//        mFrequentContactsAdapter.notifyDataSetChanged();
//        ListHeightUtil.setListHeight(mFrequentContactsListView);

        // contacts
        for (Person person : mContacts) {
            if (personId.equals(person.getID())) {
                person.setSelected(isSelected);
                break;
            }
        }
//        mContactsAdapter.notifyDataSetChanged();
//        ListHeightUtil.setListHeight(mContactsListView);

        // mSearchedContacts 由mFrequentContacts 和 mContacts组合而来，因此不需要重新设置person.setSelected()
//        mSearchedContactsAdapter.notifyDataSetChanged();

        selectedAdapter.notifyDataSetChanged();
    }

    private void refreshView(String personId, boolean isSelected) {
        if (TextUtils.isEmpty(personId)) {
            notifyAllPersonSelectedChanged();
        } else {
            notifyPersonSelectedChanged(personId, isSelected);
        }
        btnOK.setText(String.format(getResources().getString(R.string.ok_with_num),
                String.valueOf(mSelectedPerson.size())));
		// If there is no selected person, or one person who is added when create this multi-chat from single-chat, except myself
		if (isOnlyContainsInitPersons()) {
			btnOK.setEnabled(false);
		} else {
			btnOK.setEnabled(true);
		}
		setGridWidth();
	}

	/**
	 * Judge whether the selectedPersons only contains the initPerson
	 * who is added when create the multi-chat from single-chat(except myself)
	 * @return true, only one person who is added when create the multi-chat from single-chat;
	 *     false, not only
	 */
    private boolean isOnlyContainsInitPersons() {
        boolean isOnlyInitPerson = true;
        for (Person selectedPerson : mSelectedPerson) {
            // there is new member added
            if (!mInitMemberIds.contains(selectedPerson.getID())) {
                isOnlyInitPerson = false;
                break;
            }
        }
        Log.d("multi-select, selectedMembers is " + mSelectedPerson.size()
                + ", mInitMemberIds is " + mInitMemberIds.size() + ", isOnlyInitPerson is " + isOnlyInitPerson);
        return isOnlyInitPerson;
    }

    private void commitSelection() {
        if(mSelectedPerson.isEmpty()) {
            this.setResult(Activity.RESULT_CANCELED);
            finish();
        }

        final Person[] persons = new Person[mSelectedPerson.size()];
        mSelectedPerson.toArray(persons);

        // mSelectedPerson doesn't contain myself).
        if(1 == mSelectedPerson.size() && TextUtils.isEmpty(mOriginalGid)) {
            Intent data = new Intent();
            data.putExtra("is_group_chat", false);
            data.putExtra("buddy_id", mSelectedPerson.get(0).getID());
            this.setResult(RESULT_OK, data);
            finish();
        } else {
            mMsgBox.showWait();

            new AsyncTask<Parcelable, Integer, String>() {
                @Override
                protected String doInBackground(Parcelable... params) {
                    String gid = null;
                    if (TextUtils.isEmpty(mOriginalGid)) {
                        gid = GroupChatRoomHelper.createTmp(params,
                                WowTalkWebServerIF.getInstance(MultiSelectActivityForBiz.this),
                                mDbHelper,
                                MultiSelectActivityForBiz.this);
                    } else {
                        GroupChatRoomHelper.addMembers(
                                mOriginalGid,
                                params,
                                WowTalkWebServerIF.getInstance(MultiSelectActivityForBiz.this),
                                mDbHelper,
                                MultiSelectActivityForBiz.this);
                        gid = mOriginalGid;
                    }
                    return gid;
                }

                @Override
                protected void onPostExecute(String gid) {
                    mMsgBox.dismissWait();
                    if(TextUtils.isEmpty(gid)) {
                        mMsgBox.toast(R.string.operation_failed);
                        setResult(Activity.RESULT_CANCELED);
                    } else {
                        Intent data = new Intent();
                        data.putExtra("is_group_chat", true);
                        data.putExtra("gid", gid);
                        setResult(RESULT_OK, data);
                    }
                    finish();
                }
            }.execute(persons);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
                handleBackEvent();
                break;
            case R.id.group_enter:
                if (mCurrentDisplayMode != DISPLAY_MODE_GROUP) {
                    changeDisplayMode(DISPLAY_MODE_GROUP);
                    getMyGroupsFromLocal();
                    // 登录的刷新群组和成员完成了，也是其中一个刷新的前提条件；如果登录后的刷新未完成，此时不需要刷新
                    if (mPref.isGroupMembersUptodatePerfectly() && GlobalValue.isNeedToRefreshAllGroups()) {
                        Log.i("MultiSelectActivityForBiz#onClick(group_enter), need to refresh from server.");
                        getMyGroupsFromServer();
                    }
                } else {
                    changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
                }
                break;
            case R.id.field_clear:
                edtSearch.setText("");
                changeDisplayMode(DISPLAY_MODE_SEARCHING);
                break;
             // 取消搜索，获取点击覆盖的mSearchGlassImageView
            case R.id.cancel_btn:
            case R.id.search_glass_img:
            case R.id.layout_groups:
                // 如果软键盘仍存在，需要手动关闭
                InputMethodManager inputManager = ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE));
                inputManager.hideSoftInputFromWindow(
                        edtSearch.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                if (edtSearch.length() != 0) {
                    edtSearch.setText("");
                }
                changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
                break;
            case R.id.txt_group:
                Intent groupChatIntent = new Intent(MultiSelectActivityForBiz.this, PickTempGroupActivity.class);
                startActivity(groupChatIntent);
                break;
            case R.id.txt_local_contacts:
                LocalContactsActivity.launchForMultiSelecting(this, REQ_SELECT_LOCAL_CONTACTS);
                break;
            case R.id.frequent_contacts_subtitle_layout:
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
            case R.id.btn_ok:
                commitSelection();
//                finish();
                break;
            default:
                break;
        }
    }

    /**
     * 设置当前显示的群组名称，首先获取当前群组
     */
    private void setDispalyGroupName() {
        if (null != mCurrentChatRoom) {
            if (!mCurrentChatRoom.isEditable && TextUtils.isEmpty(mCurrentChatRoom.parentGroupId)) {
                mTitleText.setText(R.string.contactsforbiz_root_group_name_display);
            } else {
                mTitleText.setText(mCurrentChatRoom.groupNameOriginal);
            }
            // 只采用联系人界面保存的当前群组，多人会话选择时，不保存
            // 保存最新显示的group
//            mPref.setDisplayGroupId(mCurrentChatRoom.groupID);
        }
    }

    private void getMyGroupsFromServer() {
        new AsyncTask<Void, Void, Integer> () {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(MultiSelectActivityForBiz.this)
                        .getGroupsByCompanyId(PrefUtil.getInstance(MultiSelectActivityForBiz.this).getCompanyId());
            }

            protected void onPostExecute(Integer result) {
                Log.i("MultiSelectActivityForBiz#getMyGroupsFromServer, resultCode is " + result);
                // 重新获取群组，会重新刷新群组列表，导致展开的状态不正确，等待下一次再打开时显示
                if (result == ErrorCode.OK/* && mCurrentDisplayMode == DISPLAY_MODE_GROUP*/) {
                    GlobalValue.setNeedToRefreshAllGroups(false);
//                    getMyGroupsFromLocal();
                }
            };
        }.execute((Void)null);
    }

	/**
     * 获取所有群组，树形结构(根节点：收藏群组，一般群组)
     */
    private void getMyGroupsFromLocal() {
        mFavoriteGroupRooms.clear();
        mFavoriteGroupRooms = mDbHelper.fetchFavoriteGroupChatRooms();
        mAllGroupRooms.clear();
        mAllGroupRooms = mDbHelper.fetchNonTempGroupChatRooms(true);
        // 处理 mGroupRooms，为childGroups赋值
        ArrayList<GroupChatRoom> rootGroups = new ArrayList<GroupChatRoom>();
        rootGroups.add(treeFavoriteGroups());
        rootGroups.addAll(ContactsActivityForBiz.treeAllGroupRooms(MultiSelectActivityForBiz.this, mAllGroupRooms));

        if (null == mGroupIterationAdapter) {
            mGroupIterationAdapter = new ContactGroupIterationAdapter(MultiSelectActivityForBiz.this, rootGroups, mSelectedGroupListener);
            mGroupsListView.setAdapter(mGroupIterationAdapter);
        } else {
            mGroupIterationAdapter.setRootDataSource(rootGroups);
            mGroupIterationAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 构造收藏的群组树
     * @return
     */
    private GroupChatRoom treeFavoriteGroups() {
        // 构造最外层的收藏的根群组
        String rootRoomName = getString(R.string.contactsforbiz_root_favorite_group_name);
        GroupChatRoom rootChatRoom = new GroupChatRoom(ContactGroupIterationAdapter.GROUP_ID_ROOT,
                rootRoomName, rootRoomName, "", 0, 0, false);
        rootChatRoom.isEditable = false;
        rootChatRoom.parentGroupId = null;
        rootChatRoom.childGroups = new ArrayList<GroupChatRoom>();
        // 将收藏的群组的parentGroupId设置为构造的根群组的id
        for (GroupChatRoom favoriteGroup : mFavoriteGroupRooms) {
            favoriteGroup.parentGroupId = ContactGroupIterationAdapter.GROUP_ID_ROOT;
            rootChatRoom.childGroups.add(favoriteGroup);
        }
        return rootChatRoom;
    }

    private void getLocalNormalContacts() {
        ArrayList<Buddy> members =
                mDbHelper.fetchNormalBuddies();

        mContactsAll = new ArrayList<Person>();
        Person temPerson = null;
        for (Buddy buddy : members) {
            // 结果中不包含自己
            if (!mMyOwnId.equals(buddy.userID)) {
                temPerson = Person.fromBuddy(buddy);
                if (mSelectedPersonIds.contains(buddy.userID)) {
                    temPerson.setSelected(true);
                }
                mContactsAll.add(temPerson);
            }
        }

        int endIndex = LOAD_COUNT_PER_TIME >= mContactsAll.size()
                ? mContactsAll.size() : LOAD_COUNT_PER_TIME;
        List<Person> subList = mContactsAll.subList(0, endIndex);
        mContacts = new ArrayList<Person>();
        mContacts.addAll(subList);

        mHandler.sendEmptyMessage(HANDLER_CONTACTS_LIST);
    }

    /**
     * 根据当前群组，查找成员，包括子部门的成员
     */
    private void getContactsListFromLocal() {
        // 当前群组为空，说明未下载到群组
        if (null == mCurrentChatRoom) {
            Log.e("MultiSelectActivityForBiz#getContactsListFromLocal, the current dept is null.");
            return;
        }
        ArrayList<GroupMember> members =
                mDbHelper.fetchGroupMembers(mCurrentChatRoom.groupID);

        mContactsAll = new ArrayList<Person>();
        Person temPerson = null;
        for (GroupMember groupMember : members) {
            // 结果中不包含自己
            if (!mMyOwnId.equals(groupMember.userID)) {
                temPerson = Person.fromBuddy(groupMember);
                if (mSelectedPersonIds.contains(groupMember.userID)) {
                    temPerson.setSelected(true);
                }
                mContactsAll.add(temPerson);
            }
        }

        int endIndex = LOAD_COUNT_PER_TIME >= mContactsAll.size()
                ? mContactsAll.size() : LOAD_COUNT_PER_TIME;
        List<Person> subList = mContactsAll.subList(0, endIndex);
        mContacts = new ArrayList<Person>();
        mContacts.addAll(subList);

        mHandler.sendEmptyMessage(HANDLER_CONTACTS_LIST);
    }

    private void loadMorePersons() {
        if (mIsLoadingMore) {
            return;
        }
        mIsLoadingMore = true;
        setLoadMoreView(false);
        int start = mContacts.size();
        int end = start + LOAD_COUNT_PER_TIME >= mContactsAll.size()
                ? mContactsAll.size() : start + LOAD_COUNT_PER_TIME;
        List<Person> subList = mContactsAll.subList(start, end);
        for (Iterator<Person> iterator = subList.iterator(); iterator.hasNext();) {
            Person person = iterator.next();
            if (mMyOwnId.equals(person.getID())) {
                iterator.remove();
            } else if (mSelectedPersonIds.contains(person.getID())) {
                person.setSelected(true);
            }
        }
        mContacts.addAll(subList);
        mHandler.sendEmptyMessage(HANDLER_CONTACTS_LIST);
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

//    private void getContactsListFromServer(final String groupID) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int resultCode = WowTalkWebServerIF.getInstance(MultiSelectActivityForBiz.this).fGroupChat_GetMembersIteration(groupID);
//                Log.i("MultiSelectActivityForBiz#getContactsListFromServer by groupId(" + groupID + "), resultCode is " + resultCode);
//                if (resultCode == ErrorCode.OK) {
//                    GlobalValue.setDontNeedToRefreshMembers(groupID);
//                    // 服务器数据加载完成后，还是原先请求的时候的群组，则刷新界面；否则不做处理
//                    if (groupID.equals(mCurrentChatRoom.groupID)) {
//                        // 此处需要刷新收藏的联系人，因为可能原先收藏的某些联系人没有下载到本地
//                        getFrequentContacts();
//                        getContactsListFromLocal();
//                        getSearchedContacts();
//                    }
//                }
//            }
//        }).start();
//    }

    private void getSearchedContacts() {
        mSearchedContacts.clear();
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

        mHandler.sendEmptyMessage(HANDLER_SEARCHED_CONTACTS);
    }

    private void handleBackEvent() {
        // if in the search mode, return the normal mode first.
        if (edtSearch.getText().length() != 0) {
            edtSearch.setText("");
        } else {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
        mMsgBox = new MessageBox(this);

        setContentView(R.layout.activity_multi_select_biz);
        mDbHelper = new Database(MultiSelectActivityForBiz.this);
        mPref = PrefUtil.getInstance(MultiSelectActivityForBiz.this);
        mMyOwnId = mPref.getUid();
        Intent intent = getIntent();
        if (null != intent){
            mIsOnlyShowContacts = intent.getBooleanExtra("isOnlyShowContacts", false);
            String[] initIds = intent.getStringArrayExtra("currentMemberIds");
            mOriginalGid = intent.getStringExtra("group_id");
            if (null != initIds) {
                Buddy selectedBuddy = null;
                for (String initId : initIds) {
                    if (!mMyOwnId.equals(initId)) {
                        mInitMemberIds.add(initId);
                        selectedBuddy = mDbHelper.buddyWithUserID(initId);
                        if (null != selectedBuddy) {
                            mSelectedPersonIds.add(selectedBuddy.userID);
                            mSelectedPerson.add(Person.fromBuddy(selectedBuddy));
                        }
                    }
                }
            }
        }

        initView();

        changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
        setData();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_FINISH_LOAD_MEMBERS, mLoadMembersAfterLoginObserver);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Database.removeDBTableChangeListener(mLoadMembersAfterLoginObserver);
    }

    @Override
    protected void onActivityResult(int requestCode, int requestResult, Intent data) {
        super.onActivityResult(requestCode, requestResult, data);
        if(requestCode == REQ_SELECT_LOCAL_CONTACTS && requestResult == RESULT_OK) {
            List<Person> persons = data.getParcelableArrayListExtra(
                    LocalContactsActivity.EXTRA_SELECTION);
            for (Iterator<Person> iterator = persons.iterator(); iterator.hasNext();) {
                Person person = iterator.next();
                for (Person p : mSelectedPerson) {
                    if (person.getSortKey().equals(p.getSortKey())) {
                        iterator.remove();
                        // if the person is equals to the current selected person, break the inner loop.
                        break;
                    }
                }
            }
            if(persons != null && !persons.isEmpty()) {
                mSelectedPerson.addAll(persons);
                refreshView(null, true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentDisplayMode == DISPLAY_MODE_CONTACTS_WITH_SEARCH) {
            handleBackEvent();
        } else {
            changeDisplayMode(DISPLAY_MODE_CONTACTS_WITH_SEARCH);
        }
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

            if (mContactsAdapter == null)
                return;
            ArrayList<Person> persons = mContactsAdapter.getPersonSource();
            if (persons == null || persons.isEmpty())
                return;

            // TODO 此处计算height，需要根据xml中的布局来调整，修改布局文件后，此处也需要修改
            int height = 0;
            // search_layout
            View heightView = mMainRelativeLayout.getChildAt(0);
            if (heightView != null) {
                height += heightView.getHeight();
            }
            // contacts_layout
            heightView = mMainRelativeLayout.getChildAt(1);
            if (heightView instanceof RelativeLayout) {
                RelativeLayout relativeLayout = (RelativeLayout) heightView;
                // multi_chat_layout
                heightView = relativeLayout.getChildAt(0);
                if (null != heightView) {
                    height += heightView.getHeight();
                }
                // layout_frequent_contacts
                heightView = relativeLayout.getChildAt(1);
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
}
