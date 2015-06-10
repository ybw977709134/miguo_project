package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.TimeHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.component.*;
import org.wowtalk.video.Version;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class IncallActivity extends Activity implements OnClickListener, 
		org.wowtalk.api.WowTalkUICallProcessDelegate{
	private static IncallActivity instance;
    public static boolean isOnStackTop = false;
	private static final int INTENT_VIDEO_ACTVITY = 100;
	private final int BUTTON_UNABLED_ALPHA = 150;
	public static final String CONTACT_UID = "contact_uid";
	public static final String CONTACT_DISPLAY = "contact_displayname";
	
	private String mStrContact;
	private String mStrContactDisplayName;
	
	private ImageView imgPhoto;
	private TextView txtCallDirection;
	private IncallTimer callTimer;
	private TextView txtCallName;
	private PauseResumeButton btnHold;
	private AddVideoButton btnAddVideo;
	private MuteMicButton btnMute;
	private SpeakerButton btnSpeaker;
	private HangCallButton btnEndCall;
	private HangCallButton btnDecline;
	private CallButton btnAnswer;
	private LinearLayout mCalling;
	private LinearLayout mConnected;
	
	private boolean bIsStartingVideo;
	private boolean isResetingCall;
	private int mDuration;
	private CallLog callLog;
	private ChatMessage chatMsg;
	private Database dbHelper;
	private TimerTask mCallProcessTimer;
	private TimerTask mEndCallTimer;
	private MediaPlayer mRingerInPlayer;
	private MediaPlayer mRingerOutPlayer;
	private Handler mHandler = new Handler();
	private Timer timer = new Timer();

    private boolean mUiInitialized = false;
	
	public static final IncallActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}
	
	
	private boolean isCallConnected=false;
	@Override
	public void displayCallConnectedForUser(String username, String displayName) {
		Log.e("CallProcess : displayCallConnectedForUser:[username=" + username + ", displayName=" + displayName + "]");
		stopRingIn();
		stopRingOut();
		stopEndCallTimer();
		txtCallDirection.setVisibility(View.GONE);
		callTimer.setVisibility(View.VISIBLE);
		mCalling.setVisibility(View.GONE);
		mConnected.setVisibility(View.VISIBLE);
		btnEndCall.setVisibility(View.VISIBLE);
		btnAddVideo.mCalleeName = WowTalkVoipIF.mCalleeName;
		btnAddVideo.reset();
		WowTalkVoipIF.mCallState = CallState.Call_Connected;
		isResetingCall = false;
		mDuration = 0;
        isCallConnected=true;
		
		mCallProcessTimer = new TimerTask() {

			@Override
			public void run() {
				if (!WowTalkVoipIF.fIsIncall())
					return;
				mDuration = WowTalkVoipIF.fGetCurrentCallDuration();
				Log.e("call connected duration = " + mDuration);
				if (mDuration == 0)
					return;
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						callTimer.setText(String.valueOf(mDuration));
					}
					
				});
				final int quality = (int) WowTalkVoipIF.fGetCurrentCallQuality();
			}
			
		};
		timer.scheduleAtFixedRate(mCallProcessTimer, 0, 1000);
	}
	
	@Override
	public void displayCallFailed(String username, String displayName, String error) {
		Log.e("CallProcess : displayCallFailed:[username=" + username + ", displayName=" + displayName + ", error=" + error + "]");
		final String strError = error;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
                endupCallProcess();
//                finish();
			}
			
		});
	}

	/**
	 * 处理ios在后台的情况，以及android/ios没有网络的情况
	 */
	@Override
	public void displayCallRingingWhenCalleeOffline(String userId, String displayName) {
		Log.e("callProcess : " + "displayCallRingingWhenCalleeOffline:[userId=" + userId + ", " + "displayName=" + displayName + "]");
//		if (WowTalkVoipIF.mCallState != CallState.OutgoCall_Init ||
//				userId.equals(WowTalkVoipIF.mCalleeName))
//				return;

		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeOffline;

		mEndCallTimer = new TimerTask() {

			@Override
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						endupCallByTimer();

//                        endupCallProcess();
					}
					
				});
			}
			
		};
		timer.scheduleAtFixedRate(mEndCallTimer, 30000, 1000);
	}
	@Override
	public void displayCallTimeout(String username, String displayName) {
		Log.e("callProcess : " + "displayCallTimeout:[username=" + username + ", " + "displayName=" + displayName + "]");
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CallNoAnswer;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
                endupCallProcess();
			}
			
		});
	}
	@Override
	public void displayCalleeBusy(String username, String displayName) {
		Log.e("CallProcess : diaplayCalleeBusy:[username=" + username + ",displayName=" + displayName + "]");
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeBusy;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
                endupCallProcess();
			}
			
		});
	}

    private boolean isCalledRinged=false;
	@Override
	public void displayCalleeRinging(String username, String displayName) {
		Log.e("callProcess : " + "displayCalleeRinging:[username=" + username + ", displayName" + displayName + "]");
        isCalledRinged=true;
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeRinging;
	}

    /**
     * 对方已经logout或用户不存在的情况
     */
    @Override
    public void displayMakeRegularCall(String username, String displayName) {
        Log.e("callProcess : " + "displayMakeRegularCall:[username=" + username + ", displayName=" + displayName + "]");
        // 此处应该是对方不在线（已经logout），但由于和offline混淆(#displayCallRingingWhenCalleeOffline)，
        // 所以此处使用CallState.OutgoCall_Callee_Logout_NotExist代替
        WowTalkVoipIF.mCallState = CallState.OutgoCall_Callee_Logout_NotExist;
        mEndCallTimer = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        WowTalkVoipIF.fTerminateCall(true);
                        endupCallProcess();
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(mEndCallTimer, 5000, 1000);
    }

	@Override
	public void displayPausedByCallee(String username, String displayName) {
		Log.e("callProcess : " + "displayPausedByCallee:[username=" + username + ", displayName=" + displayName + "]");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				btnHold.setEnabled(false);
				btnHold.setCheckedWithoutAction(false);
				
			}
			
		});
	}
	@Override
	public void displayPausedByMe() {
		Log.e("callProcess : " + "displayPausedByMe");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				btnAddVideo.setEnabled(false);
			}
			
		});
	}
	@Override
	public void displayResume() {
		Log.e("CallProcess : " + "displayResume");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				btnHold.setEnabled(true);
				btnHold.setCheckedWithoutAction(false);
				btnAddVideo.setEnabled(true);
				
			}
			
		});
	}
	@Override
	public void displayVideoCallRejected(String username, String displayName) {
		Log.e("callProcess : " + "displayVideoCallRejected:[username=" + username + ", displayName=" + displayName + "]");
		
		if (username == null)
			return;
		
		if (!username.equals(WowTalkVoipIF.mCalleeName))
			return;
		
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				btnAddVideo.setOff();
                new MessageBox(IncallActivity.this).toast(R.string.video_request_rejected_by_peer);
			}
			
		});
	}
	@Override
	public void displayVideoRequest(String username, String displayName, boolean addVideo) {
		Log.e("callProcess : " + "displayVideoRuquest:[username=" + username + ", displayName=" + displayName + "]");
		
		if (username == null)
			return;
		
		if (!username.equals(WowTalkVoipIF.mCalleeName))
			return;

        if (mInitWithVideo) {
            btnAddVideo.acceptVideoAndReinvite();
        } else {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    new AlertDialog.Builder(IncallActivity.this)
                            .setTitle("")
                            .setMessage(getResources().getString(R.string.video_request))
                            .setPositiveButton(getResources().getString(R.string.video_request_answer),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            arg0.dismiss();
                                            btnAddVideo.acceptVideoAndReinvite();
                                        }
                                    })
                            .setNegativeButton(getResources().getString(R.string.video_request_decline),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            WowTalkVoipIF.fRejectTheVideoCallInvite(WowTalkVoipIF.mCalleeName);
                                            btnAddVideo.setOff();
                                        }
                                    }).show();
                }

            });
        }
	}

    private int getCallFinishAbnormalToastMsgId() {
        int retId=0;
        if(isCalledRinged) {
            retId=R.string.callee_no_answer;
        } else {
            retId=R.string.callee_offline;
        }

        return retId;
    }
	
	private void finalizeCallLogging(int duration) {
		Log.e("finalizeCallLogging... : WowTalkVoipIF.mCallState = " + WowTalkVoipIF.mCallState);
        if(null == callLog) {
            Log.e("call log null");
            return;
        }
		callLog.duration = duration;		
		callLog.quality = 5;

        int toastMsgId=0;

		switch(WowTalkVoipIF.mCallState) {
		case CallState.OutgoCall_CalleeBusy:
			chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
		case CallState.OutgoCall_CallNoAnswer:
			chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
		case CallState.OutgoCall_CalleeOffline:
			callLog.status = CallLog.WowTalkCallSuccess;
			WowTalkVoipIF.getInstance(this).fNotifyCalleeForMissedCall(callLog);
			chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
        case CallState.OutgoCall_Callee_Logout_NotExist:
            chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId = getCallFinishAbnormalToastMsgId();
            break;
		case CallState.OutgoCall_CalleeRinging:
			chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
		case CallState.Call_Connected:
			callLog.status = CallLog.WowTalkCallSuccess;
			chatMsg.msgType = ChatMessage.MSGTYPE_CALL_LOG;
			chatMsg.messageContent = String.valueOf(duration);
			break;
		case CallState.OutgoCall_Init:
			chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
		case CallState.IncomeCall_Init:
			callLog.status = CallLog.WowTalkCallMissed;
			chatMsg.msgType = ChatMessage.MSGTYPE_GET_MISSED_CALL;
//            toastMsgId=getCallFinishAbnormalToastMsgId();
			break;
        case CallState.OUTGOCALL_HANGUP:
            if(isCallConnected) {
                callLog.status = CallLog.WowTalkCallSuccess;
                chatMsg.msgType = ChatMessage.MSGTYPE_CALL_LOG;
                chatMsg.messageContent = String.valueOf(duration);
            } else {
                chatMsg.msgType = ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED;
            }
            break;
		default:
			break;
		}

        if (0 != toastMsgId) {
            new MessageBox(IncallActivity.this).toast(toastMsgId);
        }
        Log.e("finalizeCallLogging...... msgType : " + chatMsg.msgType);
        if (chatMsg.displayName == null || chatMsg.displayName.equals("")) {
            chatMsg.displayName = mStrContactDisplayName;
        }
        dbHelper.storeNewCallLog(callLog);
        dbHelper.storeNewChatMessage(chatMsg, true);
		Log.e("Database calllogs count=", ""
				+ dbHelper.fetchAllCallLogs().size());
		
		if (callLog.status == CallLog.WowTalkCallMissed) {
			if (StartActivity.isInstanciated()) {
				
			} else {
				Log.e("broadcast missed call intent!");
				Intent intent = new Intent(GlobalValue.INCOME_MISSEDCALL_INTENT);
				intent.putExtras(callLog.toBundle());
				sendBroadcast(intent);
			}
		}
	}
	
	@Override
	public void endupCallProcess() {
		Log.e("CallProcess : " + "endupCallProcess, mCallState is " + WowTalkVoipIF.mCallState);
		if (WowTalkVoipIF.mCallState == CallState.CallEnd) {
			return;
		}
		
		if (isResetingCall) {
			isResetingCall = false;
			return;
		}
		
		stopRingIn();
		stopRingOut();
		stopEndCallTimer();
		finalizeCallLogging(mDuration);
		
		if (mCallProcessTimer != null) {
			mCallProcessTimer.cancel();
			mCallProcessTimer = null;
		}
		
		WowTalkVoipIF.mCallState = CallState.CallEnd;
		btnAddVideo.setOff();
		btnAddVideo.setEnabled(false);
		btnAnswer.setEnabled(false);
		btnDecline.setEnabled(false);
		btnEndCall.setEnabled(false);

        instance = null;
        WowTalkVoipIF.fSetCallProcessDelegate(null);
        finish();

        // 未接来电才发送通知
        // 未接／拒绝，msgType都是ChatMessage.MSGTYPE_GET_MISSED_CALL
        // 未接，ioType是ChatMessage.IOTYPE_INPUT_UNREAD；
        // 拒绝，ioType是ChatMessage.IOTYPE_INPUT_READED
        if (ChatMessage.MSGTYPE_GET_MISSED_CALL.equals(chatMsg.msgType)
                && ChatMessage.IOTYPE_INPUT_UNREAD.equals(chatMsg.ioType)) {
            // 发送通知的时候，会判断是否在StartActivity或者MessageComposerActivity，
            // 但当前页面(IncallActivity)非上述两个，为了避免在上述两个界面有未接电话时仍有通知，
            // 此处使用Handler#postDelayed
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    chatMsg.messageContent = getString(R.string.call_missed);
                    IncomeMessageIntentReceiver.showNotification(getApplicationContext(), chatMsg, null);
                }
            }, 1000);
        }
	}
	@Override
	public void finishVideoActivity() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				finishActivity(INTENT_VIDEO_ACTVITY);
			}
			
		});
		
	}
	
	private void stopEndCallTimer() {
		if (mEndCallTimer != null) {
			mEndCallTimer.cancel();
			mEndCallTimer = null;
		}
	}
	
	private void endupCallByTimer() {
		if (WowTalkVoipIF.mCallState == CallState.OutgoCall_CalleeOffline) {
			endupCallProcess();
			return;
		}
		WowTalkVoipIF.fTerminateCall(false);
	}
	
	@Override
	public void getCalleeBackOnlineMessage(String userId, String displayName) {
		if (WowTalkVoipIF.mCallState == CallState.OutgoCall_CalleeOffline && WowTalkVoipIF.mCalleeName.equals(userId)) {
			WowTalkVoipIF.getInstance(this).fNewOutgoingCall(userId, displayName, false);
			stopEndCallTimer();
			return;
		}
		
		if (WowTalkVoipIF.mCallState == CallState.OutgoCall_Init) {
			isResetingCall = true;
			WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeOffline;
			
			mEndCallTimer = new TimerTask() {

				@Override
				public void run() {
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							endupCallByTimer();
						}
						
					});
				}
				
			};
			timer.schedule(mEndCallTimer, 30000, 1000);
			WowTalkVoipIF.fTerminateCall(false);
			WowTalkVoipIF.getInstance(this).fNewOutgoingCall(userId, displayName, false);
			return;
		}
		
	}
	@Override
	public void startVideoActivity() {
		this.bIsStartingVideo = true;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				startActivityForResult(new Intent().setClass(IncallActivity.this, VideoCallActivity.class), INTENT_VIDEO_ACTVITY);
			}
			
		});
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_endcall:
            Log.e("endupCallProcess called by onClick!");
            Log.d("onclick#endcall,hangup.");
            WowTalkVoipIF.mCallState = CallState.OUTGOCALL_HANGUP;
            endupCallProcess();
            break;
        case R.id.btn_decline:
        case R.id.btn_answer:
            chatMsg.ioType = ChatMessage.IOTYPE_INPUT_READED;
            break;
        default:
            endupCallProcess();
            break;
        }
    }

	public void showDisplayMsg(int resId) {
		
	}
	
	public void showDisplayMsg(String msg) {
		
	}
	
	void startCallLogging(String direction, String username, String displayName) {
		if (callLog != null) {
			callLog = null;
		}
		
		callLog = new CallLog();
		callLog.startDate = Database.callLog_dateToUTCString(new Date());
		callLog.direction = direction;
		callLog.contact = username;
		callLog.displayName = displayName;
		
		if (chatMsg != null) {
			chatMsg = null;
		}
		chatMsg = new ChatMessage();
        chatMsg.sentDate = TimeHelper.getTimeForMessage(this);
        chatMsg.chatUserName = username;
        chatMsg.displayName = displayName;
        if (direction.equals(WowTalkVoipIF.IO_IN)) {
            chatMsg.ioType = ChatMessage.IOTYPE_INPUT_UNREAD;
        } else if (direction.equals(WowTalkVoipIF.IO_OUT)) {
            Log.e("call out >>>>>>>>>>>>>>>>>!");
            chatMsg.ioType = ChatMessage.IOTYPE_OUTPUT;
        }
    }

    private static boolean mInitWithVideo = false;

	private void displayCallIncomingViewForUser(String username, String displayName, boolean initWithVideo) {
		Log.e("CallProcess : displayCallIncomingViewForUser:[username=" + username + ", displayName=" + displayName + "]");
        getBuddyDetailByUserId(username, imgPhoto, txtCallName);
		stopEndCallTimer();
		WowTalkVoipIF.mCalleeName = username;
		mDuration = 0;
		txtCallDirection.setVisibility(View.VISIBLE);
		callTimer.setVisibility(View.GONE);
        if (initWithVideo) {
            txtCallDirection.setText(getString(R.string.apns_video)+" "+getString(R.string.call_imcoming));
        } else {
            txtCallDirection.setText(R.string.call_imcoming);
        }
		mCalling.setVisibility(View.VISIBLE);
		mConnected.setVisibility(View.GONE);
		btnEndCall.setVisibility(View.GONE);
		Buddy b = dbHelper.buddyWithUserID(username);
		PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, b, false);
		
		startCallLogging(WowTalkVoipIF.IO_IN, username, displayName);
		
		if (WowTalkVoipIF.mCallState != CallState.IncomeCall_Init) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					instance = null;
					finish();
				}
				
			}, 2000);
			return;
		}

        mUiInitialized  = true;
	}
	
	private void displayCallOutgoingViewForUser(String username, String displayName, boolean initWithVideo) {
		Log.e("CallProcess : displayCallOutgoingViewForUser:[username=" + username + ", displayName=" + displayName + "]");
		txtCallDirection.setVisibility(View.VISIBLE);
		callTimer.setVisibility(View.GONE);
        if (initWithVideo) {
            txtCallDirection.setText(getString(R.string.apns_video)+" "+getString(R.string.call_calling));
        } else {
            txtCallDirection.setText(R.string.call_calling);
        }
		mCalling.setVisibility(View.GONE);
		mConnected.setVisibility(View.GONE);
		btnEndCall.setVisibility(View.VISIBLE);
		Buddy b = dbHelper.buddyWithUserID(username);
		PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, b, false);
