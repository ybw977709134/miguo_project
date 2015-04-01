package co.onemeter.oneapp.ui;

import org.wowtalk.ui.MessageDialog;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 学生请假
 * @author hutianfeng
 * @date 2015/4/1
 *
 */
public class StudentAbsenceActivity extends Activity implements OnClickListener{
	
	private RelativeLayout layout_student_absence;
	
	//标题头
	private ImageButton title_back;
	private TextView textView_home_back;
	private TextView title_apply;
	
	//班级
	private LinearLayout layout_absence_class;
	private TextView textView_class_name;
	
	//课程
	private LinearLayout layout_absence_lesson;
	private TextView textView_lesson_name;
	
	//老师
	private LinearLayout layout_absence_teacher;
	private TextView textView_teacher_name;
	
	
	//请假事由
	private EditText editText_absence_reason;
	
	public final static int REQ_ABSENCE_CLASS = 1;//班级
	public final static int REQ_ABSENCE_LESSON = 2;//课程
	public final static int REQ_ABSENCE_TEACHER = 3;//教师
	
	private String classID = null;
	private String lessonID = null;
	private String teacherID = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_student_absence);
		initView();
		
		layout_student_absence.setFocusable(true);
		layout_student_absence.setFocusableInTouchMode(true);
		layout_student_absence.requestFocus();
	}

	/**
	 * 初始化各个控件
	 */
	private void initView() { 
		
		layout_student_absence = (RelativeLayout) findViewById(R.id.layout_student_absence);
		layout_student_absence.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_student_absence.setFocusable(true);
				layout_student_absence.setFocusableInTouchMode(true);
				layout_student_absence.requestFocus();
				return false;
			}
		});
		
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_home_back = (TextView) findViewById(R.id.textView_home_back);
		title_apply = (TextView) findViewById(R.id.title_apply);
		
		layout_absence_class = (LinearLayout) findViewById(R.id.layout_absence_class);
		textView_class_name = (TextView) findViewById(R.id.textView_class_name);
		
		layout_absence_lesson = (LinearLayout) findViewById(R.id.layout_absence_lesson);
		textView_lesson_name = (TextView) findViewById(R.id.textView_lesson_name);
		
		layout_absence_teacher = (LinearLayout) findViewById(R.id.layout_absence_teacher);
		textView_teacher_name = (TextView) findViewById(R.id.textView_teacher_name);
		
		editText_absence_reason = (EditText) findViewById(R.id.editText_absence_reason);
		
		
		title_back.setOnClickListener(this);
		textView_home_back.setOnClickListener(this);
		title_apply.setOnClickListener(this);
		
		layout_absence_class.setOnClickListener(this);
		layout_absence_lesson.setOnClickListener(this);
		layout_absence_teacher.setOnClickListener(this);
		
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
		case R.id.textView_home_back:
			finish();
			break;
			
		case R.id.title_apply://申请
			
			if (classID == null) {
				alert("请选择请假班级");
			} else if (lessonID == null) {
				alert("请选择请假课程");
			} else if (teacherID == null){
				alert("请选择请假教师");
			} else {
				//向老师请假
			}
			
			break;
			
		case R.id.layout_absence_class://获取班级名和id
			Intent intent = new Intent(this,SelectClassActivity.class);
			startActivityForResult(intent, REQ_ABSENCE_CLASS);
			break;
			
		case R.id.layout_absence_lesson://获取课程名和id
			if (classID != null) {
				//跳转
			} else {
				alert("请选择请假班级");
			}
			break;
			
		case R.id.layout_absence_teacher://获取老师名和id
			
			if (classID == null) {
				alert("请选择请假班级");
			} else if (lessonID == null) {
				alert("请选择请假课程");
			} else {
				//跳转
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
			if (requestCode == REQ_ABSENCE_CLASS) {//班级
				if (data != null) {
					classID = data.getStringExtra("class_id");
					textView_class_name.setText(data.getStringExtra("class_name"));
				} 
			}
			
			if (requestCode == REQ_ABSENCE_LESSON) {//课程
				if (data != null) {
					lessonID = data.getStringExtra("lesson_id");
					textView_lesson_name.setText(data.getStringExtra("lesson_name"));
				} 
			}
			
			if (requestCode == REQ_ABSENCE_TEACHER) {//老师
				if (data != null) {
					teacherID = data.getStringExtra("teacher_id");
					textView_teacher_name.setText(data.getStringExtra("teacher_name"));
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
		MessageDialog dialog = new MessageDialog(StudentAbsenceActivity.this,false, MessageDialog.SIZE_NORMAL);
        dialog.setMessage(message);
        dialog.show();
	}
	
}





















