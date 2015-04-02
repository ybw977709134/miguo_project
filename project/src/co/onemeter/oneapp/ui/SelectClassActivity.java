package co.onemeter.oneapp.ui;

import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 教室班级页面。
 * Created by zz on 03/31/2015.
 */
public class SelectClassActivity extends Activity implements OnClickListener, OnItemClickListener{
	private List<GroupChatRoom> classrooms;
	
	private ListView listView_class_show;
	private MessageBox msgbox;
	private MyClassAdapter adapter;
	private WowTalkWebServerIF talkwebserver;
	private SelectClassActivity instance = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_class);
		instance = this;
		initView();
		getClassInfo();

	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.class_refresh).clicked(this);
		listView_class_show = (ListView) findViewById(R.id.listView_class_show);
		msgbox = new MessageBox(this);
		talkwebserver =  WowTalkWebServerIF.getInstance(SelectClassActivity.this);
		classrooms = new LinkedList<GroupChatRoom>();
		listView_class_show.setOnItemClickListener(this);
		
	}

	private void getClassInfo(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classrooms.clear();
				classrooms.addAll(talkwebserver.getSchoolClassRooms(null));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				adapter = new MyClassAdapter(classrooms);
				listView_class_show.setAdapter(adapter);
				
				if (classrooms.size() == 0) {
					msgbox.show(null, "你还没有进入任何班级");
					msgbox.dismissDialog();
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
		case R.id.class_refresh:
			getClassInfo();
			break;
		}
		
	}
	
	class MyClassAdapter extends BaseAdapter{
    	private List<GroupChatRoom> classrooms;
    	private int currPosition = -1;
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
				holder.myclass_item_icon = (ImageView) convertView.findViewById(R.id.myclass_item_icon);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.item_myclass_textview.setText(classrooms.get(position).groupNameOriginal);
			if(currPosition == position){
				holder.myclass_item_icon.setVisibility(View.VISIBLE);
			}else{
				holder.myclass_item_icon.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}
		class ViewHolder{
			ImageView item_myclass_imageview;
			TextView item_myclass_textview;
			ImageView myclass_item_icon;
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
		data.putExtra("class_id", classrooms.get(position).getGUID());
		data.putExtra("class_name", classrooms.get(position).groupNameOriginal);
		setResult(RESULT_OK, data);
		
		instance.finish();
		
	}
	
	@Override
	protected void onDestroy() {
		if (instance != null) {
			instance = null;
		}
		
		if (classrooms != null) {
			classrooms = null;
		}
		
		if (talkwebserver != null) {
			talkwebserver = null;
		}
		
		super.onDestroy();
	}


}
