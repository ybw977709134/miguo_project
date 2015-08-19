package co.onemeter.oneapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

public class SchoolInvitationActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_SCHOOL_ID = "school_id";
    public static final String EXTRA_SCHOOL_NAME = "school_name";
    public static final String EXTRA_CLASS_NAME = "class_name";

    String phone;
    String schoolId;
    String schoolName;
    String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_invitation);

        getArgs(getIntent().getExtras());

        AQuery q = new AQuery(this);

        String contentText = TextUtils.isEmpty(className) ?
                getString(R.string.school_invitation_notification_content_text) :
                getString(R.string.school_invitation_notification_content_text_with_class_name,
                        className);

        q.find(R.id.txt_content).text(schoolName + contentText);
        q.find(R.id.btn_accept).clicked(this);
        q.find(R.id.btn_reject).clicked(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle args = intent.getExtras();
        getArgs(args);
    }

    private void getArgs(Bundle args) {
        if (args != null) {
            phone = args.getString(EXTRA_PHONE);
            schoolId = args.getString(EXTRA_SCHOOL_ID);
            schoolName = args.getString(EXTRA_SCHOOL_NAME);
            className = args.getString(EXTRA_CLASS_NAME);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_school_invitation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_accept:
                sendProcessInvitationRequest(true);
                break;
            case R.id.btn_reject:
                sendProcessInvitationRequest(false);
                break;
            default:
        }
    }

    private void sendProcessInvitationRequest(final boolean accepted) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                return LessonWebServerIF.getInstance(SchoolInvitationActivity.this)
                        .processInvitation(phone, schoolId, accepted);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                MessageBox messageBox = new MessageBox(SchoolInvitationActivity.this);
                if (errno == ErrorCode.OK) {
                    messageBox.toast(R.string.operation_done);
                    finish();
                } else {
                    messageBox.toast(R.string.operation_failed);
                }
            }
        });
    }
}
