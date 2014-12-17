package co.onemeter.oneapp.utils;

import android.text.TextUtils;

import java.util.LinkedList;

/**
 * 维护一个 Runnable 队列，执行它们，保证最小时间间隔。
 * Created by pzy on 12/16/14.
 */
public class PeriodRunnableManager {
    public static class Task {
        public String id;
        public Runnable runnable;

        /**
         * Last execute time, in unix timestamp in ms.
         */
        public long lastExecute;

        public long minIntervalMs;

        /**
         * @param uuid
         * @param runnable
         * @param minIntervalSec interval in seconds.
         */
        public Task(String uuid, Runnable runnable, int minIntervalSec) {
            this.id = uuid;
            this.runnable = runnable;
            this.minIntervalMs = minIntervalSec * 1000;
            lastExecute = 0;
        }
    }

    private static PeriodRunnableManager theInstance;
    LinkedList<Task> tasks = new LinkedList<>();

    private PeriodRunnableManager() {
    }

    public static PeriodRunnableManager instance() {
        if (theInstance == null) {
            theInstance = new PeriodRunnableManager();
        }
        return theInstance;
    }

    public void invoke() {
        for (Task task : tasks) {
            if (System.currentTimeMillis() - task.lastExecute >= task.minIntervalMs) {
                task.lastExecute = System.currentTimeMillis();
                task.runnable.run();
            }
        }
    }

    /**
     * 自动重复 Task。
     * @param uuid
     * @param runnable
     * @param minIntervalSec
     * @return
     */
    public Task addTask(String uuid, Runnable runnable, int minIntervalSec) {
        Task task = findTask(uuid);
        if (task == null) {
            task = new Task(uuid, runnable, minIntervalSec);
            tasks.add(task);
        }
        return task;
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void removeTask(String uuid) {
        tasks.remove(findTask(uuid));
    }

    private Task findTask(String uuid) {
        for (Task task : tasks) {
            if (TextUtils.equals(task.id, uuid)) {
                return task;
            }
        }
        return null;
    }
}
