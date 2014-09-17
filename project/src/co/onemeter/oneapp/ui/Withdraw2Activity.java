package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

public class Withdraw2Activity extends Activity implements OnClickListener{
	
	private TextView txtEmail;
	private TextView txtPhone;
	private ImageView imgDivider;
	private Button btnBack;
	private Button btnNext;
	private String[] binds;
	private int bindsSize = 0;
	
	private boolean _isbindEmail;
	private String wowtalkId;
    private MessageBox mMsgBox;
	
	private void initView() {
		txtEmail = (TextView) findViewById(R.id.txt_email);
		txtPhone = (TextView) findViewById(R.id.txt_phone);
		imgDivider = (ImageView) findViewById(R.id.img_divider);
		btnBack = (Button) findViewById(R.id.btn_back);
		btnNext = (Button) findViewById(R.id.btn_next);
		txtEmail.setText(String.format(getResources().getString(R.string.email_with_risk), binds[0]));
		txtPhone.setText(String.format(getResources().getString(R.string.phone_with_risk), binds[1]));
		txtEmail.setOnClickListener(this);
		txtPhone.setOnClickListener(this);
		btnBack.setOnClickListener(this);
		btnNext.setOnClickListener(this);
		if (binds[0] == null || binds[0].equals("")) {
			txtEmail.setVisibility(View.GONE);
		} else {
			bindsSize++;
		}

		if (binds[1] == null || binds[1].equals("")) {
			txtPhone.setVisibility(View.GONE);
		} else {
			bindsSize++;
		}
		
		if (bindsSize == 2) {
			_isbindEmail = true;
			txtEmail.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_selected, 0, 0, 0);
			txtPhone.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_unselected, 0, 0, 0);
		} else {
			imgDivider.setVisibility(View.GONE);
			if (binds[0] != null && !binds[0].equals("")) {
				txtEmail.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_selected, 0, 0, 0);
				_isbindEmail = true;
			} else {
				txtPhone.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_selected, 0, 0, 0);
				_isbindEmail = false;
			}
		}
	}
	
	private void fetchVertification() {
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int method;
				if (_isbindEmail) {
					method = 0;
				} else {
					method = 1;
				}
				//return WowTalkWebServerIF.getInstance(Withdraw2Activity.this).fRetrievePassword(wowtalkId, method);
				return null;
			}
			@Override
			protected void onPostExecute(Integer result) {
				if (result == ErrorCode.OK) {
					Intent intent = new Intent(Withdraw2Activity.this, WithdrawSuccessActivity.class);
					intent.putExtra(Withdraw1Activity.WOWTALK_ID, wowtalkId);
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
		case R.id.btn_next:
			fetchVertification();
			break;
		case R.id.txt_email:
			if (bindsSize == 2) {
				_isbindEmail = true;
				txtEmail.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_selected, 0, 0, 0);
				txtPhone.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_unselected, 0, 0, 0);
			}
			break;
		case R.id.txt_phone:
			if (bindsSize == 2) {
				_isbindEmail = false;
				txtEmail.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_unselected, 0, 0, 0);
				txtPhone.setCompoundDrawablesWithIntrinsicBounds(R.drawable.forgot_selected, 0, 0, 0);
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.withdraw_pwd2);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        if (getIntent() == null)
			return;
        mMsgBox = new MessageBox(this);
		binds = getIntent().getStringArrayExtra("binds");
		wowtalkId = getIntent().getStringExtra(Withdraw1Activity.WOWTALK_ID);
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

