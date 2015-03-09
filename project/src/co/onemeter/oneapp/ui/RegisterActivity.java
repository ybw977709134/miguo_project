package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

public class RegisterActivity extends Activity implements OnClickListener{
	
	private static final int MSG_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_ALREADY_EXIST = 102;
    private static final int MSG_ERROR_UNKNOWN = 104;    
	
	private EditText edtAccount;
	private EditText edtPwd;
	private EditText edtPwdConfirm;
	
	private View btnBack;
	
	private LinearLayout layout_register_user_type;
	private TextView textView_register_user_type;
	private ImageView imageview_show_password;//显示密码
	private ImageView imageview_hint_password;//隐藏密码
	private TextView textView_isshow_password;//显示密码文本提示
	private TextView textView_verification_newPassword;//验证注册的密码
	private Button btnCreate;

    private MessageBox mMsgBox;
    private int userType;
    
    private InputMethodManager mInputMethodManager;
	
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
//				AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
//				dialog.setTitle(null).setMessage(R.string.reg_failed_username_was_taken);
//				dialog.setNegativeButton("OK", 
//						new DialogInterface.OnClickListener() {
//
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//					}
//				}).create().show();
				alert(getResources().getString(R.string.reg_failed_username_was_taken));
			}
			break;
			default:
//                mMsgBox.show(null, getString(R.string.register_failure));
                alert(getResources().getString(R.string.login_net_error));
                break;
			}
		}
	};
	
	private void initView() {
		btnBack = findViewById(R.id.title_back);
		
		layout_register_user_type = (LinearLayout) findViewById(R.id.layout_register_user_type);
		
		textView_register_user_type = (TextView) findViewById(R.id.textView_register_user_type);
		imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
    	imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
    	textView_isshow_password = (TextView) findViewById(R.id.textView_isshow_password);
    	textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
    	
		btnCreate = (Button) findViewById(R.id.create_button);
		
		edtAccount = (EditText) findViewById(R.id.account_edit);
		edtPwd = (EditText) findViewById(R.id.pwd_edit);
		edtPwdConfirm = (EditText) findViewById(R.id.pwd_confirm_edit);
		
		btnBack.setOnClickListener(this);
		btnCreate.setOnClickListener(this);
		
		//每次密码框重新获得焦点时，清空密码框
		edtPwd.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					edtPwd.setText("");
				}
				
			}
		});
		
		//每次确认密码框重新获得焦点时，清空密码框
		edtPwdConfirm.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					edtPwdConfirm.setText("");
				}
				
			}
		});
		
		
		layout_register_user_type.setOnClickListener(this);
		imageview_show_password.setOnClickListener(this);
    	imageview_hint_password.setOnClickListener(this);
		
	}

	private void fRegister() {
		
		final String strUserName = edtAccount.getText().toString();
		final String strPassword = edtPwd.getText().toString();
		final String strPwdConfirm = edtPwdConfirm.getText().toString();	
        
        /**
         * 帐号验证
         */
        if (strUserName.length() <= 0) {//用户名不能为空
        	alert(getResources().getString(R.string.register_username_empty));
        	return;
        }
        
        if (strUserName.length() < 2) {//用户名不能小于2个字符
        	alert(getResources().getString(R.string.register_username_less));
        	return;
        }
        
        if (strUserName.length() > 20) {//用户名不能大于20个字符
        	alert(getResources().getString(R.string.register_username_more));
        	return;
        }
        
        /**
         * 密码验证
         */
        if (strPassword.length() <= 0) {//密码不能为空
        	alert(getResources().getString(R.string.register_password_empty));
        	return;
        }
        
        if (strPassword.length() < 6) {//密码不能小于6个字符
        	alert(getResources().getString(R.string.register_password_less));
        	return;
        }
        
        if (strPassword.length() > 20) {//密码不能大于20个字符
        	alert(getResources().getString(R.string.register_password_more));
        	return;
        }
        
        /**
         * 确认密码验证
         */
        if (strPwdConfirm.length() <= 0) {//确认密码不能为空
        	alert(getResources().getString(R.string.register_pwdConfrim_empty));
        	return;
        }
        
        if (strPwdConfirm.length() < 6) {//确认密码不能小于6个字符
        	alert(getResources().getString(R.string.register_pwdConfrim_less));
        	return;
        }
        
        if (strPwdConfirm.length() > 20) {//确认密码不能大于20个字符
        	alert(getResources().getString(R.string.register_pwdConfrim_more));
        	return;
        }
        
        
        if (!Utils.verifyUsername(strUserName)) {//注册用户名的帐号错误
            alert(getResources().getString(R.string.setting_username_format_error));
            return;
        }

        if (!Utils.verifyWowTalkPwd(strPassword)) {//密码格式错误
            alert(getResources().getString(R.string.settings_account_passwd_format_error));
            return;
        }
        
        if (!Utils.verifyWowTalkPwd(strPwdConfirm)) {//确认密码格式错误
            alert(getResources().getString(R.string.settings_account_pwdConfrim_format_error));
            return;
        }

		if (fIsPasswordFit()) {
		    mMsgBox.showWait();
			new Thread(new Runnable() {

				@Override
				public void run() {
                    int result = WowTalkWebServerIF.getInstance(RegisterActivity.this)
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
	
	/**
	 * 验证两次输入的密码是否一致
	 * @return
	 */
	private boolean fIsPasswordFit() {
		if (edtPwd.getText().toString().equals(edtPwdConfirm.getText().toString())) {
			return true;
		} else {		
				alert(getResources().getString(R.string.register_pwd_must_fit));
			return false;
		}
	}
	
	/**
	 * 处理各个监听事件
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
			
		case R.id.layout_register_user_type://选择账号类型
			showPopupMenu();
			break;
			
		//显示密码
		case R.id.imageview_show_password:
			imageview_show_password.setVisibility(View.GONE);
			imageview_hint_password.setVisibility(View.VISIBLE);
			edtPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			edtPwdConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			textView_isshow_password.setText("隐藏密码");
			break;
						
		//隐藏密码	
		case R.id.imageview_hint_password:
			imageview_show_password.setVisibility(View.VISIBLE);
			imageview_hint_password.setVisibility(View.GONE);
			edtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			edtPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			textView_isshow_password.setText("显示密码");
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
        mInputMethodManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);

        mMsgBox = new MessageBox(this);
		initView();
	}
	
	/**
	 * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	// TODO Auto-generated method stub
    	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
    		  if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
    			  mInputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);  
    			  }  
    		  }
    	return super.onTouchEvent(event);
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
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, findViewById(R.id.layout_register));

        //邮箱找回
        bottomBoard.add("老师", BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomBoard.dismiss();
                //process
                userType = Buddy.ACCOUNT_TYPE_TEACHER;
                textView_register_user_type.setText("老师");
                
            }
        });
        
        bottomBoard.add("学生", BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomBoard.dismiss();
                //process
                userType = Buddy.ACCOUNT_TYPE_STUDENT;
                textView_register_user_type.setText("学生");
                
                
            }
        });

        //close popupMenu
        bottomBoard.addCancelBtn(getString(R.string.login_findPassWord_cancel));
        bottomBoard.show();
    }
}
