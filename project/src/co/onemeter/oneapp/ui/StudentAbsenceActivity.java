package co.onemeter.oneapp.ui;

import java.util.Date;

import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Connect2;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.msg.MessageComposerActivityBase;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
	private int lessonID = 0;
	private String teacherID = null;
	private String classId_intent = null; //通过intent传进的classId
	
	private PrefUtil mPref; 
	private MessageBox messageBox;
	private boolean isSendSuccess = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_student_absence);
		
		mPref = PrefUtil.getInstance(StudentAbsenceActivity.this);
		messageBox = new MessageBox(this);
		initView();
		layout_student_absence.setFocusable(true);
		layout_student_absence.setFocusableInTouchMode(true);
		layout_student_absence.requestFocus();

        /**
         * 处理来自ClassDetailActivity来的请假
         */
        classId_intent = getIntent().getStringExtra("classId");
		if(classId_intent != null){
            classID = classId_intent;
            layout_absence_class.setVisibility(View.GONE);
            findViewById(R.id.img_diliver_class_up).setVisibility(View.GONE);
            findViewById(R.id.img_diliver_class_bottom).setVisibility(View.GONE);
            ImageView img_divider = (ImageView) findViewById(R.id.img_diliver_lesson_up);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) img_divider.getLayoutParams();
            params.topMargin = 0;
            img_divider.setLayoutParams(params);
            textView_home_back.setText(getString(R.string.back));
        }
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		 if(event.getAction() == MotionEvent.ACTION_DOWN){  
		     if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
		    	 InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);  

			        return imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);   
		     }  
		  }  
		return super.onTouchEvent(event);
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
	
	/**
	 * 获取发送消息时的时间
	 * @return
	 */
	protected String getSentDate () {
        // set the sentDate according to the UTC offset
        long localDate = System.currentTimeMillis();
        int offset = mPref.getUTCOffset();
        long adjustedTime = localDate + offset * 1000L;
        Date adjustedDate = new Date(adjustedTime);
        return Database.chatMessage_dateToUTCString(adjustedDate);
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
			} else if (lessonID == 0) {
				alert("请选择请假课程");
			} else if (teacherID == null){
				alert("请选择请假教师");
			} else if (editText_absence_reason.getText().toString().length() == 0) {
				alert("请填写请假事由");
			} else {
				
				messageBox.showWaitImageSuccess("请假申请发送成功");
				
				//向老师请假
				String reason = "[请假]"+textView_teacher_name.getText().toString()+"你好"+"\n"
						+"班级： "+textView_class_name.getText().toString()+"\n"
						+"课程： "+textView_lesson_name.getText().toString()+"\n"
						+"事由： "+editText_absence_reason.getText().toString();

				
				final ChatMessage message = new ChatMessage();
				message.chatUserName = teacherID;

				message.messageContent = reason;
				message.msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
				message.sentStatus = ChatMessage.SENTSTATUS_SENDING;
				message.sentDate = getSentDate();
				message.uniqueKey = Database.chatMessageSentDateToUniqueKey(message.sentDate);
				message.ioType = ChatMessage.IOTYPE_OUTPUT;
				
				message.primaryKey = new Database(StudentAbsenceActivity.this)
			                            .storeNewChatMessage(message, false);

			        new Thread(new Runnable() {
			        	
			            @Override
			            public void run() {

			            	WowTalkVoipIF.getInstance(StudentAbsenceActivity.this).fSendChatMessage(message);
			            	
			            	try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							StudentAbsenceActivity.this.finish();

			            }
			        }).start();
		}
			
			break;
			
		case R.id.layout_absence_class://获取班级名和id
			Intent intentClass = new Intent(this,SelectClassActivity.class);
			startActivityForResult(intentClass, REQ_ABSENCE_CLASS);
			break;
			
		case R.id.layout_absence_lesson://获取课程名和id
			if (classID != null) {
				Intent intentLesson = new Intent(this,SelectLessonActivity.class);
				intentLesson.putExtra("classId", classID);
				startActivityForResult(intentLesson, REQ_ABSENCE_LESSON);
			} else {
				alert("请选择请假班级");
			}
			break;
			
		case R.id.layout_absence_teacher://获取老师名和id
			
			if (classID == null) {
				alert("请选择请假班级");
			} else if (lessonID == 0) {
				alert("请选择请假课程");
			} else {
				Intent intentLesson = new Intent(this,SelectTeacherActivity.class);
				intentLesson.putExtra("classId", classID);
				startActivityForResult(intentLesson, REQ_ABSENCE_TEACHER);
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
					lessonID = data.getIntExtra("lesson_id", 0);
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
	
	@Override
	protected void onDestroy() {
		if (mPref != null) {
			mPref = null;
		}
		super.onDestroy();
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





















