package co.onemeter.oneapp;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.SchoolInvitation;
import org.wowtalk.ui.MessageBox;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 展示学校邀请的列表。
 */
public class SchoolInvitationActivity extends ListActivity implements View.OnClickListener {

    /** 我收到的所有邀请 */
    List<SchoolInvitation> invitations = new ArrayList<>();
    SchoolInvitationAdatper adatper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 只是为了获得统一的外观
        setContentView(R.layout.activity_school_invitations);

        setupEventHandlers();

        loadData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        loadData();
    }

    private void setupEventHandlers() {
        AQuery q = new AQuery(this);
        q.find(R.id.title_back).clicked(this);
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
            case R.id.title_back:
                onBackPressed();
                break;
        }
    }

    private void loadData() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                List<SchoolInvitation> list = new ArrayList<>();
                int errno = LessonWebServerIF.getInstance(SchoolInvitationActivity.this)
                        .getMyInvitations(list);
                synchronized (invitations) {
                    invitations.clear();
                    invitations.addAll(list);
                }
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    adatper = new SchoolInvitationAdatper(SchoolInvitationActivity.this,
                            invitations);
                    setListAdapter(adatper);
                } else {
                    new MessageBox(SchoolInvitationActivity.this)
                            .toast(getString(R.string.err_failed_to_load_data_with_errno, errno));
                }
            }
        });
    }

    private void sendProcessInvitationRequest(final String phone, final String schoolId, final boolean accepted) {
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
                } else {
                    messageBox.toast(R.string.operation_failed);
                }
            }
        });
    }

    static class SchoolInvitationAdatper extends ArrayAdapter<SchoolInvitation> {
        WeakReference<SchoolInvitationActivity> activityRef;

        public SchoolInvitationAdatper(SchoolInvitationActivity activity, List<SchoolInvitation> objects) {
            super(activity, R.layout.listitem_school_invitation, R.id.txt_content, objects);
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            AQuery q = new AQuery(view);

            final SchoolInvitation obj = getItem(position);

            String contentText;
            if (obj.classroomNames != null && obj.classroomNames.length > 0) {
                contentText = getContext().getString(R.string.school_invitation_notification_content_text_with_class_name,
                        Arrays.toString(obj.classroomNames));
            } else {
                contentText = getContext().getString(R.string.school_invitation_notification_content_text);
            }

            q.find(R.id.txt_content).text(obj.schoolName + contentText);
            q.find(R.id.btn_accept).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (activityRef.get() != null) {
                        obj.status = "accepted";
                        activityRef.get().adatper.notifyDataSetChanged();
                        activityRef.get().sendProcessInvitationRequest(obj.phone, obj.schoolId, true);
                    }
                }
            });
            q.find(R.id.btn_reject).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (activityRef.get() != null) {
                        obj.status = "rejected";
                        activityRef.get().adatper.notifyDataSetChanged();
                        activityRef.get().sendProcessInvitationRequest(obj.phone, obj.schoolId, false);
                    }
                }
            });

            // set status
            if (TextUtils.equals(obj.status, "accepted")) {
                q.find(R.id.txt_status).visible().text(R.string.school_invitation_status_accepted);
                q.find(R.id.btn_accept).gone();
                q.find(R.id.btn_reject).gone();
            } else if (TextUtils.equals(obj.status, "rejected")) {
                q.find(R.id.txt_status).visible().text(R.string.school_invitation_status_rejected);
                q.find(R.id.btn_accept).gone();
                q.find(R.id.btn_reject).gone();
            } else if (TextUtils.equals(obj.status, "sent")) {
                q.find(R.id.txt_status).gone();
                q.find(R.id.btn_accept).visible();
                q.find(R.id.btn_reject).visible();
            } else {
                // this shouldn't happen
                q.find(R.id.txt_status).gone();
                q.find(R.id.btn_accept).gone();
                q.find(R.id.btn_reject).gone();
            }

            return view;
        }
    }
}
