package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.LocationHelper;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 图文音混合消息编辑器。
 */
public class HybirdMessageEditor extends Activity implements View.OnClickListener,
        InputBoardManager.ChangeToOtherAppsListener {

    /** 页面标题。 */
    public static final String EXTRA_PAGE_TITLE = "page_title";
    /** 文本框提示文本。 */
    public static final String EXTRA_TEXT_HINT = "text_hint";
    /** 最多允许添加的图片数量。 */
    public static final String EXTRA_IMAGE_MAX_COUNT = "image_max_count";
    /** 是否允许添加语音？ */
    public static final String EXTRA_ALLOW_VOICE = "allow_voice";
    /** 是否允许添加位置？ */
    public static final String EXTRA_ALLOW_LOC = "allow_loc";

    /** 是否必须输入文本？ */
    public static final String EXTRA_REQUIRE_TEXT = "require_text";
    /** 是否必须输入图片？ */
    public static final String EXTRA_REQUIRE_IMAGE = "require_image";
    /** 是否必须输入语音？ */
    public static final String EXTRA_REQUIRE_VOICE = "require_image";
    /** 是否必须添加位置？ */
    public static final String EXTRA_REQUIRE_LOC = "require_loc";

    /** 输出图片绝对路径。 这是一个数组，[2*i] 表示第 i 个原图，[2*i+1] 表示第 i 个缩略图。*/
    public static final String EXTRA_OUT_IMAGE_FILENAME = "out_image_filename";
    /** 输出语音绝对路径。 */
    public static final String EXTRA_OUT_VOICE_FILENAME = "out_voice_filename";
    /** 输出语音时长（秒）。 */
    public static final String EXTRA_OUT_VOICE_DURATION = "out_voice_duration";
    /** 输出消息文本。 */
    public static final String EXTRA_OUT_TEXT = "out_text";
    /** 输出地理位置-纬度。 */
    public static final String EXTRA_OUT_LOC_LATITUDE = "out_loc_lat";
    /** 输出地理位置-经度。 */
    public static final String EXTRA_OUT_LOC_LONGITUDE = "out_loc_lon";
    /** 输出地理位置-地址。 */
    public static final String EXTRA_OUT_LOC_ADDR = "out_loc_addr";

    public static final int MOMENTS_WORDS_OVER = 600;

    private MediaInputHelper mediaHelper;
    private MessageBox mMsgBox;

    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA = 1;
    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY = 2;

    // save text & location
    private Moment moment = new Moment();
    // save audio
    private File mLastVoiceFile;
    // save images
    private ArrayList<CreateMomentActivity.WMediaFile> listPhoto;

    private static HybirdMessageEditor instance;

    private LinearLayout addedImgLayout;


    private HorizontalScrollView hsvImgList;
    private boolean isCapturingVoice = false;
    private ImageView ivCaptureInnerInd;
    private TextView tvCaptureInnerInd;

    private ImageView ivReadyCaptureVoicePlay;
    private TextView tvReadyCaptureVoiceTimeLength;

    private ImageView ivPickLocationImgInd;
    private TextView tvPickLocationTxtInd;

    private MediaRecorder mMediaRecorder;
    //    private MediaPlayer mPlayer;
    private TimeElapseReportRunnable timeElapseReportForCaptureVoiceRunnable;

    private LocationHelper locationHelper;
    private boolean isGettingLocation = false;
    private boolean isLocGot = false;

    private EditText etMomentMsgContent;

    private MediaPlayerWraper mediaPlayerWraper;

    // inputted extras
    private boolean requireText;
    private boolean requireImage;
    private boolean requireVoice;
    private boolean requireLoc;
    private int imageMaxCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybird_message_editor);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        if (savedInstanceState != null) {
            getExtras(savedInstanceState);
        } else {
            getExtras(getIntent().getExtras());
        }

        initView(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlayingVoice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationHelper.setOnLocationGotListener(null);
        locationHelper.stop();
        recycleStoredBitmapDrawable();

        if (null != mMediaRecorder) {
            stopRecording();
        }

        Log.w("create moment destroyed");
    }

    private boolean isContentValid() {
        if (requireText && Utils.isNullOrEmpty(etMomentMsgContent.getText().toString()))
            return false;
        if (requireImage && (listPhoto == null || listPhoto.isEmpty()))
            return false;
        if (requireVoice && (mLastVoiceFile == null || !mLastVoiceFile.exists()))
            return false;
        if (requireVoice && (null != mMediaRecorder && voiceDuration < MOMENT_VOICE_MIN_LEN_IN_MS))
            return false;
        if (requireLoc && !isLocGot)
            return false;

        return true;
    }

    private boolean isGetingLocation() {
        return isGettingLocation;
    }

    private void checkRightBtnStatus() {
        View rightConfirm = findViewById(R.id.btn_commit);

        if (isContentValid() && !isGetingLocation()) {
            rightConfirm.setEnabled(true);
        } else {
            rightConfirm.setEnabled(false);
        }
    }

    private void updateLocation(double latitude, double longitude, String strAddress) {
        moment.place = strAddress;
        moment.latitude = latitude;
        moment.longitude = longitude;

        tvPickLocationTxtInd.setText(String.format(getString(R.string.event_place_label), strAddress));
        ivPickLocationImgInd.setImageResource(R.drawable.timeline_location_a);
        findViewById(R.id.pick_location_layout).setBackgroundResource(R.drawable.text_field);
        findViewById(R.id.pick_location_delete).setVisibility(View.VISIBLE);

        isGettingLocation = false;
        isLocGot = true;
    }

    LocationHelper.OnLocationGotListener mOnLocationGotListener = new LocationHelper.OnLocationGotListener() {
        @Override
        public void onLocationGot(Location location, String strAddress) {
            try {
                updateLocation(location.getLatitude(), location.getLongitude(), strAddress);
                locationHelper.setOnLocationGotListener(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNoLocationGot() {
            moment.place = null;
            moment.latitude = moment.longitude = 0;
            mMsgBox.toast(R.string.moments_create_obtain_location_failed);

            tvPickLocationTxtInd.setText(R.string.place);
            isGettingLocation = false;
            locationHelper.setOnLocationGotListener(null);
        }
    };

    private void initView(Bundle savedInstanceState) {
        instance = this;

        mediaPlayerWraper = new MediaPlayerWraper(this);

        locationHelper = new LocationHelper(this);

        etMomentMsgContent = (EditText) findViewById(R.id.edt_moment_content);
        addedImgLayout = (LinearLayout) findViewById(R.id.added_images_layout);
        hsvImgList = (HorizontalScrollView) findViewById(R.id.hsv_img_list);

        mMsgBox = new MessageBox(this);


        Bundle bundle = getIntent().getExtras();

        setTitle(bundle.getString(EXTRA_PAGE_TITLE));
        etMomentMsgContent.setHint(bundle.getString(EXTRA_TEXT_HINT));

        findViewById(R.id.title_back).setOnClickListener(this);
        findViewById(R.id.btn_commit).setOnClickListener(this);
        findViewById(R.id.trigger_add_img_layout).setOnClickListener(this);


        ivCaptureInnerInd = (ImageView) findViewById(R.id.capture_inner_img_ind);
        tvCaptureInnerInd = (TextView) findViewById(R.id.capture_inner_txt_ind);

        ivReadyCaptureVoicePlay = (ImageView) findViewById(R.id.ready_captured_voice_play);
        tvReadyCaptureVoiceTimeLength = (TextView) findViewById(R.id.ready_captured_voice_time_length);

        ivPickLocationImgInd = (ImageView) findViewById(R.id.pick_location_img_ind);
        tvPickLocationTxtInd = (TextView) findViewById(R.id.pick_location_txt_ind);

        findViewById(R.id.capture_voice_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_inner_left_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_delete).setOnClickListener(this);

        findViewById(R.id.pick_location_layout).setOnClickListener(this);
        findViewById(R.id.pick_location_delete).setOnClickListener(this);


        mediaHelper = new MediaInputHelper(this);
        listPhoto = new ArrayList<CreateMomentActivity.WMediaFile>();

        if (null != savedInstanceState) {
            try {
                String savedMsg = savedInstanceState.getString("moment_msg_content");
                if (!TextUtils.isEmpty(savedMsg)) {
                    etMomentMsgContent.setText(savedMsg);
                }
                mediaHelper = savedInstanceState.getParcelable("media_helper");
                listPhoto = savedInstanceState.getParcelableArrayList("list_photo");

                String lastVoiceFilePath = savedInstanceState.getString("last_voice_file");
                if (!TextUtils.isEmpty(lastVoiceFilePath)) {
                    File aFile = new File(lastVoiceFilePath);
                    if (null != aFile && aFile.exists()) {
                        mLastVoiceFile = aFile;
                        updateGotVoice();
                    }
                }

                moment.latitude = savedInstanceState.getDouble("location_latitude");
                moment.longitude = savedInstanceState.getDouble("location_longitude");

                String strAddress = savedInstanceState.getString("location_address");
                if (!TextUtils.isEmpty(strAddress)) {
                    moment.place = strAddress;
                    updateLocation(moment.latitude, moment.longitude, strAddress);
                }

                notifyFileChanged(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bundle.getBoolean(EXTRA_ALLOW_LOC)) {
            findViewById(R.id.location_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.location_layout).setVisibility(View.GONE);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        TextView tvTitle = (TextView) findViewById(R.id.title_txt);
        tvTitle.setText(title);
    }

    private void updateGotVoice() {
        isCapturingVoice = false;

        //restore capture layout,as when voice file deleted, status correct
        ivCaptureInnerInd.setImageResource(R.drawable.timeline_record);
        tvCaptureInnerInd.setText(R.string.capture_voice_click_record);
        findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.bkg_e6e6e6);

        //switch to recored state
        findViewById(R.id.capture_voice_layout).setVisibility(View.GONE);
        findViewById(R.id.ready_captured_voice_layout).setVisibility(View.VISIBLE);

        addVoice2moment();

        setVoiceDuration();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pick_location_delete:
                moment.place = null;
                moment.latitude = moment.longitude = 0;

                findViewById(R.id.pick_location_delete).setVisibility(View.GONE);
                tvPickLocationTxtInd.setText(R.string.place);
                findViewById(R.id.pick_location_layout).setBackgroundResource(R.drawable.bkg_e6e6e6);
                ivPickLocationImgInd.setImageResource(R.drawable.timeline_location);
                break;
            case R.id.pick_location_layout:
                if (!isGettingLocation && TextUtils.isEmpty(moment.place)) {
                    tvPickLocationTxtInd.setText(R.string.getting_location_info);
                    isGettingLocation = true;
                    locationHelper.setOnLocationGotListener(mOnLocationGotListener);
                    locationHelper.getLocationWithAMap(true);
                } else {
                    if (TextUtils.isEmpty(moment.place))
                        tvPickLocationTxtInd.setText(R.string.place);
                    isGettingLocation = false;
                    locationHelper.setOnLocationGotListener(null);
                    locationHelper.stop();
                }
                break;
            case R.id.ready_captured_voice_delete:
                stopPlayingVoice();

                findViewById(R.id.capture_voice_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.ready_captured_voice_layout).setVisibility(View.GONE);

                removeVoiceFromMoment();
                break;
            case R.id.ready_captured_voice_inner_left_layout:
                tryPlayOrStopVoice();
                break;
            case R.id.capture_voice_layout:
                if (!isCapturingVoice) {
                    if (startRecording()) {
                        isCapturingVoice = true;

                        ivCaptureInnerInd.setImageResource(R.drawable.timeline_record_a);
                        findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.text_field);
                    }
                } else {
                    stopRecording();
                    updateGotVoice();
                }
                break;
            case R.id.title_back:
                releaseMediaFiles();
                finish();
                break;
            case R.id.btn_commit:
                commit();
                break;
            case R.id.trigger_add_img_layout:
                if (listPhoto == null || listPhoto.size() < imageMaxCnt) {
                    showPickImgSelector();
                } else {
                    mMsgBox.show(getString(R.string.app_name),
                            getString(R.string.hybird_message_editor_exceed_max_img_cnt, imageMaxCnt));
                }
                break;
            default:
                break;
        }
    }

    private void commit() {

        stopRecording();

        if (!isContentValid()) {
            mMsgBox.show(getString(R.string.app_name),
                    getString(R.string.hybird_message_editor_content_required));
            return;
        }

        Bundle data = new Bundle();
        data.putString(EXTRA_OUT_TEXT, etMomentMsgContent.getText().toString());
        if (!listPhoto.isEmpty()) {
            String[] path = new String[listPhoto.size() * 2];
            for (int i = 0; i < listPhoto.size(); ++i) {
                path[i * 2] = listPhoto.get(i).localPath;
                path[i * 2 + 1] = listPhoto.get(i).localThumbnailPath;
            }
            data.putStringArray(EXTRA_OUT_IMAGE_FILENAME, path);
        }
        if (mLastVoiceFile != null)
            data.putString(EXTRA_OUT_VOICE_FILENAME, mLastVoiceFile.getAbsolutePath());
        if (isLocGot) {
            data.putDouble(EXTRA_OUT_LOC_LATITUDE, moment.latitude);
            data.putDouble(EXTRA_OUT_LOC_LONGITUDE, moment.longitude);
            data.putString(EXTRA_OUT_LOC_ADDR, moment.place);
        }
        setResult(RESULT_OK, new Intent().putExtras(data));
        finish();
    }

    private void stopPlayingVoice() {
        mediaPlayerWraper.stop();
    }

    private int MOMENT_VOICE_MIN_LEN_IN_MS = 1000;

    private void setVoiceDuration() {
        if (voiceDuration > MOMENT_VOICE_MIN_LEN_IN_MS) {
            tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper.makeMyTimeDisplayFromMS(voiceDuration));
        } else {
            mMsgBox.toast(R.string.msg_voice_too_short);
            findViewById(R.id.ready_captured_voice_delete).performClick();
        }
    }

    private void tryPlayOrStopVoice() {
        if (!mLastVoiceFile.getAbsolutePath().equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(tvReadyCaptureVoiceTimeLength, false);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_play);
                }

                @Override
                public void onPlayBegin(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_play);
                }
            });
            mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(), voiceDuration);
        } else {
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(), voiceDuration);
        }

    }

    private void stopRecording() {
        if (null != timeElapseReportForCaptureVoiceRunnable) {
            timeElapseReportForCaptureVoiceRunnable.stop();
        }
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder = null;
        }
    }

    private boolean startRecording() {
        boolean ret = false;

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        } else {
            mMediaRecorder.reset();
        }

        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioSamplingRate(16000);
            mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_VOICE, ".m4a");
            mMediaRecorder.setOutputFile(mLastVoiceFile.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();

            timeElapseReportForCaptureVoiceRunnable = new TimeElapseReportRunnable();
            timeElapseReportForCaptureVoiceRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
                @Override
                public void reportElapse(final long elapsed) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isCapturingVoice) {
                                String myFormatTime = MediaPlayerWraper.makeMyTimeDisplayFromMS(elapsed);
                                tvCaptureInnerInd.setText(String.format(getString(R.string.capture_voice_click_stop), myFormatTime));
                            }
                        }
                    });
                }
            });
            new Thread(timeElapseReportForCaptureVoiceRunnable).start();
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
            stopRecording();

            if (null != mLastVoiceFile && mLastVoiceFile.exists()) {
                mLastVoiceFile.delete();
            }
            mLastVoiceFile = null;
            mMsgBox.toast(R.string.media_record_not_avaliable);
        }

        return ret;
    }

    private void releaseMediaFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != listPhoto) {
                    for (CreateMomentActivity.WMediaFile aWPhoto : listPhoto) {
                        deleteWPhotoFile(aWPhoto);
                    }
                }

                if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
                    Database.deleteAFile(mLastVoiceFile.getAbsolutePath());
                }
            }
        }).start();
    }

    private void showPickImgSelector() {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        bottomBoard.add(getString(R.string.image_take_photo), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        mediaHelper.takePhoto(HybirdMessageEditor.this, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA);
                    }
                });
        bottomBoard.add(getString(R.string.image_pick_from_local), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        int i = 0;
                        for (CreateMomentActivity.WMediaFile photo : listPhoto) {
                            if (!photo.isFromGallery) {
                                i++;
                            }
                        }
                        Intent intent = new Intent(HybirdMessageEditor.this, SelectPhotoActivity.class);
                        intent.putExtra("num", imageMaxCnt - listPhoto.size());
                        ThemeHelper.putExtraCurrThemeResId(intent, HybirdMessageEditor.this);
                        ArrayList<String> listPath = new ArrayList<String>();
                        for (CreateMomentActivity.WMediaFile photo : listPhoto) {
                            if (photo.isFromGallery) {
                                listPath.add(photo.galleryPath);
                            }
                        }
                        intent.putStringArrayListExtra("list", listPath);
                        startActivityForResult(intent, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY);
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();
    }

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

    private void copyFileForMomentMultimedia(WFile aFile) {
        String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                aFile.fileid, aFile.getExt());
        FileUtils.copyFile(aFile.localPath, destFilePath);

        // make thumbnail
        if (null != aFile.thumb_fileid) {
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

    private void addPhoto2moment(CreateMomentActivity.WMediaFile aPhoto) {
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(aPhoto.localPath)) {
                Log.w("duplicate photo add 2 momet, omit");
                return;
            }
        }
        String ext = FileUtils.getExt(aPhoto.localPath);
        String fakeFileId = String.valueOf(Math.random());
        WFile f = new WFile(ext, fakeFileId, null, aPhoto.localPath);
        f.thumb_fileid = String.valueOf(Math.random());
        f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
        moment.multimedias.add(f);

        copyFileForMomentMultimedia(f);
        aPhoto.localThumbnailPath = f.localThumbnailPath;
    }

    private void removePhotoFromMoment(CreateMomentActivity.WMediaFile aPhoto) {
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

    private int voiceDuration;
    private void addVoice2moment() {
        if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
            String localFilename = mLastVoiceFile.getAbsolutePath();
            String ext = FileUtils.getExt(localFilename);
            String fakeFileId = String.valueOf(Math.random());
            MediaPlayer mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
                mPlayer.prepare();
                voiceDuration = mPlayer.getDuration();
            } catch (IOException e) {
                voiceDuration = 0;
                e.printStackTrace();
            } finally {
                mPlayer.release();
                mPlayer = null;
            }
            WFile f = new WFile(ext, fakeFileId, voiceDuration/1000, localFilename);
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
        voiceDuration=0;

        mLastVoiceFile.delete();
        mLastVoiceFile=null;
    }

    private void recycleAView(View view) {
        ImageView aView=(ImageView) view.findViewById(R.id.img_photo);
        if(null != aView && null != aView.getDrawable()) {
            try {
                BitmapDrawable bmpDrawable=(BitmapDrawable)aView.getDrawable();
                Bitmap bmp=bmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);

                deleteWPhotoFile((CreateMomentActivity.WMediaFile)view.getTag());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteWPhotoFile(CreateMomentActivity.WMediaFile aWPhoto) {
        if(null != aWPhoto) {
            Database.deleteAFile(aWPhoto.localPath);
        }
    }

    private void notifyFileChanged(boolean isAdded) {
        updateTriggerAddImgDescTxtStatus();

        if (isAdded) {
            final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
            final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(listPhoto.size() - 1).localPath));
            bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

            addPhoto2moment(listPhoto.get(listPhoto.size() - 1));
            listPhoto.get(listPhoto.size() - 1).relativeView=view;

            View imgDelete = view.findViewById(R.id.btn_delete);
            imgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAImage(view);
                }
            });
