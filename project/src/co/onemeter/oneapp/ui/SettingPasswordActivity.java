package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;


import org.wowtalk.api.Account;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.Iterator;

public class SettingPasswordActivity extends Activity {
	
	private RelativeLayout layout_setting_password;
	private ImageButton btnTitleBack;
	private TextView textView_fixPassword_back;
	private Button btn_newPassWord_ok;
    private EditText edtOld;
	private EditText edtPwd;
	private EditText edtConfirm;
	
	//清除文本
	private ImageButton field_clear_old;
	private ImageButton field_clear_pwd;
	private ImageButton field_clear_confirm;
	
	private ImageView imageview_show_password;//显示密码
	private ImageView imageview_hint_password;//隐藏密码
	private TextView textView_verification_newPassword;//确认两次是否一致
	
    private MessageBox mMsgBox;

    private void setPassword(final String password, final String oldPassword) {
        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WowTalkWebServerIF.getInstance(SettingPasswordActivity.this)
                        .fChangePassword(password, oldPassword);
                if (resultCode == ErrorCode.OK) {
                    // 更新SP_root中的帐号信息
                    PrefUtil prefUtil = PrefUtil.getInstance(SettingPasswordActivity.this);
                    ArrayList<Account> accounts = prefUtil.getAccountList();
                    Account tempAccount = null;
                    for (Iterator<Account> iterator = accounts.iterator();
                         iterator.hasNext(); ) {
                        tempAccount = iterator.next();
                        if (prefUtil.getUid().equals(tempAccount.uid)) {
                            tempAccount.password = prefUtil.getPassword();
                            prefUtil.setAccountList(accounts);
                            break;
                        }
                    }
                }
                return resultCode;
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                switch (result) {
                    case ErrorCode.OK:
                        setResult(Activity.RESULT_OK);
                        mMsgBox.showWaitImageSuccess("密码修改成功");
                        
                        new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
								SettingPasswordActivity.this.finish();
								
							}
						}).start();
                        
                        break;
                    case ErrorCode.ERR_OPERATION_DENIED:
                    	textView_verification_newPassword.setVisibility(View.VISIBLE);
                    	textView_verification_newPassword.setText(getString(R.string.settingpassword_old_pwd_error));
//                        mMsgBox.show(null, getString(R.string.settingpassword_old_pwd_error));
                        break;
                    default:
                    	textView_verification_newPassword.setVisibility(View.VISIBLE);
                    	textView_verification_newPassword.setText(getString(R.string.settingpassword_failure));
//                        mMsgBox.show(null, getString(R.string.settingpassword_failure));
                        break;
                }
            }
        });
	}
	
	private void initView() {
		
		layout_setting_password = (RelativeLayout) findViewById(R.id.layout_setting_password);
		layout_setting_password.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_setting_password.setFocusable(true);
				layout_setting_password.setFocusableInTouchMode(true);
				layout_setting_password.requestFocus();
				field_clear_old.setVisibility(View.GONE);
				field_clear_pwd.setVisibility(View.GONE);
				field_clear_confirm.setVisibility(View.GONE);
				
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);  
		        return imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);  

			}
		});
		
		
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		textView_fixPassword_back = (TextView) findViewById(R.id.textView_fixPassword_back);
		btn_newPassWord_ok = (Button) findViewById(R.id.btn_newPassWord_ok);
        edtOld = (EditText) findViewById(R.id.edt_old);
		edtPwd = (EditText) findViewById(R.id.edt_pwd);
		edtConfirm = (EditText) findViewById(R.id.edt_confirm);
		
		field_clear_old = (ImageButton) findViewById(R.id.field_clear_old);
		field_clear_pwd = (ImageButton) findViewById(R.id.field_clear_pwd);
		field_clear_confirm = (ImageButton) findViewById(R.id.field_clear_confirm);
		
		imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
		imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
		textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
		
		//显示密码
		imageview_show_password.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				imageview_show_password.setVisibility(View.GONE);
				imageview_hint_password.setVisibility(View.VISIBLE);
				edtOld.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				edtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				edtConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				
			}
		});
		
		//隐藏密码
		imageview_hint_password.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				imageview_show_password.setVisibility(View.VISIBLE);
				imageview_hint_password.setVisibility(View.GONE);
				edtOld.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				edtPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				edtConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				
			}
		});
		
		
		//老密码
		edtOld.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear_old.setVisibility(View.GONE);
					
				} else {
					field_clear_old.setVisibility(View.VISIBLE);
					
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
		
		
		edtOld.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtOld.getText().toString().length() > 0) {
						field_clear_old.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_old.setVisibility(View.GONE);
				}
			}
		});
		
		field_clear_old.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				edtOld.setText("");
				
			}
		});
		

		edtPwd.addTextChangedListener(new TextWatcher() {
			
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
		
		
		edtPwd.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtPwd.getText().toString().length() > 0) {
						field_clear_pwd.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_pwd.setVisibility(View.GONE);
				}
			}
		});
		
		
		field_clear_pwd.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				edtPwd.setText("");
				
			}
		});

		//确认密码
		edtConfirm.addTextChangedListener(new TextWatcher() {
	
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
		
		edtConfirm.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {

					textView_verification_newPassword.setVisibility(View.GONE);
					if (edtConfirm.getText().toString().length() > 0) {
						field_clear_confirm.setVisibility(View.VISIBLE);
					}
				} else {
					field_clear_confirm.setVisibility(View.GONE);
				}
			}
		});

		field_clear_confirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				edtConfirm.setText("");
				
			}
		});
		
		

		//返回
		textView_fixPassword_back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				finish();
				
			}
		});
		//返回
		btnTitleBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
			
		});
		btn_newPassWord_ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
                String oldPassword = edtOld.getText().toString();
				String password = edtPwd.getText().toString();
				String confirmPassword = edtConfirm.getText().toString();
                if (password.equals(confirmPassword)) {
                    if (Utils.verifyWowTalkPwd(password)) {
                        setPassword(password, oldPassword);
                    } else {
                    	textView_verification_newPassword.setVisibility(View.VISIBLE);
                    	textView_verification_newPassword.setText(getString(R.string.settings_account_passwd_format_error));
//                        mMsgBox.show(null, getString(R.string.settings_account_passwd_format_error));
                    }
                } else {
                	textView_verification_newPassword.setVisibility(View.VISIBLE);
                	textView_verification_newPassword.setText(getString(R.string.settings_account_passwd_retypo));
//                    mMsgBox.show(null, getString(R.string.settings_account_passwd_retypo));
                }
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting_pwd);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
