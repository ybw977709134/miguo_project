package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

public class LessonParentFeedbackActivity extends Activity implements OnClickListener, InputBoardManager.ChangeToOtherAppsListener {

	public static final int MOMENTS_WORDS_OVER = 600;

	/**
	 * 输入 Moment 对象（可选），或在 发布成功后，在 Activity Result 中输出 Moment 对象。
	 */
	public static final String EXTRA_MOMENT = "moment";

	private TextView tvShareRange;

	private MediaInputHelper mediaHelper;
	private MessageBox mMsgBox;
	private Database mDb;

	private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA = 1;
	private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY = 2;
	private final static int ACTIVITY_REQ_ID_SHARE_RANGE_SELECT = 3;

	public final static int PHOTO_THUMBNAIL_WIDTH = 180;
	public final static int PHOTO_THUMBNAIL_HEIGHT = 120;

	private ArrayList<CreateMomentActivity.WMediaFile> listPhoto;

	private static LessonParentFeedbackActivity instance;

	private LinearLayout addedImgLayout;

	private Moment moment;
	private File mLastVoiceFile;

	private HorizontalScrollView hsvImgList;
	private boolean isCapturingVoice = false;
	private ImageView ivCaptureInnerInd;
	private TextView tvCaptureInnerInd;

	private ImageView ivReadyCaptureVoicePlay;
	private TextView tvReadyCaptureVoiceTimeLength;

	private MediaRecorder mMediaRecorder;
	// private MediaPlayer mPlayer;
	private TimeElapseReportRunnable timeElapseReportForCaptureVoiceRunnable;
	// private TimeElapseReportRunnable playingVoiceRunnable;

	private EditText etMomentMsgContent;

	private MediaPlayerWraper mediaPlayerWraper;

