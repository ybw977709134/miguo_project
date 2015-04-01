package co.onemeter.oneapp.ui;


import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.LessonInfoEditActivity.LessonComparator;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 课程选择页面。
 * Created by zz on 04/01/2015.
 */
public class SelectLessonActivity extends Activity implements OnClickListener, OnItemClickListener{
	private ListView lvCourtable;
	private List<Lesson> lessons;
	private ImageButton title_back;
	private ImageView lesson_refresh;
	private CourseTableAdapter adapter;
	private MessageBox mMsgBox;
	private Database mDBHelper;
	private String classId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_lesson);
		
		initView();
		getLessonInfo();


	}
	
	private void initView(){
		lvCourtable = (ListView) findViewById(R.id.listView_lesson_show);
		title_back = (ImageButton) findViewById(R.id.title_back);
		lesson_refresh = (ImageView) findViewById(R.id.lesson_refresh);
		
		lvCourtable.setOnItemClickListener(this);
		lesson_refresh.setOnClickListener(this);
		title_back.setOnClickListener(this);
		lessons = new LinkedList<Lesson>();
		adapter = new CourseTableAdapter(lessons);
		lvCourtable.setAdapter(adapter);
		mMsgBox = new MessageBox(this);
		mDBHelper = new Database(this);
		classId = "1678ff8f-2a41-438a-bb22-4f55530857f1";
		lessons.addAll(mDBHelper.fetchLesson(classId));
		Collections.sort(lessons, new LessonComparator());
		adapter.notifyDataSetChanged();
	}
	
	private void getLessonInfo(){
		mMsgBox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

				@Override
				protected Integer doInBackground(Void... params) {
					return LessonWebServerIF.getInstance(SelectLessonActivity.this).getLesson(classId);
				}

				protected void onPostExecute(Integer result) {
					mMsgBox.dismissWait();
					if (ErrorCode.OK == result) {
						refreshLessonInfo();
					}
				};

			});
	}
	private void refreshLessonInfo(){
		lessons.clear();
		Database db = Database.open(SelectLessonActivity.this);
		lessons.addAll(db.fetchLesson(classId));
//		db.close();
		Collections.sort(lessons, new LessonInfoEditActivity.LessonComparator());
		adapter.notifyDataSetChanged();
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.lesson_refresh:
			getLessonInfo();
			break;
		}
		
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		adapter.setCurrPosition(position);
		
	}
	
	
	class CourseTableAdapter extends BaseAdapter{
		private List<Lesson> alessons;
		private int currPosition = -1;
		
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
			SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm");
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_coursetable, parent, false);
				holder.item_name = (TextView) convertView.findViewById(R.id.coursetable_item_name);
				holder.item_time = (TextView) convertView.findViewById(R.id.coursetable_item_time);
				holder.item_date = (TextView) convertView.findViewById(R.id.coursetable_item_date);
				holder.item_msg = (TextView) convertView.findViewById(R.id.coursetable_item_msg);
				holder.mylesson_item_icon =  (ImageView) convertView.findViewById(R.id.mylesson_item_icon);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Lesson lesson = alessons.get(position);
			holder.item_name.setText(lesson.title);
			holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			if(currPosition == position){
				holder.mylesson_item_icon.setVisibility(View.VISIBLE);
			}else{
				holder.mylesson_item_icon.setVisibility(View.INVISIBLE);
			}
			
			long now = Utils.getDayStampMoveMillis();
			long startdate = lesson.start_date;
			long enddata = lesson.end_date;
			long curTime = System.currentTimeMillis()/1000;
			if(curTime > enddata){
				holder.item_name.setTextColor(0xff8eb4e6);		
			}else if(curTime > now && curTime <startdate){
				holder.item_name.setTextColor(getResources().getColor(R.color.gray));
			}else if(curTime > startdate && curTime < enddata){
				holder.item_name.setTextColor(Color.RED);
			}
			holder.item_date.setText(sdf.format(new Date(startdate * 1000)));
			holder.item_time.setText(sdf_time.format(new Date(startdate * 1000)) + " - " + sdf_time.format(new Date(enddata * 1000)));
			holder.item_msg.setText("");
			holder.item_msg.setVisibility(View.GONE);
			return convertView;
		}
		class ViewHodler{
			TextView item_name;
			TextView item_date;
			TextView item_time;
			TextView item_msg;
			ImageView mylesson_item_icon;
		}
		public void setCurrPosition(int currPosition){
			this.currPosition = currPosition;
			notifyDataSetChanged();
		}
		
	}


}
