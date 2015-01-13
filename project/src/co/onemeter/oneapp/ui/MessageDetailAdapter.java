package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.MyUrlSpanHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import junit.framework.Assert;
import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.*;
import org.wowtalk.ui.msg.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageDetailAdapter extends BaseAdapter{

    public static final String TAG_IS_PLAYING = "isplaying";
    public static final String TAG_NOT_PLAYING = "notplaying";

    private static final int CELL_TYPE_COUNT = 13;

    private static final int CELL_TYPE_IN = 0;
    private static final int CELL_TYPE_IN_LOC = 1;
    private static final int CELL_TYPE_IN_STAMP = 2;
    private static final int CELL_TYPE_IN_PHOTO = 3;
    private static final int CELL_TYPE_IN_VOICE = 4;
    private static final int CELL_TYPE_IN_CALL = 5;

    private static final int CELL_TYPE_OUT = 6;
    private static final int CELL_TYPE_OUT_LOC = 7;
    private static final int CELL_TYPE_OUT_STAMP = 8;
    private static final int CELL_TYPE_OUT_PHOTO = 9;
    private static final int CELL_TYPE_OUT_VOICE = 10;
    private static final int CELL_TYPE_OUT_CALL = 11;
    private static final int CELL_TYPE_SYSTEM_PROMPT = 12;

    private static final float THUMBNAIL_DENSITY_SCALE = 1 / 1.5f;

    // map ChatMessage.MSGTYPE_MULTIMEDIA_* to MEDIA_TYPE_*
    private HashMap<String, Integer> msgtype2mediatype = null;
    private HashMap<String, String> msgtype2mime = null;

    private HashMap<String,PngDrawable> mPngDrawableHashMap = new HashMap<String,PngDrawable>();

    private Activity mContext;
    private Database mDbHelper;
    private MessageBox mMsgBox;
    private Handler mHandler;
	private MediaPlayerWraper mediaPlayerWraper;
    private SimpleDateFormat messageDateFormatter;
    private SimpleDateFormat messageTimeFormatter;
    private MessageDetailListener mMessageListener;

    private HashMap<Integer,ViewHolder> msgViewHolderMap = new HashMap<Integer,ViewHolder>();
    private ArrayList<ChatMessage> mLogMsg;
    private Bitmap mTargetThumbnail;
    private float mThumbnailDensity;
    private boolean mIsShowHistory;
    private boolean mIsVoicePlayTimeWithAppendAllTime;

    /**
     * it's for MessageComposerActivityBase
     * @param context
     * @param logMsg
     * @param isShowHistory
     * @param handler
     * @param messageListener
     */
    public MessageDetailAdapter(Activity context, ArrayList<ChatMessage> logMsg, boolean isShowHistory, Handler handler, MessageDetailListener messageListener) {
        mContext = context;
        mMsgBox = new MessageBox(context);
        mDbHelper = new Database(context);
        mediaPlayerWraper = new MediaPlayerWraper(context);
        mLogMsg = logMsg;
        mIsShowHistory = isShowHistory;
        mHandler = handler;
        mMessageListener = messageListener;
        initMsgTimeFormat();
        initMsgTypeMap();
        mThumbnailDensity = mContext.getResources().getDisplayMetrics().density * THUMBNAIL_DENSITY_SCALE;
    }

    private void initMsgTimeFormat() {
        try {
            messageDateFormatter = new SimpleDateFormat("MM/dd", Locale.getDefault());
            messageDateFormatter.setTimeZone(TimeZone.getDefault());
        } catch (Exception e) {
        }
        try {
            messageTimeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
            messageTimeFormatter.setTimeZone(TimeZone.getDefault());
        } catch (Exception e) {
        }
    }

    private void initMsgTypeMap() {
        if(msgtype2mediatype == null) {
            msgtype2mediatype = new HashMap<String, Integer>();
            msgtype2mediatype.put(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO,
                    MediaInputHelper.MEDIA_TYPE_IMAGE);
            msgtype2mediatype.put(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE,
                    MediaInputHelper.MEDIA_TYPE_VOICE);
            msgtype2mediatype.put(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE,
                    MediaInputHelper.MEDIA_TYPE_VIDEO);
            msgtype2mediatype.put(ChatMessage.MSGTYPE_PIC_VOICE,
                    MediaInputHelper.MEDIA_TYPE_IMAGE);
        }
        if(msgtype2mime == null) {
            msgtype2mime = new HashMap<String, String>();
            msgtype2mime.put(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO, "image/*");
            msgtype2mime.put(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE, "audio/*");
            msgtype2mime.put(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE, "video/*");
            msgtype2mime.put(ChatMessage.MSGTYPE_PIC_VOICE, "image/*");
        }
    }

    public void setDataSource(ArrayList<ChatMessage> logMsg) {
        mLogMsg = logMsg;
    }

    public int getCount() {
        return mLogMsg.size();
    }

    public ChatMessage getItem(int position) {
        return mLogMsg.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {
            if(ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE.equals(message.msgType))
                return CELL_TYPE_OUT;
            if(ChatMessage.MSGTYPE_MULTIMEDIA_STAMP.equals(message.msgType))
                return CELL_TYPE_OUT_STAMP;
            if(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE.equals(message.msgType))
                return CELL_TYPE_OUT_VOICE;
            if(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO.equals(message.msgType))
                return CELL_TYPE_OUT_PHOTO;
            if(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE.equals(message.msgType))
                return CELL_TYPE_OUT_PHOTO;
            if(ChatMessage.MSGTYPE_PIC_VOICE.equals(message.msgType))
                return CELL_TYPE_OUT_PHOTO;
            if(ChatMessage.MSGTYPE_LOCATION.equals(message.msgType))
                return CELL_TYPE_OUT_LOC;
            if (ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED.equals(message.msgType) || ChatMessage.MSGTYPE_GET_MISSED_CALL.equals(message.msgType) || ChatMessage.MSGTYPE_CALL_LOG.equals(message.msgType))
                return CELL_TYPE_OUT_CALL;
            return CELL_TYPE_OUT;
        }

        if(ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE.equals(message.msgType))
            return CELL_TYPE_IN;
        if(ChatMessage.MSGTYPE_MULTIMEDIA_STAMP.equals(message.msgType))
            return CELL_TYPE_IN_STAMP;
        if(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE.equals(message.msgType))
            return CELL_TYPE_IN_VOICE;
        if(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO.equals(message.msgType))
            return CELL_TYPE_IN_PHOTO;
        if(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE.equals(message.msgType))
            return CELL_TYPE_IN_PHOTO;
        if(ChatMessage.MSGTYPE_PIC_VOICE.equals(message.msgType))
            return CELL_TYPE_IN_PHOTO;
        if(ChatMessage.MSGTYPE_LOCATION.equals(message.msgType))
            return CELL_TYPE_IN_LOC;
        if (ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED.equals(message.msgType) || ChatMessage.MSGTYPE_GET_MISSED_CALL.equals(message.msgType) || ChatMessage.MSGTYPE_CALL_LOG.equals(message.msgType))
            return CELL_TYPE_IN_CALL;
        if (ChatMessage.MSGTYPE_SYSTEM_PROMPT.equals(message.msgType)) {
            return CELL_TYPE_SYSTEM_PROMPT;
        }
        return CELL_TYPE_IN;
    }

    @Override
    public boolean isEnabled(int position) {
        return (CELL_TYPE_SYSTEM_PROMPT == getItemViewType(position)) ? false : super.isEnabled(position);
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        View lView = null;
        ViewHolder holder = null;

        final ChatMessage message = getItem(position);
        Log.i("MessageDetailAdapter#getView : messageType " + message.msgType
                + ", ioType " + message.ioType + ", primarykey " + message.primaryKey
                + ", unique_key " + message.uniqueKey);
        Date oldDate;
        if (position == 0) {
            oldDate = new Date(0, 0, 0);
        } else {
            oldDate = Database.chatMessage_UTCStringToDate((getItem(position - 1)).sentDate);
        }
        if (convertView != null) {
            lView = convertView;
            holder = (ViewHolder)convertView.getTag();
        } else {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new ViewHolder();
            switch(getItemViewType(position)) {
            case CELL_TYPE_OUT:
                lView = inflater.inflate(R.layout.msg_list_outgo_msg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_OUT_LOC:
                lView = inflater.inflate(R.layout.msg_list_outgo_locmsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_OUT_STAMP:
                lView = inflater.inflate(R.layout.msg_list_outgo_stampmsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = (AnimImage)lView.findViewById(R.id.img_thumbnail);
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = null;
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_OUT_PHOTO:
                lView = inflater.inflate(R.layout.msg_list_outgo_photomsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_OUT_VOICE:
                lView = inflater.inflate(R.layout.msg_list_outgo_voicemsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_OUT_CALL:
                lView = inflater.inflate(R.layout.msg_list_outgo_call, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.txt_outgo);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = (TextView)lView.findViewById(R.id.txt_sentstatus);
                holder.txtContactName = null;
                holder.txtDuration = (TextView)lView.findViewById(R.id.txt_time);
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = (ImageView)lView.findViewById(R.id.imgMarkFailed);
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall =  lView.findViewById(R.id.layout_call);
                holder.vgMissedCall =  lView.findViewById(R.id.layout_callmissed);
                break;
            case CELL_TYPE_IN_LOC:
                lView = inflater.inflate(R.layout.msg_list_income_locmsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.imgStampAnim = null;
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_IN_STAMP:
                lView = inflater.inflate(R.layout.msg_list_income_stampmsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.txtDuration = null;
                holder.imgStampAnim = (AnimImage)lView.findViewById(R.id.img_thumbnail);
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = null;
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_IN_VOICE:
                lView = inflater.inflate(R.layout.msg_list_income_voicemsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_IN_CALL:
                lView = inflater.inflate(R.layout.msg_list_income_call, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.txtDuration = (TextView)lView.findViewById(R.id.txt_time);
                holder.imgStampAnim = null;
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall =  lView.findViewById(R.id.layout_call);
                holder.vgMissedCall =  lView.findViewById(R.id.layout_callmissed);
                break;
            case CELL_TYPE_IN_PHOTO:
                lView = inflater.inflate(R.layout.msg_list_income_photomsg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            case CELL_TYPE_SYSTEM_PROMPT:
                lView = inflater.inflate(R.layout.msg_list_system_prompt_msg, parent, false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = null;
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = null;
                holder.imgMsgThumbnail = null;
                holder.imgMarkFailed = null;
                holder.progressbar = null;
                holder.vgBalloon = null;
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            default:
                lView = inflater.inflate(R.layout.msg_list_income_msg, parent,  false);
                holder.txtMsgDate = (TextView) lView.findViewById(R.id.msg_date);
                holder.txtContent = (TextView) lView.findViewById(R.id.messagedetail_text);
                holder.txtDate = (TextView) lView.findViewById(R.id.messagedetail_date);
                holder.txtSentStatus = null;
                holder.txtContactName = (TextView)lView.findViewById(R.id.txt_contact_name);
                holder.txtDuration = null;
                holder.imgStampAnim = null;
                holder.imgContactPhoto = (ImageView)lView.findViewById(R.id.img_contact_thumbnail);
                holder.imgMsgThumbnail = (ImageView)lView.findViewById(R.id.img_thumbnail);
                holder.imgMarkFailed = null;
                holder.progressbar = (ProgressBar)lView.findViewById(R.id.progressBar1);
                holder.vgBalloon = lView.findViewById(R.id.layout_balloon);
                holder.vgCall = null;
                holder.vgMissedCall = null;
                break;
            }

            lView.setTag(holder);
        }

        msgViewHolderMap.put(message.primaryKey, holder);

        if (null != holder.imgContactPhoto) {
            final String buddyId = message.isGroupChatMessage ? message.groupChatSenderID : message.chatUserName;
            holder.imgContactPhoto.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    co.onemeter.oneapp.ui.Log.d("click in messageComposer, buddy id is " + buddyId
                            + ", message.isGroupChatMessage is " + message.isGroupChatMessage);
                    ContactInfoActivity.launch(mContext,
                            buddyId,
                            ContactInfoActivity.BUDDY_TYPE_UNKNOWN);
                }
            });
        }

        if (message.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {

            if (message.sentStatus.equals(ChatMessage.SENTSTATUS_NOTSENT)) {
                holder.imgMarkFailed.setVisibility(mIsShowHistory ? View.GONE: View.VISIBLE);
                holder.txtSentStatus.setVisibility(View.GONE);
                holder.imgMarkFailed.setOnClickListener(new FailedMarkOnClickListener(position));
            } else {
                boolean markTextView = false;
                boolean markFailedImage = false;
                holder.txtSentStatus.setVisibility(mIsShowHistory ? View.GONE: View.VISIBLE);
                holder.imgMarkFailed.setVisibility(View.GONE);
                String strSentStatus = "";
                if (message.sentStatus.equals(ChatMessage.SENTSTATUS_SENT)) {
                    strSentStatus = mContext.getString(R.string.msg_sent);
                    markTextView = true;
                } else if (message.sentStatus.equals(ChatMessage.SENTSTATUS_REACHED_CONTACT)) {
//                    strSentStatus = mContext.getString(R.string.msg_reached);
                    strSentStatus = mContext.getString(R.string.msg_sent);
                } else if (message.sentStatus.equals(ChatMessage.SENTSTATUS_READED_BY_CONTACT)) {
                    if (message.isGroupChatMessage) {
                        if (message.readCount > 0) {
                            strSentStatus = String.format(mContext.getString(R.string.msg_readed_with_count),
                                    message.readCount);
                        } else {
                            strSentStatus = mContext.getString(R.string.msg_readed);
                        }
                    } else {
                        strSentStatus = mContext.getString(R.string.msg_readed);
                    }
                } else if (message.sentStatus.equals(ChatMessage.SENTSTATUS_SENDING)) {
                    strSentStatus = mContext.getString(R.string.msg_sending);
                    markTextView = true;
                    markFailedImage = true;
                } else if (message.sentStatus.equals(ChatMessage.SENTSTATUS_IN_PROCESS)) {
                    strSentStatus = mContext.getString(R.string.msg_sending);
                    markTextView = true;
                    markFailedImage = true;
                }
                holder.txtSentStatus.setText(strSentStatus);

                if (markTextView) {
                    // hold reference to this text view, so we can access it later when status changed
                    message.initExtra();
                    message.extraObjects.put("txtSentStatus", holder.txtSentStatus);
                }
                if (markFailedImage) {
                    message.extraObjects.put("imgMarkFailed", holder.imgMarkFailed);
                }
            }
        }

        Date sentDate = Database.chatMessage_UTCStringToDate(message.sentDate);
        Date nowDate = new Date();
        if(sentDate != null) {
            if (sentDate.getYear() < nowDate.getYear()) {
                messageDateFormatter = new SimpleDateFormat(mContext.getResources().getString(R.string.msg_date_format_with_year));
            } else {
                messageDateFormatter = new SimpleDateFormat(mContext.getResources().getString(R.string.msg_date_format_without_year));
            }
            if (oldDate == null
                    || (sentDate.getDate() > oldDate.getDate()
                            && sentDate.getMonth() == oldDate.getMonth()
                            && sentDate.getYear() == oldDate.getYear())
                    || (sentDate.getMonth() > oldDate.getMonth() && sentDate.getYear() == oldDate.getYear())
                    || sentDate.getYear() > oldDate.getYear()) {
                holder.txtMsgDate.setVisibility(View.VISIBLE);
                holder.txtMsgDate.setText(messageDateFormatter.format(sentDate));
            } else {
                holder.txtMsgDate.setVisibility(View.GONE);
            }
            holder.txtDate.setText(messageTimeFormatter.format(sentDate));
        } else {
            holder.txtMsgDate.setVisibility(View.GONE);
            holder.txtDate.setText("");
        }

        // progress bar will be referenced by ChatMessage object, so we can access it latter
        if(holder.progressbar != null) {
            holder.progressbar.setMax(100);
            message.initExtra();
            message.extraObjects.put("progressBar", holder.progressbar);
            if(message.extraData.getBoolean("isTransferring", false)) {
                holder.progressbar.setVisibility(View.VISIBLE);
                holder.progressbar.setProgress(0);
            } else {
                holder.progressbar.setVisibility(View.GONE);
            }
        }

        if (message.msgType.equals(ChatMessage.MSGTYPE_NORMAL_TXT_MESSAGE)) {
            setViewForText(lView, holder, message);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
            setViewForVoice(lView, message);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)
                || message.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)
                || message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
            setViewForPhotoOrVideo(lView, holder, message);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_STAMP)){
            setViewForStamp(lView, holder, message, sentDate);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_LOCATION)){
            setViewForLocation(lView, holder, message);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_JOIN_ROOM)) {
            Buddy b = mDbHelper.buddyWithUserID(message.groupChatSenderID);
            String memberName = (b != null ?
                    (TextUtils.isEmpty(b.alias) ? b.nickName : b.alias)
                    : mContext.getString(R.string.msg_session_unknown_buddy));
            holder.txtContent.setText(memberName + mContext.getResources().getString(R.string.msg_someone_join_group));
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_SOMEONE_QUIT_ROOM)) {
            Buddy b = mDbHelper.buddyWithUserID(message.groupChatSenderID);
            String memberName = (b != null ?
                    (TextUtils.isEmpty(b.alias) ? b.nickName : b.alias)
                    : mContext.getString(R.string.msg_session_unknown_buddy));
            holder.txtContent.setText(memberName + mContext.getResources().getString(R.string.msg_someone_leave_group));
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_GROUPCHAT_JOIN_REQUEST)){
            holder.txtContent.setText(R.string.msg_someone_request_group);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_CALL_LOG)
                || message.msgType.equals(ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED)
                || message.msgType.equals(ChatMessage.MSGTYPE_GET_MISSED_CALL)) {
            setViewForCall(lView, holder, message);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_THIRDPARTY_MSG)) {
            String uid = message.getUidAsBuddyRequest();
            if (uid != null) {
                holder.txtContent.setText(R.string.sms_say_hello_to_you);
            } else {
                holder.txtContent.setText(R.string.newer_chatmessage_receive);
            }
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_SYSTEM_PROMPT)) {
            holder.txtContent.setText(message.messageContent);
        } else {
            if(holder.txtContent != null)
                holder.txtContent.setText(R.string.msg_newer_chatmessage_receive);
        }

        if (!ChatMessage.MSGTYPE_SYSTEM_PROMPT.equals(message.msgType)) {
            lView.setClickable(true);
            lView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (null != mMessageListener) {
                        mMessageListener.onViewItemClicked();
                    }
                }
            });
        }

        if (!message.ioType.equals(ChatMessage.IOTYPE_OUTPUT) && !ChatMessage.MSGTYPE_SYSTEM_PROMPT.equals(message.msgType)) {
            Buddy sender = null;
            if(message.isGroupChatMessage()) {
                sender = mDbHelper.buddyWithUserID(message.groupChatSenderID);
            } else {
                sender = mDbHelper.buddyWithUserID(message.chatUserName);
            }

            // display photo
            if (mTargetThumbnail != null) {
                holder.imgContactPhoto.setImageBitmap(mTargetThumbnail);
            } else {
                if(sender != null) {
                    holder.imgContactPhoto.setBackgroundDrawable(null);
                    PhotoDisplayHelper.displayPhoto(mContext,
                            holder.imgContactPhoto,
                            R.drawable.default_avatar_90, sender, true);
                }
            }

            // display name
            if (message.isGroupChatMessage && !message.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {
                String displayName = "";
                if (sender != null) {
                    if (TextUtils.isEmpty(sender.alias)) {
                        displayName = TextUtils.isEmpty(sender.nickName) ? "" : sender.nickName;
                    } else {
                        displayName = sender.alias;
                    }
                }
                displayName = TextUtils.isEmpty(displayName) ? message.displayName : displayName;
                holder.txtContactName.setText(String.format("%s", displayName));
                holder.txtContactName.setVisibility(View.VISIBLE);

//              if(sender != null && sender.nickName != null) {
//                  holder.txtContactName.setText(String.format("%s", sender.nickName));
//              } else {
//                  holder.txtContactName.setText(String.format("%s", message.displayName));
//              }
            } else {
                holder.txtContactName.setVisibility(View.GONE);
            }
        }

        return lView;

    }

    @Override
    public int getViewTypeCount() {
        return CELL_TYPE_COUNT;
    }

    public void setTargetThumbnail(Bitmap targetThumbnail) {
        mTargetThumbnail = targetThumbnail;
    }

    private void setViewForText(View lView, ViewHolder holder, final ChatMessage message) {
        holder.txtContent.setText(message.messageContent);
        holder.txtContent.setVisibility(View.VISIBLE);

//        MomentAdapter.setStringAsURLIfAvaliable(holder.txtContent, message.messageContent,false);

        final MyUrlSpanHelper spanHelper = new MyUrlSpanHelper(holder.txtContent);
        holder.txtContent.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                spanHelper.onLongClicked();
                if (null != mMessageListener) {
//                    String[] phones = PhoneNumberHelper.extractPhoneNumbers(message.messageContent);
//                    String[] links = HyperLinkHelper.extractHyperLinks(message.messageContent);
                    mMessageListener.onMessageTextClicked(message, null, null);
                }
                return true;
            }
        });
    }

    private void setViewForCall(View lView, ViewHolder holder, final ChatMessage message) {
        message.fixMessageSenderDisplayName(mContext, R.string.session_unknown_buddy, R.string.session_unknown_group);
        OnClickListener listener = new OnClickListener() {

            @Override
            public void onClick(View v) {
//              CallMainActivity.startNewOutGoingCall(MessageComposerActivityBase.this, _targetUID, message.displayName, false);
                if (null != mMessageListener) {
                    mMessageListener.onConfirmOutgoingCall();
                }
            }

        };
        if (message.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {
            holder.vgCall.setOnClickListener(listener);
            if (message.msgType.equals(ChatMessage.MSGTYPE_CALL_LOG)) {
                holder.txtContent.setText(String.format("%02d:%02d",
                        Integer.valueOf(message.messageContent) / 60, Integer.valueOf(message.messageContent) % 60));
            } else if (message.msgType.equals(ChatMessage.MSGTYPE_NORMAL_CALL_REJECTED)) {
                holder.txtContent.setText(mContext.getResources().getString(R.string.msg_cancelled_call));
            }
        }
        if (message.ioType.equals(ChatMessage.IOTYPE_INPUT_READED)) {
            holder.vgCall.setOnClickListener(listener);
            holder.vgMissedCall.setOnClickListener(listener);
            if (message.msgType.equals(ChatMessage.MSGTYPE_CALL_LOG)) {
                holder.vgCall.setVisibility(View.VISIBLE);
                holder.vgMissedCall.setVisibility(View.GONE);
                holder.txtDuration.setText(String.format("%s:%s",
                        String.format("%02d", Integer.valueOf(message.messageContent) / 60),
                        String.format("%02d", Integer.valueOf(message.messageContent) % 60)));
            } else if (message.msgType.equals(ChatMessage.MSGTYPE_GET_MISSED_CALL)) {
                holder.vgCall.setVisibility(View.GONE);
                holder.vgMissedCall.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setViewForLocation(View lView, ViewHolder holder, ChatMessage message) {

        double lat = 0, lon = 0;
        String addr = null;

        if (message.messageContent!=null){

            try {
                JSONObject json=new JSONObject(message.messageContent);
                if (json.has("address")){
                    addr = json.getString("address");
                    //txtContent.setText(Utils.abbr(addr, 10));
                    holder.txtContent.setText(addr);
                }
                if (json.has("longitude")){
                    lon= Utils.tryParseDouble(json.getString("longitude"), 0.0);
                }
                if (json.has("latitude")){
                    lat = Double.parseDouble(json.getString("latitude"));
                }
            } catch (JSONException e) {

                e.printStackTrace();
            }

        }

        final double flat = lat;
        final double flon = lon;
        final String faddr = addr;
        holder.txtContent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                viewLocationInfo(mContext,flat,flon,faddr);

//                String uriStr = String.format("geo:%f,%f?z=17&q=%f,%f",
//                                flat, flon, flat, flon);
//                if (!Utils.isNullOrEmpty(faddr))
//                    uriStr += "(" + faddr + ")";
//                Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
//                PackageManager pm = mContext.getPackageManager();
//                List<ResolveInfo> activities = pm.queryIntentActivities(geoIntent, 0);
//                if(activities.isEmpty()) {
//                    Intent i = new Intent(mContext, PickLocActivity.class);
//                    i.putExtra("target_lat", flat);
//                    i.putExtra("target_lon", flon);
//                    i.putExtra("no_pick", true);
//                    mContext.startActivity(i);
//                } else {
//                    AppStatusService.setIsMonitoring(false);
//                    mContext.startActivity(geoIntent);
//                }
            }
        });
    }

    public static void viewLocationInfo(Context context,double flat,double flon,String faddr) {
        String uriStr = String.format("geo:%f,%f?z=17&q=%f,%f",
                flat, flon, flat, flon);
        if (!Utils.isNullOrEmpty(faddr))
            uriStr += "(" + faddr + ")";
        Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(geoIntent, 0);
        if(activities.isEmpty()) {
            Intent i = new Intent(context, PickLocActivity.class);
            i.putExtra("target_lat", flat);
            i.putExtra("target_lon", flon);
            i.putExtra("no_pick", true);
            context.startActivity(i);
        } else {
            AppStatusService.setIsMonitoring(false);
            context.startActivity(geoIntent);
        }
    }

    private void setViewForVoice(View rootView, final ChatMessage message) {
        ViewHolder holder = msgViewHolderMap.get(message.primaryKey);

        OnClickListener cl = new OnClickListener(){
            @Override
            public void onClick(View arg0) {
                // click to play voice
                vgBalloonClicked(message);
            }
        };
        holder.imgMsgThumbnail.setOnClickListener(cl);
        // the icon may be too small for clicking
        holder.vgBalloon.setOnClickListener(cl);
        holder.txtContent.setOnClickListener(cl);

        setVoicePlayInfo(message);
    }

    /**
     * set view for photo/video message, will be called by getView().
     * @param rootView
     * @param holder
     * @param message
     */
    private void setViewForPhotoOrVideo(View rootView, final ViewHolder holder, final ChatMessage message) {

        setupThumbnailForImageOrVideo(message, holder.imgMsgThumbnail);

        // download media and thumb nail
        if(message.pathOfThumbNail == null || !new File(message.pathOfThumbNail).exists()) {
            downloadThumbAndShowIt(message, holder.imgMsgThumbnail);
        }

        // click to view original media
        holder.imgMsgThumbnail.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0) {
                thumbnailClicked(message);
            }

        });
    }

    private void setVoicePlayInfo_Play(ChatMessage cm) {
        ViewHolder holder = msgViewHolderMap.get(cm.primaryKey);
        holder.imgMsgThumbnail.setImageResource(R.drawable.play_transparent);

        AnimationDrawable ad;
        if(cm.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {
            holder.imgMsgThumbnail.setBackgroundResource(R.drawable.playing_right_animation);
            ad = (AnimationDrawable) holder.imgMsgThumbnail.getBackground();
        } else {
            holder.imgMsgThumbnail.setBackgroundResource(R.drawable.playing_left_animation);
            ad = (AnimationDrawable) holder.imgMsgThumbnail.getBackground();
        }
        ad.start();

        mediaPlayerWraper.setPlayingTimeTV(holder.txtContent,mIsVoicePlayTimeWithAppendAllTime);
    }

    private void setVoicePlayInfo_NotPlay(ChatMessage cm) {
        ViewHolder holder = msgViewHolderMap.get(cm.primaryKey);
        if(cm.ioType.equals(ChatMessage.IOTYPE_OUTPUT)) {
            holder.imgMsgThumbnail.setImageResource(R.drawable.play_right);
        } else {
            holder.imgMsgThumbnail.setImageResource(R.drawable.play_left);
        }
        holder.imgMsgThumbnail.setBackgroundResource(0);

        int duration = cm.getVoiceDuration();
        holder.txtContent.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, duration / 60, duration % 60));
        holder.txtContent.setVisibility(View.VISIBLE);
    }

    private void setVoicePlayInfo(ChatMessage cm) {
        if(!TextUtils.isEmpty(cm.pathOfMultimedia) &&
                cm.pathOfMultimedia.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            setVoicePlayInfo_Play(cm);
        } else {
            setVoicePlayInfo_NotPlay(cm);
        }
    }

    private void downloadThumbAndShowIt(ChatMessage message, final ImageView iv) {
        // prevent multiple download
        message.initExtra();
        if(!message.extraData.getBoolean("isTransferring", false)) {
            message.extraData.putBoolean("isTransferring", true);

            final String pathofthumbnailincloud = message.getThumbnailFileID();
            if(pathofthumbnailincloud != null) {
                final ChatMessage cm = message;
                final String pathOfThumbNail = MediaInputHelper.makeOutputMediaFile(
                        MediaInputHelper.MEDIA_TYPE_THUMNAIL, null).getAbsolutePath();

                AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        WowTalkWebServerIF.getInstance(mContext).fGetFileFromServer(
                                pathofthumbnailincloud,
                                new NetworkIFDelegate() {

                                    @Override
                                    public void didFailNetworkIFCommunication(
                                            int arg0, byte[] arg1) {
                                        cm.extraData.putBoolean("isTransferring", false);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
//                                                mMsgBox.show(null,
//                                                        mContext.getString(R.string.msg_operation_failed));
                                            }
                                        });
                                    }

                                    @Override
                                    public void didFinishNetworkIFCommunication(
                                            int arg0, byte[] arg1) {
                                        cm.extraData.putBoolean("isTransferring", false);
                                        cm.pathOfThumbNail = pathOfThumbNail;
                                        if (mIsShowHistory) {
                                            mDbHelper.updateChatMessageHistory(cm, true);
                                        } else {
                                            mDbHelper.updateChatMessage(cm, true);
                                        }
                                    }

                                    @Override
                                    public void setProgress(int arg0, int arg1) {
                                    }

                                }, 0, pathOfThumbNail);
                        return null;
                    }

                });
            }
        }
    }

    private void setupThumbnailForImageOrVideo(ChatMessage message, ImageView iv) {
        if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
            if(message.pathOfThumbNail != null && new File(message.pathOfThumbNail).exists()) {
                iv.setScaleType(ScaleType.FIT_XY);
                setupThumbnail(message, iv, false);
            } else {
                iv.setImageResource(R.drawable.broken_thumbnail);
            }
            iv.setBackgroundResource(0);
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
            iv.setScaleType(ScaleType.CENTER_INSIDE);
            iv.setImageResource(R.drawable.play_video);
            if(message.pathOfThumbNail != null && new File(message.pathOfThumbNail).exists()) {
                setupThumbnail(message, iv, true);
            } else {
                iv.setBackgroundResource(R.drawable.broken_thumbnail);
            }
        } else if (message.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)) {
            iv.setScaleType(ScaleType.CENTER_INSIDE);
            iv.setImageResource(R.drawable.play_hybird);
            if(message.pathOfThumbNail != null && new File(message.pathOfThumbNail).exists()) {
                setupThumbnail(message, iv, true);
            } else {
                iv.setBackgroundResource(R.drawable.broken_thumbnail);
            }
        }
    }

    private void setupThumbnail(final ChatMessage message, final ImageView iv, final boolean background) {
        PngDrawable pngDrawable = mPngDrawableHashMap.get(message.pathOfThumbNail);

        if(null == pngDrawable) {
            Bitmap bmp, bmp2;

            bmp = BitmapFactory.decodeFile(message.pathOfThumbNail);
            if(null == bmp) {
                bmp=BitmapFactory.decodeResource(mContext.getResources(),R.drawable.feed_default_pic);
            }
            bmp2 = BmpUtils.roundCorner(bmp,
                    (int) mContext.getResources().getDimension(R.dimen.multimedia_thumbnail_round_radius));

            pngDrawable = new PngDrawable(bmp2, mThumbnailDensity);
            mPngDrawableHashMap.put(message.pathOfThumbNail, pngDrawable);
            if (background) {
                iv.setBackgroundDrawable(pngDrawable);
            } else {
                iv.setImageDrawable(pngDrawable);
            }

            BmpUtils.recycleABitmap(bmp);
            BmpUtils.recycleABitmap(bmp2);
        } else {
            Log.d("MessageDetailAdapter#setupThumbnail, get a png from cache in mPngDrawableHashMap");
            if (background) {
                iv.setBackgroundDrawable(pngDrawable);
            } else {
                iv.setImageDrawable(pngDrawable);
            }
        }
    }

    private void setViewForStamp(View rootView, final ViewHolder holder, 
            ChatMessage message, Date sentDate) {

        holder.imgStampAnim.setSourceCode(message.messageContent);
        // the animation will no auto start if this message is too old
        if(Calendar.getInstance().getTimeInMillis() 
                - sentDate.getTime() < 24 * 3600 * 1000) {
            mHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    holder.imgStampAnim.playStamp();
                }
            }, 1000);
        }
    }

    private void vgBalloonClicked(final ChatMessage cm) {
        if(TextUtils.isEmpty(cm.pathOfMultimedia)) {
            // download first

            final ProgressBar progressBar = (ProgressBar)cm.extraObjects.get("progressBar");
            if(progressBar != null)
                showProgressbarOnUiThread(progressBar);

            // prevent multiple download
            cm.initExtra();
            if(!cm.extraData.getBoolean("isTransferring", false)) {
                cm.extraData.putBoolean("isTransferring", true);

                final String pathoffileincloud = cm.getMediaFileID();
                if(pathoffileincloud != null) {
                    String ext = cm.getFilenameExt();
                    cm.pathOfMultimedia = MediaInputHelper.makeOutputMediaFile(
                            msgtype2mediatype.get(cm.msgType), ext).getAbsolutePath();

                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
                        @Override
                        protected Void doInBackground(
                                Void... params) {
                            WowTalkWebServerIF.getInstance(mContext).fGetFileFromServer(
                                    pathoffileincloud,
                                    new NetworkIFDelegate() {

                                        @Override
                                        public void didFailNetworkIFCommunication(
                                                int arg0, byte[] arg1) {
                                            cm.pathOfMultimedia = null;
                                        }

                                        @Override
                                        public void didFinishNetworkIFCommunication(
                                                int arg0, byte[] arg1) {
                                            if (mIsShowHistory) {
                                                mDbHelper.updateChatMessageHistory(cm, false);
                                            } else {
                                                mDbHelper.updateChatMessage(cm, false);
                                            }
                                            mContext.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    startPlayingVoice(cm);
                                                }
                                            });
                                        }

                                        @Override
                                        public void setProgress(int arg0, int arg1) {
                                            publishProgress(arg1);
                                        }

                                    }, 0, cm.pathOfMultimedia);
                            return null;
                        }

                        @Override
                        protected void onProgressUpdate(Integer... values) {
                            if (progressBar != null)
                                progressBar.setProgress(values[0]);
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            cm.extraData.putBoolean("isTransferring", false);
                            if (progressBar != null)
                                progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        } else {
            startPlayingVoice(cm);
        }
    }

    private void startPlayingVoice(final ChatMessage cm) {
        if(TextUtils.isEmpty(cm.pathOfMultimedia)) {
            return;
        }

        ViewHolder holder = msgViewHolderMap.get(cm.primaryKey);
        if(!cm.pathOfMultimedia.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(holder.txtContent,mIsVoicePlayTimeWithAppendAllTime);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    setVoicePlayInfo_NotPlay(cm);
                }
                @Override
                public void onPlayBegin(String path) {
                    setVoicePlayInfo_Play(cm);
                }
                @Override
                public void onPlayComplete(String path) {
                    setVoicePlayInfo_NotPlay(cm);
                }
            });
            mediaPlayerWraper.triggerPlayer(cm.pathOfMultimedia,cm.getVoiceDuration());
        } else {
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(cm.pathOfMultimedia,cm.getVoiceDuration());
        }
    }

    public void stopPlayingVoice() {
        mediaPlayerWraper.stop();
    }

    private void showProgressbarOnUiThread(final ProgressBar progressBar) {
        if(progressBar != null) {
            progressBar.post(new Runnable(){
                @Override
                public void run() {
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void thumbnailClicked(final ChatMessage cm) {
        if(cm.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
            viewPhotoInPager(cm);
            return;
        }
        if(cm.pathOfMultimedia == null || !new File(cm.pathOfMultimedia).exists()) {
            cm.initExtra();
            if(cm.extraData.getBoolean("isTransferring")) {
                AlertDialog d = new AlertDialog.Builder(mContext)
                        .setMessage(R.string.msg_are_you_sure_to_cancel_download)
                        .setPositiveButton(R.string.msg_yes, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                CancelFlag cf = ((CancelFlag)cm.extraObjects
                                        .get("cancelFlag"));
                                if (cf != null) {
                                    cf.cancelled = true;
                                }
                            }
                        })
                        .setNegativeButton(R.string.msg_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // cancel the dialog
                            }
                        })
                        .create();
                d.setCanceledOnTouchOutside(true);
                d.show();
            } else {
                downloadPhotoOrVideoAndViewIt(cm);
            }
        } else {
            if (ChatMessage.MSGTYPE_PIC_VOICE.equals(cm.msgType)) {
                mContext.startActivity(new Intent(mContext, HybirdImageVoiceTextPreview.class)
                                .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_TEXT, cm.getText())
                                .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_IMAGE_FILENAME, cm.pathOfMultimedia)
                                .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_VOICE_FILENAME, cm.pathOfMultimedia2)
                                .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_VOICE_DURATION, cm.getVoiceDuration())
                );
            } else {
                Uri uri = Uri.fromFile(new File(cm.pathOfMultimedia));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, msgtype2mime.get(cm.msgType));
                mContext.startActivity(intent);

            }
        }
    }

    private void viewPhotoInPager(ChatMessage chatMsg) {
        PhotoWithThumbInfo photoWithThumbInfo=new PhotoWithThumbInfo(chatMsg);
        ImageViewActivity.launch(mContext, photoWithThumbInfo.getPhotoPathIndex(),
                photoWithThumbInfo.getPhotoWithThumbList(),ImageViewActivity.UPDATE_WITH_CHAT_MESSAGE,
                mIsShowHistory);
    }

    private void downloadPhotoOrVideoAndViewIt(final ChatMessage msg) {

        final String[] pathoffileincloud = new String[2];
        pathoffileincloud[0] = msg.getMediaFileID();
        if(pathoffileincloud[0] == null)
            return;
        if (ChatMessage.MSGTYPE_PIC_VOICE.equals(msg.msgType)) {
            pathoffileincloud[1] = msg.getMediaFileID(ChatMessage.HYBIRD_COMPONENT_AUDIO);
        }

        // prevent multiple download
        msg.initExtra();
        if(msg.extraData.getBoolean("isTransferring", false)) 
            return;
        msg.extraData.putBoolean("isTransferring", true);

        String ext = null;
        Log.w("downloading for " + msg.msgType + ",content " + msg.messageContent);
        ext = msg.getFilenameExt();
        Log.w("video ext got "+ext);

        final String[] pathOfMultimedia = new String[2];
        pathOfMultimedia[0] = MediaInputHelper.makeOutputMediaFile(
                msgtype2mediatype.get(msg.msgType), ext).getAbsolutePath();
        if (ChatMessage.MSGTYPE_PIC_VOICE.equals(msg.msgType)) {
            pathOfMultimedia[1] = MediaInputHelper.makeOutputMediaFile(
                    msgtype2mediatype.get(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE),
                    msg.getFilenameExt(ChatMessage.HYBIRD_COMPONENT_AUDIO)).getAbsolutePath();
        }

        final int fileNum = ChatMessage.MSGTYPE_PIC_VOICE.equals(msg.msgType) ? 2 : 1;

        final ProgressBar progressBar = msg.extraObjects == null ? null : (ProgressBar)msg.extraObjects.get("progressBar");
        showProgressbarOnUiThread(progressBar);

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
            boolean status = false;
            CancelFlag f;

            @Override
            protected Void doInBackground(
                    Void... params) {
                f = new CancelFlag();
                msg.extraObjects.put("cancelFlag", f);

                for (int fileIdx = 0; fileIdx < fileNum; ++fileIdx) {
                    final int finalFileIdx = fileIdx;
                    WowTalkWebServerIF.getInstance(mContext).fGetFileFromServer(
                            pathoffileincloud[fileIdx],
                            new NetworkIFDelegate() {

                                @Override
                                public void didFailNetworkIFCommunication(
                                        int arg0, byte[] arg1) {
                                    status = false;
                                }

                                @Override
                                public void didFinishNetworkIFCommunication(
                                        int arg0, byte[] arg1) {
                                    status = true;
                                }

                                @Override
                                public void setProgress(int arg0, int arg1) {
                                    publishProgress((int) (arg1 * (float) (1 + finalFileIdx) / fileNum));
                                }

                            }, 0, pathOfMultimedia[fileIdx], f);

                    // 如果任意一个文件下载失败，就不再继续了，视为整体失败。
                    if (!status)
                        break;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                if (progressBar != null)
                    progressBar.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void param) {
                msg.extraData.putBoolean("isTransferring", false);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (status) {
                    msg.pathOfMultimedia = pathOfMultimedia[0];
                    msg.pathOfMultimedia2 = pathOfMultimedia[1];
                    if (mIsShowHistory) {
                        mDbHelper.updateChatMessageHistory(msg, true);
                    } else {
                        mDbHelper.updateChatMessage(msg, true);
                    }

                    //if msg content is photo,use our own photo viewer
                    Assert.assertFalse(msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO));

                    if (msg.msgType.equals(ChatMessage.MSGTYPE_PIC_VOICE)) {
                        mContext.startActivity(new Intent(mContext, HybirdImageVoiceTextPreview.class)
                                        .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_TEXT, msg.getText())
                                        .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_IMAGE_FILENAME, pathOfMultimedia[0])
                                        .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_VOICE_FILENAME, pathOfMultimedia[1])
                                        .putExtra(HybirdImageVoiceTextPreview.EXTRA_IN_VOICE_DURATION, msg.getVoiceDuration())
                        );
                    } else {
                        //if not photo,call system viewer service
                        Uri uri = Uri.fromFile(new File(msg.pathOfMultimedia));
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, msgtype2mime.get(msg.msgType));
                        mContext.startActivity(intent);
                    }
                } else {
                    if (!f.cancelled) {
                        mMsgBox.show(null, mContext.getString(R.string.msg_operation_failed));
                    }

                    //delete tmp transferred file

                    try {
                        File tmpFile = new File(msg.pathOfMultimedia);
                        if (tmpFile.exists()) {
                            tmpFile.delete();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    msg.pathOfMultimedia = null;
                }
            }
        });
    }

    public void releaseRes() {
        mPngDrawableHashMap.clear();
    }

    static class ViewHolder {
        TextView txtMsgDate;
        TextView txtContactName;
        TextView txtContent;
        TextView txtDate;
        TextView txtSentStatus;
        TextView txtDuration;
        ImageView imgContactPhoto;
        ImageView imgMsgThumbnail;
        AnimImage imgStampAnim;
        ImageView imgMarkFailed;
        ProgressBar progressbar;
        View vgBalloon;
        View vgCall;
        View vgMissedCall;
    }

    final class FailedMarkOnClickListener implements OnClickListener {
        private final int position;

        private FailedMarkOnClickListener(int position) {
            this.position = position;
        }

        public void onClick(View v) {
            new AlertDialog.Builder(mContext)
            .setTitle(R.string.msg_resend_this_message)
            .setMessage(R.string.msg_resend_this_message_msg)
            .setPositiveButton(R.string.msg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(
                        DialogInterface dialog,
                        int whichButton) {
                    if(null != mMessageListener) {
                        mMessageListener.onResendMessage(mLogMsg.get(position));
                    }

                }
            })
            .setNegativeButton(R.string.msg_cancel, null)
            .show();
        }
    }

    private class PhotoWithThumbInfo {
        private int curPhotoIndex=0;
        private ArrayList<PhotoWithThumbPair> allPhotoWithThumbPairList = new ArrayList<PhotoWithThumbPair>();

        public PhotoWithThumbInfo(ChatMessage selectedMsg) {
            int idx=0;
            for(ChatMessage chatMsg : mLogMsg) {
                if(chatMsg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
                    //if no thumbnail exist,no view it now
                    if(TextUtils.isEmpty(chatMsg.pathOfThumbNail) || !new File(chatMsg.pathOfThumbNail).exists()) {
                        continue;
                    }
                    PhotoWithThumbPair aPair=new PhotoWithThumbPair();
                    aPair.setExt(String.valueOf(chatMsg.primaryKey));
                    aPair.setThumbPath(chatMsg.pathOfThumbNail);
                    aPair.setThumbFileId(chatMsg.getThumbnailFileID());
                    if(TextUtils.isEmpty(chatMsg.pathOfMultimedia)) {
                        aPair.setPhotoPath(MediaInputHelper.makeOutputMediaFile(
                                msgtype2mediatype.get(chatMsg.msgType), null).getAbsolutePath());
                    } else {
                        aPair.setPhotoPath(chatMsg.pathOfMultimedia);
                    }

                    aPair.setPhotoFileId(chatMsg.getMediaFileID());
                    allPhotoWithThumbPairList.add(aPair);

                    if (chatMsg.primaryKey == selectedMsg.primaryKey) {
                        curPhotoIndex=idx;
                    }
                    ++idx;
                }
            }
        }

        public int getPhotoPathIndex() {
            return curPhotoIndex;
        }

        public ArrayList<PhotoWithThumbPair> getPhotoWithThumbList() {
            return allPhotoWithThumbPairList;
        }
    }

    public interface MessageDetailListener {
        void onViewItemClicked();
        void onMessageTextClicked(ChatMessage message, String[] phones, String[] links);
        void onConfirmOutgoingCall();
        void onResendMessage(ChatMessage msg);
    }
}