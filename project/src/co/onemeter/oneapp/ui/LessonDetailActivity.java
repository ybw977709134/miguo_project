package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Camera;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.LessonDetail;
import org.wowtalk.api.LessonHomework;
import org.wowtalk.api.LessonParentFeedback;
import org.wowtalk.api.LessonPerformance;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 课表详情页面。
 * Created by yl on 23/12/2014.
 */
public class LessonDetailActivity extends Activity implements OnClickListener {
	
	private static final int REQ_PARENT_FEEDBACK = 100;
	private static final int REQ_CLASSROOM_FEEDBACK = 1001;
	private static final int REQ_CAMERA_FEEDBACK = 1002;
	private int lessonId;
	private String classId;
	private String schoolId;
	private Lesson lesson;
	private long startdate;
	private long enddate;
	private int roomId;
//	private String roomName;
//	private String classTime;
//	private String classLength;
	
	private TextView text_classroom_name;
	private TextView text_camera_num;
	private TextView text_issecond;
	private TextView text_isfirst;
	private TextView text_suggusiton_count;
	private TextView text_third_r;
	
	private List<LessonDetail> lessonDetails;
	private List<Camera> lessoonDetails_camera;
	private List<LessonHomework> lessoonDetails_homework;
	private List<LessonPerformance> lessoonDetails_performance;
	private List<LessonParentFeedback> lessoonDetails_parent_feedback;
	
	private long currentTime;
//	private long classTimesStamps;
//	private long classEndTimeStamps;
	
	private MessageBox msgbox;
	private ImageView bottom_camera_standard_list_divider;
	private ImageView top_camera_standard_list_divider;
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lesson_detail);
		initView();
		getLessonDetail();
	};

	private void initView(){
		msgbox = new MessageBox(this);
		AQuery q = new AQuery(this);
		Intent intent = getIntent();
		if(null != intent){
			q.find(R.id.lesson_title).text(intent.getStringExtra("title"));
			lessonId = intent.getIntExtra(Constants.LESSONID,0);
			classId = intent.getStringExtra("classId");
			lesson = intent.getParcelableExtra("lesson");
			schoolId = intent.getStringExtra("schoolId");
			startdate = intent.getLongExtra("startdate", 0);
			enddate = intent.getLongExtra("enddate", 0);
//			classTime = intent.getStringExtra("classTime");
//			classLength = intent.getStringExtra("classLength");
		}
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.title_refresh).clicked(this);
		q.find(R.id.tv_back).clicked(this);
		
		LinearLayout lay_classroom = (LinearLayout) q.find(R.id.les_lay_classroom).getView();
		LinearLayout lay_first = (LinearLayout) q.find(R.id.les_lay_first).getView();
		LinearLayout lay_second = (LinearLayout) q.find(R.id.les_lay_second).getView();
		LinearLayout lay_third = (LinearLayout) q.find(R.id.les_lay_third).getView();
		LinearLayout lay_camera = (LinearLayout) q.find(R.id.les_lay_camera).getView();
		bottom_camera_standard_list_divider = (ImageView) findViewById(R.id.bottom_camera_standard_list_divider);
		top_camera_standard_list_divider = (ImageView) findViewById(R.id.top_camera_standard_list_divider);
		
		
		q.find(R.id.text_camera).textColor(getResources().getColor(R.color.text_gray4));
		lay_camera.setEnabled(false);
		
		if(lesson.end_date * 1000 > System.currentTimeMillis()){
			q.find(R.id.text_first).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_second).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_third).textColor(getResources().getColor(R.color.text_gray4));
			lay_first.setEnabled(false);
			lay_second.setEnabled(false);
			lay_third.setEnabled(false);
		}
