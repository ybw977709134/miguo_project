package co.onemeter.oneapp.ui;

import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class WithdrawSuccessActivity extends Activity implements OnClickListener{
	private EditText edtVerification;
	private EditText edtPwd;
	private EditText edtConfirm;
	
	private Button btnBack;
	private Button btnDone;
	private String wowtalkId;
    private MessageBox mMsgBox;
	
	private void resetPassword(final String verificationCode, final String newPassword) {
        mMsgBox.showWait();
		new AsyncTask<Void, Integer,Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(WithdrawSuccessActivity.this).fResetPassword(wowtalkId, verificationCode, newPassword);
			}
			@Override
			protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
				if (ErrorCode.OK == result) {
                    Withdraw1Activity.instance().finish();
                    finish();
                }
			}
		}.execute((Void)null);
	}


	private void initView() {
		edtVerification = (EditText) findViewById(R.id.edt_verification);
		edtPwd = (EditText) findViewById(R.id.edt_pwd);
		edtConfirm = (EditText) findViewById(R.id.edt_confirm);
		
		btnBack = (Button) findViewById(R.id.btn_back);
		btnDone = (Button) findViewById(R.id.btn_done);
		
		btnBack.setOnClickListener(this);
		btnDone.setOnClickListener(this);
		
		edtVerification.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() == 0) {
					edtVerification.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtVerification.setTextColor(Color.WHITE);
				}
			}
			
		});
		edtPwd.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() == 0) {
					edtPwd.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtPwd.setTextColor(Color.WHITE);
				}
			}
			
		});
		
		edtConfirm.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() == 0) {
					edtConfirm.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtConfirm.setTextColor(Color.WHITE);
				}
			}
			
		});
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.btn_back:
			finish();
			break;
		case R.id.btn_done:
			final String verificationCode = edtVerification.getText().toString();
			final String newPassword = edtPwd.getText().toString();
            final String newPasswordConfirm = edtConfirm.getText().toString();
            if (verificationCode.equals("")) {
                mMsgBox.toast(getString(R.string.withdraw_password_must_input_verification));
            } else if (!newPassword.equals(newPasswordConfirm)) {
                mMsgBox.toast(getString(R.string.register_pwd_must_fit));
            } else if (!Utils.verifyWowTalkPwd(newPassword)) {
                mMsgBox.toast(getString(R.string.settings_account_passwd_format_error));
            } else {
                resetPassword(verificationCode, newPassword);
            }
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.withdraw_pwd_success);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        if (getIntent() == null)
			return;
        mMsgBox = new MessageBox(this);
		wowtalkId = getIntent().getStringExtra(Withdraw1Activity.WOWTALK_ID);
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}

