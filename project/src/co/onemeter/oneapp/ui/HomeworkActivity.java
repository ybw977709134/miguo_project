package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonHomework;
import org.wowtalk.api.LessonWebServerIF;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

import com.androidquery.AQuery;

/**
 * 家庭作业作业页面。 Created by pzy on 11/10/14. Modified by yl on 23/12/2014
 */
public class HomeworkActivity extends Activity implements View.OnClickListener {
	private int lessonId;
	private List<LessonHomework> lessonHomeworkz;
	private List<String> homeworktitles;
	private AQuery q;
	private ArrayAdapter<String> adapter;

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
		new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				try {
					return LessonWebServerIF.getInstance(
							HomeworkActivity.this).getLessonHomework(lessonId);
				} catch (Exception e) {
					return ErrorCode.BAD_RESPONSE;
				}
			}

			protected void onPostExecute(Integer result) {
				Database db = Database.getInstance(HomeworkActivity.this);
				lessonHomeworkz = db.fetchLessonHomework(lessonId);
				for (int i = 0; i < lessonHomeworkz.size(); i++) {
					homeworktitles.add(i + 1 + "." + lessonHomeworkz.get(i).title);
				}
				adapter = new ArrayAdapter<>(HomeworkActivity.this,
						android.R.layout.simple_list_item_1,
						android.R.id.text1, homeworktitles);
				if (Utils.isAccoTeacher(HomeworkActivity.this)) {
					lvHomework.addFooterView(footerView());
				}
				lvHomework.setAdapter(adapter);
				
			};
		}.execute((Void) null);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.lay_add_lesson, null);
		final EditText edName = (EditText) view.findViewById(R.id.ed_dialog_time);
		edName.setMinLines(3);
		view.findViewById(R.id.lay_dialog_date).setVisibility(View.GONE);
		TextView tv =  (TextView)view.findViewById(R.id.txt_dialog_first);
		tv.setText(getString(R.string.class_add_homework));
		builder.setView(view);
		builder.setTitle(getString(R.string.class_add_homework))
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
		builder.show();
	}

	private void addPostHomework(final LessonHomework homework) {
		new AsyncTask<Void, Void, Integer>() {

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
						homeworktitles.add(i + 1 + "."+ lessonHomeworkz.get(i).title);
					}
					adapter.notifyDataSetChanged();
				}
			};

		}.execute((Void) null);
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
}