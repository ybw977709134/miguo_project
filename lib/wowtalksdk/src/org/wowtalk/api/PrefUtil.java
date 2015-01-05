package org.wowtalk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.*;

/**
 * SharedPreference说明：
 * 
 * <p>涉及的SharedPreference文件(只有login,logout,switchAccount才会使用到后两个SP)：
 * <ul>
 * <li>"org.wowtech.yuanqu_preferences.xml"(以下简称SP_current)     当前帐号使用的所有缓存；
 * <li>"org.wowtech.yuanqu_root.xml"(以下简称SP_root)               所有帐号共用的缓存；
 * <li>"org.wowtech.yuanqu_uid.xml"(以下简称SP_uid)                 指定uid对应帐号使用的缓存；
 * </ul>
 * 
 * <p>SharedPreference的实现：
 * <ol>
 * <li> 用户登录后，所有字段都存在SP_current中；
 * <li> 退出的时候，从SP_current中拷贝帐号私有的信息到SP_uid中,拷贝公共的信息到SP_root中；
 *              清除SP_current中的缓存；
 * <li> 登录的时候，将SP_root以及SP_uid中的信息拷贝到SP_current中
 * </ol>
 */
public class PrefUtil {

    /***=======SP_root的key===============begin===================*/
    private static final String UID = "uid_preference";
    private static final String PREV_UID = "prev_uid_preference";
    private static final String ACCOUNT_LIST = "account_list";
    private static final String ACCOUNT_ID_LIST = "account_id_list";
    /***=======SP_root的key===============end=====================*/

    /***=======SP_uid的key================begin===================*/
    private static final String APP_VERSION_CODE = "app_version_code";

    public static final String MY_NICKNAME = "MY_NICKNAME";
    public static final String MY_STATUS = "MY_STATUS";
    public static final String MY_BIRTHDAY = "MY_BIRTHDAY";
    public static final String MY_SEX = "MY_SEX";
    public static final String MY_AREA = "MY_AREA";
    public static final String MY_PRONUNCIATION = "MY_PRONUNCIATION";
    public static final String MY_PHONE = "MY_PHONE";
    public static final String MY_MOBILE = "MY_MOBILE";
    public static final String MY_EMAIL = "MY_EMAIL";
    public static final String MY_JOB_TITLE = "MY_JOB_TITLE";
    public static final String MY_EMPLOYEE_ID = "MY_EMPLOYEE_ID";
    public static final String MY_ACCOUNT_TYPE = "MY_ACCOUNT_TYPE";
    public static final String MY_PHOTO_UPLOADED_TIMESTAMP = "MY_PHOTO_UPLOADED_TIMESTAMP";
    public static final String WOWTALK_ID = "MY_WOWTALK_ID";
    private static final String PASSWORD_HASHED_FOR_SIP = "password_preference"; // for sip
    private static final String PASSWORD_HASHED = "web_password_preference"; // for web
//  private static final String PREF_DOMAIN = "domain_preference";
    private static final String WOWTALKID_CHANGED = "wowtalkid_changed_pref";
    private static final String PASSWORD_CHANGED = "password_changed_pref";
    /**
     * Indicate login state.
     *
     * For WowTalk:
     * 1, value 1 means waiting for mobile phone number to be verified by access code,
     * 2, value 2 means it's ready to register with sip server;
     *
     * For WowCity or WowTalk biz:
     * value 2 means it's ready to register with sip server.
     */
    private static final String SETUP_STEP = "SETUP_STEP";
    private static final String APPLY_TIMES = "APPLY_TIMES";
    private static final String COUNTRYCODE = "countrycode_preference";
    private static final String USERNAME = "username_preference";
    private static final String DEMO_MODE_CODE = "DEMO_MODE_CODE";
    private static final String TOKEN_REPORTED_TO_SERVER = "TOKEN_REPORTED_TO_SERVER";
    private static final String SERVER_VERSION = "server_version";
    private static final String ANDROID_CLIENT_VERSION = "android_client_version";
    private static final String TIME_OFFSET = "time_offset";
    private static final String ACTIVE_APP_TYPE = "active_app_type";
    private static final String WEB_DOMAIN = "web_domain";
    private static final String SIP_DOMAIN = "domain_preference";
    private static final String USE_S3 = "use_s3";
    private static final String S3_UID = "s3_uid";
    private static final String S3_PASSWORD = "s3_password";
    private static final String S3_BUCKET = "s3_bucket";
    /**
     * 离线消息是否获取成功
     */
    private static final String OFFLINE_MSG_GOT_SUCCESS = "offline_msg_got_success";
    /**
     * 最后一条离线消息的时间戳
     */
    private static final String OFFLINE_MSG_TIMESTAMP = "offline_msg_timestamp";
    /**
     * 最新消息的时间戳，每次收到消息都会更新
     */
    private static final String LATEST_MSG_TIMESTAMP = "latest_msg_timestamp";
    /**
     * 服务器是否有更多的最近联系人
     */
    private static final String HAS_MORE_LATEST_CHAT_TARGET_IN_SERVER = "latest_chat_target_in_server";
    private static final String TAB_NUM = "tab_num";
    private static final String SECURITY_LEVEL = "security_level";
    private static final String SHOW_PLUG_IN_CONTACT = "show_plug_in_contact";
    private static final String CONTACT_NEARBY_RESULT_LAYOUT = "contact_nearby_result_layout";
    private static final String CONTACT_UP_TO_DATE = "contact_up_to_data";
    private static final String GROUP_UP_TO_DATE = "group_up_to_date";
    private static final String FAVORITES_UP_TO_DATE = "favorites_up_to_date";
    private static final String CONTACTS_UP_TO_DATE_AFTER_LOGIN = "buddies_up_to_date_after_login";
    private static final String GROUP_MEMBERS_UP_TO_DATE_PERFECTLY = "group_members_up_to_date_perfectly";
    private static final String CONTACTS_DISPLAY_GROUP_ID = "contacts_display_group_id";
    public static final String AUTO_LOGIN = "auto_login";
    private static final String LOCAL_GROUP_LAST_MODIFIED = "local_grp_last_modified";
    private static final String LOCAL_CONTACT_LAST_MODIFIED = "local_contact_last_modified";
    private static final String ACCEPT_MESSAGE = "accept_message";
    private static final String SHOW_PUBLIC_IN_CONTACTLIST = "show_public_in_contact";
    private static final String COMPANY_ID = "company_id";
    private static final String LATEST_REVIEW_TIMESTAMP = "latest_review_timestamp";
    private static final String ADD_BUDDY_AUTOMATICALLY = "add_buddy_automatically";
    private static final String PEOPLE_CAN_ADD_ME = "people_can_add_me";
    private static final String UNKNOWN_BUDDY_CAN_CALL_ME = "unknown_buddy_can_call_me";
    private static final String UNKNOWN_BUDDY_CAN_MESSAGE_ME = "unknown_buddy_can_message_me";
    private static final String PUSH_SHOW_DETAIL_FLAG = "push_show_detail_flag";
    private static final String LIST_ME_IN_NEARBY_RESULT = "list_me_in_nearby_result";
    private static final String IS_UPLOADING_MY_AVATAR = "is_uploading_my_avatar";
    private static final String LOCALE_LANGUAGE = "locale_language";
    private static final String PINYIN_DATA_RES_ID = "pinyin_data_res_id";
    private final static String CURRENT_LOCATION_LATITUDE = "amap_cur_location_latitude";
    private final static String CURRENT_LOCATION_LONGITUDE = "amap_cur_location_longitude";

