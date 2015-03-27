package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.AppUpgradeService;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.ArrayList;

public class SettingActivity extends Activity implements OnClickListener {
    ;

    //--------------------------------------------------------------------------------

    private static final int MSG_LOGOUT_SUCCESS = 100;
    private static final int HANDLER_GET_ACCOUNT_UNREAD_COUNT = 1;
    /**
     * 指示（输入）或回传（输出） app 是否有更新。若有更新，则显示红点。
     */
    public static final String EXTRA_APP_UPDATES_AVAILABLE = "app_updates_available";

    private ImageView imgPhoto;
	private TextView textView_settings_myinfo;
	
//	private ImageButton title_back;
//	private TextView textView_home_back;
	
	private MessageBox mMsgBox;

    private WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
    private boolean appUpdatesAvailable;

    private ArrayList<Account> mAccountDatas;
    
    
    private static SettingActivity instance;
    
	public static final SettingActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}

	public static final boolean isInstanciated() {
		return instance != null;
	}

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_GET_ACCOUNT_UNREAD_COUNT:
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
    
/**
 * 初始化各个控件
 */
    private void initView() {
		imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
		textView_settings_myinfo = (TextView) findViewById(R.id.settings_myinfo);
//		title_back = (ImageButton) findViewById(R.id.title_back);
//		textView_home_back = (TextView) findViewById(R.id.textView_home_back);
		
		
        AQuery q = new AQuery(this);
        
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.textView_home_back).clicked(this);

        q.find(R.id.img_thumbnail).clicked(this);
        q.find(R.id.settings_myinfo).clicked(this);
        q.find(R.id.layout_settings_myinfo).clicked(this);
        q.find(R.id.settings_my_qrcode).clicked(this);
        q.find(R.id.settings_account).clicked(this);
        q.find(R.id.settings_privacy).clicked(this);
        q.find(R.id.settings_upgrade_check_for_updates).clicked(this);
        q.find(R.id.settings_tell_friend).clicked(this);
        q.find(R.id.settings_rate).clicked(this);
        q.find(R.id.settings_about).clicked(this);

        updateUi();

        updateNoticeStatus();
	}

    private void updateUi() {
        AQuery q = new AQuery(this);
//        q.find(R.id.app_updates_available_indicator).visibility(appUpdatesAvailable ? View.VISIBLE : View.GONE);
        if (appUpdatesAvailable) {
        	q.find(R.id.app_updates_available_indicator).visibility(View.VISIBLE);
        	
        } else {
        	q.find(R.id.app_updates_available_indicator).visibility(View.GONE);
        }
    }

    private void updateNoticeStatus() {
        if(PrefUtil.getInstance(this).isSysNoticeEnabled()) {
        } else {
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
        	case R.id.title_back:
        	case R.id.textView_home_back:
        		finish();
        		break;
            case R.id.img_thumbnail:
            case R.id.settings_myinfo:
//            case R.id.layout_settings_myinfo:
                startActivity(intent.setClass(SettingActivity.this, MyInfoActivity.class));
                break;
            case R.id.settings_my_qrcode: {
                Intent i = new Intent(this, MyQRCodeActivity.class);
                ThemeHelper.putExtraCurrThemeResId(i, this);
                startActivity(i);
                break;
            }
            case R.id.settings_account:
                intent.setClass(SettingActivity.this, AccountSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.settings_privacy:
                intent.setClass(SettingActivity.this, PrivacySettingActivity.class);
                startActivity(intent);
                break;
            case R.id.settings_about:
                startActivity(intent.setClass(SettingActivity.this, AboutPage.class));
                break;
            case R.id.settings_tell_friend:
                tellFriend(v);
                break;
            case R.id.logout:
                logoutAccount();
                break;
            case R.id.settings_rate:
                if (true) {
                    mMsgBox.toast(R.string.not_implemented);
                } else {
                    Intent rateIntent = new Intent(Intent.ACTION_VIEW);
                    rateIntent.setData(Uri.parse("market://details?id=" + getPackageName()));
                    if (this.getPackageManager().queryIntentActivities(rateIntent, 0).size() != 0) {
                        AppStatusService.setIsMonitoring(false);
                        startActivity(rateIntent);
                    } else {
                        mMsgBox.toast("device not suport");
                    }
                }
                break;
            case R.id.settings_upgrade_check_for_updates:
                StartActivity.instance().changeNewUpdateFlagView(View.GONE);
                checkForUpdates();
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
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
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
                }
                catch (Exception e) {
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
                            appUpdatesAvailable = false;
                            mMsgBox.show(null, getString(R.string.settings_upgrade_is_uptodate));
                        } else {
                            appUpdatesAvailable = true;
                            StringBuilder sb = new StringBuilder();
                            sb.append(getString(R.string.settings_upgrade_curr_ver_is) + currVerName + "\n");
                            sb.append(getString(R.string.settings_upgrade_latest_ver_is) + updatesInfo.versionName + "\n");
                            if (0 < updatesInfo.changeLog.length) {
                                sb.append(getString(R.string.settings_upgrade_changelogs_are) + "\n");
                                for (int i = 0; i < updatesInfo.changeLog.length; ++i)
                                    sb.append("  " + (i + 1) + ". " + updatesInfo.changeLog[i] + "\n");
                            }

//                            new AlertDialog.Builder(SettingActivity.this)
//                                    .setTitle(R.string.settings_upgrade_will_you_upgrade_now)
//                                    .setMessage(sb.toString())
//                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialogInterface, int i) {
//                                            // upgrade method:
//                                            // 0 - go to download web page
//                                            // 1 - auto download apk and install it later
//                                            // 2 - go to market
//                                            final int method = 1;
//
//                                            if (method == 0) { // go to download web page
//                                                startActivity(new Intent(
//                                                        Intent.ACTION_VIEW,
//                                                        Uri.parse("http://www.onemeter.co/dl/")));
//                                            } else if (method == 1) {
//                                                String destFilename = getExternalCacheDir()
//                                                        + "/om_im_" + updatesInfo.versionCode + ".apk";
//                                                startService(new Intent(SettingActivity.this, AppUpgradeService.class)
//                                                                .putExtra(AppUpgradeService.EXTRA_URL, updatesInfo.link)
//                                                                .putExtra(AppUpgradeService.EXTRA_MD5SUM, updatesInfo.md5sum)
//                                                                .putExtra(AppUpgradeService.EXTRA_DEST_FILENAME, destFilename)
//                                                );
//                                            } else { // go to market
//                                                Intent intent = new Intent(Intent.ACTION_VIEW);
//                                                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
//                                                if (getPackageManager().queryIntentActivities(intent, 0).size() != 0) {
//                                                    AppStatusService.setIsMonitoring(false);
//                                                    startActivity(intent);
//                                                } else {
//                                                    mMsgBox.toast(R.string.settings_upgrade_no_market);
//                                                }
//                                            }
//                                        }
//                                    })
//                                    .setNegativeButton(R.string.cancel, null)
//                                    .create().show();
                            
                            
                            //新的弹框显示，版本更新操作
                            MessageDialog dialog = new MessageDialog(SettingActivity.this);
                            dialog.setTitle(R.string.settings_upgrade_will_you_upgrade_now);
                            dialog.setMessage(sb.toString());
                            dialog.setOnRightClickListener(getResources().getString(R.string.cancel), null);
                            dialog.setOnLeftClickListener(getResources().getString(R.string.ok), new MessageDialog.MessageDialogClickListener() {
                                @Override
                                public void onclick(MessageDialog dialog) {
                                    dialog.dismiss();
                                    // upgrade method:
                                    // 0 - go to download web page
                                    // 1 - auto download apk and install it later
                                    // 2 - go to market
                                    final int method = 1;

                                    if (method == 0) { // go to download web page
                                        startActivity(new Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("http://www.onemeter.co/dl/")));
                                    } else if (method == 1) {
                                        String destFilename = getExternalCacheDir()
                                                + "/om_im_" + updatesInfo.versionCode + ".apk";
                                        startService(new Intent(SettingActivity.this, AppUpgradeService.class)
                                                        .putExtra(AppUpgradeService.EXTRA_URL, updatesInfo.link)
                                                        .putExtra(AppUpgradeService.EXTRA_MD5SUM, updatesInfo.md5sum)
                                                        .putExtra(AppUpgradeService.EXTRA_DEST_FILENAME, destFilename)
                                        );
                                    } else { // go to market
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                                        if (getPackageManager().queryIntentActivities(intent, 0).size() != 0) {
                                            AppStatusService.setIsMonitoring(false);
                                            startActivity(intent);
                                        } else {
                                            mMsgBox.toast(R.string.settings_upgrade_no_market);
                                        }
                                    }
                                    
                                }
                            });
                            dialog.show();
                            
                        }

                        updateUi();
                        setResult(RESULT_OK, new Intent().putExtra(EXTRA_APP_UPDATES_AVAILABLE, appUpdatesAvailable));
                    }
                    catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    mMsgBox.show(null, getString(R.string.operation_failed));
                }
            }

        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_APP_UPDATES_AVAILABLE, appUpdatesAvailable);
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        
        instance = this;

        mMsgBox = new MessageBox(this);
        mWeb = WowTalkWebServerIF.getInstance(SettingActivity.this);
        mPrefUtil = PrefUtil.getInstance(this);
        appUpdatesAvailable = savedInstanceState != null
                ? savedInstanceState.getBoolean(EXTRA_APP_UPDATES_AVAILABLE)
                : getIntent().getBooleanExtra(EXTRA_APP_UPDATES_AVAILABLE, false);
        
