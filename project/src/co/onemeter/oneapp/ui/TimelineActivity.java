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
        instance = this;

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

        hideUnavailableNewMomentButtons();
        hideNewMomentPanel();
        switchToAll();
    }

    public static TimelineActivity instance() {
        return instance;
    }

    public boolean handleBackPress() {
        if (isNewMomentPanelShowing()) {
            hideNewMomentPanel();
            return true;
        }
        if (allTimelineFragment.handleBackPress() || myTimelineFragment.handleBackPress()) {
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

    private void hideUnavailableNewMomentButtons() {
        if (Buddy.ACCOUNT_TYPE_TEACHER != PrefUtil.getInstance(this).getMyAccountType()) {
            q.find(R.id.vg_new_notice).invisible();
            q.find(R.id.vg_new_video).invisible();
        }
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
                gotoCreateMoment(MomentActivity.TAG_STUDY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_life:
                gotoCreateMoment(MomentActivity.TAG_LIFE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_notice:
                gotoCreateMoment(MomentActivity.TAG_NOTICE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_question:
                gotoCreateMoment(MomentActivity.TAG_QA_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_vote:
                gotoCreateMoment(MomentActivity.TAG_SURVEY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_video:
                gotoCreateMoment(MomentActivity.TAG_VIDEO_IDX);
                hideNewMomentPanel();
                break;
        }
    }

    private void gotoCreateMoment(int tagIdx) {
        startActivityForResult(
                new Intent(this, CreateNormalMomentWithTagActivity.class)
                        .putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_TAG_ID, tagIdx),
                REQ_CREATE_MOMENT
        );
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