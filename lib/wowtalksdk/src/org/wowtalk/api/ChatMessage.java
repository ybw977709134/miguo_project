package org.wowtalk.api;


import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import com.amazonaws.util.StringInputStream;
import junit.framework.Assert;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
[type definition]



1:tell message sender i have received ur msg
//messgae payload :   type |  content(groupID optional)
// length         :    "1" |  "{"$groupID"}" "$msgId"

2:tell caller i am online now
//messgae payload :   type |  yyyy/MM/dd HH:mm| 
// length         :    "2" |  <------16------>|

3:tell callee you missed my call
//messgae payload :   type | yyyy/MM/dd HH:mm| 
// length         :    "3" |  <------16------>|

4:tell callee you must kill your call now.
//messgae payload :   type |  yyyy/MM/dd HH:mm| 
// length         :    "4" |  <------16------>|

5:tell callee i reject for your normal call
//messgae payload :   type |  yyyy/MM/dd HH:mm| 
// length         :    "5" |  NULL
6:tell callee i want to start video call
//messgae payload :   type |  yyyy/MM/dd HH:mm| 
// length         :    "6" |  <------16------>|

7:tell callee i reject for your video call
//messgae payload :   type |  yyyy/MM/dd HH:mm| 
// length         :    "7" |  <------16------>|

MSGTYPE_SENT_MSG_READED_RECEIPT
8:tell message sender i have readed ur msg
//messgae payload :   type |  content(groupID optional)
// length         :    "8" | "{"$groupID"}""$msgId"



MSGTYPE_ENCRIPTED_TXT_MESSAGE
9:encripted text message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content  ($senderID optional)
// length         :    "9" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" $body

0:text message
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id    |content
// length         :    "0" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" $body

MSGTYPE_LOCATION
a:location message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "a" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" <経度,緯度><Others>

MSGTYPE_MULTIMEDIA_STAMP
b:stamp message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "b" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" <stampID><stampText><Others>

MSGTYPE_MULTIMEDIA_PHOTO
c:photo message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "c" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" <thumbnail url><content url>

MSGTYPE_MULTIMEDIA_VOICE_NOTE
d:voice message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "d" | <------16------>|"{"$msgId"}"  |"{"$senderID"}" <content url>

MSGTYPE_MULTIMEDIA_VIDEO_NOTE
e:video message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "e" | <------16------>|"{"$msgId"}"  |"{"$senderID"}"  <thumbnail url><content url>

MSGTYPE_MULTIMEDIA_VCF
f:vcf message
//encripted part           | <------------encripted---------------------------------------------->|
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content ($senderID optional)
// length         :    "f" | <------16------>|"{"$msgId"}"  |"{"$senderID"}"  <vcf content>

MSGTYPE_GROUPCHAT_JOIN_REQUEST
g:invitation for group chat room 
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content        　 
// length         :    "g" | <------16------>|"{"$msgId"}"  |{}$groupid


MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM
h:someone join the group i am involved
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content
// length         :    "h" | <------16------>|"{"$msgId"}"  |"{"$senderID"}"

MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM
i:someone leave the group i am involved
//messgae payload :   type | yyyy/MM/dd HH:mm| msg id       |content
// length         :    "i" | <------16------>|"{"$msgId"}"  |"{"$senderID"}"

MSGTYPE_PIC_VOICE
s:hybird(image+voice+text) msg content:
{
    "text":"...",

    // image (field names are compliant with MSGTYPE_MULTIMEDIA_PHOTO)
    "pathoffileincloud":"8f623afb-e841-4178-b538-ff7813867b55",
    "pathofthumbnailincloud":"be298ccb-42a3-4d79-825f-bf1bc01a77bf",
    "ext":".jpg"

    // audio
    "audio_pathoffileincloud":"8f623afb-e841-4178-b538-ff7813867b55",
    "audio_ext":".m4a"
    "duration":"9",
}


*/
public class ChatMessage {

	/** {@link #extraData} 的 key：是否正在上传或下载附件？ */
	public static final String EXTRA_DATA_IS_TRANSFERRING = "isTransferring";
	/** {@link #extraData} 的 key：上传或下载附件的进度（百分比）？ */
	public static final String EXTRA_DATA_PROGRESS = "progress";
	/** {@link #extraObjects} 的 key：UI视图中的进度条 */
	public static final String EXTRA_OBJ_PROGRESSBAR = "progressBar";