//        if (mPrefUtil.getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
//        	findViewById(R.id.imageView_tag_tea).setVisibility(View.VISIBLE);
//        } else {
//        	findViewById(R.id.imageView_tag_tea).setVisibility(View.GONE);
//        }
        Log.d("--create setting");
        initView();
    }

	@Override
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
		Buddy me = new Buddy();
		me.userID = PrefUtil.getInstance(this).getUid();
		me.photoUploadedTimeStamp = PrefUtil.getInstance(this).getMyPhotoUploadedTimestamp();
		PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, me, true);
		//获取个人的账号信息
		textView_settings_myinfo.setText(PrefUtil.getInstance(this).getMyNickName());
        updateNoticeStatus();
        setAccountData();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_ALBUM_COVER_GOT, mAlbumCoverObserver);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this); 
        Database.removeDBTableChangeListener(mAlbumCoverObserver);
    }
    
     
    @Override
    protected void onDestroy() {
    	Log.d("---onDestroy");
    	if (instance != null) {
    		instance = null;
    	}
    	super.onDestroy();
    }

    /**
     * 这是经过简化的 {@link co.onemeter.oneapp.ui.ManageAccountsActivity#logoutAccount(android.os.Handler)}，
     * 移除了自动切换帐号的逻辑。
     */
    private void logoutAccount() {
        mMsgBox.showWait();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int resultCode =  mWeb.fLogout();
                if (resultCode == ErrorCode.OK) {
                    ManageAccountsActivity.deleteDatasInDB(SettingActivity.this);
                    mPrefUtil.logoutAccount();
                    WowTalkVoipIF.getInstance(SettingActivity.this).fStopWowTalkService();
                    Database database = new Database(SettingActivity.this);
                    database.close();

                    mMsgBox.dismissWait();

                    // goto login page
                    if (StartActivity.isInstanciated()) {
                        StartActivity.instance().finish();
                    }
                    Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    mMsgBox.dismissWait();
                    mMsgBox.toast(R.string.manage_account_logout_failure);
                }
            }
        }).start();
    }
}
