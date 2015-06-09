package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GetLessonHomework;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 家庭作业作业页面.
 * Created by pzy on 11/10/14. Modified by yl on 23/12/2014 Modified by zz on 10/05/2015
 */
public class HomeworkActivity extends Activity implements OnClickListener, OnItemClickListener{
	private static final int REQ_PARENT_ADDHOMEWORK = 100;
	private static final int REQ_PARENT_DELHOMEWORK = 1001;
	private static final int REQ_STUDENT_SIGNUP = 1002;
	private static final int REQ_PARENT_DELHOMEWORKRESULT = 1003;
	private TextView tv_class_name;
	private TextView tv_lesson_name;
	private TextView tv_addhomework_state;
	private TextView tv_signup_homework_state;
	private int lessonId;
	private List<Map<String, Object>> homeworkStates;
	private HomeworkStateAdapter adapter;
	private AQuery q;
	private ListView lvHomework;
	private Database mDb;
	private LessonAddHomework addHomework ;
	private MessageBox msgbox;
	private int homework_id;
	
	private GetLessonHomework getLessonHomework;
	private String studentId = null;
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
		initView();
		if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
			getHomeworkState_student(lessonId,studentId);
		}else if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			getHomeworkState(lessonId);
		}
		
	}
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what != ErrorCode.INVALID_ARGUMENT){
				msgbox.dismissWait();
				msgbox.toast(R.string.class_lesson_no_homework,500);
			}else {
				msgbox.dismissWait();
			}
		};
	};
	private void initView() {
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
		lvHomework = (ListView) findViewById(R.id.lv_homework);
		q = new AQuery(this);
		mDb = new Database(this);
		msgbox = new MessageBox(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.layout_sign_class).clicked(this);
		q.find(R.id.layout_signup_homework).clicked(this);

		homeworkStates = new ArrayList<Map<String, Object>>();
		tv_class_name = (TextView) findViewById(R.id.tv_class_name);
		tv_lesson_name = (TextView) findViewById(R.id.tv_lesson_name);
		tv_class_name.setText(getIntent().getStringExtra("class_name"));
		tv_lesson_name.setText(getIntent().getStringExtra("lesson_name"));
		tv_addhomework_state = (TextView) findViewById(R.id.tv_addhomework_state);
		tv_signup_homework_state = (TextView) findViewById(R.id.tv_signup_homework_state);
		lvHomework.setOnItemClickListener(this);
		studentId = PrefUtil.getInstance(this).getUid();
		getLessonHomework = new GetLessonHomework();
		addHomework = mDb.fetchLessonAddHomework(lessonId);
		if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
			lvHomework.setVisibility(View.GONE);
			q.find(R.id.layout_signup_homework).visibility(View.VISIBLE);
		}else{
			lvHomework.setVisibility(View.VISIBLE);
			q.find(R.id.layout_signup_homework).visibility(View.GONE);
			q.find(R.id.divider_signup_homework).visibility(View.GONE);
		}
		adapter = new HomeworkStateAdapter(homeworkStates);
		lvHomework.setAdapter(adapter);
	}

	private void getHomeworkState_student(final int lessonId,final String studentId){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(HomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,1);
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				if(result == 1){
					homework_id = getLessonHomework.id;
					tv_addhomework_state.setText("已布置");
				}else if(result == 0){
					tv_addhomework_state.setText("未布置");
				}
				
			}
			
		});
		
	}
	private void getHomeworkState(final int lessonId){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, List<Map<String, Object>>>() {
			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {			
				List<Map<String, Object>> result= LessonWebServerIF.getInstance(HomeworkActivity.this).get_homework_state(lessonId);
				return result;
			}
			protected void onPostExecute(List<Map<String, Object>> result) {
				msgbox.dismissWait();
				homeworkStates.clear();	
				homeworkStates.addAll(result);
				if(homeworkStates.size() == 0){
					tv_addhomework_state.setText("未布置");
				}else{
					tv_addhomework_state.setText("已布置");
				}
				
				ListViewUtils.setListViewHeightBasedOnChildren(lvHomework);
			}
			
		});
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;
			
		case R.id.layout_sign_class:	
			if(isTeacher()){
				if(homeworkStates.size() == 0){
					Intent i = new Intent(HomeworkActivity.this, AddHomeworkActivity.class);
					i.putExtra("lessonId",lessonId);
					startActivityForResult(i, REQ_PARENT_ADDHOMEWORK);
				}else{
					Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
					Moment moment = mDb.fetchMoment(addHomework.moment_id + "");
					i.putExtra("moment", moment);
			        i.putExtra("lessonId",lessonId);
			        i.putExtra("studentId", String.valueOf(homeworkStates.get(0).get("stu_uid")));
			        startActivityForResult(i, REQ_PARENT_DELHOMEWORK);
				}
			}else{
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						int errno;
						try {
							errno = LessonWebServerIF.getInstance(HomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,1);
							if(errno == ErrorCode.INVALID_ARGUMENT){						
								Moment moment = mDb.fetchMoment(String.valueOf(getLessonHomework.moment_id));
								if(moment != null){
									Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
									i.putExtra("moment", moment);
							        i.putExtra("lessonId",lessonId);
							        i.putExtra("studentId", studentId);
							        i.putExtra("flag", 1);
									startActivity(i);
									mHandler.sendEmptyMessage(errno);
								}else{
									mHandler.sendEmptyMessage(errno);
								}
							}else{
								mHandler.sendEmptyMessage(errno);
							}
						} catch (Exception e) {
							mHandler.sendEmptyMessage(ErrorCode.BAD_RESPONSE);
						}
					}
				}).start();
			}
			
			
			break;
		case R.id.layout_signup_homework:
			if(homework_id == 0){
				Toast.makeText(this, "还未布置作业", Toast.LENGTH_SHORT).show();
			}else{
				int momentId = 0;
				int homeworkResult_id = 0;
				for(int j = 0;j < getLessonHomework.stuResultList.size();j++){
					if(studentId.equals(getLessonHomework.stuResultList.get(j).student_id)){
						momentId = getLessonHomework.stuResultList.get(j).moment_id;
						homeworkResult_id = getLessonHomework.stuResultList.get(j).id;
					}
				}
				if(momentId == 0){
					Intent i = new Intent(HomeworkActivity.this, SignHomeworkResultkActivity.class);
					i.putExtra("homework_id", homework_id);
					startActivityForResult(i, REQ_STUDENT_SIGNUP);
				}else{				
//					Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);				
//					Moment moment = mDb.fetchMoment(momentId + "");
//					i.putExtra("moment", moment);
//			        i.putExtra("lessonId",lessonId);
//			        i.putExtra("homeworkResult_id",homeworkResult_id);
//			        i.putExtra("flag", 2);
//			        startActivityForResult(i, REQ_PARENT_DELHOMEWORKRESULT);
					Intent i = new Intent(HomeworkActivity.this, SubmitHomeWorkActivity.class);
					i.putExtra("lessonId",lessonId);
					i.putExtra("student_uid",studentId);		
					startActivity(i);
				}
				
				
			}
			break;
		case R.id.lay_footer_add:
