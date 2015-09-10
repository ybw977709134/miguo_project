package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassDetailActivity.MyClassAdapter;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 我的课堂页面。
 * Created by pzy on 11/10/14.
 */
public class MyClassesActivity extends Activity implements View.OnClickListener, OnItemClickListener {
    public static final String TAG = "MyClassesActivity";
    
    private final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";

    private List<GroupChatRoom> classrooms;
    private List<GroupChatRoom> schoolrooms;
    
    private MyClassAdapter adapter;
    private ListView lvMyClass;
    private WowTalkWebServerIF talkwebserver;
    private int errno;
    private LinearLayout layout_add_class;
    private ProgressBar loading;
    private MessageBox msgbox = new MessageBox(this);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myclasses);

        AQuery q = new AQuery(this);
        classrooms = new LinkedList<GroupChatRoom>();

        if(!getIntent().getBooleanExtra(TAG,false)){
            q.find(R.id.title_back).visibility(View.VISIBLE);
        }
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_add).clicked(this);
        loading = (ProgressBar) findViewById(R.id.loading);
        lvMyClass = (ListView) findViewById(R.id.lv_myClass);
        layout_add_class = (LinearLayout) findViewById(R.id.layout_add_class);
        
        lvMyClass.setOnItemClickListener(this);
        lvMyClass.setEmptyView(loading);
        
        talkwebserver =  WowTalkWebServerIF.getInstance(MyClassesActivity.this);
        
        schoolrooms = new ArrayList<GroupChatRoom>();
        
        //这里用两层异步任务，先取到学校的信息，在取班级信息
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//
//            protected void onPreExecute() {
//                //msgBox.showWait();
//            }
//
//            ;
//
//            @Override
//            protected Void doInBackground(Void... params) {
//            	classrooms.clear();
////                schoolrooms = talkwebserver.getMySchools(true);
//                errno = talkwebserver.getMySchoolsErrno(true, schoolrooms);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                if (errno == ErrorCode.OK) {
//                	new Database(MyClassesActivity.this).storeSchools(schoolrooms);
//                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//
//                        @Override
//                        protected Void doInBackground(Void... params) {
//                            for (GroupChatRoom school : schoolrooms) {
//                                List<GroupChatRoom> claz = talkwebserver.getSchoolClassRooms(school.groupID);
//                                for (GroupChatRoom classroom : claz) {
//                                    classrooms.add(classroom);
//                                }
//                            }
//                            return null;
//                        }
//
//                        protected void onPostExecute(Void result) {
//                            //msgBox.dismissWait();
//                            adapter = new MyClassAdapter(classrooms);
//                            lvMyClass.setAdapter(adapter);
//                        }
//
//                        ;
//                    });
//                } else {
//                    //msgBox.dismissWait();
//                    Toast.makeText(MyClassesActivity.this,R.string.conn_time_out,Toast.LENGTH_LONG).show();
//                    finish();
//                }
//            }
//        });

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classrooms.clear();
				classrooms.addAll(talkwebserver.getSchoolClassRooms(null));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
                if(classrooms.isEmpty()){
                    /*
                        处理无班级内容
                     */

//                	if(!getIntent().getBooleanExtra(TAG,false)){
//                		View emptyView = lvMyClass.getEmptyView();
//                        emptyView.setVisibility(View.GONE);
//                        RelativeLayout lay_main = (RelativeLayout) findViewById(R.id.lay_myclass_main);
//                        TextView textView = new TextView(MyClassesActivity.this);
//                        textView.setText(getString(R.string.class_not_bind));
//                        textView.setGravity(Gravity.CENTER);
//                        RelativeLayout.LayoutParams params =
//                              new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
//                        textView.setLayoutParams(params);
//                        lay_main.addView(textView);
//                	}else{
                		lvMyClass.setVisibility(View.GONE);
                    	loading.setVisibility(View.GONE);
                    	layout_add_class.setVisibility(View.VISIBLE);
//                	}
                	
                }else {
                    adapter = new MyClassAdapter(classrooms);
                    lvMyClass.setAdapter(adapter);
                }
			}

		});

    }

    /**
     * 通过邀请码绑定课堂
     */
    private void submit() {
        String invitationCode = new AQuery(this).find(R.id.txt_code).getText().toString();
        if (TextUtils.isEmpty(invitationCode))
            return;

        msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Integer>() {
            @Override
            protected Integer doInBackground(String... strings) {
                int errno = WowTalkWebServerIF.getInstance(MyClassesActivity.this)
                        .fBindInvitationCode(strings[0]);
                return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {

                    msgbox.toast("添加新课堂成功");

                    lvMyClass.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                    layout_add_class.setVisibility(View.GONE);

                    //刷新我的课堂
                    refresh();

                } else {
                    if (errno == ErrorCode.ERR_EXPIRED_INVITATION_CODE) {
                        msgbox.toast(R.string.invite_code_out_time, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_SCHOOL_USER_HAD_BOUND) {
                        msgbox.toast(R.string.invite_code_used, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_BOUND_SAME_SCHOOL_USER) {
                        msgbox.toast(R.string.invite_code_school, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_SCHOOL_USER_TYPE_NOT_MATCH) {
                    	if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(getApplicationContext()).getMyAccountType()){
                    		msgbox.toast(R.string.invite_code_type_not_match_teacher, Toast.LENGTH_SHORT);
                    	}else if(Buddy.ACCOUNT_TYPE_STUDENT == PrefUtil.getInstance(getApplicationContext()).getMyAccountType()){
                    		msgbox.toast(R.string.invite_code_type_not_match_student, Toast.LENGTH_SHORT);
                    	}
                        
                    } else if (errno == ErrorCode.ERR_INVITATION_CODE_NOT_EXIST) {
                        msgbox.toast(R.string.invite_code_type_not_exist, Toast.LENGTH_SHORT);
                    } else{
                    	msgbox.toast(R.string.invite_code_type_time_out, Toast.LENGTH_SHORT);
                    }
                }
            }
        }, invitationCode);

    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
                
            case R.id.btn_add:
            	submit();
            	break;

        }
    }


    //刷新组织架构
    private void refresh() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {

                List<GroupChatRoom> result = new LinkedList<>();
                int errno = WowTalkWebServerIF.getInstance(MyClassesActivity.this).getMySchoolsErrno(true, result);
                if (errno == ErrorCode.OK) {
                    classrooms.clear();
                    classrooms.addAll(result);
                }

                return errno;
            }
            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {

                    adapter = new MyClassAdapter(classrooms);
                    lvMyClass.setAdapter(adapter);

                    new Database(MyClassesActivity.this).storeSchools(classrooms);

                } else {

                   msgbox.toast(R.string.operation_failed);

                }

            }
        });
    }




    // 测试课堂相关的API
