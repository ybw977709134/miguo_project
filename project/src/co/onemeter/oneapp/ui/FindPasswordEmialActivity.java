package co.onemeter.oneapp.ui;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class FindPasswordEmialActivity extends Activity implements OnClickListener{
	
	
	private ImageButton title_back;
	private TextView textView_findPassword_back;//返回
	private TextView textView_findPassword_cancel;//取消
	
	
	private EditText txt_bind_account;//输入账号
	private EditText txt_bind_email;//输入绑定邮箱
	
	private TextView textView_verification_email_result;//账号和邮箱的验证结果
	private Button btn_verification_email;//确认找回密码按钮
	
	private MessageBox mMsgBox;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_password_email);
		mMsgBox = new MessageBox(this);
		initView();
	}
	
	
	
	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_findPassword_back = (TextView) findViewById(R.id.textView_findPassword_back);
		textView_findPassword_cancel = (TextView) findViewById(R.id.textView_findPassword_cancel);
		
		txt_bind_account = (EditText) findViewById(R.id.txt_bind_account);
		txt_bind_email = (EditText) findViewById(R.id.txt_bind_email);
		
		textView_verification_email_result = (TextView) findViewById(R.id.textView_verification_email_result);
		btn_verification_email = (Button) findViewById(R.id.btn_verification_email);
		
		title_back.setOnClickListener(this);
				
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
		case R.id.textView_findPassword_back:
			finish();
			break;
		case R.id.textView_findPassword_cancel:
			break;
			
		case R.id.btn_verification_email://重新获得密码
			textView_verification_email_result.setVisibility(View.INVISIBLE);
			resetPassword(txt_bind_account.getText().toString(),txt_bind_email.getText().toString());
			break;

		default:
			break;
		}
		
	}
	
	private void resetPassword(final String wowtalk_id,final String emailAddress) {
    	mMsgBox.showWait();
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FindPasswordEmialActivity.this).newRetrievePassword(wowtalk_id,emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0
                    	mMsgBox.show(null,"重置密码成功,请查收你的绑定邮箱");
                    	mMsgBox.dismissDialog();
                        break;
                        
                    case ErrorCode.EMAIL_ADDRESS_VERIFICATION_CODE_ERROR:
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("你输入的邮箱格式有误");
                    	break;
                    	
                    case ErrorCode.USER_NOT_EXISTS:
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("你输入的账号不存在");
                    	break;
                    	
                    case ErrorCode.ERR_OPERATION_DENIED:
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("该用户已绑定到其它邮箱");
                    	break;
                    	
                    case ErrorCode.EMAIL_USED_BY_OTHERS:
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("该邮箱已被其他用户绑定");
                    	break;
                    	
                    	
                    default://邮箱绑定失败
                        mMsgBox.show(null, "你输入的账号或邮箱有误，请重新输入");
                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
    }
}
























