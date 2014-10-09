package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.animation.*;
import android.widget.*;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.*;
import org.wowtalk.ui.*;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.Stamp;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactGroupIterationAdapter;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;

import java.io.File;
import java.util.*;

public class MomentActivity extends Activity
        implements OnClickListener, NetworkIFDelegate,
        MomentAdapter.ReplyDelegate,
        InputBoardManager.InputResultHandler, InputBoardManager.ChangeToOtherAppsListener,
        MomentAdapter.LoadDelegate {

    public static final int COMMENT_MOST_WORDS = 280;
    private static final int HANDLER_HEAD_CONTENT_MYINFO = 1;
    private static final int HANDLER_TITLE_SINGLE = 2;
    private static final int HANDLER_TITLE_ALL = 3;
    private static final int HANDLER_ALBUM_COVER_LOCAL = 4;
    private static final int HANDLER_TRIGGER_LOAD_MOMENTS = 5;

    private static final String EXTRA_UID = "uid";
    private static final String EXTRA_WITH_FAVORITE = "with_favorite";
    private static final String EXTRA_REPLY_TO_MOMENT_POS = "reply_to_moment_pos";
    private static final String EXTRA_REPLY_TO_MOMENT_ID = "reply_to_moment_id";
    private static final String EXTRA_REPLY_TO_REVIEW = "reply_to_review_id";
    private static final String EXTRA_SHOWN_DEPARTMENT = "showing_department";
    private static final String EXTRA_SELECTED_TAG_IDX = "selected_tag_idx";
    private static final String EXTRA_HEADER_BTNS_MODE ="header_btns_mode";

    private static final String IMAGE_CACHE_DIR = "momentmultimedia";

	public static final String TREND_SELECT_MODE = "trend_select_mode";
	public static final int SELECT_MODE_ALL = 101;
	public static final int SELECT_MODE_SINGLE = 102;

//    private static final int LOADING_REASON_INIT = 0;
    private static final int LOADING_REASON_REFRESH = 1;
    private static final int LOADING_REASON_MORE = 2;
//    private static final int LOADING_REASON_PULL_DONE_APPEND = 3;
//    private static final int LOADING_REASON_PULL_DONE_RESET = 4;
    private static final int LOADING_REASON_PULL_REFRESH = 5;

    public static final int PAGE_SIZE = 20;

    private static final int HEADER_VIEW_ID = 123;
    private static final int ROTATION_ANIMATION_DURATION = 1200;

    // activity request codes
    private static final int REQ_CREATE_MOMENT = 124;
    private static final int REQ_SHOW_MYPROFILE = 125;

    public static final int NETWORK_TAG_UPLOADING_ALBUMCOVER = 123;

    private static final long INTERVAL_REFRESH_MOMENTS = 120;

    /** 检查新评论的最小时间间隔。这是一种保护措施，避免过度频繁地请求服务器。*/
    private static final long INTERVAL_CHECK_REVIEWS = 120;

    private static final long INTERVAL_CHECK_ALBUMCOVER = 180;

    private static final int INVALID_TIMESTAMP_VALUE = 0;

    private static MomentActivity instance;

    private ImageButton btnTitleLeft;
    private ImageButton btnTitleEdit;
    private TextView tvTitle;
    private TextView txtName;
    private TextView txtSignature;
    private ImageView imgThumbnail;
    private ListView mLvSingle;
    private PullToRefreshListView mPtrSingle;
    private ProgressBar mProgressUploadingAlbumcover;
    private View mAlbumCoverView;
//    private Button btnGotoNewReviews;

    private Database dbHelper;
    private WowTalkWebServerIF mWeb;
    private WowMomentWebServerIF mMomentWeb;
    private PrefUtil mPrefUtil;

    private AsyncTask<Void, Integer, Integer> mLoadingTask;
    private String mMyUid;
    private String mTargetUid = null; // 相册封面显示谁的信息？
    private int _selectMode = SELECT_MODE_ALL; // 显示谁的动态？（所有人 vs 特定人）
//    private long mWillRefreshFromTimestamp = 0; // always get moments older than this. 0 means now.
    private boolean mNoMoreToPull = false; // no more moments to pull from server?
    private Boolean mIsLoadingMore = false;
    private boolean mAdapterRemoved = true;
    private ArrayList<Moment> momentsAll = new ArrayList<Moment>();
    private MomentAdapter mAdapter;
    private MediaInputHelper mMediaInput;
    private MessageBox mMsgBox;
    private InputBoardManager mInputMgr;
    private String mInputedPhotoPathForAlbumcover;
    private AlbumCover mAlbumCover;
    private ImageResizer mImageFetcher;
    private static long mLastRefreshTime = INVALID_TIMESTAMP_VALUE; // 最近一次刷新动态的时间
    private static long mLastCheckReviewTime = INVALID_TIMESTAMP_VALUE; // 最近一次检查新评论的时间
//    private static long mLastCheckAlbumCoverTime = INVALID_TIMESTAMP_VALUE; // 最近一次刷新相册封面的时间

    //-------- PullToRefresh for single people's moments list view
//    ImageView mHeaderRotateImageView;
//    ImageView mHeaderRotateImageDummy;
//    View mHeaderRotateImageParent;
    ImageView mHeaderBgImageView;
    private float mRotationPivotX, mRotationPivotY;
    private Matrix mHeaderImageMatrix;
    private RotateAnimation mRotateAnimation;
    static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();
    private BottomButtonBoard mMenu;

    private int[] lastItemIndex=new int[TAG_LIST_ITEM_COUNT];
    private int[] topOffset=new int[TAG_LIST_ITEM_COUNT];

    private boolean isValidMomentsLoaded=false;
    private LinkedList<Pair<String,Review>> newCreatedReviewList=new LinkedList<Pair<String,Review>>();

    TextView tvAll;
    TextView tvTimeLine;
    TextView tvQA;
    TextView tvQuestionnaire;
    TextView tvReport;
    TextView tvAttendance;
    TextView tvTodo;

    View btnAll;
    View btnMy;

    private ImageView ivAllIndicator;
    private ImageView ivTimeLineIndicator;
    private ImageView ivQAIndicator;
    private ImageView ivQuestionnaireIndicator;
    private ImageView ivReportIndicator;
    private ImageView ivAttendanceIndicator;
    private ImageView ivTodoIndicator;

    private ArrayList<GroupChatRoom> departmentList;
    private ArrayList<GroupChatRoom> rootGroups;

    private String curShownDepartmentId;

    private final static int TAG_ALL_IDX=0;
    public final static int TAG_NOTICE_IDX =1;
    public final static int TAG_QA_IDX=2;
    public final static int TAG_STUDY_IDX =3;
    public final static int TAG_SURVEY_IDX=4;
    public final static int TAG_LIFE_IDX =5;
    public final static int TAG_VIDEO_IDX =6;
    private final static int TAG_LIST_ITEM_COUNT=7;
    private int curSelectedTagIdx=TAG_ALL_IDX;

    public final static int ACTIVITY_REQ_ID_NEW_REVIEWS=134;

    private ArrayList<Buddy> buddyListInCurDepartment=new ArrayList<Buddy>();

    private boolean isWithFavorite=false;

    private boolean isInitScrollOk=false;

    private final static int HEADER_OP_BUTTONS_MODE_NORMAL=0;
    private final static int HEADER_OP_BUTTONS_MODE_STAY_TOP=1;
    private int headerOpButtonsMode=HEADER_OP_BUTTONS_MODE_NORMAL;

    private MomentActivity.OnMomentReviewDeleteListener onMomentReviewDeleteListener;

    private BeginUploadAlbumCover mBeginUploadAlbumCover = new BeginUploadAlbumCover() {
        @Override
        public void onBeginUploadCover(String filePath) {
            if (mProgressUploadingAlbumcover == null) {
                mProgressUploadingAlbumcover =
                        (ProgressBar)findViewById(R.id.progress_uploading_albumcover);
            }
            if (mProgressUploadingAlbumcover != null) {
                mProgressUploadingAlbumcover.setVisibility(View.VISIBLE);
                mProgressUploadingAlbumcover.setProgress(0);
            }

            mInputedPhotoPathForAlbumcover = filePath;
        }
    };

    public static MomentActivity instance() {
        if (instance == null) {
            instance = new MomentActivity();
        }
        return instance;
    }

    public void doLikeMoment_async(final Moment moment) {
        if(moment.likedByMe) {
            Review likeReview=null;
            for(Review aReview : moment.reviews) {
                if(Review.TYPE_LIKE == aReview.type && aReview.uid.equals(mMyUid)) {
                    likeReview=aReview;
                    break;
                }
            }
            if(null != likeReview) {
                MomentActivity.deleteMomentReview(this,moment.id,likeReview,onMomentReviewDeleteListener);
            } else {
                Log.e("delete like review null");
            }
            return;
        }

        // 1) list view 更新机制：修改 adapter 的内存数据；
        // 2) 本地数据库更新机制：仅当请求成功后才更新，且由 mMomentWeb.fReviewMoment() 更新；

        final String moment_id = moment.id;
        final Review r = new Review();
        r.id = "0"; // not generated yet
        r.type = Review.TYPE_LIKE;
        r.uid = mMyUid;
        r.nickname = mPrefUtil.getMyNickName();

        mMsgBox.showWait();
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mMomentWeb.fReviewMoment(moment_id, Review.TYPE_LIKE, null, null, r);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (errno == ErrorCode.OK) {
                    moment.likedByMe = true;
                    //as review has been stored in db,and moment is updated too,observer can detect this,no need call this any more
                    addReviewToList(moment, r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setLayoutForTimelineMoment(moment,getLikeBtnClickListener(MomentActivity.this,moment_id));
                        mInputMgr.setInputText("");
                    }
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        }.execute((Void)null);
    }

    private void doReviewMoment_async(
            final Moment moment,
            final String replyToReviewId,
            final String strComment) {

        // 1) list view 更新机制：修改 adapter 的内存数据；
        // 2) 本地数据库更新机制：仅当请求成功后才更新，且由 mMomentWeb.fReviewMoment() 更新；

        final String moment_id = moment.id;
        final Review r = new Review();
        r.id = "0"; // not generated yet
        r.type = Review.TYPE_TEXT;
        r.uid = mMyUid;
        r.nickname = mPrefUtil.getMyNickName();
        r.text = strComment;
        r.read = true;

        mMsgBox.showWait();
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mMomentWeb.fReviewMoment(moment_id, Review.TYPE_TEXT,
                            strComment, replyToReviewId, r);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if (errno == ErrorCode.OK) {
                    //as review has been stored in db,and moment is updated too,observer can detect this,no need call this any more
                    addReviewToList(moment, r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setInputText("");
                    }
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        }.execute((Void)null);
    }

    private void deleteAReview(String momentId,Review r) {
        Moment moment2op=null;

        if(null == momentsAll || null == r || TextUtils.isEmpty(momentId)) {
            return;
        }
        for(Moment aMoment : momentsAll) {
            if(aMoment.id.equals(momentId)) {
                moment2op=aMoment;
                break;
            }
        }

        if(null == moment2op || null == moment2op.reviews) {
            return;
        }
        Review review2remove=null;
        for(Review aReview : moment2op.reviews) {
            if(aReview.id.equals(r.id)) {
                review2remove=aReview;
                break;
            }
        }
        if(null != review2remove) {
            moment2op.reviews.remove(review2remove);
        }

        if(Review.TYPE_LIKE == r.type) {
            moment2op.likedByMe=false;

            if (mInputMgr != null) {
                final Moment moment2like=moment2op;
                mInputMgr.setLayoutForTimelineMoment(moment2op,new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doLikeMoment_async(moment2like);
                    }
                });
            }
        }

        notifyAdapterDataChanged();
    }

    /**
     *
     * @param m should be in momentsAll list.
     * @param r
     */
    private void addReviewToList(Moment m, Review r) {
        if (m != null) {
            if(refreshingMomentsFromServer) {
                //if loading moments from server,the moment fetched may not contain the review created now;
                //so store new created reviews here,and when load from server finish,store such review in db again;
                //if such review already exist in deb,never mind,it will be updated,no duplicate will happen
                newCreatedReviewList.add(new Pair<String,Review>(m.id,r));
            }
            if (m.reviews == null)
                m.reviews = new ArrayList<Review>();
            updateAReviewShowName(r);

            boolean contained=false;
            for(Review aReview : m.reviews) {
                if(aReview.id.equals(r.id)) {
                    contained=true;
                    break;
                }
            }

            if(!contained) {
                m.reviews.add(m.reviews.size(), r);
            }
            notifyAdapterDataChanged();
        }
    }

