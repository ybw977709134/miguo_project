package org.wowtalk.api;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;
import org.wowtalk.Log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Database Class
 * 
 * @author coca Usage sample: Database db = new Database(context); db.open();
 *         db.storeNewIdentityWithoutUpdate(wowtalkuser);
 *         Do not call db.close() if multi-thread is using it in the app
 * 
 */
public class Database {
    public final static int INVALID_LATLON = -1;

    public static final String TBL_MESSAGES = "chatmessages";
    public static final String TBL_CHATMESSAGE_READED = "chatmessage_readed";
    public static final String TBL_MESSAGES_HISTORY="chatmessages_history";
    public static final String TBL_PENDING_REQUESTS = "pending_requests";
    public static final String TBL_GROUP = "group_chatroom";
    public static final String TBL_GROUP_MEMBER = "group_member";
    public static final String TBL_MOMENT = "moment";
    public static final String TBL_MOMENT_MEDIAS = "moment_media";
    public static final String TBL_MOMENT_REVIEWS = "moment_review";
    public static final String TBL_BUDDIES = "buddies";
    public static final String TBL_BUDDY_DETAIL = "buddydetail";
    public static final String TBL_UNSENT_RECEIPTS = "unsent_receipts";
    public static final String TBL_LESSON = "lesson";
    public static final String TBL_LESSON_PERFORMANCE = "lesson_performance";
    public static final String TBL_LESSON_HOMEWORK = "lesson_homework";
    public static final String TBL_LESSON_PARENT_FEEDBACK = "lesson_parent_feedback";
    public static final String TBL_LESSON_ADD_HOMEWORK = "lesson_add_homework";
    /** 学生在特定学校的备注名称。*/
    public static final String TBL_STUDENT_ALIAS = "student_alias";
    @Deprecated
    public static final String TBL_LATEST_CONTACTS = "latest_contacts";
    public static final String TBL_LATEST_CHAT_TARGET = "latest_chat_target";
    public static final String TBL_LATEST_CHAT_TARGET_UNREAD_COUNT = "latest_chat_target_unread_count";
    /**
     * 公司的名称／头像变化（即发送通知的人）
     */
    public static final String DUMMY_TBL_NOTICE_MAN_CHANGED = "dummy_notice_man_changed";
    /**
     * 头像，status,pronunciation变化时触发
     */
    public static final String DUMMY_TBL_MY_INFO_UPDATED = "dummy_my_info_updated";
    /**
     * 不是真实存在的表，登录／从后台切到前台之后，下载latest chat target完成后触发监听
     */
    public static final String DUMMY_TBL_LATEST_CHAT_TARGET_FINISHED = "dummy_latest_chat_target_after_login";
    public static final String TBL_EVENT = "event";
    public static final String TBL_FAVORITE = "favorite";
    public static final String TBL_SURVEY = "survey";
    
    /**
     * 文字草稿箱
     */
    public static final String TBL_SAVE_MESSAGE = "save_message";
    
    
    /**
     * 不是真实存在的表，只是起到监听用户切换的作用
     */
    public static final String DUMMY_TBL_SWITCH_ACCOUNT = "dummy_switch_account";
    /**
     * 不是真实存在的表，监听动态封面下载成功
     */
    public static final String DUMMY_TBL_ALBUM_COVER_GOT = "dummy_album_cover_got";
    /**
     * 不是真实存在的表，只是起到监听favorite group变化的作用
     */
    public static final String DUMMY_TBL_FAVORITE_GROUP = "dummy_favorite_group";
    /**
     * 不是真实存在的表，只是起到监听frequent contacts变化的作用
     */
    public static final String DUMMY_TBL_FAVORITE_CONTACT = "dummy_favorite_contact";

    /**
     * 不是真实存在的表，只是起到监听群组成员是否加载完成的作用
     */
    public static final String DUMMY_TBL_FINISH_LOAD_MEMBERS = "dummy_finish_load_members";

    /**
     * 不是真实存在的表，只是起到监听好友列表是否加载完成的作用
     */
    public static final String DUMMY_TBL_FINISH_LOAD_CONTACTS = "dummy_finish_load_contacts";

    /**
     * 不是真实存在的表，只是起到监听消息的未读／已读状态变化的作用
     */
    public static final String DUMMY_TBL_CHAT_MESSAGES_READED = "dummy_chat_messages_readed";

    /**
     * 不是真实存在的表，是在改变语言环境时，重新赋值sort_key后，需要更新联系人界面
     */
    public static final String DUMMY_TBL_SORT_KEY_UPDATED = "dummy_sort_key_updated";

    /**
     * latest contacts in ContactsActivity for biz
     */
    public static final String DUMMY_LATEST_CONTACTS_UPDATE = "dummy_latest_contacts_update";

    private static final String LOG_TAG = "onemeter/Database";

    public static int sFlagIndex;
    private static int mFlagIndex;

    // Database fields
    private Context context;
    private static SQLiteDatabase database;
    private static DatabaseHelper dbHelper;
    private static String ownerUid;
    private PrefUtil mPrefUtil;

    /**
     * Constructor.
     *
     * @param context
     */
    public Database(Context context) {
        this.context = context;
        mPrefUtil = PrefUtil.getInstance(context);
        mFlagIndex = sFlagIndex;
        open();
    }  

    /**
     * 这个方法是给 WowTalkVoipIF 调用的。
     *
     * java.lang.NoSuchMethodError: org.wowtalk.api.Database.getInstance
         at org.wowtalk.api.WowTalkVoipIF.fSendChatMessage(WowTalkVoipIF.java:1034)
     * @param context
     * @return
     */
    public static Database getInstance(Context context) {
        return new Database(context);
    }

    /**
     * This has to be called before any function is called.
     * <p>This will be called in constructor automatically.</p>
     * 此处为遗留代码，sdk在调用，上层调用时，不需要显式调用此方法(已在构造方法中调用)
     *
     * @return
     * @throws SQLException
     */
    public Database open() throws SQLException {
        String uid = mPrefUtil.getUid();
        if (null == dbHelper || dbHelper.flagIndex != sFlagIndex
                || null == database || !database.isOpen()
                || !TextUtils.equals(ownerUid, uid)) {
            dbHelper = new DatabaseHelper(context, uid, mFlagIndex);
            database = dbHelper.getWritableDatabase();
            ownerUid = uid;
        }
        return this;
    }

    public void close() {
        if (null != dbHelper) {
            dbHelper.close();
        }
        dbHelper = null;
        database = null;
    }

    private boolean isDBUnavailable() {
        return null == database || !database.isOpen() || mFlagIndex != sFlagIndex;
    }

    private void beginTrasaction() {
        if (isDBUnavailable()) {
            return;
        }
        database.beginTransaction();
    }

    private void endTranscation() {
        if (isDBUnavailable()) {
            return;
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /**
     * 切换用户，触发数据库的切换
     */
    public static void switchAccount() {
        Database.sFlagIndex++;
        Connect2.sFlagIndex++;
        markDBTableModified(DUMMY_TBL_SWITCH_ACCOUNT);
    }

	/**
	 * Save buddies into `buddies` table and `buddydetail` table.
	 * 
	 * @param buddies
	 * @return
	 * 
	 */
    public synchronized int storeBuddies(ArrayList<Buddy> buddies) {
        return storeBuddies(buddies, true);
    }

    public synchronized int storeBuddies(ArrayList<Buddy> buddies, boolean isObserver) {

        int cnt = 0;
        if (isDBUnavailable()) {
            return cnt;
        }
        beginTrasaction();
        for (Buddy b : buddies) {
            cnt += this.storeNewBuddyWithUpdate(b, false);
        }
        endTranscation();
        if (isObserver) {
            markDBTableModified("buddies");
            markDBTableModified("buddydetail");
        }
        return cnt;
    }

    public void storeGroupMemberIds(String groupId, ArrayList<String> buddyIds) {
        if (TextUtils.isEmpty(groupId) || null == buddyIds || buddyIds.isEmpty()) {
            return;
        }

        if (isDBUnavailable()) {
            return;
        }

        beginTrasaction();
        database.delete(TBL_GROUP_MEMBER, "group_id=?", new String[] {groupId});

        ContentValues values = new ContentValues();
        for (String buddyId : buddyIds) {
            values.clear();
            values.put("group_id", groupId);
            values.put("member_id", buddyId);
            values.put("level", GroupMember.LEVEL_DEFAULT);
            database.insert(TBL_GROUP_MEMBER, null, values);
        }

        endTranscation();
    }

    public void storeGroupMemberIds(ArrayList<String> groupIds, ArrayList<ArrayList<String>> buddyIdLists) {
        if (null == groupIds || groupIds.isEmpty()
                || null == buddyIdLists || buddyIdLists.isEmpty()
                || groupIds.size() != buddyIdLists.size()) {
            return;
        }

        if (isDBUnavailable()) {
            return;
        }

        beginTrasaction();
        for (int i = 0; i < groupIds.size(); i++) {
            String groupId = groupIds.get(i);
            ArrayList<String> buddyIds = buddyIdLists.get(i);
            database.delete(TBL_GROUP_MEMBER, "group_id=?", new String[] {groupId});

            ContentValues values = new ContentValues();
            for (String buddyId : buddyIds) {
                values.clear();
                values.put("group_id", groupId);
                values.put("member_id", buddyId);
                values.put("level", GroupMember.LEVEL_DEFAULT);
                database.insert(TBL_GROUP_MEMBER, null, values);
            }
        }
        endTranscation();
    }

	/**
	 * 同#storeBuddies(ArrayList<Buddy> buddies)， 由于泛型的使用，必须重载
	 * @param members
	 * @return
	 */
	public synchronized int storeMembersAsBuddies(ArrayList<GroupMember> members) {

        int cnt = 0;
        if (isDBUnavailable()) {
            return cnt;
        }
        beginTrasaction();
        for (Buddy b : members) {
            cnt += this.storeNewBuddyWithUpdate(b);
        }
        endTranscation();
        return cnt;
    }

    public synchronized int storeNewBuddyWithUpdate(Buddy buddy) {
        return storeNewBuddyWithUpdate(buddy, true);
    }

	/**
	 * Insert a Buddy ( Update if username exists)
	 * 
	 * @param buddy
	 * @return number of records has been updated
	 */
	public synchronized int storeNewBuddyWithUpdate(Buddy buddy, boolean isObserver) {
		if (buddy.userID == null || buddy.userID.equals(""))
			return 0;

        if (isDBUnavailable()) {
            return -1;
        }
//		if (buddy.userID.endsWith(WebServerIF.getInstance(context)
//				.fGetMyUserIDFromLocal())) {
//			Log.w("storeNewBuddyWithUpdate(): this buddy has the same userID as mine, don't store.");
//			return 0;
//		}

		Log.i("storeNewBuddyWithUpdate:" + buddy.userID + ","
                + buddy.phoneNumber + "," + buddy.getFriendShipWithMe());

		Cursor mCursor = database.query(true, "buddies",
				new String[] { "friendship" }, "uid='" + buddy.userID+"'", null,
				null, null, null, null);

        ContentValues values = new ContentValues();
		values.put("uid", buddy.userID);
		values.put("phone_number", buddy.phoneNumber);
        if (mPrefUtil.getUid().equals(buddy.userID)) {
            values.put("friendship", Buddy.RELATIONSHIP_SELF);
        } else if (buddy.getAccountType() == Buddy.ACCOUNT_TYPE_NOTICE_MAN) {
            values.put("friendship", Buddy.RELATIONSHIP_NONE);
        } else {
            if (buddy.getFriendShipWithMe() != Buddy.RELATIONSHIP_UNSET)
                values.put("friendship", buddy.getFriendShipWithMe());
            else
                values.put("friendship", Buddy.RELATIONSHIP_NONE);
        }
        values.put("will_block_msg", buddy.willBlockMsg ? 1 : 0);
        values.put("will_block_msg_notification", buddy.willBlockNotification ? 1 : 0);
        values.put("hidden", buddy.hidden ? 1 : 0);
        values.put("favorite", buddy.favorite ? 1 : 0);
        values.put("alias", buddy.alias);

		int rst = -1;
		if (mCursor != null && mCursor.moveToFirst()) {
            rst = database.update("buddies", values, "uid='" + buddy.userID+"'",
                        null);
		} else {
			rst = (int) database.insert("buddies", null, values);
		}
		
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}

        if (isObserver) {
            markDBTableModified("buddies");
        }

        storeNewBuddyDetailWithUpdate(buddy, isObserver);

		// update the display name of chat message
		updateChatMessageDisplayNameWithUser(buddy.userID, TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);

        // 既然已经是好友了，加入 pending requests 表中有相关条目，应该清理掉
        if (Buddy.RELATIONSHIP_FRIEND_BOTH == (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_BOTH))
            deletePendingRequest(null, buddy.userID);

		return rst;
	}

    public int storeNewBuddyDetailWithUpdate(Buddy ident) {
        return storeNewBuddyDetailWithUpdate(ident, true);
    }

	/**
	 * Insert a BuddyDetail ( Update if username exists)
	 * 
	 * @param ident
	 * @return number of records has been updated
	 */
	public int storeNewBuddyDetailWithUpdate(Buddy ident, boolean isObserver) {

		Log.i("storeNewBuddyDetailWithUpdate:" + ident.userID);

        int rst = -1;
        if (isDBUnavailable()) {
            return rst;
        }
        Cursor mCursor = null;
        synchronized ("storeNewBuddyDetailWithUpdate") {
            mCursor = database.query(true, "buddydetail", new String[] {
                    "photo_upload_timestamp", "photo_filepath",
            "thumbnail_filepath" }, "uid='" + ident.userID+"'", null, null,
            null, null, null);

            boolean isUpdating = false;
            if (mCursor != null && mCursor.moveToFirst()) {
                isUpdating = true;
                Buddy oldIdent = new Buddy();
                oldIdent.photoUploadedTimeStamp = mCursor.getLong(0);
                oldIdent.pathOfPhoto = mCursor.getString(1);
                oldIdent.pathOfThumbNail = mCursor.getString(2);

                Log.i("oldIdent:" + oldIdent.photoUploadedTimeStamp + ","
                        + oldIdent.pathOfPhoto + ",", oldIdent.pathOfThumbNail);

                if (oldIdent.pathOfThumbNail != null
                        && !oldIdent.pathOfThumbNail.equals("")
                        && ident.photoUploadedTimeStamp <= oldIdent.photoUploadedTimeStamp) {
                    ident.needToDownloadThumbnail = false;
                    ident.pathOfThumbNail = oldIdent.pathOfThumbNail;
                } else {
                    ident.needToDownloadThumbnail = true;
                }

                if (oldIdent.pathOfPhoto != null
                        && !oldIdent.pathOfPhoto.equals("")
                        && ident.photoUploadedTimeStamp <= oldIdent.photoUploadedTimeStamp) {
                    ident.needToDownloadPhoto = false;
                    ident.pathOfPhoto = oldIdent.pathOfPhoto;
                } else {
                    ident.needToDownloadPhoto = true;
                }
            }

            if (null == ident.pronunciation || TextUtils.isEmpty(ident.pronunciation.trim())) {
                if (null == ident.alias || TextUtils.isEmpty(ident.alias.trim())) {
                    ident.sortKey = Utils.makeSortKey(context, ident.nickName);
                } else {
                    ident.sortKey = Utils.makeSortKey(context, ident.alias);
                }
            } else {
                ident.sortKey = Utils.makeSortKey(context, ident.pronunciation);
            }

            ContentValues values = new ContentValues();
            values.put("uid", ident.userID);
            values.put("nickname", ident.nickName);
            values.put("wowtalkid", ident.username);
            values.put("last_status", ident.status);
            values.put("sex", ident.getSexFlag());
            values.put("area", ident.area);
            values.put("email", ident.getEmail());
            values.put("pronunciation", ident.pronunciation);
            values.put("mobile_phone", ident.mobile);
            values.put("job_title", ident.jobTitle);
            values.put("employee_id", ident.employeeId);
            values.put("device_number", ident.deviceNumber);
            values.put("app_ver", ident.appVer);
            values.put("sort_key", ident.sortKey);
            values.put("account_type", ident.getAccountType());
            values.put("photo_upload_timestamp", ident.photoUploadedTimeStamp);
            values.put("photo_filepath", ident.pathOfPhoto);
            values.put("thumbnail_filepath", ident.pathOfThumbNail);
            values.put("need_to_download_photo", ident.needToDownloadPhoto ? 1 : 0);
            values.put("need_to_download_thumbnail",
                    ident.needToDownloadThumbnail ? 1 : 0);

            if (isUpdating) {
                rst = database.update("buddydetail", values, "uid='" + ident.userID+"'",
                        null);
            } else {
                rst = (int) database.insert("buddydetail", null, values);
            }

        }

		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}

        if (isObserver) {
            markDBTableModified("buddydetail");
        }

		return rst;
	}

    public void updateBuddiesFavorite(ArrayList<String> buddyies, boolean isFavorite) {
        if (null == buddyies || buddyies.isEmpty()) {
            return;
        }
        beginTrasaction();
        for (String buddyId : buddyies) {
            updateBuddyFavorite(buddyId, isFavorite);
        }
        endTranscation();
    }

	/**
     * 更新buddy的favorite状态(收藏状态)，即更新关联表 TBL_FAVORITE
     * @param buddyId
     * @param isFavorite
     * @return
     */
    public void updateBuddyFavorite(String buddyId, boolean isFavorite) {
        if (isDBUnavailable()) {
            return;
        }
        // 先删除关联关系，如果新增，则添加；否则只删除
        database.delete(TBL_FAVORITE, "type=0 and target_id=?", new String[] {buddyId});
        if (isFavorite) {
            database.execSQL("insert into favorite(type,target_id,favorite_level) values (0,?,0)",
                    new String[] {buddyId});
        }
        markDBTableModified(DUMMY_TBL_FAVORITE_CONTACT);
    }

    /**
     * 批量更新联系人的sort_key字段
     * @param buddies
     * @return
     */
    public int updateBuddiesSortKey(ArrayList<Buddy> buddies) {
        int cnt = 0;
        if (null == buddies || buddies.isEmpty() || isDBUnavailable()) {
            return cnt;
        }
        ContentValues values = new ContentValues();
        for (Buddy buddy : buddies) {
            values.clear();
            values.put("sort_key", buddy.sortKey);
            cnt += this.updateBuddySortKey(buddy.userID, values);
        }
        if (cnt > 0) {
            markDBTableModified(DUMMY_TBL_SORT_KEY_UPDATED);
        }
        return cnt;
    }

    private int updateBuddySortKey(String userID, ContentValues values) {
        if (isDBUnavailable()) {
            return 0;
        }
        return database.update(TBL_BUDDY_DETAIL, values, "uid=?", new String[] {userID});
    }

    /**
     * 更新自己的某个信息
     * <br>当为以下信息(pronunciation, status, photo)时，为了其他界面能及时从数据库获取到，会触发observer
     * @param myself
     * @param fieldType
     */
    public void updateMyselfInfo(Buddy myself, int fieldType) {
        String tableName = TBL_BUDDY_DETAIL;
        ContentValues values = new ContentValues();
        switch (fieldType) {
        case Buddy.FIELD_FLAG_PRONUNCIATION:
            values.put("pronunciation", myself.pronunciation);
            // 修改sort_key
            if (!TextUtils.isEmpty(myself.pronunciation)) {
                String sortKey = Utils.makeSortKey(context, myself.pronunciation);
                values.put("sort_key", sortKey);
            }
            break;
        case Buddy.FIELD_FLAG_STATUS:
            values.put("last_status", myself.status);
            break;
        case Buddy.FIELD_FLAG_PHOTO:
            // photo是在获取到服务器的photoUploadTimeStamp保存之后再调次方法，只需要触发Observer即可
            break;
        case Buddy.FIELD_FLAG_PHONE:
            tableName = TBL_BUDDIES;
            values.put("phone_number", myself.phoneNumber);
            break;
        case Buddy.FIELD_FLAG_MOBILE:
            values.put("mobile_phone", myself.mobile);
            break;
        case Buddy.FIELD_FLAG_EMAIL:
            values.put("email", myself.getEmail());
            break;
        case Buddy.FIELD_FLAG_AREA:
            values.put("area", myself.area);
            break;
        default:
            // 如果是非以上几个变量，则直接return
            return;
        }

        if (fieldType == Buddy.FIELD_FLAG_PHOTO) {
            markDBTableModified(DUMMY_TBL_MY_INFO_UPDATED);
        } else {
            int resultCount = database.update(tableName, values, "uid=?", new String[] {myself.userID});
            if ((fieldType == Buddy.FIELD_FLAG_PRONUNCIATION || fieldType == Buddy.FIELD_FLAG_STATUS)
                    && resultCount > 0) {
                markDBTableModified(DUMMY_TBL_MY_INFO_UPDATED);
            }
        }
    }

    /**
	 * Delete a Buddy from database
	 * 
	 * @param ident
	 * @return true if succeed
	 */
	public boolean deleteBuddy(Buddy ident) {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("buddies", "uid='" + ident.userID+"'", null) >= 0;

        markDBTableModified("buddies");

        return ret;
	}

	
	/**
	 * Delete a Buddy from database
	 * 
	 * @param uID
	 * @return true if succeed
	 */
	public boolean deleteBuddyByUID(String uID) {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("buddies", "uid='" + uID+"'", null) >= 0;

        markDBTableModified("buddies");

        return ret;
	}
	

	/**
	 * Delete a Buddy from database
	 * 
	 * @param uID
	 * @return true if succeed
	 */
	public boolean deleteBuddyByPhoneNumber(String strPhoneNumber) {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("buddies", "phone_number='" + strPhoneNumber+"'", null) >= 0;

        markDBTableModified("buddies");

        return ret;
	}
	
	
	/**
	 * Delete all Buddies from database
	 * 
	 * @return
	 */
	public int deleteAllBuddies() {
        if (isDBUnavailable()) {
            return 0;
        }
		int ret= database.delete("buddies", "1", null);

        markDBTableModified("buddies");

        return ret;
	}

    public int deleteAllBuddyDetails() {
        if (isDBUnavailable()) {
            return 0;
        }
        int count = database.delete(TBL_BUDDY_DETAIL, null, null);
        return count;
	}

	/**
	 * Get Buddy from database by UserName
	 * 
	 * @param uid
	 * @return null if not exists
	 */
	public Buddy buddyWithUserID(String uid) {

		if (TextUtils.isEmpty(uid)) {
			return null;
		}
        if (isDBUnavailable()) {
            return null;
        }

        Buddy b = new Buddy(uid);
        return fetchBuddyDetail(b);
	}

    /**
     * Get Buddy from database by PhoneNumber
     *
     * @param strPhoneNumber
     * @return null if not exists
     */
    public Buddy buddyWithPhoneNumber(String strPhoneNumber) {

        if (strPhoneNumber == null) {
            return null;
        }
        if (isDBUnavailable()) {
            return null;
        }

        Cursor cursor = database.rawQuery(
                "SELECT `buddydetail`.`uid` FROM `buddydetail`"
                        + "WHERE `buddies`.`phone_number`='" + strPhoneNumber+"'", null);

        Buddy user=null;

        if (cursor != null && cursor.moveToFirst()) {
            user = new Buddy(cursor.getString(0));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return user == null ? null : fetchBuddyDetail(user);
    }

    /**
     * @param result
     * @return 'result' param on success, or null if:
     *      'result' param is null or 'result.userId' is null, or buddy info not found in db.
     */
    public Buddy fetchBuddyDetail(Buddy result) {

        if (result == null || result.userID == null) {
            return null;
        }
        if (isDBUnavailable()) {
            return null;
        }

        Cursor cursor = database.rawQuery(
                "SELECT"
                        + "`block_list`.`uid` as `block_flag`,`phone_number`,`mobile_phone`,`nickname`,`pronunciation`,`last_status`,`sex`,`device_number`,"
                        + "`app_ver`,`friendship`,`photo_upload_timestamp`,`photo_filepath`,`thumbnail_filepath`,`need_to_download_photo`,`need_to_download_thumbnail`,"
                        + "`buddies`.`uid` as `exist_flag`,"
                        + "`wowtalkid`,`email`,`area`,`job_title`,`employee_id`,`sort_key`,`account_type`, "
                        + "`will_block_msg`,`will_block_msg_notification`,`hidden`,`favorite`,`alias`,`favorite`.`target_id`"
                        + "FROM `buddydetail`"
                        + "LEFT JOIN `buddies` ON `buddydetail`.`uid`= `buddies`.`uid` "
                        + "LEFT JOIN `block_list` ON `buddydetail`.`uid`= `block_list`.`uid` "
                        + "LEFT JOIN `favorite` ON (`buddydetail`.`uid`= `favorite`.`target_id` and `favorite`.`type`=0) "
                        + "WHERE `buddydetail`.`uid`='" + result.userID + "' ", null);


        boolean found = false;

        if (cursor != null && cursor.moveToFirst()) {
            found = true;
            int i = -1;
            result.isBlocked = (cursor.getString(++i) != null);
            result.phoneNumber = cursor.getString(++i);
            result.mobile = cursor.getString(++i);
            result.nickName = cursor.getString(++i);
            result.pronunciation = cursor.getString(++i);
            result.status = cursor.getString(++i);
            result.setSexFlag(cursor.getInt(++i));
            result.deviceNumber = cursor.getString(++i);
            result.appVer = cursor.getString(++i);
            result.setFriendshipWithMe(cursor.getInt(++i));
            result.photoUploadedTimeStamp = cursor.getLong(++i);
            result.pathOfPhoto = cursor.getString(++i);
            result.pathOfThumbNail = cursor.getString(++i);
            result.needToDownloadPhoto = (cursor.getInt(++i) == 1);
            result.needToDownloadThumbnail = (cursor.getInt(++i) == 1);
            result.mayNotExist = (cursor.getString(++i) == null);
            result.username = cursor.getString(++i);
            result.setEmail(cursor.getString(++i));
            result.area = cursor.getString(++i);
            result.jobTitle = cursor.getString(++i);
            result.employeeId = cursor.getString(++i);
            result.sortKey = cursor.getString(++i);
            result.setAccountType(cursor.getInt(++i));
            result.willBlockMsg = cursor.getInt(++i) == 1;
            result.willBlockNotification = cursor.getInt(++i) == 1;
            result.hidden = cursor.getInt(++i) == 1;
            result.favorite = cursor.getInt(++i) == 1;
            result.alias = cursor.getString(++i);
            // `favorite`.`target_id`只要此值不空，说明此buddy为常用联系人(与favorite表关联的)
            result.isFrequent = !TextUtils.isEmpty(cursor.getString(++i));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return found ? result : null;
    }


	/**
	 * Fetch all my buddies from database, excluding pending out ones.
	 * 
	 * @return ArrayList<Buddy>
	 */
	private ArrayList<Buddy> fetchAllBuddies() {
		ArrayList<Buddy> list = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return list;
        }

        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`buddies`.`uid`"
                        + " FROM `buddies`"
                        + " LEFT JOIN `buddydetail` ON `buddies`.`uid`= `buddydetail`.`uid`"
                        + " LEFT JOIN `block_list` ON `buddies`.`uid`= `block_list`.`uid`"
                        + " WHERE 0 != (`friendship` & " + Buddy.RELATIONSHIP_FRIEND_HERE + ")"
                        + " ORDER BY `sort_key`",
                null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				Buddy user = new Buddy(cursor.getString(0));
                if (null != fetchBuddyDetail(user))
                    list.add(user);
			} while (cursor.moveToNext());
		}
		
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

    /** 是否存在发出的好友请求 */
    public boolean hasPendingOutBuddies() {
        ArrayList<PendingRequest> reqs = new ArrayList<>();
        fetchPendingRequest(reqs);
        for (PendingRequest p : reqs) {
            if (p.type == PendingRequest.BUDDY_OUT)
                return true;
        }

        return false;
    }

    /**
     * (Teachers or students) and is_my_friend
     * @return
     */
    public ArrayList<Buddy> fetchNormalBuddies() {
        ArrayList<Buddy> normalBuddy=new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return normalBuddy;
        }

        ArrayList<Buddy> allBuddy=fetchAllBuddies();
        if(null != allBuddy) {
            for(Buddy aBuddy : allBuddy) {
                if((Buddy.ACCOUNT_TYPE_STUDENT == aBuddy.getAccountType()
                        || Buddy.ACCOUNT_TYPE_TEACHER == aBuddy.getAccountType())
                        && 0 != (aBuddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    normalBuddy.add(aBuddy);
                }
            }
        }

        return normalBuddy;
    }

