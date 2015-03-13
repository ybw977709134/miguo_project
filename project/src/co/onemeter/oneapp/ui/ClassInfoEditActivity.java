package co.onemeter.oneapp.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * 
 * @author jacky
 *this activity is used for editting class' info
 */
public class ClassInfoEditActivity extends Activity implements View.OnClickListener{


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
	private TimePicker tpTime;
	private TimePicker tpLength;
	private EditText dtPlace;

	private Database mDBHelper;
	private GroupChatRoom classroom;
	private MessageBox mMsgBox;
	
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
		classId = classroom.groupID;
		//android.util.Log.i("-->>", classroom.groupID);

		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);
		dpDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_date);
		tpTime = (TimePicker) findViewById(R.id.timePicker_lesinfo_time);
		tpLength = (TimePicker) findViewById(R.id.timePicker_lesinfo_length);
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);

		
		mMsgBox = new MessageBox(this);
		mDBHelper = new Database(this);
		AQuery q = new AQuery(this);
		
		
		q.find(R.id.cancel).clicked(this);
		q.find(R.id.save).clicked(this);

		
			tpTime.setIs24HourView(true);
			tpLength.setIs24HourView(true);
			tpLength.setCurrentHour(0);
			tpLength.setCurrentMinute(0);
			
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);

			dtTerm.addTextChangedListener(textwatcher);
			dtSubject.addTextChangedListener(textwatcher);
			dtGrade.addTextChangedListener(textwatcher);
			dtPlace.addTextChangedListener(textwatcher);

			if(classroom != null){
				String[] infos = getStrsByComma(classroom.description);
				if(null != infos && infos.length == 7){
					dtTerm.setText(infos[0]);
					dtGrade.setText(infos[1]);
					dtSubject.setText(infos[2]);
					dtPlace.setText(infos[5]);
					
					String[] trsdates = infos[3].split("-");
					if(trsdates.length == 3){
						dpDate.init(Integer.parseInt(trsdates[0]),Integer.parseInt(trsdates[1]) - 1 ,Integer.parseInt(trsdates[2]) , null);
					}
					
					String[] trstime = infos[4].split(":");
					if(trstime.length == 2){
						tpTime.setCurrentHour(Integer.parseInt(trstime[0]));
						tpTime.setCurrentMinute(Integer.parseInt(trstime[1]));
					}
					String[] trslength = infos[6].split(":");
					if(trslength.length == 2){
						tpLength.setCurrentHour(Integer.parseInt(trslength[0]));
						tpLength.setCurrentMinute(Integer.parseInt(trslength[1]));
					}
				}
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
				updateClassInfo();
			break;
		default:
			break;
		}
	}
	
	private void updateClassInfo() {
		if(classroom == null){
			mMsgBox.toast(R.string.class_err_denied, 500);
			return;
		}
		classroom.isEditable = true;
		
		final Calendar resultTime = Calendar.getInstance();
		//resultTime.setTimeZone(TimeZone.getTimeZone("GMT"));
		resultTime.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth());
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
		
		final int hour = tpTime.getCurrentHour();
		final int minite = tpTime.getCurrentMinute();
		final int hourLength = tpLength.getCurrentHour();
		final int miniteLength = tpLength.getCurrentMinute();
		classroom.description = dtTerm.getText().toString() 
				+ Constants.COMMA + dtGrade.getText().toString()
				+ Constants.COMMA + dtSubject.getText().toString()
				+ Constants.COMMA + sdf.format(resultTime.getTimeInMillis())
				+ Constants.COMMA + (hour < 10 ? ("0"+ String.valueOf(hour)) : String.valueOf(hour)) + ":" 
				+ (minite < 10 ? ("0"+ String.valueOf(minite)) : String.valueOf(minite))
				+ Constants.COMMA + dtPlace.getText().toString()
				+ Constants.COMMA + (hourLength < 10 ? ("0"+ String.valueOf(hourLength)) : String.valueOf(hourLength)) + ":" 
						+ (miniteLength < 10 ? ("0"+ String.valueOf(miniteLength)) : String.valueOf(miniteLength));
		mMsgBox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... params) {
				mDBHelper.updateGroupChatRoom(classroom);
				return WowTalkWebServerIF.getInstance(ClassInfoEditActivity.this).fGroupChat_UpdateInfo(classroom);
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
					data.putExtra(DATE, new SimpleDateFormat("yyyy-MM-dd").format(resultTime.getTimeInMillis()));
					data.putExtra(TIME, (hour < 10 ? ("0" + String.valueOf(hour)) : String.valueOf(hour)) + ":"
							+ (minite < 10 ? ("0" + String.valueOf(minite)) : String.valueOf(minite)));
					data.putExtra(PLACE, dtPlace.getText().toString());
					data.putExtra(LENGTH, String.valueOf(hourLength) + ":"
							+ (miniteLength < 10 ? ("0" + String.valueOf(miniteLength)) : String.valueOf(miniteLength))) ;
					setResult(RESULT_OK, data);
					finish();
				} else if (result == ErrorCode.ERR_OPERATION_DENIED) {
					mMsgBox.toast(R.string.class_err_denied, 500);
				}
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		mDBHelper.close();
		mMsgBox = null;
	}
	
}
