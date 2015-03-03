package co.onemeter.oneapp.ui;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.LessonParentFeedback;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

/**
 * 课表详情页面。
 * Created by yl on 23/12/2014.
 */
public class LessonDetailActivity extends Activity implements OnClickListener {
	
	private static final int REQ_PARENT_FEEDBACK = 100;
	
	private int lessonId;
	private String classId;
	private Lesson lesson;
	
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lesson_detail);
		initView();
	};

	private void initView(){
		AQuery q = new AQuery(this);
		Intent intent = getIntent();
		if(null != intent){
			q.find(R.id.lesson_title).text(intent.getStringExtra("title"));
			lessonId = intent.getIntExtra(Constants.LESSONID,0);
			classId = intent.getStringExtra("classId");
			lesson = intent.getParcelableExtra("lesson");
		}
		q.find(R.id.title_back).clicked(this);
		
		LinearLayout lay_classroom = (LinearLayout) q.find(R.id.les_lay_classroom).getView();
		LinearLayout lay_first = (LinearLayout) q.find(R.id.les_lay_first).getView();
		LinearLayout lay_second = (LinearLayout) q.find(R.id.les_lay_second).getView();
		LinearLayout lay_third = (LinearLayout) q.find(R.id.les_lay_third).getView();
		LinearLayout lay_camera = (LinearLayout) q.find(R.id.les_lay_camera).getView();
		
		if(lesson.end_date * 1000 > System.currentTimeMillis()){
			q.find(R.id.text_first).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_second).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_third).textColor(getResources().getColor(R.color.text_gray4));
			q.find(R.id.text_classroom).textColor(getResources().getColor(R.color.text_gray4));
			lay_first.setEnabled(false);
			lay_second.setEnabled(false);
			lay_third.setEnabled(false);
			lay_classroom.setEnabled(false);
			lay_camera.setEnabled(false);
		}
		if(isTeacher()){
			q.find(R.id.text_first).text(getString(R.string.class_lesson_situation_table));
			q.find(R.id.text_second).text(getString(R.string.class_set_homework));
			q.find(R.id.text_third).text(getString(R.string.class_parent_suggestion));
			q.find(R.id.text_classroom).text(getString(R.string.class_lesson_classroom));
			q.find(R.id.text_first_r).text("");
			q.find(R.id.text_third_r).text("");
			q.find(R.id.text_classroom_r).text("");
		}else{
			q.find(R.id.text_first_r).text(getString(R.string.class_wait_confirm));
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
		case R.id.les_lay_first:
			if(isTeacher()){
				intent.setClass(this, TeacherCheckActivity.class);
				intent.putExtra("classId", classId);
				intent.putExtra("lvFlag", 0);
			}else{
				intent.putExtra(Constants.STUID, mPre.getUid());
				intent.putExtra(LessonStatusActivity.FALG, false);
				intent.setClass(this, LessonStatusActivity.class);
			}
			startActivity(intent);
			break;
		case R.id.les_lay_second:
			intent.setClass(this, HomeworkActivity.class);
			startActivity(intent);
			break;
		case R.id.les_lay_third:
			if(isTeacher()){
				intent.setClass(this, TeacherCheckActivity.class);
				intent.putExtra("classId", classId);
				intent.putExtra("lvFlag", 1);
				startActivity(intent);
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
						FeedbackDetailActivity.launch(LessonDetailActivity.this,moment,PrefUtil.getInstance(this).getUserName());
					}else{
						new MessageBox(this).toast(R.string.class_parent_opinion_not_submitted,500);
					}
				}
			}
			break;
			
		case R.id.les_lay_classroom:
			intent.setClass(this, ClassroomActivity.class);
			startActivity(intent);
			break;
		case R.id.les_lay_camera:
			intent.setClass(this, CameraActivity.class);
			startActivity(intent);
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
			}
		}
	}
	
	private boolean isTeacher(){
		if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
			return true;
		}
		return false;
	}
	
}
