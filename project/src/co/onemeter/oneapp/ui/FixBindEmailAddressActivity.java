package co.onemeter.oneapp.ui;

import java.util.List;
import java.util.Map;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 修改绑定邮箱
 * @author hutianfeng
 * @date 2015/3/5
 *
 */
public class FixBindEmailAddressActivity extends Activity implements OnClickListener{
	
	private ImageButton title_back;
	private TextView textView_fixBindEmail_back;//返回
	
	private TextView textView_fixBindEmail_cancel;//取消
	
	private TextView txt_access_code;//验证码
	private Button btn_access_code;//获取验证码按钮
	
	private TextView textView_verification_code_result;//验证结果
	
	private Button btn_verification_code;//确认
	
	private MessageBox mMsgBox;
	String bindEmail= null;
	
	private int time;
	private Timer mTimer;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				btn_access_code.setText("获取验证码");
				btn_access_code.setEnabled(true);
				btn_access_code.setTextColor(getResources().getColor(R.color.blue));
				btn_access_code.setBackground(getResources().getDrawable(R.drawable.btn_small_valid));
				mTimer.cancel();
			} else {
				btn_access_code.setText("重新发送验证码" + "("+ time + "s" + ")");
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fix_bind_email_address);
		mMsgBox = new MessageBox(this);
		initView();
		
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
	
	
	/**
	 * 初始化各个控件
	 */
	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_fixBindEmail_back = (TextView) findViewById(R.id.textView_fixBindEmail_back);
		textView_fixBindEmail_cancel = (TextView) findViewById(R.id.textView_fixBindEmail_cancel);
		
		txt_access_code = (TextView) findViewById(R.id.txt_access_code);
		btn_access_code = (Button) findViewById(R.id.btn_access_code);
		
		textView_verification_code_result = (TextView) findViewById(R.id.textView_verification_code_result);
		btn_verification_code = (Button) findViewById(R.id.btn_verification_code);
		btn_verification_code.setEnabled(false);
		btn_verification_code.setBackgroundResource(getResources().getColor(R.color.gray));
		
		title_back.setOnClickListener(this);
		textView_fixBindEmail_back.setOnClickListener(this);
		
		textView_fixBindEmail_cancel.setOnClickListener(this);
		btn_access_code.setOnClickListener(this);
		btn_verification_code.setOnClickListener(this);
	}
	
	@Override
	protected void onDestroy() {
		if (mTimer != null) {
			mTimer.cancel();
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
			
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("提示");
			builder.setMessage("你确定要取消修改绑定邮箱吗？");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					//取消绑定邮箱，解绑
//					unBindEmailAddress();
					finish();
				}
			});
			
			builder.setNegativeButton("取消", null);
			builder.create().show();
			
			break;
			
		case R.id.btn_access_code:
			stopGetAccessCode();
			getAccessCode(bindEmail);
			break;
			
		case R.id.btn_verification_code://确认，跳转到绑定邮箱界面
			verifyBindEmailAddress(txt_access_code.getText().toString(),bindEmail);
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
		btn_access_code.setTextColor(getResources().getColor(R.color.gray));
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
	private void getAccessCode(final String emailAddress) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fBindEmialAddress(emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0
                    	btn_verification_code.setEnabled(true);
                    	btn_verification_code.setBackgroundResource(getResources().getColor(R.color.blue));
                        break;
  
                    default://获取验证码失败
                        mMsgBox.show(null, "获取验证码失败");
                        break;
                }
            }
        });
	}
	
	/**
	 * 验证绑定邮箱收到的验证码
	 * @param access_code
	 * @param emailAddress
	 * @author hutainfeng
	 * @date 2015/3/5
	 */
	private void verifyBindEmailAddress(final String access_code,final String emailAddress) {
		mMsgBox.showWait();
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fVerifyEmailAddress(access_code,emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0 
                    	unBindEmailAddress();
                    	
                        
                        break;
                        
                    case ErrorCode.ACCESS_CODE_ERROR://22:无效的验证码，验证码有有效期，目前是一天的有效期
                    	textView_verification_code_result.setVisibility(View.VISIBLE);
                    	textView_verification_code_result.setText(getString(R.string.access_code_error));
                        break;
                        
                    default:
                        mMsgBox.show(null, getString(R.string.fix_bind_email_failed));
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
                return WowTalkWebServerIF.getInstance(FixBindEmailAddressActivity.this).fUnBindEmailAddress();
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0 //解绑成功后方可跳转到重新绑定邮箱的界面       	
                    	Intent intent = new Intent(FixBindEmailAddressActivity.this,BindEmailAddressActivity.class);
                        startActivity(intent);
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