package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;

import java.util.ArrayList;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MessagesAdapter;

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
        adapter = new MessagesAdapter(getActivity(),messages);
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
}
