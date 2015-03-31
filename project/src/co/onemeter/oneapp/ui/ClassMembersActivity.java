package co.onemeter.oneapp.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Classroom;
import org.wowtalk.api.Database;
import org.wowtalk.api.IHasPhoto;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;


/**
 * 师生名单页面。
 * Created by zz on 03/30/2015.
 */
public class ClassMembersActivity extends Activity implements OnClickListener{
	private ListView listView_student;
	private ListView listView_teacher;
	private ImageButton btn_classmember_back;
	private List<Buddy> classmembers;
	private List<Buddy> result_teachers;
	private List<Buddy> result_students;
	private Map<String, List<Buddy>> memberResult;
	private ClassTeacherMemberAdapter teacherAdapter;
	private ClassStudentMemberAdapter studentAdapter;
	private String schoolId;
	private String classId;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_members);
		
		initView();
		getClassMember();

	}
	
	private void initView(){
		schoolId = getIntent().getStringExtra("schoolId");
		classId = getIntent().getStringExtra("classId");
		btn_classmember_back = (ImageButton) findViewById(R.id.btn_classmember_back);
		listView_student = (ListView) findViewById(R.id.listView_student);
		listView_teacher = (ListView) findViewById(R.id.listView_teacher);
		classmembers = new LinkedList<Buddy>();
		result_teachers = new LinkedList<Buddy>();
		result_students = new LinkedList<Buddy>();
		memberResult = new HashMap<String, List<Buddy>>();
		btn_classmember_back.setOnClickListener(this);
		listView_student.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String uid = result_students.get(position).getGUID();
				Buddy buddy = new Database(ClassMembersActivity.this).buddyWithUserID(uid);
				ContactInfoActivity.launch(ClassMembersActivity.this, Person.fromBuddy(buddy),
						buddy == null ? 0 : buddy.getFriendShipWithMe(),true);
				
			}
		});
		listView_teacher.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String uid = result_teachers.get(position).getGUID();
				Buddy buddy = new Database(ClassMembersActivity.this).buddyWithUserID(uid);
				ContactInfoActivity.launch(ClassMembersActivity.this, Person.fromBuddy(buddy),
						buddy == null ? 0 : buddy.getFriendShipWithMe(),true);
			}
		});
	}
	private void getClassMember(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				memberResult = WowTalkWebServerIF.getInstance(ClassMembersActivity.this).getSchoolMembers(schoolId);
				classmembers = memberResult.get(classId);
				for(Buddy buddy:classmembers){
					if(buddy.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
						result_teachers.add(buddy);
					}else if(buddy.getAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
						result_students.add(buddy);
					}
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {				
				teacherAdapter = new ClassTeacherMemberAdapter(ClassMembersActivity.this,result_teachers);
				studentAdapter = new ClassStudentMemberAdapter(ClassMembersActivity.this,result_students);
				listView_teacher.setAdapter(teacherAdapter);
				listView_student.setAdapter(studentAdapter);
				ListViewUtils.setListViewHeightBasedOnChildren(listView_teacher);
				ListViewUtils.setListViewHeightBasedOnChildren(listView_student);
				teacherAdapter.notifyDataSetChanged();
				studentAdapter.notifyDataSetChanged();
			}
			
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_classmember_back:
			finish();
			break;
		}		
	}
	
	class ClassTeacherMemberAdapter extends BaseAdapter{
		private List<Buddy> classmembers;
		private Context context;
		public ClassTeacherMemberAdapter(Context context,List<Buddy> classmembers){
			this.context = context;
			this.classmembers = classmembers;
		}
		@Override
		public int getCount() {
			return classmembers.size();
		}

		@Override
		public Object getItem(int position) {
			return classmembers.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_contact, parent, false);
				holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
				holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			holder.txtContactName.setTextColor(Color.BLACK);
			Buddy b = classmembers.get(position);
			if(b.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
				holder.txtContactName.setText(b.alias);
			}
			IHasPhoto entity = b;
			PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                    R.drawable.default_official_avatar_90, entity, true);
			return convertView;
		}
		class ViewHodler{
			ImageView imgPhoto;
			TextView txtContactName;
		}
		
	}
	
	class ClassStudentMemberAdapter extends BaseAdapter{

		private List<Buddy> classmembers;
		private Context context;
		public ClassStudentMemberAdapter(Context context,List<Buddy> classmembers){
			this.context = context;
			this.classmembers = classmembers;
		}
		@Override
		public int getCount() {
			return classmembers.size();
		}

		@Override
		public Object getItem(int position) {
			return classmembers.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_contact, parent, false);
				holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
				holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			holder.txtContactName.setTextColor(Color.BLACK);
			Buddy b = classmembers.get(position);
			if(b.getAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
				holder.txtContactName.setText(b.alias);
			}
			IHasPhoto entity = b;
			PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                    R.drawable.default_official_avatar_90, entity, true);
			
			return convertView;
		}
		class ViewHodler{
			ImageView imgPhoto;
			TextView txtContactName;
		}
		
	}
}
