package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

/**
 * <p>Login with invitation code.</p>
 * Created by pzy on 11/21/14.
 */
public class LoginInvitedActivity extends Activity implements View.OnClickListener {

    private MessageBox msgbox = new MessageBox(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_invite);

        AQuery q = new AQuery(this);

        q.find(R.id.btn_login).clicked(this);
        q.find(R.id.btn_login_others).clicked(this);
        q.find(R.id.btn_signup).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_login_others:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.btn_signup:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
            default:
                new MessageBox(this).toast(R.string.not_implemented);
                break;
        }
    }

    private void login() {
        String invitationCode = new AQuery(this).find(R.id.txt_invite_code).getText().toString();
        login(this, invitationCode, msgbox);
    }

    public static void login(final Context context, String invitationCode, final MessageBox msgbox) {

        msgbox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Integer>() {
            Buddy result = new Buddy();

            @Override
            protected Integer doInBackground(String... invitationCode) {
                WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(context);
                int errno = web.fLoginWithInvitationCode(
                        invitationCode[0], result);
                if (errno == ErrorCode.OK) {
                    errno = web.fGetMyProfile();
                }
                return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    context.startActivity(new Intent(context, StartActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } else {
                    msgbox.toast(context.getString(R.string.operation_failed_with_errcode_msg,
                            errno,
                            ErrorCode.getErrorName(context, errno)));
                }
            }
        }, invitationCode);
    }
}