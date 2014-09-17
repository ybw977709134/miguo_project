package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-5
 * Time: 下午4:18
 * To change this template use File | Settings | File Templates.
 */
public class FamilyContactRelationHandleActivity extends Activity implements View.OnClickListener{
    public final static String ACTIVITY_INTENT_EXT_KEY_BUDDY="buddy";

    private MessageBox mMsgBox;

    private Buddy buddy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.family_contact_relation_handle_layout);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);

        initView();
    }

    private void initView() {
        buddy=getIntent().getParcelableExtra(ACTIVITY_INTENT_EXT_KEY_BUDDY);

        ImageView ivPhoto = (ImageView) findViewById(R.id.person_photo);
        TextView tvName = (TextView) findViewById(R.id.person_name_text);
        Button   btnAdd=(Button) findViewById(R.id.btn_add);
        TextView tvAlreadyFriendDesc = (TextView) findViewById(R.id.person_already_my_friend);

        PhotoDisplayHelper.displayPhoto(this, ivPhoto, R.drawable.default_avatar_90, buddy, true);

        btnAdd.setOnClickListener(this);
        findViewById(R.id.title_back).setOnClickListener(this);

        tvName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);

        if(buddy.userID.equals(PrefUtil.getInstance(this).getUid()) ||
                0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
            //myself or my friend
            btnAdd.setVisibility(View.GONE);
            tvAlreadyFriendDesc.setVisibility(View.VISIBLE);
        } else {
            //not friend
            btnAdd.setVisibility(View.VISIBLE);
            tvAlreadyFriendDesc.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_add:
                addAsFriend();
                break;
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    private void addAsFriend() {
        mMsgBox.showWait();

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int errno = WowTalkWebServerIF.getInstance(FamilyContactRelationHandleActivity.this).fAddBuddy(buddy.userID);
                return errno;
            }
            @Override
            protected void onPostExecute(Integer s) {
                mMsgBox.dismissWait();
                if(s == ErrorCode.OK) {
                    if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
                        mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
                    } else {
                        mMsgBox.show(null, getString(R.string.contacts_add_buddy_pending_out));
                    }

                    Button   btnAdd=(Button) findViewById(R.id.btn_add);
                    btnAdd.setVisibility(View.GONE);
                } else if (s == ErrorCode.ERR_OPERATION_DENIED){
                    mMsgBox.show(null, getString(R.string.contactinfo_add_friend_denied));
                } else {
                    mMsgBox.show(null, getString(R.string.operation_failed));
                }
            }
        }.execute((Void)null);
    }
}
