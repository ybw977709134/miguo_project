package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.RoundedImageView;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Wow
 * Date: 13-4-23
 * Time: 下午1:08
 * To change this template use File | Settings | File Templates.
 */
public class ApplyInfoActivity extends Activity implements View.OnClickListener {
    private ImageButton btnTitleBack;
    private RoundedImageView imgPhoto;
    private TextView txtName;
    private LinearLayout infoLayout;
    private TextView txtExtra;
    private Button btnAgree;
    private Button btnDecline;

    private ArrayList<Buddy> buddyList;
    private PendingRequest mPendingRequest;
    private MessageBox mMsgBox;
    private Database dbHelper;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        imgPhoto = (RoundedImageView) findViewById(R.id.img_photo);
        txtName = (TextView) findViewById(R.id.txt_name);
        infoLayout = (LinearLayout) findViewById(R.id.info_layout);
        txtExtra = (TextView) findViewById(R.id.extra_info);
        btnAgree = (Button) findViewById(R.id.btn_agree);
        btnDecline = (Button) findViewById(R.id.btn_decline);

        txtName.setText(mPendingRequest.nickname);
        if(Utils.isNullOrEmpty(mPendingRequest.msg)) {
            infoLayout.setVisibility(View.GONE);
        } else {
            infoLayout.setVisibility(View.VISIBLE);
            txtExtra.setText(mPendingRequest.msg);
        }
        displayBuddyThumb(mPendingRequest.uid);

        btnTitleBack.setOnClickListener(this);
        btnAgree.setOnClickListener(this);
        btnDecline.setOnClickListener(this);
    }

    private void displayBuddyThumb(String userId) {
        Buddy buddy = dbHelper.buddyWithUserID(userId);
        if (buddy == null) {
            buddy = new Buddy(userId);
            buddy.photoUploadedTimeStamp = mPendingRequest.buddy_photo_timestamp;
        }
        PhotoDisplayHelper.displayPhoto(this, imgPhoto,
                R.drawable.default_avatar_90, buddy, true);
    }

    private void acceptBuddy() {
        mMsgBox.showWait();
        Buddy buddy = new Buddy(mPendingRequest.uid);
        buddyList = new ArrayList<Buddy>();
        buddyList.add(buddy);
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ApplyInfoActivity.this)
                        .fGroupChat_AddMembers(mPendingRequest.group_id, buddyList, true);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    dbHelper.deletePendingRequest(mPendingRequest.id);
                    GroupMember groupMember = new GroupMember(mPendingRequest.uid, mPendingRequest.group_id);
                    groupMember.setLevel(GroupMember.LEVEL_DEFAULT);
                    dbHelper.addNewBuddyToGroupChatRoomByID(groupMember, false);
                    Intent intent = new Intent();
                    intent.putExtra("result", "accept");
                    intent.putExtra("pid", mPendingRequest.id);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (result == ErrorCode.PENDING_REQUEST_ALREADY_PROCESSED) {
                    mMsgBox.toast(R.string.group_pending_request_already_processed);
                    dbHelper.deletePendingRequest(mPendingRequest.id);
                    Intent intent = new Intent();
                    intent.putExtra("result", "accept");
                    intent.putExtra("pid", mPendingRequest.id);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }.execute((Void)null);
    }

    private void rejectBuddy() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ApplyInfoActivity.this)
                        .fGroupChat_Reject(mPendingRequest.group_id, mPendingRequest.uid);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    dbHelper.deletePendingRequest(mPendingRequest.id);
                    Intent intent = new Intent();
                    intent.putExtra("result", "reject");
                    intent.putExtra("pid", mPendingRequest.id);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (result == ErrorCode.PENDING_REQUEST_ALREADY_PROCESSED) {
                    mMsgBox.toast(R.string.group_pending_request_already_processed);
                    dbHelper.deletePendingRequest(mPendingRequest.id);
                    Intent intent = new Intent();
                    intent.putExtra("result", "reject");
                    intent.putExtra("pid", mPendingRequest.id);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }.execute((Void)null);
    }

    public static void launch(Activity activity, PendingRequest p, int requestCode) {
        Intent intent = new Intent(activity, ApplyInfoActivity.class);
        intent.putExtra("mPendingRequest", p);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.btn_agree:
                acceptBuddy();
                break;
            case R.id.btn_decline:
                rejectBuddy();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_info);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Intent data = getIntent();
        if (data == null)
            return;

        if (null != savedInstanceState) {
            mPendingRequest = savedInstanceState.getParcelable("mPendingRequest");
        }
        if (null == mPendingRequest) {
            mPendingRequest = data.getParcelableExtra("mPendingRequest");
        }
        mMsgBox = new MessageBox(this);
        dbHelper = new Database(this);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
