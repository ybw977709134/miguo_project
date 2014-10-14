package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Review;
import org.wowtalk.api.WowMomentWebServerIF;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

/**
 * <p>(时间线|分享|动态)页面。</p>
 * Created by pzy on 10/13/14.
 */
public class TimelineActivity extends FragmentActivity implements View.OnClickListener {

    private static TimelineActivity instance;
    AllTimelineFragment allTimelineFragment;
    MyTimelineFragment myTimelineFragment;
    AQuery q = new AQuery(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        q.find(R.id.btn_all).clicked(this);
        q.find(R.id.btn_me).clicked(this);
        q.find(R.id.title_edit).clicked(this);

        allTimelineFragment =  new AllTimelineFragment();
        myTimelineFragment = new MyTimelineFragment();

        switchToAll();
    }

    public static TimelineActivity instance() {
        if (instance == null) {
            instance = new TimelineActivity();
        }
        return instance;
    }

    public boolean handleBackPress() {
        // TODO
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_edit:
                new MessageBox(this).show(null, getString(R.string.not_implemented));
                break;
            case R.id.btn_all:
                switchToAll();
                break;
            case R.id.btn_me:
                switchToMy();
                break;
        }
    }

    private void switchToAll() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_a).textColorId(R.color.white);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right).textColorId(R.color.text_gray1);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, allTimelineFragment)
                .commit();
    }

    private void switchToMy() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left).textColorId(R.color.text_gray1);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_a).textColorId(R.color.white);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, myTimelineFragment)
                .commit();
    }

    /**
     * 检查新评论。内置频率保护。
     */
    public static class CheckNewReviewsTask extends AsyncTask<Context, Void, Integer> {

        @Override
        protected Integer doInBackground(Context... contexts) {

            ArrayList<Review> reviews = new ArrayList<Review>();

            try {
                int errno = WowMomentWebServerIF.getInstance(contexts[0]).fGetReviewsOnMe(reviews);
                if (ErrorCode.OK == errno) {
                    GlobalValue.unreadMomentReviews = reviews.size();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorCode.BAD_RESPONSE;
            }
            return ErrorCode.OK;
        }
    };
}