	private static final String PREFIX = "org.wowtalk.chatmessage.";
	private static final String EXTRAS_PRIMARY_ID = PREFIX
			+ "EXTRAS_PRIMARY_ID";
	private static final String EXTRAS_CONTACT_ID = PREFIX
			+ "EXTRAS_CONTACT_ID";
	private static final String EXTRAS_CONTACT_DISPLAYNAME = PREFIX
			+ "EXTRAS_CONTACT_DISPLAYNAME";
	private static final String EXTRAS_MESSAGE_TYPE = PREFIX
			+ "EXTRAS_MESSAGE_TYPE";
	private static final String EXTRAS_MESSAGE_CONTENT = PREFIX
			+ "EXTRAS_MESSAGE_CONTENT";
	private static final String EXTRAS_SENTDATE = PREFIX + "EXTRAS_SENTDATE";

	private static final String EXTRAS_IS_WOWTALK_MSG = PREFIX
			+ "EXTRAS_IS_WOWTALK_MSG";
	private static final String EXTRAS_IOTYPE = PREFIX
			+ "EXTRAS_IOTYPE";
	private static final String EXTRAS_ISGROUPCHAT = PREFIX
			+ "EXTRAS_ISGROUPCHAT";
	private static final String EXTRAS_GROUP_SENDER = PREFIX
			+ "EXTRAS_GROUP_SENDER";
    private static final String EXTRAS_UNIQUE_KEY = PREFIX
            + "EXTRAS_UNIQUE_KEY";
    private static final String EXTRAS_SENT_STATUS = PREFIX
            + "EXTRAS_SENT_STATUS";
    private static final String EXTRAS_READ_COUNT = PREFIX
            + "EXTRAS_READ_COUNT";
    private static final String EXTRAS_PATH_THUMB = PREFIX
            + "EXTRAS_PATH_THUMB";
    private static final String EXTRAS_PATH_MEDIA = PREFIX
            + "EXTRAS_PATH_MEDIA";

	/**Internal User Only!*/
	public static String MSGTYPE_SENT_MSG_RECEIPT = "1";
	/**Internal User Only!*/
	public static String MSGTYPE_CALLEE_GETBACK_ONLINE = "2";
	/**Internal User Only!*/
	public static String MSGTYPE_GET_MISSED_CALL = "3";
	/**Internal User Only!*/
	public static String MSGTYPE_FORCE_TO_TEMINATE_CALL = "4";
	/**Internal User Only!*/
	public static String MSGTYPE_NORMAL_CALL_REJECTED = "5";
	/**Internal User Only!*/
	public static String MSGTYPE_VIDEO_CALL_REQUEST = "6";
	/**Internal User Only!*/
	public static String MSGTYPE_VIDEO_CALL_REJECTED = "7";
	/**Internal User Only!*/
	public static String MSGTYPE_SENT_MSG_READED_RECEIPT = "8";
	// End of internal message type

	/**Message Type : Text*/
	public static String MSGTYPE_ENCRIPTED_TXT_MESSAGE = "9";
	/**Message Type : （Old）Text*/
	public static String MSGTYPE_NORMAL_TXT_MESSAGE = "0";
	/**Message Type : Location*/
	public static String MSGTYPE_LOCATION = "a";
	/**Message Type : Stamp*/
	public static String MSGTYPE_MULTIMEDIA_STAMP = "b";
	/**Message Type : Photo*/
	public static String MSGTYPE_MULTIMEDIA_PHOTO = "c";
	/**Message Type : Voice File*/
	public static String MSGTYPE_MULTIMEDIA_VOICE_NOTE = "d";
	/**Message Type : Video File*/
	public static String MSGTYPE_MULTIMEDIA_VIDEO_NOTE = "e";
	/**Message Type : VCF*/
	public static String MSGTYPE_MULTIMEDIA_VCF = "o"; // 本来是"f"，暂时借它让 MSGTYPE_PIC_VOICE 通过。

	/**Message Type : GroupChat：Join Request*/
	public static String MSGTYPE_GROUPCHAT_JOIN_REQUEST = "g";
	/**Message Type : GroupChat：Someone joined the group chatroom*/
	public static String MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM = "h";
	/**Message Type : GroupChat：Someone left the group chatroom*/
	public static String MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM = "i";

