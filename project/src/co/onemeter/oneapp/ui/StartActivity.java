package co.onemeter.oneapp.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.*;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.utils.WebServerEventPoller;

import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.core.RegistrationState;
import org.wowtalk.core.WowTalkChatMessageState;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.AppStatusService.AppStatusBinder;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

@SuppressWarnings("deprecation")
public class StartActivity extends TabActivity
implements OnClickListener, WowTalkUIChatMessageDelegate, WowTalkNotificationDelegate, OnTabChangeListener {
	
	public static final int TAB_SMS = 0;
	public static final int TAB_CONTACTS = 1;
	public static final int TAB_FRIENDS = 2;
	public static final int TAB_HOME = 3;
	public static final int TAB_SETTING = 4;

    public static final String KEY_IS_START_FROM_LOGIN = "is_start_from_login";

    private static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
//    private static final String ACTION_WIFI_STATE_CHANGE = "android.net.wifi.STATE_CHANGE";
    private NetworkStateChangeReceiver mNetworkStateChangedReceiver = new NetworkStateChangeReceiver();

	/**
	 * 在tab上显示的统计未读条数的最大值，超过则显示"99+"
	 */
	private static final int MAX_COUNTS_ON_TABS = 99;

    private static final int HANDLER_FINISHED = 0;
    private static final int HANDLER_GET_COMPANY_STRUCTURE = 1;
    private static final int HANDLER_GET_DEPTS = 2;
    private static final int HANDLER_GET_TEMP_GROUPS = 3;
    private static final int HANDLER_GET_FAVORITE_GROUPS_AND_CONTACTS = 4;
    private static final int HANDLER_GET_LATEST_CHAT_TARGETS = 5;
    private static final int HANDLER_GET_CONTACTS = 6;
    private static final int HANDLER_GET_SECURITY_LEVEL = 10;

	private static StartActivity instance;
    private static Object sSyncObj = new Object();
    /**
     * 正在下载部门／员工
     */
    private static boolean sIsDownloading = false;


	private TabHost mHost;
	private Intent mSmsIntent;
	private Intent mContactIntent;
	private Intent mFriendIntent;
	private Intent mHomeIntent;
	private Intent mSettingIntent;
	
//	private View mTabbar;
	private View mTabSms;
	private View mTabContact;
	private View mTabFriend;
	private View mTabEvent;
	private View mTabSetting;
	
	private ImageView imgTabSms;
	private ImageView imgTabContacts;
	private ImageView imgTabFriends;
	private ImageView imgTabHome;
	private ImageView imgTabSetting;
	private TextView mNewUpdateView;
	
	private TextView txtTabSms;
	private TextView txtTabContact;
	private TextView txtTabFriend;
	private TextView txtTabHome;
	private TextView txtTabSetting;
	private int _selectedTabIndex;
	private TextView txt_unreadMsg;
    private TextView txt_friends_news;
    private TextView txt_pendingin_requests;
    private WowTalkWebServerIF mWeb = null;
    private Database mDb = null;
    private PrefUtil mPrefUtil;
    private MessageBox mMsgbBox;
    private long exitTime = 0;
    private int mSecurityLevel;
//    private boolean mIsBindMonitorService;
    public static boolean isOnStackTop = false;

    private final static int NOTE_TXT_BKG_ALPHA=180;

    private final static long NO_NET_REMIND_INTERVAL=2*1000;
    private long lastNoNetRemindTime;
    private boolean mIsStartFromLogin;
    private boolean mHasBoundService;

	public static final StartActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}

	public static final boolean isInstanciated() {
		return instance != null;
	}

    private ServiceConnection mAppStatusServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppStatusService.AppStatusBinder binder = (AppStatusBinder) service;
            AppStatusService appService = binder.getService();
            appService.startAppStatusListener();
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case HANDLER_FINISHED:
//                mMsgbBox.dismissWait();
                break;
            case HANDLER_GET_COMPANY_STRUCTURE:
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_company_structure);
                break;
            case HANDLER_GET_DEPTS:
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_depts);
                break;
            case HANDLER_GET_TEMP_GROUPS:
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_temp_groups);
                break;
            case HANDLER_GET_FAVORITE_GROUPS_AND_CONTACTS:
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_favorite_items);
                break;
            case HANDLER_GET_LATEST_CHAT_TARGETS:
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_latest_chat_targets);
                break;
            case HANDLER_GET_CONTACTS :
