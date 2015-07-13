package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.HomeWorkAdapter;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * Created with Eclipse.
 * User: zz
 * Date: 19-5-15
 * 查看老师布置的作业和学生提交的作业
 */
public class LessonHomeworkActivity extends Activity implements OnClickListener{
	private static final String IMAGE_CACHE_DIR = "image";
	private ImageButton title_back;
    private TextView textView__back;
	private TextView tv_del;
	private TextView txt_content;
    private Moment moment;
    private int homework_id;
    private int lessonId;
    private GetLessonHomework getLessonHomework;
    
    private TableLayout imageTable;

    private ImageResizer mImageResizer;
    private WFile voiceFile;
    private ArrayList<WFile> photoFiles;
    private MediaPlayerWraper mediaPlayerWraper;
    private int flag;
    private TextView tv_title_name;
    private int homeworkResult_id;
    private TextView tv_modify_homework;
    private String studentId;
    private static LessonHomeworkActivity instance;
    private String path = null;
    private ArrayList<String> list_path = new ArrayList<>();
    private String schoolId;
    private String class_name;
    private String lesson_name;
    private String teacherID;
    private int momentId = 0;

    private int REQ_PARENT_MODIFYHOMEWORK = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_homework);
        
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        
        instance = this;
        
        mediaPlayerWraper= new MediaPlayerWraper(this);

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        mImageResizer.setLoadingImage(R.drawable.feed_default_pic);
        mImageResizer.addImageCache(cacheParams);

        moment = getIntent().getParcelableExtra("moment");
        homeworkResult_id = getIntent().getIntExtra("homeworkResult_id", -1);
        lessonId = getIntent().getIntExtra("lessonId", -1);
        studentId = getIntent().getStringExtra("studentId");
        lesson_name = getIntent().getStringExtra("lesson_name");
		class_name = getIntent().getStringExtra("class_name");
		schoolId = getIntent().getStringExtra("schoolId");
		teacherID = getIntent().getStringExtra("teacherID");
		momentId = getIntent().getIntExtra("momentId", -1);
        flag = getIntent().getIntExtra("flag", -1);//学生账号查看作业
        if(null == moment) {
            finish();
            return;
        }
        initView();
