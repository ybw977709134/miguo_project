package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.wowtalk.api.Account;
import org.wowtalk.api.AlbumCover;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Connect2;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.IDBTableChangeListener;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.AccountsListAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

public class ManageAccountsActivity extends Activity implements OnClickListener, OnItemClickListener {

    public static final int HANDLER_SWITCHING_ACCOUNT = 1;
    public static final int HANDLER_SWITCH_ACCOUNT_SUCCESSFUL = 2;
    public static final int HANDLER_SWITCH_ACCOUNT_FAILURE = 3;
    public static final int HANDLER_SWITCH_ACCOUNT_HOME = 4;

    private static final int HANDLER_GET_ACCOUNT_UNREAD_COUNT = 1;

    private MessageBox mMsgBox;
    private WebServerIF mWeb;
    private PrefUtil mPrefUtil;

    private ImageButton mBackBtn;
    private TextView mEditView;
    private ListView mAccountsListView;
    private TextView mLogoutView;
    private TextView mAddAccountView;
    private TextView mCompanyView;
    private TextView mUserNameView;
    private TextView mChangePwdView;
    private AccountsListAdapter mAccountsAdapter;

    private ArrayList<Account> mAccountDatas;
    private boolean mIsDeleteMode;
    private BottomButtonBoard mBottomMenu;

