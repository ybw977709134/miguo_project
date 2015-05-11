package org.wowtalk.ui.msg;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.pzy.paint.BitmapPreviewActivity;
import com.pzy.paint.DoodleActivity;

import org.wowtalk.Log;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.Moment;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.crop.ImagePreviewActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

/**
 * Let user input text, voice, image, video, location, emotion, etc.
 *
 * <p>Usage:
 * <ul>
 *     <li>show(), hide().</li>
 *     <li>you handle the input result as a InputResultHandler.</li>
 *     <li>you have to call handleActivityResult() in your Activity.onAcitivityResult().</li>
 *     <li>you configure button drawables by accessing drawableResId().</li>
 * </ul>
 * </p>
 */
public class InputBoardManager implements Parcelable,
        View.OnClickListener, StampAdapter.OnStampSelectedListener,
        View.OnFocusChangeListener,
        HeightAwareRelativeLayout.OnHeightChangedListener {

    public interface InputResultHandler {
        public void setInputBoardMangager(InputBoardManager m);
        public InputBoardManager getInputBoardMangager();

        public void toastCannotSendMsg();

        public void onHeightChanged(int height);
        public void onTextInputted(String text);
        public void onVoiceInputted(String path, int duration);
        public void onStampInputted(Stamp s);
        public void onPhotoInputted(String path, String thumbPath);
        /** 输入了混合类型的消息：文字、图、音。*/
        public void onHybirdInputted(String text,
                                     String imagePath, String imageThumbPath,
                                     String voicePath, int voiceDuration);
        /** 用户想发送图文音消息。 */
        public void onHybirdRequested();
        public void onVideoInputted(String path, String thumbPath);
        
        /** 开始录制语音 */
        public void willRecordAudio();

        /**
         *
         * @param latitude
         * @param longitude
         * @param address may be null.
         */
        public void onLocationInputted(double latitude, double longitude, String address);
        public void onCallRequested();
        public void onVideoChatRequested();
    }

    public interface ChangeToOtherAppsListener {
        void changeToOtherApps();
    }

    /**
     * A collection of drawable resource ID.
     */
    public static class DrawableResId {
        public int open = 0;
        public int gotoEmotion = 0;
        public int close = 0;
        public int keyboard = 0;
        public int voiceNormal = 0;
        public int voicePressed = 0;
    }

    public static final int FLAG_SHOW_NONE = 0;
    public static final int FLAG_SHOW_TEXT = 1;
    public static final int FLAG_SHOW_MEDIA = 2;
    public static final int FLAG_SHOW_PHOTO = FLAG_SHOW_MEDIA;
    public static final int FLAG_SHOW_VIDEO = FLAG_SHOW_MEDIA;
    public static final int FLAG_SHOW_LOC = FLAG_SHOW_MEDIA;
    public static final int FLAG_SHOW_VOICE = 4;
    public static final int FLAG_SHOW_EMOTION = 8;
    public static final int FLAG_SHOW_KAOMOJI = FLAG_SHOW_EMOTION;
    public static final int FLAG_SHOW_STAMP = FLAG_SHOW_EMOTION;

    public final static int PHOTO_THUMBNAIL_WIDTH = 180;
    public final static int PHOTO_THUMBNAIL_HEIGHT = 120;
    // resize photo to VGA size before sending
    public final static int PHOTO_SEND_WIDTH = 640;
    public final static int PHOTO_SEND_HEIGHT = 480;

    // map ChatMessage.MSGTYPE_MULTIMEDIA_* to MEDIA_TYPE_*
    //private static HashMap<String, Integer> msgtype2mediatype = null;
    //private static HashMap<String, String> msgtype2mime = null;

    /**
     * Activity request code for inputting video.
     *
     * Client Activity can change its value to avoid conflict.
     */
    public static final int REQ_INPUT_VIDEO = 80871;

    /**
     * Activity request code for inputting photo.
     *
     * Client Activity can change its value to avoid conflict.
     */
    public static final int REQ_INPUT_PHOTO = 80872;

    private static final int REQ_INPUT_PHOTO_FOR_DOODLE = 80873;
    private static final int REQ_INPUT_DOODLE = 80874;


    /**
     * Activity request code for inputting location.
     *
     * Client Activity can change its value to avoid conflict.
     */
    public static final int REQ_INPUT_LOC = 80875;
    
    public static final int REQ_INPUT_CROP = 80876;
    public static final int REQ_INPUT_PHOTO_FOR_DOODLE_CROP = 80877;

    private Activity mContext;
    private ViewGroup mContainer;
    public View mRootView;
    /* layoutTextInnerWrapper aligns to the top of layoutTextWrapper,
     * distance from the bottom of layoutTextInnerWrapper to the bottom of layoutTextWrapper
     * should be the height of layoutMediaWrapper or layoutStampWrapper.
     */
    private View layoutTextWrapper;
    public View layoutTextInnerWrapper;
    private View layoutMediaWrapper;
    private View layoutStampWrapper;
    public EditText mTxtContent;
    private View mBtnSend;
    private View mBtnMedia;
    private Button mBtnEmotion; 
    private Dialog voicePreviewDlg;
    private TextView btnSpeak;
    private TimerTextView mVoiceTimer;
    
//    public static EditText pubContent = (EditText) mRootView.findViewById(R.id.layoutText).findViewById(R.id.txt_content);

    private View layoutVoiceWrapper;
    private FrameLayout mIndicatorBg;
    private TextView mIndicatorText;
    private TimerTextView voiceTimer;

    private MediaInputHelper mMediaInputHelper;
    private File mLastVoiceFile = null;
    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;
    private PowerManager.WakeLock recordingWakeLock;

    /** drawable res id */
    private DrawableResId mDrawableResId = new DrawableResId();

    EmotionBoard mEmotionBoard;

    // full height of UI components
    private int mHeight_mediaPanel = 0,
            mHeight_stampPanel = 0,
            mHeight_textBox = 0;
    private int mHeight_familyTop=0;

    private InputResultHandler mResultHandler;
    private ChangeToOtherAppsListener mChangeAppsListener;
    private int mShowingFlags;
    private Bundle mExtra;

    private boolean  mIsKeyboardShowing = false;
    private boolean mIsWithMultimediaMethod = true;
    private boolean mIsWithCallMethod = true;
    private boolean mIsPlayingMedia;
    private boolean mCanSendMsg = true;
    private boolean mIsOnDeleteView = false;

    // used in onHeightChanged()
    private Runnable mRunnableOnResize;

    private String outputfilename;
    //private String previewOutFilename;
    
    private Uri outputUri = null;
    private Uri outputUri2 = null;
    
    private TextView textView_spare_time;
//    private int recLen = 11;
    
    private Handler handler = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what){
    		case 0:
    			btnSpeak.setEnabled(true);
    			textView_spare_time.setVisibility(View.GONE);
    			break;
    		case 1:
            	stopRecording();
            	mResultHandler.onVoiceInputted(mLastVoiceFile.getAbsolutePath(), mVoiceTimer.getElapsed());
            	break;
    		case 2:
    			textView_spare_time.setVisibility(View.VISIBLE);
    			textView_spare_time.setText("还可以说  10  秒");
    			handler.removeMessages(2);
//    			recLen--;
//    			textView_spare_time.setText("还可以说 " + recLen + " 秒");
//    			if(recLen > 0){ 
//    				Message message = handler.obtainMessage(2);
//                    handler.sendMessageDelayed(message, 1000);
//    			}else{
//    				textView_spare_time.setVisibility(View.GONE);
//    			}
    			break;
    		}   		
    		
    		    		
