package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.util.Calendar;
import java.util.Date;

/**
 * 大量的注释部分是利用自定义的日期选择控件，由于修改了activity的theme，3.0以上系统能都显示很友好的日期选择控件，暂时放弃使用自定义的日期选择控件
 * @author jacky
 *this activity is used for editting class' info
 */
public class ClassInfoEditActivity extends Activity implements View.OnClickListener, View.OnTouchListener {


	public static final String TERM = "term";
	public static final String GRADE = "grade";
	public static final String SUBJECT = "subject";
	public static final String DATE = "date";
	public static final String TIME = "time";
	public static final String PLACE = "place";
	public static final String LENGTH = "length";
	
	
//	public static final int REQ_START_DATE=1;//开始日期请求码
//	public static final int REQ_END_DATE=2;//结束日期请求码
//	public static final int REQ_START_TIME=3;//开始时间请求码
//	public static final int REQ_CLASS_TIME=4;//上课时长请求码
//
	private String classId;

	private EditText dtTerm;
	private EditText dtGrade;
	private EditText dtSubject;
	private DatePicker dpDate;
	private DatePicker dpEndDate;
	private TimePicker tpTime;
	private TimePicker tpLength;
	private EditText dtPlace;
//	
//	//开始日期
//	private LinearLayout layout_classinfo_edit_start_date;
//	private TextView textVIew_start_date;
//	
//	//结束日期
//	private LinearLayout layout_classinfo_edit_end_date;
//	private TextView textView_end_date;
//	
//	//开始时间
//	private LinearLayout layout_classinfo_edit_start_time;
//	private TextView textView_start_time;
//	
//	//上课时长
//	private LinearLayout layout_classinfo_edit_class_time;
//	private TextView textView_class_time;
//	
//	
//	private int start_date_year;
//	private int start_date_month;
//	private int start_date_day;
//	
//	
//	private int end_date_year;
//	private int end_date_month;
//	private int end_date_day;
//	
//	private int start_time_hour = 0;
//	private int start_time_minute = 0;
//	
//	private int class_time_hour = 0;
//	private int class_time_minute = 0;
	

	private Database mDBHelper;
	private GroupChatRoom classroom;
	private MessageBox mMsgBox;
	private LinearLayout layout_lesinfo_time;
	private LinearLayout layout_lesinfo_length;

	
	private TextWatcher textwatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if(s.length() >= 20){
				Toast.makeText(ClassInfoEditActivity.this, "最多输入20个字符", Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_classinfo_edit);

		initView();
	}

	private void initView() {
		Intent intent = getIntent();
		classroom = intent.getParcelableExtra("class");
		String reDate = intent.getStringExtra("tvDate");
		String reTime = intent.getStringExtra("tvTime");

		if(TextUtils.isEmpty(reDate)){
			classId = intent.getStringExtra("classId");
		}
//		classId = intent.getStringExtra("classId");
		else{
			classId = classroom.groupID;
		}
				
		
//		Log.d("-------------classroom---------------", classroom+"");

		//android.util.Log.i("-->>", classroom.groupID);

		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);//学期
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);//年级
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);//学科
		
		dpDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_date);//开始日期
		dpEndDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_enddate);//结束日期
		tpTime = (TimePicker) findViewById(R.id.timePicker_lesinfo_time);//开始上课时间
		tpLength = (TimePicker) findViewById(R.id.timePicker_lesinfo_length);//上课时长
		
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);//地点
		
		layout_lesinfo_time = (LinearLayout) findViewById(R.id.layout_lesinfo_time);
		layout_lesinfo_length = (LinearLayout) findViewById(R.id.layout_lesinfo_length);
		tpTime.setIs24HourView(true);
		
		
        dpDate.setOnClickListener(this);
        dpEndDate.setOnClickListener(this);
        
        