//    private void removeReviewFromList(Moment m, Review r) {
//        if (m != null && m.reviews != null) {
//            m.reviews.remove(r);
//            notifyAdapterDataChanged();
//        }
//    }
    private void notifyAdapterDataChanged() {
        if (mAdapter == null)
            return;
        Moment dummy = new Moment();
        mAdapter.add(dummy);
        mAdapter.remove(dummy);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * get current list view being shown.
     * @return
     */
//    private ListView getListView() {
//        return mLvSingle;
//    }

    /**
     * get current PullToRefreshListView being shown.
     * @return
     */
    private PullToRefreshListView getPTR() {
        return mPtrSingle;
    }

    private boolean isMe() {
        if (mTargetUid == null || mTargetUid.equals("")) {
            return true;
        }
        if (mTargetUid.equals(mMyUid)) {
            return true;
        } else {
            return false;
        }
    }

    private int cur_moment_show_count=PAGE_SIZE;
    private ArrayList<Moment> loadMomentFromDB() {
        ArrayList<Moment> moments = null;

        if(isWithFavorite) {
            moments = dbHelper.fetchMomentsOfFavorite(-1, cur_moment_show_count);
        } else {
            if (_selectMode == SELECT_MODE_ALL) {
                moments = dbHelper.fetchMomentsOfAllBuddies(-1, cur_moment_show_count);
            } else {
                moments = dbHelper.fetchMomentsOfSingleBuddy(mTargetUid, -1, cur_moment_show_count);
            }
        }

        return moments;
    }

    private void updateAReviewShowName(Review aReview) {
        String myUserId = mPrefUtil.getUid();
        String myNameMe = getString(R.string.friends_moment_comment_me);

        if(TextUtils.isEmpty(myUserId)) {
            return;
        }
        if (myUserId.equals(aReview.uid)) {
            aReview.nickname = myNameMe;
        }
        if (myUserId.equals(aReview.replyToUid)) {
            aReview.replyToNickname = myNameMe;
        }
    }

    private ArrayList<Moment> pendingMomentList;
    private void updateMomentsShow(ArrayList<Moment> moments) {
        if(null != mAdapter && !refreshingMomentsFromServer) {
            mAdapter.setLoadingFromServer(false);
        }

//        if(null != mAdapter && mAdapter.isMediaPlaying()) {
//            pendingMomentList=moments;
//            mAdapter.setOnMediaPlayFinishListener(new MomentAdapter.OnMediaPlayFinishListener() {
//                @Override
//                public void onMediaPlayFinished() {
//                    if(null != pendingMomentList) {
//                        Log.w("update moments with pending");
//                        updateMomentsShow(pendingMomentList);
//                    }
//                }
//            });
//            Log.w("update moments delayed when media is playing");
//            return;
//        }
        //as problem in server,if adding a friend and that request is not accepted
        //server will return such friend's moment,as required,should not display it
        Iterator<Moment> momentIterator= moments.iterator();
        while(momentIterator.hasNext()) {
            Moment aMoment=momentIterator.next();
            Buddy aBuddy=dbHelper.buddyWithUserID(aMoment.owner.userID);
            if(null != aBuddy
                    && (0 == (aBuddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_BOTH))
                    && !aMoment.owner.userID.equals(mMyUid)) {
                momentIterator.remove();
            }
        }

        // modify the comment name to 'me' when it's me.
        String myUserId = mPrefUtil.getUid();
//        String myNameMe = getString(R.string.friends_moment_comment_me);
        if (null != moments && !TextUtils.isEmpty(myUserId)) {
            for (Moment moment : moments) {
                ArrayList<Review> reviews = moment.reviews;
                if (null != reviews) {
                    for (Review review : reviews) {
                        updateAReviewShowName(review);
                    }
                }
            }
        }

        //test if load more can be shown
//        int momentCount=dbHelper.getMomentsCount();
        Log.i("load moment from server,cur moment count "+moments.size());
        if(moments.size() < cur_moment_show_count) {
            mNoMoreToPull = true;
        } else {
            mNoMoreToPull = false;
        }

        if (null != moments) {
            /* 由于 HeaderViewListAdapter 有这样的副作用：
                即使执行 ListView.setAdapter(null)，
                ListView.getAdapter() 也不为 null，
                所以需要借助 mAdapterRemoved。
             */
            momentsAll = moments;
            fixMomentOwnerInfo();

            setContentMomentWithFilter(false);
        }
    }

    public static String getSelectedTagServerDesc(Context context,int tagIdx,boolean surveyMultiSelect) {
        String tag="";
        switch(tagIdx) {
            case TAG_ALL_IDX:
                break;
            case TAG_NOTICE_IDX:
                tag=Moment.SERER_MOMENT_TAG_FOR_LIFE;
                break;
            case TAG_QA_IDX:
                tag=Moment.SERER_MOMENT_TAG_FOR_QA;
                break;
            case TAG_SURVEY_IDX:
                if(surveyMultiSelect) {
                    tag=Moment.SERER_MOMENT_TAG_FOR_SURVEY_MULTI;
                } else {
                    tag=Moment.SERER_MOMENT_TAG_FOR_SURVEY_SINGLE;
                }
                break;
            case TAG_STUDY_IDX:
                tag=Moment.SERER_MOMENT_TAG_FOR_NOTICE;
                break;
            case TAG_LIFE_IDX:
                tag=Moment.SERER_MOMENT_TAG_FOR_STUDY;
                break;
            case TAG_VIDEO_IDX:
                tag=Moment.SERER_MOMENT_TAG_FOR_VIDEO;
                break;
            default:
                break;
        }
        return tag;
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

    /**
     * 隶属的所有部门id，同时包含父部门和子部门
     */
    private ArrayList<String> mDeptIds = new ArrayList<String>();

    /**
     * 获取自己所属的部门id集合，用于过滤动态
     */
    private void setupMyGroupInfo() {
        mDeptIds = dbHelper.getGroupIdsByMemberId(mMyUid);
    }

    private void setContentMomentWithFilter(boolean onlyFilter) {
        ArrayList<Moment> momentsFilteredByTag;

        if(null != momentsAll) {
            Log.i("all moments count "+momentsAll.size());
        }
        //1. filter by tag,if survey will have 2 tag
        String curSelectedTag=getSelectedTagServerDesc(this, curSelectedTagIdx,false);
        String curSelectedTag2=getSelectedTagServerDesc(this, curSelectedTagIdx,true);
        Log.w("filter by tag "+curSelectedTag);
        if(TextUtils.isEmpty(curSelectedTag)) {
            momentsFilteredByTag = momentsAll;
        } else {
            momentsFilteredByTag= new ArrayList<Moment>();
            for(Moment aMoment : momentsAll) {
                if((!TextUtils.isEmpty(aMoment.tag) && (aMoment.tag.equals(curSelectedTag) || aMoment.tag.equals(curSelectedTag2))) ||
                        (TextUtils.isEmpty(aMoment.tag) && TAG_NOTICE_IDX ==curSelectedTagIdx)) {
                    momentsFilteredByTag.add(aMoment);
                }
            }
        }
        //2. filter by department and the limits of the moments
        // (BEGIN this filter is disabled)
//        ArrayList<Moment> momentsFilteredByDeptAndLimits = new ArrayList<Moment>();
//        for(Moment aMoment : momentsFilteredByTag) {
//            // dept
//            if (buddyListInCurDepartment.contains(aMoment.owner)) {
//                // 自己发的动态，自己能看到
//                if (!TextUtils.isEmpty(mMyUid) && mMyUid.equals(aMoment.owner.userID)) {
//                    momentsFilteredByDeptAndLimits.add(aMoment);
//                } else if (aMoment.limitedDepartmentList.isEmpty()) {
//                    // 没有limits，则都可见
//                    momentsFilteredByDeptAndLimits.add(aMoment);
//                } else if (!mDeptIds.isEmpty()) {
//                    // limits
//                    for (String deptId : mDeptIds) {
//                        if (aMoment.limitedDepartmentList.contains(deptId)) {
//                            momentsFilteredByDeptAndLimits.add(aMoment);
//                            break;
//                        }
//                    }
//                }
//            }
//        }
        // (END this filter is disabled)
        ArrayList<Moment> momentsFilteredByDeptAndLimits = momentsFilteredByTag;

        //3. filter by favorite
        ArrayList<Moment> momentsFilteredByFavorite;
        if(isWithFavorite) {
            momentsFilteredByFavorite=new ArrayList<Moment>();

            for(Moment aMoment : momentsFilteredByDeptAndLimits) {
                if(aMoment.isFavorite) {
                    momentsFilteredByFavorite.add(aMoment);
                }
            }
        } else {
            momentsFilteredByFavorite=momentsFilteredByDeptAndLimits;
        }

        ArrayList<Moment> momentsFiltered=momentsFilteredByFavorite;


//        if(0 == momentsFiltered.size()) {
//            mLvSingle.setDivider(null);
//        } else {
////            mLvSingle.setDivider(new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),R.drawable.divider_320)));
//            mLvSingle.setDivider(null);
//        }

        if (mLvSingle.getAdapter() == null || mAdapterRemoved) {
            setListAdapter(momentsFiltered,!mNoMoreToPull);
            mAdapterRemoved = false;
        } else {
            mAdapter.clear();
            mAdapter.removeMediaPlayListener();
            for (Moment moment : momentsFiltered) {
                mAdapter.add(moment);
            }
            mAdapter.setShowLoadMoreAsLastItem(!mNoMoreToPull);
            mAdapter.notifyDataSetChanged();
        }

        Log.i("set selection to "+lastItemIndex[curSelectedTagIdx]+","+topOffset[curSelectedTagIdx]);
        mLvSingle.setSelectionFromTop(lastItemIndex[curSelectedTagIdx], topOffset[curSelectedTagIdx]);
        updateHeaderBtnMode();
    }

    private void loadMomentsFromLocal() {
        ++loadingMomentId;

        if(null != mAdapter) {
            mAdapter.setLoadingFromServer(true);
        }

        ArrayList<Moment> moments=loadMomentFromDB();

        updateMomentsShow(moments);
    }

    private void loadDummyMoments() {
        Log.w("loading moments for moment activity");
        ArrayList<Moment> moments=new ArrayList<Moment>();

        updateMomentsShow(moments);
    }

    private void loadMomentsFromLocal_async(final long curLoadingMomentId) {
        Log.w("loading moments for moment activity async "+curLoadingMomentId);
        if(null != mAdapter) {
            mAdapter.setLoadingFromServer(true);
        }
        new AsyncTask<Void, Void, ArrayList<Moment>>() {
            @Override
            protected ArrayList<Moment> doInBackground(Void... params) {
                ArrayList<Moment> curMomentList=null;
                if(curLoadingMomentId == loadingMomentId || !isValidMomentsLoaded) {
                    curMomentList=loadMomentFromDB();
                }
                return curMomentList;
            }

            @Override
            protected void onPostExecute(ArrayList<Moment> moments) {
                if(curLoadingMomentId == loadingMomentId || !isValidMomentsLoaded) {
                    Log.w("loading moments for moment activity async updating "+curLoadingMomentId);
                    if(null != moments && moments.size() > 0) {
                        isValidMomentsLoaded=true;
                    }
                    updateMomentsShow(moments);
                }
            }
        }.execute((Void)null);
    }

    private boolean refreshingMomentsFromServer=false;
//    private boolean loadingRefreshFromServerSync=false;
    private void loadMomentsFromServer(final int loadReason) {
        if(isWithFavorite) {
            if(mIsLoadingMore) {
                mIsLoadingMore = false;
                mAdapter.notifyLoadingCompleted();
                triggerReloadMoments();
            }

            onRefreshComplete();
            return;
        }
        mLastRefreshTime = new java.util.Date().getTime() / 1000;
        refreshingMomentsFromServer=true;

        if(null != mAdapter) {
            mAdapter.setLoadingFromServer(true);
        }
        //if album cover not exist,check it also,as 120s interval will be a little long on resume
//        long now = new java.util.Date().getTime() / 1000;
//        if (mAlbumCover == null || mAlbumCover.timestamp == -1 || mAlbumCover.fileId == null || now - mLastCheckAlbumCoverTime > INTERVAL_CHECK_ALBUMCOVER) {
//            checkoutAlbumCover();
//        }

        checkForNewReviews_async();
//        if(LOADING_REASON_PULL_REFRESH == loadReason ||
//                LOADING_REASON_MORE == loadReason) {
//            loadingRefreshFromServerSync=true;
//        }
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    long momentTimeStampFromServer=-1;
                    if(LOADING_REASON_MORE == loadReason) {
                        momentTimeStampFromServer=getMinTimeStamp();
                    }

                    if (_selectMode == SELECT_MODE_ALL) {
                        if(TextUtils.isEmpty(curShownDepartmentId)) {
                            return mMomentWeb.fGetMomentsOfAll(momentTimeStampFromServer, PAGE_SIZE, true);
                        } else {
                            return mMomentWeb.fGetMomentsOfGroup(curShownDepartmentId,momentTimeStampFromServer, PAGE_SIZE, true);
                        }
                    } else {
                        return mMomentWeb.fGetMomentsOfBuddy(mTargetUid, momentTimeStampFromServer, PAGE_SIZE, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mIsLoadingMore = false;
                onRefreshComplete();
                mAdapter.notifyLoadingCompleted();

                refreshingMomentsFromServer=false;
                if(null != newCreatedReviewList && newCreatedReviewList.size()>0) {
                    for(Pair<String,Review> aPair : newCreatedReviewList) {
                        Log.w("restore review for moment "+aPair.first+",with review id "+aPair.second.id);
                        mMomentWeb.storeReview2db(aPair.first,aPair.second);
                    }
                    newCreatedReviewList.clear();
                }
                //if no mements for this situation,noMomentDescView progressbar can not update,so trigger it here
                triggerReloadMoments();
//                loadingRefreshFromServerSync=false;
//
//                if (ErrorCode.OK == errno) {
//                    // tip on new reviews
////                    Moment m = new Moment(null);
////                    GlobalValue.unreadMomentReviews = dbHelper.fetchNewReviews(m);
////                    updateNewReviewButton();
//
//                    triggerReloadMoment();
//                }
            }
        }.execute((Void) null);
    }

//    private void triggerReloadMoment() {
//        Database.markDBTableModified(Database.TBL_MOMENT);
//    }

    private void loadMoreHandle() {
        cur_moment_show_count += PAGE_SIZE;
//        int momentCount=dbHelper.getMomentsCount();
//        if(momentCount >= cur_moment_show_count) {
//            loadMomentsFromLocal_async();
//        }

        //however,we have to check from server to update status,as we do not know if moment stored local is deleted or updated in server

        loadMomentsFromServer(LOADING_REASON_MORE);
    }

    /**
     * Load moments older than mWillRefreshFromTimestamp, into list view.
     *
     * @param loadingReason LOADING_REASON_*
     */
//    private void loadMoments(final int loadingReason) {
//
//        ArrayList<Moment> moments = null;
//        boolean needPull = false;
//        final long maxTimestampForThisPull = mWillRefreshFromTimestamp;
//
//        // first, load moments from local db
//        if (LOADING_REASON_INIT == loadingReason
//                || LOADING_REASON_MORE == loadingReason
//                || LOADING_REASON_PULL_DONE_APPEND == loadingReason
//                || LOADING_REASON_PULL_DONE_RESET == loadingReason) {
//            if (_selectMode == SELECT_MODE_ALL) {
//                moments = dbHelper.fetchMomentsOfAllBuddies(mWillRefreshFromTimestamp, PAGE_SIZE);
//            } else {
//                moments = dbHelper.fetchMomentsOfSingleBuddy(mTargetUid, mWillRefreshFromTimestamp, PAGE_SIZE);
//            }
//            //as problem in server,if adding a friend and that request is not accepted
//            //server will return such friend's moment,as required,should not display it
//            Iterator<Moment> momentIterator= moments.iterator();
//            while(momentIterator.hasNext()) {
//                Moment aMoment=momentIterator.next();
//                Buddy aBuddy=dbHelper.buddyWithUserID(aMoment.owner.userID);
//                if(null != aBuddy && aBuddy.getFriendShipWithMe() != Buddy.RELATIONSHIP_FRIEND_BOTH && !aMoment.owner.userID.equals(mMyUid)) {
//                    momentIterator.remove();
//                }
//            }
//
//            // modify the comment name to 'me' when it's me.
//            String myUserId = mWeb.fGetMyUserIDFromLocal();
//            String myNameMe = getString(R.string.friends_moment_comment_me);
//            if (null != moments && !TextUtils.isEmpty(myUserId)) {
//                for (Moment moment : moments) {
//                    ArrayList<Review> reviews = moment.reviews;
//                    if (null != reviews) {
//                        for (Review review : reviews) {
//                            if (myUserId.equals(review.uid)) {
//                                review.nickname = myNameMe;
//                            }
//                            if (myUserId.equals(review.replyToUid)) {
//                                review.replyToNickname = myNameMe;
//                            }
//                        }
//                    }
//                }
//            }
//            if (null == moments || (moments.size() < PAGE_SIZE && LOADING_REASON_MORE == loadingReason)) {
//                needPull = true;
//            }
//        } else if (LOADING_REASON_REFRESH == loadingReason) {
//            needPull = true;
//        }
//
//        // then, display whatever we got
//        if (null != moments) {
//            /* 由于 HeaderViewListAdapter 有这样的副作用：
//                即使执行 ListView.setAdapter(null)，
//                ListView.getAdapter() 也不为 null，
//                所以需要借助 mAdapterRemoved。
//             */
//            if (mLvSingle.getAdapter() == null || mAdapterRemoved) {
//                momentsAll = moments;
//                setListAdapter(!mNoMoreToPull);
//                mAdapterRemoved = false;
//            } else {
//                if (LOADING_REASON_INIT == loadingReason
//                        || LOADING_REASON_REFRESH == loadingReason
//                        || LOADING_REASON_PULL_DONE_RESET == loadingReason) {
//                    mAdapter.clear();
//                }
//                for (Moment moment : moments) {
//                    mAdapter.add(moment);
//                }
//                mAdapter.notifyDataSetChanged();
//            }
//        }
//
//        // compute mNoMoreToPull if we have just pulled
//        if ((LOADING_REASON_PULL_DONE_APPEND == loadingReason)
//                && null != moments) {
//            mNoMoreToPull = moments.size() < PAGE_SIZE;
//            mAdapter.setShowLoadMoreAsLastItem(false);
//            mAdapter.notifyDataSetChanged();
//        }
//
//        // need pull ?
//        if (needPull && (LOADING_REASON_PULL_DONE_APPEND != loadingReason
//                && LOADING_REASON_PULL_DONE_RESET != loadingReason)) {
//            mLoadingTask = new AsyncTask<Void, Integer, Integer>() {
//                @Override
//                protected Integer doInBackground(Void... params) {
//                    try {
//                        if (_selectMode == SELECT_MODE_ALL) {
//                            return mMomentWeb.fGetMomentsOfAll(maxTimestampForThisPull, PAGE_SIZE, true);
//                        } else {
//                            return mMomentWeb.fGetMomentsOfBuddy(
//                                    mTargetUid, maxTimestampForThisPull, PAGE_SIZE, true);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        return ErrorCode.BAD_RESPONSE;
//                    }
//                }
//
//                @Override
//                protected void onPostExecute(Integer errno) {
//                    mIsLoadingMore = false;
//                    onRefreshComplete();
//                    mAdapter.notifyLoadingCompleted();
//                    if (ErrorCode.OK == errno) {
//                        // tip on new reviews
//                        Moment m = new Moment(null);
//                        GlobalValue.unreadMomentReviews = dbHelper.fetchNewReviews(m);
//                        updateNewReviewButton();
//
//                        if (LOADING_REASON_MORE == loadingReason) {
//                            updateTimestampToLoadFrom();
//                            loadMoments(LOADING_REASON_PULL_DONE_APPEND);
//                        } else {
//                            loadMoments(LOADING_REASON_PULL_DONE_RESET);
//                        }
//                    }
//                }
//
//                @Override
//                protected void onCancelled() {
//                    mIsLoadingMore = false;
//                }
//            };
//            mLoadingTask.execute((Void) null);
//        } else {
//            mIsLoadingMore = false;
//            onRefreshComplete();
//            mAdapter.notifyLoadingCompleted();
//        }
//    }

    /**
     * checkout moment owners' buddy info, fetch it from server if needed,
     * then notify list view.
     */
    private void fixMomentOwnerInfo() {
        HashSet<String> unknownUids = new HashSet<String>();
        for (Moment m : momentsAll) {
            if (m.owner != null && m.owner.userID != null && Utils.isNullOrEmpty(m.owner.nickName)) {
                unknownUids.add(m.owner.userID);
            }
        }

        // fetch detail info for each unknown buddy.
        if (!unknownUids.isEmpty()) {
            String[] us = new String[unknownUids.size()];
            int i = -1;
            for (String uid : unknownUids) {
                us[++i] = uid;
            }
            new AsyncTask<String, Intent, Void>() {
                @Override
                protected Void doInBackground(String... params) {
                    try {
                        for (String uid : params)
                            mWeb.fGetBuddyWithUID(uid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                @Override
                protected void onPostExecute(Void v) {
                    if (momentsAll == null)
                        return;
                    for (Moment m : momentsAll) {
                        if (m.owner != null && m.owner.userID != null
                                && Utils.isNullOrEmpty(m.owner.nickName)) {
                            m.owner = dbHelper.buddyWithUserID(m.owner.userID);
                        }
                    }
                    notifyAdapterDataChanged();
                }
            }.execute(us);
        }
    }

    private void setListAdapter(ArrayList<Moment> momentsFiltered,boolean hasMore) {
        mAdapter = new MomentAdapter(MomentActivity.this, MomentActivity.this,
                momentsFiltered,
                _selectMode == SELECT_MODE_SINGLE,isWithFavorite,
                mImageFetcher, MomentActivity.this,mTargetUid,mMsgBox);
        mAdapter.setShowLoadMoreAsLastItem(hasMore);
        mAdapter.setLoadDelegate(this);
        mLvSingle.setAdapter(mAdapter);
    }

    private void changeModeAll() {
        _selectMode = SELECT_MODE_ALL;

        //onResume will call this,as do not known whether user info is changed
//        setupHeaderContent();
        checkForNewReviews_async();
        resetState();
        if (null != mLoadingTask)
            mLoadingTask.cancel(true);
//        loadMoments(LOADING_REASON_INIT);
        initDatas();
        btnAll.setBackgroundResource(R.drawable.tab_button_left_a);
        btnMy.setBackgroundResource(R.drawable.tab_button_right);
    }

    private void changeModeSingle() {
        _selectMode = SELECT_MODE_SINGLE;

        //onResume will call this,as do not known whether user info is changed
//        setupHeaderContent();
        checkForNewReviews_async();
        resetState();
        if (null != mLoadingTask)
            mLoadingTask.cancel(true);
//        loadMoments(LOADING_REASON_INIT);

        //in single mode,view only one person's moment
        //if not seeing my own moment,hide edit button
        if(!TextUtils.isEmpty(mMyUid) && !TextUtils.isEmpty(mTargetUid)) {
            if(!mMyUid.equals(mTargetUid)) {
                btnTitleEdit.setVisibility(View.INVISIBLE);
            } else {
                btnTitleEdit.setVisibility(View.VISIBLE);
                //if mine moments,right button show more and when clicked show bottomPopupWindow
                btnTitleEdit.setImageResource(R.drawable.nav_more_selector);
            }
        }

        initDatas();
        btnAll.setBackgroundResource(R.drawable.tab_button_left);
        btnMy.setBackgroundResource(R.drawable.tab_button_right_a);
    }

    /**
     * 设置表头（相册封面区域）的内容，都在子线程中处理
     */
    private void setupHeaderContent() {
        setMyInfo();
        setTitleText();
        setAlbumCover();
    }

    /**
     * 设置个人名字，签名，头像等
     */
    private void setMyInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Buddy buddy = dbHelper.buddyWithUserID(mTargetUid);
                Message message = mHandler.obtainMessage();
                message.what = HANDLER_HEAD_CONTENT_MYINFO;
                message.obj = buddy;
                mHandler.sendMessage(message);
            }
        }).start();
    }

    /**
     * 设置标题
     */
    private void setTitleText() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(_selectMode == SELECT_MODE_SINGLE) {
                    Buddy buddy = dbHelper.buddyWithUserID(mTargetUid);
                    Message message = mHandler.obtainMessage();
                    message.what = HANDLER_TITLE_SINGLE;
                    message.obj = buddy;
                    mHandler.sendMessage(message);
                } else {
                    GroupChatRoom currentChatRoom = dbHelper.fetchGroupChatRoom(curShownDepartmentId);
                    Message message = mHandler.obtainMessage();
                    message.what = HANDLER_TITLE_ALL;
                    message.obj = currentChatRoom;
                    mHandler.sendMessage(message);
                }
            }
        }).start();
    }

    /**
     * 设置封面
     */
    private void setAlbumCover() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAlbumCover = dbHelper.getAlbumCover(mTargetUid);
                if (mAlbumCover != null) {
                    mHandler.sendEmptyMessage(HANDLER_ALBUM_COVER_LOCAL);
                }
            }
        }).start();
    }


    private void checkForNewReviews_async() {
        mLastCheckReviewTime = new java.util.Date().getTime() / 1000;
        // 顺便触发新评论的检查
        new CheckNewReviewsTask() {
            @Override
            protected void onPostExecute(Integer errno) {
//                updateNewReviewButton();
            }
        }.execute(this);
    }

    private void resetState() {
        mIsLoadingMore = false;
        mAdapterRemoved = true;
        mNoMoreToPull = false;
//        loadingRefreshFromServerSync=false;
//        mWillRefreshFromTimestamp = 0;
    }

    /**
     * checkout album cover for updates.
     * <p>每次检查album cover的时间戳，判断是否需要下载cover
     */
    private void checkoutAlbumCover() {
//        mLastCheckAlbumCoverTime = new java.util.Date().getTime() / 1000;
        new AsyncTask<Void, Integer, Integer>() {

            AlbumCover ac = new AlbumCover();

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mWeb.fGetAlbumCover(mTargetUid, ac);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (ErrorCode.OK == errno) {
                    dbHelper.storeAlbumCover(mTargetUid, ac);

                    if (mAlbumCover == null
                            || mAlbumCover.fileId == null
                            || !mAlbumCover.fileId.equals(ac.fileId)
                            || mAlbumCover.timestamp != ac.timestamp) {
                        mAlbumCover = ac;
                        // 改变SP中保存的背景图片
                        saveAlbumCover2SP(mAlbumCover);
                        displayAlbumCover();
                    }
                }
            }
        }.execute((Void)null);
    }

    /**
     * 保存albumCover到SP中
     * @param albumCover 
     */
    private void saveAlbumCover2SP(AlbumCover albumCover) {
        ArrayList<Account> prefAccounts = mPrefUtil.getAccountList();
        Account account = null;
        for (Iterator<Account> iterator = prefAccounts.iterator(); iterator.hasNext();) {
            account = iterator.next();
            if (mTargetUid.equals(account.uid)) {
                if (albumCover.timestamp == -1) {
                    account.albumCoverTimeStamp = -1;
                    account.albumCoverFileId = "";
                    account.albumCoverExt = "";
                } else {
                    account.albumCoverTimeStamp = albumCover.timestamp;
                    account.albumCoverFileId = null == albumCover.fileId ? "" : albumCover.fileId;
                    account.albumCoverExt = null == albumCover.ext ? "" : albumCover.ext;
                }
                break;
            }
        }
        mPrefUtil.setAccountList(prefAccounts);
    }

    private void displayAlbumCover() {
        if (mAlbumCover != null && mAlbumCover.timestamp != -1 && mAlbumCover.fileId != null) {
            PhotoDisplayHelper.displayPhoto(this, mHeaderBgImageView,
                    R.drawable.moment_default_album,
                    mAlbumCover.fileId, mAlbumCover.ext, GlobalSetting.S3_MOMENT_FILE_DIR, null);
        } else {
            mHeaderBgImageView.setImageResource(R.drawable.moment_default_album);
        }
    }

    private void setMode() {
        if (_selectMode == SELECT_MODE_ALL) {
            changeModeAll();
        } else {
            changeModeSingle();
        }


    }

    private boolean listContentEmptyWithException=false;

    private void initView() {
        btnTitleLeft = (ImageButton) findViewById(R.id.title_left);
        btnAll = findViewById(R.id.btn_all);
        btnMy = findViewById(R.id.btn_me);

        // disabled as per UI design
        btnTitleLeft.setVisibility(View.GONE);

        if(SELECT_MODE_ALL == _selectMode && !isWithFavorite) {
//            btnTitleLeft.setVisibility((_selectMode == SELECT_MODE_ALL) ? View.GONE : View.VISIBLE);
            btnTitleLeft.setImageResource(R.drawable.nav_group_list_selector);
        }

        btnTitleEdit = (ImageButton) findViewById(R.id.title_edit);
        if(isWithFavorite) {
            btnTitleEdit.setVisibility(View.INVISIBLE);
        } else {
            btnTitleEdit.setVisibility(View.VISIBLE);
        }
        tvTitle = (TextView) findViewById(R.id.tv_title);
        mPtrSingle = (PullToRefreshListView) findViewById(R.id.list_single);
        // 两种 select mode 需要不同的 header view，这两个 header view 里有一些相同 ID 的 view，
        // 为了简化，只创建一个 header view 实例。由于目前不允许在两种模式之间切换，这样做是可以的。
        mAlbumCoverView = _selectMode == SELECT_MODE_SINGLE ?
                getLayoutInflater().inflate(R.layout.piece_moment_album_cover, null) :
                getLayoutInflater().inflate(R.layout.piece_moment_album_cover_for_all, null);
        mAlbumCoverView.setId(HEADER_VIEW_ID);
        txtName = (TextView) mAlbumCoverView.findViewById(R.id.txt_name);
        txtSignature = (TextView) mAlbumCoverView.findViewById(R.id.txt_signature);
        imgThumbnail = (ImageView) mAlbumCoverView.findViewById(R.id.img_thumbnail);
//        btnGotoNewReviews = (Button) mAlbumCoverView.findViewById(R.id.btn_new_reviews);
        mLvSingle = mPtrSingle.getRefreshableView();
        mLvSingle.setHeaderDividersEnabled(false);
        mHeaderBgImageView = (ImageView) mAlbumCoverView.findViewById(R.id.imgAlbumBg);

        if (_selectMode == SELECT_MODE_SINGLE) {
            mLvSingle.addHeaderView(mAlbumCoverView);
        } else {
            mLvSingle.addHeaderView(mAlbumCoverView);
        }

        mAlbumCoverView.findViewById(R.id.moment_header_non_op_layout).setVisibility(View.VISIBLE);
//        if(_selectMode == SELECT_MODE_SINGLE || isWithFavorite) {
//            mAlbumCoverView.findViewById(R.id.moment_header_non_op_layout).setVisibility(View.VISIBLE);
//        } else {
//            mAlbumCoverView.findViewById(R.id.moment_header_non_op_layout).setVisibility(View.GONE);
//        }

        initTagViews();

        getPTR().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
//                if (AbsListView.OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
//                    lastItemIndex=mLvSingle.getFirstVisiblePosition();
//                    View v = mLvSingle.getChildAt(0);
//                    topOffset = (v == null) ? 0 : v.getTop();
//                    Log.i("first visible position "+lastItemIndex+","+topOffset);
//                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int visibleCount, int totalCount) {
                if(0 == visibleCount && 0 == totalCount) {
                    Log.w("both 0 detected");
                    listContentEmptyWithException=true;
                } else {
                    if (listContentEmptyWithException) {
                        listContentEmptyWithException=false;
                        mLvSingle.setSelectionFromTop(lastItemIndex[curSelectedTagIdx], topOffset[curSelectedTagIdx]);
                    } else {
                        lastItemIndex[curSelectedTagIdx]=mLvSingle.getFirstVisiblePosition();
                        View v = mLvSingle.getChildAt(0);
                        topOffset[curSelectedTagIdx] = (v == null) ? 0 : v.getTop();
                        Log.i("first visible position " + lastItemIndex[curSelectedTagIdx] + "," + topOffset[curSelectedTagIdx]);

                        updateHeaderBtnMode();
                    }
                }

            }
        });

        setupPullToRefresh();

        tvTitle.setOnClickListener(this);
        findViewById(R.id.tv_title_bottom_drawable).setOnClickListener(this);

        btnTitleLeft.setOnClickListener(this);
        btnTitleEdit.setOnClickListener(this);
        btnAll.setOnClickListener(this);
        btnMy.setOnClickListener(this);