//    		if(msg.what == 0){
//    			btnSpeak.setEnabled(true);
//    			textView_spare_time.setVisibility(View.GONE);
//    		}else if(msg.what == 1){
//            	stopRecording();
//            	mResultHandler.onVoiceInputted(mLastVoiceFile.getAbsolutePath(), mVoiceTimer.getElapsed());
//    		}else if(msg.what == 2){
//    			textView_spare_time.setVisibility(View.VISIBLE);
////    			textView_spare_time.setText("还可以说  10  秒");
//    			recLen--;
//    			textView_spare_time.setText("还可以说 " + recLen + " 秒");
//    			if(recLen > 0){ 
//    				Message message = handler.obtainMessage(2);
//                    handler.sendMessageDelayed(message, 1000);
//    			}else{
//    				textView_spare_time.setVisibility(View.GONE);
//    			}
//    		}
    	};
    };
    

    /**
     * @param context need to be able to receive Activity result.
     * @param container SHOULD be IHeightAwareView
     * @param handler can be null.
     */
    public InputBoardManager(
            Activity context, ViewGroup container,
            InputResultHandler handler,
            ChangeToOtherAppsListener listener) {
        mShowingFlags = FLAG_SHOW_NONE;
        init(context, container, handler, listener);
    }

    public void setIsWithMultimediaMethod(boolean isWithMultimediaMethod) {
        mIsWithMultimediaMethod = isWithMultimediaMethod;
    }

    public void setIsWithCallMethod(boolean isWithCallMethod) {
        mIsWithCallMethod = isWithCallMethod;
    }

    public void setCanSendMsg(boolean canSendMsg) {
        mCanSendMsg = canSendMsg;
    }

    /**
     * After recover from Bundle, you may want to re-init these properties.
     * @param context need to be able to receive Activity result.
     * @param container SHOULD be IHeightAwareView
     * @param handler
     * @param listener 
     */
    public void init(
            Activity context, ViewGroup container,
            InputResultHandler handler, ChangeToOtherAppsListener listener) {
        mContext = context;
        mContainer = container;
        mResultHandler = handler;
        mChangeAppsListener = listener;
//        configDefaultDrawable();
        setupViews();
    }

    public void hide() {
        show(FLAG_SHOW_NONE);
    }

    public boolean isShowing() {
        return mShowingFlags != FLAG_SHOW_NONE;
    }

    public int showingFlags() {
        return mShowingFlags;
    }

    public boolean show(int flags) {
        if (mRootView == null) {
            if (!setupViews()) return false;
        }
        setInputMode(flags);
        return true;
    }

    /**
     * reset layout after set some attributes, such as mIsWithCallMethod
     * @return
     */
    public boolean reShowLayout(int flags) {
        if (mRootView != null) {
            hideOrShowSomeViews();
            setInputMode(flags);
            return true;
        }
        return false;
    }

    public View getRootView() {
        return mRootView;
    }

    public boolean setupViews() {
        if(null == mContext) {
            return false;
        }
        mRootView = View.inflate(mContext, R.layout.msg_input_dialog, null);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mContainer.addView(mRootView, lp);

        if (mRootView == null)
            return false;

        layoutVoiceWrapper = mRootView.findViewById(R.id.layoutVoice);
        mIndicatorBg = (FrameLayout) layoutVoiceWrapper.findViewById(R.id.indicatorBg);
        mIndicatorText = (TextView) layoutVoiceWrapper.findViewById(R.id.txt_indicator);
        voiceTimer = (TimerTextView) layoutVoiceWrapper.findViewById(R.id.txt_timer);
        textView_spare_time = (TextView) layoutVoiceWrapper.findViewById(R.id.textView_spare_time);
        layoutVoiceWrapper.setVisibility(View.GONE);       

        layoutStampWrapper = mRootView.findViewById(R.id.layoutStamp);
        layoutMediaWrapper = mRootView.findViewById(R.id.layoutMedia);
        layoutTextWrapper = mRootView.findViewById(R.id.layoutText);
        layoutTextInnerWrapper = layoutTextWrapper.findViewById(R.id.layoutTextInnerWrapper);
        mTxtContent = (EditText)layoutTextWrapper.findViewById(R.id.txt_content);
        mBtnSend = layoutTextWrapper.findViewById(R.id.btn_send);
        mBtnMedia = layoutTextWrapper.findViewById(R.id.btn_toggle_media);
        mBtnEmotion = (Button)layoutTextWrapper.findViewById(R.id.btn_toggle_emotion);
        btnSpeak = (TextView)layoutTextWrapper.findViewById(R.id.btn_speak);
        mVoiceTimer = (TimerTextView)mRootView.findViewById(R.id.txtTimer);
        // hide or show some views according to the flags, such as mIsWithMultimediaMethod, mIsWithCallMethod
        hideOrShowSomeViews();

        mBtnSend.setOnClickListener(this);
        mBtnMedia.setOnClickListener(this);
        mBtnEmotion.setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_pic).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_video).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_doodle).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_voice).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_free_call).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_video_chat).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_picvoice).setOnClickListener(this);
        layoutMediaWrapper.findViewById(R.id.btn_input_loc).setOnClickListener(this);

        // check mic availability
