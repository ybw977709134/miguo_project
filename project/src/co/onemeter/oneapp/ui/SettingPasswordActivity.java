package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.Account;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

public class SettingPasswordActivity extends Activity {
	private ImageButton btnTitleBack;
	private ImageButton btnTitleConfirm;
    private EditText edtOld;
	private EditText edtPwd;
	private EditText edtConfirm;
    private MessageBox mMsgBox;

    private void setPassword(final String password, final String oldPassword) {
        mMsgBox.showWait();

        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WebServerIF.getInstance(SettingPasswordActivity.this)
                        .fChangePassword(password, oldPassword);
                if (resultCode == ErrorCode.OK) {
                    // 更新SP_root中的帐号信息
                    PrefUtil prefUtil = PrefUtil.getInstance(SettingPasswordActivity.this);
                    ArrayList<Account> accounts = prefUtil.getAccountList();
                    Account tempAccount = null;
                    for (Iterator<Account> iterator = accounts.iterator();
                            iterator.hasNext();) {
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
                    finish();
                    break;
                case ErrorCode.ERR_OPERATION_DENIED:
                    mMsgBox.show(null, getString(R.string.settingpassword_old_pwd_error));
                    break;
                default:
                    mMsgBox.show(null, getString(R.string.settingpassword_failure));
                    break;
                }
			}
		}.execute((Void)null);
	}
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        edtOld = (EditText) findViewById(R.id.edt_old);
		edtPwd = (EditText) findViewById(R.id.edt_pwd);
		edtConfirm = (EditText) findViewById(R.id.edt_confirm);
		btnTitleBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
			
		});
		btnTitleConfirm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
                String oldPassword = edtOld.getText().toString();
				String password = edtPwd.getText().toString();
				String confirmPassword = edtConfirm.getText().toString();
                if (password.equals(confirmPassword)) {
                    if (Utils.verifyWowTalkPwd(password)) {
                        setPassword(password, oldPassword);
                    } else {
                        mMsgBox.show(null, getString(R.string.settings_account_passwd_format_error));
                    }
                } else {
                    mMsgBox.show(null, getString(R.string.settings_account_passwd_retypo));
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
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