//			showAddHomeworkDialog();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(requestCode == REQ_PARENT_ADDHOMEWORK){
				getHomeworkState(lessonId);
			}else if(requestCode == REQ_PARENT_DELHOMEWORK){
				getHomeworkState(lessonId);
			}else if(requestCode == REQ_STUDENT_SIGNUP){
				tv_signup_homework_state.setText("已提交");
			}else if(requestCode == REQ_PARENT_DELHOMEWORKRESULT){
				tv_signup_homework_state.setText("未提交");
			}
		}
	}
	class HomeworkStateAdapter extends BaseAdapter{
		private List<Map<String, Object>> homeworkStates;
		public HomeworkStateAdapter(List<Map<String, Object>> homeworkStates){
			this.homeworkStates = homeworkStates;
		}
		@Override
		public int getCount() {
			return homeworkStates.size();
		}

		@Override
		public Object getItem(int position) {
			return homeworkStates.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_homework_state, parent, false);
				holder.tv_student_name = (TextView) convertView.findViewById(R.id.tv_student_name);
				holder.tv_homework_state = (TextView) convertView.findViewById(R.id.tv_homework_state);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}	
			int state = Integer.parseInt(String.valueOf(homeworkStates.get(position).get("stu_state")));
			String stateStr = null;
			if(state == 0){
				stateStr = "提醒交作业";
				holder.tv_homework_state.setBackgroundResource(R.drawable.btn_small_valid);
                holder.tv_homework_state.setPadding(2,2,2,2);
				holder.tv_homework_state.setTextColor(0xff8eb4e6);
			}else if(state == 1){
				stateStr = "新提交";
				holder.tv_homework_state.setTextColor(0xff8eb4e6);
			}else if(state == 2){
				stateStr = "已批改";
			}
			holder.tv_student_name.setText(String.valueOf(homeworkStates.get(position).get("stu_name")));
			holder.tv_homework_state.setText(stateStr);
			return convertView;
		}
		class ViewHodler{
			TextView tv_student_name;
			TextView tv_homework_state;
		}		
		
	}
	private boolean isTeacher(){
		if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
			return true;
		}
		return false;
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if(Integer.parseInt(String.valueOf(homeworkStates.get(position).get("stu_state"))) != 0){
			Intent i = new Intent(HomeworkActivity.this, SubmitHomeWorkActivity.class);
			i.putExtra("lessonId",lessonId);
			i.putExtra("student_uid",String.valueOf(homeworkStates.get(position).get("stu_uid")));		
			i.putExtra("result_id", Integer.parseInt(String.valueOf(homeworkStates.get(position).get("result_id"))));
			i.putExtra("stu_name",String.valueOf(homeworkStates.get(position).get("stu_name")));	
			startActivity(i);
		}
	}
}