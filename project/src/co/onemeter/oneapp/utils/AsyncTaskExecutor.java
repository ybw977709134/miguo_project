package co.onemeter.oneapp.utils;

import android.os.AsyncTask;
import android.os.Build;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 根据任务类型，在指定 Executor 上执行 AsyncTask。
 * Created by pzy on 1/12/15.
 */
public class AsyncTaskExecutor {
    private static Executor NON_NETWORK_EXECUTOR = Executors.newCachedThreadPool();
    private static Executor SHORT_NETWORK_EXECUTOR = Executors.newCachedThreadPool();
    private static Executor LONG_NETWORK_EXECUTOR = Executors.newCachedThreadPool();

    public static <Params> void executeNonNetworkTask(AsyncTask task, Params... params) {
        executeOnExecutor(task, NON_NETWORK_EXECUTOR, params);
    }

    public static <Params> void executeShortNetworkTask(AsyncTask task, Params... params) {
        executeOnExecutor(task, SHORT_NETWORK_EXECUTOR, params);
    }

    public static <Params> void executeLongNetworkTask(AsyncTask task, Params... params) {
        executeOnExecutor(task, LONG_NETWORK_EXECUTOR, params);
    }

    public static <Params> void executeOnExecutor(AsyncTask task, Executor executor, Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(executor, params);
        } else {
            task.execute(params);
        }
    }
}