//		else{
//			q.find(R.id.text_classroom).textColor(getResources().getColor(R.color.text_gray4));
//			q.find(R.id.text_camera).textColor(getResources().getColor(R.color.text_gray4));
//			lay_classroom.setEnabled(false);
//			lay_camera.setEnabled(false);
//		}
		if(isTeacher()){
			q.find(R.id.text_first).text(getString(R.string.class_lesson_situation_table));
			q.find(R.id.text_second).text(getString(R.string.class_set_homework));
			q.find(R.id.text_third).text(getString(R.string.class_parent_suggestion));
			q.find(R.id.text_classroom).text(getString(R.string.class_lesson_classroom));
			q.find(R.id.text_first_r).text("");
			q.find(R.id.text_third_r).text("");
			q.find(R.id.text_classroom_r).text("");
		}else{
//			q.find(R.id.text_isfirst).text(getString(R.string.class_no_situation));
			q.find(R.id.text_classroom).textColor(getResources().getColor(R.color.text_gray4));
			lay_classroom.setEnabled(false);
			lay_camera.setVisibility(View.GONE);
			bottom_camera_standard_list_divider.setVisibility(View.GONE);
			top_camera_standard_list_divider.setVisibility(View.GONE);
		}
		lay_first.setOnClickListener(this);
		lay_second.setOnClickListener(this);
		lay_third.setOnClickListener(this);
		lay_classroom.setOnClickListener(this);
		lay_camera.setOnClickListener(this);
		
		if(!isTeacher()){
			if(Database.getInstance(this).fetchLessonParentFeedback(lessonId, PrefUtil.getInstance(this).getUid()) != null){
				q.find(R.id.text_third_r).text(getString(R.string.class_parent_opinion_submitted));
			}else{
				q.find(R.id.text_third_r).text(getString(R.string.class_wait_submit));
			}
		}
		lessonDetails = new ArrayList<LessonDetail>();
		lessoonDetails_camera = new ArrayList<Camera>();
		lessoonDetails_homework = new ArrayList<LessonHomework>();
		lessoonDetails_performance = new ArrayList<LessonPerformance>();
		lessoonDetails_parent_feedback = new ArrayList<LessonParentFeedback>();
		
		text_classroom_name = (TextView) findViewById(R.id.text_classroom_name);
		text_camera_num = (TextView) findViewById(R.id.text_camera_num);
		text_issecond = (TextView) findViewById(R.id.text_issecond);
		text_isfirst = (TextView) findViewById(R.id.text_isfirst);
		text_suggusiton_count = (TextView) findViewById(R.id.text_suggusiton_count);
		text_third_r = (TextView) findViewById(R.id.text_third_r);
		
//		String[] classTimes = classTime.split(":");
//		String[] classLengths = classLength.split(":");
		currentTime = System.currentTimeMillis()/1000;