    private static final String SYS_NOTICE_ENABLE = "sys_notice_enable";
    private static final String SYS_NOTICE_MUSIC = "sys_notice_music";
    private static final String SYS_NOTICE_VIBRATE = "sys_notice_vibrate";
    private static final String SYS_NOTICE_NEW_MESSAGE = "sys_notice_new_message";
    private static final String SYS_NOTICE_TELEPHONE = "sys_notice_telephone";
    private static final String SYS_NOTICE_ALL_MOMENT = "sys_notice_all_moment";
    private static final String SYS_NOTICE_FREQUENT_CONTACTS_MOMENT = "sys_notice_frequent_contacts_moment";
    private static final String SYS_NOTICE_MOMENT_TIMELINE = "sys_notice_moment_timeline";
    private static final String SYS_NOTICE_MOMENT_QA = "sys_notice_moment_qa";
    private static final String SYS_NOTICE_MOMENT_SHARE = "sys_notice_moment_share";
    private static final String SYS_NOTICE_MOMENT_VOTE = "sys_notice_moment_vote";
    /***=======SP_uid的key================end=====================*/

    private static final boolean SYS_NOTICE_ENABLE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_MUSIC_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_VIBRATE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_NEW_MESSAGE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_TELEPHONE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_ALL_MOMENT_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_FREQUENT_CONTACTS_MOMENT_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_MOMENT_TIMELINE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_MOMENT_QA_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_MOMENT_SHARE_DEF_VALUE=true;
    private static final boolean SYS_NOTICE_MOMENT_VOTE_DEF_VALUE=true;

    public static final double DEF_UNUSED_LOCATION = 0.0;
    public static final int LIST_LAYOUT = 1;
    public static final int GRID_LAYOUT = 2;

    /**
     * OFFLINE_MSG_GOT_SUCCESS, OFFLINE_MSG_TIMESTAMP, LATEST_MSG_TIMESTAMP 的类同步锁
     */
    private static Object sMsgTimestampLock = new Object();

    private static PrefUtil instance;
    private static Context sContext;

    private static SharedPreferences sPref;
    private static SharedPreferences sPrefRoot;

    public static final synchronized PrefUtil getInstance(Context context) {
        if (instance == null) {
            instance = new PrefUtil(context);
        }
        return instance;
    }

