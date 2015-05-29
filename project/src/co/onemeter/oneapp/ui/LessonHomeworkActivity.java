package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
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
    

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_homework);
        
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        
        mediaPlayerWraper= new MediaPlayerWraper(this);

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        mImageResizer.setLoadingImage(R.drawable.feed_default_pic);
        mImageResizer.addImageCache(cacheParams);

        moment = getIntent().getParcelableExtra("moment");
        homeworkResult_id = getIntent().getIntExtra("homeworkResult_id", -1);
        lessonId = getIntent().getIntExtra("lessonId", -1);
        flag = getIntent().getIntExtra("flag", -1);
        if(null == moment) {
            finish();
            return;
        }
        initView();
        setupContent(moment);
        getLessonHomework();
    }

	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
		tv_del = (TextView) findViewById(R.id.tv_del);
		txt_content = (TextView) findViewById(R.id.txt_content);
		tv_title_name = (TextView) findViewById(R.id.title_name);
		tv_modify_homework = (TextView) findViewById(R.id.tv_modify_homework);
		
		imageTable = (TableLayout) findViewById(R.id.imageTable);
		getLessonHomework = new GetLessonHomework();
		
		if(flag == 1){
			tv_del.setVisibility(View.GONE);
			tv_modify_homework.setVisibility(View.GONE);
		}
		if(flag == 2){
			tv_title_name.setText(R.string.class_signup);
			tv_modify_homework.setVisibility(View.GONE);
		}
		title_back.setOnClickListener(this);
		tv_del.setOnClickListener(this);
		tv_modify_homework.setOnClickListener(this);
	}
	private void getLessonHomework(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(LessonHomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework);		
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if(result == 1){
					homework_id = getLessonHomework.id;
				}
				
			}
			
		});
	}
	
	private void delLessonHomework(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
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
	private void delLessonHomeworkResult(){
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
        mImageResizer.setExitTasksEarly(false);
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setPauseWork(false);
        mImageResizer.setExitTasksEarly(true);
        mImageResizer.flushCache();

        mImageResizer.clearCacheInMem();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mImageResizer.closeCache();
        mediaPlayerWraper.stop();
    }
    
    private void setupContent(final Moment moment) {
        if(null == moment) {
            return;
        }

        if (moment.text == null || moment.text.equals("")) {
        	txt_content.setVisibility(View.GONE);
        } else {
        	txt_content.setVisibility(View.VISIBLE);
        	txt_content.setText(moment.text);
        }

        photoFiles = new ArrayList<WFile>();
        if (moment != null && moment.multimedias != null && !moment.multimedias.isEmpty()) {
            for (WFile file : moment.multimedias) {
                if (file.isAudioByExt()) {
                    voiceFile = file;
                } else {
                    photoFiles.add(file);
                }
            }
        }

        MomentAdapter.setImageLayout(this, moment,mImageResizer, photoFiles, imageTable);
    }
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;
		case R.id.tv_del:
			if(isTeacher()){
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
			}else{
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
			
			break;
		case R.id.tv_modify_homework:
			Log.d("------------------------", "modify");
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

}
