package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.*;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.LocationHelper;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.TimerTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 发布动态时：
 * 1）动态文本提交成功后即返回；
 * 2）多媒体附件在后台服务中上传；
 * 3）返回之前，会在本地数据库为多媒体附件创建fake条目，因此可以立即显示完整的动态内容；
 * 4）上传完成后，本地数据库中的 fake multimedia 会被更新；
 */
public class CreateMomentActivity extends Activity implements OnClickListener, InputBoardManager.ChangeToOtherAppsListener {

    private static final int REQ_INPUT_VIDEO_TAKE = 90;
    private static final int REQ_INPUT_VIDEO_PICK = 91;
    private static final int REQ_TAKE_PHOTO = 98;
	private static final int REQ_INPUT_PHOTO = 99;

    private static final int MSG_RESIZE = 102;
    public static final int TOTAL_PHOTO_ALLOWED = 9;

    private final int BIGGER = 103;
    private final int SMALLER = 104;
    public static final int PHOTO_SEND_WIDTH = 600;
    public static final int PHOTO_SEND_HEIGHT = 600;
    private static final int PHOTO_THUMB_WIDTH = 200;
    private static final int PHOTO_THUMB_HEIGHT = 200;

    public final static String EXTRA_KEY_MOMENT_MAX_TIMESTAMP="moment_max_timestamp";
    /** 附带的媒体类型，值为 MEDIA_TYPE_* 常量。 */
    public final static String EXTRA_MEDIA_TYPE = "media_type";
    public final static String EXTRA_KEY_MOMENT_TAG_ID="moment_tag_id";
    public final static String ALIAS_ID_PREFIX="moment_alias_id_";

    /** 媒体类型。*/
    public final static int MEDIA_TYPE_PLAIN_TEXT = 0;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_PIC_ALBUM = 1;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_PIC_CAMERA = 10;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_VIDEO = 2;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_QA = 3;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_VOTE = 4;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_VOICE = 5;
    /** 媒体类型。*/
    public final static int MEDIA_TYPE_LOC = 6;

//    private FrameLayout mFrame;

    private LinearLayout layoutPic;
    private LinearLayout hGridInnerLaout;
    private YQResizeLayout resizeLayout;
	private ImageButton btnTitleBack;
	private ImageButton btnTitleConfirm;
	private EditText edtContent;
    private LinearLayout locationSearchingLayout;
    private TextView txtAddress;
    private ImageView imgNone;
	private Button btnTakePhoto;
	private Button btnPickImage;
    private HorizontalScrollView mGrid;

    private LinearLayout layoutMic;
    private LinearLayout micInfo;
    private TextView micStatus;
    private TimerTextView micTime;
    private ImageView micImage;
    private Button micBtn;
    private LinearLayout micDone;
    private View micPlay;
    private View micDelete;

	private Moment moment = new Moment();
	private MediaInputHelper mediaHelper;
//	private String[] path = new String[2];
    private ArrayList<WMediaFile> listPhoto;

    private MediaRecorder mRecorder;
    private File mLastVoiceFile;
    private MediaPlayer mPlayer;
    private LocationHelper locationHelper;

    private int mediaType;
    private boolean mLocationOn = false;
    private boolean mIsLongClickRecord = false;

    private MessageBox mMsgBox;

    private Database mDb;

