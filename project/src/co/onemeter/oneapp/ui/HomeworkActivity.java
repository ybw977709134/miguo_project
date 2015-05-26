package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import org.wowtalk.api.Database;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.ui.MessageBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 家庭作业作业页面.
 * Created by pzy on 11/10/14. Modified by yl on 23/12/2014
 */
public class HomeworkActivity extends Activity implements OnClickListener, OnItemClickListener{
	private static final int REQ_PARENT_ADDHOMEWORK = 100;
	private static final int REQ_PARENT_DELHOMEWORK = 1001;
	private TextView tv_class_name;
	private TextView tv_lesson_name;
	private TextView tv_addhomework_state;
	private int lessonId;
//	private List<LessonHomework> lessonHomeworkz;
	private List<Map<String, Object>> homeworkStates;
	private HomeworkStateAdapter adapter;
//	private List<String> homeworktitles;
	private AQuery q;
//	private HomeWorkArrayAdater adapter;
//
	private ListView lvHomework;
	private Database mDb;
	private LessonAddHomework addHomework ;
	private MessageBox msgbox;
	private int homeworkId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
		initView();
		getHomeworkState(lessonId);
	}

	private void initView() {
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
		lvHomework = (ListView) findViewById(R.id.lv_homework);
		q = new AQuery(this);
		mDb = new Database(this);
		msgbox = new MessageBox(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.layout_sign_class).clicked(this);

		homeworkStates = new ArrayList<Map<String, Object>>();
		tv_class_name = (TextView) findViewById(R.id.tv_class_name);
		tv_lesson_name = (TextView) findViewById(R.id.tv_lesson_name);
		tv_class_name.setText(getIntent().getStringExtra("class_name"));
		tv_lesson_name.setText(getIntent().getStringExtra("lesson_name"));
		tv_addhomework_state = (TextView) findViewById(R.id.tv_addhomework_state);
		lvHomework.setOnItemClickListener(this);
		
		addHomework = mDb.fetchLessonAddHomework(lessonId);
	}

	private void getHomeworkState(final int lessonId){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, List<Map<String, Object>>>() {
			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {
				
				List<Map<String, Object>> result= LessonWebServerIF.getInstance(HomeworkActivity.this).get_homework_state(lessonId);
				Log.d("---------------result.size---------------", result.size()+"");
				return result;
			}
			protected void onPostExecute(List<Map<String, Object>> result) {
				msgbox.dismissWait();
				homeworkStates.clear();
				homeworkStates.addAll(result);
				adapter = new HomeworkStateAdapter(homeworkStates);
				adapter.notifyDataSetChanged();
				lvHomework.setAdapter(adapter);
				ListViewUtils.setListViewHeightBasedOnChildren(lvHomework);
				Log.d("---------------homeworkStates.size---------------", homeworkStates.size()+"");
				if(homeworkStates.size() == 0){
					tv_addhomework_state.setText("未布置");
				}else{
					tv_addhomework_state.setText("已布置");
				}
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
			if(homeworkStates.size() == 0){
				Intent i = new Intent(HomeworkActivity.this, AddHomeworkActivity.class);
				i.putExtra("lessonId",lessonId);
				startActivityForResult(i, REQ_PARENT_ADDHOMEWORK);
			}else{
				Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
				Moment moment = mDb.fetchMoment(addHomework.moment_id + "");
				i.putExtra("moment", moment);
		        i.putExtra("lessonId",lessonId);
		        startActivityForResult(i, REQ_PARENT_DELHOMEWORK);
//				LessonHomeworkActivity.launch(HomeworkActivity.this,moment,lessonId);
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
//				getHomeworkState(lessonId);
				tv_addhomework_state.setText("已布置");
			}else if(requestCode == REQ_PARENT_DELHOMEWORK){
				getHomeworkState(lessonId);
				tv_addhomework_state.setText("未布置");
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
			}else if(state == 1){
				stateStr = "新提交";
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.d("-------------homeworkStates--------------", homeworkStates.get(0).get("homework_id")+"");
	}
}