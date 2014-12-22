package co.onemeter.oneapp.liveplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.*;
import de.greenrobot.event.EventBus;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自定义视频播放界面
 * 
 * @author liyou
 * 
 */
public class VideoPlayingActivity extends Activity {

	class EventHandler extends Handler {
		public EventHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_PLAY:

				//如果已经播放了，等待上一次播放结束
				if (mPlayerStatus != PLAYER_STATUS.PLAYER_IDLE) {
					SystemClock.sleep(2000);
				}

				try {
					mLock.tryLock(2, TimeUnit.SECONDS);
					// 设置播放url
					mVV.setVideoPath(mVideoSource);

					//续播，如果需要如此
					if (mLastPos > 0) {
						mVV.seekTo(mLastPos);
						mLastPos = 0;
					}

					mVV.start();//开始播放

					mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARING;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mLock.unlock();
				}
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 播放状态
	 */
	private enum PLAYER_STATUS {
		PLAYER_IDLE, PLAYER_PREPARED, PLAYER_PREPARING,
	}

	static {
		try {
			System.loadLibrary("audioels");
			System.loadLibrary("ffmpeg");
			System.loadLibrary("liveplayer");
			System.loadLibrary("livesender");
		} catch (Exception e) {
			L.e("load library failed");
		}
	}

	protected static final int NOTIFY_ERROR = 102;

	private static final String POWER_LOCK = "VideoPlayingActivity";

	private final int UI_EVENT_UPDATE_CURRPOSITION = 1;

	private final int EVENT_PLAY = 0;
	private volatile PLAYER_STATUS mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;

	private String description;//摄像头名称
	private boolean barShow = true;

	//顶部控件相关
	private RelativeLayout mController = null;//顶部控制条

	private ImageButton mBack = null;//返回按钮
	private TextView tv_play_description;//摄像头描述
	private long showTimestamp = 0;//直接切到点播时，需要显示的时间戳

	//播放相关
	private BVideoView mVV = null;

	private String mVideoSource = null;
	private long mTouchTime;
	private int mLastPos = 0;//记录当前播放位置
	private EventHandler mEventHandler;
	private HandlerThread mHandlerThread;
	private boolean mIsHwDecode = false;

	/**
	 * ProgressBar 刷新布局
	 */
	private LinearLayout ll_pb_refresh;

	private Lock mLock;

	Handler mUIHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UI_EVENT_UPDATE_CURRPOSITION:
				ll_pb_refresh.setVisibility(View.GONE);

				break;
			case NOTIFY_ERROR:
				ll_pb_refresh.setVisibility(View.GONE);
				String info = msg.getData().getString("errorInfo");
				if (!"错误显示".equals(info)) {
					T.showShort(VideoPlayingActivity.this, info);
				}
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 从上一个界面获取参数
	 * 
	 * 必须传入的参数
	 */
	private void getParamFromLastPage(Intent intent) {
		description = intent.getStringExtra("description");
	}

	/**
	 * 获取显示时间戳
	 * @return
	 */
	public long getShowTimestamp() {
		return showTimestamp;
	}

	/**
	 * 初始化BVideoView
	 */
	private void initBVideoView() {
		//设置ak及sk的前16位
		BVideoView.setAKSK("ZIAgdlC7Vw7syTjeKG9zS4QP", "pavlqfU4mzYQ1dH0NG3b7LyXNBy5SYk6".substring(0, 16));

		mVV = (BVideoView) findViewById(R.id.video_view);
		mVV.setVideoScalingMode(BVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT);//设置画面填充模式
		mVV.showCacheInfo(false);

		/**
		 * 在调用 BVideoView.start()后需要等待 OnPreparedListener.onPrepared()来表示 视频准备播放就绪，
		 * 这个过程你可能需要在界面做一些等待信息或提示。
		 */
		mVV.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared() {
				L.e("【【【【【onPrepared】】】】】");
				mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARED;
				mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
			}
		});

