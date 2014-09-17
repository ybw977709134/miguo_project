package co.onemeter.oneapp.utils;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-27
 * Time: 上午10:21
 * To change this template use File | Settings | File Templates.
 */
public class TimeElapseReportRunnable implements Runnable{
    private boolean isRunning=true;
    private long startTime;
    private TimeElapseReportListener listener;

    private final static long SLEEP_INTERVAL=200;

    public static interface TimeElapseReportListener {
        //elapsed in ms
        void reportElapse(final long elapsed);
    }

    public TimeElapseReportRunnable() {
        startTime=System.currentTimeMillis();
    }

    public void setElapseReportListener(TimeElapseReportListener l) {
        listener=l;
    }

    public void stop() {
        isRunning=false;
    }

    public void reset() {
        startTime=System.currentTimeMillis();
    }
    @Override
    public void run() {
        while(isRunning) {
            try {
                Thread.sleep(SLEEP_INTERVAL);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(null != listener) {
                listener.reportElapse(System.currentTimeMillis()-startTime);
            }
        }
    }
}
