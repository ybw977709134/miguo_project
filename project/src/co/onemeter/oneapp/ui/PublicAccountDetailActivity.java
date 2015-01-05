package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.ImageViewActivity;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/29/13
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class PublicAccountDetailActivity extends Activity implements View.OnClickListener {

    public static final String PERSON_DETAIL = "person_detail";

    private ImageView imgThumbnail;
    private TextView txtFunctionDetail;
    private TextView txtAccoutName;
    private TextView txtAccountId;
    private Button btnFollow;

    private LinearLayout extraLayout;

    private ImageView btnAccept;
    private ImageView btnShow;

    private Person person;
    private Buddy buddy;

    private Database dbHelper;

    private MessageBox mMsgBox;
    private boolean isFriend;

    private void initView() {
        AQuery q = new AQuery(this);

        q.find(R.id.navbar_btn_left).clicked(this);
        q.find(R.id.btn_goto_moments).clicked(this);
        q.find(R.id.img_thumbnail).clicked(this);

        imgThumbnail = (ImageView) findViewById(R.id.img_thumbnail);
        txtAccoutName = (TextView) findViewById(R.id.account_name);
        txtAccountId = (TextView) findViewById(R.id.account_id);
        txtFunctionDetail = (TextView) findViewById(R.id.function_detail);
        extraLayout = (LinearLayout) findViewById(R.id.layout2);
        btnFollow = (Button) findViewById(R.id.btn_follow);
        btnAccept = (ImageView) findViewById(R.id.img_accept);
        btnShow = (ImageView) findViewById(R.id.img_show);

        txtAccoutName.setText(person.getName());
        if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
            txtAccountId.setText(String.format(getString(R.string.contact_info_wowid), person.getWowTalkId()));
        } else {
            txtAccountId.setText(person.getWowTalkId());
        }
        txtFunctionDetail.setText(person.getPersonState());
        refreshView();

        btnAccept.setOnClickListener(this);
        btnShow.setOnClickListener(this);
    }

    private void refreshView() {
        buddy = dbHelper.buddyWithUserID(person.getID());
        if (buddy != null && buddy.getFriendShipWithMe() == Buddy.RELATIONSHIP_FRIEND_HERE) {
            isFriend = true;
            extraLayout.setVisibility(View.VISIBLE);
            btnFollow.setText(getString(R.string.send_message));
            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MessageComposerActivity.launchToChatWithBuddy(PublicAccountDetailActivity.this,
                            MessageComposerActivity.class, person.getID());
                }
            });
        } else {
            isFriend = false;
            extraLayout.setVisibility(View.GONE);
            btnFollow.setText(getString(R.string.follow));
            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    followBuddy();
                }
            });
        }
        PhotoDisplayHelper.displayPhoto(this, imgThumbnail,
                R.drawable.default_official_avatar_90, buddy, false);
        btnAccept.setBackgroundResource(buddy.willBlockMsg
                ? R.drawable.switch_off : R.drawable.switch_on);
        btnShow.setBackgroundResource(buddy.hidden
                ? R.drawable.switch_off : R.drawable.switch_on);
    }

    private void changeAcceptionOfMsg() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WebServerIF.getInstance(PublicAccountDetailActivity.this)
                        .fOperateBuddy(buddy);
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == ErrorCode.OK) {

                }
            }
        }.execute((Void)null);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.navbar_btn_left:
                finish();
                break;
            case R.id.btn_goto_moments:
                startActivity(new Intent(this, TimelineActivity.class)
                        .putExtra(TimelineActivity.EXTRA_UID, person.getID())
                        .putExtra(TimelineActivity.EXTRA_PAGE_TITLE, person.getName()));
                break;
            case R.id.btn_follow:
                MessageComposerActivity.launchToChatWithBuddy(PublicAccountDetailActivity.this,
                        MessageComposerActivity.class, person.getID());
                break;
            case R.id.img_accept:
                buddy.willBlockMsg = !buddy.willBlockMsg;
                dbHelper.storeNewBuddyWithUpdate(buddy);
                btnAccept.setBackgroundResource(buddy.willBlockMsg
                        ? R.drawable.switch_off : R.drawable.switch_on);
                changeAcceptionOfMsg();
                break;
            case R.id.img_show:
                buddy.hidden = !buddy.hidden;
                dbHelper.storeNewBuddyWithUpdate(buddy);
                btnShow.setBackgroundResource(buddy.hidden
                        ? R.drawable.switch_off : R.drawable.switch_on);
                break;
            case R.id.img_thumbnail:
                ImageViewActivity.launch(this, person.toBuddy());
                break;
            default:
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.public_account_detail);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        dbHelper = new Database(this);
        mMsgBox = new MessageBox(this);
        person = getIntent().getParcelableExtra(PERSON_DETAIL);
        initView();
    }

    private void cancelFollow() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WebServerIF.getInstance(PublicAccountDetailActivity.this)
                        .fRemoveBuddy(buddy.userID);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    finish();
                }
            }
        }.execute((Void)null);
    }

    private void followBuddy() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WebServerIF.getInstance(PublicAccountDetailActivity.this)
                        .fAddBuddy(buddy.userID);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
                    PublicAccountDetailActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshView();
                        }
                    });
                }
            }
        }.execute((Void)null);
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
