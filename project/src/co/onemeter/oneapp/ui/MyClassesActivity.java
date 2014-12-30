package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;

import com.androidquery.AQuery;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 我的课堂页面。
 * Created by pzy on 11/10/14.
 */
public class MyClassesActivity extends Activity implements View.OnClickListener, OnItemClickListener {
    private static final String TAG = "MyClassesActivity";
    
    private final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
    
    
    private List<GroupChatRoom> classrooms;
    private List<GroupChatRoom> schoolrooms;
    
    private MyClassAdapter adapter;
    private ListView lvMyClass;
    private MessageBox msgBox;
    private WowTalkWebServerIF talkwebserver;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myclasses);

        AQuery q = new AQuery(this);
        msgBox = new MessageBox(this);
        classrooms = new LinkedList<GroupChatRoom>();
        

        q.find(R.id.title_back).clicked(this);
        lvMyClass = (ListView) findViewById(R.id.lv_myClass);
        //q.find(R.id.livingClass).clicked(this);

        //test();
//        test1();
        
        lvMyClass.setOnItemClickListener(this);
        
        talkwebserver =  WowTalkWebServerIF.getInstance(MyClassesActivity.this);
        //这里用两层异步任务，先取到学校的信息，在取班级信息
        new AsyncTask<Void, Void, Void>(){

        	protected void onPreExecute() {
        		msgBox.showWait();
        	};
        	
			@Override
			protected Void doInBackground(Void... params) {
				schoolrooms = talkwebserver.getMySchools(true);
				return null;
			}
        	
			@Override
			protected void onPostExecute(Void result) {
				if(!isEmpty()){
					new AsyncTask<Void, Void, Void>(){
	
						@Override
						protected Void doInBackground(Void... params) {
							for(GroupChatRoom school:schoolrooms){
								List<GroupChatRoom> claz = talkwebserver.getSchoolClassRooms(school.groupID);
								for(GroupChatRoom classroom:claz){
									classrooms.add(classroom);
								}
							}
							return null;
						}
						
						protected void onPostExecute(Void result) {
							msgBox.dismissWait();
							adapter = new MyClassAdapter(classrooms);
							lvMyClass.setAdapter(adapter);
						};
					}.execute((Void)null);
				}else{
					msgBox.dismissWait();
					msgBox.toast(R.string.not_binded);
				}
			}
        }.execute((Void)null);
        
