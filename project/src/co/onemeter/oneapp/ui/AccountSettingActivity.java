package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.R;

public class AccountSettingActivity extends Activity implements OnClickListener{
    private static final String TAG = "AccountSettingActivity";
	public static final int REQ_INPUT_ID = 11;
	public static final int REQ_INPUT_PASSWORD = 12;

	private ImageButton btnTitleBack;
	
	private TextView txtYuanquID;
	private TextView txtPwdSetting;
	private TextView txtPhonenumber;
	private TextView txtEmail;
	private Button btnLogout;
	
	private LinearLayout mUsername;
	private LinearLayout mPassword;
	private LinearLayout mBindPhone;
	private LinearLayout mBindEmail;
	private String[] binds = new String[2];
	private static AccountSettingActivity instance;

    WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
	private MessageBox mMsgBox;

	public static final AccountSettingActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnLogout = (Button) findViewById(R.id.btn_logout);
		txtYuanquID = (TextView) findViewById(R.id.yuanqu_id_text);
		txtPwdSetting = (TextView) findViewById(R.id.pwd_text);
		txtPhonenumber = (TextView) findViewById(R.id.phonenumber_text);
		txtEmail = (TextView) findViewById(R.id.email_text);
		mUsername = (LinearLayout) findViewById(R.id.layout_id);
		mPassword = (LinearLayout) findViewById(R.id.layout_password);
		mBindPhone = (LinearLayout) findViewById(R.id.layout_phone);
		mBindEmail = (LinearLayout) findViewById(R.id.layout_email);
		txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
		txtPwdSetting.setTextColor(getResources().getColor(R.color.orange));
		
		btnTitleBack.setOnClickListener(this);
		btnLogout.setOnClickListener(this);
		mUsername.setOnClickListener(this);
		mPassword.setOnClickListener(this);
		mBindPhone.setOnClickListener(this);
		mBindEmail.setOnClickListener(this);
	}
	
	private void fetchData() {

		if (mPrefUtil.getMyPasswordChangedState()) {
            txtPwdSetting.setText(getResources().getString(R.string.settings_account_setted));
            txtPwdSetting.setTextColor(getResources().getColor(R.color.text_gray1));
		} else {
            txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
            txtPwdSetting.setTextColor(getResources().getColor(R.color.orange));
		}

        if (mPrefUtil.getMyUsernameChangedState()) {
            txtYuanquID.setText(mPrefUtil.getMyUsername());
            txtYuanquID.setTextColor(getResources().getColor(R.color.text_gray1));
        } else {
            txtYuanquID.setText(getResources().getString(R.string.settings_account_not_set));
            txtYuanquID.setTextColor(getResources().getColor(R.color.orange));
        }
	}
	
	private void queryBindings() {
        mBindPhone.setEnabled(false);
        mBindEmail.setEnabled(false);
        final long startTime = System.currentTimeMillis();
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(AccountSettingActivity.this).fGetBindedStuff(binds);
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == ErrorCode.OK) {
					if (binds[0] != null && !binds[0].equals("")) {
						txtEmail.setText(binds[0]);
					} else {
						txtEmail.setText(getResources().getString(R.string.not_binded));
					}
					if (binds[1] != null && !binds[1].equals("")) {
						txtPhonenumber.setText(binds[1]);
					} else {
						txtPhonenumber.setText(getResources().getString(R.string.not_binded));
					}
				}
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mBindPhone.setEnabled(true);
                        mBindEmail.setEnabled(true);
                        long endTime = System.currentTimeMillis();
                        Log.d(TAG, ", onResume, the time of invoking queryBinds() is " + (endTime - startTime) + " milliseconds.");
                    }
                });
			}

		}.execute((Void)null);
	}
	
	private void logout() {
        mMsgBox.showWait();
		new AsyncTask<Void, Integer ,Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {

				return mWeb.fLogout();
			}

			@Override
			protected void onPostExecute(Integer result) {
                PrefUtil.getInstance(AccountSettingActivity.this).clear();

                WowTalkVoipIF.getInstance(AccountSettingActivity.this).fStopWowTalkService();

                // If the network is not available, the MsgBox will show the toast,
                // so we should dismiss it before finish the activity.
                mMsgBox.dismissWait();
                mMsgBox.dismissToast();

                if (StartActivity.isInstanciated()) {
                    StartActivity.instance().finish();
                }
                Intent intent = new Intent(AccountSettingActivity.this, LoginActivity.class);
                startActivity(intent);

                finish();
			}
		}.execute((Void)null);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.btn_logout:
