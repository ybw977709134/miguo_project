package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

/**
 * Created with IntelliJ IDEA.
 * User: jianxingdong
 * Date: 13-5-27
 * Time: PM5:43
 * To change this template use File | Settings | File Templates.
 */
public class ExternalSearchActivity extends Activity {
    private LinearLayout layoutSearch;
    private LinearLayout layoutResult;

    private ImageButton btnTitleBack;
    private Button btnSearch;
    private EditText edtCompany;
    private EditText edtID;

    private ImageView imgPhoto;
    private TextView txtName;
    private TextView txtCompany;
    private Button btnAdd;

    private boolean mAllowAdd = true;
    private MessageBox mMsgBox;
    private Buddy buddy = new Buddy();

    private void initView() {
        layoutSearch = (LinearLayout) findViewById(R.id.search_layout);
        layoutResult = (LinearLayout) findViewById(R.id.result_layout);

        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnSearch = (Button) findViewById(R.id.btn_search);
        edtCompany = (EditText) findViewById(R.id.edt_company);
        edtID = (EditText) findViewById(R.id.edt_id);

        imgPhoto = (ImageView) findViewById(R.id.img_photo);
        txtName = (TextView) findViewById(R.id.txt_name);
        txtCompany = (TextView) findViewById(R.id.txt_company);
        btnAdd = (Button) findViewById(R.id.btn_add);

        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchExternalContact();
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBuddy();
            }
        });
    }

    private void addBuddy() {
        if (!mAllowAdd) {
            mMsgBox.show(null, getString(R.string.you_are_already_friends));
        } else {
            mMsgBox.showWait();
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    return WowTalkWebServerIF.getInstance(ExternalSearchActivity.this)
                            .fAddBuddy(buddy.userID);
                }

                @Override
                protected void onPostExecute(Integer result) {
                    mMsgBox.dismissWait();
                    if (result == ErrorCode.OK) {
                        mAllowAdd = false;
                        if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
                            mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
                        } else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
                            mMsgBox.show(null, getString(R.string.contacts_add_buddy_pending_out));
                        }
                        btnAdd.setVisibility(View.GONE);
                    } else if (result == ErrorCode.ERR_OPERATION_DENIED) {
                        mMsgBox.show(null, getString(R.string.contactinfo_add_friend_denied));
                    } else {
                        mMsgBox.show(null, getString(R.string.operation_failed));
                    }
                }
            });
        }
    }

    private void searchExternalContact() {
        String companyID = edtCompany.getText().toString();
        String id = edtID.getText().toString();
        final String username = new StringBuilder().append(companyID).append("_").append(id).toString();
        if (companyID == null || companyID.equals("")) {
            return;
        }
        if (id == null || id.equals("")) {
            return;
        }
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(ExternalSearchActivity.this)
                        .fGetBuddyByUsername(username, buddy);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                mAllowAdd = result == ErrorCode.OK
                        && !Utils.isNullOrEmpty(buddy.userID)
                        && 0 == (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)
                        && !buddy.userID.equals(PrefUtil.getInstance(ExternalSearchActivity.this).getUid());
                if (ErrorCode.OK == result) {
                    imgPhoto.setBackgroundDrawable(null);
                    layoutSearch.setVisibility(View.GONE);
                    layoutResult.setVisibility(View.VISIBLE);
                    PhotoDisplayHelper.displayPhoto(ExternalSearchActivity.this,
                            imgPhoto, R.drawable.default_avatar_90, buddy, true);
                    if (buddy.status.equals("")) {
                        txtCompany.setVisibility(View.GONE);
                    }
                    txtName.setText(buddy.nickName);
                    btnAdd.setVisibility(mAllowAdd ? View.VISIBLE : View.GONE);
                }

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_external);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
