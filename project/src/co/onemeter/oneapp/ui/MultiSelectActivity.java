package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.RoundedImageView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MultiSelectActivity extends Activity implements OnClickListener {

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
				lView = LayoutInflater.from(MultiSelectActivity.this).inflate(R.layout.listitem_group_selected, null);
			} else {
				lView = convertView;
			}
			final RoundedImageView imgThumbnail = (RoundedImageView) lView.findViewById(R.id.img_thumbnail);
            imgThumbnail.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position < adapterSelectedPerson.size()) {
                        Person selectedPerson = adapterSelectedPerson.get(position);
                        selectedPerson.setSelected(false);
                        adapterSelectedPerson.remove(position);

                        // the person is contained in mCurrentMemberIds, it doesn't be displayed in list, so we should add it to the lvContact list.
                        if (isNeededToAddToLvContact(selectedPerson.getID())) {
                            // 将从单人会话转多人会话时，带进来的那个未显示在联系人列表中的person添加进去，
                            // 由于联系人排序问题，不能直接add在最后，需要重新获取ContactUtil.allPersons，并设置isSelected属性
                            ArrayList<Person> newSelectedPersons = new ArrayList<Person>();
                            ContactUtil.fFetchNormalPersons(MultiSelectActivity.this);
                            // Set the selection of allPersons.
                            if (!adapterSelectedPerson.isEmpty()) {
                                for (Iterator<Person> iterator = ContactUtil.allPersons.iterator(); iterator.hasNext();) {
                                    Person eachOfAllPerson = iterator.next();
                                    for (Person tempSelectedPerson : adapterSelectedPerson) {
                                        if (tempSelectedPerson.getID().equals(eachOfAllPerson.getID())) {
                                            eachOfAllPerson.setSelected(true);
                                            newSelectedPersons.add(eachOfAllPerson);
                                            continue;
                                        }
                                    }
                                }
                                // 在选中的列表中，删除成员时，会更新确认按钮上的计数
                                // 计数采用的时全局变量mSelectedPerson，而不是Adapter中的adapterSelectedPerson，但这两个变量指向同一个引用，所以可以实现
                                // 因此此处不能给adapterSelectedPerson用"="重新赋值，只能修改其内容，不能修改引用。
                                adapterSelectedPerson.clear();
                                adapterSelectedPerson.addAll(newSelectedPersons);
                            }
                            contactAdapter.setPersonSource(ContactUtil.allPersons);
                        }

                        refreshView();
                    }
                }
            });
			if (position < adapterSelectedPerson.size()) {
                Person p = adapterSelectedPerson.get(position);
                if(!TextUtils.isEmpty(p.getID())) {
                    Buddy buddy = new Buddy();
                    buddy.userID = adapterSelectedPerson.get(position).getID();
                    buddy.photoUploadedTimeStamp = adapterSelectedPerson.get(position).photoUploadedTimestamp;
                    PhotoDisplayHelper.displayPhoto(MultiSelectActivity.this, imgThumbnail, R.drawable.default_avatar_90, buddy, true);
                } else {
                    // local (non-wowtalk) user?
                    // TODO display avatar & nick
                    if (p.getLocalPersonPhotoID() > 0) {
                        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, p.getLocalContactID());
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(MultiSelectActivity.this.getContentResolver(), uri);
                        imgThumbnail.setImageBitmap(BitmapFactory.decodeStream(input));
                    } else {
                        PhotoDisplayHelper.displayPhoto(MultiSelectActivity.this, imgThumbnail, R.drawable.default_avatar_90, null, true);
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
	 * change the chat mode from single to multi-chat
	 */
	public static final String INTENT_CHANGE_MULTI_FROM_SINGLE = "intent_change_multi_from_single";
    private static final int REQ_SELECT_LOCAL_CONTACTS = 123;
    private static MultiSelectActivity instance;
	private ImageButton btnTitleBack;
//	private View mFilterView;
	private View mSelectMultiView;
	private EditText edtSearch;
	private ImageButton mClearButton;
	private View btnSelectGroup;
	private ImageView mDividerImage;
    private View btnSelectLocalContacts;
	private ListView lvContact;
	private GridView gridSelected;
	private Button btnOK;
	
	private ContactListAdapter contactAdapter;
	private SelectedAdapter selectedAdapter;
	private ArrayList<Person> mSelectedPerson = new ArrayList<Person>();
	ArrayList<LatestChatTarget> latestContacts = new ArrayList<LatestChatTarget>();
	private String[] mCurrentMemberIds;

	/**
	 * whether should display the enter of chatRooms at the head of it.
	 */
	private boolean mIsOnlyShowContacts;
	/**
	 * this multi-select is from the single-chat
	 */
	private boolean mIsChangeMultiFromSingle;
	/**
	 * the original person id if this multi-select is from the single-chat
	 */
	private String mInitPersonIdFromSingle;

	/**
	 * the original person who is from the single-chat has been removed from selectedPerson, and added to the lvContact list
	 */
	private boolean mHasInitPersonAddedToLvContacts;

    private MessageBox mMsgBox;

	private TextWatcher mSearchWacher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(contactAdapter == null)
                return;

            if (s.length() == 0) {
                mClearButton.setVisibility(View.GONE);
                contactAdapter.setPersonSource(ContactUtil.allPersons);
                contactAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(lvContact);
            } else {
                mClearButton.setVisibility(View.VISIBLE);
                ContactUtil.fFetchPersonsFromBuddyByCondition(s.toString(), true);
                contactAdapter.setPersonSource(ContactUtil.curPersons);
                contactAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(lvContact);
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

    public static MultiSelectActivity instance() {
        if (instance == null)
            return new MultiSelectActivity();
        return instance;
    }

    private IDBTableChangeListener mFinishLoadBuddiesObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("MultiSelectActivity, load buddies finished, trigger the mFinishLoadBuddiesObserver.");
                    mMsgBox.dismissWait();
                    setData();
                }
            });
        }
    };

	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		lvContact = (ListView) findViewById(R.id.list_contact);
		gridSelected = (GridView) findViewById(R.id.grid_selected);
		btnOK = (Button) findViewById(R.id.btn_ok);
		btnOK.setText(String.format(getResources().getString(R.string.ok_with_num), mSelectedPerson.size()));
		btnOK.setEnabled(false);

        LayoutInflater li = this.getLayoutInflater();
