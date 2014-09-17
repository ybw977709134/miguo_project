package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 4/2/13
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class TimePiece {
    private static final int DEFAULT_SECONDS = 60;

    private static final int MSG_TIMER = 0;

    private boolean isCountDown = false;

    private int countSecond = 0;

    private int countDownSecond;

    private Context mContext;

    private Activity mActivity;

    private OnCountChangedListener mOnCountChangedListener;

    private OnCountDownChangedListener mOnCountDownChangedListener;

    private OnCountDownFinished mOnCountDownFinished;

    private Timer timer = new Timer(true);

    private TimerTask task = new TimerTask() {

        @Override
        public void run() {
            Message message = new Message();
            message.what = MSG_TIMER;
            mHandler.sendMessage(message);
        }
    };

    final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TIMER:
                    if (isCountDown) {
                        countDownSecond--;
                        if (mOnCountDownChangedListener != null) {
                            mOnCountDownChangedListener.onCountDownChanged(countDownSecond);
                        }
                        if (countDownSecond == 0) {
                            if (mOnCountDownFinished != null) {
                                mOnCountDownFinished.onCountDownFinished();
                            }
                            timer.cancel();
                        }
                    } else {
                        countSecond++;
                        if (mOnCountChangedListener != null) {
                            mOnCountChangedListener.onCount(countSecond);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public TimePiece() {}

    public TimePiece(Activity activity){
        mActivity = activity;
    }

    public void startCount() {
        countSecond = 0;
        timer.schedule(task, 1000, 1000);
    }

    public void startCountDown() {
        isCountDown = true;
        countDownSecond = DEFAULT_SECONDS;
        timer.schedule(task, 1000, 1000);
    }

    public void startCountDown(int countDownSecond) {
        isCountDown = true;
        this.countDownSecond = countDownSecond;
        timer.schedule(task, 1000, 1000);
    }

    public void stop() {
        timer.cancel();
    }

    public void setOnCountChangedListener(OnCountChangedListener l) {
        mOnCountChangedListener = l;
    }

    public void setOnCountDownChangedListener(OnCountDownChangedListener l) {
        this.mOnCountDownChangedListener = l;
    }

    public void setOnCountDownFinished(OnCountDownFinished l) {
        this.mOnCountDownFinished = l;
    }

    public interface OnCountChangedListener {
        public void onCount(int second);
    }

    public interface OnCountDownChangedListener {
        public void onCountDownChanged(int countDownSecond);
    }

    public interface OnCountDownFinished {
        public void onCountDownFinished();
    }
}
