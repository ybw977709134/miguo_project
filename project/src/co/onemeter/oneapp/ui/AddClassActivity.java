package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

/**
 * 添加课堂
 * Created by pzy on 11/8/14.
 */
public class AddClassActivity extends Activity implements View.OnClickListener {

    private MessageBox msgbox = new MessageBox(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper.setTheme(this, getIntent().getExtras());
        setContentView(R.layout.activity_add_class);

        AQuery q = new AQuery(this);
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_add).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
            case R.id.btn_add:
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
//                    msgbox.toast(R.string.operation_done);
        			Builder alertDialog = new AlertDialog.Builder(AddClassActivity.this);
        			alertDialog.setTitle("提示");
        			alertDialog.setMessage("添加新课堂成功！可以在校园内板块看到相应的更新");
        			alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
        				
        				@Override
        				public void onClick(DialogInterface arg0, int arg1) {
        					setResult(RESULT_OK);
                            finish();
        				}
        			});        			
        			alertDialog.create().show();

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
                        msgbox.toast(R.string.invite_code_type_not_match, Toast.LENGTH_SHORT);
                    }
                }
            }
        }, invitationCode);

    }
}