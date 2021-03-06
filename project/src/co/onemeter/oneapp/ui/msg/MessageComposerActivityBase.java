package co.onemeter.oneapp.ui.msg;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.*;
import co.onemeter.oneapp.ui.MessageDetailAdapter.MessageDetailListener;
import co.onemeter.oneapp.utils.TimeHelper;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;
import co.onemeter.utils.AsyncTaskExecutor;
import com.handmark.pulltorefresh.widget.PullToRefreshListView;
import com.handmark.pulltorefresh.widget.PullToRefreshListView.OnRefreshListener;
import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.Stamp;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Do SMS chat here.
 * <p>
 * To launch this activity, please use the static launch* methods.
 */
public abstract class MessageComposerActivityBase extends Activity
        implements InputBoardManager.InputResultHandler, InputBoardManager.ChangeToOtherAppsListener {

    private static final int REQ_HYBIRD = 9469021;

    private MessageBox mMsgBox;
    private TimeElapseReportRunnable timeElapseReportForCaptureVoiceRunnable;

    /**
     * Override me.
     * @param message
     * @param phones
     * @param links
     */
    protected void messageTextClicked(ChatMessage message, String[] phones, String[] links) {
    }

    // Keys for intent extras
    public final static String KEY_TARGET_UID = "target_uid";
	public final static String KEY_TARGET_DISPLAYNAME = "target_name";
	public final static String KEY_TARGET_PHONENUMBER = "target_phone";
	public final static String KEY_TARGET_IS_NORMAL_GROUP = "is_normal_group";
	public final static String KEY_TARGET_IS_TMP_GROUP = "is_tmp_group";
    /** 自动向对方发送的消息 */
    public final static String KEY_HELLO_MESSAGE = "hello_message";

	/**
	 * 默认显示的信息条数
	 */
	private static final int DEFAULT_SHOW_ITEMS = 15;
	/**
	 * 每次下拉刷新的条数
	 */
	private static final int DEFAULT_REFRESH_ITEMS = 15;

	/**
	 * 从launchXXX()启动的,则不需要在onCreate()中对mCanSendMsg进行赋值；
	 * 否则，没有对mCanSendMsg进行赋值，在onCreate()中赋默认值 CAN_SEND_MSG_OK
	 */
	private static boolean sCanSendMsgInited;
	/**
	 * 可以发送消息
	 */
	private static final int CAN_SEND_MSG_OK = 0;
	/**
	 * 不是好友关系，不能发送消息
	 */
	private static final int CAN_SEND_MSG_NO_FRIENDS = 1;
	/**
	 * 不是群组成员，不能发送消息
	 */
	private static final int CAN_SEND_MSG_NO_GROUP_MEMBER = 2;

	LayoutInflater mInflater;

	private PullToRefreshListView lv_message;
	private TextView txtTitle;

    /**
     * 对方ID
     */
	protected String _targetUID;
	protected String _targetGlobalPhoneNumber;
    /**
     * 目标是否是常规群组
     */
	protected boolean _targetIsNormalGroup = false;
    /**
     * 目标是否是临时群组
     */
	protected boolean _targetIsTmpGroup = false;
    /**
     * 目标显示名
     */
	protected String _targetDisplayName = null;

    protected Database mDbHelper;
    protected WowTalkWebServerIF mWebif;
    protected PrefUtil mPref;

	protected ArrayList<ChatMessage> log_msg;
	private MessageDetailAdapter myAdapter;

	private static MessageComposerActivityBase instance;

	private Handler mHandler = new Handler();

    protected InputBoardManager mInputMgr;
    private boolean mIsNoticeMessage;

    /**
     * 能否发送消息：
     * 0(CAN_SEND_MSG_OK)，可以发送；
     * 1(CAN_SEND_MSG_NO_FRIENDS)，不能发送(不是好友)；
     * 2(CAN_SEND_MSG_NO_GROUP_MEMBER)，不能发送(不是群组成员)
     */
    private static int mCanSendMsg = CAN_SEND_MSG_OK;

	/**
	 * 如果是第一次进入此activity,则需要将listView滑到底部；否则，根据其他条件判断，见#fRefetchAndReloadTableData()
	 */
	private boolean mIsFirstStartToScroll;

	protected MessageDetailListener mMessageDetailListener;

    /**
     * 网页版聊天发送之后，SDK会将此数据添加到数据库，并发出broadcast
     */
    private BroadcastReceiver mOutgoMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GlobalSetting.OUTGO_MESSAGE_INTENT.equals(intent.getAction())) {
                fRefetchAndReloadTableData(++loadingId);
            }
        }
    };

	public static MessageComposerActivityBase instance() {
		if (instance != null)
			return instance;

		return null;
	}

	// ////////////////////////////////////////////////////////////////////////
	public static boolean isInstanciated() {
		return instance != null;
	}

	private void fNotifyAllUnreadMsg() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                ArrayList<ChatMessage> list_unread_msgs = mDbHelper
                        .fetchUnreadChatMessagesWithUser(_targetUID);

                if(0 == list_unread_msgs.size()) {
                    //no unread message
                    return null;
                }
                mDbHelper.setChatMessageAllReaded(_targetUID);

                for (ChatMessage msgTmp : list_unread_msgs) {
                    boolean ret;
                    int maxRetryCount=3;
                    for(int i=0; i<maxRetryCount; ++i) {
                        if (_targetIsNormalGroup || _targetIsTmpGroup) {
                            ret = WowTalkVoipIF.getInstance(MessageComposerActivityBase.this)
                                    .fNotifyMessageReadInGroupChat(
                                            msgTmp.chatUserName,
                                            msgTmp.groupChatSenderID,
                                            msgTmp.uniqueKey);
                        } else {
                            ret=WowTalkVoipIF.getInstance(MessageComposerActivityBase.this)
                                    .fNotifyMessageRead(_targetUID, msgTmp.uniqueKey);
                        }
                        if(ret) {
                            break;
                        }
                    }
                }

                return null;
            }
        });

	}

    //if message table changed too often,request loading may be not handled in time
    //the last request will reflect states of messages
    //so,load the last one
    private long loadingId=0;
    private boolean isMsgFromDBLoaded=false;
	private void fRefetchAndReloadTableData(final long curLoadingId) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground(Void... params) {
                ArrayList<ChatMessage> msgInDb=null;

                if(curLoadingId == loadingId) {
                    if(isMsgFromDBLoaded && null != log_msg && log_msg.size()>=DEFAULT_SHOW_ITEMS) {
                        String sentDate = log_msg.get(0).sentDate;
                        msgInDb = mDbHelper.fetchChatMessagesAfterInclude(_targetUID, sentDate);
                    } else {
                        msgInDb = mDbHelper.fetchChatMessagesWithUser(_targetUID, DEFAULT_SHOW_ITEMS, 0);
                    }

                    isMsgFromDBLoaded=true;
                }
                return msgInDb;
            }

            @Override
            protected void onPostExecute(ArrayList <ChatMessage> chatMessageList) {
                if(curLoadingId == loadingId) {

                    // 如果当前页的最后一项不是整个adapter的最后一项，则不滑动到底部；否则滑到底部
                    // 如果mIsFirstStartToScroll为true，说明是第一次进入，需要滑动到底部
                    // **不能用lv_message.getLastVisiblePosition() == -1 判断是否是第一次进入，有时显示-1，有时显示第一屏的最后一个位置**
                    // 此处必须在重新给 log_msg 赋值之前判断
                    int lastVisiblePosition = lv_message.getLastVisiblePosition();
                    boolean isScrollToBottom = mIsFirstStartToScroll || lastVisiblePosition == log_msg.size();
                    co.onemeter.oneapp.ui.Log.d("the lastVisiblePosition is " + lv_message.getLastVisiblePosition()
                            + ", the size is " + log_msg.size() + ", mIsFirstStartToScroll is " + mIsFirstStartToScroll
                            + ", isScrollToBottom is " + isScrollToBottom);
                    mIsFirstStartToScroll = false;

                    if (null != log_msg) {

                        // 保留内存中的 ChatMessage.extraObjects，否则就丢失了上传状态
                        HashMap<String, ChatMessage> oldMsgs = new HashMap<>(log_msg.size());
                        for (ChatMessage oldMsg : log_msg) {
                            if (!TextUtils.isEmpty(oldMsg.uniqueKey))
                                oldMsgs.put(oldMsg.uniqueKey, oldMsg);
                        }
                        for (ChatMessage newMsg : chatMessageList) {
                            if (!TextUtils.isEmpty(newMsg.uniqueKey)) {
                                ChatMessage oldMsg = oldMsgs.get(newMsg.uniqueKey);
                                if (oldMsg != null) {
                                    newMsg.extraData = oldMsg.extraData;
                                    newMsg.extraObjects = oldMsg.extraObjects;
                                }
                            }
                        }

                        log_msg.clear();
                    }
                    log_msg=chatMessageList;
                    myAdapter.setDataSource(log_msg);

                    refreshMsgListView(isScrollToBottom);
                    fNotifyAllUnreadMsg();
                }
            }
        });
    }

    /**
     * 获取更早的聊天消息
     */
    private void loadEarlierMsgs() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground(Void... params) {

                if (null == log_msg || log_msg.isEmpty()) {
                    return null;
                }

                long startTime = System.currentTimeMillis();
                long endTime = 0;
                ArrayList<ChatMessage> msgInDb = null;
                String sentDate = log_msg.get(0).sentDate;
                msgInDb = mDbHelper.fetchChatMessagesEarlier(_targetUID,
                        sentDate, DEFAULT_REFRESH_ITEMS);
                // 刷新时间若太短，UI上体验不好
                endTime = System.currentTimeMillis();
                if (endTime - startTime < 1500) {
                    try {
                        Thread.sleep(1500 - (endTime - startTime));
                    }
                    catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
                return msgInDb;
            }

            @Override
            protected void onPostExecute(ArrayList<ChatMessage> chatMessageList) {
                co.onemeter.oneapp.ui.Log.d("#loadEarlierMsgs, load finished.");
                lv_message.onRefreshComplete();

                if (null != chatMessageList && !chatMessageList.isEmpty()) {
                    log_msg.addAll(0, chatMessageList);
                    myAdapter.setDataSource(log_msg);
//                        lv_message.setSelection(log_msg.size() - 1);
                    refreshMsgListView(chatMessageList.size());
                } else {
                    // 本地已没有数据，请在聊天记录中查看
                    mMsgBox.toast(R.string.messagecomposerbase_local_msg_fully);
                }
            }
        });
    }

	protected void fRefreshUserInfo() {
		mHandler.post(new Runnable() {
            public void run() {
                if (_targetUID != null) {
                    txtTitle.setText(getTargetDisplayName());
                }
            }
        });
	}

	protected void refreshMsgListView(final boolean scrollToBottom) {
        myAdapter.notifyDataSetChanged();
        if (scrollToBottom) {
            scrollMsgListToBotomDelay();
        }
	}

	protected void refreshMsgListView(int selection) {
        myAdapter.notifyDataSetChanged();
        lv_message.setSelection(selection);
    }

	public String targetUID() {
		return _targetUID;
	}

	/**
	 * send text message.
	 */
	protected void fSendTextMsg_async(String msgContent) {
		if (_targetUID == null) {
			return;
		}

		if (msgContent.length() <= 0)
			return;

		ChatMessage msg = new ChatMessage();
		msg.chatUserName = _targetUID;
		msg.isGroupChatMessage = _targetIsNormalGroup || _targetIsTmpGroup;
        if (msg.isGroupChatMessage) {
            msg.groupChatSenderID = mPref.getUid();
        }
//		msg.displayName = Utils.isNullOrEmpty(_targetDisplayName) ?
//				_targetGlobalPhoneNumber : _targetDisplayName;
        msg.fixMessageSenderDisplayName(MessageComposerActivityBase.this, R.string.session_unknown_buddy, R.string.session_unknown_group);
		msg.messageContent = msgContent;
		msg.msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
//        msg.ioType = ChatMessage.IOTYPE_OUTPUT;
//        msg.sentDate = Database.chatMessage_dateToUTCString(new Date());

		onSendMessage(msg);
	}
	
    protected void resendTextMsgAsync(ChatMessage chatMessage) {
        onResendMessage(chatMessage);
    }

    protected void fSendStampMsg_async(String stampcontent) {
        if (_targetUID == null) {
            return;
        }
        ChatMessage msg = new ChatMessage();
        msg.chatUserName = _targetUID;
        msg.isGroupChatMessage = _targetIsTmpGroup || _targetIsNormalGroup;
        if (msg.isGroupChatMessage) {
            msg.groupChatSenderID = mPref.getUid();
        }
        msg.fixMessageSenderDisplayName(MessageComposerActivityBase.this, R.string.session_unknown_buddy, R.string.session_unknown_group);
        msg.pathOfThumbNail = null;
        msg.pathOfMultimedia = null;
        msg.messageContent = stampcontent;
        msg.msgType = ChatMessage.MSGTYPE_MULTIMEDIA_STAMP;
//        msg.ioType = ChatMessage.IOTYPE_OUTPUT;

        onSendMessage(msg);
    }

    protected void resendStampMsgAsync(ChatMessage chatMessage) {
        onResendMessage(chatMessage);
    }

    /**
     * 发送表情消息
     * @param stamp
     */
    public void fSendStampMsg_async(Stamp stamp) {
        fSendStampMsg_async(stamp.getMessageContent());
    }

    /**
     * 发送位置消息
     * @param lat
     * @param lon
     */
    protected void fSendLocMsg_async(final double lat, final double lon) {
        if(!Connect2.confirmSend2server()) {
            //...no handle now
        }
        //first show
        JSONObject json=new JSONObject();

        try {
            json.put("longitude", lon);
            json.put("latitude", lat);
            json.put("address", "......");
        } catch (JSONException e) {
        }
        final ChatMessage msg = new ChatMessage();
        msg.chatUserName = _targetUID;
        msg.isGroupChatMessage = _targetIsTmpGroup || _targetIsNormalGroup;
        if (msg.isGroupChatMessage) {
            msg.groupChatSenderID = mPref.getUid();
        }
        msg.fixMessageSenderDisplayName(MessageComposerActivityBase.this, R.string.session_unknown_buddy, R.string.session_unknown_group);
//                msg.displayName = _targetGlobalPhoneNumber;
        msg.messageContent = json.toString();
        msg.msgType = ChatMessage.MSGTYPE_LOCATION;
        msg.sentStatus = ChatMessage.SENTSTATUS_SENDING;
        msg.sentDate = TimeHelper.getTimeForMessage(this);
        msg.uniqueKey = Database.chatMessageSentDateToUniqueKey(msg.sentDate);
        msg.ioType = ChatMessage.IOTYPE_OUTPUT;
        msg.primaryKey = new Database(MessageComposerActivityBase.this)
                            .storeNewChatMessage(msg, false);

        addToLogMsg(msg);
        sendLocMsgInThread(msg, lat, lon);
    }

    protected void resendLocMsgAsync(final ChatMessage chatMessage) {
        addToLogMsg(chatMessage);

        // 获取经纬度
        double lat = 0, lon = 0;
        try {
            JSONObject json=new JSONObject(chatMessage.messageContent);
            if (json.has("longitude")){
                lon = org.wowtalk.ui.msg.Utils.tryParseDouble(json.getString("longitude"), 0.0);
            }
            if (json.has("latitude")){
                lat = Double.parseDouble(json.getString("latitude"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendLocMsgInThread(chatMessage, lat, lon);
    }

    /**
     * 发送位置信息: 子线程中根据经纬度获取address，然后发送消息
     * @param chatMessage
     * @param lat
     * @param lon
     */
    private void sendLocMsgInThread(final ChatMessage chatMessage, final double lat, final double lon) {
        // request addr first, then send location message
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String addr = PickLocActivity.getAddr(lat, lon);
//                JSONObject json=new JSONObject();
//                try {
//                    json.put("longitude", lon);
//                    json.put("latitude", lat);
//                    json.put("address", addr);
//                } catch (JSONException e) {
//                }
//                chatMessage.messageContent = json.toString();
//
//                new Database(MessageComposerActivityBase.this).updateChatMessage(chatMessage, true);
//                if(chatMessage.isGroupChatMessage) {
//                    mWebif.fGroupChat_SendMessage(chatMessage.chatUserName, chatMessage);
//                } else {
//                    WowTalkVoipIF.getInstance(MessageComposerActivityBase.this).fSendChatMessage(chatMessage);
//                }
//            }
//        }).start();
    	co.onemeter.oneapp.utils.LocationHelper.addressForMessageComposer(this, chatMessage, lat, lon);
    }

    /**
     * Do the sending work.
     * @param msg
     */
    protected void onSendMessage(final ChatMessage msg) {
        msg.sentStatus = ChatMessage.SENTSTATUS_SENDING;
        msg.sentDate = TimeHelper.getTimeForMessage(this);
        msg.uniqueKey = Database.chatMessageSentDateToUniqueKey(msg.sentDate);
        msg.ioType = ChatMessage.IOTYPE_OUTPUT;
        msg.primaryKey = new Database(MessageComposerActivityBase.this)
                            .storeNewChatMessage(msg, false);
        addToLogMsg(msg);

        if(!Connect2.confirmSend2server()) {
            //...no handle now
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(msg.isGroupChatMessage) {
                    mWebif.fGroupChat_SendMessage(msg.chatUserName, msg);
                } else {
                    WowTalkVoipIF.getInstance(MessageComposerActivityBase.this).fSendChatMessage(msg);
                }
            }
        }).start();
    }

    protected void onResendMessage(final ChatMessage msg) {
        addToLogMsg(msg);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(msg.isGroupChatMessage) {
                    mWebif.fGroupChat_SendMessage(msg.chatUserName, msg);
                } else {
                    WowTalkVoipIF.getInstance(MessageComposerActivityBase.this).fSendChatMessage(msg);
                }
            }
        }).start();
    }

    @Override
    public void toastCannotSendMsg() {
        if (mCanSendMsg == CAN_SEND_MSG_NO_FRIENDS) {
            mMsgBox.toast(R.string.messagecomposerbase_no_friends);
        } else if (mCanSendMsg == CAN_SEND_MSG_NO_GROUP_MEMBER) {
            mMsgBox.toast(R.string.messagecomposerbase_no_group_member);
        }
    }

    /**
     * 此处只是加入到Adapter的数据源中，待状态更新后重新从数据库获取刷新数据源
     * @param msg
     */
    protected void addToLogMsg(ChatMessage msg) {

        // 重新发送的，需要添加到最后
        log_msg.remove(msg);
        log_msg.add(msg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshMsgListView(true);
            }
        });
    }

    /**
     * send multimedia message.
     *
     * @param pathOfMultimedia
     * @param pathOfThumbNail not required for voice msg.
     * @param voiceDuration only required for voice msg.
     * @param msgType ChatMessage.MSGTYPE_*
     */
    protected void fSendMediaMsg_async(
            final String pathOfMultimedia,
            final String pathOfThumbNail,
            final int voiceDuration,
            final String msgType) {
        if (_targetUID == null) {
            return;
        }

        if(!Connect2.confirmSend2server()) {
            //...no handle now
        }
		/*
		 *  1, generate a message object and display it, before we actually post the thumb nail and media file.
		 */
        final ChatMessage msg = new ChatMessage();
        msg.chatUserName = _targetUID;
        msg.displayName = _targetGlobalPhoneNumber;
        msg.isGroupChatMessage = _targetIsTmpGroup || _targetIsNormalGroup;
        if (msg.isGroupChatMessage) {
            msg.groupChatSenderID = mPref.getUid();
        }
        msg.fixMessageSenderDisplayName(MessageComposerActivityBase.this, R.string.session_unknown_buddy, R.string.session_unknown_group);
        msg.msgType = msgType;
        msg.pathOfMultimedia = pathOfMultimedia;
        msg.pathOfThumbNail = pathOfThumbNail;
        msg.initExtra();
        msg.extraData.putBoolean("isTransferring", true);

        // WowTalkVoipIF.fSendChatMessage() would set these fields, but we need them now.
        msg.ioType = ChatMessage.IOTYPE_OUTPUT;
        msg.sentStatus = ChatMessage.SENTSTATUS_SENDING;
        msg.sentDate = TimeHelper.getTimeForMessage(this);
        msg.uniqueKey = Database.chatMessageSentDateToUniqueKey(msg.sentDate);

        // postMultimediaAndSend() will set the mediaFileId and duration, but we need it now.
        // Set the duration.
        if (ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE.equals(msg.msgType)) {
            msg.formatContentAsVoiceMessage("", voiceDuration);
        }

        // 多媒体文件时，先存入数据库，此时缺少消息对应的详细content，此处只是为了本地能够显示，后面发送成功后会更新这条数据
        msg.primaryKey = mDbHelper.storeNewChatMessage(msg, false);
        addToLogMsg(msg);

		/*
		 *  2, upload, asynchronously, so the UI can refresh immediately.
		 */
        if(pathOfThumbNail != null) {
            postThumbnailAndMultimediaAndSend(pathOfMultimedia, pathOfThumbNail,
                    voiceDuration, msg);
        } else {
            postMultimediaAndSend(pathOfMultimedia, null, msg);
        }
    }

    /**
     * 发送图文音类型消息
     * @param text
     * @param imagePath
     * @param imageThumbPath
     * @param audioPath
     * @param duration
     */
	protected void fSendHybird_async(final String text,
                                 String imagePath, String imageThumbPath,
                                 String audioPath, final int duration) {
		if (_targetUID == null) {
			return;
		}

        if(!Connect2.confirmSend2server()) {
            //...no handle now
        }
		/*
		 *  1, generate a message object and display it, before we actually post the thumb nail and media file.
		 */
		final ChatMessage msg = new ChatMessage();

        final String imageExt = imagePath.substring(imagePath.lastIndexOf("."));
        final String audioExt = audioPath.substring(audioPath.lastIndexOf("."));
        msg.msgType = ChatMessage.MSGTYPE_PIC_VOICE;
        msg.formatContentAsHybird(text,
                imagePath, imageExt, imageThumbPath,
                audioPath, audioExt, duration);

		msg.chatUserName = _targetUID;
		msg.displayName = _targetGlobalPhoneNumber;
		msg.isGroupChatMessage = _targetIsTmpGroup || _targetIsNormalGroup;
        if (msg.isGroupChatMessage) {
            msg.groupChatSenderID = mPref.getUid();
        }
		msg.fixMessageSenderDisplayName(MessageComposerActivityBase.this, R.string.session_unknown_buddy, R.string.session_unknown_group);
		msg.pathOfMultimedia = imagePath;
        msg.pathOfMultimedia2 = audioPath;
		msg.pathOfThumbNail = imageThumbPath;
		msg.initExtra();
		msg.extraData.putBoolean("isTransferring", true);

		// WowTalkVoipIF.fSendChatMessage() would set these fields, but we need them now.
		msg.ioType = ChatMessage.IOTYPE_OUTPUT;
		msg.sentStatus = ChatMessage.SENTSTATUS_SENDING;
		msg.sentDate = TimeHelper.getTimeForMessage(this);
		msg.uniqueKey = Database.chatMessageSentDateToUniqueKey(msg.sentDate);

        // 多媒体文件时，先存入数据库，此时缺少消息对应的详细content，此处只是为了本地能够显示，后面发送成功后会更新这条数据
        msg.primaryKey = mDbHelper.storeNewChatMessage(msg, false);
        addToLogMsg(msg);

		/*
		 *  2, upload, asynchronously, so the UI can refresh immediately.
		 */
        AsyncTaskExecutor.executeShortNetworkTask(new PostFileTask(this){
            @Override
            protected Boolean doInBackground(final String... pathes) {
                boolean status = super.doInBackground(pathes);
                if (status) {
                    msg.formatContentAsHybird(text,
                            fileIds[0], imageExt, fileIds[1],
                            fileIds[2], audioExt, duration);
                    status = WowTalkVoipIF.getInstance(MessageComposerActivityBase.this)
                            .fSendChatMessage(msg);
                } else {
                    msg.sentStatus = ChatMessage.SENTSTATUS_NOTSENT;
                    mDbHelper.setChatMessageCannotSent(msg);
                }
                return status;
            }

            @Override
            public void onPostExecute(Boolean status) {
                if (!status) {
                    mMsgBox.toast(R.string.messagecomposerbase_upload_file_failure);
                }
                msg.extraData.putBoolean("isTransferring", false);
                final ProgressBar progressBar = (ProgressBar)msg.extraObjects.get("progressBar");
                if(progressBar != null)
                    progressBar.setVisibility(View.GONE);
            }

            @Override
            protected void onProgressUpdate(Integer... param) {
                ProgressBar progressBar = (ProgressBar)msg.extraObjects.get("progressBar");
                if(progressBar != null) {
                    progressBar.setProgress(param[0]);
                }
            }
        }, imagePath, imageThumbPath, audioPath);
	}

    protected void resendMediaMsgAsync(ChatMessage chatMessage) {

        addToLogMsg(chatMessage);

        if(!TextUtils.isEmpty(chatMessage.pathOfThumbNail)) {
            String pathOfMultimedia = chatMessage.pathOfMultimedia;
            String pathOfThumbNail = chatMessage.pathOfThumbNail;
            int voiceDuration = 0;
            if (chatMessage.msgType == ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE) {
                voiceDuration = chatMessage.getVoiceDuration();
                pathOfThumbNail = null;
            }
            postThumbnailAndMultimediaAndSend(pathOfMultimedia, pathOfThumbNail,
                    voiceDuration, chatMessage);
        } else {
            postMultimediaAndSend(chatMessage.pathOfMultimedia, null, chatMessage);
        }
    }

	/**
	 * will overwrite msg.messageContent.
	 * @param pathOfMultimedia
	 * @param pathOfThumbNail
	 * @param msg
	 */
    private void postThumbnailAndMultimediaAndSend(
            final String pathOfMultimedia,
            final String pathOfThumbNail,
            final int voiceDuration,
            final ChatMessage msg) {
        msg.initExtra();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {

            String thumb_id = null;
            String media_id = null;
            boolean isFileUploadSuccess = true;

            @Override
            protected Void doInBackground(Void... arg0) {
                WowTalkWebServerIF.getInstance(MessageComposerActivityBase.this).fPostFileToServer(
                        pathOfThumbNail, true,
                        new NetworkIFDelegate() {

                            @Override
                            public void didFailNetworkIFCommunication(int arg0,
                                                                      byte[] arg1) {
                                isFileUploadSuccess = false;
                                Log.e("upload thumb file failure.");
                            }

                            @Override
                            public void didFinishNetworkIFCommunication(int arg0,
                                                                        byte[] arg1) {
                                thumb_id = new String(arg1);
                            }

                            @Override
                            public void setProgress(int tag, final int progress) {
                                // assume thumb nail takes 20% of total uploading
                                publishProgress(Math.round(progress * .2f));
                            }

                        }, 0);

                if (thumb_id == null) {
                    mDbHelper.setChatMessageCannotSent(msg);
                    return null;
                }

                WowTalkWebServerIF.getInstance(MessageComposerActivityBase.this).fPostFileToServer(
                        pathOfMultimedia, true,
                        new NetworkIFDelegate() {

                            @Override
                            public void didFailNetworkIFCommunication(int arg0,
                                                                      byte[] arg1) {
                                isFileUploadSuccess = false;
                                Log.e("upload multimedia file failure.");
                            }

                            @Override
                            public void didFinishNetworkIFCommunication(int arg0,
                                                                        byte[] arg1) {
                                media_id = new String(arg1);
                            }

                            @Override
                            public void setProgress(int tag, final int progress) {
                                publishProgress(20 + Math.round(progress * .8f));
                            }

                        }, 0);


                if (media_id != null) {
                    if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)
                            || msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO))
                        msg.formatContentAsPhotoMessage(media_id, thumb_id);
                    else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
                        msg.formatContentAsVoiceMessage(media_id, voiceDuration);
                    }

                    if (msg.isGroupChatMessage) {
                        mWebif.fGroupChat_SendMessage(msg.chatUserName, msg);
                    } else {
                        WowTalkVoipIF.getInstance(MessageComposerActivityBase.this).fSendChatMessage(msg);
                    }
                } else {
                    mDbHelper.setChatMessageCannotSent(msg);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... param) {
                ProgressBar progressBar = (ProgressBar) msg.extraObjects.get("progressBar");
                if (progressBar != null) {
                    progressBar.setProgress(param[0]);
                }
                msg.extraData.putInt(ChatMessage.EXTRA_DATA_PROGRESS, param[0]);
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isFileUploadSuccess) {
                    mMsgBox.toast(R.string.messagecomposerbase_upload_file_failure);
                }

                msg.extraData.putBoolean("isTransferring", false);