//        PackageManager pm = mContext.getPackageManager();
//        if (!pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
//            layoutMediaWrapper.findViewById(R.id.btn_input_voice).setEnabled(false);
//        }

        if (mContainer instanceof IHeightAwareView) {
            ((IHeightAwareView)mContainer).setOnHeightChangedListener(this);
        }

        // goto text input mode on:
        // 1, txt_content is focused
        // 2, txt_content is clicked (OnFocusChange may not happen if it's already focused)
        mTxtContent.setOnClickListener(this);
        mTxtContent.setOnFocusChangeListener(this);

        // 3, txt_content needs a textwatcher to listen some ui
        mBtnSend.setEnabled(false);
        mTxtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0){
                    mBtnSend.setEnabled(true);
                    mBtnSend.setVisibility(View.VISIBLE);
                }else {
                    mBtnSend.setEnabled(false);
                    mBtnSend.setVisibility(View.GONE);
                }
            }
        });

        // hold-to-speak button 处理按压在移动取消
        btnSpeak.setOnTouchListener(new View.OnTouchListener(){

			/* NOTE:
			 * on MOTO XOOM/MILESTONE, ACTION_UP fires automatically after typically 15~30 second timeout
			 * http://stackoverflow.com/questions/4168687/ontouchlistener-action-up-fires-automatically-after-30-second-timeout
			 * http://www.eoeandroid.com/thread-80903-1-1.html
			 */

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                int[] position = new int[2];
                btnSpeak.getLocationInWindow(position);
                int[] positoin1 = new int[2];
                layoutVoiceWrapper.findViewById(R.id.timerBg).getLocationInWindow(positoin1);
                int height = Math.abs(position[1] - positoin1[1]);
                switch(arg1.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIsOnDeleteView = false;
                        mIndicatorBg.setBackgroundColor(mContext.getResources().getColor(R.color.libmsg_sms_voice_indicator_send));
                        mIndicatorText.setText(R.string.msg_drag_to_upside_to_cancel);
                        btnSpeak.setText(R.string.msg_release_to_send);
                        btnSpeak.setBackgroundResource(mDrawableResId.voicePressed);
                        mContext.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        mResultHandler.willRecordAudio();
                        startRecording();                  

                        handler.sendEmptyMessageDelayed(2, 50000);
                        handler.sendEmptyMessageDelayed(1, 60000);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    	if(mVoiceTimer.getElapsed() >= 60){
                    		btnSpeak.setEnabled(false);
                    		handler.sendEmptyMessageDelayed(0, 1000);
                            mContext.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btnSpeak.setText(R.string.msg_hold_to_speak);
                            btnSpeak.setBackgroundResource(mDrawableResId.voiceNormal);
                    		break;
                    	}else{
                    		btnSpeak.setEnabled(false);
                    		handler.sendEmptyMessageDelayed(0, 1000);
                    		mContext.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            btnSpeak.setText(R.string.msg_hold_to_speak);
                            btnSpeak.setBackgroundResource(mDrawableResId.voiceNormal);
                            stopRecording();
                            if(mLastVoiceFile == null) {
                            	// failed to record voice
                            	} else if(mVoiceTimer.getElapsed() < 2 ) {
                            		// too short
//                            AlertDialog a = new AlertDialog.Builder(mContext)
//                                    .setMessage(R.string.msg_voice_too_short).create();
//                            a.setCanceledOnTouchOutside(true);
//                            a.show();
                            		Toast.makeText(mContext, R.string.msg_voice_too_short, Toast.LENGTH_SHORT).show();
                            		} else {
                            			if (mIsOnDeleteView) {
                            				if (mIsPlayingMedia && null != mPlayer && mPlayer.isPlaying()) {
                            					mPlayer.stop();
                            					mPlayer.release();
                            					}
                            				} else {
                            					if (mResultHandler != null) {
                            						mResultHandler.onVoiceInputted(mLastVoiceFile.getAbsolutePath(), mVoiceTimer.getElapsed());
                            						}
                            					}                  	
//                            showVoicePreviewDialog();
                            			}  
                            
                            handler.removeMessages(1);
                    		if(mVoiceTimer.getElapsed() < 50){
                    			handler.removeMessages(2);
                    		}else if(mVoiceTimer.getElapsed() >= 50){
                    			
                    		}
                            break;
                            }

                    case MotionEvent.ACTION_MOVE:
                        if (arg1.getY() < -height) {
                            mIsOnDeleteView = true;
                            mIndicatorBg.setBackgroundColor(mContext.getResources().getColor(R.color.libmsg_sms_voice_indicator_delete));
                            mIndicatorText.setText(R.string.msg_release_to_cancel_send);
                        } else {
                            mIsOnDeleteView = false;
                            mIndicatorBg.setBackgroundColor(mContext.getResources().getColor(R.color.libmsg_sms_voice_indicator_send));
                            mIndicatorText.setText(R.string.msg_drag_to_upside_to_cancel);
                        }
                        break;
                }
                return false;
            }
        });

        mEmotionBoard = new EmotionBoard(mContext, layoutStampWrapper, this);
//        mEmotionBoard.preload();

		/*
		 *  get UI components height
		 */
        //old mHeight_mediaPanel