//                mMsgbBox.showOrUpdateWatiMsg(R.string.start_downloading_buddies);
                break;
            case HANDLER_GET_SECURITY_LEVEL:
                synchronized (StartActivity.class) {
//                    if (mSecurityLevel == Constants.SECURITY_LEVEL_NORMAL) {
//                        // 是否已有监听服务，有则unbind
//                        if (mIsBindMonitorService) {
//                            unbindService(mAppStatusServiceConnection);
//                        }
//                        mIsBindMonitorService = false;
//                    } else {
//                        // 监听前后台切换，确定是否刷新群组成员
//                        Intent appStatusService = new Intent(StartActivity.this, AppStatusService.class);
//                        // 当前StartActivity被销毁时，绑定的服务也要被销毁
//                        bindService(appStatusService, mAppStatusServiceConnection, Context.BIND_AUTO_CREATE);
//                        mIsBindMonitorService = true;
//                    }

                    // 监听前后台切换，确定是否刷新群组成员
                    Intent appStatusService = new Intent(StartActivity.this, AppStatusService.class);
                    // 当前StartActivity被销毁时，绑定的服务也要被销毁
                    mHasBoundService = bindService(appStatusService, mAppStatusServiceConnection, Context.BIND_AUTO_CREATE);
                }
                break;
            default:
                break;
            }
        };
    };

	private TabHost.TabSpec buildTabSpec(String tag, String resLabel, int resIcon, Intent content) {
		return this.mHost.newTabSpec(tag).setIndicator(resLabel, getResources().getDrawable(resIcon)).setContent(content);
	}
	
	private void initView() {
//		mTabbar =  findViewById(R.id.tab_bar);
		mTabSms =  findViewById(R.id.tab_sms);
		mTabContact =  findViewById(R.id.tab_contact);
		mTabFriend =  findViewById(R.id.tab_friend);
		mTabEvent =  findViewById(R.id.tab_home);
		mTabSetting =  findViewById(R.id.tab_setting);
		
		imgTabSms = (ImageView) findViewById(R.id.tab_sms_image);
		imgTabContacts = (ImageView) findViewById(R.id.tab_contact_image);
		imgTabFriends = (ImageView) findViewById(R.id.tab_friend_image);
		imgTabHome = (ImageView) findViewById(R.id.tab_home_image);
		imgTabSetting = (ImageView) findViewById(R.id.tab_setting_image);
		
		txtTabSms = (TextView) findViewById(R.id.tab_sms_text);
		txtTabContact = (TextView) findViewById(R.id.tab_contact_text);
		txtTabFriend = (TextView) findViewById(R.id.tab_friend_text);
		txtTabHome = (TextView) findViewById(R.id.tab_home_text);
		txtTabSetting = (TextView) findViewById(R.id.tab_setting_text);
		txt_unreadMsg = (TextView)findViewById(R.id.txt_unreadMsg);
        txt_friends_news = (TextView)findViewById(R.id.txt_friends_have_news);
        txt_pendingin_requests = (TextView)findViewById(R.id.txt_pendingin_requests);
        mNewUpdateView = (TextView) findViewById(R.id.new_update);

        txt_unreadMsg.getBackground().setAlpha(NOTE_TXT_BKG_ALPHA);
        txt_friends_news.getBackground().setAlpha(NOTE_TXT_BKG_ALPHA);
        txt_pendingin_requests.getBackground().setAlpha(NOTE_TXT_BKG_ALPHA);

		mTabSms.setOnClickListener(this);
		mTabContact.setOnClickListener(this);
		mTabFriend.setOnClickListener(this);
		mTabEvent.setOnClickListener(this);
		mTabSetting.setOnClickListener(this);
	}
	
	private void setDefaultImageView() {
		imgTabSms.setImageResource(R.drawable.tabbar_sms);
		imgTabContacts.setImageResource(R.drawable.tabbar_contact);
		imgTabFriends.setImageResource(R.drawable.tabbar_feed);
		imgTabHome.setImageResource(R.drawable.tabbar_home_green);
		imgTabSetting.setImageResource(R.drawable.tabbar_settings);
		
		txtTabSms.setTextColor(getResources().getColor(R.color.gray));
		txtTabContact.setTextColor(getResources().getColor(R.color.gray));
		txtTabFriend.setTextColor(getResources().getColor(R.color.gray));
		txtTabHome.setTextColor(getResources().getColor(R.color.gray));
		txtTabSetting.setTextColor(getResources().getColor(R.color.gray));
	}
	
	private void setTab(int index) {
		setDefaultImageView();
        _selectedTabIndex = index;

		switch (index) {
		case TAB_SMS:
			txtTabSms.setTextColor(getResources().getColor(R.color.red));
			imgTabSms.setImageResource(R.drawable.tabbar_sms_a);
            imgTabHome.setImageResource(R.drawable.tabbar_home_red);
			break;
		case TAB_CONTACTS:
			txtTabContact.setTextColor(getResources().getColor(R.color.blue));
			imgTabContacts.setImageResource(R.drawable.tabbar_contact_a);
            imgTabHome.setImageResource(R.drawable.tabbar_home_blue);
			break;
		case TAB_FRIENDS:
			txtTabFriend.setTextColor(getResources().getColor(R.color.blue));
			imgTabFriends.setImageResource(R.drawable.tabbar_feed_a);
            imgTabHome.setImageResource(R.drawable.tabbar_home_blue);
			break;
		case TAB_HOME:
			txtTabHome.setTextColor(getResources().getColor(R.color.green));
			imgTabHome.setImageResource(R.drawable.tabbar_home_green);
			break;
		case TAB_SETTING:
			txtTabSetting.setTextColor(getResources().getColor(R.color.green));
			imgTabSetting.setImageResource(R.drawable.tabbar_settings_a);
            imgTabHome.setImageResource(R.drawable.tabbar_home_green);
			break;
		default:
            setTab(TAB_SMS);
			return;
		}
		mHost.setCurrentTab(index);
	}
	
	@Override
	protected void onDestroy() {
        if (mHasBoundService) {
            unbindService(mAppStatusServiceConnection);
        }
        mPrefUtil.setDefaultTabNum(mHost.getCurrentTab());
		GlobalValue.clearNeedToRefreshMembersGroups();
		GlobalValue.setNeedToRefreshAllGroups(true);

        instance=null;
        unregisterReceiver(mNetworkStateChangedReceiver);
		super.onDestroy();
	}

	private void setupIntent() {
		mHost = getTabHost();
		mHost.setOnTabChangedListener(this);
			
		mSmsIntent = new Intent(StartActivity.this, SmsActivity.class);
        mContactIntent = new Intent(StartActivity.this, ContactsActivity.class);
		mFriendIntent = new Intent(StartActivity.this, TimelineActivity.class);
        mHomeIntent = new Intent(StartActivity.this, HomeActivity.class);
		mSettingIntent = new Intent(StartActivity.this, SettingActivity.class);
		
		mHost.addTab(buildTabSpec(String.valueOf(TAB_SMS), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mSmsIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_CONTACTS), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mContactIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_FRIENDS), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mFriendIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_HOME), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mHomeIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_SETTING), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mSettingIntent));
		
        _selectedTabIndex=TAB_HOME;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tab_sms:
			setTab(TAB_SMS);
			break;
		case R.id.tab_contact:
			setTab(TAB_CONTACTS);
			break;
		case R.id.tab_friend:
			setTab(TAB_FRIENDS);
			break;
		case R.id.tab_home:
			setTab(TAB_HOME);
			break;
		case R.id.tab_setting:
			setTab(TAB_SETTING);
			break;
		default:
			_selectedTabIndex = TAB_SMS;
			break;
		}
	}
	
