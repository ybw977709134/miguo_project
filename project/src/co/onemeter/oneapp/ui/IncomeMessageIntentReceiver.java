package co.onemeter.oneapp.ui;

import android.app.*;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ChatMessageHandler;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class IncomeMessageIntentReceiver extends BroadcastReceiver {

    private static Dialog sDialog;
    private static int sCounter;
    private static Context context;

	@Override
	public void onReceive(final Context context, Intent intent) {
		this.context = context;
        Connect2.setContext(context);

        /* @Deprecated
         * 这里主要用来为新消息显示通知，针对新消息的业务逻辑请在
         *  WowTalkUIChatMessageDelegate.getChatMessage()
         * 中处理。
         */
        // 原先是通知提示和业务逻辑分开的，但会调同一个handle方法
        // 现在在此处处理通知提示和业务逻辑
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
            final Database database=new Database(context);

			ChatMessage msgTmp = new ChatMessage(bundle);

            final ChatMessage msg;
            ChatMessage msgInDb=database.fetchChatMessageByPrimaryKey(msgTmp.primaryKey);
            if(null != msgInDb) {
                Log.i("msg for "+msgInDb.primaryKey+" not empty in db,unique_key="+msgInDb.uniqueKey);
                msg=msgInDb;
            } else {
                Log.e("msg not exist in db?????can not set readed!!!");
                msg=msgTmp;
            }
            Log.i("IncomeMessageIntentReceiver#onReceive, the original "
                    + "msg display name is " + msg.displayName);

            // 修正一些属性以便展示，比如把通知消息的JSON翻译为可读文字。
            // 为了避免覆盖其它线程对数据库的修改，这里不把 handle 结果保存到数据库中。

            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    ChatMessageHandler h = new ChatMessageHandler(context);
                    h.handle(msg, true, null);

                    String name2show = msg.displayName;
                    if (msg.isGroupChatMessage) {
                        Buddy b = database.buddyWithUserID(msg.groupChatSenderID);
                        if (null != b) {
                            name2show = TextUtils.isEmpty(b.nickName) ? b.username : b.nickName;
                        }
                    }
                    return name2show;
                }

                @Override
                protected void onPostExecute(String name2show) {
                    if (TextUtils.isEmpty(name2show) || "Unknown".equals(name2show)) {
                        Log.e("IncomeMessageIntentReceiver#onReceive, the display name is error!(" + name2show);
                        return;
                    }
                    database.triggerChatMessageObserver();
                    PrefUtil mPrefUtil = PrefUtil.getInstance(context);
                    if (mPrefUtil.isSysNoticeEnabled() && mPrefUtil.isSysNoticeNewMessageOn()) {
                        // 先尝试发通知，如果此消息不需发通知，也就不需要铃声和震动了1
                        if (showNotification(context, msg, name2show)) {
                            if (mPrefUtil.isSysNoticeMusicOn()) {
                                SoundPoolManager.playSoundFromRaw(context, R.raw.new_msg_incoming);
                            }
                            if (mPrefUtil.isSysNoticeVibrateOn()) {
                                long[] vibrates = new long[]{
                                        Utils.VIRBRATE_TIME_DELAY,
                                        Utils.VIRBRATE_TIME_FIRST,
                                        Utils.VIRBRATE_TIME_INTERVAL,
                                        Utils.VIRBRATE_TIME_SECOND};
                                Utils.triggerVibrate(context, vibrates, Utils.VIRBRATE_REPEAT_NO);
                            }

                        }
                    }
                }

            });