	/**Message Type : Call Log*/
	public static String MSGTYPE_CALL_LOG = "j";

	/**Message Type : Buddylist has been updated*/
	public static String MSGTYPE_BUDDYLIST_INCREASED = "k";
	public static String MSGTYPE_BUDDYLIST_DECREASED = "l";
	public static String MSGTYPE_ACTIVE_APP_TYPE_CHANGED = "m";
    /**Message Type : communicate with public account. */
    public static String MSGTYPE_OFFICIAL_ACCOUNT_MSG = "n";

	public static String MSGTYPE_LOGIN_PLACE_CHANGED = "o"; // NOT USED
	public static String MSGTYPE_BUDDY_INFO_UPDATED = "p";
    public static String MSGTYPE_GROUP_INFO_UPDATED = "q";
    /** added in 2014/8 **/
    public static String MSGTYPE_MOMENT = "r";
	/**Message Type : hybird of text, image, and voice. */
	public static String MSGTYPE_PIC_VOICE = "o";
    public static String MSGTYPE_OUTGOING_MSG = "z";

    public static String MSGTYPE_SYSTEM_PROMPT = "local_a";
    /**
     * 请求被拒绝，此种消息类型，不会显示在数据库中，只会以通知形式出现
     */
    public static String MSGTYPE_REQUEST_REJECT = "local_b";


	/**Message Type : For Third Party use **/
	public static String MSGTYPE_THIRDPARTY_MSG = "x";


	public static String IOTYPE_OUTPUT = "0";
	public static String IOTYPE_INPUT_READED = "1";
	public static String IOTYPE_INPUT_UNREAD = "2";

    public static String SENTSTATUS_FILE_UPLOADINIT = "-3";
    public static String SENTSTATUS_IN_PROCESS = "-1";
	public static String SENTSTATUS_NOTSENT = "0";
	public static String SENTSTATUS_SENT = "1";
	public static String SENTSTATUS_REACHED_CONTACT = "2";
	public static String SENTSTATUS_READED_BY_CONTACT = "3";
    public static String SENTSTATUS_SENDING = "4";

	public static final int HYBIRD_COMPONENT_IMAGE = 0;
	public static final int HYBIRD_COMPONENT_AUDIO = 1;

	/************************Data should be saved in db*****************************/
	/** insert key of this message in db **/
	public int    primaryKey = -1;
    /**
     * local item id for the history messages
     */
    public int localHistoryId = -1;
	/** the unique key of the message(local and remote) **/
	public String    uniqueKey = "";
    /** userID / groupID /userName  **/
	public String chatUserName = "";
	/** user nick name in server **/
	public String displayName = ""; 
    /** message content **/
	public String messageContent = "";
    /** message sent date **/
	public String sentDate = "";

	/** message type:MSGTYPE_* **/
	public String msgType = ""; 
	/** io type : IOTYPE_*  **/
	public String ioType = ""; 
	/** sent status : SENTSTATUS_* **/
	public String sentStatus = ""; 
	
	
	/** true when this is a group chat message **/
	public boolean isGroupChatMessage =false;
	/** Message sender when in group chat, "" when 1:1 chat **/
	public String groupChatSenderID="";   

	/** read count of this message **/
	public int readCount;

    /**
     * the count of unreaded msgs whose owner is the same with this msg owner.
     * <br>it's maybe used in SmsActivity to show the unreaded msgs count.
     */
    public int unreadCount = 0;

	/** field that can be used to save the multimedia thumbnail downloaded from a message **/
	public String pathOfThumbNail;
	/** field that can be used to save the multimedia downloaded from a message **/
	public String pathOfMultimedia;
	/** field that can be used to save the second multimedia downloaded from a message.
	 * <p>for {@link #MSGTYPE_PIC_VOICE}, the second multimedia is the audio,
	 * For other types of messages, there's no second multimedia.</p>**/
	public String pathOfMultimedia2;

	/************************End of Data should be saved in db*****************************/

	
	
	
	
	// ///////////// properties below shouldn't be stored in DB////////
	/** Internal Use Only**/
	public String compositeName = ""; // user composite name in address book
	
