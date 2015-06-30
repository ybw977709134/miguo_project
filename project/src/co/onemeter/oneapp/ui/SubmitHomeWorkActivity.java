package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.GetLessonHomework;
import org.wowtalk.api.HomeWorkResult;
import org.wowtalk.api.HomeWorkReview;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.HomeWorkAdapter;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * Created by hutianfeng on 2015/5/29
 */
public class SubmitHomeWorkActivity extends Activity implements View.OnClickListener {
	private static final int REQ_PARENT_ADDHOMEWORKREVIEW = 100;
    //返回
    private ImageButton title_back;
    private TextView textView_homework_back;
    private TextView contact_current_group;
    private Button btn_homework_state;


    private ListView listView_submit;

    private GetLessonHomework getLessonHomework;
    private HomeWorkAdapter adapter;
    private ImageResizer imageResizer;
    private List<HomeWorkResult> stuResultList;
    private List<String> teaReview;
    List<HomeWorkResult> unStuResultList;//反序

    private int lessonId;//需要传进来
    private String schoolId;
    private int result_id;
    private String student_uid;
    private String stu_name;
    private int homework_id;
    private String teacherId;
    private String lesson_name;
    private String class_name;
    private static SubmitHomeWorkActivity instance;
    private MessageBox messageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_home_work);
        messageBox = new MessageBox(this);
        initView();
    }
    
    public static SubmitHomeWorkActivity getInstance(){
    	if(instance != null){
    		return instance;
    	}
    	return null;
    }

    /**
     * 初始化数据
     */
    private void initView () {
    	instance = this;
    	student_uid = getIntent().getStringExtra("student_uid");
    	schoolId = getIntent().getStringExtra("schoolId");
    	stu_name = getIntent().getStringExtra("stu_name");
    	lesson_name = getIntent().getStringExtra("lesson_name");
    	class_name = getIntent().getStringExtra("class_name");
    	lessonId = getIntent().getIntExtra(Constants.LESSONID, 0);
    	result_id = getIntent().getIntExtra("result_id", 0);
        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_homework_back = (TextView) findViewById(R.id.textView_homework_back);
        contact_current_group = (TextView) findViewById(R.id.contact_current_group);
        contact_current_group.setText(stu_name);
        btn_homework_state = (Button) findViewById(R.id.btn_homework_state);

        listView_submit = (ListView) findViewById(R.id.listView_submit);

        getLessonHomework = new GetLessonHomework();
        stuResultList = new ArrayList<HomeWorkResult>();
        teaReview = new ArrayList<String>();
        unStuResultList = new ArrayList<HomeWorkResult>();

        imageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));

        fillListView();

        title_back.setOnClickListener(this);
        textView_homework_back.setOnClickListener(this);
        btn_homework_state.setOnClickListener(this);

    }


    private void fillListView(){
        messageBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int status = LessonWebServerIF.getInstance(SubmitHomeWorkActivity.this).getLessonHomeWork(lessonId, getLessonHomework,student_uid,2);
                return status;
            }

            @Override
            protected void onPostExecute(Integer result) {
                messageBox.dismissWait();
                if (result == 1) {//成功
                    stuResultList.clear();
                    unStuResultList.clear();
                    stuResultList.addAll(getLessonHomework.stuResultList);
                    for(int i = 0;i < getLessonHomework.stuResultList.size();i++){
                    	if(getLessonHomework.stuResultList.get(i).homeWorkReview != null){
                    		teaReview.add(getLessonHomework.stuResultList.get(i).homeWorkReview.text);
                    	}
                    	 
                    }
                    for (int i = stuResultList.size() -1; i >= 0; i--) {
                    	unStuResultList.add(stuResultList.get(i));
                    }
                    homework_id = getLessonHomework.id;
                    teacherId = getLessonHomework.teacher_id;
                    
                    if (adapter == null) {
                        adapter = new HomeWorkAdapter(SubmitHomeWorkActivity.this,unStuResultList,imageResizer,getLessonHomework);
                    }

                    listView_submit.setAdapter(adapter);

//                    if(PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
//
//                    	 if(teaReview.size() != stuResultList.size()){
//                    		 listView_submit.addFooterView(buildHeader());//为listview的后面添加button
//                         }
//
//                    }else{//学生账号可以一直修改
//                    	listView_submit.addFooterView(buildHeader());//为listview的后面添加button
//                    }

                    //对老师评分的按钮和学生修改作业的按钮做修改
                    if(PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){

                        if(teaReview.size() != stuResultList.size()){
                            btn_homework_state.setVisibility(View.VISIBLE);
                        } else {
                            btn_homework_state.setVisibility(View.GONE);
                        }

                        btn_homework_state.setText("去评分");

                    }else{//学生账号可以一直修改
                        btn_homework_state.setVisibility(View.VISIBLE);
                        btn_homework_state.setText("修改作业");
                    }

                } else {
                    Toast.makeText(SubmitHomeWorkActivity.this,"网络请求失败",Toast.LENGTH_SHORT).show();
                }

            }

        });
    }

