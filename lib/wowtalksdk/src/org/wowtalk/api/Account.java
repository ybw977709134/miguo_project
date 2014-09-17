package org.wowtalk.api;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable{

    private static final String KEY_UID = "uid";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_WOWTALK_ID = "wowtalk_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_COMPANY = "company";
    private static final String KEY_PORTRAIT_UPLOAD_TIME_STAMP = "portrait_upload_time_stamp";
    private static final String KEY_ALBUM_COVER_TIME_STAMP = "album_cover_time_stamp";
    private static final String KEY_ALBUM_COVER_FILEID = "album_cover_fileid";
    private static final String KEY_ALBUMCOVER_EXT = "album_cover_ext";
    private static final String KEY_IS_ONLINE = "is_online";
    private static final String KEY_UNREAD_COUNTS = "unread_counts";

    public String uid = "";
    public String password;
    public String wowtalkId;
    public String name = "";
    public String company = "";
    /**
     * 头像
     */
    public long photoUploadTimeStamp;
    public long albumCoverTimeStamp;
    public String albumCoverFileId = "";
    public String albumCoverExt = "";
    public boolean isOnline;
    public int unreadCounts;

    public Account() {
    }

    public Account(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            uid = jsonObject.getString(KEY_UID);
            password = jsonObject.getString(KEY_PASSWORD);
            wowtalkId = jsonObject.getString(KEY_WOWTALK_ID);
            name = jsonObject.getString(KEY_NAME);
            company = jsonObject.getString(KEY_COMPANY);
            photoUploadTimeStamp = jsonObject.getLong(KEY_PORTRAIT_UPLOAD_TIME_STAMP);
            if (!jsonObject.isNull(KEY_ALBUM_COVER_TIME_STAMP)) {
                albumCoverTimeStamp = jsonObject.getLong(KEY_ALBUM_COVER_TIME_STAMP);
            }
            albumCoverFileId = jsonObject.getString(KEY_ALBUM_COVER_FILEID);
            albumCoverExt = jsonObject.getString(KEY_ALBUMCOVER_EXT);
            isOnline = jsonObject.getBoolean(KEY_IS_ONLINE);
            unreadCounts = jsonObject.getInt(KEY_UNREAD_COUNTS);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_UID, uid);
            json.put(KEY_PASSWORD, password);
            json.put(KEY_WOWTALK_ID, wowtalkId);
            json.put(KEY_NAME, name);
            json.put(KEY_COMPANY, company);
            json.put(KEY_PORTRAIT_UPLOAD_TIME_STAMP, photoUploadTimeStamp);
            json.put(KEY_ALBUM_COVER_TIME_STAMP, albumCoverTimeStamp);
            json.put(KEY_ALBUM_COVER_FILEID, albumCoverFileId);
            json.put(KEY_ALBUMCOVER_EXT, albumCoverExt);
            json.put(KEY_IS_ONLINE, isOnline);
            json.put(KEY_UNREAD_COUNTS, unreadCounts);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return json;
    }

    /**
     * 只获取在SP中有保存的部分信息
     * @param prefUtil
     * @return 当前登录的帐户信息，如果没有登录的帐户，则返回null
     */
    public static Account getAccountFromSP(PrefUtil prefUtil) {
        if (null == prefUtil || !prefUtil.isLogined()) {
            return null;
        }

        Account account = new Account();
        account.uid = prefUtil.getUid();
        account.password = prefUtil.getPassword();
        account.wowtalkId = prefUtil.getMyWowtalkID();
        account.name = prefUtil.getMyNickName();
        account.company = prefUtil.getCompanyId();
        account.photoUploadTimeStamp = prefUtil.getMyPhotoUploadedTimestamp();
        return account;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(password);
        dest.writeString(wowtalkId);
        dest.writeString(name);
        dest.writeString(company);
        dest.writeLong(photoUploadTimeStamp);
        dest.writeLong(albumCoverTimeStamp);
        dest.writeString(albumCoverFileId);
        dest.writeString(albumCoverExt);
        dest.writeInt(isOnline ? 1 : 0);
        dest.writeInt(unreadCounts);
    }

    public static final Parcelable.Creator<Account> CREATOR
            = new Parcelable.Creator<Account>() {
                @Override
                public Account createFromParcel(Parcel source) {
                    Account account = new Account();
                    account.uid = source.readString();
                    account.password = source.readString();
                    account.wowtalkId = source.readString();
                    account.name = source.readString();
                    account.company = source.readString();
                    account.photoUploadTimeStamp = source.readLong();
                    account.albumCoverTimeStamp = source.readLong();
                    account.albumCoverFileId = source.readString();
                    account.albumCoverExt = source.readString();
                    account.isOnline = source.readInt() == 1;
                    account.unreadCounts = source.readInt();
                    return account;
                }

                @Override
                public Account[] newArray(int size) {
                    return new Account[size];
                }
            };

}
