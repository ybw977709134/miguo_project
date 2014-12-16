package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;

public class FriendValidateActivity extends Activity implements OnClickListener{
	private TextView textView_validate_cancel;
	private TextView textView_validate_send;
	private EditText editText_validate_message;
	private EditText editText_validate_name;
	private String validateContent = "";

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friend_validate);
		initView();

	}

	private void initView() {
		textView_validate_send = (TextView) findViewById(R.id.textView_validate_send);
		textView_validate_cancel = (TextView) findViewById(R.id.textView_validate_cancel);
		editText_validate_message =  (EditText) findViewById(R.id.editText_validate_message);
		editText_validate_name =  (EditText) findViewById(R.id.editText_validate_name);
		textView_validate_send.setOnClickListener(this);
		textView_validate_cancel.setOnClickListener(this);
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
		finish();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.textView_validate_cancel:
			finish();
			break;
		case R.id.textView_validate_send:
			sendMessage();
			break;
		}
		
	}
}