//        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
//        mHeight_mediaPanel = (int)(metrics.widthPixels / 1.5f);
        //new mHeight_mediaPanel,by yuan
        layoutMediaWrapper.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(mHeight_mediaPanel > 0) return;
                        if(layoutMediaWrapper.getVisibility() == View.VISIBLE){
                            mHeight_mediaPanel = layoutMediaWrapper.getHeight();
                            setInputMode(FLAG_SHOW_MEDIA,false);
                        }
                    }
                });
        layoutTextInnerWrapper.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        if(mHeight_textBox > 0) return; // execute only once
                        if(layoutTextInnerWrapper.getVisibility() == View.VISIBLE) {
                            mHeight_textBox = layoutTextInnerWrapper.getHeight();
                            setInputMode(FLAG_SHOW_TEXT, false);
                        }
                    }
                });
        final ImageView ivFamilyTop=(ImageView)mRootView.findViewById(R.id.family_layout_top_img);
        ivFamilyTop.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        if(mHeight_familyTop > 0) return; // execute only once
                        if(ivFamilyTop.getVisibility() == View.VISIBLE) {
                            mHeight_familyTop = ivFamilyTop.getHeight();
                            if (mResultHandler != null) {
                                mResultHandler.onHeightChanged(mHeight_familyTop);
                            }
                        }
                    }
                });
        //mHeight_stampPanel will not be available until its first show.

        return true;
    }

    /**
     * hide or show some views according to the flags,
     * such as mIsWithMultimediaMethod, mIsWithCallMethod
     */
    private void hideOrShowSomeViews() {
        if (mIsWithMultimediaMethod) {
            mBtnMedia.setVisibility(View.VISIBLE);
            mBtnEmotion.setVisibility(View.VISIBLE);
        } else {
            mBtnMedia.setVisibility(View.GONE);
            mBtnEmotion.setVisibility(View.GONE);
        }
        if (mIsWithCallMethod) {
            layoutMediaWrapper.findViewById(R.id.layout_freecall).setVisibility(View.VISIBLE);
            layoutMediaWrapper.findViewById(R.id.layout_videochat).setVisibility(View.VISIBLE);
        } else {
            layoutMediaWrapper.findViewById(R.id.layout_freecall).setVisibility(View.GONE);
            layoutMediaWrapper.findViewById(R.id.layout_videochat).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_send) {
            if (mResultHandler != null) {
                if (!mCanSendMsg) {
                    mResultHandler.toastCannotSendMsg();
                    return;
                }
                mResultHandler.onTextInputted(mTxtContent.getText().toString());
            }

        } else if (i == R.id.btn_toggle_media) {
            if (!mCanSendMsg) {
                mResultHandler.toastCannotSendMsg();
                return;
            }
            setInputMode(FLAG_SHOW_PHOTO);

        } else if (i == R.id.family_layout_top_img) {
            if (!mCanSendMsg) {
                mResultHandler.toastCannotSendMsg();
                return;
            }

            if(layoutMediaWrapper.getVisibility() != View.VISIBLE) {
                setInputMode(FLAG_SHOW_PHOTO);
            } else {
                setMultimediaPanelVisibility(View.INVISIBLE);
                if (mResultHandler != null) {
                    mResultHandler.onHeightChanged(mHeight_familyTop);
                }
            }
        } else if (i == R.id.btn_toggle_emotion) {
            if (!mCanSendMsg) {
                mResultHandler.toastCannotSendMsg();
                return;
            }
            setInputMode(FLAG_SHOW_STAMP);
        } else if (i == R.id.btn_input_pic) {
        	if(DoubleClickedUtils.isFastDoubleClick()){
        		return;  
        	}
            inputImage(REQ_INPUT_PHOTO);
//            inputImage(REQ_INPUT_PHOTO_FOR_DOODLE);

        } else if (i == R.id.btn_input_video) {
        	if(DoubleClickedUtils.isFastDoubleClick()){
        		return;  
        	}
            inputVideo();
        } else if (i == R.id.btn_input_doodle) {
        	if(DoubleClickedUtils.isFastDoubleClick()){
        		return;  
        	}
            inputDoodle();
        } else if (i == R.id.btn_input_picvoice) {
            mResultHandler.onHybirdRequested();
            setInputMode(FLAG_SHOW_TEXT, false);
        } else if (i == R.id.btn_input_voice) {// replace text inputbox with hold-to-speak button
            PackageManager pm = mContext.getPackageManager();
            if (!pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                Toast.makeText(mContext, R.string.media_record_not_avaliable, Toast.LENGTH_SHORT).show();
            } else {
                setInputMode(FLAG_SHOW_VOICE);
            }
        } else if (i == R.id.btn_input_loc) {
            inputLocation();
        } else if (i == R.id.txt_content) {
            setInputMode(FLAG_SHOW_TEXT);

        } else if (i == R.id.btn_free_call) {
            if (mResultHandler != null) {
                mResultHandler.onCallRequested();
            }
        } else if (i == R.id.btn_video_chat) {
            if (mResultHandler != null) {
                mResultHandler.onVideoChatRequested();
            }
        }
    }

    private void inputImage(int activityReqCode) {
        if (mMediaInputHelper == null) {
            mMediaInputHelper = new MediaInputHelper(mChangeAppsListener);
        } else {
            mMediaInputHelper.setChangeAppsListener(mChangeAppsListener);
        }
        mMediaInputHelper.inputImage(mContext, activityReqCode, null);
    }

    private void inputVideo() {
        if (mMediaInputHelper == null) {
            mMediaInputHelper = new MediaInputHelper(mChangeAppsListener);
        } else {
            mMediaInputHelper.setChangeAppsListener(mChangeAppsListener);
        }
        mMediaInputHelper.inputVideo(mContext, REQ_INPUT_VIDEO);
    }

    private void inputDoodle() {
        inputImage(REQ_INPUT_PHOTO_FOR_DOODLE);
    }

    /**
     * use AMAP to pick location
     */
    private void inputLocation() {
//        boolean googleMapExist= (ConnectionResult.SUCCESS==GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext));
//
//        if(googleMapExist) {
//            org.wowtalk.Log.w("google map exist");
//            Intent mapIntent = new Intent(mContext, PickLocActivity.class);
//            mapIntent.putExtra("auto_loc", true);
//            mContext.startActivityForResult(mapIntent, REQ_INPUT_LOC);
//        } else {
            //org.wowtalk.Log.w("google map not exist");
            Intent mapIntent = new Intent(mContext, PickLocActivityWithAMap.class);
            mapIntent.putExtra("auto_loc", true);
            mContext.startActivityForResult(mapIntent, REQ_INPUT_LOC);
        //}
    }

    /**
     * 设置点赞图片的状态
     */
    private void setMomentLikeStatus() {
        if(null != mBtnEmotion && null != likeRelateMoment) {
            mBtnEmotion.setVisibility(View.VISIBLE);
            if(likeRelateMoment.likedByMe) {
                mBtnEmotion.setBackgroundResource(momentLikedDrawableResId);
            } else {
                mBtnEmotion.setBackgroundResource(momentUnlikeDrawableResId);
            }
//            if(likeRelateMoment.likedByMe ||
//                    Moment.SERVER_MOMENT_TAG_FOR_QA.equals(likeRelateMoment.tag) ||
//                    Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(likeRelateMoment.tag) ||
//                    Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(likeRelateMoment.tag)) {
//                mBtnEmotion.setVisibility(View.GONE);
//            } else {
//                mBtnEmotion.setVisibility(View.VISIBLE);
//            }
            mBtnEmotion.setOnClickListener(likeMomentListener);
        }
    }

    /**
     * 未赞的图片资源ID
     */
    private int momentUnlikeDrawableResId;
    /**
     * 已赞的图片资源ID
     */
    private int momentLikedDrawableResId;

    /**
     * 好友圈输入框设置点“赞”资源图片。
     * @param unlike
     * @param liked
     */
    public void setMomentLikeDrawable(int unlike,int liked) {
        momentUnlikeDrawableResId=unlike;
        momentLikedDrawableResId=liked;
    }

    /**
     * 点“赞”Listener
     */
    private View.OnClickListener likeMomentListener;
    /**
     * 传入的moment
     */
    private Moment likeRelateMoment;

    /**
     * 为好友圈点赞按钮设置Listener并初始化点赞状态
     * @param moment
     * @param listener
     */
    public void setLayoutForTimelineMoment(Moment moment,View.OnClickListener listener) {
        likeRelateMoment=moment;
        likeMomentListener=listener;

        setMomentLikeStatus();
    }

    public void setLayoutForFamily() {
        layoutTextInnerWrapper.setVisibility(View.GONE);
        mRootView.findViewById(R.id.family_layout_top_img).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.family_layout_top_img).setOnClickListener(this);

        setEmotionPanelVisibility(View.GONE);
        setMultimediaPanelVisibility(View.GONE);

        //move location pick to last
        TableRow tr2=(TableRow) mRootView.findViewById(R.id.tableRow2);
        View locItem=tr2.getChildAt(0);
        tr2.removeViewAt(0);
        tr2.addView(locItem,2);

        //move video chat from tr2 to tr1
        TableRow tr1=(TableRow) mRootView.findViewById(R.id.tableRow1);
        View videoChat=tr2.getChildAt(1);
        View inputVoice=tr1.getChildAt(0);
        tr1.removeViewAt(0);
        tr2.removeViewAt(1);
        tr1.addView(videoChat,0);
        tr2.addView(inputVoice,2);
    }

    /**
     * 获取多媒体面板的显示状态
     * @return
     */
    public int getMediaLayoutVisibility() {
        return layoutMediaWrapper.getVisibility();
    }

    /**
     * set input UI for specified msg type.
     * @param showFlags FLAG_SHOW_* constants.
     * @param auto_keyboard auto set keyboard visibility depending on input mode?
     */
    public void setInputMode(int showFlags, boolean auto_keyboard) {
        /**
         * NOTE ON IMPLEMENTATION:
         * If toggling of soft keyboard is evolved, the computing of UI components' padding
         * must be after the change of available screen height, to ensure this, we use
         * HeightAwareRelativeLayout.OnHeightChangedListener.
         */

        mShowingFlags = showFlags;
        mRunnableOnResize = null;

        switch (showFlags) {
            /**
             * show nothing
             */
        case FLAG_SHOW_NONE:
            layoutTextWrapper.setVisibility(View.GONE);
            layoutMediaWrapper.setVisibility(View.GONE);
            layoutStampWrapper.setVisibility(View.GONE);
            if (mResultHandler != null)
                mResultHandler.onHeightChanged(0);
            setSoftKeyboardVisibility(false);
            break;
            /**
             * show text inputboard
             */
        case FLAG_SHOW_TEXT:
            layoutTextWrapper.setVisibility(View.VISIBLE);
            // text vs voice
            View viewTextVisible = layoutTextInnerWrapper.findViewById(R.id.layout_input_text);
            viewTextVisible.setVisibility(View.VISIBLE);
            btnSpeak.setVisibility(View.GONE);

//            mBtnEmotion.setVisibility(View.VISIBLE);
            mBtnEmotion.setVisibility(View.GONE);
            mBtnMedia.setVisibility(View.GONE);

//            setMultimediaPanelVisibility(View.INVISIBLE);
//            setEmotionPanelVisibility(View.INVISIBLE);

            setMultimediaPanelVisibility(View.GONE);
            setEmotionPanelVisibility(View.GONE);

            if(auto_keyboard)
                setSoftKeyboardVisibility(true);

            if (mResultHandler != null) {
                if(0 == mHeight_textBox && mHeight_familyTop > 0) {
                    mResultHandler.onHeightChanged(mHeight_familyTop);
                } else {
                    mResultHandler.onHeightChanged(mHeight_textBox);
                }
            }

            // next action: text -> multi media
//            mBtnMedia.setBackgroundResource(mDrawableResId.open);
//            mBtnMedia.setOnClickListener(new View.OnClickListener(){
//                @Override
//                public void onClick(View arg0) {
//                    if (!mCanSendMsg) {
//                        mResultHandler.toastCannotSendMsg();
//                        return;
//                    }
//                    setInputMode(FLAG_SHOW_PHOTO);
//                }
//            });
//
//            mBtnEmotion.setBackgroundResource(mDrawableResId.gotoEmotion);
//            mBtnEmotion.setOnClickListener(new View.OnClickListener(){
//                @Override
//                public void onClick(View arg0) {
//                    if (!mCanSendMsg) {
//                        mResultHandler.toastCannotSendMsg();
//                        return;
//                    }
//                    setInputMode(FLAG_SHOW_STAMP);
//                }
//            });
            break;
            /**
             * show voice UI
             */
        case FLAG_SHOW_VOICE:
            // text vs voice
            mTxtContent.clearFocus();
            View viewTextInvisible = layoutTextInnerWrapper.findViewById(R.id.layout_input_text);
            viewTextInvisible.setVisibility(View.INVISIBLE);
            btnSpeak.setVisibility(View.VISIBLE);
            mBtnEmotion.setVisibility(View.GONE);

            btnSpeak.setText(R.string.msg_hold_to_speak);
            btnSpeak.setBackgroundResource(mDrawableResId.voiceNormal);
            setMultimediaPanelVisibility(View.INVISIBLE);
            setEmotionPanelVisibility(View.INVISIBLE);
            setSoftKeyboardVisibility(false);

            if (mResultHandler != null)
                mResultHandler.onHeightChanged(mHeight_textBox);

            // next action: voice -> text
            mBtnMedia.setBackgroundResource(mDrawableResId.close);
            mBtnMedia.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    setInputMode(FLAG_SHOW_TEXT, false);
                }
            });
            break;
            /**
             * show media photo...
             */
        case FLAG_SHOW_PHOTO:
            mTxtContent.clearFocus();
            mRunnableOnResize = new Runnable() {
                @Override
                public void run() {
                    setEmotionPanelVisibility(View.GONE);

                    if (mResultHandler != null)
                        mResultHandler.onHeightChanged(mHeight_textBox + mHeight_mediaPanel);

                    setMultimediaPanelVisibility(View.VISIBLE);

                    // next action: multi media -> text
                    mBtnMedia.setBackgroundResource(mDrawableResId.close);
                    mBtnMedia.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View arg0) {
                            setInputMode(FLAG_SHOW_TEXT, false);
                        }
                    });

                    mBtnEmotion.setBackgroundResource(mDrawableResId.gotoEmotion);
                    mBtnEmotion.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View arg0) {
                            setInputMode(FLAG_SHOW_STAMP);
                        }
                    });

                    mRunnableOnResize = null; // run only once in onHeightChanged();
                }
            };

            if(mIsKeyboardShowing) {
                // show multi media panel AFTER keyboard has been closed
                InputMethodManager imm = (InputMethodManager)mContext
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mTxtContent.getWindowToken(), 0, null);
            } else {
                mRunnableOnResize.run();
            }
            break;
            /**
             * show emotion expression
             */
        case FLAG_SHOW_STAMP:
            mRunnableOnResize = new Runnable(){
                @Override
                public void run() {
                    setMultimediaPanelVisibility(View.GONE);

                    if(mHeight_stampPanel == 0) {
                        mHeight_stampPanel = layoutStampWrapper.getHeight();
                        if (mResultHandler != null)
                            mResultHandler.onHeightChanged(mHeight_textBox + mHeight_stampPanel);

                        // set offset again later(more accurate, but may not happen)
                        layoutStampWrapper.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                            @Override
                            public void onGlobalLayout() {
                                if(mHeight_stampPanel >= layoutStampWrapper.getHeight()) return;
                                mHeight_stampPanel = layoutStampWrapper.getHeight();
                                if (mResultHandler != null)
                                    mResultHandler.onHeightChanged(mHeight_textBox + mHeight_stampPanel);
                            }
                        });
                    } else {
                        if (mResultHandler != null)
                            mResultHandler.onHeightChanged(mHeight_textBox + mHeight_stampPanel);
                    }

                    setEmotionPanelVisibility(View.VISIBLE);

                    // next action: text -> multi media
                    mBtnMedia.setBackgroundResource(mDrawableResId.open);
                    mBtnMedia.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View arg0) {
                            setInputMode(FLAG_SHOW_PHOTO, true);
                        }
                    });

                    mBtnEmotion.setBackgroundResource(mDrawableResId.keyboard);
                    mBtnEmotion.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            mTxtContent.requestFocus();
                            setInputMode(FLAG_SHOW_TEXT, true);
                        }
                    });

                    mRunnableOnResize = null; // run only once in onHeightChanged();
                }
            };

            if(mIsKeyboardShowing) {
                // show emotion panel after keyboard being closed
                InputMethodManager imm = (InputMethodManager)mContext
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                // a little too earlier for mRunnableOnResize
                imm.hideSoftInputFromWindow(mTxtContent.getWindowToken(), 0);
            } else {
                mRunnableOnResize.run();
            }
            break;

        default:
            break;
        }

        setMomentLikeStatus();
    }

    /**
     *
     * @param type FLAG_SHOW_*
     */
    private void setInputMode(int type) {
        setInputMode(type, true);
    }

    /**
     *
     * @param visibility View.VISIBLE|View.GONE
     */
    private void setMultimediaPanelVisibility(int visibility) {
        if(View.VISIBLE == visibility) {
            ((ImageView)mRootView.findViewById(R.id.family_layout_top_img)).setImageResource(R.drawable.sms_fold_btn);

            layoutMediaWrapper.setVisibility(View.VISIBLE);
//            if(layoutMediaWrapper.getLayoutParams().height != mHeight_mediaPanel) {
//                layoutMediaWrapper.getLayoutParams().height = mHeight_mediaPanel;
//                layoutMediaWrapper.requestLayout();
//            }
        } else {
            ((ImageView)mRootView.findViewById(R.id.family_layout_top_img)).setImageResource(R.drawable.sms_unfold_btn);

            layoutMediaWrapper.setVisibility(View.GONE);
        }
    }

    /**
     * indicates that voice recording is in progress.
     * @param show show or hide?
     */
    private void showVoiceRecordingIndicator(boolean show) {
        View v = mRootView.findViewById(R.id.layout_recording_indicator);
        View view = mRootView.findViewById(R.id.layoutVoice);
        if(v == null) return;

        if(show) {
            v.setVisibility(View.GONE);
            mVoiceTimer.reset();
            mVoiceTimer.start();

            view.setVisibility(View.VISIBLE);
            voiceTimer.reset();
            voiceTimer.start();

            mVoiceTimer.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, 0, 0));
            voiceTimer.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, 0, 0));
        } else {
            v.setVisibility(View.GONE);
            mVoiceTimer.stop();

            view.setVisibility(View.GONE);
            voiceTimer.stop();
        }
    }

    /**
     * preview, send or discard the latest recorded voice.
     */
