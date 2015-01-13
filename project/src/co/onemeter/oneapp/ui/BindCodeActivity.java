package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.util.Timer;
import java.util.TimerTask;

public class BindCodeActivity extends Activity {
	public static final String REQ_INTENT = "req_intent";
	public static final int INTENT_PHONE = 1;
	public static final int INTENT_EMAIL = 2;
    public static final String PHONE_EMAIL_VALUE = "phone_email_value";
    private static final String TAG = "BindCodeActivity";
	private TextView titleText;
	private ImageButton btnTitleBack;
	private EditText edtCode;
	private TextView txtMsg;
	private Button btnDone;
	private int fromIntent;
	private String mPhoneEmailValue;
	private int seconds = 60;
	private Timer timer;
    private MessageBox mMsgBox;

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
            if (seconds >= 0) {
//                  txtMsg.setText(String.format(getResources().getString(R.string.bind_code_msg), String.valueOf(seconds)));
                seconds--;
            } else {
                timer.cancel();
                btnTitleBack.setEnabled(true);
            }
		}
	};
	
	private TimerTask task = new TimerTask() {

		@Override
		public void run() {
			Message msg = Message.obtain();
			msg.what = 0;
			handler.sendMessage(msg);
		}
		
	};
	
	private void bindPhone(final String code) {
        mMsgBox.showWait();

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(BindCodeActivity.this).fVerifyMobile(mPhoneEmailValue, code);
			}

			@Override
			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();

				Log.i(TAG, ", Verify phone, the result code is " + result + ", the phoneNumber is " + mPhoneEmailValue);
				if (result == ErrorCode.OK) {
					final MessageBox box = new MessageBox(BindCodeActivity.this);
					box.toast(R.string.bind_done);
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							box.dismissToast();
							BindPhoneActivity.instance().finish();
							finish();
						}
					}, 1000);
				} else if (result == ErrorCode.PHONE_VERIFICATION_CODE_ERROR) {
					mMsgBox.show(getString(R.string.operation_failed),
							getString(R.string.settings_account_verification_code_wrong));
				} else if (result == ErrorCode.PHONE_USED_BY_OTHERS) {
					mMsgBox.show(getString(R.string.operation_failed),
							getString(R.string.settings_account_phone_used_by_others));
				} else {
					mMsgBox.show(null, getString(R.string.bind_failed));
				}
			}
		});
	}
	
	private void bindEmail(final String code) {
        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowTalkWebServerIF.getInstance(BindCodeActivity.this).fVerifyEmail(mPhoneEmailValue, code);
			}

			@Override
			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();

				Log.i(TAG, ", Verify email, the result code is " + result + ", the email is " + mPhoneEmailValue);
				if (result == ErrorCode.OK) {
					final MessageBox box = new MessageBox(BindCodeActivity.this);
					box.toast(R.string.bind_done);
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							box.dismissToast();
							BindEmailActivity.instance().finish();
							finish();
						}
					}, 1000);
				} else if (result == ErrorCode.EMAIL_VERIFICATION_CODE_ERROR) {
					mMsgBox.show(getString(R.string.operation_failed),
							getString(R.string.settings_account_verification_code_wrong));
				} else if (result == ErrorCode.EMAIL_USED_BY_OTHERS) {
					mMsgBox.show(getString(R.string.operation_failed),
							getString(R.string.settings_account_email_used_by_others));
				} else {
					mMsgBox.show(null, getString(R.string.bind_failed));
				}
			}
		});
	}
	
	private void initView() {
		titleText = (TextView) findViewById(R.id.title_text);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtMsg = (TextView) findViewById(R.id.txt_msg);
		edtCode = (EditText) findViewById(R.id.edt_code);
		btnDone = (Button) findViewById(R.id.btn_done);

        // 这个逻辑是不合理的：初始时 back button 不可用，60sec 或成功后才可用。
        // 如果用户想取消呢？
//		btnTitleBack.setEnabled(false);

		titleText.setText(fromIntent == INTENT_EMAIL ? R.string.bind_email : R.string.bind_phone);
		btnTitleBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
			
		});
		btnDone.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final String code = edtCode.getText().toString();
				if (TextUtils.isEmpty(code)) {
                    mMsgBox.show(getString(R.string.operation_failed), getString(R.string.settings_account_verification_code_empty));
				    return;
                }
				if (fromIntent == INTENT_EMAIL) {
					bindEmail(code);
				}
				if (fromIntent == INTENT_PHONE) {
					bindPhone(code);
				}
			}
			
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bind_code);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Intent intent = getIntent();
		if (intent == null)
			return;
		fromIntent = intent.getIntExtra(REQ_INTENT, 0);
		mPhoneEmailValue = intent.getStringExtra(PHONE_EMAIL_VALUE);
		initView();
		timer = new Timer(true);
		timer.schedule(task, 0, 1000);
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
