package co.onemeter.oneapp.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * 
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

	private String classId;

	private EditText dtTerm;
	private EditText dtGrade;
	private EditText dtSubject;
	private DatePicker dpDate;
	private DatePicker dpEndDate;
	private TimePicker tpTime;
	private TimePicker tpLength;
	private EditText dtPlace;

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

		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);
		dpDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_date);
		dpEndDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_enddate);
		tpTime = (TimePicker) findViewById(R.id.timePicker_lesinfo_time);
		tpLength = (TimePicker) findViewById(R.id.timePicker_lesinfo_length);
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);
		layout_lesinfo_time = (LinearLayout) findViewById(R.id.layout_lesinfo_time);
		layout_lesinfo_length = (LinearLayout) findViewById(R.id.layout_lesinfo_length);
		tpTime.setIs24HourView(true);
        dpDate.setOnClickListener(this);
        dpEndDate.setOnClickListener(this);
        
		if(reDate !=null && !TextUtils.isEmpty(reDate)){
			String[] dates = reDate.split(" - ");
			String[] startDate = dates[0].split("-");
			String[] endDate = dates[1].split("-");
			
			String[] times = reTime.split(" - ");
			String[] startTime = times[0].split(":");
			String[] endTime = times[1].split(":");
			int timeLengthTag = (Integer.parseInt(endTime[0])*3600 + Integer.parseInt(endTime[1])*60 - 
					Integer.parseInt(startTime[0])*3600 - Integer.parseInt(startTime[1])*60);
			int hourLength = timeLengthTag/3600;
			int minuteLength = timeLengthTag % 3600 / 60;
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
//			if(minuteLength == 0){
//				tpLength.setCurrentHour(0);
//			    tpLength.setCurrentMinute(0);
//			}
			
			
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);

			dtTerm.addTextChangedListener(textwatcher);
			dtSubject.addTextChangedListener(textwatcher);
			dtGrade.addTextChangedListener(textwatcher);
			dtPlace.addTextChangedListener(textwatcher);

			if(!TextUtils.isEmpty(reDate)){
				String[] infos = getStrsByComma(classroom.description);
				if(null != infos && infos.length == 4){
					dtTerm.setText(infos[0]);
					dtGrade.setText(infos[1]);
					dtSubject.setText(infos[2]);
					dtPlace.setText(infos[3]);
					
//					String[] trsdates = infos[3].split("-");
//					if(trsdates.length == 3){
//						dpDate.init(Integer.parseInt(trsdates[0]),Integer.parseInt(trsdates[1]) - 1 ,Integer.parseInt(trsdates[2]) , new DatePicker.OnDateChangedListener() {
//                            @Override
//                            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//                                closeInputBoard();
//                            }
//                        });
//					}
//					
//					String[] trstime = infos[4].split(":");
//					if(trstime.length == 2){
//						tpTime.setCurrentHour(Integer.parseInt(trstime[0]));
//						tpTime.setCurrentMinute(Integer.parseInt(trstime[1]));
//					}
//					String[] trslength = infos[6].split(":");
//					if(trslength.length == 2){
//						tpLength.setCurrentHour(Integer.parseInt(trslength[0]));
//						tpLength.setCurrentMinute(Integer.parseInt(trslength[1]));
//					}
//					
//					String[] trsenddates = infos[7].split("-");
//					if(trsenddates.length == 3){
//						dpEndDate.init(Integer.parseInt(trsenddates[0]),Integer.parseInt(trsenddates[1]) - 1 ,Integer.parseInt(trsenddates[2]) , new DatePicker.OnDateChangedListener() {
//                            @Override
//                            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//                                closeInputBoard();
//                            }
//                        });
//					}
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

    private String[] getStrsByComma(String str){
		if(TextUtils.isEmpty(str)&& !str.contains(Constants.COMMA)){
			return null;
		}
		return str.split(Constants.COMMA);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
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
		
		final int hour = tpTime.getCurrentHour();
		final int minite = tpTime.getCurrentMinute();
		final int hourLength = tpLength.getCurrentHour();
		final int miniteLength = tpLength.getCurrentMinute();
		classroom.description = dtTerm.getText().toString() 
				+ Constants.COMMA + dtGrade.getText().toString()
				+ Constants.COMMA + dtSubject.getText().toString()
				+ Constants.COMMA + dtPlace.getText().toString();
		
		final Calendar startDay = Calendar.getInstance();
		final Calendar endDay = Calendar.getInstance();
		
		long currentDayTimps = Utils.getDayStampMoveMillis();
		int classTimps = hour * 3600 + minite * 60;
		int classLengthTimps = hourLength * 3600 + miniteLength * 60;
		final long startClassTimps = currentDayTimps + classTimps;
		final long endClassTimps = startClassTimps + classLengthTimps;
		startDay.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth());
		endDay.set(dpEndDate.getYear(), dpEndDate.getMonth(), dpEndDate.getDayOfMonth());
		
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
				if (result == ErrorCode.OK) {
					// update the display name of chatmessages.
					mDBHelper.updateChatMessageDisplayNameWithUser(classroom.groupID, classroom.groupNameOriginal);
					Intent data = new Intent();
					data.putExtra(TERM, dtTerm.getText().toString());
					data.putExtra(GRADE, dtGrade.getText().toString());
					data.putExtra(SUBJECT, dtSubject.getText().toString());
//					data.putExtra(DATE, new SimpleDateFormat("yyyy-MM-dd").format(resultTime.getTimeInMillis()));
//					data.putExtra(TIME, (hour < 10 ? ("0" + String.valueOf(hour)) : String.valueOf(hour)) + ":"
//							+ (minite < 10 ? ("0" + String.valueOf(minite)) : String.valueOf(minite)));
					data.putExtra(PLACE, dtPlace.getText().toString());
//					data.putExtra(LENGTH, String.valueOf(hourLength) + ":"
//							+ (miniteLength < 10 ? ("0" + String.valueOf(miniteLength)) : String.valueOf(miniteLength))) ;
					setResult(RESULT_OK, data);
					finish();
				} else if (result == ErrorCode.ERR_OPERATION_DENIED) {
					mMsgBox.toast(R.string.class_err_denied, 500);
				}
			}
			
		});
	}
