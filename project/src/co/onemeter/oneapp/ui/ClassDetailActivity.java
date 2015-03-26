package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.w3c.dom.Text;
import org.wowtalk.api.*;
import org.wowtalk.ui.HorizontalListView;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.DoubleClickedUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 课堂详情页面。
 * Created by yl on 21/12/2014.
 */
public class ClassDetailActivity extends Activity implements OnClickListener, OnItemClickListener {
	private AQuery query;
	private WowTalkWebServerIF mWTWebSer;
	private MessageBox msgBox;
	
	private String classId;
	private String parent_group_id;
	private List<Lesson> lessons;
	private List<GroupMember> members;
	private CourseTableAdapter courseAdapter;
	private TeachersAdapter teaAdapter;

	private ListView lvLessonTable;
//	private HorizontalListView lvTeachers;
	private TextView tvTerm;
	private TextView tvGrade;
	private TextView tvSubject;
	private TextView tvDate;
	private TextView tvTime;
	private TextView tvPlace;
//	private TextView tvLength;
	private long currentTime;
	private int lessonId;
	private String lessonName;
	private int isOnResume = 1;
	
	private long startDateStamps;
	private long endDateStamps;
	
	private GroupChatRoom class_group = new GroupChatRoom();
	private DrawerLayout layout_main_drawer;
	private RelativeLayout layout_main_leftdrawer;
//	private LinearLayout layout_main_show;
		
    private List<GroupChatRoom> classrooms;
//    private List<GroupChatRoom> schoolrooms;   
    private MyClassAdapter adapter;
    private ListView lvMyClass;
    private TextView myclasses_title;
    private List<ClassInfo> classInfos;
    
    private String end_time;
    private String start_time;
    private String schoolId;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_detail);
		
		initView();
		if(classId != null){
			getLessonInfo();
//			setClassInfo();
			getClassInfo(classId,class_group);
		}
		
	}

	private void initView(){
		
		tvTerm = (TextView) findViewById(R.id.class_term);
		tvGrade = (TextView) findViewById(R.id.class_grade);
		tvSubject = (TextView) findViewById(R.id.class_subject);
		tvDate = (TextView) findViewById(R.id.class_date);
		tvTime = (TextView) findViewById(R.id.class_time);
		tvPlace = (TextView) findViewById(R.id.class_place);
//		tvLength = (TextView) findViewById(R.id.class_length);
		myclasses_title = (TextView) findViewById(R.id.myclasses_title);
//		lvTeachers = (HorizontalListView) findViewById(R.id.hor_lv_teachers);
		layout_main_drawer = (DrawerLayout) findViewById(R.id.layout_main_drawer);
		layout_main_leftdrawer = (RelativeLayout) findViewById(R.id.layout_main_leftdrawer);
//		layout_main_show = (LinearLayout) findViewById(R.id.layout_main_show);
		
		lvMyClass = (ListView) findViewById(R.id.listview_lessons);
		
		mWTWebSer = WowTalkWebServerIF.getInstance(this);
		
		msgBox = new MessageBox(this);
		query = new AQuery(this);
		lessons = new LinkedList<Lesson>();
		classInfos = new LinkedList<ClassInfo>();
		courseAdapter = new CourseTableAdapter(lessons);
		
		
		
		Intent intent = getIntent();
//		classId = "1678ff8f-2a41-438a-bb22-4f55530857f1";
//		classId = intent.getStringExtra("classId");
//		schoolId = intent.getStringExtra("schoolId");
//		schoolId = "60f05397-dfa8-4ddc-9e0d-b4fad549f184";
		Database db = Database.getInstance(this);
		if(classId != null){
			parent_group_id = db.getParentGroupId(classId);
		}
		
//		members = mdb.fetchGroupMembers(classId);
//		if(members == null || members.isEmpty()){
//			AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
//
//				@Override
//				protected Integer doInBackground(Void... params) {
//					return (Integer) mWTWebSer.fGroupChat_GetMembers(classId).get("code");
//				}
//
//				protected void onPostExecute(Integer result) {
//					if (ErrorCode.OK == result) {
//						Database mdb = new Database(ClassDetailActivity.this);
//						members = mdb.fetchGroupMembers(classId);
////						mdb.close();
//						//android.util.Log.i("-->>", buddies.toString());
//						teaAdapter = new TeachersAdapter(members);
//						lvTeachers.setAdapter(teaAdapter);
//					}
//				}
//
//				;
//
//			});
//		}else{
//			teaAdapter = new TeachersAdapter(members);
//			lvTeachers.setAdapter(teaAdapter);
//		}
		
//		query.find(R.id.class_detail_title).text(intent.getStringExtra("classroomName"));
//		query.find(R.id.myclasses_title).text("a");
//		query.find(R.id.title_back).clicked(this);
		query.find(R.id.btn_myclass_addclass).clicked(this);
		query.find(R.id.class_live_class).clicked(this);
		query.find(R.id.more).clicked(this);
		
		query.find(R.id.btn_gotoclass).clicked(this);
		query.find(R.id.btn_tea_stu_list).clicked(this);
		query.find(R.id.btn_class_notice).clicked(this);
		query.find(R.id.btn_holiday_apply).clicked(this);
		
		query.find(R.id.btn_apply_lesson).clicked(this);
		query.find(R.id.btn_photo_answering).clicked(this);
		query.find(R.id.btn_homework_online).clicked(this);
		
		lvLessonTable = (ListView) findViewById(R.id.lvLessonTable);
		lvLessonTable.setAdapter(courseAdapter);
		lvLessonTable.setOnItemClickListener(this);
		
		classrooms = new LinkedList<GroupChatRoom>();
		lvMyClass.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(classrooms != null ){
					schoolId = classrooms.get(position).schoolID;
				    classId = classrooms.get(position).groupID;
				    myclasses_title.setText(classrooms.get(position).groupNameOriginal);
				    layout_main_drawer.closeDrawer(layout_main_leftdrawer);
				    getLessonInfo();
//				    setClassInfo();
				    getClassInfo(classId,class_group);
				}
				
				
			}
		});
		lvMyClass.setEmptyView(this.findViewById(R.id.loading));
		lvMyClass.addFooterView(footerView());
