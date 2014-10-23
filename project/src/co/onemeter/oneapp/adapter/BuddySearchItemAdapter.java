package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;

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

    public BuddySearchItemAdapter(Context context, ArrayList<Buddy> buddy, String str,MessageBox box) {
        contextRef=context;
        buddyList=buddy;
        if(null == buddyList) {
            buddyList=new ArrayList<Buddy>();
        }
        strContent=str;

        myLocalUid = PrefUtil.getInstance(context).getUid();

        mMsgBox=box;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        if(null == convertView) {
            convertView= LayoutInflater.from(contextRef).inflate(R.layout.listitem_buddysearch, null);
        }

        ImageView ivThumb=(ImageView) convertView.findViewById(R.id.img_thumbnail);
        PhotoDisplayHelper.displayPhoto(contextRef, ivThumb,
                R.drawable.default_avatar_90, buddyList.get(position), true);


        TextView wowtalkId=(TextView) convertView.findViewById(R.id.wowtalk_id);
        TextView nickName=(TextView) convertView.findViewById(R.id.nick_name);
        TextView alias=(TextView) convertView.findViewById(R.id.alias);

        final Buddy buddy=buddyList.get(position);
        setNameWithColor(wowtalkId,contextRef.getString(R.string.settings_account_wowid)+NAME_SPLIT+" "+buddy.wowtalkID);
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
                    onAddFriendPressed(buddy,btnAdd);
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
                contextRef.getResources().getColor(R.color.group_search_blue)),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        tvName.setText(ssb);
    }

    private void onAddFriendPressed(final Buddy buddy,final Button btn) {
        mMsgBox.showWait();

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int errno = WowTalkWebServerIF.getInstance(contextRef).fAddBuddy(buddy.userID);
                return errno;
            }
            @Override
            protected void onPostExecute(Integer s) {
                mMsgBox.dismissWait();
                if(s == ErrorCode.OK) {
                    btn.setVisibility(View.GONE); // update UI
                    if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
                        mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
                    } else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
                        mMsgBox.show(null, contextRef.getString(R.string.contacts_add_buddy_pending_out));
                    }
                } else if (s == ErrorCode.ERR_OPERATION_DENIED){
                    mMsgBox.show(null, contextRef.getString(R.string.contactinfo_add_friend_denied));
                } else {
                    mMsgBox.show(null, contextRef.getString(R.string.operation_failed));
                }
            }
        }.execute((Void)null);
    }

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
