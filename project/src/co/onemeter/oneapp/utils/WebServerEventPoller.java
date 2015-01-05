package co.onemeter.oneapp.utils;

import android.content.Context;
import org.wowtalk.api.*;
import co.onemeter.oneapp.ui.GlobalValue;

import java.util.LinkedList;

/**
 * 轮询Web服务器上发生的事件，比如有人向我发送了好友请求。
 * 这是为了弥补某些事件缺少推送通知的不足。
 * Created by pzy on 12/16/14.
 */
public class WebServerEventPoller {

    public final static String TASK_ID_NEW_FRIEND_REQ = "2fe13e73-6128-43ef-be7f-a82342766000";
    public final static String TASK_ID_NEW_REVIEWS = "2fe13e73-6128-43ef-be7f-a82342766001";

    private static WebServerEventPoller theInstance;
    private Context context;

    protected WebServerEventPoller(Context context) {
        this.context = context;
        PeriodRunnableManager p = PeriodRunnableManager.instance();
        p.addTask(TASK_ID_NEW_FRIEND_REQ, newFriendReqTask, 60);
        p.addTask(TASK_ID_NEW_REVIEWS, newReviewsTask, 60);
    }

    public static WebServerEventPoller instance(Context context) {
        if (theInstance == null) {
            theInstance = new WebServerEventPoller(context);
        }
        return theInstance;
    }

    /**
     * 在工作线程中执行Web请求。
     */
    public void invoke() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PeriodRunnableManager.instance().invoke();
            }
        }).start();
    }

    private Runnable newFriendReqTask = new Runnable() {
        @Override
        public void run() {
            WebServerIF web = WebServerIF.getInstance(context);
            web.fGetPendingRequests();
        }
    };

    private Runnable newReviewsTask = new Runnable() {
            @Override
            public void run() {
                MomentWebServerIF web = MomentWebServerIF.getInstance(context);
                LinkedList<Review> reviews = new LinkedList<Review>();
                int errno = web.fGetReviewsOnMe(reviews);

                if (ErrorCode.OK == errno) {
                    GlobalValue.unreadMomentReviews = reviews.size();
                }
            }
        };
}
