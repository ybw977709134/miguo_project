package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;

import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;

import java.util.Timer;
import java.util.TimerTask;

public class CallMainActivity extends Activity implements SensorEventListener, WowTalkUIMakeCallDelegate{
	public static final String EXTRA_OUTGOING_CALL_TARGET_ID = "a";
	public static final String EXTRA_OUTGOING_CALL_TARGET_DISPLAYNAME= "b";
    public static final String EXTRA_OUTGOING_CALL_INIT_WITH_VIDEO = "c";

	public static final int REQ_INCALL_ACTIVITY = 1000;
	public static final int REQ_VEDIO_ACTIVITY = 1001;
	
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private WakeLock mWakeLock;
	private String mUserID;
	private String mUserDisplayName;
    private boolean mInitWithVideo;
	private Timer mStartTimer;
	private static CallMainActivity instance;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			finishActivity(REQ_INCALL_ACTIVITY);
			finish();
		};
	};
	
	public static final void startNewOutGoingCall(final Context context, final String _targetID, final String _targetDisplayName, boolean initWithVideo) {
		doStartNewOutGoingCall(context, _targetID, _targetDisplayName, initWithVideo);
	}
	
	static void doStartNewOutGoingCall(Context context, String _targetID, String _targetDisplayName, boolean initWithVideo) {
		Log.e("CallMainActivity : displayName = " + _targetDisplayName);
		boolean connected;
		try {
			connected = Utils.isNetworkConnected(context) && WowTalkVoipIF.fIsNetworkReachable();
		} catch (Exception e) {
			connected = false;
		}
		Log.e("startNewOutGoingCall net connected : " + connected);
		if (connected) {
			Intent intent = new Intent(context, CallMainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra(EXTRA_OUTGOING_CALL_TARGET_ID, _targetID);
			intent.putExtra(EXTRA_OUTGOING_CALL_TARGET_DISPLAYNAME, _targetDisplayName);
            intent.putExtra(EXTRA_OUTGOING_CALL_INIT_WITH_VIDEO, initWithVideo);
			context.startActivity(intent);
		} else {
            if(!Connect2.confirmSend2server()) {
                //...no handle now
            }
		}
	}

    private String fixCallOutDiaplayName(String targetUID) {
        String displayName = "";
        Buddy buddy = new Database(CallMainActivity.this).buddyWithUserID(targetUID);
        if (null != buddy) {
            displayName = TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias;
        }
        return displayName;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
        Connect2.setContext(this);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "YuanQu");
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        WowTalkVoipIF.fSetCallProcessDelegate(null);

		mStartTimer = new Timer();
		mStartTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if (WowTalkVoipIF.fGetCallProcessDelegate() != null) {
					WowTalkVoipIF.fSetMakeCallDelegate(CallMainActivity.this);
					WowTalkVoipIF.getInstance(CallMainActivity.this).fEnterIncallModeIfCallPending();
					
					processIntent();
                    if(null != mStartTimer) {
                        mStartTimer.cancel();
                        mStartTimer = null;
                    }
				}
			}
		}, 300, 300);

        mUserID = getIntent().getStringExtra(EXTRA_OUTGOING_CALL_TARGET_ID);
        mUserDisplayName = getIntent().getStringExtra(EXTRA_OUTGOING_CALL_TARGET_DISPLAYNAME);
        mInitWithVideo = getIntent().getBooleanExtra(EXTRA_OUTGOING_CALL_INIT_WITH_VIDEO, false);

		Intent intent = new Intent(this, IncallActivity.class);
        intent.putExtra(EXTRA_OUTGOING_CALL_TARGET_ID, mUserID);
        intent.putExtra(EXTRA_OUTGOING_CALL_TARGET_DISPLAYNAME, mUserDisplayName);

		startActivityForResult(intent, REQ_INCALL_ACTIVITY);
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
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.e("CallMainActivity : onNewIntent!");
		super.onNewIntent(intent);
		
		if (intent.getData() == null) {
			finishActivity(REQ_INCALL_ACTIVITY);
			finish();
			Intent mIntent = new Intent(this, CallMainActivity.class);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(mIntent);
			return;
		}
		processIntent();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(RESULT_OK, data);
		finish();
	}
	
	
	private void processIntent() {
		Log.e("CallMainActivity : processIntent!");
		Intent intent = getIntent();
		if (!intent.hasExtra(EXTRA_OUTGOING_CALL_TARGET_ID)) {
			return;
		}
        Log.e("CallMainActivity : processIntent! mInitWithVideo = " + mInitWithVideo);

		WowTalkVoipIF.fNewOutgoingCall(mUserID, mUserDisplayName, mInitWithVideo);
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			mHandler.sendEmptyMessageDelayed(1, 2000L);
		} else {
			mHandler.removeMessages(1);
		}
	}
	
	private void startIncallActivity(final String _userID, final String _userDisplayName, String _direction, boolean initWithVideo) {
		Log.e("CallMainActivity : startIncallActivity! + direction = " + _direction);
//		String displayName;
//		if (_userDisplayName == null || _userDisplayName.equals("") || _userDisplayName.equals("0")) {
//			displayName = fixCallDiaplayName(_userID);
//		} else {
//			displayName = _userDisplayName;
//		}
		IncallActivity activity = (IncallActivity) WowTalkVoipIF.fGetCallProcessDelegate();
        if(null != activity) {
            mInitWithVideo = initWithVideo;
            activity.startCall(_userID, _userDisplayName, _direction, mInitWithVideo);
        }
	}
	
	public synchronized void startOrientationSensor() {
		if (mSensorManager != null) {
			mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		onSensorChanged(null);
	}
	
	public synchronized void stopOrientationSensor() {
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this, mSensor);
		}
	}
	
	protected void onDestroy() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
		if (mStartTimer != null) {
			mStartTimer.cancel();
			mStartTimer = null;
		}
