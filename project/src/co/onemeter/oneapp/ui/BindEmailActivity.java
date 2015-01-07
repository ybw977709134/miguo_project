package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

public class BindEmailActivity extends Activity implements OnClickListener{
    private static final String TAG = "BindEmailActivity";
	private ImageButton btnTitleBack;
	private EditText edtEmail;
	private EditText edtPassword;
	private TextView mBindEmailMsg;
	private Button btnNext;
    private MessageBox mMsgBox;
    private boolean mIsForceBound;

	private static BindEmailActivity instance;
	
	public static BindEmailActivity instance() {
		if (instance == null)
			return new BindEmailActivity();
		return instance;
	}

	private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // If the email or password is changed, the bound style should be changed from force to normal.
            if (mIsForceBound) {
                mIsForceBound = false;
                changeBindStyle();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

	private void bindEmail(final String strEmail, final String password) {
        mMsgBox.showWait();

		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(BindEmailActivity.this).fBindEmail(strEmail, password, mIsForceBound);
			}
			@Override
			protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                Log.i(TAG, ", bind email, the result code is " + result + "(is_force:" + mIsForceBound + ")");
				switch (result) {
                case ErrorCode.OK:
                    Intent intent = new Intent(BindEmailActivity.this, BindCodeActivity.class);
                    intent.putExtra(BindCodeActivity.REQ_INTENT, BindCodeActivity.INTENT_EMAIL);
                    intent.putExtra(BindCodeActivity.PHONE_EMAIL_VALUE, strEmail);
                    startActivity(intent);
                    break;
                case ErrorCode.AUTH:
                    mMsgBox.show(null, getString(R.string.bind_email_pwd_err));
                    break;
                case ErrorCode.EMAIL_USED_BY_OTHERS:
                    mIsForceBound = true;
                    changeBindStyle();
                    mMsgBox.show(null, getString(R.string.settings_account_email_used_by_others));
                    break;
                default:
                    mMsgBox.show(null, getString(R.string.bind_email_failed));
                    break;
                }
			}
		}.execute((Void)null);
	}
	
	/**
     * Refresh the text of confirm button and the prompt message.
     * When "mIsForceBound" is true, it's start to change to force bind from normal bind.
     */
    private void changeBindStyle() {
        if (mIsForceBound) {
            btnNext.setText(R.string.bindemail_force_bind);
            btnNext.setBackgroundResource(R.drawable.btn_large_red);
            mBindEmailMsg.setText(R.string.bindemail_force_bind_msg);
        } else {
            btnNext.setText(R.string.next_step);
            btnNext.setBackgroundResource(R.drawable.btn_large_blue_selector);
            mBindEmailMsg.setText(R.string.bind_email_msg);
        }
    }

    private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		edtEmail = (EditText) findViewById(R.id.edt_email);
		edtPassword = (EditText) findViewById(R.id.edt_pwd);
		mBindEmailMsg = (TextView) findViewById(R.id.txt_msg);
		btnNext = (Button) findViewById(R.id.btn_next);

		edtEmail.addTextChangedListener(mTextWatcher);
		edtPassword.addTextChangedListener(mTextWatcher);
		btnTitleBack.setOnClickListener(this);
		btnNext.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.btn_next:
			final String strEmail = edtEmail.getText().toString();
			final String strPassword = edtPassword.getText().toString();
            if (strEmail.equals("") || strPassword.equals("")) {
                mMsgBox.toast(R.string.bind_email_error_msg);
            } else if (!Utils.verifyEmail(strEmail)) {
                mMsgBox.toast(R.string.bind_email_format_error_msg);;
            } else {
                bindEmail(strEmail, strPassword);
            }
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bind_email_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView();
		instance = this;
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
