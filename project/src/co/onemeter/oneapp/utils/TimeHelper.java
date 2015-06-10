package co.onemeter.oneapp.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;

import java.util.Date;

/**
 * Created by pzy on 6/10/15.
 */
public class TimeHelper {

    private static final String TAG = "TimeHelper";

    /**
     * 从服务器同步时间。不会改变系统时间，只是把时间差记录下来。
     * @param context
     */
    public static void syncTime(final Context context) {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                WowTalkWebServerIF.getInstance(context).fAdjustUTCTimeWithServer();
                return null;
            }
        });
    }

    /**
     * 获取发送消息时的时间。考虑了与服务器的时间差。
     * @return 可以直接赋值给 {@link org.wowtalk.api.ChatMessage#sentDate}。
     */
    public static String getTimeForMessage (Context context) {
        long localDate = System.currentTimeMillis();
        int offset = PrefUtil.getInstance(context).getUTCOffset();
        Log.d(TAG, "offset = " + offset);
        long adjustedTime = localDate + offset * 1000L;
        Date adjustedDate = new Date(adjustedTime);
        return Database.chatMessage_dateToUTCString(adjustedDate);
    }
}