	/** Internal Use Only**/
	public int chatUserRecordID = -1;
	/**
	 *  internal use */
	public boolean isSelected;
	/**
	 * hold temp non-persistent data for client, e.g., a flag indicating downloading or 
	 * uploading is in progress.
	 *
	 * <p>See also EXTRA_DATA_* constants.</p>
	 */
	public Bundle extraData = null;
	/**
	 * anything can't put into extraData goes here.
	 *
	 * <p>See also EXTRA_OBJ_* constants.</p>
	 */
	public Map<String, Object> extraObjects = null;


	

	public ChatMessage(Bundle b) {
		primaryKey = b.getInt(EXTRAS_PRIMARY_ID);
		sentDate = b.getString(EXTRAS_SENTDATE);
		chatUserName = b.getString(EXTRAS_CONTACT_ID);
		displayName = b.getString(EXTRAS_CONTACT_DISPLAYNAME);
		msgType = b.getString(EXTRAS_MESSAGE_TYPE);
		messageContent=b.getString(EXTRAS_MESSAGE_CONTENT);
		ioType = b.getString(EXTRAS_IOTYPE);
		isGroupChatMessage = b.getBoolean(EXTRAS_ISGROUPCHAT);
		groupChatSenderID = b.getString(EXTRAS_GROUP_SENDER);
        uniqueKey=b.getString(EXTRAS_UNIQUE_KEY);
        sentStatus=b.getString(EXTRAS_SENT_STATUS);
        readCount=b.getInt(EXTRAS_READ_COUNT);
        pathOfThumbNail=b.getString(EXTRAS_PATH_THUMB);
        pathOfMultimedia=b.getString(EXTRAS_PATH_MEDIA);
	}

	
	public ChatMessage() {

	}

	public boolean hasPrimaryKey() {
		return primaryKey != -1;
	}
	
	public boolean hasUniqueKey() {
		return !TextUtils.isEmpty(uniqueKey);
	}

	

	/**
	 * Convert all ChatMessage data to an extras bundle to send via an intent
	 */
	public Bundle toBundle() {
		Bundle b = new Bundle();
		b.putBoolean(EXTRAS_IS_WOWTALK_MSG, true);

		b.putInt(EXTRAS_PRIMARY_ID, primaryKey);
		b.putString(EXTRAS_SENTDATE, sentDate);
		b.putString(EXTRAS_MESSAGE_CONTENT, messageContent);
		b.putString(EXTRAS_MESSAGE_TYPE, msgType);

		b.putString(EXTRAS_CONTACT_ID, chatUserName);
		b.putString(EXTRAS_CONTACT_DISPLAYNAME, displayName);
		b.putString(EXTRAS_IOTYPE, ioType);
		b.putBoolean(EXTRAS_ISGROUPCHAT, isGroupChatMessage());
		b.putString(EXTRAS_GROUP_SENDER, groupChatSenderID);
        b.putString(EXTRAS_UNIQUE_KEY, uniqueKey);
        b.putString(EXTRAS_SENT_STATUS, sentStatus);
        b.putInt(EXTRAS_READ_COUNT, readCount);
        b.putString(EXTRAS_PATH_THUMB, pathOfThumbNail);
        b.putString(EXTRAS_PATH_MEDIA, pathOfMultimedia);
		return b;
	}

	public boolean isGroupChatMessage(){
		return isGroupChatMessage;
	}
	
	/**
	 * create extraData and extraObjects if not before.
	 */
	public void initExtra() {
		if(extraData == null)
			extraData = new Bundle();
		if(extraObjects == null)
			extraObjects = new HashMap<String, Object>();
	}
	
	public void fixMessageSenderDisplayName(Context context,
			int defaultBuddyNameResId, int defaultGroupNameResId) {
		if(isGroupChatMessage())
			displayName = getGroupDisplayName(context, chatUserName, defaultGroupNameResId);
		else
			displayName = getBuddyNick(context, chatUserName, defaultBuddyNameResId);
	}

