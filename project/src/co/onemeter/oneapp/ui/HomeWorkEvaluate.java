package co.onemeter.oneapp.ui;

import java.util.List;
import java.util.Map;

import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

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
import co.onemeter.oneapp.utils.TimeHelper;
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
    private boolean isEvaluate = false;


    //星星评分对应的数值
    private int confirmNum = 0;
    private int timelyNum = 0;
    private int exactNum = 0;
    
    private MessageBox mMsgBox;
    private int homeworkresult_id;
    private String stu_name;
    private String student_uid;
    private String schoolId;
    private String my_uid;
    private String lesson_name;
    private String class_name;
    private Database mDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_work_evaluate);
        initView();
    }


    private void initView() {
    	lesson_name = getIntent().getStringExtra("lesson_name");
		class_name = getIntent().getStringExtra("class_name");
    	schoolId = getIntent().getStringExtra("schoolId");
    	homeworkresult_id = getIntent().getIntExtra("homeworkresult_id", 0);
    	stu_name = getIntent().getStringExtra("stu_name");
    	student_uid = getIntent().getStringExtra("student_uid");
    	my_uid = PrefUtil.getInstance(this).getUid();
    	mMsgBox = new MessageBox(this);
    	mDb = new Database(this);
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
                if (s.length() >= 20 && !isEvaluate) {
                    Toast.makeText(HomeWorkEvaluate.this,"评语不能超过20字",Toast.LENGTH_SHORT).show();                    
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 20) {
                    isEvaluate = false;
                }
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
                    mMsgBox.showWaitImageCaution("请填写评语");
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(2000);
								mMsgBox.dismissWait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
						}
					}).start();
				}else if(result == 0){
					if(confirmNum == 0 && timelyNum == 0 && exactNum == 0){
						mMsgBox.showWaitImageCaution("请填写星星评分");
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									Thread.sleep(2000);
									mMsgBox.dismissWait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
							}
						}).start();
					}else{
						setResult(RESULT_OK);
						mMsgBox.showWaitImageSuccess("评分成功");
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									Thread.sleep(3000);
									finish();
									if(SubmitHomeWorkActivity.getInstance() != null){
			                        	SubmitHomeWorkActivity.getInstance().finish();
			                        }
									
									if (HomeworkActivity.getInstance() != null) {
										HomeworkActivity.getInstance().finish();
									}
									
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
							}
						}).start();
					}
					
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
            	MessageDialog dialog = new MessageDialog(HomeWorkEvaluate.this);
                dialog.setTitle("提示");
                dialog.setMessage("你确定评分该作业吗?");
                dialog.setCancelable(false);
                dialog.setRightBold(true);
                dialog.setOnLeftClickListener("取消", null);
                
                dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        addHomeworkReview();
                        noticeTeacherHomeworkReview();
                    }
                }
                );
                dialog.show();	
            	
                break;

            case R.id.btn_comment://获得模板
                isEvaluate = true;//使用了评语模板，不受editText字数的限制
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
//                    isEvaluate = true;//使用了评语模板，不受editText字数的限制
                }
                break;

        }
    }
    
    private void noticeTeacherHomeworkReview(){
		String teacher_alias = mDb.fetchStudentAlias(schoolId, my_uid);
		String reason = class_name+"课"+lesson_name+"班的"+teacher_alias+"老师已经批改了你的作业，快去查看";
	
		final ChatMessage message = new ChatMessage();
		message.chatUserName = student_uid;

		message.messageContent = reason;
		message.msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
		message.sentStatus = ChatMessage.SENTSTATUS_SENDING;
		message.sentDate = TimeHelper.getTimeForMessage(HomeWorkEvaluate.this);
		message.uniqueKey = Database.chatMessageSentDateToUniqueKey(message.sentDate);
		message.ioType = ChatMessage.IOTYPE_OUTPUT;
		
		message.primaryKey = new Database(HomeWorkEvaluate.this)
	                            .storeNewChatMessage(message, false);
		WowTalkVoipIF.getInstance(HomeWorkEvaluate.this).fSendChatMessage(message);
	}
}
