package co.onemeter.oneapp.ui;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class LessonInfoEditActivity extends Activity implements OnClickListener {

	public static final int TAG_CLASS_INFO = 0;
	public static final int TAG_LES_TABLE = 1;

	public static final String TERM = "term";
	public static final String GRADE = "grade";
	public static final String SUBJECT = "subject";
	public static final String DATE = "date";
	public static final String TIME = "time";
	public static final String PLACE = "place";

	private int tag;
	private String classId;
	private int originSize;
	private long time_openclass;

	private ListView lvCourtable;
	private EditText dtTerm;
	private EditText dtGrade;
	private EditText dtSubject;
	private DatePicker dpDate;
	private TimePicker tpTime;
	private EditText dtPlace;

	private Database mDBHelper;
	private GroupChatRoom classroom;
	private MessageBox mMsgBox;
	private CourseTableAdapter adapter;
	private List<Lesson> lessons;
	private List<String> delLessons;
	private List<Lesson> addLessons;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what == 0){
				mMsgBox.dismissWait();
				finish();
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lessoninfo_edit);

		initView();
		registerForContextMenu(lvCourtable);
	}

	private void initView() {
		Intent intent = getIntent();
		tag = intent.getIntExtra("tag", 0);
		classroom = intent.getParcelableExtra("class");
		classId = classroom.groupID;
		//android.util.Log.i("-->>", classroom.groupID);

		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);
		dpDate = (DatePicker) findViewById(R.id.datePicker_lesinfo_date);
		tpTime = (TimePicker) findViewById(R.id.timePicker_lesinfo_time);
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);
		lvCourtable = (ListView) findViewById(R.id.lv_courtable);

		
		lessons = new LinkedList<Lesson>();
		delLessons = new ArrayList<String>();
		addLessons = new ArrayList<Lesson>();
		adapter = new CourseTableAdapter(lessons);
		mMsgBox = new MessageBox(this);
		mDBHelper = new Database(this);
		AQuery q = new AQuery(this);
		
		lvCourtable.addFooterView(footerView());
		lvCourtable.setAdapter(adapter);
		
		q.find(R.id.cancel).clicked(this);
		q.find(R.id.save).clicked(this);

		
		if (tag == TAG_LES_TABLE) {
			String opentime = classroom.description.split(Constants.COMMA)[3];
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				time_openclass = sdf.parse(opentime).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			findViewById(R.id.lay_info_edit).setVisibility(View.GONE);
			findViewById(R.id.lay_les_edit).setVisibility(View.VISIBLE);
			q.find(R.id.title).text(getString(R.string.class_coursetable_info));
			
			lessons.addAll(mDBHelper.fetchLesson(classId));
			Collections.sort(lessons, new LessonComparator());
			adapter.notifyDataSetChanged();
			originSize = lessons.size();
		} else {
			tpTime.setIs24HourView(true);
			
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);
			findViewById(R.id.lay_les_edit).setVisibility(View.GONE);


			if(classroom != null){
				String[] infos = getStrsByComma(classroom.description);
				if(null != infos && infos.length == 6){
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
	
	private void deleteLessons(){
		if(delLessons.isEmpty()){
			return;
		}
		mMsgBox.showWait();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				LessonWebServerIF webserver = LessonWebServerIF.getInstance(LessonInfoEditActivity.this);
				for(String lesson_id:delLessons){
					webserver.deleteLesson(lesson_id);
				}
				mHandler.sendEmptyMessage(0);
			}
		}).start();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
		case R.id.save:
			if (tag == TAG_LES_TABLE) {
				if(!delLessons.isEmpty()){
					deleteLessons();
				}else if(!addLessons.isEmpty()){
					addPostLesson(addLessons);
				}else{
					finish();
				}
			} else {
				updateClassInfo();
			}
			break;
		case R.id.lay_footer_add:
			showAddLessonDialog();
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, 1, Menu.NONE, getString(R.string.contacts_local_delete)); 
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo=(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
	    final int id=(int) menuInfo.id;
		switch (item.getItemId()) {
		case 1:
			if(lessons.get(id).start_date * 1000 < System.currentTimeMillis()){
				mMsgBox.toast(R.string.class_not_del_earlier);
				return true;
			}
			originSize = lessons.size() - addLessons.size();
			delLessons.add(lessons.get(id).lesson_id + "");
			lessons.remove(id);
			if(!addLessons.isEmpty() && id >= originSize){
				addLessons.remove(id - originSize);
			}
			adapter.notifyDataSetChanged();
			break;

		default:
			break;
		}
		return true;
	}

	private View footerView() {
		View view = getLayoutInflater().inflate(R.layout.lay_lv_footer, null);
		TextView txt_footer = (TextView) view.findViewById(R.id.txt_footer_add);
		txt_footer.setText(getString(R.string.class_add_lesson));
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.lay_footer_add);
		layout.setOnClickListener(this);
		return view;
	}
	
	private void showAddLessonDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
		final DatePicker datepicker = (DatePicker) view.findViewById(R.id.datepicker_dialog);
		builder.setView(view);
		builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String content = edName.getText().toString();
				if(TextUtils.isEmpty(content)){
					mMsgBox.toast(R.string.class_lessontitle_not_null);
					notdismissDialog(dialog);
					return;
				}
				
				Calendar result = Calendar.getInstance();
				result.setTimeZone(TimeZone.getTimeZone("GMT"));
				result.set(datepicker.getYear(), datepicker.getMonth(), datepicker.getDayOfMonth());
				long resultTime = result.getTimeInMillis();
				
				if(resultTime < System.currentTimeMillis()){
					mMsgBox.toast(R.string.class_time_ealier);
					notdismissDialog(dialog);
					return;
				}
				
				if(resultTime < time_openclass){
					mMsgBox.toast(R.string.class_les_not_before_start);
					notdismissDialog(dialog);
					return;
				}
				
				Lesson lesson = new Lesson();
				lesson.class_id = classId;
				lesson.title = content;
				lesson.start_date = resultTime/1000;
				lesson.end_date = resultTime/1000 + 45 * 60;
				addLessons.add(lesson);
				lessons.add(lesson);
				adapter.notifyDataSetChanged();
					
				dismissDialog(dialog);
			}
		}).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				try {
					Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
					field.setAccessible(true);
					field.set(dialog, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).create();
		builder.show();
	}
	
	private void notdismissDialog(DialogInterface dialog){
		//利用反射使得dialog不消失
		try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void dismissDialog(DialogInterface dialog){
		try {
			Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, true);
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	private void addPostLesson(final List<Lesson> lessons){
		new AsyncTask<Void, Void, Integer>(){

			protected void onPreExecute() {
				mMsgBox.showWait();
			};
			
			@Override
			protected Integer doInBackground(Void... params) {
				LessonWebServerIF lesWeb = LessonWebServerIF.getInstance(LessonInfoEditActivity.this);
				int errno = 0;
				for(Lesson lesson: lessons){
					errno = lesWeb.addOrModifyLesson(lesson);
				}
				return errno;
			}
			
			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();
				if(ErrorCode.OK == result){
					mMsgBox.toast(R.string.class_submit_success);
					finish();
				}
			};
		}.execute((Void)null);
	}
	
	private void updateClassInfo() {
		if(classroom == null){
			mMsgBox.toast(R.string.class_err_denied, 500);
			return;
		}
		classroom.isEditable = true;
		
		Calendar result = Calendar.getInstance();
		result.setTimeZone(TimeZone.getTimeZone("GMT"));
		result.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth() - 1);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		int hour = tpTime.getCurrentHour();
		int minite = tpTime.getCurrentMinute();
		classroom.description = dtTerm.getText().toString() 
				+ Constants.COMMA + dtGrade.getText().toString()
				+ Constants.COMMA + dtSubject.getText().toString()
				+ Constants.COMMA + sdf.format(result.getTimeInMillis())
				+ Constants.COMMA + (hour < 10 ? ("0"+ String.valueOf(hour)) : String.valueOf(hour)) + ":" 
				+ (minite < 10 ? ("0"+ String.valueOf(minite)) : String.valueOf(minite))
				+ Constants.COMMA + dtPlace.getText().toString();
		mMsgBox.showWait();
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... params) {
				mDBHelper.updateGroupChatRoom(classroom);
				return WowTalkWebServerIF.getInstance(LessonInfoEditActivity.this).fGroupChat_UpdateInfo(classroom);
			}

			@Override
			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();
				if (result == ErrorCode.OK) {
					// update the display name of chatmessages.
					mDBHelper.updateChatMessageDisplayNameWithUser(
							classroom.groupID, classroom.groupNameOriginal);
					//setResult(RESULT_OK);
					finish();
				}else if(result == ErrorCode.ERR_OPERATION_DENIED){
					mMsgBox.toast(R.string.class_err_denied, 500);
				}
			}
		}.execute((Void) null);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
	static class LessonComparator implements Comparator<Lesson>{

		@Override
		public int compare(Lesson arg0, Lesson arg1) {
			long start0 = arg0.start_date;
			long start1 = arg1.start_date;
			if(start0 > start1){
				return 1;
			}else if(start0 < start1){
				return -1;
			}
			return 0;
		}
		
	}
	
	class CourseTableAdapter extends BaseAdapter{
		private List<Lesson> alessons;
		private long now = System.currentTimeMillis();
		
		public CourseTableAdapter(List<Lesson> lessons){
			this.alessons = lessons;
		}
		
		@Override
		public int getCount() {
			return alessons.size();
		}

		@Override
		public Object getItem(int position) {
			return alessons.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_coursetable, parent, false);
				holder.item_name = (TextView) convertView.findViewById(R.id.coursetable_item_name);
				holder.item_time = (TextView) convertView.findViewById(R.id.coursetable_item_time);
				holder.item_msg = (TextView) convertView.findViewById(R.id.coursetable_item_msg);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			holder.item_name.setText(alessons.get(position).title);
			if(alessons.get(position).end_date * 1000 < now){
				holder.item_name.setTextColor(0xff8eb4e6);
			}
			holder.item_time.setText(sdf.format(new Date(alessons.get(position).start_date * 1000)));
			holder.item_msg.setText("");
			return convertView;
		}
		class ViewHodler{
			TextView item_name;
			TextView item_time;
			TextView item_msg;
		}
		
	}
}
