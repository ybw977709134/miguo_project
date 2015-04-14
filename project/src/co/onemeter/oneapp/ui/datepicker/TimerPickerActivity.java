package co.onemeter.oneapp.ui.datepicker;

import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.widget.PickerView;
import co.onemeter.oneapp.ui.widget.PickerView.onSelectListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class TimerPickerActivity extends Activity{
	
	PickerView hour_pv;//时
	PickerView minute_pv;//分

	
	List<String> hours = new ArrayList<String>();
	List<String> minutes = new ArrayList<String>();

	
	private int hour = 01;
	private int minute = 00;
	
	private TextView textView_time_picker_ok;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_timer_picker);
		
		textView_time_picker_ok = (TextView) findViewById(R.id.textView_time_picker_ok);
		textView_time_picker_ok.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				
				bundle.putInt("hour", hour);
				bundle.putInt("minute", minute);
				intent.putExtras(bundle);
				
				setResult(RESULT_OK, intent);
				
				TimerPickerActivity.this.finish();
				
			}
		});
		
		hour_pv = (PickerView) findViewById(R.id.hour_pv);//时
		minute_pv = (PickerView) findViewById(R.id.minute_pv);//分

		
		//添加数据源
		//时
		for (int i = 0; i <= 23; i++)
		{
			hours.add(i < 10 ? "0" + i : "" + i);
		}
		
		//分
		for (int i = 0; i <= 59; i++)
		{
			minutes.add(i < 10 ? "0" + i : "" + i);
		}
		

		
		
		hour_pv.setData(hours);
		hour_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
				Toast.makeText(TimerPickerActivity.this, "选择了 " + text + " 时",
						Toast.LENGTH_SHORT).show();
				hour = Integer.valueOf(text);
			}
		});
		
		
		minute_pv.setData(minutes);
		minute_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
				Toast.makeText(TimerPickerActivity.this, "选择了 " + text + " 分",
						Toast.LENGTH_SHORT).show();
				
				minute = Integer.valueOf(text);
				
				}
			
		});

		//初始化年月日的值，默认为总链表长度2分之1位置的值
		hour_pv.setSelected(0);
		minute_pv.setSelected(0);
		
		Bundle bundle = getIntent().getExtras();
		
		if (bundle == null) {
			hour_pv.setSelected(0);
			minute_pv.setSelected(0);
		} else {

			hour = bundle.getInt("hour");
			minute = bundle.getInt("minute")-1;

			hour_pv.setSelected(hour);
			minute_pv.setSelected(minute);
		}
		
	}
}