//        Database db = Database.getInstance(this);
//        schoolrooms = db.fetchSchools();
//        for(GroupChatRoom school:schoolrooms){
//			List<GroupChatRoom> claz = talkwebserver.getSchoolClassRooms(school.groupID);
//			for(GroupChatRoom classroom:claz){
//				classrooms.add(classroom);
//			}
//		}
//        adapter = new MyClassAdapter(classrooms);
//		lvMyClass.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
//            case R.id.livingClass:
//            	Intent intent = new Intent(this, VideoPlayingActivity.class);
//            	startActivity(intent);
//            	break;
        }
    }

    // 测试课堂相关的API
    private void test() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
                WowLessonWebServerIF lessonWeb = WowLessonWebServerIF.getInstance(MyClassesActivity.this);
                Database db = new Database(MyClassesActivity.this);
                String myUid = PrefUtil.getInstance(MyClassesActivity.this).getUid();

                // add lesson1
                Lesson lesson1 = new  Lesson();
                lesson1.class_id = classId;
                lesson1.title = "第一课";
                lesson1.start_date = new Date(2014 - 1900, 11, 29, 9, 0).getTime() / 1000;
                lesson1.end_date = lesson1.start_date + 45 * 60;
                lessonWeb.addOrModifyLesson(lesson1);

                // add lesson2
                Lesson lesson2 = new  Lesson();
                lesson2.class_id = classId;
                lesson2.title = "第二课";
                lesson2.start_date = new Date(2015 - 1900, 0, 8, 14, 30).getTime() / 1000;
                lesson2.end_date = lesson2.start_date + 45 * 60;
                lessonWeb.addOrModifyLesson(lesson2);

                // get lessons
                lessonWeb.getLesson(classId);

                // modify lesson1
                lesson1.title = "lesson 1";
                lessonWeb.addOrModifyLesson(lesson1);

                // get lessons
                lessonWeb.getLesson(classId);

                List<Lesson> lessonList = db.fetchLesson(classId);
                for (Lesson l : lessonList) {
                    Log.i(String.format("%s %d %s %d %d", l.class_id, l.lesson_id,
                            l.title, l.start_date, l.end_date));
                }

                // performance

                List<LessonPerformance> performances = new LinkedList<>();
                String[] names = getResources().getStringArray(R.array.lesson_performance_names);
                for (int i = 0; i < names.length; ++i) {
                    LessonPerformance lp = new LessonPerformance();
                    lp.lesson_id = lesson1.lesson_id;
                    lp.student_id = myUid;
                    lp.property_id = i + 1;
                    lp.property_value = 1 + i % 3;
                    performances.add(lp);
                }
                lessonWeb.addOrModifyLessonPerformance(performances);

                lessonWeb.getLessonPerformance(lesson1.lesson_id, myUid);
                List<LessonPerformance> performances2 = db.fetchLessonPerformance(lesson1.lesson_id, myUid);
                for (LessonPerformance p : performances2) {
                    Log.i(String.format("%d %s %d %s %d", p.lesson_id, p.student_id, p.property_id,
                            names[p.property_id - 1], p.property_value));
                }

                // homework
                LessonHomework homework = new LessonHomework();
                homework.lesson_id = lesson1.lesson_id;
                homework.title = "家庭作业，#1";
                lessonWeb.addOrModifyLessonHomework(homework);
                LessonHomework homework2 = new LessonHomework();
                homework2.lesson_id = lesson1.lesson_id;
                homework2.title = "家庭作业，#2";
                lessonWeb.addOrModifyLessonHomework(homework2);

                lessonWeb.getLessonHomework(lesson1.lesson_id);
                List<LessonHomework> homeworks = db.fetchLessonHomework(lesson1.lesson_id);
                for (LessonHomework h : homeworks) {
                    Log.i(String.format("%s %d %s", h.lesson_id, h.homework_id, h.title));
                }

                // feedback

                LessonParentFeedback feedback = new LessonParentFeedback();
                feedback.lesson_id = lesson1.lesson_id;
                feedback.student_id = myUid;
                Moment moment = new Moment(null);
                moment.text = "很好";
                lessonWeb.addOrModifyLessonParentFeedback(feedback, moment);

                lessonWeb.getLessonParentFeedback(lesson1.lesson_id, myUid);
                LessonParentFeedback feedback2 = db.fetchLessonParentFeedback(lesson1.lesson_id, myUid);
                Moment moment2 = db.fetchMoment(Integer.toString(feedback2.moment_id));
                Log.i(String.format("%d %s %d %s", feedback2.lesson_id, feedback2.student_id,
                        feedback2.moment_id, moment2.text));

                return null;
            }
        }.execute((Void)null);
    }
    
 // 测试课堂相关的API
    private void test1() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
                WowLessonWebServerIF lessonWeb = WowLessonWebServerIF.getInstance(MyClassesActivity.this);
                Database db = new Database(MyClassesActivity.this);
                String myUid = PrefUtil.getInstance(MyClassesActivity.this).getUid();

                // add lesson1
                Lesson lesson1 = new  Lesson();
                lesson1.class_id = classId;
                lesson1.title = "第一课";
                lesson1.start_date = new Date(2014 - 1900, 11, 29, 9, 0).getTime() / 1000;
                lesson1.end_date = lesson1.start_date + 45 * 60;
                lessonWeb.addOrModifyLesson(lesson1);


                // get lessons
                lessonWeb.getLesson(classId);


                List<Lesson> lessonList = db.fetchLesson(classId);
                for (Lesson l : lessonList) {
                    Log.i(String.format("%s %d %s %d %d", l.class_id, l.lesson_id,
                            l.title, l.start_date, l.end_date));
                }

                // performance

                List<LessonPerformance> performances = new LinkedList<>();
                String[] names = getResources().getStringArray(R.array.lesson_performance_names);
                for (int i = 0; i < names.length; ++i) {
                    LessonPerformance lp = new LessonPerformance();
                    lp.lesson_id = lesson1.lesson_id;
                    lp.student_id = myUid;
                    lp.property_id = i + 1;
                    lp.property_value = 1 + i % 3;
                    performances.add(lp);
                }
                lessonWeb.addOrModifyLessonPerformance(performances);

                lessonWeb.getLessonPerformance(lesson1.lesson_id, myUid);
                List<LessonPerformance> performances2 = db.fetchLessonPerformance(lesson1.lesson_id, myUid);
                for (LessonPerformance p : performances2) {
                    Log.i(String.format("%d %s %d %s %d", p.lesson_id, p.student_id, p.property_id,
                            names[p.property_id - 1], p.property_value));
                }

                // homework
                LessonHomework homework = new LessonHomework();
                homework.lesson_id = lesson1.lesson_id;
                homework.title = "家庭作业，#1";
                lessonWeb.addOrModifyLessonHomework(homework);
                LessonHomework homework2 = new LessonHomework();
                homework2.lesson_id = lesson1.lesson_id;
                homework2.title = "家庭作业，#2";
                lessonWeb.addOrModifyLessonHomework(homework2);

                lessonWeb.getLessonHomework(lesson1.lesson_id);
                List<LessonHomework> homeworks = db.fetchLessonHomework(lesson1.lesson_id);
                for (LessonHomework h : homeworks) {
                    Log.i(String.format("%s %d %s", h.lesson_id, h.homework_id, h.title));
                }

                // feedback

                LessonParentFeedback feedback = new LessonParentFeedback();
                feedback.lesson_id = lesson1.lesson_id;
                feedback.student_id = myUid;
                Moment moment = new Moment(null);
                moment.text = "很好";
                lessonWeb.addOrModifyLessonParentFeedback(feedback, moment);

                lessonWeb.getLessonParentFeedback(lesson1.lesson_id, myUid);
                LessonParentFeedback feedback2 = db.fetchLessonParentFeedback(lesson1.lesson_id, myUid);
                Moment moment2 = db.fetchMoment(Integer.toString(feedback2.moment_id));
                Log.i(String.format("%d %s %d %s", feedback2.lesson_id, feedback2.student_id,
                        feedback2.moment_id, moment2.text));

                return null;
            }
        }.execute((Void)null);
    }
    
    class MyClassAdapter extends BaseAdapter{
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
			return convertView;
		}
		class ViewHolder{
			ImageView item_myclass_imageview;
			TextView item_myclass_textview;
		}
    	
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(this, ClassDetailActivity.class);
		intent.putExtra("classroomId", classrooms.get(position).groupID);
		intent.putExtra("classroomName", classrooms.get(position).groupNameOriginal);
		startActivity(intent);
	}
	
	private boolean isEmpty(){
		return schoolrooms == null ||schoolrooms.isEmpty();
	}
}