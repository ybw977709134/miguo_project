package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
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

        new AsyncTask<String, Void, Integer>() {
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
                    msgbox.toast(R.string.operation_done);
                    setResult(RESULT_OK);
                } else {
                    msgbox.toast(R.string.operation_failed);
                }
            }
        }.execute(invitationCode);

    }
}