//        btnGotoNewReviews.setOnClickListener(this);
        mAlbumCoverView.findViewById(R.id.frame).setOnClickListener(this);

        mAlbumCoverView.setOnClickListener(this);
    }

    private final static int MSG_ID_UPDATE_HEADER_BUTTONS=1;

    private final static int UPDATE_HEADER_BUTTONS_DELAY=50;
    private Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_UPDATE_HEADER_BUTTONS:
                    doHeaderBtnMode();
                    break;
            }
        }
    };

    /**
     * UI加载handler
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLER_HEAD_CONTENT_MYINFO:
                Object myInfoObj = msg.obj;
                if (null != myInfoObj) {
                    Buddy buddy = (Buddy) myInfoObj;
                    txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);

                    View headerBottomBg=findViewById(R.id.box_info_bg);
                    if (buddy.status == null || buddy.status.equals("")) {
                        txtSignature.setText("");
                        headerBottomBg.setVisibility(View.GONE);
                    } else {
                        txtSignature.setText(buddy.status);
                        headerBottomBg.setVisibility(View.VISIBLE);
                    }
                    PhotoDisplayHelper.displayPhoto(MomentActivity.this,
                            imgThumbnail, R.drawable.default_avatar_90, buddy, true);
                }
                break;
            case HANDLER_TITLE_SINGLE:
                Object buddyObj = msg.obj;
                if (null != buddyObj) {
                    Buddy singleBuddy = (Buddy) buddyObj;
                    tvTitle.setText(TextUtils.isEmpty(singleBuddy.alias) ? singleBuddy.nickName : singleBuddy.alias);
                } else {
                    tvTitle.setText(mTargetUid);
                }
                findViewById(R.id.tv_title_bottom_drawable).setVisibility(View.GONE);
                if(isWithFavorite) {
                    findViewById(R.id.tv_title_bottom_drawable).setVisibility(View.GONE);
                    tvTitle.setText(R.string.my_store);
                }
                break;
            case HANDLER_TITLE_ALL:
                Object groupObj = msg.obj;
                findViewById(R.id.tv_title_bottom_drawable).setVisibility(View.VISIBLE);
                if(null != groupObj) {
                    GroupChatRoom currentChatRoom = (GroupChatRoom) groupObj;
                    if (!currentChatRoom.isEditable && TextUtils.isEmpty(currentChatRoom.parentGroupId)) {
                        tvTitle.setText(R.string.contactsforbiz_root_group_name_display);
                    } else {
                        tvTitle.setText(currentChatRoom.groupNameOriginal);
                    }
                } else {
                    tvTitle.setText(R.string.contactsforbiz_root_group_name_display);
                }
                if(isWithFavorite) {
                    findViewById(R.id.tv_title_bottom_drawable).setVisibility(View.GONE);
                    tvTitle.setText(R.string.my_store);
                }
                break;
            case HANDLER_ALBUM_COVER_LOCAL:
                displayAlbumCover();
                break;
            case HANDLER_TRIGGER_LOAD_MOMENTS:
                triggerReloadMoments();
                break;

            default:
                break;
            }
        };
    };

    private void updateHeaderBtnMode() {
        if(isInitScrollOk) {
            updateHandler.removeMessages(MSG_ID_UPDATE_HEADER_BUTTONS);
        }

        if(isHeaderBtnModeWillChange()) {
            updateHandler.sendEmptyMessage(MSG_ID_UPDATE_HEADER_BUTTONS);
        } else {
            updateHandler.sendEmptyMessageDelayed(MSG_ID_UPDATE_HEADER_BUTTONS,UPDATE_HEADER_BUTTONS_DELAY);
        }
    }

    private boolean isHeaderBtnModeWillChange() {
//        View opButtonLayout=findViewById(R.id.moment_header_bottom_op_line_whole_layout);
//        View titleBar=findViewById(R.id.title_bar);

//        Rect titleRect = new Rect();
//        titleBar.getGlobalVisibleRect(titleRect);
////        Log.i(titleRect.toString());
//
//        Rect btnRect = new Rect();
//        opButtonLayout.getGlobalVisibleRect(btnRect);
//        Log.i(btnRect.toString());

        boolean ret=false;
        if(isInitScrollOk) {
            if(isOpButtonsCovered()) {
                if(HEADER_OP_BUTTONS_MODE_NORMAL == headerOpButtonsMode) {
                    ret=true;
                }
            } else {
                if(0 == mLvSingle.getFirstVisiblePosition() || 1 == mLvSingle.getFirstVisiblePosition() ||
                        HEADER_OP_BUTTONS_MODE_STAY_TOP == headerOpButtonsMode) {
                    ret=true;
                }
            }
        }

        return ret;
    }

    private int getScrollY() {
        View firstView = mLvSingle.getChildAt(0); //this is the first visible row
        int scrollY = (null == firstView) ? 0 : -firstView.getTop();

        Rect nonBtnRect = new Rect();
        mAlbumCoverView.findViewById(R.id.moment_header_non_op_layout).getGlobalVisibleRect(nonBtnRect);
//        Log.e("album non op btns: "+nonBtnRect.toString());

        int firstVisiblePos=mLvSingle.getFirstVisiblePosition();
        int headerNonOpTotalHeight=getHeaderNonOpBtnAvaliableHeight();
        Log.i("height0="+nonBtnRect.height()+",height1="+headerNonOpTotalHeight);
        if(nonBtnRect.height() < headerNonOpTotalHeight) {
            firstVisiblePos=0;
        }
        for (int i = 0; i < firstVisiblePos; ++i) {
            View childView=mLvSingle.getChildAt(i);
            if(null != childView) {
                scrollY += childView.getMeasuredHeight();
            }
        }
        return scrollY;
    }

    private int getHeaderNonOpBtnAvaliableHeight() {
        View headerNonOpLayout=mAlbumCoverView.findViewById(R.id.moment_header_non_op_layout);
        int headerNonOpLayoutHeight=headerNonOpLayout.getMeasuredHeight();

        return headerNonOpLayoutHeight-headerHideHeight;
    }

    private boolean isOpButtonsCovered() {
        int headerNonOpLayoutHeight=getHeaderNonOpBtnAvaliableHeight();

        Log.i("headerNonOpHeight="+headerNonOpLayoutHeight+",scrollY="+getScrollY());
        if(headerNonOpLayoutHeight > 0 && getScrollY()>headerNonOpLayoutHeight) {
            return true;
        }
        return false;
//        View opButtonLayout=findViewById(R.id.moment_header_bottom_op_line_whole_layout);
//        View titleBar=findViewById(R.id.title_bar);
//
//        Rect titleRect = new Rect();
//        titleBar.getGlobalVisibleRect(titleRect);
////        Log.i("title:"+titleRect.toString());
//
//        Rect btnRect = new Rect();
//        opButtonLayout.getGlobalVisibleRect(btnRect);
////        Log.i("btn:"+btnRect.toString());
//
//        return isInitScrollOk &&
//                (btnRect.top < titleRect.bottom ||
//                btnRect.height()<getResources().getDimension(R.dimen.moment_header_bottom_op_line_height));
    }

    private void doHeaderBtnMode() {
        View opButtonLayout=findViewById(R.id.moment_header_bottom_op_line_whole_layout);
        View titleBar=findViewById(R.id.title_bar);

        Rect titleRect = new Rect();
        titleBar.getGlobalVisibleRect(titleRect);
//        Log.i("title:"+titleRect.toString());

        Rect btnRect = new Rect();
        opButtonLayout.getGlobalVisibleRect(btnRect);
//        Log.i("btn:"+btnRect.toString());

        Log.i("is op button covered: " + isOpButtonsCovered());
        //
        if(!isInitScrollOk) {
            if(btnRect.top >= titleRect.bottom) {
                isInitScrollOk=true;
                Log.i("init scroll ok");
            }
        } else {
            LinearLayout stayTopLayout=(LinearLayout)findViewById(R.id.moment_scroll_top_layout_op_line);
            LinearLayout normalLayout=(LinearLayout)mAlbumCoverView.findViewById(R.id.moment_header_bottom_op_line_whole_layout);

            if(!isOpButtonsCovered() || isPulling) {
                headerOpButtonsMode=HEADER_OP_BUTTONS_MODE_NORMAL;
                Log.i("op btn mode change to normal");

                View opBtnsView=stayTopLayout.findViewById(R.id.hsv_tag_content);
                if(null != opBtnsView) {
                    stayTopLayout.removeView(opBtnsView);
                    normalLayout.addView(opBtnsView);
                }
            } else {
                headerOpButtonsMode=HEADER_OP_BUTTONS_MODE_STAY_TOP;
                Log.i("op btn mode change to stay top");

                View opBtnsView=normalLayout.findViewById(R.id.hsv_tag_content);
                if(null != opBtnsView) {
                    normalLayout.removeView(opBtnsView);
                    stayTopLayout.addView(opBtnsView);
                }
            }
        }
    }

    private void initTagViews() {
        tvAll=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_all);
        tvTimeLine=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_timeline);
        tvQA=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_qa);
        tvQuestionnaire=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_questionnaire);
        tvReport=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_report);
        tvAttendance=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_attendance);
        tvTodo=(TextView)mAlbumCoverView.findViewById(R.id.tv_bottom_selector_todo);

        ivAllIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_all_indicator);
        ivTimeLineIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_timeline_indicator);
        ivQAIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_qa_indicator);
        ivQuestionnaireIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_questionnaire_indicator);
        ivReportIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_report_indicator);
        ivAttendanceIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_attendance_indicator);
        ivTodoIndicator=(ImageView)mAlbumCoverView.findViewById(R.id.iv_bottom_selector_todo_indicator);

//        mAlbumCoverView.findViewById(R.id.hsv_tag_content).setOnClickListener(this);
        tvAll.setOnClickListener(this);
        tvTimeLine.setOnClickListener(this);
        tvQA.setOnClickListener(this);
        tvQuestionnaire.setOnClickListener(this);
        tvReport.setOnClickListener(this);
        tvAttendance.setOnClickListener(this);
        tvTodo.setOnClickListener(this);

        ivAllIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvAll.performClick();
            }
        });
        ivTimeLineIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvTimeLine.performClick();
            }
        });
        ivQAIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvQA.performClick();
            }
        });
        ivQuestionnaireIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvQuestionnaire.performClick();
            }
        });
        ivReportIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvReport.performClick();
            }
        });
        ivAttendanceIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvAttendance.performClick();
            }
        });
        ivTodoIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvTodo.performClick();
            }
        });
        findViewById(R.id.layout_department).setOnClickListener(this);
        findViewById(R.id.title_bar).setOnClickListener(this);

        performContentSelector(curSelectedTagIdx);
    }

    private void showMomentOpBottomPopup() {
        final BottomButtonBoard bottomBoard= new BottomButtonBoard(this,findViewById(R.id.title_edit));
        bottomBoard.add(getString(R.string.review_list),
                BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomBoard.dismiss();

                Intent intent = new Intent(MomentActivity.this,AllMyReviewActivity.class);
                startActivity(intent);
            }
        });
        bottomBoard.add(getString(R.string.create_new_moment),
                BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomBoard.dismiss();
                showCreateOptionWithTag();
            }
        });
        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }

    private void triggerCreateMoment(int tagType) {
        Intent intent;
        long maxTimeStamp=0;
        if(null != momentsAll && momentsAll.size() > 0) {
            maxTimeStamp=momentsAll.get(0).timestamp;
        }

        switch(tagType) {
            case TAG_NOTICE_IDX:
            case TAG_QA_IDX:
            case TAG_STUDY_IDX:
            case TAG_SURVEY_IDX:
                //normal moment with tag
                intent = new Intent(MomentActivity.this,CreateNormalMomentWithTagActivity.class);
                intent.putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP,maxTimeStamp);
                intent.putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_TAG_ID,tagType);
                startActivityForResult(intent, REQ_CREATE_MOMENT);
                break;
            case TAG_LIFE_IDX:
            case TAG_VIDEO_IDX:
            default:
                intent = new Intent(MomentActivity.this,
                        CreateMomentActivity.class);

                intent.putExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP,maxTimeStamp);
                startActivityForResult(intent, REQ_CREATE_MOMENT);
                break;
        }

    }

    public void showCreateOptionWithTag() {
        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView= lf.inflate(R.layout.create_moment_option_tag, null);

        final PopupWindow tagOptionPopWindow = Utils.getFixedPopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        View createTagTimeline=contentView.findViewById(R.id.create_tag_notice);
        View createTagQA=contentView.findViewById(R.id.create_tag_qa);
        View createTagQuestion=contentView.findViewById(R.id.create_tag_life);
        View createTagReport=contentView.findViewById(R.id.create_tag_study);
        View createTagAttendance=contentView.findViewById(R.id.create_tag_survey);
        View createTagTodo=contentView.findViewById(R.id.create_tag_video);

        contentView.findViewById(R.id.root_layout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
            }
        });
        createTagTimeline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_NOTICE_IDX);
            }
        });
        createTagQA.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_QA_IDX);
            }
        });
        createTagQuestion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_SURVEY_IDX);
            }
        });
        createTagReport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_STUDY_IDX);
            }
        });
        createTagAttendance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_LIFE_IDX);
            }
        });
        createTagTodo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tagOptionPopWindow.dismiss();
                triggerCreateMoment(TAG_VIDEO_IDX);
            }
        });

        tagOptionPopWindow.setFocusable(true);
        tagOptionPopWindow.setTouchable(true);
        tagOptionPopWindow.setOutsideTouchable(true);
        tagOptionPopWindow.setBackgroundDrawable(new BitmapDrawable());

//        tagOptionPopWindow.setWidth((int)getResources().getDimension(R.dimen.create_moment_option_tags_width));
        tagOptionPopWindow.showAtLocation(btnTitleEdit, Gravity.CENTER, 0, 0);
        tagOptionPopWindow.update();
    }

    private void doMomentOp() {
        if(SELECT_MODE_SINGLE == _selectMode) {
            showMomentOpBottomPopup();
        } else {
            showCreateOptionWithTag();
        }
    }

    private void setAOpBottomWidth(TextView tv,ImageView iv) {
        ViewGroup.LayoutParams layoutParams = iv.getLayoutParams();

        layoutParams.width=tv.getWidth();
        iv.setLayoutParams(layoutParams);


    }

    private void triggersetOpBottomWidth(final TextView tv,final ImageView iv) {
        if(tv.getWidth() != 0) {
            setAOpBottomWidth(tv,iv);
        } else {
            final TimeElapseReportRunnable setWidthRunnable=new TimeElapseReportRunnable();
            setWidthRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
                @Override
                public void reportElapse(final long elapsed) {
                    MomentActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAOpBottomWidth(tv,iv);
                            if(tv.getWidth() != 0) {
                                setWidthRunnable.stop();
                            }
                        }
                    });
                }
            });
            new Thread(setWidthRunnable).start();
        }
    }
//
//    private boolean widthSet=false;
//    private void setTagBottomLineWidth() {
//        if(!widthSet) {
////            widthSet=true;
//
//            Log.i("tvAll width "+tvAll.getWidth());
//            Log.i("tvTimeLine width "+tvTimeLine.getWidth());
//            Log.i("tvQA width "+tvQA.getWidth());
//            Log.i("tvQuestionnaire width "+tvQuestionnaire.getWidth());
//            Log.i("tvReport width "+tvReport.getWidth());
//            Log.i("tvAttendance width "+tvAttendance.getWidth());
//            Log.i("tvTodo width "+tvTodo.getWidth());
//
//
//            Log.i("ivAllIndicator width "+ivAllIndicator.getWidth());
//            setAOpBottomWidth(tvAll,ivAllIndicator);
//
//            Log.i("ivTimeLineIndicator width "+ivTimeLineIndicator.getWidth());
//            setAOpBottomWidth(tvTimeLine,ivTimeLineIndicator);
//
//            Log.i("ivQAIndicator width "+ivQAIndicator.getWidth());
//            setAOpBottomWidth(tvQA,ivQAIndicator);
//
//            Log.i("ivQuestionnaireIndicator width "+ivQuestionnaireIndicator.getWidth());
//            setAOpBottomWidth(tvQuestionnaire,ivQuestionnaireIndicator);
//
//            Log.i("ivReportIndicator width "+ivReportIndicator.getWidth());
//            setAOpBottomWidth(tvReport,ivReportIndicator);
//
//            Log.i("ivAttendanceIndicator width "+ivAttendanceIndicator.getWidth());
//            setAOpBottomWidth(tvAttendance,ivAttendanceIndicator);
//
//            Log.i("ivTodoIndicator width "+ivTodoIndicator.getWidth());
//            setAOpBottomWidth(tvTodo,ivTodoIndicator);
//        }
//    }

    private void performContentSelector(int id) {
        float defTextSize=getResources().getDimension(R.dimen.moment_header_bottom_line_txt_size);

        tvAll.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvAll.setTextColor(getResources().getColor(R.color.gray));

        tvTimeLine.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvTimeLine.setTextColor(getResources().getColor(R.color.gray));

        tvQA.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvQA.setTextColor(getResources().getColor(R.color.gray));

        tvQuestionnaire.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvQuestionnaire.setTextColor(getResources().getColor(R.color.gray));

        tvReport.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvReport.setTextColor(getResources().getColor(R.color.gray));

        tvAttendance.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvAttendance.setTextColor(getResources().getColor(R.color.gray));

        tvTodo.setTextSize(TypedValue.COMPLEX_UNIT_PX,defTextSize);
        tvTodo.setTextColor(getResources().getColor(R.color.gray));

        ivAllIndicator.setVisibility(View.INVISIBLE);
        ivTimeLineIndicator.setVisibility(View.INVISIBLE);
        ivQAIndicator.setVisibility(View.INVISIBLE);
        ivQuestionnaireIndicator.setVisibility(View.INVISIBLE);
        ivReportIndicator.setVisibility(View.INVISIBLE);
        ivAttendanceIndicator.setVisibility(View.INVISIBLE);
        ivTodoIndicator.setVisibility(View.INVISIBLE);

        TextView tvSelected=null;
        ImageView ivSelected=null;
        switch(id) {
            case TAG_ALL_IDX:
                tvSelected=tvAll;
                ivSelected=ivAllIndicator;
                break;
            case TAG_NOTICE_IDX:
                tvSelected=tvTimeLine;
                ivSelected=ivTimeLineIndicator;
                break;
            case TAG_QA_IDX:
                tvSelected=tvQA;
                ivSelected=ivQAIndicator;
                break;
            case TAG_SURVEY_IDX:
                tvSelected=tvQuestionnaire;
                ivSelected=ivQuestionnaireIndicator;
                break;
            case TAG_STUDY_IDX:
                tvSelected=tvReport;
                ivSelected=ivReportIndicator;
                break;
            case TAG_LIFE_IDX:
                tvSelected=tvAttendance;
                ivSelected=ivAttendanceIndicator;
                break;
            case TAG_VIDEO_IDX:
                tvSelected=tvTodo;
                ivSelected=ivTodoIndicator;
                break;
            default:
                Log.e("unknown id for moment header op line "+id);
                break;
        }

        if(null != tvSelected) {
            Log.i("select moment line "+id);
            triggersetOpBottomWidth(tvSelected,ivSelected);
            //select handle
//            tvSelected.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimension(R.dimen.moment_header_bottom_line_txt_size_clicked));
            tvSelected.setTextColor(getResources().getColor(R.color.black));
        }

        if(null != ivSelected) {
            ivSelected.setVisibility(View.VISIBLE);
        }

        curSelectedTagIdx=id;
        setContentMomentWithFilter(true);
    }

    private void getContactsListFromLocal(GroupChatRoom mCurrentChatRoom) {
        ArrayList<GroupMember> members = new ArrayList<GroupMember>();
        // 获取当前群组下的所有成员，包括子部门
        // 此时返回的members可能存在重复的成员（同时属于多个部门）
//        dbHelper.fetchGroupMembersIteration(mCurrentChatRoom, members);
        members = dbHelper.fetchGroupMembers(mCurrentChatRoom.groupID);

        // 过滤重复的成员
        buddyListInCurDepartment.clear();
        buddyListInCurDepartment.addAll(members);

//        ArrayList<String> addedIds = new ArrayList<String>();
//        for (GroupMember groupMember : members) {
//            if (!addedIds.contains(groupMember.userID)) {
//                buddyListInCurDepartment.add(groupMember);
//                addedIds.add(groupMember.userID);
//            }
//        }
    }

    private ContactGroupIterationAdapter.GroupNameClickedListener mSelectedGroupListener
            = new ContactGroupIterationAdapter.GroupNameClickedListener() {

        @Override
        public void onGroupNameClicked(final GroupChatRoom chatRoom) {
            showDepartment();
            curShownDepartmentId=chatRoom.groupID;
            setTitleText();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    getContactsListFromLocal(chatRoom);
                    return null;
                }

                protected void onPostExecute(Void result) {
                    setContentMomentWithFilter(true);
                };
            }.execute((Void)null);
        }

        @Override
        public void onSendMsgClicked() {
        }
    };
    private ContactGroupIterationAdapter mGroupIterationAdapter;

    private void doShowDepartment() {
        ListView lvDepartment=(ListView) findViewById(R.id.lv_department);
//        if (null == mGroupIterationAdapter) {
            mGroupIterationAdapter = new ContactGroupIterationAdapter(this, rootGroups, mSelectedGroupListener);
            mGroupIterationAdapter.setShowGroupNameOnly(true,false,null);
            lvDepartment.setAdapter(mGroupIterationAdapter);
            lvDepartment.setVisibility(View.VISIBLE);
//        }
        mGroupIterationAdapter.notifyDataSetChanged();

        View departmentLayout=findViewById(R.id.layout_department);
        if(View.GONE == departmentLayout.getVisibility()) {
            departmentLayout.setVisibility(View.VISIBLE);
            if(!isWithFavorite) {
                btnTitleLeft.setImageResource(R.drawable.nav_up_selector);
            }
        } else {
            departmentLayout.setVisibility(View.GONE);
            if(!isWithFavorite) {
                btnTitleLeft.setImageResource(R.drawable.nav_group_list_selector);
            }
        }
    }

    private void showDepartment() {
        if(SELECT_MODE_ALL != _selectMode || isWithFavorite) {
            return;
        }
//        if(null == departmentList) {
//            mMsgBox.showWait();
//        } else {
//            doShowDepartment();
//        }
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                departmentList=dbHelper.fetchNonTempGroupChatRooms(true);
                ArrayList<GroupChatRoom> favoriteGroups=dbHelper.fetchFavoriteGroupChatRooms();
                GroupChatRoom rootFavoriteChatRoom =
                        ContactsActivityForBiz.treeFavoriteGroups(getString(R.string.contactsforbiz_root_favorite_group_name),favoriteGroups);

                rootGroups = new ArrayList<GroupChatRoom>();
                rootGroups.add(rootFavoriteChatRoom);
                rootGroups.addAll(ContactsActivityForBiz.treeAllGroupRooms(MomentActivity.this, departmentList));
                return null;
            }
            
            @Override
            protected void onPostExecute(Integer newReviewsCount) {
//                mMsgBox.dismissWait();
                doShowDepartment();
            }
        }.execute((Void)null);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.layout_department:
            case R.id.title_bar:
            case R.id.tv_title_bottom_drawable:
            case R.id.tv_title:
                showDepartment();
                break;
            case R.id.btn_all:
                changeModeAll();
                break;
            case R.id.btn_me:
                changeModeSingle();
                break;
//            case R.id.hsv_tag_content:
//                break;
            case R.id.tv_bottom_selector_all:
                performContentSelector(TAG_ALL_IDX);
                break;
            case R.id.tv_bottom_selector_timeline:
                performContentSelector(TAG_NOTICE_IDX);
                break;
            case R.id.tv_bottom_selector_qa:
                performContentSelector(TAG_QA_IDX);
                break;
            case R.id.tv_bottom_selector_questionnaire:
                performContentSelector(TAG_SURVEY_IDX);
                break;
            case R.id.tv_bottom_selector_report:
                performContentSelector(TAG_STUDY_IDX);
                break;
            case R.id.tv_bottom_selector_attendance:
                performContentSelector(TAG_LIFE_IDX);
                break;
            case R.id.tv_bottom_selector_todo:
                performContentSelector(TAG_VIDEO_IDX);
                break;
            case R.id.title_left:
                if(SELECT_MODE_ALL == _selectMode && !isWithFavorite) {
                    tvTitle.performClick();
                } else {
                    finish();
                }
                break;
            case R.id.title_edit:
                doMomentOp();
                break;
            case R.id.frame:
                // 跳转规则：
                // 所有人的动态 => 单人动态
                // 单人动态 => 个人资料
                if (_selectMode == SELECT_MODE_SINGLE) {
                    if (isMe()) {
                        Intent myInfoIntent = new Intent(this, MyInfoActivity.class);
                        startActivityForResult((myInfoIntent), REQ_SHOW_MYPROFILE);
                    } else {
                        ContactInfoActivity.launch(this, mTargetUid, ContactInfoActivity.BUDDY_TYPE_IS_FRIEND);
                    }
                } else {
                    MomentActivity.launch(this, mTargetUid);
                }
                break;
            case HEADER_VIEW_ID:
                // 更改我的相册封面
                if (isMe()) {
                    if (mProgressUploadingAlbumcover != null
                            && mProgressUploadingAlbumcover.getVisibility() == View.VISIBLE) {
                        mMsgBox.toast(getString(R.string.moments_uploading_in_progress));
                    } else {
//                        if (mMediaInput == null) {
//                            mMediaInput = new MediaInputHelper();
//                        }
//                        mMediaInput.inputImage(this, REQ_INPUT_ALBUMCOVER,
//                                getString(R.string.moments_change_album_cover));
                        showInputPhotoBoard();
                    }
                }
                break;
//            case R.id.btn_new_reviews:
//                Intent newReviewsIntent = new Intent(this, NewReviewsActivity.class);
//                newReviewsIntent.putExtra(NewReviewsActivity.EXTRA_KEY_HOSTTYPE,
//                        NewReviewsActivity.EXTRA_VALUE_HOSTTYPE_MOMENT);
//                startActivityForResult(newReviewsIntent,ACTIVITY_REQ_ID_NEW_REVIEWS);
//                break;
            default:
                break;
        }
    }

    private void showInputPhotoBoard() {
        handleItemClick();
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        if (mMediaInput == null) {
            mMediaInput = new MediaInputHelper(this);
        }
        bottomBoard.add(getString(R.string.image_change_cover), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        AlbumCoverChangeActivity.launchActivity(MomentActivity.this, mMediaInput, MomentActivity.this, mBeginUploadAlbumCover);
                    }
                });
        if (mAlbumCover.timestamp != -1 && !TextUtils.isEmpty(mAlbumCover.fileId)) {
            bottomBoard.add(getString(R.string.image_remove_cover), BottomButtonBoard.BUTTON_BLUE,
                    new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomBoard.dismiss();
                    mInputedPhotoPathForAlbumcover = null;
                    removeCoverImage();
                }
            });
        }
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isWithFavorite=savedInstanceState.getBoolean(EXTRA_WITH_FAVORITE);
        } else {
            isWithFavorite=getIntent().getBooleanExtra(EXTRA_WITH_FAVORITE,false);
        }
        if(isWithFavorite) {
            setContentView(R.layout.activity_moment_favorite);
        } else {
            setContentView(R.layout.activity_moment);
        }

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);

        onMomentReviewDeleteListener=new OnMomentReviewDeleteListener() {
            @Override
            public void onMomentDelete(String momentId, Review review) {
                if (null != instance) {
                    instance.deleteAReview(momentId, review);
                }
            }
        };
        mMsgBox = new MessageBox(this);
        mPrefUtil = PrefUtil.getInstance(this);
        dbHelper = new Database(this);
        mWeb = WowTalkWebServerIF.getInstance(MomentActivity.this);
        mMomentWeb = WowMomentWebServerIF.getInstance(MomentActivity.this);
        mMyUid = mPrefUtil.getUid();

        if (savedInstanceState != null) {
            mTargetUid = savedInstanceState.getString(EXTRA_UID);
            _selectMode = savedInstanceState.getInt("_selectMode");
            mInputMgr = savedInstanceState.getParcelable("mInputMgr");
            mMediaInput = savedInstanceState.getParcelable("mMediaInput");
            mInputedPhotoPathForAlbumcover = savedInstanceState.getString(
                    "mInputedPhotoPathForAlbumcover");

            if (mInputMgr != null) {
                mInputMgr.init(this,
                        (ViewGroup)findViewById(R.id.input_board_holder),
                        this, this);
            }

            curShownDepartmentId=savedInstanceState.getString(EXTRA_SHOWN_DEPARTMENT);
            curSelectedTagIdx=savedInstanceState.getInt(EXTRA_SELECTED_TAG_IDX);
            headerOpButtonsMode=savedInstanceState.getInt(EXTRA_HEADER_BTNS_MODE);
        } else {
            mTargetUid = getIntent().getStringExtra(EXTRA_UID);

            if (Utils.isNullOrEmpty(mTargetUid)) {
                _selectMode = SELECT_MODE_ALL;
                mTargetUid = mMyUid;
            } else {
                _selectMode = SELECT_MODE_SINGLE;
            }
        }

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageFetcher = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        mImageFetcher.setLoadingImage(R.drawable.feed_default_pic);
        mImageFetcher.addImageCache(cacheParams);

//        mWillRefreshFromTimestamp = 0;
        initView();
        setMode();

        //to start up activity create,first does not show moments,load moments in background
        loadDummyMoments();
        initDatas();

        updateNewReviewButton();

        Database.addDBTableChangeListener(Database.DUMMY_TBL_SWITCH_ACCOUNT,mAccountSwitchObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FINISH_LOAD_MEMBERS, mFinishLoadMembersObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT,momentObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP,groupObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP_MEMBER,groupObserver);
        Database.addDBTableChangeListener(Database.TBL_GROUP,groupChatRoomObserver);
        Database.addDBTableChangeListener(Database.DUMMY_TBL_FAVORITE_GROUP, groupChatRoomObserver);
    }

    /**
     * 数据初始化，在子线程中处理，否则阻塞UI线程
     */
    private void initDatas() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getGroupsAndMembers();
                setupMyGroupInfo();
                mHandler.sendEmptyMessage(HANDLER_TRIGGER_LOAD_MOMENTS);
            }
        }).start();
    }

