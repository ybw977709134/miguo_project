package co.onemeter.oneapp.ui;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.PrefUtil;

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
			q.find(R.id.text_third_r).text(getString(R.string.class_wait_submit));
		}
		q.find(R.id.les_lay_first).clicked(this);
		q.find(R.id.les_lay_second).clicked(this);
		q.find(R.id.les_lay_third).clicked(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(Constants.LESSONID, lessonId);
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.les_lay_first:
			intent.setClass(this, LessonStatusActivity.class);
			startActivity(intent);
			break;
		case R.id.les_lay_second:
			intent.setClass(this, HomeworkActivity.class);
			startActivity(intent);
			break;
		case R.id.les_lay_third:
			intent.setClass(this, LessonParentFeedbackActivity.class);
			startActivity(intent);
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