//        layout_classinfo_edit_start_date = (LinearLayout) findViewById(R.id.layout_classinfo_edit_start_date);
//        textVIew_start_date = (TextView) findViewById(R.id.textVIew_start_date);
//        
//        layout_classinfo_edit_end_date = (LinearLayout) findViewById(R.id.layout_classinfo_edit_end_date);
//        textView_end_date = (TextView) findViewById(R.id.textView_end_date);
//        
//        layout_classinfo_edit_start_time = (LinearLayout) findViewById(R.id.layout_classinfo_edit_start_time);
//        textView_start_time = (TextView) findViewById(R.id.textView_start_time);
//        
//        layout_classinfo_edit_class_time = (LinearLayout) findViewById(R.id.layout_classinfo_edit_class_time);
//        textView_class_time = (TextView) findViewById(R.id.textView_class_time);
//        
//        layout_classinfo_edit_start_date.setOnClickListener(this);
//        layout_classinfo_edit_end_date.setOnClickListener(this);
//        layout_classinfo_edit_start_time.setOnClickListener(this);
//        layout_classinfo_edit_class_time.setOnClickListener(this);
        
        //分解传过来的值
		if(reDate !=null && !TextUtils.isEmpty(reDate)){
			String[] dates = reDate.split(" - ");//日期
			String[] startDate = dates[0].split("-");//开始日期
			String[] endDate = dates[1].split("-");//结束日期
			
			String[] times = reTime.split(" - ");//时间
			String[] startTime = times[0].split(":");//上课时间
			String[] endTime = times[1].split(":");//结束时间
			
			int timeLengthTag = (Integer.parseInt(endTime[0])*3600 + Integer.parseInt(endTime[1])*60 - 
					Integer.parseInt(startTime[0])*3600 - Integer.parseInt(startTime[1])*60);//结束-开始，上课时长的时间戳
			
			int hourLength = timeLengthTag/3600;//上课的小时
			int minuteLength = timeLengthTag % 3600 / 60;//上课的分钟
			
			
			
//			start_date_year = Integer.parseInt(startDate[0]);
//			start_date_month = Integer.parseInt(startDate[1]);
//			start_date_day = Integer.parseInt(startDate[2]);
//			
//			String start_date = start_date_year + "年" + start_date_month + "月" + start_date_day + "日";
//			textVIew_start_date.setText(start_date);
//			
//			end_date_year = Integer.parseInt(endDate[0]);
//			end_date_month = Integer.parseInt(endDate[1]);
//			end_date_day = Integer.parseInt(endDate[2]);
//			
//			String end_date = end_date_year + "年" + end_date_month + "月" + end_date_day + "日";
//			textView_end_date.setText(end_date);
//			
//			start_time_hour = Integer.parseInt(startTime[0]);
//			start_time_minute = Integer.parseInt(startTime[1]);
//			
//			String start_time = start_time_hour + "点" + start_time_minute + "分";
//			textView_start_time.setText(start_time);
//			
//			class_time_hour = hourLength;
//			class_time_minute = minuteLength;
//			
//			String class_time = start_time_minute + "点" + class_time_minute + "分";
//			textView_class_time.setText(class_time);
//			
//			if (class_time_hour > 0) {
//				layout_classinfo_edit_start_time.setVisibility(View.GONE);
//		        layout_classinfo_edit_class_time.setVisibility(View.GONE);
//			}
			
			
			dpDate.init(Integer.parseInt(startDate[0]), Integer.parseInt(startDate[1]) - 1, Integer.parseInt(startDate[2]), null);
	        dpEndDate.init(Integer.parseInt(endDate[0]), Integer.parseInt(endDate[1]) - 1, Integer.parseInt(endDate[2]), null);
	        
	        tpTime.setCurrentHour(Integer.parseInt(startTime[0]));
	        tpTime.setCurrentMinute(Integer.parseInt(startTime[1]));
	        
	        tpLength.setCurrentHour(hourLength);
	        tpLength.setCurrentMinute(minuteLength);
	        
	        if(String.valueOf(tpTime.getCurrentHour()) != null){
	        	tpTime.setEnabled(false);
	        	tpLength.setEnabled(false);
	        	layout_lesinfo_time.setVisibility(View.GONE);
	        	layout_lesinfo_length.setVisibility(View.GONE);
	        }
	        
		}else{
			
//			SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd");     
//			Date   curDate   =   new   Date(System.currentTimeMillis());//获取当前时间     
//			String   str   =   formatter.format(curDate); 
//			int curYear = Integer.valueOf(str.substring(0, 4));
//			int curMonth = Integer.valueOf(str.substring(5, 7));
//			int curDay = Integer.valueOf(str.substring(8));
			
			
//			start_date_year = curYear;
//			start_date_month = curMonth;
//			start_date_day = curDay;		
//			
//			end_date_year = curYear;
//			end_date_month = curMonth;
//			end_date_day = curDay;	
//			
//			formatter = new SimpleDateFormat("HH-mm");
//			String time = formatter.format(curDate); 
//			int curHour = Integer.valueOf(time.substring(0, 2));
//			int curMinute = Integer.valueOf(time.substring(3));
//			
//			start_time_hour = curHour;
//			start_time_minute = curMinute;
//			
//			class_time_hour = 1;
//			class_time_minute = 0;
			
			
			
			
			
			tpLength.setCurrentHour(1);
	        tpLength.setCurrentMinute(0);
		}
		
		mMsgBox = new MessageBox(this);
		mDBHelper = new Database(this);
		AQuery q = new AQuery(this);
		
		
		q.find(R.id.cancel).clicked(this);
		q.find(R.id.save).clicked(this);

        tpTime.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                closeInputBoard();
            }
        });

        tpLength.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                closeInputBoard();
            }
        });
        

			tpTime.setIs24HourView(true);
			tpLength.setIs24HourView(true);
			
			
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);

			dtTerm.addTextChangedListener(textwatcher);
			dtSubject.addTextChangedListener(textwatcher);
			dtGrade.addTextChangedListener(textwatcher);
			dtPlace.addTextChangedListener(textwatcher);

			if(!TextUtils.isEmpty(reDate)){
				String[] infos = ClassDetailActivity.getStrsByComma(classroom.description);
				if(null != infos && infos.length == 4){
					dtTerm.setText(infos[0]);
					dtGrade.setText(infos[1]);
					dtSubject.setText(infos[2]);
					dtPlace.setText(infos[3]);
					
				}else{
					Date date = new Date();
					tpTime.setCurrentHour(date.getHours());
					tpTime.setCurrentMinute(date.getMinutes());
				}
			}
        findViewById(R.id.lay_classinfo_main).setOnTouchListener(this);
        findViewById(R.id.scrollView_classinfo).setOnTouchListener(this);
        findViewById(R.id.datePicker_lesinfo_date).setOnTouchListener(this);
	}

    private void closeInputBoard(){
        View view = getWindow().peekDecorView();
        if(view != null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
			
//		case R.id.layout_classinfo_edit_start_date://选择开始日期
//			Intent startDateIntent = new Intent(ClassInfoEditActivity.this,DatePickerActivity.class);
//			
//			Bundle startDate = new Bundle();
//			startDate.putInt("year", start_date_year);
//			startDate.putInt("month", start_date_month);
//			startDate.putInt("day", start_date_day);
//			startDateIntent.putExtras(startDate);
//			
//			startActivityForResult(startDateIntent, REQ_START_DATE);
//			break;
//			
//		case R.id.layout_classinfo_edit_end_date://选择结束日期
//			Intent endDateIntent = new Intent(ClassInfoEditActivity.this,DatePickerActivity.class);
//			
//			Bundle endDate = new Bundle();
//			endDate.putInt("year", end_date_year);
//			endDate.putInt("month", end_date_month);
//			endDate.putInt("day", end_date_day);
//			endDateIntent.putExtras(endDate);
//			
//			startActivityForResult(endDateIntent, REQ_END_DATE);
//			break;
//			
//		case R.id.layout_classinfo_edit_start_time://选择上课时间
//			Intent startTimeIntent = new Intent(ClassInfoEditActivity.this,TimerPickerActivity.class);
//			
//			Bundle startTime = new Bundle();
//			startTime.putInt("hour", start_time_hour);
//			startTime.putInt("minute", start_time_minute);
//			startTimeIntent.putExtras(startTime);
//			
//			startActivityForResult(startTimeIntent, REQ_START_TIME);
//			break;
//			
//		case R.id.layout_classinfo_edit_class_time://选择上课时长
//			Intent classTimeIntent = new Intent(ClassInfoEditActivity.this,TimerPickerActivity.class);
//			
//			Bundle classTime = new Bundle();
//			classTime.putInt("hour", class_time_hour);
//			classTime.putInt("minute", class_time_minute);			
//			classTimeIntent.putExtras(classTime);
//			
//			startActivityForResult(classTimeIntent, REQ_CLASS_TIME);
//			break;
			
		case R.id.save:
			modifyClassInfo(classId);
			break;
        case R.id.datePicker_lesinfo_date:
            closeInputBoard();
            break;
        case R.id.datePicker_lesinfo_enddate:
        	closeInputBoard();
        	break;
		default:
			break;
		}
	}
	
	private void modifyClassInfo(final String cId){
		if(classroom == null){
			mMsgBox.toast(R.string.class_err_denied, 500);
			return;
		}
		classroom.isEditable = true;
		
		
		final Calendar resultTime = Calendar.getInstance();
		final Calendar resultEndTime = Calendar.getInstance();
		
		//resultTime.setTimeZone(TimeZone.getTimeZone("GMT"));
		resultTime.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth());
		resultEndTime.set(dpEndDate.getYear(), dpEndDate.getMonth(), dpEndDate.getDayOfMonth());
