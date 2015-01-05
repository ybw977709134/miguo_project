package co.onemeter.oneapp.ui;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import co.onemeter.oneapp.Constants;
import org.wowtalk.Log;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.MomentWebServerIF;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.GlobalValue;

import java.util.List;

public class AppStatusService extends Service {

    public class AppStatusBinder extends Binder {
        public AppStatusService getService() {
            return AppStatusService.this;
        }
    }

    private static final Object sSyncObj = new Object();
    private static boolean sIsGettingOfflineMsg;
    private static boolean mIsMonitoring = true;
    private AppStatusBinder mBinder = new AppStatusBinder();
    private ActivityManager mActivityManager;
    private String mPackageName;
    private PrefUtil mPrefUtil;
    private MomentWebServerIF mMomentIF;
    private WebServerIF mWebIF;

    private boolean mIsStopThread;
    private boolean mIsPreviousOnForeground;
    private boolean mIsCurrentOnForeground;
    private boolean mIsFirstChangeToForeground = true;

    public static synchronized void setIsMonitoring(boolean isMonitoring) {
        mIsMonitoring = isMonitoring;
        Log.i("AppStatusService, setIsMonitoring " + isMonitoring);
    }

    public void startAppStatusListener() {
        Log.i("AppStatusService#startAppStatusListener");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mIsStopThread) {
                    synchronized (AppStatusService.class) {
                        if (mIsMonitoring) {
                            mIsCurrentOnForeground = isAppOnForeground();
                            Log.d("AppStatusService, the app is in "
                                    + (mIsCurrentOnForeground ? "foreground"
                                            : "background"));
                            if (!mIsCurrentOnForeground
                                    && mIsPreviousOnForeground) {
                                // 切换到后台，清除所有标记，再次回到前台时，需要刷新群组和成员(半自动)
                                GlobalValue.setNeedToRefreshAllGroups(true);
                                GlobalValue.clearNeedToRefreshMembersGroups();

                                changeFromForegroundToBack();
                            } else if (mIsCurrentOnForeground
                                    && !mIsPreviousOnForeground) {
                                changeFromBackgroundToFore();
                            }
                            // 前后台状态
                            mIsPreviousOnForeground = mIsCurrentOnForeground;
                        }
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 根据security_level决定是否删除 聊天记录，群组/联系人
     */
    private void changeFromForegroundToBack() {
        Log.i("AppStatusService, the app is changed from foreground to background.");
        ManageAccountsActivity.deleteDatasInDB(this);
    }

    /**
     * 根据security_level决定是否后台下载群组/联系人/timeline
     */
    private void changeFromBackgroundToFore() {
        // 第一次从LoginActivity启动，则不需要此处下载成员；否则(不是第一次，或直接从StartActivity启动)，根据安全级别确定是否需要下载
        if (GlobalValue.IS_BOOT_FROM_LOGIN && mIsFirstChangeToForeground) {
            mIsFirstChangeToForeground = false;
            return;
        }

        mIsFirstChangeToForeground = false;

        Log.i("AppStatusService, the app is changed from background to foreground.");
        if (Constants.SECURITY_LEVEL_HIGH == mPrefUtil.getSecurityLevel()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    LoginActivity.downloadContactsAndGroups(AppStatusService.this);
                    mWebIF.getLatestChatTargets(0, SmsActivity.LIMIT_COUNT_PER_PAGE, true);
                    // timeline
                    mMomentIF.fGetMomentsOfAll(0, TimelineFragment.PAGE_SIZE, true);
                }
            }).start();
        }

        // 重启wowtalkservice之前通过web API接口获取离线消息
        getOfflineMessages(AppStatusService.this);

        if (!IncallActivity.isOnStackTop) {
            // 同一帐号在另一处登录，暂时未能获得回调，暂时使用如下重启sip服务的方法迫使重新注册
            WowTalkVoipIF.getInstance(AppStatusService.this).fStopWowTalkService();
            WowTalkVoipIF.getInstance(AppStatusService.this).fStartWowTalkService();
        }
    }

    /**
     * 获取离线消息（初次启动，后台切前台，网络切换等），需要在重启wowtalk service之前
     * @param context
     */
    public static void getOfflineMessages(final Context context) {
        synchronized (sSyncObj) {
            if (sIsGettingOfflineMsg) {
                Log.w("AppStatusService#getOfflineMessages is running!");
                return;
            }
            sIsGettingOfflineMsg = true;
        }

        Log.i("AppStatusService#getOfflineMessages, begin...");
        // 方案一是较好的方案，但是由于sip服务器的推送问题，不能确保再收到新消息前，调用离线消息接口
        // 为确保不丢消息，暂时使用方案二实现
        // ****方案一：
        // 获取离线消息，如果上次离线消息获取成功且LATEST_MSG_TIMESTAMP有值，
        // 则使用LATEST_MSG_TIMESTAMP作为入参；否则，使用OFFLINE_MSG_TIMESTAMP
        /**
        boolean isOfflineGotSuccesss = PrefUtil.getInstance(context).isOfflineMsgGotSuccess();
        long latestTimeStamp = PrefUtil.getInstance(context).getLatestMsgTimestamp();
        long tempTimeStamp = -1;
        if (isOfflineGotSuccesss && -1 != latestTimeStamp) {
            tempTimeStamp = latestTimeStamp;
        } else {
            tempTimeStamp = PrefUtil.getInstance(context).getOfflineMsgTimestamp();
        }
        */
        // end****方案一********
        // *****方案二：
        // 使用上次获取的离线消息中的最大值作为离线消息接口的入参
        // 如果未调用过离线消息接口，则使用-1，在接口返回时保存本地时间作为时间戳）
        long tempTimeStamp = PrefUtil.getInstance(context).getOfflineMsgTimestamp();
        // end****方案二********

        final long timeStamp = tempTimeStamp;
        new Thread(new Runnable() {
            @Override
            public void run() {
                WebServerIF.getInstance(context).getOfflineMessages(timeStamp);
                sIsGettingOfflineMsg = false;
            }
        }).start();
    }

    /**
     * is app running in foreground.
     * @return
     */
    private boolean isAppOnForeground() {
        List<RunningAppProcessInfo> apps = mActivityManager.getRunningAppProcesses();
        if (null == apps) {
            return false;
        }

        for (RunningAppProcessInfo appInfo : apps) {
            if (appInfo.processName.equals(mPackageName)) {
                return appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("AppStatusService#onBind");
        mPrefUtil = PrefUtil.getInstance(this);
        mWebIF = WebServerIF.getInstance(this);
        mMomentIF = MomentWebServerIF.getInstance(this);
        mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mPackageName = this.getPackageName();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("AppStatusService#onUnbind");
        mIsStopThread = true;
        return super.onUnbind(intent);
    }
}
