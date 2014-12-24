package co.onemeter.oneapp.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowLessonWebServerIF;
import org.wowtalk.ui.BottomButtonBoard;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;

/**
 * 课堂详情页面。
 * Created by yl on 21/12/2014.
 */
public class ClassDetailActivity extends Activity implements OnClickListener, OnItemClickListener {
	private AQuery query;
	private WowLessonWebServerIF lesWebSer;
	
	private String classId;
	private List<Lesson> lessons;
	private CourseTableAdapter courseAdapter;

	private ListView lvLessonTable;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_detail);
		
		initView();
		
	}

	private void initView(){
		query = new AQuery(this);
		lesWebSer = WowLessonWebServerIF.getInstance(this);
		lessons = new LinkedList<Lesson>();
		courseAdapter = new CourseTableAdapter(lessons);
		
		Intent intent = getIntent();
		classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
		//classId = intent.getStringExtra("classroomId");
		query.find(R.id.class_detail_title).text(intent.getStringExtra("classroomName"));
		query.find(R.id.title_back).clicked(this);
		query.find(R.id.class_live_class).clicked(this);
		query.find(R.id.more).clicked(this);
		
		lvLessonTable = (ListView) findViewById(R.id.lvLessonTable);
		lvLessonTable.setAdapter(courseAdapter);
		lvLessonTable.setOnItemClickListener(this);
		
		TextView tv_class_live = (TextView) findViewById(R.id.class_live_class);
		TextView tv_more = (TextView) findViewById(R.id.more);
		if(PrefUtil.getInstance(this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			tv_class_live.setVisibility(View.GONE);
			tv_more.setVisibility(View.VISIBLE);
		}else{
			tv_class_live.setVisibility(View.VISIBLE);
			tv_more.setVisibility(View.GONE);
		}
		
		getLessonInfo();
	}
	
	private void getLessonInfo(){
		new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return lesWebSer.getLesson(classId);
			}
			
			protected void onPostExecute(Integer result) {
				if(ErrorCode.OK == result){
					Database db = Database.open(ClassDetailActivity.this);
					lessons.addAll(db.fetchLesson(classId));
					courseAdapter.notifyDataSetChanged();
				}
			};
			
		}.execute((Void)null);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.class_live_class:
			
			break;
		case R.id.more:
			showMore(v);
			break;
		default:
			break;
		}
	}

	private void showMore(View parentView) {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, parentView);
        // class live
        bottomBoard.add(getString(R.string.class_live_class), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        
                    	
                        bottomBoard.dismiss();
                    }
                });
        // edit class info 
        bottomBoard.add(getString(R.string.class_edit_class_info), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ClassDetailActivity.this, LessonInfoEditActivity.class);
                        intent.putExtra("tag", LessonInfoEditActivity.TAG_CLASS_INFO);
                        startActivity(intent);
                        bottomBoard.dismiss();
                    }
                });
        //edit coursetable info
        bottomBoard.add(getString(R.string.class_edit_cursetable_info), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	Intent intent = new Intent(ClassDetailActivity.this, LessonInfoEditActivity.class);
                    	intent.putExtra("tag", LessonInfoEditActivity.TAG_LES_TABLE);
                        startActivity(intent);
                        bottomBoard.dismiss();
                    }
                });
        //Cancel
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent();
		intent.setClass(this, LessonDetailActivity.class);
		intent.putExtra(Constants.LESSONID, lessons.get(position).lesson_id);
		intent.putExtra("title", lessons.get(position).title);
		startActivity(intent);
	}

}