//		schoolrooms = new ArrayList<GroupChatRoom>();
		
		TextView tv_class_live = (TextView) findViewById(R.id.class_live_class);
		TextView tv_more = (TextView) findViewById(R.id.more);
		if(PrefUtil.getInstance(this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			tv_class_live.setVisibility(View.GONE);
			tv_more.setVisibility(View.VISIBLE);
		}else{
			tv_class_live.setVisibility(View.VISIBLE);
			tv_more.setVisibility(View.GONE);
		}
		
		layout_main_drawer.setDrawerListener(new DrawerListener() {
			
			@Override
			public void onDrawerStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onDrawerSlide(View arg0, float arg1) {
//				getSchoolClassInfo();
				
			}
			
			@Override
			public void onDrawerOpened(View arg0) {
//				getSchoolClassInfo();
				
			}
			
			@Override
			public void onDrawerClosed(View arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(classId != null){
			refreshLessonInfo();
		}
		
	}
	private void getSchoolClassInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classrooms.clear();
				classrooms.addAll(mWTWebSer.getSchoolClassRooms(null));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				adapter = new MyClassAdapter(classrooms);
                lvMyClass.setAdapter(adapter);
			}

		});
	}
//	private void getSchoolClassInfo(){
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
//            	schoolrooms.clear();
////                schoolrooms = talkwebserver.getMySchools(true);
//                errno = mWTWebSer.getMySchoolsErrno(true, schoolrooms);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                if (errno == ErrorCode.OK) {
//                	new Database(ClassDetailActivity.this).storeSchools(schoolrooms);
//                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
//
//                        @Override
//                        protected Void doInBackground(Void... params) {
//                        	classrooms.clear();
//                            for (GroupChatRoom school : schoolrooms) {
//                                List<GroupChatRoom> claz = mWTWebSer.getSchoolClassRooms(school.groupID);
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
//                    Toast.makeText(ClassDetailActivity.this,R.string.conn_time_out,Toast.LENGTH_LONG).show();
//                    finish();
//                }
//            }
//        });
//	}
	private void getLessonInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

				@Override
				protected Integer doInBackground(Void... params) {
					return LessonWebServerIF.getInstance(ClassDetailActivity.this).getLesson(classId);
				}

				protected void onPostExecute(Integer result) {
					if (ErrorCode.OK == result) {
						refreshLessonInfo();
					}
				};

			});
	}
	private void getClassInfo(final String classId,final GroupChatRoom g){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classInfos.clear();
				return LessonWebServerIF.getInstance(ClassDetailActivity.this).getClassInfo(classId,classInfos,g);
			}

			protected void onPostExecute(Integer result) {
				if (ErrorCode.OK == result) {
					String start_date = Utils.stampsToDate(classInfos.get(0).start_day);
					String end_date = Utils.stampsToDate(classInfos.get(0).end_day);
					start_time = Utils.stampsToTime(classInfos.get(0).start_time);
					end_time = Utils.stampsToTime(classInfos.get(0).end_time);
					final String date = getString(R.string.class_date);
					final String time = getString(R.string.class_time);
					if(classInfos.get(0).start_day != 0){
						tvDate.setText(date + start_date + " - " + end_date);
					    tvTime.setText(time + start_time + " - " + end_time);
					}
					refreshClassInfo();
					
				}
			};

		});
	}
	private void refreshLessonInfo(){
		lessons.clear();
		Database db = Database.open(ClassDetailActivity.this);
		lessons.addAll(db.fetchLesson(classId));
//		db.close();
		Collections.sort(lessons, new LessonInfoEditActivity.LessonComparator());
		courseAdapter.notifyDataSetChanged();
	}
	
	private String[] getStrsByComma(String str){
		if(TextUtils.isEmpty(str)&& !str.contains(Constants.COMMA)){
			return null;
		}
		return str.split(Constants.COMMA);
	}
	
	private View footerView() {
		View view = getLayoutInflater().inflate(R.layout.drawerlay_lv_footer, null);
		TextView txt_footer = (TextView) view.findViewById(R.id.txt_footer_add);
		txt_footer.setText(getString(R.string.class_add_classes));
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.lay_footer_add);
		layout.setOnClickListener(this);
		
		return view;
	}
