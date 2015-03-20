package co.onemeter.oneapp.ui;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Discovery page ,created by jacky on 3/20/2015
 * @author jacky
 *
 */
public class DiscoveryActivity extends Activity implements OnClickListener {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_discovery);
		 initView();
	};
	
	private void initView(){
		findViewById(R.id.img_friends).setOnClickListener(this);
		findViewById(R.id.img_events).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.img_friends:
			Intent intent = new Intent(this,EventActivity.class);
			startActivity(intent);
			break;

		case R.id.img_events:
			intent = new Intent(this,TimelineActivity.class);
			startActivity(intent);
		break;
		default:
			break;
		}
	}
	
}
