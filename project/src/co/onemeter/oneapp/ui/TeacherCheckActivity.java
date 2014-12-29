package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupMember;
import org.wowtalk.api.LessonParentFeedback;
import org.wowtalk.api.Moment;
import org.wowtalk.api.WowEventWebServerIF;
import org.wowtalk.api.WowLessonWebServerIF;
import org.wowtalk.api.WowMomentWebServerIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 学生列表页面。
 * Created by yl on 25/12/2014.
 */
public class TeacherCheckActivity extends Activity implements OnItemClickListener, OnClickListener {

	public static final int LESSITUATION = 0;
	public static final int PARENTSUG = 1;
	
	private ListView lvStu;
	private TextView txtTitle;
	
	private int lessonId;
	private int lvFlag;
	private String classId;
	
	private List<GroupMember> members;
	private List<GroupMember> stus;
	private Database mdbHelper;
	private StuAdapter adapter;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what != ErrorCode.OK){
				new MessageBox(TeacherCheckActivity.this).toast(R.string.class_parent_opinion_not_submitted);
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_teacher_check);
		initView();
		
		
	}

	private void initView(){
		lvStu= (ListView) findViewById(R.id.lvStu);
		txtTitle = (TextView) findViewById(R.id.txt_title);
		ImageButton titleBack = (ImageButton) findViewById(R.id.title_back);
		
		
		mdbHelper = Database.getInstance(this);
		
		Intent intent = getIntent();
		if(intent != null){
			lvFlag = intent.getIntExtra("lvFlag", 0);
			lessonId = intent.getIntExtra(Constants.LESSONID, 0);
			classId = intent.getStringExtra("classId");
			if(lvFlag == LESSITUATION){
				txtTitle.setText(R.string.class_lesson_situation_table);
			}else if(lvFlag == PARENTSUG){
				txtTitle.setText(R.string.class_parent_suggestion);
			}
		}
		
		stus = new ArrayList<GroupMember>();
		
//		members = mdbHelper.fetchGroupMembers(classId);
//		android.util.Log.i("-->>", members.toString());
		if(members == null || members.isEmpty()){
			new AsyncTask<Void, Void, Integer>(){

				@Override
				protected Integer doInBackground(Void... params) {
					return (Integer) WowTalkWebServerIF.getInstance(TeacherCheckActivity.this).fGroupChat_GetMembers(classId).get("code");
				}
				
				protected void onPostExecute(Integer result) {
					if(ErrorCode.OK == result){
						members = mdbHelper.fetchGroupMembers(classId);
						for (GroupMember m: members) {
							if(m.getAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
								stus.add(m);
							}
						}
						adapter = new StuAdapter(stus);
						lvStu.setAdapter(adapter);
					}
				};
				
			}.execute((Void)null);
		}else{
			adapter = new StuAdapter(stus);
			lvStu.setAdapter(adapter);
		}
		
		lvStu.setOnItemClickListener(this);
		titleBack.setOnClickListener(this);
	}
	

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if(lvFlag == LESSITUATION){
			Intent intent = new Intent();
			intent.putExtra(Constants.LESSONID, lessonId);
			intent.putExtra(Constants.STUID, stus.get(position).userID);
			intent.putExtra(LessonStatusActivity.FALG, true);
			intent.setClass(this, LessonStatusActivity.class);
			startActivity(intent);
		}else if(lvFlag == PARENTSUG){
//			intent.putExtra(LessonStatusActivity.FALG, true);
//			intent.setClass(this, LessonParentFeedbackActivity.class);
			final int pos = position;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					int errno = WowLessonWebServerIF.getInstance(TeacherCheckActivity.this).getLessonParentFeedback(lessonId, stus.get(pos).userID);
					if(errno == ErrorCode.OK){
						Database db = new Database(TeacherCheckActivity.this);
						LessonParentFeedback feedback = db.fetchLessonParentFeedback(lessonId, stus.get(pos).userID);
						Moment moment = db.fetchMoment(feedback.moment_id + "");
						MomentDetailActivity.launch(TeacherCheckActivity.this,moment);
					}else{
						mHandler.sendEmptyMessage(errno);
					}
				}
			}).start();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;

		default:
			break;
		}
	}
	
class StuAdapter extends BaseAdapter{
		
		private List<GroupMember> members;
		
		public StuAdapter(List<GroupMember> list){
			this.members = list;
		}

		@Override
		public int getCount() {
			return members.size();
		}

		@Override
		public Object getItem(int position) {
			return members.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(null == convertView){
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.listitem_stu, parent, false);
				holder.name = (TextView) convertView.findViewById(R.id.tv_stu_name);
				holder.msg = (TextView) convertView.findViewById(R.id.tv_stu_msg);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			GroupMember member = members.get(position);
			holder.name.setText(member.nickName);
			holder.msg.setText("");
			return convertView;
		}
		
		class ViewHolder{
			TextView name;
			TextView msg;
		}
		
	}
}