//    private final static int MSG_ID_RELOAD_MOMENT=1;
//    private final static long RELOAD_MOMENT_INTERVAL=300;
    private long loadingMomentId=0;
//    private Handler msgHandler= new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch(msg.what) {
//                case MSG_ID_RELOAD_MOMENT:
//                    loadMomentsFromLocal_async(++loadingMomentId);
//                    break;
//            }
//        }
//    };

    private void triggerReloadMoments() {
        loadMomentsFromLocal_async(++loadingMomentId);
//        msgHandler.removeMessages(MSG_ID_RELOAD_MOMENT);
//        msgHandler.sendEmptyMessageDelayed(MSG_ID_RELOAD_MOMENT,intrval);
    }

    private IDBTableChangeListener groupObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GroupChatRoom rootRoom=null;
                    setupMyGroupInfo();

                    if(!TextUtils.isEmpty(curShownDepartmentId)) {
                        rootRoom=dbHelper.fetchGroupChatRoom(curShownDepartmentId);
                        if(null != rootRoom) {
                            getContactsListFromLocal(rootRoom);
                            setContentMomentWithFilter(true);
                        }
                    } else {
                        rootRoom=dbHelper.fetchRootGroupChatRoom();
                        if(null != rootRoom) {
                            curShownDepartmentId=rootRoom.groupID;

                            getContactsListFromLocal(rootRoom);
                            setTitleText();
                            setContentMomentWithFilter(true);
                        }
                    }
                }
            });
        }
    };

    private IDBTableChangeListener groupChatRoomObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    departmentList=null;
                    if(View.VISIBLE == findViewById(R.id.layout_department).getVisibility()) {
                        //if shown,close it,next open will show changed departments
                        doShowDepartment();
                    }
                }
            });
        }
    };

    private IDBTableChangeListener mAccountSwitchObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            // 修改listview 以及 headview
            mMyUid = mPrefUtil.getUid();
            mTargetUid = mMyUid;
            curShownDepartmentId = "";
            curSelectedTagIdx = TAG_ALL_IDX;
            dbHelper = new Database(MomentActivity.this);
            initDatas();
            updateNewReviewButton();
        }
    };

    private IDBTableChangeListener mFinishLoadMembersObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            initDatas();
        }
    };

    private IDBTableChangeListener momentObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    triggerReloadMoments();
                }
            });
        }
    };

    private IDBTableChangeListener momentReviewObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateNewReviewButton();
                }
            });
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
        mImageFetcher.setExitTasksEarly(false);
        instance = this;

        findViewById(R.id.layout_department).setVisibility(View.GONE);
        if(SELECT_MODE_ALL == _selectMode && !isWithFavorite) {
            btnTitleLeft.setImageResource(R.drawable.nav_group_list_selector);
        }

        /**
         * 自动检查新数据的触发策略：
         *
         * 第一次运行 Activity 时，刷新动态；
         * 以后每次激活 Activity 时，如果时间间隔符合条件，就刷新动态，或仅检查新评论，以及检查相册封面是否更新。
         */
        long now = new java.util.Date().getTime() / 1000;

        // need refresh?
        //  mLastRefreshTime < mPrefUtil.getLatestReviewTimestamp()
        if (mLastRefreshTime == INVALID_TIMESTAMP_VALUE
                || now - mLastRefreshTime > INTERVAL_REFRESH_MOMENTS
                || 0 == dbHelper.getMomentsCount()) {
            loadMomentsFromServer(LOADING_REASON_REFRESH);
        }

        if(mLastCheckReviewTime == INVALID_TIMESTAMP_VALUE || now - mLastCheckReviewTime > INTERVAL_CHECK_REVIEWS) {
            checkForNewReviews_async();
        }