//                mDbHelper.updateChatMessage(msg, true);
                final ProgressBar progressBar = (ProgressBar) msg.extraObjects.get("progressBar");
                if (progressBar != null)
                    progressBar.setVisibility(View.GONE);
//				showSentStatus(msg);
            }

        });
    }


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		mIsFirstStartToScroll = true;
		setContentView(R.layout.msg_message_composer);

        String hello = null;

		Bundle b;
		if(savedInstanceState != null) {
			b = savedInstanceState;
		} else {
			b = getIntent().getExtras();
		}
		if (b != null) {
			_targetUID = b.getString(KEY_TARGET_UID);
			_targetGlobalPhoneNumber = b.getString(KEY_TARGET_DISPLAYNAME);
			_targetIsNormalGroup = b.getBoolean(KEY_TARGET_IS_NORMAL_GROUP, false);
			_targetIsTmpGroup = b.getBoolean(KEY_TARGET_IS_TMP_GROUP, false);
            hello = b.getString(KEY_HELLO_MESSAGE);
		}

        if(savedInstanceState != null) {
            InputBoardManager inputBoardManager = savedInstanceState.getParcelable("mInputMgr");
            if (null != inputBoardManager) {
                inputBoardManager.init(
                        this,
                        (ViewGroup)findViewById(R.id.input_board_holder),
                        this,
                        this);
                inputBoardManager.setupViews();
                mInputMgr = inputBoardManager;
            }
        }

		if (_targetUID == null)
			return;

        // mCanSendMsg 是static的，每次启动后，要对其重新赋值
        if (!sCanSendMsgInited) {
            mCanSendMsg = CAN_SEND_MSG_OK;
        }
        sCanSendMsgInited = false;

        initActivity();

        if (hello != null)
            fSendTextMsg_async(hello);

        if (_targetIsNormalGroup) {
            // 由于群组资料的更新没有推送通知，这里应该自动刷新
            refreshGroupInfo(_targetUID);
        }
        
        String lastMessage = mDbHelper.getLastMessage(_targetUID);
        if(lastMessage != null){
        	mInputMgr.mTxtContent.setText(lastMessage);
        }
        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TARGET_UID, _targetUID);
        outState.putString(KEY_TARGET_DISPLAYNAME, _targetGlobalPhoneNumber);
        outState.putBoolean(KEY_TARGET_IS_NORMAL_GROUP, _targetIsNormalGroup);
        outState.putBoolean(KEY_TARGET_IS_TMP_GROUP, _targetIsTmpGroup);
        if (mInputMgr != null) {
            outState.putParcelable("mInputMgr", mInputMgr);
        }
    }

    private void initActivity() {
		mDbHelper = new Database(MessageComposerActivityBase.this);
		mWebif = WowTalkWebServerIF.getInstance(this);
        mMsgBox = new MessageBox(this);
        mPref = PrefUtil.getInstance(this);

        if (mInputMgr == null) {
            mInputMgr = new InputBoardManager(this,
                    (ViewGroup)findViewById(R.id.input_board_holder),
                    this, this);
            configInputBoardDrawable(mInputMgr);
        }

        Buddy b = mDbHelper.buddyWithUserID(_targetUID);
        mIsNoticeMessage = false;
        if (!_targetIsNormalGroup && !_targetIsTmpGroup) {
            mInputMgr.setIsWithCallMethod(true);
            if (null != b && b.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
                mIsNoticeMessage = true;
                mInputMgr.setIsWithMultimediaMethod(false);
                mInputMgr.setIsWithCallMethod(false);
            } else if (null != b && b.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
                mInputMgr.setIsWithMultimediaMethod(true);
                mInputMgr.setIsWithCallMethod(true);
            }
        } else {
            mInputMgr.setIsWithCallMethod(false);
        }

		// findViewById
		lv_message = (PullToRefreshListView) findViewById(R.id.message_history);
        ImageView btnNaviRight = (ImageView) findViewById(R.id.img_call);
		txtTitle = (TextView) findViewById(R.id.txtMessagesTitle);
        ImageView btnBack = (ImageView) findViewById(R.id.img_back);

		lv_message.setDividerHeight(0);
		lv_message.setonRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadEarlierMsgs();
            }
        });

		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		btnBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//聊完天后都直接 跳转到主消息页面
				Intent intent = new Intent(MessageComposerActivityBase.this,StartActivity.class);
				startActivity(intent);
			}
		});

		btnNaviRight.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onRightNaviButtonClicked();
			}
		});

        log_msg = new ArrayList<ChatMessage>();
        myAdapter = new MessageDetailAdapter(this, log_msg, false, mHandler, mMessageDetailListener);
        lv_message.setAdapter(myAdapter);
        lv_message.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                messageClicked();
                return false;
            }
        });

        updateUiAboutCanSendMsg();
        if (mCanSendMsg != CAN_SEND_MSG_OK) {
            recheckIfCanSendMsg();
        }

        mInputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
	}

    private void updateUiAboutCanSendMsg() {
        ImageView btnNaviRight = (ImageView) findViewById(R.id.img_call);
        if (mCanSendMsg != CAN_SEND_MSG_OK) {
            btnNaviRight.setVisibility(View.INVISIBLE);
        }
        mInputMgr.setCanSendMsg(mCanSendMsg == CAN_SEND_MSG_OK);
    }

    /**
     * 请求服务器确认是否能够发信息给对方
     */
    private void recheckIfCanSendMsg() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                if (_targetIsNormalGroup || _targetIsTmpGroup) {
                    // TODO
                } else if (!isFromSchool()) {
                    if (ErrorCode.OK == mWebif.fGetBuddyWithUID(_targetUID)) {
                        Buddy buddy = new Database(MessageComposerActivityBase.this).buddyWithUserID(_targetUID);
                        if (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result && mCanSendMsg != CAN_SEND_MSG_OK) {
                    Log.i("MessageComposerActivityBase recheck send availability succeed.");
                    mCanSendMsg = CAN_SEND_MSG_OK;
                    updateUiAboutCanSendMsg();
                }
            }
        });
    }

    private boolean isFromSchool() {
        Buddy oldInfo = new Database(MessageComposerActivityBase.this).buddyWithUserID(_targetUID);
        return Buddy.ACCOUNT_TYPE_SCHOOL == oldInfo.getAccountType();
    }

    /**
     * 群组聊天需要刷新群组资料
     * @param groupId
     */
    private void refreshGroupInfo(final String groupId) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mWebif.fGroupChat_GetGroupDetail(groupId);
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (ErrorCode.OK == result) {
                    GroupChatRoom groupRoom = mDbHelper.fetchGroupChatRoom(groupId);
                    _targetDisplayName = groupRoom.getDisplayName();
                    txtTitle.setText(getTargetDisplayName());
                }
            }
        });
    }

    /**
     * 此处设置InputBoard的按钮资源文件
     * @param mgr InputBoardManager
     */
    protected void configInputBoardDrawable(InputBoardManager mgr) {
        mgr.drawableResId().open = R.drawable.icon_message_text_input_more_selector;
        mgr.drawableResId().close = R.drawable.icon_message_text_input_keyboard_selector;
        mgr.drawableResId().gotoEmotion = R.drawable.icon_message_text_input_emotion;
        mgr.drawableResId().keyboard = R.drawable.sms_keyboard;
        mgr.drawableResId().voiceNormal = R.drawable.btn_blue;
        mgr.drawableResId().voicePressed = R.drawable.btn_blue_p;
    }

    /**
	 * Override me.
	 */
	protected void onRightNaviButtonClicked() {
	}

    /**
     * 返回按钮按压处理的一些操作
     */
    @Override
	public void onBackPressed() {
        // back steps:
        // multimedia -> text -> back
        Buddy b = mDbHelper.buddyWithUserID(_targetUID);

        if (mInputMgr != null && mInputMgr.showingFlags() != InputBoardManager.FLAG_SHOW_TEXT) {
            boolean needReset=false;
            if(null != b && b.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
                if(View.VISIBLE == mInputMgr.getMediaLayoutVisibility()) {
                    needReset=true;
                }
            } else {
                needReset=true;
            }

            if (needReset) {
//            mInputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
                mInputMgr.setInputMode(InputBoardManager.FLAG_SHOW_TEXT, false);

                return;
            }
        }

        super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		instance = null;
		if (null != myAdapter) {
		    myAdapter.releaseRes();
        }

		super.onDestroy();
	}

    /**
     * override from Activity
     * @param intent
     */
	@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mIsFirstStartToScroll = true;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            _targetUID = bundle.getString(KEY_TARGET_UID);
            _targetGlobalPhoneNumber = bundle.getString(KEY_TARGET_DISPLAYNAME);
            _targetIsNormalGroup = bundle.getBoolean(KEY_TARGET_IS_NORMAL_GROUP, false);
            _targetIsTmpGroup = bundle.getBoolean(KEY_TARGET_IS_TMP_GROUP, false);
        }
        Buddy buddy = null;

        // the launch mode is singleTask, we should invoke "setIsWithMultimediaMethod"
        // and "setIsWithCallMethod" to set whether it's true or false.
        mIsNoticeMessage = false;
        if (!_targetIsNormalGroup && !_targetIsTmpGroup) {
            buddy = mDbHelper.buddyWithUserID(_targetUID);
            if (null != buddy && buddy.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
                mIsNoticeMessage = true;
                mInputMgr.setIsWithMultimediaMethod(false);
                mInputMgr.setIsWithCallMethod(false);
            } else {
                mInputMgr.setIsWithMultimediaMethod(true);
                mInputMgr.setIsWithCallMethod(true);
            }
        } else {
            mInputMgr.setIsWithMultimediaMethod(true);
            mInputMgr.setIsWithCallMethod(false);
        }

        if (null != buddy && buddy.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
            mInputMgr.show(InputBoardManager.FLAG_SHOW_MEDIA);
            mInputMgr.setLayoutForFamily();
        } else {
            mInputMgr.reShowLayout(InputBoardManager.FLAG_SHOW_TEXT);
        }
        new Thread() {
            public void run() {
                fRefreshUserInfo();
                fRefetchAndReloadTableData(++loadingId);
            }
        }.start();
    }

    /**
     * ChatMessage表的观察者，监听表数据变化，更新UI消息
     */
    private IDBTableChangeListener chatmessageObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fRefetchAndReloadTableData(++loadingId);
                }
            });
        }
    };

    private IDBTableChangeListener mNoticeManChangedObserver = new IDBTableChangeListener() {
        @Override
        public void onDBTableChanged(String tableName) {
            fRefetchAndReloadTableData(++loadingId);
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Database db = new Database(MessageComposerActivityBase.this);
                    Buddy buddy = db.buddyWithUserID(_targetUID);
                    if (null != buddy && (!TextUtils.isEmpty(buddy.nickName))) {
                        _targetGlobalPhoneNumber = buddy.nickName;
                    }
                    return null;
                }

                protected void onPostExecute(Void result) {
                    txtTitle.setText(_targetGlobalPhoneNumber);
                };
            });
        }
    };

	@Override
	public void onResume() {
		instance = this;

        super.onResume();
        //启动AppStatusService监控
        AppStatusService.setIsMonitoring(true);

        if(lv_message == null) {
            // this is a new process (the previous one has been killed)?
            initActivity();
        }
        fRefetchAndReloadTableData(++loadingId);
        Database.addDBTableChangeListener(Database.TBL_MESSAGES,chatmessageObserver);
        Database.addDBTableChangeListener(Database.TBL_CHATMESSAGE_READED, chatmessageObserver);
        // 当前聊天为通知时，增加监听
        if (mIsNoticeMessage) {
            Database.addDBTableChangeListener(Database.DUMMY_TBL_NOTICE_MAN_CHANGED, mNoticeManChangedObserver);
        }
        //注册向外发送ChatMessage的广播
        registerReceiver(mOutgoMsgReceiver, new IntentFilter(GlobalSetting.OUTGO_MESSAGE_INTENT));
	}

    @Override
    protected void onPause() {
        instance = null;
//        if (mPlayer != null) {
//            mPlayer.release();
//            mPlayer = null;
//        }
        super.onPause();

        Database.removeDBTableChangeListener(chatmessageObserver);
        if (mIsNoticeMessage) {
            Database.removeDBTableChangeListener(mNoticeManChangedObserver);
        }
        //onPause（）需要注销广播
        unregisterReceiver(mOutgoMsgReceiver);
    }

    private int lastBottomInputViewHeight=0;
    private void startHeightRelativeRunnable() {
        timeElapseReportForCaptureVoiceRunnable=new TimeElapseReportRunnable();
        timeElapseReportForCaptureVoiceRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
            @Override
            public void reportElapse(final long elapsed) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(null != mInputMgr) {
                            View rootView=mInputMgr.getRootView();
                            if(null != rootView) {
                                View layoutText=rootView.findViewById(org.wowtalk.ui.msg.R.id.layoutText);
                                View layoutVoice=rootView.findViewById(org.wowtalk.ui.msg.R.id.layoutVoice);

                                int[] position = new int[2];
                                if(View.VISIBLE == layoutVoice.getVisibility()) {
                                    layoutVoice.getLocationOnScreen(position);
                                } else {
                                    layoutText.getLocationOnScreen(position);
                                }

                                DisplayMetrics dm = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(dm);
                                int viewHeight=dm.heightPixels-position[1];


                                if(0 == lastBottomInputViewHeight) {
                                    Log.w("init view height "+viewHeight);
                                    lastBottomInputViewHeight=viewHeight;
                                    return;
                                }
                                if(lastBottomInputViewHeight != viewHeight) {
                                    Log.w("new view height "+viewHeight);
                                    lastBottomInputViewHeight=viewHeight;
                                    lv_message.setSelection(lv_message.getCount()-1);
                                }
                            }
                        }
                    }
                });
            }
        });
        new Thread(timeElapseReportForCaptureVoiceRunnable).start();
    }

    private void stopHeightRelativeRunnable() {
        timeElapseReportForCaptureVoiceRunnable.stop();
    }

	@Override
	public void onStart() {
		instance = this;
		super.onStart();

        startHeightRelativeRunnable();
	}

    /**
     * 停止相关语音，线程，数据库做一些操作
     */
	@Override
	public void onStop() {
		instance = null;
		super.onStop();

        myAdapter.stopPlayingVoice();
        stopHeightRelativeRunnable();
        
        if(mDbHelper.getTargetUIDCount(_targetUID) == 1){
        	mDbHelper.updateLastMessage(mInputMgr.mTxtContent.getText().toString(), _targetUID);
        }else if(mDbHelper.getTargetUIDCount(_targetUID) == 0){
        	mDbHelper.saveLastMessage(mInputMgr.mTxtContent.getText().toString(), _targetUID);
        }
	}

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (mInputMgr != null
                && mInputMgr.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        /**
         * 处理图文音发送操作
         */
        if (requestCode == REQ_HYBIRD) {
            if (resultCode == RESULT_OK) {
                String[] path = data.getStringArrayExtra(HybirdMessageEditor.EXTRA_OUT_IMAGE_FILENAME);
                onHybirdInputted(
                        data.getStringExtra(HybirdMessageEditor.EXTRA_OUT_TEXT),
                        path[0],
                        path[1],
                        data.getStringExtra(HybirdMessageEditor.EXTRA_OUT_VOICE_FILENAME),
                        data.getIntExtra(HybirdMessageEditor.EXTRA_OUT_VOICE_DURATION, 0)
                );
            }
            return;
        }
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * will overwrite msg.messageContent.
	 * @param pathOfMultimedia
	 * @param thumb_id
	 * @param msg
	 */
	private void postMultimediaAndSend(final String pathOfMultimedia, final String thumb_id,
			final ChatMessage msg) {
		msg.initExtra();

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
			String media_id = null;
			boolean isFileUploadSuccess = true;

			@Override
			protected Void doInBackground(Void... params) {
				WowTalkWebServerIF.getInstance(MessageComposerActivityBase.this).fPostFileToServer(
						pathOfMultimedia, true,
                        new NetworkIFDelegate(){
                            @Override
                            public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                                isFileUploadSuccess = false;
                            }

							@Override
							public void didFinishNetworkIFCommunication(int arg0,
									byte[] arg1) {
								media_id = new String(arg1);
							}

							@Override
							public void setProgress(int tag, final int progress) {
								publishProgress(progress);
							}

						}, 0);


				if(media_id != null) {
                    if(msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)
                            || msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO))
                        msg.formatContentAsPhotoMessage(media_id, thumb_id);
                    else if(msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
						MediaPlayer player = new MediaPlayer();
						try {
							player.setDataSource(pathOfMultimedia);
							player.prepare();
						} catch (Exception e) {
							e.printStackTrace();
						}
						int duration = player.getDuration() / 1000;
						player.release();
						player = null;
                        msg.formatContentAsVoiceMessage(media_id, duration);
					}
                    if(msg.isGroupChatMessage) {
                        mWebif.fGroupChat_SendMessage(msg.chatUserName, msg);
                    } else {
                        boolean ok = WowTalkVoipIF.getInstance(MessageComposerActivityBase.this).fSendChatMessage(msg);
                    }
				} else {
					msg.sentStatus = ChatMessage.SENTSTATUS_NOTSENT;
					mDbHelper.setChatMessageCannotSent(msg);
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... param) {
				ProgressBar progressBar = (ProgressBar)msg.extraObjects.get("progressBar");
				if(progressBar != null) {
					progressBar.setProgress(param[0]);
				}
			}

            @Override
            protected void onPostExecute(Void result) {
                if (!isFileUploadSuccess) {
                    mMsgBox.toast(R.string.messagecomposerbase_upload_file_failure);
                }
                msg.extraData.putBoolean("isTransferring", false);
//                mDbHelper.updateChatMessage(msg, true);
                final ProgressBar progressBar = (ProgressBar)msg.extraObjects.get("progressBar");
                if(progressBar != null)
                    progressBar.setVisibility(View.GONE);
//                showSentStatus(msg);
			}
		});
	}

    /**
     * 进入此页面，请使用launch方法
     * @param context
     * @param subclassType
     * @param uid
     */
    public static void launchToChatWithBuddy(Context context, Class<?> subclassType, String uid) {
        launchToChatWithBuddy(context, subclassType, uid, null);
    }

    /**
     * @param context
     * @param subclassType
     * @param uid
     * @param hello 若非 null，则自动向对方发送此文本消息。
     */
    public static void launchToChatWithBuddy(Context context, Class<?> subclassType, String uid, String hello) {

		Database db = new Database(context);
		// 查询是否还是好友
		Buddy b = db.buddyWithUserID(uid);
        if(null == b) {
            return;
        }
		mCanSendMsg = (0 != (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) ? CAN_SEND_MSG_OK : CAN_SEND_MSG_NO_FRIENDS;
		if(0 == (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)){
			ArrayList<String> groupIds = db.getGroupIdsByMemberId(uid);
			if(groupIds != null){
				for(String groupId : groupIds){
					GroupChatRoom group = db.fetchGroupChatRoom(groupId);
					if(group != null){
						if(group.category.equals(GroupChatRoom.CATEGORY_CLASSROOM)){
							mCanSendMsg = CAN_SEND_MSG_OK;
						}
					}
				}
			}
		}
		sCanSendMsgInited = true;
		String displayName = (b == null ?
				context.getString(R.string.msg_session_unknown_buddy)
				: (TextUtils.isEmpty(b.alias) ? b.nickName : b.alias));
		String phoneNumber = (b == null ? null : b.phoneNumber);

		Intent intent = new Intent(context, subclassType);
		intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID, 
				uid);
		intent.putExtra(
				MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME, 
				displayName);
		intent.putExtra(
				MessageComposerActivityBase.KEY_TARGET_PHONENUMBER, 
				phoneNumber);
        intent.putExtra(KEY_HELLO_MESSAGE, hello);
		context.startActivity(intent);
	}

    /**
     * used this methord to relay picture
     * @param context
     * @param uid
     * @param path
     * @param isFinish
     */
    public static void launchToChatWithBuddyWithPicture(Context context, String uid,String[] path,boolean isFinish){
        Database db = new Database(context);
        // 查询是否还是好友
        Buddy b = db.buddyWithUserID(uid);
        if(null == b) {
            return;
        }
        mCanSendMsg = (0 != (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) ? CAN_SEND_MSG_OK : CAN_SEND_MSG_NO_FRIENDS;
        if(0 == (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)){
            ArrayList<String> groupIds = db.getGroupIdsByMemberId(uid);
            if(groupIds != null){
                for(String groupId : groupIds){
                    GroupChatRoom group = db.fetchGroupChatRoom(groupId);
                    if(group != null){
                        if(group.category.equals(GroupChatRoom.CATEGORY_CLASSROOM)){
                            mCanSendMsg = CAN_SEND_MSG_OK;
                        }
                    }
                }
            }
        }
        sCanSendMsgInited = true;
        String displayName = (b == null ?
                context.getString(R.string.msg_session_unknown_buddy)
                : (TextUtils.isEmpty(b.alias) ? b.nickName : b.alias));
        String phoneNumber = (b == null ? null : b.phoneNumber);

        Intent intent = new Intent(context, MessageComposerActivity.class);
        intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID,uid);
        intent.putExtra(MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME,displayName);
        intent.putExtra(MessageComposerActivityBase.KEY_TARGET_PHONENUMBER,phoneNumber);
        intent.putExtra(MessageComposerActivity.LAUCH_WITH_SEND_MSG,true);
        intent.putExtra(MessageComposerActivity.SEND_PIC_PATH,path[0]);
        intent.putExtra(MessageComposerActivity.SEND_PIC_THUMB,path[1]);
        context.startActivity(intent);
        if(isFinish && context instanceof Activity){
            ((Activity)context).finish();
        }
    }

	public static void launchToChatWithGroup(Context context, Class<?> subclassType, String group_id) {
		Database db = new Database(context);
		GroupChatRoom g = db.fetchGroupChatRoom(group_id);
		if(g == null)
			return;
		launchToChatWithGroup(context, subclassType, g);
	}

	public static void launchToChatWithGroup(Context context, Class<?> subclassType, GroupChatRoom g) {
		if(g == null)
			return;

        // 查询是否还属于此群组
        Database db = new Database(context);
        String myUid = PrefUtil.getInstance(context).getUid();
        mCanSendMsg = db.isExistsInGroupChatRoom(myUid, g.groupID) ? CAN_SEND_MSG_OK : CAN_SEND_MSG_NO_GROUP_MEMBER;
        sCanSendMsgInited = true;
		Intent intent = new Intent(context,
				subclassType);
		intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID, 
				g.groupID);
		if(g.isTemporaryGroup) {
			intent.putExtra(
					MessageComposerActivityBase.KEY_TARGET_IS_TMP_GROUP, 
					true);
		} else {
			intent.putExtra(
					MessageComposerActivityBase.KEY_TARGET_IS_NORMAL_GROUP, 
					true);
		}
		intent.putExtra(
				MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME, 
				g.getDisplayName());
		context.startActivity(intent);
	}

    /**
     * this methord is used to send picture to group
     * @param context
     * @param group_id
     * @param path
     * @param isFinish
     */
    public static void launchToChatWithGroupWithPicture(Context context, String group_id,String[] path,boolean isFinish) {
        Database db = new Database(context);
        GroupChatRoom g = db.fetchGroupChatRoom(group_id);
        if(g == null)
            return;

        // 查询是否还属于此群组
        String myUid = PrefUtil.getInstance(context).getUid();
        mCanSendMsg = db.isExistsInGroupChatRoom(myUid, g.groupID) ? CAN_SEND_MSG_OK : CAN_SEND_MSG_NO_GROUP_MEMBER;
        sCanSendMsgInited = true;
        Intent intent = new Intent(context,MessageComposerActivity.class);
        intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID,g.groupID);
        intent.putExtra(MessageComposerActivity.LAUCH_WITH_SEND_MSG,true);
        intent.putExtra(MessageComposerActivity.SEND_PIC_PATH,path[0]);
        intent.putExtra(MessageComposerActivity.SEND_PIC_THUMB,path[1]);
        if(g.isTemporaryGroup) {
            intent.putExtra(MessageComposerActivityBase.KEY_TARGET_IS_TMP_GROUP,true);
        } else {
            intent.putExtra(MessageComposerActivityBase.KEY_TARGET_IS_NORMAL_GROUP,true);
        }
        intent.putExtra(
                MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME,
                g.getDisplayName());
        context.startActivity(intent);
        if(isFinish && context instanceof Activity){
            ((Activity) context).finish();
        }
    }

	/**
	 * 组织架构内聊天时，不需要查询是否属于此群组；但临时会话还需要判断 {@link #launchToChatWithGroup()}
	 * @param context
	 * @param subclassType
	 * @param g
	 */
	public static void launchToChatWithGroupForBizDept(Context context, Class<?> subclassType, GroupChatRoom g) {
        if(g == null)
            return;

        mCanSendMsg = CAN_SEND_MSG_OK;
        sCanSendMsgInited = true;
        Intent intent = new Intent(context,
                subclassType);
        intent.putExtra(MessageComposerActivityBase.KEY_TARGET_UID, 
                g.groupID);
        if(g.isTemporaryGroup) {
            intent.putExtra(
                    MessageComposerActivityBase.KEY_TARGET_IS_TMP_GROUP, 
                    true);
        } else {
            intent.putExtra(
                    MessageComposerActivityBase.KEY_TARGET_IS_NORMAL_GROUP, 
                    true);
        }
        intent.putExtra(
                MessageComposerActivityBase.KEY_TARGET_DISPLAYNAME, 
                g.getDisplayName());
        context.startActivity(intent);
    }

    /**
     * 隐藏键盘
     */
    protected void messageClicked() {
	     //txt_content.clearFocus(); // cause a flicker on keyboard
	     mInputMgr.setSoftKeyboardVisibility(false);
	     mInputMgr.setInputMode(InputBoardManager.FLAG_SHOW_TEXT, false); // close multimedia/emotion panel
	 }

	 protected String getTargetDisplayName() {
		 return _targetDisplayName == null ? _targetGlobalPhoneNumber : _targetDisplayName;
	 }

    /**
     * 滑动listview到底部
     */
    private void scrollMsgListToBotomDelay() {
        mHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                if(lv_message.getCount() > 0) {
                    lv_message.setSelection(lv_message.getCount() - 1);
                }
            }
        }, 300);
    }

    /**
     * 向外进行语音电话操作
     * @param uid
     * @param displayName
     * @param initWithVideo
     */
    protected void confirmOutgoingCall(final String uid,final String displayName,final boolean initWithVideo) {
        Buddy buddy=mDbHelper.buddyWithUserID(uid);
        String userName=TextUtils.isEmpty(buddy.alias)?buddy.nickName:buddy.alias;
        CallMainActivity.startNewOutGoingCall(MessageComposerActivityBase.this, uid, displayName, initWithVideo);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(R.string.call);
//        builder.setMessage(userName);
//
//        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.dismiss();
//                CallMainActivity.startNewOutGoingCall(MessageComposerActivityBase.this, uid, displayName, initWithVideo);
//            }
//        });
//        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.dismiss();
//            }
//        });
//        AlertDialog dialog = builder.create();
//        dialog.show();
    }

    @Override
    public void onHeightChanged(int height) {
        lv_message.setPadding(0, 0, 0, 10 + height);
    }

    @Override
    public void onTextInputted(String text) {
        if (TextUtils.isEmpty(text.trim())) {
            mMsgBox.toast(R.string.msg_text_is_empty);
            return;
        }
        mInputMgr.setInputText(""); // prevent duplicate sending
        fSendTextMsg_async(text);
    }

    @Override
    public void onVoiceInputted(String path, int duration) {
        fSendMediaMsg_async(path, null, duration, ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE);
    }

    @Override
    public void onStampInputted(Stamp s) {
        fSendStampMsg_async(s);
    }

    @Override
    public void onPhotoInputted(String path, String thumbPath) {
        Log.i("photo inputted");
        fSendMediaMsg_async(path, thumbPath, 0, ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO);
    }

    @Override
    public void onHybirdInputted(String text,
                                 String imagePath, String imageThumbPath,
                                 String audioPath, int duration) {
        fSendHybird_async(text, imagePath, imageThumbPath, audioPath, duration);
    }

    @Override
    public void onHybirdRequested() {
        startActivityForResult(
                new Intent(this, HybirdMessageEditor.class)
                        .putExtra(HybirdMessageEditor.EXTRA_PAGE_TITLE, getString(R.string.title_hybird_message))
                        .putExtra(HybirdMessageEditor.EXTRA_IMAGE_MAX_COUNT, 1)
                        .putExtra(HybirdMessageEditor.EXTRA_ALLOW_VOICE, true)
                , REQ_HYBIRD);
    }

    @Override
    public void onVideoInputted(String path, String thumbPath) {
        fSendMediaMsg_async(path, thumbPath, 0, ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE);
    }

    @Override
    public void onVideoChatRequested() {
//        CallMainActivity.startNewOutGoingCall(this, _targetUID, _targetDisplayName, true);
        confirmOutgoingCall(_targetUID, _targetDisplayName, true);
    }

    @Override
    public void onCallRequested() {
//        CallMainActivity.startNewOutGoingCall(this, _targetUID, _targetDisplayName, false);
        confirmOutgoingCall(_targetUID, _targetDisplayName, false);
    }

    @Override
    public void onLocationInputted(double latitude, double longitude, String address) {
        fSendLocMsg_async(latitude, longitude);
    }

    @Override
    public void setInputBoardMangager(InputBoardManager m) {
        mInputMgr = m;
    }

    @Override
    public InputBoardManager getInputBoardMangager() {
        return mInputMgr;
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }

    @Override
    public void willRecordAudio() {
        myAdapter.stopPlayingVoice();
    }
}

