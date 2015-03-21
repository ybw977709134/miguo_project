package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.DoubleClickedUtils;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 课表修改以及班级信息修改页面。
 * Created by yl on 21/12/2014.
 */
public class LessonInfoEditActivity extends Activity implements OnClickListener, OnItemClickListener {

	private String classId;
	private int originSize;
	private long time_openclass;

	private ListView lvCourtable;

	private Database mDBHelper;
	private GroupChatRoom classroom;
	private MessageBox mMsgBox;
	private CourseTableAdapter adapter;
	private List<Lesson> lessons;
	private List<String> delLessons;
	private List<Lesson> addLessons;
	
	private AlertDialog.Builder dialog;
	private String reLength;
	private String reTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lessoninfo_edit);

		initView();
		registerForContextMenu(lvCourtable);
	}

	private void initView() {
		Intent intent = getIntent();
		classroom = intent.getParcelableExtra("class");
		reLength = intent.getStringExtra("reLength");
//		reTime = intent.getStringExtra("reTime");
		reTime= intent.getExtras().getString("retime");
		
		classId = classroom.groupID;
		//android.util.Log.i("-->>", classroom.groupID);

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
		lvCourtable.setOnItemClickListener(this);
		
		q.find(R.id.cancel).clicked(this);
		q.find(R.id.save).clicked(this);

		
			String opentime = classroom.description.split(Constants.COMMA)[3];
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				time_openclass = sdf.parse(opentime).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			findViewById(R.id.lay_les_edit).setVisibility(View.VISIBLE);
			q.find(R.id.title).text(getString(R.string.class_coursetable_info));
			
			lessons.addAll(mDBHelper.fetchLesson(classId));
			Collections.sort(lessons, new LessonComparator());
			adapter.notifyDataSetChanged();
			originSize = lessons.size();
			
			ContextThemeWrapper themedContext;
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			    themedContext = new ContextThemeWrapper( this, android.R.style.Theme_Holo_Light_Dialog);
			}
			else {
			    themedContext = new ContextThemeWrapper( this, android.R.style.Theme_Light );
			}
			dialog = new AlertDialog.Builder(themedContext);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
		case R.id.save:
				if(delLessons.isEmpty() && addLessons.isEmpty()){
					finish();
				}else{
					addOrDeletePostLesson(addLessons,delLessons);
				}
			break;
		case R.id.lay_footer_add:
			if(!DoubleClickedUtils.isFastDoubleClick()){
				showAddOrModifyLessonDialog(true,null,-1,false);
			}
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Lesson lesson = lessons.get(position);
		boolean isBeafore = true;
		//允许今后的课程能够修,已经上过的课可以修改课名
		if(lesson.start_date * 1000 > System.currentTimeMillis()){
			isBeafore = false;
		}
		if(!DoubleClickedUtils.isFastDoubleClick()){
			showAddOrModifyLessonDialog(false,view, position,isBeafore);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		//menu.add(0, 1, Menu.NONE, getString(R.string.contacts_local_delete)); 
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
	
	
	
	private void showAddOrModifyLessonDialog(final boolean isAdd,final View item,final int position,boolean isBefore){
		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
		edName.setFocusable(true);
		edName.setFocusableInTouchMode(true); 
		edName.requestFocus();
		final DatePicker datepicker = (DatePicker) view.findViewById(R.id.datepicker_dialog);
		
		Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager)edName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
				imm.showSoftInput(edName, InputMethodManager.RESULT_SHOWN);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); 
			}
		}, 200);
        
		if(!isAdd){
			dialog.setTitle("修改课程");
			view.findViewById(R.id.lay_lesson_name).setVisibility(View.VISIBLE);
			String[] startTime = ((TextView)item.findViewById(R.id.coursetable_item_time)).getText().toString().split("-");
			datepicker.init(Integer.parseInt(startTime[0]), Integer.parseInt(startTime[1]) - 1, Integer.parseInt(startTime[2]), null);
            edName.setText(lessons.get(position).title);
        }else{
		}
		
		if(isBefore){
			view.findViewById(R.id.lay_dialog_date).setVisibility(View.GONE);
		}
		
		dialog.setView(view);
		dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Calendar result = Calendar.getInstance();
				//result.setTimeZone(TimeZone.getTimeZone("GMT"));
				result.set(datepicker.getYear(), datepicker.getMonth(), datepicker.getDayOfMonth(),0,0,0);
				long resultTime = result.getTimeInMillis();
				Log.i("---resultTime---" + resultTime);
				String[] times = reTime.split(":");
				String[] lengths = reLength.split(":");
				
				String content = edName.getText().toString();

				if(isAdd){
//					long now = System.currentTimeMillis();
//					if(resultTime < now){
//						mMsgBox.toast(R.string.class_time_ealier);
//						notdismissDialog(dialog);
//						return;
//					}

                    if(TextUtils.isEmpty(content)){
                        mMsgBox.toast(R.string.class_lessontitle_not_null);
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
					
					lesson.start_date = resultTime/1000 + Integer.parseInt(times[0])*3600 + Integer.parseInt(times[1])*60;
					lesson.end_date = lesson.start_date + Integer.parseInt(lengths[0])*3600 + Integer.parseInt(lengths[1])*60;
					addLessons.add(lesson);
					lessons.add(lesson);
					adapter.notifyDataSetChanged();
						
					dismissDialog(dialog);
				}else{
					if(position >= 0){
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						((TextView)item.findViewById(R.id.coursetable_item_time)).setText(sdf.format(resultTime));
						lessons.get(position).start_date = resultTime/1000 + Integer.parseInt(times[0])*3600 + Integer.parseInt(times[1])*60;
						lessons.get(position).end_date = lessons.get(position).start_date + Integer.parseInt(lengths[0])*3600 + Integer.parseInt(lengths[1])*60;
						Lesson lesson = lessons.get(position);
						if(lesson.lesson_id > 0){
							lesson.title = content;
                            if(TextUtils.isEmpty(content)){
                                lesson.title = edName.getHint().toString();
                            }
							modifyPostLesson(lesson);
						}else{
							int size = addLessons.size();
							int totalsize = lessons.size();
							int pos = position - (totalsize - size);
							addLessons.get(pos).start_date = resultTime/1000 + Integer.parseInt(times[0])*3600 + Integer.parseInt(times[1])*60;
							addLessons.get(pos).end_date = addLessons.get(pos).start_date + Integer.parseInt(lengths[0])*3600 + Integer.parseInt(lengths[1])*60;
						}
					}
				}
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
		dialog.show();
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
	
	private void modifyPostLesson(final Lesson lesson){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				LessonWebServerIF lesWeb = LessonWebServerIF.getInstance(LessonInfoEditActivity.this);
				int errno = lesWeb.addOrModifyLesson(lesson);
				if(errno == ErrorCode.OK){
					Database db = Database.getInstance(LessonInfoEditActivity.this);
					db.storeLesson(lesson);
				}
			}
		}).start();
	}
	
	private void addOrDeletePostLesson(final List<Lesson> alessons,final List<String> dlessons){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			protected void onPreExecute() {
				if (dlessons.isEmpty() && alessons.isEmpty()) {
					return;
				}
				mMsgBox.showWait();
			}

			;

			@Override
			protected Integer doInBackground(Void... params) {
				LessonWebServerIF lesWeb = LessonWebServerIF.getInstance(LessonInfoEditActivity.this);
				if (!dlessons.isEmpty()) {
					for (String lesson_id : dlessons) {
						lesWeb.deleteLesson(lesson_id);
					}
				}
				int errno = ErrorCode.BAD_RESPONSE;
				if (!alessons.isEmpty()) {
					for (Lesson lesson : alessons) {
						errno = lesWeb.addOrModifyLesson(lesson);
					}
				}
				return errno;
			}

			protected void onPostExecute(Integer result) {
				mMsgBox.dismissWait();
				if (ErrorCode.OK == result) {
					mMsgBox.toast(R.string.class_submit_success);
				}
				finish();
			}

			;
		});
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		mDBHelper.close();
		mMsgBox = null;
	}
	
	/*
	 * 将课表按时间从早排序
	 */
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
			Lesson lesson = alessons.get(position);
			holder.item_name.setText(lesson.title);
			holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			
			long now = Utils.getDayStampMoveMillis();
			long startdate = lesson.start_date;
			if(startdate < now){
				holder.item_name.setTextColor(0xff8eb4e6);
			}
			if(startdate == now){
				holder.item_name.setTextColor(Color.RED);
			}
			holder.item_time.setText(sdf.format(new Date(startdate * 1000)));
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
