package co.onemeter.oneapp.ui;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GlobalSetting;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.MomentWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WFile;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.DoubleClickedUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager.ChangeToOtherAppsListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.TimeHelper;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 布置作业
 * Created by zz on 05/15/2015.
 */
public class SignHomeworkResultkActivity extends Activity implements OnClickListener, ChangeToOtherAppsListener{
	private List<Moment> listMoment;
	private String momentId = null;
	private int lessonId;
	private LinearLayout trigger_add_img_layout;
	private ImageButton title_back;
	private MessageBox mMsgBox;
	private MediaInputHelper mediaHelper;
	private EditText edt_moment_content;
	private TextView btn_ok;
	private ArrayList<CreateMomentActivity.WMediaFile> listPhoto;
	private LinkedList<BitmapDrawable> bmpDrawableList=new LinkedList<BitmapDrawable>();
	private Moment moment;
	private LinearLayout addedImgLayout;
	public static final String EXTRA_MOMENT = "moment";
	private HorizontalScrollView hsvImgList;
	private Database mDb;
	public static final int TOTAL_PHOTO_ALLOWED = 6;
	private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA = 1;
	private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY = 2;
	private TextView tv_class_notice_title;
	private int homework_id;
	private String teacherID;
	private String lesson_name;
	private String class_name;
	private String schoolId;
	private String my_uid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_homework);
		initData(savedInstanceState);

        initView(savedInstanceState);
	}
	
	private void initData(Bundle savedInstanceState){
		if (savedInstanceState != null) {
			if (moment == null)
				moment = savedInstanceState.getParcelable(EXTRA_MOMENT);
			mediaHelper = savedInstanceState.getParcelable("media_helper");
			listPhoto = savedInstanceState.getParcelableArrayList("list_photo");
		}
		if (moment == null)
			moment = new Moment();
		if (mediaHelper == null)
            mediaHelper = new MediaInputHelper(this);
		
		if (listPhoto == null)
			listPhoto = new ArrayList<>();
	}
	private void initView(Bundle savedInstanceState) {
		title_back = (ImageButton) findViewById(R.id.btn_notice_back);
		tv_class_notice_title = (TextView) findViewById(R.id.class_notice_title);
		listMoment = new LinkedList<Moment>();
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
		addedImgLayout = (LinearLayout) findViewById(R.id.added_images_layout);
		trigger_add_img_layout = (LinearLayout) findViewById(R.id.trigger_add_img_layout);
		edt_moment_content = (EditText) findViewById(R.id.edt_moment_content);
		hsvImgList=(HorizontalScrollView) findViewById(R.id.hsv_img_list);
		btn_ok = (TextView) findViewById(R.id.btn_ok);
		trigger_add_img_layout.setOnClickListener(this);
		btn_ok.setOnClickListener(this);
		title_back.setOnClickListener(this);
		mDb = new Database(this);
		mMsgBox = new MessageBox(this);
		tv_class_notice_title.setText(R.string.class_signup);
		homework_id = getIntent().getIntExtra("homework_id", 0);
		teacherID = getIntent().getStringExtra("teacherID");
		lesson_name = getIntent().getStringExtra("lesson_name");
		class_name = getIntent().getStringExtra("class_name");
		schoolId = getIntent().getStringExtra("schoolId");
		my_uid = PrefUtil.getInstance(this).getUid();
		if (null != moment) {
			try {
				if (!TextUtils.isEmpty(moment.text)) {
					edt_moment_content.setText(moment.text);
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

	private void addLessonHomework(final int lessonId,final Moment moment){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>(){

			@Override
			protected Integer doInBackground(Void... params) {
				moment.id = Moment.ID_PLACEHOLDER_PREFIX
						+ System.currentTimeMillis();
				moment.text = edt_moment_content.getText().toString();
				moment.timestamp = getIntent().getLongExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP, 0) + 1;
//				Log.w("local moment timestamp set to "
//						+ moment.timestamp);
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
								SignHomeworkResultkActivity.this)
								.fAddMoment(moment, true);
						int errno2 = LessonWebServerIF.getInstance(
								SignHomeworkResultkActivity.this)
								.signupHomeworkResult(homework_id, moment);
						if (errno == ErrorCode.OK
								&& errno2 == ErrorCode.OK) {
							Intent intent = new Intent(
									SignHomeworkResultkActivity.this,
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
			protected void onPostExecute(Integer result) {	
				mMsgBox.dismissWait();

                //布置作业添加提示弹框
                MessageDialog dialog = new MessageDialog(SignHomeworkResultkActivity.this);
                dialog.setTitle("提示");
                dialog.setMessage("你确定要提交作业吗?");
                dialog.setCancelable(false);
                dialog.setRightBold(true);
                dialog.setOnLeftClickListener("取消",null);
                dialog.setOnRightClickListener("确定",new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        finish();
                        if(SubmitHomeWorkActivity.getInstance() != null){
                        	SubmitHomeWorkActivity.getInstance().finish();
                        }
                    }
                });

                dialog.show();
			}
			
		});
	}


	private void hideIME() {
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(edt_moment_content.getWindowToken(), 0);
	}
	private void showPickImgSelector() {
		hideIME();
        final BottomButtonBoard bottomBoard=new BottomButtonBoard(this, getWindow().getDecorView());
        bottomBoard.add(getString(R.string.image_take_photo), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (listPhoto.size() >= TOTAL_PHOTO_ALLOWED) {
                            mMsgBox.toast(String.format(SignHomeworkResultkActivity.this.getString(R.string.settings_account_moment_take_photos_oom), TOTAL_PHOTO_ALLOWED));
                            return;
                        }
                        mediaHelper.takePhoto(SignHomeworkResultkActivity.this, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA);
                    }
                });
        bottomBoard.add(getString(R.string.image_pick_from_local), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (listPhoto.size() >= TOTAL_PHOTO_ALLOWED) {
                            mMsgBox.toast(String.format(SignHomeworkResultkActivity.this.getString(R.string.settings_account_moment_take_photos_oom), TOTAL_PHOTO_ALLOWED));
                            return;
                        }
                        int i = 0;
                        for (CreateMomentActivity.WMediaFile photo : listPhoto) {
                            if (!photo.isFromGallery) {
                                i++;
                            }
                        }
                        Intent intent = new Intent(SignHomeworkResultkActivity.this, SelectPhotoActivity.class);
                        intent.putExtra("num", TOTAL_PHOTO_ALLOWED - i);
                        ThemeHelper.putExtraCurrThemeResId(intent, SignHomeworkResultkActivity.this);
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

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_notice_back:
			onBackPressed();
			break;
		case R.id.trigger_add_img_layout:
			showPickImgSelector();
			break;
		case R.id.btn_ok:
			if(DoubleClickedUtils.isFastDoubleClick()){
				break;  
        	}
			addLessonHomework(lessonId, moment);
			noticeTeacherHomeworkResult();
			break;
		default:
			break;
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
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
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<ArrayList<String>, Void, Void>() {
                    //                        boolean firstAdd=true;
                    @Override
                    protected Void doInBackground(ArrayList<String>... params) {
                        for (String path : params[0]) {
                            CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(true);
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
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            publishProgress((Void) null);
                        }
                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(Void... errno) {
                        notifyFileChanged(true);
                    }

                    @Override
                    protected void onPostExecute(Void errno) {
                        mMsgBox.dismissWait();
                        notifyFileChanged(false);
                    }
                }, listPath);
            }
			break;
		case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA:
            if (resultCode == RESULT_OK) {
                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Intent, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Intent... params) {
                        String[] path = new String[2];
                        boolean handleImageRet = mediaHelper.handleImageResult(SignHomeworkResultkActivity.this, params[0],
                                CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT,
                                0, 0,
                                path);
                        if (handleImageRet) {
//                            Log.i("handle result ok,path[0]=" + path[0]);
                        } else {
//                            Log.e("handle image error");
                        }
                        CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(true);
                        photo.localPath = path[0];
                        photo.isFromGallery = false;
                        listPhoto.add(photo);
                        return handleImageRet;
                    }

                    @Override
                    protected void onPostExecute(Boolean status) {
                        if (status) {
                            Assert.assertTrue(!listPhoto.isEmpty());
                            notifyFileChanged(true);
                        }
                    }
                }, data);
            }
            break;
		}
	}
	
	private void recycleStoredBitmapDrawable() {
        for(BitmapDrawable aBmpDrawable : bmpDrawableList) {
            if(null != aBmpDrawable) {
                Bitmap bmp=aBmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);
            }
        }
        bmpDrawableList.clear();
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
//            tvDesc.setVisibility(View.GONE);
            hsvImgList.setBackgroundResource(R.drawable.table_white);
        } else {
//            tvDesc.setVisibility(View.VISIBLE);
        	hsvImgList.setBackgroundResource(R.drawable.table_white);
        }
    }
	private void addMedia2moment(CreateMomentActivity.WMediaFile file) {
		for (WFile aFile : moment.multimedias) {
			if (aFile.localPath.equals(file.localPath)) {
//				Log.w("duplicate photo add 2 momet, omit");
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
	private void notifyFileChanged(boolean isAdded) {
        updateTriggerAddImgDescTxtStatus();

        if (isAdded) {
            Assert.assertTrue(!listPhoto.isEmpty());
            addMedia2moment(listPhoto.get(listPhoto.size() - 1));

            final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
            final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
            		listPhoto.get(listPhoto.size() - 1).localThumbnailPath));
            bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

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
                addMedia2moment(listPhoto.get(i));

                final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
                final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
                imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
                		listPhoto.get(i).localThumbnailPath));
                bmpDrawableList.add((BitmapDrawable) imgPhoto.getDrawable());

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

	private void noticeTeacherHomeworkResult(){
		String student_alias = mDb.fetchStudentAlias(schoolId, my_uid);
		String reason = lesson_name+"班的"+class_name+"课"+student_alias+"学生提交作业啦！请批阅";

		final ChatMessage message = new ChatMessage();
		message.chatUserName = teacherID;

		message.messageContent = reason;
		message.msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
		message.sentStatus = ChatMessage.SENTSTATUS_SENDING;
		message.sentDate = TimeHelper.getTimeForMessage(SignHomeworkResultkActivity.this);
		message.uniqueKey = Database.chatMessageSentDateToUniqueKey(message.sentDate);
		message.ioType = ChatMessage.IOTYPE_OUTPUT;
		
		message.primaryKey = new Database(SignHomeworkResultkActivity.this)
	                            .storeNewChatMessage(message, false);
		WowTalkVoipIF.getInstance(SignHomeworkResultkActivity.this).fSendChatMessage(message);
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
        if(null != aWPhoto) {
            Database.deleteAFile(aWPhoto.localPath);
        }
    }
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		moment.text = edt_moment_content.getText().toString();

		outState.putParcelable(EXTRA_MOMENT, moment);
		outState.putParcelable("media_helper", mediaHelper);
		outState.putParcelableArrayList("list_photo", listPhoto);

	}
	@Override
	public void changeToOtherApps() {
		AppStatusService.setIsMonitoring(false);		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		recycleStoredBitmapDrawable();
	}

}
