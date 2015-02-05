package co.onemeter.oneapp.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import co.onemeter.oneapp.BuildConfig;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.Log;
import co.onemeter.oneapp.ui.MessageComposerActivity;
import co.onemeter.oneapp.ui.StartActivity;
import co.onemeter.utils.AsyncTaskExecutor;

import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.api.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * 当接收到新消息时，要执行的业务逻辑。
 *
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-17
 * Time: PM4:30
 * To change context template use File | Settings | File Templates.
 */
public class ChatMessageHandler {

    /**
     * Handle 完毕时要执行的动作。
     */
    public static interface CompleteListener {
        /**
         * 当初传递给 {@link ChatMessageHandler#handle} 的 ChatMessage 对象。
         * @param m
         */
        public void onDisplayNameAsynchrouslyFixed(ChatMessage m);
    }

    private static final String LOG_TAG = "WowTalk/ChatMessageHandler";

    /**
     * 系统消息来自这个 uid.
     */
    private static final String SYSTEM_NOTIFICATION_SENDER = "10000";
    private static final String NOTI_ACTION_REVIEW = "NOTIFICATION/MOMENT_REVIEW";

    private Context context;
    private Database mDb;
    private WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
    private boolean mSaveDb = false;

    public ChatMessageHandler(Context context) {
        this.context = context;
        mWeb = WowTalkWebServerIF.getInstance(context);
        mDb = Database.open(context);
        mPrefUtil = PrefUtil.getInstance(context);
    }