//    private void showVoicePreviewDialog() {
//        if(voicePreviewDlg != null)
//            voicePreviewDlg.dismiss();
//
//        View dlgView = LayoutInflater.from(mContext).inflate(R.layout.msg_dialog_voice_preview, null, false);
//        mIsPlayingMedia = false;
//        dlgView.findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(final View btnPlay) {
//                if (mIsPlayingMedia) {
//                    mIsPlayingMedia = false;
//                    if (null != mPlayer) {
//                        mPlayer.stop();
//                    }
//                    ((ImageButton)btnPlay).setImageResource(R.drawable.play);
//                } else {
//                    mPlayer = new MediaPlayer();
//                    try {
//                        mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
//                        mPlayer.prepare();
//                        mPlayer.start();
//                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                            public void onCompletion(MediaPlayer arg0) {
//                                mIsPlayingMedia = false;
//                                ((ImageButton)btnPlay).setImageResource(R.drawable.play);
//                            }
//                        });
//                        ((ImageButton)btnPlay).setImageResource(R.drawable.stop);
//                        mIsPlayingMedia = true;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        dlgView.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View arg0) {
//                voicePreviewDlg.dismiss();
//                if (mResultHandler != null) {
//                    mResultHandler.onVoiceInputted(
//                            mLastVoiceFile.getAbsolutePath(),
//                            mVoiceTimer.getElapsed());
//                }
//                if (mIsPlayingMedia && null != mPlayer && mPlayer.isPlaying()) {
//                    mPlayer.stop();
//                    mPlayer.release();
//                }
//            }
//        });
//        dlgView.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View arg0) {
//                voicePreviewDlg.dismiss();
//                if (mIsPlayingMedia && null != mPlayer && mPlayer.isPlaying()) {
//                    mPlayer.stop();
//                    mPlayer.release();
//                }
//            }
//        });
//
//        voicePreviewDlg = new Dialog(mContext, R.style.borderless_dialog);
//        voicePreviewDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        voicePreviewDlg.setContentView(dlgView);
//        voicePreviewDlg.show();
//        // 100% width
//        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//        lp.copyFrom(voicePreviewDlg.getWindow().getAttributes());
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//        voicePreviewDlg.getWindow().setAttributes(lp);
//    }

    private void setEmotionPanelVisibility(int visibility) {
        if (visibility == View.VISIBLE) {
            layoutStampWrapper.setVisibility(View.VISIBLE);
            if (!mEmotionBoard.isPrepared()) {
                mEmotionBoard.prepare();
            }
        } else {
            layoutStampWrapper.setVisibility(View.GONE);
        }
    }

    /**
     * Set default input text. Should be called after first show().
     * @param text
     */
    public void setInputText(String text) {
        if (mTxtContent != null) {
            mTxtContent.setText(text);
        }
    }

    /**
     * Set hint of EditText. Should be called after first show().
     * @param hint
     */
    public void setInputHint(String hint) {
        if (mTxtContent != null) {
            mTxtContent.setHint(hint);
        }
    }

    /**
     * Access user extra data.
     * @return will never be null.
     */
    public Bundle extra() {
        if (mExtra == null)
            mExtra = new Bundle();
        return mExtra;
    }

    /**
     * Access drawable res id configuration.
     * @return will never be null.
     */
    public DrawableResId drawableResId() {
        return mDrawableResId;
    }

    /**
     * set keyboard's visibility
     * @param visible
     */
    public void setSoftKeyboardVisibility(boolean visible) {
        if (mTxtContent == null)
            return;

        final InputMethodManager imm = (InputMethodManager)mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if(visible)
            imm.showSoftInput(mTxtContent, 0);
        else
            imm.hideSoftInputFromWindow(mTxtContent.getWindowToken(), 0);
    }

    private void startRecording() {
        if(mRecorder == null) {
            mRecorder = new MediaRecorder();
        } else {
            mRecorder.reset();
        }

        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioSamplingRate(16000);
            mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(
                    MediaInputHelper.MEDIA_TYPE_VOICE, "." + ChatMessage.SEND_AUDIO_EXT);
            mRecorder.setOutputFile(mLastVoiceFile.getAbsolutePath());
            mRecorder.prepare();
            mRecorder.start();

            showVoiceRecordingIndicator(true);
        } catch (Exception e) {
            e.printStackTrace();
            stopRecording();
//            alert(R.string.msg_recorder_error);

            // prevent invalid voice from being sent
            mLastVoiceFile.delete();
            mLastVoiceFile = null;
        }
    }

    private void stopRecording() {
        try {
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder = null;
        } finally {
            showVoiceRecordingIndicator(false);
            if(recordingWakeLock != null) {
                recordingWakeLock.release();
                recordingWakeLock = null;
            }
        }
    }

    @Override
    public void OnStampSelected(Stamp s) {
        if (mResultHandler != null) {
            mResultHandler.onStampInputted(s);
        }
    }

    @Override
    public void OnKomojiSelected(String kaomoji) {
        int idx=mTxtContent.getSelectionStart();
        if(-1 != idx) {
            mTxtContent.getText().insert(idx, kaomoji);
        } else {
            mTxtContent.getText().append(kaomoji);
        }
    }

    /**
     * Handle Activity result for inputting photo/video/location.
     *
     * Call me in the front of onAcitivityResult().
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return whether this result has been consumed.
     */
    public boolean handleActivityResult (int requestCode, int resultCode, Intent data) {
        boolean result = false;
        if (Activity.RESULT_OK != resultCode || null == mResultHandler) {
            return result;
        }

        Log.i("InputBoardManager#handleActivityResult, "
                + "mMediaInputHelper is null(" + (null == mMediaInputHelper));
        switch (requestCode) {
            case REQ_INPUT_LOC:
                final double lat = data.getExtras().getDouble("target_lat");
                final double lon = data.getExtras().getDouble("target_lon");
                mResultHandler.onLocationInputted(lat, lon, null);
                result = true;
                break;
            case REQ_INPUT_VIDEO:
                if (null != mMediaInputHelper) {
                    String[] videoPath = new String[2];
                    if(mMediaInputHelper.handleVideoResult(
                            mContext,
                            data,
                            PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                            PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                            videoPath)) {
                        mResultHandler.onVideoInputted(videoPath[0], videoPath[1]);
                        result = true;
                    }
                }
                break;
            case REQ_INPUT_PHOTO:
            	if(data != null){
                	Uri  mImageCaptureUri = data.getData();
//                	if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
//                		File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
//                		outputUri = Uri.fromFile(f);
//                	}
//                	Log.i("--mImageCaptureUri--" + mImageCaptureUri);
//                	doCrop(mImageCaptureUri,outputUri);	
                	 outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                 	 mContext.startActivityForResult(new Intent().setData(mImageCaptureUri).setClass(mContext, ImagePreviewActivity.class).putExtra(ImagePreviewActivity.OUTPUTPATH, outputfilename), REQ_INPUT_DOODLE);
            	}else{
                    if (null != mMediaInputHelper) {
                        String[] photoPath = new String[2];
                        if (mMediaInputHelper.handleImageResult(
                                mContext,
                                data,
                                PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                                PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                                photoPath)) {
                            outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                            mContext.startActivityForResult(
                                    new Intent(mContext, BitmapPreviewActivity.class)
                                            .putExtra(BitmapPreviewActivity.EXTRA_MAX_WIDTH, PHOTO_SEND_WIDTH)
                                            .putExtra(BitmapPreviewActivity.EXTRA_MAX_HEIGHT, PHOTO_SEND_HEIGHT)
                                            .putExtra(BitmapPreviewActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
                                            .putExtra(BitmapPreviewActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                                    REQ_INPUT_DOODLE
                            );
                        }
                    }
            	}

                result = true; 
                break;
            case REQ_INPUT_CROP:
                if (null != mMediaInputHelper) {
                	//Log.i("--after crop data --" + data.getData());
                	//针对部分4.4机型返回的intent(data)为空，重新传进之前的Uri
                	if(data.getData() == null){
                		data = new Intent();
                		data.setData( outputUri);
                	}
                    String[] photoPath = new String[2];
                    boolean istrue =mMediaInputHelper.handleImageResult(
                            mContext,
                            data,
                            PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                            PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                            photoPath);
                    if (istrue) {
                        outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                        mContext.startActivityForResult(
                                new Intent(mContext, BitmapPreviewActivity.class)
                                        .putExtra(BitmapPreviewActivity.EXTRA_MAX_WIDTH, PHOTO_SEND_WIDTH)
                                        .putExtra(BitmapPreviewActivity.EXTRA_MAX_HEIGHT, PHOTO_SEND_HEIGHT)
                                        .putExtra(BitmapPreviewActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
                                        .putExtra(BitmapPreviewActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                                REQ_INPUT_DOODLE
                        );
                    }
                }
                result = true;
                break;
            case REQ_INPUT_DOODLE: {
                String[] photoPath = new String[2];
                photoPath[0] = outputfilename;

                // generate thumbnail
                File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_THUMNAIL, ".jpg");
                if (f != null) {
                    photoPath[1] = f.getAbsolutePath();
                    Bitmap thumbnail = BmpUtils.decodeFile(photoPath[0],
                            PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT);
                    Log.i("--photo[0]--" + photoPath[0]);
                    Log.i("--photo[1]--" + photoPath[1]);
                    try {
                        FileOutputStream fos = new FileOutputStream(photoPath[1]);
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }

                mResultHandler.onPhotoInputted(photoPath[0], photoPath[1]);
                result = true;
                break;
            }
            case REQ_INPUT_PHOTO_FOR_DOODLE:
            	if(data != null){
            		Uri mImageCaptureUri2 = data.getData();
//            		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
//                		File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
//                		outputUri2 = Uri.fromFile(f);
//                	}
//            		doCrop_doodle(mImageCaptureUri2,outputUri2);
            		 outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                 	 mContext.startActivityForResult(new Intent().setData(mImageCaptureUri2)
                 			 .setClass(mContext, ImagePreviewActivity.class)
                 			 .putExtra(ImagePreviewActivity.OUTPUTPATH, outputfilename)
                 			 .putExtra(ImagePreviewActivity.ISDOODLE, true), REQ_INPUT_PHOTO_FOR_DOODLE_CROP);
            	}else {
                    if (null != mMediaInputHelper) {
                    	Log.i("--mLastImageUri" + mMediaInputHelper.getLastImageUri());
                    	File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
                		outputUri2 = Uri.fromFile(f);
                    	doCrop_doodle(mMediaInputHelper.getLastImageUri(), outputUri2);
//                        String[] photoPath = new String[2];
//                        if (mMediaInputHelper.handleImageResult(
//                                mContext,
//                                data,
//                                PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
//                                PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
//                                photoPath)) {
//                            outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
//                            mContext.startActivityForResult(
//                                    new Intent(mContext, DoodleActivity.class)
//                                            .putExtra(DoodleActivity.EXTRA_MAX_WIDTH, PHOTO_SEND_WIDTH)
//                                            .putExtra(DoodleActivity.EXTRA_MAX_HEIGHT, PHOTO_SEND_HEIGHT)
//                                            .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
//                                            .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
//                                    REQ_INPUT_DOODLE
//                            );
//                        }
                    }
            	}
            	
                result = true;
                break;
            case REQ_INPUT_PHOTO_FOR_DOODLE_CROP:
                if (null != mMediaInputHelper) {
                	Log.i("--after crop data --" + data.getData());
                	//针对部分4.4机型返回的intent(data)为空，重新传进之前的Uri
                	if(data.getData() == null){
                		data = new Intent();
                		data.setData( outputUri2);
                	}
                    String[] photoPath = new String[2];
                    if (mMediaInputHelper.handleImageResult(
                            mContext,
                            data,
                            PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                            PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                            photoPath)) {
                        outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                        mContext.startActivityForResult(
                                new Intent(mContext, DoodleActivity.class)
                                        .putExtra(DoodleActivity.EXTRA_MAX_WIDTH, PHOTO_SEND_WIDTH)
                                        .putExtra(DoodleActivity.EXTRA_MAX_HEIGHT, PHOTO_SEND_HEIGHT)
                                        .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
                                        .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                                REQ_INPUT_DOODLE
                        );
                    }
                }
            	result = true;
                break;
            default:
                break;
        }

        Log.i("InputBoardManager#handleActivityResult, handle result is " + result);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mMediaInputHelper, 0);
        String f = mLastVoiceFile == null ? null : mLastVoiceFile.getAbsolutePath();
        dest.writeString(f);
        dest.writeParcelable(mExtra, 0);
        dest.writeInt(mHeight_textBox);
        dest.writeInt(mHeight_mediaPanel);
        dest.writeInt(mHeight_stampPanel);
    }

    public static final Parcelable.Creator<InputBoardManager> CREATOR = new Creator<InputBoardManager>() {
        @Override
        public InputBoardManager createFromParcel(Parcel source) {
            InputBoardManager r = new InputBoardManager(null, null, null, null);
            r.mMediaInputHelper = source.readParcelable(MediaInputHelper.class.getClassLoader());
            String f = source.readString();
            if (f != null)
                r.mLastVoiceFile = new File(f);
            r.mExtra = source.readParcelable(Bundle.class.getClassLoader());
            r.mHeight_textBox = source.readInt();
            r.mHeight_mediaPanel = source.readInt();
            r.mHeight_stampPanel = source.readInt();
            return r;
        }

        @Override
        public InputBoardManager[] newArray(int size) {
            return new InputBoardManager[size];
        }
    };

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        int i = v.getId();
        if (i == R.id.txt_content) {
            if (hasFocus) {
                if (!mCanSendMsg) {
                    mResultHandler.toastCannotSendMsg();
//                    mTxtContent.setEnabled(false);
                    mTxtContent.setInputType(InputType.TYPE_NULL);
                    mTxtContent.clearFocus();
                    return;
                }
                setInputMode(FLAG_SHOW_TEXT, true);
            }

        }
    }

    @Override
    public void onHeightChanged(int requestHeight, int maxHeight) {
        if(requestHeight < maxHeight - 100) {
            mIsKeyboardShowing = true;
        } else if(requestHeight > maxHeight - 100) {
            mIsKeyboardShowing = false;
        }

        if(mRunnableOnResize != null) {
            // if run mRunnableOnResize directly here, some logic seems to be truncated.
            mRootView.post(mRunnableOnResize);
        }
    }

    /**
     * this methord is useless,in MessageComposerActivity reassignment
     */
    private void configDefaultDrawable() {
        drawableResId().open = R.drawable.sms_add_btn;
        drawableResId().close = R.drawable.sms_close_btn;
        drawableResId().gotoEmotion = R.drawable.sms_kaomoji_btn;
        drawableResId().keyboard = R.drawable.sms_keyboard;
        drawableResId().voiceNormal = R.drawable.sms_voice_btn;
        drawableResId().voicePressed = R.drawable.sms_voice_btn_p;
    }
    
	private void doCrop(Uri imageUri,Uri outputUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(imageUri, "image/*");
		intent.putExtra("scale", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scaleUpIfNeeded", true);
		intent.putExtra("return-data", false);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,outputUri);
		//Log.i("--outputUri--" + outputUri);
		mContext.startActivityForResult(intent, REQ_INPUT_CROP);			
	}

	private void doCrop_doodle(Uri imageUri, Uri outputUri2) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(imageUri, "image/*");
		intent.putExtra("scale", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scaleUpIfNeeded", true);
		intent.putExtra("return-data", false);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,outputUri2);
		mContext.startActivityForResult(intent, REQ_INPUT_PHOTO_FOR_DOODLE_CROP);			
	}
}
