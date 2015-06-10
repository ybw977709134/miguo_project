package co.onemeter.oneapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import co.onemeter.oneapp.utils.TimeHelper;

/**
 * 系统时间改变后，刷新与服务器的时间差。
 * Created by pzy on 6/10/15.
 */
public class SystemTimeChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TimeHelper.syncTime(context);
    }
}
