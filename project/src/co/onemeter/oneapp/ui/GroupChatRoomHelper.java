package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;

import org.wowtalk.api.*;
import co.onemeter.oneapp.contacts.model.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * User: pan
 * Date: 3/27/13
 * Time: 2:20 PM
 */
public class GroupChatRoomHelper {

    /**
     * Create tmp group chat room for specified persons.
     * @param persons accept both wowtalk users and phone users.
     * @param webIf
     * @return group id.
     */
    public static String createTmp(Parcelable[] persons,
                                   WebServerIF webIf,
                                   Database dbHelper,
                                   Context context) {
        return addMembers(null, persons, webIf, dbHelper, context)[0];
    }

    /**
     * Add members to group.
     * @param gid if null, a temp group will be created automatically.
     * @param persons accept both wowtalk users and phone users.
     * @param webIf
     * @param context
     * @return result[0], group id; result[1], isAddMembersSuccess.
     */
    public static String[] addMembers(String gid, Parcelable[] persons,
                                    WebServerIF webIf,
                                    Database dbHelper,
                                    Context context) {
        String[] results = new String[2];
        boolean isAddMembersSuccess = false;
        List<Buddy> buddies = new ArrayList<Buddy>();
        ArrayList<String> phoneUsers = new ArrayList<String>();
        String groupName = null;
//        int groupNameParts = 0;
        if(gid == null) {
            // build group name
            if (persons.length == 1){
                groupName = ((Person)persons[0]).getName();
            } else {
                groupName = context.getResources().getString(co.onemeter.oneapp.R.string.group_chat_title_default);
            }
        }

        for(int i = 0; i < persons.length; ++i) {
            Person p = (Person) persons[i];
            if(p == null)
                continue;

            if(!TextUtils.isEmpty(p.getID())) {
                Buddy b = new Buddy();
                b.userID = p.getID();
                b.nickName = p.getName();
                buddies.add(b);
            } else {
                phoneUsers.add(p.getGlobalPhoneNumber()
                        + ":" + p.getName());
            }
        }
        if (!phoneUsers.isEmpty()) {
            String[] phoneUserArrays = new String[phoneUsers.size()];
            phoneUsers.toArray(phoneUserArrays);
            List<Buddy> localBuddies = webIf.fAddLocalContactsAsBuddies(phoneUserArrays);
            buddies.addAll(localBuddies);
        }

        if(gid == null) {
            // create tmp group
            if(groupName == null)
                return null;
            String[] gids = webIf.fGroupChat_Create(
                    groupName, true, "", "", 0, 0, null);
            if (null != gids) {
                gid = gids[0];
            }
            // save me in group member table
            if (gid != null) {
                GroupMember gm = new GroupMember(PrefUtil.getInstance(context).getUid(), gid);
                gm.setLevel(GroupMember.LEVEL_CREATOR);
                dbHelper.addNewBuddyToGroupChatRoomByID(gm, false);
            }
        }

        if(gid != null) {
            // add members
            try {
                if(ErrorCode.OK == webIf.fGroupChat_AddMembers(gid, buddies, false)) {
                    // The buddies and phoneUsers has both changed to buddies.
                    for (Buddy buddy : buddies) {
                        GroupMember gm = new GroupMember(null, gid);
                        gm.setLevel(GroupMember.LEVEL_DEFAULT);
                        Person.fromBuddy(buddy).fillBuddy(gm);
                        if (!TextUtils.isEmpty(buddy.alias)) {
                            gm.alias = buddy.alias;
                        }
                        dbHelper.addNewBuddyToGroupChatRoomByID(gm, false);
                    }
                    isAddMembersSuccess = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        results[0] = gid;
        results[1] = String.valueOf(isAddMembersSuccess);
        return results;
    }

}
