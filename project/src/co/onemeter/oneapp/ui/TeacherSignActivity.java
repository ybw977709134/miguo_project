package co.onemeter.oneapp.ui;

import org.wowtalk.api.Database;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageDialog;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 老师签到
 * @author hutianfeng
 * @date 2015/4/1
 *
 */
public class TeacherSignActivity extends Activity implements OnClickListener{
	
	//标题头
		private ImageButton title_back;
		private TextView textView_home_back;
		private TextView title_sure;
		private TextView title_name;
		
		//班级
		private LinearLayout layout_sign_class;
		private TextView textView_class_name;
		
		//课程
		private LinearLayout layout_sign_lesson;
		private TextView textView_lesson_name;
		

		private String classID = null;
        private String classId_intent = null;
        private String className_intent = null;
        private String schoolID = null;
        private String homeworkTag = null;
		private int lessonID = 0;	
		private long endDate;
		
		public final static int REQ_SIGN_CLASS = 1;//班级
		public final static int REQ_SIGN_LESSON = 2;//课程

		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.activity_teacher_sign);
        classId_intent = getIntent().getStringExtra("classId");
        className_intent = getIntent().getStringExtra("classname");
        schoolID = getIntent().getStringExtra("schoolId");
        homeworkTag = getIntent().getStringExtra("homework");
		initView();
	}
	
	/**
	 * 初始化各个控件
	 */
	private void initView() { 
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_home_back = (TextView) findViewById(R.id.textView_home_back);
		title_sure = (TextView) findViewById(R.id.title_sure);
		title_name = (TextView) findViewById(R.id.title_name);
		
		layout_sign_class = (LinearLayout) findViewById(R.id.layout_sign_class);
		textView_class_name = (TextView) findViewById(R.id.textView_class_name);
		
		layout_sign_lesson = (LinearLayout) findViewById(R.id.layout_sign_lesson);
		textView_lesson_name = (TextView) findViewById(R.id.textView_lesson_name);
		if(homeworkTag != null){
			if(homeworkTag.equals("homework")){
				title_name.setText(R.string.class_homework);
			}
		}
		
		title_back.setOnClickListener(this);
		textView_home_back.setOnClickListener(this);
		title_sure.setOnClickListener(this);
		
		layout_sign_class.setOnClickListener(this);
		layout_sign_lesson.setOnClickListener(this);

		if(classId_intent != null){
            layout_sign_class.setVisibility(View.GONE);
            textView_home_back.setText(getString(R.string.back));
            findViewById(R.id.divider_teachersign_class_up).setVisibility(View.GONE);
            findViewById(R.id.divider_teachersign_class_bottom).setVisibility(View.GONE);
            ImageView img_diliver = (ImageView) findViewById(R.id.divider_teachersign_lesson_up);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) img_diliver.getLayoutParams();
            params.topMargin = 0;
            img_diliver.setLayoutParams(params);
        }
	}
	
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.title_back:
		case R.id.textView_home_back:
			finish();
			break;
			
		case R.id.title_sure://确定
            if(classId_intent != null){
                if(lessonID != 0){
                	if(homeworkTag != null){
            			if(homeworkTag.equals("homework")){
            				Intent homeworkIntent = new Intent(this, HomeworkActivity.class);
            				homeworkIntent.putExtra(Constants.LESSONID, lessonID);
            				homeworkIntent.putExtra("schoolId", schoolID);
            				homeworkIntent.putExtra("lesson_name", textView_lesson_name.getText().toString());
            				   
            				if(className_intent != null){
            					homeworkIntent.putExtra("class_name", className_intent);
            				}
            				startActivity(homeworkIntent);
            				return;
            			}
            		}
                    Intent intentSign = new Intent(this,RollCallOnlineActivity.class);
                    intentSign.putExtra("classId", classId_intent);
                    intentSign.putExtra("schoolId", schoolID); 
                    intentSign.putExtra("lessonId", lessonID);
                    intentSign.putExtra("end_date", endDate);
                    intentSign.putExtra("lesson_name", textView_lesson_name.getText().toString());
                    startActivity(intentSign);
                    return;
                }
            }
			if (classID == null) {
				alert("请选择请假班级");
			} else if (lessonID == 0) {
				alert("请选择请假课程");
			} else {
				if(homeworkTag != null){
        			if(homeworkTag.equals("homework")){
        				Intent homeworkIntent = new Intent(this, HomeworkActivity.class);
            			homeworkIntent.putExtra(Constants.LESSONID, lessonID);
            			homeworkIntent.putExtra("schoolId", schoolID);
            			homeworkIntent.putExtra("lesson_name", textView_lesson_name.getText().toString());
            			homeworkIntent.putExtra("class_name", textView_class_name.getText().toString());
            			startActivity(homeworkIntent);    				
        				return;
        			}
        		}
                Intent intentSign = new Intent(this,RollCallOnlineActivity.class);
                intentSign.putExtra("classId", classID);
                intentSign.putExtra("schoolId", schoolID); 
                intentSign.putExtra("lessonId", lessonID);
                intentSign.putExtra("end_date", endDate);
                intentSign.putExtra("lesson_name", textView_lesson_name.getText().toString());
                startActivity(intentSign);
                return;
			}
			break;
			
		case R.id.layout_sign_class://获取班级名和id
			Intent intentClass = new Intent(this,SelectClassActivity.class);
			startActivityForResult(intentClass, REQ_SIGN_CLASS);
			break;
			
		case R.id.layout_sign_lesson://获取课程名和id
            if(classId_intent != null){
                Intent intentLesson = new Intent(this,SelectLessonActivity.class);
                intentLesson.putExtra("classId", classId_intent);
                intentLesson.putExtra("homework", homeworkTag);
                startActivityForResult(intentLesson, REQ_SIGN_LESSON);
                return;
            }
			if (classID != null) {
				Intent intentLesson = new Intent(this,SelectLessonActivity.class);
				intentLesson.putExtra("classId", classID);
				intentLesson.putExtra("homework", homeworkTag);
				startActivityForResult(intentLesson, REQ_SIGN_LESSON);
			} else {
				alert("请选择请假班级");
			}
			break;

		default:
			break;
		}
		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (resultCode) {
		case RESULT_OK:
			if (requestCode == REQ_SIGN_CLASS) {//班级
				if (data != null) {
					classID = data.getStringExtra("class_id");
					schoolID = data.getStringExtra("school_id");
					textView_class_name.setText(data.getStringExtra("class_name"));
                    lessonID = 0;
                    textView_lesson_name.setText("");
				} 
			}
			
			if (requestCode == REQ_SIGN_LESSON) {//课程
				if (data != null) {
					lessonID = data.getIntExtra("lesson_id", 0);
					endDate = data.getLongExtra("end_date", -1);
					textView_lesson_name.setText(data.getStringExtra("lesson_name"));
				} 
			}
			

			
			break;

		default:
			break;
		}
	}
	
	
	/**
	 * 信息缺省提示框
	 * @param message
	 */
	private void alert(String message){
		MessageDialog dialog = new MessageDialog(TeacherSignActivity.this,false, MessageDialog.SIZE_NORMAL);
        dialog.setMessage(message);
        dialog.show();
	}
	
	
}