//		
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		
//		resultTime.set(start_date_year, start_date_month-1, start_date_day);
//		resultEndTime.set(end_date_year, end_date_month-1, end_date_day);
		
		
		long firstlesTime = getIntent().getLongExtra("firstlesdate", -1);
		if(firstlesTime != -1){
		    if( firstlesTime < resultTime.getTimeInMillis() / 1000 ){
			    if(!mMsgBox.isWaitShowing()){
				    mMsgBox.toast("开班时间不得晚于课程时间！");
			    }
			    return;
		    }
		}
		if(resultEndTime.getTimeInMillis() <= resultTime.getTimeInMillis()){
			if(!mMsgBox.isWaitShowing()){
			    mMsgBox.toast("结束时间不得早于开始时间！");
		    }
		    return;
		}
		
		//开始上课时间
		final int hour = tpTime.getCurrentHour();
		final int minite = tpTime.getCurrentMinute();
		
		//上课时长
		final int hourLength = tpLength.getCurrentHour();
		final int miniteLength = tpLength.getCurrentMinute();
		
		classroom.description = dtTerm.getText().toString() 
				+ Constants.COMMA + dtGrade.getText().toString()
				+ Constants.COMMA + dtSubject.getText().toString()
				+ Constants.COMMA + dtPlace.getText().toString();
		
		//当天0点的时间
		final Calendar startDay = Calendar.getInstance();//当天
		final Calendar endDay = Calendar.getInstance();//结束
		
		long currentDayTimps = Utils.getDayStampMoveMillis();
		
		int classTimps = hour * 3600 + minite * 60;//上课时间
		int classLengthTimps = hourLength * 3600 + miniteLength * 60;//上课时长时间戳
		
		final long startClassTimps = currentDayTimps + classTimps;//开始的时间戳
		
		final long endClassTimps = startClassTimps + classLengthTimps;//结束的时间戳
		
		startDay.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth());
		endDay.set(dpEndDate.getYear(), dpEndDate.getMonth(), dpEndDate.getDayOfMonth());
		