//	public void fShowTabBar() {
//		mTabbar.setVisibility(View.VISIBLE);
//	}
//
//	public void fHideTabBar() {
//		mTabbar.setVisibility(View.GONE);
//	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setContentView(R.layout.tabmaster);
        UmengUpdateAgent.setUpdateOnlyWifi(false);
        UmengUpdateAgent.update(this);
        PinyinHelper.instance().init(this, R.raw.pinyin);

        mWeb = WowTalkWebServerIF.getInstance(this);
        mDb = new Database(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mMsgbBox = new MessageBox(this);

		initView();
		setupIntent();

        if (null == savedInstanceState) {
            mIsStartFromLogin = getIntent().getBooleanExtra(KEY_IS_START_FROM_LOGIN, false);
        } else {
            mIsStartFromLogin = savedInstanceState.getBoolean(KEY_IS_START_FROM_LOGIN, false);
        }

        Connect2.setContext(this);
        // !!! 所有的网络操作，都在此方法中的mWeb.getServerInfo()返回之后，才能调用，因为服务器地址可能有变换
        setupApplication();

        Connect2.setOnNetworkStateChangeListener(new Connect2.NetworkStateIndListener() {
            @Override
            public void onNetworkStateInd(final boolean connected) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!connected && (System.currentTimeMillis()-lastNoNetRemindTime>=NO_NET_REMIND_INTERVAL)) {
                            lastNoNetRemindTime = System.currentTimeMillis();
                            LayoutInflater inflater = getLayoutInflater();
                            View layout = inflater.inflate(R.layout.no_network_toast_layout, null);

                            Toast toast = new Toast(getApplicationContext());
                            toast.setGravity(Gravity.CENTER,0,0);
                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(layout);
                            toast.show();
                        }
                    }
                });
            }
        });

        tryHandleIncomeMsgNotification(getIntent());

        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ACTION_CONNECTIVITY_CHANGE);
