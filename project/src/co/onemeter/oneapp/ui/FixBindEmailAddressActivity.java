package co.onemeter.oneapp.ui;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 修改绑定邮箱
 * @author hutianfeng
 * @date 2015/3/5
 *
 */
public class FixBindEmailAddressActivity extends Activity implements OnClickListener{
	
	private RelativeLayout layout_fix_bind_email;
	InputMethodManager mInputMethodManager ;
	private ImageButton title_back;
	private TextView textView_fixBindEmail_back;//返回
	
	private TextView textView_fixBindEmail_cancel;//取消
	
	private TextView txt_access_code;//验证码
	private Button btn_access_code;//获取验证码按钮
	
	private TextView textView_verification_code_result;//验证结果
	
	private Button btn_verification_code;//确认
	
	public static final String FIX_BIND_EMAIL = "fix_bind_email";//修改绑定邮箱页面的请求码
	
	private MessageBox mMsgBox;
	String bindEmail= null;
	
	private int time = 60;
	private Timer mTimer;
	private static FixBindEmailAddressActivity instance;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				btn_access_code.setText("重新获取验证码");
				btn_access_code.setEnabled(true);
				btn_access_code.setTextColor(getResources().getColor(R.color.blue_12));
				btn_access_code.setBackground(getResources().getDrawable(R.drawable.btn_small_valid));
				mTimer.cancel();
			} else {
				btn_access_code.setText("重新发送验证码" + "("+ time + "s" + ")");
			}
		};
	};
	
	
	public static final FixBindEmailAddressActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}

	public static final boolean isInstanciated() {
		return instance != null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fix_bind_email_address);
		
		instance = this;
		mMsgBox = new MessageBox(this);
		initView();
		
		mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        
		txt_access_code.setFocusable(true);
		txt_access_code.setFocusableInTouchMode(true);
		txt_access_code.requestFocus();
        
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {		
				mInputMethodManager.showSoftInput(txt_access_code, InputMethodManager.RESULT_SHOWN);
				mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
		}, 200);
        
        
		
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, List<Map<String, Object>>> () {
			
            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fEmailBindStatus();
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
            	mMsgBox.dismissWait();
            	if (result != null) {
            	bindEmail = (String) result.get(0).get("email");//有值解绑
            	
            } else {
            	Toast.makeText(FixBindEmailAddressActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
            }
            
            } 
        });
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	/**
	 * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

    	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
    		  if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
    			  closeSoftKeyboard();
    			  
    			  }
    		  }
    	return super.onTouchEvent(event);
    }
    
    private void closeSoftKeyboard() {
		mInputMethodManager.hideSoftInputFromWindow(txt_access_code.getWindowToken() , 0);
		Log.i("---fix_txt_access_code");
}
	
	
	/**
	 * 初始化各个控件
	 */
	private void initView() {
		layout_fix_bind_email = (RelativeLayout) findViewById(R.id.layout_fix_bind_email);
		
		layout_fix_bind_email.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					layout_fix_bind_email.setFocusable(true);
					layout_fix_bind_email.setFocusableInTouchMode(true);
					layout_fix_bind_email.requestFocus();
					return false;
				}
			});
		  
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_fixBindEmail_back = (TextView) findViewById(R.id.textView_fixBindEmail_back);
		textView_fixBindEmail_cancel = (TextView) findViewById(R.id.textView_fixBindEmail_cancel);
		
		txt_access_code = (TextView) findViewById(R.id.txt_access_code);
		
		//输入验证码时，清除图片按钮的控制
		txt_access_code.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					
					btn_verification_code.setTextColor(getResources().getColor(R.color.white_40));
					btn_verification_code.setEnabled(false);
					btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_gray_medium_selector));
				} else {
					
					btn_verification_code.setTextColor(getResources().getColor(R.color.white));
					btn_verification_code.setEnabled(true);
					btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_blue_medium_selector));
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
    	
		btn_access_code = (Button) findViewById(R.id.btn_access_code);
		
		textView_verification_code_result = (TextView) findViewById(R.id.textView_verification_code_result);
		btn_verification_code = (Button) findViewById(R.id.btn_verification_code);
		btn_verification_code.setEnabled(false);
		btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_gray_medium_selector));
		title_back.setOnClickListener(this);
		textView_fixBindEmail_back.setOnClickListener(this);
		
		textView_fixBindEmail_cancel.setOnClickListener(this);
		btn_access_code.setOnClickListener(this);
		btn_verification_code.setOnClickListener(this);
	}
	
	
	@Override
	protected void onPause() {

		if (txt_access_code.hasFocus()) {
			closeSoftKeyboard();
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (mTimer != null) {
			mTimer.cancel();
		}
		
		if (instance != null) {
			instance = null;
		}
		super.onDestroy();
	}
	
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
		case R.id.textView_fixBindEmail_back:
			finish();
			break;
			
		case R.id.textView_fixBindEmail_cancel:
			
			 MessageDialog dialog = new MessageDialog(FixBindEmailAddressActivity.this);
             
             dialog.setMessage("你确定要取消修改绑定邮箱吗？");
             dialog.setOnRightClickListener("取消", null);
             dialog.setOnLeftClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                 @Override
                 public void onclick(MessageDialog dialog) {
                     dialog.dismiss();
                     Intent intent = new Intent(FixBindEmailAddressActivity.this,AccountSettingActivity.class);
 					 startActivity(intent);
 					 FixBindEmailAddressActivity.this.finish();
                 }
             });
             dialog.show();

			break;
			
		case R.id.btn_access_code:
			textView_verification_code_result.setVisibility(View.GONE);
			getAccessCode(PrefUtil.getInstance(FixBindEmailAddressActivity.this).getMyUsername(),bindEmail);
			
			break;
			
		case R.id.btn_verification_code://确认，跳转到绑定邮箱界面
			
			checkCodeRetrievePassword(PrefUtil.getInstance(FixBindEmailAddressActivity.this).getMyUsername(),txt_access_code.getText().toString());
			
			break;
		default:
			break;
		}
		
	}
	
	
	
	/**
	 * 控制重新获取验证的按钮状态
	 * @author hutainfeng
	 * @date 2015/3/5
	 * 
	 */
	private void stopGetAccessCode() {
		//60秒内不可点击  重新获取验证码
		btn_access_code.setEnabled(false);
		btn_access_code.setTextColor(getResources().getColor(R.color.gray_13));
		btn_access_code.setBackground(getResources().getDrawable(R.drawable.btn_small_invalid));
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
	 * 
	 * 绑定邮箱地址
	 * @param emailAddress
	 * @author hutianfeng
	 * @date 2015/3/4
	 */
	private void getAccessCode(final String wowtalk_id,final String emailAddress) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fSendCodeRetrievePassword(wowtalk_id, emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
            	mMsgBox.dismissWait();
            	Log.i("---result:"+result);
                switch (result) {
                	
                    case ErrorCode.OK://0
                    	if (mTimer != null) {
                			mTimer.cancel();
                		}
                    	stopGetAccessCode();
                        break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR_OVER://23:验证码一天最多只能验证5次
                    	closeSoftKeyboard();
                    	MessageDialog dialog = new MessageDialog(FixBindEmailAddressActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setMessage("今天邮的箱验证次数已用完"+"\n"+"请明天再试。");                      
                        dialog.show();
                        break;
 
                    default://获取验证码失败
//                        mMsgBox.show(null, "获取验证码失败");
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("获取验证码失败");	
//                        Toast.makeText(FixBindEmailAddressActivity.this, "获取验证码失败", Toast.LENGTH_SHORT).show();
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
	 * @date 2015/3/12
	 */
	private void checkCodeRetrievePassword (final String wowtalk_id,final String access_code) {
		mMsgBox.showWaitProgressbar("验证中");
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fCheckCodeRetrievePassword(wowtalk_id, access_code);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0 
                    	//跳转到绑定邮箱界面
                    	unBindEmailAddress();                    	
                        break;
                        
                    case ErrorCode.USER_NOT_EXISTS://-99
                    	textView_verification_code_result.setVisibility(View.VISIBLE);
                    	textView_verification_code_result.setText("你输入的账号不存在");
                    	break;
                    	
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_FALSE://1108
                    	textView_verification_code_result.setVisibility(View.VISIBLE);
                    	textView_verification_code_result.setText("验证码不正确");
                    	break;
                    	
                    case ErrorCode.FORGET_PWD_ACCESS_CODE_OUT_TIME://1109
                    	textView_verification_code_result.setVisibility(View.VISIBLE);
                    	textView_verification_code_result.setText("验证码已过时");
                    	break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR_OVER://24:验证码一天最多只能验证5次
                    	MessageDialog dialog = new MessageDialog(FixBindEmailAddressActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setMessage("今天邮箱验证次数已用完，请明天再试。");                      
                        dialog.show();
                        break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR://22:无效的验证码，验证码有有效期，目前是一天的有效期
                    	textView_verification_code_result.setVisibility(View.VISIBLE);
                    	textView_verification_code_result.setText(getString(R.string.access_code_error));
                        break;
                        
                    default:
                        mMsgBox.show(null, "验证不通过");
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

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fUnBindEmailAddress();
            }

            @Override
            protected void onPostExecute(Integer result) {

                switch (result) {
                    case ErrorCode.OK://0 //解绑成功后方可跳转到重新绑定邮箱的界面       	
            	
                    	Toast.makeText(FixBindEmailAddressActivity.this, "原绑定邮箱已解绑", Toast.LENGTH_SHORT).show();
                    	Intent bindIntent = new Intent(FixBindEmailAddressActivity.this,BindEmailAddressActivity.class);
                    	bindIntent.putExtra(FIX_BIND_EMAIL, true);
                    	startActivity(bindIntent);
                    	FixBindEmailAddressActivity.this.finish();
                    	
                        break;
                        
                    default:
                    	Toast.makeText(FixBindEmailAddressActivity.this, "连接服务器失败,请检查网络", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
	}

}
