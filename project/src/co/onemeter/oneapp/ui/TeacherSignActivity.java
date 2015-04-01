package co.onemeter.oneapp.ui;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
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
		
		//班级
		private LinearLayout layout_absence_class;
		private TextView textView_class_name;
		
		//课程
		private LinearLayout layout_absence_lesson;
		private TextView textView_lesson_name;
		
		
		//请假事由
		private EditText editText_absence_reason;
		
		
		private String classID = null;
		private String lessonID = null;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.activity_teacher_sign);
		initView();
	}
	
	/**
	 * 初始化各个控件
	 */
	private void initView() { 
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_home_back = (TextView) findViewById(R.id.textView_home_back);
		title_sure = (TextView) findViewById(R.id.title_sure);
		
		layout_absence_class = (LinearLayout) findViewById(R.id.layout_absence_class);
		textView_class_name = (TextView) findViewById(R.id.textView_class_name);
		
		layout_absence_lesson = (LinearLayout) findViewById(R.id.layout_absence_lesson);
		textView_lesson_name = (TextView) findViewById(R.id.textView_lesson_name);
		
		editText_absence_reason = (EditText) findViewById(R.id.editText_absence_reason);
		
		
		title_back.setOnClickListener(this);
		textView_home_back.setOnClickListener(this);
		title_sure.setOnClickListener(this);
		
		layout_absence_class.setOnClickListener(this);
		layout_absence_lesson.setOnClickListener(this);
		
		
	}
	
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
		case R.id.textView_home_back:
			finish();
			break;
			
		case R.id.title_sure:
			
			break;
			
		case R.id.layout_absence_class://获取班级名和id
			finish();
			break;
			
		case R.id.layout_absence_lesson://获取课程名和id
			finish();
			break;

		default:
			break;
		}
		
	}
	
	
}
