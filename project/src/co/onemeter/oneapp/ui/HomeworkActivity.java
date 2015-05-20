package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.w3c.dom.Text;
import org.wowtalk.api.Classroom;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.HomeworkState;
import org.wowtalk.api.LessonAddHomework;
import org.wowtalk.api.LessonHomework;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.Moment;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 家庭作业作业页面.
 * Created by pzy on 11/10/14. Modified by yl on 23/12/2014
 */
public class HomeworkActivity extends Activity implements OnClickListener, OnItemClickListener{
	private static final int REQ_PARENT_ADDHOMEWORK = 100;
	private TextView tv_class_name;
	private TextView tv_lesson_name;
	private TextView tv_addhomework_state;
	private int lessonId;
//	private List<LessonHomework> lessonHomeworkz;
	private List<Map<String, Object>> homeworkStates;
	private HomeworkStateAdapter adapter;
//	private List<String> homeworktitles;
	private AQuery q;
//	private HomeWorkArrayAdater adapter;
//
	private ListView lvHomework;
	private Database mDb;
	private LessonAddHomework addHomework ;
	private MessageBox msgbox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
		initView();
		getHomeworkState(lessonId);
	}

	private void initView() {
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
		lvHomework = (ListView) findViewById(R.id.lv_homework);
		q = new AQuery(this);
		mDb = new Database(this);
		msgbox = new MessageBox(this);
		q.find(R.id.title_back).clicked(this);
		q.find(R.id.layout_sign_class).clicked(this);

		homeworkStates = new ArrayList<Map<String, Object>>();
		tv_class_name = (TextView) findViewById(R.id.tv_class_name);
		tv_lesson_name = (TextView) findViewById(R.id.tv_lesson_name);
		tv_class_name.setText(getIntent().getStringExtra("class_name"));
		tv_lesson_name.setText(getIntent().getStringExtra("lesson_name"));
		tv_addhomework_state = (TextView) findViewById(R.id.tv_addhomework_state);
		lvHomework.setOnItemClickListener(this);
		
		addHomework = mDb.fetchLessonAddHomework(lessonId);
		if(addHomework == null){
			tv_addhomework_state.setText("未布置");
		}else{
			tv_addhomework_state.setText("已布置");
		}
	}

	private void getHomeworkState(final int lessonId){
		msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, List<Map<String, Object>>>() {
			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {
				List<Map<String, Object>> result= LessonWebServerIF.getInstance(HomeworkActivity.this).get_homework_state(lessonId);
				return result;
			}
			protected void onPostExecute(List<Map<String, Object>> result) {
				msgbox.dismissWait();
				homeworkStates.clear();
				homeworkStates.addAll(result);
				adapter = new HomeworkStateAdapter(homeworkStates);
				adapter.notifyDataSetChanged();
				lvHomework.setAdapter(adapter);
				ListViewUtils.setListViewHeightBasedOnChildren(lvHomework);
				
			}
			
		});
	}
//	private void getLessonHomework(final int lessonId) {
//		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
//
//			@Override
//			protected Integer doInBackground(Void... params) {
//				try {
//					return LessonWebServerIF.getInstance(
//							HomeworkActivity.this).getLessonHomework(lessonId);
//				}
//				catch (Exception e) {
//					return ErrorCode.OK;
//				}
//			}
//
//			protected void onPostExecute(Integer result) {
//                if(result == ErrorCode.OK){
//                    Database db = Database.getInstance(HomeworkActivity.this);
//                    lessonHomeworkz = db.fetchLessonHomework(lessonId);
//                    for (int i = 0; i < lessonHomeworkz.size(); i++) {
//                        homeworktitles.add(i + 1 + "." + lessonHomeworkz.get(i).title);
//                    }
//                    adapter = new HomeWorkArrayAdater(HomeworkActivity.this, homeworktitles);
//                    if (Utils.isAccoTeacher(HomeworkActivity.this)) {
//                        lvHomework.addFooterView(footerView());
//                    }
//                    lvHomework.setAdapter(adapter);
//                }
//
//
//			}
//
//			;
//		});
//	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;
			
		case R.id.layout_sign_class:	
			if(addHomework == null){
				Intent i = new Intent(HomeworkActivity.this, AddHomeworkActivity.class);
				i.putExtra("lessonId",lessonId);
				startActivityForResult(i, REQ_PARENT_ADDHOMEWORK);
			}else{
				Moment moment = mDb.fetchMoment(addHomework.moment_id + "");
				FeedbackDetailActivity.launch(HomeworkActivity.this,moment,null,"布置作业");
			}
			
			break;
		case R.id.lay_footer_add:
//			showAddHomeworkDialog();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(requestCode == REQ_PARENT_ADDHOMEWORK){
				getHomeworkState(lessonId);
				tv_addhomework_state.setText("已布置");
			}
		}
	}