    private static CreateMomentActivity instance;

	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch(msg.what) {
            case MSG_RESIZE:
//                if (msg.arg1 == BIGGER) {
//                    mFrame.setVisibility(View.VISIBLE);
//                } else {
//                    mFrame.setVisibility(View.GONE);
//                }
                break;
			default:
				break;
			}
		}
	};

	private void initView() {
        AQuery q = new AQuery(this);

        int[] layoutIds = new int[] {
                R.id.vg_gallery,
                R.id.vg_input_loc,
                R.id.vg_input_pic,
                R.id.vg_input_video,
                R.id.vg_input_voice };
        for (int id : layoutIds) {
            q.find(id).visibility(View.GONE);
        }
        switch (mediaType) {
            case MEDIA_TYPE_PIC_ALBUM:
            case MEDIA_TYPE_PIC_CAMERA:
                q.find(R.id.vg_gallery).visible();
                q.find(R.id.vg_input_pic).visible();
                break;
            case MEDIA_TYPE_VIDEO:
                q.find(R.id.vg_gallery).visible();
                q.find(R.id.vg_input_video).visible();
                break;
            case MEDIA_TYPE_VOICE:
                q.find(R.id.vg_input_voice).visible();
                break;
            case MEDIA_TYPE_LOC:
                q.find(R.id.vg_input_loc).visible();
                break;
        }

        q.find(R.id.btn_take_video).clicked(this);
        q.find(R.id.btn_pick_video).clicked(this);

//        mFrame = (FrameLayout) findViewById(R.id.frame);
        layoutPic = (LinearLayout) findViewById(R.id.pic_layout);
        hGridInnerLaout = (LinearLayout) findViewById(R.id.hgrid_inner_layout);
        resizeLayout = (YQResizeLayout) findViewById(R.id.resizeLayout);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
		edtContent = (EditText) findViewById(R.id.txt_moment_content);
        locationSearchingLayout = (LinearLayout) findViewById(R.id.location_searching_layout);
        txtAddress = (TextView) findViewById(R.id.txt_address);
        imgNone = (ImageView) findViewById(R.id.img_none);
		btnTakePhoto = (Button) findViewById(R.id.btn_take_pic);
		btnPickImage = (Button) findViewById(R.id.btn_pick_photo);
        mGrid = (HorizontalScrollView) findViewById(R.id.mGrid);

        layoutMic = (LinearLayout) findViewById(R.id.mic_layout);
        micInfo = (LinearLayout) findViewById(R.id.mic_info);
        micStatus = (TextView) findViewById(R.id.mic_status);
        micTime = (TimerTextView) findViewById(R.id.mic_time);
        micImage = (ImageView) findViewById(R.id.mic_image);
        micBtn = (Button) findViewById(R.id.mic_btn);
        micDone = (LinearLayout) findViewById(R.id.mic_done);
        micPlay = findViewById(R.id.btn_play);
        micDelete = findViewById(R.id.btn_delete);

		btnTitleBack.setOnClickListener(this);
		btnTitleConfirm.setOnClickListener(this);
		btnTakePhoto.setOnClickListener(this);
		btnPickImage.setOnClickListener(this);
        micBtn.setOnClickListener(this);
        micPlay.setOnClickListener(this);
        micDelete.setOnClickListener(this);
        resizeLayout.setOnResizeListener(new YQResizeLayout.OnResizeListener() {
            @Override
            public void onResize(int w, int h, int oldw, int oldh) {
                Message msg = Message.obtain();
                msg.what = MSG_RESIZE;
                if (h < oldh) {
                    msg.arg1 = SMALLER;
                }
                if (h > oldh && h > ((org.wowtalk.ui.GlobalValue.screenH / 5) * 4)) {
                    msg.arg1 = BIGGER;
                }
                mHandler.sendMessage(msg);
            }
        });

        micBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (startRecording()) {
                    mIsLongClickRecord = true;
                    micImage.setVisibility(View.GONE);
                    micInfo.setVisibility(View.VISIBLE);
                    micStatus.setText(getString(R.string.moments_voice_recording));
                    micBtn.setText(getResources().getString(R.string.msg_release_to_send));
                }
                return false;
            }
        });
        micBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mIsLongClickRecord) {
                            mIsLongClickRecord = false;
                            micBtn.setText(getResources().getString(R.string.moments_voice_hold_to_speak));
                            micStatus.setText(getString(R.string.moments_voice_done));
                            micDone.setVisibility(View.VISIBLE);
                            micBtn.setVisibility(View.GONE);
                            stopRecording();

                            MediaPlayer aPlayer = new MediaPlayer();
                            int duration;
                            try {
                                aPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
                                aPlayer.prepare();
                                duration = aPlayer.getDuration() / 1000;
                            } catch (IOException e) {
                                duration = 0;
                                e.printStackTrace();
                            } finally {
                                aPlayer.release();
                            }

                            micTime.setMaxElapse(duration);
                            micTime.setMaxTime();

                            addVoice2moment();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
	}

    private boolean startRecording() {
        boolean ret=false;


        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
        } else {
            mRecorder.reset();
        }

        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioSamplingRate(16000);
            mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_VOICE, ".m4a");
            mRecorder.setOutputFile(mLastVoiceFile.getAbsolutePath());
            mRecorder.prepare();
            mRecorder.start();

            micTime.reset();
            micTime.start();
            ret=true;
        } catch (Exception e) {
            e.printStackTrace();
            stopRecording();
            if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
                mLastVoiceFile.delete();
            }
            mLastVoiceFile = null;
            mMsgBox.toast(R.string.media_record_not_avaliable);
        }

        return ret;
    }

    private void stopRecording() {
        try {
            if (mRecorder != null) {
                micTime.stop();
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder = null;
        }
    }

    //    private void notifyMicChanged() {
//
//    }

    //hold all bitmap drawable,will be recycled when finish
    private LinkedList<BitmapDrawable> bmpDrawableList=new LinkedList<BitmapDrawable>();
    private void recycleStoredBitmapDrawable() {
        for(BitmapDrawable aBmpDrawable : bmpDrawableList) {
            if(null != aBmpDrawable) {
                Bitmap bmp=aBmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);
            }
        }
        bmpDrawableList.clear();
    }

    private void copyFileForMomentMultimedia(WMediaFile aFile) {
        String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                aFile.fileid, aFile.getExt());
        FileUtils.copyFile(aFile.localPath, destFilePath);

        // make thumbnail
        if (null != aFile.thumb_fileid && null == aFile.localThumbnailPath && aFile.isPhoto) {
            Bitmap thumb = BmpUtils.decodeFile(aFile.localPath, 200, 200, true);
            aFile.localThumbnailPath = PhotoDisplayHelper.makeLocalFilePath(
                    aFile.thumb_fileid, aFile.getExt());
            boolean saved = false;
            OutputStream os = null;
            try {
                os = new FileOutputStream(aFile.localThumbnailPath);
                saved = thumb.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX format should be same with main file?
                os.close();

                BmpUtils.recycleABitmap(thumb);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!saved) {
                aFile.thumb_fileid = aFile.localThumbnailPath = null;
            }
        }
    }

    private void addMedia2moment(WMediaFile aPhoto) {
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(aPhoto.localPath)) {
                Log.w("duplicate photo add 2 momet, omit");
                return;
            }
        }
        aPhoto.setExt(FileUtils.getExt(aPhoto.localPath));
        aPhoto.fileid = String.valueOf(Math.random());
        aPhoto.thumb_fileid = String.valueOf(Math.random());
        aPhoto.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
        moment.multimedias.add(aPhoto);
        copyFileForMomentMultimedia(aPhoto);
    }

    private void removePhotoFromMoment(WMediaFile aPhoto) {
        WFile file2remove=null;
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(aPhoto.localPath)) {
                file2remove=aFile;
                break;
            }
        }

        if(null != file2remove) {
            moment.multimedias.remove(file2remove);
        }
    }

    private void addVoice2moment() {
        if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
            String localFilename = mLastVoiceFile.getAbsolutePath();
            String ext = FileUtils.getExt(localFilename);
            String fakeFileId = String.valueOf(Math.random());
            int duration;
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
                mPlayer.prepare();
                duration = mPlayer.getDuration() / 1000;
            } catch (IOException e) {
                duration = 0;
                e.printStackTrace();
            } finally {
                mPlayer.release();
                mPlayer = null;
            }
            WMediaFile f = new WMediaFile(ext, fakeFileId, duration, localFilename);
            f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
            moment.multimedias.add(f);

            copyFileForMomentMultimedia(f);
        }
    }

    private void removeVoiceFromMoment() {
        WFile file2remove=null;
        String localFilename = mLastVoiceFile.getAbsolutePath();
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(localFilename)) {
                file2remove=aFile;
                break;
            }
        }

        if(null != file2remove) {
            moment.multimedias.remove(file2remove);
        }
    }

    private void notifyFileChanged(boolean isAdded) {
        if ( listPhoto == null || listPhoto.size() == 0) {
            mGrid.setVisibility(View.GONE);
            imgNone.setVisibility(View.VISIBLE);
        } else {
            mGrid.setVisibility(View.VISIBLE);
            imgNone.setVisibility(View.GONE);
        }

        refreshCameraView();

        if (isAdded) {
            final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, hGridInnerLaout, false);
            final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(listPhoto.size() - 1).localThumbnailPath));
            bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

            addMedia2moment(listPhoto.get(listPhoto.size() - 1));
            listPhoto.get(listPhoto.size() - 1).relativeView=view;

            ImageButton imgDelete = (ImageButton) view.findViewById(R.id.btn_delete);
            imgDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAImage(view);
                }
            });
            LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            viewLayoutParams.setMargins(DensityUtil.dip2px(CreateMomentActivity.this, 10), 0, 0, 0);
            view.setTag(listPhoto.get(listPhoto.size() - 1));
            hGridInnerLaout.addView(view, 0);
            ViewGroup.LayoutParams params = hGridInnerLaout.getLayoutParams();
            params.width += imgPhoto.getLayoutParams().width;
            hGridInnerLaout.setLayoutParams(params);
        } else {
            hGridInnerLaout.removeAllViews();
            recycleStoredBitmapDrawable();
            moment.multimedias.clear();

            int fileNum = listPhoto.size();
            for (int i = 0; i < fileNum; i++) {
                final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, hGridInnerLaout, false);
                final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
                imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(i).localThumbnailPath));
                bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

                addMedia2moment(listPhoto.get(i));
                listPhoto.get(i).relativeView=view;

                ImageButton imgDelete = (ImageButton) view.findViewById(R.id.btn_delete);
                imgDelete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAImage(view);
                    }
                });
                LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
                viewLayoutParams.setMargins(DensityUtil.dip2px(CreateMomentActivity.this, 10), 0, 0, 0);
                view.setTag(listPhoto.get(i));
                hGridInnerLaout.addView(view,0);
                ViewGroup.LayoutParams params = hGridInnerLaout.getLayoutParams();
                params.width += imgPhoto.getLayoutParams().width;
                hGridInnerLaout.setLayoutParams(params);
            }
        }
    }

    private void deleteAImage(View view) {
        WMediaFile path = (WMediaFile) view.getTag();
        listPhoto.remove(path);

        removePhotoFromMoment(path);

        recycleAView(view);
        ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
        bmpDrawableList.remove(imgPhoto.getDrawable());

        recycleAView(view);
        notifyFileChanged(false);
    }

    private void recycleAView(View view) {
        ImageView aView=(ImageView) view.findViewById(R.id.img_photo);
        if(null != aView && null != aView.getDrawable()) {
            try {
                BitmapDrawable bmpDrawable=(BitmapDrawable)aView.getDrawable();
                Bitmap bmp=bmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);

                deleteWPhotoFile((WMediaFile)view.getTag());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteWPhotoFile(WMediaFile aWPhoto) {
        if(null != aWPhoto) {
            Database.deleteAFile(aWPhoto.localPath);
        }
    }

    private void releaseMediaFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(null != listPhoto) {
                    for(WMediaFile aWPhoto : listPhoto) {
                        deleteWPhotoFile(aWPhoto);
                    }
                }

                if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
                    Database.deleteAFile(mLastVoiceFile.getAbsolutePath());
                }
            }
        }).start();
    }

    private void createMoment() {
        String content = edtContent.getText().toString();
        if (content.length() > CreateNormalMomentWithTagActivity.MOMENTS_WORDS_OVER) {
            mMsgBox.show(null, getString(R.string.moments_words_over_failed));
            return;
        }
        moment.text = content;
        if (Utils.isNullOrEmpty(moment.text)
                && (listPhoto == null || listPhoto.isEmpty())
                && (mLastVoiceFile == null || !mLastVoiceFile.exists())) {
            mMsgBox.show(null, getString(
                    R.string.settings_account_moment_text_cannot_be_empty));
        } else {
            //store local moment
            mMsgBox.showWait();
            new AsyncTask<Void, Integer, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    String uid = PrefUtil.getInstance(CreateMomentActivity.this).getUid();
                    //alias id and timestamp,timestamp should be the largest
                    //will be updated when returned by server
                    moment.id = ALIAS_ID_PREFIX+System.currentTimeMillis();
                    moment.timestamp = getIntent().getLongExtra(EXTRA_KEY_MOMENT_MAX_TIMESTAMP,0)+1;
                    Log.w("local moment timestamp set to "+moment.timestamp);
                    if (null == moment.owner)
                        moment.owner = new Buddy();
                    moment.owner.userID = uid;
                    moment.likedByMe = false;
                    mDb.storeMoment(moment,null);
                    for (WFile f : moment.multimedias) {
                        mDb.storeMultimedia(moment, f);
                    }

                    Intent data = new Intent();
                    setResult(RESULT_OK, data);

                    //upload to server
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int errno = WowMomentWebServerIF.getInstance(CreateMomentActivity.this).fAddMoment(moment);
                            if (errno == ErrorCode.OK) {
                                Intent intent = new Intent(CreateMomentActivity.this, DownloadingAndUploadingService.class);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_ACTION,
                                        DownloadingAndUploadingService.ACTION_UPLOAD_MOMENT_FILE);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_MOMENT_ID, moment.id);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_WFILES, moment.multimedias);
                                startService(intent);
                            }
                        }
                    }).start();
                    return 0;
                }

                @Override
                protected void onPostExecute(Integer errno) {
                    mMsgBox.dismissWait();
                    finish();
                }
            }.execute((Void)null);

