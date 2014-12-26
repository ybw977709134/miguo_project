package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.ThemeHelper;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.util.ArrayList;

public class SingleContactChatDetailActivity extends Activity implements OnClickListener{
    private class MemberAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return chatMembers.size();
        }

        @Override
        public Object getItem(int position) {
            return chatMembers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            OnClickListener listener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userID = PrefUtil.getInstance(SingleContactChatDetailActivity.this).getUid();
                    if (position == chatMembers.size() - 1) {
                        Intent addIntent = new Intent();
                        if (GlobalValue.RELEASE_AS_WOWCITY) {
                            addIntent.setClass(SingleContactChatDetailActivity.this, MultiSelectActivity.class);
                            ThemeHelper.putExtraCurrThemeResId(addIntent, SingleContactChatDetailActivity.this);
                        } else if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
                            addIntent.setClass(SingleContactChatDetailActivity.this, MultiSelectActivityForBiz.class);
                        }
                        String[] memberIds = new String[] {mTargetUId};
                        addIntent.putExtra("currentMemberIds", memberIds);
                        startActivityForResult(addIntent, REQ_ADD_MEMBER);
                    } else {
                        Buddy member = chatMembers.get(position);
                        if (null != member && !TextUtils.isEmpty(member.userID)) {
                            int friendType = ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
                            if (member.userID.equals(userID)) {
                                friendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
                            } else if (0 != (member.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                                friendType = ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
                            }
                            ContactInfoActivity.launch(SingleContactChatDetailActivity.this,
                                    Person.fromBuddy(member),
                                    friendType);
                        }
                    }
                }
            };
            if (convertView == null) {
                convertView = LayoutInflater.from(SingleContactChatDetailActivity.this).inflate(R.layout.listitem_groupchat_member,
                        parent, false);
            }
            ImageView imgPhoto = (ImageView) convertView.findViewById(R.id.img_photo);
            ImageView imgDelete = (ImageView) convertView.findViewById(R.id.img_delete);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            imgPhoto.setOnClickListener(listener);
            if (position < chatMembers.size() - 1) {
                Buddy buddy = chatMembers.get(position);
                PhotoDisplayHelper.displayPhoto(SingleContactChatDetailActivity.this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
                txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
            } else {
                PhotoDisplayHelper.displayPhoto(SingleContactChatDetailActivity.this, imgPhoto, R.drawable.add_member, new Buddy(), true);
                txtName.setText(getResources().getString(R.string.add));
            }
            imgDelete.setVisibility(View.GONE);
            return convertView;
        }
    }

    private static final int REQ_ADD_MEMBER = 123;
    private ImageButton btnTitleBack;
    private RelativeLayout mCatChatHistory;
    private TextView txtClear;
    private YQGridView gridMembers;
    private MessageBox mMsgBox;

    private String mTargetUId;
    private String mTargetName;
    private Database mDBHelper;
    private MemberAdapter memberAdapter;
    private ArrayList<Buddy> chatMembers = new ArrayList<Buddy>();
    private boolean mIsClearedChatHistory;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        mCatChatHistory = (RelativeLayout) findViewById(R.id.cat_chat_history_layout);
        txtClear = (TextView) findViewById(R.id.txt_clear);
        gridMembers = (YQGridView) findViewById(R.id.grid_members);
        memberAdapter = new MemberAdapter();
        gridMembers.setAdapter(memberAdapter);

        btnTitleBack.setOnClickListener(this);
        mCatChatHistory.setOnClickListener(this);
        txtClear.setOnClickListener(this);
    }

    private void getChatMemberFromLocal() {
        // targetUId
        Buddy target = new Buddy(mTargetUId);
        mDBHelper.fetchBuddyDetail(target);
        mTargetName = TextUtils.isEmpty(target.alias) ? target.nickName : target.alias;
        chatMembers.add(target);
        // the "+" , add chat members.
        chatMembers.add(new Buddy());
        memberAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                Intent data = new Intent();
                data.putExtra(MessageComposerActivity.IS_CLEAR_CHAT_HISTORY, mIsClearedChatHistory);
                setResult(RESULT_OK, data);
                finish();
                break;
            case R.id.cat_chat_history_layout:
                catChatHistory();
                break;
            case R.id.txt_clear:
                deleteChatMsg();
                break;
            default:
                break;
        }
    }

    private void catChatHistory() {
        Intent intent = new Intent(SingleContactChatDetailActivity.this, MessageHistoryActivity.class);
        intent.putExtra(MessageComposerActivity.KEY_TARGET_UID, mTargetUId);
        intent.putExtra(MessageComposerActivity.KEY_TARGET_DISPLAYNAME, mTargetName);
        startActivity(intent);
    }

    private void deleteChatMsg() {
        final BottomButtonBoard board = new BottomButtonBoard(SingleContactChatDetailActivity.this, findViewById(R.id.layout));
        board.add(getResources().getString(R.string.clear_chat_msg_ok), BottomButtonBoard.BUTTON_RED, new OnClickListener() {
            @Override
            public void onClick(View v) {
                board.dismiss();
                mIsClearedChatHistory = true;
                mDBHelper.deleteChatMessageWithUser(mTargetUId);
                mMsgBox.toast(R.string.groupchatinfo_clear_chat_msg_toast);
            }
        });
        board.addCancelBtn(getString(R.string.clear_chat_msg_cancel));
        board.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_contact_chat_info);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        Intent intent = getIntent();
        String memberIds[] = intent.getStringArrayExtra("currentMemberIds");
        if (null != memberIds) {
            mTargetUId = memberIds[0];
        }
        if (TextUtils.isEmpty(mTargetUId))
            return;

        mDBHelper = new Database(SingleContactChatDetailActivity.this);

        initView();
        getChatMemberFromLocal();
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

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(REQ_ADD_MEMBER == requestCode && RESULT_OK == resultCode) {
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
                                this,
                                MessageComposerActivity.class,
                                gid);
                    }
                }
            }
        }
    }
}
