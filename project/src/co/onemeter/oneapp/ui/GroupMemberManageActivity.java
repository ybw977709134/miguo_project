package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.RoundedImageView;

import java.util.ArrayList;

public class GroupMemberManageActivity extends Activity implements View.OnClickListener {
    public static final String INTENT_DATA_ACCEPT = "accept";
    public static final String INTENT_DATA_REJECT = "reject";
    public static final String INTENT_DATA_BUDDY= "buddy";

    private static final int REQ_APPLY_MANAGE = 1;
    private Database mDBHelper;

    private class ApplyAdapter extends BaseAdapter {
        private Context mContext;

        public ApplyAdapter(Context context) {
            mContext = context;
            mDBHelper = new Database(context);
        }

        @Override
        public int getCount() {
            return pendingRequests.size();
        }

        @Override
        public Object getItem(int position) {
            return pendingRequests.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PendingRequest p = pendingRequests.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_group_apply, parent, false);
            } 
            RoundedImageView imgPhoto = (RoundedImageView) convertView.findViewById(R.id.img_photo);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            imgPhoto.setBackgroundDrawable(null);
            Buddy b = new Buddy(p.uid);
            b.photoUploadedTimeStamp = p.buddy_photo_timestamp;
            PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_avatar_90, b, true);
            txtName.setText(p.nickname);
            return convertView;
        }
    }

    private class MemberAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<GroupMember> persons;
        private boolean withButton;

        public MemberAdapter(Context context, ArrayList<GroupMember> persons, boolean withButton) {
            mContext = context;
            this.persons = persons;
            this.withButton = withButton;
        }

        @Override
        public int getCount() {
            return persons.size();
        }

        @Override
        public Object getItem(int position) {
            return persons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final GroupMember person = persons.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_member_in_groupmanage, parent, false);
            }
            RoundedImageView imgPhoto = (RoundedImageView) convertView.findViewById(R.id.img_photo);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            final Button btnSet = (Button) convertView.findViewById(R.id.btn_set);
            PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_avatar_90, person, true);
            txtName.setText(TextUtils.isEmpty(person.alias) ? person.nickName : person.alias);
            btnSet.setVisibility(withButton ? View.VISIBLE : View.GONE);
            if (person.getLevel() == GroupMember.LEVEL_ADMIN) {
                btnSet.setBackgroundResource(R.drawable.btn_blue_medium);
                btnSet.setText(getString(R.string.groupmembermanage_cancel_admin));
                btnSet.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeBuddyLevel(person, btnSet);
                    }
                });
            } else {
                btnSet.setBackgroundResource(R.drawable.btn_gray_medium_selector);
                btnSet.setText(getString(R.string.group_set_as_admin));
                btnSet.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeBuddyLevel(person, btnSet);
                    }
                });
            }
            return convertView;
        }

        protected void changeBuddyLevel(final GroupMember person, final Button btnSet) {
            final String groupID = person.groupID;
            final String userID = person.userID;
            // get the level of the person when click the button every time.
            int currentLevel = person.getLevel();
            final int targetLevel = (currentLevel == GroupMember.LEVEL_DEFAULT) ? GroupMember.LEVEL_ADMIN : GroupMember.LEVEL_DEFAULT;
            Log.i("GroupMemberManageActivity, set the member level to " + targetLevel);

            final MessageBox msgBox = new MessageBox(mContext);
            msgBox.showWait();
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

                @Override
                protected Integer doInBackground(Void... params) {
                    int resultCode = WowTalkWebServerIF.getInstance(mContext).fGroupChat_SetMemberLevel(groupID, userID, targetLevel);
                    return resultCode;
                }

                protected void onPostExecute(Integer result) {
                    msgBox.dismissWait();

                    if (result == ErrorCode.OK) {
                        mDBHelper.updateGroupMemberLevel(groupID, userID, targetLevel);

                        switch (targetLevel) {
                            case GroupMember.LEVEL_ADMIN:
                                // Change the text and background to admin.
                                btnSet.setBackgroundResource(R.drawable.btn_blue_medium);
                                btnSet.setText(getString(R.string.groupmembermanage_cancel_admin));

                                // change the buddy_level of it.
                                person.setLevel(GroupMember.LEVEL_ADMIN);
                                // Set the managerMembers
                                managePersons.add(person);
                                break;
                            case GroupMember.LEVEL_DEFAULT:
                                // Change the text and background to default_level.
                                btnSet.setBackgroundResource(R.drawable.btn_gray_medium_selector);
                                btnSet.setText(getString(R.string.group_set_as_admin));

                                // change the buddy_level of it.
                                person.setLevel(GroupMember.LEVEL_DEFAULT);
                                // Remove it from the managerMembers
                                managePersons.remove(person);
                                break;
                            default:
                                break;
                        }
                    } else {
                        msgBox.toast(R.string.operation_failed);
                    }
                }

                ;
            });
        }
    }

    private ImageButton btnTitleBack;
    private ImageButton btnTitleEdit;
    private TextView titleText;
    private ImageView mDividerBtnTitleEdit;

    private TextView txtApply;
    private TextView txtMember;
    private TextView txtManager;
    private ListView listApply;
    private ListView listMember;
    private ListView listManager;

    private BottomButtonBoard bottomBoard;

    private int myLevel;

    private Database dbHelper;
    private GroupChatRoom groupRoom;