//            mMsgBox.showWait();
//
//            new AsyncTask<Void, Integer, Integer>() {
//                @Override
//                protected Integer doInBackground(Void... params) {
//                    int errno = WowMomentWebServerIF.getInstance(CreateMomentActivity.this).fAddMoment(moment);
//                    if (errno == ErrorCode.OK) {
//                        updateMultimedia();
//                    }
//                    return errno;
//                }
//
//                @Override
//                protected void onPostExecute(Integer errno) {
//                    mMsgBox.dismissWait();
//                    mMsgBox.dismissToast(); // displayed by updateMultimedia();
//                    if (errno == ErrorCode.OK) {
//                        Intent data = new Intent();
////                        data.putExtra("moment", moment);
//                        setResult(RESULT_OK, data);
//                        finish();
//                    } else {
//                        mMsgBox.show(null, getString(R.string.operation_failed));
//                    }
//                }
//            }.execute((Void)null);
		}
	}

//	private void updateMultimedia() {
//
//        //
//        // store files into local db, get localDbId.
//        // In order to display the newly created moment immediately and correctly,
//        // a fake file ID for each photo is required.
//        //
//
////        if (moment.multimedias == null)
////            moment.multimedias = new ArrayList<WFile>();
//
////        if (listPhoto != null && !listPhoto.isEmpty()) {
////            for (WPhoto file : listPhoto) {
////                String ext = FileUtils.getExt(file.localPath);
////                String fakeFileId = String.valueOf(Math.random());
////                WFile f = new WFile(ext, fakeFileId, null, file.localPath);
////                f.thumb_fileid = String.valueOf(Math.random());
////                f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
////                moment.multimedias.add(f);
////            }
////        }
//
////        if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
////            String localFilename = mLastVoiceFile.getAbsolutePath();
////            String ext = FileUtils.getExt(localFilename);
////            String fakeFileId = String.valueOf(Math.random());
////            int duration;
////            mPlayer = new MediaPlayer();
////            try {
////                mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
////                mPlayer.prepare();
////                duration = mPlayer.getDuration() / 1000;
////            } catch (IOException e) {
////                duration = 0;
////                e.printStackTrace();
////            } finally {
////                mPlayer.release();
////                mPlayer = null;
////            }
////            WFile f = new WFile(ext, fakeFileId, duration, localFilename);
////            f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
////            moment.multimedias.add(f);
////        }
//
////        if (moment.multimedias.isEmpty()) return;
////
////        // copy src file to image cache dir
////        for (WFile f : moment.multimedias) {
//////            String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
//////                    f.fileid, f.getExt());
//////            FileUtils.copyFile(f.localPath, destFilePath);
//////
//////            // make thumbnail
//////            if (null != f.thumb_fileid) {
//////                Bitmap thumb = BmpUtils.decodeFile(f.localPath, 200, 200, true);
//////                f.localThumbnailPath = PhotoDisplayHelper.makeLocalFilePath(
//////                        f.thumb_fileid, f.getExt());
//////                boolean saved = false;
//////                OutputStream os = null;
//////                try {
//////                    os = new FileOutputStream(f.localThumbnailPath);
//////                    saved = thumb.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX format should be same with main file?
//////                    os.close();
//////
//////                    BmpUtils.recycleABitmap(thumb);
//////                } catch (Exception e) {
//////                    e.printStackTrace();
//////                }
//////
//////                if (!saved) {
//////                    f.thumb_fileid = f.localThumbnailPath = null;
//////                }
//////            }
////
////            mDb.storeMultimedia(moment, f);
////        }
//
//        //
//        // upload files
//        //
//        Intent intent = new Intent(this, DownloadingAndUploadingService.class);
//        intent.putExtra(DownloadingAndUploadingService.EXTRA_ACTION,
//                DownloadingAndUploadingService.ACTION_UPLOAD_MOMENT_FILE);
//        intent.putExtra(DownloadingAndUploadingService.EXTRA_MOMENT_ID, moment.id);
//        intent.putExtra(DownloadingAndUploadingService.EXTRA_WFILES, moment.multimedias);
//        startService(intent);
//	}

    private void pickPhoto() {
        Intent intent = new Intent(CreateMomentActivity.this, SelectPhotoActivity.class);
        int i = 0;
        for (WMediaFile photo : listPhoto) {
            if (!photo.isFromGallery) {
                i++;
            }
        }
        intent.putExtra("num", TOTAL_PHOTO_ALLOWED - i);
        ArrayList<String> listPath = new ArrayList<String>();
        for (WMediaFile photo : listPhoto) {
            if (photo.isFromGallery) {
                listPath.add(photo.galleryPath);
            }
        }
        intent.putStringArrayListExtra("list", listPath);
        startActivityForResult(intent, REQ_INPUT_PHOTO);
    }

	private void refreshCameraView() {
	}
	
	private void refreshLocationView() {
	}

    @Override
	public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
                releaseMediaFiles();
                finish();
                break;
            case R.id.title_confirm:
                createMoment();
                break;
            case R.id.mic_btn:
                break;
            case R.id.btn_play:
                playMicVoice();
