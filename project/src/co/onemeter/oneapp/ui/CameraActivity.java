package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Camera;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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
	private String lessonName;
	private int studentTag;
	private MessageBox msgbox;
	private TextView carema_empty;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		initView();
		studentTag = getIntent().getIntExtra("student_live", 0);
		if(studentTag == 1){
			getCameraInfo_Student();
		}else{
			if(roomId != 0){
				getCameraInfo();
			}
		  
		}
		
	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		msgbox = new MessageBox(this);
		schoolId = getIntent().getStringExtra("schoolId");
		roomId = getIntent().getIntExtra("roomId",0);
		lessonName = getIntent().getStringExtra("lessonName");
		lessonId = getIntent().getIntExtra("lessonId", 0);
		cameras = new  ArrayList<Camera>();
		listView_camera_show = (ListView) findViewById(R.id.listView_camera_show);
		carema_empty = (TextView) findViewById(R.id.carema_empty);
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
				setResult(RESULT_OK);
				finish();
			}

		});
	}
	
	private void getCameraInfo(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				cameras.addAll(LessonWebServerIF.getInstance(CameraActivity.this)
						.getCamera(schoolId, String.valueOf(roomId)));
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				if(cameras.isEmpty() || cameras == null){
					carema_empty.setVisibility(View.VISIBLE);
					listView_camera_show.setVisibility(View.GONE);
				}else{
					carema_empty.setVisibility(View.GONE);
					listView_camera_show.setVisibility(View.VISIBLE);
				}
				cameraAdapter.notifyDataSetChanged();
			}

		});
	}
	private void getCameraInfo_Student(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				cameras.addAll(LessonWebServerIF.getInstance(CameraActivity.this)
						.getCameraByLesson(schoolId, lessonId));
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				if(cameras.isEmpty() || cameras == null){
					carema_empty.setVisibility(View.VISIBLE);
					listView_camera_show.setVisibility(View.GONE);
				}else{
					carema_empty.setVisibility(View.GONE);
					listView_camera_show.setVisibility(View.VISIBLE);
				}
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
				holder.camera_iv_isONorOFF = (ImageView) convertView.findViewById(R.id.camera_iv_isONorOFF);
				holder.camera_tv_isONorOFF =  (TextView) convertView.findViewById(R.id.camera_tv_isONorOFF);
				holder.camera_item_name = (TextView) convertView.findViewById(R.id.camera_item_name);
				holder.camera_LinearLayout_isONorOFF = (LinearLayout) convertView.findViewById(R.id.camera_LinearLayout_isONorOFF);
				if(studentTag == 1){
					holder.camera_item_icon.setVisibility(View.GONE);
					holder.camera_LinearLayout_isONorOFF.setVisibility(View.VISIBLE);
					
				}else{
					holder.camera_LinearLayout_isONorOFF.setVisibility(View.GONE);
				    holder.camera_item_icon.setVisibility(View.VISIBLE);	
				}
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			if(status == 0){
				holder.camera_item_icon.setImageResource(offId);
				holder.camera_tv_isONorOFF.setText("OFF");
				holder.camera_iv_isONorOFF.setImageResource(R.drawable.icon_section_row_invalid);
				isOn = false;
			}else{
				holder.camera_item_icon.setImageResource(onId);
				holder.camera_tv_isONorOFF.setText("ON");
				holder.camera_iv_isONorOFF.setImageResource(R.drawable.icon_section_row);
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
			TextView camera_tv_isONorOFF;
			ImageView camera_iv_isONorOFF;
			LinearLayout camera_LinearLayout_isONorOFF;
		}
		
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, final int position,
			long id) {
		if(cameras.get(position).status == 0){
			Toast.makeText(this, R.string.class_camera_closed, Toast.LENGTH_SHORT).show();
		}else{
			if(!Utils.isNetworkAvailable(this)){
				Toast.makeText(this, R.string.network_connection_unavailable, Toast.LENGTH_SHORT).show();
			}else{
				if(Utils.is3G(this)){
					Builder alertDialog = new AlertDialog.Builder(CameraActivity.this);
					alertDialog.setTitle(R.string.class_camera_title);
					alertDialog.setMessage(R.string.class_camera_is3G);
					alertDialog.setPositiveButton(R.string.class_camera_ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							gotoVideoPlayer(position);
						}
					});  
					alertDialog.setNegativeButton(R.string.class_camera_cancel, new DialogInterface.OnClickListener() {			
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							
						}
					});
					alertDialog.create().show();
				}else if(Utils.isWifi(this)){
					gotoVideoPlayer(position);
				}
			}
		}
	}
	private void gotoVideoPlayer(int position){
		String httpUrl = cameras.get(position).httpURL;
	    Intent i = new Intent();
	    i.putExtra("httpURL", httpUrl);
	    i.putExtra("lessonName", lessonName);
	    i.setClass(this, VideoPlayingActivity.class);
	    startActivity(i);
	}


}
