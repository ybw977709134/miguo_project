package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

public class RegisterActivity extends Activity implements OnClickListener{
	
	private static final int MSG_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_ALREADY_EXIST = 102;
    private static final int MSG_ERROR_UNKNOWN = 104;    
	
    
    private RelativeLayout layout_register;
	private EditText edtAccount;
	private EditText edtPwd;
	private EditText edtPwdConfirm;

	private ImageButton title_back;
	private ImageButton field_clear_account;
	private ImageButton field_clear_pwd;
	private ImageButton field_clear_confirm;
	
	private TextView textView_find_password_back;//返回
	private LinearLayout layout_register_user_type;
	private TextView textView_register_user_type;
	private ImageView imageview_show_password;//显示密码
	private ImageView imageview_hint_password;//隐藏密码
	private TextView textView_isshow_password;//显示密码文本提示
	private TextView textView_verification_newPassword;//验证注册的密码
	private Button btnCreate;

    private MessageBox mMsgBox;
    private int userType;
    private String cellPhone;
    
    private InputMethodManager mInputMethodManager;
	
	private Buddy buddy = new Buddy();

    /*注册成功后自动登录到应用的首页*/
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_REGISTER_SUCCESS:
			{
				final String[] args = (String[])msg.obj;
				mMsgBox.showWaitImageSuccess("注册成功");

				new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							Thread.sleep(3000);

                            Intent intent = new Intent (RegisterActivity.this, LoginActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString(LoginActivity.EXTRA_USERNAME, args[0]);
                            bundle.putString(LoginActivity.EXTRA_PASSWORD, args[1]);
                            intent.putExtras(bundle);
							startActivity(intent);

//							.putExtra(LoginActivity.EXTRA_USERNAME, args[0])
//							.putExtra(LoginActivity.EXTRA_PASSWORD, args[1]));
//							finish();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					}
				}).start();
				
			}
			break;
			
			case MSG_USER_ALREADY_EXIST:
				textView_verification_newPassword.setVisibility(View.VISIBLE);
	        	textView_verification_newPassword.setText(getResources().getString(R.string.reg_failed_username_was_taken));
			break;
			
			default:
                textView_verification_newPassword.setVisibility(View.VISIBLE);
	        	textView_verification_newPassword.setText(getResources().getString(R.string.login_net_error));
                break;
			}
		}
	};

    /**
     * 初始化各个控件
     */
	private void initView() {

        cellPhone = getIntent().getStringExtra("cellPhone");
		layout_register = (RelativeLayout) findViewById(R.id.layout_register);
		
		layout_register.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_register.setFocusable(true);
				layout_register.setFocusableInTouchMode(true);
				layout_register.requestFocus();
				field_clear_account.setVisibility(View.GONE);
				field_clear_pwd.setVisibility(View.GONE);
				field_clear_confirm.setVisibility(View.GONE);
				return false;
			}
		});
		
		title_back = (ImageButton) findViewById(R.id.title_back);
		
		field_clear_account = (ImageButton) findViewById(R.id.field_clear_account);
		field_clear_pwd = (ImageButton) findViewById(R.id.field_clear_pwd);
		field_clear_confirm = (ImageButton) findViewById(R.id.field_clear_confirm);
		
		
		textView_find_password_back =  (TextView) findViewById(R.id.textView_find_password_back);
		layout_register_user_type = (LinearLayout) findViewById(R.id.layout_register_user_type);
		
		textView_register_user_type = (TextView) findViewById(R.id.textView_register_user_type);
		imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
    	imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
    	textView_isshow_password = (TextView) findViewById(R.id.textView_isshow_password);
    	textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
    	
		btnCreate = (Button) findViewById(R.id.create_button);
		
		edtAccount = (EditText) findViewById(R.id.account_edit);
		
		//默认弹起键盘 用户账号输入界面
		
		edtAccount.setFocusable(true);
		edtAccount.setFocusableInTouchMode(true); 
		edtAccount.requestFocus();
        
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mInputMethodManager.showSoftInput(edtAccount, InputMethodManager.RESULT_SHOWN);
				mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); 
			}
		}, 200);

		
		edtPwd = (EditText) findViewById(R.id.pwd_edit);
		edtPwdConfirm = (EditText) findViewById(R.id.pwd_confirm_edit);
		
		btnCreate.setOnClickListener(this);
		
		//每次确认密码框重新获得焦点时，清空密码框
		edtAccount.setOnFocusChangeListener(new OnFocusChangeListener() {
					
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					edtAccount.setText("");
					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtAccount.getText().toString().length() > 0) {
						field_clear_account.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_account.setVisibility(View.GONE);
				}
			}
		});
		
		//每次密码框重新获得焦点时，清空密码框
		edtPwd.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					edtPwd.setText("");
					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtPwd.getText().toString().length() > 0) {
						field_clear_pwd.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_pwd.setVisibility(View.GONE);
				}
			}
		});
		
		//每次确认密码框重新获得焦点时，清空密码框
		edtPwdConfirm.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
