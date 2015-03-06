package co.onemeter.oneapp.ui;

import java.util.Timer;
import java.util.TimerTask;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author hutianfeng
 * @date 2015/3/3
 *
 */
public class BindEmailAddressActivity extends Activity implements OnClickListener {
	private static int BIND_EMAIL_PAGE = 1;//验证绑定邮箱阶段
	private static int AUTH_CODE_PAGE = 2;//验证验证码阶段
	//验证阶段判断标志
	private int pageFlag = BIND_EMAIL_PAGE;//默认为验证绑定邮箱阶段
	
	private TextView textView_bindEmail_back;//文本返回
	private TextView textView_findPassword_cancel;//取消
	private ImageButton title_back;//箭头返回
	
	//验证邮箱
	
	private RelativeLayout layout_verification_email;
	private EditText txt_bind_email;//绑定邮箱
	private ImageButton field_clear_email;//清除绑定邮箱按钮图片
	private TextView textView_verification_email_result;//邮箱格式验证的结果
	private Button btn_verification_email;//确定验证绑定邮箱信息
	
	//验证验证码
	
	private RelativeLayout layout_verification_auth_code;
	private TextView textView_show_bind_email;//显示验证后绑定的邮箱
	private EditText txt_auth_code;//邮箱收到的验证码
	private ImageButton field_clear_auth_code;//清除验证码的按钮图片
	private TextView textView_verification_authCode_result;//验证码验证结果
	private Button btn_verification_auth_code;//确认验证验证码
	private Button btn_again_receive_auth_code;//重新获取验证码

	private MessageBox mMsgBox;
	private int time;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				btn_again_receive_auth_code.setText("重新获取验证码");
				btn_again_receive_auth_code.setEnabled(true);
				btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue_light));
				mTimer.cancel();
			} else {
				btn_again_receive_auth_code.setText("重新获取验证码" + "("+ time + "s" + ")");
			}
		};
	};
	

	private Timer mTimer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_email_adress);
        initView();
        mMsgBox = new MessageBox(this);
        
        
    }
    
    //如何在绑定邮箱的过程中，任何时候按物理的返回键都绑定不成功，即使前面绑定了，还需要解绑
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		unBindEmailAddress();//返回解绑
    	}
    	return super.onKeyDown(keyCode, event);
    } 
    
    
    @Override
    protected void onDestroy() {
    	if (mTimer != null) {
    		mTimer.cancel();
    	}
    	super.onDestroy();
    }
    
    /**
     * 初始化控件
     */
    private void initView() {
    	
    	//页标题
    	textView_bindEmail_back = (TextView) findViewById(R.id.textView_bindEmail_back);
    	textView_findPassword_cancel = (TextView) findViewById(R.id.textView_findPassword_cancel);
    	title_back = (ImageButton) findViewById(R.id.title_back);
    	
    	//验证邮箱
    	layout_verification_email = (RelativeLayout) findViewById(R.id.layout_verification_email);
    	txt_bind_email = (EditText) findViewById(R.id.txt_bind_email);
    	field_clear_email = (ImageButton) findViewById(R.id.field_clear_email);
    	textView_verification_email_result = (TextView) findViewById(R.id.textView_verification_email_result);
    	btn_verification_email = (Button) findViewById(R.id.btn_verification_email);
    	
    	//验证验证码
    	layout_verification_auth_code = (RelativeLayout) findViewById(R.id.layout_verification_auth_code);
    	textView_show_bind_email = (TextView) findViewById(R.id.textView_show_bind_email);
    	txt_auth_code = (EditText) findViewById(R.id.txt_auth_code);
    	field_clear_auth_code = (ImageButton) findViewById(R.id.field_clear_auth_code);
    	textView_verification_authCode_result = (TextView) findViewById(R.id.textView_verification_authCode_result);
    	btn_verification_auth_code = (Button) findViewById(R.id.btn_verification_auth_code);
    	btn_again_receive_auth_code = (Button) findViewById(R.id.btn_again_receive_auth_code);
    	
    	//对各个控件设置监听事件
    	
    	//输入绑定邮箱时，清除图片按钮的控制
    	txt_bind_email.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_email.setVisibility(View.GONE);
				} else {
					field_clear_email.setVisibility(View.VISIBLE);
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
    	
    	//输入验证码时，清除图片按钮的控制
    	txt_auth_code.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_auth_code.setVisibility(View.GONE);
				} else {
					field_clear_auth_code.setVisibility(View.VISIBLE);
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
    	
    	textView_bindEmail_back.setOnClickListener(this);
    	textView_findPassword_cancel.setOnClickListener(this);
    	title_back.setOnClickListener(this);
    	field_clear_email.setOnClickListener(this);
    	btn_verification_email.setOnClickListener(this);
    	field_clear_auth_code.setOnClickListener(this);
    	btn_verification_auth_code.setOnClickListener(this);
    	btn_again_receive_auth_code.setOnClickListener(this);
    	
    }
    
    /**
     * 处理各个控件的监听事件
     */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		//清除绑定邮箱中文本框中的内容
		case R.id.field_clear_email:
			txt_bind_email.setText("");
			textView_verification_email_result.setVisibility(View.GONE);
			break;
		//清除验证码文本框中的内容	
		case R.id.field_clear_auth_code:
			txt_auth_code.setText("");
			textView_verification_authCode_result.setVisibility(View.GONE);
						
			break;
		//返回
		case R.id.textView_bindEmail_back:
		case R.id.title_back:
			if (pageFlag == BIND_EMAIL_PAGE) {
//				Intent intent = new Intent();
//                setResult(RESULT_OK, intent);
              //返回时，没验证或验证不通过的都要解绑
    			unBindEmailAddress();
				finish();
			} else if (pageFlag == AUTH_CODE_PAGE) {
				pageFlag = BIND_EMAIL_PAGE;
				layout_verification_auth_code.setVisibility(View.GONE);
				layout_verification_email.setVisibility(View.VISIBLE);
				txt_auth_code.setText("");
				txt_bind_email.setText("");
				textView_verification_email_result.setVisibility(View.GONE);
				textView_verification_authCode_result.setVisibility(View.GONE);
			} 
			break;
			
		//取消
		case R.id.textView_findPassword_cancel:
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("提示");
			builder.setMessage("你确定要取消绑定邮箱吗？");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					//取消绑定邮箱，解绑
					unBindEmailAddress();
					Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
					finish();
				}
			});
			
			builder.setNegativeButton("取消", null);
			builder.create().show();
			
			break;
			
		//验证绑定邮箱	
		case R.id.btn_verification_email:
			
			bindEmailAddress(txt_bind_email.getText().toString());
			//60秒内不可点击  重新获取验证码
			stopGetAccessCode();