		/**
		 * 执行 BVideoView.stopPlayback（）停止视频，此操作是异步操作，
		 * 需要等待OnCompletionListener.onCOmpletion()，
		 * 这表示播放真正结束，才能进行下一次播放
		 */
		mVV.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion() {
				L.e("播放结束后回调函数【【【【【onCompletion】】】】】");

				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						mVV.setBackgroundResource(android.R.color.black);
						ll_pb_refresh.setVisibility(View.GONE);
					}
				});

				mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
			}
		});

		/**
		 * 
		 */
		mVV.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(int what, int extra) {
				L.e("播放期间发生错误的回调函数【【【【【onError】】】】】  --> " + what + "   " + extra);
				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						mVV.setBackgroundResource(android.R.color.black);
					}
				});

				mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);

				String errorInfo = null;
				switch (what) {
				case BVideoView.MEDIA_ERROR_UNKNOWN:
					L.e("BVideoView.MEDIA_ERROR_UNKNOWN");
					errorInfo = "MEDIA_ERROR_UNKNOWN";
					break;
				case BVideoView.MEDIA_ERROR_NO_INPUTFILE:
				case BVideoView.MEDIA_ERROR_INVALID_INPUTFILE:
					L.e("BVideoView.MEDIA_ERROR_INVALID_INPUTFILE");
					errorInfo = "MEDIA_ERROR_INVALID_INPUTFILE";
					break;
				case BVideoView.MEDIA_ERROR_NO_SUPPORTED_CODEC:
					L.e("BVideoView.MEDIA_ERROR_NO_SUPPORTED_CODEC");
					errorInfo = "MEDIA_ERROR_NO_SUPPORTED_CODEC";
					break;
				case BVideoView.MEDIA_ERROR_DISPLAY:
					errorInfo = "乐现君也不知道怎么了，重试下?";
					break;
				default:
					break;
				}
				Message message = Message.obtain();
				Bundle data = message.getData();
				data.putString("errorInfo", errorInfo);
				message.what = NOTIFY_ERROR;
				mUIHandler.sendMessage(message);
				return true;
			}
		});

		/**
		 * 
		 */
		mVV.setOnInfoListener(new OnInfoListener() {
			@Override
			public boolean onInfo(int what, int extra) {
				switch (what) {
				case BVideoView.MEDIA_INFO_BUFFERING_START:
					L.e("##### 开始缓冲 #####");
					break;
				case BVideoView.MEDIA_INFO_BUFFERING_END:
					L.e("##### 结束缓冲 #####");
					break;
				case BVideoView.MEDIA_INFO_UNKNOWN:
					L.e("Unspecified player info");
					break;
				case BVideoView.MEDIA_INFO_VIDEO_TRACK_LAGGING:
					L.e("The video is too complex for the decoder: it can't decode frames fast enough.");
					break;
				case BVideoView.MEDIA_INFO_BAD_INTERLEAVING:
					L.e("Bad interleaving means that a media has been improperly interleaved or not interleaved at all, e.g has all the video samples first then all the audio ones.");
					break;
				case BVideoView.MEDIA_INFO_NOT_SEEKABLE:
					L.e("The media cannot be seeked (e.g live stream).");
					break;
				default:
					break;
				}
				return true;
			}
		});

		/**
		 * 当前缓冲的百分比， 可以配合onInfo中的开始缓冲和结束缓冲来显示百分比到界面
		 */
		mVV.setOnPlayingBufferCacheListener(new OnPlayingBufferCacheListener() {
			@Override
			public void onPlayingBufferCache(final int percent) {
				L.i("缓冲比: " + percent);
			}
		});

		/**
		 * 设置解码模式
		 */
		mVV.setDecodeMode(mIsHwDecode ? BVideoView.DECODE_HW : BVideoView.DECODE_SW);
	}

	/**
	 * 初始化顶部
	 */
	private void initTop() {
		mController = (RelativeLayout) findViewById(R.id.controlbar);
		mBack = (ImageButton) mController.findViewById(R.id.back);
	}

	/**
	 * 初始化界面
	 */
	private void initUI() {
		ll_pb_refresh = (LinearLayout) findViewById(R.id.ll_pb_refresh);

		initTop();

		initBVideoView();

		registerCallbackForControl();
	}

	//	/**
	//	 * 发起直播
	//	 */
	//	private void laucherLivePlay() {
	//		if (mPlayerStatus != PLAYER_STATUS.PLAYER_IDLE) {
	//			try {
	//				mLock.tryLock(2, TimeUnit.SECONDS);
	//				mVV.stopPlayback();
	//				mVV.setBackgroundResource(android.R.color.black);
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			} finally {
	//				mLock.unlock();
	//			}
	//		}
	//	}

	/**
	 * 执行UI回调函数
	 * @param runnables
	 */
	public void runUICallback(Runnable... runnables) {
		for (Runnable r : runnables) {
			if (r != null) {
				r.run();
			}
		}
	}

	/**
	 * 发送播放消息
	 */
	public void sendPlayMessage() {
		if (mEventHandler.hasMessages(EVENT_PLAY)) {
			mEventHandler.removeMessages(EVENT_PLAY);
		}
		mEventHandler.sendEmptyMessage(EVENT_PLAY);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_controller_playing);

		Intent intent = getIntent();
		getParamFromLastPage(intent);

		mIsHwDecode = intent.getBooleanExtra("isHW", false);

		initUI();

		updateControlBar(true);

		/**
		 * 开启后台事件处理线程
		 */
		mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mEventHandler = new EventHandler(mHandlerThread.getLooper());

		mUIHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				updateControlBar(false);
			}
		}, 15000);

		mLock = new ReentrantLock();
		getLink();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		/**
		 * 退出后台事件处理线程
		 */
		mHandlerThread.quit();
	}

	@Override
	protected void onPause() {
		super.onPause();

		/**
		 * 在停止播放前 记录当前播放的位置,以便以后可以续播
		 */
		try {
			mLock.tryLock(2, TimeUnit.SECONDS);
			mLastPos = mVV.getCurrentPosition();
			mVV.stopPlayback();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mLock.unlock();
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		L.i("#####onStop");
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ll_pb_refresh.setVisibility(View.VISIBLE);
		String shareLiveplayUrl = getLink();
		EventBus.getDefault().post(new FetchRtmpEvent(shareLiveplayUrl));
	}

	/**
	 * 获取rtmp
	 * @param fre
	 */
	public void onEventBackgroundThread(FetchRtmpEvent fre) {
		try {
		Live live = Live.getRtmp(fre.getLiveplayUrl());
		if (TextUtils.isEmpty(live.getStatus()) || Integer.parseInt(live.getStatus()) <= 0) {
//		mVideoSource = "";
		//mVideoSource = "rtmp://cdn-sov-2.musicradio.com:80/LiveVideo/Capital";
//		mVideoSource = "http://119.188.38.82/youku/6573F6708B53282529E4E24285/0300080100539A52ACC84E0182ECB0A0A37A56-D3AF-E97E-ECA2-C52485E883C6.mp4";
		} else {
		mVideoSource = live.getUrl();


		//rtmp测试地址
//		mVideoSource = "rtmp://qd.bms.baidu.com:1935/live/ad6a71fc859f11e4815aac853dd1c904?deviceid=130077497566464&sign=DTAES-CqnyQSm05YocyMV8Skl5IwA2-Vg5RoMgnv0eXf8hmtAphNb7STzo%3D&time=1418810545&expire=1418810780&liveid=141881054576125";

		L.e("mVideoSource-->" + mVideoSource);
		}
		EventBus.getDefault().post(mEventHandler);
		} catch (IOException e) {
		e.printStackTrace();
		}
		}

	/**
	 * 发起直播
	 * @param lle
	 */
	public void onEventMainThread(EventHandler handler) {
		sendPlayMessage();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mTouchTime = System.currentTimeMillis();
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			long time = System.currentTimeMillis() - mTouchTime;
			if (time < 400) {
				updateControlBar(!barShow);
			}
		}
		return true;
	}

	//	/**
	//	 * 处理直播
	//	 * 
	//	 * @param timestamp
	//	 */
	//	private void processLiving(long timestamp) {
	//		laucherLivePlay();
	//	}

	/**
	 * 为控件注册回调处理函数
	 */
	private void registerCallbackForControl() {
		mBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	public void setShowTimestamp(long showTimestamp) {
		this.showTimestamp = showTimestamp;
	}

	public void updateControlBar(boolean show) {
		if (show) {
			mController.setVisibility(View.VISIBLE);
		} else {
			mController.setVisibility(View.INVISIBLE);
		}

		barShow = show;
	}
	
	private String getLink(){
//		String strLink = "c2lIUmV4MHpUS0Y2bnkwTG85OWwxS2Q5dzdnblFzeTE2WHA4VlRZZWMzaVhMcURUMDY0dkRqelBOUUVmRUVVV2dKdjg3YTVFeUdmN2FHdDZyODNkWWJXNjBNVVhDc0VyZjQ0UDNUMVZqUkN1SnMvditJeG4vNzBSWXJkOWd0L1dTOFUvNnNhL3k3aFRmKzdUajRYVHFSSC95NE5YeFJPSw==";
		String strLink = "c2lIUmV4MHpUS0Y2bnkwTG85OWwxS2Q5dzdnblFzeTE2WHA4VlRZZWMzaVhMcURUMDY0dkRqelBOUUVmRUVVV2VuL0srMXVaT3R0c0FmUld5THBEUUNJeTMzNjQxWHBBdENIdnRFNW44RnpxekQrSVpjcldsYjBSWXJkOWd0L1dqWGNlNVJFS2tuV1NoZEZDSXI3RWdoSC95NE5YeFJPSw==";
		
		DES des = new DES();
		String decodeLink = des.authcode(strLink, "ENCODE", "JCLX-OneMeter789");
		Log.d("------------------", decodeLink);
		return decodeLink;
	}

}