//        networkFilter.addAction(ACTION_WIFI_STATE_CHANGE);
        registerReceiver(mNetworkStateChangedReceiver, networkFilter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // set to false, after invoked #getDatasFromServer
        outState.putBoolean(KEY_IS_START_FROM_LOGIN, false);
    }

    public void setTabBarStatus(final boolean hide) {
        //TODO this should be removed when FriendActivity is come back
        //not work well,comment it now
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                LinearLayout tabBarLayout=(LinearLayout) findViewById(R.id.tab_bar);
//                if(hide) {
//                    tabBarLayout.setVisibility(View.GONE);
//                } else {
//                    tabBarLayout.setVisibility(View.VISIBLE);
//                }
            }
        });
    }

    private IDBTableChangeListener mSwitchAccountObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            // 其他重启wowtalk_service在 ManageAccountsActivity中处理了
            fRefreshTabBadge_unreadMsg();
            fRefreshTabBadge_social();
            getSecurityLevelFromServer();
        }
    };

    private IDBTableChangeListener chatmessageObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fRefreshTabBadge_unreadMsg();
                }
            });
        }
    };

    private IDBTableChangeListener pendingRequestObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fRefreshTabBadge_contact();
                }
            });
        }
    };

    private IDBTableChangeListener momentReviewObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fRefreshTabBadge_social();
                }
            });
        }
    };

    @Override
	protected void onResume() {
		super.onResume();
        isOnStackTop = true;

		NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notifManager.cancel(org.wowtalk.ui.GlobalValue.NOTIFICATION_FOR_CHATMESSAGE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        org.wowtalk.ui.GlobalValue.screenW = displayMetrics.widthPixels;
        org.wowtalk.ui.GlobalValue.screenH = displayMetrics.heightPixels;

        MobclickAgent.onResume(this);

        if (GlobalValue.RELEASE_AS_WOWCITY) {
            fRefreshTabBadge_contact();
        }

        Database.addDBTableChangeListener(Database.DUMMY_TBL_SWITCH_ACCOUNT, mSwitchAccountObserver);
        Database.addDBTableChangeListener(Database.TBL_LATEST_CHAT_TARGET, chatmessageObserver);
        Database.addDBTableChangeListener(Database.TBL_LATEST_CHAT_TARGET_UNREAD_COUNT, chatmessageObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,pendingRequestObserver);
        }

        setTab(_selectedTabIndex);

        fRefreshTabBadge_unreadMsg();
        fRefreshTabBadge_social();
        fRefreshTabBadge_contact();

        WebServerEventPoller.instance(this).invoke();
	}

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        tryHandleIncomeMsgNotification(intent);
    }

    private void tryHandleIncomeMsgNotification(Intent intent) {
        try {
            if(null != intent) {
                String chatUserName=intent.getStringExtra(IncomeMessageIntentReceiver.INCOME_MSG_NOTIFICATION_PARA_ID);
                boolean isGroupMsg=intent.getBooleanExtra(IncomeMessageIntentReceiver.INCOME_MSG_NOTIFICATION_PARA_IS_GROUP_MSG,false);
                Log.i("chatUserName got in startActivity " + chatUserName);
                if(!TextUtils.isEmpty(chatUserName)) {
                    if(isGroupMsg) {
                        MessageComposerActivity.launchToChatWithGroup(
                                this,
                                MessageComposerActivity.class,
                                chatUserName);
                    } else {
                        MessageComposerActivity.launchToChatWithBuddy(
                                this,
                                MessageComposerActivity.class,
                                chatUserName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnStackTop = false;
        MobclickAgent.onPause(this);

        Database.removeDBTableChangeListener(mSwitchAccountObserver);
        Database.removeDBTableChangeListener(chatmessageObserver);
        Database.removeDBTableChangeListener(momentReviewObserver);
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            Database.removeDBTableChangeListener(pendingRequestObserver);
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		return false;
	}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() ==   KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            //as original friend_activity is replaced with TimelineActivity
            //TimelineActivity is sub activity in tabActivity,it can not receive onBackPressed mask by this function
            //so check it manually
            if(TAB_FRIENDS == _selectedTabIndex && null != TimelineActivity.instance() && TimelineActivity.instance().handleBackPress()) {
                //TODO this should be removed when FriendActivity is come back
                Log.i("moment activity handled back press");
            } else {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    exitTime = System.currentTimeMillis();
                    Toast.makeText(StartActivity.this,
                            getResources().getString(R.string.tap_again_to_close), Toast.LENGTH_SHORT).show();
                } else {
                    finish();
//                System.exit(0);
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

//	private void alert(final String msg) {
//		this.runOnUiThread(new Runnable(){
//
//			@Override
//			public void run() {
//				new android.app.AlertDialog.Builder(StartActivity.this).setMessage(msg).create().show();
//			}
//
//		});
//	}
	
    /* !!!important!! */
    private void setupWowtalkService(final boolean isRestart) {
        // set delegate
        WowTalkVoipIF.fSetChatMessageDelegate(instance);
        WowTalkVoipIF.fSetMainActivity(CallMainActivity.class);
        WowTalkVoipIF.fSetNotificationDelegate(instance);

        // start wowtalk as background
        WowTalkVoipIF.getInstance(this).fSetTransportMethod("udp");

        PrefUtil prefUtil = PrefUtil.getInstance(this);
        int setup_step = prefUtil.getSetupStep();
        Log.i("setup_step = " + setup_step);
        Log.i("uid_preference = " + prefUtil.getUid());
        Log.i("web_domain = " + prefUtil.getWebDomain());
        Log.i("web_password_preference = " + prefUtil.getPassword());
        Log.i("domain_preference = " + prefUtil.getSipDomain());
        Log.i("password_preference = " + prefUtil.getSipPassword());

        if (setup_step == 2) {
            Log.i("starting wowtalk service");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (isRestart) {
                        Log.i("the sip domain has changed, restart the service...");
                        WowTalkVoipIF.getInstance(StartActivity.this).fStopWowTalkService();
                    }
                    WowTalkVoipIF.getInstance(StartActivity.this).fStartWowTalkService();
                    WowTalkVoipIF.fDisableRingingInSDK(true);
                }
            }).start();
        }
    }

	@Override
	public void getActiveAppTypeChangeNotification(String arg0) {
	}

    @Override
    public void registrationStateUpdate(RegistrationState registrationState, String stateName) {
        final String state = registrationState.toString();
        Log.i("SIP auth status " + state);
        if (state.equals(RegistrationState.RegistrationInProgress.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.registration_logging));
        } else if (state.equals(RegistrationState.RegistrationIdle.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.registration_idle_state));
        } else if (state.equals(RegistrationState.RegistrationSuccess.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.app_name));
        } else if (state.equals(RegistrationState.RegistrationCleared.toString())) {

        } else if (state.equals(RegistrationState.RegistrationFailed.toString())) {
            if ("Authentication failure".equals(stateName)) {
                // 你已经在别的地方登录，返回登录界面

                Log.e("SIP auth failed, force logout");
                mWeb.fLogout();

                WowTalkVoipIF.getInstance(this).fStopWowTalkService();
                dismissNotification(GlobalValue.NOTIFICATION_FOR_REGISTRATIONSTATE);

                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra(LoginActivity.EXTRA_PROMPT,
                        getString(R.string.account_has_logged_in_somewhere_else));
                startActivity(loginIntent);

                finish();
            } else {
                SmsActivity.setNavTitle(getResources().getString(R.string.registration_failed));
            }
        }
    }

    /**
     * 服务器发送的类型为 l 的消息在这里处理。
     *
     * 有人删除了他和我的好友关系时，我会收到这样的通知。
     *
     * @param uid
     * @param displayName
     */
    @Override
	public void getBuddyListDecreaseNotification(String uid, String displayName) {
        Log.i("getBuddyListDecreaseNotification uid=", uid, ", displayname=", displayName);

        Database.open(this).deleteBuddyByUID(uid);

        mPrefUtil.setLocalContactListLastModified();
	}

    /**
     * 服务器发送的类型为 k 的消息在这里处理。
     * <p>
     * 在下列情形中，我会收到这样的通知：
     * <ul>
     * <li>有人添加我为好友，</li>
     * <li>有人同意了我的好友请求时，</li>
     * <li>有人请求加入我的群，</li>
     * </ul>
     * </p>
     * <p>
     * 不幸的是，由于该回调函数原型的限制，消息的细节丢失了，这里无法在以上情形之间进行区分。
     * </p>
     * @param uid
     * @param displayName 实测为 "unknown"
     */
	@Override
	public void getBuddyListIncreaseNotification(String uid, String displayName) {
        Log.i("getBuddyListIncreaseNotification uid=", uid, ", displayname=", displayName);

        // show notification
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, NewFriendsActivity.class), 0);
        showNotification(GlobalValue.NOTIFICATION_FOR_PENDINGREQUESTS,
                getString(R.string.contacts_new_friends_prompt_title),
                getString(R.string.contacts_new_friends_prompt_msg),
                intent);

        getPendingRequestsFromServerAsync();
	}

	@Override
	public void getChatMessage(ChatMessage msg) {

        /* ********
         * It does nothing, which will be dealt in IncomeMessageIntentReceiver#onReceive
         * ********
         */

//		Log.i("getChatMessage:(id:", msg.chatUserName + ",displayname:"
//                + msg.displayName + ",groupSenderID:" + msg.groupChatSenderID
//                + "):" + msg.messageContent);

//        SoundPoolManager.playSoundFromRaw(this,R.raw.new_msg_incoming);

//        boolean isHumanReadable = new ChatMessageHandler(this).handle(msg, true,
//                new ChatMessageHandler.CompleteListener() {
//                    @Override
//                    public void onDisplayNameAsynchrouslyFixed(ChatMessage m) {
//                        // 这个消息可能已经显示在会话界面了，而且显示的用户名称可能是"unknown"，所以
//                        // 需要刷新一下。
////                        if (_selectedTabIndex == TAB_SMS
////                                && SmsActivity.isInstanciated()
////                                && !MessageComposerActivity.isInstanciated()) {
////                            SmsActivity.instance().fRefetchTableData();
////                        }
//                    }
//                });

//		if (_selectedTabIndex == TAB_SMS
//				&& SmsActivity.isInstanciated()
//				&& !MessageComposerActivity.isInstanciated()) {
//		    if (isHumanReadable) {
//                SmsActivity.instance().addChatMessage(msg);
//	        } else {
//                SmsActivity.instance().fRefetchTableData();
//            }
//		}

		// if message log show and it is with that user ,reload that view too
//		if (MessageComposerActivity.isInstanciated()
//				&& MessageComposerActivity.instance().targetUID()
//						.equals(msg.chatUserName)) {
////			Log.i("getChatMessage",
////                    "MessageComposerActivity exist for the same user,refresh it too!");
////			MessageComposerActivity.instance().fProcessNewIncomeMsg(msg);
//		} else {
////		    if (isHumanReadable){
////		        GlobalValue.unreadMsgCount++;
////		        fRefreshTabBadge_unreadMsg();
////		    }
//		}
	}

    /**
	 * safe to be called from worker thread.
	 */
    private long loadingId=0;
    private void fRefreshTabBadge_unreadMsg() {
//        int unreadMsgCount = new Database(StartActivity.this).open().countAllUnreadChatMessages();
        final long curLoadingId=++loadingId;
        new AsyncTask<Void,Void,Integer>() {
            @Override
            protected Integer doInBackground(Void... contexts) {
                int unreadMsgCount=0;
                if(curLoadingId == loadingId) {
//                    unreadMsgCount = new Database(StartActivity.this).countAllUnreadChatMessages();
                    unreadMsgCount = new Database(StartActivity.this).countUnreadMessagesByTargets(SmsActivity.getCurrentChatTargets());
                }
                return unreadMsgCount;
            }

            @Override
            protected void onPostExecute(Integer unreadCount) {
                if(curLoadingId == loadingId) {
                    if (0 == unreadCount) {
                        txt_unreadMsg.setText("");
                        txt_unreadMsg.setVisibility(View.GONE);
                    } else {
                        txt_unreadMsg.setText(unreadCount <= MAX_COUNTS_ON_TABS ? String.valueOf(unreadCount) : MAX_COUNTS_ON_TABS + "+");
                        txt_unreadMsg.setVisibility(View.VISIBLE);
                    }
                }
            }
        }.execute((Void)null);
//        int unreadMsgCount=0;
//        ArrayList<ChatMessage> lst = mDb.fetchAllChatMessages(false);
//        if(lst != null) {
//            for(ChatMessage msg : lst) {
////                Log.w("primaryKey: "+msg.primaryKey+",chatUserName: "+msg.chatUserName+",displayName: "+msg.displayName+
////                ",msgType: "+msg.msgType+",messageContent: "+msg.messageContent+",ioType: "+msg.ioType+",isGroupChatMessage: "+msg.isGroupChatMessage+
////                ",groupChatSenderID: "+msg.groupChatSenderID);
//                if (ChatMessage.IOTYPE_INPUT_UNREAD.equals(msg.ioType) && msg.isBelongsToBuddyOrGroup(this)) {
//                    ++unreadMsgCount;
//                }
//            }
//        }
//        if (0 == unreadMsgCount) {
//            txt_unreadMsg.setText("");
//            txt_unreadMsg.setVisibility(View.GONE);
//        } else {
//            txt_unreadMsg.setText(unreadMsgCount <= MAX_COUNTS_ON_TABS ? String.valueOf(unreadMsgCount) : MAX_COUNTS_ON_TABS + "+");
//            txt_unreadMsg.setVisibility(View.VISIBLE);
//        }

	}

    /**
     * safe to be called from worker thread.
     */
    private void fRefreshTabBadge_social() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Moment dummy = new Moment(null);
                int newReviewsCount = mDb.fetchNewReviews(dummy);
                if (0 == newReviewsCount) {
                    txt_friends_news.setText("");
                    txt_friends_news.setVisibility(View.GONE);
                } else {
                    txt_friends_news.setText(newReviewsCount <= MAX_COUNTS_ON_TABS ? String.valueOf(newReviewsCount) : MAX_COUNTS_ON_TABS + "+");
                    txt_friends_news.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void fRefreshTabBadge_contact() {
        ArrayList<PendingRequest> pendingRequests=new ArrayList<PendingRequest>();
        ArrayList<PendingRequest> pendings = new ArrayList<PendingRequest>();
        mDb.fetchPendingRequest(pendingRequests);
        for (PendingRequest p : pendingRequests) {
            if (p.type != PendingRequest.GROUP_OUT) {
                pendings.add(p);
            }
        }
        Database.open(StartActivity.this).fetchPendingRequest(pendingRequests);
        boolean pendingRequestExist=false;
        if(pendings.size() > 0) {
            for(PendingRequest p : pendingRequests) {
                if (p.type == PendingRequest.BUDDY_IN
                        || p.type == PendingRequest.GROUP_IN
                        || p.type == PendingRequest.GROUP_ADMIN) {
                    pendingRequestExist=true;
                    break;
                }
            }
        }

        if (!pendingRequestExist) {
            txt_pendingin_requests.setVisibility(View.GONE);
        } else {
        	txt_pendingin_requests.setText(pendings.size()+"");
            txt_pendingin_requests.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void getChatMessage_ReachedReceipt(int arg0, String arg1) {
        Log.i("getChatMessage_ReachedReceipt() fromUserName = [" + arg1 +"]");
//        if (MessageComposerActivity.isOnStackTop) {
//            if (MessageComposerActivityBase.isInstanciated()) {
//                MessageComposerActivityBase.instance().updateSentStatus(arg0, arg1);
//            }
//        }
	}

	@Override
	public void getChatMessage_ReadedReceipt(String uniqueKey, String fromUserName) {
        Log.i("getChatMessage_ReadedReceipt() fromUserName = [" + fromUserName +"]");
//        if (MessageComposerActivity.isOnStackTop) {
//            if (MessageComposerActivity.isOnStackTop) {
//                if (MessageComposerActivityBase.isInstanciated()) {
//                    MessageComposerActivityBase.instance().updateSentStatus(arg0, arg1);
//                }
//            }
//        }
	}

	@Override
	public void getMissedCallNotification(String arg0, String arg1, Date arg2) {
	}

    @Override
    public void sendChatMessageUpdate(String uniqueKey, WowTalkChatMessageState wowTalkChatMessageState) {
        // 在回调之前，SDK已经更新了数据库的发送状态，此处不再需要
        Log.e("msg " + uniqueKey + ", status " + wowTalkChatMessageState.toString()
                + "(" + wowTalkChatMessageState.toInt() + ")");
//        ChatMessage chatMessage=mDb.fetchChatMessageByUniqueKey(uniqueKey);
//        if(null != chatMessage) {
//            public final static WowTalkChatMessageState Idle = new WowTalkChatMessageState(0,"Idle");
//            public final static WowTalkChatMessageState InProgress = new WowTalkChatMessageState(1,"InProgress");
//            public final static WowTalkChatMessageState Delivered = new WowTalkChatMessageState(2,"Delivered");
//            public final static WowTalkChatMessageState NotDelivered = new WowTalkChatMessageState(3,"NotDelivered");
//            switch(wowTalkChatMessageState.toInt()) {
//                case 0:
//                    chatMessage.sentStatus = ChatMessage.SENTSTATUS_FILE_UPLOADINIT;
//                    break;
//                case 1:
//                    chatMessage.sentStatus = ChatMessage.SENTSTATUS_IN_PROCESS;
//                    break;
//                case 2:
//                    chatMessage.sentStatus = ChatMessage.SENTSTATUS_SENT;
//                    break;
//                case 3:
//                    chatMessage.sentStatus = ChatMessage.SENTSTATUS_NOTSENT;
//                    break;
//                default:
//                    chatMessage.sentStatus = ChatMessage.SENTSTATUS_IN_PROCESS;
//                    break;
//            }
//
//            mDb.updateChatMessage(chatMessage, true);
//        }
    }

    /**
     *
     * @param id
     * @param title
     * @param msg
     * @param intent seems can not be null on Android 2.3, otherwise get:
     *               java.lang.IllegalArgumentException: contentIntent required
     */
    private void showNotification(int id, String title, String msg, PendingIntent intent) {
        NotificationManager notiManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        Notification note = new Notification(R.drawable.icon, msg,
                System.currentTimeMillis());
        note.setLatestEventInfo(this, title, msg, intent);
        notiManager.notify(id, note);
    }

    private void dismissNotification(int id) {
        NotificationManager notiManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.cancel(id);
    }

    @Override
    public void onTabChanged(String tabId) {
        if(Utils.isNetworkConnected(this)) {
            WebServerEventPoller.instance(this).invoke();
        }
    }

    /**
     * check whether the application needs to update.
     */
    private void checkAppUpdate() {
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
                if (ErrorCode.OK == errno) {
                    try {
                        int currVerCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        Log.i("checkAppUpdate when start app, the currVercode is " + currVerCode + ", the remoteVersion is " + updatesInfo.versionCode);
                        // There is new version of the app.
                        if (currVerCode < updatesInfo.versionCode) {
                            changeNewUpdateFlagView(View.VISIBLE);
                        } else {
                            changeNewUpdateFlagView(View.GONE);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute((Void) null);
    }

    /**
     * 1.get server info:必须在所有的网络操作之前调用。<br>
     * 2.start wowtalkservice, checkAppUpdate, getSecurityLevel, report info , and so on.
     */
    private void setupApplication() {
        // 从login进入，才需要从服务器下载信息
        if (mIsStartFromLogin) {
//            mMsgbBox.showWait();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                // 下面调用的所有的网络操作，除第一个(后续网络操作的前提)和最后一个（后面没有操作了，且此处调用的地方已经是子线程）外，
                // 其他的都重启子线程完成，因为它们之间没有先后顺序
                int[] result = mWeb.getServerInfo();

                AppStatusService.getOfflineMessages(StartActivity.this);

                // result[1] == 1，标识sip_domain变化了，需要重启wowtalkservcie;
                // 否则直接启动
                boolean isNeedRestart = (result[0] == ErrorCode.OK && result[1] == 1);
                setupWowtalkService(isNeedRestart);

                if (mIsStartFromLogin) {
                    getDatasFromServer();
                } else {
                    needDownloadGroupsAndMembers();
                }

                checkLocalLanguage();

                // check update of version
                checkAppUpdate();

                getSecurityLevelFromServer();

                // report push sercie token to server
                mWeb.fReportInfoWithPushToken();

            }
        }).start();
    }

    private void getDatasFromServer() {
        // 在子线程获取，子线程中使用顺序形式
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 先清除最近联系人，在获取部门／联系人之前
                // 登录成功后，清除本地的最近联系人列表(可能出现的情况：用户在另一个手机上使用一段时间后，又使用回原来的手机)
                new Database(StartActivity.this).clearLatestChatTargets();
                PrefUtil.getInstance(StartActivity.this).setHasMoreLatestChatTargetInServer(true);
                downloadContactsAndGroups(StartActivity.this, mHandler);
                getLatestChatTargets(StartActivity.this, mHandler);
                getAlbumCoverFromServer();
                mHandler.sendEmptyMessage(HANDLER_FINISHED);
            }
        }).start();
    }

    /**
     * 获取新帐户的动态封面(网络请求操作)
     */
    private void getAlbumCoverFromServer() {
//        mHandler.sendEmptyMessage(HANDLER_GET_ALBUMCOVER);
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlbumCover ac = new AlbumCover();
                String uid = mPrefUtil.getUid();
                if (ErrorCode.OK == WowTalkWebServerIF.getInstance(StartActivity.this)
                        .fGetAlbumCover(uid, ac)) {
                    String myUid = mPrefUtil.getUid();
                    ArrayList<Account> accounts = mPrefUtil.getAccountList();
                    Account tempAccount = null;
                    for (Iterator<Account> iterator = accounts.iterator(); iterator.hasNext();) {
                        tempAccount = iterator.next();
                        if (myUid.equals(tempAccount.uid)) {
                            tempAccount.albumCoverFileId = null == ac.fileId ? "" : ac.fileId;
                            tempAccount.albumCoverExt = null == ac.ext ? "" : ac.ext;
                            mPrefUtil.setAccountList(accounts);
                            break;
                        }
                    }
                    Database database = new Database(StartActivity.this);
                    database.storeAlbumCover(uid, ac);
                }
            }
        }).start();
    }
    /**
     * biz刷新所有群组及成员.
     *
     * <p>biz刷新群组及成员机制：
     * <p>1.全自动刷新：
     *      每次从登录界面进入，刷新群组和成员；
     * <p>2.半自动刷新：
     *      后台切到前台，第一次点击显示群组结构，刷新群组；
     *      第一次点击某个群组，刷新该组成员；
     * <p>3.手动刷新：
     *      联系人列表界面，右上角的刷新按钮，刷新当前群组成员
     */
    public static void getMembersAndChatTargetsFromServer(final Context context) {
        // 先清除最近联系人，在获取部门／联系人之前
        // 登录成功后，清除本地的最近联系人列表(可能出现的情况：用户在另一个手机上使用一段时间后，又使用回原来的手机)
        new Database(context).clearLatestChatTargets();
        PrefUtil.getInstance(context).setHasMoreLatestChatTargetInServer(true);

        // 此处属于第1种刷新机制
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadContactsAndGroups(context, null);
                getLatestChatTargets(context, null);
            }
        }).start();
    }

    public static void downloadContactsAndGroups (Context context, Handler handler) {
        synchronized (sSyncObj) {
            if (sIsDownloading) {
                Log.w("LoginActivity#downloadContactsAndGroups is running!");
                return;
            }
            sIsDownloading = true;
        }

        WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(context);
        Database dbHelper = new Database(context);
        PrefUtil prefUtil = PrefUtil.getInstance(context);
        prefUtil.setGroupMembersUptodatePerfectly(false);

        String companyId = prefUtil.getCompanyId();
        // 1. 组织架构（部门id，成员id的关系）
        Log.i("downloading(1/4) company structure");
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_COMPANY_STRUCTURE);
        }
        int errno = webIF.getCompanyStructure(companyId);
        Log.i("StartActivity#downloadContactsAndGroups, Finish downloading(1/4) company structure (errno:" + errno);

        // 公司用来发通知的buddy信息,其wowtalk_id格式为companyId_companyId
        // 此过程不需要显示在界面上（属于组织架构部分）
        errno = webIF.fGetBuddyByWowtalkId(companyId + "_" + companyId, new Buddy());

        // 2. 部门
        Log.i("downloading(2/4) all depts for biz");
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_DEPTS);
        }
        errno = webIF.getGroupsByCompanyId(prefUtil.getCompanyId());
        Log.i("StartActivity#downloadContactsAndGroups, finish downloading(2/4) all groups for biz (errno:" + errno);
        if (ErrorCode.OK == errno) {
            prefUtil.setLocalGroupListLastModified();
            prefUtil.setGroupUptodate(true);
        }

        // 3. 临时会话群组
