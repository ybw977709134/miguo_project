package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;

import java.util.ArrayList;

public class FriendValidateActivity extends Activity implements OnClickListener{
	private TextView textView_validate_cancel;
	private TextView textView_validate_send;
	private EditText editText_validate_message;
	private Database mDbHelper = new Database(this);
	WowTalkWebServerIF mwebserver;
    private ArrayList<Buddy> buddyList = null;
    private ArrayList<Buddy> buddyList2 = null;
    private int position;
	private Buddy buddy;


    @SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friend_validate);
		initView();
		buddyList = (ArrayList<Buddy>) getIntent().getSerializableExtra("buddyList");
		buddyList2 = (ArrayList<Buddy>) getIntent().getSerializableExtra("buddyList2");
		position = getIntent().getExtras().getInt("position");
	}

	private void initView() {
		textView_validate_send = (TextView) findViewById(R.id.textView_validate_send);
		textView_validate_cancel = (TextView) findViewById(R.id.textView_validate_cancel);
		editText_validate_message =  (EditText) findViewById(R.id.editText_validate_message);
//		editText_validate_name =  (EditText) findViewById(R.id.editText_validate_name);
		textView_validate_send.setOnClickListener(this);
		textView_validate_cancel.setOnClickListener(this);
		mwebserver = WowTalkWebServerIF.getInstance(this);
	}
	private void sendMessage(){
		Toast toast;
		toast = Toast.makeText(this, getResources().getString(R.string.contacts_validate_toast), Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		LinearLayout toastView = (LinearLayout) toast.getView();
		ImageView imageOK = new ImageView(getApplicationContext());
		imageOK.setImageResource(R.drawable.icon_contact_add_success);
		toastView.addView(imageOK, 0);
		toast.show();
//		Intent intent = new Intent(this, StartActivity.class);
//		startActivity(intent);
//		finish();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.textView_validate_cancel:
			finish();
			break;
		case R.id.textView_validate_send:
			android.util.Log.d("--------------", buddyList+"");
//			String messageContent = editText_validate_message.getText().toString();
			
			if(buddyList2 != null){
				buddy = buddyList2.get(0);
			}else{
				buddy = buddyList.get(position);
			}
			onAddFriendPressed(buddy);
			sendMessage();
			
			break;
		}
		
	}
	
    private void onAddFriendPressed(final Buddy buddy) {
    	final String messageContent = editText_validate_message.getText().toString();
//        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Buddy, Integer, Void>() {
			int errno = ErrorCode.OK;
			PendingRequest pr;

			@Override
			protected Void doInBackground(Buddy... params) {
				Buddy b = params[0];
				errno = mwebserver.faddBuddy_askforRequest(b.userID, messageContent);
				if (ErrorCode.OK == errno) {
					pr = new PendingRequest();
					pr.uid = b.userID;
					pr.nickname = b.nickName;
					pr.buddy_photo_timestamp = b.photoUploadedTimeStamp;
					pr.type = PendingRequest.BUDDY_OUT;

				}
				return null;

			}

			@Override
			protected void onPostExecute(Void v) {
//                mMsgBox.dismissWait();
				if (errno == ErrorCode.OK) {
					mDbHelper.storePendingRequest(pr);
					if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
//                        mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
					} else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
//                        mMsgBox.show(null, getResources().getString(R.string.contacts_add_buddy_pending_out));
					}
					setResult(RESULT_OK);
					finish();
				} else if (errno == ErrorCode.ERR_OPERATION_DENIED) {
//                    mMsgBox.show(null, getResources().getString(R.string.contactinfo_add_friend_denied));
				} else {
//                    mMsgBox.show(null, getResources().getString(R.string.operation_failed));
				}
			}

		}, buddy);
    }
}
