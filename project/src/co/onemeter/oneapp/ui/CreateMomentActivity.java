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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
 * @deprecated use {@link co.onemeter.oneapp.ui.CreateNormalMomentWithTagActivity} instead.
 * 发布动态时：
 * 1）动态文本提交成功后即返回；
 * 2）多媒体附件在后台服务中上传；
 * 3）返回之前，会在本地数据库为多媒体附件创建fake条目，因此可以立即显示完整的动态内容；
 * 4）上传完成后，本地数据库中的 fake multimedia 会被更新；
 */
public class CreateMomentActivity extends Activity implements OnClickListener, InputBoardManager.ChangeToOtherAppsListener {

    private static final int MSG_RESIZE = 102;
    public static final int TOTAL_PHOTO_ALLOWED = 9;
    private static final int REQ_IMAGE = 123;

    private final int BIGGER = 103;
    private final int SMALLER = 104;
    public static final int PHOTO_SEND_WIDTH = 600;
    public static final int PHOTO_SEND_HEIGHT = 600;

    public final static String EXTRA_KEY_MOMENT_MAX_TIMESTAMP="moment_max_timestamp";
    public final static String EXTRA_PAGE_TITLE = "pagetitle";
    /** moment tag, refer to {@link Moment#SERVER_MOMENT_TAG_FOR_QA} */
    public final static String EXTRA_SERVER_MOMENT_TAG = "category";
    /** 允许附带的媒体类型，值为 MEDIA_FLAG_* 常量的位组合。 */
    public final static String EXTRA_MEDIA_FLAGS = "media_type";
    public final static String EXTRA_KEY_MOMENT_TAG_ID="moment_tag_id";
    public final static String ALIAS_ID_PREFIX="moment_alias_id_";

    /** 媒体类型。*/
    public final static int MEDIA_FLAG_PLAIN_TEXT = 1;
    /** 媒体类型。*/
    public final static int MEDIA_FLAG_IMAGE = 2;
    /** 媒体类型。*/
    public final static int MEDIA_FLAG_VIDEO = 4;
    /** 媒体类型。*/
    public final static int MEDIA_FLAG_VOICE = 8;
    /** 媒体类型。*/
    public final static int MEDIA_FLAG_LOC = 16;
    /** 媒体类型。*/
    public final static int MEDIA_FLAG_ALL = 0xFFFFFFFF;

//    private FrameLayout mFrame;

    private YQResizeLayout resizeLayout;
	private ImageButton btnTitleBack;
	private ImageButton btnTitleConfirm;
	private EditText edtContent;

    private TextView btnVoiceRecord;
    private TimerTextView btnVoicePreview;
    private View btnVoiceDel;

	private Moment moment = new Moment();
//	private String[] path = new String[2];
    private ArrayList<WMediaFile> listPhoto;

    private ImageVideoInputWidget imageInputWidget;
    private MediaRecorder mRecorder;
    private File mLastVoiceFile;
    private MediaPlayer mPlayer;
    private LocationHelper locationHelper;

    private int mediaFlag;
    private String pageTitle;
    private boolean mLocationOn = false;
    private boolean mIsRecordingVoice = false;
    private Location lastLoc;

    private MessageBox mMsgBox;