//    private ArrayList<GroupMember> applyPersons;
    private ArrayList<GroupMember> memberPersons;
    private ArrayList<GroupMember> managePersons;
    private ArrayList<PendingRequest> pendingRequests;

    private ApplyAdapter applyAdapter;
    private MemberAdapter memberAdapter;
    private MemberAdapter managerAdapter;
    private String mStrTitle;
    /**
     * It's the current mode, not the target mode.
     * It's false when first came in,
     * it will become to true if click the button(btnTitleEdit).
     */
    private boolean mIsManageMode;

    private IDBTableChangeListener mPendingRequestObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pendingRequests.clear();
                    Database.open(GroupMemberManageActivity.this).fetchPendingsByGroupId(groupRoom.groupID, pendingRequests);
                    if (pendingRequests.isEmpty()) {
                        titleText.setText(mStrTitle.substring(0, mStrTitle.length() - 4));
                        txtApply.setVisibility(View.GONE);
                        listApply.setVisibility(View.GONE);
                    } else {
                        titleText.setText(String.format(mStrTitle, pendingRequests.size()));
                        txtApply.setVisibility(View.VISIBLE);
                        listApply.setVisibility(View.VISIBLE);
                        ListHeightUtil.setListHeight(listApply);
                        txtApply.setText(String.format(getResources().getString(R.string.group_apply),
                                pendingRequests.size()));
                    }
                    applyAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        mDividerBtnTitleEdit = (ImageView) findViewById(R.id.divider_before_title_edit);
        btnTitleEdit = (ImageButton) findViewById(R.id.title_edit);
        titleText = (TextView) findViewById(R.id.title_text);

        txtApply = (TextView) findViewById(R.id.txt_apply);
        txtMember = (TextView) findViewById(R.id.txt_member);
        txtManager = (TextView) findViewById(R.id.txt_manager);
        listApply = (ListView) findViewById(R.id.list_apply);
        listMember = (ListView) findViewById(R.id.list_member);
        listManager = (ListView) findViewById(R.id.list_manager);

        btnTitleBack.setOnClickListener(this);
        btnTitleEdit.setOnClickListener(this);
        if (pendingRequests == null || pendingRequests.size() == 0) {
            titleText.setText(mStrTitle.substring(0, mStrTitle.length() - 4));
        } else {
            titleText.setText(String.format(mStrTitle, pendingRequests.size()));
        }
        listApply.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(GroupMemberManageActivity.this, ApplyInfoActivity.class);
