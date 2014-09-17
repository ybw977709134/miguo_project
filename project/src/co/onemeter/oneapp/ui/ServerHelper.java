package co.onemeter.oneapp.ui;

import android.content.Context;
import org.wowtalk.api.*;

import java.util.ArrayList;

public class ServerHelper {

    public static void notifyGroupProfileChanged(Context context, String groupID) {
        Database db = new Database(context);

        String myNick = PrefUtil.getInstance(context).getMyNickName();

//                ChatMessage msg = new ChatMessage();
//                msg.chatUserName = groupRoom.groupID;
//                msg.displayName = myNick;
//                msg.groupChatSenderID = myUid;
//                msg.msgType = ChatMessage.MSGTYPE_THIRDPARTY_MSG;
//                msg.isGroupChatMessage = true;
//                msg.formatContentAsGroupProfileUpdated(groupRoom.groupID);
//                mWebif.fGroupChat_SendMessage(groupRoom.groupID, msg);

        ArrayList<GroupMember> buddies = db.fetchGroupMembers(groupID);
        if(buddies != null && !buddies.isEmpty()) {
            for(GroupMember b : buddies) {
                ChatMessage msg = new ChatMessage();
                msg.chatUserName = b.userID;
                msg.displayName = myNick;
                msg.msgType = ChatMessage.MSGTYPE_THIRDPARTY_MSG;
                msg.isGroupChatMessage = false;
                msg.formatContentAsGroupProfileUpdated(groupID);
//                WowTalkVoipIF.getInstance(context).fSendChatMessage(msg);

                // this message will not displayed
//                db.deleteChatMessage(msg);
            }
        }
    }

}
