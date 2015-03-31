package co.onemeter.oneapp.ui;

import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Classroom;
import org.wowtalk.api.WowTalkWebServerIF;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;
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
	private ClassMemberAdapter adapter;
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
		Log.d("-------------schoolId----------------", schoolId);
		classId = getIntent().getStringExtra("classId");
		btn_classmember_back = (ImageButton) findViewById(R.id.btn_classmember_back);
		listView_student = (ListView) findViewById(R.id.listView_student);
		listView_teacher = (ListView) findViewById(R.id.listView_teacher);
		classmembers = new LinkedList<Buddy>();
		adapter = new ClassMemberAdapter(classmembers);
		listView_teacher.setAdapter(adapter);
		btn_classmember_back.setOnClickListener(this);

	}
	private void getClassMember(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				
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
	
	class ClassMemberAdapter extends BaseAdapter{
		private List<Buddy> classmembers;
		public ClassMemberAdapter(List<Buddy> classmembers){
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
			
			
			return convertView;
		}
		class ViewHodler{
			ImageView imgPhoto;
			TextView txtContactName;
		}
		
	}
}
