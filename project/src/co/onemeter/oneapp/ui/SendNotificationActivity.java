package co.onemeter.oneapp.ui;


import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Classroom;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.api.MomentWebServerIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import android.app.Activity;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassMembersActivity.ClassStudentMemberAdapter;
import co.onemeter.oneapp.ui.ClassMembersActivity.ClassTeacherMemberAdapter;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 发送通知页面。
 * Created by zz on 04/01/2015.
 */
public class SendNotificationActivity extends Activity implements OnClickListener{
	private EditText edit_text_notice;
	private TextView btn_ok;
	private ImageButton btn_notice_back;
	private List<Moment> listMoment;
	private String classId;
	private String momentId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_notification);
		
		initView();

	}
	
	private void initView(){
		edit_text_notice = (EditText) findViewById(R.id.edit_text_notice);
		btn_notice_back = (ImageButton) findViewById(R.id.btn_notice_back);
		btn_ok = (TextView) findViewById(R.id.btn_ok);
		btn_ok.setOnClickListener(this);
		btn_notice_back.setOnClickListener(this);
		listMoment = new LinkedList<Moment>();
		classId = "1678ff8f-2a41-438a-bb22-4f55530857f1";

	}
	private void getMomentId(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				WowTalkWebServerIF.getInstance(SendNotificationActivity.this).getMomentId(listMoment,edit_text_notice.getText().toString());
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {	
				momentId = listMoment.get(0).id;
				sendNotice();
				
			}
			
		});
		
	}
	
	private void sendNotice(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				WowTalkWebServerIF.getInstance(SendNotificationActivity.this).addClassBulletin(classId, momentId);
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_notice_back:
			finish();
			break;
		case R.id.btn_ok:
			getMomentId();
			break;
		}
		
	}
	

}
