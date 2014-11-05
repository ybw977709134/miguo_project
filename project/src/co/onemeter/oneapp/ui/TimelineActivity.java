package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.Review;
import org.wowtalk.api.WowMomentWebServerIF;
import org.wowtalk.ui.GlobalValue;

import java.util.ArrayList;

/**
 * <p>(时间线|分享|动态)页面。</p>
 * Created by pzy on 10/13/14.
 */
public class TimelineActivity extends FragmentActivity implements View.OnClickListener {

    private static final int REQ_CREATE_MOMENT = 124;
    private static TimelineActivity instance;

    private AllTimelineFragment allTimelineFragment;
    private MyTimelineFragment myTimelineFragment;
    private View newMomentPanel;
    private AQuery q = new AQuery(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        q.find(R.id.btn_all).clicked(this);
        q.find(R.id.btn_me).clicked(this);
        q.find(R.id.title_edit).clicked(this);
        q.find(R.id.vg_new_text).clicked(this);
        q.find(R.id.vg_new_pic).clicked(this);
        q.find(R.id.vg_new_camera).clicked(this);
        q.find(R.id.vg_new_question).clicked(this);
        q.find(R.id.vg_new_vote).clicked(this);
        q.find(R.id.vg_new_video).clicked(this);

        newMomentPanel = q.find(R.id.new_moment_panel).clicked(this).getView();

        allTimelineFragment =  new AllTimelineFragment();
        myTimelineFragment = new MyTimelineFragment();
        Bundle args = new Bundle();
        args.putString("uid", PrefUtil.getInstance(this).getUid());
        myTimelineFragment.setArguments(args);

        hideNewMomentPanel();

        switchToAll();

        instance = this;
    }

    public static TimelineActivity instance() {
        if (instance == null) {
            instance = new TimelineActivity(); // XXX silly
        }
        return instance;
    }

    public boolean handleBackPress() {
        if (isNewMomentPanelShowing()) {
            hideNewMomentPanel();
            return true;
        }
        return false;
    }

    private boolean isNewMomentPanelShowing() {
        return newMomentPanel != null && newMomentPanel.getVisibility() == View.VISIBLE;
    }

    private void hideNewMomentPanel() {
        newMomentPanel.setVisibility(View.GONE);
    }

    private void toggleNewMomentPanel() {
        newMomentPanel.setVisibility(isNewMomentPanelShowing() ?
                View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_edit:
                toggleNewMomentPanel();
                break;
            case R.id.new_moment_panel:
                hideNewMomentPanel();
                break;
            case R.id.btn_all:
                switchToAll();
                break;
            case R.id.btn_me:
                switchToMy();
                break;
            case R.id.vg_new_text:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_PLAIN_TEXT);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_pic:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_PIC_ALBUM);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_camera:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_PIC_CAMERA);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_question:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_QA);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_vote:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_VOTE);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_video:
                gotoCreateMoment(CreateMomentActivity.MEDIA_TYPE_VIDEO);
                hideNewMomentPanel();
                break;
        }
    }

    private void gotoCreateMoment(int mediaType) {
        long maxTimeStamp=0; // TODO
        Intent intent = new Intent(this, CreateMomentActivity.class)
                .putExtra(CreateMomentActivity.EXTRA_MEDIA_TYPE, mediaType)
                .putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP, maxTimeStamp);
        startActivityForResult(intent, REQ_CREATE_MOMENT);
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