//        if (mLastCheckAlbumCoverTime == INVALID_TIMESTAMP_VALUE
//                || now - mLastCheckAlbumCoverTime > INTERVAL_CHECK_ALBUMCOVER) {
//            checkoutAlbumCover();
//        }
        checkoutAlbumCover();

        setupHeaderContent();
    }

    private void getGroupsAndMembers() {
        GroupChatRoom rootRoom=null;
        if(!TextUtils.isEmpty(curShownDepartmentId)) {
            rootRoom=dbHelper.fetchGroupChatRoom(curShownDepartmentId);
        } else {
            rootRoom=dbHelper.fetchRootGroupChatRoom();
        }
        if(null != rootRoom) {
            curShownDepartmentId = rootRoom.groupID;
            getContactsListFromLocal(rootRoom);
        }
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

    private void updateNewReviewButton() {
        if(!TextUtils.isEmpty(mMyUid) && !TextUtils.isEmpty(mTargetUid)) {
            if(SELECT_MODE_SINGLE == _selectMode && !mMyUid.equals(mTargetUid)) {
                //if not seeing my own moment,do not showing new moment count
                return;
            }
        }

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Moment dummy = new Moment(null);
                return dbHelper.fetchNewReviews(dummy);
            }

            @Override
            protected void onPostExecute(Integer newReviewsCount) {
                if(null != mAdapter) {
                    mAdapter.setNewReviewCount(newReviewsCount);
                    mAdapter.notifyDataSetInvalidated();
                }

//                if(0 == newReviewsCount) {
//                    btnGotoNewReviews.setVisibility(View.GONE);
//                } else {
//                    btnGotoNewReviews.setVisibility(View.VISIBLE);
//                    btnGotoNewReviews.setText(String.format(getString(R.string.moments_new_review_with_count),newReviewsCount));
//                }
            }
        }.execute((Void)null);

