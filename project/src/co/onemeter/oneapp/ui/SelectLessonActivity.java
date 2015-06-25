package co.onemeter.oneapp.ui;


import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Lesson;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
	private String classId = null;
	private String homeworkTag = null;
	
	private SelectLessonActivity instance;
	private long curTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_lesson);
		instance = this;
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
		classId = getIntent().getStringExtra("classId");
		homeworkTag = getIntent().getStringExtra("homework");
		curTime = System.currentTimeMillis()/1000;
//		for(int i =0 ;i < mDBHelper.fetchLesson(classId).size();i++){
//			if(curTime < mDBHelper.fetchLesson(classId).get(i).start_date){
//				lessons.add(mDBHelper.fetchLesson(classId).get(i));
//			}
//		}
////		lessons.addAll(mDBHelper.fetchLesson(classId));
//		Collections.sort(lessons, new LessonComparator());
//		adapter.notifyDataSetChanged();
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

                    //如果没有课程数据将给出应有的提示
                    if (lessons == null || lessons.size() == 0) {
                        MessageDialog dialog = new MessageDialog(SelectLessonActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setMessage("该班级还没有开设任何课程!");
                        dialog.setCancelable(false);
                        dialog.setOnLeftClickListener("返回", new MessageDialog.MessageDialogClickListener() {
                            @Override
                            public void onclick(MessageDialog dialog) {
                                dialog.dismiss();
                                finish();
                            }
                        });

                        dialog.show();
                    }


				};

			});


	}
	
	private void refreshLessonInfo(){
		lessons.clear();
		Database db = Database.open(SelectLessonActivity.this);
		if(isTeacher()){ //老师账号 签到 在线作业 显示全部课程
			lessons.addAll(db.fetchLesson(classId));
		}else{           //学生账号  在线作业 显示全部课程；请假 只显示还未结束的课
			if(homeworkTag != null){
				lessons.addAll(db.fetchLesson(classId));
			}else{
				for(int i =0 ;i < mDBHelper.fetchLesson(classId).size();i++){
					if(curTime < mDBHelper.fetchLesson(classId).get(i).end_date){
						lessons.add(mDBHelper.fetchLesson(classId).get(i));
						}
					}
			}
				
		}
		
//		lessons.addAll(db.fetchLesson(classId));
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
		
		Intent data = new Intent();
		data.putExtra("lesson_id", lessons.get(position).lesson_id);
		data.putExtra("lesson_name", lessons.get(position).title);
		data.putExtra("end_date", lessons.get(position).end_date);
		setResult(RESULT_OK, data);
		
		instance.finish();
		
	}
	
	private boolean isTeacher(){
		if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
			return true;
		}
		return false;
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

			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.item_select_lesson, parent, false);
				
				holder.textView_item_lesson_name = (TextView) convertView.findViewById(R.id.textView_item_lesson_name);
				holder.textView_item_lesson_date = (TextView) convertView.findViewById(R.id.textView_item_lesson_date);
				holder.imageView_item_lesson_icon =  (ImageView) convertView.findViewById(R.id.imageView_item_lesson_icon);
				
				convertView.setTag(holder);
				
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			
			Lesson lesson = alessons.get(position);
			long startdate = lesson.start_date;
			
			holder.textView_item_lesson_name.setText(lesson.title);
			holder.textView_item_lesson_name.setTextColor(getResources().getColor(R.color.black_24));
			if(homeworkTag != null){
				if(homeworkTag.equals("homework")){
					holder.textView_item_lesson_date.setVisibility(View.VISIBLE);
					holder.textView_item_lesson_date.setText(Utils.stampsToDate(startdate));
				}
			}
			
			if(currPosition == position){
				holder.imageView_item_lesson_icon.setVisibility(View.VISIBLE);
			}else{
				holder.imageView_item_lesson_icon.setVisibility(View.INVISIBLE);
			}

			return convertView;
		}
		class ViewHodler{
			TextView textView_item_lesson_name;
			TextView textView_item_lesson_date;
			ImageView imageView_item_lesson_icon;
		}
		public void setCurrPosition(int currPosition){
			this.currPosition = currPosition;
			notifyDataSetChanged();
		}
		
		
	}
	
	@Override
	protected void onDestroy() {
		if (instance != null) {
			instance = null;
		}
		
		if (lessons != null) {
			lessons = null;
		}
		
		if (mDBHelper != null) {
			mDBHelper = null;
		}
		super.onDestroy();
	}


}
