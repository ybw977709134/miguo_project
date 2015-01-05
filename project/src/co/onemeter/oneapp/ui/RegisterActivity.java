package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.MessageBox;

public class RegisterActivity extends Activity implements OnClickListener{
	
	private static final int MSG_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_ALREADY_EXIST = 102;
    private static final int MSG_ERROR_UNKNOWN = 104;
	
	private EditText edtAccount;
	private EditText edtPwd;
	private EditText edtPwdConfirm;
	
	private View btnBack;
	private Button btnCreate;

    private MessageBox mMsgBox;
	
	private Buddy buddy = new Buddy();
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_REGISTER_SUCCESS:
			{
				String[] args = (String[])msg.obj;
				startActivity(new Intent(RegisterActivity.this, LoginActivity.class)
						.putExtra(LoginActivity.EXTRA_USERNAME, args[0])
						.putExtra(LoginActivity.EXTRA_PASSWORD, args[1]));
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
		btnBack = findViewById(R.id.title_back);
		btnCreate = (Button) findViewById(R.id.create_button);
		
		edtAccount = (EditText) findViewById(R.id.account_edit);
		edtPwd = (EditText) findViewById(R.id.pwd_edit);
		edtPwdConfirm = (EditText) findViewById(R.id.pwd_confirm_edit);
		
		btnBack.setOnClickListener(this);
		btnCreate.setOnClickListener(this);
	}

	private void fRegister() {
		
		final String strUserName = edtAccount.getText().toString();
		final String strPassword = edtPwd.getText().toString();
        final int userType = new AQuery(this).find(R.id.rad_teacher).isChecked() ?
                Buddy.ACCOUNT_TYPE_TEACHER : Buddy.ACCOUNT_TYPE_STUDENT;

        if (!Utils.verifyWowTalkId(strUserName)) {
            mMsgBox.toast(R.string.setting_wowtalkid_format_error);
            return;
        }

        if (!Utils.verifyWowTalkPwd(strPassword)) {
            mMsgBox.toast(R.string.settings_account_passwd_format_error);
            return;
        }

		if (fIsPasswordFit()) {
		    mMsgBox.showWait();
			new Thread(new Runnable() {

				@Override
				public void run() {
                    int result = WebServerIF.getInstance(RegisterActivity.this)
                            .fRegister(strUserName, strPassword, userType, buddy);
					Log.i("register, the result_code is " + result);

					Message msg = Message.obtain();
					if (result == ErrorCode.OK) {
						msg.what = MSG_REGISTER_SUCCESS;
						msg.obj = new String[]{ strUserName, strPassword };
						mHandler.sendMessage(msg);
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
		case R.id.title_back:
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
		setContentView(R.layout.activity_register_page);

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
