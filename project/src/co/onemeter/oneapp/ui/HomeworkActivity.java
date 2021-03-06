package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.oneapp.utils.TimeHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GetLessonHomework;
import org.wowtalk.api.HomeWorkReview;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.DoubleClickedUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 家庭作业作业页面.
 * Created by pzy on 11/10/14. Modified by yl on 23/12/2014 Modified by zz on 10/05/2015
 */
public class HomeworkActivity extends Activity implements OnClickListener, OnItemClickListener ,SignHomeworkResultkActivity.RefreshHomework{
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
	private String schoolId;

    private MessageBox messageBox;
	
    private static HomeworkActivity instance;
    private int momentId = 0;
    private int homeworkResult_id = 0;
    private String teacherID;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
        messageBox = new MessageBox(this);
        instance =this;
		initView();
		if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
			getHomeworkState_student(lessonId,studentId);//学生账号 执行该网络请求获取作业信息
			
		}else if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			getHomeworkState(lessonId);//老师账号 获取作业列表
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
		schoolId = getIntent().getStringExtra("schoolId");
		lvHomework = (ListView) findViewById(R.id.lv_homework);
		q = new AQuery(this);
		mDb = new Database(this);
		msgbox = new MessageBox(this);
		q.find(R.id.title_back).clicked(this);
        q.find(R.id.textView_back).clicked(this);
		q.find(R.id.layout_sign_class).clicked(this);
		q.find(R.id.layout_signup_homework).clicked(this);
		q.find(R.id.title_refresh).clicked(this);

		homeworkStates = new ArrayList<Map<String, Object>>();
		tv_class_name = (TextView) findViewById(R.id.tv_class_name);
		tv_lesson_name = (TextView) findViewById(R.id.tv_lesson_name);
		tv_class_name.setText(getIntent().getStringExtra("class_name"));
		tv_lesson_name.setText(getIntent().getStringExtra("lesson_name"));
		tv_addhomework_state = (TextView) findViewById(R.id.tv_addhomework_state);
		tv_signup_homework_state = (TextView) findViewById(R.id.tv_signup_homework_state);
		lvHomework.setOnItemClickListener(this);
		studentId = PrefUtil.getInstance(this).getUid();//如果是老师账号类型,studentId为老师的账号ID
		getLessonHomework = new GetLessonHomework();
		addHomework = mDb.fetchLessonAddHomework(lessonId);

		if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
			lvHomework.setVisibility(View.GONE);
			q.find(R.id.layout_signup_homework).visibility(View.VISIBLE);
            //学生显示作业
            q.find(R.id.textView_sign_work).text("作业");
		}else{
			lvHomework.setVisibility(View.VISIBLE);
			q.find(R.id.layout_signup_homework).visibility(View.GONE);
			q.find(R.id.divider_signup_homework).visibility(View.GONE);
		}

		adapter = new HomeworkStateAdapter(homeworkStates);
		lvHomework.setAdapter(adapter);
	}
	
	@Override
	protected void onDestroy() {
		if (instance != null) {
			instance = null;
		}
		super.onDestroy();
	}

	public static HomeworkActivity getInstance() {
    	if (instance != null) {
    		return instance;
    	}
    	return null;
    }
	
	private void getHomeworkState_student(final int lessonId,final String studentId){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status = LessonWebServerIF.getInstance(HomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,1);
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				if(result == 1){
					getMomentId();
					homework_id = getLessonHomework.id;
					tv_addhomework_state.setText("已布置");
					int momentId = 0;
                    HomeWorkReview homeWorkReview = null;
					for(int j = 0;j < getLessonHomework.stuResultList.size();j++){
						if(studentId.equals(getLessonHomework.stuResultList.get(j).student_id)){
							momentId = getLessonHomework.stuResultList.get(j).moment_id;
                            homeWorkReview = getLessonHomework.stuResultList.get(j).homeWorkReview;
						}
					}

					if(momentId == 0){
						tv_signup_homework_state.setText("未提交");
					}else{

                        if (homeWorkReview != null) {
                            tv_signup_homework_state.setText("已批改");
                        } else {
                            tv_signup_homework_state.setText("已提交");
                        }
					}

				}else if(result == 0){
					tv_addhomework_state.setText("未布置");
                    tv_signup_homework_state.setText("");
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
//				homeworkStates.clear();
//				homeworkStates.addAll(result);
//                Log.d("---homeworkStates_size:",homeworkStates.size()+"");
//				if(homeworkStates.size() == 0){
//					tv_addhomework_state.setText("未布置");
//				}else{
//					tv_addhomework_state.setText("已布置");
//				}


                if (result == null) {
                    tv_addhomework_state.setText("未布置");
                } else {
                    homeworkStates.clear();
                    homeworkStates.addAll(result);
                    Log.d("---homeworkStates_size:",homeworkStates.size()+"");

//                    if(Integer.parseInt(String.valueOf(homeworkStates.get(0).get("homework_flag")).trim()) == 1){
                    if (homeworkStates.size() == 0) {

                        tv_addhomework_state.setText("未布置");
                    }else{
                        tv_addhomework_state.setText("已布置");
                    }
                }

				ListViewUtils.setListViewHeightBasedOnChildren(lvHomework);
			}
			
		});
	}

	private void getMomentId(){//获取momentId
		teacherID = getLessonHomework.teacher_id;
		for(int j = 0;j < getLessonHomework.stuResultList.size();j++){
			if(studentId.equals(getLessonHomework.stuResultList.get(j).student_id)){
				momentId = getLessonHomework.stuResultList.get(j).moment_id;
				homeworkResult_id = getLessonHomework.stuResultList.get(j).id;
			}
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
        case R.id.textView_back:
			onBackPressed();
			break;
			
		case R.id.layout_sign_class:	
			if(DoubleClickedUtils.isFastDoubleClick()){
				break;  
        	}
			if(isTeacher()){ //老师账号
                Log.d("---homeworkStates_size:",homeworkStates.size()+"");
				if(homeworkStates.size() == 0){//没有布置过作业
//                if (homeworkStates.get(0).get("homework_id") == null) {
                    Log.d("---homeworkStates:","老师没有布置作业");
					Intent i = new Intent(HomeworkActivity.this, AddHomeworkActivity.class);
					i.putExtra("lessonId",lessonId);
					startActivityForResult(i, REQ_PARENT_ADDHOMEWORK);
				}else{//布置过作业
                    Log.d("---homeworkStates:","老师布置过作业");
//					Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
//					addHomework = mDb.fetchLessonAddHomework(lessonId);
//					if(addHomework != null){//判断本地是否存储
//						Moment moment = mDb.fetchMoment(addHomework.moment_id + "");
//						i.putExtra("moment", moment);
//				        i.putExtra("lessonId",lessonId);
////				        i.putExtra("studentId", String.valueOf(homeworkStates.get(0).get("stu_uid")));
//				        startActivityForResult(i, REQ_PARENT_DELHOMEWORK);
//					}else{
                        //网络请求获取，实现不同端老师对作业的修改
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								int errno;
								try {
                                    if (TextUtils.isEmpty(studentId)) {
                                        studentId = "";
                                    }
									errno = LessonWebServerIF.getInstance(HomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,1);
									if(errno == ErrorCode.INVALID_ARGUMENT){						
										Moment moment = mDb.fetchMoment(String.valueOf(getLessonHomework.moment_id));
										if(moment != null){
											Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
											i.putExtra("moment", moment);
									        i.putExtra("lessonId",lessonId);


//									        i.putExtra("studentId", studentId);
//                                            i.putExtra("studentId", String.valueOf(homeworkStates.get(0).get("stu_uid")));
//											startActivity(i);
                                            startActivityForResult(i, REQ_PARENT_DELHOMEWORK);
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
					
//				}

			} else {//学生账号 直接网络请求
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						int errno;
						try {
							errno = LessonWebServerIF.getInstance(HomeworkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,studentId,1);
							if (errno == ErrorCode.INVALID_ARGUMENT) {
								Moment moment = mDb.fetchMoment(String.valueOf(getLessonHomework.moment_id));
								if(moment != null){
									Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);
									i.putExtra("moment", moment);
							        i.putExtra("lessonId",lessonId);
							        i.putExtra("studentId", studentId);
							        i.putExtra("schoolId", schoolId);
							        i.putExtra("teacherID", teacherID);
							        i.putExtra("momentId", momentId);
									i.putExtra("lesson_name", tv_lesson_name.getText().toString());
									i.putExtra("class_name", tv_class_name.getText().toString());
							        i.putExtra("flag", 1);
									startActivity(i);
									mHandler.sendEmptyMessage(errno);
								} else {
									mHandler.sendEmptyMessage(errno);
								}
							} else {
								mHandler.sendEmptyMessage(errno);
							}
						} catch (Exception e) {
							mHandler.sendEmptyMessage(ErrorCode.BAD_RESPONSE);
						}
					}
				}).start();
			}
			break;

		case R.id.layout_signup_homework://提交作业
			if(homework_id == 0){
				Toast.makeText(this, "还未布置作业", Toast.LENGTH_SHORT).show();
			}else{
				
				if(momentId == 0){//没有布置过作业
					Intent i = new Intent(HomeworkActivity.this, SignHomeworkResultkActivity.class);
					i.putExtra("homework_id", homework_id);
					i.putExtra("teacherID", teacherID);
					i.putExtra("schoolId", schoolId);
					i.putExtra("lesson_name", tv_lesson_name.getText().toString());
					i.putExtra("class_name", tv_class_name.getText().toString());
					startActivityForResult(i, REQ_STUDENT_SIGNUP);

				}else{//已有布置作业	
//					Intent i = new Intent(HomeworkActivity.this, LessonHomeworkActivity.class);				
//					Moment moment = mDb.fetchMoment(momentId + "");
//					i.putExtra("moment", moment);
//			        i.putExtra("lessonId",lessonId);
//			        i.putExtra("homeworkResult_id",homeworkResult_id);
//			        i.putExtra("flag", 2);
//			        startActivityForResult(i, REQ_PARENT_DELHOMEWORKRESULT);
					Intent i = new Intent(HomeworkActivity.this, SubmitHomeWorkActivity.class);
					i.putExtra("lessonId",lessonId);
					i.putExtra("schoolId", schoolId);
					i.putExtra("student_uid",studentId);	
					i.putExtra("lesson_name", tv_lesson_name.getText().toString());
					i.putExtra("class_name", tv_class_name.getText().toString());
					startActivity(i);
				}
			}
			break;

		case R.id.lay_footer_add:
//			showAddHomeworkDialog();
			break;

		case R.id.title_refresh:
			if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
				getHomeworkState_student(lessonId,studentId); //学生账号 执行
			}else if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
				getHomeworkState(lessonId);//老师账号 执行
				adapter.notifyDataSetChanged();
			}
			break;

		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){
			if(requestCode == REQ_PARENT_ADDHOMEWORK){
				getHomeworkState(lessonId);
                adapter.notifyDataSetChanged();
			}else if(requestCode == REQ_PARENT_DELHOMEWORK){
				getHomeworkState(lessonId);
			}else if(requestCode == REQ_STUDENT_SIGNUP){
				getHomeworkState_student(lessonId, studentId);
				tv_signup_homework_state.setText("已提交");
			}else if(requestCode == REQ_PARENT_DELHOMEWORKRESULT){
				tv_signup_homework_state.setText("未提交");
			}
		}
	}

    /**
     * 重写刷新当前页的接口回调方法，实现在其它页面刷新本页面的功能
     */
    @Override
    public void refresh() {
        if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
            getHomeworkState_student(lessonId,studentId); //学生账号 执行
        }else if(PrefUtil.getInstance(HomeworkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
            getHomeworkState(lessonId);//老师账号 执行
            adapter.notifyDataSetChanged();
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

                if (state == 0) {
                    stateStr = "提醒交作业";
                    holder.tv_homework_state.setBackgroundResource(R.drawable.btn_small_valid);
                    holder.tv_homework_state.setPadding(2, 2, 2, 2);
                    holder.tv_homework_state.setTextColor(0xff8eb4e6);

                } else if (state == 1) {
                    stateStr = "新提交";
                    holder.tv_homework_state.setBackgroundResource(R.color.white);
                    holder.tv_homework_state.setTextColor(0xff8eb4e6);

                } else if (state == 2) {
                    stateStr = "已批改";
                    holder.tv_homework_state.setTextColor(getResources().getColor(R.color.gray));
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
		if (Integer.parseInt(String.valueOf(homeworkStates.get(position).get("stu_state"))) != 0) {
			Intent i = new Intent(HomeworkActivity.this, SubmitHomeWorkActivity.class);
			i.putExtra("lessonId",lessonId);
			i.putExtra("schoolId",schoolId);
			i.putExtra("class_name", tv_class_name.getText().toString());
			i.putExtra("lesson_name", tv_lesson_name.getText().toString());
			i.putExtra("student_uid",String.valueOf(homeworkStates.get(position).get("stu_uid")));		
			i.putExtra("result_id", Integer.parseInt(String.valueOf(homeworkStates.get(position).get("result_id"))));
			i.putExtra("stu_name",String.valueOf(homeworkStates.get(position).get("stu_name")));	
			startActivity(i);
		} else {
			String uid = String.valueOf(homeworkStates.get(position).get("stu_uid"));
//			String name = String.valueOf(homeworkStates.get(position).get("stu_name"));
			String student_alias = mDb.fetchStudentAlias(schoolId, studentId);
			String reason = tv_lesson_name.getText().toString()+"课"+tv_class_name.getText().toString()+"班的"+student_alias+"老师提醒你，该交作业啦！";

			final ChatMessage message = new ChatMessage();
			message.chatUserName = uid;

			message.messageContent = reason;
			message.msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
			message.sentStatus = ChatMessage.SENTSTATUS_SENDING;
			message.sentDate = TimeHelper.getTimeForMessage(HomeworkActivity.this);
			message.uniqueKey = Database.chatMessageSentDateToUniqueKey(message.sentDate);
			message.ioType = ChatMessage.IOTYPE_OUTPUT;
			
			message.primaryKey = new Database(HomeworkActivity.this)
		                            .storeNewChatMessage(message, false);
    		WowTalkVoipIF.getInstance(HomeworkActivity.this).fSendChatMessage(message);
//    		Toast.makeText(this, "提醒提交作业成功", Toast.LENGTH_SHORT).show();
            messageBox.toast("作业提醒已发出");
		}
	}
}