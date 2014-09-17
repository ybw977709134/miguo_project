package co.onemeter.oneapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-22
 * Time: PM6:44
 * To change this template use File | Settings | File Templates.
 */
public class NetworkExceptionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        String msg = context.getString(R.string.network_connection_unavailable);
//        String title = context.getString(R.string.network_connection_unavailable_title);
//        String  e = intent.getExtras().getString("exception-str");
//        if (e != null) {
//            android.util.Log.d("WowTalk/network", e);
//            // TODO update msg depending on exception detail
//        }
//        showNotification(context, GlobalValue.NOTIFICATION_FOR_NETWORKERROR, title, msg);
//        new MessageBox(context.getApplicationContext()).toast(msg);
    }

//    private void showNotification(Context context, int notificationId, String title, String msg) {
//        if (null == context) {
//            return;
//        }
//        NotificationManager notiManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification note = new Notification(R.drawable.icon, msg, System.currentTimeMillis());
//        note.flags = Notification.FLAG_AUTO_CANCEL;
//        Intent intent = new Intent();
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        note.setLatestEventInfo(context, title, msg, pendingIntent);
//        notiManager.notify(notificationId, note);
//        SmsActivity.setNavTitle("Connection Problem");
//    }
}
