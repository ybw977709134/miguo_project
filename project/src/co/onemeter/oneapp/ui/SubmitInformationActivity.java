package co.onemeter.oneapp.ui;

import org.wowtalk.ui.MessageBox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.R;
public class SubmitInformationActivity extends Activity implements OnClickListener {
	public final static String SUBMITNAME = "name";
	public final static String SUBMITPHONE = "phone";
	
	private ImageButton submit_left_button;
	private TextView submit_right_button;
	private Button submit_confirm;
	private EditText submit_accountInput;
	private EditText submit_telephone;
	private MessageBox msgbox;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit_information);
		initView();

        
	}

	private void initView() {
		submit_left_button = (ImageButton) findViewById(R.id.submit_left_button);
		submit_right_button = (TextView) findViewById(R.id.submit_right_button);
		submit_confirm = (Button) findViewById(R.id.submit_confirm);
		submit_accountInput = (EditText) findViewById(R.id.submit_accountInput);
		submit_telephone = (EditText) findViewById(R.id.submit_telephone);
		
		submit_left_button.setOnClickListener(this);
		submit_right_button.setOnClickListener(this);
		submit_confirm.setOnClickListener(this);
		
		msgbox = new MessageBox(this);
		
	}

	private boolean isInfoFill(){
		if(!TextUtils.isEmpty(submit_accountInput.getText().toString())&&!TextUtils.isEmpty(submit_telephone.getText().toString())){
			return true;
		}
		return false;
	}
	
	private void setResultInfo(){
		if(isInfoFill()){
			Intent data = new Intent();
			data.putExtra(SUBMITNAME, submit_accountInput.getText().toString());
			data.putExtra(SUBMITPHONE, submit_telephone.getText().toString());
			setResult(RESULT_OK, data);
			finish();
		}else{
			msgbox.toast(R.string.event_info_fill,1000);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.submit_left_button:
			finish();
			break;
		case R.id.submit_right_button:
			setResultInfo();
			break;
		case R.id.submit_confirm:
			setResultInfo();
			break;
		}
		
	}


}
