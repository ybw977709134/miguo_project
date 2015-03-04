package co.onemeter.oneapp.ui;

import com.androidquery.AQuery;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import co.onemeter.oneapp.R;
/**
 * 摄像头控制页面。
 * Created by zz on 03/03/2015.
 */
public class CameraActivity extends Activity implements OnClickListener{
	private boolean mIsOpenCamera = true;
	private ImageButton btn_isOpenCamera_front;
    private ImageButton btn_isOpenCamera_middle;
    private ImageButton btn_isOpenCamera_back;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		initView();

	}
	
	private void initView(){
		AQuery q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);
		btn_isOpenCamera_front = (ImageButton) findViewById(R.id.btn_isOpenCamera_front);
		btn_isOpenCamera_middle = (ImageButton) findViewById(R.id.btn_isOpenCamera_middle);
		btn_isOpenCamera_back = (ImageButton) findViewById(R.id.btn_isOpenCamera_back);
		btn_isOpenCamera_front.setOnClickListener(this);
		btn_isOpenCamera_middle.setOnClickListener(this);
		btn_isOpenCamera_back.setOnClickListener(this);
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.btn_isOpenCamera_front:
			changeFrontIsOpen(!mIsOpenCamera);
			break;
		case R.id.btn_isOpenCamera_middle:
			changeFrontIsMiddle(!mIsOpenCamera);
			break;
		case R.id.btn_isOpenCamera_back:
			changeFrontIsBack(!mIsOpenCamera);
			break;
		}
		
	}	
	private void changeFrontIsOpen(boolean isOpen) {
    	mIsOpenCamera = isOpen;
    	btn_isOpenCamera_front.setBackgroundResource(mIsOpenCamera ? R.drawable.icon_switch_on : R.drawable.icon_switch_off);
	}
	private void changeFrontIsMiddle(boolean isOpen) {
    	mIsOpenCamera = isOpen;
    	btn_isOpenCamera_middle.setBackgroundResource(mIsOpenCamera ? R.drawable.icon_switch_on : R.drawable.icon_switch_off);
	}
	private void changeFrontIsBack(boolean isOpen) {
    	mIsOpenCamera = isOpen;
    	btn_isOpenCamera_back.setBackgroundResource(mIsOpenCamera ? R.drawable.icon_switch_on : R.drawable.icon_switch_off);
	}

}