//                intent.putExtra("groupId", groupRoom.groupID);
//                intent.putExtra("buddy", applyPersons.get(position));
//                startActivityForResult(intent, REQ_APPLY_MANAGE);
                final PendingRequest p = pendingRequests.get(position);
                ApplyInfoActivity.launch(GroupMemberManageActivity.this, p, REQ_APPLY_MANAGE);
            }
        });

        // It must be GroupMember.LEVEL_CREATOR who came to this activity.
        txtMember.setVisibility(View.VISIBLE);
        listMember.setVisibility(View.VISIBLE);
//        if (myLevel == GroupMember.LEVEL_CREATOR) {
//            txtMember.setVisibility(View.VISIBLE);
//            listMember.setVisibility(View.VISIBLE);
//        } else if (myLevel == GroupMember.LEVEL_ADMIN) {
//            txtMember.setVisibility(View.GONE);
//            listMember.setVisibility(View.GONE);
//        }
    }

    private void getMembers() {
//        applyPersons = new ArrayList<Buddy>();
//        memberPersons = new ArrayList<Buddy>();
//        ArrayList<Buddy> allPersons = dbHelper.fetchGroupMembers(groupRoom.groupID);
//        for (Buddy b : allPersons) {
//            if (b.getLevel() == GroupMember.LEVEL_PENDING) {
//                applyPersons.add(b);
//            } else {
//                memberPersons.add(b);
//            }
//        }
        if (pendingRequests.size() == 0) {
            txtApply.setVisibility(View.GONE);
            listApply.setVisibility(View.GONE);
        } else {
            txtApply.setVisibility(View.VISIBLE);
            listApply.setVisibility(View.VISIBLE);
            txtApply.setText(String.format(getString(R.string.group_apply), pendingRequests.size()));
        }
        if (memberPersons.size() == 0) {
            txtMember.setVisibility(View.GONE);
            listMember.setVisibility(View.GONE);
        } else {
            txtMember.setVisibility(View.VISIBLE);
            listMember.setVisibility(View.VISIBLE);
            txtMember.setText(String.format(getString(R.string.members), memberPersons.size()));
        }
        if (managePersons.size() == 0) {
            txtManager.setVisibility(View.GONE);
            listManager.setVisibility(View.GONE);
        } else {
            txtManager.setVisibility(View.VISIBLE);
            listManager.setVisibility(View.VISIBLE);
            txtManager.setText(String.format(getString(R.string.creator_and_admin_count), managePersons.size()));
        }
        applyAdapter = new ApplyAdapter(this);
        memberAdapter = new MemberAdapter(this, memberPersons, false);
        managerAdapter = new MemberAdapter(this, managePersons, false);

        listApply.setAdapter(applyAdapter);
        listMember.setAdapter(memberAdapter);
        listManager.setAdapter(managerAdapter);
        ListHeightUtil.setListHeight(listApply);
        ListHeightUtil.setListHeight(listMember);
        ListHeightUtil.setListHeight(listManager);
    }

