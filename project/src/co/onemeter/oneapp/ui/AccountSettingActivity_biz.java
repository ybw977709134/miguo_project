package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.Connect2;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;

public class AccountSettingActivity_biz extends Activity implements OnClickListener{
	public static final int REQ_INPUT_ID = 11;
	public static final int REQ_INPUT_PASSWORD = 12;

	private ImageButton btnTitleBack;

    private TextView txtCompanyID;
	private TextView txtYuanquID;
	private TextView txtPwdSetting;
	private Button btnLogout;
	
	private LinearLayout mPassword;
	private static AccountSettingActivity_biz instance;

    WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
	private MessageBox mMsgBox;

	public static final AccountSettingActivity_biz instance() {
		if (instance != null)
			return instance;
		return null;
	}
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnLogout = (Button) findViewById(R.id.btn_logout);
        txtCompanyID = (TextView) findViewById(R.id.txt_company);
		txtYuanquID = (TextView) findViewById(R.id.yuanqu_id_text);
		txtPwdSetting = (TextView) findViewById(R.id.pwd_text);
		mPassword = (LinearLayout) findViewById(R.id.layout_password);
		txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
		txtPwdSetting.setTextColor(getResources().getColor(R.color.setting_orange));
		
		btnTitleBack.setOnClickListener(this);
		btnLogout.setOnClickListener(this);
		mPassword.setOnClickListener(this);
	}
	
	private void fetchData() {
	    String companyId = PrefUtil.getInstance(this).getCompanyId();
        txtCompanyID.setText(companyId);

		if (mPrefUtil.getMyPasswordChangedState()) {
            txtPwdSetting.setText(getResources().getString(R.string.settings_account_setted));
            txtPwdSetting.setTextColor(getResources().getColor(R.color.text_gray3));
		} else {
            txtPwdSetting.setText(getResources().getString(R.string.settings_account_not_set));
            txtPwdSetting.setTextColor(getResources().getColor(R.color.text_orange));
		}

        if (mPrefUtil.getMyWowtalkIdChangedState()) {
            String wowtalkIdWithCompanyId = mPrefUtil.getMyWowtalkID();
            String wowtalkId = wowtalkIdWithCompanyId;
            // companyId 和 wowtalkId 不区分大小写
            // 为不改变登录时的 companyId 和 wowtalkId 的大小写，使用tempXxxx进行判断
            String tempLowerCompanyId = companyId.toLowerCase();
            String tempLowerwowtalkIdWithCompanyId = wowtalkIdWithCompanyId.toLowerCase();
            if (tempLowerwowtalkIdWithCompanyId.startsWith(tempLowerCompanyId + "_")) {
                // companyId + "_" + wowtalkId
                wowtalkId = wowtalkIdWithCompanyId.substring(companyId.length() + 1);
            }
            txtYuanquID.setText(wowtalkId);
        } else {
            txtYuanquID.setText(getResources().getString(R.string.settings_account_not_set));
        }
	}
	
	private void logout() {
        mMsgBox.showWait();
		new AsyncTask<Void, Integer ,Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {

				return mWeb.fLogout();
			}

			@Override
			protected void onPostExecute(Integer result) {
                // 现在不在这个类中logout，所以此处不需要了
//                deleteDatasInDB(AccountSettingActivity_biz.this);
				PrefUtil.getInstance(AccountSettingActivity_biz.this).clear();
                Database database = new Database(AccountSettingActivity_biz.this);
                database.close();
                // 变换Database,Connect2的标志位(用户判断是否切换了用户)
                Database.sFlagIndex++;
                Connect2.sFlagIndex++;

                WowTalkVoipIF.getInstance(AccountSettingActivity_biz.this).fStopWowTalkService();

                // If the network is not available, the MsgBox will show the toast,
                // so we should dismiss it before finish the activity.
                mMsgBox.dismissWait();
                mMsgBox.dismissToast();

                if (StartActivity.isInstanciated()) {
                    StartActivity.instance().finish();
                }
                Intent intent = new Intent(AccountSettingActivity_biz.this, LoginActivity.class);
                startActivity(intent);

                finish();
            }
        }.execute((Void)null);
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.btn_logout:
            if(mPrefUtil.getMyWowtalkIdChangedState()
                    && mPrefUtil.getMyPasswordChangedState()) {
                logout();
            } else {
                mMsgBox.show(null, getString(R.string.settings_account_logout_without_set_id_pwd));
            }
			break;
		case R.id.layout_password:
			Intent passwordIntent = new Intent(AccountSettingActivity_biz.this, SettingPasswordActivity.class);
			startActivityForResult(passwordIntent, REQ_INPUT_PASSWORD);
			break;
		default:
			mMsgBox.toast(R.string.not_implemented);
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_setting_biz);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mWeb = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
		instance = this;
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        fetchData();

    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_INPUT_ID && resultCode == Activity.RESULT_OK) {
		} else if (requestCode == REQ_INPUT_PASSWORD && resultCode == Activity.RESULT_OK) {
		}
	}
}