	public boolean isBelongsToBuddyOrGroup(Context context) {
	    boolean isBelongs = false;
	    Database db = new Database(context);
        if(isGroupChatMessage()) {
            GroupChatRoom grp = db.fetchGroupChatRoom(chatUserName);
            if(grp != null) {
                isBelongs = true;
            }
        } else {
            Buddy buddy = new Buddy(chatUserName);
            if (null != db.fetchBuddyDetail(buddy)) {
                isBelongs = true;
            }
        }
	    return isBelongs;
	}

	private String getBuddyNick(Context context, String uid, int defaultValue) {
		Database db = Database.open(context);
        Buddy b = new Buddy(uid);
        if (null != db.fetchBuddyDetail(b)) {
            return Utils.isNullOrEmpty(b.nickName) ? b.username : b.nickName;
        }
		return TextUtils.isEmpty(displayName)?context.getString(defaultValue):displayName;
	}

	private String getGroupDisplayName(Context context, String uid, int defaultValue) {
		Database db = new Database(context);
		GroupChatRoom grp = db.fetchGroupChatRoom(uid);
		if(grp != null)
			return grp.getDisplayName();
		return TextUtils.isEmpty(displayName)?context.getString(defaultValue):displayName;
	}

    /**
     * Format message body for MSGTYPE_THIRDPARTY_MSG to notify buddy profile updating.
     * @param buddy_uid
     * @return
     */
    public void formatContentAsBuddyProfileUpdated(String buddy_uid) {
        messageContent = String.format(Locale.getDefault(),
                "{\"reason\":\"buddy_profile_updated\",\"id\":\"%s\"}", buddy_uid);
    }

    /**
     * Format message body for {@link #MSGTYPE_MULTIMEDIA_PHOTO} or
     * {@link #MSGTYPE_MULTIMEDIA_VIDEO_NOTE}.
     * @param mediaFileID
     * @param thumbnailFileID
     */
    public void formatContentAsPhotoMessage(
            String mediaFileID, String thumbnailFileID) {
        String ext=pathOfMultimedia.substring(pathOfMultimedia.lastIndexOf(".")+1);
        if(TextUtils.isEmpty(ext)) {
            if(MSGTYPE_MULTIMEDIA_PHOTO.equals(msgType)) {
                ext = "jpg";
            } else if (MSGTYPE_MULTIMEDIA_VIDEO_NOTE.equals(msgType)) {
                ext = "mp4";
            }
        }
        messageContent = String.format("{\"pathoffileincloud\":\"%s\",\"pathofthumbnailincloud\":\"%s\",\"ext\":\".%s\"}",
                mediaFileID, thumbnailFileID,ext);
    }

    /**
     * Format message body for {@link #MSGTYPE_MULTIMEDIA_VOICE_NOTE}.
     * @param mediaFileID
     * @param duration in seconds.
     */
    public void formatContentAsVoiceMessage(
            String mediaFileID, int duration) {
        messageContent = String.format(
                "{\"pathoffileincloud\":\"%s\",\"duration\":\"%d\",\"ext\":\".m4a\"}",
                mediaFileID, duration);
    }

