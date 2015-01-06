package co.onemeter.oneapp.ui;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
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

/**
 * 课表详情页面。
 * Created by yl on 23/12/2014.
 */
public class LessonDetailActivity extends Activity implements OnClickListener {
	
	private int lessonId;
	private String classId;
	private Lesson lesson;
	private MessageBox mMsgBox = new MessageBox(this);
	
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
		if(isTeacher()){
			q.find(R.id.text_first).text(getString(R.string.class_lesson_situation_table));
			q.find(R.id.text_second).text(getString(R.string.class_set_homework));
			q.find(R.id.text_third).text(getString(R.string.class_parent_suggestion));
			q.find(R.id.text_first_r).text("");
			q.find(R.id.text_third_r).text("");
		}else{
			q.find(R.id.text_first_r).text(getString(R.string.class_wait_confirm));
		}
		q.find(R.id.les_lay_first).clicked(this);
		q.find(R.id.les_lay_second).clicked(this);
		q.find(R.id.les_lay_third).clicked(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(!isTeacher()){
			AQuery q = new AQuery(this);
			if(Database.getInstance(this).fetchLessonParentFeedback(lessonId, PrefUtil.getInstance(this).getUid()) != null){
				q.find(R.id.text_third_r).text(getString(R.string.class_parent_opinion_submitted));
			}else{
				q.find(R.id.text_third_r).text(getString(R.string.class_wait_submit));
			}
		}
		
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
			if(lesson.start_date * 1000 > System.currentTimeMillis()){
				if(!mMsgBox.isWaitShowing()){
					mMsgBox.toast(R.string.class_lesson_no_start);
				}
				return;
			}
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
			if(lesson.start_date * 1000 > System.currentTimeMillis()){
				if(!mMsgBox.isWaitShowing()){
					mMsgBox.toast(R.string.class_lesson_no_start);
				}
				return;
			}
			intent.setClass(this, HomeworkActivity.class);
			startActivity(intent);
			break;
		case R.id.les_lay_third:
			if(lesson.start_date * 1000 > System.currentTimeMillis()){
				if(!mMsgBox.isWaitShowing()){
					mMsgBox.toast(R.string.class_lesson_no_start);
				}
				return;
			}
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
					startActivity(intent);
				}else{
					Moment moment = mDbHelper.fetchMoment(feedback.moment_id + "");
					if(moment != null){
						MomentDetailActivity.launch(LessonDetailActivity.this,moment);
					}else{
						new MessageBox(this).toast(R.string.class_parent_opinion_not_submitted,500);
					}
				}
			}
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
	
}