//	private void setClassInfo(){
//		final Handler hanlder = new Handler();
//		
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				int errno = WowTalkWebServerIF.getInstance(ClassDetailActivity.this).fGroupChat_GetByID(classId, class_group);
////				android.util.Log.i("-->>", class_group.description);
//				if(errno == ErrorCode.OK){
//					hanlder.post(new Runnable() {
//						
//						@Override
//						public void run() {
//							if(class_group != null){
//								refreshClassInfo();
//							}
//						}
//					});
//				}
//			}
//		}).start();
//		
//			
//	}
	
	private void refreshClassInfo(){
		final String term = getString(R.string.class_term);
		final String grade = getString(R.string.class_grade);
		final String subject = getString(R.string.class_subject);
//		final String date = getString(R.string.class_date);
//		final String time = getString(R.string.class_time);
		final String place = getString(R.string.class_place);
//		final String length = getString(R.string.class_length);
		String[] infos = getStrsByComma(class_group.description);
		if(null != infos && infos.length == 4){
			tvTerm.setText(term + infos[0]);
			tvGrade.setText(grade + infos[1]);
			tvSubject.setText(subject + infos[2]);
//			tvDate.setText(date + infos[3]);
//			tvTime.setText(time + infos[4]);
			tvPlace.setText(place + infos[3]);
//			tvLength.setText(length + infos[6]);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.btn_myclass_addclass:
			layout_main_drawer.openDrawer(layout_main_leftdrawer);
			if(!DoubleClickedUtils.isFastDoubleClick()){
				getSchoolClassInfo();
			}
			
			break;
		case R.id.class_live_class:
			currentTime = System.currentTimeMillis()/1000;
//			String time = tvTime.getText().toString().substring(5);
//			String length = tvLength.getText().toString().substring(5);	
			if(currentTime < lessons.get(0).start_date){
				Builder alertDialog = new AlertDialog.Builder(ClassDetailActivity.this);
    			alertDialog.setTitle("提示");
    			alertDialog.setMessage("现在没有课程正在直播");
    			alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface arg0, int arg1) {

    				}
    			});        			
    			alertDialog.create().show();
			}else{
//				String[] times = time.split(":");
//				String[] lengths = length.split(":");
				String[] startTimes = start_time.split(":");
				String[] endTimes = end_time.split(":");
				startDateStamps =Integer.parseInt(startTimes[0])*3600 + Integer.parseInt(startTimes[1])*60;
				endDateStamps =Integer.parseInt(endTimes[0])*3600 + Integer.parseInt(endTimes[1])*60;
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				for(Lesson lesson:lessons){
					String date = sdf.format(new Date(lesson.start_date * 1000));
					String[] dates = date.split("-");
					long timeStamps = Utils.getTimeStamp(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]), Integer.parseInt(dates[2]))/1000;
					if(currentTime > (timeStamps + startDateStamps) && currentTime < (timeStamps + endDateStamps)){
						lessonId = lesson.lesson_id;
						lessonName = lesson.title;
						}
					}
				Intent intent = new Intent();
				intent.putExtra("student_live", 1);
				intent.putExtra("lessonId", lessonId);
				intent.putExtra("lessonName", lessonName);
				intent.putExtra("schoolId", schoolId);
				intent.setClass(this, CameraActivity.class);
				startActivity(intent);
			}		

			break;
		case R.id.more:
			showMore(v);
			break;
		case R.id.btn_gotoclass:
			Intent intent = new Intent(this,TimelineActivity.class);
			startActivity(intent);
			break;
		case R.id.lay_footer_add:
			Intent i = new Intent(this,AddClassActivity.class);
			startActivityForResult(i, 1001);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 0 && resultCode == RESULT_OK){
			final String term = getString(R.string.class_term);
			final String grade = getString(R.string.class_grade);
			final String subject = getString(R.string.class_subject);
//			final String date = getString(R.string.class_date);
//			final String time = getString(R.string.class_time);
			final String place = getString(R.string.class_place);
//			final String length = getString(R.string.class_length);
			String reTerm = data.getStringExtra(ClassInfoEditActivity.TERM);
			String reGrade = data.getStringExtra(ClassInfoEditActivity.GRADE);
			String reSubject = data.getStringExtra(ClassInfoEditActivity.SUBJECT);
//			String reDate = data.getStringExtra(ClassInfoEditActivity.DATE);
//			String reTime = data.getStringExtra(ClassInfoEditActivity.TIME);
			String rePlace = data.getStringExtra(ClassInfoEditActivity.PLACE);
//			String reLength = data.getStringExtra(ClassInfoEditActivity.LENGTH);
			tvTerm.setText(term + reTerm);
			tvGrade.setText(grade + reGrade);
			tvSubject.setText(subject + reSubject);
//			tvDate.setText(date + reDate);
//			tvTime.setText(time + reTime);
			tvPlace.setText(place + rePlace);
//			tvLength.setText(length + reLength);
			class_group.description = reTerm + Constants.COMMA + 
					reGrade + Constants.COMMA + 
					reSubject + Constants.COMMA +
					rePlace;
			
			getClassInfo(classId,class_group);
		
		}else if(requestCode == 1001 && resultCode == RESULT_OK){
			getSchoolClassInfo();
		}
	}
	
	private void showMore(View parentView) {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, parentView);
        // class live
