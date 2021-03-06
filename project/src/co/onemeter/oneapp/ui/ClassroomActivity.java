package co.onemeter.oneapp.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.wowtalk.api.Classroom;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import com.androidquery.AQuery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
	private String roomName;
	private MessageBox msgbox;
	private TextView classroom_empty;
	private LinearLayout LinearLayout_release;

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
		LinearLayout_release = (LinearLayout) findViewById(R.id.LinearLayout_release);
		listView_classroom_show = (ListView) findViewById(R.id.listView_classroom_show);
		classroom_empty = (TextView) findViewById(R.id.classroom_empty);
		listView_classroom_show.setOnItemClickListener(this);

		LinearLayout_release.setOnClickListener(this);
		msgbox = new MessageBox(this);
		
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
		case R.id.LinearLayout_release:
			MessageDialog dialog = new MessageDialog(ClassroomActivity.this);
			dialog.setTitle("提示");
			dialog.setMessage("确定要解除绑定教室吗？");
            dialog.setRightBold(true);
            dialog.setOnLeftClickListener("取消", null);
            dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
            	@Override
            	public void onclick(MessageDialog dialog) {
            		releaseClassroom();
            		
                   }
            	});
            dialog.show();
            break;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		roomId = classroom.get(position).id;
		roomName = classroom.get(position).room_name;
		classroomAdapter.setCurrPosition(position);
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
					msgbox.showWaitImageSuccess("选择教室成功");
					Intent intent = new Intent();
				    setResult(RESULT_OK);
				    new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
                	
							finish();
							
						}
					}).start();
				}else if(result == 0){
					msgbox.showWaitImageCaution("该教室已被使用");
				}
			}
			
		});

	}
	private void releaseClassroom(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(ClassroomActivity.this).releaseRoom(lessonId);				
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				if(result == 1){
					msgbox.showWaitImageSuccess("解除成功");
				    setResult(RESULT_OK);
				    
				    new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
                	
							finish();
							
						}
					}).start();
				    
				}else if(result == 0){
					msgbox.showWaitImageCaution("还没有选择教室");
				}
			}
			
		});
	}
	private void getClassroomInfo(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classroom.clear();
				classroom.addAll(LessonWebServerIF.getInstance(ClassroomActivity.this)
						.getClassroom(schoolId, startdate, enddate));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				
				if(classroom.isEmpty() || classroom == null){
					classroom_empty.setVisibility(View.VISIBLE);
					listView_classroom_show.setVisibility(View.GONE);
				}else{
					classroom_empty.setVisibility(View.GONE);
					listView_classroom_show.setVisibility(View.VISIBLE);
				}
				
				classroomAdapter.notifyDataSetChanged();
			}

		});
	}

	
	class ClassroomAdapter extends BaseAdapter{

		private List<Classroom> classroom;
		private int currPosition = -1;
		
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
			holder.classroom_item_name.setText(cl.room_name+" - "+cl.room_num);
			holder.classroom_item_icon.setVisibility(View.INVISIBLE);
			if(currPosition == position){
				holder.classroom_item_icon.setVisibility(View.VISIBLE);
			}
			
			return convertView;
		}
		class ViewHodler{
			ImageView classroom_item_icon;
			TextView classroom_item_name;
		}		
		public void setCurrPosition(int currPosition){
			this.currPosition = currPosition;
			notifyDataSetChanged();
		}
		
	}

}