//    private void test() {
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
//                LessonWebServerIF lessonWeb = LessonWebServerIF.getInstance(MyClassesActivity.this);
//                Database db = new Database(MyClassesActivity.this);
//                String myUid = PrefUtil.getInstance(MyClassesActivity.this).getUid();
//
//                // add lesson1
//                Lesson lesson1 = new Lesson();
//                lesson1.class_id = classId;
//                lesson1.title = "第一课";
//                lesson1.start_date = new Date(2014 - 1900, 11, 29, 9, 0).getTime() / 1000;
//                lesson1.end_date = lesson1.start_date + 45 * 60;
//                lessonWeb.addOrModifyLesson(lesson1,1);
//
//                // add lesson2
//                Lesson lesson2 = new Lesson();
//                lesson2.class_id = classId;
//                lesson2.title = "第二课";
//                lesson2.start_date = new Date(2015 - 1900, 0, 8, 14, 30).getTime() / 1000;
//                lesson2.end_date = lesson2.start_date + 45 * 60;
//                lessonWeb.addOrModifyLesson(lesson2,1);
//
//                // get lessons
//                lessonWeb.getLesson(classId);
//
//                // modify lesson1
//                lesson1.title = "lesson 1";
//                lessonWeb.addOrModifyLesson(lesson1,1);
//
//                // get lessons
//                lessonWeb.getLesson(classId);
//
//                List<Lesson> lessonList = db.fetchLesson(classId);
//                for (Lesson l : lessonList) {
//                    Log.i(String.format("%s %d %s %d %d", l.class_id, l.lesson_id,
//                            l.title, l.start_date, l.end_date));
//                }
//
//                // performance
//
//                List<LessonPerformance> performances = new LinkedList<>();
//                String[] names = getResources().getStringArray(R.array.lesson_performance_names);
//                for (int i = 0; i < names.length; ++i) {
//                    LessonPerformance lp = new LessonPerformance();
//                    lp.lesson_id = lesson1.lesson_id;
//                    lp.student_id = myUid;
//                    lp.property_id = i + 1;
//                    lp.property_value = 1 + i % 3;
//                    performances.add(lp);
//                }
//                lessonWeb.addOrModifyLessonPerformance(performances);
//
//                lessonWeb.getLessonPerformance(lesson1.lesson_id, myUid);
//                List<LessonPerformance> performances2 = db.fetchLessonPerformance(lesson1.lesson_id, myUid);
//                for (LessonPerformance p : performances2) {
//                    Log.i(String.format("%d %s %d %s %d", p.lesson_id, p.student_id, p.property_id,
//                            names[p.property_id - 1], p.property_value));
//                }
//
//                // homework
//                LessonHomework homework = new LessonHomework();
//                homework.lesson_id = lesson1.lesson_id;
//                homework.title = "家庭作业，#1";
//                lessonWeb.addOrModifyLessonHomework(homework);
//                LessonHomework homework2 = new LessonHomework();
//                homework2.lesson_id = lesson1.lesson_id;
//                homework2.title = "家庭作业，#2";
//                lessonWeb.addOrModifyLessonHomework(homework2);
//
//                lessonWeb.getLessonHomework(lesson1.lesson_id);
//                List<LessonHomework> homeworks = db.fetchLessonHomework(lesson1.lesson_id);
//                for (LessonHomework h : homeworks) {
//                    Log.i(String.format("%s %d %s", h.lesson_id, h.homework_id, h.title));
//                }
//
//                // feedback
//
//                LessonParentFeedback feedback = new LessonParentFeedback();
//                feedback.lesson_id = lesson1.lesson_id;
//                feedback.student_id = myUid;
//                Moment moment = new Moment(null);
//                moment.text = "很好";
//                lessonWeb.addOrModifyLessonParentFeedback(feedback, moment);
//
//                lessonWeb.getLessonParentFeedback(lesson1.lesson_id, myUid);
//                LessonParentFeedback feedback2 = db.fetchLessonParentFeedback(lesson1.lesson_id, myUid);
//                Moment moment2 = db.fetchMoment(Integer.toString(feedback2.moment_id));
//                Log.i(String.format("%d %s %d %s", feedback2.lesson_id, feedback2.student_id,
//                        feedback2.moment_id, moment2.text));
//
//                return null;
//            }
//        });
//    }
//    
// // 测试课堂相关的API
//    private void test1() {
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
//                LessonWebServerIF lessonWeb = LessonWebServerIF.getInstance(MyClassesActivity.this);
//                Database db = new Database(MyClassesActivity.this);
//                String myUid = PrefUtil.getInstance(MyClassesActivity.this).getUid();
//
//                // add lesson1
//                Lesson lesson1 = new Lesson();
//                lesson1.class_id = classId;
//                lesson1.title = "第一课";
//                lesson1.start_date = new Date(2014 - 1900, 11, 29, 9, 0).getTime() / 1000;
//                lesson1.end_date = lesson1.start_date + 45 * 60;
//                lessonWeb.addOrModifyLesson(lesson1,1);
//
//
//                // get lessons
//                lessonWeb.getLesson(classId);
//
//
//                List<Lesson> lessonList = db.fetchLesson(classId);
//                for (Lesson l : lessonList) {
//                    Log.i(String.format("%s %d %s %d %d", l.class_id, l.lesson_id,
//                            l.title, l.start_date, l.end_date));
//                }
//
//                // performance
//
//                List<LessonPerformance> performances = new LinkedList<>();
//                String[] names = getResources().getStringArray(R.array.lesson_performance_names);
//                for (int i = 0; i < names.length; ++i) {
//                    LessonPerformance lp = new LessonPerformance();
//                    lp.lesson_id = lesson1.lesson_id;
//                    lp.student_id = myUid;
//                    lp.property_id = i + 1;
//                    lp.property_value = 1 + i % 3;
//                    performances.add(lp);
//                }
//                lessonWeb.addOrModifyLessonPerformance(performances);
//
//                lessonWeb.getLessonPerformance(lesson1.lesson_id, myUid);
//                List<LessonPerformance> performances2 = db.fetchLessonPerformance(lesson1.lesson_id, myUid);
//                for (LessonPerformance p : performances2) {
//                    Log.i(String.format("%d %s %d %s %d", p.lesson_id, p.student_id, p.property_id,
//                            names[p.property_id - 1], p.property_value));
//                }
//
//                // homework
//                LessonHomework homework = new LessonHomework();
//                homework.lesson_id = lesson1.lesson_id;
//                homework.title = "家庭作业，#1";
//                lessonWeb.addOrModifyLessonHomework(homework);
//                LessonHomework homework2 = new LessonHomework();
//                homework2.lesson_id = lesson1.lesson_id;
//                homework2.title = "家庭作业，#2";
//                lessonWeb.addOrModifyLessonHomework(homework2);
//
//                lessonWeb.getLessonHomework(lesson1.lesson_id);
//                List<LessonHomework> homeworks = db.fetchLessonHomework(lesson1.lesson_id);
//                for (LessonHomework h : homeworks) {
//                    Log.i(String.format("%s %d %s", h.lesson_id, h.homework_id, h.title));
//                }
//
//                // feedback
//
//                LessonParentFeedback feedback = new LessonParentFeedback();
//                feedback.lesson_id = lesson1.lesson_id;
//                feedback.student_id = myUid;
//                Moment moment = new Moment(null);
//                moment.text = "很好";
//                lessonWeb.addOrModifyLessonParentFeedback(feedback, moment);
//
//                lessonWeb.getLessonParentFeedback(lesson1.lesson_id, myUid);
//                LessonParentFeedback feedback2 = db.fetchLessonParentFeedback(lesson1.lesson_id, myUid);
//                Moment moment2 = db.fetchMoment(Integer.toString(feedback2.moment_id));
//                Log.i(String.format("%d %s %d %s", feedback2.lesson_id, feedback2.student_id,
//                        feedback2.moment_id, moment2.text));
//
//                return null;
//            }
//        });
//    }
    
    class MyClassAdapter extends BaseAdapter{
    	private int[] imgbackgroud = {R.drawable.class_bg_1,R.drawable.class_bg_2,R.drawable.class_bg_3};
    	private List<GroupChatRoom> classrooms;
    	
    	public MyClassAdapter(List<GroupChatRoom> classrooms){
    		this.classrooms = classrooms;
    	}

		@Override
		public int getCount() {
			return classrooms.size();
		}

		@Override
		public Object getItem(int position) {
			return classrooms.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(null == convertView){
				convertView = getLayoutInflater().inflate(R.layout.listitem_myclass, parent, false);
				holder = new ViewHolder();
				holder.item_myclass_textview = (TextView) convertView.findViewById(R.id.item_myclass_textview);
				holder.item_myclass_imageview = (ImageView) convertView.findViewById(R.id.item_myclass_imageview);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.item_myclass_textview.setText((position + 1) + "." + classrooms.get(position).groupNameOriginal);
			//int groupId_2 = classrooms.get(position).groupID.charAt(2) - '0';
			holder.item_myclass_imageview.setImageResource(imgbackgroud[position % 3]);
			return convertView;
		}
		class ViewHolder{
			ImageView item_myclass_imageview;
			TextView item_myclass_textview;
		}
    	
    }

    /**
     * 点击某学校的班级进入相应班级的详情页
     * @param parent
     * @param view
     * @param position
     * @param id
     */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(this, ClassDetailActivity.class);
		intent.putExtra("classId", classrooms.get(position).groupID);//班级的ID
		intent.putExtra("schoolId", classrooms.get(position).schoolID);//班级所在学校的id，一个学生在某段时间内只能在一个学校
		intent.putExtra("classroomName", classrooms.get(position).groupNameOriginal);//班级名
		startActivity(intent);
	}
	
}