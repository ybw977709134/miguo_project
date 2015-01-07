package co.onemeter.oneapp.ui;

import android.graphics.PixelFormat;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class PrivacySettingActivity extends Activity implements OnClickListener{

    private static final int OTHERS_CAN_NOT_ADD_ME = 0;
    private static final int OTHERS_CAN_ADD_ME_NEED_AUTH = 1;
    private static final int OTHERS_CAN_ADD_ME_WITHOUT_AUTH = 2;

    private ImageButton mBackBtn;
    private ImageButton mConfirmBtn;
    private ImageButton mOthersCanAddMeBtn;
    private ImageButton mAuthNeededBtn;
    private ImageButton mNearbyCanFindMeBtn;
    private RelativeLayout mOthersLayout;
    private ImageView mDivImageView;
    private RelativeLayout mAuthLayout;

    private boolean mIsOthersCanAddMe = true;
    private boolean mIsAuthNeeded = true;
    private boolean mIsNearbyCanFindMe = true;

    private WowTalkWebServerIF mWebIF;
    private MessageBox mMsgBox;
    private PrefUtil mPrefUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_privacy);

        getWindow().setFormat(PixelFormat.RGBA_8888);
        
        mMsgBox = new MessageBox(this);
        mWebIF = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);

        initView();
        getSettingsFromServer();
    }

    private void initView() {
        mBackBtn = (ImageButton) findViewById(R.id.title_back);
        mConfirmBtn = (ImageButton) findViewById(R.id.title_confirm);
        mOthersCanAddMeBtn = (ImageButton) findViewById(R.id.others_can_add_me);
        mAuthNeededBtn = (ImageButton) findViewById(R.id.auth_is_needed);
        mNearbyCanFindMeBtn = (ImageButton) findViewById(R.id.nearby_can_find_me);
        mOthersLayout = (RelativeLayout) findViewById(R.id.others_can_add_me_layout);
        mDivImageView = (ImageView) findViewById(R.id.div_others_and_auth);
        mAuthLayout = (RelativeLayout) findViewById(R.id.auth_is_needed_layout);

        mBackBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mOthersCanAddMeBtn.setOnClickListener(this);
        mAuthNeededBtn.setOnClickListener(this);
        mNearbyCanFindMeBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            handleBackEvent();
            break;
        case R.id.title_confirm:
            confirmModify();
            break;
        case R.id.others_can_add_me:
            changeOthersCanAddMe(!mIsOthersCanAddMe);
            break;
        case R.id.auth_is_needed:
            changeAuthIsNeeded(!mIsAuthNeeded);
            break;
        case R.id.nearby_can_find_me:
            changeNearbyCanFindMe(!mIsNearbyCanFindMe);
            break;
        default:
            break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackEvent();
    }

    private void handleBackEvent() {
        finish();
    }

    private void getSettingsFromServer() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = mWebIF.fGetPrivacySetting();
                return resultCode;
            }

            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == 0) {
                    int othersCanAddMe = mPrefUtil.getOthersCanAddMe();
                    switch (othersCanAddMe) {
                        case OTHERS_CAN_ADD_ME_NEED_AUTH:
                            mIsOthersCanAddMe = true;
                            mIsAuthNeeded = true;
                            break;
                        case OTHERS_CAN_ADD_ME_WITHOUT_AUTH:
                            mIsOthersCanAddMe = true;
                            mIsAuthNeeded = false;
                            break;
                        // "can't add" is same with the default case.
                        case OTHERS_CAN_NOT_ADD_ME:
                        default:
                            mIsOthersCanAddMe = false;
                            mIsAuthNeeded = true;
                            break;
                    }
                    mIsNearbyCanFindMe = mPrefUtil.getListMeInNearbyResult();
                    Log.i("getPrivacySetting, canAddMe is " + othersCanAddMe + ", nearbyCanFindMe is " + mIsNearbyCanFindMe);
                    changeOthersCanAddMe(mIsOthersCanAddMe);
                    changeAuthIsNeeded(mIsAuthNeeded);
                    changeNearbyCanFindMe(mIsNearbyCanFindMe);
                }
            };
        }.execute((Void)null);
    }

    private void changeOthersCanAddMe(boolean isOthersCanAddMe) {
        mIsOthersCanAddMe = isOthersCanAddMe;
        mOthersCanAddMeBtn.setBackgroundResource(mIsOthersCanAddMe ? R.drawable.switch_on_little : R.drawable.switch_off_little);
        // display or hide the authNeeded layout
        if (mIsOthersCanAddMe) {
//            mOthersLayout.setBackgroundResource(R.drawable.group_list_bg_begin);
            mDivImageView.setVisibility(View.VISIBLE);
            mAuthLayout.setVisibility(View.VISIBLE);
        } else {
//            mOthersLayout.setBackgroundResource(R.drawable.group_list_bg_single);
            mDivImageView.setVisibility(View.GONE);
            mAuthLayout.setVisibility(View.GONE);
        }
    }

    private void changeAuthIsNeeded(boolean isAuthNeeded) {
        mIsAuthNeeded = isAuthNeeded;
        mAuthNeededBtn.setBackgroundResource(mIsAuthNeeded ? R.drawable.switch_on_little : R.drawable.switch_off_little);
    }

    private void changeNearbyCanFindMe(boolean isNearbyCanFindMe) {
        mIsNearbyCanFindMe = isNearbyCanFindMe;
        mNearbyCanFindMeBtn.setBackgroundResource(mIsNearbyCanFindMe ? R.drawable.switch_on_little : R.drawable.switch_off_little);
    }

    private void confirmModify() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                Log.i("privacySetting, isOthersCanAddMe is " + mIsOthersCanAddMe + ", isAuthNeeded is " + mIsAuthNeeded + ", isNearbyCanFindMe is " + mIsNearbyCanFindMe);

                // if mIsOthersCanAddMe is false, allowOthersAddMe = OTHERS_CAN_NOT_ADD_ME
                int allowOthersAddMe = OTHERS_CAN_NOT_ADD_ME;
                if (mIsOthersCanAddMe) {
                    allowOthersAddMe = mIsAuthNeeded ? OTHERS_CAN_ADD_ME_NEED_AUTH : OTHERS_CAN_ADD_ME_WITHOUT_AUTH;
                }
                int resultCode = mWebIF.fSetPrivacy(true, allowOthersAddMe, true, true, true, mIsNearbyCanFindMe);
                return resultCode;
            }

            protected void onPostExecute(Integer result) {
                Log.i("privacySetting resultCode is " + result);
                mMsgBox.dismissWait();
                if (result == 0) {
                    finish();
                } else {
                    mMsgBox.show(null, getResources().getString(R.string.privacysetting_failure));
                }
            };
        }.execute((Void)null);
    }
}