//            if(mPrefUtil.getMyUsernameChangedState()
//                    && mPrefUtil.getMyPasswordChangedState()) {
//                logout();
//            } else {
//                mMsgBox.show(null, getString(R.string.settings_account_logout_without_set_id_pwd));
//            }
			
			Builder builder = new AlertDialog.Builder(AccountSettingActivity.this);
			builder.setTitle("提示");
			builder.setMessage("你确定要退出吗?");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					
		            if(mPrefUtil.getMyUsernameChangedState()
                  && mPrefUtil.getMyPasswordChangedState()) {
		            	logout();
		            } else {
		            	mMsgBox.show(null, getString(R.string.settings_account_logout_without_set_id_pwd));
		            }
//					Toast.makeText(AccountSettingActivity.this, "我被点击了", Toast.LENGTH_SHORT).show();
					
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					
				}
			});
			
			builder.create().show();
			
			break;
		case R.id.layout_id:
			Intent idIntent = new Intent(AccountSettingActivity.this, SettingUsernameActivity.class);
            idIntent.putExtra(SettingUsernameActivity.EXTRA_WOWID,
                    mPrefUtil.getMyUsername());
            idIntent.putExtra(SettingUsernameActivity.EXTRA_READONLY,
                    mPrefUtil.getMyUsernameChangedState());
			startActivityForResult(idIntent, REQ_INPUT_ID);
			break;
		case R.id.layout_password:
			Intent passwordIntent = new Intent(AccountSettingActivity.this, SettingPasswordActivity.class);
			startActivityForResult(passwordIntent, REQ_INPUT_PASSWORD);
			break;
		case R.id.layout_phone:
		    String phoneNumber = txtPhonenumber.getText().toString();
		    // judge whether the password is empty
		    if (!mPrefUtil.getMyPasswordChangedState()) {
		        mMsgBox.toast(R.string.accountsetting_bindphone_email_set_pwd_first);
		    } else if (!TextUtils.isEmpty(phoneNumber) && !getResources().getString(R.string.not_binded).equals(phoneNumber)) {
		        Intent bindPhoneIntent = new Intent(AccountSettingActivity.this, UnBindActivity.class);
		        bindPhoneIntent.putExtra(UnBindActivity.UNBIND_PHONE_EMAIL_VALUE, phoneNumber);
		        bindPhoneIntent.putExtra(UnBindActivity.UNBIND_TYPE, UnBindActivity.UNBIND_TYPE_PHONE);
                startActivity(bindPhoneIntent);
            } else {
                Intent bindPhoneIntent = new Intent(AccountSettingActivity.this, BindPhoneActivity.class);
                startActivity(bindPhoneIntent);
            }
			break;
		case R.id.layout_email:
		    String email = txtEmail.getText().toString();
		    if (!mPrefUtil.getMyPasswordChangedState()) {
		        mMsgBox.toast(R.string.accountsetting_bindphone_email_set_pwd_first);
		    } else if (!TextUtils.isEmpty(email) && !getResources().getString(R.string.not_binded).equals(email)) {
		        Intent bindEmailIntent = new Intent(AccountSettingActivity.this, UnBindActivity.class);
		        bindEmailIntent.putExtra(UnBindActivity.UNBIND_PHONE_EMAIL_VALUE, email);
		        bindEmailIntent.putExtra(UnBindActivity.UNBIND_TYPE, UnBindActivity.UNBIND_TYPE_EMAIL);
                startActivity(bindEmailIntent);
            } else {
                Intent bindEmailIntent = new Intent(AccountSettingActivity.this, BindEmailActivity.class);
                startActivity(bindEmailIntent);
            }
			break;
		default:
			mMsgBox.toast(R.string.not_implemented);
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_setting);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mWeb = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
		instance = this;
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        fetchData();
        queryBindings();

    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
