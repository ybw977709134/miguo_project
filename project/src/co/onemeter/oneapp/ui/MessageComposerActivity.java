package co.onemeter.oneapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.MessageDetailAdapter.MessageDetailListener;
import co.onemeter.oneapp.ui.msg.MessageComposerActivityBase;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;

public class MessageComposerActivity extends MessageComposerActivityBase {

    public static final String IS_CLEAR_CHAT_HISTORY = "is_clear_chat_history";
    private static MessageComposerActivity instance;
    private BottomButtonBoard mMenu;
    public static boolean isOnStackTop = false;
    private static final int REQ_LAUNCH_TEMP_GROUP = 0;
    private static final int REQ_LAUNCH_GROUP = 1;
    private static final int REQ_CHANGE_TO_MULTI_CHAT = 2;

    public static MessageComposerActivity instance() {
        if (instance == null)
            return new MessageComposerActivity();
        return instance;
    }

    private MessageDetailListener mMessageDetailListenerImpl  = new MessageDetailListener() {
        @Override
        public void onViewItemClicked() {
            messageClicked();
        }

        @Override
        public void onMessageTextClicked(ChatMessage message, String[] phones,
                String[] links) {
            messageTextClicked(message, phones, links);
        }

        @Override
        public void onConfirmOutgoingCall() {
            confirmOutgoingCall(_targetUID, _targetDisplayName, false);
        }

        @Override
        public void onResendMessage(final ChatMessage msg2Resend) {

            if(null == msg2Resend) {
                return;
            }

//            if (!Connect2.confirmSend2server() || !WowTalkVoipIF.fIsNetworkReachable()) {
//                new AlertDialog.Builder(mContext)
//                .setMessage(R.string.msg_error_need_internet_for_msg)
//                .setPositiveButton(R.string.msg_ok,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick( DialogInterface dialog, int whichButton) {
//                            }
//                        }
//                ).show();
//                return;
//            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 重新发送的消息，只需要修改sentStatus／sentStatus属性即可
                    msg2Resend.sentStatus = ChatMessage.SENTSTATUS_SENDING;
                    msg2Resend.sentDate = getSentDate();
                    msg2Resend.uniqueKey = Database.chatMessageSentDateToUniqueKey(msg2Resend.sentDate);
                    new Database(MessageComposerActivity.this).updateChatMessage(msg2Resend);
                    if (ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE.equals(msg2Resend.msgType)) {
                        resendTextMsgAsync(msg2Resend);
                    } else if (ChatMessage.MSGTYPE_MULTIMEDIA_STAMP.equals(msg2Resend.msgType)) {
                        resendStampMsgAsync(msg2Resend);
                    } else if (ChatMessage.MSGTYPE_LOCATION.equals(msg2Resend.msgType)) {
                        resendLocMsgAsync(msg2Resend);
                    } else if (ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE.equals(msg2Resend.msgType)
                            || ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE.equals(msg2Resend.msgType)
                            || ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO.equals(msg2Resend.msgType)) {
                        resendMediaMsgAsync(msg2Resend);
                    }
                }
            }).start();
        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    // it should be before super.onCreate();
	    mMessageDetailListener = mMessageDetailListenerImpl;
		super.onCreate(savedInstanceState);
        instance = this;

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
	}

    @Override
    public void onResume() {
        super.onResume();
        isOnStackTop = true;

        Database db = new Database(this);
        Buddy target = db.buddyWithUserID(_targetUID);
        // update the single chat title
        if (null != target && target.getAccountType() == Buddy.ACCOUNT_TYPE_STUDENT) {
            _targetGlobalPhoneNumber = TextUtils.isEmpty(target.alias) ? target.nickName : target.alias;
        }

        // update the group title
        GroupChatRoom chatRoom = mDbHelper.fetchGroupChatRoom(_targetUID);
        if (null != chatRoom){
            if (!chatRoom.isEditable && TextUtils.isEmpty(chatRoom.parentGroupId)) {
                _targetGlobalPhoneNumber = getString(co.onemeter.oneapp.R.string.contactsforbiz_root_group_name_display);
            } else {
                _targetGlobalPhoneNumber = chatRoom.groupNameOriginal;
            }
        }
        fRefreshUserInfo();

        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnStackTop = false;
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LAUNCH_TEMP_GROUP && resultCode == RESULT_OK) {
            if (null != data) {
                boolean isClearChatHistory = data.getBooleanExtra(IS_CLEAR_CHAT_HISTORY, false);
                if (isClearChatHistory) {
                    log_msg.clear();
                    refreshMsgListView(true);
                }
            }
        } else if (requestCode == REQ_CHANGE_TO_MULTI_CHAT && resultCode == RESULT_OK) {
            String gid=data.getExtras().getString("gid");
            Parcelable[] persons = data.getExtras().getParcelableArray("persons");
            if(!TextUtils.isEmpty(gid)) {
                MessageComposerActivity.launchToChatWithGroup(
                        MessageComposerActivity.this,
                        MessageComposerActivity.class,
                        gid);
            } else if (null != persons && persons.length == 1) {
                // If the gid is empty, it selected only one person in multiSelectActivity
                MessageComposerActivity.launchToChatWithBuddy(
                        MessageComposerActivity.this,
                        MessageComposerActivity.class,
                        ((Person) persons[0]).getID());
            }
        }
    }

    @Override
    protected void onRightNaviButtonClicked() {
        if(_targetIsNormalGroup) {
            ContactGroupInfoActivity.launchForResult(this, _targetUID, REQ_LAUNCH_GROUP);
        } else if (_targetIsTmpGroup) {
            GroupChatInfoActivity.launchForResult(this, _targetUID, REQ_LAUNCH_TEMP_GROUP);
        } else {
            Intent intent = new Intent(MessageComposerActivity.this, SingleContactChatDetailActivity.class);
            intent.putExtra("currentMemberIds", new String[] {_targetUID});
            startActivityForResult(intent, REQ_CHANGE_TO_MULTI_CHAT);
        }
    }

    public void messageTextClicked(final ChatMessage message, String[] phones, String[] links) {

        mInputMgr.setSoftKeyboardVisibility(false);

        if (mMenu == null)
            mMenu = new BottomButtonBoard(this, findViewById(android.R.id.content));
        else
            mMenu.clearView();

        TextOperationHelper.fillMenu(this, mMenu, message.messageContent, phones, links, true);

        mMenu.show();
    }
}
