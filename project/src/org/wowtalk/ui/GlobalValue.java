package org.wowtalk.ui;

import java.util.HashSet;

import co.onemeter.oneapp.ui.Log;

public class GlobalValue {
    /** Release option, release as WowCity? */
    public final static boolean RELEASE_AS_WOWCITY = false;
    /** Release option, release as WowTalkBiz? */
    public final static boolean RELEASE_AS_WOWTALKBIZ = true;

	/*don't change this*/
	public static final String INCOME_MISSEDCALL_INTENT="org.wowtalk.intent.missedcall";
    /*end of this*/
	public final static  String BROADCAST_SHOULD_SHOW_KEYPAD_ACTION = "org.wowtalk.intent.should_showkeypad";

	
	public final static  String BROADCAST_WOWTALKUSER_UPDATE_ACTION = "org.wowtalk.intent.wowtalkuser_updated";
	public final static  String BROADCAST_GROUPS_UPDATE_ACTION = "org.wowtalk.intent.groups_updated";
	public final static  String BROADCAST_ALL_CONTACTS_UPDATE_ACTION = "org.wowtalk.intent.allcontacts_updated";
	public final static  String BROADCAST_CURRENT_GROUP_CONTACTS_UPDATE_ACTION = "org.wowtalk.intent.current_group_contacts_updated";

	public static boolean IS_BOOT_FROM_LOGIN;

	public final static int NOTIFICATION_FOR_CHATMESSAGE = 1234;
    public final static int NOTIFICATION_FOR_REGISTRATIONSTATE = 1235;
    public static final int NOTIFICATION_FOR_PENDINGREQUESTS = 1236;
    public static final int NOTIFICATION_FOR_NETWORKERROR = 1237;

    /** 已经知道有新内容，但不知道具体数量，此时把数量标记此特殊值，渲染UI时可以酌情处理。*/
    public static final int BADGE_VALUE_NONZERO_NOTSURE = -2;

    public static boolean _weStartANormalPhoneCall=false;

    /** 未读的动态评论的数量。*/
    public static int unreadMomentReviews;

	public static int screenW;
	public static int screenH;

    /**
     * https://www.bugsense.com
     */
    public static final String BUGSENSE_APIKEY = "e87cb7e1";

    /**
     * 标记刷新成员到最新状态的群组id，如果某个group_id不在这个Set里面，则需要请求服务器刷新
     */
    private static HashSet<String> mLatestMembersGroupList = new HashSet<String>();
    private static boolean mIsNeedToRefreshAllGroups = false;

    public static boolean isNeedToRefreshMembers(String groupId) {
        return !mLatestMembersGroupList.contains(groupId);
    }

    public static void setDontNeedToRefreshMembers(String groupId) {
        mLatestMembersGroupList.add(groupId);
    }

    public static void clearNeedToRefreshMembersGroups() {
        Log.i("GlobalValue#clearNeedToRefreshAllGroups");
        mLatestMembersGroupList.clear();
    }

    public static boolean isNeedToRefreshAllGroups() {
        return mIsNeedToRefreshAllGroups;
    }

    public static void setNeedToRefreshAllGroups(boolean isNeed) {
        mIsNeedToRefreshAllGroups = isNeed;
    }

    /**
     * clear global values
     */
    public static void clearGolableValues() {
        unreadMomentReviews = 0;
    }
}
