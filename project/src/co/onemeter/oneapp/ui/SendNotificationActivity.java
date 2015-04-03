package co.onemeter.oneapp.ui;


import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.Moment;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageDialog;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassNotificationActivity.ClassFilterBar;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 发送通知页面。
 * Created by zz on 04/01/2015.
 */
public class SendNotificationActivity extends Activity implements OnClickListener,MenuBar.OnDropdownMenuItemClickListener{
	private EditText edit_text_notice;
	private TextView btn_ok;
	private ImageButton btn_notice_back;
	private List<Moment> listMoment;
	private String classId;
	private String momentId;
	private ClassFilterBar filterBar;
	private String[] className = new String[]{};
	private List<GroupChatRoom> classrooms;
	private String[] classIds = new String[]{};
	private String classDetail_classId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_notification);
		
		initView();
		getSchoolClassInfo();
	}
	
	private void initView(){
		edit_text_notice = (EditText) findViewById(R.id.edit_text_notice);
		btn_notice_back = (ImageButton) findViewById(R.id.btn_notice_back);
		btn_ok = (TextView) findViewById(R.id.btn_ok);
		btn_ok.setOnClickListener(this);
		btn_notice_back.setOnClickListener(this);
		listMoment = new LinkedList<Moment>();
		className = getIntent().getExtras().getStringArray("className");
		classDetail_classId = getIntent().getExtras().getString("classDetail_classId");	
		classrooms = new LinkedList<GroupChatRoom>();
		View c = findViewById(R.id.dialog_container);
        c.setVisibility(View.INVISIBLE);
        filterBar = new ClassFilterBar(this,c);
        LinearLayout lay_class_filter = (LinearLayout) findViewById(R.id.class_filter);       
        if(classDetail_classId == null){
        	View bar_view = filterBar.getView();
            bar_view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            lay_class_filter.addView(bar_view);          
		}else{
			classId = classDetail_classId;
		}      
        filterBar.setOnFilterChangedListener(this);
        filterBar.setStringArrayData(className);

	}
	private void getMomentId(final String classId,final String[] classIds){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				WowTalkWebServerIF.getInstance(SendNotificationActivity.this).getMomentId(listMoment,edit_text_notice.getText().toString());
				return null;
			}			
			@Override
			protected void onPostExecute(Integer result) {	
				momentId = listMoment.get(0).id;
				sendNotice(classId,classIds);				
			}		
		});
		
	}
	
	private void sendNotice(final String classId,final String[] classIds){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				WowTalkWebServerIF.getInstance(SendNotificationActivity.this).addClassBulletin(classId,classIds,momentId);
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}
	private void getSchoolClassInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classrooms.clear();
				classrooms.addAll(WowTalkWebServerIF.getInstance(SendNotificationActivity.this).getSchoolClassRooms(null));
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				if(classId == null ){
					int length = classrooms.size();
					classIds = new String[length];
					for(int i= 0;i < length;i++){
					classIds[i] = classrooms.get(i).groupID;
					}
				}
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
			if(!TextUtils.isEmpty(edit_text_notice.getText().toString())){
				getMomentId(classId,classIds);
			}else{
				MessageDialog dialog = new MessageDialog(SendNotificationActivity.this,false,MessageDialog.SIZE_NORMAL);
	            dialog.setTitle("");
	            dialog.setMessage("公告内容不能为空");
	            dialog.show();
			}			
			break;
		}
		
	}

	@Override
	public void onDropdownMenuShow(int subMenuResId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDropdownMenuItemClick(int subMenuResId, int itemIdx) {
		if(itemIdx == 0){
			int length = classrooms.size();
			classIds = new String[length];
			for(int i= 0;i < length;i++){
				classIds[i] = classrooms.get(i).groupID;
			}
			classId = null;
		}else{
			classId = classrooms.get(itemIdx-1).groupID;
		}
	}
	

}