//    private void updateClassInfo() {
//		if(classroom == null){
//			mMsgBox.toast(R.string.class_err_denied, 500);
//			return;
//		}
//		classroom.isEditable = true;
//		
//		final Calendar resultTime = Calendar.getInstance();
//		//resultTime.setTimeZone(TimeZone.getTimeZone("GMT"));
//		resultTime.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth());
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		
//		long firstlesTime = getIntent().getLongExtra("firstlesdate", -1);
//		if(firstlesTime != -1){
//		    if( firstlesTime < resultTime.getTimeInMillis() / 1000 ){
//			    if(!mMsgBox.isWaitShowing()){
//				    mMsgBox.toast("开班时间不得晚于课程时间！");
//			    }
//			    return;
//		    }
//		}
//		
//		final int hour = tpTime.getCurrentHour();
//		final int minite = tpTime.getCurrentMinute();
//		final int hourLength = tpLength.getCurrentHour();
//		final int miniteLength = tpLength.getCurrentMinute();
//		classroom.description = dtTerm.getText().toString() 
//				+ Constants.COMMA + dtGrade.getText().toString()
//				+ Constants.COMMA + dtSubject.getText().toString()
//				+ Constants.COMMA + dtPlace.getText().toString();
//				
//		mMsgBox.showWait();
//		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
//			@Override
//			protected Integer doInBackground(Void... params) {
//				mDBHelper.updateGroupChatRoom(classroom);
//				return WowTalkWebServerIF.getInstance(ClassInfoEditActivity.this).fGroupChat_UpdateInfo(classroom);
//			}
//
//			@Override
//			protected void onPostExecute(Integer result) {
//				mMsgBox.dismissWait();
//				if (result == ErrorCode.OK) {
//					// update the display name of chatmessages.
//					mDBHelper.updateChatMessageDisplayNameWithUser(classroom.groupID, classroom.groupNameOriginal);
//					Intent data = new Intent();
//					data.putExtra(TERM, dtTerm.getText().toString());
//					data.putExtra(GRADE, dtGrade.getText().toString());
//					data.putExtra(SUBJECT, dtSubject.getText().toString());
////					data.putExtra(DATE, new SimpleDateFormat("yyyy-MM-dd").format(resultTime.getTimeInMillis()));
////					data.putExtra(TIME, (hour < 10 ? ("0" + String.valueOf(hour)) : String.valueOf(hour)) + ":"
////							+ (minite < 10 ? ("0" + String.valueOf(minite)) : String.valueOf(minite)));
//					data.putExtra(PLACE, dtPlace.getText().toString());
////					data.putExtra(LENGTH, String.valueOf(hourLength) + ":"
////							+ (miniteLength < 10 ? ("0" + String.valueOf(miniteLength)) : String.valueOf(miniteLength))) ;
//					setResult(RESULT_OK, data);
//					finish();
//				} else if (result == ErrorCode.ERR_OPERATION_DENIED) {
//					mMsgBox.toast(R.string.class_err_denied, 500);
//				}
//			}
//		});
//	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		mDBHelper.close();
		mMsgBox = null;
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
