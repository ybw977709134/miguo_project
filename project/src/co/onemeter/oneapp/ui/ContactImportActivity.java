package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

public class ContactImportActivity extends Activity implements OnClickListener {
	private ImageButton btnTitleBack;
	
	private TextView txtFromPhone;
	private TextView txtFromQQ;
	private TextView txtFromWeiXin;
	private TextView txtFromWeiBo;
    private MessageBox mMsgBox;

	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		
		txtFromPhone = (TextView) findViewById(R.id.from_phone);
		txtFromQQ = (TextView) findViewById(R.id.from_qq);
		txtFromWeiXin = (TextView) findViewById(R.id.from_weixin);
		txtFromWeiBo = (TextView) findViewById(R.id.from_weibo);
		
		btnTitleBack.setOnClickListener(this);
		txtFromPhone.setOnClickListener(this);
		txtFromQQ.setOnClickListener(this);
		txtFromWeiXin.setOnClickListener(this);
		txtFromWeiBo.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.from_phone:
			mMsgBox.toast(R.string.not_implemented);
			break;
		case R.id.from_qq:
			mMsgBox.toast(R.string.not_implemented);
			break;
		case R.id.from_weixin:
			mMsgBox.toast(R.string.not_implemented);
			break;
		case R.id.from_weibo:
			mMsgBox.toast(R.string.not_implemented);
			break;
		default:
			mMsgBox.toast(R.string.not_implemented);
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_import_page);

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