//		WowTalkVoipIF.mCallState = CallState.OutgoCall_Init;
		WowTalkVoipIF.mCalleeName = mStrContact;
		
		startCallLogging(WowTalkVoipIF.IO_OUT, username, displayName);
		
		// #displayCallRingingWhenCalleeOffline会修改WowTalkVoipIF.mCallState值，
		// 且和此方法不是同步的，所以不能保证这个值没有被修改过
//		if (WowTalkVoipIF.mCallState != CallState.OutgoCall_Init) {
//			mHandler.postDelayed(new Runnable() {
//
//				@Override
//				public void run() {
//					instance = null;
//					finish();
//				}
//
//			}, 2000);
//			return;
//		}

        mUiInitialized  = true;
	}
	
	public void startCall(final String username, final String userDisplayName, final String direction, final boolean initWithVideo) {
		Log.e("CallProcess : IncallActivity startCall! direction = " + direction);
        mInitWithVideo = initWithVideo;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mStrContact = username;
				mStrContactDisplayName = userDisplayName;
//				if (mStrContactDisplayName != null && !String.valueOf("").equals(mStrContactDisplayName)) {
//					txtCallName.setText(mStrContactDisplayName);
//				} else {
//					txtCallName.setText("unknown");
//				}
                fixDisplayNameOfContact(username, userDisplayName, txtCallName);
				mCalling.setVisibility(View.VISIBLE);
				mConnected.setVisibility(View.GONE);
				btnEndCall.setVisibility(View.GONE);
				Log.e("startCall : direction = " + direction);
				if (direction.equals(WowTalkVoipIF.IO_IN)) {
					displayCallIncomingViewForUser(mStrContact, mStrContactDisplayName, initWithVideo);
					startRingIn();
				} else if (direction.equals(WowTalkVoipIF.IO_OUT)) {
					displayCallOutgoingViewForUser(mStrContact, mStrContactDisplayName, initWithVideo);
					startRingOut();
				} else if (direction.equals(WowTalkVoipIF.IO_TALKING)) {
					mCalling.setVisibility(View.GONE);
					mConnected.setVisibility(View.GONE);
					btnEndCall.setVisibility(View.VISIBLE);
					txtCallDirection.setVisibility(View.GONE);
					callTimer.setVisibility(View.VISIBLE);
				}
			}
			
		});
	}

    private void fixDisplayNameOfContact(final String uid, final String username, final TextView txtContact) {
        if (username == null || username.equals("") || username.equals("0")) {
            Buddy b = dbHelper.buddyWithUserID(uid);
            if (b == null || b.nickName.equals("")) {
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... params) {
						return WowTalkWebServerIF.getInstance(IncallActivity.this)
								.fGetBuddyWithUID(uid);
					}

					@Override
					protected void onPostExecute(Integer result) {
						if (result == ErrorCode.OK) {
							Buddy buddy = dbHelper.buddyWithUserID(uid);
							if (buddy != null && !buddy.nickName.equals("")) {
								mStrContactDisplayName = TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias;
								txtContact.setText(mStrContactDisplayName);
							} else {
								txtContact.setText("unknown");
							}
						}
					}
				});
            } else {
                mStrContactDisplayName = TextUtils.isEmpty(b.alias) ? b.nickName : b.alias;
                txtContact.setText(mStrContactDisplayName);
            }
        } else {
            txtContact.setText(username);
        }
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (WowTalkVoipIF.mCallState != CallState.Call_Connected) {
				btnEndCall.performClick();
//				startActivity(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e("IncallActivity onCreate!");
		super.onCreate(savedInstanceState);
        isOnStackTop = true;
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		instance = this;
		setContentView(R.layout.call_view);
		dbHelper = new Database(IncallActivity.this);
		imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
		txtCallDirection = (TextView) findViewById(R.id.call_direction_text);
		callTimer = (IncallTimer) findViewById(R.id.call_timer);
		txtCallName = (TextView) findViewById(R.id.call_name_text);
		btnHold = (PauseResumeButton) findViewById(R.id.btn_hold);
		btnAddVideo = (AddVideoButton) findViewById(R.id.btn_addvideo);
		btnAddVideo.setEnabled(Version.isVideoCapable());
		btnMute = (MuteMicButton) findViewById(R.id.btn_mute);
		btnSpeaker = (SpeakerButton) findViewById(R.id.btn_speaker);
		btnEndCall = (HangCallButton) findViewById(R.id.btn_endcall);
		btnEndCall.setExternalClickListener(this);
		btnDecline = (HangCallButton) findViewById(R.id.btn_decline);
		btnDecline.setExternalClickListener(this);
		btnAnswer = (CallButton) findViewById(R.id.btn_answer);
		btnAnswer.setExternalClickListener(this);
		mCalling = (LinearLayout) findViewById(R.id.layout_calling);
		mConnected = (LinearLayout) findViewById(R.id.layout_connected);
		
		
		WowTalkVoipIF.fSetCallProcessDelegate(instance);

        // 偶尔会发生这种情况：Activity运行了，界面内容（头像、按钮等）没有初始化，不响应按钮事件，也无法通过后退硬键返回，
        // 以致整个APP完全停滞在这个界面，强制退出程序后才能恢复。
        // 为了缓解这个后果，这里加入一个超时保护，检测到UI没有正确初始化后，自动结束Activity。
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mUiInitialized) {
                    Log.e("IncallActivity#onCreate, finish the activity because initing ui failure.");
                    finish();
                }
            }
        }, 2000);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		WowTalkVoipIF.fStartProximitySensorForActivity(this);
		bIsStartingVideo = false;

        //tmp show call info,actual info with be shown when callback is called(startCall)
        String tmpUid = getIntent().getStringExtra(CallMainActivity.EXTRA_OUTGOING_CALL_TARGET_ID);
        String tmpDName = getIntent().getStringExtra(CallMainActivity.EXTRA_OUTGOING_CALL_TARGET_DISPLAYNAME);
        fixDisplayNameOfContact(tmpUid, tmpDName, txtCallName);
        Buddy b = dbHelper.buddyWithUserID(tmpUid);
        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, b, false);
        if(!mUiInitialized) {
            mCalling.setVisibility(View.GONE);
            mConnected.setVisibility(View.GONE);
            btnEndCall.setVisibility(View.GONE);
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		WowTalkVoipIF.fStopProximitySensorForActivity(this);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    isOnStackTop = false;
	}

    private void getBuddyDetailByUserId(final String userId, final ImageView imageView, final TextView txtName) {
        final Buddy buddy = dbHelper.buddyWithUserID(userId);
        if (buddy != null) {
            PhotoDisplayHelper.displayPhoto(IncallActivity.this, imageView, R.drawable.default_avatar_90, buddy, false);
            txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
        }
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
			int errno = ErrorCode.OK;

			@Override
			protected Void doInBackground(Void... params) {
				errno = WowTalkWebServerIF.getInstance(IncallActivity.this).fGetBuddyWithUID(userId);
				return null;  //To change body of implemented methods use File | Settings | File Templates.
			}

			@Override
			protected void onPostExecute(Void result) {
				if (errno == ErrorCode.OK) {
					Buddy buddy = dbHelper.buddyWithUserID(userId);
					PhotoDisplayHelper.displayPhoto(IncallActivity.this, imageView, R.drawable.default_avatar_90, buddy, false);
					txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
				}
			}
		});

    }

    private Timer mVibrateTimer = new Timer();

    /**
     * 接收方响铃，调用系统的手机铃声
     */
    private void startRingIn() {

        PrefUtil prefUtil = PrefUtil.getInstance(this);
        if(!prefUtil.isSysNoticeEnabled() || !prefUtil.isSysNoticeTelephoneOn()) {
            return;
        }

        if(prefUtil.isSysNoticeMusicOn()) {
            try {
                if (mRingerInPlayer == null) {
                    mRingerInPlayer = new MediaPlayer();
                    mRingerInPlayer.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                    mRingerInPlayer.prepare();
                    mRingerInPlayer.setLooping(true);
                }
                mRingerInPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(prefUtil.isSysNoticeVibrateOn()) {
            mVibrateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Utils.triggerVibrate(IncallActivity.this,1000);
                }
            },2000,2000);
        }
    }

    /**
     * 停止接收方响铃
     */
    private void stopRingIn() {
        try {
            if (mRingerInPlayer != null) {
                mRingerInPlayer.stop();
                mRingerInPlayer.release();
                mRingerInPlayer = null;
            }

            if(null != mVibrateTimer) {
                mVibrateTimer.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拨出方响铃
     */
    private void startRingOut() {
        try {
            if (mRingerOutPlayer == null) {
                mRingerOutPlayer = new MediaPlayer();
                AssetFileDescriptor afd = getAssets().openFd("ringback.mp3");
                try {
                    mRingerOutPlayer.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(), afd.getLength());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mRingerOutPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                mRingerOutPlayer.prepare();
                mRingerOutPlayer.setLooping(true);
            }
            mRingerOutPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止拨出方响铃
     */
    private void stopRingOut() {
        try {
            if (mRingerOutPlayer != null) {
                mRingerOutPlayer.stop();
                mRingerOutPlayer.release();
                mRingerOutPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