//            mPlayer = new MediaPlayer();
//            try {
//                mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
//                mPlayer.prepare();
//                mPlayer.start();
//                micTime.reset();
//                micTime.start();
//                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                    @Override
//                    public void onCompletion(MediaPlayer mediaPlayer) {
//                        micTime.stop();
//                    }
//                });
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
                break;
            case R.id.btn_delete:
                // stop the playing media.
//            if (null != mPlayer && mPlayer.isPlaying()) {
//                mPlayer.stop();
//                mPlayer.release();
//            }
                micTime.setMaxElapse(-1);
                stopMicVoice();
                removeVoiceFromMoment();
                if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
                    mLastVoiceFile.delete();
                    mLastVoiceFile = null;
                }
                micInfo.setVisibility(View.GONE);
                micImage.setVisibility(View.VISIBLE);
                micBtn.setVisibility(View.VISIBLE);
                micDone.setVisibility(View.GONE);
                break;
            case R.id.btn_take_pic:
                if (listPhoto.size() == TOTAL_PHOTO_ALLOWED) {
                    mMsgBox.toast(String.format(getString(R.string.settings_account_moment_take_photos_oom), TOTAL_PHOTO_ALLOWED));
                    return;
                }
                mediaHelper.takePhoto(CreateMomentActivity.this, REQ_TAKE_PHOTO);
                break;
            case R.id.btn_pick_photo:
                pickPhoto();
                break;
            case R.id.btn_take_video:
                mediaHelper.takeVideo(this, REQ_INPUT_VIDEO_TAKE);
                break;
            case R.id.btn_pick_video:
                mediaHelper.pickVideo(this, REQ_INPUT_VIDEO_PICK);
                break;
            default:
                break;
        }
    }

    private void requireLoc() {
        mLocationOn = !mLocationOn;
        refreshLocationView();
        if (mLocationOn) {
//                getLocation();
            locationSearchingLayout.setVisibility(View.VISIBLE);
            txtAddress.setVisibility(View.GONE);
            locationHelper.getLocationWithAMap(true);
        } else {
            locationSearchingLayout.setVisibility(View.GONE);
            txtAddress.setVisibility(View.GONE);
        }
    }

    private void stopMicVoice() {
        if (null != mPlayer) {
            micTime.stop();
            micTime.setMaxTime();
            mPlayer.release();
            mPlayer=null;
        }
    }

    private void playMicVoice() {
        if(null != mPlayer) {
            Log.w("playing mic voice not null");
            return;
        }
        mPlayer = new MediaPlayer();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
                    mPlayer.prepare();
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {

                            stopMicVoice();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void errno) {
                micTime.reset();
                micTime.start();
                mPlayer.start();
            }
        }.execute((Void) null);
    }

    @Override
    public void onStop() {
        super.onStop();

        locationHelper.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        recycleStoredBitmapDrawable();
        Log.w("create moment destroyed");
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_trend);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        moment.multimedias = new ArrayList<WFile>();

        mMsgBox = new MessageBox(this);
        mDb = new Database(this);

        getData(savedInstanceState == null ? getIntent().getExtras() : savedInstanceState);

		initView();

        locationHelper = new LocationHelper(this);
        locationHelper.setOnLocationGotListener(new LocationHelper.OnLocationGotListener() {
            @Override
            public void onLocationGot(Location location, String strAddress) {
                locationSearchingLayout.setVisibility(View.GONE);
                txtAddress.setVisibility(View.VISIBLE);
                txtAddress.setText(strAddress);
                moment.place = strAddress;
                if (null != location) {
                    moment.latitude = location.getLatitude();
                    moment.longitude = location.getLongitude();
                }
            }

            @Override
            public void onNoLocationGot() {
                Log.w("no location got");
                mLocationOn = false;
                moment.place = null;
                moment.latitude = moment.longitude = 0;
                locationSearchingLayout.setVisibility(View.GONE);
                txtAddress.setVisibility(View.GONE);
                refreshLocationView();
                mMsgBox.toast(R.string.moments_create_obtain_location_failed);
            }
        });

        instance=this;


        //store backed state
        if(null != savedInstanceState) {
            mediaHelper=savedInstanceState.getParcelable("media_helper");
            listPhoto=savedInstanceState.getParcelableArrayList("list_photo");

            try {
                String lastVoiceFilePath=savedInstanceState.getString("last_voice_file");
                if(!TextUtils.isEmpty(lastVoiceFilePath)) {
                    File aFile=new File(lastVoiceFilePath);
                    if(null != aFile && aFile.exists()) {
                        mLastVoiceFile=aFile;
                        micTime.setText("00:00");
                    }
                }

                mLocationOn=savedInstanceState.getBoolean("location_on");
                if(mLocationOn) {
                    String strAddress=savedInstanceState.getString("location_address");
                    if(!TextUtils.isEmpty(strAddress)) {
                        txtAddress.setText(strAddress);
                        moment.place = strAddress;

                        locationSearchingLayout.setVisibility(View.GONE);
                        txtAddress.setVisibility(View.VISIBLE);

                        refreshLocationView();
                    } else {
                        mLocationOn=false;
                        Log.w("mLocationOn on but address null");
                    }

                    moment.latitude = savedInstanceState.getDouble("location_latitude");
                    moment.longitude = savedInstanceState.getDouble("location_longitude");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            notifyFileChanged(false);
        } else {
            mediaHelper = new MediaInputHelper(this);
            listPhoto = new ArrayList<WMediaFile>();
        }
	}

    private void getData(Bundle bundle) {
        if (bundle != null) {
            mediaType = bundle.getInt(EXTRA_MEDIA_TYPE, MEDIA_TYPE_PLAIN_TEXT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("media_helper", mediaHelper);
        outState.putParcelableArrayList("list_photo",listPhoto);
        outState.putInt(EXTRA_MEDIA_TYPE, mediaType);

        if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
            outState.putString("last_voice_file",mLastVoiceFile.getAbsolutePath());
        }

        if(mLocationOn) {
            outState.putBoolean("location_on",mLocationOn);
            outState.putString("location_address",moment.place);
            if(0 != moment.latitude || 0 != moment.longitude) {
                outState.putDouble("location_latitude",moment.latitude);
                outState.putDouble("location_longitude",moment.longitude);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);

        stopMicVoice();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
            case REQ_INPUT_PHOTO:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> listPath = data.getStringArrayListExtra("list");
                    ArrayList<WMediaFile> photos = new ArrayList<WMediaFile>();
                    ArrayList<WMediaFile> photo2del = new ArrayList<WMediaFile>();
                    for (int i = 0; i < listPhoto.size(); i++) {
                        boolean needAdd=false;
                        if (!listPhoto.get(i).isFromGallery) {
                            needAdd=true;
                        } else {
                            if(listPath.contains(listPhoto.get(i).galleryPath)) {
                                listPath.remove(listPhoto.get(i).galleryPath);
                                needAdd=true;
                            } else {
                                //not contained,delete this
                                photo2del.add(listPhoto.get(i));
                            }
                        }

                        if(needAdd) {
                            photos.add(listPhoto.get(i));
                        }
                    }

                    for(WMediaFile aPhoto : photo2del) {
                        deleteAImage(aPhoto.relativeView);
                    }
                    listPhoto = photos;
                    mMsgBox.showWait();
                    new AsyncTask<ArrayList<String>, Void, Void>() {
                        @Override
                        protected Void doInBackground(ArrayList<String>... params) {
                            for (String path : params[0]) {
                                WMediaFile photo = new WMediaFile(true);
                                Bitmap bmp = BmpUtils.decodeFile(path, PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT);
                                File file = MediaInputHelper.makeOutputMediaFile(
                                        MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
                                try {
                                    OutputStream os = new FileOutputStream(file);
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
                                    os.close();

                                    BmpUtils.recycleABitmap(bmp);

                                    photo.localPath = photo.localThumbnailPath = file.getAbsolutePath();
                                    photo.galleryPath = path;
                                    photo.isFromGallery = true;
                                    listPhoto.add(photo);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                publishProgress((Void)null);
                            }
                            return null;
                        }

                        @Override
                        protected void onProgressUpdate(Void... errno) {
                            instance.notifyFileChanged(true);
                        }

                        @Override
                        protected void onPostExecute(Void errno) {
                            mMsgBox.dismissWait();
                            instance.notifyFileChanged(false);
                        }
                    }.execute(listPath);
                }
                break;
            case REQ_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
//                layoutPic.setVisibility(View.VISIBLE);

                    new AsyncTask<Intent, Void, Void>() {
                        @Override
                        protected Void doInBackground(Intent... params) {
                            String[] path = new String[2];
                            boolean handleImageRet=mediaHelper.handleImageResult(CreateMomentActivity.this, params[0],
                                    PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                                    PHOTO_THUMB_WIDTH, PHOTO_THUMB_HEIGHT,
                                    path);
                            if(handleImageRet) {
                                Log.i("handle result ok,path[0]="+path[0]);
                            } else {
                                Log.e("handle image error");
                            }
                            WMediaFile photo = new WMediaFile(true);
                            photo.localPath = path[0];
                            photo.localThumbnailPath = path[1];
                            photo.isFromGallery = false;
                            listPhoto.add(photo);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void errno) {
                            instance.notifyFileChanged(true);
                        }
                    }.execute(data);
                }
                break;
            case REQ_INPUT_VIDEO_PICK:
                if (resultCode == RESULT_OK) {
                    handleVideoResult(data, true);
                }
                break;
            case REQ_INPUT_VIDEO_TAKE:
                if (resultCode == RESULT_OK) {
                    handleVideoResult(data, false);
                }
                break;
            default:
                break;
        }
	}

    private void handleVideoResult(Intent data, final boolean isFromGallery) {
        new AsyncTask<Intent, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Intent... params) {
                String[] path = new String[2];
                boolean handleRet=mediaHelper.handleVideoResult(CreateMomentActivity.this, params[0],
                        PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                        PHOTO_THUMB_WIDTH, PHOTO_THUMB_HEIGHT,
                        path);
                if(handleRet) {
                    Log.i("handle result ok,path[0]=" + path[0]);
                } else {
                    Log.e("handle video error");
                }
                if (new File(path[0]).exists()) {
                    WMediaFile photo = new WMediaFile();
                    photo.localPath = path[0];
                    photo.localThumbnailPath = path[1];
                    photo.isFromGallery = isFromGallery;
                    listPhoto.add(photo);
                    return true;
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean ok) {
                if (ok)
                    instance.notifyFileChanged(true);
                else
                    new MessageBox(CreateMomentActivity.this).toast(R.string.msg_operation_failed);
            }
        }.execute(data);
    }

    public static class WMediaFile extends WFile {
        public boolean isPhoto = false;
        boolean isFromGallery;
        String galleryPath;

        View relativeView;

        public WMediaFile() {
            isPhoto = false;
        }

        public WMediaFile(boolean photo) {
            isPhoto = photo;
        }

        public WMediaFile(String ext, String fileid, String thumb_fileid, String localPath) {
            super(ext, fileid, thumb_fileid, localPath);
        }

        public WMediaFile(String ext, String fileid, int duration, String localPath) {
            super(ext, fileid, duration, localPath);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(isFromGallery ? 1 : 0);
            parcel.writeString(galleryPath);
            parcel.writeInt(isPhoto ? 1 : 0);
        }

        @Override
        protected void loadFromParcel(Parcel parcel) {
            super.loadFromParcel(parcel);
            isFromGallery = parcel.readInt() == 1;
            galleryPath = parcel.readString();
            isPhoto = parcel.readInt() == 1;
        }
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }
}
