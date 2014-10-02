package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.AccountsListAdapter;
import co.onemeter.oneapp.utils.AppUpgradeTask;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SettingActivity extends Activity implements OnClickListener, OnItemClickListener {
    class MyAppUpgradeTask extends AppUpgradeTask {

        public MyAppUpgradeTask(Context context, String md5sum) {
            super(context, md5sum);
        }

        @Override
        public void onPostExecute(String path) {
            mUpgradeProgressBar.setVisibility(View.GONE);
            if (!isCancelled() && null == path) {
                String errmsg;
                switch(lastError()) {
                    case ERR_CHECKSUM:
                        errmsg = getString(R.string.settings_upgrade_file_corrupted);
                        break;
                    default:
                        errmsg = null;
                }
                if (errmsg == null)
                    mMsgBox.show(null, getString(R.string.operation_failed));
                else
                    mMsgBox.show(getString(R.string.operation_failed), errmsg);
            }
            super.onPostExecute(path);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mUpgradeProgressBar.setProgress(progress[0]);
        }

        @Override
        protected void onCancelled() {
            mUpgradeProgressBar.setVisibility(View.GONE);
            super.onCancelled();
        }

    };

    //--------------------------------------------------------------------------------

    private static final int MSG_LOGOUT_SUCCESS = 100;
    private static final int HANDLER_GET_ACCOUNT_UNREAD_COUNT = 1;

    private ListView mAccountListView;
    private TextView mManageAccounts;
	private RelativeLayout mInformation;
	private RelativeLayout mAccount;
	
	private TextView mTrends;
	private TextView mPrivacy;
	private TextView mPlugin;
	private TextView mTellFriend;
	private TextView mRateus;
	private TextView mAboutus;
    private View mCheckForUpdates;

	private ImageView imgPhoto;
	private TextView txtPwdState;
	private TextView mNewUpdateView;
    private ProgressBar mUpgradeProgressBar;
	private MessageBox mMsgBox;

    private WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;

    private AppUpgradeTask mUpgradeTask;

    private RelativeLayout sysNoticeSettingLayout;
    private TextView tvNoticeStatus;

    private AccountsListAdapter mAccountAdapter;
    private ArrayList<Account> mAccountDatas;

    private Handler mSwitchAccountHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case ManageAccountsActivity.HANDLER_SWITCHING_ACCOUNT:
                mMsgBox.showOrUpdateWatiMsg(String.valueOf(msg.obj));
                break;
            case ManageAccountsActivity.HANDLER_SWITCH_ACCOUNT_SUCCESSFUL:
                mMsgBox.dismissWait();
                updateNoticeStatus();
                Buddy me = new Buddy();
                me.userID = PrefUtil.getInstance(SettingActivity.this).getUid();
                me.photoUploadedTimeStamp = PrefUtil.getInstance(SettingActivity.this).getMyPhotoUploadedTimestamp();
                PhotoDisplayHelper.displayPhoto(SettingActivity.this, imgPhoto, R.drawable.default_avatar_90, me, true);
                setAccountData();
                getAccountUnreadCounts();
                break;
            case ManageAccountsActivity.HANDLER_SWITCH_ACCOUNT_FAILURE:
                mMsgBox.dismissWait();
                mMsgBox.show(null, String.valueOf(msg.obj));
                break;

            default:
                break;
            }
        };
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_GET_ACCOUNT_UNREAD_COUNT:
                mAccountAdapter.setDataSource(mAccountDatas);
                mAccountAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(mAccountListView);
                break;
            default:
                break;
            }
        };
    };

    private IDBTableChangeListener mAlbumCoverObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAccountData();
                }
            });
        }
    };

    private void initView() {
        mAccountListView = (ListView) findViewById(R.id.accounts_listview);
        mManageAccounts = (TextView) findViewById(R.id.manage_accounts);
		mInformation = (RelativeLayout) findViewById(R.id.information);
		mAccount = (RelativeLayout) findViewById(R.id.account);
		mTrends = (TextView) findViewById(R.id.trends);
		mPrivacy = (TextView) findViewById(R.id.privacy);
		mPlugin = (TextView) findViewById(R.id.plug_in);
		mTellFriend = (TextView) findViewById(R.id.tell_friend);
		mRateus = (TextView) findViewById(R.id.rate_us);
		mAboutus = (TextView) findViewById(R.id.about_us);
		mNewUpdateView = (TextView) findViewById(R.id.new_update);
		if (StartActivity.instance().isNewUpdate()) {
		    mNewUpdateView.setVisibility(View.VISIBLE);
		} else {
            mNewUpdateView.setVisibility(View.GONE);
        }
        mUpgradeProgressBar = (ProgressBar)findViewById(R.id.progressBar);
		imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
		txtPwdState = (TextView) findViewById(R.id.pwd_state);
        mCheckForUpdates = findViewById(R.id.check_for_updates);

        sysNoticeSettingLayout=(RelativeLayout) findViewById(R.id.sys_notice_layout);
        tvNoticeStatus = (TextView) findViewById(R.id.sys_notice_layout_status_ind);
        updateNoticeStatus();

        mUpgradeProgressBar.setVisibility(View.GONE);
        mUpgradeProgressBar.setMax(100);

        mAccountListView.setOnItemClickListener(this);
        mManageAccounts.setOnClickListener(this);
        sysNoticeSettingLayout.setOnClickListener(this);
		mInformation.setOnClickListener(this);
        imgPhoto.setOnClickListener(this);
		mAccount.setOnClickListener(this);
		mTrends.setOnClickListener(this);
		mPrivacy.setOnClickListener(this);
		mPlugin.setOnClickListener(this);
		mTellFriend.setOnClickListener(this);
		mRateus.setOnClickListener(this);
		mAboutus.setOnClickListener(this);
        mCheckForUpdates.setOnClickListener(this);
        findViewById(R.id.emergency_contact).setOnClickListener(this);
        findViewById(R.id.favorite_moment).setOnClickListener(this);
	}

    private void startMassSMS(boolean isIvitation) {
        Intent intent = new Intent();
    }

    private void startMassEmail() {

    }

    private void updateNoticeStatus() {
        if(PrefUtil.getInstance(this).isSysNoticeEnabled()) {
            tvNoticeStatus.setText(R.string.sys_notice_enabled);
        } else {
            tvNoticeStatus.setText(R.string.sys_notice_disabled);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQ_ID_SYS_NOTICE_SETTING == requestCode) {
            updateNoticeStatus();
        }
    }

	private final static int REQ_ID_SYS_NOTICE_SETTING=3;
	@Override
	public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.manage_accounts:
                intent.setClass(SettingActivity.this, ManageAccountsActivity.class);
                startActivity(intent);
                break;
            case R.id.sys_notice_layout:
                intent.setClass(SettingActivity.this, SysNoticeSetting.class);
                startActivityForResult(intent, REQ_ID_SYS_NOTICE_SETTING);
                break;
            case R.id.emergency_contact:
                intent.setClass(SettingActivity.this, EmergencyContactActivity.class);
                startActivity(intent);
                break;
            case R.id.information:
            case R.id.img_thumbnail:
                intent.setClass(SettingActivity.this, MyInfoActivity.class);
                startActivity(intent);
                break;
            case R.id.account:
                if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
                    intent.setClass(SettingActivity.this, AccountSettingActivity_biz.class);
                } else {
                    intent.setClass(SettingActivity.this, AccountSettingActivity.class);
                }
                startActivity(intent);
                break;
            case R.id.trends:
                MomentActivity.launchMy(SettingActivity.this);
                break;
            case R.id.favorite_moment:
                MomentActivity.launchFavorite(SettingActivity.this);
                break;
            case R.id.privacy:
                intent.setClass(SettingActivity.this, PrivacySettingActivity.class);
                startActivity(intent);
                break;
            case R.id.about_us:
                intent.setClass(SettingActivity.this, AboutPage.class);
                startActivity(intent);
                break;
            case R.id.plug_in:
                break;
            case R.id.tell_friend:
                tellFriend(v);
                break;
            case R.id.rate_us:
                Intent rateIntent = new Intent(Intent.ACTION_VIEW);
                rateIntent.setData(Uri.parse("market://details?id=" + getPackageName()));
                if (this.getPackageManager().queryIntentActivities(rateIntent, 0).size() != 0) {
                    AppStatusService.setIsMonitoring(false);
                    startActivity(rateIntent);
                } else {
                    mMsgBox.toast("device not suport");
                }
                break;
            case R.id.check_for_updates:
                mNewUpdateView.setVisibility(View.GONE);
                StartActivity.instance().changeNewUpdateFlagView(View.GONE);
                if (null != mUpgradeTask && mUpgradeTask.isExecuting()) {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.settings_upgrade_will_you_cancel)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (null != mUpgradeTask) {
                                        mUpgradeTask.cancel(true);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    checkForUpdates();
                }
                break;
            default:
                break;
        }
    }

    private void tellFriend(View parentView) {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, parentView);
        // Send Email
        bottomBoard.add(getString(R.string.setting_send_email), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        // use this line for testing in the emulator
//                        emailIntent.setType("text/plain");
                        // use from live device
                        emailIntent.setType("message/rfc822");
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.setting_tell_friend_email_subject));
                        emailIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.setting_tell_friend_email_content));
                        startActivity(Intent.createChooser(emailIntent, null));

                        bottomBoard.dismiss();
                    }
                });
        // Send SMS
        bottomBoard.add(getString(R.string.setting_send_sms), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri smsUri = Uri.parse("smsto:");
                        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
                        intent.putExtra("sms_body", getResources().getString(R.string.setting_tell_friend_sms_content));
                        startActivity(intent);
                        bottomBoard.dismiss();
                    }
                });
        //Cancel
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }

    private void checkForUpdates() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            public UpdatesInfo updatesInfo = new UpdatesInfo();

            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    int errno = mWeb.fCheckForUpdates(updatesInfo);
                    if (ErrorCode.OK != errno)
                        return errno;
                    if (0 == updatesInfo.versionCode)
                        return ErrorCode.BAD_RESPONSE;
                    return errno;
                } catch (Exception e) {
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == errno) {
                    try {
                        int currVerCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        String currVerName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                        if (currVerCode >= updatesInfo.versionCode) {
                            mMsgBox.show(null, getString(R.string.settings_upgrade_is_uptodate));
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append(getString(R.string.settings_upgrade_curr_ver_is) + currVerName + "\n");
                            sb.append(getString(R.string.settings_upgrade_latest_ver_is) + updatesInfo.versionName + "\n");
                            if (0 < updatesInfo.changeLog.length) {
                                sb.append(getString(R.string.settings_upgrade_changelogs_are) + "\n");
                                for (int i = 0; i < updatesInfo.changeLog.length; ++i)
                                    sb.append("  " + (i + 1) + ". " + updatesInfo.changeLog[i] + "\n");
                            }

                            new AlertDialog.Builder(SettingActivity.this)
                                    .setTitle(R.string.settings_upgrade_will_you_upgrade_now)
                                    .setMessage(sb.toString())
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                                            if (getPackageManager().queryIntentActivities(intent, 0).size() != 0) {
                                                AppStatusService.setIsMonitoring(false);
                                                startActivity(intent);
                                            } else {
                                                mMsgBox.toast(R.string.settings_upgrade_no_market);
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .create().show();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    mMsgBox.show(null, getString(R.string.operation_failed));
                }
            }

        }.execute((Void) null);
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mWeb = WowTalkWebServerIF.getInstance(SettingActivity.this);
        mPrefUtil = PrefUtil.getInstance(this);
        initView();
    }

	@Override
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
		if (PrefUtil.getInstance(this).getMyPasswordChangedState()) {
			txtPwdState.setVisibility(View.GONE);
		} else {
			txtPwdState.setVisibility(View.VISIBLE);
		}
		Buddy me = new Buddy();
		me.userID = PrefUtil.getInstance(this).getUid();
		me.photoUploadedTimeStamp = PrefUtil.getInstance(this).getMyPhotoUploadedTimestamp();
		PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, me, true);

        updateNoticeStatus();
        setAccountData();
        getAccountUnreadCounts();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_ALBUM_COVER_GOT, mAlbumCoverObserver);
    }

    private void getAccountUnreadCounts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, Integer> accountUnreadMap = new HashMap<String, Integer>();
                int resultCode = mWeb.getAccountUnreadCounts(mPrefUtil.getAccountIds(), accountUnreadMap);
                if (resultCode == ErrorCode.OK) {
                    if (!accountUnreadMap.isEmpty()) {
                        ArrayList<Account> tempAccounts = mPrefUtil.getAccountList();
                        Account tempAccount = null;
                        for (Iterator<Account> iterator = tempAccounts.iterator(); iterator.hasNext();) {
                            tempAccount = iterator.next();
                            if (!mPrefUtil.getUid().equals(tempAccount.uid)) {
                                tempAccount.unreadCounts = accountUnreadMap.get(tempAccount.uid);
                            }
                        }
                        mPrefUtil.setAccountList(tempAccounts);
                        mAccountDatas = tempAccounts;
                        mHandler.sendEmptyMessage(HANDLER_GET_ACCOUNT_UNREAD_COUNT);
                    }
                }
            }
        }).start();
    }

    private void setAccountData() {
        mAccountDatas = mPrefUtil.getAccountList();

        if (mAccountDatas.isEmpty()) {
            // 老版本升级，没有account_list的SP，将当前帐号添加进account_list
            Account account = Account.getAccountFromSP(mPrefUtil);
            Database database = new Database(SettingActivity.this);
            AlbumCover albumCover = database.getAlbumCover(account.uid);
            if (null != albumCover) {
                account.albumCoverFileId = null == albumCover.fileId ? "" : albumCover.fileId;
                account.albumCoverExt = null == albumCover.ext ? "" : albumCover.ext;
            }
            mPrefUtil.saveAccountsList(null, account);
            mAccountDatas.add(account);
        }

        if (null == mAccountAdapter) {
            mAccountAdapter = new AccountsListAdapter(this, mAccountDatas);
            mAccountListView.setAdapter(mAccountAdapter);
        } else {
            mAccountAdapter.setDataSource(mAccountDatas);
            mAccountAdapter.notifyDataSetChanged();
        }
        ListHeightUtil.setListHeight(mAccountListView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        Database.removeDBTableChangeListener(mAlbumCoverObserver);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position,
            long id) {
        // Wowtalk biz 的逻辑是切换帐号，
        // 一米家校的逻辑改成了进入个人资料页面。
        Intent intent = new Intent();
        intent.setClass(SettingActivity.this, MyInfoActivity.class);
        startActivity(intent);
    }
}