    public ArrayList<Buddy> fetchPublicAccounts() {
        ArrayList<Buddy> list = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return list;
        }

        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`buddies`.`uid`"
                        + " FROM `buddies`"
                        + " LEFT JOIN `buddydetail` ON `buddies`.`uid`= `buddydetail`.`uid`"
                        + " LEFT JOIN `block_list` ON `buddies`.`uid`= `block_list`.`uid`"
                        + " WHERE account_type == " + Buddy.ACCOUNT_TYPE_PUBLIC + ""
                        + " ORDER BY `sort_key`",
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Buddy user = new Buddy(cursor.getString(0));
                if (null != fetchBuddyDetail(user))
                    list.add(user);
            } while (cursor.moveToNext());
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    public ArrayList<Buddy> fetchFamilyBuddies() {
        ArrayList<Buddy> familyBuddy=new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return familyBuddy;
        }

        ArrayList<Buddy> allBuddy=fetchAllBuddies();
        if(null != allBuddy) {
            for(Buddy aBuddy : allBuddy) {
                if(aBuddy.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
                    familyBuddy.add(aBuddy);
                }
            }
        }

        return familyBuddy;
    }

	/**
     * 获取常用联系人列表
     * @return
     */
    public ArrayList<Buddy> fetchFrequentContacts() {
        ArrayList<Buddy> frequentContacts = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return frequentContacts;
        }
        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`buddies`.`uid`"
                        + " FROM `buddies`"
                        + " LEFT JOIN `buddydetail` ON `buddies`.`uid`= `buddydetail`.`uid`"
                        + " LEFT JOIN `block_list` ON `buddies`.`uid`= `block_list`.`uid`"
                        + " JOIN `favorite` ON (`buddies`.`uid`= `favorite`.`target_id` and `favorite`.`type`=0)"
                        + " WHERE 0 != (`friendship` & " + Buddy.RELATIONSHIP_FRIEND_HERE + ")"
                        + " ORDER BY `sort_key`",
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Buddy user = new Buddy(cursor.getString(0));
                if (null != fetchBuddyDetail(user))
                    frequentContacts.add(user);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return frequentContacts;
    }

	/**
	 * Fetch all matched buddies (Buddies) from database
	 * 
	 * @return ArrayList<Buddy>
	 */
	public ArrayList<Buddy> fetchAllMatchedBuddies() {
		ArrayList<Buddy> list = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`buddies`.`uid`, `block_list`.`uid` as `block_flag`,"
                        + "`phone_number`,`nickname`,`last_status`,`sex`,"
                        + "`device_number`,`app_ver`,`friendship`,"
                        + "`photo_upload_timestamp`,`photo_filepath`,"
                        + "`thumbnail_filepath`,`need_to_download_photo`,"
                        + "`need_to_download_thumbnail`,`sort_key`,`account_type`"
                        + " FROM `buddies`"
                        + " LEFT JOIN `buddydetail` ON `buddies`.`uid`= `buddydetail`.`uid`"
                        + " LEFT JOIN `block_list` ON `buddies`.`uid`= `block_list`.`uid`"
                        + " WHERE `friendship`=" + Buddy.RELATIONSHIP_FRIEND_BOTH
                        + " ORDER BY `sort_key`", null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				Buddy user = new Buddy();
				user.userID = cursor.getString(0);
				user.isBlocked = (cursor.getString(1) != null);
				user.phoneNumber = cursor.getString(2);
				user.nickName = cursor.getString(3);
				user.status = cursor.getString(4);
				user.setSexFlag(cursor.getInt(5));
				user.deviceNumber = cursor.getString(6);
				user.appVer = cursor.getString(7);
				user.setFriendshipWithMe(cursor.getInt(8));
				user.photoUploadedTimeStamp = cursor.getLong(9);
				user.pathOfPhoto = cursor.getString(10);
				user.pathOfThumbNail = cursor.getString(11);
				user.needToDownloadPhoto = (cursor.getInt(12) == 1);
				user.needToDownloadThumbnail = (cursor.getInt(13) == 1);
                user.sortKey = cursor.getString(14);
                user.setAccountType(cursor.getInt(15));

				list.add(user);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * Delete all matched buddies (Buddy) from database
	 * 
	 * @return true if succeed
	 */
	public boolean deleteAllMatchedBuddies() {
        if (isDBUnavailable()) {
            return false;
        }
		Log.i("deleteAllMatchedBuddies called");
		boolean ret= database.delete("buddies", "friendship=" + Buddy.RELATIONSHIP_FRIEND_BOTH, null) >= 0;

        markDBTableModified("buddies");

        return ret;
	}

	/**
	 * Fetch all possible buddies (Buddies) from database
	 * 
	 * @return ArrayList<Buddy>
	 */
	public ArrayList<Buddy> fetchAllPossibleBuddies() {
		ArrayList<Buddy> list = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`buddies`.`uid`, `block_list`.`uid` as `block_flag`,"
                        + "`phone_number`,`nickname`,`last_status`,`sex`,"
                        + "`device_number`,`app_ver`,`friendship`,`photo_upload_timestamp`,"
                        + "`photo_filepath`,`thumbnail_filepath`,`need_to_download_photo`,"
                        + "`need_to_download_thumbnail`,`sort_key`,`account_type`"
                        + " FROM `buddies`"
                        + " LEFT JOIN `buddydetail` ON `buddies`.`uid`= `buddydetail`.`uid`"
                        + " LEFT JOIN `block_list` ON `buddies`.`uid`= `block_list`.`uid`"
                        + " WHERE `friendship`<>" + Buddy.RELATIONSHIP_FRIEND_BOTH
                        + " ORDER BY `sort_key`", null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				Buddy user = new Buddy();
				user.userID = cursor.getString(0);
				user.isBlocked = (cursor.getString(1) != null);
				user.phoneNumber = cursor.getString(2);
				user.nickName = cursor.getString(3);
				user.status = cursor.getString(4);
				user.setSexFlag(cursor.getInt(5));
				user.deviceNumber = cursor.getString(6);
				user.appVer = cursor.getString(7);
				user.setFriendshipWithMe(cursor.getInt(8));
				user.photoUploadedTimeStamp = cursor.getLong(9);
				user.pathOfPhoto = cursor.getString(10);
				user.pathOfThumbNail = cursor.getString(11);
				user.needToDownloadPhoto = (cursor.getInt(12) == 1);
				user.needToDownloadThumbnail = (cursor.getInt(13) == 1);
                user.sortKey = cursor.getString(14);
                user.setAccountType(cursor.getInt(15));

				list.add(user);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * Delete all possible buddies (Buddy) from database
	 * 
	 * @return true if succeed
	 */
	public boolean deleteAllPossibleBuddies() {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("buddies", "friendship!=" + Buddy.RELATIONSHIP_FRIEND_BOTH, null) >= 0;

        markDBTableModified("buddies");

        return ret;
	}

	/**
	 * Save thumbnail file cache to local db
	 * 
	 * @param buddy_uid
	 *            : Buddy's userID
	 * @param filepath
	 *            : filepath in local of the Buddy's thumbnail 
	 * @return true if update succeed
	 */
	public boolean setBuddyThumbnailFilePath(String buddy_uid, String filepath) {
        if (isDBUnavailable()) {
            return false;
        }
		ContentValues values = new ContentValues();
		values.put("thumbnail_filepath", filepath);
		values.put("need_to_download_thumbnail", 0);
		boolean ret= database.update("buddydetail", values, "uid='" + buddy_uid+"'", null) > 0;

        markDBTableModified("buddydetail");

        return ret;
	}

	/**
	 * Save photo file cache to local db
	 * 
	 * @param buddy_uid
	 *            : Buddy's userID
	 * @param filepath
	 *            : filepath in local of the Buddy's photo 
	 * @return true if update succeed
	 */
	public boolean setBuddyPhotoFilePath(String buddy_uid, String filepath) {

        if (isDBUnavailable()) {
            return false;
        }
		ContentValues values = new ContentValues();
		values.put("photo_filepath", filepath);
		values.put("need_to_download_photo", 0);
		boolean ret= database.update("buddydetail", values, "uid='" + buddy_uid+"'", null) > 0;

        markDBTableModified("buddydetail");

        return ret;
	}

	/**
	 * Convert a Date object to UTC String with "yyyy/MM/dd HH:mm:ss.SSS" format
	 * 
	 * @param localDate
	 * @return
	 */
	public static String chatMessage_dateToUTCString(Date localDate) {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss.SSS");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = dateFormatter.format(localDate);
		return dateString;
	}

	/**
	 * Convert UTC String with "yyyy/MM/dd HH:mm:ss.SSS" format to a Date object
	 * 
	 * @param string
	 * @return null if exception happen
	 */
	public static Date chatMessage_UTCStringToDate(String string) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss.SSS");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return date;
	}

    /**
     * 将发送时间的long型，截掉前四位后剩下的作为消息的unique_key（暂时的方案）
     * @param utcDateString
     * @return
     */
    public static String chatMessageSentDateToUniqueKey(String utcDateString) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss.SSS");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        int timeKey = 0;
        try {
            Date date = dateFormatter.parse(utcDateString);
            long time = date.getTime();
            timeKey = (int) (time % 1000000000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return String.valueOf(timeKey);
    }

    /**
     * 设置最新的消息时间（用于获取离线消息）
     * @param string
     * @return
     * @see PrefUtil#setLatestMsgTimestamp(long)
     */
    public static long chatMessage_UTCStringToDateLong(String string) {
        long dateLong = -1;
        if (TextUtils.isEmpty(string)) {
            return dateLong;
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss.SSS");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = dateFormatter.parse(string);
            dateLong = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateLong;
    }

	/**
	 * Convert UTC String with "yyyy/MM/dd HH:mm:ss.SSS" format to a String with
	 * "yyyy/MM/dd HH:mm" in local time zone
	 * 
	 * @param string
	 * @return null if exception happen
	 */
	public static String chatMessage_UTCStringToLocalString(String utcString) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss.SSS");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(utcString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		dateFormatter.setTimeZone(TimeZone.getDefault());
		String dateString = dateFormatter.format(date);
		return dateString;
	}

    /**
     * Check wether a ChatMessage object is already in db
     *
     * @param msg
     */
    public boolean isChatMessageInDB(ChatMessage msg) {
        if(msg==null){
            return false;
        }
        if (isDBUnavailable()) {
            return false;
        }

        boolean rst = false;
        Cursor cursor;
        if(msg.isGroupChatMessage){
            cursor = database.query(true, "chatmessages",
                    new String[] { "id" },
                    "chattarget=? AND msgtype=? AND sentdate=? AND group_chat_sender_id=?",
                    new String[] { msg.chatUserName, msg.msgType, msg.sentDate, msg.groupChatSenderID },
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                rst= true;
            }
        } else{
            cursor = database.query(true, "chatmessages",
                    new String[] { "id" },
                    "chattarget=? AND msgtype=? AND sentdate=?",
                    new String[]{ msg.chatUserName, msg.msgType, msg.sentDate},
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                rst= true;
            }
        }

        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        return rst;
    }

	/**
	 * Store a ChatMessage object without observer
	 * <p>收到消息时会调用此方法，但此时的content为未处理过的json格式，在聊天界面显示时出错，所以此处不能触发observer；
	 * 本地发送信息需要保存时，调用{@link storeNewChatMessage(ChatMessage msg, boolean isObserver)}.
	 * @param msg ChatMessage
	 * @return insert row id (use as ChatMessage's primaryKey)
	 */
    public int storeNewChatMessage(ChatMessage msg) {
        return storeNewChatMessage(msg, false);
    }

    public void triggerChatMessageObserver() {
        markDBTableModified(TBL_MESSAGES);
        markDBTableModified(TBL_LATEST_CHAT_TARGET);
    }

    private int storeNewChatMessageWithOffline(ChatMessage msg, boolean isOfflineMsg) {
        int insertKey = -1;
        if (isDBUnavailable()) {
            return insertKey;
        }

        android.util.Log.i(LOG_TAG, "(chatwith:" + msg.chatUserName + ", sentat:" + msg.sentDate + ")");

        // accept a message only if we can display it properly
        String[] acceptableMsgTypes = new String[] {
                ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE,
                ChatMessage.MSGTYPE_LOCATION,
                ChatMessage.MSGTYPE_MULTIMEDIA_STAMP ,
                ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO ,
                ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE ,
                ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE ,
                ChatMessage.MSGTYPE_PIC_VOICE,
                ChatMessage.MSGTYPE_MULTIMEDIA_VCF ,
                ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST ,
                ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM ,
                ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM ,
                ChatMessage.MSGTYPE_CALL_LOG,
                ChatMessage.MSGTYPE_OFFICIAL_ACCOUNT_MSG,
                ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED,
                ChatMessage.MSGTYPE_GET_MISSED_CALL
        };
        int i = 0, n = acceptableMsgTypes.length;
        while (i < n && !acceptableMsgTypes[i].equals(msg.msgType))
            ++i;
        if (i >= n) {
            android.util.Log.w(LOG_TAG, " denied msg type (chatwith:" + msg.chatUserName + ", sentat:" + msg.sentDate + ")");
            return insertKey;
        }

        ContentValues values = new ContentValues();
        values.put("chattarget", msg.chatUserName);
        values.put("display_name", msg.displayName);
        values.put("msgtype", msg.msgType);
        values.put("sentdate", msg.sentDate);
        values.put("iotype", msg.ioType);
        values.put("sentstatus", msg.sentStatus);
        values.put("is_group_chat", msg.isGroupChatMessage ? 1 : 0);
        values.put("group_chat_sender_id", msg.groupChatSenderID);
        values.put("read_count", msg.readCount);
        values.put("path_thumbnail", msg.pathOfThumbNail);
        values.put("path_multimedia", msg.pathOfMultimedia);
        values.put("path_multimedia2", msg.pathOfMultimedia2);
        values.put("msgcontent", msg.messageContent);
        values.put("unique_key", msg.uniqueKey);

        insertKey = (int) database.insert("chatmessages", null, values);

        // 此时的content为未处理过的json格式，在聊天界面显示时出错，所以此处不能触发observer
//        markDBTableModified("chatmessages");
        Log.i("insertID(PrimaryKey) =", String.valueOf(insertKey));

        // 收到/发送消息时，更新最近联系人列表;
        // 接收离线消息/发送消息，才触发observer;
        // 接收非离线消息时，需要在IncomeMessageIntentReceiver#onReceive中处理完成后再触发observer
        if (isOfflineMsg) {
            storeLatestChatTarget(msg.chatUserName, String.valueOf(insertKey), msg.sentDate, msg.isGroupChatMessage, true);
        } else {
            if (ChatMessage.IOTYPE_OUTPUT.equals(msg.ioType)) {
                storeLatestChatTarget(msg.chatUserName, String.valueOf(insertKey), msg.sentDate, msg.isGroupChatMessage, true);
            } else {
                storeLatestChatTarget(msg.chatUserName, String.valueOf(insertKey), msg.sentDate, msg.isGroupChatMessage, false);
                // 收到在线消息时，更新时间戳
                mPrefUtil.setLatestMsgTimestamp(chatMessage_UTCStringToDateLong(msg.sentDate));
            }
        }
        return insertKey;
    }

    /**
     * store a new chat_message triggering the observer
     * @param msg ChatMessage
     * @param isObserver whether trigger the observer
     * @return insert row id (use as ChatMessage's primaryKey)
     */
    public int storeNewChatMessage(ChatMessage msg, boolean isObserver) {
        if (isDBUnavailable()) {
            return -1;
        }
        int primaryKey = storeNewChatMessageWithOffline(msg, false);
        if (isObserver) {
            markDBTableModified("chatmessages");
        }
        return primaryKey;
    }

    /**
     * 保存离线消息
     * @param offlineMsgs
     * @param isObserver
     * @return
     */
    public boolean storeOfflineMessages(ArrayList<ChatMessage> offlineMsgs, boolean isObserver) {
        if (isDBUnavailable()) {
            return false;
        }
        boolean hasInserted = false;
        beginTrasaction();
        for (ChatMessage chatMessage : offlineMsgs) {
            hasInserted |= storeOfflineMessage(chatMessage);
        }
        endTranscation();
        if (isObserver && hasInserted) {
            markDBTableModified("chatmessages");
        }

        return hasInserted;
    }

    private boolean storeOfflineMessage(ChatMessage chatMessage) {
        if (isDBUnavailable()) {
            return false;
        }

        boolean isInserted = false;
        // 如果本地已经有此消息，则不做变动
        if (!isChatMessageInDB(chatMessage)) {
            isInserted = storeNewChatMessageWithOffline(chatMessage, true) > 0;
        }
        return isInserted;
    }


	public int storeChatMessageHistory(ChatMessage msg) {
	    int insertKey = -1;
        if (isDBUnavailable()) {
            return insertKey;
        }

        android.util.Log.i(LOG_TAG, "(chatwith:" + msg.chatUserName + ", sentdate:" + msg.sentDate + ")");

        // accept a message only if we can display it properly
        String[] acceptableMsgTypes = new String[] {
                ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE,
                ChatMessage.MSGTYPE_LOCATION,
                ChatMessage.MSGTYPE_MULTIMEDIA_STAMP ,
                ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO ,
                ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE ,
                ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE ,
                ChatMessage.MSGTYPE_PIC_VOICE,
                ChatMessage.MSGTYPE_MULTIMEDIA_VCF ,
                ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST ,
                ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM ,
                ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM ,
                ChatMessage.MSGTYPE_CALL_LOG,
                ChatMessage.MSGTYPE_OFFICIAL_ACCOUNT_MSG,
                ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED,
                ChatMessage.MSGTYPE_GET_MISSED_CALL
        };
        int i = 0, n = acceptableMsgTypes.length;
        while (i < n && !acceptableMsgTypes[i].equals(msg.msgType))
            ++i;
        if (i >= n) {
            android.util.Log.w(LOG_TAG, " denied msg type (chatwith:" + msg.chatUserName + ", sentat:" + msg.sentDate + ")");
            return insertKey;
        }

        ContentValues values = new ContentValues();
        values.put("chattarget", msg.chatUserName);
        values.put("msgtype", msg.msgType);
        values.put("sentdate", msg.sentDate);
        values.put("iotype", msg.ioType);
        values.put("is_group_chat", msg.isGroupChatMessage ? 1 : 0);
        values.put("group_chat_sender_id", msg.groupChatSenderID);
        values.put("path_thumbnail", msg.pathOfThumbNail);
        values.put("path_multimedia", msg.pathOfMultimedia);
        values.put("path_multimedia2", msg.pathOfMultimedia2);
        values.put("msgcontent", msg.messageContent);
        values.put("local_item", msg.localHistoryId);

        insertKey = (int) database.insert(TBL_MESSAGES_HISTORY, null, values);

        // 此时的content为未处理过的json格式，在聊天界面显示时出错，所以此处不能触发observer
//        markDBTableModified("chatmessages");
        Log.i("insertID(PrimaryKey) =", String.valueOf(insertKey));
	    return insertKey;
	}

	/**
	 * update resource path of message history
	 * @param msg
	 * @return
	 */
    public boolean updateChatMessageHistory(ChatMessage msg, boolean isObserver) {
        if (isDBUnavailable()) {
            return false;
        }
        Log.i("#updateChatMessageHistory", "(id:" + msg.primaryKey);

        ContentValues values = new ContentValues();
        values.put("path_thumbnail", msg.pathOfThumbNail);
        values.put("path_multimedia", msg.pathOfMultimedia);
        values.put("path_multimedia2", msg.pathOfMultimedia2);

        boolean ret= database.update(TBL_MESSAGES_HISTORY, values, "id=" + msg.primaryKey, null) > 0;
        if (isObserver) {
            markDBTableModified(TBL_MESSAGES_HISTORY);
        }
        return  ret;
	}

	/**
	 * 获得指定targetId的聊天记录
	 * @param targetId 聊天对象
	 * @param startLocalItem 数据库local_item对应的大小，用于保存和某人的聊天记录顺序
	 * @param limit 条数
	 * @return
	 */
	public ArrayList<ChatMessage> getMessageHistoryByTargetId(String targetId, int startLocalItem, int limit) {
	    Log.i("getMessageHistoryByTargetId ", "targetId:" + targetId);

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.query(TBL_MESSAGES_HISTORY,
                new String[] { "id",
                    "chattarget", "msgtype", "sentdate", "iotype",
                    "is_group_chat", "group_chat_sender_id",
                    "path_thumbnail", "path_multimedia", "path_multimedia2",
                    "msgcontent"},
                "local_item > ? and chattarget=? AND msgtype<>?",
                new String[] {String.valueOf(startLocalItem), targetId, ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST},
                null,
                null,
                "local_item",
                String.valueOf(limit));

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.msgType = cursor.getString(2);
                msg.sentDate = cursor.getString(3);
                msg.ioType = cursor.getString(4);
                msg.isGroupChatMessage = (cursor.getInt(5) == 1);
                msg.groupChatSenderID = cursor.getString(6);
                msg.pathOfThumbNail = cursor.getString(7);
                msg.pathOfMultimedia = cursor.getString(8);
                msg.pathOfMultimedia2 = cursor.getString(9);
                msg.messageContent = cursor.getString(10);

                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
	}

    public ChatMessage getMessageHistoryByPrimaryKey(String id) {
        if (isDBUnavailable()) {
            return null;
        }
        Log.i("getMessageHistoryByPrimaryKey ", "id:" + id);

        Cursor cursor = database.query(TBL_MESSAGES_HISTORY,
                new String[] { "id",
                    "chattarget", "msgtype", "sentdate", "iotype",
                    "is_group_chat", "group_chat_sender_id",
                    "path_thumbnail", "path_multimedia", "path_multimedia2",
                    "msgcontent"},
                "id=?",
                new String[] {id},
                null,
                null,
                null,
                null);

        ChatMessage msg = null;
        if (cursor != null && cursor.moveToFirst()) {
            msg = new ChatMessage();
            msg.primaryKey = cursor.getInt(0);
            msg.chatUserName = cursor.getString(1);
            msg.msgType = cursor.getString(2);
            msg.sentDate = cursor.getString(3);
            msg.ioType = cursor.getString(4);
            msg.isGroupChatMessage = (cursor.getInt(5) == 1);
            msg.groupChatSenderID = cursor.getString(6);
            msg.pathOfThumbNail = cursor.getString(7);
            msg.pathOfMultimedia = cursor.getString(8);
            msg.pathOfMultimedia2 = cursor.getString(9);
            msg.messageContent = cursor.getString(10);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return msg;
    }

    private ArrayList<ChatMessage> getAllMessageHistory() {
        Log.i("#getAllMessageHistory.");

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.query(TBL_MESSAGES_HISTORY,
                new String[] { "id",
                    "chattarget", "msgtype", "sentdate", "iotype",
                    "is_group_chat", "group_chat_sender_id",
                    "path_thumbnail", "path_multimedia", "path_multimedia2",
                    "msgcontent"},
                null,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.msgType = cursor.getString(2);
                msg.sentDate = cursor.getString(3);
                msg.ioType = cursor.getString(4);
                msg.isGroupChatMessage = (cursor.getInt(5) == 1);
                msg.groupChatSenderID = cursor.getString(6);
                msg.pathOfThumbNail = cursor.getString(7);
                msg.pathOfMultimedia = cursor.getString(8);
                msg.pathOfMultimedia2 = cursor.getString(9);
                msg.messageContent = cursor.getString(10);

                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    public int deleteAllMessageHistoryWithRes() {
        if (isDBUnavailable()) {
            return 0;
        }
        //delete photo/voice/video
        ArrayList<ChatMessage> lst = getAllMessageHistory();
        for(ChatMessage msg : lst) {
            clearMessageResource(msg);
        }

        int count = database.delete(TBL_MESSAGES_HISTORY, null, null);
        return count;
    }

    /**
     * SmsActivity中加载更多时，不需要触发数据库监听
     * @param latestContacts
     * @param isObserver
     */
    public void storeLatestChatTargets(ArrayList<LatestChatTarget> latestContacts,
            boolean isObserver) {
        beginTrasaction();
        for (LatestChatTarget latestContact : latestContacts) {
            storeLatestChatTarget(latestContact.targetId, null,
                    latestContact.sentDate, latestContact.isGroup, false);
        }
        endTranscation();
        if (isObserver) {
            markDBTableModified(DUMMY_TBL_LATEST_CHAT_TARGET_FINISHED);
        }
    }

    /**
     * insert or update latest contacts
     * @param targetId
     * @param messageId
     * @param sentDate
     * @param isGroup
     * @param isObserver
     */
    private void storeLatestChatTarget(String targetId, String messageId,
            String sentDate, boolean isGroup, boolean isObserver) {
        if (isDBUnavailable()) {
            return;
        }
        boolean isUpdate = false;
        String oldSentDate = "";
        Cursor cursor = database.query(TBL_LATEST_CHAT_TARGET,
                new String[]{"sentdate"},
                "target_id=? and is_group=?",
                new String[]{targetId, isGroup ? "1" : "0"},
                null,
                null,
                null);
        if (null != cursor && cursor.moveToFirst()) {
            isUpdate = true;
            oldSentDate = cursor.getString(0);
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }

        // 没有对应的消息时，从本地查找更新此字段
        if (TextUtils.isEmpty(messageId)) {
            Cursor messageIdCursor = database.query(TBL_MESSAGES, new String[] {"id"},
                    "chattarget=?", new String[] {targetId},
                    null, null, "sentdate desc", String.valueOf(1));
            if (null != messageIdCursor) {
                if (messageIdCursor.moveToFirst()) {
                    messageId = messageIdCursor.getString(0);
                }
                if (!messageIdCursor.isClosed()) {
                    messageIdCursor.close();
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put("target_id", targetId);
        values.put("message_id", null == messageId ? "" : messageId);
        values.put("sentdate", sentDate);
        values.put("is_group", isGroup ? 1 : 0);

        if (isUpdate) {
            if (chatMessage_UTCStringToDateLong(sentDate)
                    > chatMessage_UTCStringToDateLong(oldSentDate)) {
                database.update(TBL_LATEST_CHAT_TARGET,
                        values,
                        "target_id=? and is_group=?",
                        new String[]{targetId, isGroup ? "1" : "0"});
            }
        } else {
            database.insert(TBL_LATEST_CHAT_TARGET, null, values);
        }

        if (isObserver) {
            markDBTableModified(TBL_LATEST_CHAT_TARGET);
        }
    }

    public int deleteLatestChatTarget(String targetId) {
        if (isDBUnavailable()) {
            return 0;
        }
        int count = database.delete(TBL_LATEST_CHAT_TARGET, "target_id=?",
                new String[] {targetId});

        markDBTableModified(TBL_LATEST_CHAT_TARGET);
        return count;
    }

    public int clearLatestChatTargets() {
        if (isDBUnavailable()) {
            return 0;
        }
        int count = database.delete(TBL_LATEST_CHAT_TARGET, null, null);

        markDBTableModified(TBL_LATEST_CHAT_TARGET);
        return count;
    }

    public void triggerNoticeManChanged() {
        markDBTableModified(DUMMY_TBL_NOTICE_MAN_CHANGED);
    }

    public void triggerUnreadCount() {
        markDBTableModified(TBL_LATEST_CHAT_TARGET_UNREAD_COUNT);
    }

    public static void deleteAFile(String fileName) {
        try {
            if(!TextUtils.isEmpty(fileName)) {
                File file2del=new File(fileName);
                if(null != file2del && file2del.exists()) {
                    file2del.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearMessageResource(ChatMessage message) {
        //voice->pathOfMultimedia
        if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
            deleteAFile(message.pathOfMultimedia);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)
                || message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
            //path or video->pathOfThumbNail,pathOfMultimedia
            deleteAFile(message.pathOfThumbNail);
            deleteAFile(message.pathOfMultimedia);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)) {
            deleteAFile(message.pathOfThumbNail);
            deleteAFile(message.pathOfMultimedia);
//            deleteAFile(message.getMediaFileID(ChatMessage.HYBIRD_COMPONENT_AUDIO)); // TODO
        }
    }

    /**
	 * Delete all ChatMessages from database
	 * 
	 * @return
	 */
	public int deleteAllChatMessages() {
        if (isDBUnavailable()) {
            return 0;
        }
        //delete photo
        ArrayList<ChatMessage> lst = fetchAllChatMessages(false);
        for(ChatMessage aMsg : lst) {
            clearMessageResource(aMsg);
        }

        int ret= database.delete("chatmessages", "1", null);
        markDBTableModified("chatmessages");

        ContentValues values = new ContentValues();
        values.put("message_id", "");
        database.update(TBL_LATEST_CHAT_TARGET, values, null, null);
        markDBTableModified(TBL_LATEST_CHAT_TARGET);

        return ret;
	}

	/**
	 * Fetch the list of latest ChatMessage (optional: distinct by username)
	 * 
	 * @param isDistinctByUserName
	 * @return
	 */
	public ArrayList<ChatMessage> fetchAllChatMessages(
			boolean isDistinctByUserName) {

		ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor;
		if (isDistinctByUserName) {
            // 分组函数
//            cursor = database.rawQuery("select id,"
//                    + " chattarget,"
//                    + "display_name,msgtype,sentdate,"
//                    + "iotype,sentstatus,is_group_chat,group_chat_sender_id,"
//                    + "read_count,path_thumbnail,path_multimedia,path_multimedia2,"
//                    + "msgcontent,unique_key"
//                    + " from chatmessages"
//                    + " group by chattarget"
//                    + " having msgtype<>'"+ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST+"'"
//                    + " and sentdate in (select max(`sentdate`) from chatmessages group by chattarget)"
//                    + " order by sentdate desc",
//                    null);
            cursor = database.rawQuery("select id,"
                    + " chattarget,"
                    + "display_name,msgtype,sentdate,"
                    + "iotype,sentstatus,is_group_chat,group_chat_sender_id,"
                    + "read_count,path_thumbnail,path_multimedia,path_multimedia2,"
                    + "msgcontent,unique_key"
                    + " from chatmessages"
                    + " where msgtype<>'"+ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST+"'"
                    + " and sentdate in (select max(`sentdate`) from chatmessages group by chattarget)"
                    + " order by sentdate desc,id desc",
                    null);
		} else {
			cursor = database.query("chatmessages", new String[] { "id",
					"chattarget", "display_name", "msgtype", "sentdate",
					"iotype", "sentstatus", "is_group_chat",
					"group_chat_sender_id", "read_count", "path_thumbnail",
					"path_multimedia", "path_multimedia2", "msgcontent", "unique_key" },
					"msgtype<>'" + ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST
							+ "'", null, null, null, null);
		}

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ChatMessage msg = new ChatMessage();
				msg.primaryKey = cursor.getInt(0);
				msg.chatUserName = cursor.getString(1);
				msg.displayName = cursor.getString(2);
				msg.msgType = cursor.getString(3);
				msg.sentDate = cursor.getString(4);
				msg.ioType = cursor.getString(5);
				msg.sentStatus = cursor.getString(6);

				msg.isGroupChatMessage = (cursor.getInt(7) == 1);
				msg.groupChatSenderID = cursor.getString(8);
				msg.readCount = cursor.getInt(9);
				msg.pathOfThumbNail = cursor.getString(10);
				msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
				msg.messageContent = cursor.getString(13);
				msg.uniqueKey = cursor.getString(14);

                // SmsActivity查询信息列表时，需要查询该条信息对应的成员与自己的所有未读信息条数，避免在显示时查询导致数据不一致
                if (isDistinctByUserName) {
                    msg.unreadCount = countUnreadChatMessagesWithUser(msg.chatUserName);
                }

				list.add(msg);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;

	}

    /**
     * 获取最近n个联系人的最后一条信息(SmsActivity)
     * @param limitCount
     * @return
     */
    public ArrayList<ChatMessage> getLatestedMessages(int limitCount) {
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return messages;
        }

        Cursor cursor = database.rawQuery(
                "select temp_chatmessages.id,latest_chat_target.target_id,display_name,msgtype,"
                + " ifnull(temp_chatmessages.sentdate, latest_chat_target.sentdate) as temp_sentdate,"
                + " iotype,sentstatus,latest_chat_target.is_group,"
                + " group_chat_sender_id,read_count,path_thumbnail,"
                + " path_multimedia, path_multimedia2, msgcontent,unique_key"
                + " from latest_chat_target"
                + " left join"
                + "     (select id,chattarget,display_name,msgtype,iotype,"
                + "     sentstatus,group_chat_sender_id,read_count,"
                + "     path_thumbnail,path_multimedia, path_multimedia2, msgcontent,unique_key,"
                + "     max(sentdate) as sentdate"
                + "     from chatmessages"
                + "     group by chattarget) temp_chatmessages"
                + " on latest_chat_target.target_id=temp_chatmessages.chattarget"
                + " where temp_chatmessages.id is not null and (msgtype is null or msgtype<>?)"
                + " order by temp_sentdate desc"
                + " limit ?",
                new String[]{ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                        String.valueOf(limitCount)});
        generateLatestMessagesFromCursor(cursor, messages);
        return messages;
    }

    /**
     * 获取最近联系人的最后一条信息，时间点>=sentDate (SmsActivity调用)
     * @param sentDate
     * @return
     */
    public ArrayList<ChatMessage> getLatestedMessages(String sentDate) {
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return messages;
        }

        Cursor cursor = database.rawQuery(
                "select temp_chatmessages.id,latest_chat_target.target_id,display_name,msgtype,"
                + " ifnull(temp_chatmessages.sentdate, latest_chat_target.sentdate) as temp_sentdate,"
                + " iotype,sentstatus,latest_chat_target.is_group,"
                + " group_chat_sender_id,read_count,path_thumbnail,"
                + " path_multimedia, path_multimedia2, msgcontent,unique_key"
                + " from latest_chat_target"
                + " left join"
                + "     (select id,chattarget,display_name,msgtype,iotype,"
                + "     sentstatus,group_chat_sender_id,read_count,"
                + "     path_thumbnail,path_multimedia, path_multimedia2, msgcontent,unique_key,"
                + "     max(sentdate) as sentdate"
                + "     from chatmessages"
                + "     group by chattarget) temp_chatmessages"
                + " on latest_chat_target.target_id=temp_chatmessages.chattarget"
                + " where ((msgtype is not null and msgtype<>?) or msgtype is null)"
                + " and temp_sentdate>=?"
                + " order by temp_sentdate desc",
                new String[]{ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                        sentDate});
        generateLatestMessagesFromCursor(cursor, messages);
        return messages;
    }

    private void generateLatestMessagesFromCursor(Cursor cursor,
            ArrayList<ChatMessage> messages) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                String primaryKey = cursor.getString(0);
                if (!TextUtils.isEmpty(primaryKey)) {
                    msg.primaryKey = Integer.parseInt(primaryKey);
                };
                msg.chatUserName = cursor.getString(1);
                msg.displayName = cursor.getString(2);
                msg.msgType = cursor.getString(3);
                msg.sentDate = cursor.getString(4);
                msg.ioType = cursor.getString(5);
                msg.sentStatus = cursor.getString(6);

                msg.isGroupChatMessage = (cursor.getInt(7) == 1);
                msg.groupChatSenderID = cursor.getString(8);
                String readCout = cursor.getString(9);
                if (!TextUtils.isEmpty(readCout)) {
                    msg.readCount = Integer.parseInt(readCout);
                }
                msg.pathOfThumbNail = cursor.getString(10);
                msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
                msg.messageContent = cursor.getString(13);
                msg.uniqueKey = cursor.getString(14);

                // SmsActivity查询信息列表时，需要查询该条信息对应的成员与自己的所有未读信息条数，避免在显示时查询导致数据不一致
                msg.unreadCount = countUnreadChatMessagesWithUser(msg.chatUserName);

                messages.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

	/**
	 * Fetch the list of all the group chat invitation
	 * 
	 * @return ArrayList<ChatMessage>
	 */
	public ArrayList<ChatMessage> fetchAllGroupChatInvitaion() {

		ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor;

		cursor = database.query("chatmessages", new String[] { "id",
				"chattarget", "display_name", "msgtype", "sentdate", "iotype",
				"sentstatus", "is_group_chat", "group_chat_sender_id",
				"read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
				"msgcontent", "unique_key" }, "msgtype="
				+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST, null, null, null,
				null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ChatMessage msg = new ChatMessage();
				msg.primaryKey = cursor.getInt(0);
				msg.chatUserName = cursor.getString(1);
				msg.displayName = cursor.getString(2);
				msg.msgType = cursor.getString(3);
				msg.sentDate = cursor.getString(4);
				msg.ioType = cursor.getString(5);
				msg.sentStatus = cursor.getString(6);

				msg.isGroupChatMessage = (cursor.getInt(7) == 1);
				msg.groupChatSenderID = cursor.getString(8);
				msg.readCount = cursor.getInt(9);
				msg.pathOfThumbNail = cursor.getString(10);
				msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
				msg.messageContent = cursor.getString(13);
				msg.uniqueKey = cursor.getString(14);

				list.add(msg);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;

	}

    /**
     * 获取所有的群组的总数, 用于判断数据库有没有数据，是否需要触发下载部门和成员
     * @return
     */
    public int getCountsOfGroups() {
        int count = 0;
        if (isDBUnavailable()) {
            return count;
        }
        Cursor cursor = database.query(TBL_GROUP,
                new String[]{"count(*)"}, null, null, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        return count;
    }

	/**
	 * Fetch the list of ChatMessage which contains the specified string
	 * 
	 * @param searchMsgText
	 * @return ArrayList<ChatMessage>
	 */
	public ArrayList<ChatMessage> searchChatMessagesByContent(
			String searchMsgText) {

		ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor = database.query("chatmessages", new String[] { "id",
				"chattarget", "display_name", "msgtype", "sentdate", "iotype",
				"sentstatus", "is_group_chat", "group_chat_sender_id",
				"read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
				"msgcontent", "unique_key" }, "msgcontent LIKE '%"
				+ searchMsgText + "%'", null, "chattarget", null, null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ChatMessage msg = new ChatMessage();
				msg.primaryKey = cursor.getInt(0);
				msg.chatUserName = cursor.getString(1);
				msg.displayName = cursor.getString(2);
				msg.msgType = cursor.getString(3);
				msg.sentDate = cursor.getString(4);
				msg.ioType = cursor.getString(5);
				msg.sentStatus = cursor.getString(6);

				msg.isGroupChatMessage = (cursor.getInt(7) == 1);
				msg.groupChatSenderID = cursor.getString(8);
				msg.readCount = cursor.getInt(9);
				msg.pathOfThumbNail = cursor.getString(10);
				msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
				msg.messageContent = cursor.getString(13);
				msg.uniqueKey = cursor.getString(14);

				list.add(msg);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;

	}

    /**
     * fetch ChatMessages(the last limit and offset, it's desc, but the returned list is asc)
     *
     * @param userID
     *            /groupID
     * @param limit
     * @param offset
     * @return
     */
    public ArrayList<ChatMessage> fetchChatMessagesWithUser(String userID,
            int limit, int offset) {
        Log.i("fetchChatMessagesWithUser ", "uid:" + userID + " limit:" + limit
                + " offset:" + offset);

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }

        Cursor cursor = database.rawQuery("SELECT chatmessages.id,chattarget,"
                + "     display_name,msgtype,sentdate,iotype,sentstatus,is_group_chat,"
                + "     group_chat_sender_id,msg_readed.read_count,path_thumbnail,path_multimedia,path_multimedia2,"
                + "     msgcontent,chatmessages.unique_key"
                + " FROM chatmessages "
                + " left join "
                + " (select count(*) as read_count,unique_key from chatmessage_readed group by unique_key) as msg_readed"
                + " on chatmessages.unique_key=msg_readed.unique_key "
                + " where chattarget=? and msgtype<>?"
                + " order by sentdate desc,id desc"
                + " limit ?,?",
                new String[] {userID, ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                        String.valueOf(offset), String.valueOf(limit)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.displayName = cursor.getString(2);
                msg.msgType = cursor.getString(3);
                msg.sentDate = cursor.getString(4);
                msg.ioType = cursor.getString(5);
                msg.sentStatus = cursor.getString(6);
                msg.isGroupChatMessage = (cursor.getInt(7) == 1);
                msg.groupChatSenderID = cursor.getString(8);
                msg.readCount = cursor.getInt(9);
                msg.pathOfThumbNail = cursor.getString(10);
                msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
                msg.messageContent = cursor.getString(13);
                msg.uniqueKey = cursor.getString(14);

                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        Collections.reverse(list);
        return list;
    }

	/**
	 * fetch ChatMessages by userID/groupID
	 * 
	 * @param userID
	 *            /groupID
	 * @return
	 */
	public ArrayList<ChatMessage> fetchChatMessagesWithUser(String userID) {
		Log.i("fetchChatMessagesWithUser ", "userID:" + userID);

		ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor = database.query("chatmessages", new String[] { "id",
				"chattarget", "display_name", "msgtype", "sentdate", "iotype",
				"sentstatus", "is_group_chat", "group_chat_sender_id",
				"read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
				"msgcontent", "unique_key" }, "chattarget='" + userID
				+ "' AND msgtype<>'"
				+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'", null, null,
				null,
				"sentdate");

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ChatMessage msg = new ChatMessage();
				msg.primaryKey = cursor.getInt(0);
				msg.chatUserName = cursor.getString(1);
				msg.displayName = cursor.getString(2);
				msg.msgType = cursor.getString(3);
				msg.sentDate = cursor.getString(4);
				msg.ioType = cursor.getString(5);
				msg.sentStatus = cursor.getString(6);

				msg.isGroupChatMessage = (cursor.getInt(7) == 1);
				msg.groupChatSenderID = cursor.getString(8);
				msg.readCount = cursor.getInt(9);
				msg.pathOfThumbNail = cursor.getString(10);
				msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
				msg.messageContent = cursor.getString(13);
				msg.uniqueKey = cursor.getString(14);

				list.add(msg);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

    public ArrayList<ChatMessage> fetchChatMessagesAfterInclude(String userID, String sentDate) {
        Log.i("fetchChatMessagesAfter ", "uid:" + userID + ", sentDate:" + sentDate);

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }

        Cursor cursor = database.rawQuery("SELECT chatmessages.id,chattarget,"
                + "     display_name,msgtype,sentdate,iotype,sentstatus,is_group_chat,"
                + "     group_chat_sender_id,msg_readed.read_count,path_thumbnail,path_multimedia,path_multimedia2,"
                + "     msgcontent,chatmessages.unique_key"
                + " FROM chatmessages "
                + " left join "
                + " (select count(*) as read_count,unique_key from chatmessage_readed group by unique_key) as msg_readed"
                + " on chatmessages.unique_key=msg_readed.unique_key "
                + " where chattarget=? and msgtype<>? and sentdate>=?"
                + " order by sentdate desc,id desc",
                new String[] {userID,
                        ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                        sentDate});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.displayName = cursor.getString(2);
                msg.msgType = cursor.getString(3);
                msg.sentDate = cursor.getString(4);
                msg.ioType = cursor.getString(5);
                msg.sentStatus = cursor.getString(6);
                msg.isGroupChatMessage = (cursor.getInt(7) == 1);
                msg.groupChatSenderID = cursor.getString(8);
                msg.readCount = cursor.getInt(9);
                msg.pathOfThumbNail = cursor.getString(10);
                msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
                msg.messageContent = cursor.getString(13);
                msg.uniqueKey = cursor.getString(14);

                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        Collections.reverse(list);
        return list;
    }

    public ArrayList<ChatMessage> fetchChatMessagesEarlier(String userID,
            String sentDate, int limit) {
        Log.i("fetchChatMessagesEarlier ", "uid:" + userID + ", sentDate:" + sentDate
                + ", limit:" + limit);

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }

        Cursor cursor = database.rawQuery("SELECT chatmessages.id,chattarget,"
                + "     display_name,msgtype,sentdate,iotype,sentstatus,is_group_chat,"
                + "     group_chat_sender_id,msg_readed.read_count,path_thumbnail,path_multimedia,path_multimedia2,"
                + "     msgcontent,chatmessages.unique_key"
                + " FROM chatmessages "
                + " left join "
                + " (select count(*) as read_count,unique_key from chatmessage_readed group by unique_key) as msg_readed"
                + " on chatmessages.unique_key=msg_readed.unique_key "
                + " where chattarget=? and msgtype<>? and sentdate<?"
                + " order by sentdate desc,id desc"
                + " limit ?",
                new String[] {userID, ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                        sentDate, String.valueOf(limit)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.displayName = cursor.getString(2);
                msg.msgType = cursor.getString(3);
                msg.sentDate = cursor.getString(4);
                msg.ioType = cursor.getString(5);
                msg.sentStatus = cursor.getString(6);

                msg.isGroupChatMessage = (cursor.getInt(7) == 1);
                msg.groupChatSenderID = cursor.getString(8);
                msg.readCount = cursor.getInt(9);
                msg.pathOfThumbNail = cursor.getString(10);
                msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
                msg.messageContent = cursor.getString(13);
                msg.uniqueKey = cursor.getString(14);

                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        Collections.reverse(list);
        return list;
    }

	/**
	 * fetch Unread ChatMessages by userID/groupID
	 * 
	 * @param userID
	 *            /groupID
	 * @return ArrayList<ChatMessage>
	 */
	public ArrayList<ChatMessage> fetchUnreadChatMessagesWithUser(String userID) {
		Log.i("fetchUnreadChatMessagesWithUser ", "userID:" + userID);

		ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor = database.query("chatmessages", new String[] { "id",
				"chattarget", "display_name", "msgtype", "sentdate", "iotype",
				"sentstatus", "is_group_chat", "group_chat_sender_id",
				"read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
				"msgcontent", "unique_key" }, "chattarget='" + userID
				+ "' AND msgtype<>'"
				+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'"
				+ " AND iotype=" + ChatMessage.IOTYPE_INPUT_UNREAD, null, null,
				null,
				// "sentdate");
				"id");

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ChatMessage msg = new ChatMessage();
				msg.primaryKey = cursor.getInt(0);
				msg.chatUserName = cursor.getString(1);
				msg.displayName = cursor.getString(2);
				msg.msgType = cursor.getString(3);
				msg.sentDate = cursor.getString(4);
				msg.ioType = cursor.getString(5);
				msg.sentStatus = cursor.getString(6);

				msg.isGroupChatMessage = (cursor.getInt(7) == 1);
				msg.groupChatSenderID = cursor.getString(8);
				msg.readCount = cursor.getInt(9);
				msg.pathOfThumbNail = cursor.getString(10);
				msg.pathOfMultimedia = cursor.getString(11);
                msg.pathOfMultimedia2 = cursor.getString(12);
				msg.messageContent = cursor.getString(13);
				msg.uniqueKey = cursor.getString(14);

				list.add(msg);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * fetch ChatMessages by primaryKey
	 * 
	 * @param primaryKey
	 * @return ChatMessage
	 */
	public ChatMessage fetchChatMessageByPrimaryKey(int primaryKey) {
		Log.i("fetchChatMessageByPrimaryKey ", "primaryKey:" + primaryKey);

        if (isDBUnavailable()) {
            return null;
        }
		Cursor cursor = database.query("chatmessages", new String[] { "id",
				"chattarget", "display_name", "msgtype", "sentdate", "iotype",
				"sentstatus", "is_group_chat", "group_chat_sender_id",
				"read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
				"msgcontent", "unique_key" }, "id='" + primaryKey
				+ "' AND msgtype<>'"
				+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'", null, null,
				null, null);

		ChatMessage msg = null;
		if (cursor != null && cursor.moveToFirst()) {

			msg = new ChatMessage();
			msg.primaryKey = cursor.getInt(0);
			msg.chatUserName = cursor.getString(1);
			msg.displayName = cursor.getString(2);
			msg.msgType = cursor.getString(3);
			msg.sentDate = cursor.getString(4);
			msg.ioType = cursor.getString(5);
			msg.sentStatus = cursor.getString(6);

			msg.isGroupChatMessage = (cursor.getInt(7) == 1);
			msg.groupChatSenderID = cursor.getString(8);
			msg.readCount = cursor.getInt(9);
			msg.pathOfThumbNail = cursor.getString(10);
			msg.pathOfMultimedia = cursor.getString(11);
            msg.pathOfMultimedia2 = cursor.getString(12);
			msg.messageContent = cursor.getString(13);
			msg.uniqueKey = cursor.getString(14);

		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return msg;
	}

    /**
     * fetch the chat message via unique_key
     * @param uniqueKey
     * @return
     */
    public ChatMessage fetchChatMessageByUniqueKey(String uniqueKey) {
        Log.i("fetchChatMessageByUniqueKey ", "uniqueKey:" + uniqueKey);

        if (isDBUnavailable()) {
            return null;
        }
        Cursor cursor = database.query("chatmessages", new String[] { "id",
                "chattarget", "display_name", "msgtype", "sentdate", "iotype",
                "sentstatus", "is_group_chat", "group_chat_sender_id",
                "read_count", "path_thumbnail", "path_multimedia", "path_multimedia2",
                "msgcontent", "unique_key" },
                "unique_key=? AND msgtype<>?",
                new String[]{ uniqueKey, ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST},
                null, null, null);

        ChatMessage msg = null;
        if (cursor != null && cursor.moveToFirst()) {
            msg = new ChatMessage();
            msg.primaryKey = cursor.getInt(0);
            msg.chatUserName = cursor.getString(1);
            msg.displayName = cursor.getString(2);
            msg.msgType = cursor.getString(3);
            msg.sentDate = cursor.getString(4);
            msg.ioType = cursor.getString(5);
            msg.sentStatus = cursor.getString(6);
            msg.isGroupChatMessage = (cursor.getInt(7) == 1);
            msg.groupChatSenderID = cursor.getString(8);
            msg.readCount = cursor.getInt(9);
            msg.pathOfThumbNail = cursor.getString(10);
            msg.pathOfMultimedia = cursor.getString(11);
            msg.pathOfMultimedia2 = cursor.getString(12);
            msg.messageContent = cursor.getString(13);
            msg.uniqueKey = cursor.getString(14);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return msg;
    }

	/**
	 * get the totol number of the ChatMessages with userID/groupID
	 * 
	 * @param userID
	 *            /groupID
	 * @return
	 */
	public int countChatMessagesWithUser(String userID) {
		int recordNumber = 0;
        if (isDBUnavailable()) {
            return recordNumber;
        }

		Cursor cursor = database.query("chatmessages",
				new String[] { "COUNT(`id`)" }, "chattarget='" + userID
						+ "' AND msgtype<>'"
						+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'",
				null, null, null, null);

		if (cursor.moveToFirst()) {
			recordNumber = cursor.getInt(0);

		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return recordNumber;

	}

	/**
	 * get the totol number of the unread ChatMessages with userID/groupID
	 * 
	 * @param userID
	 *            /groupID
	 * @return
	 */
	public int countUnreadChatMessagesWithUser(String userID) {
		int recordNumber = 0;
        if (isDBUnavailable()) {
            return recordNumber;
        }

		Cursor cursor = database.query("chatmessages",
				new String[] { "COUNT(`id`)" }, "chattarget='" + userID
						+ "' AND msgtype<>'"
						+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'"
						+ " AND iotype=" + ChatMessage.IOTYPE_INPUT_UNREAD,
				null, null, null, null);

		if (cursor.moveToFirst()) {
			recordNumber = cursor.getInt(0);

		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return recordNumber;

	}
	
	/**
	 * get the totol number of the unread ChatMessage
	 * 
	 * @return
	 */
	public int countAllUnreadChatMessages() {
		int recordNumber = 0;
        if (isDBUnavailable()) {
            return recordNumber;
        }

		Cursor cursor = database.query("chatmessages",
				new String[] { "COUNT(`id`)" }, "msgtype<>'"
						+ ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST + "'"
						+ " AND iotype=" + ChatMessage.IOTYPE_INPUT_UNREAD,
				null, null, null, null);

		if (cursor != null&& cursor.moveToFirst()) {
			recordNumber = cursor.getInt(0);

		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return recordNumber;

	}

    /**
     * 获取n个最近联系人的未读信息条数
     * @param countTargets
     * @return
     */
    public int countUnreadMessagesByTargets(int countTargets) {
        int recordNumber = 0;
        if (isDBUnavailable()) {
            return recordNumber;
        }

        String sql = "select count(*)"
                + " from chatmessages"
                + " where chattarget in"
                + "         (select target_id from latest_chat_target"
                + "         order by sentdate desc limit ?)"
                + " and msgtype<>? and iotype=?";
        String[] selectionArgs = new String[] {String.valueOf(countTargets),
                ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST,
                ChatMessage.IOTYPE_INPUT_UNREAD};
        Cursor cursor = database.rawQuery(sql, selectionArgs);

        if (cursor != null&& cursor.moveToFirst()) {
            recordNumber = cursor.getInt(0);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return recordNumber;
    }


	/**
	 * set all the ChatMessages with userID/groupID readed
	 * 
	 * @param userID
	 *            /groupID
	 * @return
	 */
	public int setChatMessageAllReaded(String userID) {
        if (isDBUnavailable()) {
            return 0;
        }
		ContentValues values = new ContentValues();
		values.put("iotype", ChatMessage.IOTYPE_INPUT_READED);
		int ret= database.update("chatmessages", values, "chattarget='" + userID
				+ "' AND iotype="+ChatMessage.IOTYPE_INPUT_UNREAD, null);

        if (ret > 0) {
            markDBTableModified("chatmessages");
            markDBTableModified(DUMMY_TBL_CHAT_MESSAGES_READED);
        }

        return ret;
	}
	

	/**
	 * set the ChatMessage with userID/groupID readed
	 * 
	 * @param userID
	 *            /groupID
	 * @return
	 */
	public int setChatMessageReaded(ChatMessage msg) {
        if (isDBUnavailable()) {
            return 0;
        }
		ContentValues values = new ContentValues();
		values.put("iotype", ChatMessage.IOTYPE_INPUT_READED);
		int ret= database.update("chatmessages", values, "id=" + msg.primaryKey
				, null);

        markDBTableModified("chatmessages");

        return ret;
	}

	

	/**
	 * Set ChatMessage cannot be sent
	 * 
	 * @param msg
	 * @return
	 */
	public boolean setChatMessageCannotSent(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
		ContentValues values = new ContentValues();
		values.put("sentstatus", ChatMessage.SENTSTATUS_NOTSENT);
		boolean ret= database.update("chatmessages", values, "unique_key=" + msg.uniqueKey,
				null) > 0;

        markDBTableModified("chatmessages");

        return ret;
	}

	/**
	 * Set ChatMessage resent is successful
	 * 
	 * @param msg
	 * @return
	 */
	public boolean setChatMessageSent(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }

        Cursor cursor = database.query("chatmessages",
                new String[] { "sentstatus" },
                "unique_key=?",
                new String[] {msg.uniqueKey} ,
                null, null, "sentdate desc");

        boolean rlt = false;
        if (cursor != null && cursor.moveToFirst()) {
            int oldSentStatus = cursor.getInt(0);
            if (!ChatMessage.SENTSTATUS_READED_BY_CONTACT.equals(String.valueOf(oldSentStatus))
                    && !ChatMessage.SENTSTATUS_SENT.equals(String.valueOf(oldSentStatus))) {
                ContentValues values = new ContentValues();
                values.put("sentstatus", ChatMessage.SENTSTATUS_SENT);
                values.put("sentdate", msg.sentDate);
                rlt = database.update("chatmessages", values, "unique_key=" + msg.uniqueKey,
                        null) > 0;
                markDBTableModified("chatmessages");
            }
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return rlt;
    }


    private boolean setChatMessageForNewSentStatus(ChatMessage msg,
                                                   String newSentStatus, boolean updateSentDate) {

        if (isDBUnavailable()) {
            return false;
        }
        Cursor cursor = database.query("chatmessages",
                new String[] { "sentstatus" }, "id=" + msg.primaryKey, null,
                null, null, "id");

        boolean rlt = false;
        if (cursor != null && cursor.moveToFirst()) {

            int oldSentStatus = cursor.getInt(0);
            if (oldSentStatus < Integer.valueOf(newSentStatus)) {
                ContentValues values = new ContentValues();
                values.put("sentstatus", newSentStatus);
                if (updateSentDate)
                    values.put("sentdate", msg.sentDate);
                rlt = database.update("chatmessages", values, "id="
                        + msg.primaryKey, null) > 0;

                markDBTableModified("chatmessages");

            }

        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return rlt;

    }

    /**
     * Set ChatMessage is being sent
     *
     * @param msg
     * @return
     */
    public boolean setChatMessageInProgress(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
        return this.setChatMessageForNewSentStatus(msg,
                ChatMessage.SENTSTATUS_IN_PROCESS, false);
    }

	/**
	 * Set ChatMessage reached contact
	 * 
	 * @param msg
	 * @return
	 */
	public boolean setChatMessageReachedContact(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
		ContentValues values = new ContentValues();
		values.put("sentstatus", ChatMessage.SENTSTATUS_REACHED_CONTACT);
		boolean ret= database.update("chatmessages", values, "id=" + msg.primaryKey+ " AND sentstatus<>" + ChatMessage.SENTSTATUS_READED_BY_CONTACT,
				null) > 0;

        markDBTableModified("chatmessages");

        return ret;
	}

	/**
	 * Set ChatMessage readed by contact
	 * 
	 * @param msg
	 * @return
	 * @deprecated
	 */
	public boolean setChatMessageReadedByContact(ChatMessage msg) {
		Log.i("setChatMessageReadedByContact called for msg unique_key:", msg.uniqueKey);

        if (isDBUnavailable()) {
            return false;
        }

		int cnt = 0;
		Cursor cursor = database.query("chatmessages",
				new String[] { "read_count" }, "id=" + msg.primaryKey, null,
				null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			cnt = cursor.getInt(0);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		cnt++;

		ContentValues values = new ContentValues();
		values.put("sentstatus", ChatMessage.SENTSTATUS_READED_BY_CONTACT);
		values.put("read_count", cnt);

		boolean ret= database.update("chatmessages", values, "id=" + msg.primaryKey,
				null) > 0;

        markDBTableModified("chatmessages");

        return ret;
	}

    /**
     * 单人聊天时，对方已读回执
     * @param uniqueKey
     * @param buddyId
     * @return
     */
    public boolean setChatMessageReadedByContact(String uniqueKey, String buddyId) {
        Log.i("#setChatMessageReadedByContact, msg unique_key " +  uniqueKey
                + ", read by buddy_id " + buddyId);
        if (isDBUnavailable() || TextUtils.isEmpty(uniqueKey) || TextUtils.isEmpty(buddyId)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("sentstatus", ChatMessage.SENTSTATUS_READED_BY_CONTACT);

        boolean ret= database.update("chatmessages",
                values,
                "unique_key=? and chattarget=? and iotype=?",
                new String[]{ uniqueKey, buddyId, ChatMessage.IOTYPE_OUTPUT }) > 0;

        if (ret) {
            markDBTableModified("chatmessages");
        }
        return ret;
    }

    /**
     * 多人聊天时，某个成员已读回执
     * @param uniqueKey
     * @param memberId
     */
    public void setChatMessageReadedByGroupMember(String uniqueKey, String memberId) {
        Log.i("#setChatMessageReadedByGroupMember, msg unique_key " + uniqueKey
                + ", read by member_id " + memberId);

        if (isDBUnavailable() || TextUtils.isEmpty(uniqueKey) || TextUtils.isEmpty(memberId)) {
            return;
        }

        // 只要有成员读过此消息，则状态变为"已读"，计数则从TBL_CHATMESSAGE_READED获得
        ContentValues chatmessageValues = new ContentValues();
        chatmessageValues.put("sentstatus", ChatMessage.SENTSTATUS_READED_BY_CONTACT);
        database.update("chatmessages",
                chatmessageValues,
                "unique_key=? and iotype=?",
                new String[]{ uniqueKey, ChatMessage.IOTYPE_OUTPUT });

        Cursor cursor = database.query(TBL_CHATMESSAGE_READED,
                null,
                "unique_key=? and member_id=?",
                new String[]{ uniqueKey, memberId },
                null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            if (!cursor.isClosed()) {
                cursor.close();
            }
            return;
        }

        ContentValues values = new ContentValues();
        values.put("unique_key", uniqueKey);
        values.put("member_id", memberId);
        boolean isSuccess = database.insert(TBL_CHATMESSAGE_READED, null, values) > 0;
        if (isSuccess) {
            markDBTableModified(TBL_CHATMESSAGE_READED);
        }
        return;
    }

	/**
	 * Delete ChatMessage
	 * 
	 * @param msg
	 * @return
	 */
	public boolean deleteChatMessage(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
		if (msg.primaryKey > 0) {
			boolean ret= database
					.delete("chatmessages", "id=" + msg.primaryKey, null) > 0;
            markDBTableModified("chatmessages");

            return ret;
        }

		return false;
	}

	/**
	 * Delete all ChatMessages with specified user/group
	 * 
	 * @param userID /groupID
	 * @return total number of unreaded message with the specified user
	 */
	public int deleteChatMessageWithUser(String userID) {
        if (isDBUnavailable()) {
            return 0;
        }
        List<ChatMessage> lst=fetchChatMessagesWithUser(userID);
        for(ChatMessage aMsg : lst) {
            clearMessageResource(aMsg);
        }

		int unReadedRecordNumber = database.delete("chatmessages",
				"iotype=2 AND chattarget='" + userID + "'", null);
		unReadedRecordNumber = unReadedRecordNumber > 0 ? unReadedRecordNumber
				: 0;

		database.delete("chatmessages", "chattarget='" + userID + "'", null);

        markDBTableModified("chatmessages");

        ContentValues values = new ContentValues();
        values.put("message_id", "");
        database.update(TBL_LATEST_CHAT_TARGET, values, "target_id=?", new String[] {userID});
        markDBTableModified(TBL_LATEST_CHAT_TARGET);

		return unReadedRecordNumber;
	}

    /**
     * 新登录后，群组成员加载完成，提示监听此变化的对象
     */
    public void finishLoadGroupMembersPerfectly() {
        markDBTableModified(DUMMY_TBL_FINISH_LOAD_MEMBERS);
    }

    /**
     * 在MultiSelectActivity中判断是否好友加载完成的监听
     */
    public void finishLoadBuddiesAfterLogin() {
        markDBTableModified(DUMMY_TBL_FINISH_LOAD_CONTACTS);
    }

	/**
	 * Update ChatMessage manually: e.g. after file(multimedia) transfered with server
	 * 
	 * @param msg
	 * @param isObserver
	 * @return true if succeed
	 */
	public boolean updateChatMessage(ChatMessage msg, boolean isObserver) {
        if (isDBUnavailable()) {
            return false;
        }
	    boolean ret= updateChatMessage(msg);

        if (isObserver) {
            markDBTableModified("chatmessages");
        }
        return ret;
	}

    /**
     * update chat message, without observer
     *
     * @param msg
     * @return
     */
    public boolean updateChatMessage(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
        Log.i("updateChatMessage", "(id:" + msg.primaryKey);

        ContentValues values = new ContentValues();
        values.put("chattarget", msg.chatUserName);
        values.put("display_name", msg.displayName);
        values.put("msgtype", msg.msgType);
        values.put("sentdate", msg.sentDate);
        values.put("iotype", msg.ioType);
        values.put("sentstatus", msg.sentStatus);

        values.put("is_group_chat", msg.isGroupChatMessage ? 1 : 0);
        values.put("group_chat_sender_id", msg.groupChatSenderID);
        values.put("read_count", msg.readCount);
        values.put("path_thumbnail", msg.pathOfThumbNail);
        values.put("path_multimedia", msg.pathOfMultimedia);
        values.put("path_multimedia2", msg.pathOfMultimedia2);
        values.put("msgcontent", msg.messageContent);
        values.put("unique_key", msg.uniqueKey);

        boolean ret= database.update("chatmessages", values, "id=" + msg.primaryKey, null) > 0;
        return  ret;
    }

	/**
	 * update displayName of chatmessages with userId.
	 * @param userId userid
	 * @param displayName displayName of chatmessages
	 * @return
	 */
	public void updateChatMessageDisplayNameWithUser(String userId, String displayName) {
        if (isDBUnavailable()) {
            return;
        }
        updateChatMessageDisplayNameWithUser(userId, displayName, true);
    }

	public void updateChatMessageDisplayNameWithUser(String userId, String displayName, boolean isObserver) {

        if (isDBUnavailable()) {
            return;
        }
        Log.i("updateChatMessagesWithUser", "(userId:" + userId);
        if(TextUtils.isEmpty(userId)) {
            Log.w("userId empty");
            return;
        }

        ContentValues values = new ContentValues();
        values.put("display_name", displayName);

        database.update("chatmessages", values, "chattarget = ? ", new String[]{userId});

        // 我被加入某个群组时，不需要再触发；因为保存群组后会继续update chat message
        if (isObserver) {
            markDBTableModified(TBL_LATEST_CHAT_TARGET);
        }
    }

	/**
	 * Convert a Date object to a UTC String with "yyyy/MM/dd HH:mm:ss.SSS" format
	 * 
	 * @param (Date)localDate
	 * @return
	 */
	public static String callLog_dateToUTCString(Date localDate) {
        if(null == localDate) {
            localDate=new Date();
        }
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss.SSS");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = dateFormatter.format(localDate);
		return dateString;
	}

	/**
	 * Convert a UTC String with "yyyy/MM/dd HH:mm:ss" format to a Date object
	 * 
	 * @param string
	 * @return
	 */
	public static Date callLog_UTCStringToDate(String string) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss.SSS");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return date;
	}

	/**
	 * Store a new CallLog to local db
	 * 
	 * @param log
	 * @return
	 */
	public int storeNewCallLog(CallLog log) {
        if (isDBUnavailable()) {
            return 0;
        }
		Log.i("storeNewCallLog:", log.contact + "," + log.startDate);

		ContentValues values = new ContentValues();
		values.put("contact", log.contact);
		values.put("display_name", log.displayName);
		values.put("callstatus", log.status);
		values.put("direction", log.direction);
		values.put("startdate", log.startDate);
		values.put("duration", log.duration);
		values.put("quality", log.quality);

		int ret=(int)database.insert("calllogs", null, values);

        markDBTableModified("calllogs");

        return ret;

	}

	/**
	 * Delete all CallLogs in local db
	 * 
	 * @return
	 */
	public boolean deleteAllCallLogs() {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("calllogs", "1", null) >= 0;

        markDBTableModified("calllogs");

        return ret;
	}

	/**
	 * 
	 * Get all CallLogs from local db
	 * 
	 * @return ArrayList<CallLog>
	 */
	public ArrayList<CallLog> fetchAllCallLogs() {

		ArrayList<CallLog> list = new ArrayList<CallLog>();
        if (isDBUnavailable()) {
            return list;
        }
		Cursor cursor = database.query("calllogs", new String[] { "id",
				"contact","display_name", "callstatus", "direction", "startdate", "duration",
				"quality" }, null, null, null, null, "startdate desc");
		if (cursor.moveToFirst()) {
			do {
				CallLog log = new CallLog();
				log.primaryKey = cursor.getInt(0);
				log.contact = cursor.getString(1);
				log.displayName = cursor.getString(2);
				log.status = Integer.parseInt(cursor.getString(3));
				log.direction = cursor.getString(4);
				log.startDate = cursor.getString(5);
				log.duration = Integer.parseInt(cursor.getString(6));
				log.quality = Integer.parseInt(cursor.getString(7));
				list.add(log);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * Delete CallLog
	 * 
	 * @param log
	 * @return
	 */
	public boolean deleteCallLog(CallLog log) {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("calllogs", "id=" + log.primaryKey, null) >= 0;

        markDBTableModified("calllogs");

        return ret;

	}

	// /////////////////////////////////////////////////////
	// GroupChat

    public boolean storeGroupChatRooms(ArrayList<GroupChatRoom> groupRooms, boolean isMeBelongs) {
        beginTrasaction();
        for (GroupChatRoom groupChatRoom : groupRooms) {
            storeGroupChatRoom(groupChatRoom, true, isMeBelongs);
        }
        endTranscation();
        return true;
    }

	/**
	 * Store a GroupChatRoom to local DB
	 * 
	 * @param groupChatRoom
	 * @return
	 */
	public boolean storeGroupChatRoom(GroupChatRoom groupChatRoom) {
        if (isDBUnavailable()) {
            return false;
        }
		return storeGroupChatRoom(groupChatRoom, true, true);
	}

	/**
	 * 保存群组，默认自己属于此群组；biz中自己不一定属于群组，调用{@link #storeGroupChatRoom(GroupChatRoom, boolean, boolean)}
	 * @param groupChatRoom
	 * @param isObserverChatMsg
	 * @return
	 */
	public boolean storeGroupChatRoom(GroupChatRoom groupChatRoom, boolean isObserverChatMsg) {
        if (isDBUnavailable()) {
            return false;
        }
        return storeGroupChatRoom(groupChatRoom, isObserverChatMsg, true);
    }

	/**
	 * 保存群组
	 * @param groupChatRoom
	 * @param isObserverChatMsg
	 * @param isMeBelongs 自己是否属于该群组，biz中默认false，待下载群组成员后再更新此字段
	 * @return
	 */
	public boolean storeGroupChatRoom(GroupChatRoom groupChatRoom, boolean isObserverChatMsg, boolean isMeBelongs) {
        if (isDBUnavailable()) {
            return false;
        }
        Log.i("store a GroupChatRoomWithUpdate:", groupChatRoom.groupID + "," + groupChatRoom.groupNameOriginal);
        ContentValues values = new ContentValues();
        values.put("group_id", groupChatRoom.groupID);
        values.put("group_name_original", groupChatRoom.groupNameOriginal);
        values.put("group_status", groupChatRoom.groupStatus);
        values.put("max_number", groupChatRoom.maxNumber);
        values.put("member_count", groupChatRoom.memberCount);
        values.put("photo_upload_timestamp", groupChatRoom.getPhotoUploadedTimestamp());
        values.put("temp_group_flag", groupChatRoom.isTemporaryGroup ? 1 : 0);
        values.put("short_group_id", groupChatRoom.shortGroupID);
        values.put("short_group_id", groupChatRoom.shortGroupID);
        values.put("lat_e6", groupChatRoom.location == null ? INVALID_LATLON : (int)(groupChatRoom.location.y * 1000000));
        values.put("lon_e6", groupChatRoom.location == null ? INVALID_LATLON : (int)(groupChatRoom.location.y * 1000000));
        values.put("place", groupChatRoom.place);
        values.put("category", groupChatRoom.category);
        values.put("will_block_msg", groupChatRoom.willBlockMsg ? 1 : 0);
        values.put("will_block_msg_notification", groupChatRoom.willBlockNotification ? 1 : 0);
        values.put("alias", groupChatRoom.groupNameLocal);
        values.put("my_nick_here", groupChatRoom.myNickHere);
        values.put("is_group_name_changed", groupChatRoom.isGroupNameChanged ? 1 : 0);
        values.put("is_me_belongs", groupChatRoom.isMeBelongs ? 1 : 0);
        values.put("editable", groupChatRoom.isEditable ? 1 : 0);
        values.put("parent_group_id", groupChatRoom.parentGroupId);
        values.put("weight", groupChatRoom.weight);

        Cursor mCursor = null;
        boolean isSuccess = false;
        // 不加锁的时候，会出现多线程问题，导致插入同一条数据，主键冲突
        synchronized ("storeGroupChatRoom") {
            mCursor = database.query(true, TBL_GROUP,
                    new String[] { "group_id" }, "group_id='"
                            + groupChatRoom.groupID + "'", null, null, null, null,
                            null);

            if (mCursor != null && mCursor.moveToFirst()) {
                isSuccess =  database.update(TBL_GROUP, values, "group_id='"
                        + groupChatRoom.groupID + "'", null) >= 0;
            } else {
                values.put("group_name_local", groupChatRoom.groupNameLocal);
                isSuccess =  database.insert(TBL_GROUP, null, values) >= 0;
            }
        }

        markDBTableModified(TBL_GROUP);

        // update the display name of chat messages if there are chat messages belongs to the group name;
        // If there is no chat message, it will update nothing.
        updateChatMessageDisplayNameWithUser(groupChatRoom.groupID, groupChatRoom.groupNameOriginal, isObserverChatMsg);

        if (null != mCursor && !mCursor.isClosed()) {
            mCursor.close();
        }

        return isSuccess;
	}

	/**
	 * Remove a GroupChatRoom from DB
	 * 
	 * @param groupChatRoom
	 * @return
	 */
	public boolean deleteGroupChatRoom(GroupChatRoom groupChatRoom) {
		if (groupChatRoom.groupID == null) {
			return false;
		}
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete(TBL_GROUP, "group_id='"
				+ groupChatRoom.groupID + "'", null) >= 0;

        markDBTableModified(TBL_GROUP);

        return ret;

	}

	/**
	 * Remove a GroupChatRoom from local DB
	 * 
	 * @param groupID
	 * @return
	 */
	public boolean deleteGroupChatRoomWithID(String groupID) {
		if (groupID == null) {
			return false;
		}
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete(TBL_GROUP, "group_id='" + groupID + "'",
				null) >= 0;

        markDBTableModified(TBL_GROUP);

        return ret;
	}

	/**
	 * Add a array list of Buddies to GroupChatRoom in local db
	 * 
	 * @return
	 */
    public void addBuddiesToGroupChatRoomByID(
            String groupID, ArrayList<GroupMember> buddies) {
        if (groupID == null) {
            return;
        }
        if (isDBUnavailable()) {
            return;
        }
        beginTrasaction();
        for (GroupMember buddy : buddies) {
            buddy.groupID = groupID;
            this.addNewBuddyToGroupChatRoomByID(buddy, true);
        }
        endTranscation();
    }

	/**
	 * Add a Buddy to GroupChatRoom in local db
	 *
     * @param buddy
	 * @param isUpdateBuddy
	 * @return
	 */
	public synchronized boolean addNewBuddyToGroupChatRoomByID(GroupMember buddy, boolean isUpdateBuddy) {
        String groupID = buddy.groupID;

		if (groupID == null) {
			return false;
		}
        if (isDBUnavailable()) {
            return false;
        }
		Log.i("store a Buddy:", buddy.userID + ", to group:" + groupID + ", isUpdateBuddy is " + isUpdateBuddy);

		ContentValues values = new ContentValues();
		values.put("group_id", groupID);
		values.put("member_id", buddy.userID);
		values.put("level", buddy.getLevel());

		Cursor mCursor = database.query(true, TBL_GROUP_MEMBER,
				new String[] { "group_id" }, "group_id='" + groupID
						+ "' AND member_id ='" + buddy.userID + "'", null, null, null,
				null, null);

		boolean rst = false;
		if (mCursor != null && mCursor.moveToFirst()) {
			Log.w("group_member record exist ,do nothing");
			rst = true;
		} else {
			Log.i("group_member record doesnt exist ,do insert");
			rst = database.insert(TBL_GROUP_MEMBER, null, values) >= 0;

            markDBTableModified(TBL_GROUP_MEMBER);
		}

		if (null != mCursor && !mCursor.isClosed()) {
		    mCursor.close();
		}

		if (isUpdateBuddy) {
		    storeNewBuddyDetailWithUpdate(buddy);
		}

		return rst;
	}
	
	public boolean deleteBuddyFromGroupChatRoom(String groupID, String userID) {
		if(groupID == null || userID == null)
			return false;
        if (isDBUnavailable()) {
            return false;
        }
		int ret= database.delete(TBL_GROUP_MEMBER,
				"group_id=? AND member_id=?",
				new String[] {groupID, userID});

        // update the member_count of TBL_GROUP
        database.execSQL("update " + TBL_GROUP + " set member_count = member_count - 1 where group_id = ?", new String[]{groupID});
        markDBTableModified(TBL_GROUP_MEMBER);

        return 1==ret;
	}

	/**
	 * 移除自己，要从群组关系表中删除自己(invoke #deleteBuddyFromGroupChatRoom(...))，同时需要更新group_chatroom中的is_me_belongs标志位
	 * @param groupID
	 * @param myUserID
	 * @return
	 */
    public void deleteMyselfFlagFromGroupChatRoom(String groupID) {
        if(groupID == null) {
            return;
        }
        if (isDBUnavailable()) {
            return;
        }

        // update the flag is_me_belongs to '0'
        database.execSQL("update " + TBL_GROUP + " set is_me_belongs = 0 where group_id = ?", new String[]{groupID});
        markDBTableModified(TBL_GROUP);
    }

    /**
     * 获取跟部门, for biz
     * @return root GroupChatRoom
     */
    public GroupChatRoom fetchRootGroupChatRoom() {
        if (isDBUnavailable()) {
            return null;
        }
        GroupChatRoom chatRoom = null;
        ArrayList<GroupChatRoom> groupChatRooms = _fetchGroupChatRooms(
                "editable=? and temp_group_flag=? and (parent_group_id is null or parent_group_id=?)",
                new String[] {String.valueOf(0), String.valueOf(0), ""});
        if (!groupChatRooms.isEmpty()) {
            chatRoom = groupChatRooms.get(0);
        }
        return chatRoom;
    }

	/**
	 * wowcity获取所有群组（自己仍然是群组成员的群组）， biz获取群组时用 {@link #fetchAllGroupChatRooms(boolean)}
     * 忽略校园组织架构。
	 * @param
	 * @return
	 */
	public ArrayList<GroupChatRoom> fetchAllGroupChatRooms() {
		return fetchAllGroupChatRooms(false);
	}

	/**
	 * wowcity获取所有非临时会话群组（自己仍然是群组成员的群组）， biz获取群组时用 {@link #fetchNonTempGroupChatRooms(boolean)}
	 * @return
	 */
	public ArrayList<GroupChatRoom> fetchNonTempGroupChatRooms() {
		return fetchNonTempGroupChatRooms(false);
	}

	/**
	 * wowcity获取所有临时会话群组（自己仍然是群组成员的群组）， biz也用此方法
	 * @return
	 */
	public ArrayList<GroupChatRoom> fetchTempGroupChatRooms() {
		return _fetchGroupChatRooms(1);
	}

	/**
     * （wowcity获取群组自己仍然是群组成员的群组， biz获取群组时不用考虑自己是否属于此群组）
     * 忽略校园组织架构。
     *
     * @param
     * @return
     */
    public ArrayList<GroupChatRoom> fetchAllGroupChatRooms(boolean isForBiz) {
        ArrayList<GroupChatRoom> list = new ArrayList<GroupChatRoom>();
        return _fetchGroupChatRooms(2);
    }

    /**
     * （wowcity获取群组自己仍然是群组成员的群组， biz获取群组时不用考虑自己是否属于此群组）
     * @param isForBiz
     * @return
     */
    public ArrayList<GroupChatRoom> fetchNonTempGroupChatRooms(boolean isForBiz) {
        ArrayList<GroupChatRoom> list = new ArrayList<GroupChatRoom>();
        return _fetchGroupChatRooms(0);
    }

	/**
	 * Fetch a GroupChatRoom by groupID from local db
	 * 
	 * @param groupID
	 * @return
	 */
	public GroupChatRoom fetchGroupChatRoom(String groupID) {
		if (groupID == null) {
			return null;
		}
        if (isDBUnavailable()) {
            return null;
        }
		ArrayList<GroupChatRoom> lst = _fetchGroupChatRooms(
				"group_id = ?", new String[] { groupID });
		if(lst != null && !lst.isEmpty())
			return lst.get(0);
		return null;
	}

	/**
	 * 获取群组（wowcity获取群组自己仍然是群组成员的群组， biz获取群组时不用考虑自己是否属于此群组）。
     * 忽略校园组织架构。
	 * @param tempFlag 0:normal group, 1:temp group, 2:all
	 * @return
	 */
	private ArrayList<GroupChatRoom> _fetchGroupChatRooms(int tempFlag) {
        if (isDBUnavailable()) {
            return new ArrayList<>();
        }
        String selection = " (category <> ? AND category <> ?) ";
        if (tempFlag == 2) {
            return _fetchGroupChatRooms( selection,
                    new String[] {
                            GroupChatRoom.CATEGORY_SCHOOL,
                            GroupChatRoom.CATEGORY_CLASSROOM });
        }
        else {
            selection += " and is_me_belongs=1 and temp_group_flag=?";
            return _fetchGroupChatRooms(
                    selection,
                    new String[] {
                            GroupChatRoom.CATEGORY_SCHOOL,
                            GroupChatRoom.CATEGORY_CLASSROOM,
                            Integer.toString(tempFlag) });
        }
	}

	/**
	 * 获取群组（自己仍然是群组成员的群组）
	 * @param selection
	 * @param selectionArgs
	 * @return
	 */
	private ArrayList<GroupChatRoom> _fetchGroupChatRooms(String selection, String[] selectionArgs) {
		ArrayList<GroupChatRoom> list = new ArrayList<GroupChatRoom>();
        if (isDBUnavailable()) {
            return list;
        }

//		Cursor cursor = database.query(TBL_GROUP,
//				new String[] { "group_id", "short_group_id", "group_name_original",
//				"group_name_local", "group_status", "max_number",
//				"member_count", "temp_group_flag", "photo_upload_timestamp",
//                "lat_e6", "lon_e6", "place", "category",
//                "will_block_msg", "will_block_msg_notification", "alias",
//                "my_nick_here", "is_group_name_changed", "editable", "parent_group_id"},
//				selection, selectionArgs,
//				null, null, "temp_group_flag desc");
		Cursor cursor = database.rawQuery(
                "SELECT "
                        + " group_id, short_group_id, group_name_original, group_name_local, group_status,"
                        + " max_number, member_count, temp_group_flag, photo_upload_timestamp, "
                        + " lat_e6, lon_e6, place, category, will_block_msg, will_block_msg_notification,"
                        + " alias, my_nick_here, is_group_name_changed, is_me_belongs, editable, parent_group_id,"
                        + " `favorite`.`target_id`" // 此字段只要不空，说明此group为 favorite的
                        + " FROM `group_chatroom`"
                        + " left join `favorite` ON (`group_chatroom`.`group_id`= `favorite`.`target_id` and `favorite`.`type`=1)"
                        + " where " + selection
                        + " order by temp_group_flag desc, weight asc, group_name_original asc",
                selectionArgs);

		if (cursor.moveToFirst()) {
			do {
				GroupChatRoom room = new GroupChatRoom();
				int i = -1;
				room.groupID = cursor.getString(++i);
				room.shortGroupID = cursor.getString(++i);
				room.groupNameOriginal = cursor.getString(++i);
				room.groupNameLocal = cursor.getString(++i);
				room.groupStatus = cursor.getString(++i);
				room.maxNumber = cursor.getInt(++i);
				room.memberCount = cursor.getInt(++i);
				room.isTemporaryGroup = (cursor.getInt(++i) == 1);
				room.setPhotoUploadedTimestamp(cursor.getLong(++i));
                int lat = cursor.getInt(++i);
                int lon = cursor.getInt(++i);
                if (lat != INVALID_LATLON && lon != INVALID_LATLON)
                    room.location = new PointF(lon / 1000000f, lat / 1000000f);
                room.place = cursor.getString(++i);
                room.category = cursor.getString(++i);
                room.willBlockMsg = 1 == cursor.getInt(++i);
                room.willBlockNotification = 1 == cursor.getInt(++i);
                room.groupNameLocal = cursor.getString(++i);
                room.myNickHere = cursor.getString(++i);
                room.isGroupNameChanged = cursor.getInt(++i) == 1;
                room.isMeBelongs = cursor.getInt(++i) == 1;
                room.isEditable = cursor.getInt(++i) == 1;
                room.parentGroupId = cursor.getString(++i);
                room.isFavorite = !TextUtils.isEmpty(cursor.getString(++i));
				list.add(room);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	/**
	 * 获取所有的收藏的群组
	 * @return
	 */
	public ArrayList<GroupChatRoom> fetchFavoriteGroupChatRooms() {
	    ArrayList<GroupChatRoom> list = new ArrayList<GroupChatRoom>();
        if (isDBUnavailable()) {
            return list;
        }
	    Cursor cursor = database.rawQuery(
                "SELECT "
                        + " group_id, short_group_id, group_name_original, group_name_local, group_status,"
                        + " max_number, member_count, temp_group_flag, photo_upload_timestamp, "
                        + " lat_e6, lon_e6, place, category, will_block_msg, will_block_msg_notification,"
                        + " alias, my_nick_here, is_group_name_changed, is_me_belongs, editable, parent_group_id, favorite_level"
                        + " FROM `group_chatroom`"
                        + " JOIN `favorite` ON (`group_chatroom`.`group_id`= `favorite`.`target_id` and `favorite`.`type`=1)"
                        + " order by `favorite_level`",
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                GroupChatRoom room = new GroupChatRoom();
                int i = -1;
                room.groupID = cursor.getString(++i);
                room.shortGroupID = cursor.getString(++i);
                room.groupNameOriginal = cursor.getString(++i);
                room.groupNameLocal = cursor.getString(++i);
                room.groupStatus = cursor.getString(++i);
                room.maxNumber = cursor.getInt(++i);
                room.memberCount = cursor.getInt(++i);
                room.isTemporaryGroup = (cursor.getInt(++i) == 1);
                room.setPhotoUploadedTimestamp(cursor.getLong(++i));
                int lat = cursor.getInt(++i);
                int lon = cursor.getInt(++i);
                if (lat != INVALID_LATLON && lon != INVALID_LATLON)
                    room.location = new PointF(lon / 1000000f, lat / 1000000f);
                room.place = cursor.getString(++i);
                room.category = cursor.getString(++i);
                room.willBlockMsg = 1 == cursor.getInt(++i);
                room.willBlockNotification = 1 == cursor.getInt(++i);
                room.groupNameLocal = cursor.getString(++i);
                room.myNickHere = cursor.getString(++i);
                room.isGroupNameChanged = cursor.getInt(++i) == 1;
                room.isMeBelongs = 1 == cursor.getInt(++i);
                room.isEditable = cursor.getInt(++i) == 1;
                room.parentGroupId = cursor.getString(++i);
                room.isFavorite = true;
                room.favoriteLevel = cursor.getInt(++i);
                list.add(room);
            } while (cursor.moveToNext());
        }
        
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
	    return list;
	}

	/**
	 * 获取所有没有成员的群组，说明是新下载的群组，需要从服务器下载其成员
	 * @return
	 */
	public ArrayList<String> fetchAllGroupChatRoomsWithoutMembers() {
        ArrayList<String> newGroups = new ArrayList<String>();
        if (isDBUnavailable()) {
            return newGroups;
        }
        Cursor cursor = database.rawQuery(
                "SELECT group_id "
                        + " FROM `" + TBL_GROUP + "` "
                        + " where not exists (select 1 from group_member where group_chatroom.group_id=group_member.group_id)",
                        null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                newGroups.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return newGroups;
	}

	/**
	 * get the dept name of the member.
	 * @param memberId
	 * @return
	 */
    public final static String PERSON_DEPS_SEP="/";

    /**
     * 根据memberId获得此成员的最小部门组合
     * @param memberId
     * @return memberId对应的最小部门组合(只使用GroupChatRoom的两个字段)：
     * <p>isEditable标识是否为根部门；
     * <p>groupNameOriginal为部门的名称的连接，分割符为"/"
     */
    public GroupChatRoom getDeptInfoByMemberId(String memberId) {
        if (TextUtils.isEmpty(memberId)) {
            return null;
        }
        if (isDBUnavailable()) {
            return null;
        }

        String deptName = "";
        StringBuffer deptNameBuffer = new StringBuffer();
        Cursor cursor = database.rawQuery(
                " select result_room.group_name_original,result_room.editable,result_room.parent_group_id from "
                + " (SELECT room.group_id,room.group_name_original,room.parent_group_id,room.editable "
                        + " FROM group_chatroom as room "
                        + " join group_member as member "
                        + " on room.group_id=member.group_id "
                        + " where room.temp_group_flag=0 and member.member_id=?) as result_room "
                + " where not exists "
                        + " (select 1 from (SELECT room.group_id,room.group_name_original,room.parent_group_id "
                                        + " FROM group_chatroom as room "
                                        + " join group_member as member "
                                        + " on room.group_id=member.group_id "
                                        + " where room.temp_group_flag=0 and member.member_id=?) as result_room2"
                        + " where result_room.group_id=result_room2.parent_group_id)", new String[] {memberId, memberId});

        GroupChatRoom dept = null;
        boolean isEditable = false;
        String parentGroupId = "";
        if (cursor != null && cursor.moveToFirst()) {
            dept = new GroupChatRoom();
            dept.isEditable = true;
            do {
                deptNameBuffer.append(cursor.getString(0)).append(PERSON_DEPS_SEP);
                isEditable = cursor.getInt(1) == 1;
                parentGroupId = cursor.getString(2);
                // 不属于任何子部门，只属于公司的，在显示时隐藏部门信息
                if (!isEditable && TextUtils.isEmpty(parentGroupId)) {
                    dept.isEditable = false;
                    break;
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        // remove the "/"
        if (!TextUtils.isEmpty(deptNameBuffer)) {
            deptName = deptNameBuffer.substring(0, deptNameBuffer.length() - 1);
            dept.groupNameOriginal = deptName;
        }
        return dept;
    }

    /**
     * 获取memberId对应的部门id的组合(父子部门都会出现)
     * @param memberId
     * @return
     */
    public ArrayList<String> getGroupIdsByMemberId(String memberId) {
        ArrayList<String> deptIds = new ArrayList<String>();
        if (TextUtils.isEmpty(memberId)) {
            return deptIds;
        }
        if (isDBUnavailable()) {
            return deptIds;
        }

        Cursor cursor = database.rawQuery(
                "SELECT group_id"
                    + " FROM group_member"
                    + " where member_id=?",
                new String[] {memberId});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                deptIds.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return deptIds;
    }

	/**
	 * Fetch the buddy list of GroupChatRooms from local db
	 * 
	 * @param groupID
	 * @return
	 */
	public ArrayList<GroupMember> fetchGroupMembers(String groupID) {
		if (groupID == null) {
			return null;
		}
		ArrayList<GroupMember> list = new ArrayList<GroupMember>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.rawQuery(
                "SELECT"
                        + "`buddydetail`.`uid`,level "
                        + "FROM `" + TBL_GROUP_MEMBER + "` "
                        + "LEFT JOIN `buddies` ON `" + TBL_GROUP_MEMBER + "`.`member_id`= `buddies`.`uid`"
                        + "LEFT JOIN `buddydetail` ON `" + TBL_GROUP_MEMBER + "`.`member_id`= `buddydetail`.`uid`"
                        + "LEFT JOIN `block_list` ON `" + TBL_GROUP_MEMBER + "`.`member_id`= `block_list`.`uid`"
                        + "WHERE `group_id`='" + groupID + "'"
                        + "ORDER BY trim(`sort_key`), trim(`buddydetail`.`nickname`)", null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
                int i = - 1;
				GroupMember user = new GroupMember(cursor.getString(++i));
                user.setLevel(cursor.getInt(++i));
                if (null != fetchBuddyDetail(user)) {
                    user.groupID = groupID;
                    list.add(user);
                }
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		return list;

	}

	/**
	 * 获得此群组下的所有成员，此时返回的members中可能存在重复的成员(同时属于多个群组)
	 * @param chatRoom 构建过组织架构的GroupChatRoom
	 * @param members
	 */
    public void fetchGroupMembersIteration(GroupChatRoom chatRoom, ArrayList<GroupMember> members) {
        if (null == chatRoom || TextUtils.isEmpty(chatRoom.groupID)) {
            return;
        }
        if (isDBUnavailable()) {
            return;
        }

        ArrayList<GroupMember> tempMembers = fetchGroupMembers(chatRoom.groupID);
        if (null != tempMembers) {
            members.addAll(tempMembers);
        }
        if (null != chatRoom.childGroups && !chatRoom.childGroups.isEmpty()) {
            for (GroupChatRoom childGroup : chatRoom.childGroups) {
                fetchGroupMembersIteration(childGroup, members);
            }
        }
	}

	/**
	 * whether the member belongs to the group.
	 * @param memberId
	 * @param groupId
	 * @return true, belongs; false, not.
	 */
	public boolean isExistsInGroupChatRoom(String memberId, String groupId){
        if (isDBUnavailable()) {
            return false;
        }
	    Cursor cursor = null;
	    String myUserId = mPrefUtil.getUid();
	    if (!TextUtils.isEmpty(myUserId) && myUserId.equals(memberId)) {
	        cursor = database.query(TBL_GROUP,
	                null,
	                "group_id = ? and is_me_belongs = 1",
	                new String[] {groupId},
                    null, null, null);
	    } else {
	        cursor = database.query(TBL_GROUP_MEMBER, null, "group_id = ? and member_id = ? and level in (?, ?, ?)",
	                new String[]{groupId,
	                memberId,
	                String.valueOf(GroupMember.LEVEL_CREATOR),
	                String.valueOf(GroupMember.LEVEL_ADMIN),
	                String.valueOf(GroupMember.LEVEL_DEFAULT)},
	                null,
	                null,
	                null);
        }
	    boolean isExist = (cursor != null && cursor.getCount() != 0);
	    if (cursor != null && !cursor.isClosed()){
	        cursor.close();
	    }
	    return isExist;
	}

	/**
	 * Remove the buddy list of GroupChatRooms from local db
	 * 
	 * @param groupID
	 * @return
	 */
	public boolean deleteAllBuddiesInGroupChatRoomByID(String groupID) {
		if (groupID == null) {
			return false;
		}
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete(TBL_GROUP_MEMBER, "group_id='" + groupID + "'",
				null) >= 0;

        markDBTableModified(TBL_GROUP_MEMBER);

        return ret;
	}

    public void updateGroupChatRoomsFavorite(ArrayList<String> groupIds, boolean isFavorite) {
        if (null == groupIds || groupIds.isEmpty()) {
            return;
        }
        beginTrasaction();
        for (String groupId : groupIds) {
            updateGroupChatRoomFavorite(groupId, isFavorite);
        }
        endTranscation();
    }

	/**
	 * 更新group_chat_room的favorite状态，即更新关联表 TBL_FAVORITE
	 * @param groupId
	 * @param isFavorite
	 * @return
	 */
	public void updateGroupChatRoomFavorite(String groupId, boolean isFavorite) {
        if (isDBUnavailable()) {
            return;
        }
	    // 先删除关联关系，如果新增，则添加；否则只删除
	    database.delete(TBL_FAVORITE, "type=1 and target_id=?", new String[] {groupId});
	    if (isFavorite) {
	        database.execSQL("insert into favorite(type,target_id,favorite_level) values (1,?,(select max(favorite_level)+1 from favorite))",
	                new String[] {groupId});
	    }
	    markDBTableModified(DUMMY_TBL_FAVORITE_GROUP);
	}

	/**
	 * 更新group的favorite_level
	 * @param group
	 */
	public void sortFavoriteGroupChatRooms(String[] groupIds) {
	    if (null == groupIds || groupIds.length == 0) {
	        return;
	    }
        if (isDBUnavailable()) {
            return;
        }

	    // 清空关联表
	    database.delete(TBL_FAVORITE, "type=1", null);

	    // 
	    int favoriteLevel = 0;
	    for (String groupId : groupIds) {
	        ContentValues values = new ContentValues();
	        values.put("type", 1);
            values.put("target_id", groupId);
	        values.put("favorite_level", favoriteLevel++);
	        database.insert(TBL_FAVORITE, null, values);
        }
        markDBTableModified(DUMMY_TBL_FAVORITE_GROUP);
    }

	/**
	 * 清空收藏的联系人和群组(即情况关联表)
	 */
    public void clearFavoriteContactsAndGroups() {
        if (isDBUnavailable()) {
            return;
        }
        // 清空关联表
        database.delete(TBL_FAVORITE, null, null);
    }

	/**
	 * Update a GroupChatRoom's information
	 * 
	 * @param groupChatRoom
	 * @return
	 */
	public boolean updateGroupChatRoom(GroupChatRoom groupChatRoom) {
        if (isDBUnavailable()) {
            return false;
        }

		ContentValues values = new ContentValues();
		values.put("group_id", groupChatRoom.groupID);
		values.put("group_name_original", groupChatRoom.groupNameOriginal);
		values.put("group_name_local", groupChatRoom.groupNameLocal);
		values.put("group_status", groupChatRoom.groupStatus);
		values.put("max_number", groupChatRoom.maxNumber);
		values.put("member_count", groupChatRoom.memberCount);
		values.put("place", groupChatRoom.place);
		values.put("category", groupChatRoom.category);
		values.put("temp_group_flag", groupChatRoom.isTemporaryGroup ? 1 : 0);
		values.put("photo_upload_timestamp", groupChatRoom.getPhotoUploadedTimestamp());
		values.put("is_group_name_changed", groupChatRoom.isGroupNameChanged ? 1 : 0);
		values.put("editable", groupChatRoom.isEditable ? 1 : 0);
        values.put("parent_group_id", groupChatRoom.parentGroupId);

		boolean ret= database.update(TBL_GROUP, values, "group_id='"
				+ groupChatRoom.groupID + "'", null) >= 0;

        markDBTableModified(TBL_GROUP);

        return ret;
	}

	/**
	 * 更新群组信息中的is_me_belongs，我属于此群组的设为1
	 * @param myUserId
	 */
//    public void updateGroupIsMeBelongs(String myUserId) {
//        // 我属于的群组的is_me_belongs属性设为1,默认为0，但wowcity默认为1，所以在保存群组的时候要区分处理
//        database.execSQL("update group_chatroom set is_me_belongs=1 "
//                + "       where exists (select 1 from group_member "
//                + "                         where group_chatroom.group_id=group_member.group_id and group_member.member_id=?)",
//                new String[]{myUserId});
//        markDBTableModified(TBL_GROUP);
//    }

    public boolean updateGroupMemberLevel(String groupID, String memberID, int level) {
        if (isDBUnavailable()) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("level", level);

        boolean ret= database.update(TBL_GROUP_MEMBER, values,
                "group_id=? AND member_id=?",
                new String[] { groupID, memberID }) >= 0;

        markDBTableModified(TBL_GROUP_MEMBER);

        return ret;
    }

	/**
	 * Change a GroupChatRoom's local name
	 * 
	 * @param newName
	 * @param groupID
	 * @return
	 */
	public boolean changeGroupChatRoomLocalName(String newName, String groupID) {
		if (newName == null || groupID == null)
			return false;
        if (isDBUnavailable()) {
            return false;
        }

		ContentValues values = new ContentValues();
		values.put("group_name_local", newName);

		boolean ret= database.update(TBL_GROUP, values, "group_id='" + groupID
				+ "'", null) >= 0;

        markDBTableModified(TBL_GROUP);

        return ret;
	}

	/**
	 * Internal use only!!
	 * Change a GroupChatRoom's member count
	 * 
	 * @param newCount
	 * @param groupID
	 * @return
	 */
	public boolean changeGroupChatRoomMemberCount(int newCount, String groupID) {
		if (groupID == null)
			return false;
        if (isDBUnavailable()) {
            return false;
        }

		ContentValues values = new ContentValues();
		values.put("member_count", newCount);

		boolean ret= database.update(TBL_GROUP, values, "group_id='" + groupID
				+ "'", null) >= 0;

        markDBTableModified(TBL_GROUP);

        return ret;
	}

	/**
	 * Store the block list to local db
	 * 
	 * @param ArrayList<Buddy>
	 */
	public void storeBlockList(ArrayList<Buddy> buddies) {
		if (buddies == null) {
			return;
		}
        if (isDBUnavailable()) {
            return;
        }
		for (Buddy buddy : buddies) {
			this.storeBuddyToBlockList(buddy);
		}
	}

	
	/**
	 * Internal use only!
	 * @param buddy
	 */
	public void storeBuddyToBlockList(Buddy buddy) {
        if (isDBUnavailable()) {
            return;
        }
		if (buddy.userID.endsWith(mPrefUtil.getUid())) {
			Log.w("this buddy has the same userID as mine ,dont block");
			return;
		}

		this.blockBuddy(buddy.userID);
		this.storeNewBuddyDetailWithUpdate(buddy);
	}

	
	/**
	 * Add buddy id to block list
	 * @param userID
	 * @return
	 */
	public boolean blockBuddy(String userID) {
        if (isDBUnavailable()) {
            return false;
        }
		if (userID == null)
			return false;
		Log.i("blockBuddy:", userID);

		ContentValues values = new ContentValues();
		values.put("uid", userID);

		Cursor mCursor = database.query(true, "block_list",
				new String[] { "uid" }, "uid='" + userID + "'", null, null,
				null, null, null);

		boolean isSuccess = false;
		if (mCursor != null && mCursor.moveToFirst()) {
		    isSuccess = true;
		} else {
		    isSuccess = database.insert("block_list", null, values) >= 0;

            markDBTableModified("block_list");
		}
		if (null != mCursor && !mCursor.isClosed()) {
		    mCursor.close();
		}

		return isSuccess;

	}
	/**
	 * Remove buddy id from block list
	 * @param userID
	 * @return
	 */
	public boolean unblockBuddy(String userID) {
        if (isDBUnavailable()) {
            return false;
        }
		if (userID == null)
			return false;

		boolean ret= database.delete("block_list", "uid='" + userID + "'", null) >= 0;

        markDBTableModified("block_list");

        return ret;
	}

	/**
	 * Remove the block list
	 * @return
	 */
	public boolean deleteAllBlockList() {
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("block_list", "1", null) >= 0;

        markDBTableModified("block_list");

        return ret;
	}

	/**
	 * Fetch all blocked buddies (Buddies) from database
	 * 
	 * @return ArrayList<Buddy>
	 */
	public ArrayList<Buddy> fetchAllBlockedBuddies() {
		ArrayList<Buddy> list = new ArrayList<Buddy>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.rawQuery(
                "SELECT "
                        + "`block_list`.`uid`,"
                        + "FROM `block_list`"
                        + "LEFT JOIN `buddies` ON `block_list`.`uid`= `buddies`.`uid`",
                null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				Buddy user = new Buddy(cursor.getString(0));
                if (null != fetchBuddyDetail(user))
                    list.add(user);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

    /**
     * You dont wont to call this unless you know what you are doing
     */
    public static void dropDatabase(Context context, String uid){
        context.deleteDatabase(GlobalSetting.DATABASE_NAME + "_" + uid);
    }

	
	/**
	 * Remove the list of video call unsupported device list from local db
	 */
	public boolean deleteAllVideoCallUnsupportedDevices(){
        if (isDBUnavailable()) {
            return false;
        }
		boolean ret= database.delete("videocall_unsupported_device_list", "1", null) >= 0;

        markDBTableModified("videocall_unsupported_device_list");

        return ret;
	}

	/**
	 * Store the list of video call unsupported device list
	 * 
	 * @param deviceNumberList
	 */
	public void storeVideoCallUnsupportedDevices(ArrayList<String>deviceNumberList){

        if (isDBUnavailable()) {
            return;
        }
		if (deviceNumberList == null) {
			return;
		}
		for (String deviceNumber : deviceNumberList) {
			ContentValues values = new ContentValues();
			values.put("device_number", deviceNumber);
			database.insert("videocall_unsupported_device_list", null, values);
		}

        if(deviceNumberList.size() > 0) {
            markDBTableModified("videocall_unsupported_device_list");
        }
	}

	/**
	 * Check whether deviceNumber is video call unsupported or not
	 * 
	 * @param deviceNumber
	 */
	public boolean isDeviceNumberSupportedForVideoCall(String deviceNumber){
	    if(deviceNumber==null){
	        return false;
	    }
        if (isDBUnavailable()) {
            return false;
        }

		Cursor mCursor = database.query(true, "videocall_unsupported_device_list",
				new String[] { "device_number" }, "device_number='" + deviceNumber +"'", null,
				null, null, null, null);

		boolean isUnsupported = !(mCursor != null && mCursor.moveToFirst());
		if (null != mCursor && !mCursor.isClosed()) {
		    mCursor.close();
		}
		return isUnsupported;
	}

    /**
     * internal use:  used to store unsent receipt when network is down or failed to login to server
     *
     * @param ChatMessage
     */
    public boolean storeUnsentReceipt(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
        Cursor mCursor = database.query(true, "unsent_receipts",
                new String[] { "id" }, "chattarget='" + msg.chatUserName + "'"
                + " AND " + "msgtype='" + msg.msgType + "' AND "
                + "messagebody='" + msg.messageContent + "'", null,
                null, null, null, null);
        boolean isUnNeedStore = (mCursor != null && mCursor.moveToFirst());
        if (null != mCursor && !mCursor.isClosed()) {
            mCursor.close();
        }
        if (isUnNeedStore) {
            return true;
        }

        ContentValues values = new ContentValues();
        values.put("chattarget", msg.chatUserName);
        values.put("msgtype", msg.msgType);
        values.put("messagebody", msg.messageContent);

        boolean ret= database.insert("unsent_receipts", null, values) >= 0;

        markDBTableModified("unsent_receipts");

        return ret;
    }
    /**
     * internal use
     *
     * @param ChatMessage
     */
    public boolean deleteUnsentReceipt(ChatMessage msg) {
        if (isDBUnavailable()) {
            return false;
        }
        boolean ret= database.delete("unsent_receipts", "chattarget='"
                + msg.chatUserName + "'" + " AND " + "msgtype='" + msg.msgType
                + "' AND " + "messagebody='" + msg.messageContent + "'", null) >= 0;

        markDBTableModified("unsent_receipts");

        return ret;
    }

    /**
     * get all the unsent receipt
     *
     */
    public ArrayList<ChatMessage> fetchAllUnsentReceipt() {

        ArrayList<ChatMessage> list = new ArrayList<ChatMessage>();
        if (isDBUnavailable()) {
            return list;
        }
        Cursor cursor = database.query("unsent_receipts", new String[] { "id",
                "chattarget", "msgtype", "messagebody" }, null, null, null,
                null, "id");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ChatMessage msg = new ChatMessage();
                msg.primaryKey = cursor.getInt(0);
                msg.chatUserName = cursor.getString(1);
                msg.msgType = cursor.getString(2);
                msg.messageContent = cursor.getString(3);
                list.add(msg);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    ///////////////////////////////Stuffs for update from old wowtalk v1.x//////////////////////////////////
	/**
	 * wowtalk v1.x  only!!!
	 * Convert a Date object to UTC String with "yyyy/MM/dd HH:mm" format
	 * 
	 * @param localDate
	 * @return
	 */
	public static String chatMessage_dateToUTCString_oldversion(Date localDate) {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = dateFormatter.format(localDate);
		return dateString;
	}

	/**
	 *  wowtalk v1.x  only!!!
	 * Convert UTC String with "yyyy/MM/dd HH:mm" format to a Date object
	 * 
	 * @param string
	 * @return null if exception happen
	 */
	public static Date chatMessage_UTCStringToDate_oldversion(String string) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return date;
	}

	/**
	 * wowtalk v1.x  only!!!
	 * Convert UTC String with "yyyy/MM/dd HH:mm" format to a String with
	 * "yyyy/MM/dd HH:mm" in local time zone
	 * 
	 * @param string
	 * @return null if exception happen
	 */
	public static String chatMessage_UTCStringToLocalString_oldversion(String utcString) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(utcString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		dateFormatter.setTimeZone(TimeZone.getDefault());
		String dateString = dateFormatter.format(date);
		return dateString;
	}

	
	/**
	 * wowtalk v1.x  only!!!
	 * Convert a Date object to a UTC String with "yyyy/MM/dd HH:mm:ss" format
	 * 
	 * @param (Date)localDate
	 * @return
	 */
	public static String callLog_dateToUTCString_oldversion(Date localDate) {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = dateFormatter.format(localDate);
		return dateString;
	}

	/**
	 * wowtalk v1.x  only!!!
	 * Convert a UTC String with "yyyy/MM/dd HH:mm:ss" format to a Date object
	 * 
	 * @param string
	 * @return
	 */
	public static Date callLog_UTCStringToDate_oldversion(String string) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = dateFormatter.parse(string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return date;
	}

	public void clearEvent() {
        if (isDBUnavailable()) {
            return;
        }
		final String tblEvent = "event";
		final String tblReview = "event_review";
		database.delete(tblEvent, null, null);
        markDBTableModified(tblEvent);

		database.delete(tblReview, null, null);
        markDBTableModified(tblReview);
	}

    public void deleteAEvent(String eventId) {
        if (isDBUnavailable()) {
            return;
        }
        database.delete(Database.TBL_EVENT,"id = ?",new String[]{eventId});
        markDBTableModified(Database.TBL_EVENT);
    }

    public void setEventJoined(String eventId) {
        if (isDBUnavailable()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("membership", WEvent.MEMBER_SHIP_JOINED);

        int ret=database.update(TBL_EVENT, values,"id=?", new String[] { eventId });

        if(ret > 0) {
            markDBTableModified(TBL_EVENT);
        }
    }
	
	/**
	 * Store into `event` table.
     *
     * <p>Existing records will be completely overridden, except that
	 * local paths of multi media are preserved. </p>
     *
	 * @param e
	 * @return
	 */
	public boolean storeEvent(WEvent e) {
        if (isDBUnavailable()) {
            return false;
        }
		final String tblEvent = "event";
//		final String tblReview = "event_review";
		
		Cursor mCursor = database.query(true, tblEvent,
				new String[] { "id" },
				"id=?",
				new String[] { e.id },
				null, null, null, null);

		boolean isUpdating = false;

		if (mCursor != null && mCursor.moveToFirst()) {
			isUpdating = true;
		}

		ContentValues values = new ContentValues();
		values.put("address", e.address);
		values.put("telephone", e.telephone);
		values.put("allowReview", e.allowReview ? 1 : 0);
		values.put("capacity", e.capacity);
		values.put("costGolds", e.costGolds);
		values.put("createdTime", e.createdTime == null ? 0 :e.createdTime.getTime());
		values.put("description", e.description);
		values.put("id", e.id);
		values.put("isOfficial", e.isOfficial ? 1 : 0);
		values.put("latitude", e.latitude);
		values.put("longitude", e.longitude);
		values.put("membership", e.membership);
		values.put("needWork", e.needWork ? 1 : 0);
		values.put("is_get_member_info", e.is_get_member_info ? 1 : 0);
		values.put("owner_uid", e.owner_uid);
        values.put("host", e.host);
		values.put("privacy_level", e.privacy_level);
		values.put("size", e.size);
		values.put("startTime", e.startTime == null ? 0 : e.startTime.getTime());
        values.put("endTime", e.endTime == null ? 0 : e.endTime.getTime());
		values.put("target_user_type", e.target_user_type);
		values.put("title", e.title);
        values.put("contact_email",e.contactEmail);

        values.put("joinedMemberCount",e.joinedMemberCount);
        values.put("possibleJoinedMemberCount",e.possibleJoinedMemberCount);
        values.put("event_start_date",e.event_start_date);
        values.put("event_dead_line",e.event_dead_line);
        values.put("timeStamp",e.timeStamp);
        values.put("thumbNail",e.thumbNail);
        values.put("event_type",e.event_type);
        values.put("tag",e.tag);
        values.put("category",e.category);

		boolean failed = false;
		if (isUpdating) {
			if(1 != database.update(tblEvent, values,
					"id=?", new String[] { e.id }))
				failed = true;
		} else {
			if(-1 == database.insert(tblEvent, null, values))
				failed = true;
		}

		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}
		
		if(failed)
			return false;

        markDBTableModified(tblEvent);

		// update media table
        storeMultimedias(e, false);

		// update review table
        storeReviews(e, true);

		return !failed;
	}

    private boolean storeMultimedias(IHasMultimedia e, boolean clearOldOnes) {
        if (isDBUnavailable()) {
            return false;
        }
        if (clearOldOnes) {
            database.delete(e.getMediaDataTableName(),
                    e.getMediaDataTablePrimaryKeyName() + "=?",
                    new String[]{ e.getMediaDataTablePrimaryKeyValue() });
            markDBTableModified(e.getMediaDataTableName());
        }

        Iterator<WFile> it = e.getMediaIterator();
        if (it != null) {
            while (it.hasNext()) {
                storeMultimedia(e, it.next());
            }
        }
        return true;
    }

    /**
     * If f.localDbId >= 0, update, otherwise insert, in which case the insert ID will be output
     * to f.localDbId.
     * @param e
     * @param f
     * @return
     */
    public boolean storeMultimedia(IHasMultimedia e, WFile f) {
        if (isDBUnavailable()) {
            return false;
        }

        Cursor cur = null;
        if (0 <= f.localDbId)
            cur = database.query(e.getMediaDataTableName(),
                    new String[] { e.getMediaDataTablePrimaryKeyName() },
                    "localDbId=" + f.localDbId,
                    null, null, null, null);

        //I assume no two multimedia with same fileid
        //so,if they are same,they are the same multimedia,update it
        //when created a new moment with multi photo and photoes not all uploaded, get moment will return less photo than to be uploaded
        //when store such moment,update multimedia,not insert
        if(null == cur || cur.getCount() == 0) {
            closeACursor(cur);
            Log.w("no multimedia with local db id "+f.localDbId);
            cur=database.query(e.getMediaDataTableName(),
                    new String[] { "localDbId" },
                    "fileid=?",
                    new String[]{f.fileid}, null, null, null);
            if(null != cur && cur.getCount() != 0) {
                int localdbidIdx=cur.getColumnIndex("localDbId");
                cur.moveToFirst();
                f.localDbId=cur.getInt(localdbidIdx);
                Log.i("repeat fileid multimedia found "+f.fileid);
            }
        }

        ContentValues values = new ContentValues();
        values.put(e.getMediaDataTablePrimaryKeyName(), e.getMediaDataTablePrimaryKeyValue());
        values.put("ext", f.getExt());
        values.put("fileid", f.fileid);
        values.put("thumb_fileid", f.thumb_fileid);
        values.put("duration", f.duration);
        values.put("remoteDbId", f.remoteDbId);

        boolean isSuccess = false;
        if (null != cur && cur.moveToFirst()) {
            // update
            isSuccess = (1 == database.update(e.getMediaDataTableName(), values, "localDbId=" + f.localDbId, null));
        } else {
            // insert
            if (f.localDbId > 0)
                values.put("localDbId", f.localDbId);
            f.localDbId = (int)database.insert(e.getMediaDataTableName(), null, values);
            isSuccess = (f.localDbId != -1);
        }
        if (null != cur && !cur.isClosed()) {
            cur.close();
        }

        markDBTableModified(e.getMediaDataTableName());
        return isSuccess;
    }

    private void closeACursor(Cursor cursor) {
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private boolean storeReviews(IHasReview e, boolean clearOldOnes) {
        if (isDBUnavailable()) {
            return false;
        }
        if (clearOldOnes) {
            database.delete(e.getReviewDataTableName(),
                    e.getReviewDataTablePrimaryKeyName() + "=?",
                    new String[]{ e.getReviewDataTablePrimaryKeyValue() });
            markDBTableModified(e.getReviewDataTableName());
        }

        Iterator<Review> it = e.getReviewIterator();
        if (it != null) {
            while (it.hasNext()) {
                storeReview(e, it.next());
            }
        }
        return true;
    }

    /**
     * Update local dbid of a WEvent's media dbid.
     * @param fileid
     * @param path
     * @return
     */
    public boolean updateEventMediaPath(String fileid, String path) {
        if (isDBUnavailable()) {
            return false;
        }
        final String tblMedia = "event_media";
        ContentValues values = new ContentValues();
        values.put("remoteDbId", path);
        int ret= database.update(tblMedia, values,
                "fileid=?", new String[] { fileid });
        markDBTableModified(tblMedia);

        return 1==ret;
    }


	public ArrayList<WEvent> fetchAllEvents() {
		return fetchEvents(null, null);
	}

    public WEvent fetchEvent(String id) {
        List<WEvent> lst = fetchEvents("id=?", new String[]{id});
        if (!lst.isEmpty())
            return lst.get(0);
        return null;
    }

	public ArrayList<WEvent> fetchOfficialEvents() {
		return fetchEvents("isOfficial=1", null);
	}

	public ArrayList<WEvent> fetchMyEvents() {
		String uid = mPrefUtil.getUid();
		return fetchEvents("owner_uid=?", new String[]{uid});
	}

    public ArrayList<WEvent> fetchNotJoinedEvents() {
        return fetchEvents("membership <> "+WEvent.MEMBER_SHIP_JOINED, null);
    }

	public ArrayList<WEvent> fetchJoinedEvents() {
		return fetchEvents("membership="+WEvent.MEMBER_SHIP_JOINED, null);
	}

	public ArrayList<WEvent> fetchAppliedEvents() {
		return fetchEvents("membership=1", null);
	}

	public ArrayList<WEvent> fetchInvitedEvents() {
		return fetchEvents("membership=2", null);
	}

	private ArrayList<WEvent> fetchEvents(
            String selection, String[] selectionArgs) {
		ArrayList<WEvent> data = new ArrayList<WEvent>();
        if (isDBUnavailable()) {
            return data;
        }

		final String tblEvent = "event";

        Cursor cur = database.query(true, tblEvent,
				new String[] {
				"address",
				"telephone",
				"allowReview",
				"capacity",
				"costGolds",
				"createdTime",
				"description",
				"id",
				"isOfficial",
				"latitude",
				"longitude",
				"membership",
				"needWork",
				"is_get_member_info",
				"owner_uid",
                "host",
				"privacy_level",
				"size",
				"startTime",
                "endTime",
				"target_user_type",
				"title",
                "contact_email",
                        "joinedMemberCount",
                        "possibleJoinedMemberCount",
                        "event_start_date",
                        "event_dead_line",
                        "timeStamp",
                        "thumbNail",
                        "event_type",
                        "tag",
                        "category"
		},
		selection, selectionArgs, null, null, "timeStamp DESC", null);

		if(cur == null || !cur.moveToLast()) {
		    if (null != cur && !cur.isClosed()) {
		        cur.close();
		    }
		    return data;
		}

		do {
			WEvent e = new WEvent();
			int i = -1;
			e.address = cur.getString(++i);
			e.telephone = cur.getString(++i);
			e.allowReview = cur.getInt(++i) == 1;
			e.capacity = cur.getInt(++i);
			e.costGolds = cur.getInt(++i);
			e.createdTime = new Date(cur.getLong(++i));
			e.description = cur.getString(++i);
			e.id = cur.getString(++i);
			e.isOfficial = cur.getInt(++i) == 1;
			e.latitude = cur.getFloat(++i);
			e.longitude = cur.getFloat(++i);
			e.membership = cur.getInt(++i);
			e.needWork = cur.getInt(++i) == 1;
			e.is_get_member_info = cur.getInt(++i) == 1;
			e.owner_uid = cur.getString(++i);
            e.host = cur.getString(++i);
			e.privacy_level = cur.getInt(++i);
			e.size = cur.getInt(++i);
			e.startTime = new Date(cur.getLong(++i));
            e.endTime = new Date(cur.getLong(++i));
			e.target_user_type = cur.getInt(++i);
			e.title = cur.getString(++i);
            e.contactEmail = cur.getString(++i);
            e.joinedMemberCount=cur.getInt(++i);
            e.possibleJoinedMemberCount=cur.getInt(++i);
            e.event_start_date = cur.getString(++i);
            e.event_dead_line = cur.getString(++i);
            e.timeStamp = cur.getString(++i);
            e.thumbNail = cur.getString(++i);
            e.event_type=cur.getInt(++i);
            e.tag=cur.getString(++i);
            e.category=cur.getString(++i);
			data.add(e);
		} while(cur.moveToPrevious());

		if (null != cur && !cur.isClosed()) {
            cur.close();
        }

		for(WEvent e : data) {
            fetchMultimedias(e);
		}

		for(WEvent e : data) {
            fetchReviews(e, false);
		}

		return data;
	}

    public int fetchNewReviews(IHasReview dummy) {
        if (dummy == null)
            return 0;
        if (isDBUnavailable()) {
            return 0;
        }
        int n = dummy.getReviewsCount();
        fetchReviews(dummy, true);
        return dummy.getReviewsCount() - n;
    }

    /**
     *
     * @param e can be dummy with key value of null.
     * @param newsOnly
     */
    private void fetchReviews(IHasReview e, boolean newsOnly) {
        if (isDBUnavailable()) {
            return;
        }
        String tableName = e.getReviewDataTableName();
        String whereKey = e.getReviewDataTablePrimaryKeyName();
        String whereValue = e.getReviewDataTablePrimaryKeyValue();
        ArrayList<String> selectionArgs = new ArrayList<String>();
        String sql = "SELECT id, text, type, " + tableName + ".uid, nickname, reply_to_review_id, reply_to_uid, "
                + "reply_to_nickname, buddies.alias, reply_to_buddies.alias as reply_to_alias, read, timestamp, "
                + e.getReviewDataTablePrimaryKeyName()
                + " FROM " + tableName
                + " LEFT JOIN  buddies ON " + tableName + ".uid = buddies.uid"
                + " LEFT JOIN  buddies AS reply_to_buddies ON " + tableName + ".reply_to_uid = reply_to_buddies.uid "
                + " WHERE 1";
        if (whereValue != null) {
            sql += " AND " + whereKey + " = ? ";
            selectionArgs.add(whereValue);
        }
        if (newsOnly)
            sql += " AND read = 0";
        sql += " ORDER BY timestamp ASC";

        Cursor cur = database.rawQuery(sql, selectionArgs.toArray(new String[]{}));

        if(cur == null || !cur.moveToFirst()) {
            if (null != cur && !cur.isClosed()) {
                cur.close();
            }
            return;
        }

        String tempAlias = "";
        String tempReplyToAlias = "";
        do {
            Review r = new Review();
            int i = -1;
            r.id = cur.getString(++i);
            r.text = cur.getString(++i);
            r.type = cur.getInt(++i);
            r.uid = cur.getString(++i);
            r.nickname = cur.getString(++i);
            r.replyToReviewId = cur.getString(++i);
            r.replyToUid = cur.getString(++i);
            r.replyToNickname = cur.getString(++i);
            // alias
            tempAlias = cur.getString(++i);
            r.nickname = TextUtils.isEmpty(tempAlias) ? r.nickname : tempAlias;
            // reply_to_alias
            tempReplyToAlias = cur.getString(++i);
            r.read = cur.getInt(++i) == 1;
            r.timestamp = cur.getLong(++i);
            r.hostId = cur.getString(++i);
            r.replyToNickname = TextUtils.isEmpty(tempReplyToAlias) ? r.replyToNickname : tempReplyToAlias;
            e.addReview(r);
        } while (cur.moveToNext());

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }
    }

    public boolean isMomentFavoriteLocal(String momentId) {
        if (isDBUnavailable()) {
            return false;
        }
        boolean ret=false;

        Cursor mCursor;
        mCursor = database.query(true, TBL_MOMENT,
                new String[] {"is_favorite"},
                "id=?",
                new String[] { momentId },
                null, null, null, null);
        if (mCursor != null && mCursor.moveToFirst()) {
            ret = mCursor.getInt(0)!=0;
        }

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        return ret;
    }
    /**
     * Store into `moment` table.
     *
	 * <p>Existing records will be completely overridden, except that
	 * local paths of multi media are preserved.</p>
     *
	 * @param e
     * @param oldMomentId,this is alias momentId,to be updated by actual moment id
	 * @return
	 */
    public boolean storeMoment(Moment e,String oldMomentId) {
        return storeMoment(e, oldMomentId, true);
    }

    /**
     * 开启事务保存moments
     * @param momentsFromServerList
     * @return
     */
    public boolean storeMoments(List<Moment> momentsFromServerList) {
        beginTrasaction();
        boolean isSuccess = true;
        for(Moment aMoment : momentsFromServerList) {
            isSuccess = isSuccess && storeMoment(aMoment,null, false);
        }
        endTranscation();
        markDBTableModified("moment");
        return isSuccess;
    }

    private boolean storeMoment(Moment e,String oldMomentId, boolean isObserver) {

        if (isDBUnavailable()) {
            return false;
        }
        final String tblMoment = "moment";

        Log.i("storing moment "+e.id);
        Cursor mCursor;
        if(TextUtils.isEmpty(oldMomentId)) {
            mCursor = database.query(true, tblMoment,
                    new String[] { "id" },
                    "id=?",
                    new String[] { e.id },
                    null, null, null, null);
        } else {
            mCursor = database.query(true, tblMoment,
                    new String[] { "id" },
                    "id=?",
                    new String[] { oldMomentId },
                    null, null, null, null);
        }

		boolean isUpdating = false;

		if (mCursor != null && mCursor.moveToFirst()) {
			isUpdating = true;
		}

		ContentValues values = new ContentValues();
		values.put("id", e.id);
        values.put("allow_review", e.allowReview ? 1 : 0);
		values.put("latitude", e.latitude);
        values.put("liked_by_me", e.likedByMe ? 1 : 0);
		values.put("longitude", e.longitude);
        if (e.owner != null) {
            values.put("owner_uid", e.owner.userID);
        }
        values.put("place", e.place);
		values.put("privacy_level", e.privacyLevel);
		values.put("timestamp", e.timestamp);
        values.put("text", e.text);
        values.put("tag", e.tag);
        values.put("survey_allow_multi_select", e.isSurveyAllowMultiSelect);
//        values.put("survey_voted_option", e.getVotedOption());
        values.put("survey_dead_line", e.surveyDeadLine);
        values.put("limited_departments",e.getLimitedDepartment());
        values.put("is_favorite",e.isFavorite?1:0);

		boolean failed = false;
		if (isUpdating) {
            int ret=0;
            if(TextUtils.isEmpty(oldMomentId)) {
                ret=database.update(tblMoment, values,"id=?", new String[] { e.id });
            } else {
                ret=database.update(tblMoment, values,"id=?", new String[] { oldMomentId });
            }
			if(1 != ret)
				failed = true;
		} else {
		    long insertId = database.insert(tblMoment, null, values);
			if(-1 == insertId)
				failed = true;
		}

		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}

		if(failed)
			return false;

        if (isObserver) {
            markDBTableModified(tblMoment);
        }

		// overwrite table
        fixMomentVoiceTime(e);
        storeMultimedias(e, false);

		// update review table
        storeReviews(e, true);

        storeSurvyOptions(e,oldMomentId,true);

		return !failed;
	}

    private void fixMomentVoiceTime(Moment moment) {
        if (isDBUnavailable()) {
            return;
        }
        MediaPlayer player=new MediaPlayer();

        for (WFile file : moment.multimedias) {
            if ((file.isAudioByExt())
                    && file.duration <= 0) {
                String filePath=makeLocalFilePath(file.fileid, file.getExt());
                if (new File(filePath).exists()) {
                    try {
                        player.setDataSource(filePath);
                        player.prepare();

                        file.duration = player.getDuration() / 1000;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if(null != player) {
            player.release();
            player=null;
        }
    }

    private void storeSurvyOptions(Moment moment,String oldMomentId,boolean clearOld) {
        if (isDBUnavailable()) {
            return;
        }
        Log.i("storing survy options " + moment.surveyOptions.size());
        if(moment.surveyOptions.size() <= 0) {
            return;
        }

        String momentIdKey=moment.id;
        if(!TextUtils.isEmpty(oldMomentId)) {
            momentIdKey=oldMomentId;
        }
//        if (clearOld) {
            database.delete(TBL_SURVEY,
                    "moment_id = ?",
                    new String[]{ momentIdKey });
            markDBTableModified(TBL_SURVEY);
//        }

        for(int i=0; i<moment.surveyOptions.size(); ++i) {
            Moment.SurveyOption aOption=moment.surveyOptions.get(i);

            if(!TextUtils.isEmpty(oldMomentId)) {
                aOption.momentId=moment.id;
            }
            storeASurveyOption(aOption);
        }
    }

    private boolean storeASurveyOption(Moment.SurveyOption aOption) {
        if (isDBUnavailable()) {
            return false;
        }
        Cursor mCursor;
        mCursor = database.query(true, TBL_SURVEY,
                new String[] { "moment_id" },
                "moment_id=?",
                new String[] { aOption.momentId },
                null, null, null, null);

        boolean isUpdating = false;

        if (mCursor != null) {
            if(mCursor.moveToFirst()) {
//            isUpdating = true;
            }
            if(!mCursor.isClosed()) {
                mCursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put("moment_id", aOption.momentId);
        values.put("option_id", aOption.optionId);
        values.put("option_desc", aOption.optionDesc);
        values.put("voted_num", aOption.votedNum);
        values.put("is_voted", aOption.isVoted?1:0);

        Log.i("store option "+aOption.optionDesc+" with update "+isUpdating);
        boolean failed = false;
        if (isUpdating) {
            int ret=0;
            ret=database.update(TBL_SURVEY, values,"moment_id=?", new String[] { aOption.momentId });
            if(1 != ret)
                failed = true;
        } else {
            if(-1 == database.insert(TBL_SURVEY, null, values))
                failed = true;
        }

        return failed;
    }

    public void updateMomentLikedAttr(String momentId, boolean liked) {
        if (isDBUnavailable()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("liked_by_me", liked ? 1 : 0);
        database.update(TBL_MOMENT, values, "id=" + momentId, null);

        markDBTableModified(TBL_MOMENT);
    }

	/**
	 * Update local dbid of a Moment's media dbid.
	 * @param fileid
	 * @param path
	 * @return
	 */
	public boolean updateMomentMediaPath(String fileid, String path) {
        if (isDBUnavailable()) {
            return false;
        }
		final String tblMedia = "moment_media";
		ContentValues values = new ContentValues();
		values.put("remoteDbId", path);
		int ret= database.update(tblMedia, values,
				"fileid=?", new String[] { fileid });
        markDBTableModified(tblMedia);

        return 1==ret;
	}

    public int getMomentsCount() {
        int ret=0;
        if (isDBUnavailable()) {
            return ret;
        }

        Cursor cur = database.rawQuery("select count(id) from moment",null);
        if(null != cur && cur.moveToFirst()) {
            ret=cur.getInt(0);

            cur.close();
        }

        return ret;
    }
    
    

    /**
     *
     * @param selection see Database.query()
     * @param selectionArgs see Database.query()
     * @param count
     * @param accountType -1 means all.
     * @return
     */
    private ArrayList<Moment> fetchMoments(
            String selection, String[] selectionArgs, int count,int accountType) {
        ArrayList<Moment> data = new ArrayList<>();
        if (isDBUnavailable()) {
            return data;
        }

        // 注意我自己不在 buddydetail 表里，所以 JOIN 查询中 buddydetail 记录可那为空
        StringBuilder sql = new StringBuilder("SELECT " +
                        "a.allow_review," +
                        "a.id," +
                        "a.latitude," +
                        "a.longitude," +
                        "a.liked_by_me," +
                        "a.owner_uid," +
                        "a.place," +
                        "a.privacy_level," +
                        "a.timestamp," +
                        "a.text," +
                        "a.tag," +
                        "a.survey_allow_multi_select," +
                        "a.survey_dead_line," +
                        "a.limited_departments," +
                        "a.is_favorite " +
                        "FROM moment AS a " +
                        "LEFT JOIN buddydetail as b " +
                        "ON (a.owner_uid = b.uid) ");

        sql.append(" WHERE ");
        if (accountType != -1) {
            sql.append("(")
                    .append(" b.account_type=").append(String.valueOf(accountType));
            int myAccountType = PrefUtil.getInstance(context).getMyAccountType();
            if (myAccountType == accountType) {
                String myUid = PrefUtil.getInstance(context).getUid();
                sql.append(" OR a.owner_uid='").append(myUid).append("'");
            }
            sql.append(") AND ");
        }

        if (TextUtils.isEmpty(selection))
            sql.append("1");
        else
            sql.append(selection);

        sql.append(" ORDER BY a.timestamp DESC ");

        if (count > 0)
            sql.append(" LIMIT ").append(String.valueOf(count));

        Cursor cur = database.rawQuery(sql.toString(), selectionArgs);

        if(cur == null || !cur.moveToFirst()) {
            if (null != cur && !cur.isClosed()) {
                cur.close();
            }
            return data;
        }

        do {
            Moment e = new Moment();
            int i = -1;
            e.allowReview = cur.getInt(++i) == 1;
            e.id = cur.getString(++i);
            e.latitude = cur.getFloat(++i);
            e.longitude = cur.getFloat(++i);
            e.likedByMe = cur.getInt(++i) == 1;
            String owner_uid = cur.getString(++i);
            if (!Utils.isNullOrEmpty(owner_uid)) {
                e.owner = buddyWithUserID(owner_uid);
                if (e.owner == null) {
                    e.owner = new Buddy();
                    e.owner.userID = owner_uid;
                }
            } else {
                e.owner = null;
            }
            e.place = cur.getString(++i);
            e.privacyLevel = cur.getInt(++i);
            e.timestamp = cur.getLong(++i);
            e.text = cur.getString(++i);
            e.tag=cur.getString(++i);
            e.isSurveyAllowMultiSelect=(cur.getInt(++i)==1);
            e.surveyDeadLine = cur.getLong(++i);
            e.setLimitedDepartment(cur.getString(++i));
            e.isFavorite=cur.getInt(++i)==1?true:false; 
//            if (accountType == -1 || accountType == mPrefUtil.getMyAccountType()) {
//            	data.add(e);
//            }  
            data.add(e);
        } while(cur.moveToNext());
        cur.close();

        for(Moment e : data) {
            fetchMultimedias(e);

            // fix WFile.remoteDir
            if (e.multimedias != null && !e.multimedias.isEmpty()) {
                for (WFile f : e.multimedias) {
                    f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
                }
            }
        }

        for(Moment e : data) {
            fetchReviews(e, false);
        }

        for(Moment e : data) {
            fetchSurveyOptions(e);
        }

        return data;
    }

    private void fetchSurveyOptions(Moment moment) {

        if (isDBUnavailable()) {
            return;
        }
        Cursor cur = database.query(TBL_SURVEY,
                new String[] { "moment_id",  "option_id","option_desc", "voted_num","is_voted"},
                "moment_id = ?",
                new String[] { moment.id },
                null, null, null, null);
        if(cur == null || !cur.moveToFirst()) {
            if (null != cur && !cur.isClosed()) {
                cur.close();
            }
            return;
        }

        Log.i("fetch survey options with "+moment.id);

        do {
            int i = -1;

            Moment.SurveyOption aSurveyOption=new Moment.SurveyOption();
            aSurveyOption.momentId=cur.getString(++i);
            aSurveyOption.optionId=cur.getString(++i);
            aSurveyOption.optionDesc=cur.getString(++i);
            aSurveyOption.votedNum=cur.getInt(++i);
            aSurveyOption.isVoted=cur.getInt(++i)==1;

            Log.i("sruvey option got "+aSurveyOption.optionDesc);
            moment.surveyOptions.add(aSurveyOption);
        } while (cur.moveToNext());

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }

        moment.sortSurveyOption();
    }

    private void fetchMultimedias(IHasMultimedia e) {
        if (isDBUnavailable()) {
            return;
        }
        Cursor cur = database.query(e.getMediaDataTableName(),
                new String[] { "fileid",  "ext", "thumb_fileid", "duration", "localDbId", "remoteDbId" },
                e.getMediaDataTablePrimaryKeyName() + "=?",
                new String[] { e.getMediaDataTablePrimaryKeyValue() },
                null, null, null, null);
        if(cur == null || !cur.moveToFirst()) {
            if (null != cur && !cur.isClosed()) {
                cur.close();
            }
            return;
        }

        do {
            WFile f = new WFile();
            int i = -1;
            f.fileid = cur.getString(++i);
            f.setExt(cur.getString(++i));
            f.thumb_fileid = cur.getString(++i);
            f.duration = cur.getInt(++i);
            f.localDbId = cur.getInt(++i);
            f.remoteDbId = cur.getString(++i);
            e.addMedia(f);
        } while (cur.moveToNext());

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }
    }
    
    /**
     * 通过uid来加载好友圈的动态
     * @param countType
     * @param maxTimestamp
     * @param count
     * @author hutianfeng
     * @return
     */
//    public ArrayList<Moment> fetchBuddyDetailUID(int countType, long maxTimestamp, int count) {
//    	Cursor cursor = database.query("buddydetail", null, "account_type = ?", new String[]{countType+""}, null, null, null);
//    	
////    	String sql = "select uid form buddydetail where account_type = "+countType+"";
//    	ArrayList<Moment> data = new ArrayList<Moment>();
//    	
//    	while (cursor.moveToNext()) {
//    		String uid = cursor.getString(cursor.getColumnIndex("uid"));
//    		ArrayList<Moment> data1 = new ArrayList<Moment>();
//    		data1 = fetchMomentsOfSingleBuddy(uid, maxTimestamp, count);
//    		data.addAll(data1);
//    	}
//    	
//    	return data;
//    }
    
    /**
     * 通过uid来获得账号的类型
     * @param uid
     * @return
     * @author hutianfeng
     */
    public int getBuddyCountType(String uid) {//account_type 
    	Cursor cursor = database.query("buddydetail", null, "uid = ?", new String[]{uid}, null, null, null);
    	int countType = -1;
    	while (cursor.moveToNext()) {
    		countType = cursor.getInt(cursor.getColumnIndex("account_type"));//查询获得账号的类型
    	}
    	return countType; 
    }

    /**
     * Fetch all Moments of a Buddy, ordered by timestamp desc.
     *
     * @param maxTimestamp 0 means not limited
     * @param count
     * @param tag Tag index, 0 means not limited
     * @return
     */
    public ArrayList<Moment> fetchMomentsOfSingleBuddy(String uid, long maxTimestamp, int count, String tag,int countType) {
        String[] selection = new String[3];
        selection[0] = "owner_uid=?";
        selection[1] = (maxTimestamp > 0) ? "timestamp<?" : "1";
        // tag == -1 全部
        if ("-1".equals(tag)) {
            selection[2] = "1";
        } else if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(tag)) {
            // 投票分单选和多选
            selection[2] = "tag=? or tag=?";
        } else {
            selection[2] = "tag=?";
        }

        ArrayList<String> args = new ArrayList<String>();
        args.add(uid);
        if (maxTimestamp > 0) {
            args.add(String.valueOf(maxTimestamp));
        }
        if (!"-1".equals(tag)) {
            if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(tag)) {
                // 投票分单选和多选
                args.add(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE);
                args.add(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI);
            } else {
                args.add(tag);
            }
        }
        return fetchMoments(TextUtils.join(" AND ", selection), args.toArray(new String[args.size()]), count,countType);
    }

    public ArrayList<Moment> fetchMomentsOfSingleBuddy(String uid, long maxTimestamp, int count,int countType) {
        return fetchMomentsOfSingleBuddy(uid, maxTimestamp, count, "-1",countType);
    }

    /**
     * Fetch the latest Moment of each of my buddies.
     *
     * @param maxTimestamp 0 means not limited
     * @param count
     * @return
     */
    public ArrayList<Moment> fetchMomentsOfAllBuddies(long maxTimestamp, int count, String tag,int countType) {
        String[] selection = new String[3]; // timestamp, tag, owner_uid
        selection[0] = (maxTimestamp > 0) ? "timestamp<?" : "1";
        // tag == -1 全部
        if ("-1".equals(tag)) {
            selection[1] = "1";
        } else if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(tag)) {
            // 投票分单选和多选
            selection[1] = "tag=? or tag=?";
        } else {
            selection[1] = "tag=?";
        }
        selection[2] = "owner_uid<>'" + Moment.ANONYMOUS_UID + "'";

        ArrayList<String> args = new ArrayList<String>();
        if (maxTimestamp > 0) {
            args.add(String.valueOf(maxTimestamp));
        }
        if (!"-1".equals(tag)) {
            if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(tag)) {
                // 投票分单选和多选
                args.add(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE);
                args.add(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI);
            } else {
                args.add(tag);
            }
        }
        return fetchMoments(TextUtils.join(" AND ", selection), args.toArray(new String[args.size()]), count,countType);
    }
    
    public ArrayList<Moment> fetchMomentsOfAllBuddies(long maxTimestamp, int count,int countType) {
        return fetchMomentsOfAllBuddies(maxTimestamp, count, "-1",countType);
    }

    public Moment fetchMoment(String id) {
        ArrayList<Moment> lst = fetchMoments("id=?", new String[]{id}, 1,-1);//传入参数-1，代表全部
        if (lst != null && !lst.isEmpty())
            return lst.get(0);
        return null;
    }

    //this function is original in PhotoDisplayHelper,but to delete moment multimedia,we need this,so move it here
    public static String makeLocalFilePath(String fileid, String ext) {
        if (TextUtils.isEmpty(fileid)) {
            return "";
        }
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "/onemeter/.cache/file/");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return "";
            }
        }
        String s = mediaStorageDir.getAbsolutePath() + "/" + fileid;
        if (ext != null) {
            if (ext.startsWith(".")) {
                s += ext;
            } else {
                s += "." + ext;
            }
        }
        return s;
    }

    public boolean deleteMoment(String moment_id) {
        return deleteMoment(moment_id, true);
    }

    public boolean deleteMoment(String moment_id, boolean alsoDeleteMediaFiles) {
        if (isDBUnavailable()) {
            return false;
        }
        //first get the moment before delete
        Moment moment2del=fetchMoment(moment_id);

        //To change body of created methods use File | Settings | File Templates.
        boolean ret= database.delete("moment", "id='" + moment_id +"'", null) >= 0;
        markDBTableModified("moment");

        database.delete("moment_media", "moment_id='" + moment_id + "'", null);
        markDBTableModified("moment_media");

        database.delete("moment_review", "moment_id='" + moment_id + "'", null);
        markDBTableModified("moment_review");

        //clear media files
        if(null != moment2del && alsoDeleteMediaFiles) {
            for (WFile file : moment2del.multimedias) {
                if (file.isAudioByExt()) {
                    //audio,video
                    String localPath = makeLocalFilePath(file.fileid, file.getExt());
                    deleteAFile(localPath);
                    Log.i("delete a audio file "+localPath);
                } else {
                    //photo
                    String thumbPath = makeLocalFilePath(file.thumb_fileid, file.getExt());
                    deleteAFile(thumbPath);
                    Log.i("delete a thumb photo file "+thumbPath);
                    String completePath = makeLocalFilePath(file.fileid, file.getExt());
                    deleteAFile(completePath);
                    Log.i("delete a photo file "+completePath);
                }
            }
        }

        return ret;
    }

    /**
     * 清除moment和review的数据库记录(包括文件)
     */
    public boolean clearMomentsAndReviews() {
        if (isDBUnavailable()) {
            return false;
        }
        ArrayList<Moment> moments = fetchMomentsOfAllBuddies(0, 0,-1);

        boolean ret= database.delete("moment", null, null) >= 0;
        markDBTableModified("moment");

        database.delete("moment_media", null, null);
        markDBTableModified("moment_media");

        database.delete("moment_review", null, null);
        markDBTableModified("moment_review");

        //clear media files
        for (Moment moment2del : moments) {
            if(null != moment2del) {
                for (WFile file : moment2del.multimedias) {
                    if (file.isAudioByExt()) {
                        //audio,video
                        String localPath = makeLocalFilePath(file.fileid, file.getExt());
                        deleteAFile(localPath);
                        Log.i("delete a audio file "+localPath);
                    } else {
                        //photo
                        String thumbPath = makeLocalFilePath(file.thumb_fileid, file.getExt());
                        deleteAFile(thumbPath);
                        Log.i("delete a thumb photo file "+thumbPath);
                        String completePath = makeLocalFilePath(file.fileid, file.getExt());
                        deleteAFile(completePath);
                        Log.i("delete a photo file "+completePath);
                    }
                }
            }
        }

        return ret;
    }

    public boolean deleteMomentReview(String moment_id, String review_id) {
        if (isDBUnavailable()) {
            return false;
        }
        boolean ret= database.delete("moment_review", "moment_id=? AND id=?",
               new String[]{ moment_id, review_id }) > 0;
        markDBTableModified("moment_review");

        return ret;
    }

    public boolean storeReview(IHasReview attachedTo, Review r) {
        if (isDBUnavailable()) {
            return false;
        }

        // 如果评论的目标不是我，则把它设置为已读，以免UI提示未读评论。
        String myUid = PrefUtil.getInstance(context).getUid();
        if (!TextUtils.equals(attachedTo.getOwnerUid(), myUid)) {
            r.read = true;
        }

        ContentValues values = new ContentValues();
        values.put(attachedTo.getReviewDataTablePrimaryKeyName(),
                attachedTo.getReviewDataTablePrimaryKeyValue());
        values.put("id", r.id);
        values.put("text", r.text);
        values.put("uid", r.uid);
        values.put("type", r.type);
        values.put("nickname", r.nickname);
        values.put("reply_to_review_id", r.replyToReviewId);
        values.put("reply_to_uid", r.replyToUid);
        values.put("reply_to_nickname", r.replyToNickname);
        values.put("timestamp", r.timestamp);
        values.put("read", r.read ? 1 : 0);

        boolean isSuccess = false;
        Cursor cur = null;
        try {
            cur = database.query(attachedTo.getReviewDataTableName(), new String[] { "id" }, "id=?", new String[] { r.id },
                    null, null, null);
            if (cur != null && cur.moveToFirst()) {
                isSuccess = (1 == database.update(attachedTo.getReviewDataTableName(), values, "id=?", new String[]{r.id}));
            } else {
                isSuccess = (-1 != database.insert(attachedTo.getReviewDataTableName(), null, values));
            }

            markDBTableModified(attachedTo.getReviewDataTableName());
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
        } finally {
            if (null != cur && !cur.isClosed()) {
                cur.close();
            }
        }
        return isSuccess;
    }

    /**
     * Store album cover info.
     * @param uid
     * @param ac
     * @return
     */
    public boolean storeAlbumCover(String uid, AlbumCover ac) {
        if (isDBUnavailable()) {
            return false;
        }
        if (uid == null || ac == null)
            return false;

        ContentValues values = new ContentValues();
        values.put("album_cover_fileid", ac.fileId);
        values.put("album_cover_ext", ac.ext);
        values.put("album_cover_upload_timestamp", ac.timestamp);
        int ret = database.update("buddydetail", values, "uid=?", new String[]{ uid });
        markDBTableModified("buddydetail");
        markDBTableModified(DUMMY_TBL_ALBUM_COVER_GOT);

        return 1==ret;
    }

    /**
     * Get album cover info.
     * @param uid
     * @return
     */
    public AlbumCover getAlbumCover(String uid) {
        if (isDBUnavailable()) {
            return null;
        }
        if (uid == null)
            return null;

        Cursor cur = database.query("buddydetail",
                new String[] {"album_cover_fileid", "album_cover_ext", "album_cover_upload_timestamp"},
                "uid=?", new String[]{ uid }, null, null, null);
        AlbumCover ac = null;
        if (cur != null && cur.moveToFirst()) {
            ac = new AlbumCover();
            ac.fileId = cur.getString(0);
            ac.ext = cur.getString(1);
            ac.timestamp = cur.getLong(2);
        }
        if (null != cur && !cur.isClosed()) {
            cur.close();
        }
        return ac;
    }

    public void clearPendingRequests() {
        if (isDBUnavailable()) {
            return;
        }
        database.delete(TBL_PENDING_REQUESTS, null, null);
        markDBTableModified(TBL_PENDING_REQUESTS);
    }

    public void deletePendingRequest(int id) {
        if (isDBUnavailable()) {
            return;
        }
        database.delete(TBL_PENDING_REQUESTS, "id=" + id, null);
        markDBTableModified(TBL_PENDING_REQUESTS);
    }

    public void deletePendingRequest(String group_id, String uid) {
        if (isDBUnavailable()) {
            return;
        }
        int n = database.delete(TBL_PENDING_REQUESTS,
                "group_id=? AND uid=?",
                new String[] { group_id, uid });
        markDBTableModified(TBL_PENDING_REQUESTS);
    }

    public void storePendingRequest(PendingRequest pr) {
        if (isDBUnavailable()) {
            return;
        }
        if (null == pr || null == pr.uid || PendingRequest.INVALID_TYPE_VALUE == pr.type)
            return;
        ContentValues values = new ContentValues();
        if (pr.id > 0)
            values.put("id", pr.id);
        values.put("type", pr.type);
        values.put("uid", pr.uid);
        values.put("nickname", pr.nickname);
        values.put("buddy_photo_timestamp", pr.buddy_photo_timestamp);
        values.put("group_id", pr.group_id);
        values.put("group_name", pr.group_name);
        values.put("group_photo_timestamp", pr.group_photo_timestamp);
        values.put("msg", pr.msg);
        database.insert(TBL_PENDING_REQUESTS, null, values);

        markDBTableModified(TBL_PENDING_REQUESTS);
    }

    public void fetchPendingRequest(ArrayList<PendingRequest> dest) {
        if (isDBUnavailable()) {
            return;
        }
        Cursor cur = database.rawQuery("SELECT id, type, req.uid, nickname, buddy_photo_timestamp, "
                + "group_id, group_name, group_photo_timestamp, msg, buddy.alias FROM "
                + TBL_PENDING_REQUESTS + " as req left join " + TBL_BUDDIES + " as buddy on req.uid = buddy.uid",
                null);
        if (null != cur) {
            String tempAlias = null;
            while(cur.moveToNext()) {
                int i = -1;
                PendingRequest pr = new PendingRequest();
                pr.id = cur.getInt(++i);
                pr.type = cur.getInt(++i);
                pr.uid = cur.getString(++i);
                pr.nickname = cur.getString(++i);
                pr.buddy_photo_timestamp = cur.getLong(++i);
                pr.group_id = cur.getString(++i);
                pr.group_name = cur.getString(++i);
                pr.group_photo_timestamp = cur.getLong(++i);
                pr.msg = cur.getString(++i);
                tempAlias = cur.getString(++i);
                pr.nickname = TextUtils.isEmpty(tempAlias) ? pr.nickname : tempAlias;
                dest.add(pr);
            }
        }

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }
    }

    /**
     * 查询请求加入我的群（我是创建者或管理员）的请求数量
     * @param groupId 群组id
     * @return 请求的数量
     */
    public int fetchPendingCountsByGroupId(String groupId) {
        int counts = 0;
        if (isDBUnavailable()) {
            return counts;
        }
        Cursor cur = database.query(TBL_PENDING_REQUESTS,
                null,
                " type=? and group_id=? ",
                new String[] {String.valueOf(PendingRequest.GROUP_ADMIN), groupId},
                null, null, null, null);

        if (null != cur) {
            counts = cur.getCount();
        }

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }

        return counts;
    }

    /**
     * 查询请求加入我的群（我是创建者或管理员）的请求
     * @param groupId
     * @param pendingLists
     */
    public void fetchPendingsByGroupId(String groupId, List<PendingRequest> pendingLists) {
        if (isDBUnavailable()) {
            return;
        }
        Cursor cur = database.rawQuery("SELECT id, type, req.uid, nickname, buddy_photo_timestamp, "
                + "group_id, group_name, group_photo_timestamp, msg, buddy.alias FROM "
                + TBL_PENDING_REQUESTS + " as req left join " + TBL_BUDDIES + " as buddy on req.uid = buddy.uid "
                + " where req.type=? and req.group_id=?",
                new String[] {String.valueOf(PendingRequest.GROUP_ADMIN), groupId});
        if (null != cur) {
            String tempAlias = null;
            while(cur.moveToNext()) {
                int i = -1;
                PendingRequest pending = new PendingRequest();
                pending.id = cur.getInt(++i);
                pending.type = cur.getInt(++i);
                pending.uid = cur.getString(++i);
                pending.nickname = cur.getString(++i);
                pending.buddy_photo_timestamp = cur.getLong(++i);
                pending.group_id = cur.getString(++i);
                pending.group_name = cur.getString(++i);
                pending.group_photo_timestamp = cur.getLong(++i);
                pending.msg = cur.getString(++i);
                tempAlias = cur.getString(++i);
                pending.nickname = TextUtils.isEmpty(tempAlias) ? pending.nickname : tempAlias;
                pendingLists.add(pending);
            }
        }

        if (null != cur && !cur.isClosed()) {
            cur.close();
        }
    }

    public void clearGroups(boolean alsoClearMembers) {
        if (isDBUnavailable()) {
            return;
        }
//        database.delete(TBL_GROUP, null, null);
        // 删除没有聊天记录的群组
        database.rawQuery("delete from group_chatroom where not exists (select * from chatmessages b where group_chatroom.group_id=b.chattarget)",
                null);
        markDBTableModified(TBL_GROUP);

        if (alsoClearMembers) {
            database.delete(TBL_GROUP_MEMBER, null, null);
            markDBTableModified(TBL_GROUP_MEMBER);
        }
    }

    public void clearGroupsForBiz(boolean alsoClearMembers) {
        if (isDBUnavailable()) {
            return;
        }
      database.delete(TBL_GROUP, null, null);
      markDBTableModified(TBL_GROUP);

      if (alsoClearMembers) {
          database.delete(TBL_GROUP_MEMBER, null, null);
          markDBTableModified(TBL_GROUP_MEMBER);
      }
  }
    
    public void saveLastMessage(String messageContent, String _targetUID) {
    	if (isDBUnavailable()) {
            return;
        }
        ContentValues values = new ContentValues();

        values.put("_targetUID_id", _targetUID);
        values.put("messageContent", messageContent);
        database.insert(TBL_SAVE_MESSAGE, null, values);

        markDBTableModified(TBL_SAVE_MESSAGE);
    }
    
    public void updateLastMessage(String messageContent, String _targetUID) {
    	if (isDBUnavailable()) {
            return;
        }
    	ContentValues values = new ContentValues();

        values.put("messageContent", messageContent);
        
        database.update(TBL_SAVE_MESSAGE, values,
                "_targetUID_id = ?",
                new String[] {_targetUID});
    	

        markDBTableModified(TBL_SAVE_MESSAGE);
    }
    
    public String getLastMessage(String _targetUID){
        if (isDBUnavailable()) {
            return null;
        }
        Cursor cursor = database.rawQuery(
                "SELECT messageContent FROM save_message WHERE _targetUID_id = ?", new String[]{_targetUID});
        String content = null;
        if(cursor != null && cursor.moveToFirst()){
        	content = cursor.getString(0);
        }      
        
        if (null != cursor && !cursor.isClosed()) {
        	cursor.close();
        }

		return content;
        
    }
    
    public int getTargetUIDCount(String _targetUID){
        Cursor cursor = database.rawQuery(
                "SELECT  * FROM save_message WHERE _targetUID_id = ?", new String[]{_targetUID});
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
        	count = 1;
        }            
        if (null != cursor && !cursor.isClosed()) {
        	cursor.close();
        }

		return count;
        
    }

    /**
     * Create and open.
     * @param context
     * @return
     */
    public static Database open(Context context) {
        Database db = new Database(context);
        db.open();
        return db;
    }

    /**
     * Set all reviews on the specified IHasReview object as read.
     *
     * @param hasReview
     */
    public void setReviewsRead(IHasReview hasReview) {
        if (isDBUnavailable()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("read", 1);
        database.update(hasReview.getReviewDataTableName(), values,
                hasReview.getReviewDataTablePrimaryKeyName() + "=?",
                new String[] {hasReview.getReviewDataTablePrimaryKeyValue()});
        markDBTableModified(hasReview.getReviewDataTableName());
    }

    public void setSpecificReviewReaded(String reviewId) {
        if (isDBUnavailable()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("read", 1);

        database.update(TBL_MOMENT_REVIEWS, values,
                "id = ?",
                new String[] {reviewId});
        markDBTableModified(TBL_MOMENT_REVIEWS);
    }

    public String getParentGroupId(String classId){
        if (isDBUnavailable()) {
            return null;
        }
        Cursor cursor = database.query(TBL_GROUP, new String[]{"parent_group_id"}, "group_id = ?", new String[]{classId}, null, null, null);
        String parent_group_id = null;
        if(cursor != null && cursor.moveToFirst()){
        	parent_group_id = cursor.getString(0);
        }      
        
        if (null != cursor && !cursor.isClosed()) {
        	cursor.close();
        }

		return parent_group_id;
    	
    }
    public List<GroupChatRoom> fetchSchools() {
        List<GroupChatRoom> schools = _fetchGroupChatRooms(
                "category=?", new String[] { GroupChatRoom.CATEGORY_SCHOOL });
        if (schools != null && !schools.isEmpty()) {
            for (GroupChatRoom school : schools) {
                fetchClassRooms(school.groupID, school);
            }
        }

        return schools;
    }

    private void fetchClassRooms(String schoolId, GroupChatRoom parent) {
        ArrayList<GroupChatRoom> classrooms = _fetchGroupChatRooms(
                "parent_group_id=?", new String[] { parent.groupID });
        if (classrooms != null && !classrooms.isEmpty()) {
            parent.childGroups = classrooms;
            for (GroupChatRoom classroom : classrooms) {
                classroom.level = parent.level + 1;

                // recursive
                fetchClassRooms(schoolId, classroom);
            }
        }

        ArrayList<GroupMember> students = fetchGroupMembers(parent.groupID);
        if (students != null && !students.isEmpty()) {
            for (Buddy student : students) {
                student.alias = fetchStudentAlias(schoolId, student.userID);
                parent.addMember(student);
            }
        }
    }

    /**
     * 获得仅有学校班级的list
     * @return
     */
    public List<GroupChatRoom> fetchSchoolsNoBuddies() {
        List<GroupChatRoom> schools = _fetchGroupChatRooms(
                "category=?", new String[] { GroupChatRoom.CATEGORY_SCHOOL });
        if (schools != null && !schools.isEmpty()) {
            for (GroupChatRoom school : schools) {
                fetchClassRoomsNoBuddies(school.groupID, school);
            }
        }
        return schools;
    }

    private void fetchClassRoomsNoBuddies(String schoolId, GroupChatRoom parent) {
        ArrayList<GroupChatRoom> classrooms = _fetchGroupChatRooms(
                "parent_group_id=?", new String[] { parent.groupID });
        if (classrooms != null && !classrooms.isEmpty()) {
            parent.childGroups = classrooms;
            for (GroupChatRoom classroom : classrooms) {
                classroom.level = parent.level + 1;
                fetchClassRoomsNoBuddies(schoolId, classroom);
            }
        }
    }

    /**
     * 校园里只获取老师的列表
     * @return list
     */
    public List<GroupChatRoom> fetchSchoolsJustTeacher() {
        List<GroupChatRoom> schools = _fetchGroupChatRooms(
                "category=?", new String[] { GroupChatRoom.CATEGORY_SCHOOL });
        if (schools != null && !schools.isEmpty()) {
            for (GroupChatRoom school : schools) {
                fetchClassRoomsJustTeacher(school.groupID, school);
            }
        }
        return schools;
    }

    private void fetchClassRoomsJustTeacher(String schoolId, GroupChatRoom parent) {
        ArrayList<GroupChatRoom> classrooms = _fetchGroupChatRooms(
                "parent_group_id=?", new String[] { parent.groupID });
        if (classrooms != null && !classrooms.isEmpty()) {
            parent.childGroups = classrooms;
            for (GroupChatRoom classroom : classrooms) {
                classroom.level = parent.level + 1;
                fetchClassRoomsJustTeacher(schoolId, classroom);
            }
        }

        ArrayList<GroupMember> members = fetchGroupMembers(parent.groupID);
        if (members != null && !members.isEmpty()) {
            for (Buddy mem : members) {
                if(mem.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
                    mem.alias = fetchStudentAlias(schoolId, mem.userID);
                    parent.addMember(mem);
                }
            }
        }
    }

    public void storeSchools(List<GroupChatRoom> schools) {

        // clear old data
        database.delete(TBL_GROUP, "category=?", new String[] { GroupChatRoom.CATEGORY_SCHOOL });
        database.delete(TBL_GROUP, "category=?", new String[] { GroupChatRoom.CATEGORY_CLASSROOM });

        for (GroupChatRoom school : schools) {
            school.category = GroupChatRoom.CATEGORY_SCHOOL;
            storeGroupChatRoom(school);

            if (school.childGroups != null && !school.childGroups.isEmpty()) {
                for (GroupChatRoom classroom : school.childGroups) {
                    storeClassRoom(school.groupID, classroom);
                }
            }
        }
    }

    private void storeClassRoom(String schoolId, GroupChatRoom classroom) {
        classroom.category = GroupChatRoom.CATEGORY_CLASSROOM;
        storeGroupChatRoom(classroom);
        if (classroom.memberList != null) {
            ArrayList<String> studentIds = new ArrayList<>(classroom.memberList.size());
            for (Buddy student : classroom.memberList) {
                studentIds.add(student.userID);
                // save student alias
                if (student.alias != null)
                    storeStudentAlias(schoolId, student.userID, student.alias);
            }
            storeGroupMemberIds(classroom.groupID, studentIds);

            // store buddies
            // NOTE: buddy info in classroom member list is not complete,
            // so we only save a buddy into db if that buddy is not already in db,
            // to avoid override valid fields with empty fields.
            ArrayList<Buddy> buddiesToStore = new ArrayList<>(classroom.memberList.size());
            for (Buddy member : classroom.memberList) {
                if (null == fetchBuddyDetail(member)) {
                    buddiesToStore.add(member);
                }
            }
            if (!buddiesToStore.isEmpty())
                storeBuddies(buddiesToStore);
        }

        // recursive
        if (classroom.childGroups != null && !classroom.childGroups.isEmpty()) {
            for (GroupChatRoom subClassroom : classroom.childGroups) {
                storeClassRoom(schoolId, subClassroom);
            }
        }
    }

    public long storeLesson(Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put("lesson_id", lesson.lesson_id);
        values.put("class_id", lesson.class_id);
        values.put("title", lesson.title);
        values.put("start_date", lesson.start_date);
        values.put("end_date", lesson.end_date);
        values.put("live", lesson.live);
        return database.insertWithOnConflict(TBL_LESSON, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long storeLessonPerformance(LessonPerformance lessonPerformance) {
        ContentValues values = new ContentValues();
        values.put("lesson_id", lessonPerformance.lesson_id);
        values.put("student_id", lessonPerformance.student_id);
        values.put("property_id", lessonPerformance.property_id);
        values.put("property_value", lessonPerformance.property_value);
        return database.insertWithOnConflict(TBL_LESSON_PERFORMANCE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long storeLessonHomework(LessonHomework lessonHomework) {
        ContentValues values = new ContentValues();
        values.put("lesson_id", lessonHomework.lesson_id);
        values.put("homework_id", lessonHomework.homework_id);
        values.put("title", lessonHomework.title);
        return database.insertWithOnConflict(TBL_LESSON_HOMEWORK, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long storeLessonParentFeedback(LessonParentFeedback feedback) {
        ContentValues values = new ContentValues();
        values.put("lesson_id", feedback.lesson_id);
        values.put("student_id", feedback.student_id);
        values.put("moment_id", feedback.moment_id);
        return database.insertWithOnConflict(TBL_LESSON_PARENT_FEEDBACK, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public long storeLessonAddHomework(LessonAddHomework homework,int homework_id) {
        ContentValues values = new ContentValues();
        values.put("homework_id", homework_id);
        values.put("lesson_id", homework.lesson_id);
        values.put("moment_id", homework.moment_id);
        return database.insertWithOnConflict(TBL_LESSON_ADD_HOMEWORK, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long deleteLessonHomework(int homework_id){
    	return database.delete(TBL_LESSON_ADD_HOMEWORK, "homework_id=?", new String[] { String.valueOf(homework_id) });
    }
    /**
     * @param class_id null 则清除所有课程的。
     * @return
     */
    public long deleteLesson(String class_id) {
        if (class_id == null)
            return database.delete(TBL_LESSON, null, null);
        else
            return database.delete(TBL_LESSON, "class_id=?", new String[] { class_id });
    }
    
    /**通过lessonId删除本地lesson
     * @param 
     * @return
     */
    public long deleteLessonById(String lessonId) {
         return database.delete(TBL_LESSON, "lesson_id=?", new String[] { lessonId });
    }

    public long deleteLessonPerformance(int lesson_id, String student_id) {
        return database.delete(TBL_LESSON_PERFORMANCE, "lesson_id=? AND student_id=?",
                new String[]{Integer.toString(lesson_id), student_id});
    }

    public long deleteLessonHomework(int lesson_id, int homework_id) {
        return database.delete(TBL_LESSON_HOMEWORK, "lesson_id=? AND homework_id=?",
                new String[]{Integer.toString(lesson_id), Integer.toString(homework_id)});
    }

    public List<Lesson> fetchLesson(String classId) {
        List<Lesson> result = new LinkedList<>();
        Cursor cur = database.query(TBL_LESSON,
                new String[] { "lesson_id", "title", "start_date", "end_date" ,"live"},
                "class_id=?", new String[] { classId },
                null, null, null);
        if (cur.moveToFirst()) {
            do {
                Lesson lesson = new Lesson();
                int i = -1;
                lesson.lesson_id = cur.getInt(++i);
                lesson.class_id = classId;
                lesson.title = cur.getString(++i);
                lesson.start_date = cur.getLong(++i);
                lesson.end_date = cur.getLong(++i);
                lesson.live = cur.getInt(++i);
                result.add(lesson);
            } while (cur.moveToNext());
        }
        return result;
    }

    public List<LessonPerformance> fetchLessonPerformance(int lessonId, String studentId) {
        List<LessonPerformance> result = new LinkedList<>();
        Cursor cur = database.query(TBL_LESSON_PERFORMANCE,
                new String[] { "property_id", "property_value" },
                "lesson_id=? AND student_id=?", new String[] { Integer.toString(lessonId), studentId },
                null, null, null);
        if (cur.moveToFirst()) {
            do {
                LessonPerformance performance = new LessonPerformance();
                int i = -1;
                performance.lesson_id = lessonId;
                performance.student_id = studentId;
                performance.property_id = cur.getInt(++i);
                performance.property_value = cur.getInt(++i);
                result.add(performance);
            } while (cur.moveToNext());
        }
        return result;
    }

    public List<LessonHomework> fetchLessonHomework(int lessonId) {
        List<LessonHomework> result = new LinkedList<>();
        Cursor cur = database.query(TBL_LESSON_HOMEWORK,
                new String[] { "homework_id", "title" },
                "lesson_id=?", new String[] { Integer.toString(lessonId) },
                null, null, null);
        if (cur.moveToFirst()) {
            do {
                LessonHomework homework = new LessonHomework();
                int i = -1;
                homework.lesson_id = lessonId;
                homework.homework_id = cur.getInt(++i);
                homework.title = cur.getString(++i);
                result.add(homework);
            } while (cur.moveToNext());
        }
        return result;
    }

    public LessonParentFeedback fetchLessonParentFeedback(int lessonId, String studentId) {
        LessonParentFeedback result = null;
        Cursor cur = database.query(TBL_LESSON_PARENT_FEEDBACK,
                new String[] { "moment_id" },
                "lesson_id=? AND student_id=?", new String[] { Integer.toString(lessonId), studentId },
                null, null, null);
        if (cur.moveToFirst()) {
            LessonParentFeedback feedback = new LessonParentFeedback();
            int i = -1;
            feedback.lesson_id = lessonId;
            feedback.student_id = studentId;
            feedback.moment_id = cur.getInt(++i);
            result = feedback;
        }
        return result;
    }
    public LessonAddHomework fetchLessonAddHomework(int lessonId) {
    	LessonAddHomework result = null;
        Cursor cur = database.query(TBL_LESSON_ADD_HOMEWORK,
                new String[] { "moment_id" },
                "lesson_id=?", new String[] { Integer.toString(lessonId)},
                null, null, null);
        if (cur.moveToFirst()) {
            LessonAddHomework addHomework = new LessonAddHomework();
            int i = -1;
            addHomework.lesson_id = lessonId;
            addHomework.moment_id = cur.getInt(++i);
            result = addHomework;
        }
        return result;
    }

    /**
     * @param schoolId
     * @param studentId
     * @param alias not null.
     * @return row ID.
     */
    public long storeStudentAlias(String schoolId, String studentId, String alias) {
        ContentValues values = new ContentValues(3);
        values.put("school_id", schoolId);
        values.put("student_id", studentId);
        values.put("alias", alias);
        return database.insertWithOnConflict(TBL_STUDENT_ALIAS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String fetchStudentAlias(String schoolId, String studentId) {
        String result = null;
        Cursor cur = database.query(TBL_STUDENT_ALIAS,
                new String[] { "alias" },
                "school_id=? AND student_id=?", new String[] { schoolId, studentId },
                null, null, null);
        if (cur.moveToFirst()) {
            result = cur.getString(0);
        }
        cur.close();
        return result;
    }

    /**
     * get school's ID by arguement "classId"
     * @param classId
     * @return
     */
    public String fetchSchoolIdByClassId(String classId){
    	String result = null;
    	Cursor cursor = database.query(TBL_GROUP, 
    			new String[]{"parent_group_id"}, 
    			"category = '__classroom__' AND group_id = ?", new String[]{classId}, 
    			null, null, null);
    	 if (cursor.moveToFirst()) {
             result = cursor.getString(0);
         }
    	 cursor.close();
    	return result;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //db table change notification wraper

    private  static LinkedList<Pair<String,IDBTableChangeListener>> dbTableChangeListenerList=new LinkedList<Pair<String,IDBTableChangeListener>>();
    public static void addDBTableChangeListener(String tableName,IDBTableChangeListener listener) {
        if(TextUtils.isEmpty(tableName) || null == listener) {
            Log.w("addDBTableChangeListener parameter wrong");
            return;
        }
        Log.i("listener for "+tableName+" added");
        synchronized (dbTableChangeListenerList) {
            dbTableChangeListenerList.add(new Pair<String,IDBTableChangeListener>(tableName,listener));
        }
    }

    public static void removeDBTableChangeListener(IDBTableChangeListener listener) {
        synchronized (dbTableChangeListenerList) {
            ListIterator<Pair<String,IDBTableChangeListener>> iterator=dbTableChangeListenerList.listIterator();
            while(iterator.hasNext()) {
                Pair<String,IDBTableChangeListener> aPair=iterator.next();
                if(aPair.second.equals(listener)) {
                    Log.i("listener for "+aPair.first+" removed");
                    iterator.remove();
                }
            }
        }
    }

    private static class dbTableModifiedNotifyRunnable implements Runnable {
        private boolean runningFlag=false;

        private final static long SLEEP_INTERVAL_WITH_NO_NOTIFY=500;//ms
        private final static long SLEEP_INTERVAL_WITH_NOTIFY=200;//ms
        private long sleep_interval=SLEEP_INTERVAL_WITH_NO_NOTIFY;

        private HashMap<String,Long> tableModifiedFirstTriggerMap=new HashMap<String,Long>();
        private HashMap<String,Long> tableModifiedMap=new HashMap<String,Long>();
        private ReentrantLock tableModifiedListLock = new ReentrantLock();

        private final static long NOTIFY_INTERVAL=400;//ms

        public dbTableModifiedNotifyRunnable() {

        }

        @Override
        public void run() {
            while(runningFlag) {
                try {
                    LinkedList<Pair<String,IDBTableChangeListener>> changeListenersList=new LinkedList<Pair<String,IDBTableChangeListener>>();

                    //lock and copy pair need update to list
                    //can not call listener in lock state,as you do not known how much time will be sonsumed by the listener
                    if(tableModifiedListLock.tryLock()) {
                        try {
                            Set<String> keySets=tableModifiedMap.keySet();
                            List<String> removedKeys=new LinkedList<String>();

                            for(String aKey : keySets) {
                                Long aValue=tableModifiedMap.get(aKey);
                                if(null != aValue && System.currentTimeMillis()-aValue >= NOTIFY_INTERVAL) {
                                    for(Pair<String,IDBTableChangeListener> aPair : dbTableChangeListenerList) {
                                        if(aPair.first.equals(aKey) && null != aPair.second) {
                                            changeListenersList.add(aPair);
                                        }
                                    }

                                    removedKeys.add(aKey);
                                }
                            }

                            for(String aRemovedKey : removedKeys) {
                                tableModifiedMap.remove(aRemovedKey);
                                tableModifiedFirstTriggerMap.remove(aRemovedKey);
                            }

                            if(tableModifiedMap.isEmpty()) {
                                sleep_interval=SLEEP_INTERVAL_WITH_NO_NOTIFY;
                            }
                        } finally {
                            tableModifiedListLock.unlock();
                        }
                    }

                    //call listener
                    for(Pair<String,IDBTableChangeListener> aPair : changeListenersList) {
                        Log.i("notify "+aPair.first+" modified");
                        aPair.second.onDBTableChanged(aPair.first);
                    }

                    Thread.sleep(sleep_interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            setRunningFlag(false);
        }

        public boolean isRunning() {
            return runningFlag;
        }

        public void setRunningFlag(boolean flag) {
            runningFlag=flag;
        }

        public void addDBModifiedToNotify(String table) {
            tableModifiedListLock.lock();
            try {
                //if a table is modified very frequently,it will not notify change for a long time
                //in such case,avoid it
                boolean notifyNew=true;
                if(!tableModifiedFirstTriggerMap.containsKey(table)) {
                    tableModifiedFirstTriggerMap.put(table,System.currentTimeMillis());
                } else {
                    long triggerTime=tableModifiedFirstTriggerMap.get(table);
                    if(System.currentTimeMillis()-triggerTime > 3*NOTIFY_INTERVAL) {
                        notifyNew=false;
                    }
                }

                if(notifyNew) {
                    tableModifiedMap.remove(table);
                    tableModifiedMap.put(table,System.currentTimeMillis());
                }
            } finally {
                tableModifiedListLock.unlock();
            }

            sleep_interval=SLEEP_INTERVAL_WITH_NOTIFY;
        }
    }
    private static dbTableModifiedNotifyRunnable delayNotifyRunnable=new dbTableModifiedNotifyRunnable();

    private static void markDBTableModified(String table) {
        //if thread not started,start it
        if(!delayNotifyRunnable.isRunning()) {
            delayNotifyRunnable.setRunningFlag(true);
            new Thread(delayNotifyRunnable).start();
        }

        //add table to notify
        synchronized (dbTableChangeListenerList) {
            Log.i("table "+table+" modified");
            delayNotifyRunnable.addDBModifiedToNotify(table);
//            for(Pair<String,IDBTableChangeListener> aPair : dbTableChangeListenerList) {
//                if(aPair.first.equals(table) && null != aPair.second) {
//                    aPair.second.onDBTableChanged(table);
//                }
//            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME_PRE = GlobalSetting.DATABASE_NAME;
    private static final int DATABASE_VERSION = 13;
    public int flagIndex;

	private static final String DATABASE_CREATE_TBL_CHATMESSAGES = "CREATE TABLE IF NOT EXISTS `chatmessages` "
			+ "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ " `chattarget` TEXT,"
			+ " `display_name` TEXT,"
			+ " `msgtype` TEXT,"
			+ " `sentdate` TEXT,"
			+ " `iotype` TEXT,"
			+ " `sentstatus` TEXT,"
			+ " `is_group_chat` INTEGER,"
			+ " `group_chat_sender_id` TEXT,"
			+ " `read_count` INTEGER,"
			+ " `path_thumbnail` TEXT,"
			+ " `path_multimedia` TEXT,"
            + " `path_multimedia2` TEXT,"
			+ " `msgcontent` TEXT,"
			+ " `unique_key` TEXT); ";

	private static final String DATABASE_CREATE_TBL_CHATMESSAGE_READED = "CREATE TABLE IF NOT EXISTS " + Database.TBL_CHATMESSAGE_READED
			+ "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ " `unique_key` TEXT,"
			+ " `member_id` TEXT); ";

	private static final String DATABASE_CREATE_TBL_CALLLOGS = "CREATE TABLE IF NOT EXISTS `calllogs` "
			+ "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ " `contact` TEXT,"
			+ " `display_name` TEXT,"
			+ " `callstatus` TEXT,"
			+ " `direction` TEXT,"
			+ " `startdate` TEXT," + " `duration` TEXT," + " `quality` TEXT);";

	private static final String DATABASE_CREATE_TBL_GROUP = "CREATE TABLE IF NOT EXISTS " + Database.TBL_GROUP + " ("
			+ " `group_id` TEXT PRIMARY KEY DEFAULT '',"
			+ " `short_group_id` TEXT DEFAULT '',"
			+ " `group_name_original` TEXT DEFAULT '',"
			+ " `group_name_local` TEXT DEFAULT '',"
			+ " `group_status` TEXT DEFAULT '',"
			+ " `max_number` INTEGER,"
			+ " `member_count` INTEGER,"
			+ " `photo_upload_timestamp` INTEGER,"
            + " `lat_e6` INTEGER NOT NULL DEFAULT " + Database.INVALID_LATLON + ","
            + " `lon_e6` INTEGER NOT NULL DEFAULT " + Database.INVALID_LATLON + ","
            + " `place` TEXT DEFAULT ''," // place name, or address
            + " `category` TEXT DEFAULT ''," // { family, friends, etc }
            + " `will_block_msg` INTEGER DEFAULT 0," // block new msg from this buddy?
            + " `will_block_msg_notification` INTEGER DEFAULT 0," // block notification of new msg from this buddy?
            + " `alias` TEXT DEFAULT '',"
            + " `my_nick_here` TEXT DEFAULT '',"
			+ " `temp_group_flag` INTEGER,"
            + " `is_group_name_changed` INTEGER DEFAULT 0,"
			+ " `is_me_belongs` INTEGER DEFAULT 1,"
			+ " `owner` TEXT DEFAULT '',"
			+ " `weight` INTEGER DEFAULT 0,"
            + " `editable` INTEGER DEFAULT 1,"
            + " `parent_group_id` TEXT);";

	private static final String DATABASE_CREATE_TBL_GROUP_MEMBER = "CREATE TABLE IF NOT EXISTS " + Database.TBL_GROUP_MEMBER + " ("
			+ "`group_id` TEXT," + " `member_id` TEXT," + " `level` INTEGER);";

	// Database creation sql statement
	private static final String DATABASE_CREATE_TBL_BUDDIES = "CREATE TABLE IF NOT EXISTS `buddies` "
			+ "(`uid` TEXT PRIMARY KEY NOT NULL DEFAULT '',"
			+ " `phone_number` TEXT DEFAULT '',"
            + " `alias` TEXT DEFAULT ''," // alias name
            + " `will_block_msg` INTEGER DEFAULT 0," // block new msg from this buddy?
            + " `will_block_msg_notification` INTEGER DEFAULT 0," // block notification of new msg from this buddy?
            + " `favorite` INTEGER DEFAULT 0,"
            + " `hidden` INTEGER DEFAULT 0," // hide this buddy from contacts list view?
			+ " `friendship` INTEGER); "; // see Buddy.FRIENDSHIP_* constants.

	// Database creation sql statement
	private static final String DATABASE_CREATE_TBL_BUDDY_DETAIL = "CREATE TABLE IF NOT EXISTS " + Database.TBL_BUDDY_DETAIL
			+ "(`uid` TEXT PRIMARY KEY,"
			+ " `wowtalkid` TEXT,"
			+ " `nickname` TEXT,"
			+ " `pronunciation` TEXT,"
			+ " `mobile_phone` TEXT,"
			+ " `last_status` TEXT,"
			+ " `sex` INTEGER,"
			+ " `area` TEXT,"
			+ " `email` TEXT,"
			+ " `job_title` TEXT,"
			+ " `employee_id` TEXT,"
			+ " `device_number` TEXT,"
			+ " `app_ver` TEXT,"
            + " `sort_key` TEXT,"
            + " `account_type` INTEGER,"
            + " `album_cover_fileid` TEXT,"
            + " `album_cover_ext` TEXT,"
			+ " `album_cover_upload_timestamp` INTEGER,"
			+ " `photo_upload_timestamp` INTEGER,"
			+ " `photo_filepath` TEXT,"
			+ " `thumbnail_filepath` TEXT,"
			+ " `need_to_download_photo` INTEGER,"
			+ " `need_to_download_thumbnail` INTEGER); ";

	// Database creation sql statement
	private static final String DATABASE_CREATE_TBL_BLOCK_LIST = "CREATE TABLE IF NOT EXISTS `block_list` "
			+ "(`uid` TEXT PRIMARY KEY); ";
	
	// Database creation sql statement
	private static final String DATABASE_CREATE_TBL_VIDEOCALL_UNSUPPORTED_DEVICE_LIST = "CREATE TABLE IF NOT EXISTS `videocall_unsupported_device_list` "
			+ "(`device_number` TEXT PRIMARY KEY); ";

    // Database creation sql statement
    private static final String DATABASE_CREATE_TBL_UNSENT_RECEIPTS = "CREATE TABLE IF NOT EXISTS `unsent_receipts` "
            + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " `chattarget` TEXT,"
            + " `msgtype` TEXT,"
            + " `messagebody` TEXT); ";

    // create events table
	private static final String DATABASE_CREATE_TBL_EVENT = "CREATE TABLE IF NOT EXISTS `event` "
			+ "(`id` TEXT PRIMARY KEY,"
			+ "`address` TEXT,"
			+ "`telephone` TEXT,"
			+ "`allowReview` INTEGER,"
			+ "`capacity` INTEGER,"
			+ "`costGolds` INTEGER,"
			+ "`createdTime` INTEGER,"
			+ "`description` TEXT,"
			+ "`isOfficial` INTEGER,"
			+ "`latitude` FLOAT,"
			+ "`longitude` FLOAT,"
			+ "`membership` INTEGER,"
			+ "`needWork` INTEGER,"
			+ "`is_get_member_info` INTEGER,"
			+ "`owner_uid` TEXT,"
            + "`host` TEXT,"
			+ "`privacy_level` INTEGER,"
			+ "`size` INTEGER,"
			+ "`startTime` INTEGER,"
            + "`endTime` INTEGER,"
			+ "`target_user_type` INTEGER,"
			+ "`title` TEXT,"
            + "`contact_email` TEXT,"
            + "`joinedMemberCount` INTEGER,"
            + "`possibleJoinedMemberCount` INTEGER,"
            + "`event_start_date` TEXT,"
            + "`event_dead_line` TEXT,"
            + "`timeStamp` TEXT,"
            + "`thumbNail` TEXT,"
            + "`event_type` INTEGER,"
            + "`tag` TEXT,"
            + "`category` TEXT);";
	
	// create event multimedia table
	private static final String DATABASE_CREATE_TBL_EVENT_MEDIA = "CREATE TABLE IF NOT EXISTS `event_media` "
			+ "(`event_id` TEXT,"
			+ "`ext` TEXT,"
            + "`fileid` TEXT,"
            + "`thumb_fileid` TEXT," // only if this media has thumbnail
            + "`duration` INTEGER," // only if this media is voice, in seconds
            + "`localDbId` INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "`remoteDbId` TEXT);";
	
	// create event review table
	private static final String DATABASE_CREATE_TBL_EVENT_REVIEW = "CREATE TABLE IF NOT EXISTS `event_review` "
			+ "(`id` TEXT PRIMARY KEY,"
			+ "`event_id` TEXT,"
            + "`timestamp` INTEGER,"
			+ "`text` TEXT,"
			+ "`type` INTEGER,"
            + "`uid` TEXT,"
			+ "`nickname` TEXT,"
            + "`read` INTEGER DEFAULT 1," // read or unread?
            + "`reply_to_review_id` TEXT,"
            + "`reply_to_uid` TEXT,"
            + "`reply_to_nickname` TEXT);";

    // create moments table
    private static final String DATABASE_CREATE_TBL_MOMENT = "CREATE TABLE IF NOT EXISTS `moment` "
            + "(`id` TEXT PRIMARY KEY,"
            + "`text` TEXT,"
            + "`latitude` FLOAT,"
            + "`longitude` FLOAT,"
            + "`place` TEXT,"
            + "`allow_review` INTEGER,"
            + "`owner_uid` TEXT,"
            + "`privacy_level` INTEGER,"
            + "`timestamp` INTEGER,"
            + "`tag` TEXT,"
            + "`survey_allow_multi_select` INTEGER DEFAULT 0,"
            + "`survey_voted_option` TEXT,"
            + "`survey_dead_line` INT," // unix time stamp
            + "`limited_departments` TEXT,"
            + "`is_favorite` INTEGER,"
            + "`liked_by_me` INTEGER);";

    private static final String DATABASE_CREATE_TBL_SURVEY = "CREATE TABLE IF NOT EXISTS `survey` "
            + "(`moment_id` TEXT,"
            + "`option_id` TEXT,"
            + "`is_voted` INTEGER,"
            + "`option_desc` TEXT,"
            + "`voted_num` INTEGER DEFAULT 0);";

    // create event multimedia table
    private static final String DATABASE_CREATE_TBL_MOMENT_MEDIA = "CREATE TABLE IF NOT EXISTS `moment_media` "
            + "(`moment_id` TEXT,"
            + "`ext` TEXT,"
            + "`fileid` TEXT,"
            + "`thumb_fileid` TEXT," // only if this media has thumbnail
            + "`duration` INTEGER," // only if this media is voice, in seconds
            + "`localDbId` INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "`remoteDbId` TEXT);";

    // create event review table
    private static final String DATABASE_CREATE_TBL_MOMENT_REVIEW = "CREATE TABLE IF NOT EXISTS `moment_review` "
            + "(`id` TEXT PRIMARY KEY,"
            + "`moment_id` TEXT,"
            + "`timestamp` INTEGER,"
            + "`text` TEXT,"
            + "`type` INTEGER,"
            + "`uid` TEXT,"
            + "`nickname` TEXT,"
            + "`read` INTEGER DEFAULT 1," // read or unread?
            + "`reply_to_review_id` TEXT,"
            + "`reply_to_uid` TEXT,"
            + "`reply_to_nickname` TEXT);";

    private static final String DATABASE_CREATE_TBL_PENDING_REQUESTS = "CREATE TABLE IF NOT EXISTS " + Database.TBL_PENDING_REQUESTS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "type INTEGER NOT NULL," // enum PendingRequest
            + "uid TEXT NOT NULL,"
            + "nickname TEXT NOT NULL,"
            + "buddy_photo_timestamp INTEGER,"
            + "group_id TEXT,"
            + "group_name TEXT,"
            + "group_photo_timestamp INTEGER,"
            + "msg TEXT"
            + ");";

    @Deprecated
    private static final String DATABASE_CREATE_TBL_LATEST_CONTACTS = "CREATE TABLE IF NOT EXISTS " + Database.TBL_LATEST_CONTACTS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            // 0, single_contact; 1, temp_group / group
            + "is_group INTEGER NOT NULL,"
            + "target_id TEXT NOT NULL,"
            + "latest_time LONG NOT NULL"
            + ");";

    private static final String DATABASE_CREATE_TBL_LATEST_CHAT_TARGET = "CREATE TABLE IF NOT EXISTS " + Database.TBL_LATEST_CHAT_TARGET + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            // 0, single_contact; 1, temp_group / group
            + "is_group INTEGER NOT NULL,"
            + "target_id TEXT NOT NULL,"
            + "sentdate TEXT,"
            + "message_id TEXT"
            + ");";

    private static final String DATABASE_CREATE_TBL_FAVORITE = "CREATE TABLE IF NOT EXISTS " + Database.TBL_FAVORITE + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            // 0, contact; 1, group
            + "type INTEGER NOT NULL,"
            + "target_id TEXT NOT NULL,"
            + "favorite_level INTEGER"
            + ");";

    private static final String DATABASE_CREATE_TBL_CHATMESSAGES_HISTORY = "CREATE TABLE IF NOT EXISTS " + Database.TBL_MESSAGES_HISTORY + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "chattarget TEXT NOT NULL,"
            + "msgtype TEXT,"
            + "sentdate TEXT,"
            + "iotype TEXT,"
            + "is_group_chat INTEGER,"
            + "group_chat_sender_id TEXT,"
            + "path_thumbnail TEXT,"
            + "path_multimedia TEXT,"
            + "path_multimedia2 TEXT,"
            + "msgcontent TEXT,"
            + "local_item INTEGER"
            + ");";

    private static final String DATABASE_CREATE_TBL_LESSON =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_LESSON + " ("
                    + "lesson_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "class_id TEXT NOT NULL,"
                    + "title TEXT,"
                    + "start_date INTEGER,"
                    + "end_date INTEGER,"
                    + "live INTEGER"
                    + ");";

    private static final String DATABASE_CREATE_TBL_LESSON_PERFORMANCE =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_LESSON_PERFORMANCE + " ("
                    + "performance_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "lesson_id INTEGER NOT NULL,"
                    + "student_id TEXT NOT NULL,"
                    + "property_id INTEGER,"
                    + "property_value INTEGER"
                    + ");";

    private static final String DATABASE_CREATE_TBL_LESSON_HOMEWORK =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_LESSON_HOMEWORK + " ("
                    + "lesson_id INTEGER NOT NULL,"
                    + "homework_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL"
                    + ");";

    private static final String DATABASE_CREATE_TBL_LESSON_PARENT_FEEDBACK =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_LESSON_PARENT_FEEDBACK + " ("
                    + "feedback_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "lesson_id INTEGER NOT NULL,"
                    + "student_id TEXT NOT NULL,"
                    + "moment_id INTEGER NOT NULL"
                    + ");";
                    
    private static final String DATABASE_CREATE_TBL_LESSON_ADD_HOMEWORK =
    		"CREATE TABLE IF NOT EXISTS " + Database.TBL_LESSON_ADD_HOMEWORK + " ("
                    + "homework_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "lesson_id INTEGER NOT NULL,"
                    + "moment_id INTEGER NOT NULL"
                    + ");";                 

    private static final String DATABASE_CREATE_TBL_STUDENT_ALIAS =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_STUDENT_ALIAS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "school_id INTEGER NOT NULL,"
                    + "student_id TEXT NOT NULL,"
                    + "alias TEXT NOT NULL"
                    + ");";
    
    private static final String DATABASE_CREATE_TBL_SAVE_LAST_MESSAGE =
            "CREATE TABLE IF NOT EXISTS " + Database.TBL_SAVE_MESSAGE + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "_targetUID_id TEXT NOT NULL,"
                    + "messageContent TEXT "
                    + ");";

    private static final String DATABASE_CREATE_INDEX_0="CREATE INDEX IF NOT EXISTS " + " idx_moment_ownUid "
            + " ON " + Database.TBL_MOMENT + " ( " + " owner_uid " + " );";
    private static final String DATABASE_CREATE_INDEX_1="CREATE INDEX IF NOT EXISTS " + " idx_moment_timestamp "
            + " ON " + Database.TBL_MOMENT + " ( " + " timestamp " + " );";
    private static final String DATABASE_CREATE_INDEX_2="CREATE INDEX IF NOT EXISTS " + " idx_moment_review_momentIdAndRead "
            + " ON " + Database.TBL_MOMENT_REVIEWS + " ( " + " moment_id,read " + " );";
    private static final String DATABASE_CREATE_INDEX_3="CREATE INDEX IF NOT EXISTS " + " idx_chatmessages_chatTargetAndID "
            + " ON " + Database.TBL_MESSAGES + " ( " + " chattarget,id " + " );";
    private static final String DATABASE_CREATE_INDEX_4="CREATE INDEX IF NOT EXISTS " + " idx_Event_timeStamp "
            + " ON " + Database.TBL_EVENT + " ( " + " timeStamp " + " );";
    private static final String DATABASE_CREATE_INDEX_5="CREATE UNIQUE INDEX IF NOT EXISTS " + " idx_lesson_performance "
            + " ON " + Database.TBL_LESSON_PERFORMANCE + " ( lesson_id, student_id, property_id );";
    private static final String DATABASE_CREATE_INDEX_6="CREATE UNIQUE INDEX IF NOT EXISTS " + " idx_lesson_parent_feedback "
            + " ON " + Database.TBL_LESSON_PARENT_FEEDBACK + " ( lesson_id, student_id );";
    private static final String DATABASE_CREATE_INDEX_7="CREATE UNIQUE INDEX IF NOT EXISTS " + " idx_school_student "
            + " ON " + Database.TBL_STUDENT_ALIAS + " ( school_id, student_id );";
    private static final String DATABASE_CREATE_INDEX_8="CREATE UNIQUE INDEX IF NOT EXISTS " + " idx_target_uid_id "
            + " ON " + Database.TBL_SAVE_MESSAGE + " ( _targetUID_id );";

    public DatabaseHelper(Context context, String uid, int flagIndex) {
        super(context, DATABASE_NAME_PRE + "_" + uid, null, DATABASE_VERSION);
        this.flagIndex = flagIndex;
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_TBL_CHATMESSAGES);
        database.execSQL(DATABASE_CREATE_TBL_CHATMESSAGE_READED);
        database.execSQL(DATABASE_CREATE_TBL_CALLLOGS);
        database.execSQL(DATABASE_CREATE_TBL_GROUP);
        database.execSQL(DATABASE_CREATE_TBL_GROUP_MEMBER);
        database.execSQL(DATABASE_CREATE_TBL_BUDDIES);
        database.execSQL(DATABASE_CREATE_TBL_BUDDY_DETAIL);
        database.execSQL(DATABASE_CREATE_TBL_BLOCK_LIST);
        database.execSQL(DATABASE_CREATE_TBL_VIDEOCALL_UNSUPPORTED_DEVICE_LIST);
        database.execSQL(DATABASE_CREATE_TBL_UNSENT_RECEIPTS);
        database.execSQL(DATABASE_CREATE_TBL_EVENT);
        database.execSQL(DATABASE_CREATE_TBL_EVENT_MEDIA);
        database.execSQL(DATABASE_CREATE_TBL_EVENT_REVIEW);
        database.execSQL(DATABASE_CREATE_TBL_MOMENT);
        database.execSQL(DATABASE_CREATE_TBL_MOMENT_MEDIA);
        database.execSQL(DATABASE_CREATE_TBL_MOMENT_REVIEW);
        database.execSQL(DATABASE_CREATE_TBL_PENDING_REQUESTS);
//        database.execSQL(DATABASE_CREATE_TBL_LATEST_CONTACTS);
        database.execSQL(DATABASE_CREATE_TBL_LATEST_CHAT_TARGET);
        database.execSQL(DATABASE_CREATE_TBL_FAVORITE);
        database.execSQL(DATABASE_CREATE_TBL_CHATMESSAGES_HISTORY);
        database.execSQL(DATABASE_CREATE_TBL_SURVEY);

        database.execSQL(DATABASE_CREATE_TBL_LESSON);
        database.execSQL(DATABASE_CREATE_TBL_LESSON_PERFORMANCE);
        database.execSQL(DATABASE_CREATE_TBL_LESSON_HOMEWORK);
        database.execSQL(DATABASE_CREATE_TBL_LESSON_PARENT_FEEDBACK);
        database.execSQL(DATABASE_CREATE_TBL_LESSON_ADD_HOMEWORK);
        database.execSQL(DATABASE_CREATE_TBL_STUDENT_ALIAS);
        database.execSQL(DATABASE_CREATE_TBL_SAVE_LAST_MESSAGE);

        database.execSQL(DATABASE_CREATE_INDEX_0);
        database.execSQL(DATABASE_CREATE_INDEX_1);
        database.execSQL(DATABASE_CREATE_INDEX_2);
        database.execSQL(DATABASE_CREATE_INDEX_3);
        database.execSQL(DATABASE_CREATE_INDEX_4);
        database.execSQL(DATABASE_CREATE_INDEX_5);
        database.execSQL(DATABASE_CREATE_INDEX_6);
        database.execSQL(DATABASE_CREATE_INDEX_7);
        database.execSQL(DATABASE_CREATE_INDEX_8);

    }

    // Method is called during an upgrade of the database, e.g. if you increase
    // the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion
                + " to " + newVersion);

        if (oldVersion == 1) {
            database.execSQL("drop table " + Database.TBL_LATEST_CONTACTS);
            database.execSQL(DATABASE_CREATE_TBL_LATEST_CHAT_TARGET);
            oldVersion++;
        }

        if (oldVersion == 2) {
            database.execSQL("delete from " + Database.TBL_UNSENT_RECEIPTS);
            oldVersion++;
        }

        if (oldVersion == 3) {
            // 修改remote_key的字段名称(remote_key == > unique_key)及属性(Integer == > TEXT)
            // 不能直接修改字段的属性，分以下5步：
            // 1. 将表名改为临时表
            database.execSQL("ALTER TABLE chatmessages RENAME TO chatmessages_temp;");
            // 2. 创建新表
            database.execSQL(DATABASE_CREATE_TBL_CHATMESSAGES);
            // 3. 导入数据
            database.execSQL("INSERT INTO chatmessages"
                    + "     (id,chattarget,display_name,msgtype,sentdate,iotype,"
                    + "     sentstatus,is_group_chat,group_chat_sender_id,read_count,"
                    + "     path_thumbnail,path_multimedia,path_multimedia2,msgcontent,unique_key)"
                    + " SELECT id,chattarget,display_name,msgtype,sentdate,iotype,"
                    + "     sentstatus,is_group_chat,group_chat_sender_id,read_count,"
                    + "     path_thumbnail,path_multimedia,path_multimedia2,msgcontent,remote_key"
                    + " FROM chatmessages_temp;");
            // 4. 更新sqlite_sequence
            database.execSQL("update sqlite_sequence set seq=(select max(id) from chatmessages) where name='chatmessages';");
            // 5. 删除临时表
            database.execSQL("DROP TABLE chatmessages_temp;");

            database.execSQL(DATABASE_CREATE_TBL_CHATMESSAGE_READED);

            oldVersion++;
        }

        if (oldVersion == 4) {
            // 添加 endTime 字段到 event 表
            database.execSQL("ALTER TABLE event ADD COLUMN endTime INTEGER AFTER startTime;");
            database.execSQL("UPDATE event SET endTime=startTime;");
            // 添加 host 字段到 event 表
            database.execSQL("ALTER TABLE event ADD COLUMN host TEXT AFTER owner_uid;");
            ++oldVersion;
        }

        if (oldVersion == 5) {
            // 添加 path_multimedia2 字段到 chatmessages, chatmessages_history 表
            database.execSQL("ALTER TABLE " + Database.TBL_MESSAGES +
                " ADD COLUMN path_multimedia2 TEXT AFTER path_multimedia;");
            database.execSQL("ALTER TABLE " + Database.TBL_MESSAGES_HISTORY +
                    " ADD COLUMN path_multimedia2 TEXT AFTER path_multimedia;");
            ++oldVersion;
        }

        if (oldVersion == 6) {
            database.execSQL("ALTER TABLE event ADD COLUMN is_get_member_info INTEGER;");

            database.execSQL(DATABASE_CREATE_TBL_LESSON);
            database.execSQL(DATABASE_CREATE_TBL_LESSON_PERFORMANCE);
            database.execSQL(DATABASE_CREATE_TBL_LESSON_HOMEWORK);
            database.execSQL(DATABASE_CREATE_TBL_LESSON_PARENT_FEEDBACK);

            database.execSQL(DATABASE_CREATE_INDEX_5);
            database.execSQL(DATABASE_CREATE_INDEX_6);

            ++oldVersion;
        }

        if (oldVersion == 7) {
            database.execSQL(DATABASE_CREATE_TBL_STUDENT_ALIAS);
            database.execSQL(DATABASE_CREATE_INDEX_7);
            ++oldVersion;
        }
        if(oldVersion == 8){
        	database.execSQL(DATABASE_CREATE_TBL_SAVE_LAST_MESSAGE);
            database.execSQL(DATABASE_CREATE_INDEX_8);
        	++oldVersion;
        }
        if(oldVersion == 9){
        	database.execSQL("ALTER TABLE event ADD COLUMN telephone Text;");
        	++oldVersion;
        }

        if (oldVersion == 10) {
            // moment table:
            //  1, drop share_range col
            //  2, survey_dead_line TEXT -> INT
            // Sqlite ALTER TABLE 语句不支持 DROP COLUMN 和 MODIFY COLUMN，
            // 所以干脆 DROP & CREATE TABLE，反正 moment 表的数据清除也没关系。
            database.execSQL("DROP TABLE moment;");
            database.execSQL(DATABASE_CREATE_TBL_MOMENT);
            ++oldVersion;
        }
        if(oldVersion == 11){
        	database.execSQL("ALTER TABLE lesson ADD COLUMN live INTEGER;");
        }
        if(oldVersion == 12){
        	database.execSQL(DATABASE_CREATE_TBL_LESSON_ADD_HOMEWORK);
        	
        }
    }
}

