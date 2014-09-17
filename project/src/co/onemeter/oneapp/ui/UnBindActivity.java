package co.onemeter.oneapp.ui;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

public class UnBindActivity extends Activity implements OnClickListener {

    public static final String UNBIND_TYPE = "unbind_type";
    public static final String UNBIND_PHONE_EMAIL_VALUE = "unbind_phone_email_value";
    public static final int UNBIND_TYPE_PHONE = 1;
    public static final int UNBIND_TYPE_EMAIL = 2;
    
    private ImageButton mBtnTitleBack;
    private TextView mUnbindTitle;
	private EditText mPhoneEmail;
	private EditText mEdtPassword;
	private TextView mUnbindMsg;
	private Button mBtnConfirm;
    private MessageBox mMsgBox;

    private String mPhoneEmailValue;
    private int mUnbindType;

	private void initView() {
		mBtnTitleBack = (ImageButton) findViewById(R.id.title_back);
		mUnbindTitle = (TextView) findViewById(R.id.unbind_title);
		mPhoneEmail = (EditText) findViewById(R.id.phone_email_text);
		mEdtPassword = (EditText) findViewById(R.id.edt_pwd);
		mUnbindMsg = (TextView) findViewById(R.id.unbind_msg);
		mBtnConfirm = (Button) findViewById(R.id.btn_confirm);
		mBtnTitleBack.setOnClickListener(this);
		mBtnConfirm.setOnClickListener(this);

		mPhoneEmail.setText(mPhoneEmailValue);
		switch (mUnbindType) {
        case UNBIND_TYPE_PHONE:
            mUnbindTitle.setText(R.string.unbind_phone_title);
            mUnbindMsg.setText(R.string.unbind_phone_msg);
            break;
        case UNBIND_TYPE_EMAIL:
            mUnbindTitle.setText(R.string.unbind_email_title);
            mUnbindMsg.setText(R.string.unbind_email_msg);
            break;
        default:
            break;
        }
	}
	
	private void unbind() {
        final String strPassword = mEdtPassword.getText().toString();
        if(TextUtils.isEmpty(strPassword)){
            mMsgBox.toast(R.string.unbind_pwd_empty);
            return;
        }

        mMsgBox.showWait();

		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
			    int resultCode = -1;
			    switch (mUnbindType) {
                case UNBIND_TYPE_PHONE:
                    resultCode = WowTalkWebServerIF.getInstance(UnBindActivity.this).fUnBindMobile(strPassword);
                    break;
                case UNBIND_TYPE_EMAIL:
                    resultCode = WowTalkWebServerIF.getInstance(UnBindActivity.this).fUnBindEmail(strPassword);
                    break;
                default:
                    break;
                }
				return resultCode;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

				if (result == ErrorCode.OK) {
					finish();
				} else if (result == ErrorCode.AUTH) {
				    mMsgBox.toast(getString(R.string.unbind_pwd_error));
                } else {
                    mMsgBox.toast(getString(R.string.unbind_failure));
                }
			}
			
		}.execute((Void)null);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.btn_confirm:
			unbind();
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unbind_phone_email_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Intent intent = getIntent();
		if (null != intent) {
		    mUnbindType = intent.getIntExtra(UNBIND_TYPE, UNBIND_TYPE_PHONE);
		    mPhoneEmailValue = intent.getStringExtra(UNBIND_PHONE_EMAIL_VALUE);
		}
		initView();
        mMsgBox = new MessageBox(this);
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

