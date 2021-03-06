package co.onemeter.oneapp.ui.datepicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

public class DatePickerActivity extends Activity{

	PickerView year_pv;//年
	PickerView month_pv;//月
	PickerView day_pv;//日
	
	List<String> years = new ArrayList<String>();
	List<String> months = new ArrayList<String>();
	List<String> days = new ArrayList<String>();
	
	private int year = 0000;
	private int month = 00;
	private int day = 00;
	
	private TextView textView_date_picker_ok;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_date_picker);
		
		textView_date_picker_ok = (TextView) findViewById(R.id.textView_date_picker_ok);
		textView_date_picker_ok.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				
				bundle.putInt("year", year);
				bundle.putInt("month", month);
				bundle.putInt("day", day);
				intent.putExtras(bundle);
				
				setResult(RESULT_OK, intent);
				
				DatePickerActivity.this.finish();
				
			}
		});
		
		year_pv = (PickerView) findViewById(R.id.year_pv);//年
		month_pv = (PickerView) findViewById(R.id.month_pv);//月
		day_pv = (PickerView) findViewById(R.id.day_pv);//日

		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd");     
		Date   curDate   =   new   Date(System.currentTimeMillis());//获取当前时间     
		String   str   =   formatter.format(curDate); 
		int curYear = Integer.valueOf(str.substring(0, 4));
		
		
		//添加数据源
		
		//年
		for (int i = 1900; i <= curYear; i++)
		{
			years.add("" + i);
		}
		
		//月
		for (int i = 1; i <= 12; i++)
		{
			months.add(i < 10 ? "0" + i : "" + i);
		}
		
		//日
		for (int i = 1; i <= 30; i++)
		{
			days.add(i < 10 ? "0" + i : "" + i);
		}
		
		
		year_pv.setData(years);
		year_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
//				Toast.makeText(DatePickerActivity.this, "选择了 " + text + " 年",
//						Toast.LENGTH_SHORT).show();
				year = Integer.valueOf(text);
			}
		});
		
		month_pv.setData(months);
		month_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
//				Toast.makeText(DatePickerActivity.this, "选择了 " + text + " 月",
//						Toast.LENGTH_SHORT).show();
				
				month = Integer.valueOf(text);
				
				if (year%4 == 0 && year%100!=0 || year%400 == 0) {//闰年(2月29天)
					
					if (month==1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {//31天
						days.clear();
						//日
						for (int i = 1; i <= 31; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					} else if (month == 2){//29
						days.clear();
						//日
						for (int i = 1; i <= 29; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					} else {//30
						for (int i = 1; i <= 30; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					}
					
				} else {//平年(2月28天)
					if (month==1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {//31
						days.clear();
						//日
						for (int i = 1; i <= 31; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					} else if (month == 2) {//28
						days.clear();
						//日
						for (int i = 1; i <= 28; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					} else {//30
						days.clear();
						//日
						for (int i = 1; i <= 30; i++)
						{
							days.add(i < 10 ? "0" + i : "" + i);
						}
					}
				}
				day_pv.setData(days);
				
				//如果没有滚动天数，保持当前天数，当月份的天数小于当前天数，重置为01天
				int curDay = day -1;
				if (days.size() >= curDay) {
					day_pv.setSelected(curDay);
				} else {
					day_pv.setSelected(0);
				}
				
				month--;
			}
		});
		
		
		day_pv.setData(days);
		day_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
//				Toast.makeText(DatePickerActivity.this, "选择了 " + text + " 日",
//						Toast.LENGTH_SHORT).show();
				
				day = Integer.valueOf(text);
			}
		});
		
		
		//初始化年月日的值，默认为总链表长度2分之1位置的值
		
		
		Bundle bundle = getIntent().getExtras();
		
		if (bundle == null) {

            //初始化生日
            year = 2000;
            month = 00;
            day = 01;

			year_pv.setSelected(100);
			month_pv.setSelected(0);
			day_pv.setSelected(0);
		} else {

			year = bundle.getInt("year");
			month = bundle.getInt("month")-1;
			day = bundle.getInt("day");
			
			year_pv.setSelected(year-1900);
			month_pv.setSelected(month);
			day_pv.setSelected(day-1);
		}
	}
	
	
	@Override
	protected void onDestroy() {
		if (year_pv != null) {
			year_pv = null;
		}
		
		if (month_pv != null) {
			month_pv = null;
		}
		
		if (day_pv != null) {
			day_pv = null;
		}
		super.onDestroy();
	}
}
