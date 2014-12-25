package co.onemeter.oneapp.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.WowLessonWebServerIF;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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

	private ListView lvCourtable;
	private EditText dtTerm;
	private EditText dtGrade;
	private EditText dtSubject;
	private EditText dtDate;
	private EditText dtTime;
	private EditText dtPlace;

	private Database mDBHelper;
	private GroupChatRoom classroom;
	private MessageBox mMsgBox;
	private CourseTableAdapter adapter;
	private List<Lesson> lessons;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lessoninfo_edit);

		initView();
	}

	private void initView() {
		Intent intent = getIntent();
		tag = intent.getIntExtra("tag", 0);
		classId = intent.getStringExtra("classId");

		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);
		dtDate = (EditText) findViewById(R.id.ed_lesinfo_date);
		dtTime = (EditText) findViewById(R.id.ed_lesinfo_time);
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);
		lvCourtable = (ListView) findViewById(R.id.lv_courtable);

		lessons = new LinkedList<Lesson>();
		adapter = new CourseTableAdapter(lessons);
		mMsgBox = new MessageBox(this);
		mDBHelper = new Database(this);
		AQuery q = new AQuery(this);
		lvCourtable.setAdapter(adapter);
		
		q.find(R.id.cancel).clicked(this);
		q.find(R.id.save).clicked(this);
		q.find(R.id.lay_les_edit_add).clicked(this);

		classroom = mDBHelper.fetchGroupChatRoom(classId);
		
		if (tag == TAG_LES_TABLE) {
			findViewById(R.id.lay_info_edit).setVisibility(View.GONE);
			findViewById(R.id.lay_les_edit).setVisibility(View.VISIBLE);
			q.find(R.id.title).text(getString(R.string.class_coursetable_info));
			lessons.addAll(mDBHelper.fetchLesson(classId));
			adapter.notifyDataSetChanged();
		} else {
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);
			findViewById(R.id.lay_les_edit).setVisibility(View.GONE);
			dtTerm.setText(intent.getStringExtra(TERM));
			dtGrade.setText(intent.getStringExtra(GRADE));
			dtSubject.setText(intent.getStringExtra(SUBJECT));
			dtDate.setText(intent.getStringExtra(DATE));
			dtTime.setText(intent.getStringExtra(TIME));
			dtPlace.setText(intent.getStringExtra(PLACE));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
		case R.id.save:
			if (tag == TAG_LES_TABLE) {

			} else {
				updateClassInfo();
			}
			break;
		case R.id.lay_les_edit_add:
			showAddLessonDialog();
			break;
		default:
			break;
		}
	}

	private void showAddLessonDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
		final DatePicker datepicker = (DatePicker) view.findViewById(R.id.datepicker_dialog);
		builder.setView(view);
		builder.setTitle(getString(R.string.class_add_lesson)).setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Lesson lesson = new Lesson();
				lesson.class_id = classId;
				lesson.title = edName.getText().toString();
				Calendar result = Calendar.getInstance();
				result.set(datepicker.getYear(), datepicker.getMonth(), datepicker.getDayOfMonth());
				lesson.start_date = result.getTimeInMillis()/1000;
				lesson.end_date = result.getTimeInMillis()/1000 + 45 * 60;
				addPostLesson(lesson);
			}
		}).setNegativeButton(getString(R.string.cancel), null).create();
		builder.show();
	}
	
	private void addPostLesson(final Lesson lesson){
		new AsyncTask<Void, Void, Integer>(){

			@Override
			protected Integer doInBackground(Void... params) {
				
				return WowLessonWebServerIF.getInstance(LessonInfoEditActivity.this).addOrModifyLesson(lesson);
			}
			
			protected void onPostExecute(Integer result) {
				if(ErrorCode.OK == result){
					mMsgBox.toast("添加成功");
					lessons.clear();
					lessons.addAll(mDBHelper.fetchLesson(classId));
					adapter.notifyDataSetChanged();
				}
			};
		}.execute((Void)null);
	}
	
	//暂时以GroupChatRoom中place字段存储信息
	private void updateClassInfo() {
		classroom.isEditable = true;
		classroom.place = dtTerm.getText().toString() 
				+ Constants.COMMA + dtGrade.getText().toString()
				+ Constants.COMMA + dtSubject.getText().toString()
				+ Constants.COMMA + dtDate.getText().toString()
				+ Constants.COMMA + dtTime.getText().toString()
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
	
	class CourseTableAdapter extends BaseAdapter{
		private List<Lesson> alessons;
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
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