//        mFilterView = li.inflate(R.layout.piece_filter_bar, null);
        mSelectMultiView = li.inflate(R.layout.activity_multi_select_list_header, null);
        lvContact.setVisibility(View.VISIBLE);
//        lvContact.addHeaderView(mFilterView);
        lvContact.addHeaderView(mSelectMultiView);

        edtSearch = (EditText) findViewById(R.id.edt_search);
        mClearButton = (ImageButton) findViewById(R.id.field_clear);
        btnSelectGroup = findViewById(R.id.txt_group);
        btnSelectLocalContacts = findViewById(R.id.txt_local_contacts);
        mDividerImage = (ImageView) findViewById(R.id.divider_image);
        if (mIsOnlyShowContacts) {
            btnSelectGroup.setVisibility(View.GONE);
            mDividerImage.setVisibility(View.GONE);
        }

		btnTitleBack.setOnClickListener(this);
		mClearButton.setOnClickListener(this);
		btnSelectGroup.setOnClickListener(this);
        mDividerImage.setOnClickListener(this);
        btnSelectLocalContacts.setOnClickListener(this);
		btnOK.setOnClickListener(this);
		edtSearch.addTextChangedListener(mSearchWacher);
		lvContact.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> view, View parent, int position,
					long id) {
                position -= lvContact.getHeaderViewsCount();
                Person clickedPerson = contactAdapter.getPersonSource().get(position);
				final boolean isSelected = clickedPerson.isSelected();
				if (isSelected) {
			        mSelectedPerson.remove(clickedPerson);
				} else {
					mSelectedPerson.add(clickedPerson);
				}
				Log.e("refresh view selectedPerson size : " + mSelectedPerson.size());
				clickedPerson.setSelected(!isSelected);

				refreshView();
			}
			
		});
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
        if (null == mCurrentMemberIds || mCurrentMemberIds.length == 0){
            contactAdapter = new ContactListAdapter(this);
        } else {
            contactAdapter = new ContactListAdapter(this, mCurrentMemberIds);
        }
        contactAdapter.setDefaultSelected(true);
        lvContact.setAdapter(contactAdapter);

		selectedAdapter = new SelectedAdapter(mSelectedPerson);
		gridSelected.setAdapter(selectedAdapter);
		setGridWidth();
	}
	
	private void refreshView() {
		contactAdapter.notifyDataSetChanged();

		selectedAdapter.notifyDataSetChanged();
		btnOK.setText(String.format(getResources().getString(R.string.ok_with_num),
                String.valueOf(mSelectedPerson.size())));
		// If there is no selected person, or one person who is added when create this multi-chat from single-chat
		if (mSelectedPerson.size() == 0 || isOnlyContainsInitPerson()) {
			btnOK.setEnabled(false);
		} else {
			btnOK.setEnabled(true);
		}
		setGridWidth();
	}

	/**
	 * Judge whether the selectedPersons only contains the initPerson
	 * who is added when create the multi-chat from single-chat
	 * @return true, only one person who is added when create the multi-chat from single-chat;
	 *     false, not only
	 */
	private boolean isOnlyContainsInitPerson() {
	    boolean isOnlyInitPerson = false;
	    if (mIsChangeMultiFromSingle && !TextUtils.isEmpty(mInitPersonIdFromSingle) && mSelectedPerson.size() == 1) {
	        isOnlyInitPerson = mInitPersonIdFromSingle.equals(mSelectedPerson.get(0).getID());
	    }
	    return isOnlyInitPerson;
	}

	/**
	 * Whether the person should be added to the lvContact list
	 * @param personId
	 * @return true, if the person is initPerson from single-chat, and has removed from selectedPerson.
	 */
	private boolean isNeededToAddToLvContact(String personId) {
	    boolean isNeeded = false;
        if (!mHasInitPersonAddedToLvContacts && mIsChangeMultiFromSingle && !TextUtils.isEmpty(mInitPersonIdFromSingle)) {
            isNeeded = mInitPersonIdFromSingle.equals(personId);
            // if isNeeded is false, the field mIsInitPersonNeededToAddToLvContacts should not be changed.
            if (isNeeded) {
                mHasInitPersonAddedToLvContacts = true;
            }
        }
        Log.i("MultiSelectActivity, isNeededToAddToLvContact list(" + isNeeded + ")");
        return isNeeded;
	}

	private String mOriginalGid;
	private Database mDbHelper;
	
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

            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Parcelable, Integer, String>() {
                @Override
                protected String doInBackground(Parcelable... params) {
                    String gid = null;
                    if (TextUtils.isEmpty(mOriginalGid)) {
                        gid = GroupChatRoomHelper.createTmp(params,
                                WowTalkWebServerIF.getInstance(MultiSelectActivity.this),
                                mDbHelper,
                                MultiSelectActivity.this);
                    } else {
                        GroupChatRoomHelper.addMembers(
                                mOriginalGid,
                                params,
                                WowTalkWebServerIF.getInstance(MultiSelectActivity.this),
                                mDbHelper,
                                MultiSelectActivity.this);
                        gid = mOriginalGid;
                    }
                    return gid;
                }

                @Override
                protected void onPostExecute(String gid) {
                    mMsgBox.dismissWait();
                    if (gid == null || TextUtils.isEmpty(gid)) {
                        mMsgBox.toast(R.string.operation_failed);
                        setResult(Activity.RESULT_CANCELED);
                    } else {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
                        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date curDate = new Date(System.currentTimeMillis());
                        String str = formatter.format(curDate);

                        LatestChatTarget latest = new LatestChatTarget(gid, str, true);
                        latestContacts.add(latest);
                        mDbHelper.storeLatestChatTargets(latestContacts, true);
                        Intent data = new Intent();
//                        data.putExtra("is_group_chat", true);
                        data.putExtra("persons", persons);
                        data.putExtra("gid", gid);
                        setResult(RESULT_OK, data);
                    }
                    finish();
                }
            }, persons);
        }
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
            case R.id.title_back:
                handleBackEvent();
                break;
            case R.id.field_clear:
                edtSearch.setText("");
                break;
            case R.id.txt_group:
                Intent groupChatIntent = new Intent(MultiSelectActivity.this, PickTempGroupActivity.class);
                startActivity(groupChatIntent);
                break;
            case R.id.divider_image:
                //no handle for bug 1935
                break;
            case R.id.txt_local_contacts:
                LocalContactsActivity.launchForMultiSelecting(this, REQ_SELECT_LOCAL_CONTACTS);
                break;
            case R.id.btn_ok:
                commitSelection();