//        Moment dummy = new Moment(null);
//        int newReviewsCount=dbHelper.fetchNewReviews(dummy);
//        if(0 == newReviewsCount) {
//            btnGotoNewReviews.setVisibility(View.GONE);
//        } else {
//            btnGotoNewReviews.setVisibility(View.VISIBLE);
//            btnGotoNewReviews.setText(String.format(getString(R.string.moments_new_review_with_count),newReviewsCount));
//        }

//        if (GlobalValue.unreadMomentReviews > 0) {
//            btnGotoNewReviews.setVisibility(View.VISIBLE);
//            btnGotoNewReviews.setText(String.format(getString(R.string.moments_new_review_with_count),
//                    GlobalValue.unreadMomentReviews));
//        } else {
//            btnGotoNewReviews.setVisibility(View.GONE);
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        instance = null;
        mAdapter.stopMedia();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();

        mImageFetcher.clearCacheInMem();

        if (mInputMgr != null && mInputMgr.isShowing()) {
            mInputMgr.hide();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mAdapter.stopMedia();
        mImageFetcher.closeCache();

        Database.removeDBTableChangeListener(mAccountSwitchObserver);
        Database.removeDBTableChangeListener(mFinishLoadMembersObserver);
        Database.removeDBTableChangeListener(momentObserver);
        Database.removeDBTableChangeListener(momentReviewObserver);
        Database.removeDBTableChangeListener(groupObserver);
        Database.removeDBTableChangeListener(groupChatRoomObserver);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {

        state.putInt("_selectMode", _selectMode);
        state.putString(EXTRA_UID, mTargetUid);
        state.putBoolean(EXTRA_WITH_FAVORITE, isWithFavorite);
        state.putString(EXTRA_SHOWN_DEPARTMENT, curShownDepartmentId);
        state.putInt(EXTRA_SELECTED_TAG_IDX,curSelectedTagIdx);
        state.putInt(EXTRA_HEADER_BTNS_MODE,headerOpButtonsMode);

        if (mInputMgr != null) {
            state.putParcelable("mInputMgr", mInputMgr);
        }

        if (mMediaInput != null) {
            state.putParcelable("mMediaInput", mMediaInput);
        }

        if (mInputedPhotoPathForAlbumcover != null) {
            state.putString("mInputedPhotoPathForAlbumcover",
                    mInputedPhotoPathForAlbumcover);
        }
    }

    public boolean handleItemClick() {
        if (mInputMgr != null && mInputMgr.isShowing()) {
            mInputMgr.hide();
            return true;
        }

        return false;
    }

    public boolean handleBackPress() {
        if (mInputMgr != null && mInputMgr.isShowing()) {
            if (true || mInputMgr.showingFlags() == InputBoardManager.FLAG_SHOW_TEXT) {
                mInputMgr.hide();

                setStartActivityTabBarStatus(false);
                return true;
            }
        }

        return false;
    }

    private void setStartActivityTabBarStatus(boolean hide) {
        if(null != StartActivity.instance()) {
            StartActivity.instance().setTabBarStatus(hide);
        }
    }

    @Override
    public void onBackPressed() {
        // back steps:
        // multimedia -> text -> hide -> back
//        if (mInputMgr != null && mInputMgr.isShowing()) {
//            if (mInputMgr.showingFlags() == InputBoardManager.FLAG_SHOW_TEXT)
//                mInputMgr.hide();
//            else
//                mInputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
//            return;
//        }
        if(!handleBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mInputMgr != null && mInputMgr
                .handleActivityResult(requestCode, resultCode, data))
            return;

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CREATE_MOMENT && resultCode == RESULT_OK) {
            //keep original mode
//            changeModeSingle();

            loadMomentsFromLocal();

            Log.i("lastLVPos reset to 0");
            lastItemIndex[curSelectedTagIdx]=0;
            topOffset[curSelectedTagIdx]=0;
        } else if (requestCode == REQ_SHOW_MYPROFILE && resultCode == RESULT_OK) {
            setupHeaderContent();
        } else if (ACTIVITY_REQ_ID_NEW_REVIEWS == requestCode && resultCode == RESULT_OK) {
            Log.w("new reviews view return");
//            btnGotoNewReviews.setVisibility(View.GONE);
        }
    }


    /**
     * @param context
     * @param uid null for all.
     */
    public static void launch(Context context, String uid) {
        Intent intent = new Intent(context, MomentActivity.class);
        intent.putExtra(MomentActivity.EXTRA_UID, uid);
        context.startActivity(intent);
    }

    public static void launchMy(Context context) {
        launch(context, PrefUtil.getInstance(context).getUid());
    }

    public static void launchFavorite(Context context) {
        Intent intent = new Intent(context, MomentActivity.class);
        intent.putExtra(MomentActivity.EXTRA_WITH_FAVORITE, true);
        context.startActivity(intent);
    }

    private void removeCoverImage() {
        if (ErrorCode.OK == mWeb.removeAlbumCover()) {
            mAlbumCover = new AlbumCover();
            mAlbumCover.timestamp = -1;
            dbHelper.storeAlbumCover(mMyUid, mAlbumCover);
            saveAlbumCover2SP(mAlbumCover);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayAlbumCover();
                }
            });
        } else {
            mMsgBox.show(null, getString(R.string.operation_failed));
        }
    }



    @Override
    public void didFinishNetworkIFCommunication(int tag, byte[] bytes) {
        if (tag == NETWORK_TAG_UPLOADING_ALBUMCOVER && mProgressUploadingAlbumcover != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressUploadingAlbumcover.setVisibility(View.GONE);
                }
            });
        }

        String fid = new String(bytes);
        String path = PhotoDisplayHelper.makeLocalFilePath(fid, "jpg");
        File srcFile = new File(mInputedPhotoPathForAlbumcover);
        File dstFile = new File(path);
        if (srcFile.exists()) {
            FileUtils.copyFile(srcFile, dstFile);
        }

        HashMap<String, Object> resultMap = mWeb.fSetAlbumCover(fid, "jpg");
        if (ErrorCode.OK == (Integer)resultMap.get("result_code")) {
            mAlbumCover = new AlbumCover(fid, "jpg");
            Object timeObject = resultMap.get("update_timestamp");
            if (null != timeObject && timeObject instanceof Long) {
                mAlbumCover.timestamp = (Long) timeObject;
            } else {
                mAlbumCover.timestamp = System.currentTimeMillis();
            }
            dbHelper.storeAlbumCover(mMyUid, mAlbumCover);
            saveAlbumCover2SP(mAlbumCover);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayAlbumCover();
                }
            });
        } else {
            mMsgBox.show(null, getString(R.string.operation_failed));
        }
    }

    @Override
    public void didFailNetworkIFCommunication(int tag, byte[] bytes) {
        if (tag == NETWORK_TAG_UPLOADING_ALBUMCOVER
                && mProgressUploadingAlbumcover != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressUploadingAlbumcover.setVisibility(View.GONE);
                }
            });
        }
        mMsgBox.show(getString(R.string.operation_failed), new String(bytes));
    }

    @Override
    public void setProgress(int tag, final int progress) {
        if (tag == NETWORK_TAG_UPLOADING_ALBUMCOVER
                && mProgressUploadingAlbumcover != null) {
            // the progress is very inaccurate, so we'd better not show it.
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mProgressUploadingAlbumcover.setProgress(progress);
//                }
//            });
        }
    }

    /**
     * Perform replying action.
     */
    @Override
    public void replyToMoment(int momentPosition, final String momentId, final Review replyTo) {
        if (mMenu == null)
            mMenu = new BottomButtonBoard(this, findViewById(android.R.id.content));
        else
            mMenu.clearView();

        if (replyTo == null) {
            replyToMoment_helper(momentPosition, momentId, replyTo, this, this,
                    this, getLikeBtnClickListener(MomentActivity.this,momentId));
        } else {
            doWithReview(momentPosition, momentId, replyTo, mMenu, this, this,
                    this, onMomentReviewDeleteListener, getLikeBtnClickListener(MomentActivity.this, momentId));
        }
    }

    public interface OnMomentReviewDeleteListener {
        void onMomentDelete(String momentId,Review review);
    }

    private View.OnClickListener getLikeBtnClickListener(final Context context,final String momentId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Database db = new Database(context);
                final Moment moment=db.fetchMoment(momentId);
                if(null != moment) {
                    doLikeMoment_async(moment);
                }
            }
        };
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
            final OnMomentReviewDeleteListener momentReviewDelListener,
            final OnClickListener onLikeBtnClickListener) {

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

    public static void deleteMomentReview(final Activity activity,final String momentId, final Review replyTo,final OnMomentReviewDeleteListener momentReviewDelListener) {
        final MessageBox msgBox = new MessageBox(activity);
        msgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                WowMomentWebServerIF mMomentWeb = WowMomentWebServerIF.getInstance(activity);
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

    public static void replyToMoment_helper(int momentPosition, String momentId, Review replyTo,
                                             Activity activity,
                                             InputBoardManager.InputResultHandler handler,
                                             InputBoardManager.ChangeToOtherAppsListener changeAppsListener,
                                             OnClickListener onLikeClickListener) {
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
            title = activity.getString(R.string.moments_comment_hint);
        else
            title = String.format(activity.getResources().getString(R.string.moments_reply_hint),
                    replyTo.nickname);

        inputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
        inputMgr.setInputHint(title);
        inputMgr.extra().putInt(EXTRA_REPLY_TO_MOMENT_POS, momentPosition);
        inputMgr.extra().putString(EXTRA_REPLY_TO_MOMENT_ID, momentId);
        inputMgr.extra().putParcelable(EXTRA_REPLY_TO_REVIEW, replyTo);

        inputMgr.setLayoutForTimelineMoment(moment,onLikeClickListener);


        instance().setStartActivityTabBarStatus(true);
    }

    @Override
    public void setInputBoardMangager(InputBoardManager m) {
        mInputMgr = m;
    }

    @Override
    public InputBoardManager getInputBoardMangager() {
        return mInputMgr;
    }

    @Override
    public void onHeightChanged(int height) {
    }

    @Override
    public void onTextInputted(String text) {
        // 发表评论
        if(TextUtils.isEmpty(text.trim())) {
            mMsgBox.toast(R.string.comment_content_cannot_empty);
            return;
        } else if (text.length() > COMMENT_MOST_WORDS) {
            mMsgBox.toast(R.string.moments_comment_oom);
            return;
        }
        if (mInputMgr == null)
            return;

        mInputMgr.setSoftKeyboardVisibility(false);
        String mid = mInputMgr.extra().getString(EXTRA_REPLY_TO_MOMENT_ID);
        int mpos = mInputMgr.extra().getInt(EXTRA_REPLY_TO_MOMENT_POS);
        Review replyTo = mInputMgr.extra().getParcelable(EXTRA_REPLY_TO_REVIEW);
        if (mid == null)
            return;

        Moment moment = null;

        // doReviewMoment_async() 需要一个 Moment 参数，我们应该传 Adapter 引用的那个对象，
        // 而不是创建一个具有相同 ID 的新对象，这样才能让 Adapter.notifyAdapterDataChanged() 立即生效。

        if (mAdapter != null && mpos >= 0 && mpos < mAdapter.getCount()) {
            moment = mAdapter.getItem(mpos);
            if (moment != null && !mid.equals(moment.id))
                moment = null;
        }

        if (moment == null)
             moment = dbHelper.fetchMoment(mid);

        if (moment == null)
            return;

        doReviewMoment_async(moment,
                replyTo == null ? null : replyTo.id,
                text);
        mInputMgr.hide();
    }

    @Override
    public void onVoiceInputted(String path, int duration) {
        mMsgBox.show(null, getString(R.string.moments_voice_not_supported_in_review));
    }

    @Override
    public void onStampInputted(Stamp s) {
        mMsgBox.show(null, getString(R.string.moments_stamp_not_supported_in_review));
    }

    @Override
    public void onPhotoInputted(String path, String thumbPath) {
        mMsgBox.show(null, getString(R.string.moments_photo_not_supported_in_review));
    }

    @Override
    public void onVideoInputted(String path, String thumbPath) {
        mMsgBox.show(null, getString(R.string.moments_video_not_supported_in_review));
    }

    @Override
    public void onVideoChatRequested() {

    }

    @Override
    public void onCallRequested() {
    }

    @Override
    public void onLocationInputted(double latitude, double longitude, String address) {
        mMsgBox.show(null, getString(R.string.moments_loc_not_supported_in_review));
    }

    private int headerRefreshImgHeight=0;
    private int getHeaderRefreshImgHeight() {
        if(0 == headerRefreshImgHeight) {
            ImageView aImageView=new ImageView(this);
            aImageView.setImageResource(R.drawable.refresh);
            aImageView.measure(0,0);
            headerRefreshImgHeight=aImageView.getMeasuredHeight();
        }
        Log.e("ret header refresh img height="+headerRefreshImgHeight);
        return headerRefreshImgHeight;
    }

    private int headerHideHeight;
    private ImageView ivRefreshingRotateInd;
    private boolean isPullToRefreshHeaderPullThresholdSet=false;
    private boolean isPulling=false;
    private void setupPullToRefresh() {
        ivRefreshingRotateInd=(ImageView) findViewById(R.id.imgRefreshRotate);
//        mHeaderRotateImageView = (ImageView) findViewById(R.id.imgRotate);
        if(isWithFavorite) {
            ivRefreshingRotateInd.setImageResource(R.drawable.transparent_10x10);
        }
//        mHeaderRotateImageDummy = (ImageView) findViewById(R.id.img_rotate_dummy);
//        mHeaderRotateImageParent =  findViewById(R.id.box_image);

        // let mHeaderBgImageView.height = mHeaderBgImageView.width
        mHeaderBgImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mHeaderBgImageView.getWidth() > 0 && !isPullToRefreshHeaderPullThresholdSet) {
                            isPullToRefreshHeaderPullThresholdSet=true;

                            ViewGroup.LayoutParams lp = mHeaderBgImageView.getLayoutParams();
//                            lp.height = (int)(mHeaderBgImageView.getWidth()*1.0/3);
                            lp.height = (int)(findViewById(R.id.box_info).getMeasuredHeight()+
                                    2*getHeaderRefreshImgHeight());
                            mHeaderBgImageView.setLayoutParams(lp);

//                            int hideHeight = (int)(lp.height * (_selectMode == SELECT_MODE_SINGLE ? 0.28f : 0.4f));
                            headerHideHeight = 2*getHeaderRefreshImgHeight();
                            getPTR().setRefreshableViewMarginTop(-headerHideHeight);
//                            getPTR().setRefreshableViewMarginTop(0);

                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) ivRefreshingRotateInd.getLayoutParams();
                            if (rlp != null) {
//                                rlp.setMargins(rlp.leftMargin,
//                                        hideHeight - (int)(ivRefreshingRotateInd.getDrawable().getIntrinsicHeight()
//                                        * (_selectMode == SELECT_MODE_SINGLE ? 1.0f : 1.2f)),
//                                        rlp.rightMargin, rlp.bottomMargin);
                                setHeaderRotateImgPos2hide();
                            }
                            mHeaderBgImageView.requestLayout();

                            getPTR().setHeaderPullThreshold(0);

                        }
                    }
                }
        );

        mRotationPivotX = ivRefreshingRotateInd.getDrawable().getIntrinsicWidth() / 2;
        mRotationPivotY = ivRefreshingRotateInd.getDrawable().getIntrinsicHeight() / 2;

        ivRefreshingRotateInd.setScaleType(ImageView.ScaleType.MATRIX);
        mHeaderImageMatrix = new Matrix();
        ivRefreshingRotateInd.setImageMatrix(mHeaderImageMatrix);

        mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);

        PullToRefreshBase.OnPullEventListener onPullEventListener =
                new PullToRefreshBase.OnPullEventListener<ListView>() {
                    @Override
                    public void onPullEvent(PullToRefreshBase<ListView> refreshView,
                                            PullToRefreshBase.State state,
                                            PullToRefreshBase.Mode direction) {
                        if (state == PullToRefreshBase.State.REFRESHING) {
                            // header view 会向上 smooth scroll。我们希望的效果是，在此过程中 rotate image 相对于屏幕的位置保持不变，
                            // 因此我们让 rotate image 相对于 header view 做向下的平移动画，通过调节动画的幅度和时长，使其刚好抵消
                            // header view 的向上运动。
                            //
                            // 让 rotate image 一边平移一边绕自己的中心旋转有点困难(试过 AnimationSet，image 的轨迹呈螺线型），所以
                            // 让 image parent 和 image 分布进行平移和旋转。
                            //
                            // 这又产生了新的问题：平移如期发生了，旋转就没有，解决办法是让一个隐藏的 dummy image 也进行旋转。原理未知。
//                            TranslateAnimation ta = new TranslateAnimation(
//                                    Animation.ABSOLUTE, 0,
//                                    Animation.ABSOLUTE, 0,
//                                    Animation.ABSOLUTE, 0,
//                                    Animation.ABSOLUTE, getPTR().getHeaderPullThreshold());
//
//                            ta.setInterpolator(ANIMATION_INTERPOLATOR);
//                            ta.setDuration(PullToRefreshBase.SMOOTH_SCROLL_DURATION_MS);
//                            ta.setFillEnabled(true);
//                            ta.setFillAfter(true);

//                            mHeaderRotateImageParent.startAnimation(ta);
                            setHeaderRotateImgPos2rotate();
                            ivRefreshingRotateInd.startAnimation(mRotateAnimation);
//                            mHeaderRotateImageDummy.startAnimation(mRotateAnimation);
                        } else {
                            if (state == PullToRefreshBase.State.RESET) {
                                setHeaderRotateImgPos2hide();
                            }
                            ivRefreshingRotateInd.clearAnimation();
                            mHeaderImageMatrix.reset();
                            ivRefreshingRotateInd.setImageMatrix(mHeaderImageMatrix);
                        }

                        if(state == PullToRefreshBase.State.PULL_TO_REFRESH ||
                                state == PullToRefreshBase.State.RELEASE_TO_REFRESH) {
                            isPulling=true;
                        } else {
                            isPulling=false;
                        }
                        Log.e("state: "+state);
                        updateHandler.sendEmptyMessage(MSG_ID_UPDATE_HEADER_BUTTONS);
                    }

                    @Override
                    public void onPullScaleChanged(float scaleOfLayout) {
                        rotatePtrImage(scaleOfLayout);
                    }

                    private void rotatePtrImage(float scaleOfLayout) {
                        float angle = scaleOfLayout * 90f;
                        mHeaderImageMatrix.setRotate(angle, mRotationPivotX, mRotationPivotY);
                        ivRefreshingRotateInd.setImageMatrix(mHeaderImageMatrix);
                    }
                };

        PullToRefreshBase.OnRefreshListener<ListView> rl =
                new PullToRefreshBase.OnRefreshListener<ListView>() {
                    @Override
                    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
//                        mWillRefreshFromTimestamp = 0;
//                        loadMoments(LOADING_REASON_REFRESH);
                        if(!isWithFavorite) {
                            cur_moment_show_count = PAGE_SIZE;
                        }
                        loadMomentsFromServer(LOADING_REASON_PULL_REFRESH);
                    }
                };

        mPtrSingle.setOnRefreshListener(rl);

