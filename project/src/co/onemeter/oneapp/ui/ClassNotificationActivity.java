package co.onemeter.oneapp.ui;


import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Bulletins;
import org.wowtalk.api.Classroom;
import org.wowtalk.api.Database;
import org.wowtalk.api.IHasPhoto;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.MomentWebServerIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 班级通知页面。
 * Created by zz on 03/31/2015.
 */
public class ClassNotificationActivity extends Activity implements OnClickListener, OnItemClickListener{
	private ImageButton btn_notice_back;
	private ImageButton btn_add;
	private ListView listView_notice_show;
	private List<Bulletins> bulletins;
	private BulletinAdapter adapter;
	private String classId;
	private MessageBox msgbox;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_notification);
		
		initView();
		getClassBulletin();

	}
	
	private void initView(){
		btn_notice_back = (ImageButton) findViewById(R.id.btn_notice_back);
		btn_add = (ImageButton) findViewById(R.id.btn_add);
		listView_notice_show = (ListView) findViewById(R.id.listView_notice_show);
		btn_notice_back.setOnClickListener(this);
		btn_add.setOnClickListener(this);
		listView_notice_show.setOnItemClickListener(this);
		bulletins = new LinkedList<Bulletins>();
		classId = "1678ff8f-2a41-438a-bb22-4f55530857f1";
		msgbox = new MessageBox(this);

	}
	private void getClassBulletin(){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(ClassNotificationActivity.this);
				bulletins  = web.fGetClassBulletin(classId,0,0);
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				msgbox.dismissWait();
				adapter = new BulletinAdapter(ClassNotificationActivity.this,bulletins);
				listView_notice_show.setAdapter(adapter);
				adapter.notifyDataSetChanged();
				
			}

		});
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_notice_back:
			finish();
			break;
		case R.id.btn_add:
			Intent intent = new Intent(ClassNotificationActivity.this, SendNotificationActivity.class);
			startActivityForResult(intent, 1001);;
			break;
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(requestCode == 1001){
				getClassBulletin();
			}
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		
	}
	
	class BulletinAdapter extends BaseAdapter{
		private List<Bulletins> bulletins;
		private Context context;

		public BulletinAdapter(Context context,List<Bulletins> bulletins){
			this.context = context;
			this.bulletins = bulletins;
		}
		@Override
		public int getCount() {
			return bulletins.size();
		}

		@Override
		public Object getItem(int position) {
			return bulletins.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_notice, parent, false);
				holder.notice_photo = (ImageView) convertView.findViewById(R.id.notice_photo);
				holder.notice_name = (TextView) convertView.findViewById(R.id.notice_name);
				holder.notice_time = (TextView) convertView.findViewById(R.id.notice_time);
				holder.textView_notice = (TextView) convertView.findViewById(R.id.textView_notice);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Bulletins b = bulletins.get(position);
			holder.textView_notice.setText(b.text);
			holder.notice_time.setText(String.valueOf(Utils.stampsToDateTime(b.timestamp)));
//			Buddy buddy = new Buddy(b.uid);
			Database dbHelper=new Database(context);
            Buddy buddy=dbHelper.buddyWithUserID(b.uid);
            IHasPhoto entity = buddy;
			PhotoDisplayHelper.displayPhoto(context, holder.notice_photo,
                    R.drawable.default_official_avatar_90, entity, true);
			
//			String name =!TextUtils.isEmpty(buddy.alias)?buddy.alias:buddy.nickName;
			holder.notice_name.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
			return convertView;
		}
		class ViewHodler{
			ImageView notice_photo;
			TextView notice_name;
			TextView notice_time;
			TextView textView_notice;
		}	
		
	}

}
