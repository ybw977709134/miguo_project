package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

/**
 * 添加课堂
 * Created by pzy on 11/8/14.
 */
public class AddClassActivity extends Activity implements View.OnClickListener {

    private MessageBox msgbox = new MessageBox(this);
    private EditText et_code;
    private InputMethodManager imm;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper.setTheme(this, getIntent().getExtras());
        setContentView(R.layout.activity_add_class);

        AQuery q = new AQuery(this);
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_add).clicked(this);
        et_code = (EditText) findViewById(R.id.txt_code);
        et_code.setFocusable(true);
        et_code.setFocusableInTouchMode(true); 
        et_code.requestFocus();
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				imm = (InputMethodManager)et_code.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
				imm.showSoftInput(et_code, InputMethodManager.RESULT_SHOWN);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); 
			}
		}, 200);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
            	closeSoftKeyboard();
                onBackPressed();
                break;
            case R.id.btn_add:
            	closeSoftKeyboard();
                submit();
                break;
        }
    }

    private void submit() {
        String invitationCode = new AQuery(this).find(R.id.txt_code).getText().toString();
        if (TextUtils.isEmpty(invitationCode))
            return;

        msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Integer>() {
            @Override
            protected Integer doInBackground(String... strings) {
                int errno = WowTalkWebServerIF.getInstance(AddClassActivity.this)
                        .fBindInvitationCode(strings[0]);
                return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {

        			msgbox.showWaitImageSuccess("添加新课堂成功");
        			
        			closeSoftKeyboard();
                    
                    
                    new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(3000);
                                setResult(RESULT_OK);
                                finish();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}
					}).start();
        			
//        			 MessageDialog dialog = new MessageDialog(AddClassActivity.this);
//                     dialog.setTitle("");
//                     dialog.setMessage("添加新课堂成功！可以在校园内板块看到相应的更新");
//                     dialog.setRightBold(true);
//
//                     dialog.setOnLeftClickListener("确定", new MessageDialog.MessageDialogClickListener() {
//                         @Override
//                         public void onclick(MessageDialog dialog) {
//                             dialog.dismiss();
//                             setResult(RESULT_OK);
//                             closeSoftKeyboard();
//                             finish();
//                             
//                         }
//                     });
//                     dialog.show();
        			
        			

                } else {
//                    msgbox.toast(getString(R.string.operation_failed_with_errcode_msg,
//                            errno,
//                            ErrorCode.getErrorName(AddClassActivity.this, errno)));
                    if (errno == ErrorCode.ERR_EXPIRED_INVITATION_CODE) {
                        msgbox.toast(R.string.invite_code_out_time, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_SCHOOL_USER_HAD_BOUND) {
                        msgbox.toast(R.string.invite_code_used, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_BOUND_SAME_SCHOOL_USER) {
                        msgbox.toast(R.string.invite_code_school, Toast.LENGTH_SHORT);
                    } else if (errno == ErrorCode.ERR_SCHOOL_USER_TYPE_NOT_MATCH) {
                    	if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(getApplicationContext()).getMyAccountType()){
                    		msgbox.toast(R.string.invite_code_type_not_match_teacher, Toast.LENGTH_SHORT);
                    	}else if(Buddy.ACCOUNT_TYPE_STUDENT == PrefUtil.getInstance(getApplicationContext()).getMyAccountType()){
                    		msgbox.toast(R.string.invite_code_type_not_match_student, Toast.LENGTH_SHORT);
                    	}
                        
                    } else if (errno == ErrorCode.ERR_INVITATION_CODE_NOT_EXIST) {
                        msgbox.toast(R.string.invite_code_type_not_exist, Toast.LENGTH_SHORT);
                    } else{
                    	msgbox.toast(R.string.invite_code_type_time_out, Toast.LENGTH_SHORT);
                    }
                }
            }
        }, invitationCode);

    }
    
    private void closeSoftKeyboard(){
		if (et_code.hasFocus()) {
			imm.hideSoftInputFromWindow(et_code.getWindowToken() , 0);
    	}
	}
}