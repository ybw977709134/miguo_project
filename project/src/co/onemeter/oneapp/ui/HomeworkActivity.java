package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonHomework;
import org.wowtalk.api.WowLessonWebServerIF;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;

import com.androidquery.AQuery;

/**
 * 家庭作业作业页面。 Created by pzy on 11/10/14. Modified by yl on 23/12/2014
 */
public class HomeworkActivity extends Activity implements View.OnClickListener {
	private int lessonId;
	private List<LessonHomework> lessonHomeworkz;
	private List<String> homeworktitles;
	private AQuery q;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homework);
		initView();
	}

	private void initView() {
		lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);

		q = new AQuery(this);
		q.find(R.id.title_back).clicked(this);

		getLessonHomework(lessonId);

		homeworktitles = new ArrayList<String>();

	}

	private void getLessonHomework(final int lessonId) {
		new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowLessonWebServerIF.getInstance(HomeworkActivity.this)
						.getLessonHomework(lessonId);
			}

			protected void onPostExecute(Integer result) {
				if (ErrorCode.OK == result) {
					Database db = Database.getInstance(HomeworkActivity.this);
					lessonHomeworkz = db.fetchLessonHomework(lessonId);
					for (int i = 0; i < lessonHomeworkz.size(); i++) {
						homeworktitles.add(i + 1 + "."
								+ lessonHomeworkz.get(i).title);
					}
					ArrayAdapter<String> adapter = new ArrayAdapter<>(
							HomeworkActivity.this,
							android.R.layout.simple_list_item_1,
							android.R.id.text1, homeworktitles);
					q.find(R.id.lv_homework).adapter(adapter);
				}
			};
		}.execute((Void) null);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;
		default:
			break;
		}
	}
}