    private boolean handleGroupJoin(ChatMessage msg) {
        if (BuildConfig.DEBUG)
            android.util.Log.d(LOG_TAG, "ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM: gid:"
                    + msg.chatUserName + ", uid: " + msg.groupChatSenderID);

        String group_id = null;
        String buddy_id = null;
        String buddy_nickname = null;
        try {
            JSONObject json = new JSONObject(msg.messageContent);
            group_id = json.getString("group_id");
            buddy_id = json.getString("buddy_id");
            buddy_nickname = json.getString("buddy_nickname");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (group_id == null || buddy_id == null || buddy_nickname == null)
            return false;

        WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(context);

        String myUid = PrefUtil.getInstance(context).getUid();

        if (buddy_id.equals(myUid)) {
            // my request to join a group was just approved.
            // some group added me as a member.
            GroupChatRoom g = new GroupChatRoom();
            web.fGroupChat_GetByID(group_id, g);

            // 修改 msgtype 和 content，使其适于展示。
            if (null != g && !TextUtils.isEmpty(g.groupNameOriginal)) {
                msg.displayName = g.groupNameOriginal;
            }
            msg.messageContent = buddy_nickname + " " + context.getString(R.string.msg_someone_join_group);
            msg.msgType = ChatMessage.MSGTYPE_SYSTEM_PROMPT;
            if (mSaveDb) {
                mDb.storeGroupChatRoom(g, false);
                mDb.deletePendingRequest(group_id, myUid);

                // get the members of group_chat_room
                Map<String, Object> resultMap = mWeb.fGroupChat_GetMembers(group_id);
                if ("0".equals(String.valueOf(resultMap.get("code")))) {
                    ArrayList<GroupMember> buddyList = (ArrayList<GroupMember>) resultMap.get("data");
                    if (null != buddyList) {
                        Log.d("group(id:" + group_id + ") added me, the size of members is " + buddyList.size());
                    }
                } else {
                    Log.w("ChatMessageHandler#handleGroupJoin(), get group members failure(code is " + resultMap.get("code") + ") when I was added to group(" + group_id + ")");
                }
                mDb.updateChatMessage(msg, true);
            }
        } else {
            // maybe the group hasn't been saved in local db.
            // some others and I are added together.
            GroupChatRoom room = mDb.fetchGroupChatRoom(group_id);
            boolean isObserver = (null != room);

            // 修改 msgtype 和 content，使其适于展示。
            GroupChatRoom g = new GroupChatRoom();
            web.fGroupChat_GetByID(group_id, g);
            if (null != g) {
                msg.displayName = g.groupNameOriginal;
            }
            msg.messageContent = buddy_nickname + " " + context.getString(R.string.msg_someone_join_group);
            msg.msgType = ChatMessage.MSGTYPE_SYSTEM_PROMPT;

            if (mSaveDb) {
                GroupMember gm = new GroupMember();
                gm.groupID = group_id;
                gm.userID = buddy_id;
                gm.setLevel(GroupMember.LEVEL_DEFAULT);
                mDb.addNewBuddyToGroupChatRoomByID(gm, false);

                // 如果本地没有此成员，则先从服务器获取后再更新本地
                Buddy buddy = new Buddy(buddy_id);
                buddy = mDb.fetchBuddyDetail(buddy);
                if (null == buddy) {
                    mWeb.fGetBuddyWithUID(buddy_id);
                    buddy = mDb.fetchBuddyDetail(buddy);
                    if (null != buddy) {
                        Log.d("ChatMessageHandler#handleGroupJoin(), get group member(" + buddy_id + ") success");
                        Person.fromBuddy(buddy).fillBuddy(gm);
                        if (!TextUtils.isEmpty(buddy.alias)) {
                            gm.alias = buddy.alias;
                        }
                        mDb.addNewBuddyToGroupChatRoomByID(gm, true);
                    } else {
                        Log.w("ChatMessageHandler#handleGroupJoin(), get group member(" + buddy_id + ") failure");
                    }
                }

                mDb.updateChatMessage(msg, isObserver);
            }
        }

        return true;
    }

    /**
     *
     * @param msg
     * @return success?
     */
    private boolean handleGroupQuit(ChatMessage msg) {
        if (BuildConfig.DEBUG)
            android.util.Log.d(LOG_TAG, "ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM: gid:"
                    + msg.chatUserName + ", uid: " + msg.groupChatSenderID);

        android.util.Log.i("abc",msg.messageContent);

        String action = null;
        String group_id = null;
        String groupName = null;
        String buddy_id = null;
        String buddy_nickname = null;
        try {
            JSONObject json = new JSONObject(msg.messageContent);
            action = json.getString("action");
            group_id = json.getString("group_id");
            groupName = json.getString("group_name");
            buddy_id = json.getString("buddy_id");
            buddy_nickname = json.getString("buddy_nickname");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String myUid = PrefUtil.getInstance(context).getUid();
        // action = "reject_join_group_request"
        if ("reject_join_group".equals(action)) {
            msg.displayName = groupName;
            msg.messageContent = context.getString(R.string.chatmessagehandler_reject_join_group_request);
            msg.msgType = ChatMessage.MSGTYPE_REQUEST_REJECT;
            mDb.deletePendingRequest(group_id, myUid);
            mDb.deleteChatMessage(msg);
            return true;
        }
        
        if("disband_group".equals(action)){
        	 if (!TextUtils.isEmpty(groupName)) {
                 msg.displayName = groupName;
             }
             msg.messageContent = buddy_nickname + " " + context.getString(R.string.msg_someone_leave_group);
             msg.msgType = ChatMessage.MSGTYPE_SYSTEM_PROMPT;
             if (mSaveDb) {
//             mDb.deleteGroupChatRoomWithID(msg.chatUserName);
                 mDb.deletePendingRequest(msg.chatUserName, myUid);
//                 mDb.deleteChatMessage(msg);
                 mDb.updateChatMessage(msg, true);
                 mDb.deleteBuddyFromGroupChatRoom(msg.chatUserName, buddy_id);
                 mDb.deleteMyselfFlagFromGroupChatRoom(msg.chatUserName);
//             mDb.deleteChatMessageWithUser(group_id);
             }
             return true;
         } 

        // action = "remove_from_group"
        if (group_id == null || buddy_id == null || buddy_nickname == null)
            return false;
        
	    android.util.Log.i("abc", buddy_id.equals(myUid) +"");    
        if (buddy_id.equals(myUid)) {
            // I was kicked out, or reject from the group,
            // or the group has been disbanded
            if (!TextUtils.isEmpty(groupName)) {
                msg.displayName = groupName;
            }
            msg.messageContent = buddy_nickname + " " + context.getString(R.string.msg_someone_leave_group);
            msg.msgType = ChatMessage.MSGTYPE_SYSTEM_PROMPT;
            if (mSaveDb) {
//            mDb.deleteGroupChatRoomWithID(msg.chatUserName);
                mDb.deletePendingRequest(msg.chatUserName, myUid);
//                mDb.deleteChatMessage(msg);
                mDb.updateChatMessage(msg, true);
                mDb.deleteBuddyFromGroupChatRoom(msg.chatUserName, buddy_id);
                mDb.deleteMyselfFlagFromGroupChatRoom(msg.chatUserName);
//            mDb.deleteChatMessageWithUser(group_id);
            }
        } else {
            mDb.deleteBuddyFromGroupChatRoom(msg.chatUserName, buddy_id);

            // 修改 msgtype 和 content，使其适于展示。
            if (!TextUtils.isEmpty(groupName)) {
                msg.displayName = groupName;
            }
            msg.messageContent = buddy_nickname + " " + context.getString(R.string.msg_someone_leave_group);
            msg.msgType = ChatMessage.MSGTYPE_SYSTEM_PROMPT;
            if (mSaveDb)
                mDb.updateChatMessage(msg, true);
        }

        return true;
    }

    private void handleGroupRequest(ChatMessage msg) {
        if (BuildConfig.DEBUG)
            android.util.Log.d(LOG_TAG, "ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST: gid:"
                    + msg.chatUserName + ", uid: " + msg.groupChatSenderID);
    }

    /**
     * Handle incoming chat message.
     * 其中有数据库／网络操作，此方法须在子线程中调用
     * @param msg
     * @param saveDb update chat message in local db?
     * @param completeListener do something after handle complete? can be null.
     * @return is this message human readable?
     */
    public void handle(final ChatMessage msg, boolean saveDb, final CompleteListener completeListener) {
        mSaveDb = saveDb;
        boolean humanReadable = true;
        // process some kinds of notification msg

        if (SYSTEM_NOTIFICATION_SENDER.equals(msg.chatUserName)) {
            if (handleSystemNotification(msg)) {
                return;
            }
        }

		if(ChatMessage.MSGTYPE_THIRDPARTY_MSG.equals(msg.msgType)) {
            humanReadable = false;
            handleX(msg);
        } else if (ChatMessage.MSGTYPE_OFFICIAL_ACCOUNT_MSG.equals(msg.msgType)) {
            msg.parseContentFromPublicAccount();
            if (mSaveDb)
                mDb.updateChatMessage(msg, true);
        } else if(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM.equals(msg.msgType)) {
            humanReadable = false;
            handleGroupJoin(msg);
		} else if(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM.equals(msg.msgType)) {
            humanReadable = false;
            handleGroupQuit(msg);
            if(!MessageComposerActivity.activityIsNull()){
            	MessageComposerActivity.instance().finish();
            }
		} else if(ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST.equals(msg.msgType)) {
            humanReadable = false;
            handleGroupRequest(msg);
        } else {
            if (SYSTEM_NOTIFICATION_SENDER.equals(msg.chatUserName)) {
                humanReadable = false;
            }
        }

        // fix message attributes for displaying
        if (humanReadable) {

            // if the involved group does not exist in local mDb, fetch it,
            // anyway, call showNotification latter.
            boolean needReloadCompanyStructure=false;
            if (msg.isGroupChatMessage()) {
                GroupChatRoom g = mDb.fetchGroupChatRoom(msg.chatUserName);
                if(g == null) {
                    if(ErrorCode.OK == mWeb.fGroupChat_GetMyGroups()) {
                        PrefUtil.getInstance(context).setLocalGroupListLastModified();
                    }
                }
                // 群组聊天时，使用groupChatSenderID作为对方buddyId
                Buddy b = mDb.buddyWithUserID(msg.groupChatSenderID);
                if (null == b) {
                    mWeb.fGetBuddyWithUID(msg.groupChatSenderID);
                    needReloadCompanyStructure=true;
                }
            } else {
                Buddy b = mDb.buddyWithUserID(msg.chatUserName);
                if (null == b) {
                    mWeb.fGetBuddyWithUID(msg.chatUserName);
                    needReloadCompanyStructure=true;
                } else if (Buddy.ACCOUNT_TYPE_PUBLIC == b.getAccountType()) {
                    // 公司通知，公司名字和图片可能有变动
                    int resultCode = mWeb.fGetBuddyWithUID(msg.chatUserName);
                    if (ErrorCode.OK == resultCode) {
                        Buddy noticeBuddy = mDb.buddyWithUserID(msg.chatUserName);
                        if (null != noticeBuddy
                                && ((!TextUtils.isEmpty(noticeBuddy.nickName) && !noticeBuddy.nickName.equals(b.nickName))
                                        || noticeBuddy.photoUploadedTimeStamp != b.photoUploadedTimeStamp)) {
                            mDb.triggerNoticeManChanged();
                        }
                    }
                }
            }
            if(needReloadCompanyStructure && mPrefUtil.isGroupMembersUptodatePerfectly()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StartActivity.downloadContactsAndGroups(context, null);
                    }
                }).start();
            }

            msg.fixMessageSenderDisplayName(
                    context, R.string.session_unknown_buddy, R.string.session_unknown_group);
            if (mSaveDb) {
                mDb.updateChatMessage(msg, true);
            }
            if (completeListener != null) {
                completeListener.onDisplayNameAsynchrouslyFixed(msg);
            }
        } else {
//            mDb.setChatMessageReaded(msg);
        }
    }

    private void handleX(ChatMessage msg) {
        JSONObject json = null;
        String action = null;
        try {
            json = new JSONObject(msg.messageContent);
            action = json.getString("action");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (NOTI_ACTION_REVIEW.equals(action)) {
            mPrefUtil.setLatestReviewTimestamp();

            try {
                final String moment_id = json.getString("moment_id");
                final String review_id = json.getString("review_id");
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        MomentWebServerIF.getInstance(context).fGetReviewById(moment_id, review_id, null);
                        return null;
                    }
                });
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param msg
     * @return true : the msg is consumed (no more handling is required)
     */
    private boolean handleSystemNotification(ChatMessage msg) {
        // 1, 把 json 翻译为可读消息
        // 2, 如果是 request_passed，刷新 buddylist

        JSONObject j = null;
        try {
            j = new JSONObject(msg.messageContent);
            String action = j.getString("action");

            if (action != null && action.equals("friend_request_is_passed")) {
                msg.messageContent =  context.getString(R.string.msg_friend_request_is_passed);
                msg.chatUserName = j.getString("uid");
                msg.displayName = j.getString("nickname");
                if (mSaveDb)
                    mDb.updateChatMessage(msg, true);

                // add buddy to buddies table
                Buddy b = new Buddy(msg.chatUserName);
                if (null == mDb.fetchBuddyDetail(b)) {
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Void>() {
                        @Override
                        protected Void doInBackground(String... params) {
                            String uid = params[0];
                            if (ErrorCode.OK == mWeb.fGetBuddyWithUID(uid)) {
                                Buddy b = new Buddy(uid);
                                if (b != null) {
                                    PrefUtil mPrefUtil = PrefUtil.getInstance(context);
                                    mPrefUtil.setLocalContactListLastModified();
                                    b.setFriendshipWithMe(Buddy.RELATIONSHIP_FRIEND_BOTH);
                                    mDb.storeNewBuddyWithUpdate(b);
                                }
                            }
                            return null;
                        }
                    }, b.userID);
                } else {
                    b.setFriendshipWithMe(Buddy.RELATIONSHIP_FRIEND_BOTH);
                    mDb.storeNewBuddyWithUpdate(b);
                    PrefUtil mPrefUtil = PrefUtil.getInstance(context);
                    mPrefUtil.setLocalContactListLastModified();
                }

                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }
}
