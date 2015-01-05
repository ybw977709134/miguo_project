package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import co.onemeter.oneapp.R;

import com.androidquery.AQuery;

import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.InputBoardManager;

/**
 * <p>(时间线|分享|动态)页面。</p>
 * Created by pzy on 10/13/14.
 */
public class TimelineActivity extends FragmentActivity implements View.OnClickListener {

    /** optional moment owner's uid */
    public static final String EXTRA_UID = "uid";
    /** If page title is empty, buttons will show instead. */
    public static final String EXTRA_PAGE_TITLE = "title";
    public static final int COMMENT_MOST_WORDS = 260;
    public static final int NETWORK_TAG_UPLOADING_ALBUMCOVER = 123;

    private final static int TAG_ALL_IDX=0;
    public final static int TAG_NOTICE_IDX =1;
    public final static int TAG_QA_IDX=2;
    public final static int TAG_STUDY_IDX =3;
    public final static int TAG_SURVEY_IDX=4;
    public final static int TAG_LIFE_IDX =5;
    public final static int TAG_VIDEO_IDX =6;

    private static final int REQ_CREATE_MOMENT = 124;
    private static TimelineActivity instance;

    private AllTimelineFragment allTimelineFragment;
    private MyTimelineFragment myTimelineFragment;
    private TimelineFragment currTimelineFragment;
    private View newMomentPanel;
    private AQuery q = new AQuery(this);
    private String uid;
    private String pageTitle;

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

        getData(savedInstanceState == null ? getIntent().getExtras() : savedInstanceState);

        allTimelineFragment =  new AllTimelineFragment();
        myTimelineFragment = new MyTimelineFragment();
        Bundle args = new Bundle();
        args.putString(MyTimelineFragment.EXTRA_UID,
                uid != null ? uid : PrefUtil.getInstance(this).getUid());
        myTimelineFragment.setArguments(args);

        hideUnavailableNewMomentButtons();
        hideNewMomentPanel();

        if (uid != null) {
        	if (!TextUtils.isEmpty(pageTitle)) {//好友的成长日记
        		switchToSingle();
        		setTitle(pageTitle);
        	} else {
        		switchToSingle();//自己的成长日记
        	}
            
        } else {
            switchToAll();
        }

        
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (title != null) {
            q.find(R.id.btn_layout).invisible();
            q.find(R.id.title_text).visible().text(title);
            q.find(R.id.title_edit).visibility(View.GONE);
        } else {
            q.find(R.id.btn_layout).visible();
            q.find(R.id.title_text).invisible();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 发布动态成功后，刷新
        if (requestCode == REQ_CREATE_MOMENT && resultCode == Activity.RESULT_OK
                && currTimelineFragment != null) {
            currTimelineFragment.checkNewMoments();
        }
    }

    public static TimelineActivity instance() {
        return instance;
    }

