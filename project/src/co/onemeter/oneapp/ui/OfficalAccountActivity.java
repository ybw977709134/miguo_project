package co.onemeter.oneapp.ui;

import com.umeng.analytics.MobclickAgent;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class OfficalAccountActivity extends Activity implements OnClickListener{
	
	private ImageButton btnTitleBack;
	private TextView txtNewest;
	private TextView txtRecomended;
	private ListView lvNewest;
	private ListView lvRecomended;
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtNewest = (TextView) findViewById(R.id.newest_text);
		txtRecomended = (TextView) findViewById(R.id.recomended_text);
		lvNewest = (ListView) findViewById(R.id.newest_list);
		lvRecomended = (ListView) findViewById(R.id.recomended_list);
		
		txtNewest.setText(getResources().getString(R.string.newest_offical_id));
		txtRecomended.setText(getResources().getString(R.string.offical_recommendations));
		
		btnTitleBack.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;

		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.offical_account);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

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