//        setupContent(moment);
        getLessonHomework();

    }

    public static LessonHomeworkActivity getInstance(){
    	if(instance != null){
    		return instance;
    	}
    	return null;
    	 
    }
	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
        textView__back = (TextView) findViewById(R.id.textView__back);
		tv_del = (TextView) findViewById(R.id.tv_del);
		txt_content = (TextView) findViewById(R.id.txt_content);
		tv_title_name = (TextView) findViewById(R.id.title_name);
		tv_modify_homework = (TextView) findViewById(R.id.tv_modify_homework);
		
		imageTable = (TableLayout) findViewById(R.id.imageTable);
		getLessonHomework = new GetLessonHomework();

        photoFiles = new ArrayList<WFile>();

		if(flag == 1){//-1 学生账号查看作业
			tv_del.setText("提交作业");
            tv_title_name.setText(R.string.lesson_homework);
			tv_modify_homework.setVisibility(View.GONE);
		}
		if(flag == 2){
			tv_title_name.setText(R.string.class_signup);
			tv_modify_homework.setVisibility(View.GONE);
		}
		title_back.setOnClickListener(this);
        textView__back.setOnClickListener(this);
		tv_del.setOnClickListener(this);
		tv_modify_homework.setOnClickListener(this);
	}

    /**
     * 获取作业列表
     */
	private void getLessonHomework(){//获取作业列表
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(LessonHomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,2);

                for (int i = 0; i < getLessonHomework.teacherMoment.homeWorkMultimedias.size(); i++) {
                    Log.d("----list_path--:",getLessonHomework.teacherMoment.homeWorkMultimedias.get(i).multimedia_content_path);
//                    list_path.add(getLessonHomework.teacherMoment.homeWorkMultimedias.get(i).multimedia_thumbnail_path);
                }

                Log.d("----space --:","--------------------------------------");
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if(result == 0){
					homework_id = getLessonHomework.id;
				}else if(result == 1){
					homework_id = getLessonHomework.id;
				}
                setupContent(moment);

			}
			
		});

	}

    /**
     * 老师删除作业
     */
	private void delLessonHomework(){//老师账号可以删除作业
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
                Log.d("----homework_id:",homework_id+"");
				int status= LessonWebServerIF.getInstance(LessonHomeworkActivity.this).delHomework(homework_id);
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if(result == -1){
					setResult(RESULT_OK);
					finish();
				}
			}
			
		});
	}

    /**
     * 学生删除自己已经提交的作业
     */
	private void delLessonHomeworkResult(){//学生账号删除已经提交的作业
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(LessonHomeworkActivity.this).delHomeworkResult(homeworkResult_id);
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if(result == ErrorCode.OK){
					setResult(RESULT_OK);
					finish();
				}
			}
			
		});
	}

	@Override
    protected void onResume() {
        super.onResume();
//        mImageResizer.setExitTasksEarly(false);
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    public void onPause() {
        super.onPause();
//        mImageResizer.setPauseWork(false);
//        mImageResizer.setExitTasksEarly(true);
        mImageResizer.flushCache();

//        mImageResizer.clearCacheInMem();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mImageResizer.closeCache();
        mediaPlayerWraper.stop();
        if(instance != null){
        	instance = null;
        }
    }
    
    private void setupContent(final Moment moment) {
        if(null == moment) {
            return;
        }

        if (moment.text == null || moment.text.equals("")) {
            txt_content.setText("");
        	txt_content.setVisibility(View.GONE);
        } else {
        	txt_content.setVisibility(View.VISIBLE);
        	txt_content.setText(moment.text);
        }

//        photoFiles = new ArrayList<WFile>();
        photoFiles.clear();
        if (moment != null && moment.multimedias != null && !moment.multimedias.isEmpty()) {
            for (WFile file : moment.multimedias) {
                if (file.isAudioByExt()) {
                    voiceFile = file;
                } else {
                    photoFiles.add(file);
                }
            }
        }


        HomeWorkAdapter.setImageLayout(this, moment,mImageResizer, photoFiles, imageTable);
    }

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
        case R.id.textView__back:
			onBackPressed();
			break;

		case R.id.tv_del:
			if(flag == 1){//判断是学生账号进入查看作业界面
				if (momentId == 0) {
					Intent i = new Intent(LessonHomeworkActivity.this, SignHomeworkResultkActivity.class);
					i.putExtra("homework_id", homework_id);
					i.putExtra("teacherID", teacherID);
					i.putExtra("schoolId", schoolId);
					i.putExtra("lesson_name",lesson_name);
					i.putExtra("class_name", class_name);
					startActivity(i);

				} else {
					Intent i = new Intent(LessonHomeworkActivity.this, SubmitHomeWorkActivity.class);
					i.putExtra("lessonId",lessonId);
					i.putExtra("schoolId", schoolId);
					i.putExtra("student_uid",studentId);	
					i.putExtra("lesson_name", lesson_name);
					i.putExtra("class_name", class_name);
					startActivity(i);
				}
				
			} else {
				if (isTeacher()) {
					MessageDialog dialog = new MessageDialog(LessonHomeworkActivity.this);
		            dialog.setTitle("提示");
		            dialog.setMessage("你确定要删除该作业吗?");
		            dialog.setCancelable(false);
		            dialog.setRightBold(true);
		            dialog.setOnLeftClickListener("取消", null);
		               
		               dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
		                   @Override
		                   public void onclick(MessageDialog dialog) {
		                	   dialog.dismiss();
		                	   delLessonHomework();                
		                   }
		               }
		               );
		               dialog.show();

				} else {
					MessageDialog dialog = new MessageDialog(LessonHomeworkActivity.this);
		            dialog.setTitle("提示");
		            dialog.setMessage("你确定要删除该作业吗?");
		            dialog.setCancelable(false);
		            dialog.setRightBold(true);
		            dialog.setOnLeftClickListener("取消", null);
		               
		               dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
		                   @Override
		                   public void onclick(MessageDialog dialog) {
		                	   dialog.dismiss();
		                	   delLessonHomeworkResult();              
		                   }
		               }
		               );
		               dialog.show();
				}
			}
			break;

		case R.id.tv_modify_homework://修改作业
			Intent intent = new Intent(LessonHomeworkActivity.this, AddHomeworkActivity.class);
			intent.putExtra("text", txt_content.getText().toString());
			intent.putExtra("lessonId", lessonId);
			intent.putExtra("homework_id", homework_id);
			intent.putExtra("tag_intent_AddHomeworkActivity", "tag_intent_AddHomeworkActivity");

            list_path.clear();
            for(int i = 0;i < photoFiles.size();i++){
                getPhotos(photoFiles.get(i));
                list_path.add(path);
            }

//            intent.putExtra("listWMediaFile", listWMediaFile);
            intent.putExtra("list_path", list_path);
            startActivityForResult(intent,REQ_PARENT_MODIFYHOMEWORK);
			break;
		default:
			break;
		}
	}

	private boolean isTeacher(){
		if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
			return true;
		}
		return false;
	}

	private void getPhotos(final WFile file){
		path = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());

        Log.d("---newPath---:",path);

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
            boolean ok;

            @Override
            protected Void doInBackground(Void... voids) {
                WowTalkWebServerIF.getInstance(LessonHomeworkActivity.this).fGetFileFromServer(file.fileid,
                        GlobalSetting.S3_MOMENT_FILE_DIR,
                        new NetworkIFDelegate() {
                            @Override
                            public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                ok = true;
                            }

                            @Override
                            public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                ok = false;
                            }

                            @Override
                            public void setProgress(int i, int i2) {
                                publishProgress(i2);
                            }
                        }, 0, path, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
               
            }

            @Override
            protected void onProgressUpdate(Integer... params) {

            }
        });
	}
//	/**
//     * 跳转到此页面
//     * @param context
//     * @param moment
//     */
//    public static void launch(Context context, Moment moment,int lessonId) {
//        Intent intent = new Intent(context, LessonHomeworkActivity.class);
//        intent.putExtra("moment", moment);
//        intent.putExtra("lessonId",lessonId);
//        context.startActivity(intent);
//    }


    /**
     * 修改完作业返回刷新最新的作业列表
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == REQ_PARENT_MODIFYHOMEWORK) {
                moment = data.getParcelableExtra("modifyMoment");

                getLessonHomework();
//                setupContent(moment);


            }
        }
    }
}