    private PrefUtil(Context context) {
        sContext = context;
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        sPrefRoot = sContext.getSharedPreferences(
                sContext.getPackageName() + "_root", Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences() {
        return sPref;
    }

    public boolean isLogined() {
        return !TextUtils.isEmpty(getUid()) && !TextUtils.isEmpty(getPassword());
    }

    /**
     * 判断是否升级了，用于上报客户端信息（如果升级了，则将新的app_version_code写入SP）
     * @return true, 升级了； false, 未升级
     * @see WebServerIF#fReportInfoWithPushToken()
     */
    public boolean isAppUpgraded() {
        // 当前版本app_version_code
        int appVerison = 0;
        try {
            PackageInfo pkgInfo = sContext.getPackageManager().getPackageInfo(sContext.getPackageName(), 0);
            appVerison = pkgInfo.versionCode;
        } catch (NameNotFoundException exception) {
            exception.printStackTrace();
        }
        // SP中保存的app_version_code
        int savedAppVer = sPref.getInt(APP_VERSION_CODE, -1);

        boolean hasAppUpgraded = false;
        if (savedAppVer == -1 || savedAppVer != appVerison) {
            Editor editor = sPref.edit();
            editor.putInt(APP_VERSION_CODE, appVerison);
            editor.commit();
            hasAppUpgraded = true;
        }
        return hasAppUpgraded;
    }

    /**
     * 在#isAppUpgraded之后调用，才能确保有值
     * @return
     */
    public int getAppVersion() {
        return sPref.getInt(APP_VERSION_CODE, -1);
    }

    public void setAccountIds(ArrayList<String> accountIds) {
        if (null == accountIds) {
            return;
        }
        JSONArray accountIdJson = new JSONArray();
        for (String accountId : accountIds) {
            accountIdJson.put(accountId);
        }
        Editor editor = sPrefRoot.edit();
        editor.putString(ACCOUNT_ID_LIST, accountIdJson.toString());
        editor.commit();
    }

    public ArrayList<String> getAccountIds() {
        String accountIdJson = sPrefRoot.getString(ACCOUNT_ID_LIST, null);
        ArrayList<String> accountIds = new ArrayList<String>();
        if (!TextUtils.isEmpty(accountIdJson)) {
            try {
                JSONArray json = new JSONArray(accountIdJson);
                for (int i = 0; i < json.length(); i++) {
                    accountIds.add(json.getString(i));
                }
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }
        return accountIds;
    }

    public void setAccountList(ArrayList<Account> accounts) {
        if (null == accounts) {
            return;
        }
        JSONArray accountJson = new JSONArray();
        for (Account account : accounts) {
            accountJson.put(account.toJsonObject().toString());
        }
        Editor editor = sPrefRoot.edit();
        editor.putString(ACCOUNT_LIST, accountJson.toString());
        editor.commit();
    }

    public ArrayList<Account> getAccountList() {
        String accountListJson = sPrefRoot.getString(ACCOUNT_LIST, "");
        ArrayList<Account> accounts = new ArrayList<Account>();
        if (!TextUtils.isEmpty(accountListJson)) {
            try {
                JSONArray json = new JSONArray(accountListJson);
                for (int i = 0; i < json.length(); i++) {
                    accounts.add(new Account(json.getString(i)));
                }
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }
        return accounts;
    }

    public void setPrevUid(String prevUid) {
        Editor editor = sPref.edit();
        editor.putString(PREV_UID, prevUid);
        editor.commit();
    }

    /**
     * 获得公共SP中的"prev_uid_preference"
     * @return
     */
    public String getPrevUid() {
        return sPref.getString(PREV_UID, "");
    }

    public void setUid(String currentUid) {
        Editor editor = sPref.edit();
        editor.putString(UID, currentUid);
        editor.commit();
    }

    /**
     * 获取当前uid对应的SP中的uid
     * @return
     */
    public String getUid() {
        return sPref.getString(UID, "");
    }

    public void setPassword(String passwordHashed) {
        Editor editor = sPref.edit();
        editor.putString(PASSWORD_HASHED, passwordHashed);
        editor.commit();
    }

    public String getPassword() {
        return sPref.getString(PASSWORD_HASHED, "");
    }

    public void setOfflineMsgGotSuccess(boolean isSuccess) {
        synchronized (sMsgTimestampLock) {
            Editor editor = sPref.edit();
            editor.putBoolean(OFFLINE_MSG_GOT_SUCCESS, isSuccess);
            editor.commit();
        }
    }

    public boolean isOfflineMsgGotSuccess() {
        return sPref.getBoolean(OFFLINE_MSG_GOT_SUCCESS, true);
    }

    public long getOfflineMsgTimestamp() {
        return sPref.getLong(OFFLINE_MSG_TIMESTAMP, -1);
    }

    public void setOfflineMsgTimestamp(long timestamp) {
        synchronized (sMsgTimestampLock) {
            Editor editor = sPref.edit();
            editor.putLong(OFFLINE_MSG_TIMESTAMP, timestamp);
            editor.commit();
        }
    }

    public long getLatestMsgTimestamp() {
        return sPref.getLong(LATEST_MSG_TIMESTAMP, -1);
    }

    public void setLatestMsgTimestamp(long timestamp) {
        synchronized (sMsgTimestampLock) {
            Editor editor = sPref.edit();
            editor.putLong(LATEST_MSG_TIMESTAMP, timestamp);
            editor.commit();
        }
    }

    public void setHasMoreLatestChatTargetInServer(boolean hasMore) {
        Editor editor = sPref.edit();
        editor.putBoolean(HAS_MORE_LATEST_CHAT_TARGET_IN_SERVER, hasMore);
        editor.commit();
    }

    public boolean hasMoreLatestChatTargetInServer () {
        return sPref.getBoolean(HAS_MORE_LATEST_CHAT_TARGET_IN_SERVER, true);
    }

    public boolean isSysNoticeAllMomentOn() {
        return sPref.getBoolean(SYS_NOTICE_ALL_MOMENT,SYS_NOTICE_ALL_MOMENT_DEF_VALUE);
    }

    public void setSysNoticeAllMomentOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_ALL_MOMENT, b);
        editor.commit();
    }

    public boolean isSysNoticeFrequentContactsMomentOn() {
        return sPref.getBoolean(SYS_NOTICE_FREQUENT_CONTACTS_MOMENT,SYS_NOTICE_FREQUENT_CONTACTS_MOMENT_DEF_VALUE);
    }

    public void setSysNoticeFrequentContactsMomentOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_FREQUENT_CONTACTS_MOMENT, b);
        editor.commit();
    }

    public boolean isSysNoticeMomentTimelineOn() {
        return sPref.getBoolean(SYS_NOTICE_MOMENT_TIMELINE,SYS_NOTICE_MOMENT_TIMELINE_DEF_VALUE);
    }

    public void setSysNoticeMomentTimelineOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_MOMENT_TIMELINE, b);
        editor.commit();
    }

    public boolean isSysNoticeMomentQAOn() {
        return sPref.getBoolean(SYS_NOTICE_MOMENT_QA,SYS_NOTICE_MOMENT_QA_DEF_VALUE);
    }

    public void setSysNoticeMomentQAOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_MOMENT_QA, b);
        editor.commit();
    }

    public boolean isSysNoticeMomentShareOn() {
        return sPref.getBoolean(SYS_NOTICE_MOMENT_SHARE,SYS_NOTICE_MOMENT_SHARE_DEF_VALUE);
    }

    public void setSysNoticeMomentShareOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_MOMENT_SHARE, b);
        editor.commit();
    }

    public boolean isSysNoticeMomentVoteOn() {
        return sPref.getBoolean(SYS_NOTICE_MOMENT_VOTE,SYS_NOTICE_MOMENT_VOTE_DEF_VALUE);
    }

    public void setSysNoticeMomentVoteOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_MOMENT_VOTE, b);
        editor.commit();
    }

    public boolean isSysNoticeEnabled() {
        return sPref.getBoolean(SYS_NOTICE_ENABLE,SYS_NOTICE_ENABLE_DEF_VALUE);
    }

    public void setSysNoticeEnabled(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_ENABLE, b);
        editor.commit();
    }

    public boolean isSysNoticeMusicOn() {
        return sPref.getBoolean(SYS_NOTICE_MUSIC,SYS_NOTICE_MUSIC_DEF_VALUE);
    }

    public void setSysNoticeMusicOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_MUSIC, b);
        editor.commit();
    }

    public boolean isSysNoticeVibrateOn() {
        return sPref.getBoolean(SYS_NOTICE_VIBRATE,SYS_NOTICE_VIBRATE_DEF_VALUE);
    }

    public void setSysNoticeVibrateOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_VIBRATE, b);
        editor.commit();
    }

    public boolean isSysNoticeNewMessageOn() {
        return sPref.getBoolean(SYS_NOTICE_NEW_MESSAGE,SYS_NOTICE_NEW_MESSAGE_DEF_VALUE);
    }

    public void setSysNoticeNewMessageOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_NEW_MESSAGE, b);
        editor.commit();
    }

    public boolean isSysNoticeTelephoneOn() {
        return sPref.getBoolean(SYS_NOTICE_TELEPHONE,SYS_NOTICE_TELEPHONE_DEF_VALUE);
    }

    public void setSysNoticeTelephoneOn(boolean b) {
        Editor editor = sPref.edit();
        editor.putBoolean(SYS_NOTICE_TELEPHONE, b);
        editor.commit();
    }

    public void resetSysNoticeStatus() {
        setSysNoticeEnabled(SYS_NOTICE_ENABLE_DEF_VALUE);
        setSysNoticeMusicOn(SYS_NOTICE_MUSIC_DEF_VALUE);
        setSysNoticeVibrateOn(SYS_NOTICE_VIBRATE_DEF_VALUE);
        setSysNoticeNewMessageOn(SYS_NOTICE_NEW_MESSAGE_DEF_VALUE);
        setSysNoticeTelephoneOn(SYS_NOTICE_TELEPHONE_DEF_VALUE);
        setSysNoticeAllMomentOn(SYS_NOTICE_ALL_MOMENT_DEF_VALUE);
        setSysNoticeFrequentContactsMomentOn(SYS_NOTICE_FREQUENT_CONTACTS_MOMENT_DEF_VALUE);
        setSysNoticeMomentTimelineOn(SYS_NOTICE_MOMENT_TIMELINE_DEF_VALUE);
        setSysNoticeMomentQAOn(SYS_NOTICE_MOMENT_QA_DEF_VALUE);
        setSysNoticeMomentShareOn(SYS_NOTICE_MOMENT_SHARE_DEF_VALUE);
        setSysNoticeMomentVoteOn(SYS_NOTICE_MOMENT_VOTE_DEF_VALUE);
    }

    /**
     * utc offset in seconds.
     * @param offset
     */
    public void setUTCOffset(int offset) {
        Editor editor = sPref.edit();
        editor.putInt(TIME_OFFSET, offset);
        editor.commit();
    }

    /**
     * utc offset in seconds.
     * @return
     */
    public int getUTCOffset() {
        return sPref.getInt(TIME_OFFSET, 0);
    }

    public void setLocaleLanguage(String locale) {
        Editor editor = sPref.edit();
        editor.putString(LOCALE_LANGUAGE, locale);
        editor.commit();
    }

    public String getLocaleLanguage() {
        return sPref.getString(LOCALE_LANGUAGE, "");
    }

	public int getDefaultTabNum() {
		return sPref.getInt(TAB_NUM, 0);
	}
	
	public void setDefaultTabNum(int num) {
		Editor editor = sPref.edit();
		editor.putInt(TAB_NUM, num);
		editor.commit();
	}
	
	public boolean isShowPlugInContact() {
		return sPref.getBoolean(SHOW_PLUG_IN_CONTACT, false);
	}
	
	public void setIsShowPlugInContact(boolean isShow) {
		Editor editor = sPref.edit();
		editor.putBoolean(SHOW_PLUG_IN_CONTACT, isShow);
		editor.commit();
	}
	
	public int getNearbyResultLayout() {
		return sPref.getInt(CONTACT_NEARBY_RESULT_LAYOUT, LIST_LAYOUT);
	}
	
	public void setNearbyResultLayout(int layout) {
		Editor editor = sPref.edit();
		editor.putInt(CONTACT_NEARBY_RESULT_LAYOUT, layout);
		editor.commit();
	}
	
	public boolean isContactUptodate() {
		return sPref.getBoolean(CONTACT_UP_TO_DATE, false);
	}
	
	public void setContactUptodate(boolean isContactUptodate) {
		Editor editor = sPref.edit();
		editor.putBoolean(CONTACT_UP_TO_DATE, isContactUptodate);
		editor.commit();
	}
	
	public boolean isGroupUptodate() {
		return sPref.getBoolean(GROUP_UP_TO_DATE, false);
	}
	
	public void setGroupUptodate(boolean isGroupUptodate) {
		Editor editor = sPref.edit();
		editor.putBoolean(GROUP_UP_TO_DATE, isGroupUptodate);
		editor.commit();
	}

    public boolean isFavoritesUptodate() {
        return sPref.getBoolean(FAVORITES_UP_TO_DATE, false);
    }

    public void setFavoritesUptodate(boolean isFavoritesUptodate) {
        Editor editor = sPref.edit();
        editor.putBoolean(FAVORITES_UP_TO_DATE, isFavoritesUptodate);
        editor.commit();
    }

    public boolean isGroupMembersUptodatePerfectly() {
        return sPref.getBoolean(GROUP_MEMBERS_UP_TO_DATE_PERFECTLY, true);
    }

    public void setGroupMembersUptodatePerfectly(boolean isGroupUptodate) {
        Editor editor = sPref.edit();
        editor.putBoolean(GROUP_MEMBERS_UP_TO_DATE_PERFECTLY, isGroupUptodate);
        editor.commit();
    }

    public boolean isContactsUptodateAfterLogin() {
        return sPref.getBoolean(CONTACTS_UP_TO_DATE_AFTER_LOGIN, false);
    }

    public void setContactsUptodateAfterLogin(boolean isContactsUptodate) {
        Editor editor = sPref.edit();
        editor.putBoolean(CONTACTS_UP_TO_DATE_AFTER_LOGIN, isContactsUptodate);
        editor.commit();
    }

    /**
     * biz联系人界面titleBar上显示的群组名对应的group_id
     * @return group_id
     */
    public String getDisplayGroupId() {
        return sPref.getString(CONTACTS_DISPLAY_GROUP_ID, "");
    }

    public void setDisplayGroupId(String groupId) {
        Editor editor = sPref.edit();
        editor.putString(CONTACTS_DISPLAY_GROUP_ID, groupId);
        editor.commit();
    }

	public boolean isAutoLogin() {
		return sPref.getBoolean(AUTO_LOGIN, false);
	}
	
	public void setAutoLogin(boolean autoLogin) {
		Editor editor = sPref.edit();
		editor.putBoolean(AUTO_LOGIN, autoLogin);
		editor.commit();
	}
	
	/**
	 * set last modified time stamp of local group list.
	 */
	public void setLocalGroupListLastModified() {
		Editor editor = sPref.edit();
		editor.putLong(LOCAL_GROUP_LAST_MODIFIED, new Date().getTime() / 1000);
		editor.commit();
	}
	
	public long getLocalGroupListLastModified() {
		return sPref.getLong(LOCAL_GROUP_LAST_MODIFIED, 0);
	}

    /**
     * set last modified time stamp of local group list.
     */
    public void setLocalContactListLastModified() {
        Editor editor = sPref.edit();
        editor.putLong(LOCAL_CONTACT_LAST_MODIFIED, new Date().getTime() / 1000);
        editor.commit();
    }

    public long getLocalContactListLastModified() {
        return sPref.getLong(LOCAL_CONTACT_LAST_MODIFIED, 0);
    }

    public void setShowPublicInContactlist(boolean isshow) {
        Editor editor = sPref.edit();
        editor.putBoolean(SHOW_PUBLIC_IN_CONTACTLIST, isshow);
        editor.commit();
    }

    public boolean getShowPublicInContactlist() {
        return sPref.getBoolean(SHOW_PUBLIC_IN_CONTACTLIST, false);
    }

    public void setAcceptMessage(boolean acceptMessage) {
        Editor editor = sPref.edit();
        editor.putBoolean(ACCEPT_MESSAGE, acceptMessage);
        editor.commit();
    }

    public boolean getAcceptMessage() {
        return sPref.getBoolean(ACCEPT_MESSAGE, false);
    }

    /**
     * 刷新服务器上最新评论的时间戳。
     */
    public void setLatestReviewTimestamp() {
        Editor editor = sPref.edit();
        editor.putLong(LATEST_REVIEW_TIMESTAMP, new Date().getTime() / 1000);
        editor.commit();
    }

    /**
     * 获取服务器上最新评论的时间戳。
     */
    public long getLatestReviewTimestamp() {
        return sPref.getLong(LATEST_REVIEW_TIMESTAMP, 0);
    }

    public void setCompanyId(String companyId) {
        Editor editor = sPref.edit();
        editor.putString(COMPANY_ID, companyId);
        editor.commit();
    }

    public String getCompanyId() {
        return sPref.getString(COMPANY_ID, "");
    }

    public void setAddBuddyAuto(boolean isAuto) {
        Editor editor = sPref.edit();
        editor.putBoolean(ADD_BUDDY_AUTOMATICALLY, isAuto);
        editor.commit();
    }

    public boolean getAddBuddyAutomatically() {
        return sPref.getBoolean(ADD_BUDDY_AUTOMATICALLY, true);
    }

    public void setOthersCanAddMe(String addType) {
        Editor editor = sPref.edit();
        editor.putString(PEOPLE_CAN_ADD_ME, addType);
        editor.commit();
    }

    public int getOthersCanAddMe() {
        int othersCanAddMe = 0;
        try {
            othersCanAddMe = Integer.parseInt(sPref.getString(PEOPLE_CAN_ADD_ME, "0"));
        } catch (NumberFormatException exception) {
        }
        return othersCanAddMe;
    }

    public void setUnknownCanCallMe(boolean canCall) {
        Editor editor = sPref.edit();
        editor.putBoolean(UNKNOWN_BUDDY_CAN_CALL_ME, canCall);
        editor.commit();
    }

    public boolean getUnknownCanCallMe() {
        return sPref.getBoolean(UNKNOWN_BUDDY_CAN_CALL_ME, true);
    }

    public void setUnknownCanMsgMe(boolean canMsg) {
        Editor editor = sPref.edit();
        editor.putBoolean(UNKNOWN_BUDDY_CAN_MESSAGE_ME, canMsg);
        editor.commit();
    }

    public boolean getUnknownCanMsgMe() {
        return sPref.getBoolean(UNKNOWN_BUDDY_CAN_MESSAGE_ME, true);
    }

    public void setPushShowDetailFlag(boolean showDetail) {
        Editor editor = sPref.edit();
        editor.putBoolean(PUSH_SHOW_DETAIL_FLAG, showDetail);
        editor.commit();
    }

    public boolean getPushShowDetailFlag() {
        return sPref.getBoolean(PUSH_SHOW_DETAIL_FLAG, true);
    }

    public void setListMeInNearbyResult(boolean isList) {
        Editor editor = sPref.edit();
        editor.putBoolean(LIST_ME_IN_NEARBY_RESULT, isList);
        editor.commit();
    }

    public boolean getListMeInNearbyResult() {
        return sPref.getBoolean(LIST_ME_IN_NEARBY_RESULT, true);
    }

    public void setIsUploadingMyAvatar(boolean isUploading) {
        Editor editor = sPref.edit();
        editor.putBoolean(IS_UPLOADING_MY_AVATAR, isUploading);
        editor.commit();
    }

    public boolean isUploadingMyAvatar() {
        return sPref.getBoolean(IS_UPLOADING_MY_AVATAR, false);
    }

    public void setSecurityLevel(int level) {
        Editor editor = sPref.edit();
        editor.putInt(SECURITY_LEVEL, level);
        editor.commit();
    }

    public int getSecurityLevel() {
        return sPref.getInt(SECURITY_LEVEL, 0);
    }

    public void setUseS3(boolean isUseS3) {
        Editor editor = sPref.edit();
        editor.putBoolean(USE_S3, isUseS3);
        editor.commit();
    }

    /** Use Aliyun OSS as storage service. */
    public boolean isUseOss() {
        return true;
    }

    public String getOssUid() {
        return "u9HhKKGaN779cDQQ";
    }

    public String getOssKey() {
        return "E67LFFrjqeSQ4FKmVFhXgpACUrSgEf";
    }

    public String getOssBucket() {
        return "om-im-dev01";
    }

    /** Use Amazon S3 as storage service. */
    public boolean isUseS3() {
        return sPref.getBoolean(USE_S3, false);
    }

    public void setS3Uid(String s3Uid) {
        Editor editor = sPref.edit();
        String desS3Uid = s3Uid;
        if (!TextUtils.isEmpty(s3Uid)) {
            try {
                desS3Uid = new DesUtils().encrypt(s3Uid);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        editor.putString(S3_UID, desS3Uid);
        editor.commit();
    }

    public String getS3Uid() {
        String desS3Uid = sPref.getString(S3_UID, GlobalSetting.S3_ACCESS_KEY_ID);
        String s3Uid = desS3Uid;
        if (!TextUtils.isEmpty(desS3Uid)) {
            try {
                s3Uid = new DesUtils().decrypt(desS3Uid);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return s3Uid;
    }

    public void setS3Pwd(String s3Pwd) {
        Editor editor = sPref.edit();
        String desS3pwd = s3Pwd;
        if (!TextUtils.isEmpty(s3Pwd)) {
            try {
                desS3pwd = new DesUtils().encrypt(s3Pwd);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        editor.putString(S3_PASSWORD, desS3pwd);
        editor.commit();
    }

    public String getS3Pwd() {
        String desS3pwd = sPref.getString(S3_PASSWORD, GlobalSetting.S3_SECRET_KEY);
        String s3Pwd = desS3pwd;
        if (!TextUtils.isEmpty(desS3pwd)) {
            try {
                s3Pwd = new DesUtils().decrypt(desS3pwd);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return s3Pwd;
    }

    public void setS3Bucket(String s3Bucket) {
        Editor editor = sPref.edit();
        editor.putString(S3_BUCKET, s3Bucket);
        editor.commit();
    }

    public String getS3Bucket() {
        return sPref.getString(S3_BUCKET, GlobalSetting.S3_BUCKET);
    }

    public void setWebDomain(String webDomain) {
        Editor editor = sPref.edit();
        editor.putString(WEB_DOMAIN, webDomain);
        editor.commit();
    }

    public String getWebDomain() {
        return sPref.getString(WEB_DOMAIN, "");
    }

    public void setSipDomain(String sipDomain) {
        Editor editor = sPref.edit();
        editor.putString(SIP_DOMAIN, sipDomain);
        editor.commit();
    }

    public String getSipDomain() {
        return sPref.getString(SIP_DOMAIN, "");
    }

    public void setSipPassword(String sipHashedPwd) {
        Editor editor = sPref.edit();
        editor.putString(PASSWORD_HASHED_FOR_SIP, sipHashedPwd);
        editor.commit();
    }

    public String getSipPassword() {
        return sPref.getString(PASSWORD_HASHED_FOR_SIP, "");
    }

    public void setActiveAppType(String activeAppType) {
        Editor editor = sPref.edit();
        editor.putString(ACTIVE_APP_TYPE, activeAppType);
        editor.commit();
    }

    public String getActiveAppType() {
        return sPref.getString(ACTIVE_APP_TYPE, "");
    }

    /**
     * Get AutoAddBuddyRule from local db
     * 
     * @return
     */
    public boolean fGetLocalPrivacySetting_getAutoAddBuddyRule() {
        return sPref.getBoolean("add_buddy_automatically", true);
    }

    /**
     * Get PeopleCanAddMeRule from local db
     * 
     * @return
     */
    public boolean fGetLocalPrivacySetting_getPeopleCanAddMeRule() {
        return sPref.getBoolean("people_can_add_me", true);

    }

    /**
     * Get UnknownPeopleCanCallMeRule from local db
     * 
     * @return
     */
    public boolean fGetLocalPrivacySetting_getUnknownPeopleCanCallMeRule() {
        return sPref.getBoolean("unknown_buddy_can_call_me", false);

    }

    /**
     * Get UnknownPeopleCanMessageMeRule from local db
     * 
     * @return
     */
    public boolean fGetLocalPrivacySetting_getUnknownPeopleCanMessageMeRule() {

        return sPref.getBoolean("unknown_buddy_can_message_me", true);

    }

    /**
     * Get ShowPushMsgDetailRule from local db
     * 
     * @return
     */
    public boolean fGetLocalPrivacySetting_getShowPushMsgDetailRule() {
        return sPref.getBoolean("push_show_detail_flag", true);
    }

    public void setApplyTimes(int applyTimes) {
        Editor editor = sPref.edit();
        editor.putInt(APPLY_TIMES, applyTimes);
        editor.commit();
    }

    public int getApplyTimes() {
        return sPref.getInt(APPLY_TIMES, 0);
    }

    public void setUserName(String userName) {
        Editor editor = sPref.edit();
        editor.putString(USERNAME, userName);
        editor.commit();
    }

    public String getUserName() {
        return sPref.getString(USERNAME, "");
    }

    public void setCountryCode(String countryCode) {
        Editor editor = sPref.edit();
        editor.putString(COUNTRYCODE, countryCode);
        editor.commit();
    }

    public String getMyCountryCode() {
        return sPref.getString(COUNTRYCODE, "");
    }

    public String getMyPhoneNumber() {
        return sPref.getString(MY_PHONE, "");
    }

    public String getMyNickName() {
        return sPref.getString(MY_NICKNAME, "");
    }

    public String getMyPronunciation() {
        return sPref.getString(MY_PRONUNCIATION, "");
    }

    public String getMyMobile() {
        return sPref.getString(MY_MOBILE, "");
    }

    public void setMyEmail(String email) {
        sPref.edit().putString(MY_EMAIL, email).commit();
    }

    public String getMyEmail() {
        return sPref.getString(MY_EMAIL, "");
    }

    public String getMyJobTitle() {
        return sPref.getString(MY_JOB_TITLE, "");
    }

    public String getMyEmployeeId() {
        return sPref.getString(MY_EMPLOYEE_ID, "");
    }

    public String getMyStatus() {
        return sPref.getString(MY_STATUS, "");
    }

    public String getMyBirthday() {
        return sPref.getString(MY_BIRTHDAY, "");
    }

    public int getMySex() {
        return sPref.getInt(MY_SEX, Buddy.SEX_NULL);
    }

    public void setWowtalkId(String wowtalkId) {
        Editor editor = sPref.edit();
        editor.putString(WOWTALK_ID, wowtalkId);
        editor.commit();
    }

    public String getMyWowtalkID() {
        return sPref.getString(WOWTALK_ID, "");
    }

    public int getMyAccountType() {
        return sPref.getInt(MY_ACCOUNT_TYPE, Buddy.ACCOUNT_TYPE_NULL);
    }

    public void setPasswordChanged(boolean isChanged) {
        Editor editor = sPref.edit();
        editor.putBoolean(PASSWORD_CHANGED, isChanged);
        editor.commit();
    }

    public boolean getMyPasswordChangedState() {
        return sPref.getBoolean(PASSWORD_CHANGED, false);
    }

    public void setWowtalkIdChanged(boolean isChanged) {
        Editor editor = sPref.edit();
        editor.putBoolean(WOWTALKID_CHANGED, isChanged);
        editor.commit();
    }

    public boolean getMyWowtalkIdChangedState() {
        return sPref.getBoolean(WOWTALKID_CHANGED, false);
    }

    public String getMyArea() {
        return sPref.getString(MY_AREA, "");
    }
    
    public long getMyPhotoUploadedTimestamp() {
        return sPref.getLong(MY_PHOTO_UPLOADED_TIMESTAMP, 0);
    }

    public void setServerVersion(int serverVersion) {
        Editor editor = sPref.edit();
        editor.putInt(SERVER_VERSION, serverVersion);
        editor.commit();
    }

    public int getLatestServerSDKVersion() {
        return sPref.getInt(SERVER_VERSION, -1);
    }

    public void setClientVersion(int clientVersion) {
        Editor editor = sPref.edit();
        editor.putInt(ANDROID_CLIENT_VERSION, clientVersion);
        editor.commit();
    }

    public int getLatestClientSDKVersion() {
        return sPref.getInt(ANDROID_CLIENT_VERSION, -1);
    }

    public boolean isPushTokenReportedToServer() {
        return sPref.getBoolean(TOKEN_REPORTED_TO_SERVER, false);
    }

    public void setPushTokenReportedToServer(boolean flag) {
        Editor editor = sPref.edit();
        editor.putBoolean(TOKEN_REPORTED_TO_SERVER, flag);
        editor.commit();
    }


    /**
     * Check whether the input phone number is used for demo 
     * 
     * @return
     */
    public boolean fIsDemoPhoneNumber(String strPhoneNumber) {
        if (strPhoneNumber.contains("1980121619801216"))
            return true;

        return false;
    }

    public void setDemoAccessCode(String demoCode) {
        Editor editor = sPref.edit();
        editor.putString(DEMO_MODE_CODE, demoCode);
        editor.commit();
    }

    /**
     * Get the access code in demo mode 
     * 
     * @return
     */
    public  String getDemoAccessCode() {
        return sPref.getString("DEMO_MODE_CODE", "");
    }

    public void setSetupStep(int step) {
        Editor editor = sPref.edit();
        editor.putInt(SETUP_STEP, step);
        editor.commit();
    }

    /**
     * 此方法可能在未登录状态下调用，此时没有uid，spref为null
     * @return
     */
    public int getSetupStep() {
        if (null == sPref) {
            return 0;
        }
        return sPref.getInt(SETUP_STEP, 0);
    }

    public void setPinYinResId(int pinyinResId) {
        Editor editor = sPref.edit();
        editor.putInt(PINYIN_DATA_RES_ID, pinyinResId);
        editor.commit();
    }

    public int getPinyinResId() {
        return sPref.getInt(PINYIN_DATA_RES_ID, 0);
    }

    public String getLastSavedLatitude() {
        return sPref.getString(CURRENT_LOCATION_LATITUDE, String.valueOf(DEF_UNUSED_LOCATION));
    }

    public void setLastSavedLatitude(String latitude) {
        Editor editor = sPref.edit();
        editor.putString(CURRENT_LOCATION_LATITUDE, latitude);
        editor.commit();
    }

    public String getLastSavedLongitude() {
        return sPref.getString(CURRENT_LOCATION_LONGITUDE, String.valueOf(DEF_UNUSED_LOCATION));
    }

    public void setLastSavedLongitude(String longitude) {
        Editor editor = sPref.edit();
        editor.putString(CURRENT_LOCATION_LONGITUDE, longitude);
        editor.commit();
    }

    /**
     * 用户logout时清除SP数据
     */
    public void clear() {
        boolean isContactsUptodate = isContactsUptodateAfterLogin();
        boolean isSysNoticeEnabled = isSysNoticeEnabled();
        boolean isSysNoticeMusicOn = isSysNoticeMusicOn();
        boolean isSysNoticeVibrateOn = isSysNoticeVibrateOn();
        boolean isSysNoticeNewMessageOn = isSysNoticeNewMessageOn();
        boolean isSysNoticeTelephoneOn = isSysNoticeTelephoneOn();
        Editor editor = sPref.edit();
        editor.clear();
        editor.commit();
        setContactsUptodateAfterLogin(isContactsUptodate);
        setSysNoticeEnabled(isSysNoticeEnabled);
        setSysNoticeMusicOn(isSysNoticeMusicOn);
        setSysNoticeVibrateOn(isSysNoticeVibrateOn);
        setSysNoticeNewMessageOn(isSysNoticeNewMessageOn);
        setSysNoticeTelephoneOn(isSysNoticeTelephoneOn);
    }

    /**
     * 清除指定uid对应的SP
     * @param uid
     */
    public void clearByUid(String uid) {
        SharedPreferences preferences = sContext.getSharedPreferences(
                sContext.getPackageName() + "_" + uid, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    // TODO 原先WowTalkWebServerIF中的 #clearLocalData
    public void clearLocalData() {
        Editor editor = sPref.edit();
        editor.remove(SETUP_STEP);
        editor.remove(APPLY_TIMES);
        editor.remove(COUNTRYCODE);
        editor.remove(USERNAME);

        editor.remove(WEB_DOMAIN);
        editor.remove(SIP_DOMAIN);
        editor.remove(PASSWORD_HASHED_FOR_SIP);
        editor.remove(USE_S3);
        editor.remove(S3_UID);
        editor.remove(S3_PASSWORD);
        editor.remove(S3_BUCKET);

        editor.remove(COMPANY_ID);
        editor.remove(UID);
        editor.remove(PASSWORD_HASHED);
        editor.remove(WOWTALKID_CHANGED);
        editor.remove(PASSWORD_CHANGED);
        editor.remove(DEMO_MODE_CODE);
        editor.remove(APPLY_TIMES);
        editor.remove(TOKEN_REPORTED_TO_SERVER);

        editor.remove(MY_NICKNAME);
        editor.remove(MY_STATUS);
        editor.remove(MY_BIRTHDAY);
        editor.remove(MY_SEX);
        editor.remove(MY_AREA);
        editor.remove(MY_PHOTO_UPLOADED_TIMESTAMP);
        editor.remove(WOWTALK_ID);

        editor.remove(SERVER_VERSION);
        editor.remove(ANDROID_CLIENT_VERSION);
        editor.remove(TIME_OFFSET);
        editor.remove(ACTIVE_APP_TYPE);

        editor.commit();
    }

    /**
     * logout时修改数据
     */
    public void logoutAccount() {
        // 单一账户的情况(和ios统一下周出多账户版本，暂时仍保留原先的但账户logout逻辑)
//        Editor editor = sPref.edit();
//        editor.remove(UID);
//        editor.remove(PASSWORD_HASHED);
//        editor.remove(PASSWORD_HASHED_FOR_SIP);
//        editor.remove(SETUP_STEP);
//        editor.commit();

        ArrayList<Account> accounts = getAccountList();
        if (!accounts.isEmpty()) {
            // 将当前用户从account_list中移除
            String uid = getUid();
            Account tempAccount;
            for (Iterator<Account> iterator = accounts.iterator(); iterator.hasNext();) {
                tempAccount = iterator.next();
                if (uid.equals(tempAccount.uid)) {
                    iterator.remove();
                    break;
                }
            }
            ArrayList<String> accoutIds = getAccountIds();
            String tempAccountId = "";
            for (Iterator<String> iterator = accoutIds.iterator(); iterator.hasNext();) {
                tempAccountId = iterator.next();
                if (uid.equals(tempAccountId)) {
                    iterator.remove();
                    break;
                }
            }
            setAccountList(accounts);
            setAccountIds(accoutIds);

            save2AccountSP(uid);
            // 清空SP_current
            clearOldSP();
        }
    }

    /**
     * 切换用户:
     * <ol>
     * <li> 将当前SP_current中的信息保存到SP_uid中
     * <li> 清空当期SP_current中的信息
     * <li> 修改SP_root中的Account_list列表
     * <li> 将SP_uid中的信息拷贝到新的SP_current中
     * </ol>
     * @param srcUid
     * @param destUid
     * @deprecated
     */
    public void switchAccount(Account oldAccount, Account newAccount) {
        if (null == newAccount) {
            return;
        }

        String srcUid = "";
        String destUid = newAccount.uid;
        if (null != oldAccount) {
            srcUid = oldAccount.uid;
        }

        // 1. 当前用户的SP保存到SP_uid中
        saveOldSP(srcUid);

        // 2. 清空当前SP_current
        clearOldSP();

        // 3. 修改SP_root中的Account_list列表
        saveAccountsList(oldAccount, newAccount);

        // 4. 将SP_uid中的信息拷贝到新的SP_current中
        fillNewSP(destUid);
    }

    /**
     * 将当前SP中的信息保存到SP_root和SP_uid中，并清空当前SP
     * @param oldUid
     */
    public void saveOldSP(String oldUid) {
        // 当前用户的SP_current保存到SP_uid中
        if (!TextUtils.isEmpty(oldUid)) {
            save2AccountSP(oldUid);
        }
    }

    public void fillNewSP(String newUid) {
        // 从SP_uid中拷贝到SP_current
        if (!TextUtils.isEmpty(newUid)) {
            getFromAccountSP(newUid);
        }
    }

    public void clearOldSP() {
        Editor editor = sPref.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * 保存SP_root中的ACCOUNT_LIST和ACCOUNT_ID_LIST
     * @param oldAccount
     * @param newAccount
     */
    public void saveAccountsList(Account oldAccount, Account newAccount) {
        newAccount.isOnline = true;
        newAccount.unreadCounts = 0;

        // oldAccount可能和newAccount是一样的（oldAccount在别处登录，被踢掉的情况下）,
        // 此时只要添加newAccount即可
        if (null != oldAccount && !TextUtils.isEmpty(oldAccount.uid)
                && oldAccount.uid.equals(newAccount.uid)) {
            oldAccount = null;
        }
        // 之前有帐户处于登录状态时，此帐户被切换后unreadCounts应为0
        if (null != oldAccount) {
            oldAccount.isOnline = false;
            oldAccount.unreadCounts = 0;
        }

        // 改变SP_root中保存的帐户数据
        ArrayList<Account> accounts = getAccountList();
        Account tempAccount = null;
        for (Iterator<Account> iterator = accounts.iterator(); iterator.hasNext();) {
            tempAccount = iterator.next();
            if (newAccount.uid.equals(tempAccount.uid) || (null != oldAccount && oldAccount.uid.equals(tempAccount.uid))) {
                iterator.remove();
            } else {
                tempAccount.isOnline = false;
            }
        }
        // 将newAccount和oldAccount添加到SP_root中
        ArrayList<Account> newAccounts = new ArrayList<Account>();
        newAccounts.add(newAccount);
        if (null != oldAccount) {
            newAccounts.add(oldAccount);
        }
        newAccounts.addAll(accounts);
        setAccountList(newAccounts);

        // 改变保存的账户uid列表
        ArrayList<String> accountIds = getAccountIds();
        if (null != oldAccount) {
            accountIds.remove(oldAccount.uid);
        }
        accountIds.remove(newAccount.uid);
        ArrayList<String> newAccountIds = new ArrayList<String>();
        newAccountIds.add(newAccount.uid);
        if (null != oldAccount) {
            newAccountIds.add(oldAccount.uid);
        }
        newAccountIds.addAll(accountIds);
        setAccountIds(newAccountIds);
    }

    public void removeAccount(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        // 改变SP中保存的帐户数据
        ArrayList<Account> accounts = getAccountList();
        Account tempAccount = null;
        for (Iterator<Account> iterator = accounts.iterator(); iterator.hasNext();) {
            tempAccount = iterator.next();
            if (uid.equals(tempAccount.uid)) {
                iterator.remove();
                break;
            }
        }
        setAccountList(accounts);

        // 改变保存的账户uid列表
        ArrayList<String> accountIds = getAccountIds();
        accountIds.remove(uid);
        setAccountIds(accountIds);
    }

    /**
     * 从指定uid对应的SP_uid中拷贝到SP_current中,
     * @param srcUid
     */
    private void getFromAccountSP(String srcUid) {
        SharedPreferences preferences = sContext.getSharedPreferences(
                sContext.getPackageName() + "_" + srcUid, Context.MODE_PRIVATE);
        copySP(preferences, sPref);
    }

    /**
     * 从当前使用的SP_current拷贝到SP_uid中，
     * 后者中的数据会被先清除后重新写入
     * @param targetUid
     */
    private void save2AccountSP(String targetUid) {
        SharedPreferences preferences = sContext.getSharedPreferences(
                sContext.getPackageName() + "_" + targetUid, Context.MODE_PRIVATE);
        copySP(sPref, preferences);
    }

    /**
     * 从src向dest中拷贝所有变量值
     * @param src
     * @param dest
     */
    private void copySP(SharedPreferences src, SharedPreferences dest) {
        Editor editor = dest.edit();
        Map<String, ?> maps = src.getAll();
        Set<String> keys = maps.keySet();
        String tempKey = null;
        Object tempValue = null;
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            tempKey = iterator.next();
            tempValue = maps.get(tempKey);
            if (tempValue instanceof String) {
                editor.putString(tempKey, (String) tempValue);
            } else if (tempValue instanceof Boolean) {
                editor.putBoolean(tempKey, (Boolean) tempValue);
            } else if (tempValue instanceof Integer) {
                editor.putInt(tempKey, (Integer) tempValue);
            } else if (tempValue instanceof Float) {
                editor.putFloat(tempKey, (Float) tempValue);
            } else if (tempValue instanceof Long) {
                editor.putLong(tempKey, (Long) tempValue);
            }
        }
        editor.commit();
    }
}
