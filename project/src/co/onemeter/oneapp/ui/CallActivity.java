package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.wowtalk.api.CallLog;
import org.wowtalk.api.CallState;
import org.wowtalk.api.Database;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.component.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.util.ContactUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CallActivity extends Activity implements OnClickListener, org.wowtalk.api.WowTalkUICallProcessDelegate {
	private static CallActivity instance;
	private static final int INTENT_VIDEO_ACTIVITY = 100;
	private final int BUTTON_UNABLED_ALPHA = 150;
	public static final String CONTACT_UID = "contact_uid";
	public static final String CONTACT_DISPLAYNAME = "contact_displayname";
	
	private LinearLayout layoutIcons;
	private LinearLayout layoutCalling;
	private ImageView imgThumbnail;
	private TextView txtCallDirection;
	private IncallTimer callTimer;
	private TextView txtName;
	
	private PauseResumeButton btnHold;
	private AddVideoButton btnAddVideo;
	private MuteMicButton btnMute;
	private SpeakerButton btnSpeaker;
	
	private CallButton btnAnswer;
	private HangCallButton btnDecline;
	private HangCallButton btnEndCall;
	
	private String mStrContact;
	private String mStrDisplayName;
	
	private int mDuration;
	private CallLog callLog;
	private Database dbHelper;
	private TimerTask mCallProcessTimer;
	private TimerTask mEndCallTimer;
	private MediaPlayer mRingbackPlayer;
	private boolean isResetingCall;
	private Timer timer = new Timer();
	private Handler handler = new Handler();
	
	public static final CallActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}
	
	public void showDisplayMsg(int resid) {
		
	}
	
	public void showDisplayMsg(String msg) {
		
	}
	
	public void startCall(final String userID, final String userDisplayName, final String direction) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				mStrContact = userID;
				mStrDisplayName = userDisplayName;
				if (mStrDisplayName != null && !mStrDisplayName.equals("")) {
					ArrayList<Object> arrUserInfo = ContactUtil.fRecordArrayByPhoneNumber(CallActivity.this, mStrDisplayName);
					if (arrUserInfo != null) {
						txtName.setText((String) arrUserInfo.get(0));
						Bitmap bmpThumbnail = (Bitmap) arrUserInfo.get(3);
						if (bmpThumbnail != null) {
							imgThumbnail.setImageBitmap(bmpThumbnail);
						} else {
							imgThumbnail.setImageResource(R.drawable.default_avatar_90);
						}
					} else {
						txtName.setText(mStrDisplayName);
					}
				} else {
					txtName.setText(mStrContact);
				}
				
				layoutCalling.setVisibility(View.GONE);
				btnEndCall.setVisibility(View.VISIBLE);
				layoutIcons.setVisibility(View.GONE);
				
				if (direction.equals(WowTalkVoipIF.IO_IN)) {
					
				}
				if (direction.equals(WowTalkVoipIF.IO_OUT)) {
					
				}
				if (direction.equals(WowTalkVoipIF.IO_TALKING)) {
					
				}
 			}
			
		});
	}
	
	private void fStartCallLogging(String userName, String displayName, String direction) {
		if (callLog != null) {
			callLog = null;
		}
		callLog = new CallLog();
		callLog.startDate = Database.callLog_dateToUTCString(new Date());
		callLog.contact = userName;
		callLog.displayName = displayName;
		callLog.direction = direction;
	}
	
	private void fStopEndCallTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	private void fDisplayCallIncomingViewForUser(String userName, String displayName) {
		fStopEndCallTimer();
		
		WowTalkVoipIF.mCalleeName = userName;
		mDuration = 0;
		Log.e("CallProcess:fDisplayCallIncomingViewForUser:" + userName + displayName);
		txtCallDirection.setText("incoming...");
		
		btnEndCall.setVisibility(View.GONE);
		layoutCalling.setVisibility(View.VISIBLE);
		layoutIcons.setVisibility(View.GONE);
		
		fStartCallLogging(userName, displayName, WowTalkVoipIF.IO_IN);
		if (WowTalkVoipIF.mCallState != CallState.IncomeCall_Init) {
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					instance = null;
					finish();
				}
				
			}, 2000);
			return;
		}
	}
	
	private void fDisplayCallOutgoingViewForUser(String userName, String displayName) {
		txtCallDirection.setText("outgoing...");
		btnEndCall.setVisibility(View.VISIBLE);
		layoutCalling.setVisibility(View.GONE);
		layoutIcons.setVisibility(View.GONE);
		
		WowTalkVoipIF.mCalleeName = mStrContact;
		WowTalkVoipIF.mCallState = CallState.OutgoCall_Init;
		fStartCallLogging(userName, displayName, WowTalkVoipIF.IO_OUT);
		
		if (WowTalkVoipIF.mCallState != CallState.OutgoCall_Init) {
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					instance = null;
					finish();
				}
				
			}, 2000);
			return;
		}
	}
	
	private void fEndupCallByTimer() {
		if (WowTalkVoipIF.mCallState == CallState.OutgoCall_CalleeOffline) {
			endupCallProcess();
			return;
		}
		WowTalkVoipIF.fTerminateCall(false);
	}
	
	private void fFinalizeCallLogging(int duration) {
		callLog.duration = duration;
		callLog.quality = 5;
		switch (WowTalkVoipIF.mCallState) {
		case 0:
			
			break;
		case 1://CallState.OutgoCall_CalleeBusy
		case 2://CallState.OutgoCall_CallNoAnswer
		case 3://CallState.OutgoCall_CalleeOffline
			callLog.status = CallLog.WowTalkCallSuccess;
			WowTalkVoipIF.getInstance(this).fNotifyCalleeForMissedCall(callLog);
			break;
		case 4://CallState.OutgoCall_Init
		case 5://CallState.OutgoCall_CalleeRinging
		case 6://CallState.Call_Connected
			callLog.status = CallLog.WowTalkCallSuccess;
			break;
		case 7://CallState.OutgoCall_CalleeDiclined
		case 8://CallState.IncomeCall_WaitForCallAgain
		case 9://CallState.IncomeCall_Init
			callLog.status = CallLog.WowTalkCallMissed;
			break;
		default:
			break;
		}
		
		dbHelper = new Database(CallActivity.this);
		dbHelper.storeNewCallLog(callLog);
		if (callLog.status == CallLog.WowTalkCallMissed) {
			if (StartActivity.isInstanciated()) {
				// do somthing
			} else {
				Intent intent = new Intent();
				intent.putExtras(callLog.toBundle());
				sendBroadcast(intent);
			}
		}
	}
	
	private void startRingBack() {
		try {
			if (mRingbackPlayer == null) {
				mRingbackPlayer = new MediaPlayer();
				AssetFileDescriptor afd = getAssets().openFd("ringback.mp3");
				try {
					mRingbackPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				} catch (IOException e) {
					e.printStackTrace();
				}
				mRingbackPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
				mRingbackPlayer.prepare();
				mRingbackPlayer.setLooping(true);
			}
			mRingbackPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopRingBack() {
		try {
			if (mRingbackPlayer != null) {
				mRingbackPlayer.stop();
				mRingbackPlayer.release();
				mRingbackPlayer = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_view);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
		
		imgThumbnail = (ImageView) findViewById(R.id.img_thumbnail);
		txtName = (TextView) findViewById(R.id.call_name_text);
		txtCallDirection = (TextView) findViewById(R.id.call_direction_text);
		callTimer = (IncallTimer) findViewById(R.id.call_timer);
		btnEndCall = (HangCallButton) findViewById(R.id.btn_endcall);
		btnDecline = (HangCallButton) findViewById(R.id.btn_decline);
		btnAnswer = (CallButton) findViewById(R.id.btn_answer);
		
		btnHold = (PauseResumeButton) findViewById(R.id.btn_hold);
		btnAddVideo = (AddVideoButton) findViewById(R.id.btn_addvideo);
		btnMute = (MuteMicButton) findViewById(R.id.btn_mute);
		btnSpeaker = (SpeakerButton) findViewById(R.id.btn_speaker);
	}
	
	

	@Override
	public void displayCallConnectedForUser(String arg0, String arg1) {
		// TODO Auto-generated method stub
		stopRingBack();
		fEndupCallByTimer();
		
		layoutIcons.setVisibility(View.VISIBLE);
		btnEndCall.setVisibility(View.VISIBLE);
		layoutCalling.setVisibility(View.GONE);
		callTimer.setVisibility(View.VISIBLE);
		txtCallDirection.setVisibility(View.GONE);
		
		btnAddVideo.mCalleeName = WowTalkVoipIF.mCalleeName;
		btnAddVideo.reset();
		WowTalkVoipIF.mCallState = CallState.Call_Connected;
		isResetingCall = false;
		mDuration = 0;
		
		mCallProcessTimer = new TimerTask() {
			
			@Override
			public void run() {
				if (!WowTalkVoipIF.fIsIncall())
					return;
				mDuration = WowTalkVoipIF.fGetCurrentCallDuration();
				if (mDuration == 0)
					return;
				timer.schedule(mCallProcessTimer, 0, 1000);
			}
		};
	}

	@Override
	public void displayCallFailed(String username, String displayName, String error) {
		// TODO Auto-generated method stub
		final String sreError = error;
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
			}
			
		});
	}

	@Override
	public void displayCallRingingWhenCalleeOffline(String username, String displayName) {
		// TODO Auto-generated method stub
		if (WowTalkVoipIF.mCallState != CallState.OutgoCall_Init
				|| !username.equals(WowTalkVoipIF.mCalleeName)) {
			return;
		}
		
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeOffline;
		mEndCallTimer = new TimerTask() {
			
			@Override
			public void run() {
				fEndupCallByTimer();
			}
		};
		timer.schedule(mEndCallTimer, 60000, 1000);
	}

	@Override
	public void displayCallTimeout(String arg0, String arg1) {
		// TODO Auto-generated method stub
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CallNoAnswer;
		
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
			}
			
		});
	}

	@Override
	public void displayCalleeBusy(String username, String displayName) {
		// TODO Auto-generated method stub
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeBusy;
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				WowTalkVoipIF.fTerminateCall(true);
			}
			
		});
	}

	@Override
	public void displayCalleeRinging(String username, String displayName) {
		// TODO Auto-generated method stub
		startRingBack();
		WowTalkVoipIF.mCallState = CallState.OutgoCall_CalleeRinging;
		
	}

	@Override
	public void displayMakeRegularCall(String arg0, String arg1) {
		// TODO Auto-generated method stub
		WowTalkVoipIF.mCallState = CallState.CallEnd;
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				finish();
			}
			
		});
		
	}

	@Override
	public void displayPausedByCallee(String arg0, String arg1) {
		// TODO Auto-generated method stub
		handler.post(new Runnable() {

			@Override
			public void run() {
				btnHold.setEnabled(false);
				btnHold.setCheckedWithoutAction(true);
				btnAddVideo.setEnabled(false);
			}
			
		});
	}

	@Override
	public void displayPausedByMe() {
		// TODO Auto-generated method stub
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				btnAddVideo.setEnabled(false);
			}
			
		});
	}

	@Override
	public void displayResume() {
		// TODO Auto-generated method stub
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				btnHold.setEnabled(true);
				btnHold.setCheckedWithoutAction(false);
				btnAddVideo.setEnabled(true);
			}
			
		});
	}

	@Override
	public void displayVideoCallRejected(String username, String displayName) {
		// TODO Auto-generated method stub
		if (username == null)
			return;
		if (!username.equals(WowTalkVoipIF.mCalleeName))
			return;
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				btnAddVideo.setOff();
				
			}
			
		});
	}

	@Override
	public void displayVideoRequest(String username, String displayName, boolean addVideo) {
		// TODO Auto-generated method stub
		if (username == null)
			return;
		if (!username.equals(WowTalkVoipIF.mCalleeName))
			return;
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	@Override
	public void endupCallProcess() {
		// TODO Auto-generated method stub
		if (WowTalkVoipIF.mCallState == CallState.CallEnd)
			return;
		if (isResetingCall) {
			isResetingCall = false;
			return;
		}
		stopRingBack();
		fStopEndCallTimer();
		fFinalizeCallLogging(mDuration);
		
		if (mCallProcessTimer != null) {
			mCallProcessTimer.cancel();
			mCallProcessTimer = null;
		}
		WowTalkVoipIF.mCallState = CallState.CallEnd;
		
		
	}

	@Override
	public void finishVideoActivity() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getCalleeBackOnlineMessage(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startVideoActivity() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