//					edtPwdConfirm.setText("");
					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtPwdConfirm.getText().toString().length() > 0) {
						field_clear_confirm.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_confirm.setVisibility(View.GONE);
				}
			}
		});
		
		//账号的清除控制
		edtAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_account.setVisibility(View.GONE);
				} else {
					field_clear_account.setVisibility(View.VISIBLE);
					edtAccount.setTextColor(getResources().getColor(R.color.black));
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
		
		//密码的清除控制
		edtPwd.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_pwd.setVisibility(View.GONE);
				} else {
					field_clear_pwd.setVisibility(View.VISIBLE);
					edtPwd.setTextColor(getResources().getColor(R.color.black));
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

		
		//确认密码的清除按钮控制
		edtPwdConfirm.addTextChangedListener(new TextWatcher() {
	
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() == 0) {
				field_clear_confirm.setVisibility(View.GONE);
		} else {
			field_clear_confirm.setVisibility(View.VISIBLE);
			edtPwdConfirm.setTextColor(getResources().getColor(R.color.black));
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
		
		title_back.setOnClickListener(this);
		field_clear_account.setOnClickListener(this);
		field_clear_pwd.setOnClickListener(this);
		field_clear_confirm.setOnClickListener(this);
		
		textView_find_password_back.setOnClickListener(this);
		layout_register_user_type.setOnClickListener(this);
		imageview_show_password.setOnClickListener(this);
    	imageview_hint_password.setOnClickListener(this);
		
	}

	private void fRegister() {
		
//		final String strUserName = edtAccount.getText().toString();
		final String strPassword = edtPwd.getText().toString();
		final String strPwdConfirm = edtPwdConfirm.getText().toString();	
        
        /**
         * 帐号验证
         */
//        if (strUserName.length() <= 0) {//用户名不能为空
//        	textView_verification_newPassword.setVisibility(View.VISIBLE);
//        	textView_verification_newPassword.setText(getResources().getString(R.string.register_username_empty));
//        	return;
//        }
//
//        if (strUserName.length() < 2) {//用户名不能小于2个字符
//        	textView_verification_newPassword.setVisibility(View.VISIBLE);
//        	textView_verification_newPassword.setText(getResources().getString(R.string.register_username_less));
//        	return;
//        }
//
//        if (strUserName.length() > 20) {//用户名不能大于20个字符
//        	textView_verification_newPassword.setVisibility(View.VISIBLE);
//        	textView_verification_newPassword.setText(getResources().getString(R.string.register_username_more));
//        	return;
//        }
        
        /**
         * 密码验证
         */
        if (strPassword.length() <= 0) {//密码不能为空
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_password_empty));
        	return;
        }
        
        if (strPassword.length() < 6) {//密码不能小于6个字符
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_password_less));
        	return;
        }
        
        if (strPassword.length() > 20) {//密码不能大于20个字符
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_password_more));
        	return;
        }
        
        /**
         * 确认密码验证
         */
        if (strPwdConfirm.length() <= 0) {//确认密码不能为空
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_empty));
        	return;
        }
        
        if (strPwdConfirm.length() < 6) {//确认密码不能小于6个字符
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_less));
        	return;
        }
        
        if (strPwdConfirm.length() > 20) {//确认密码不能大于20个字符
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.register_pwdConfrim_more));
        	return;
        }
        
        
