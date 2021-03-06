package co.onemeter.oneapp.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.SchoolInvitationActivity;
import co.onemeter.oneapp.ui.AppStatusService.AppStatusBinder;
import co.onemeter.oneapp.utils.WebServerEventPoller;
import co.onemeter.utils.AsyncTaskExecutor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.core.RegistrationState;
import org.wowtalk.core.WowTalkChatMessageState;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public class StartActivity extends TabActivity
implements OnClickListener, WowTalkUIChatMessageDelegate, WowTalkNotificationDelegate, OnTabChangeListener {

    /**
     * 消息
     */
	public static final int TAB_SMS = 0;
    /**
     * 小伙伴
     */
	public static final int TAB_CONTACTS = 1;
    /**
     * 我的课堂
     */
	public static final int TAB_MyClass = 2;
    /**
     * 首页
     */
	public static final int TAB_HOME = 3;
    /**
     * 发现
     */
	public static final int TAB_Discov = 4;

    public static final String KEY_IS_START_FROM_LOGIN = "is_start_from_login";

    private static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String NOTI_ACTION_REVIEW = "NOTIFICATION/MOMENT_REVIEW";
    private static final int REQ_PROCESS_SCHOOL_INVITATION = 123;
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
	private Intent mMyClassIntent;
	private Intent mHomeIntent;
	private Intent mDiscovIntent;
	
//	private View mTabbar;
	private View mTabSms;
	private View mTabContact;
	private View mTabFriend;
	private View mTabHome;
	private View mTabSetting;
	
	private ImageView imgTabSms;
	private ImageView imgTabContacts;
	private ImageView imgTabMyClass;
	private ImageView imgTabHome;
	private ImageView imgTabDiscov;
	private TextView mNewUpdateView;
	
	private TextView txtTabSms;
	private TextView txtTabContact;
	private TextView txtTabMyClass;
	private TextView txtTabHome;
	private TextView txtTabDiscov;

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
    private List<GroupChatRoom> schools;

	public static final StartActivity instance() {
		if (instance != null)
			return instance;
		return null;
	}

	public static final boolean isInstanciated() {
		return instance != null;
	}

    /**
     * 绑定Service
     */
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

    /**
     * 设置TabBar的图片，Intent...
     * @param tag
     * @param resLabel
     * @param resIcon
     * @param content
     * @return
     */
	private TabHost.TabSpec buildTabSpec(String tag, String resLabel, int resIcon, Intent content) {
		return this.mHost.newTabSpec(tag).setIndicator(resLabel, getResources().getDrawable(resIcon)).setContent(content);
	}
	
	private void initView() {
//		mTabbar =  findViewById(R.id.tab_bar);
		mTabSms =  findViewById(R.id.tab_sms);
		mTabContact =  findViewById(R.id.tab_contact);
		mTabFriend =  findViewById(R.id.tab_friend);
		mTabHome =  findViewById(R.id.tab_home);
		mTabSetting =  findViewById(R.id.tab_setting);
		
		imgTabSms = (ImageView) findViewById(R.id.tab_sms_image);
		imgTabContacts = (ImageView) findViewById(R.id.tab_contact_image);
		imgTabMyClass = (ImageView) findViewById(R.id.tab_friend_image);
		imgTabHome = (ImageView) findViewById(R.id.tab_home_image);
		imgTabDiscov = (ImageView) findViewById(R.id.tab_setting_image);

        txtTabHome = (TextView) findViewById(R.id.tab_home_text);
		txtTabSms = (TextView) findViewById(R.id.tab_sms_text);
		txtTabContact = (TextView) findViewById(R.id.tab_contact_text);
		txtTabMyClass = (TextView) findViewById(R.id.tab_friend_text);
		//txtTabHome = (TextView) findViewById(R.id.tab_home_text);
		txtTabDiscov = (TextView) findViewById(R.id.tab_setting_text);
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
		mTabHome.setOnClickListener(this);
		mTabSetting.setOnClickListener(this);
	}

    /**
     * 设置默认图片
     */
	private void setDefaultImageView() {
		imgTabSms.setImageResource(R.drawable.tabbar_messages);
		imgTabContacts.setImageResource(R.drawable.tabbar_contacts);
		imgTabMyClass.setImageResource(R.drawable.tabbar_myclass);
		imgTabHome.setImageResource(R.drawable.tabbar_home);
		imgTabDiscov.setImageResource(R.drawable.tabbar_find);
		
		txtTabSms.setTextColor(getResources().getColor(R.color.gray));
		txtTabContact.setTextColor(getResources().getColor(R.color.gray));
		txtTabMyClass.setTextColor(getResources().getColor(R.color.gray));
		txtTabHome.setTextColor(getResources().getColor(R.color.gray));
		txtTabDiscov.setTextColor(getResources().getColor(R.color.gray));
	}

    /**
     * 设置当前位置TabBar
     * @param index
     */
	private void setTab(int index) {
		setDefaultImageView();
        _selectedTabIndex = index;

		switch (index) {
		case TAB_SMS:
			txtTabSms.setTextColor(getResources().getColor(R.color.blue));
			imgTabSms.setImageResource(R.drawable.tabbar_messages_press);
			break;
		case TAB_CONTACTS:
			txtTabContact.setTextColor(getResources().getColor(R.color.blue));
			imgTabContacts.setImageResource(R.drawable.tabbar_contacts_press);
			break;
		case TAB_MyClass:
			txtTabMyClass.setTextColor(getResources().getColor(R.color.blue));
			imgTabMyClass.setImageResource(R.drawable.tabbar_myclass_press);
			break;
		case TAB_HOME:
			txtTabHome.setTextColor(getResources().getColor(R.color.blue));
			imgTabHome.setImageResource(R.drawable.tabbar_home_press);
			break;
		case TAB_Discov:
			txtTabDiscov.setTextColor(getResources().getColor(R.color.blue));
			imgTabDiscov.setImageResource(R.drawable.tabbar_find_press);
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

	private void setupTabs() {
		mHost = getTabHost();
		mHost.setOnTabChangedListener(this);
			
		mSmsIntent = new Intent(StartActivity.this, SmsActivity.class);
        mContactIntent = new Intent(StartActivity.this, ContactsActivity.class);
		mMyClassIntent = new Intent(StartActivity.this, MyClassesActivity.class);
        mMyClassIntent.putExtra(MyClassesActivity.TAG, true);

        mHomeIntent = new Intent(StartActivity.this, HomeActivity.class);
		mDiscovIntent = new Intent(StartActivity.this, DiscoveryActivity.class);
		
		mHost.addTab(buildTabSpec(String.valueOf(TAB_SMS), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mSmsIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_CONTACTS), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mContactIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_MyClass), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mMyClassIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_HOME), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mHomeIntent));
		mHost.addTab(buildTabSpec(String.valueOf(TAB_Discov), getResources().getString(R.string.app_name), R.drawable.ic_action_search, mDiscovIntent));
		
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
			setTab(TAB_MyClass);
			break;
		case R.id.tab_home:
			setTab(TAB_HOME);
			break;
		case R.id.tab_setting:
			setTab(TAB_Discov);
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
        PinyinHelper.instance().init(this, R.raw.pinyin);

        mWeb = WowTalkWebServerIF.getInstance(this);
        mDb = new Database(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mMsgbBox = new MessageBox(this);
        IncomeMessageIntentReceiver.closeNoticeMessage();

		initView();
		setupTabs();

        if (null == savedInstanceState) {
            mIsStartFromLogin = getIntent().getBooleanExtra(KEY_IS_START_FROM_LOGIN, false);
        } else {
            mIsStartFromLogin = savedInstanceState.getBoolean(KEY_IS_START_FROM_LOGIN, false);
        }

        Connect2.setContext(this);
        setupApplication();

        /**
         * Connect2连接设置网络状态监听，未连接Toast提示
         */
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

        //刷新校园组织架构
        refresh();
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

    /**
     * 切换帐号observer
     */
    private IDBTableChangeListener mSwitchAccountObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            // 其他重启wowtalk_service在 ManageAccountsActivity中处理了
            fRefreshTabBadge_unreadMsg();
            fRefreshTabBadge_social();
            getSecurityLevelFromServer();
        }
    };

    /**
     * 消息observer
     */
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

    /**
     * 好友请求observer
     */
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

    /**
     * 好友圈回复observer
     */
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
		notifManager.cancel(GlobalValue.NOTIFICATION_FOR_CHATMESSAGE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        GlobalValue.screenW = displayMetrics.widthPixels;
        GlobalValue.screenH = displayMetrics.heightPixels;


        fRefreshTabBadge_contact();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_SWITCH_ACCOUNT, mSwitchAccountObserver);
        Database.addDBTableChangeListener(Database.TBL_LATEST_CHAT_TARGET, chatmessageObserver);
        Database.addDBTableChangeListener(Database.TBL_LATEST_CHAT_TARGET_UNREAD_COUNT, chatmessageObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
        Database.addDBTableChangeListener(Database.TBL_PENDING_REQUESTS,pendingRequestObserver);

        setTab(_selectedTabIndex);

        fRefreshTabBadge_unreadMsg();
        fRefreshTabBadge_social();
        fRefreshTabBadge_contact();

        WebServerEventPoller.instance(this).invoke();
        IncomeMessageIntentReceiver.closeNoticeMessage();
	}

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        tryHandleIncomeMsgNotification(intent);
    }
    private void refresh() {
    	schools = new Database(StartActivity.this).fetchSchools();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
            	schools.clear();
            	int errno = WowTalkWebServerIF.getInstance(StartActivity.this).getMySchoolsErrno(true, schools);
            	return errno;
            }
            @Override
            public void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    new Database(StartActivity.this).storeSchools(schools);
                }
            }
        });
    }


    
    private boolean isEmpty(){
		return schools == null ||schools.isEmpty();
	}

    /**
     * 处理推送过来的消息Notification
     * @param intent
     */
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

        Database.removeDBTableChangeListener(mSwitchAccountObserver);
        Database.removeDBTableChangeListener(chatmessageObserver);
        Database.removeDBTableChangeListener(momentReviewObserver);
        Database.removeDBTableChangeListener(pendingRequestObserver);
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


    /**
     * 双击back键退出
     * @param event
     * @return
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() ==   KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            //as original friend_activity is replaced with TimelineActivity
            //TimelineActivity is sub activity in tabActivity,it can not receive onBackPressed mask by this function
            //so check it manually
            if(TAB_MyClass == _selectedTabIndex && null != TimelineActivity.instance() && TimelineActivity.instance().handleBackPress()) {
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

    /**
     * 初始化WowtalkService，important
     * @param isRestart
     */
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

    /**
     * 回调方法检查Client端与Sip服务器的连接状态
     * @param registrationState
     * @param stateName
     */
    @Override
    public void registrationStateUpdate(RegistrationState registrationState, String stateName) {
        final String state = registrationState.toString();
        Log.i("SIP auth status " + state);
        if (state.equals(RegistrationState.RegistrationInProgress.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.registration_logging));
        } else if (state.equals(RegistrationState.RegistrationIdle.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.registration_idle_state));
        } else if (state.equals(RegistrationState.RegistrationSuccess.toString())) {
            SmsActivity.setNavTitle(getResources().getString(R.string.tab_sms));
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
        if(!MessageComposerActivity.activityIsNull()){
        	MessageComposerActivity.instance().finish();
        }
        
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
        startActivity(new Intent(this, StartActivity.class));
	}

	
	
	@Override
	public void getChatMessage(ChatMessage msg) {
        // nothing to do, new message is processed in IncomeMessageIntentReceiver#onReceive
	}

    /**
	 * safe to be called from worker thread.
	 */
    private long loadingId=0;
    private void fRefreshTabBadge_unreadMsg() {
//        int unreadMsgCount = new Database(StartActivity.this).open().countAllUnreadChatMessages();
        final long curLoadingId=++loadingId;
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... contexts) {
                int unreadMsgCount = 0;
                if (curLoadingId == loadingId) {
//                    unreadMsgCount = new Database(StartActivity.this).countAllUnreadChatMessages();
                    unreadMsgCount = new Database(StartActivity.this).countUnreadMessagesByTargets(SmsActivity.getCurrentChatTargets());
                }
                return unreadMsgCount;
            }

            @Override
            protected void onPostExecute(Integer unreadCount) {
                if (curLoadingId == loadingId) {
                    if (0 == unreadCount) {
                        txt_unreadMsg.setText("");
                        txt_unreadMsg.setVisibility(View.GONE);
                    } else {
                        txt_unreadMsg.setText(unreadCount <= MAX_COUNTS_ON_TABS ? String.valueOf(unreadCount) : MAX_COUNTS_ON_TABS + "+");
                        txt_unreadMsg.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
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
                    //txt_friends_news.setVisibility(View.GONE);
                } else {
                    txt_friends_news.setText(newReviewsCount <= MAX_COUNTS_ON_TABS ? String.valueOf(newReviewsCount) : MAX_COUNTS_ON_TABS + "+");
                    //txt_friends_news.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void fRefreshTabBadge_contact() {
        ArrayList<PendingRequest> pendingRequests=new ArrayList<PendingRequest>();
        ArrayList<PendingRequest> pendings = new ArrayList<PendingRequest>();
        mDb.fetchPendingRequest(pendingRequests);
        for (PendingRequest p : pendingRequests) {
            if (p.type != PendingRequest.GROUP_OUT && p.type != PendingRequest.BUDDY_OUT) {
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

                AppStatusService.getOfflineMessages(StartActivity.this);

                setupWowtalkService(true);

                if (mIsStartFromLogin) {
                    getDatasFromServer();
                } else {
                    needDownloadGroupsAndMembers();
                }

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

        int errno;

        // 收藏的群组和常用联系人
        Log.i("downloading(3/4) favorite contacts and groups for biz");
        if (null != handler) {
            handler.sendEmptyMessage(HANDLER_GET_FAVORITE_GROUPS_AND_CONTACTS);
        }
        errno = webIF.getFavoriteContactsAndGroups();
        Log.i("StartActivity#downloadContactsAndGroups, finish downloading(3/4) favorite contacts and groups for biz (errno:" + errno);
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
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(StartActivity.this).fGetPendingRequests();
            }
        });
    }
    

    @Override
    public void getBuddyInfoChangedNotification(String uid, String msgLiteralWithoutType) {
        Log.i("getBuddyInfoChangedNotification ", uid, " ", msgLiteralWithoutType);
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Integer, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                String uid = params[0];
                WowTalkWebServerIF.getInstance(StartActivity.this).fGetBuddyWithUID(uid);
                return null;
            }
        }, uid);
    }

    @Override
    public void getGroupInfoChangedNotification(String groupId, String msgLiteralWithoutType) {
        Log.i("getGroupInfoChangedNotification", groupId, " ", msgLiteralWithoutType);
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Integer, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                String uid = params[0];
                WowTalkWebServerIF.getInstance(StartActivity.this).fGroupChat_GetGroupDetail(uid);
                return null;
            }
        }, groupId);
    }

    @Override
    public void getLoginPlaceChangedNotification(String arg0, String arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void sentChatMessage(ChatMessage arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * Handle message with type {@link ChatMessage#MSGTYPE_MOMENT}.
     * @param uid
     * @param msgLiteralWithoutType
     */
    @Override
    public void getMomentRelateNotificaiton(String uid, String msgLiteralWithoutType) {
        Log.i("getMomentRelateNotificaiton", uid, " ", msgLiteralWithoutType);

        // find JSON str
        int i = msgLiteralWithoutType.lastIndexOf('{');
        if (i != -1) {
            String jsonStr = msgLiteralWithoutType.substring(i);
            JSONObject json = null;
            String action = null;
            try {
                json = new JSONObject(jsonStr);
                action = json.getString("action");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (NOTI_ACTION_REVIEW.equals(action)) {
                mPrefUtil.setLatestReviewTimestamp();

                try {
                    final String moment_id = json.getString("moment_id");
                    final String review_id = json.getString("review_id");
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            MomentWebServerIF.getInstance(StartActivity.this).fGetReviewById(moment_id, review_id, null);
                            return null;
                        }
                    });
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param uid
     * @param msg e.g. "s2015/08/19 14:13:26.178|{993606178}{6ed46c0b-3d40-4156-b0bf-7bc4a736fc44}..."
     */
    @Override
    public void getSchoolStructureChangedNotification(String uid, String msg) {
        Log.i("getSchoolStructureChangedNotification: uid ", uid, ", message ", msg);

        // extract msg body
        String body = null;
        int i = msg.indexOf('}');
        if (i != -1) {
            i = msg.indexOf('}', i + 1);
            if (i != -1 && i < msg.length()) {
                body = msg.substring(i + 1);
            }
        }

        // msg 可能是：
        // A, JSON = { event:, school_id:, school_name: }
        // B, school_id
        // 其中 A 对应的事件取决于 JSON.event，目前只有 "invite" 一种情形；
        // B 对应的事件是“学校的组织架构已经发生变化”。
        if (body != null && body.startsWith("{")) { // A
            try {
                JsonObject j = (JsonObject) new JsonParser().parse(body);
                String event = j.has("event") ? j.get("event").getAsString() : null;

                if (TextUtils.equals(event, "invite")) {
                    String phone = j.has("phone") ? j.get("phone").getAsString() : null;
                    String schoolId = j.has("school_id") ? j.get("school_id").getAsString() : null;
                    String schoolName = j.has("school_name") ? j.get("school_name").getAsString() : null;

                    if (!TextUtils.isEmpty(phone)
                            && !TextUtils.isEmpty(schoolId)
                            && !TextUtils.isEmpty(schoolName)) {

                        String className = "";
                        if (j.has("classrooms")) {
                            JsonArray arr = j.get("classrooms").getAsJsonArray();
                            i = 0;
                            for (JsonElement c : arr) {
                                if (i > 0)
                                    className += ", ";
                                className += ((JsonObject)c).get("name").getAsString();
                                ++i;
                            }
                        }

                        String contentText = TextUtils.isEmpty(className) ?
                                getString(R.string.school_invitation_notification_content_text) :
                                getString(R.string.school_invitation_notification_content_text_with_class_name,
                                        className);

                        Intent intent = new Intent(this, SchoolInvitationActivity.class);

                        // use different request code each time
                        // http://stackoverflow.com/questions/27255106/get-notification-intent-extras
                        int reqCode = 0; // (int) (Calendar.getInstance().getTimeInMillis() & 0xFFFFFFFF);

                        Notification noti = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.icon)
                                .setTicker(getString(R.string.school_invitation_notification_ticker))
                                .setContentTitle(schoolName)
                                .setContentText(contentText)
                                .setContentIntent(PendingIntent.getActivity(
                                        this, reqCode, intent, 0))
                                .setAutoCancel(true)
                                .build();

                        NotificationManager mNotificationManager = (NotificationManager)
                                getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(100, noti);

                        PrefUtil.getInstance(this).getPreferences().edit()
                                .putString(PrefUtil.LATEST_SCHOOL_INVITATION, body)
                                .commit();
                    } else {
                        Log.e("invalid School Structure Changed Notification: " + body);
                    }
                } else {
                    Log.e("unknown event of School Structure Changed Notification: " + event);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // B
            refresh();
        }
    }
}