//            showNotification(context, msg);
		}
	}

    public final static String INCOME_MSG_NOTIFICATION_PARA_ID="income_msg_notification_para_id";
    public final static String INCOME_MSG_NOTIFICATION_PARA_IS_GROUP_MSG="income_msg_notification_para_is_group_msg";

    /**
     *
     * @param context
     * @param msg
     * @param name2show
     * @return 如果消息不符合发通知的条件，则返回 false.
     */
    @SuppressWarnings("deprecation")
    public static boolean showNotification(final Context context, final ChatMessage msg,String name2show) {
		if (msg == null || msg.displayName == null || msg.messageContent == null)
			return false;

		String strCompositeName = msg.displayName;
        if(!TextUtils.isEmpty(name2show)) {
            strCompositeName=name2show;
        }
		String strMessage;
		String strTickerMsg = null;

        Intent startIntent=new Intent(context, StartActivity.class);
        startIntent.putExtra(INCOME_MSG_NOTIFICATION_PARA_ID,msg.chatUserName);
        startIntent.putExtra(INCOME_MSG_NOTIFICATION_PARA_IS_GROUP_MSG,msg.isGroupChatMessage);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent intent = PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE.equals(msg.msgType)
                || ChatMessage.MSGTYPE_SYSTEM_PROMPT.equals(msg.msgType)
                || ChatMessage.MSGTYPE_REQUEST_REJECT.equals(msg.msgType)
                || ChatMessage.MSGTYPE_GET_MISSED_CALL.equals(msg.msgType)) {
			strMessage = msg.messageContent;
			strTickerMsg = strCompositeName + ":" + strMessage;
		} else {
			if (msg.msgType.equals(ChatMessage.MSGTYPE_LOCATION)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_STAMP)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
            } else if (msg.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)) {
                strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VCF)) {
				strMessage = context.getString(R.string.newer_chatmessage_receive);
			} else if (msg.msgType.equals(ChatMessage.MSGTYPE_OFFICIAL_ACCOUNT_MSG)) {
                strMessage = msg.messageContent;
            } else if (msg.msgType.equals(ChatMessage.MSGTYPE_THIRDPARTY_MSG)) {
                // TODO 处理推送通知
                Log.i("receive MSGTYPE_THIRDPARTY_MSG: ", msg.messageContent);
                strMessage = null;
            } else {
                strMessage = null;
            }

			strTickerMsg = strTickerMsg == null ? null : (strCompositeName + " " + strMessage);
		}
        

        boolean notified = false;

        if (null != strTickerMsg) {
            // 请求被拒绝的消息，不会保存数据库中，只能通过通知形式告知用户
            if (!msg.msgType.equals(ChatMessage.MSGTYPE_REQUEST_REJECT)
                    && (StartActivity.isOnStackTop || MessageComposerActivity.isOnStackTop)) {
                return false;
            } else {
                if (null != intent) {
                    // notification
                    NotificationManager notiManager = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification note = new Notification(R.drawable.icon_notification, strTickerMsg,
                            System.currentTimeMillis());
                    note.setLatestEventInfo(context, strCompositeName, strMessage, intent);
                    notiManager.notify(GlobalValue.NOTIFICATION_FOR_CHATMESSAGE, note);
                    notified = true;
                }
            }

            // toast
            if (!isAppOnForeground(context)) {

                View toastView = LayoutInflater.from(context).inflate(R.layout.incoming_message_toast, null);
                fillToastView(toastView, strCompositeName, strMessage);
                if (null == sDialog) {
                    sDialog = new Dialog(context, R.style.system_dialog);
                    sDialog.setContentView(toastView);
                    sDialog.setCanceledOnTouchOutside(true);
                    Window dialogWindow = sDialog.getWindow();
                    WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                    dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
                    lp.y = DensityUtil.dip2px(context, 60); // 新位置Y坐标
                    lp.width = DensityUtil.dip2px(context, 280);
                    lp.height = DensityUtil.dip2px(context, 50);
                    sDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                } else {
                    sDialog.setContentView(toastView);
                }
                if (sCounter >= Integer.MAX_VALUE) {
                    sCounter = 0;
                }
                final int counter = ++sCounter;
                sDialog.show();

                toastView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (sDialog.isShowing()) {
                            sDialog.dismiss();
                        }
                        Intent intent = new Intent(context, StartActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(INCOME_MSG_NOTIFICATION_PARA_ID,msg.chatUserName);
                        intent.putExtra(INCOME_MSG_NOTIFICATION_PARA_IS_GROUP_MSG,msg.isGroupChatMessage);
                        context.startActivity(intent);
                    }
                });
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (counter == sCounter && sDialog.isShowing()) {
                            sDialog.dismiss();
                        }
                    }
                }, 2500);
            }
        }

        return notified;
	}
    
    /**
     * 该方法主要是为了在退出帐号时，取消发给这个帐号的通知，如果不取消，点击通知栏将直接进入全为空的启动页
     * @author by hutianfeng
     * @date 2015/1/22
     */
    public static void closeNoticeMessage() {
    	if (context != null) {
    		NotificationManager manager = (NotificationManager)
    				context.getSystemService(Context.NOTIFICATION_SERVICE);
    		manager.cancel(GlobalValue.NOTIFICATION_FOR_CHATMESSAGE);
    	}
    }
    
    private static void fillToastView(View toastView, String buddyName, String message) {
        TextView buddyView = (TextView) toastView.findViewById(R.id.buddy_name);
        TextView msgView = (TextView) toastView.findViewById(R.id.message_content);
        buddyView.setText(buddyName);
        msgView.setText(message);
    }

    private static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> apps = activityManager.getRunningAppProcesses();
        if (null == apps) {
            return false;
        }

        String packageName = context.getPackageName();
        for (RunningAppProcessInfo appInfo : apps) {
            if (appInfo.processName.equals(packageName)) {
                return appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }
}