    private Handler mSwitchAccountHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_SWITCHING_ACCOUNT:
                mMsgBox.showOrUpdateWatiMsg(String.valueOf(msg.obj));
                break;
            case HANDLER_SWITCH_ACCOUNT_SUCCESSFUL:
                mMsgBox.dismissWait();
                setData();
                getAccountUnreadCounts();
                break;
            case HANDLER_SWITCH_ACCOUNT_HOME:
                mMsgBox.dismissWait();
                if (StartActivity.isInstanciated()) {
                    StartActivity.instance().finish();
                }
                Intent intent = new Intent(ManageAccountsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case HANDLER_SWITCH_ACCOUNT_FAILURE:
                mMsgBox.dismissWait();
                mMsgBox.show(null, String.valueOf(msg.obj));
            default:
                break;
            }
        };
    };

    private RemoveAccountListener mRemoveAccountListener = new RemoveAccountListener() {
        @Override
        public void onRemovedAccount(Account account) {
            showRemoveAccountMenu(account);
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_GET_ACCOUNT_UNREAD_COUNT:
                mAccountsAdapter.setDataSource(mAccountDatas);
                mAccountsAdapter.notifyDataSetChanged();
                ListHeightUtil.setListHeight(mAccountsListView);
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
                    setData();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_accounts);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mWeb = WebServerIF.getInstance(this);
        mMsgBox = new MessageBox(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mBottomMenu = new BottomButtonBoard(ManageAccountsActivity.this, getWindow().getDecorView());

        initView();
    }

    protected void showRemoveAccountMenu(final Account account) {
        mBottomMenu.clearView();
        mBottomMenu.add(
                String.format(getString(R.string.manage_account_delete_confirm), account.wowtalkId),
                BottomButtonBoard.BUTTON_RED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBottomMenu.dismiss();
                        // 清除此用户uid对应的数据库
                        Database.dropDatabase(ManageAccountsActivity.this, account.uid);
                        mPrefUtil.removeAccount(account.uid);
                        mPrefUtil.clearByUid(account.uid);
                        setData();
                    }
                });
        mBottomMenu.addCancelBtn(getString(R.string.cancel));
        mBottomMenu.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
        setData();
        getAccountUnreadCounts();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_ALBUM_COVER_GOT, mAlbumCoverObserver);
    }

    private void setData() {
        mCompanyView.setText(mPrefUtil.getCompanyId());
        mUserNameView.setText(mPrefUtil.getMyNickName());

        mAccountDatas = mPrefUtil.getAccountList();
        if (null == mAccountsAdapter) {
            mAccountsAdapter = new AccountsListAdapter(this, mAccountDatas, mRemoveAccountListener);
            mAccountsListView.setAdapter(mAccountsAdapter);
        } else {
            mAccountsAdapter.setDataSource(mAccountDatas);
            mAccountsAdapter.notifyDataSetChanged();
        }
        ListHeightUtil.setListHeight(mAccountsListView);

        resetEditViewStatus();
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

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);

        Database.removeDBTableChangeListener(mAlbumCoverObserver);
    }

    private void initView() {
        mBackBtn = (ImageButton) findViewById(R.id.title_back);
        mEditView = (TextView) findViewById(R.id.edit);
        mAccountsListView = (ListView) findViewById(R.id.accounts_listview);
        mAddAccountView = (TextView) findViewById(R.id.add_account);
        mCompanyView = (TextView) findViewById(R.id.txt_company);
        mUserNameView = (TextView) findViewById(R.id.username);
        mChangePwdView = (TextView) findViewById(R.id.change_pwd);
        mLogoutView = (TextView) findViewById(R.id.logout);

        mBackBtn.setOnClickListener(this);
        mEditView.setOnClickListener(this);
        mAddAccountView.setOnClickListener(this);
        mChangePwdView.setOnClickListener(this);
        mLogoutView.setOnClickListener(this);
        mAccountsListView.setOnItemClickListener(this);
    }

    /**
     * reset the edit view of manage accounts.
     */
    private void resetEditViewStatus() {
        boolean hasMultiAccounts = (null != mAccountDatas
                && mAccountDatas.size() > 1);
        if (hasMultiAccounts) {
            mEditView.setVisibility(View.VISIBLE);
        } else {
            mEditView.setVisibility(View.GONE);
            mIsDeleteMode = false;
            mEditView.setText(getString(R.string.manage_account_title_edit));
            mAccountsAdapter.setDeleteMode(mIsDeleteMode);
            mAccountsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            finish();
            break;
        case R.id.edit:
            mIsDeleteMode = !mIsDeleteMode;
            mEditView.setText(getString(mIsDeleteMode ? R.string.manage_account_title_done
                    : R.string.manage_account_title_edit));
            mAccountsAdapter.setDeleteMode(mIsDeleteMode);
            mAccountsAdapter.notifyDataSetChanged();
            break;
        case R.id.change_pwd:
            Intent changePwdIntent = new Intent(ManageAccountsActivity.this, SettingPasswordActivity.class);
            startActivity(changePwdIntent);
            break;
        case R.id.logout:
            showLogoutMenu();
            break;
        case R.id.add_account:
            Intent addAccount = new Intent(ManageAccountsActivity.this, LoginActivity.class);
            addAccount.putExtra(LoginActivity.EXTRA_IS_ADD_ACCOUNT, true);
            startActivity(addAccount);
            break;
        default:
            break;
        }
    }

    private void showLogoutMenu() {
        mBottomMenu.clearView();
        mBottomMenu.add(getString(R.string.manage_account_logout), BottomButtonBoard.BUTTON_RED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBottomMenu.dismiss();
                        logoutAccount(mSwitchAccountHandler);
                    }
                });
        mBottomMenu.addCancelBtn(getString(R.string.cancel));
        mBottomMenu.show();
    }

    /**
     * 如果只剩当前帐号，则退出后返回登录界面；
     * 否则，退出后切换到另一个已添加的帐号B，B登陆失败，继续切换到C，直到成功或全部切完
     * @param handler
     */
    private void logoutAccount(final Handler handler) {
        mMsgBox.showWait();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg1 = handler.obtainMessage();
                msg1.what = HANDLER_SWITCHING_ACCOUNT;
                msg1.obj = String.format(
                        getString(R.string.manage_account_switching_logout),
                        mPrefUtil.getMyWowtalkID());
                handler.sendMessage(msg1);
                int resultCode =  mWeb.fLogout();
                if (resultCode == ErrorCode.OK) {
                    deleteDatasInDB(ManageAccountsActivity.this);
                    mPrefUtil.logoutAccount();
                    WowTalkVoipIF.getInstance(ManageAccountsActivity.this).fStopWowTalkService();
                    Database database = new Database(ManageAccountsActivity.this);
                    database.close();
                    // 变换Database,Connect2的标志位(用于判断是否切换了用户)
                    Database.sFlagIndex++;
                    Connect2.sFlagIndex++;

                    ArrayList<Account> accounts = mPrefUtil.getAccountList();
                    // 循环切换下一个帐号，直至切换成功
                    do {
                        // 退出的这个帐号为最后一个帐号，直接跳转到登录界面
                        if (accounts.isEmpty()) {
                            Message msg2 = handler.obtainMessage();
                            msg2.what = HANDLER_SWITCH_ACCOUNT_HOME;
                            handler.sendMessage(msg2);
                            break;
                        } else {
                            // 否则，切换到其他帐号
                            Account newAccount = accounts.get(0);
                            int loginCode = 0;
                            // 登录newAccount
                            Message msg3 = handler.obtainMessage();
                            msg3.what = HANDLER_SWITCHING_ACCOUNT;
                            msg3.obj = String.format(
                                    getString(R.string.manage_account_switching_login),
                                    newAccount.wowtalkId);
                            handler.sendMessage(msg3);
                            loginCode = mWeb.loginByHashedPwdForBiz(newAccount.company, newAccount.uid, newAccount.password, new Buddy());
                            if (loginCode == ErrorCode.OK) {
                                mPrefUtil.saveOldSP(newAccount.uid);
                                mPrefUtil.clearOldSP();
                                mPrefUtil.saveAccountsList(null, newAccount);
                                mPrefUtil.fillNewSP(newAccount.uid);
                                mPrefUtil.setDisplayGroupId("");
                                AppStatusService.getOfflineMessages(ManageAccountsActivity.this);
                                WowTalkVoipIF.getInstance(ManageAccountsActivity.this).fStartWowTalkService();
                                Database.switchAccount();
                                StartActivity.dealLatestChatTargets(ManageAccountsActivity.this);
                                Message msg4 = handler.obtainMessage();
                                msg4.what = HANDLER_SWITCH_ACCOUNT_SUCCESSFUL;
                                handler.sendMessage(msg4);
                                break;
                            } else {
                                // 删除此登录失败的帐号，继续循环下一个帐号
                                mPrefUtil.removeAccount(newAccount.uid);
                                accounts.remove(0);
                            }
                        }
                    } while (true);
                } else {
                    mMsgBox.dismissWait();
                    mMsgBox.toast(R.string.manage_account_logout_failure);
                }
            }
        }).start();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (0 == position || mIsDeleteMode) {
            return;
        }
        showSwitchAccountOption(position);
    }

    private void showSwitchAccountOption(final int position) {
        final Account newAccount = mAccountDatas.get(position);
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        bottomBoard.add(String.format(getString(R.string.manage_account_switch_confirm), newAccount.wowtalkId), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        mMsgBox.showWait(getString(R.string.manage_account_switch_prompt));
                        Account oldAccount = mAccountDatas.get(0);
                        ManageAccountsActivity.switchAccount(ManageAccountsActivity.this,
                                oldAccount, newAccount, mSwitchAccountHandler);
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();

    }

    /**
     * delete contacts, chat messages, and timelines according to the security level
     */
     public static void deleteDatasInDB(Context context) {
        PrefUtil prefUtil = PrefUtil.getInstance(context);
        int securityLevel = prefUtil.getSecurityLevel();
        Log.i("deleteMessagesAndMoments, the securtity level is " + securityLevel);
        // clear
        if (Constants.SECURITY_LEVEL_HIGH == securityLevel) {
            // 清除聊天信息／动态（联系人保留）
            Log.i("delete chat messages and timelines.");
            // 清除显示的部门id，促使重新获取
//            prefUtil.setDisplayGroupId("");
            Database dbHelper = new Database(context);
//            prefUtil.setGroupMembersUptodatePerfectly(false);
//            GlobalValue.setNeedToRefreshAllGroups(false);
            dbHelper.deleteAllChatMessages();
            dbHelper.clearLatestChatTargets();
//            dbHelper.clearGroupsForBiz(true);
//            dbHelper.deleteAllBuddies();
//            dbHelper.deleteAllBuddyDetails();
//            dbHelper.clearFavoriteContactsAndGroups();
            dbHelper.clearMomentsAndReviews();
        }
    }

    /**
     * 切换帐号，包括修改SP(方法中重起了一个子线程执行)
     * @param context
     * @param prefUtil
     * @param oldAccount
     * @param newAccount
     */
    public static void switchAccount(final Context context,
            final Account oldAccount, final Account newAccount,
            final Handler handler) {
        if (null == oldAccount || null == newAccount || null == handler) {
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                Message msg1 = handler.obtainMessage();
                msg1.what = HANDLER_SWITCHING_ACCOUNT;
                msg1.obj = String.format(context.getString(R.string.manage_account_switching_login),
                        newAccount.wowtalkId);
                handler.sendMessage(msg1);
                WebServerIF webIF = WebServerIF
                        .getInstance(context);
                PrefUtil prefUtil = PrefUtil.getInstance(context);
                // 1. close DB
                Database database = new Database(context);
                database.close();
                Database.sFlagIndex++;
                Connect2.sFlagIndex++;
                // 2. 保存老帐号数据
                prefUtil.saveOldSP(oldAccount.uid);
                prefUtil.clearOldSP();
                // 3. login
                int loginCode = webIF.loginByHashedPwdForBiz(newAccount.company,
                        newAccount.uid, newAccount.password, new Buddy());
                if (loginCode != ErrorCode.OK) {
                    prefUtil.fillNewSP(oldAccount.uid);
                    Message msg2 = handler.obtainMessage();
                    msg2.what = HANDLER_SWITCH_ACCOUNT_FAILURE;
                    msg2.obj = getLoginFailureMsg(context, loginCode);
                    handler.sendMessage(msg2);
                    return;
                }
                // profile，更新头像
                int getProfileCode = WebServerIF.getInstance(context).fGetMyProfile();
                if (getProfileCode != ErrorCode.OK) {
                    prefUtil.fillNewSP(oldAccount.uid);
                    Message msg3 = handler.obtainMessage();
                    msg3.what = HANDLER_SWITCH_ACCOUNT_FAILURE;
                    msg3.obj = context.getString(R.string.manage_account_switch_failure);
                    handler.sendMessage(msg3);
                    return;
                }
                newAccount.photoUploadTimeStamp = prefUtil.getMyPhotoUploadedTimestamp();
                newAccount.name = prefUtil.getMyNickName();
                // album_cover
                // 获取新帐户的动态封面(网络请求操作)
                AlbumCover ac = new AlbumCover();
                if (ErrorCode.OK == WebServerIF.getInstance(context)
                        .fGetAlbumCover(prefUtil.getUid(), ac)) {
                    newAccount.albumCoverFileId = null == ac.fileId ? "" : ac.fileId;
                    newAccount.albumCoverExt = null == ac.ext ? "" : ac.ext;
                    Database tempDB = new Database(context);
                    tempDB.storeAlbumCover(prefUtil.getUid(), ac);
                }
                // 4. logout，忽略logout是否成功的检验(没有更好的方案)
                webIF.logoutByUid(oldAccount.uid, oldAccount.password);
                // 5. 拷贝数据到SP_current
                prefUtil.saveOldSP(newAccount.uid);
                prefUtil.clearOldSP();
                prefUtil.saveAccountsList(oldAccount, newAccount);
                prefUtil.fillNewSP(newAccount.uid);
                // 移除原先显示的部门id，促使在联系人列表界面重新获取部门及成员
                prefUtil.setDisplayGroupId("");
                // 6. 重启wowtalk_service
                setupWowtalkService(context);
                // 7. 重新下载部门／员工，最近联系人
                StartActivity.getMembersAndChatTargetsFromServer(context);

                // 8. 触发用户切换的监听
                // 变换Database,Connect2的标志位(用于判断是否切换了用户)
                // 触发监听用户切换的操作，去变更数据库连接，以及StartActivity中更改未读信息，下载新成员等
                Database.switchAccount();
                Message msg4 = handler.obtainMessage();
                msg4.what = HANDLER_SWITCH_ACCOUNT_SUCCESSFUL;
                handler.sendMessage(msg4);

                // 9. 其他如获取安全级别在StartActivity#mSwitchAccountObserver中执行
            }
        }).start();
    }

    private static String getLoginFailureMsg(Context context, int result) {
        int stringResId = 0;
        switch (result) {
        case ErrorCode.AUTH:
            stringResId = R.string.login_auth_error;
            break;
        case ErrorCode.USER_NOT_EXISTS:
            stringResId = R.string.login_user_not_exists;
            break;
        default:
            stringResId = R.string.login_unknown_error;
            break;
        }
        return context.getString(stringResId);
    }

    /**
     * 1.get server info:必须在所有的网络操作之前调用。<br>
     * 2.getOfflineMessages，在重启wowtalkservice之前
     * 3.start wowtalkservice, checkAppUpdate, getSecurityLevel, report info , and so on.
     */
    public static void setupWowtalkService(Context context) {
        WebServerIF webIF = WebServerIF.getInstance(context);
        // 下面调用的所有的网络操作，除第一个(后续网络操作的前提)和最后一个（后面没有操作了，且此处调用的地方已经是子线程）外，
        // 其他的都重启子线程完成，因为它们之间没有先后顺序
        webIF.getServerInfo();
        AppStatusService.getOfflineMessages(context);
        restartWowtalkService(context, true);

        // report push sercie token to server
        webIF.fReportInfoWithPushToken();
    }

    /* !!!important!! */
    private static void restartWowtalkService(Context context, final boolean isRestart) {
        // set delegate
        WowTalkVoipIF.fSetChatMessageDelegate(StartActivity.instance());
        WowTalkVoipIF.fSetMainActivity(CallMainActivity.class);
        WowTalkVoipIF.fSetNotificationDelegate(StartActivity.instance());

        // start wowtalk as background
        WowTalkVoipIF.getInstance(context).fSetTransportMethod("udp");

        PrefUtil prefUtil = PrefUtil.getInstance(context);
        int setup_step = prefUtil.getSetupStep();
        Log.i("setup_step = " + setup_step);
        Log.i("uid_preference = " + prefUtil.getUid());
        Log.i("web_domain = " + prefUtil.getWebDomain());
        Log.i("web_password_preference = " + prefUtil.getPassword());
        Log.i("domain_preference = " + prefUtil.getSipDomain());
        Log.i("password_preference = " + prefUtil.getSipPassword());

        if (setup_step == 2) {
            Log.i("starting wowtalk service");
            if (isRestart) {
                Log.i("the sip domain has changed, restart the service...");
                WowTalkVoipIF.getInstance(context).fStopWowTalkService();
            }
            WowTalkVoipIF.getInstance(context).fStartWowTalkService();
            WowTalkVoipIF.fDisableRingingInSDK(true);
        }
    }

    public interface RemoveAccountListener {
        // 删除某个非当前帐号
        void onRemovedAccount(Account account);
    }
}
