package co.onemeter.oneapp;

import android.os.AsyncTask;
import junit.framework.Assert;
import org.wowtalk.api.*;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-4-27
 * Time: PM2:22
 * To change this template use File | Settings | File Templates.
 */
public class Test {
    public static void testGetPendingBuddyRequests(final YuanquActivity mActivity) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(mActivity);
                ArrayList<PendingRequest> requests = new ArrayList<PendingRequest>();
                web.fGetPendingRequests();
                Database db = new Database(mActivity);
                db.fetchPendingRequest(requests);
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }.execute((Void)null);
    }

    public static void testOptGroup(final YuanquActivity mActivity) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(mActivity);
                Database db = new Database(mActivity);
                ArrayList<GroupChatRoom> groups = db.fetchAllGroupChatRooms();
                if (!groups.isEmpty()) {
                    GroupChatRoom b = groups.get(0);
                    b.willBlockMsg = true;
                    b.willBlockNotification = true;
                    b.groupNameLocal = "he (" + b.groupNameOriginal + ")";
                    b.myNickHere = "i'm pzy";
                    Assert.assertTrue(ErrorCode.OK == web.fGroupChat_Settings(b));

                    web.fGroupChat_GetByShortID(b.shortGroupID, b);
                    GroupChatRoom newb = db.fetchGroupChatRoom(b.groupID);
                    Assert.assertTrue(b.willBlockMsg == newb.willBlockMsg);
                    Assert.assertTrue(b.willBlockNotification == newb.willBlockNotification);
                    Assert.assertTrue(b.groupNameLocal == newb.groupNameLocal);
                    Assert.assertTrue(b.myNickHere.equals(newb.myNickHere));
                }

                return null;
            }
        }.execute((Void)null);
    }

//    public static void testOptBuddy(final YuanquActivity mActivity) {
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(mActivity);
//                Database db = new Database(mActivity);
//                db.open();
//                ArrayList<Buddy> buddies = db.fetchAllBuddies();
//                if (!buddies.isEmpty()) {
//                    Buddy b = buddies.get(0); //db.buddyWithUserID("00ae7654-e9a0-4e03-ae2e-69b6368a267c");
//                    b.willBlockMsg = false;
//                    b.willBlockNotification = true;
//                    b.favorite = false;
//                    b.alias = "he (" + b.nickName + ")";
//                    Assert.assertTrue(ErrorCode.OK == web.fOperateBuddy(b));
//
//                    web.fGetBuddyWithUID(b.userID);
////                    web.fGetBuddyList();
//                    Buddy newb = db.buddyWithUserID(b.userID);
//                    Assert.assertTrue(b.willBlockMsg == newb.willBlockMsg);
//                    Assert.assertTrue(b.willBlockNotification == newb.willBlockNotification);
//                    Assert.assertTrue(b.favorite == newb.favorite);
//                    Assert.assertTrue(b.alias.equals(newb.alias));
//                }
//
//                return null;  //To change body of implemented methods use File | Settings | File Templates.
//            }
//        }.execute((Void)null);
//    }
}
