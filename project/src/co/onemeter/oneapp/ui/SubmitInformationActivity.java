package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.R;
public class SubmitInformationActivity extends Activity implements OnClickListener {
	private ImageButton submit_left_button;
	private TextView submit_right_button;
	private Button submit_confirm;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit_information);
		initView();

        
	}

	private void initView() {
		submit_left_button = (ImageButton) findViewById(R.id.submit_left_button);
		submit_right_button = (TextView) findViewById(R.id.submit_right_button);
		submit_confirm = (Button) findViewById(R.id.submit_confirm);
		
		submit_left_button.setOnClickListener(this);
		submit_right_button.setOnClickListener(this);
		submit_confirm.setOnClickListener(this);
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.submit_left_button:
			finish();
			break;
		case R.id.submit_right_button:
			break;
		case R.id.submit_confirm:
			break;
		}
		
	}


}
