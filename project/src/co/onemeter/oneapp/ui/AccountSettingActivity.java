package co.onemeter.oneapp.ui;

import java.util.List;
import java.util.Map;

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
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

public class AccountSettingActivity extends Activity implements OnClickListener{
    private static final String TAG = "AccountSettingActivity";
	public static final int REQ_INPUT_ID = 11;
	public static final int REQ_INPUT_PASSWORD = 12;

	private ImageButton btnTitleBack;
	
	private TextView txtOneMeterID;
	private TextView txtPwdSetting;
	private TextView txtPhonenumber;
	private TextView txtEmail;
	private TextView txtLogout;
	private TextView textView_setting_back;
	
	private LinearLayout mUsername;
	private LinearLayout mPassword;
	private LinearLayout mBindPhone;
	private LinearLayout mBindEmail;
	private String[] binds = new String[2];
	private static AccountSettingActivity instance;

    WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
	private MessageBox mMsgBox;
	
	private String bindEmail = null;//是否绑定邮箱
	private static final int BIND_EMAIL_REQUEST_CODE = 1;//绑定邮箱页面的请求码
	private static final int FIX_BIND_EMAIL_REQUEST_CODE = 2;//修改绑定邮箱页面的请求码
	
	public static final AccountSettingActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtLogout = (TextView) findViewById(R.id.textView_logout);
		txtOneMeterID = (TextView) findViewById(R.id.onemeter_id_text);
		txtPwdSetting = (TextView) findViewById(R.id.pwd_text);
		txtPhonenumber = (TextView) findViewById(R.id.phonenumber_text);
		txtEmail = (TextView) findViewById(R.id.email_text);
		textView_setting_back = (TextView) findViewById(R.id.textView_setting_back);
		mUsername = (LinearLayout) findViewById(R.id.layout_id);
		mPassword = (LinearLayout) findViewById(R.id.layout_password);
		mBindPhone = (LinearLayout) findViewById(R.id.layout_bind_phone);
		mBindEmail = (LinearLayout) findViewById(R.id.layout_bind_email);
//		txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
//		txtPwdSetting.setTextColor(getResources().getColor(R.color.orange));
		
		btnTitleBack.setOnClickListener(this);
		textView_setting_back.setOnClickListener(this); 
		txtLogout.setOnClickListener(this);
//		mUsername.setOnClickListener(this);
		mPassword.setOnClickListener(this);
		mBindPhone.setOnClickListener(this);
		mBindEmail.setOnClickListener(this);
	}
	
	private void fetchData() {

//		if (mPrefUtil.getMyPasswordChangedState()) {
//            txtPwdSetting.setText(getResources().getString(R.string.settings_account_setted));
//            txtPwdSetting.setTextColor(getResources().getColor(R.color.text_gray1));
//		} else {
//            txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
//            txtPwdSetting.setTextColor(getResources().getColor(R.color.orange));
//		}

        if (mPrefUtil.getMyUsernameChangedState()) {
        	txtOneMeterID.setText(mPrefUtil.getMyUsername());
        } else {
        	txtOneMeterID.setText(getResources().getString(R.string.settings_account_not_set));
        }
	}
	
//	private void queryBindings() {
//        mBindPhone.setEnabled(false);
//        mBindEmail.setEnabled(false);
//        final long startTime = System.currentTimeMillis();
//		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {
//
//			@Override
//			protected Integer doInBackground(Void... params) {
//				return WowTalkWebServerIF.getInstance(AccountSettingActivity.this).fGetBindedStuff(binds);
//			}
//
//			@Override
//			protected void onPostExecute(Integer result) {
//				if (result == ErrorCode.OK) {
//					if (binds[0] != null && !binds[0].equals("")) {
//						txtEmail.setText(binds[0]);
//					} else {
//						txtEmail.setText(getResources().getString(R.string.not_binded));
//					}
//					if (binds[1] != null && !binds[1].equals("")) {
//						txtPhonenumber.setText(binds[1]);
//					} else {
//						txtPhonenumber.setText(getResources().getString(R.string.not_binded));
//					}
//				}
//				new Handler().post(new Runnable() {
//					@Override
//					public void run() {
//						mBindPhone.setEnabled(true);
//						mBindEmail.setEnabled(true); 
//						long endTime = System.currentTimeMillis();
//						Log.d(TAG, ", onResume, the time of invoking queryBinds() is " + (endTime - startTime) + " milliseconds.");
//					}
//				});
//			}
//
//		});
//	}
	
	private void logout() {
        mMsgBox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

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
				//帐号登出前，清除消息通知
				IncomeMessageIntentReceiver.closeNoticeMessage();
				
				Intent intent = new Intent(AccountSettingActivity.this, LoginActivity.class);
				startActivity(intent);

				finish();
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
		case R.id.textView_setting_back:
			finish();
			break;
		case R.id.textView_logout:
			
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
				}
			});
			builder.setNegativeButton("取消", null);
			
			builder.create().show();
			
			break;
		case R.id.layout_id:
//			Intent idIntent = new Intent(AccountSettingActivity.this, SettingUsernameActivity.class);
//            idIntent.putExtra(SettingUsernameActivity.EXTRA_WOWID,
//                    mPrefUtil.getMyUsername());
//            idIntent.putExtra(SettingUsernameActivity.EXTRA_READONLY,
//                    mPrefUtil.getMyUsernameChangedState());
//			startActivityForResult(idIntent, REQ_INPUT_ID);
			break;
		case R.id.layout_password:
			Intent passwordIntent = new Intent(AccountSettingActivity.this, SettingPasswordActivity.class);
			startActivityForResult(passwordIntent, REQ_INPUT_PASSWORD);
			break;
		case R.id.layout_bind_phone:
			Toast.makeText(AccountSettingActivity.this, "功能尚未实现..", Toast.LENGTH_LONG).show();
			break;
		case R.id.layout_bind_email:

		    if (!mPrefUtil.getMyPasswordChangedState()) {
		        mMsgBox.toast(R.string.accountsetting_bindphone_email_set_pwd_first);
		    } else  {
		    	if (bindEmail != null) {//绑定了邮箱，点击进入，修改邮箱的解绑界面
		    		Intent fixEmailIntent = new Intent(AccountSettingActivity.this, FixBindEmailAddressActivity.class);
		    		startActivityForResult(fixEmailIntent, FIX_BIND_EMAIL_REQUEST_CODE);
		    	} else {//未绑定邮箱进入绑定邮箱界面
		    		Intent bindEmailIntent = new Intent(AccountSettingActivity.this, BindEmailAddressActivity.class);
		    		startActivityForResult(bindEmailIntent, BIND_EMAIL_REQUEST_CODE);
		    	}
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
		
//		boolean isBand = getIntent().getBooleanExtra("isband", false);
		//检测用户绑定邮箱的状态
//		bindEmailStatus ();
//		if (isBand) {
//			mMsgBox.show(null, getString(R.string.bind_email_successed));
//			mMsgBox.dismissDialog();
//		}

	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        fetchData();
        bindEmailStatus ();
//        queryBindings();

    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
    
    /**
     * 检测用户绑定邮箱的状态
     */
    private void bindEmailStatus () {
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, List<Map<String, Object>>> () {

            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(AccountSettingActivity.this).fEmailBindStatus();
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
            	if (result != null) {
            	bindEmail = (String) result.get(0).get("email");
            	
            	if (!TextUtils.isEmpty(bindEmail)) {//说明绑定邮箱，显示绑定的邮箱
            		txtEmail.setText(bindEmail);
            		
            	} else {//未绑定，显示请绑定邮箱
            		txtEmail.setText(getResources().getString(R.string.settings_account_bindEmail));
            	} 
            } else {
            	Toast.makeText(AccountSettingActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
            }
            
            } 
        });
    }
    
    /**
     * 绑定邮箱成功后返回到设置界面，显示你已经绑定的邮箱
     * @author hutianfeng
     * @date 2015/3/5
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//    	super.onActivityResult(requestCode, resultCode, data);
//    	if (resultCode == RESULT_OK) {
//			switch (requestCode) {
//			case BIND_EMAIL_REQUEST_CODE://绑定邮箱成功后的处理结果
//				
//				bindEmailStatus ();
//				if (!TextUtils.isEmpty(bindEmail)) {
//					mMsgBox.show(null, getString(R.string.bind_email_successed));
//					mMsgBox.dismissDialog();
//				} 
//				break;
//				
//			case FIX_BIND_EMAIL_REQUEST_CODE://修改绑定邮箱后的处理结果
//				
//				bindEmailStatus();
//				if (!TextUtils.isEmpty(bindEmail)) {
//					mMsgBox.show(null, getString(R.string.bind_email_successed));
//					mMsgBox.dismissDialog();
//				}
//				break;
//			default:
//				break;
//			}
//    	}
//    }
    
}
