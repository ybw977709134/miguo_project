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
    private static Executor NEW_TASK_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            new Thread(runnable).start();
        }
    };

    /**
     * 执行非网络操作。
     * @param task
     * @param params
     * @param <Params>
     * @param <Progress>
     * @param <Result>
     */
    public static <Params, Progress, Result> void executeNonNetworkTask(
            AsyncTask<Params, Progress, Result> task, Params... params) {
        executeOnExecutor(task, NON_NETWORK_EXECUTOR, params);
    }

    /**
     * 执行短暂的网络操作，比如一个普通的HTTP请求。
     * @param task
     * @param params
     * @param <Params>
     * @param <Progress>
     * @param <Result>
     */
    public static <Params, Progress, Result> void executeShortNetworkTask(
            AsyncTask<Params, Progress, Result> task, Params... params) {
        executeOnExecutor(task, SHORT_NETWORK_EXECUTOR, params);
    }

    /**
     * 执行耗时较长的网络操作，比如上传、下载文件。
     * @param task
     * @param params
     * @param <Params>
     * @param <Progress>
     * @param <Result>
     */
    public static <Params, Progress, Result> void executeLongNetworkTask(
            AsyncTask<Params, Progress, Result> task, Params... params) {
        executeOnExecutor(task, LONG_NETWORK_EXECUTOR, params);
    }

    /**
     * 总是新开一个线程执行任务。
     * @param task
     * @param params
     * @param <Params>
     * @param <Progress>
     * @param <Result>
     */
    public static <Params, Progress, Result> void executeNew(
            AsyncTask<Params, Progress, Result> task, Params... params) {
        executeOnExecutor(task, NEW_TASK_EXECUTOR, params);
    }

    public static <Params, Progress, Result> void executeOnExecutor(
            AsyncTask<Params, Progress, Result> task, Executor executor, Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(executor, params);
        } else {
            task.execute(params);
        }
    }
}
