package co.onemeter.oneapp.ui;

import org.wowtalk.api.Buddy;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SexSettingActivity extends Activity implements OnClickListener{
	
	//返回
	private ImageButton title_back;
	private TextView textView_myinfo_back;
	
	//男女性别
	private RelativeLayout layout_setting_male;
	private RelativeLayout layout_setting_female;
	
	private TextView edt_value_male;
	private TextView edt_value_female;
	
	//选中标志
	private ImageButton imageButton_male;
	private ImageButton imageButton_female;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sex_setting);
		getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
		initView();
	}
	
	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
		textView_myinfo_back = (TextView) findViewById(R.id.textView_myinfo_back);
		
		layout_setting_male = (RelativeLayout) findViewById(R.id.layout_setting_male);
		layout_setting_female = (RelativeLayout) findViewById(R.id.layout_setting_female);
		
		edt_value_male = (TextView) findViewById(R.id.edt_value_male);
		edt_value_female = (TextView) findViewById(R.id.edt_value_female);
		
		imageButton_male = (ImageButton) findViewById(R.id.imageButton_male);
		imageButton_female = (ImageButton) findViewById(R.id.imageButton_female);
		
		title_back.setOnClickListener(this);
		textView_myinfo_back.setOnClickListener(this);
//		layout_setting_male.setOnClickListener(this);
//		layout_setting_female.setOnClickListener(this);
		
		edt_value_male.setOnClickListener(this);
		edt_value_female.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back://返回
		case R.id.textView_myinfo_back:
			finish();
			break;
			
		case R.id.edt_value_male://男性
			imageButton_male.setVisibility(View.VISIBLE);
			imageButton_female.setVisibility(View.GONE);
			
//			new Thread(new Runnable() {
//				
//				@Override
//				public void run() {
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					
//					Intent intentMale = new Intent();
//					intentMale.putExtra("sex", Buddy.SEX_MALE);
//					setResult(RESULT_OK, intentMale);
//					finish();
//					
//				}
//			}).start();
			
			Intent intentMale = new Intent();
			intentMale.putExtra("sex", Buddy.SEX_MALE);
			setResult(RESULT_OK, intentMale);
			finish();
			
			break;
			
		case R.id.edt_value_female://女性
			imageButton_male.setVisibility(View.GONE);
			imageButton_female.setVisibility(View.VISIBLE);
			
//			new Thread(new Runnable() {
//				
//				@Override
//				public void run() {
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					
//					Intent intentFemale = new Intent();
//					intentFemale.putExtra("sex", Buddy.SEX_FEMALE);
//					setResult(RESULT_OK, intentFemale);
//					finish();
//					
//				}
//			}).start();
			
			Intent intentFemale = new Intent();
			intentFemale.putExtra("sex", Buddy.SEX_FEMALE);
			setResult(RESULT_OK, intentFemale);
			finish();
			
			break;
		default:
			break;
		}
		
	}
	
	
	
}
