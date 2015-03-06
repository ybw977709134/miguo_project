package co.onemeter.oneapp.ui;

import java.util.List;
import java.util.Map;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * <p>找回密码。</p>
 * Created by pzy on 11/23/14.
 */
public class ForgetPasswordActivity extends Activity implements OnClickListener {
	
	private static int BIND_EMAIL_PAGE = 1;//验证绑定邮箱阶段
	private static int AUTH_CODE_PAGE = 2;//验证验证码阶段
	private static int RESET_NEW_PASSWORD_PAGE = 3;//重置密码阶段
	//验证阶段判断标志
	private int pageFlag = BIND_EMAIL_PAGE;//默认为验证绑定邮箱阶段
	
	private TextView textView_findPassword_back;//文本返回
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
	
	//重置新密码
	
	private RelativeLayout layout_reset_password;
	private EditText txt_new_password;//新密码
	private EditText txt_confirm_new_password;//确认新密码
	private ImageView imageview_show_password;//显示密码
	private ImageView imageview_hint_password;//隐藏密码
	private TextView textView_isshow_password;//显示密码文本提示
	private TextView textView_verification_newPassword;//验证新设置的密码两次设置是否一致
	private Button btn_newPassWord_ok;//提交重新设置后的密码
	
	private MessageBox mMsgBox;
	private String bindEmail = null;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        mMsgBox = new MessageBox(this);
        initView();
        
//        bindEmailStatus ();
        
    }
    
    /**
     * 初始化控件
     */
    private void initView() {
    	
    	//页标题
    	textView_findPassword_back = (TextView) findViewById(R.id.textView_findPassword_back);
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
    	
    	//重置新密码
    	layout_reset_password = (RelativeLayout) findViewById(R.id.layout_reset_password);
    	txt_new_password =  (EditText) findViewById(R.id.txt_new_password);
    	txt_confirm_new_password =  (EditText) findViewById(R.id.txt_confirm_new_password);
    	imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
    	imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
    	textView_isshow_password = (TextView) findViewById(R.id.textView_isshow_password);
    	textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
    	btn_newPassWord_ok = (Button) findViewById(R.id.btn_newPassWord_ok);
    	
    	
    	
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
    	
    	textView_findPassword_back.setOnClickListener(this);
    	textView_findPassword_cancel.setOnClickListener(this);
    	title_back.setOnClickListener(this);
    	field_clear_email.setOnClickListener(this);
    	btn_verification_email.setOnClickListener(this);
    	field_clear_auth_code.setOnClickListener(this);
    	btn_verification_auth_code.setOnClickListener(this);
    	btn_again_receive_auth_code.setOnClickListener(this);
    	imageview_show_password.setOnClickListener(this);
    	imageview_hint_password.setOnClickListener(this);
    	btn_newPassWord_ok.setOnClickListener(this);
    	
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
			break;
		//清除验证码文本框中的内容	
		case R.id.field_clear_auth_code:
			txt_auth_code.setText("");
			break;
		//返回
		case R.id.textView_findPassword_back:
		case R.id.title_back:
			if (pageFlag == BIND_EMAIL_PAGE) {
				finish();
			} else if (pageFlag == AUTH_CODE_PAGE) {
				pageFlag = BIND_EMAIL_PAGE;
				layout_verification_auth_code.setVisibility(View.GONE);
				layout_verification_email.setVisibility(View.VISIBLE);
			} else {
				pageFlag = AUTH_CODE_PAGE;
				layout_reset_password.setVisibility(View.GONE);
				layout_verification_auth_code.setVisibility(View.VISIBLE);
				imageview_show_password.setVisibility(View.VISIBLE);
				imageview_hint_password.setVisibility(View.GONE);
				txt_new_password.setText("");
				txt_confirm_new_password.setText("");
			}
			
			break;
			
		//取消
		case R.id.textView_findPassword_cancel:
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("提示");
			builder.setMessage("你确定要取消密码找回吗？");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					finish();
				}
			});
			
			builder.setNegativeButton("取消", null);
			builder.create().show();
			
			break;
			
		//验证绑定邮箱	
		case R.id.btn_verification_email:
			//成功时
//			pageFlag = AUTH_CODE_PAGE;//2
//			layout_verification_email.setVisibility(View.GONE);
//			layout_verification_auth_code.setVisibility(View.VISIBLE);
			
//			bindEmailStatus ();
			ret("Back3","2364611478@qq.com");
			
			break;
			
		//验证验证码	
		case R.id.btn_verification_auth_code:
			//成功时
			pageFlag = RESET_NEW_PASSWORD_PAGE;//3
			layout_verification_auth_code.setVisibility(View.GONE);
			layout_reset_password.setVisibility(View.VISIBLE);
			
			break;
			
		//重新获得验证码	
		case R.id.btn_again_receive_auth_code:
			
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
			Toast.makeText(ForgetPasswordActivity.this, "功能正在实现中..", Toast.LENGTH_SHORT).show();
			break;
			
		default:
			break;
		}
		
	}
	
	
	
	 /**
     * 检测用户绑定邮箱的状态
     */
    private void bindEmailStatus () {
    	mMsgBox.showWait();
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, List<Map<String, Object>>> () {
    		
            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ForgetPasswordActivity.this).fEmailBindStatus();
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
            	mMsgBox.dismissWait();
            	if (result != null) {
            	bindEmail = (String) result.get(0).get("email");
            } else {
            	Toast.makeText(ForgetPasswordActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
            }
            
            } 
        });
    }
    
    private void ret(final String wowtalk_id,final String emailAddress) {
    	mMsgBox.showWait();
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ForgetPasswordActivity.this).newRetrievePassword(wowtalk_id,emailAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
               
                switch (result) {
                    case ErrorCode.OK://0
                    	mMsgBox.show(null,"重置密码成功");
                    	mMsgBox.dismissDialog();
                        break;
                    default://邮箱绑定失败
                        mMsgBox.show(null, getString(R.string.bind_email_failed));
                        mMsgBox.dismissDialog();
                        break;
                }
            }
        });
    }
	
}




















