package co.onemeter.oneapp.ui;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import co.onemeter.oneapp.R;

/**
 * 班级通知页面。
 * Created by zz on 03/31/2015.
 */
public class ClassNotificationActivity extends Activity implements OnClickListener{
	private ImageButton btn_notice_back;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_notification);
		
		initView();

	}
	
	private void initView(){
		btn_notice_back = (ImageButton) findViewById(R.id.btn_notice_back);
		btn_notice_back.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_notice_back:
			finish();
			break;
		}
		
	}

}