//                finish();
                break;
            default:
                break;
        }
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

        ThemeHelper.setTheme(this, getIntent().getExtras());

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
        mMsgBox = new MessageBox(this);

		setContentView(R.layout.activity_multi_select);
		Intent intent = getIntent();
		if (null != intent){
		    mCurrentMemberIds = intent.getStringArrayExtra("currentMemberIds");
		    mIsOnlyShowContacts = intent.getBooleanExtra("isOnlyShowContacts", false);
		    mOriginalGid = intent.getStringExtra("group_id");
		    mDbHelper = new Database(this);
		    mIsChangeMultiFromSingle = intent.getBooleanExtra(INTENT_CHANGE_MULTI_FROM_SINGLE, false);
		    if (mIsChangeMultiFromSingle && null != mCurrentMemberIds && mCurrentMemberIds.length > 0) {
		        // the personId of the person whose chat is this multi-chat changed from.
		        mInitPersonIdFromSingle = mCurrentMemberIds[0];
		        Database database = new Database(MultiSelectActivity.this);
		        Buddy selectedBuddy = database.buddyWithUserID(mInitPersonIdFromSingle);
		        mSelectedPerson.add(Person.fromBuddy(selectedBuddy));
		    }
		}
		initView();
		setData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FINISH_LOAD_CONTACTS,mFinishLoadBuddiesObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        Database.removeDBTableChangeListener(mFinishLoadBuddiesObserver);
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
                refreshView();
            }
        }
    }

    @Override
    public void onBackPressed() {
        handleBackEvent();
    }

}