//        Log.i("downloading(3/4) temp groups for biz");
//        if (null != handler) {
//            handler.sendEmptyMessage(HANDLER_GET_TEMP_GROUPS);
//        }
//        errno = webIF.fGroupChat_GetMyTempGroups();
//        Log.i("StartActivity#downloadContactsAndGroups, finish downloading(3/4) temp groups for biz (errno:" + errno);

        // 3. 收藏的群组和常用联系人
        Log.i("downloading(3/4) favorite contacts and groups for biz");
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_FAVORITE_GROUPS_AND_CONTACTS);
        }
        errno = webIF.getFavoriteContactsAndGroups();
        Log.i("StartActivity#downloadContactsAndGroups, finish downloading(3/4) favorite contacts and groups for biz (errno:" + errno);
        if (ErrorCode.OK == errno) {
            prefUtil.setFavoritesUptodate(true);
        }

        // 4. 我的联系人
        Log.i("downloading(4/4) favorite contacts and groups for biz");
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_CONTACTS);
        }
        errno = webIF.fGetBuddyList();
        Log.i("StartActivity#fGetBuddyList, finish downloading(4/4) (errno:" + errno);
        if (ErrorCode.OK == errno) {
            prefUtil.setFavoritesUptodate(true);
        }

        prefUtil.setGroupMembersUptodatePerfectly(true);
        // 用于供其他界面，监听群组成员是否加载完成
        dbHelper.finishLoadGroupMembersPerfectly();

        sIsDownloading = false;
    }

    /**
     * 登录／切换帐号成功后，处理最近联系人的相关信息
     * @param context
     */
    public static void dealLatestChatTargets(Context context) {
        // 登录成功后，清除本地的最近联系人列表(可能出现的情况：用户在另一个手机上使用一段时间后，又使用回原来的手机)
        new Database(context).clearLatestChatTargets();
        PrefUtil.getInstance(context).setHasMoreLatestChatTargetInServer(true);
        getLatestChatTargets(context, null);
    }

    /**
     * 获取最近的聊天联系人列表
     * @param handler
     */
    private static void getLatestChatTargets(final Context context, final Handler handler) {
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_LATEST_CHAT_TARGETS);
        }
        WowTalkWebServerIF.getInstance(context).getLatestChatTargets(
                0, SmsActivity.LIMIT_COUNT_PER_PAGE, true);
    }

    /**
     * 判断如果本地数据库没有部门资料，则需要从服务器下载。
     * <br>为了兼容老版本
     */
    private void needDownloadGroupsAndMembers() {
        int counts = mDb.getCountsOfGroups();
        if (counts == 0) {
            co.onemeter.oneapp.ui.Log.i("StartActivity, db is empty, start downloading!");
            getDatasFromServer();
        }
    }

    private void checkLocalLanguage() {
        // 记录语言
        String language = getResources().getConfiguration().locale.getLanguage();
        String originalLanguage = mPrefUtil.getLocaleLanguage();
        Log.d("system language is " + language + ", the original language is " + originalLanguage);
        if (!originalLanguage.equals(language)) {
            // 只负责发通知，更新SP中的语言在通知中处理
            sendBroadcast(new Intent(LocaleChangedReceiver.ACTION_LOCALE_CHANGED_MINE));
        }
    }

    /**
     * get the security level
     */
    private void getSecurityLevelFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String securityLevelString = mWeb.fGetSecurityLevel(mPrefUtil.getCompanyId());
                mSecurityLevel = Constants.SECURITY_LEVEL_NORMAL;
                try {
                    mSecurityLevel = Integer.parseInt(securityLevelString);
                } catch (Exception exception) {
                }
                Log.i("StartActivity#onCreate, the security level is " + mSecurityLevel);
                mPrefUtil.setSecurityLevel(mSecurityLevel);
                Message message = mHandler.obtainMessage();
                message.what = HANDLER_GET_SECURITY_LEVEL;
                mHandler.sendMessage(message);
            }
        }).start();
    }

    /**
     * change the new update flag.
     * @param visibility the visibility value
     */
    public void changeNewUpdateFlagView(int visibility) {
        mNewUpdateView.setVisibility(visibility);
    }

    /**
     * Judge whether there is new version to update.
     * @return
     */
    public boolean isNewUpdate() {
        return mNewUpdateView.getVisibility() == View.VISIBLE;
    }

    /**
     * Get the pendingRequests from server and refresh the UI in async.
     */
    private void getPendingRequestsFromServerAsync() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(StartActivity.this).fGetPendingRequests();
            }
        }.execute((Void)null);
    }

    @Override
    public void getBuddyInfoChangedNotification(String arg0, String arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getGroupInfoChangedNotification(String arg0, String arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getLoginPlaceChangedNotification(String arg0, String arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sentChatMessage(ChatMessage arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getMomentRelateNotificaiton(String arg0, String arg1) {
        // TODO Auto-generated method stub
    }
}
