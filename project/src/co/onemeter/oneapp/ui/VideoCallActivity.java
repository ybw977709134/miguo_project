package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.CallManager;
import org.wowtalk.WowTalkManager;
import org.wowtalk.WowTalkOnCallStateChangedListener;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.core.WowTalkCall;
import org.wowtalk.core.WowTalkCallState;
import org.wowtalk.ui.component.HangCallButton;
import org.wowtalk.ui.component.IncallTimer;
import org.wowtalk.video.CameraConfiguration;
import org.wowtalk.video.CameraConfiguration.AndroidCamera;
import org.wowtalk.video.VideoListener;
import org.wowtalk.video.VideoWindowImpl;
import co.onemeter.oneapp.R;

public class VideoCallActivity extends Activity implements WowTalkOnCallStateChangedListener {
	private SurfaceView mVideoViewReady;
	private SurfaceView mVideoCaptureViewReady;
	private WowTalkCall videoCall;
	private WakeLock wakeLock;
	private IncallTimer txtCallStatus;
	private ImageButton btnSwitch;
	private HangCallButton btnEnd;
	
	private VideoWindowImpl mVideoWindowImpl;
	private Runnable mCallQualityUpdater;
	
	private Handler refreshHandler = new Handler();
	
	private void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
	}
	
	private void updatePreview(boolean captureCameraEnabled) {
		mVideoCaptureViewReady = null;
		if (captureCameraEnabled) {
			findViewById(R.id.img_nowebcam).setVisibility(View.INVISIBLE);
			findViewById(R.id.video_capture_surface).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.video_capture_surface).setVisibility(View.INVISIBLE);
			findViewById(R.id.img_nowebcam).setVisibility(View.VISIBLE);
		}
		findViewById(R.id.video_layout).requestLayout();

        RelativeLayout selfWindowLayout = (RelativeLayout) findViewById(R.id.video_call_self_window_layout);
        ViewGroup.LayoutParams layoutParams = selfWindowLayout.getLayoutParams();
        if (GlobalValue.screenH > 0) {
            float screenFactor = 1.0f * GlobalValue.screenW / GlobalValue.screenH;
            float layoutHeight = getResources().getDimension(R.dimen.video_call_self_window_height);
            float layoutWidth = screenFactor * layoutHeight;
            layoutParams.width = (int) layoutWidth;
            selfWindowLayout.setLayoutParams(layoutParams);
        }
    }
	
	private void updateQualityOfSignalIcon(float quality) {
		ImageView qos = (ImageView) findViewById(R.id.QoS);
		if (quality < 2) {
			
		} else if (quality < 3) {
			
		} else {
			
		}
	}
	
	@Override
	public void onCallStateChanged(WowTalkCall call, WowTalkCallState state,
			String message) {
		if (call == videoCall && state == WowTalkCallState.CallEnd) {
			WowTalkVoipIF.fExitCallMode();
			WowTalkVoipIF.fSetVideoCallLaunched(false);
			finish();
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videocall);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        SurfaceView videoView = (SurfaceView) findViewById(R.id.video_surface);
		txtCallStatus = (IncallTimer) findViewById(R.id.call_timer);
		
		btnEnd = (HangCallButton) findViewById(R.id.btn_end);
		btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
		btnSwitch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				WowTalkVoipIF.fVideoCall_SwitchCaptureCamera();
				Log.i("VideoCallActivity#onCreate, switch to camera(id is " + WowTalkManager.getLc().getVideoDevice());
				if (mVideoCaptureViewReady != null) {
					WowTalkVoipIF.fVideoCall_SetCapturePreviewVideoWindow(mVideoCaptureViewReady);
				}
			}
		});

        Log.i("VideoCallActivity#onCreate, init camera id is " + WowTalkManager.getLc().getVideoDevice());
        // switch to front camera default.
        if (CameraConfiguration.hasFrontCamera()) {
            AndroidCamera[] camerars = CameraConfiguration.retrieveCameras();
            for (AndroidCamera androidCamera : camerars) {
                if (androidCamera.frontFacing){
                    WowTalkManager.getLc().setVideoDevice(androidCamera.id);
                    CallManager.getInstance().updateCall();
                    Log.i("VideoCallActivity#onCreate, switch to front camera.");
                    break;
                }
            }
        } else {
            // background camera
            WowTalkManager.getLc().setVideoDevice(CameraInfo.CAMERA_FACING_BACK);
            CallManager.getInstance().updateCall();
            Log.i("VideoCallActivity#onCreate, switch to background camera.");
        }

		SurfaceView captureView = (SurfaceView) findViewById(R.id.video_capture_surface);
		captureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		if (org.wowtalk.video.Version.sdkAboveOrEqual(5)) {
			fixZOrder(videoView, captureView);
		}
		
		mVideoWindowImpl = new VideoWindowImpl(videoView, captureView);
		mVideoWindowImpl.setListener(new VideoListener() {
			
			@Override
			public void onVideoRenderingSurfaceReady(VideoWindowImpl vw,
					SurfaceView surface) {
				// TODO Auto-generated method stub
				WowTalkVoipIF.fVideoCall_SetVideoWindow(vw);
				mVideoViewReady = surface;
			}
			
			@Override
			public void onVideoRenderingSurfaceDestroyed(VideoWindowImpl vw) {
				// TODO Auto-generated method stub
				WowTalkVoipIF.fVideoCall_SetVideoWindow(null);
			}
			
			@Override
			public void onVideoPreviewSurfaceReady(VideoWindowImpl vw,
					SurfaceView surface) {
				// TODO Auto-generated method stub
				mVideoCaptureViewReady = surface;
				WowTalkVoipIF.fVideoCall_SetCapturePreviewVideoWindow(mVideoCaptureViewReady);
			}
			
			@Override
			public void onVideoPreviewSurfaceDestroyed(VideoWindowImpl arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		mVideoWindowImpl.init();
		
		WowTalkVoipIF.fVideoCall_AutoAdjustVideoRotation(this);
		videoCall = WowTalkVoipIF.fGetCurrentCall();
		
		if (videoCall != null) {
			updatePreview(WowTalkVoipIF.fIsCurrentCallVideoCaptureEnable());
		}
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, Log.TAG);
		wakeLock.acquire();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);
		if (mVideoViewReady != null) {
			((GLSurfaceView) mVideoViewReady).onResume();
		}
		WowTalkVoipIF.fAddCallStateChangeListenerToVideoActivity(this);
		WowTalkVoipIF.fSetVideoCallLaunched(true);
		
		refreshHandler.postDelayed(mCallQualityUpdater = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (!WowTalkVoipIF.fIsIncall()) {
					mCallQualityUpdater = null;
					return;
				}
				int oldQuality = 0;
				float newQuality = WowTalkVoipIF.fGetCurrentCallQuality();
				if ((int) newQuality != oldQuality) {
					updateQualityOfSignalIcon(newQuality);
				}
				int mDuration = WowTalkVoipIF.fGetCurrentCallDuration();
				if (mDuration != 0) {
					txtCallStatus.setText(String.valueOf(mDuration));
				}
				if (WowTalkVoipIF.fIsVideoCallLaunched()) {
					refreshHandler.postDelayed(this, 1000);
				} else {
					mCallQualityUpdater = null;
				}
			}
			
		}, 1000);
	}
	
	protected void onDestroy() {
		mVideoWindowImpl.release();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
		super.onDestroy();
	}
	
	protected void onPause() {
		super.onPause();
        MobclickAgent.onPause(this);
		btnEnd.onClick(null);
	}
	
}
