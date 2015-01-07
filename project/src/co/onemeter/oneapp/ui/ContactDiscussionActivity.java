package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactDiscussionAdapter;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.Utils;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContactDiscussionActivity extends Activity implements OnClickListener{

    private static final int REQ_CREATE_DISCUSSION = 1;

    private ImageButton mBackBtn;
    private ImageButton mEditBtn;
    private TextView mSearchView;
    private ImageButton mClearBtn;
    private ListView mGroupLists;

    private List<GroupChatRoom> mInitGroupRooms;
    private List<GroupChatRoom> mFilterGroupRooms;
    private ContactDiscussionAdapter mGroupAdapter;
    private Database mDbHelper;
    private PrefUtil mPrefUtil;

    private TextWatcher mTextWatcher = new TextWatcher() {
        private int mLastLetterCounts;
        private int mCurrentLetterCounts;

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mCurrentLetterCounts = s.length();

            if (mCurrentLetterCounts == 0) {
                mClearBtn.setVisibility(View.GONE);
                filterGroups(s.toString().trim(), false);
            } else {
                mClearBtn.setVisibility(View.VISIBLE);
                filterGroups(s.toString().trim(), mCurrentLetterCounts > mLastLetterCounts);
            }

            mGroupAdapter.setDataSource(mFilterGroupRooms);
            mGroupAdapter.notifyDataSetChanged();
            mLastLetterCounts = mCurrentLetterCounts;
        }

        /**
         * filter groupRooms
         * @param filterString filter string characters
         * @param isGoOnFiltering filter groups in the filter result groups
         */
        private void filterGroups(String filterString, boolean isGoOnFiltering) {
            // there is no filterString
            if (TextUtils.isEmpty(filterString)) {
                mFilterGroupRooms.clear();
                mFilterGroupRooms.addAll(mInitGroupRooms);
            } else if (isGoOnFiltering){
                GroupChatRoom groupRoom = null;
                for (Iterator<GroupChatRoom> iterator = mFilterGroupRooms.iterator(); iterator.hasNext();) {
                    groupRoom = iterator.next();
                    if (!Utils.isMatchFilters(true, filterString, groupRoom.groupNameOriginal)) {
                        iterator.remove();
                    }
                }
            } else {
                mFilterGroupRooms.clear();
                for (GroupChatRoom groupRoom : mInitGroupRooms) {
                    if (Utils.isMatchFilters(true, filterString, groupRoom.groupNameOriginal)) {
                        mFilterGroupRooms.add(groupRoom);
                    }
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);
        initView();

        mDbHelper = new Database(this);
        mPrefUtil = PrefUtil.getInstance(this);
    }

    private void initView() {
        mBackBtn = (ImageButton) findViewById(R.id.title_back);
        mEditBtn = (ImageButton) findViewById(R.id.title_edit);
        mSearchView = (TextView) findViewById(R.id.edt_search);
        mClearBtn = (ImageButton) findViewById(R.id.field_clear);
        mGroupLists = (ListView) findViewById(R.id.group_list);

        mBackBtn.setOnClickListener(this);
        mEditBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
        mSearchView.addTextChangedListener(mTextWatcher);
        mGroupLists.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                GroupChatRoom groupRoom = mFilterGroupRooms.get(position);
                MessageComposerActivity.launchToChatWithGroup(
                        ContactDiscussionActivity.this,
                        MessageComposerActivity.class,
                        groupRoom);
            }
        });
    }

    private void refreshData() {
        refreshGroupsFromLocal();
    }

    private void refreshGroupsFromLocal() {
        mInitGroupRooms = mDbHelper.fetchNonTempGroupChatRooms();
        mFilterGroupRooms = new ArrayList<GroupChatRoom>();
        mFilterGroupRooms.addAll(mInitGroupRooms);
        mGroupAdapter = new ContactDiscussionAdapter(this, mFilterGroupRooms);
        mGroupLists.setAdapter(mGroupAdapter);
    }

    private void handleBackEvent() {
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            handleBackEvent();
            break;
        case R.id.title_edit:
            Intent discussionIntent = new Intent();
            discussionIntent.setClass(this, MultiSelectActivity.class);
            ThemeHelper.putExtraCurrThemeResId(discussionIntent, this);
            startActivityForResult(discussionIntent, REQ_CREATE_DISCUSSION);
            break;
        case R.id.field_clear:
            mSearchView.setText("");
            break;
        default:
            break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CREATE_DISCUSSION && resultCode == RESULT_OK) {
            Parcelable[] parArr = data.getExtras().getParcelableArray("persons");
            if(parArr != null && parArr.length > 0) {
                
                if(1 == parArr.length && !((Person) parArr[0]).getID().equals("")) {
                    Person p = (Person)parArr[0];
                    MessageComposerActivity.launchToChatWithBuddy(
                            this,
                            MessageComposerActivity.class,
                            p.getID());
                } else {
                    String gid=data.getExtras().getString("gid");
                    if(!TextUtils.isEmpty(gid)) {
                        MessageComposerActivity.launchToChatWithGroup(
                                ContactDiscussionActivity.this,
                                MessageComposerActivity.class,
                                gid);
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackEvent();
    }
}