//        if (!Utils.verifyUsername(strUserName)) {//注册用户名的帐号错误
//            textView_verification_newPassword.setVisibility(View.VISIBLE);
//        	textView_verification_newPassword.setText(getResources().getString(R.string.setting_username_format_error));
//            return;
//        }

        if (!Utils.verifyWowTalkPwd(strPassword)) {//密码格式错误
            textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.settings_account_passwd_format_error));
            return;
        }
        
        if (!Utils.verifyWowTalkPwd(strPwdConfirm)) {//确认密码格式错误
            textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.settings_account_pwdConfrim_format_error));
            return;
        }
        
        //对账号类型的确认
        if (!(textView_register_user_type.getText().equals("老师")) && !(textView_register_user_type.getText().equals("学生"))) {
        	textView_verification_newPassword.setVisibility(View.VISIBLE);
        	textView_verification_newPassword.setText(getResources().getString(R.string.settings_account_type_select));
        	return;
        }
        

		if (fIsPasswordFit()) {
		    mMsgBox.showWait();
			new Thread(new Runnable() {

				@Override
				public void run() {
                    int result = WowTalkWebServerIF.getInstance(RegisterActivity.this)
                            .fRegister(cellPhone, strPassword, userType, buddy);

					Log.i("register, the result_code is " + result);

					Message msg = Message.obtain();
					if (result == ErrorCode.OK) {
						msg.what = MSG_REGISTER_SUCCESS;
						msg.obj = new String[]{ cellPhone, strPassword };
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


	
	/**
	 * 验证两次输入的密码是否一致
	 * @return
	 */
	private boolean fIsPasswordFit() {
		if (edtPwd.getText().toString().equals(edtPwdConfirm.getText().toString())) {
			return true;
		} else {		
				textView_verification_newPassword.setVisibility(View.VISIBLE);
	        	textView_verification_newPassword.setText(getResources().getString(R.string.register_pwd_must_fit));
			return false;
		}
	}
	
	/**
	 * 处理各个监听事件
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		//返回，撤销此次的注册
		case R.id.title_back:
		case R.id.textView_find_password_back:
			finish();
			break;	
			
		case R.id.field_clear_account:
			edtAccount.setText("");
			break;
			
		case R.id.field_clear_pwd:
			edtPwd.setText("");
			break;
			
		case R.id.field_clear_confirm:
			edtPwdConfirm.setText("");
			break;
			
		case R.id.layout_register_user_type://选择账号类型
			showPopupMenu();
			break;
			
		//显示密码
		case R.id.imageview_show_password:
			imageview_show_password.setVisibility(View.GONE);
			imageview_hint_password.setVisibility(View.VISIBLE);
			edtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			edtPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			break;
						
		//隐藏密码	
		case R.id.imageview_hint_password:
			imageview_show_password.setVisibility(View.VISIBLE);
			imageview_hint_password.setVisibility(View.GONE);
			edtPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			edtPwdConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
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
        //获得系统的服务点击屏幕的其他的地方可以收起键盘
        mInputMethodManager = (InputMethodManager) RegisterActivity.this.getSystemService(this.INPUT_METHOD_SERVICE);

        mMsgBox = new MessageBox(this);
		initView();
	}
	
	/**
	 * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

    	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
    		  
    		//退出页面关起软键盘
    	    	if (edtAccount.hasFocus()) {
    	    		mInputMethodManager.hideSoftInputFromWindow(edtAccount.getWindowToken() , 0);
    	    	} else if (edtPwd.hasFocus()) {
    	    		mInputMethodManager.hideSoftInputFromWindow(edtPwd.getWindowToken() , 0);
    	    	} else {
    	    		mInputMethodManager.hideSoftInputFromWindow(edtPwdConfirm.getWindowToken() , 0); 
    	    	}
    		  
    		  }
    	return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
    	//退出页面关起软键盘
    	if (edtAccount.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(edtAccount.getWindowToken() , 0);
    	} else if (edtPwd.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(edtPwd.getWindowToken() , 0);
    	} else {
    		mInputMethodManager.hideSoftInputFromWindow(edtPwdConfirm.getWindowToken() , 0); 
    	}
        super.onPause();
    }
    
    private void alert(String strTip){
		AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
		dialog.setTitle(getResources().getString(R.string.register_failure)).setMessage(strTip).setNegativeButton("确定",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create().show();
    }
    
    /**
     * 用于选择注册账号的类型
     */
    private void showPopupMenu() {

    	 //选择账号类型时，可自动关闭打开的软键盘
    	if (edtAccount.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(edtAccount.getWindowToken() , 0);
    	} else if (edtPwd.hasFocus()) {
    		mInputMethodManager.hideSoftInputFromWindow(edtPwd.getWindowToken() , 0);
    	} else {
    		mInputMethodManager.hideSoftInputFromWindow(edtPwdConfirm.getWindowToken() , 0); 
    	}
    	mInputMethodManager.hideSoftInputFromWindow(RegisterActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, findViewById(R.id.layout_register));
        
        textView_verification_newPassword.setVisibility(View.GONE);
        //邮箱找回
        bottomBoard.add("老师", BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomBoard.dismiss();
                //process
                userType = Buddy.ACCOUNT_TYPE_TEACHER;
                textView_register_user_type.setText("老师");
                textView_register_user_type.setTextColor(getResources().getColor(R.color.black));
                showTeacherTip();
            }
        });
        
        bottomBoard.add("学生", BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomBoard.dismiss();
                //process
                userType = Buddy.ACCOUNT_TYPE_STUDENT;
                textView_register_user_type.setText("学生");
                textView_register_user_type.setTextColor(getResources().getColor(R.color.black));
                
                
            }
        });

        //close popupMenu
        bottomBoard.addCancelBtn(getString(R.string.login_findPassWord_cancel));
        bottomBoard.show();
    }

    private void showTeacherTip(){
        MessageDialog dialog = new MessageDialog(this);
        dialog.setTitle("提示");
        dialog.setTitleColor(Color.RED);
        dialog.setMessage(R.string.register_user_prompt);
        dialog.setMessageTextSize(15);
        dialog.setIsDouleBtn(false);
        dialog.setCancelable(false);
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }
}
