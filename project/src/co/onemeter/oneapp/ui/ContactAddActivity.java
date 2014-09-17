package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import com.zxing.activity.CaptureActivity;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

public class ContactAddActivity extends Activity implements OnClickListener{
	
	private ImageButton btnTitleClose;
	
	private TextView txtSearch;
    private TextView txtScanQrCode;
	private TextView txtImport;
	private TextView txtCreateGroup;
	private TextView txtOfficial;
    private TextView txtExternal;
    private MessageBox mMsgBox;
	
	private void initView() {
		btnTitleClose = (ImageButton) findViewById(R.id.title_close);
		txtSearch = (TextView) findViewById(R.id.add_search);
        txtScanQrCode = (TextView) findViewById(R.id.scan_qr_code);
		txtImport = (TextView) findViewById(R.id.add_import);
		txtCreateGroup = (TextView) findViewById(R.id.add_create_group);
		txtOfficial = (TextView) findViewById(R.id.add_offical);
        txtExternal = (TextView) findViewById(R.id.add_external);
		
		btnTitleClose.setOnClickListener(this);
		txtSearch.setOnClickListener(this);
        txtScanQrCode.setOnClickListener(this);
		txtImport.setOnClickListener(this);
		txtCreateGroup.setOnClickListener(this);
		txtOfficial.setOnClickListener(this);
        txtExternal.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_close:
                finish();
                break;
            case R.id.add_search:
                Intent searchIntent = new Intent(ContactAddActivity.this, ContactSearchActivity.class);
                startActivity(searchIntent);
                break;
            case R.id.scan_qr_code:
                Intent scanIntent = new Intent(ContactAddActivity.this, ScanQRCodeActivity.class);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_WITH_LAYOUT,true);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_LAYOUT_ID,R.layout.scan_qr_code_layout);
                scanIntent.putExtra(CaptureActivity.ACTIVITY_ARG_FINISH_AFTER_DECODE,false);
                startActivity(scanIntent);
                break;
            case R.id.add_import:
                Intent localIntent = new Intent(ContactAddActivity.this, LocalContactsActivity.class);
                startActivity(localIntent);
                break;
            case R.id.add_create_group:
                Intent createIntent = new Intent(ContactAddActivity.this, CreateGroupActivity.class);
                startActivity(createIntent);
                break;
            case R.id.add_offical:
                Intent publicIntent = new Intent(ContactAddActivity.this, PublicSearchActivity.class);
                startActivity(publicIntent);
                break;
            case R.id.add_external:
                Intent externalIntent = new Intent(ContactAddActivity.this, ExternalSearchActivity.class);
                startActivity(externalIntent);
                break;
            default:
                mMsgBox.toast(R.string.not_implemented);
                break;
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_add_page);

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