	private int lessonId;
	private String stuId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activtiy_parent_feedback);

		// fix problem on displaying gradient bmp
		getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
		stuId = getIntent().getStringExtra(Constants.STUID);
		
		initData(savedInstanceState);

		initView(savedInstanceState);
	}

	// 初始化 Moment 对象
	private void initData(Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			if (moment == null)
				moment = savedInstanceState.getParcelable(EXTRA_MOMENT);
			mediaHelper = savedInstanceState.getParcelable("media_helper");
			listPhoto = savedInstanceState.getParcelableArrayList("list_photo");

			String lastVoiceFilePath = savedInstanceState
					.getString("last_voice_file");
			if (!TextUtils.isEmpty(lastVoiceFilePath)) {
				File aFile = new File(lastVoiceFilePath);
				if (null != aFile && aFile.exists()) {
					mLastVoiceFile = aFile;
				}
			}
		}

		if (moment == null)
			moment = new Moment();

		if (mediaHelper == null)
			mediaHelper = new MediaInputHelper(this);

		if (listPhoto == null)
			listPhoto = new ArrayList<>();
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
		recycleStoredBitmapDrawable();
		if (null != mMediaRecorder) {
			stopRecording();
		}

		Log.w("create moment destroyed");
	}

	private boolean isContentValid() {
		if ((Utils.isNullOrEmpty(etMomentMsgContent.getText().toString())
				&& (listPhoto == null || listPhoto.isEmpty())
				&& (mLastVoiceFile == null || !mLastVoiceFile.exists()) && (0 == moment.latitude && 0 == moment.longitude))
				|| (null != mMediaRecorder && voiceDuration < MOMENT_VOICE_MIN_LEN_IN_MS)) {
			return false;
		}
		return true;
	}

	private void initView(Bundle savedInstanceState) {
		instance = this;

		mediaPlayerWraper = new MediaPlayerWraper(this);

		etMomentMsgContent = (EditText) findViewById(R.id.edt_moment_content);
		addedImgLayout = (LinearLayout) findViewById(R.id.added_images_layout);
		hsvImgList = (HorizontalScrollView) findViewById(R.id.hsv_img_list);

		mMsgBox = new MessageBox(this);
		mDb = new Database(this);

		tvShareRange = (TextView) findViewById(R.id.tv_share_range);

		findViewById(R.id.title_back).setOnClickListener(this);
		findViewById(R.id.share_range_layout).setOnClickListener(this);

		findViewById(R.id.trigger_add_img_layout).setOnClickListener(this);
		findViewById(R.id.btn_parent_confirm).setOnClickListener(this);

		ivCaptureInnerInd = (ImageView) findViewById(R.id.capture_inner_img_ind);
		tvCaptureInnerInd = (TextView) findViewById(R.id.capture_inner_txt_ind);

		ivReadyCaptureVoicePlay = (ImageView) findViewById(R.id.ready_captured_voice_play);
		tvReadyCaptureVoiceTimeLength = (TextView) findViewById(R.id.ready_captured_voice_time_length);

		findViewById(R.id.capture_voice_layout).setOnClickListener(this);
		findViewById(R.id.ready_captured_voice_layout).setOnClickListener(this);
		findViewById(R.id.ready_captured_voice_inner_left_layout)
				.setOnClickListener(this);
		findViewById(R.id.ready_captured_voice_delete).setOnClickListener(this);

		if (mLastVoiceFile != null)
			updateGotVoice();

		if (null != moment) {
			try {
				if (!TextUtils.isEmpty(moment.text)) {
					etMomentMsgContent.setText(moment.text);
				}
				if (moment.visibility() == Moment.VISIBVILITY_LIMITED) {
					tvShareRange.setText(R.string.share_range_private);
				} else {
					tvShareRange.setText(R.string.share_range_public);
				}

				if (moment.multimedias != null && !moment.multimedias.isEmpty()) {
					listPhoto = new ArrayList<>(moment.multimedias.size());
					for (WFile f : moment.multimedias) {
						if (f.isImageByExt() || f.isVideoByExt()) {
							listPhoto.add(new CreateMomentActivity.WMediaFile(f));
						}
					}
				} else {
					listPhoto = new ArrayList<>();
				}

				notifyFileChanged(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private void updateGotVoice() {
		isCapturingVoice = false;

		// restore capture layout,as when voice file deleted, status correct
		ivCaptureInnerInd.setImageResource(R.drawable.timeline_record);
		tvCaptureInnerInd.setText(R.string.capture_voice_click_record);
		findViewById(R.id.capture_voice_layout).setBackgroundResource(
				R.drawable.bkg_e6e6e6);

		// switch to recored state
		findViewById(R.id.capture_voice_layout).setVisibility(View.GONE);
		findViewById(R.id.ready_captured_voice_layout).setVisibility(
				View.VISIBLE);

		addVoice2moment();

		setVoiceDuration();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.ready_captured_voice_delete:
			stopPlayingVoice();

			findViewById(R.id.capture_voice_layout).setVisibility(View.VISIBLE);
			findViewById(R.id.ready_captured_voice_layout).setVisibility(
					View.GONE);

			removeVoiceFromMoment();
			break;
		case R.id.ready_captured_voice_inner_left_layout:
			tryPlayOrStopVoice();
			break;
		case R.id.capture_voice_layout:
			View imageButton = findViewById(R.id.title_moment_send);
			if (!isCapturingVoice) {
				if (startRecording()) {
					isCapturingVoice = true;

					ivCaptureInnerInd
							.setImageResource(R.drawable.timeline_record_a);
					findViewById(R.id.capture_voice_layout)
							.setBackgroundResource(R.drawable.text_field);
					imageButton.setEnabled(false);

				}
			} else {
				stopRecording();
				updateGotVoice();
				imageButton.setEnabled(true);
			}
			break;
		case R.id.title_back:
			// 发布动态退出时的提示

            MessageDialog dialog = new MessageDialog(this);
            dialog.setMessage("你确定放弃本次提交意见吗?");
            dialog.setCancelable(false);
            dialog.setLeftBold(true);
            dialog.setOnLeftClickListener("确定",new MessageDialog.MessageDialogClickListener() {
                @Override
                public void onclick(MessageDialog dialog) {
                    dialog.dismiss();
                    releaseMediaFiles();
                    finish();
                }
            });
            dialog.setOnRightClickListener("取消",null);
            dialog.show();



			break;

		case R.id.btn_parent_confirm:
			if (!isContentValid()) {
				Toast.makeText(LessonParentFeedbackActivity.this, "你还没有填写任何信息",
						Toast.LENGTH_LONG).show();
			} else {
				MessageDialog dialogConfirm = new MessageDialog(LessonParentFeedbackActivity.this);
                dialogConfirm.setTitle("提示");
                dialogConfirm.setMessage("你确定提交这次家长意见吗?");
                dialogConfirm.setCancelable(false);
                dialogConfirm.setRightBold(true);
                dialogConfirm.setOnLeftClickListener("取消", null);

                dialogConfirm.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        createParentFeedbackMoment();
                    }
                }
                );
                dialogConfirm.show();
				
			}
			break;
		case R.id.share_range_layout:
			Intent intent = new Intent(this, ShareRangeSelectActivity.class);
			intent.putStringArrayListExtra(
					ShareRangeSelectActivity.LITMITED_DEPS,
					moment.limitedDepartmentList);
			startActivityForResult(intent, ACTIVITY_REQ_ID_SHARE_RANGE_SELECT);
			break;
		case R.id.trigger_add_img_layout:
			showPickImgSelector();
			break;
		default:
			break;
		}
	}

	private void stopPlayingVoice() {
		mediaPlayerWraper.stop();
	}

	private int MOMENT_VOICE_MIN_LEN_IN_MS = 1000;

	private void setVoiceDuration() {
		if (voiceDuration > MOMENT_VOICE_MIN_LEN_IN_MS) {
			tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper
					.makeMyTimeDisplayFromMS(voiceDuration));
		} else {
			mMsgBox.toast(R.string.msg_voice_too_short);
			findViewById(R.id.ready_captured_voice_delete).performClick();
		}
	}

	private void tryPlayOrStopVoice() {
		if (!mLastVoiceFile.getAbsolutePath().equals(
				mediaPlayerWraper.getPlayingMediaPath())) {
			mediaPlayerWraper.stop();
			mediaPlayerWraper.setPlayingTimeTV(tvReadyCaptureVoiceTimeLength,
					false);
			mediaPlayerWraper
					.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
						@Override
						public void onPlayFail(String path) {
							ivReadyCaptureVoicePlay
									.setImageResource(R.drawable.timeline_play);
						}

						@Override
						public void onPlayBegin(String path) {
							ivReadyCaptureVoicePlay
									.setImageResource(R.drawable.timeline_stop);
						}

						@Override
						public void onPlayComplete(String path) {
							ivReadyCaptureVoicePlay
									.setImageResource(R.drawable.timeline_play);
						}
					});
			mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(),
					voiceDuration);
		} else {
			// second trigger,stop
			mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(),
					voiceDuration);
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
			mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(
					MediaInputHelper.MEDIA_TYPE_VOICE, "."
							+ ChatMessage.SEND_AUDIO_EXT);
			mMediaRecorder.setOutputFile(mLastVoiceFile.getAbsolutePath());
			mMediaRecorder.prepare();
			mMediaRecorder.start();

			timeElapseReportForCaptureVoiceRunnable = new TimeElapseReportRunnable();
			timeElapseReportForCaptureVoiceRunnable
					.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
						@Override
						public void reportElapse(final long elapsed) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (isCapturingVoice) {
										String myFormatTime = MediaPlayerWraper
												.makeMyTimeDisplayFromMS(elapsed);
										tvCaptureInnerInd.setText(String
												.format(getString(R.string.capture_voice_click_stop),
														myFormatTime));
										// 如果录音的时间超过120秒//将停止录音
										if (elapsed >= 120000) {
											stopRecording();
											updateGotVoice();
											Toast.makeText(
													LessonParentFeedbackActivity.this,
													"录音时间已经超过了120秒",
													Toast.LENGTH_SHORT).show();
										}
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

	private void hideIME() {
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(etMomentMsgContent.getWindowToken(), 0);
	}

	private void showPickImgSelector() {
		hideIME();

		final BottomButtonBoard bottomBoard = new BottomButtonBoard(this,
				getWindow().getDecorView());
		bottomBoard.add(getString(R.string.image_take_photo),
				BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						bottomBoard.dismiss();
						if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
							mMsgBox.toast(String.format(
									LessonParentFeedbackActivity.this
											.getString(R.string.settings_account_moment_take_photos_oom),
									CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
							return;
						}
						mediaHelper.takePhoto(
								LessonParentFeedbackActivity.this,
								ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA);
					}
				});
		bottomBoard.add(getString(R.string.image_pick_from_local),
				BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						bottomBoard.dismiss();
						if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
							mMsgBox.toast(String.format(
									LessonParentFeedbackActivity.this
											.getString(R.string.settings_account_moment_take_photos_oom),
									CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
							return;
						}
						int i = 0;
						for (CreateMomentActivity.WMediaFile photo : listPhoto) {
							if (!photo.isFromGallery) {
								i++;
							}
						}
						Intent intent = new Intent(
								LessonParentFeedbackActivity.this,
								SelectPhotoActivity.class);
						intent.putExtra("num",
								CreateMomentActivity.TOTAL_PHOTO_ALLOWED - i);
						ThemeHelper.putExtraCurrThemeResId(intent,
								LessonParentFeedbackActivity.this);
						ArrayList<String> listPath = new ArrayList<String>();
						for (CreateMomentActivity.WMediaFile photo : listPhoto) {
							if (photo.isFromGallery) {
								listPath.add(photo.galleryPath);
							}
						}
						intent.putStringArrayListExtra("list", listPath);
						startActivityForResult(intent,
								ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY);
					}
				});
		bottomBoard.addCancelBtn(getString(R.string.cancel));
		bottomBoard.show();
	}

	private static long MOMENT_ALIAS_ID_INC = 0;

	private void createParentFeedbackMoment() {

		if (etMomentMsgContent.getText().toString().length() > CreateNormalMomentWithTagActivity.MOMENTS_WORDS_OVER) {
			mMsgBox.show(null, getString(R.string.moments_words_over_failed));
			return;
		}
		if (Utils.isNullOrEmpty(etMomentMsgContent.getText().toString())
				&& (listPhoto == null || listPhoto.isEmpty())
				&& (mLastVoiceFile == null || !mLastVoiceFile.exists())) {
			mMsgBox.show(
					null,
					getString(R.string.settings_account_moment_text_cannot_be_empty));
		} else {
			// store local moment
			mMsgBox.showWait();
			AsyncTaskExecutor
					.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {
						@Override
						protected Integer doInBackground(Void... params) {
							moment.id = Moment.ID_PLACEHOLDER_PREFIX
									+ System.currentTimeMillis();
							moment.text = etMomentMsgContent.getText().toString();
							moment.timestamp = getIntent().getLongExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP, 0) + 1;
							Log.w("local moment timestamp set to "
									+ moment.timestamp);
							if (null == moment.owner)
								moment.owner = new Buddy();
							moment.owner.userID = Moment.ANONYMOUS_UID;
							moment.likedByMe = false;
							mDb.storeMoment(moment, null);
							for (WFile f : moment.multimedias) {
								mDb.storeMultimedia(moment, f);
							}

							Intent data = new Intent();
							setResult(RESULT_OK, data);

							// upload to server
							new Thread(new Runnable() {
								@Override
								public void run() {
									int errno = MomentWebServerIF.getInstance(
											LessonParentFeedbackActivity.this)
											.fAddMoment(moment, true);
									LessonParentFeedback feedback = new LessonParentFeedback();
									feedback.lesson_id = lessonId;
									feedback.student_id = stuId;
									int errno2 = LessonWebServerIF.getInstance(
											LessonParentFeedbackActivity.this)
											.addOrModifyLessonParentFeedback(
													feedback, moment);
									if (errno == ErrorCode.OK
											&& errno2 == ErrorCode.OK) {
										Intent intent = new Intent(
												LessonParentFeedbackActivity.this,
												PublishMomentService.class);
										intent.putExtra(
												PublishMomentService.EXTRA_MOMENT,
												moment);
										intent.putExtra(
												PublishMomentService.EXTRA_ANONYMOUS,
												true);
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
					});
		}
	}

	private LinkedList<BitmapDrawable> bmpDrawableList = new LinkedList<BitmapDrawable>();

	private void recycleStoredBitmapDrawable() {
		for (BitmapDrawable aBmpDrawable : bmpDrawableList) {
			if (null != aBmpDrawable) {
				Bitmap bmp = aBmpDrawable.getBitmap();
				BmpUtils.recycleABitmap(bmp);
			}
		}
		bmpDrawableList.clear();
	}

	private void copyFileForMomentMultimedia(WFile aFile, boolean isPhoto) {
		String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
				aFile.fileid, aFile.getExt());
		FileUtils.copyFile(aFile.localPath, destFilePath);

		// make thumbnail
		if (null != aFile.thumb_fileid && aFile.localThumbnailPath == null) {
			boolean saved = false;
			Bitmap thumb = null;
			if (isPhoto) {
				thumb = BmpUtils.decodeFile(aFile.localPath, 400, 400, true);
			} else {
				thumb = BitmapFactory.decodeResource(getResources(),
						R.drawable.chat_icon_video);
			}
			if (thumb != null) {
				aFile.localThumbnailPath = PhotoDisplayHelper
						.makeLocalFilePath(aFile.thumb_fileid, aFile.getExt());
				OutputStream os = null;
				try {
					os = new FileOutputStream(aFile.localThumbnailPath);
					saved = thumb.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX
																				// format
																				// should
																				// be
																				// same
																				// with
																				// main
																				// file?
					os.close();

					BmpUtils.recycleABitmap(thumb);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (!saved) {
				aFile.thumb_fileid = aFile.localThumbnailPath = null;
			}
		}
	}

	private void addMedia2moment(CreateMomentActivity.WMediaFile file) {
		for (WFile aFile : moment.multimedias) {
			if (aFile.localPath.equals(file.localPath)) {
				Log.w("duplicate photo add 2 momet, omit");
				return;
			}
		}
		if (TextUtils.isEmpty(file.getExt()))
			file.setExt(FileUtils.getExt(file.localPath));
		if (TextUtils.isEmpty(file.fileid))
			file.fileid = String.valueOf(Math.random());
		if (TextUtils.isEmpty(file.thumb_fileid))
			file.thumb_fileid = String.valueOf(Math.random());
		file.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
		moment.multimedias.add(file);

		copyFileForMomentMultimedia(file, file.isPhoto);
	}

	private void removePhotoFromMoment(CreateMomentActivity.WMediaFile aPhoto) {
		WFile file2remove = null;
		for (WFile aFile : moment.multimedias) {
			if (aFile.localPath.equals(aPhoto.localPath)) {
				file2remove = aFile;
				break;
			}
		}

		if (null != file2remove) {
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
			WFile f = new WFile(ext, fakeFileId, voiceDuration / 1000,
					localFilename);
			f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
			moment.multimedias.add(f);

			copyFileForMomentMultimedia(f, false);
		}
	}

	private void removeVoiceFromMoment() {
		WFile file2remove = null;
		String localFilename = mLastVoiceFile.getAbsolutePath();
		for (WFile aFile : moment.multimedias) {
			if (aFile.localPath.equals(localFilename)) {
				file2remove = aFile;
				break;
			}
		}

		if (null != file2remove) {
			moment.multimedias.remove(file2remove);
		}
		voiceDuration = 0;

		mLastVoiceFile.delete();
		mLastVoiceFile = null;
	}

	private void recycleAView(View view) {
		ImageView aView = (ImageView) view.findViewById(R.id.img_photo);
		if (null != aView && null != aView.getDrawable()) {
			try {
				BitmapDrawable bmpDrawable = (BitmapDrawable) aView
						.getDrawable();
				Bitmap bmp = bmpDrawable.getBitmap();
				BmpUtils.recycleABitmap(bmp);

				deleteWPhotoFile((CreateMomentActivity.WMediaFile) view
						.getTag());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void deleteWPhotoFile(CreateMomentActivity.WMediaFile aWPhoto) {
		if (null != aWPhoto) {
			Database.deleteAFile(aWPhoto.localPath);
		}
	}

	private void notifyFileChanged(boolean isAdded) {
		updateTriggerAddImgDescTxtStatus();

		if (isAdded) {
			addMedia2moment(listPhoto.get(listPhoto.size() - 1));

			final View view = LayoutInflater.from(this).inflate(
					R.layout.listitem_moment_image, addedImgLayout, false);
			final ImageView imgPhoto = (ImageView) view
					.findViewById(R.id.img_photo);
			imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
					listPhoto.get(listPhoto.size() - 1).localThumbnailPath));
			bmpDrawableList.add((BitmapDrawable) imgPhoto.getDrawable());

			listPhoto.get(listPhoto.size() - 1).relativeView = view;

			View imgDelete = view.findViewById(R.id.btn_delete);
			imgDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteAImage(view);
				}
			});
			view.setTag(listPhoto.get(listPhoto.size() - 1));
			addedImgLayout.addView(view, 0);
		} else {
			addedImgLayout.removeAllViews();
			recycleStoredBitmapDrawable();
			for (CreateMomentActivity.WMediaFile aPhoto : listPhoto) {
				removePhotoFromMoment(aPhoto);
			}
			// moment.multimedias.clear();

			int fileNum = listPhoto.size();
			for (int i = 0; i < fileNum; i++) {
				addMedia2moment(listPhoto.get(i));

				final View view = LayoutInflater.from(this).inflate(
						R.layout.listitem_moment_image, addedImgLayout, false);
				final ImageView imgPhoto = (ImageView) view
						.findViewById(R.id.img_photo);
				imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
						listPhoto.get(i).localThumbnailPath));
				bmpDrawableList.add((BitmapDrawable) imgPhoto.getDrawable());

				listPhoto.get(i).relativeView = view;

				View imgDelete = view.findViewById(R.id.btn_delete);
				imgDelete.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						deleteAImage(view);
					}
				});
				// LinearLayout.LayoutParams viewLayoutParams =
				// (LinearLayout.LayoutParams) view.getLayoutParams();
				// viewLayoutParams.setMargins(0, 0,
				// DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this,
				// 10), 0);
				view.setTag(listPhoto.get(i));
				addedImgLayout.addView(view, 0);
				// ViewGroup.LayoutParams params =
				// addedImgLayout.getLayoutParams();
				// params.width += imgPhoto.getLayoutParams().width;
				// addedImgLayout.setLayoutParams(params);
			}
		}
	}

	private void deleteAImage(View view) {
		CreateMomentActivity.WMediaFile path = (CreateMomentActivity.WMediaFile) view
				.getTag();
		listPhoto.remove(path);

		removePhotoFromMoment(path);

		recycleAView(view);
		ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
		bmpDrawableList.remove(imgPhoto.getDrawable());

		recycleAView(view);
		notifyFileChanged(false);
	}

	private void updateTriggerAddImgDescTxtStatus() {
		TextView tvDesc = (TextView) findViewById(R.id.trigger_add_img_txt_desc);
		if (listPhoto.size() > 0) {
			tvDesc.setVisibility(View.GONE);
			hsvImgList.setBackgroundResource(R.drawable.table_white);
		} else {
			tvDesc.setVisibility(View.VISIBLE);
			hsvImgList.setBackgroundResource(R.drawable.bkg_e6e6e6);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_REQ_ID_SHARE_RANGE_SELECT:
			if (resultCode == RESULT_OK) {
				int shareRangeTag = data.getIntExtra(
						ShareRangeSelectActivity.SHARE_RANGE_TAG,
						Moment.VISIBILITY_ALL);
				if (Moment.VISIBILITY_ALL == shareRangeTag) {
					tvShareRange.setText(R.string.share_range_public);
					moment.limitedDepartmentList.clear();
				} else {
					tvShareRange.setText(R.string.share_range_private);

					ArrayList<String> deps = data
							.getStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS);
					moment.limitedDepartmentList = deps;
				}
			}
			break;
		case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY:
			if (resultCode == RESULT_OK) {
				ArrayList<String> listPath = data
						.getStringArrayListExtra("list");
				ArrayList<CreateMomentActivity.WMediaFile> photo2add = new ArrayList<CreateMomentActivity.WMediaFile>();
				ArrayList<CreateMomentActivity.WMediaFile> photo2del = new ArrayList<CreateMomentActivity.WMediaFile>();
				for (int i = 0; i < listPhoto.size(); i++) {
					boolean needAdd = false;
					if (!listPhoto.get(i).isFromGallery) {
						needAdd = true;
					} else {
						if (listPath.contains(listPhoto.get(i).galleryPath)) {
							listPath.remove(listPhoto.get(i).galleryPath);
							needAdd = true;
						} else {
							// not contained,delete this
							photo2del.add(listPhoto.get(i));
						}
					}

					if (needAdd) {
						photo2add.add(listPhoto.get(i));
					}
				}

				for (CreateMomentActivity.WMediaFile aPhoto : photo2del) {
					deleteAImage(aPhoto.relativeView);
				}
				listPhoto = photo2add;

				mMsgBox.showWait();
				AsyncTaskExecutor.executeShortNetworkTask(
						new AsyncTask<ArrayList<String>, Void, Void>() {
							// boolean firstAdd=true;
							@Override
							protected Void doInBackground(
									ArrayList<String>... params) {
								for (String path : params[0]) {
									CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(
											true);
									Bitmap bmp = BmpUtils
											.decodeFile(
													path,
													CreateMomentActivity.PHOTO_SEND_WIDTH,
													CreateMomentActivity.PHOTO_SEND_HEIGHT);
									File file = MediaInputHelper
											.makeOutputMediaFile(
													MediaInputHelper.MEDIA_TYPE_IMAGE,
													".jpg");
									try {
										OutputStream os = new FileOutputStream(
												file);
										bmp.compress(
												Bitmap.CompressFormat.JPEG, 90,
												os);
										os.close();

										BmpUtils.recycleABitmap(bmp);

										photo.localPath = file
												.getAbsolutePath();
										photo.galleryPath = path;
										photo.isFromGallery = true;
										listPhoto.add(photo);
									} catch (Exception e) {
										e.printStackTrace();
									}

									publishProgress((Void) null);
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
						}, listPath);
			}
			break;
		case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA:
			if (resultCode == RESULT_OK) {
				AsyncTaskExecutor.executeShortNetworkTask(
						new AsyncTask<Intent, Void, Void>() {
							@Override
							protected Void doInBackground(Intent... params) {
								String[] path = new String[2];
								boolean handleImageRet = mediaHelper
										.handleImageResult(
												LessonParentFeedbackActivity.this,
												params[0],
												CreateMomentActivity.PHOTO_SEND_WIDTH,
												CreateMomentActivity.PHOTO_SEND_HEIGHT,
												0, 0, path);
								if (handleImageRet) {
									Log.i("handle result ok,path[0]=" + path[0]);
								} else {
									Log.e("handle image error");
								}
								CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(true);
								photo.localPath = path[0];
								photo.isFromGallery = false;
								listPhoto.add(photo);
								return null;
							}

							@Override
							protected void onPostExecute(Void errno) {
								instance.notifyFileChanged(true);
							}
						}, data);
			}
			break;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		moment.text = etMomentMsgContent.getText().toString();

		outState.putParcelable(EXTRA_MOMENT, moment);
		outState.putParcelable("media_helper", mediaHelper);
		outState.putParcelableArrayList("list_photo", listPhoto);

		if (null != mLastVoiceFile && mLastVoiceFile.exists()) {
			outState.putString("last_voice_file",
					mLastVoiceFile.getAbsolutePath());
		}
	}

	@Override
	public void changeToOtherApps() {
		AppStatusService.setIsMonitoring(false);
	}

}
