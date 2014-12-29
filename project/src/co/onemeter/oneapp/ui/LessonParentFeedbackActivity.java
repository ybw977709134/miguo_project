package co.onemeter.oneapp.ui;

import java.util.ArrayList;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.Utils;
import org.wowtalk.api.WFile;
import org.wowtalk.api.WowMomentWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.TimerTextView;

import com.androidquery.AQuery;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LessonParentFeedbackActivity extends Activity implements OnClickListener {

	public final String EXTRA_KEY_MOMENT_MAX_TIMESTAMP="moment_max_timestamp";
	private final int REQ_IMAGE = 123;
	
	private MessageBox mMsgBox;
	
	private EditText edtContent;
	private ImageVideoInputWidget imageInputWidget;
	private Button btnVoiceRecord;
	private TimerTextView btnVoicePreview;
    private View btnVoiceDel;
	
	private Moment moment = new Moment();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activtiy_parent_feedback);
		initView();
	}
	
	private void initView(){
		
		mMsgBox = new MessageBox(this);
		AQuery q = new AQuery(this);
		
		edtContent = q.find(R.id.txt_parent_feedback_content).getEditText();
		btnVoiceRecord= q.find(R.id.btn_voice_record).getButton();
		imageInputWidget = (ImageVideoInputWidget) q.find(R.id.vg_input_imagevideo).getView();
		btnVoicePreview = (TimerTextView) q.find(R.id.btn_voice_preview).clicked(this).visibility(View.GONE).getView();
		btnVoiceDel = q.find(R.id.btn_voice_del).getView();
		
		
		q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_loc).clicked(this);

        imageInputWidget.setup(this, ImageVideoInputWidget.MediaType.Photo, REQ_IMAGE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;

		default:
			break;
		}
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
                if (resultCode == RESULT_OK) {
                    imageInputWidget.handleActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                break;
        }
	}
	
	/*
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
                    String uid = PrefUtil.getInstance(LessonParentFeedbackActivity.this).getUid();
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
                            int errno = WowMomentWebServerIF.getInstance(LessonParentFeedbackActivity.this).fAddMoment(moment);
                            if (errno == ErrorCode.OK) {
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
	*/
}