//	private void showAddHomeworkDialog() {
//        ContextThemeWrapper themeWrapper = null;
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
//            themeWrapper = new ContextThemeWrapper(this,android.R.style.Theme_Holo_Light_Dialog);
//        }else{
//            themeWrapper = new ContextThemeWrapper(this,android.R.style.Theme_Light);
//        }
//		AlertDialog.Builder dialog = new AlertDialog.Builder(themeWrapper);
//		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
//		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
//		edName.setMinLines(3);
//		view.findViewById(R.id.lay_dialog_date).setVisibility(View.GONE);
//		TextView tv =  (TextView)view.findViewById(R.id.txt_dialog_first);
//		tv.setText(getString(R.string.class_add_homework));
//		dialog.setView(view);
//		dialog.setTitle(getString(R.string.class_add_homework))
//				.setPositiveButton(getString(R.string.confirm),
//						new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog,
//									int which) {
//								LessonHomework homework = new LessonHomework();
//								homework.lesson_id = lessonId;
//								homework.title = edName.getText().toString();
//								addPostHomework(homework);
//							}
//						}).setNegativeButton(getString(R.string.cancel), null)
//				.create();
//		dialog.show();
//	}
//
//	private void addPostHomework(final LessonHomework homework) {
//		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
//
//			@Override
//			protected Integer doInBackground(Void... params) {
//				return LessonWebServerIF.getInstance(HomeworkActivity.this)
//						.addOrModifyLessonHomework(homework);
//			}
//
//			protected void onPostExecute(Integer result) {
//				if (ErrorCode.OK == result) {
//					homeworktitles.clear();
//					lessonHomeworkz.clear();
//					lessonHomeworkz.addAll(Database.getInstance(HomeworkActivity.this).fetchLessonHomework(lessonId));
//					for (int i = 0; i < lessonHomeworkz.size(); i++) {
//						homeworktitles.add(i + 1 + "." + lessonHomeworkz.get(i).title);
//					}
//					adapter.notifyDataSetChanged();
//				}
//			}
//
//			;
//
//		});
//	}
//
//	private View footerView() {
//		View view = getLayoutInflater().inflate(R.layout.lay_lv_footer, null);
//		TextView txt_footer = (TextView) view.findViewById(R.id.txt_footer_add);
//		txt_footer.setText(getString(R.string.class_add_homework));
//		LinearLayout layout = (LinearLayout) view
//				.findViewById(R.id.lay_footer_add);
//		layout.setOnClickListener(this);
//		return view;
//	}
//
//    class HomeWorkArrayAdater extends BaseAdapter{
//
//        private Context mContext;
//        private List<String> mStrings;
//        public HomeWorkArrayAdater(Context context, List<String> strings) {
//            //super(context,android.R.layout.simple_list_item_1,android.R.id.text1,strings);
//            mContext = context;
//            mStrings = strings;
//        }
//
//        @Override
//        public int getCount() {
//            return mStrings.size();
//        }
//
//        @Override
//        public Object getItem(int position) {
//            return mStrings.get(position);
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//        /**
//         * 将textview字体颜色设为黑色
//         * @param position
//         * @param convertView
//         * @param parent
//         * @return
//         */
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            ViewHolder holder = null;
//            if(holder == null){
//                holder = new ViewHolder();
//                convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1,parent,false);
//                holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
//                convertView.setTag(holder);
//            }else{
//                holder = (ViewHolder)convertView.getTag();
//            }
//            holder.textView.setText(mStrings.get(position));
//            holder.textView.setTextColor(0xff000000);
//            holder.textView.setTextSize(15);
//            return convertView;
//        }
//
//        private class ViewHolder{
//            TextView textView;
//        }
//    }
	class HomeworkStateAdapter extends BaseAdapter{
		private List<Map<String, Object>> homeworkStates;
		public HomeworkStateAdapter(List<Map<String, Object>> homeworkStates){
			this.homeworkStates = homeworkStates;
		}
		@Override
		public int getCount() {
			return homeworkStates.size();
		}

		@Override
		public Object getItem(int position) {
			return homeworkStates.get(position);
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
				convertView = getLayoutInflater().inflate(R.layout.listitem_homework_state, parent, false);
				holder.tv_student_name = (TextView) convertView.findViewById(R.id.tv_student_name);
				holder.tv_homework_state = (TextView) convertView.findViewById(R.id.tv_homework_state);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
//			Classroom cl = homeworkStates.get(position);
//			holder.classroom_item_name.setText(cl.room_name+" - "+cl.room_num);
//			holder.classroom_item_icon.setVisibility(View.INVISIBLE);		
			int state = Integer.parseInt(String.valueOf(homeworkStates.get(position).get("stu_state")));
			String stateStr = null;
			if(state == 0){
				stateStr = "提醒交作业";
			}else if(state == 1){
				stateStr = "新提交";
			}else if(state == 2){
				stateStr = "已批改";
			}
			holder.tv_student_name.setText(String.valueOf(homeworkStates.get(position).get("stu_name")));
			holder.tv_homework_state.setText(stateStr);
			return convertView;
		}
		class ViewHodler{
			TextView tv_student_name;
			TextView tv_homework_state;
		}		
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.d("-------------homeworkStates--------------", homeworkStates.get(0).get("homework_id")+"");
	}
}