//    private void getPendings() {
//        pendingRequests = new ArrayList<PendingRequest>();
//        ArrayList<PendingRequest> pendings = new ArrayList<PendingRequest>();
//        dbHelper.fetchPendingRequest(pendings);
//        for (PendingRequest p : pendings) {
//            if ((p.group_id != null && p.group_id.equals(groupRoom.groupID))
//                    && p.type == PendingRequest.GROUP_ADMIN) {
//                pendingRequests.add(p);
//            }
//        }
//    }

    private void changeToManageMode() {
        mIsManageMode = true;
        String myUserId = PrefUtil.getInstance(this).getUid();
        ArrayList<GroupMember> tempList = new ArrayList<GroupMember>();
        listManager.setVisibility(View.GONE);
        txtManager.setVisibility(View.GONE);
        for (GroupMember b : memberPersons) {
            if (b.userID.equals(myUserId))
                continue;
            tempList.add(b);
        }
        memberAdapter = new MemberAdapter(this, tempList, true);
        listMember.setAdapter(memberAdapter);
        ListHeightUtil.setListHeight(listMember);
        txtMember.setText(String.format(getString(R.string.members), tempList.size()));

        // hide the changeMode button
        btnTitleEdit.setVisibility(View.GONE);
        mDividerBtnTitleEdit.setVisibility(View.GONE);
    }

    private void changeToNoramlMode() {
        mIsManageMode = false;
        listManager.setVisibility(View.VISIBLE);
        txtManager.setVisibility(View.VISIBLE);
        txtManager.setText(String.format(getString(R.string.creator_and_admin_count), managePersons.size()));
        txtMember.setText(String.format(getString(R.string.members), memberPersons.size()));

        // display the changeMode button
        btnTitleEdit.setVisibility(View.VISIBLE);
        mDividerBtnTitleEdit.setVisibility(View.VISIBLE);

        memberAdapter = new MemberAdapter(this, memberPersons, false);
        listMember.setAdapter(memberAdapter);
        ListHeightUtil.setListHeight(listMember);

        //managerListView
        managerAdapter.notifyDataSetChanged();
        ListHeightUtil.setListHeight(listManager);
    }

    /**
     * Deal with the event if the back key is pressed.
     */
    private void handleBackEvent() {
        // If currentMode is manageMode, it will return back to the normalMode;
        // else it will return to the previous activity.
        if (mIsManageMode) {
            changeToNoramlMode();
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                handleBackEvent();
                break;
            case R.id.title_edit:
                changeToManageMode();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_APPLY_MANAGE) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");
                Buddy buddy = data.getParcelableExtra("buddy");
                int id = data.getIntExtra("pid", 0);
                dbHelper.deletePendingRequest(id);
                for (PendingRequest p : pendingRequests) {
                    if (id == p.id) {
                        pendingRequests.remove(p);
                    }
                }
//                if (result.equals("accept")) {
//                } else if (result.equals("reject")) {
//                    if (applyPersons != null && !applyPersons.isEmpty()) {
//                        for (int i = 0; i < applyPersons.size(); i++) {
//                            if (buddy.userID.equals(applyPersons.get(i).userID)) {
//                                applyPersons.remove(i);
//                            }
//                        }
//                    }
//                }
                applyAdapter.notifyDataSetChanged();
                if (pendingRequests.size() == 0) {
                    titleText.setText(mStrTitle.substring(0, mStrTitle.length() - 4));
                    txtApply.setVisibility(View.GONE);
                    listApply.setVisibility(View.GONE);
                } else {
                    titleText.setText(String.format(mStrTitle, pendingRequests.size()));
                    ListHeightUtil.setListHeight(listApply);
                    txtApply.setText(String.format(getResources().getString(R.string.group_apply),
                            pendingRequests.size()));
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String group_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member_manage);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        group_id = getIntent().getStringExtra("group_id");
        pendingRequests = getIntent().getParcelableArrayListExtra("pending_requests");
        memberPersons = getIntent().getParcelableArrayListExtra("members");
        myLevel = getIntent().getIntExtra("mylevel", GroupMember.LEVEL_CREATOR);
        managePersons = new ArrayList<GroupMember>();
        for (GroupMember b : memberPersons) {
            if (b.getLevel() == GroupMember.LEVEL_CREATOR || b.getLevel() == GroupMember.LEVEL_ADMIN) {
                managePersons.add(b);
            }
        }
        dbHelper = new Database(this);
        groupRoom = dbHelper.fetchGroupChatRoom(group_id);
//        getPendings();
        mStrTitle = getString(R.string.group_member_manage);
        initView();
        getMembers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,mPendingRequestObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Database.removeDBTableChangeListener(mPendingRequestObserver);
    }

    @Override
    public void onBackPressed() {
        handleBackEvent();
    }
}
