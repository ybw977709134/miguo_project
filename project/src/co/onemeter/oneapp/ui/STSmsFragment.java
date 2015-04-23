package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.IHasPhoto;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;

import java.util.ArrayList;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MessagesAdapter;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * Created by jacky on 15-3-27.
 */
public class STSmsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private String[] path;

    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private MessagesAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getArguments().getStringArray(SendToActivity.INTENT_PAHT);
        loadMsgsFromLocal();
        adapter = new InnerMessageAdapter(getActivity(),messages);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.sms_page,container,false);
        contentView.findViewById(R.id.title_bar).setVisibility(View.GONE);
        contentView.findViewById(R.id.layout_bg).setVisibility(View.GONE);
        contentView.findViewById(R.id.search_glass_img).setVisibility(View.GONE);

        ListView listview_sms = (ListView) contentView.findViewById(R.id.sms_list);
        listview_sms.setAdapter(adapter);
        listview_sms.setOnItemClickListener(this);
        return contentView;
    }

    private void loadMsgsFromLocal(){
        int initCount = 20;
        Database dbHelper = new Database(getActivity());
        messages.addAll(dbHelper.getLatestedMessages(initCount));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
       ChatMessage msg = messages.get(position);
        Context context = getActivity();
        if(msg.isGroupChatMessage()){
            MessageComposerActivity.launchToChatWithGroupWithPicture(context,msg.chatUserName,path,true);
        }else{
            MessageComposerActivity.launchToChatWithBuddyWithPicture(context,msg.chatUserName,path,true);
        }
    }

    /**
     * 需要重写MessagesAdapter的getView方法
     */
    private class InnerMessageAdapter extends MessagesAdapter{
        Context context;
        LayoutInflater inflater;

        public InnerMessageAdapter(Context context, ArrayList<ChatMessage> data) {
            super(context, data);
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View lView = null;
            if (convertView != null) {
                lView = convertView;
            } else {
                lView = inflater.inflate(R.layout.message_cell, parent, false);
            }

            if (log_msg == null || log_msg.size() == 0)
                return lView;

            lView.findViewById(R.id.listItem_checkbox).setVisibility(View.GONE);

            lView.findViewById(R.id.message_cell_content).setVisibility(View.GONE);
            lView.findViewById(R.id.message_cell_date).setVisibility(View.GONE);
            lView.findViewById(R.id.unreadMsg).setVisibility(View.GONE);

            final TextView txtContact = (TextView) lView.findViewById(R.id.message_cell_contact);
            txtContact.setGravity(Gravity.CENTER_VERTICAL);
            final ImageView photoImageView = (ImageView) lView.findViewById(R.id.contactPhoto);
            ImageView imageView_tag_tea = (ImageView) lView.findViewById(R.id.imageView_tag_tea);

            final ChatMessage message = log_msg.get(position);
            photoImageView.setBackgroundDrawable(null);
            if(message.isGroupChatMessage) {
                final GroupChatRoom g = mDbHelper.fetchGroupChatRoom(message.chatUserName);
                if (null != g) {
                    showGroupChatMessages(photoImageView, txtContact, message, g);
                } else {
                    txtContact.setText(R.string.group_chat_title_default);
                    photoImageView.setImageResource(R.drawable.default_group_avatar_90);
                    // 此群组在本地不存在，需要下载
                    if (!mDownloadingTargets.contains(message.chatUserName)) {
                        mDownloadingTargets.add(message.chatUserName);
                        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                return WowTalkWebServerIF.getInstance(context).fGroupChat_GetGroupDetail(message.chatUserName);
                            }

                            protected void onPostExecute(Integer result) {
                                mDownloadingTargets.remove(message.chatUserName);
                                if (ErrorCode.OK == result) {
                                    GroupChatRoom groupChatRoom = mDbHelper.fetchGroupChatRoom(message.chatUserName);
                                    showGroupChatMessages(photoImageView, txtContact, message, groupChatRoom);
                                }
                            };
                        });
                    }
                }
                imageView_tag_tea.setVisibility(View.GONE);
            } else {
                fixBuddyDisplay(message, txtContact, photoImageView);
                Buddy buddy = mDbHelper.buddyWithUserID(message.chatUserName);
                IHasPhoto entity = buddy;
                if(entity != null) {
                    // 公司发通知时，发起人采用的是buddy，此buddy的 username 格式为"公司id_公司id"
                    String companyId = PrefUtil.getInstance(context).getCompanyId();
                    if ((companyId + "_" + companyId).equals(buddy.username)) {
                        // 公司默认头像暂时使用群组默认头像
                        PhotoDisplayHelper.displayPhoto(context, photoImageView, R.drawable.default_group_avatar_90, entity, true);
                    } else {
                        PhotoDisplayHelper.displayPhoto(context, photoImageView, R.drawable.default_avatar_90, entity, true);
                    }
                }
                else {
                    photoImageView.setImageResource(R.drawable.default_avatar_90);
                }
                //判断消息列表的帐号头像是否为老师
                if (buddy != null && buddy.getAccountType() == 2) {
                    imageView_tag_tea.setVisibility(View.VISIBLE);
                } else {
                    imageView_tag_tea.setVisibility(View.GONE);
                }
            }
            return lView;
        }
    }
}
