package co.onemeter.oneapp.ui;

import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Classroom;
import org.wowtalk.api.LessonWebServerIF;

import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 教室选择页面。
 * Created by zz on 03/03/2015.
 */
public class ClassroomActivity extends Activity implements OnClickListener, OnItemClickListener{
	private List<Classroom> classroom;
	private ClassroomAdapter classroomAdapter;
	private ListView listView_classroom_show;
	private String schoolId;
	private long startdate;
	private long enddate;
	private int roomId;
	private int lessonId;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_classroom);
		
		initView();
		getClassroomInfo();
	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.classroom_refresh).clicked(this);
		listView_classroom_show = (ListView) findViewById(R.id.listView_classroom_show);
		listView_classroom_show.setOnItemClickListener(this);
		
		classroom = new LinkedList<Classroom>();
		classroomAdapter = new ClassroomAdapter(classroom);
		listView_classroom_show.setAdapter(classroomAdapter);
		
		schoolId = getIntent().getStringExtra("schoolId");
		startdate = getIntent().getLongExtra("start_date", 0);
		enddate = getIntent().getLongExtra("end_date", 0);
		lessonId = getIntent().getIntExtra("lessonId", 0);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.classroom_refresh:
			getClassroomInfo();
			break;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		roomId = classroom.get(position).id;
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(ClassroomActivity.this).setUseRoom(roomId, lessonId + "");				
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				if(result == 1){
					Toast.makeText(ClassroomActivity.this, "OK", Toast.LENGTH_LONG).show();
					Intent intent = new Intent();
				    intent.putExtra("roomId", roomId);
				    setResult(RESULT_OK, intent);
				    finish();
				}else if(result == 0){
					Toast.makeText(ClassroomActivity.this, "Room is busy", Toast.LENGTH_LONG).show();
				}else if(result == -1){
					Toast.makeText(ClassroomActivity.this, "stupid", Toast.LENGTH_LONG).show();
				}
			}
			
		});

	}
	
	private void getClassroomInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classroom.addAll(LessonWebServerIF.getInstance(ClassroomActivity.this)
						.getClassroom(schoolId, startdate, enddate));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				classroomAdapter.notifyDataSetChanged();
			}

		});
	}
	
	class ClassroomAdapter extends BaseAdapter{

		private List<Classroom> classroom;
		
		public ClassroomAdapter(List<Classroom> classroom){
			this.classroom = classroom;
		}
		@Override
		public int getCount() {
			return classroom.size();
		}

		@Override
		public Object getItem(int position) {
			return classroom.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_classroomtable, parent, false);
				holder.classroom_item_icon = (ImageView) convertView.findViewById(R.id.classroom_item_icon);
				holder.classroom_item_name = (TextView) convertView.findViewById(R.id.classroom_item_name);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Classroom cl = classroom.get(position);
			holder.classroom_item_name.setText(cl.room_name);
			return convertView;
		}
		class ViewHodler{
			ImageView classroom_item_icon;
			TextView classroom_item_name;
		}
		
	}

}
