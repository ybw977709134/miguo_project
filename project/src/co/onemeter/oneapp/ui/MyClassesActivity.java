package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.liveplayer.VideoPlayingActivity;
import com.androidquery.AQuery;
import org.wowtalk.api.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 我的课堂页面。
 * Created by pzy on 11/10/14.
 */
public class MyClassesActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MyClassesActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myclasses);

        AQuery q = new AQuery(this);

        q.find(R.id.title_back).clicked(this);
        q.find(R.id.livingClass).clicked(this);

        test();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
            case R.id.livingClass:
            	Intent intent = new Intent(this, VideoPlayingActivity.class);
            	startActivity(intent);
            	break;
        }
    }

    // 测试课堂相关的API
    private void test() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                final String classId = "0b2f933f-a4d7-44de-a711-569abb04846a";
                WowLessonWebServerIF lessonWeb = WowLessonWebServerIF.getInstance(MyClassesActivity.this);
                Database db = new Database(MyClassesActivity.this);
                String myUid = PrefUtil.getInstance(MyClassesActivity.this).getUid();

                // add lesson1
                Lesson lesson1 = new  Lesson();
                lesson1.class_id = classId;
                lesson1.title = "第一课";
                lesson1.start_date = new Date(2014 - 1900, 11, 29, 9, 0).getTime() / 1000;
                lesson1.end_date = lesson1.start_date + 45 * 60;
                lessonWeb.addOrModifyLesson(lesson1);

                // add lesson2
                Lesson lesson2 = new  Lesson();
                lesson2.class_id = classId;
                lesson2.title = "第二课";
                lesson2.start_date = new Date(2015 - 1900, 0, 8, 14, 30).getTime() / 1000;
                lesson2.end_date = lesson2.start_date + 45 * 60;
                lessonWeb.addOrModifyLesson(lesson2);

                // get lessons
                lessonWeb.getLesson(classId);

                // modify lesson1
                lesson1.title = "lesson 1";
                lessonWeb.addOrModifyLesson(lesson1);

                // get lessons
                lessonWeb.getLesson(classId);

                List<Lesson> lessonList = db.fetchLesson(classId);
                for (Lesson l : lessonList) {
                    Log.i(String.format("%s %d %s %d %d", l.class_id, l.lesson_id,
                            l.title, l.start_date, l.end_date));
                }

                // performance

                List<LessonPerformance> performances = new LinkedList<>();
                String[] names = getResources().getStringArray(R.array.lesson_performance_names);
                for (int i = 0; i < names.length; ++i) {
                    LessonPerformance lp = new LessonPerformance();
                    lp.lesson_id = lesson1.lesson_id;
                    lp.student_id = myUid;
                    lp.property_id = i + 1;
                    lp.property_value = 1 + i % 3;
                    performances.add(lp);
                }
                lessonWeb.addOrModifyLessonPerformance(performances);

                lessonWeb.getLessonPerformance(lesson1.lesson_id, myUid);
                List<LessonPerformance> performances2 = db.fetchLessonPerformance(lesson1.lesson_id, myUid);
                for (LessonPerformance p : performances2) {
                    Log.i(String.format("%d %s %d %s %d", p.lesson_id, p.student_id, p.property_id,
                            names[p.property_id - 1], p.property_value));
                }

                // homework
                LessonHomework homework = new LessonHomework();
                homework.lesson_id = lesson1.lesson_id;
                homework.title = "家庭作业，#1";
                lessonWeb.addOrModifyLessonHomework(homework);
                LessonHomework homework2 = new LessonHomework();
                homework2.lesson_id = lesson1.lesson_id;
                homework2.title = "家庭作业，#2";
                lessonWeb.addOrModifyLessonHomework(homework2);

                lessonWeb.getLessonHomework(lesson1.lesson_id);
                List<LessonHomework> homeworks = db.fetchLessonHomework(lesson1.lesson_id);
                for (LessonHomework h : homeworks) {
                    Log.i(String.format("%s %d %s", h.lesson_id, h.homework_id, h.title));
                }

                // feedback

                LessonParentFeedback feedback = new LessonParentFeedback();
                feedback.lesson_id = lesson1.lesson_id;
                feedback.student_id = myUid;
                Moment moment = new Moment(null);
                moment.text = "很好";
                lessonWeb.addOrModifyLessonParentFeedback(feedback, moment);

                lessonWeb.getLessonParentFeedback(lesson1.lesson_id, myUid);
                LessonParentFeedback feedback2 = db.fetchLessonParentFeedback(lesson1.lesson_id, myUid);
                Moment moment2 = db.fetchMoment(Integer.toString(feedback2.moment_id));
                Log.i(String.format("%d %s %d %s", feedback2.lesson_id, feedback2.student_id,
                        feedback2.moment_id, moment2.text));

                return null;
            }
        }.execute((Void)null);
    }
}