    private Database mDb;

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

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        new AQuery(this).find(R.id.title_text).text(pageTitle);
    }

	private void initView() {
        AQuery q = new AQuery(this);

        if (!TextUtils.isEmpty(pageTitle))
            setTitle(pageTitle);

        int[] layoutIds = new int[] {
                R.id.vg_input_loc,
                R.id.vg_input_imagevideo,
                R.id.vg_input_voice };
        for (int id : layoutIds) {
            q.find(id).visibility(View.GONE);
        }
        if ((mediaFlag & MEDIA_FLAG_IMAGE) != 0) {
            q.find(R.id.vg_input_imagevideo).visible();
        }
        if ((mediaFlag & MEDIA_FLAG_VIDEO) != 0) {
            q.find(R.id.vg_input_imagevideo).visible();
        }
        if ((mediaFlag & MEDIA_FLAG_VOICE) != 0) {
            q.find(R.id.vg_input_voice).visible();
        }
        if ((mediaFlag & MEDIA_FLAG_LOC) != 0) {
            q.find(R.id.vg_input_loc).visible();
        }

        btnVoiceRecord = q.find(R.id.btn_voice_record).clicked(this).getTextView();
        btnVoicePreview = (TimerTextView) q.find(R.id.btn_voice_preview).clicked(this).visibility(View.GONE).getView();
        btnVoiceDel = q.find(R.id.btn_voice_del).clicked(this).visibility(View.GONE).getView();
        imageInputWidget = (ImageVideoInputWidget) q.find(R.id.vg_input_imagevideo).getView();
        q.find(R.id.btn_loc).clicked(this);

        imageInputWidget.setup(this, ImageVideoInputWidget.MediaType.Photo, REQ_IMAGE);

//        mFrame = (FrameLayout) findViewById(R.id.frame);
        resizeLayout = (YQResizeLayout) findViewById(R.id.resizeLayout);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
		edtContent = (EditText) findViewById(R.id.txt_moment_content);

		btnTitleBack.setOnClickListener(this);
		btnTitleConfirm.setOnClickListener(this);
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

            btnVoicePreview.reset();
            btnVoicePreview.start();
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
                btnVoicePreview.stop();
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;

                btnVoiceRecord.setVisibility(View.GONE);
                btnVoicePreview.setVisibility(View.VISIBLE);
                btnVoiceDel.setVisibility(View.VISIBLE);

                btnVoiceRecord.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.timeline_record), null, null, null);
                btnVoiceRecord.setText(getResources().getString(R.string.moment_add_touch_to_record));

                int duration = getVoiceDuration();

                btnVoicePreview.setMaxElapse(duration);
                btnVoicePreview.setMaxTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder = null;
        }
    }

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

    private void updateData() {
        moment.text = edtContent.getText().toString();

        moment.multimedias = new ArrayList<WFile>(imageInputWidget.getItemCount());
        for (int i = 0; i < imageInputWidget.getItemCount(); ++i) {
            moment.multimedias.add(imageInputWidget.getItem(i));
        }

        if (!mLocationOn) {
            moment.place = null;
            moment.latitude = moment.longitude = 0;
        }
    }

    private void createMoment() {
        updateData();

        if (moment.text.length() > CreateNormalMomentWithTagActivity.MOMENTS_WORDS_OVER) {
            mMsgBox.show(null, getString(R.string.moments_words_over_failed));
            return;
        }
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
		}
	}

    private void refreshLocationView() {
        TextView v = new AQuery(this).find(R.id.btn_loc).getTextView();
        if (mLocationOn) {
            if (lastLoc != null) {
                // got location
                v.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.timeline_location_a),
                        null, null, null);
                v.setText(moment.place);
            } else {
                // locating
                v.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.timeline_location),
                        null, null, null);
                v.setText(R.string.moment_add_locating);
            }
        } else {
            v.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.timeline_location),
                    null, null, null);
            v.setText(R.string.moment_add_touch_to_loc);
        }
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
            case R.id.btn_voice_record: {
                if (!mIsRecordingVoice) { // start record
                    if (startRecording()) {
                        mIsRecordingVoice = true;
                        btnVoiceRecord.setCompoundDrawablesWithIntrinsicBounds(
                                getResources().getDrawable(R.drawable.timeline_record_a), null, null, null);
                        btnVoiceRecord.setText(getResources().getString(R.string.moment_add_touch_to_stop_record));
                    }
                } else { // stop record
                    mIsRecordingVoice = false;
                    stopRecording();
                    addVoice2moment();
                }
                break;
            }
            case R.id.btn_voice_preview:
                playMicVoice();
                break;
            case R.id.btn_voice_del: {
                btnVoiceRecord.setVisibility(View.VISIBLE);
                btnVoicePreview.setVisibility(View.GONE);
                btnVoiceDel.setVisibility(View.GONE);
                btnVoicePreview.setMaxElapse(-1);
                stopMicVoice();
                removeVoiceFromMoment();
                if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
                    mLastVoiceFile.delete();
                    mLastVoiceFile = null;
                }
                break;
            }
            case R.id.btn_loc:
                toggleLocation();
                break;
            default:
                break;
        }
    }

    private int getVoiceDuration() {
        int duration;
        MediaPlayer aPlayer = new MediaPlayer();
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
        return duration;
    }

    private void toggleLocation() {
        mLocationOn = !mLocationOn;
        refreshLocationView();
        if (mLocationOn) {
            locationHelper.getLocationWithAMap(true);
        } else {
        }
    }

    private void stopMicVoice() {
        if (null != mPlayer) {
            btnVoicePreview.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.timeline_play),
                    null, null, null);
            btnVoicePreview.stop();
            btnVoicePreview.setMaxTime();
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
                btnVoicePreview.reset();
                btnVoicePreview.start();
                btnVoicePreview.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.timeline_stop),
                        null, null, null);
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
		setContentView(R.layout.create_moment);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        moment.multimedias = new ArrayList<WFile>();

        mMsgBox = new MessageBox(this);
        mDb = new Database(this);

        getData(savedInstanceState == null ? getIntent().getExtras() : savedInstanceState);

		initView();

        if (savedInstanceState != null) {
            imageInputWidget.restoreInstanceState(savedInstanceState);
        }

        locationHelper = new LocationHelper(this);
        locationHelper.setOnLocationGotListener(new LocationHelper.OnLocationGotListener() {
            @Override
            public void onLocationGot(Location location, String strAddress) {
                moment.place = strAddress;
                if (null != location) {
                    lastLoc = location;
                    moment.latitude = location.getLatitude();
                    moment.longitude = location.getLongitude();
                }
                refreshLocationView();
            }

            @Override
            public void onNoLocationGot() {
                Log.w("no location got");
                mLocationOn = false;
                moment.place = null;
                moment.latitude = moment.longitude = 0;
                refreshLocationView();
                mMsgBox.toast(R.string.moments_create_obtain_location_failed);
            }
        });


        //store backed state
        if(null != savedInstanceState) {
            listPhoto=savedInstanceState.getParcelableArrayList("list_photo");

            try {
                String lastVoiceFilePath=savedInstanceState.getString("last_voice_file");
                if(!TextUtils.isEmpty(lastVoiceFilePath)) {
                    File aFile=new File(lastVoiceFilePath);
                    if(null != aFile && aFile.exists()) {
                        mLastVoiceFile=aFile;
                        btnVoicePreview.setText("00:00");
                    }
                }

                mLocationOn=savedInstanceState.getBoolean("location_on");
                if(mLocationOn) {
                    String strAddress=savedInstanceState.getString("location_address");
                    if(!TextUtils.isEmpty(strAddress)) {
                        moment.place = strAddress;


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
        } else {
            listPhoto = new ArrayList<WMediaFile>();
        }
	}

    private void getData(Bundle bundle) {
        if (bundle != null) {
            mediaFlag = bundle.getInt(EXTRA_MEDIA_FLAGS, MEDIA_FLAG_PLAIN_TEXT);
            moment.tag = bundle.getString(EXTRA_SERVER_MOMENT_TAG);
            pageTitle = bundle.getString(EXTRA_PAGE_TITLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        imageInputWidget.saveInstanceState(outState);

        outState.putParcelableArrayList("list_photo",listPhoto);
        outState.putInt(EXTRA_MEDIA_FLAGS, mediaFlag);

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
            case REQ_IMAGE:
                if (resultCode == RESULT_OK) {
                    imageInputWidget.handleActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                break;
        }
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

        public static Creator<WMediaFile> CREATOR = new Creator<WMediaFile>() {
            @Override
            public WMediaFile createFromParcel(Parcel parcel) {
                WMediaFile f = new WMediaFile();
                f.loadFromParcel(parcel);
                return f;
            }

            @Override
            public WMediaFile[] newArray(int i) {
                return new WMediaFile[i];
            }
        };
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }
}