//		startDay.set(start_date_year, start_date_month-1, start_date_day);
//		
//		endDay.set(end_date_year, end_date_month-1, end_date_day);
		
		mMsgBox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				mDBHelper.updateGroupChatRoom(classroom);
				int errno = WowTalkWebServerIF.getInstance(ClassInfoEditActivity.this)
						.fModify_classInfo(classroom,cId,startDay.getTimeInMillis()/1000, endDay.getTimeInMillis()/1000, startClassTimps, endClassTimps);
				return errno;
			}
			@Override
			protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
				if (result == ErrorCode.OK) {
					// update the display name of chatmessages.
					mDBHelper.updateChatMessageDisplayNameWithUser(classroom.groupID, classroom.groupNameOriginal);
					Intent data = new Intent();
					data.putExtra(TERM, dtTerm.getText().toString());
					data.putExtra(GRADE, dtGrade.getText().toString());
					data.putExtra(SUBJECT, dtSubject.getText().toString());
					data.putExtra(PLACE, dtPlace.getText().toString());

					setResult(RESULT_OK, data);
					finish();
				} else if (result == ErrorCode.ERR_OPERATION_DENIED) {
					mMsgBox.toast(R.string.class_err_denied, 500);
				}
			}
			
		});
	}
	
	
	
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		
//		
//		if (resultCode == RESULT_OK) {
//			switch (requestCode) {
//			case REQ_START_DATE://开始日期请求码
//				
//				Bundle start_date = data.getExtras();
//            	if (start_date != null) {
//            		start_date_year = start_date.getInt("year");
//        			start_date_month = start_date.getInt("month")+1;
//        			start_date_day = start_date.getInt("day");
//
//        			textVIew_start_date.setText(start_date_year + "年" + start_date_month + "月" + start_date_day + "日");
//            	}
//            	
//				break;
//				
//			case REQ_END_DATE://结束日期请求码
//				
//				Bundle end_date = data.getExtras();
//            	if (end_date != null) {
//            		end_date_year = end_date.getInt("year");
//        			end_date_month = end_date.getInt("month")+1;
//        			end_date_day = end_date.getInt("day");
//        			
//        			textView_end_date.setText(end_date_year + "年" + end_date_month + "月" + end_date_day + "日");
//            	}
//            	
//				break;
//				
//			case REQ_START_TIME://开始时间请求码
//				
//				Bundle start_time = data.getExtras();
//            	if (start_time != null) {
//            		start_time_hour = start_time.getInt("hour");
//        			start_time_minute = start_time.getInt("minute");
//        			
//        			textView_start_time.setText(start_time_hour + "点" + start_time_minute + "分");
//            	}
//            	
//				break;
//				
//			case REQ_CLASS_TIME://上课时长请求码
//				
//				Bundle class_time = data.getExtras();
//            	if (class_time != null) {
//            		class_time_hour = class_time.getInt("hour");
//        			class_time_minute = class_time.getInt("minute");
//        			
//        			textView_class_time.setText(start_time_minute + "时" + class_time_minute + "分");
//            	}
//            	
//				break;
//
//			default:
//				break;
//			}
//		}
//		super.onActivityResult(requestCode, resultCode, data);
//	}

	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		mDBHelper.close();
		if (mMsgBox != null) {
			mMsgBox = null;
		}
		
	}

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()){
            case R.id.lay_classinfo_main:
                closeInputBoard();
                break;
            case R.id.scrollView_classinfo:
                closeInputBoard();
                break;
            case R.id.datePicker_lesinfo_date:
                closeInputBoard();
                break;
            case R.id.datePicker_lesinfo_enddate:
            	closeInputBoard();
            	break;
        }
        return false;
    }
}
