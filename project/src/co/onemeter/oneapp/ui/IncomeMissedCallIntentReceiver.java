package co.onemeter.oneapp.ui;

import org.wowtalk.api.CallLog;
import co.onemeter.oneapp.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class IncomeMissedCallIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			CallLog callLog = new CallLog(bundle);
			showNotification(context, callLog);
		}

	}
	
	private void showNotification(Context context, CallLog callLog) {
		if (callLog == null || callLog.displayName == null || callLog.contact == null)
			return;
		
		String strCompositeName = callLog.displayName;
		String strTickerMsg = String.format(context.getString(R.string.add), strCompositeName);
		
		NotificationManager notiManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification(R.drawable.icon, strTickerMsg, System.currentTimeMillis());
		
		PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(context, StartActivity.class), 0);
		note.setLatestEventInfo(context, strCompositeName, context.getString(R.string.app_name), intent);
		notiManager.notify(GlobalValue.NOTIFICATION_FOR_CHATMESSAGE, note);
	}

}
