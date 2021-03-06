package co.onemeter.oneapp.ui;

import java.util.Timer;
import java.util.TimerTask;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FindPasswordEmialActivity extends Activity implements OnClickListener{
	
	private RelativeLayout layout_find_password;
	
	private ImageButton title_back;
	private TextView textView_findPassword_back;//返回
	private TextView textView_findPassword_cancel;//取消
	
	//验证账号和邮箱
	
	private RelativeLayout layout_verification_email;
	private EditText txt_bind_account;//输入账号
	private ImageButton field_clear_account;//清除账号
	private EditText txt_bind_email;//输入绑定邮箱
	private ImageButton field_clear_email;//清除邮箱
	private TextView textView_verification_email_result;//账号和邮箱的验证结果
	private Button btn_verification_email;//确认找回密码按钮
	
	
	//验证验证码
	
	private RelativeLayout layout_verification_auth_code;
	private TextView textView_show_bind_email;//显示验证后绑定的邮箱
	private EditText txt_auth_code;//邮箱收到的验证码
	private ImageButton field_clear_auth_code;//清除验证码的按钮图片
	private TextView textView_verification_authCode_result;//验证码验证结果
	private Button btn_verification_auth_code;//确认验证验证码
	private Button btn_again_receive_auth_code;//重新获取验证码
	
	
	//重置新密码
	
		private RelativeLayout  layout_reset_password;
		private EditText txt_new_password;//新密码
		private EditText txt_confirm_new_password;//确认新密码
		private ImageView field_clear_pwd;//清除密码
		private ImageView field_clear_confirm;//清除确认密码
		private ImageView imageview_show_password;//显示密码
		private ImageView imageview_hint_password;//隐藏密码
		private TextView textView_isshow_password;//显示密码文本提示
		private TextView textView_verification_newPassword;//验证新设置的密码两次设置是否一致
		private Button btn_newPassWord_ok;//提交重新设置后的密码
	
	
	InputMethodManager mInputMethodManager ;
	private MessageBox mMsgBox;
	private int time;
	private Timer mTimer;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				btn_again_receive_auth_code.setText("重新获取验证码");
				btn_again_receive_auth_code.setEnabled(true);
				btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue_10));
				mTimer.cancel();
			} else {
				btn_again_receive_auth_code.setText("重新获取验证码" + "("+ time + "s" + ")");
			}
		};
	};

    private static int FIND_EMAIL_PAGE = 1;//验证绑定手机号码阶段
    private static int AUTH_CODE_PAGE = 2;//验证验证码阶段
    //验证阶段判断标志
    private int pageFlag = FIND_EMAIL_PAGE;//默认为验证绑定手机号码阶段
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_password_email);
		mMsgBox = new MessageBox(this);
		initView();
		
		mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        
		txt_bind_account.setFocusable(true);
		txt_bind_account.setFocusableInTouchMode(true);
		txt_bind_account.requestFocus();
        
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {		
				mInputMethodManager.showSoftInput(txt_bind_account, InputMethodManager.RESULT_SHOWN);
				mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
		}, 200);
	}
	
	@Override
	protected void onPause() {
		closeSoftKeyboard();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (mTimer != null) {
			mTimer.cancel();
		}
		
		if (mInputMethodManager != null) {
			mInputMethodManager = null;
		}
		super.onDestroy();
	}
	
	
	/**
	 * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

    	  if(event.getAction() == MotionEvent.ACTION_DOWN){   
    			  closeSoftKeyboard();			  
    	  }
    	return super.onTouchEvent(event);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (pageFlag == FIND_EMAIL_PAGE) {

                closeSoftKeyboard();
                FindPasswordEmialActivity.this.finish();

            } else {

                MessageDialog dialog = new MessageDialog(FindPasswordEmialActivity.this);
                dialog.setTitle("提示");
                dialog.setRightBold(true);
                dialog.setCancelable(false);
                dialog.setMessage("别急着走哦，发送验证码可能需要一些时间，请耐心稍等，留在该页还是确定返回");
                dialog.setOnLeftClickListener("返回上页",new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();

                        pageFlag = FIND_EMAIL_PAGE;

                        if (mTimer != null) {
                            mTimer.cancel();
                        }

                        layout_verification_auth_code.setVisibility(View.GONE);
                        layout_verification_email.setVisibility(View.VISIBLE);
                        txt_auth_code.setText("");
                        txt_bind_email.setText("");
                        textView_verification_email_result.setVisibility(View.GONE);
                        textView_verification_authCode_result.setVisibility(View.GONE);

                    }
                });

                dialog.setOnRightClickListener("继续等待",null);
                dialog.show();
            }

        }

        return super.onKeyDown(keyCode, event);
    }
    
    private void closeSoftKeyboard() {
    	if (txt_bind_email.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(txt_bind_email.getWindowToken() , 0);
    		Log.i("---find_txt_bind_email");
    	}
    	
    	if (txt_bind_account.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(txt_bind_account.getWindowToken() , 0);
    		Log.i("---find_txt_bind_account");
    	}
    	
    	if (txt_auth_code.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
    		Log.i("---find_txt_auth_code");
    	}
    	
    	if (txt_new_password.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(txt_new_password.getWindowToken() , 0);
    		Log.i("---find txt_new_password");
    	}
    	
    	if (txt_confirm_new_password.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(txt_confirm_new_password.getWindowToken() , 0);
    		Log.i("---find txt_confirm_new_password");
    	}
		
    }
	
	private void initView() {
		
		layout_find_password = (RelativeLayout) findViewById(R.id.layout_find_password);
		
		//点击布局时，使得布局获得事件的焦点
		layout_find_password.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_find_password.setFocusable(true);
				layout_find_password.setFocusableInTouchMode(true);
				layout_find_password.requestFocus();
				return false;
			}
		});
		
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_findPassword_back = (TextView) findViewById(R.id.textView_findPassword_back);
		textView_findPassword_cancel = (TextView) findViewById(R.id.textView_findPassword_cancel);
		
		//验证账号和邮箱
		layout_verification_email = (RelativeLayout) findViewById(R.id.layout_verification_email);
		txt_bind_account = (EditText) findViewById(R.id.txt_bind_account);
		field_clear_account = (ImageButton) findViewById(R.id.field_clear_account);
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
    	
    	//重置新密码
    	layout_reset_password = (RelativeLayout) findViewById(R.id.layout_reset_password);
    	txt_new_password =  (EditText) findViewById(R.id.txt_new_password);
    	txt_confirm_new_password =  (EditText) findViewById(R.id.txt_confirm_new_password);
    	field_clear_pwd = (ImageView) findViewById(R.id.field_clear_pwd);
    	field_clear_confirm = (ImageView) findViewById(R.id.field_clear_confirm);
    	
    	imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
    	imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
    	textView_isshow_password = (TextView) findViewById(R.id.textView_isshow_password);
    	textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
    	btn_newPassWord_ok = (Button) findViewById(R.id.btn_newPassWord_ok);
    	field_clear_account.setOnClickListener(this);
    	field_clear_auth_code.setOnClickListener(this);
    	btn_verification_auth_code.setOnClickListener(this);
    	btn_again_receive_auth_code.setOnClickListener(this);
    	field_clear_pwd.setOnClickListener(this);
    	field_clear_confirm.setOnClickListener(this);
    	imageview_show_password.setOnClickListener(this);
    	imageview_hint_password.setOnClickListener(this);
    	btn_newPassWord_ok.setOnClickListener(this);
    	
    	
    	//对各个控件设置监听事件
    	
    	//输入账号时，清除图片按钮的控制
    	txt_bind_account.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_account.setVisibility(View.GONE);
					btn_verification_email.setTextColor(getResources().getColor(R.color.white_40));
					btn_verification_email.setEnabled(false);
				} else {
					field_clear_account.setVisibility(View.VISIBLE);
					btn_verification_email.setTextColor(getResources().getColor(R.color.white));
					btn_verification_email.setEnabled(true);
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
    	
    	txt_bind_account.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					txt_bind_account.setText("");
					if (txt_bind_account.getText().toString().length() > 0 ) {
						field_clear_account.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_account.setVisibility(View.GONE);
					mInputMethodManager.hideSoftInputFromWindow(txt_bind_account.getWindowToken() , 0);
				}
				
			}
		});
    	
    	//输入绑定邮箱时，清除图片按钮的控制
    	txt_bind_email.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_email.setVisibility(View.GONE);
					btn_verification_email.setTextColor(getResources().getColor(R.color.white_40));
					btn_verification_email.setEnabled(false);
				} else {
					field_clear_email.setVisibility(View.VISIBLE);
					btn_verification_email.setTextColor(getResources().getColor(R.color.white));
					btn_verification_email.setEnabled(true);
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
    	
    	txt_bind_email.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					txt_bind_email.setText("");
					if (txt_bind_email.getText().toString().length() > 0 ) {
						field_clear_email.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_email.setVisibility(View.GONE);
					mInputMethodManager.hideSoftInputFromWindow(txt_bind_email.getWindowToken() , 0);
				}
				
			}
		});
    	
    	
    	//输入验证码时，清除图片按钮的控制
    	txt_auth_code.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_auth_code.setVisibility(View.GONE);
					btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white_40));
					btn_verification_auth_code.setEnabled(false);
				} else {
					field_clear_auth_code.setVisibility(View.VISIBLE);
					btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white));
					btn_verification_auth_code.setEnabled(true);
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
    	
    	
    	txt_auth_code.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					txt_auth_code.setText("");
					if (txt_auth_code.getText().toString().length() > 0 ) {
						field_clear_auth_code.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_auth_code.setVisibility(View.GONE);
					mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
				}
				
			}
		});
    	
    	
    	txt_new_password.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_pwd.setVisibility(View.GONE);
					
				} else {
					field_clear_pwd.setVisibility(View.VISIBLE);
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
    	
    	
    	txt_new_password.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {

					if (txt_new_password.getText().toString().length() > 0 ) {
						field_clear_pwd.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_pwd.setVisibility(View.GONE);
					mInputMethodManager.hideSoftInputFromWindow(txt_new_password.getWindowToken() , 0);
				}
				
			}
		});
    	
    	
    	
    	txt_confirm_new_password.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_confirm.setVisibility(View.GONE);
					
				} else {
					field_clear_confirm.setVisibility(View.VISIBLE);
					
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
    	
    	
    	txt_confirm_new_password.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {

					if (txt_confirm_new_password.getText().toString().length() > 0 ) {
						field_clear_confirm.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_confirm.setVisibility(View.GONE);
					mInputMethodManager.hideSoftInputFromWindow(txt_confirm_new_password.getWindowToken() , 0);
				}
				
			}
		});
    	
		
		title_back.setOnClickListener(this);
		textView_findPassword_back.setOnClickListener(this);
		textView_findPassword_cancel.setOnClickListener(this);
		field_clear_email.setOnClickListener(this);
		btn_verification_email.setOnClickListener(this);
		
				
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		//清空账号输入框中的内容
		case R.id.field_clear_account:
			txt_bind_account.setText("");
			break;
        
		//清除绑定邮箱中文本框中的内容
		case R.id.field_clear_email:
			txt_bind_email.setText("");
            textView_verification_email_result.setVisibility(View.GONE);
			btn_verification_email.setTextColor(getResources().getColor(R.color.white_40));
			btn_verification_email.setEnabled(false);
			break;
        
		//清除验证码文本框中的内容	
		case R.id.field_clear_auth_code:
			txt_auth_code.setText("");
            textView_verification_authCode_result.setVisibility(View.GONE);
			btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white_40));
			btn_verification_auth_code.setEnabled(false);
			break;
        
		case R.id.title_back:
		case R.id.textView_findPassword_back:
            if (pageFlag == FIND_EMAIL_PAGE) {

                closeSoftKeyboard();
                FindPasswordEmialActivity.this.finish();

            } else {

                MessageDialog dialog = new MessageDialog(FindPasswordEmialActivity.this);
                dialog.setTitle("提示");
                dialog.setRightBold(true);
                dialog.setCancelable(false);
                dialog.setMessage("别急着走哦，发送验证码可能需要一些时间，请耐心稍等，留在该页还是确定返回");
                dialog.setOnLeftClickListener("返回上页",new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();

                        pageFlag = FIND_EMAIL_PAGE;

                        if (mTimer != null) {
                            mTimer.cancel();
                        }

                        layout_verification_auth_code.setVisibility(View.GONE);
                        layout_verification_email.setVisibility(View.VISIBLE);
                        txt_auth_code.setText("");
                        txt_bind_email.setText("");
                        textView_verification_email_result.setVisibility(View.GONE);
                        textView_verification_authCode_result.setVisibility(View.GONE);

                    }
                });

                dialog.setOnRightClickListener("继续等待",null);
                dialog.show();
            }
			break;
        
		case R.id.textView_findPassword_cancel:
			break;
			
		case R.id.btn_verification_email://验证邮箱
			
			sendCodeRetrievePassword(txt_bind_account.getText().toString(),txt_bind_email.getText().toString());
			//60秒内不可点击  重新获取验证码
			stopGetAccessCode();
			break;
			
		//验证验证码	
		case R.id.btn_verification_auth_code:
			checkCodeRetrievePassword(txt_bind_account.getText().toString(),txt_auth_code.getText().toString());			
			break;
						
		//重新获得验证码	
		case R.id.btn_again_receive_auth_code:
			sendCodeRetrievePassword(txt_bind_account.getText().toString(),txt_bind_email.getText().toString());
			//60秒内不可点击  重新获取验证码
			stopGetAccessCode();			
		break;
		
		case R.id.field_clear_pwd:
			txt_new_password.setText("");
            textView_verification_newPassword.setVisibility(View.GONE);
			break;
			
		case R.id.field_clear_confirm:
			txt_confirm_new_password.setText("");
            textView_verification_newPassword.setVisibility(View.GONE);
			break;
						
		//显示密码
		case R.id.imageview_show_password:
			imageview_show_password.setVisibility(View.GONE);
			imageview_hint_password.setVisibility(View.VISIBLE);
			txt_new_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			txt_confirm_new_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			textView_isshow_password.setText("隐藏密码");
			break;
						
		//隐藏密码	
		case R.id.imageview_hint_password:
			imageview_show_password.setVisibility(View.VISIBLE);
			imageview_hint_password.setVisibility(View.GONE);
			txt_new_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			txt_confirm_new_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			textView_isshow_password.setText("显示密码");
			break;
						
		//重置后的密码
		case R.id.btn_newPassWord_ok:

            if (txt_new_password.getText().toString().length() == 0) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_password_empty));
            } else if (txt_new_password.getText().toString().length() < 6) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_password_less));
                break;
            } else if (txt_new_password.getText().toString().length() > 20) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_password_more));
                break;
            } else if (txt_confirm_new_password.getText().toString().length() == 0) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_empty));
                break;
            } else if (txt_confirm_new_password.getText().toString().length() < 6) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_less));
                break;
            } else if (txt_confirm_new_password.getText().toString().length() > 20) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_more));
                break;
            } else if (!Utils.verifyWowTalkPwd(txt_new_password.getText().toString())) {//密码格式错误
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.settings_account_passwd_format_error));
                break;
            } else if (!Utils.verifyWowTalkPwd(txt_confirm_new_password.getText().toString())) {//确认密码格式错误
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.settings_account_pwdConfrim_format_error));
                break;
            } else if (!txt_new_password.getText().toString().equals(txt_confirm_new_password.getText().toString())) {
                textView_verification_newPassword.setVisibility(View.VISIBLE);
                textView_verification_newPassword.setText(getResources().getString(R.string.register_pwd_must_fit));
                break;
            } else {
				resetPassword(txt_bind_account.getText().toString(),txt_new_password.getText().toString());
				break;
			}
			

		default:
			break; 
		}
		
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
		btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue_11));
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
	 * 验证绑定邮箱
	 * @param wowtalk_id
	 * @param emailAddress
	 * @author hutianfeng
	 * @date 2015/3/10
	 */
	private void sendCodeRetrievePassword(final String wowtalk_id,final String emailAddress) {
		mMsgBox.showWait();
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FindPasswordEmialActivity.this).fSendCodeRetrievePassword(wowtalk_id, emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0

                        pageFlag = AUTH_CODE_PAGE;
                    	layout_verification_auth_code.setVisibility(View.VISIBLE);
        				layout_verification_email.setVisibility(View.GONE);
        				textView_show_bind_email.setVisibility(View.VISIBLE);
        				textView_show_bind_email.setText("你输入的邮箱"+txt_bind_email.getText().toString());
                    	
                        break;
                        
                    case ErrorCode.EMAIL_ADDRESS_VERIFICATION_CODE_ERROR://18
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("你输入的邮箱格式有误");
                    	break;
                    	
                    case ErrorCode.EMAIL_VERIFICATION_CODE_ERROR://21
                    	textView_verification_email_result.setVisibility(View.VISIBLE);
                    	textView_verification_email_result.setText("账号和绑定邮箱不匹配");
                    	break;
                    	
                    case ErrorCode.USER_NOT_EXISTS://-99
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
                    	
                    case ErrorCode.ACCESS_CODE_ERROR_OVER://24:验证码一天最多只能验证5次
                    	MessageDialog dialog = new MessageDialog(FindPasswordEmialActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setCancelable(false);
                        dialog.setMessage("今天的邮箱验证次数已用完"+"\n"+"请明天再试。");
                        dialog.show();
                        break;
                    	
                    default:
                        mMsgBox.show(null, "你输入的账号或邮箱有误，请重新输入");
//                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
	}
	
	/**
	 * 验证绑定邮箱收到的验证码
	 * @param wowtalk_id
	 * @param access_code
	 * @author hutainfeng
	 * @date 2015/3/11
	 */
	private void checkCodeRetrievePassword (final String wowtalk_id,final String access_code) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FindPasswordEmialActivity.this).fCheckCodeRetrievePassword(wowtalk_id, access_code);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0        
                    	layout_verification_auth_code.setVisibility(View.GONE);
                    	layout_reset_password.setVisibility(View.VISIBLE);
                    	
                        break;
                        
                    case ErrorCode.USER_NOT_EXISTS://-99
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("你输入的账号不存在");
                    	break;
                    	
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_FALSE://1108
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("验证码不正确");
                    	break;
                    	
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_OUT_TIME://1109
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("验证码已过时");
                    	break;

                    case ErrorCode.ACCESS_CODE_ERROR://22:无效的验证码，验证码有有效期，目前是一天的有效期
                    	textView_verification_authCode_result.setVisibility(View.VISIBLE);
                    	textView_verification_authCode_result.setText(getString(R.string.access_code_error));
                        break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR_OVER://24:验证码一天最多只能验证5次
                    	MessageDialog dialog = new MessageDialog(FindPasswordEmialActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setCancelable(false);
                        dialog.setMessage("今天邮箱验证次数已用完，请明天再试。");                      
                        dialog.show();
                        break;
                        
                    default:
                        mMsgBox.show(null, "验证不通过");
//                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
	}
	
	/**
	 * 重置密码
	 * @param wowtalk_id
	 * @param password
	 * @date 2015/3/11
	 */
	private void resetPassword(final String wowtalk_id,final String password) {
    	mMsgBox.showWait();
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FindPasswordEmialActivity.this).newRetrievePassword(wowtalk_id,password);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0
                    	mMsgBox.showWaitImageSuccess("新密码设置成功");
                    	
                    	new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									Thread.sleep(3000);

                                    mMsgBox.dismissWait();
                                    Intent intent = new Intent (FindPasswordEmialActivity.this, LoginActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(LoginActivity.EXTRA_USERNAME, wowtalk_id);
                                    bundle.putString(LoginActivity.EXTRA_PASSWORD, password);
                                    intent.putExtras(bundle);
                                    startActivity(intent);

								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								finish();
								
							}
						}).start();
                    	
                        break;
                        
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_FALSE://1108
                        textView_verification_newPassword.setVisibility(View.VISIBLE);
                        textView_verification_newPassword.setText("未验证验证码");
                    	break;
                    	
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_OUT_TIME://1109
                        textView_verification_newPassword.setVisibility(View.VISIBLE);
                        textView_verification_newPassword.setText("验证码已过时");
                    	break;
                    	
                    case ErrorCode.USER_NOT_EXISTS://-99
                        textView_verification_newPassword.setVisibility(View.VISIBLE);
                        textView_verification_newPassword.setText("你输入的账号不存在");
                    	break;
                    	
                    	
                    default://重置密码失败
                        mMsgBox.showWaitImageWorng("密码重置失败");
//                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
    }
}
























