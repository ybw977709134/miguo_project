package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

public class BindPhoneActivity extends Activity implements OnClickListener {
    private static final String TAG = "BindPhoneActivity";
	private ImageButton btnTitleBack;
	private EditText edtNumber;
    private Button btnMcc;
    private EditText mMccText;
	private EditText edtPassword;
	private TextView mBindPhoneMsg;
	private Button btnConfirm;
    private MessageBox mMsgBox;

    /**
     * The phone has been bound to the other account,
     * force to bind will unbind it and bind to the new account.
     */
    private boolean mIsForceBound;
    private int mCurrMcc = 0; // current mobile country code

	private static BindPhoneActivity instance;

	private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // If the phoneNumber or password is changed, the bound style should be changed from force to normal.
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

	public static BindPhoneActivity instance() {
		if (instance == null)
			return new BindPhoneActivity();
		return instance;
	}
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnMcc = (Button) findViewById(R.id.btn_mcc);
        mMccText = (EditText) findViewById(R.id.mcc_text);
		edtNumber = (EditText) findViewById(R.id.edt_num);
		edtPassword = (EditText) findViewById(R.id.edt_pwd);
		mBindPhoneMsg = (TextView) findViewById(R.id.bind_phone_msg);
		btnConfirm = (Button) findViewById(R.id.btn_confirm);

		edtNumber.addTextChangedListener(mTextWatcher);
		edtPassword.addTextChangedListener(mTextWatcher);
		btnTitleBack.setOnClickListener(this);
		btnConfirm.setOnClickListener(this);
        btnMcc.setOnClickListener(this);

        edtNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
	}
	
	private void fetchAccessCode() {
	    String phoneNumberOnly = PhoneNumberUtils.stripSeparators(edtNumber.getText().toString());
        final String strNum = "+" + mCurrMcc + phoneNumberOnly;
        final String strPassword = edtPassword.getText().toString();
        if (strNum.equals("") || strPassword.equals("")) {
            mMsgBox.toast(R.string.bind_phone_error_msg);
            return;
        }
        if (!Utils.verifyPhoneNumber(strNum)) {
            mMsgBox.toast(R.string.bind_phone_format_error_msg);
            return;
        }

        mMsgBox.showWait();

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(BindPhoneActivity.this).fBindMobile(strNum, strPassword, mIsForceBound);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                Log.i(TAG, ", bind phone, the result code is " + result + "(is_force:" + mIsForceBound + ")");
                switch (result) {
                    case ErrorCode.OK:
                        Intent intent = new Intent(BindPhoneActivity.this, BindCodeActivity.class);
                        intent.putExtra(BindCodeActivity.REQ_INTENT, BindCodeActivity.INTENT_PHONE);
                        intent.putExtra(BindCodeActivity.PHONE_EMAIL_VALUE, strNum);
                        startActivity(intent);
                        break;
                    case ErrorCode.AUTH:
                        mMsgBox.show(null, getString(R.string.bind_phone_pwd_err));
                        break;
                    case ErrorCode.PHONE_USED_BY_OTHERS:
                        mIsForceBound = true;
                        changeBindStyle();
                        mMsgBox.show(null, getString(R.string.settings_account_phone_used_by_others));
                        break;
                    default:
                        mMsgBox.show(null, getString(R.string.bind_phone_failed));
                        break;
                }
            }

        });
	}

	/**
	 * Refresh the text of confirm button and the prompt message.
	 * When "mIsForceBound" is true, it's start to change to force bind from normal bind.
	 */
	private void changeBindStyle() {
	    if (mIsForceBound) {
            btnConfirm.setText(R.string.bindphone_force_bind);
            btnConfirm.setBackgroundResource(R.drawable.btn_large_red);
            mBindPhoneMsg.setText(R.string.bindphone_force_bind_msg);
	    } else {
	        btnConfirm.setText(R.string.next_step);
	        btnConfirm.setBackgroundResource(R.drawable.btn_large_blue_selector);
	        mBindPhoneMsg.setText(R.string.bind_phone_msg);
        }
	}


	@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.btn_confirm:
                fetchAccessCode();
                break;
            case R.id.btn_mcc: // select MCC (Mobile Country Code)

                new AlertDialog.Builder(this).setTitle(R.string.settings_account_select_country)
                .setItems(R.array.country_name_en, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int[] mcc_values = getResources().getIntArray(R.array.phone_code);
                        int mcc = mcc_values[i];
                        dialogInterface.dismiss();
                        updateMcc(mcc, getResources().getStringArray(R.array.country_name_en)[i]);
                    }
                })
                .create().show();
                break;
            default:
                break;
        }
    }

    private void updateMcc(int mcc, String title) {
        if (mcc != mCurrMcc) {
            mCurrMcc = mcc;
            mMccText.setText("+" + mCurrMcc);
            btnMcc.setText(title);
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bind_phone_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView();

		instance = this;
        mMsgBox = new MessageBox(this);

        updateMcc(86, "China");
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
