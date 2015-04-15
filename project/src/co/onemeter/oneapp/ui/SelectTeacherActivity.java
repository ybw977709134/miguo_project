package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wowtalk.api.Database;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
/**
 * 选择教师页
 * @author hutianfeng
 * @date 2015/4/2
 *
 */
public class SelectTeacherActivity extends Activity implements OnClickListener, OnItemClickListener{
	
	
	private List<Map<String, Object>> classteachers;
	
	private ListView listView_teacher_show;
	private MessageBox msgbox;
	private MyClassTeacherAdapter adapter;
	private LessonWebServerIF lessonWebServer;
	private SelectTeacherActivity instance = null;
	private String class_id;
	private String school_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_teacher);
		instance = this;
		initView();
		getClassTeacherInfo();
	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.teacher_refresh).clicked(this);
		listView_teacher_show = (ListView) findViewById(R.id.listView_teacher_show);
		
		msgbox = new MessageBox(this);
		class_id = getIntent().getStringExtra("classId");
		school_id = getIntent().getStringExtra("school_id");
		lessonWebServer =  LessonWebServerIF.getInstance(SelectTeacherActivity.this);
		classteachers = new ArrayList<Map<String,Object>>();
		listView_teacher_show.setOnItemClickListener(this);
		
	}

	private void getClassTeacherInfo(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, List<Map<String, Object>>>() {

			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {

				 List<Map<String, Object>> reslut = lessonWebServer.getClassTeachers(class_id);
				return reslut;
			}
			
			@Override
			protected void onPostExecute(List<Map<String, Object>> result) {
				msgbox.dismissWait();
				
				if (result != null) {
					
					classteachers.clear();
					classteachers.addAll(result);
					adapter = new MyClassTeacherAdapter (classteachers);
					listView_teacher_show.setAdapter(adapter);
				}
				
			}

		});
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
			
		case R.id.teacher_refresh:
			getClassTeacherInfo();
			break;
		}
		
	}
	
	class MyClassTeacherAdapter extends BaseAdapter{
    	private List<Map<String, Object>> classteachers;
    	private int currPosition = -1;
    	public MyClassTeacherAdapter(List<Map<String, Object>> classteachers){
    		this.classteachers = classteachers;
    	}

		@Override
		public int getCount() {
			return classteachers.size();
		}

		@Override
		public Object getItem(int position) {
			return classteachers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(null == convertView){
				convertView = getLayoutInflater().inflate(R.layout.item_select_lesson, parent, false);
				holder = new ViewHolder();
				
				holder.textView_item_teacher_name = (TextView) convertView.findViewById(R.id.textView_item_lesson_name);
				holder.imageView_item_teacher_icon = (ImageView) convertView.findViewById(R.id.imageView_item_lesson_icon);
				
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.textView_item_teacher_name.setText(classteachers.get(position).get("teacher_alias").toString());
			if(currPosition == position){
				holder.imageView_item_teacher_icon.setVisibility(View.VISIBLE);
			}else{
				holder.imageView_item_teacher_icon.setVisibility(View.INVISIBLE);
			}
			
			return convertView;
		}
		
		class ViewHolder{

			TextView textView_item_teacher_name;
			ImageView imageView_item_teacher_icon;
		}
		
		public void setCurrPosition(int currPosition){
			this.currPosition = currPosition;
			notifyDataSetChanged();
		}
    	
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		adapter.setCurrPosition(position);
		
		Intent data = new Intent();
		data.putExtra("teacher_id", classteachers.get(position).get("teacher_id").toString());
		data.putExtra("teacher_name", classteachers.get(position).get("teacher_alias").toString());
		setResult(RESULT_OK, data);
		
		instance.finish();
		
	}
	
	@Override
	protected void onDestroy() {
		if (instance != null) {
			instance = null;
		}
		
		if (classteachers != null) {
			classteachers = null;
		}
		
		if (lessonWebServer != null) {
			lessonWebServer = null;
		}
		
		super.onDestroy();
	}
	
}
