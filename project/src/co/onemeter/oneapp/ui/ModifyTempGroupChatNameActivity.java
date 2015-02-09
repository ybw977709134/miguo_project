package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

public class ModifyTempGroupChatNameActivity extends Activity {
    public static final String EXTRA_GROUP_ID = "tempGroupID";
    public static final String EXTRA_GROUP_NAME = "tempGroupName";

    private ImageButton mBtnTitleBack;
    private ImageButton mBtnTitleConfirm;
    private EditText mGroupNameText;
    private MessageBox mMsgBox;
    private Database mDbHelper;
    private String mGroupId = null;
    private String mOriginGroupName = null;

    private void resetTempGroupName(final String groupChatName) {
        mMsgBox.showWait();

        final GroupChatRoom chatRoom = mDbHelper.fetchGroupChatRoom(mGroupId);
        if (null == chatRoom) {
            return;
        }
        chatRoom.groupNameLocal = groupChatName;
        chatRoom.groupNameOriginal = groupChatName;
        chatRoom.isTemporaryGroup = true;
        chatRoom.isGroupNameChanged = true;

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ModifyTempGroupChatNameActivity.this)
                        .fGroupChat_UpdateInfo(chatRoom);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                if (result == ErrorCode.OK) {
                    // update local temp group name
                    Database db = new Database(ModifyTempGroupChatNameActivity.this);
                    db.updateGroupChatRoom(chatRoom);
                    // update the display name of chatmessages
                    db.updateChatMessageDisplayNameWithUser(mGroupId, groupChatName);

                    // setResult
                    Intent data = new Intent();
                    data.putExtra(EXTRA_GROUP_NAME, groupChatName);
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } else {
                   mMsgBox.show(null, getString(R.string.group_chat_title_modify_failure));
//                    Toast toast = Toast.makeText(ModifyTempGroupChatNameActivity.this, getString(R.string.group_chat_title_modify_failure), Toast.LENGTH_SHORT);
//                    toast.setGravity(Gravity.CENTER, 0, 0);
//                    toast.show();
                   mMsgBox.dismissDialog();
                   
                }
            }
        });
    }

    private void initView() {
        mBtnTitleBack = (ImageButton) findViewById(R.id.title_back);
        mBtnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        mGroupNameText = (EditText) findViewById(R.id.edit_temp_group_name);
        mGroupNameText.setText(mOriginGroupName == null ? "" : mOriginGroupName);

        mBtnTitleBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }

        });

        mBtnTitleConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final String groupName = mGroupNameText.getText().toString().trim();

                if(groupName.equals("")) {
                    mMsgBox.toast(R.string.group_chat_title_modify_empty);
                    return;
                } else if (groupName.equals(mOriginGroupName)) {
                    finish();
                }

                resetTempGroupName(groupName.trim());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_tempgroup_name);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mDbHelper = new Database(this);

        Intent intent = getIntent();
        if (null != intent){
            mGroupId = intent.getStringExtra(EXTRA_GROUP_ID);
            mOriginGroupName = intent.getStringExtra(EXTRA_GROUP_NAME);
        }
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