//        mPtrSingle.setShowViewWhileRefreshing(isWithFavorite?false:true);
        mPtrSingle.setShowViewWhileRefreshing(false);

        mPtrSingle.setOnPullEventListener(onPullEventListener);
    }

    private void setHeaderRotateImgPos2hide() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) ivRefreshingRotateInd.getLayoutParams();
        if (rlp != null) {
            int avaliableHeight=getHeaderRefreshImgHeight();
            rlp.setMargins(avaliableHeight/4,
                    avaliableHeight/2,
                    avaliableHeight/4, rlp.bottomMargin);
            ivRefreshingRotateInd.setLayoutParams(rlp);
        }
    }

    private void setHeaderRotateImgPos2rotate() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) ivRefreshingRotateInd.getLayoutParams();
        if (rlp != null) {
            int avaliableHeight=getHeaderRefreshImgHeight();
            rlp.setMargins(avaliableHeight/4,
                    (int)(avaliableHeight*2+avaliableHeight/4),
                    avaliableHeight/4, rlp.bottomMargin);
            ivRefreshingRotateInd.setLayoutParams(rlp);
        }
    }

    /**
     * 在调用 {@link com.handmark.pulltorefresh.library.PullToRefreshBase#onRefreshComplete()} 之前，执行一些必要的动画。
     */
    private void onRefreshComplete() {
        // 撤销向下的平移动画，使 rotate image 相对于 header view 的位置还原。
//        mHeaderRotateImageParent.clearAnimation();

        // 补一个向上的平移动画，使上面的撤销过程平滑。
        // PullToRefreshBase.onRefreshComplete() 要在动画结束后才调用，以确保动画完整进行。
        TranslateAnimation ta = new TranslateAnimation(
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -1);
        ta.setInterpolator(ANIMATION_INTERPOLATOR);
        ta.setDuration(PullToRefreshBase.SMOOTH_SCROLL_DURATION_MS);
        ta.setFillEnabled(true);
        ta.setFillAfter(true);
        ta.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getPTR().onRefreshComplete();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        ivRefreshingRotateInd.startAnimation(ta);

        //as view is hidden,no anim anymore,trigger finish directly
//        getPTR().onRefreshComplete();
    }

    @Override
    public boolean onLoadMore() {

        if (!mIsLoadingMore) {
            mIsLoadingMore = true;
            loadMoreHandle();
            return true;
        }
//        updateTimestampToLoadFrom();
//
//        if (!mIsLoadingMore && mWillRefreshFromTimestamp > 0) {
//            mIsLoadingMore = true;
//            loadMoments(LOADING_REASON_MORE);
//            return true;
//        }
        return false;
    }

//    private void updateTimestampToLoadFrom() {
//        if (null != momentsAll && !momentsAll.isEmpty()) {
//            Moment m = momentsAll.get(momentsAll.size() - 1);
//            if (m != null && (mWillRefreshFromTimestamp == 0 || mWillRefreshFromTimestamp > m.timestamp)) {
//                mWillRefreshFromTimestamp = m.timestamp;
//            }
//        }
//    }

    private long getMinTimeStamp() {
        long ret=-1;
        if (null != momentsAll && !momentsAll.isEmpty()) {
            Moment m = momentsAll.get(momentsAll.size() - 1);
            if (m != null) {
                ret = m.timestamp;
            }
        }

        return ret;
    }

    @Override
    public void toastCannotSendMsg() {
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        handleItemClick();
//
//        return super.dispatchTouchEvent(ev);
//    }

    /**
     * Request to check new reviews again.
     */
//    public static void requestCheckNewReviews() {
//        // just invalidate the last check timestamp will be done
//        mLastCheckReviewTime = INVALID_TIMESTAMP_VALUE;
//    }

    interface BeginUploadAlbumCover {
        void onBeginUploadCover(String filePath);
    }
}
