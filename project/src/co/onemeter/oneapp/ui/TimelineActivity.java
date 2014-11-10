package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.*;
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
        q.find(R.id.vg_new_question).clicked(this);
        q.find(R.id.vg_new_study).clicked(this);
        q.find(R.id.vg_new_life).clicked(this);
        q.find(R.id.vg_new_vote).clicked(this);
        q.find(R.id.vg_new_notice).clicked(this);
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
            case R.id.vg_new_study:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_STUDY,
                        CreateMomentActivity.MEDIA_FLAG_ALL,
                        getString(R.string.moment_add_study));
                hideNewMomentPanel();
                break;
            case R.id.vg_new_life:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_LIFE,
                        CreateMomentActivity.MEDIA_FLAG_ALL,
                        getString(R.string.moment_add_life));
                hideNewMomentPanel();
                break;
            case R.id.vg_new_notice:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_NOTICE,
                        CreateMomentActivity.MEDIA_FLAG_ALL,
                        getString(R.string.moment_add_study));
                hideNewMomentPanel();
                break;
            case R.id.vg_new_question:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_QA,
                        CreateMomentActivity.MEDIA_FLAG_ALL,
                        getString(R.string.moment_add_question));
                hideNewMomentPanel();
                break;
            case R.id.vg_new_vote:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE,
                        CreateMomentActivity.MEDIA_FLAG_ALL,
                        getString(R.string.moment_add_vote));
                hideNewMomentPanel();
                break;
            case R.id.vg_new_video:
                gotoCreateMoment(Moment.SERVER_MOMENT_TAG_FOR_VIDEO,
                        CreateMomentActivity.MEDIA_FLAG_PLAIN_TEXT
                                | CreateMomentActivity.MEDIA_FLAG_VIDEO
                                | CreateMomentActivity.MEDIA_FLAG_LOC,
                        getString(R.string.moment_add_video));
                hideNewMomentPanel();
                break;
        }
    }

    private void gotoCreateMoment(String tag, int mediaType, String pageTitle) {
        long maxTimeStamp=0; // TODO
        Intent intent = new Intent(this, CreateMomentActivity.class)
                .putExtra(CreateMomentActivity.EXTRA_SERVER_MOMENT_TAG, tag)
                .putExtra(CreateMomentActivity.EXTRA_MEDIA_FLAGS, mediaType)
                .putExtra(CreateMomentActivity.EXTRA_PAGE_TITLE, pageTitle)
                .putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP, maxTimeStamp);
        startActivityForResult(intent, REQ_CREATE_MOMENT);
    }

    private void switchToAll() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_white_a).textColorId(R.color.blue);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_white).textColorId(R.color.white);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, allTimelineFragment)
                .commit();
    }

    private void switchToMy() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_white).textColorId(R.color.white);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_white_a).textColorId(R.color.blue);
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