//        bottomBoard.add(getString(R.string.class_live_class), BottomButtonBoard.BUTTON_BLUE,
//                new OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                    	msgBox.toast("功能正在实现中...");
//                        bottomBoard.dismiss();
//                    }
//                });
        // edit class info 
        bottomBoard.add(getString(R.string.class_edit_class_info), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	String reDate = tvDate.getText().toString().substring(3);
                    	String reTime = tvTime.getText().toString().substring(3);
                        Intent intent = new Intent(ClassDetailActivity.this, ClassInfoEditActivity.class);
                        intent.putExtra("class", class_group);
                        if(!TextUtils.isEmpty(reDate) && !TextUtils.isEmpty(reTime)){
                        	intent.putExtra("tvDate", reDate);
                            intent.putExtra("tvTime", reTime);
                        }
                        
                        if(lessons!=null && !lessons.isEmpty()){
                            intent.putExtra("firstlesdate", lessons.get(0).start_date);
                        }
                        startActivityForResult(intent, 0);
                        bottomBoard.dismiss();
                    }
                });
        //edit coursetable info
        bottomBoard.add(getString(R.string.class_edit_cursetable_info), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	if(TextUtils.isEmpty(class_group.description)){
                    		msgBox.toast(R.string.class_editinfo_first);
                    		return;
                    	}
                    	Intent intent = new Intent(ClassDetailActivity.this, LessonInfoEditActivity.class);
                    	intent.putExtra("class", class_group);
                    	Bundle b = new Bundle();
                    	b.putString("retime", tvTime.getText().toString().substring(3));
                    	intent.putExtras(b);
                        startActivity(intent);
                        bottomBoard.dismiss();
                    }
                });
        //Cancel
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Lesson lesson = lessons.get(position);
		long startdate = lesson.start_date;
		long enddate = lesson.end_date;
		Intent intent = new Intent();
		intent.setClass(this, LessonDetailActivity.class);
		intent.putExtra(Constants.LESSONID, lessons.get(position).lesson_id);
		intent.putExtra("title", lessons.get(position).title);
		intent.putExtra("classId", classId);
		intent.putExtra("schoolId", schoolId);
		intent.putExtra("lesson", lessons.get(position));
		intent.putExtra("startdate", startdate);
		intent.putExtra("enddate", enddate);
		intent.putExtra("onResume", isOnResume);
		startActivity(intent);

		
	}
	
	class TeachersAdapter extends BaseAdapter{
		private List<GroupMember> members;
		
		public TeachersAdapter(List<GroupMember> lists){
			members = new ArrayList<GroupMember>();
			for(GroupMember buddy:lists){
				//android.util.Log.i("-->>", buddy.getAccountType() + "");
				if(Buddy.ACCOUNT_TYPE_TEACHER == buddy.getAccountType()){
					members.add(buddy);
					android.util.Log.d("----:", buddy.userID);
					
				}
			}
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_class_detail_teacher, parent, false);
				holder.img_photo = (ImageView) convertView.findViewById(R.id.img_photo);
				holder.img_tag_tea = (ImageView) convertView.findViewById(R.id.imageView_tag_tea);
				holder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			final GroupMember member = members.get(position);
			holder.txt_name.setText(TextUtils.isEmpty(member.alias) ? member.nickName : member.alias);
			PhotoDisplayHelper.displayPhoto(ClassDetailActivity.this, holder.img_photo, R.drawable.default_avatar_90, member, true);
			//对头像的身份进行标记
			if (member.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
				holder.img_tag_tea.setVisibility(View.VISIBLE);
			} else {
				holder.img_tag_tea.setVisibility(View.GONE);
			}
			
			View.OnClickListener listener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
				String userID = PrefUtil.getInstance(ClassDetailActivity.this).getUid();
	             if (null != member && !TextUtils.isEmpty(member.userID)) {
	                 int friendType = ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
	                 if (member.userID.equals(userID)) {
	                     friendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
	                 } else if (0 != (member.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
	                     friendType = ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
	                 }
	                 ContactInfoActivity.launch(ClassDetailActivity.this,
	                         Person.fromBuddy(member),
	                         friendType);
	             }
				}
			};
			
			holder.img_photo.setOnClickListener(listener);
			return convertView;
		}
		
		class ViewHolder{
			ImageView img_photo;
			ImageView img_tag_tea;
			TextView txt_name;
		}
		
	}
	
	class CourseTableAdapter extends BaseAdapter{
		private List<Lesson> alessons;
		
		public CourseTableAdapter(List<Lesson> lessons){
			this.alessons = lessons;
		}
		
		@Override
		public int getCount() {
			return alessons.size();
		}

		@Override
		public Object getItem(int position) {
			return alessons.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_coursetable, parent, false);
				holder.item_name = (TextView) convertView.findViewById(R.id.coursetable_item_name);
				holder.item_time = (TextView) convertView.findViewById(R.id.coursetable_item_time);
				holder.item_msg = (TextView) convertView.findViewById(R.id.coursetable_item_msg);
				holder.coursetable_item_islive = (TextView) convertView.findViewById(R.id.coursetable_item_islive);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Lesson lesson = alessons.get(position);
			holder.item_name.setText(lesson.title);
			holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			holder.coursetable_item_islive.setVisibility(View.INVISIBLE);
			long startdata = lesson.start_date;
			long enddata = lesson.end_date;
			long curTime = System.currentTimeMillis()/1000;
			long now = Utils.getDayStampMoveMillis();
//			Log.i("---startdata---" +startdata);
//			Log.i("---now---" +now);
//			if(startdata < now){
//				holder.item_name.setTextColor(0xff8eb4e6);
//			}
//			if(startdata == now){
//				holder.item_name.setTextColor(Color.RED);				
//			}
			if(curTime > enddata){
				holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			}else if(curTime > now && curTime <startdata){
				holder.item_name.setTextColor(0xff8eb4e6);
			}else if(curTime > startdata && curTime < enddata){
				holder.item_name.setTextColor(Color.RED);
			}
			if((startdata < curTime) && (curTime < enddata) && (lesson.live == 1)){
				holder.coursetable_item_islive.setVisibility(View.VISIBLE);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			holder.item_time.setText(sdf.format(new Date(startdata * 1000)));
			holder.item_msg.setText("");
			return convertView;
		}
		class ViewHodler{
			TextView item_name;
			TextView item_time;
			TextView item_msg;
			TextView coursetable_item_islive;
		}
		
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_myclass_drawerlayout, parent, false);
				holder = new ViewHolder();
				holder.item_myclass_textview = (TextView) convertView.findViewById(R.id.item_myclass_tv);
				holder.item_myclass_imageview = (ImageView) convertView.findViewById(R.id.item_myclass_iv);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.item_myclass_textview.setText(classrooms.get(position).groupNameOriginal);
			if(classId != null && classId.equals(classrooms.get(position).groupID)){
				holder.item_myclass_imageview.setImageResource(R.drawable.icon_myclass_leftpage_u);
				holder.item_myclass_textview.setTextColor(0xFF00A2E8);
			}
			return convertView;
		}
		class ViewHolder{
			ImageView item_myclass_imageview;
			TextView item_myclass_textview;
		}
    	
    }

}
