package co.onemeter.oneapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.FriendValidateActivity;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-12-9
 * Time: 下午3:56
 * To change this template use File | Settings | File Templates.
 */
public class BuddySearchItemAdapter extends BaseAdapter {
    private Context contextRef;
    private ArrayList<Buddy> buddyList;
    private String strContent;

    private String myLocalUid;
    private MessageBox mMsgBox;

    public final static String NAME_SPLIT=":";
    private WowTalkWebServerIF mWebif = null;
    private Database mDbHelper = null;
    public BuddySearchItemAdapter(Context context, ArrayList<Buddy> buddy, String str,MessageBox box) {
        contextRef=context;
        buddyList=buddy;
        if(null == buddyList) {
            buddyList=new ArrayList<Buddy>();
        }
        strContent=str;

        myLocalUid = PrefUtil.getInstance(context).getUid();

        mMsgBox=box;
        mDbHelper = new Database(contextRef);
    }
    @Override
    public int getCount() {
        return buddyList.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(null == convertView) {
            convertView= LayoutInflater.from(contextRef).inflate(R.layout.listitem_buddysearch, null);
        }

        ImageView ivThumb=(ImageView) convertView.findViewById(R.id.img_thumbnail);
        PhotoDisplayHelper.displayPhoto(contextRef, ivThumb,
                R.drawable.default_avatar_90, buddyList.get(position), true);


        TextView username=(TextView) convertView.findViewById(R.id.username);
        TextView nickName=(TextView) convertView.findViewById(R.id.nick_name);
        TextView alias=(TextView) convertView.findViewById(R.id.alias);

        final Buddy buddy=buddyList.get(position);
        setNameWithColor(username,contextRef.getString(R.string.settings_account_wowid)+NAME_SPLIT+" "+buddy.username);
        setNameWithColor(nickName,contextRef.getString(R.string.settings_name)+NAME_SPLIT+" "+buddy.nickName);

        if(TextUtils.isEmpty(buddy.alias)) {
            alias.setVisibility(View.GONE);
        } else {
            alias.setVisibility(View.VISIBLE);
            setNameWithColor(alias, String.format(contextRef.getString(R.string.contact_info_remarkname),buddy.alias));
        }

        final Button btnAdd = (Button) convertView.findViewById(R.id.btn_add);
        boolean mAllowAdd = !TextUtils.isEmpty(buddy.userID)
                && 0 == (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)
                && 0 == (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_PENDING_OUT)
                && !buddy.userID.equals(myLocalUid);
        if(mAllowAdd) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                	Log.d("--------------------", buddy+"");
                	Intent intent = new Intent(contextRef, FriendValidateActivity.class);
                	intent.putExtra("buddyList", buddyList);
                	intent.putExtra("position", position);
                	Activity ac = (Activity) contextRef;
                	ac.startActivityForResult(intent,0);
//                	btnAdd.setVisibility(View.GONE);

//                    onAddFriendPressed(buddy,btnAdd);
                }
            });
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void setNameWithColor(TextView tvName,String fullName) {
        String[] contents=fullName.split(NAME_SPLIT);

        SpannableStringBuilder ssb = new SpannableStringBuilder(fullName);
        int start = 0;
        int end = 0;
        for (int i = contents[0].length(); i < fullName.length(); i++) {
            if (String.valueOf(fullName.charAt(i)).equalsIgnoreCase(String.valueOf(strContent.charAt(0)))) {
                start = i;
                end = i + strContent.length();
                break;
            }
        }
        ssb.setSpan(new ForegroundColorSpan(
                contextRef.getResources().getColor(R.color.blue)),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        tvName.setText(ssb);
    }

//    private void onAddFriendPressed(final Buddy buddy,final Button btn,final String message) {
//        mMsgBox.showWait();
//    	Intent intent = new Intent(contextRef, FriendValidateActivity.class);
//    	contextRef.startActivity(intent);
//
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Buddy, Integer, Void>() {
//            int errno = ErrorCode.OK;
//            PendingRequest pr;
//
//            @Override
//            protected Void doInBackground(Buddy... params) {
//                Buddy b = params[0];
//                errno = WowTalkWebServerIF.getInstance(contextRef).faddBuddy_askforRequest(b.userID, message);
//                if (ErrorCode.OK == errno) {
//                    pr = new PendingRequest();
//                    pr.uid = b.getGUID();
//                    pr.nickname = b.nickName;
//                    pr.buddy_photo_timestamp = b.getPhotoUploadedTimestamp();
//                    pr.type = PendingRequest.BUDDY_OUT;
//
//                }
//                return null;
//
//            }
//
//            @Override
//            protected void onPostExecute(Void v) {
//                mMsgBox.dismissWait();
//                if (errno == ErrorCode.OK) {
//                    btn.setVisibility(View.GONE); // update UI
//                    mDbHelper.storePendingRequest(pr);
//                    if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
//                        mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
//                    } else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
//                        mMsgBox.show(null, contextRef.getString(R.string.contacts_add_buddy_pending_out));
//                    }
//                } else if (errno == ErrorCode.ERR_OPERATION_DENIED) {
//                    mMsgBox.show(null, contextRef.getString(R.string.contactinfo_add_friend_denied));
//                } else {
//                    mMsgBox.show(null, contextRef.getString(R.string.operation_failed));
//                }
//            }
//
//        }, buddy);
//    }
    

    public void clear() {
        buddyList.clear();
        notifyDataSetChanged();
    }

    public void add(Buddy b) {
        buddyList.add(b);
        notifyDataSetChanged();
    }

    public void addAll(List<Buddy> lst) {
        buddyList.addAll(lst);
        notifyDataSetChanged();
    }
    
    

}
