package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;

import org.w3c.dom.Text;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonHomework;
import org.wowtalk.api.LessonWebServerIF;

import java.util.ArrayList;
import java.util.List;

/**
 * 家庭作业作业页面.
 * Created by pzy on 11/10/14. Modified by yl on 23/12/2014
 */
public class HomeworkActivity extends Activity implements View.OnClickListener {
	private int lessonId;
	private List<LessonHomework> lessonHomeworkz;
	private List<String> homeworktitles;
	private AQuery q;
	private HomeWorkArrayAdater adapter;

	private ListView lvHomework;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
		initView();
	}

	private void initView() {
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);

		lvHomework = (ListView) findViewById(R.id.lv_homework);
		q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);

		homeworktitles = new ArrayList<String>();
		getLessonHomework(lessonId);
	}

	private void getLessonHomework(final int lessonId) {
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				try {
					return LessonWebServerIF.getInstance(
							HomeworkActivity.this).getLessonHomework(lessonId);
				}
				catch (Exception e) {
					return ErrorCode.BAD_RESPONSE;
				}
			}

			protected void onPostExecute(Integer result) {
                if(result == ErrorCode.OK){
                    Database db = Database.getInstance(HomeworkActivity.this);
                    lessonHomeworkz = db.fetchLessonHomework(lessonId);
                    for (int i = 0; i < lessonHomeworkz.size(); i++) {
                        homeworktitles.add(i + 1 + "." + lessonHomeworkz.get(i).title);
                    }
                    adapter = new HomeWorkArrayAdater(HomeworkActivity.this, homeworktitles);
                    if (Utils.isAccoTeacher(HomeworkActivity.this)) {
                        lvHomework.addFooterView(footerView());
                    }
                    lvHomework.setAdapter(adapter);
                }


			}

			;
		});
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;

		case R.id.lay_footer_add:
			showAddHomeworkDialog();
			break;
		default:
			break;
		}
	}

	private void showAddHomeworkDialog() {
        ContextThemeWrapper themeWrapper = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            themeWrapper = new ContextThemeWrapper(this,android.R.style.Theme_Holo_Light_Dialog);
        }else{
            themeWrapper = new ContextThemeWrapper(this,android.R.style.Theme_Light);
        }
		AlertDialog.Builder dialog = new AlertDialog.Builder(themeWrapper);
		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
		edName.setMinLines(3);
		view.findViewById(R.id.lay_dialog_date).setVisibility(View.GONE);
		TextView tv =  (TextView)view.findViewById(R.id.txt_dialog_first);
		tv.setText(getString(R.string.class_add_homework));
		dialog.setView(view);
		dialog.setTitle(getString(R.string.class_add_homework))
				.setPositiveButton(getString(R.string.confirm),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								LessonHomework homework = new LessonHomework();
								homework.lesson_id = lessonId;
								homework.title = edName.getText().toString();
								addPostHomework(homework);
							}
						}).setNegativeButton(getString(R.string.cancel), null)
				.create();
		dialog.show();
	}

	private void addPostHomework(final LessonHomework homework) {
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return LessonWebServerIF.getInstance(HomeworkActivity.this)
						.addOrModifyLessonHomework(homework);
			}

			protected void onPostExecute(Integer result) {
				if (ErrorCode.OK == result) {
					homeworktitles.clear();
					lessonHomeworkz.clear();
					lessonHomeworkz.addAll(Database.getInstance(HomeworkActivity.this).fetchLessonHomework(lessonId));
					for (int i = 0; i < lessonHomeworkz.size(); i++) {
						homeworktitles.add(i + 1 + "." + lessonHomeworkz.get(i).title);
					}
					adapter.notifyDataSetChanged();
				}
			}

			;

		});
	}

	private View footerView() {
		View view = getLayoutInflater().inflate(R.layout.lay_lv_footer, null);
		TextView txt_footer = (TextView) view.findViewById(R.id.txt_footer_add);
		txt_footer.setText(getString(R.string.class_add_homework));
		LinearLayout layout = (LinearLayout) view
				.findViewById(R.id.lay_footer_add);
		layout.setOnClickListener(this);
		return view;
	}

    class HomeWorkArrayAdater extends BaseAdapter{

        private Context mContext;
        private List<String> mStrings;
        public HomeWorkArrayAdater(Context context, List<String> strings) {
            //super(context,android.R.layout.simple_list_item_1,android.R.id.text1,strings);
            mContext = context;
            mStrings = strings;
        }

        @Override
        public int getCount() {
            return mStrings.size();
        }

        @Override
        public Object getItem(int position) {
            return mStrings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * 将textview字体颜色设为黑色
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(holder == null){
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1,parent,false);
                holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }
            holder.textView.setText(mStrings.get(position));
            holder.textView.setTextColor(0xff000000);
            holder.textView.setTextSize(15);
            return convertView;
        }

        private class ViewHolder{
            TextView textView;
        }
    }
}