//			mTimerTask.cancel();
			break;
			
		//验证验证码	
		case R.id.btn_verification_auth_code:
			//成功时
			verifyBindEmailAddress(txt_auth_code.getText().toString(),txt_bind_email.getText().toString());
			break;
			
		//重新获得验证码	
		case R.id.btn_again_receive_auth_code:
			
			bindEmailAddress(txt_bind_email.getText().toString());
			//60秒内不可点击  重新获取验证码
			stopGetAccessCode();
//			mTimerTask.cancel();
			break;
			
		default:
			break;
		}
		
	}
	
	/**
	 * 
	 * 绑定邮箱地址
	 * @param emailAddress
	 * @author hutianfeng
	 * @date 2015/3/4
	 */
	private void bindEmailAddress(final String emailAddress) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(BindEmailAddressActivity.this).fBindEmialAddress(emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0
                    	pageFlag = BIND_EMAIL_PAGE;
        				layout_verification_auth_code.setVisibility(View.VISIBLE);
        				layout_verification_email.setVisibility(View.GONE);
        				textView_show_bind_email.setVisibility(View.VISIBLE);
        				textView_show_bind_email.setText("你输入的邮箱"+txt_bind_email.getText().toString());
                        break;
                    case ErrorCode.EMAIL_ADDRESS_VERIFICATION_CODE_ERROR://18:邮箱格式错误
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText(getString(R.string.bind_email_format_error_msg));
                    	break;
                    case ErrorCode.AUTH://2:认证失败，错误的用户名或密码
//                        mMsgBox.show(null, getString(R.string.bind_email_pwd_err));
                        textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText(getString(R.string.bind_email_pwd_err));
                        break;
                    case ErrorCode.EMAIL_USED_BY_OTHERS://28：该邮箱已被其他用户绑定
//                        changeBindStyle();
//                        mMsgBox.show(null, getString(R.string.settings_account_email_used_by_others));
                        textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText(getString(R.string.settings_account_email_used_by_others));
                        break;
                    case ErrorCode.ERR_OPERATION_DENIED://37:该用户已绑定其它邮箱
//                    	mMsgBox.show(null, getString(R.string.settings_account_email_used));
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText(getString(R.string.settings_account_email_used));
                    	break;
                    default://邮箱绑定失败
                        mMsgBox.show(null, getString(R.string.bind_email_failed));
                        break;
                }
            }
        });
		
	}
	
	/**
	 * 控制重新获取验证的按钮状态
	 * @author hutainfeng
	 * @date 2015/3/4
	 * 
	 */
	private void stopGetAccessCode() {
		//60秒内不可点击  重新获取验证码
		btn_again_receive_auth_code.setEnabled(false);
		btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue));
		time = 60;
		
		mTimer = new Timer();
		
		mTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				mHandler.sendEmptyMessage(time--);
			}
		}, 0, 1000);
		
	}
	
	/**
	 * 验证绑定邮箱收到的验证码
	 * @param access_code
	 * @param emailAddress
	 * @author hutainfeng
	 * @date 2015/3/4
	 */
	private void verifyBindEmailAddress(final String access_code,final String emailAddress) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(BindEmailAddressActivity.this).fVerifyEmailAddress(access_code,emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0        	
                    	Intent intent = new Intent();
                        setResult(RESULT_OK, intent);//发送结果码
                        finish();
                        break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR://22:无效的验证码，验证码有有效期，目前是一天的有效期
                    	textView_verification_authCode_result.setVisibility(View.VISIBLE);
                    	textView_verification_authCode_result.setText(getString(R.string.access_code_error));
                        break;
                        
                    default:
                        mMsgBox.show(null, getString(R.string.bind_email_failed));
                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
	}
	
	/**
	 * 解除绑定邮箱
	 * @author hutianfeng
	 * @date 2015/3/5
	 */
	private void unBindEmailAddress() {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(BindEmailAddressActivity.this).fUnBindEmailAddress();
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0        	
                    	Toast.makeText(BindEmailAddressActivity.this, "邮箱未绑定", Toast.LENGTH_SHORT).show();
                        break;
                        
                    default:
                        mMsgBox.show(null, "请检查网络");
                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
	}
	
	
}























