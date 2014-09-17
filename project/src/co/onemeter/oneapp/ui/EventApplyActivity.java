package co.onemeter.oneapp.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowEventWebServerIF;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;

public class EventApplyActivity extends Activity implements OnClickListener{
    private final static int APPLY_INFO_COUNT = 140;
	private final static int MSG_EVENT_APPLY_SUCCESS = 100;

	private ImageButton btnTitleBack;
	private ImageButton btnTitleConfirm;
	
	private EditText edtApplyInfo;
    private TextView txtIndicator;
	
	private String actId;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_EVENT_APPLY_SUCCESS:
				finish();
				break;
			default:
				break;
			}
		}
	};
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
		edtApplyInfo = (EditText) findViewById(R.id.apply_info_edit);
        txtIndicator = (TextView) findViewById(R.id.txt_indicator);
		
		btnTitleBack.setOnClickListener(this);
		btnTitleConfirm.setOnClickListener(this);
        edtApplyInfo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                int stringCount = String.valueOf(charSequence).length();
                if (stringCount == 0) {
                    btnTitleConfirm.setEnabled(false);
                } else {
                    btnTitleConfirm.setEnabled(true);
                }
                if (stringCount < APPLY_INFO_COUNT) {

                } else {

                }
                txtIndicator.setText(String.valueOf(APPLY_INFO_COUNT - stringCount));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
	}
	
	private void fSubmitApplyInfo() {
		if (edtApplyInfo.getText().toString() == null || edtApplyInfo.getText().toString().equals("")) {
			
		}
		if (actId == null || actId.equals("")) {
			
		}
		final String strApplyInfo = edtApplyInfo.getText().toString();
//		ChatMessage msg = new ChatMessage();
//		msg.chatUserName = actId;
//		msg.messageContent = strApplyInfo;
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				final int errno = WowEventWebServerIF.getInstance(EventApplyActivity.this).fAskForJoining(actId, strApplyInfo);
				if (errno == ErrorCode.OK) {
					Message msg = Message.obtain();
					msg.what = MSG_EVENT_APPLY_SUCCESS;
					mHandler.sendMessage(msg);
				}
			}
			
		}).start();
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.title_confirm:
			fSubmitApplyInfo();
			break;

		default:
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_apply);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		if (getIntent() == null)
			return;
		actId = getIntent().getStringExtra(EventDetailActivity.INTENT_EXTRA_SIGNUP);
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
