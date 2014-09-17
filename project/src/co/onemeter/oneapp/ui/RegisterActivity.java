package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

public class RegisterActivity extends Activity implements OnClickListener{
	
	private static final int MSG_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_ALREADY_EXIST = 102;
    private static final int MSG_ERROR_UNKNOWN = 104;
	
	private EditText edtAccount;
	private EditText edtPwd;
	private EditText edtPwdConfirm;
	
	private Button btnBack;
	private Button btnCreate;

    private MessageBox mMsgBox;
	
	private Buddy buddy = new Buddy();
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_REGISTER_SUCCESS:
			{
                RegisterActivity.this.startActivity(
                        new Intent(RegisterActivity.this, StartActivity.class));

                LoginActivity.intance().finish();
                finish();
			}
			break;
			case MSG_USER_ALREADY_EXIST:
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
				dialog.setTitle(null).setMessage(R.string.reg_failed_username_was_taken);
				dialog.setNegativeButton("OK", 
						new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create().show();
			}
			break;
			default:
                mMsgBox.show(null, getString(R.string.register_failure));
                break;
			}
		}
	};
	
	private void initView() {
		btnBack = (Button) findViewById(R.id.back_button);
		btnCreate = (Button) findViewById(R.id.create_button);
		
		edtAccount = (EditText) findViewById(R.id.account_edit);
		edtPwd = (EditText) findViewById(R.id.pwd_edit);
		edtPwdConfirm = (EditText) findViewById(R.id.pwd_confirm_edit);
		
		btnBack.setOnClickListener(this);
		btnCreate.setOnClickListener(this);
		
		edtAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					edtAccount.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtAccount.setTextColor(Color.WHITE);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		edtPwd.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					edtPwd.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtPwd.setTextColor(Color.WHITE);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		edtPwdConfirm.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					edtPwdConfirm.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtPwdConfirm.setTextColor(Color.WHITE);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
	}

    private void loginRequire(final String oldUId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int result = WowTalkWebServerIF.getInstance(RegisterActivity.this).fLogin(
                        edtAccount.getText().toString(),
                        edtPwd.getText().toString(), buddy);
                if (result == ErrorCode.OK) {
                    Log.i("register login ok");

                    WowTalkWebServerIF.getInstance(RegisterActivity.this).fGetMyProfile();
                    PrefUtil.getInstance(RegisterActivity.this).setAutoLogin(false);
                }

                String newUId = PrefUtil.getInstance(RegisterActivity.this).getUid();
                LoginActivity.clearCachedValues(oldUId, newUId);

                mMsgBox.dismissWait();
                Message msg = Message.obtain();
                msg.what = MSG_REGISTER_SUCCESS;
                mHandler.sendMessage(msg);
            }
        }).start();
    }
	
	private void fRegister() {
		
		final String strUserName = edtAccount.getText().toString();
		final String strPassword = edtPwd.getText().toString();

        if (!Utils.verifyWowTalkId(strUserName)) {
            mMsgBox.show(null, getString(R.string.setting_wowtalkid_format_error));
            return;
        }

        if (!Utils.verifyWowTalkPwd(strPassword)) {
            mMsgBox.show(null, getString(R.string.settings_account_passwd_format_error));
            return;
        }

		if (fIsPasswordFit()) {
		    mMsgBox.showWait();
			new Thread(new Runnable() {

				@Override
				public void run() {
                    String oldUId = PrefUtil.getInstance(RegisterActivity.this).getPrevUid();
                    int result = WowTalkWebServerIF.getInstance(RegisterActivity.this)
                            .fRegister(strUserName, strPassword, buddy);
					Log.i("register, the result_code is " + result);

					Message msg = Message.obtain();
					if (result == ErrorCode.OK) {
                        loginRequire(oldUId);
					} else if (result == ErrorCode.USER_ALREADY_EXISTS) {
						msg.what = MSG_USER_ALREADY_EXIST;
						mHandler.sendMessage(msg);
					} else {
						msg.what = MSG_ERROR_UNKNOWN;
						mHandler.sendMessage(msg);
					}

                    if(result != ErrorCode.OK) {
                        mMsgBox.dismissWait();
                    }
				}
				
			}).start();
		} else {
			return;
		}
	}
	
	private boolean fIsPasswordFit() {
		if (edtPwd.getText().toString().equals(edtPwdConfirm.getText().toString())) {
			return true;
		} else {
//			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//			dialog.setTitle(null).setMessage("密码不一致!").setNegativeButton("OK",
//					new DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							dialog.dismiss();
//						}
//					}).create().show();
            mMsgBox.toast(R.string.register_pwd_must_fit);
			return false;
		}
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.back_button:
			finish();
			break;
		case R.id.create_button:
			fRegister();
			break;
		default:
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
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
