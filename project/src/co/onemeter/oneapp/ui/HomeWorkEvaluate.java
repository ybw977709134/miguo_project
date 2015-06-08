package co.onemeter.oneapp.ui;

import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

public class HomeWorkEvaluate extends Activity implements View.OnClickListener {
    private ImageButton title_back;//返回
    private TextView textView_homework_back;//返回
    private TextView textView_homework_send;//发送

    RatingBar ratingBar_confirm;//完整性
    RatingBar ratingBar_timely;//及时性
    RatingBar ratingBar_exact;//准确性

    private Button btn_comment;//评语模板
    private EditText editText_comment;//评语

    private static int REQ_TEMPLATE = 1;//评语模板请求


    //星星评分对应的数值
    private int confirmNum;
    private int timelyNum;
    private int exactNum;
    
    private MessageBox mMsgBox;
    private int homeworkresult_id;
    private String stu_name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_work_evaluate);
        initView();
    }


    private void initView() {
    	homeworkresult_id = getIntent().getIntExtra("homeworkresult_id", 0);
    	stu_name = getIntent().getStringExtra("stu_name");
    	mMsgBox = new MessageBox(this);
        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_homework_back = (TextView) findViewById(R.id.textView_homework_back);
        textView_homework_send = (TextView) findViewById(R.id.textView_homework_send);

        ratingBar_confirm = (RatingBar) findViewById(R.id.ratingBar_confirm);
        ratingBar_timely = (RatingBar) findViewById(R.id.ratingBar_timely);
        ratingBar_exact = (RatingBar) findViewById(R.id.ratingBar_exact);

        btn_comment = (Button) findViewById(R.id.btn_comment);
        editText_comment = (EditText) findViewById(R.id.editText_comment);

        textView_homework_back.setText(stu_name);
        editText_comment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 20) {
                    Toast.makeText(HomeWorkEvaluate.this,"评语不能超过20字",Toast.LENGTH_SHORT).show();
                    editText_comment.setFocusable(false);
                    editText_comment.setFocusableInTouchMode(false);
                } else {
                    editText_comment.setFocusable(false);
                    editText_comment.setFocusableInTouchMode(false);
                    editText_comment.requestFocus();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        //为各个控件添加监听事件
        title_back.setOnClickListener(this);
        textView_homework_back.setOnClickListener(this);
        textView_homework_send.setOnClickListener(this);
        ratingBar_confirm.setOnRatingBarChangeListener(new RatingBarListener());
        ratingBar_timely.setOnRatingBarChangeListener(new RatingBarListener());
        ratingBar_exact.setOnRatingBarChangeListener(new RatingBarListener());

        btn_comment.setOnClickListener(this);

    }

    private void addHomeworkReview(){
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int status= LessonWebServerIF.getInstance(HomeWorkEvaluate.this).addHomeworkReview(homeworkresult_id,confirmNum, timelyNum, exactNum, editText_comment.getText().toString());		
				return status;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				if(result == 1){
					mMsgBox.showWaitImageSuccess("作业提交失败");
				}else if(result == 0){
					setResult(RESULT_OK);
					mMsgBox.showWaitImageSuccess("作业提交成功");
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(3000);
								finish();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
					}).start();
				}
			}
			
		});
    }
    private class RatingBarListener implements RatingBar.OnRatingBarChangeListener{

        @Override
        public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
            switch (ratingBar.getId()) {
                case R.id.ratingBar_confirm://完整性
                    confirmNum = (int) rating;
                    break;

                case R.id.ratingBar_timely://及时性
                    timelyNum = (int) rating;
                    break;

                case R.id.ratingBar_exact://准确性
                    exactNum = (int) rating;
                    break;
            }

        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back://返回
            case R.id.textView_homework_back:
                finish();
                break;


            case R.id.textView_homework_send://发送
            	addHomeworkReview();
                break;

            case R.id.btn_comment://获得模板
                Intent intent = new Intent(this,HomeWorkTemplate.class);
                startActivityForResult(intent,REQ_TEMPLATE);
                break;

        }
    }

    //如果从评语模板中选择评语，会把评语设置在edittext控件
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                if (requestCode == REQ_TEMPLATE) {
                   String text = data.getStringExtra("template");
                    editText_comment.setText(text);
                }
                break;

        }
    }
}