//        if (MessageComposerActivityBase.instance() != null) {
//            MessageComposerActivityBase.instance().regetMsgList();
//        }
		super.onDestroy();
	}
	

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event == null || event.sensor == mSensor) {
			WowTalkVoipIF.fVideoCall_AutoAdjustVideoRotation(this);
		}
	}

	@Override
	public void displayIncomingCallForUser(String callerName, String displayName, boolean b) {
		Log.e("CallMainActivity : displayIncomingCallForUser:[username=" + callerName + ", displayName=" + displayName+", withVideo="+b + "]");
		WowTalkVoipIF.mCallState = CallState.IncomeCall_Init;
		startIncallActivity(callerName, displayName, WowTalkVoipIF.IO_IN, b);
	}

	@Override
	public void displayOutgoingCallForUser(String callerName, String displayName, boolean b) {
        // 显示备注名称
        String displayNameAlias = fixCallOutDiaplayName(callerName);
        displayName = TextUtils.isEmpty(displayNameAlias) ? displayName : displayNameAlias;
		Log.e("CallMainActivity : displayOutgoingCallForUser:[username=" + callerName + ", displayName=" + displayName +", withVideo="+b+ "]");
		WowTalkVoipIF.mCallState = CallState.OutgoCall_Init;
		startIncallActivity(callerName, displayName, WowTalkVoipIF.IO_OUT, b);
	}

	@Override
	public void displayTalkingCallForUser(String callerName, String displayName, boolean b) {
        Log.e("CallMainActivity : displayTalkingCallForUser:[username=" + callerName + ", displayName=" + displayName +", withVideo="+b+ "]");
		startIncallActivity(callerName, displayName, WowTalkVoipIF.IO_TALKING, b);
	}

    @Override
	public void exitCallMode() {
		WowTalkVoipIF.fExitCallMode();
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
	}

}