    private void getData(Bundle state) {
        if (state != null) {
            uid = state.getString(EXTRA_UID);
            pageTitle = state.getString(EXTRA_PAGE_TITLE);
        }
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
                switchToSingle();
                break;
            case R.id.vg_new_study:
                gotoCreateMoment(TAG_STUDY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_life:
                gotoCreateMoment(TAG_LIFE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_notice:
                gotoCreateMoment(TAG_NOTICE_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_question:
                gotoCreateMoment(TAG_QA_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_vote:
                gotoCreateMoment(TAG_SURVEY_IDX);
                hideNewMomentPanel();
                break;
            case R.id.vg_new_video:
                gotoCreateMoment(TAG_VIDEO_IDX);
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
        currTimelineFragment = allTimelineFragment;
    }

    private void switchToSingle() {
        q.find(R.id.btn_all).background(R.drawable.tab_button_left_white).textColorId(R.color.white);
        q.find(R.id.btn_me).background(R.drawable.tab_button_right_white_a).textColorId(R.color.blue);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, myTimelineFragment)
                .commit();
        currTimelineFragment = myTimelineFragment;
    }

    /**
     * 跳转到好友成长日志页面
     * @param context
     * @param uid
     * @param pageTitle
     */
    public static void launch(Context context, String uid, String pageTitle) {
        Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_PAGE_TITLE, pageTitle);
        context.startActivity(intent);
    }
    
    /**
     * 跳转到自己的成长日志页面
     * @param context
     * @param uid
     * @param pageTitle
     * @param myInfo
     */
    public static void launch(Context context, String uid, String pageTitle,String myInfo) {
        Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_UID, uid);
//        intent.putExtra(EXTRA_PAGE_TITLE, pageTitle);
        context.startActivity(intent);
    }

    public interface OnMomentReviewDeleteListener {
        void onMomentDelete(String momentId, Review review);
    }

    public static String getSelectedTagServerDesc(Context context,int tagIdx,boolean surveyMultiSelect) {
        String tag="";
        switch(tagIdx) {
            case TAG_ALL_IDX:
                break;
            case TAG_NOTICE_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_NOTICE;
                break;
            case TAG_QA_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_QA;
                break;
            case TAG_SURVEY_IDX:
                if(surveyMultiSelect) {
                    tag=Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI;
                } else {
                    tag=Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE;
                }
                break;
            case TAG_STUDY_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_STUDY;
                break;
            case TAG_LIFE_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_LIFE;
                break;
            case TAG_VIDEO_IDX:
                tag=Moment.SERVER_MOMENT_TAG_FOR_VIDEO;
                break;
            default:
                break;
        }
        
        return tag;
    }

    public static void deleteMomentReview(final Activity activity,final String momentId, final Review replyTo,final OnMomentReviewDeleteListener momentReviewDelListener) {
        final MessageBox msgBox = new MessageBox(activity);
        msgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                MomentWebServerIF mMomentWeb = MomentWebServerIF.getInstance(activity);
                return mMomentWeb.fDeleteMomentReview(momentId,replyTo);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                msgBox.dismissWait();

                if(errno == ErrorCode.OK) {
                    if(null != momentReviewDelListener) {
                        momentReviewDelListener.onMomentDelete(momentId,replyTo);
                    }
//                                        if(null != instance) {
//                                            instance.deleteAReview(momentId,replyTo);
//                                        }
                } else {
                    msgBox.toast(R.string.msg_operation_failed);
                }
            }
        }.execute((Void)null);
    }

    public static String getSelectedTagLocalDesc(Context context,int tagIdx) {
        String tag="";
        switch(tagIdx) {
            case TAG_ALL_IDX:
                break;
            case TAG_NOTICE_IDX:
                tag=context.getString(R.string.moment_tag_notice);
                break;
            case TAG_QA_IDX:
                tag=context.getString(R.string.moment_tag_qa);
                break;
            case TAG_SURVEY_IDX:
                tag=context.getString(R.string.moment_tag_survey);
                break;
            case TAG_STUDY_IDX:
                tag=context.getString(R.string.moment_tag_study);
                break;
            case TAG_LIFE_IDX:
                tag=context.getString(R.string.moment_tag_life);
                break;
            case TAG_VIDEO_IDX:
                tag=context.getString(R.string.moment_tag_video);
                break;
            default:
                break;
        }
        return tag;
    }

    public static void replyToMoment_helper(int momentPosition, String momentId, Review replyTo,
                                            Activity activity,
                                            InputBoardManager.InputResultHandler handler,
                                            InputBoardManager.ChangeToOtherAppsListener changeAppsListener,
                                            View.OnClickListener onLikeClickListener) {
        InputBoardManager inputMgr = handler.getInputBoardMangager();

        Database db = new Database(activity);
        final Moment moment=db.fetchMoment(momentId);

        if (inputMgr == null) {
            inputMgr = new InputBoardManager(activity,
                    (ViewGroup)activity.findViewById(R.id.input_board_holder),
                    handler, changeAppsListener);
            inputMgr.setIsWithMultimediaMethod(false);
            inputMgr.drawableResId().open = R.drawable.sms_add_btn;
            inputMgr.drawableResId().close = R.drawable.sms_close_btn;

            inputMgr.drawableResId().gotoEmotion = R.drawable.timeline_like_btn;
//            inputMgr.drawableResId().gotoEmotion = R.drawable.sms_kaomoji_btn;
            inputMgr.drawableResId().keyboard = R.drawable.sms_keyboard;
            inputMgr.drawableResId().voiceNormal = R.drawable.sms_voice_btn;
            inputMgr.drawableResId().voicePressed = R.drawable.sms_voice_btn_p;

            inputMgr.setMomentLikeDrawable(R.drawable.timeline_like_btn,R.drawable.timeline_like_btn_a);
            handler.setInputBoardMangager(inputMgr);
        }
        if (inputMgr == null) {
            return;
        }

        String title;
        if (replyTo == null)
            title = String.format(activity.getString(R.string.moments_comment_hint),
                    TimelineActivity.COMMENT_MOST_WORDS);
        else
            title = String.format(activity.getResources().getString(R.string.moments_reply_hint),
                    replyTo.nickname,
                    TimelineActivity.COMMENT_MOST_WORDS);

        inputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
        inputMgr.setInputHint(title);
//        inputMgr.extra().putInt(MomentDetailActivity.EXTRA_REPLY_TO_MOMENT_POS, momentPosition);
        inputMgr.extra().putString(MomentDetailActivity.EXTRA_REPLY_TO_MOMENT_ID, momentId);
        inputMgr.extra().putParcelable(MomentDetailActivity.EXTRA_REPLY_TO_REVIEW, replyTo);
//
        inputMgr.setLayoutForTimelineMoment(moment,onLikeClickListener);
    }

    /**
     * 用户可以对评论做什么？
     *
     * @param momentPosition position in list
     * @param momentId
     * @param replyTo
     * @param menu
     * @param activity
     * @param handler
     */
    public static void doWithReview(
            final int momentPosition, final String momentId, final Review replyTo,
            final BottomButtonBoard menu,
            final Activity activity,
            final InputBoardManager.InputResultHandler handler,
            final InputBoardManager.ChangeToOtherAppsListener chageAppsListener,
            final TimelineActivity.OnMomentReviewDeleteListener momentReviewDelListener,
            final View.OnClickListener onLikeBtnClickListener) {

        // 除了回复评论，还可以对评论的内容进行操作

        final InputBoardManager inputMgr = handler.getInputBoardMangager();
        if (inputMgr != null)
            inputMgr.setSoftKeyboardVisibility(false);

        menu.add(activity.getString(R.string.moments_reply), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        replyToMoment_helper(momentPosition, momentId, replyTo, activity, handler,
                                chageAppsListener, onLikeBtnClickListener);
                        menu.dismiss();
                    }
                });

        //if review is made by me or moment is mine, add delete item
        String myUserId = PrefUtil.getInstance(activity).getUid();

        Database db = new Database(activity);
        Moment replyMoment=db.fetchMoment(momentId);
        if (myUserId.equals(replyTo.uid) || (null != replyMoment && replyMoment.owner.userID.equals(myUserId))) {
            menu.add(activity.getString(R.string.contacts_local_delete), BottomButtonBoard.BUTTON_BLUE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
//                            final MessageBox msgBox = new MessageBox(activity);
                            menu.dismiss();
                            deleteMomentReview(activity,momentId,replyTo,momentReviewDelListener);
//                            msgBox.showWait();
//                            new AsyncTask<Void, Void, Integer>() {
//                                @Override
//                                protected Integer doInBackground(Void... params) {
//                                    WowMomentWebServerIF mMomentWeb = WowMomentWebServerIF.getInstance(activity);
//                                    return mMomentWeb.fDeleteMomentReview(momentId,replyTo);
//                                }
//
//                                @Override
//                                protected void onPostExecute(Integer errno) {
//                                    msgBox.dismissWait();
//
//                                    if(errno == ErrorCode.OK) {
//                                        if(null != momentReviewDelListener) {
//                                            momentReviewDelListener.onMomentDelete(momentId,replyTo);
//                                        }
////                                        if(null != instance) {
////                                            instance.deleteAReview(momentId,replyTo);
////                                        }
//                                    } else {
//                                        msgBox.toast(R.string.msg_operation_failed);
//                                    }
//                                }
//                            }.execute((Void)null);
                        }
                    });
        }


        TextOperationHelper.fillMenu(activity, menu, replyTo.text, true);

        menu.show();
    }
}