//		classTimesStamps =Integer.parseInt(classTimes[0])*3600 + Integer.parseInt(classTimes[1])*60+startdate;
//		classEndTimeStamps = Integer.parseInt(classLengths[0])*3600 + Integer.parseInt(classLengths[1])*60 + classTimesStamps;
		if(currentTime > enddate){
			q.find(R.id.text_classroom).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_camera).textColor(getResources().getColor(R.color.text_gray4));
			lay_classroom.setEnabled(false);
			lay_camera.setEnabled(false);
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
	}
	
	@Override
	public void onClick(View v) {
		PrefUtil mPre = PrefUtil.getInstance(this);
		Database mDbHelper = Database.getInstance(this);
		Intent intent = new Intent();
		intent.putExtra(Constants.LESSONID, lessonId);
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.tv_back:
			finish();
			break;
		case R.id.title_refresh:
			getLessonDetail();
			break;
		case R.id.les_lay_first:
			if(isTeacher()){
				intent.setClass(this, TeacherCheckActivity.class);
				intent.putExtra("classId", classId);
				intent.putExtra("schoolId", schoolId);
				intent.putExtra("lvFlag", 0);
				startActivity(intent);
			}else{
				int property_value = 0;
				for(LessonPerformance performance :lessoonDetails_performance){
					if(performance.student_id.equals(mPre.getUid())){
						property_value = performance.property_value;
					}
				}
				if(property_value == 2 || property_value == 1){
					msgbox.toast("学生没有出席这节课，不能查看课堂点评");
				}else{
					intent.putExtra(Constants.STUID, mPre.getUid());
					intent.putExtra(LessonStatusActivity.FALG, false);
					intent.setClass(this, LessonStatusActivity.class);
					startActivity(intent);
				}
				
			}

			break;
		case R.id.les_lay_second:
			intent.setClass(this, HomeworkActivity.class);
			startActivity(intent);
//			msgbox.toast("功能实现中。。。");
			break;
		case R.id.les_lay_third:
			if(isTeacher()){
				intent.setClass(this, TeacherCheckActivity.class);
				intent.putExtra("classId", classId);
				intent.putExtra("schoolId", schoolId);
				intent.putExtra("lvFlag", 1);
				startActivity(intent);
			}else{
				int property_value = 0;
				for(LessonPerformance performance :lessoonDetails_performance){
					if(performance.student_id.equals(mPre.getUid())){
						property_value = performance.property_value;
					}
				}
				if(property_value == 2 || property_value == 1){
					msgbox.toast("学生没有出席这节课，不能提交家长意见");
				}else{
					LessonParentFeedback feedback = mDbHelper.fetchLessonParentFeedback(lessonId, mPre.getUid());
					if(feedback == null){
						intent.putExtra(Constants.STUID, mPre.getUid());
						intent.putExtra(LessonStatusActivity.FALG, false);
						intent.setClass(this, LessonParentFeedbackActivity.class);
						startActivityForResult(intent,REQ_PARENT_FEEDBACK);
					}else{
						Moment moment = mDbHelper.fetchMoment(feedback.moment_id + "");
						if(moment != null){
							FeedbackDetailActivity.launch(LessonDetailActivity.this,moment,PrefUtil.getInstance(this).getUserName(),null);
						}else{
							new MessageBox(this).toast(R.string.class_parent_opinion_not_submitted,500);
						}
					}
				}
				
			}
			break;
			
		case R.id.les_lay_classroom:
//			String[] classTimes = classTime.split(":");
//			String[] classLengths = classLength.split(":");
//			long currentTime = System.currentTimeMillis()/1000;
//			long classTimesStamps =Integer.parseInt(classTimes[0])*3600 + Integer.parseInt(classTimes[1])*60+startdate;
//			long classEndTimeStamps = Integer.parseInt(classLengths[0])*3600 + Integer.parseInt(classLengths[1])*60 + classTimesStamps;
			if(currentTime > startdate && currentTime < enddate){
				Toast.makeText(this, "正在上课，无法修改", Toast.LENGTH_SHORT).show();
			}else{
				intent.setClass(this, ClassroomActivity.class);
			    intent.putExtra("schoolId", schoolId);
			    intent.putExtra("start_date", startdate);
			    intent.putExtra("end_date", enddate);
			    intent.putExtra("lessonId", lessonId);
			    startActivityForResult(intent,REQ_CLASSROOM_FEEDBACK);
			}

			break;
		case R.id.les_lay_camera:
			intent.setClass(this, CameraActivity.class);
			intent.putExtra("schoolId", schoolId);
			intent.putExtra("roomId", roomId);
			startActivityForResult(intent, REQ_CAMERA_FEEDBACK);
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(requestCode == REQ_PARENT_FEEDBACK){
				AQuery q = new AQuery(this);
				q.find(R.id.text_third_r).text(getString(R.string.class_parent_opinion_submitted));
			}else if(requestCode == REQ_CLASSROOM_FEEDBACK){
//				roomName = data.getStringExtra("roomName");
//				text_classroom_name.setText(roomName);
				lessonDetails.clear();
				getLessonDetail();
			}else if(requestCode == REQ_CAMERA_FEEDBACK){
				getLessonDetail();
			}
		}
	}
	
	private boolean isTeacher(){
		if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
			return true;
		}
		return false;
	}
	
	private void getLessonDetail(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				lessoonDetails_camera.clear();
				lessoonDetails_homework.clear();
				lessoonDetails_performance.clear();
				lessoonDetails_parent_feedback.clear();
				return LessonWebServerIF.getInstance(LessonDetailActivity.this).getLessonDetail(lessonId,lessonDetails,
						lessoonDetails_camera,lessoonDetails_homework,lessoonDetails_performance,lessoonDetails_parent_feedback);
			}
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				if (ErrorCode.OK == result) {
					int count = 0;
				    int count_on = 0;
					text_classroom_name.setText(lessonDetails.get(0).room_name);
					roomId = lessonDetails.get(0).room_id;

					if(lessoonDetails_camera.get(0).camera_name == null){
						text_camera_num.setText("打开0/0");
					}else{
						for(Camera camera:lessoonDetails_camera){
						count++;
						if(camera.status == 1){
							count_on++;
						}
					}
					if(lessonDetails.get(0).room_name == null || lessonDetails.get(0).room_name.isEmpty()){
						text_camera_num.setText("未设置");
						text_classroom_name.setText("未设置");
					}else{
						text_camera_num.setText("打开"+count_on+"/"+count);
						}
					}	
					
					
					if(lessoonDetails_homework.get(0).title != null){
						text_issecond.setText("已布置");
					}else if(lessoonDetails_homework.get(0).title == null){
						text_issecond.setText("未布置");
					}
					if(isTeacher()){
						if(lessoonDetails_performance.get(0).student_id != null){
							text_isfirst.setText(R.string.class_had_situation);
						}else if(lessoonDetails_performance.get(0).student_id == null){
							text_isfirst.setText(R.string.class_no_situation);
						}
						
						if(lessoonDetails_parent_feedback.get(0).student_id != null){
							text_suggusiton_count.setVisibility(View.VISIBLE);
							
							text_suggusiton_count.setText(String.valueOf(lessoonDetails_parent_feedback.size()));
						}else{
							text_suggusiton_count.setVisibility(View.GONE);
							
							text_third_r.setText(getString(R.string.class_parent_opinion_not_submitted));
						}
					}else{
						String myUid = PrefUtil.getInstance(LessonDetailActivity.this).getUid();
						String studentId = null;
						for(LessonPerformance performance : lessoonDetails_performance){
							if(performance.student_id !=null){
								if(performance.student_id.equals(myUid)){
								studentId = performance.student_id;
								}
							}
							
						}
						if(studentId != null){
							text_isfirst.setText(R.string.class_had_situation);
						}else{
							text_isfirst.setText(R.string.class_no_situation);
						}
					}
					
				}
				
			}

		});
	}
}
