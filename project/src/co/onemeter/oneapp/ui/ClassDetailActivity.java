package co.onemeter.oneapp.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.GroupMember;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.HorizontalListView;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.R.color;
import co.onemeter.oneapp.contacts.model.Person;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;

/**
 * 课堂详情页面。
 * Created by yl on 21/12/2014.
 */
public class ClassDetailActivity extends Activity implements OnClickListener, OnItemClickListener {
	private AQuery query;
	private LessonWebServerIF lesWebSer;
	private WowTalkWebServerIF mWTWebSer;
	private Database mdb;
	private MessageBox msgBox;
	
	private String classId;
	private List<Lesson> lessons;
	private List<GroupMember> members;
	private CourseTableAdapter courseAdapter;
	private TeachersAdapter teaAdapter;

	private ListView lvLessonTable;
	private HorizontalListView lvTeachers;
	private TextView tvTerm;
	private TextView tvGrade;
	private TextView tvSubject;
	private TextView tvDate;
	private TextView tvTime;
	private TextView tvPlace;
	
	private GroupChatRoom class_group = new GroupChatRoom();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_detail);
		
		initView();
		
		getLessonInfo();
	}

	private void initView(){
		
		tvTerm = (TextView) findViewById(R.id.class_term);
		tvGrade = (TextView) findViewById(R.id.class_grade);
		tvSubject = (TextView) findViewById(R.id.class_subject);
		tvDate = (TextView) findViewById(R.id.class_date);
		tvTime = (TextView) findViewById(R.id.class_time);
		tvPlace = (TextView) findViewById(R.id.class_place);
		lvTeachers = (HorizontalListView) findViewById(R.id.hor_lv_teachers);
		
		lesWebSer = LessonWebServerIF.getInstance(this);
		mWTWebSer = WowTalkWebServerIF.getInstance(this);
		mdb = Database.getInstance(this);
		
		msgBox = new MessageBox(this);
		query = new AQuery(this);
		lessons = new LinkedList<Lesson>();
		courseAdapter = new CourseTableAdapter(lessons);
		
		
		
		Intent intent = getIntent();
//		classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
		classId = intent.getStringExtra("classId");
		
//		members = mdb.fetchGroupMembers(classId);
		if(members == null || members.isEmpty()){
			new AsyncTask<Void, Void, Integer>(){

				@Override
				protected Integer doInBackground(Void... params) {
					return (Integer)mWTWebSer.fGroupChat_GetMembers(classId).get("code");
				}
				
				protected void onPostExecute(Integer result) {
					if(ErrorCode.OK == result){
						members = mdb.fetchGroupMembers(classId);
						//android.util.Log.i("-->>", buddies.toString());
						teaAdapter = new TeachersAdapter(members);
						lvTeachers.setAdapter(teaAdapter);
					}
				};
				
			}.execute((Void)null);
		}else{
			teaAdapter = new TeachersAdapter(members);
			lvTeachers.setAdapter(teaAdapter);
		}
		
		query.find(R.id.class_detail_title).text(intent.getStringExtra("classroomName"));
		query.find(R.id.title_back).clicked(this);
		query.find(R.id.class_live_class).clicked(this);
		query.find(R.id.more).clicked(this);
		
		lvLessonTable = (ListView) findViewById(R.id.lvLessonTable);
		lvLessonTable.setAdapter(courseAdapter);
		lvLessonTable.setOnItemClickListener(this);
		
		TextView tv_class_live = (TextView) findViewById(R.id.class_live_class);
		TextView tv_more = (TextView) findViewById(R.id.more);
		if(PrefUtil.getInstance(this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			tv_class_live.setVisibility(View.GONE);
			tv_more.setVisibility(View.VISIBLE);
		}else{
			tv_class_live.setVisibility(View.VISIBLE);
			tv_more.setVisibility(View.GONE);
		}
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshLessonInfo();
		setClassInfo();
	}
	
	private void getLessonInfo(){
			new AsyncTask<Void, Void, Integer>() {
	
				@Override
				protected Integer doInBackground(Void... params) {
					return lesWebSer.getLesson(classId);
				}
				
				protected void onPostExecute(Integer result) {
//					if(ErrorCode.OK == result){
//						lessons.clear();
//						Database db = Database.open(ClassDetailActivity.this);
//						lessons.addAll(db.fetchLesson(classId));
//						Collections.sort(lessons, new LessonInfoEditActivity.LessonComparator());
//						courseAdapter.notifyDataSetChanged();
//					}
				};
				
			}.execute((Void)null);
	}
	
	private void refreshLessonInfo(){
		lessons.clear();
		Database db = Database.open(ClassDetailActivity.this);
		lessons.addAll(db.fetchLesson(classId));
		Collections.sort(lessons, new LessonInfoEditActivity.LessonComparator());
		courseAdapter.notifyDataSetChanged();
	}
	
	private String[] getStrsByComma(String str){
		if(TextUtils.isEmpty(str)&& !str.contains(Constants.COMMA)){
			return null;
		}
		return str.split(Constants.COMMA);
	}
	
	private void setClassInfo(){
		final String term = getString(R.string.class_term);
		final String grade = getString(R.string.class_grade);
		final String subject = getString(R.string.class_subject);
		final String date = getString(R.string.class_date);
		final String time = getString(R.string.class_time);
		final String place = getString(R.string.class_place);
		final Handler hanlder = new Handler();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				int errno = WowTalkWebServerIF.getInstance(ClassDetailActivity.this).fGroupChat_GetByID(classId, class_group);
//				android.util.Log.i("-->>", class_group.description);
				if(errno == ErrorCode.OK){
					hanlder.post(new Runnable() {
						
						@Override
						public void run() {
							if(class_group != null){
								String[] infos = getStrsByComma(class_group.description);
								if(null != infos && infos.length == 6){
									tvTerm.setText(term + infos[0]);
									tvGrade.setText(grade + infos[1]);
									tvSubject.setText(subject + infos[2]);
									tvDate.setText(date + infos[3]);
									tvTime.setText(time + infos[4]);
									tvPlace.setText(place + infos[5]);
								}
							}
						}
					});
				}
			}
		}).start();
		
			
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.class_live_class:
			msgBox.toast("功能正在实现中...");
			break;
		case R.id.more:
			showMore(v);
			break;
		default:
			break;
		}
	}

	private String forthToEndStr(String str){
		return str.substring(3);
	}
	
	private void showMore(View parentView) {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, parentView);
        // class live
        bottomBoard.add(getString(R.string.class_live_class), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	msgBox.toast("功能正在实现中...");
                        bottomBoard.dismiss();
                    }
                });
        // edit class info 
        bottomBoard.add(getString(R.string.class_edit_class_info), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ClassDetailActivity.this, LessonInfoEditActivity.class);
                        intent.putExtra("class", class_group);
                        intent.putExtra("tag", LessonInfoEditActivity.TAG_CLASS_INFO);

                        startActivity(intent);
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
                    	intent.putExtra("tag", LessonInfoEditActivity.TAG_LES_TABLE);
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
		Intent intent = new Intent();
		intent.setClass(this, LessonDetailActivity.class);
		intent.putExtra(Constants.LESSONID, lessons.get(position).lesson_id);
		intent.putExtra("title", lessons.get(position).title);
		intent.putExtra("classId", classId);
		intent.putExtra("lesson", lessons.get(position));
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
				holder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			final GroupMember member = members.get(position);
			holder.txt_name.setText(TextUtils.isEmpty(member.alias) ? member.nickName : member.alias);
			PhotoDisplayHelper.displayPhoto(ClassDetailActivity.this, holder.img_photo, R.drawable.default_avatar_90, member, true);
			
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
			TextView txt_name;
		}
		
	}
	
	class CourseTableAdapter extends BaseAdapter{
		private List<Lesson> alessons;
		private long now = System.currentTimeMillis();
		
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
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Lesson lesson = alessons.get(position);
			holder.item_name.setText(lesson.title);
			holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			if(lesson.end_date * 1000 < now){
				holder.item_name.setTextColor(0xff8eb4e6);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			holder.item_time.setText(sdf.format(new Date(lesson.start_date * 1000)));
			holder.item_msg.setText("");
			return convertView;
		}
		class ViewHodler{
			TextView item_name;
			TextView item_time;
			TextView item_msg;
		}
		
	}

}
