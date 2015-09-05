package co.onemeter.oneapp;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.SchoolInvitation;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            protected void onPreExecute() {
                new AQuery(SchoolInvitationActivity.this).find(android.R.id.empty).text(R.string.loading);
            }

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
                    new AQuery(SchoolInvitationActivity.this).find(android.R.id.empty).text(R.string.no_data);
                    adatper = new SchoolInvitationAdatper(SchoolInvitationActivity.this,
                            invitations);
                    setListAdapter(adatper);
                } else {
                    new AQuery(SchoolInvitationActivity.this).find(android.R.id.empty).text(
                            getString(R.string.err_failed_to_load_data_with_errno, errno));
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        public SchoolInvitationAdatper(SchoolInvitationActivity activity, List<SchoolInvitation> objects) {
            super(activity, R.layout.listitem_school_invitation, R.id.txt_content, objects);
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            AQuery q = new AQuery(view);

            final SchoolInvitation obj = getItem(position);

            final String classrooms;
            if (obj.classroomNames != null && obj.classroomNames.length > 0) {
                classrooms = TextUtils.join("\n", obj.classroomNames);
            } else {
                classrooms = "(" + getContext().getString(R.string.nothing) + ")";
            }

            q.find(R.id.txt_classrooms).text(classrooms);
            q.find(R.id.txt_date).text(dateFormat.format(obj.lastModified));

            q.find(R.id.txt_content).text(obj.schoolName);
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
                        final SchoolInvitationActivity activity = activityRef.get();

                        String msg = activity.getString(
                                R.string.confirm_reject_school_invitations_of_following_classrooms) +
                                "\n" + classrooms;

                        new MessageDialog(activity, true, MessageDialog.SIZE_NORMAL)
                                .setMessage(msg)
                                .setOnLeftClickListener(activity.getString(R.string.cancel), null)
                                .setOnRightClickListener(activity.getString(R.string.reject),
                                        new MessageDialog.MessageDialogClickListener() {
                                            @Override
                                            public void onclick(MessageDialog dialog) {
                                                dialog.dismiss();

                                                obj.status = "rejected";
                                                activity.adatper.notifyDataSetChanged();
                                                activity.sendProcessInvitationRequest(obj.phone, obj.schoolId, false);
                                            }
                                        })
                                .setTextColorBtnLeftOrSingle(activity.getResources().getColor(R.color.blue))
                                .setTextColorBtnRight(activity.getResources().getColor(R.color.red))
                                .setRightBold(true)
                                .show();
                    }
                }
            });

            // set status
            if (TextUtils.equals(obj.status, "accepted")) {
                q.find(R.id.txt_status).visible().text(R.string.school_invitation_status_accepted);
                q.find(R.id.vg_actions).gone();
            } else if (TextUtils.equals(obj.status, "rejected")) {
                q.find(R.id.txt_status).visible().text(R.string.school_invitation_status_rejected);
                q.find(R.id.vg_actions).gone();
            } else if (TextUtils.equals(obj.status, "sent")) {
                q.find(R.id.txt_status).gone();
                q.find(R.id.vg_actions).visible();
            } else {
                // this shouldn't happen
                q.find(R.id.txt_status).gone();
                q.find(R.id.vg_actions).gone();
            }

            return view;
        }
    }
}
