package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.CreateMomentActivity.WMediaFile;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.TimerTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LessonParentFeedbackActivity extends Activity implements OnClickListener {

	public final String EXTRA_KEY_MOMENT_MAX_TIMESTAMP="moment_max_timestamp";
	public final static String ALIAS_ID_PREFIX="moment_alias_id_";
	
	private final int REQ_IMAGE = 123;
	
    private boolean mIsRecordingVoice = false;
    private int lessonId;
    private String stuId;
    
	
	private MessageBox mMsgBox;
	private Database mDb;
	
	private EditText edtContent;
	private ImageVideoInputWidget imageInputWidget;
	private Button btnVoiceRecord;
	private TimerTextView btnVoicePreview;
    private View btnVoiceDel;
	
	private Moment moment = new Moment();
	
	private List<WMediaFile> listPhoto;
	
    private File mLastVoiceFile;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activtiy_parent_feedback);
		
		moment.multimedias = new ArrayList<WFile>();
		listPhoto = new ArrayList<WMediaFile>();
		
		initView();
	}
	
	private void initView(){
		
		mDb = Database.getInstance(this);
		mMsgBox = new MessageBox(this);
		AQuery q = new AQuery(this);
		
		Intent intent = getIntent();
		lessonId = intent.getIntExtra(Constants.LESSONID, 0);
		stuId = intent.getStringExtra(Constants.STUID);
		
		edtContent = q.find(R.id.txt_parent_feedback_content).getEditText();
		btnVoiceRecord= q.find(R.id.btn_voice_record).getButton();
		imageInputWidget = (ImageVideoInputWidget) q.find(R.id.vg_input_imagevideo).getView();
		btnVoicePreview = (TimerTextView) q.find(R.id.btn_voice_preview).clicked(this).visibility(View.GONE).getView();
		btnVoiceDel = q.find(R.id.btn_voice_del).getView();
		
		
		q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_loc).clicked(this);
        q.find(R.id.btn_parent_confirm).clicked(this);

        imageInputWidget.setup(this, ImageVideoInputWidget.MediaType.Photo, REQ_IMAGE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			releaseMediaFiles();
			finish();
			break;

		case R.id.btn_parent_confirm:
			createFeedbackMoment();
			break;
			
		case R.id.btn_voice_record: 
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
        case R.id.btn_voice_preview:
            playMicVoice();
            break;
        case R.id.btn_voice_del: 
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
        
		default:
			break;
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

	private void updateData() {
        moment.text = edtContent.getText().toString();

        moment.multimedias = new ArrayList<WFile>(imageInputWidget.getItemCount());
        for (int i = 0; i < imageInputWidget.getItemCount(); ++i) {
            moment.multimedias.add(imageInputWidget.getItem(i));
        }
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
            case REQ_IMAGE:
				imageInputWidget.handleActivityResult(requestCode, resultCode, data);
                break;
            default:
                break;
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
	
	private void createFeedbackMoment() {
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
                    //alias id and timestamp,timestamp should be the largest
                    //will be updated when returned by server
                    moment.id = ALIAS_ID_PREFIX+System.currentTimeMillis();
                    moment.timestamp = getIntent().getLongExtra(EXTRA_KEY_MOMENT_MAX_TIMESTAMP,0)+1;
                    Log.w("local moment timestamp set to "+moment.timestamp);
                    if (null == moment.owner)
                        moment.owner = new Buddy();
                    moment.owner.userID = Moment.ANONYMOUS_UID;
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
                            int errno = MomentWebServerIF.getInstance(LessonParentFeedbackActivity.this).fAddMoment(moment,true);
                            LessonParentFeedback feedback = new LessonParentFeedback();
                            feedback.lesson_id = lessonId;
                            feedback.student_id = stuId;
                            int errno2 = LessonWebServerIF.getInstance(LessonParentFeedbackActivity.this).addOrModifyLessonParentFeedback(feedback, moment);
                            if (errno == ErrorCode.OK && errno2 == ErrorCode.OK) {
                                Intent intent = new Intent(LessonParentFeedbackActivity.this, DownloadingAndUploadingService.class);
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
    public void onDestroy() {
        super.onDestroy();
        recycleStoredBitmapDrawable();
    }

}