	/**
	 * Format message body for {@link #MSGTYPE_PIC_VOICE}.
	 * @param text
	 * @param imageFileId
	 * @param imageExt
	 * @param thumbnailFileId
	 * @param audioFileId
	 * @param audioExt
	 * @param duration
	 */
	public void formatContentAsHybird(
			String text,
			String imageFileId, String imageExt, String thumbnailFileId,
			String audioFileId, String audioExt, int duration) {
		try {
			text = Base64.encodeToString(text.getBytes("UTF-8"), 0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			text = "";
		}

		messageContent = String.format(
				"{\"text\":\"%s\"," +
						"\"pathoffileincloud\":\"%s\"," +
						"\"ext\":\".%s\"," +
						"\"pathofthumbnailincloud\":\"%s\"," +
						"\"audio_pathoffileincloud\":\"%s\"," +
						"\"audio_ext\":\"%s\"," +
						"\"duration\":\"%d\"" +
						"}",
				text,
				imageFileId, imageExt, thumbnailFileId,
				audioFileId, audioExt, duration);
	}

    public void formatContentAsGroupProfileUpdated(String group_id) {
        messageContent = String.format(Locale.getDefault(),
                "{\"reason\":\"group_profile_updated\",\"id\":\"%s\"}", group_id);
    }

    public void formatContentAsBuddyRequest(String buddy_id, String hello) {
        messageContent = String.format(Locale.getDefault(),
                "{\"reason\":\"add_buddy\",\"id\":\"%s\",\"msg\":\"%s\"}", buddy_id, hello);
    }

	/**
	 * Get text body (not JSON) of this message.
	 * @return
	 */
	public String getText() {
		if (MSGTYPE_NORMAL_TXT_MESSAGE.equals(msgType)) {
			return messageContent;
		} else {
			Pattern p = Pattern.compile("text\" *: *\"([^\"]+)");
			Matcher m = p.matcher(messageContent);
			if (m.find()) {
				String encodedTxt = m.group(1);
				try {
					return new String(Base64.decode(encodedTxt, 0), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

    /**
     * Get the original file's ID coded in message body if this message is of
     * one of the following types:
     * <ul>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_PHOTO}</li>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_VIDEO_NOTE}</li>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_VOICE_NOTE}</li>
     * </ul>
     */
	public String getMediaFileID() {
		Pattern p = Pattern.compile("pathoffileincloud\" *: *\"([0-9a-zA-Z-_]+)");
		Matcher m = p.matcher(messageContent);
		if(m.find())
			return m.group(1);
		return null;
	}

    public String getMediaFileID(int hybirdComponent) {
		if (hybirdComponent == HYBIRD_COMPONENT_IMAGE) {
			// the image component is compliant with image message.
			return getMediaFileID();
		}

		Assert.assertTrue(hybirdComponent == HYBIRD_COMPONENT_AUDIO);

		if (hybirdComponent == HYBIRD_COMPONENT_AUDIO) {
			Pattern p = Pattern.compile("audio_pathoffileincloud\" *: *\"([0-9a-zA-Z-_]+)");
			Matcher m = p.matcher(messageContent);
			if (m.find())
				return m.group(1);
		}

        return null;
    }

    /**
     * Get the thumbnail file's ID coded in message body if this message is of
     * one of the following types:
     * <ul>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_PHOTO}</li>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_VIDEO_NOTE}</li>
     * </ul>
     */
    public String getThumbnailFileID() {
        Pattern p = Pattern.compile("pathofthumbnailincloud\" *: *\"([0-9a-zA-Z-_]+)");
        Matcher m = p.matcher(messageContent);
        if(m.find())
            return m.group(1);
        return null;
    }

    /**
     * Get the voice duration coded in message body if this message is of
     * type {@link #MSGTYPE_MULTIMEDIA_VOICE_NOTE}.
     * @return duration in seconds.
     */
    public int getVoiceDuration() {
        Pattern p = Pattern.compile("duration\" *: *\"([0-9]+)");
        Matcher m = p.matcher(messageContent);
        if(m.find())
            return Utils.tryParseInt(m.group(1), 0);
        return 0;
    }

    /**
     * Get the file name extension coded in message body if this message is of
     * one of the following types:
     * <ul>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_PHOTO}</li>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_VIDEO_NOTE}</li>
     *    <li> {@link #MSGTYPE_MULTIMEDIA_VOICE_NOTE}</li>
     * </ul>
     * @return always starts with ".", e.g. ".aac"
     */
    public String getFilenameExt() {
        Pattern p = Pattern.compile("ext\" *: *\"([\\.a-zA-Z0-9]+)");
        Matcher m = p.matcher(messageContent);
        if(m.find()) {
            if(m.group(1).startsWith("."))
                return m.group(1);
            else
                return "." + m.group(1);
        }

        // audio sent by ios is .aac
        return ".aac";
    }

	/**
	 * Get the file name extension coded in message body if this message is of
	 * type {@link #MSGTYPE_PIC_VOICE}.
	 *
	 * @param hybirdComponent one of
	 * <ul>
	 *     <li>{@link #HYBIRD_COMPONENT_IMAGE}</li>
	 *     <li>{@link #HYBIRD_COMPONENT_AUDIO}</li>
	 * </ul>
	 * @return
	 */
	public String getFilenameExt(int hybirdComponent) {
		if (hybirdComponent == HYBIRD_COMPONENT_IMAGE) {
			// the image component is compliant with image message.
			return getFilenameExt();
		}

		Assert.assertTrue(hybirdComponent == HYBIRD_COMPONENT_AUDIO);

		if (hybirdComponent == HYBIRD_COMPONENT_AUDIO) {
			Pattern p = Pattern.compile("audio_ext\" *: *\"([\\.a-zA-Z0-9]+)");
			Matcher m = p.matcher(messageContent);
			if (m.find()) {
				if (m.group(1).startsWith("."))
					return m.group(1);
				else
					return "." + m.group(1);
			}
		}

		return null;
	}

    /**
     * Get the entity ID coded in message body if this message is of type
     * MSGTYPE_THIRDPARTY_MSG and is used to notify buddy profile updating.
     * @return null if nothing.
     */
    public String getEntityIdAsBuddyProfileUpdated() {
        Pattern p0 = Pattern.compile("\"reason\" *: *\"([0-9a-zA-Z-_]+)");
        Matcher m0 = p0.matcher(messageContent);
        if(m0.find() && "buddy_profile_updated".equals(m0.group(1))) {
            Pattern p = Pattern.compile("\"id\" *: *\"([0-9a-zA-Z-_]+)");
            Matcher m = p.matcher(messageContent);
            if(m.find())
                return m.group(1);
        }

        return null;
    }

    /**
     * Get the entity ID coded in message body if this message is of type
     * MSGTYPE_THIRDPARTY_MSG and is used to notify buddy profile updating.
     * @return null if nothing.
     */
    public String getEntityIdAsGroupProfileUpdated() {
        Pattern p0 = Pattern.compile("\"reason\" *: *\"([0-9a-zA-Z-_]+)");
        Matcher m0 = p0.matcher(messageContent);
        if(m0.find() && "group_profile_updated".equals(m0.group(1))) {
            Pattern p = Pattern.compile("\"id\" *: *\"([0-9a-zA-Z-_]+)");
            Matcher m = p.matcher(messageContent);
            if(m.find())
                return m.group(1);
        }

        return null;
    }

    public String getUidAsBuddyRequest() {
        Pattern p0 = Pattern.compile("\"reason\" *: *\"([0-9a-zA-Z-_]+)");
        Matcher m0 = p0.matcher(messageContent);
        if(m0.find() && "add_buddy".equals(m0.group(1))) {
            Pattern p = Pattern.compile("\"id\" *: *\"([0-9a-zA-Z-_]+)");
            Matcher m = p.matcher(messageContent);
            if(m.find())
                return m.group(1);
        }

        return null;
    }
    /**
     * Parse the message body as XML, assuming this message is from a public
     * account.
     */
    public void parseContentFromPublicAccount() {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        try {
            SAXParser parser = fac.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                boolean isFromUserTag = false;
                boolean isContentTag = false;
                boolean isTypeTag = false;

                @Override
                public void startElement (String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equalsIgnoreCase("MsgType")) {
                        isTypeTag = true;
                    } else if (qName.equalsIgnoreCase("Content")) {
                        isContentTag = true;
                        messageContent = "";
                    } else if (qName.equalsIgnoreCase("FromUserName")) {
                        isFromUserTag = true;
                        displayName = "";
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("MsgType")) {
                        isTypeTag = false;
                    } else if (qName.equalsIgnoreCase("Content")) {
                        isContentTag = false;
                    } else if (qName.equalsIgnoreCase("FromUserName")) {
                        isFromUserTag = false;
                    }
                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {
                    if (isTypeTag) {
                        String t = new String(ch, start, length);
                        if (t.equals("text")) {
                            msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
                        } else if (t.equals("image")) {
                            msgType = ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO;
                        } else if (t.equals("video")) {
                            msgType = ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE;
                        } else if (t.equals("voice")) {
                            msgType = ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE;
                        } else if (t.equals("location")) {
                            msgType = ChatMessage.MSGTYPE_LOCATION;
                        } else {
                            msgType = ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE;
                        }
                    } else if (isContentTag) {
                        messageContent += new String(ch, start, length);
                    } else if (isFromUserTag) {
                        displayName += new String(ch, start, length);
                    }
                }
            };

            parser.parse(new StringInputStream(messageContent), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ChatMessage) {
            ChatMessage target = (ChatMessage)o;
            if (this.primaryKey == -1 && target.primaryKey == -1) {
                return false;
            }
            return this.primaryKey == target.primaryKey;
        }
        return false;
    }
}