//    private View buildHeader() {
//        Button btn=null;
//        View view = LayoutInflater.from(SubmitHomeWorkActivity.this).inflate(R.layout.item_custom_button,null);
//        btn = (Button) view.findViewById(R.id.custom_button);
//
//        if (PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
//            btn.setText("去评分");
//        } else {
//            btn.setText("修改作业");
//        }
//
//        btn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
//                    //跳转到老师评分
//                    Intent intent = new Intent(SubmitHomeWorkActivity.this,HomeWorkEvaluate.class);
//                    intent.putExtra("homeworkresult_id", result_id);
//                    intent.putExtra("stu_name", stu_name);
//                    intent.putExtra("student_uid", student_uid);
//                    intent.putExtra("class_name", class_name);
//                    intent.putExtra("lesson_name", lesson_name);
//                    intent.putExtra("schoolId",schoolId);
//                    //传一些参数
//                    startActivityForResult(intent, REQ_PARENT_ADDHOMEWORKREVIEW);
//                } else {
//                   //跳转到学生修改页面
//                	Intent intent = new Intent(SubmitHomeWorkActivity.this,SignHomeworkResultkActivity.class);
//                    intent.putExtra("homework_id", homework_id);
//                    intent.putExtra("lesson_name", lesson_name);
//                    intent.putExtra("teacherID", teacherId);
//                    intent.putExtra("class_name", class_name);
//                    intent.putExtra("schoolId",schoolId);
//                    startActivity(intent);
//                }
//
//            }
//        });
//
//        return(view);
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == Activity.RESULT_OK){
			if(requestCode == REQ_PARENT_ADDHOMEWORKREVIEW){
				fillListView();
			}
		}
    }

    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	if(instance != null){
        	instance = null;
        }
    }
    
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.title_back:
            case R.id.textView_homework_back:
                finish();
                break;

            case R.id.btn_homework_state:

                if (PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
                    //跳转到老师评分
                    Intent intent = new Intent(SubmitHomeWorkActivity.this,HomeWorkEvaluate.class);
                    intent.putExtra("homeworkresult_id", result_id);
                    intent.putExtra("stu_name", stu_name);
                    intent.putExtra("student_uid", student_uid);
                    intent.putExtra("class_name", class_name);
                    intent.putExtra("lesson_name", lesson_name);
                    intent.putExtra("schoolId",schoolId);
                    //传一些参数
                    startActivityForResult(intent, REQ_PARENT_ADDHOMEWORKREVIEW);
                } else {
                    //跳转到学生修改页面
                    Intent intent = new Intent(SubmitHomeWorkActivity.this,SignHomeworkResultkActivity.class);
                    intent.putExtra("homework_id", homework_id);
                    intent.putExtra("lesson_name", lesson_name);
                    intent.putExtra("teacherID", teacherId);
                    intent.putExtra("class_name", class_name);
                    intent.putExtra("schoolId",schoolId);
                    startActivity(intent);
                }
                break;
        }
    }
    
    
}



