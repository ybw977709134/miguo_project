package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Camera;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;

import com.androidquery.AQuery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.liveplayer.VideoPlayingActivity;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
/**
 * 摄像头控制页面。
 * Created by zz on 03/03/2015.
 */
public class CameraActivity extends Activity implements OnClickListener, OnItemClickListener{
	private ListView listView_camera_show;
	private List<Camera> cameras;
	private CameraAdapter cameraAdapter;
	private String schoolId;
	private int lessonId;
	private int roomId;
	private int studentTag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		initView();
		studentTag = getIntent().getIntExtra("student_live", 0);
		if(studentTag == 1){
			getCameraInfo_Student();
		}else{
		  getCameraInfo();
		}
		
	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		
		schoolId = getIntent().getStringExtra("schoolId");
		roomId = getIntent().getIntExtra("roomId",0);
		lessonId = getIntent().getIntExtra("lessonId", 0);
		cameras = new  ArrayList<Camera>();
		listView_camera_show = (ListView) findViewById(R.id.listView_camera_show);
		cameraAdapter = new CameraAdapter();
		listView_camera_show.setAdapter(cameraAdapter);	
		listView_camera_show.setOnItemClickListener(this);
		
		if(Utils.isAccoTeacher(this)){
			listView_camera_show.setEnabled(false);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			if(Utils.isAccoTeacher(this)){
				Builder alertDialog = new AlertDialog.Builder(CameraActivity.this);
				alertDialog.setTitle("提示");
				alertDialog.setMessage("确定保存修改吗？");
				alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						saveCamerasStatus();
					}
				});  
				alertDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {			
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}
				});
				alertDialog.create().show();
			}else{
				finish();
			}


			break;
		}
		
	}	
	
	private void saveCamerasStatus(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int len = cameras.size();
				String[] camera_id = new String[len];
				int[] status = new int[len];
				for(int i = 0;i < len;i ++){
					Camera c = cameras.get(i);
					camera_id[i] = c.id + "";
					status[i] = c.status;
				}
				LessonWebServerIF.getInstance(CameraActivity.this).setCameraStatus(schoolId, camera_id, status);
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				finish();
			}

		});
	}
	
	private void getCameraInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				cameras.addAll(LessonWebServerIF.getInstance(CameraActivity.this)
						.getCamera(schoolId, String.valueOf(roomId)));
				Log.i("------cameras--------", cameras+"");
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				cameraAdapter.notifyDataSetChanged();
			}

		});
	}
	private void getCameraInfo_Student(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				cameras.addAll(LessonWebServerIF.getInstance(CameraActivity.this)
						.getCameraByLesson(schoolId, lessonId));
				Log.i("------cameras--------", cameras+"");
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				cameraAdapter.notifyDataSetChanged();
			}

		});
	}
	
	class CameraAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return cameras.size();
		}

		@Override
		public Object getItem(int position) {
			return cameras.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		private boolean isOn = true;
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final int onId = R.drawable.icon_switch_on;
			final int offId = R.drawable.icon_switch_off;
			Camera camera = cameras.get(position);
			final int status = camera.status;
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_cameratable, parent, false);
				holder.camera_item_icon = (ImageView) convertView.findViewById(R.id.camera_item_icon);
				holder.camera_item_name = (TextView) convertView.findViewById(R.id.camera_item_name);
				if(studentTag == 1){
					holder.camera_item_icon.setVisibility(View.GONE);
				}else{
				    holder.camera_item_icon.setVisibility(View.VISIBLE);	
				}
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			if(status == 0){
				holder.camera_item_icon.setImageResource(offId);
				isOn = false;
			}else{
				holder.camera_item_icon.setImageResource(onId);
				isOn = true;
			}
			Camera c = cameras.get(position);
			holder.camera_item_name.setText(c.camera_name);
			OnClickListener listener = new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(isOn){
						((ImageView)v.getTag()).setImageResource(offId);
						cameras.get(position).status = 0;
					}else{
						((ImageView)v.getTag()).setImageResource(onId);
						cameras.get(position).status = 1;
					}
					isOn = !isOn;
				}
			};
			holder.camera_item_icon.setTag(holder.camera_item_icon);
			holder.camera_item_icon.setOnClickListener(listener);
			return convertView;
		}
		class ViewHodler{
			ImageView camera_item_icon;
			TextView camera_item_name;
		}
		
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String httpUrl = cameras.get(position).httpURL;
		Intent i = new Intent();
		i.putExtra("httpURL", httpUrl);
		i.setClass(this, VideoPlayingActivity.class);
		startActivity(i);
		
	}


}
