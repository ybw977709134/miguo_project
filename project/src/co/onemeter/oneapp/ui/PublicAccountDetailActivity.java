package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/29/13
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class PublicAccountDetailActivity extends Activity implements View.OnClickListener {

    public static final String PERSON_DETAIL = "person_detail";

    private ImageButton btnTitleBack;
    private ImageButton btnTitleMore;
    private ImageView imgDivRight;
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

    private BottomButtonBoard bottomBoard;
    private MessageBox mMsgBox;
    private boolean isFriend;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleMore = (ImageButton) findViewById(R.id.title_more);
        imgDivRight = (ImageView) findViewById(R.id.div_right);
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

        btnTitleBack.setOnClickListener(this);
        btnTitleMore.setOnClickListener(this);
        btnAccept.setOnClickListener(this);
        btnShow.setOnClickListener(this);
        imgThumbnail.setOnClickListener(this);
        View imgM = findViewById(R.id.img_thumbnail_mask);
        if (imgM != null)
            imgM.setOnClickListener(this);
    }

    private void refreshView() {
        buddy = dbHelper.buddyWithUserID(person.getID());
        if (buddy != null && buddy.getFriendShipWithMe() == Buddy.RELATIONSHIP_FRIEND_HERE) {
            isFriend = true;
            btnTitleMore.setVisibility(View.VISIBLE);
            imgDivRight.setVisibility(View.VISIBLE);
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
            btnTitleMore.setVisibility(View.GONE);
            imgDivRight.setVisibility(View.GONE);
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
                return WowTalkWebServerIF.getInstance(PublicAccountDetailActivity.this)
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
            case R.id.title_back:
                finish();
                break;
            case R.id.title_more:
                bottomBoard.show();
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
            case R.id.img_thumbnail_mask:
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
        bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        bottomBoard.add(getString(R.string.cancel_follow), BottomButtonBoard.BUTTON_RED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelFollow();
                        bottomBoard.dismiss();
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));
    }

    private void cancelFollow() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(PublicAccountDetailActivity.this)
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
                return WowTalkWebServerIF.getInstance(PublicAccountDetailActivity.this)
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
