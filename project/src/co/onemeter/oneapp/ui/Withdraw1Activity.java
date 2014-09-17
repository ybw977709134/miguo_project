package co.onemeter.oneapp.ui;

import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Withdraw1Activity extends Activity implements OnClickListener{
	public static final String WOWTALK_ID = "wowtalk_id";
	
	private EditText edtInput;
	private Button mMccBtn;
	private EditText mMccText;
	private EditText edtBind;
	private Button btnBack;
	private Button btnFetch;
    private MessageBox mMsgBox;

    private int mCurrMcc;
	private String[] binds = new String[2];
    private static Withdraw1Activity instance;
	
	TextWatcher textWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() == 0) {
				
			} else {
				edtInput.setTextColor(getResources().getColor(R.color.white));
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

    public static Withdraw1Activity instance() {
        if (instance == null)
            return new Withdraw1Activity();
        return instance;
    }
	
	private void initView() {
		btnBack = (Button) findViewById(R.id.btn_back);
		btnFetch = (Button) findViewById(R.id.btn_fetch);
		edtInput = (EditText) findViewById(R.id.input_text);
		mMccBtn = (Button) findViewById(R.id.btn_mcc);
		mMccText = (EditText) findViewById(R.id.mcc_text);
		edtBind = (EditText) findViewById(R.id.input_bind);

		btnBack.setOnClickListener(this);
		mMccBtn.setOnClickListener(this);
		btnFetch.setOnClickListener(this);
		edtInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() == 0) {
					edtInput.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtInput.setTextColor(getResources().getColor(R.color.white));
				}
			}
			
		});
		edtBind.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() == 0) {
					edtBind.setTextColor(getResources().getColor(R.color.login_input_font));
				} else {
					edtBind.setTextColor(getResources().getColor(R.color.white));
				}
			}
			
		});
	}
	
	private void sendAccessCode(final String wowtalkId, final String destination) {
        mMsgBox.showWait();
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(Withdraw1Activity.this).fRetrievePassword(wowtalkId, destination);
			}
			@Override
			protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
				switch (result) {
                case ErrorCode.OK:
                    Intent successIntent = new Intent(Withdraw1Activity.this, WithdrawSuccessActivity.class);
                    successIntent.putExtra(WOWTALK_ID, wowtalkId);
                    startActivity(successIntent);
                    break;
                case ErrorCode.USER_NOT_EXISTS:
                    mMsgBox.toast(R.string.withdraw_user_not_exists);
                    break;
                case ErrorCode.FORGET_PWD_EMAIL_PHONE_NOT_BOUND:
                    Intent failIntent = new Intent(Withdraw1Activity.this, WithdrawFailActivity.class);
                    startActivity(failIntent);
                    finish();
                    break;
                case ErrorCode.FORGET_PWD_EMAIL_NOT_BOUND:
                    mMsgBox.toast(R.string.withdraw_email_not_bound);
                    break;
                case ErrorCode.FORGET_PWD_PHONE_NOT_BOUND:
                    mMsgBox.toast(R.string.withdraw_phone_not_bound);
                    break;
                case ErrorCode.FORGET_PWD_EMAIL_NOT_MATCH:
                    mMsgBox.toast(R.string.withdraw_email_not_match);
                    break;
                case ErrorCode.FORGET_PWD_PHONE_NOT_MATCH:
                    mMsgBox.toast(R.string.withdraw_phone_not_match);
                    break;
                default:
                    mMsgBox.toast(R.string.withdraw_password_fail);
                    break;
                }
			}
		}.execute((Void)null);
	}
	
	private void withdrawPassword(final String wowtalkId) {
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(Withdraw1Activity.this).fQueryBinds(wowtalkId, binds);
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if (result == ErrorCode.OK) {
					Intent intent = new Intent();
					Log.e("withdraw1activity : emailaddress = "  + binds[0] + ", phonenumber = " + binds[1]);
					if ((binds[0] == null || binds[0].equals("")) && (binds[1] == null || binds[1].equals(""))) {
						intent.setClass(Withdraw1Activity.this, WithdrawFailActivity.class);
					} else {
						intent.setClass(Withdraw1Activity.this, Withdraw2Activity.class);
						intent.putExtra("binds", binds);
						intent.putExtra(WOWTALK_ID, wowtalkId);
					}
					startActivity(intent);
				}
			}
			
		}.execute((Void)null);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_back:
			finish();
			break;
		case R.id.btn_mcc:
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
		case R.id.btn_fetch:
			//Intent intent = new Intent(Withdraw1Activity.this, Withdraw2Activity.class);
			//startActivity(intent);
			final String wowtalkId = edtInput.getText().toString();
			String destination = edtBind.getText().toString();
            if (wowtalkId.equals("") || destination.equals("")) {
                mMsgBox.toast(R.string.login_input_id_and_bind);
            } else if (destination.contains("@") && !Utils.verifyEmail(destination)) {
                mMsgBox.toast(R.string.bind_email_format_error_msg);
            }else if (!destination.contains("@") && !Utils.verifyPhoneNumber("+" + mCurrMcc + destination)) {
                mMsgBox.toast(R.string.bind_phone_format_error_msg);
            }else {
                if (!destination.contains("@")) {
                    destination = "+" + mCurrMcc + destination.trim();
                }
                sendAccessCode(wowtalkId, destination);
            }
			break;
		default:
			break;
		}
	}

    private void updateMcc(int mcc, String title) {
        if (mcc != mCurrMcc) {
            mCurrMcc = mcc;
            mMccText.setText("+" + mCurrMcc);
            mMccBtn.setText(title);
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.withdraw_pwd1);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        instance = this;
        mMsgBox = new MessageBox(this);
        initView();
        updateMcc(86, "China");
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