//            LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//            viewLayoutParams.setMargins(0,0,DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
            view.setTag(listPhoto.get(listPhoto.size() - 1));
            addedImgLayout.addView(view, 0);
//            ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//            params.width += imgPhoto.getLayoutParams().width;
//            addedImgLayout.setLayoutParams(params);
        } else {
            addedImgLayout.removeAllViews();
            recycleStoredBitmapDrawable();
            for(CreateMomentActivity.WMediaFile aPhoto : listPhoto) {
                removePhotoFromMoment(aPhoto);
            }
//            moment.multimedias.clear();

            int fileNum = listPhoto.size();
            for (int i = 0; i < fileNum; i++) {
                final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
                final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
                imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(i).localPath));
                bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

                addPhoto2moment(listPhoto.get(i));
                listPhoto.get(i).relativeView=view;

                View imgDelete = view.findViewById(R.id.btn_delete);
                imgDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAImage(view);
                    }
                });
//                LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//                viewLayoutParams.setMargins(0, 0, DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
                view.setTag(listPhoto.get(i));
                addedImgLayout.addView(view,0);
//                ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//                params.width += imgPhoto.getLayoutParams().width;
//                addedImgLayout.setLayoutParams(params);
            }
        }
    }

    private void deleteAImage(View view) {
        CreateMomentActivity.WMediaFile path = (CreateMomentActivity.WMediaFile) view.getTag();
        listPhoto.remove(path);

        removePhotoFromMoment(path);

        recycleAView(view);
        ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
        bmpDrawableList.remove(imgPhoto.getDrawable());

        recycleAView(view);
        notifyFileChanged(false);
    }

    private void updateTriggerAddImgDescTxtStatus() {
        TextView tvDesc=(TextView) findViewById(R.id.trigger_add_img_txt_desc);
        if(listPhoto.size() > 0) {
            tvDesc.setVisibility(View.GONE);
            hsvImgList.setBackgroundResource(R.drawable.table_white);
        } else {
            tvDesc.setVisibility(View.VISIBLE);
            hsvImgList.setBackgroundResource(R.drawable.bkg_e6e6e6);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> listPath = data.getStringArrayListExtra("list");
                    ArrayList<CreateMomentActivity.WMediaFile> photo2add = new ArrayList<CreateMomentActivity.WMediaFile>();
                    ArrayList<CreateMomentActivity.WMediaFile> photo2del = new ArrayList<CreateMomentActivity.WMediaFile>();
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
                            photo2add.add(listPhoto.get(i));
                        }
                    }

                    for(CreateMomentActivity.WMediaFile aPhoto : photo2del) {
                        deleteAImage(aPhoto.relativeView);
                    }
                    listPhoto = photo2add;

                    mMsgBox.showWait();
                    new AsyncTask<ArrayList<String>, Void, Void>() {
//                        boolean firstAdd=true;
                        @Override
                        protected Void doInBackground(ArrayList<String>... params) {
                            for (String path : params[0]) {
                                CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile();
                                Bitmap bmp = BmpUtils.decodeFile(path, CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT);
                                File file = MediaInputHelper.makeOutputMediaFile(
                                        MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
                                try {
                                    OutputStream os = new FileOutputStream(file);
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
                                    os.close();

                                    BmpUtils.recycleABitmap(bmp);

                                    photo.localPath = file.getAbsolutePath();
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
            case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA:
                if (resultCode == RESULT_OK) {
                    new AsyncTask<Intent, Void, Void>() {
                        @Override
                        protected Void doInBackground(Intent... params) {
                            String[] path = new String[2];
                            boolean handleImageRet=mediaHelper.handleImageResult(HybirdMessageEditor.this, params[0],
                                    CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT,
                                    0, 0,
                                    path);
                            if(handleImageRet) {
                                Log.i("handle result ok,path[0]="+path[0]);
                            } else {
                                Log.e("handle image error");
                            }
                            CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile();
                            photo.localPath = path[0];
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
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("moment_msg_content", etMomentMsgContent.getText().toString());
        outState.putParcelable("media_helper", mediaHelper);
        outState.putParcelableArrayList("list_photo", listPhoto);

        if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
            outState.putString("last_voice_file",mLastVoiceFile.getAbsolutePath());
        }

        if(!TextUtils.isEmpty(moment.place)) {
            outState.putString("location_address",moment.place);
            if(0 != moment.latitude || 0 != moment.longitude) {
                outState.putDouble("location_latitude",moment.latitude);
                outState.putDouble("location_longitude",moment.longitude);
            }
        } else {
            outState.putString("location_address","");
            outState.putDouble("location_latitude",0);
            outState.putDouble("location_longitude",0);
        }

        outState.putBoolean(EXTRA_REQUIRE_TEXT, requireText);
        outState.putBoolean(EXTRA_REQUIRE_IMAGE, requireImage);
        outState.putBoolean(EXTRA_REQUIRE_VOICE, requireVoice);
        outState.putBoolean(EXTRA_REQUIRE_LOC, requireLoc);
        outState.putInt(EXTRA_IMAGE_MAX_COUNT, imageMaxCnt);
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }

    private void getExtras(Bundle extras) {
        if (extras == null)
            return;

        requireText = extras.getBoolean(EXTRA_REQUIRE_TEXT, true);
        requireImage = extras.getBoolean(EXTRA_REQUIRE_IMAGE, true);
        requireVoice = extras.getBoolean(EXTRA_REQUIRE_VOICE, true);
        requireLoc = extras.getBoolean(EXTRA_REQUIRE_LOC, false);
        imageMaxCnt = extras.getInt(EXTRA_IMAGE_MAX_COUNT, 9);
    }
}
