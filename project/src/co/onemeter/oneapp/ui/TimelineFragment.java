package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import co.onemeter.utils.AsyncTaskExecutor;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.Stamp;

import java.util.ArrayList;

/**
 * <p>浏览动态。</p>
 * Created by pzy on 10/13/14.
 */
public abstract class TimelineFragment extends ListFragment
        implements MomentAdapter.MomentActionHandler, OnTimelineFilterChangedListener,
        PullToRefreshListView.OnRefreshListener, MomentAdapter.LoadDelegate, InputBoardManager.ChangeToOtherAppsListener, InputBoardManager.InputResultHandler {
    protected static final int PAGE_SIZE = 10;
    private static final int REQ_COMMENT = 123;
    private MomentAdapter adapter;

    
    // selected tag index on UI
    private int selectedTag = 0;
    // record max timestamp for convenience of loading more
    private long maxTimestamp = 0;
    private boolean mNoMore = false;
    
    //用于区分账号的类型
    private int countType = -1;//默认全部
    private boolean viewCreated = false;
    
    //用于判断是自己还是好友时，动态是否显示新动态提醒
    public static boolean newReviewFlag = true;


    public static final String EXTRA_REPLY_TO_MOMENT_ID = "reply_to_moment_id";
    public static final String EXTRA_REPLY_TO_REVIEW = "reply_to_review_id";
    private Moment moment;
    private BottomButtonBoard mMenu;
    private InputBoardManager mInputMgr;
    private MessageBox mMsgBox;
    private Database dbHelper;
    private PrefUtil mPrefUtil;
    private MomentWebServerIF mMomentWeb;
    private TimelineActivity.OnMomentReviewDeleteListener onMomentReviewDeleteListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedTag = savedInstanceState.getInt("selectedTag");
        }

        //弹框 进度初始化
        mMsgBox = new MessageBox(getActivity());
        dbHelper = new Database(getActivity());
        mPrefUtil = PrefUtil.getInstance(getActivity());
        mMomentWeb = MomentWebServerIF.getInstance(getActivity());

        onMomentReviewDeleteListener=new TimelineActivity.OnMomentReviewDeleteListener() {
            @Override
            public void onMomentDelete(String momentId, Review review) {
                momentReviewObserver.onDBTableChanged(Database.TBL_MOMENT);
//                momentObserver.onDBTableChanged(Database.TBL_MOMENT);
                if (mInputMgr != null) {
                    mInputMgr.setLayoutForTimelineMoment(moment,new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            doLikeMoment_async(moment);
                        }
                    });
                }
            }
        };

        // load moments
        setupListAdapter(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag),-1));
        checkNewMoments();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewCreated = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedTag", selectedTag);
    }

    /**
     * Load moments from local DB.
     * @param tag Tag index. -1 means not limited.
     * @return
     */
    protected abstract ArrayList<Moment> loadLocalMoments (long maxTimestmap, String tag,int countType);

    /**
     * Load moments from web server.
     * @param maxTimestamp 0 means now.
     * @return {@link org.wowtalk.api.ErrorCode}
     */
    protected abstract int loadRemoteMoments(long maxTimestamp);
    
    /**
     * 根据UI的从本地数据库中加载动态
     * @param maxTimestamp
     * @return
     * @author hutianfeng
     */
//    protected abstract ArrayList<Moment> loadUidMoments(int countType,long maxTimestamp);

    /**
     * fill/append/clear list view, set visibility of the "load more" item.
     * @param lst
     * @param append append or replace?
     */
    private void fillListView(ArrayList<Moment> lst, boolean append) {
        maxTimestamp = lst.isEmpty() ? 0 : lst.get(lst.size() - 1).timestamp;
        if (adapter != null) {
            if (!append) 
                adapter.clear();          	
            	adapter.addAll(lst);
//            	adapter.setShowLoadMoreAsLastItem(!append || lst.size() >= PAGE_SIZE);
            	adapter.setShowLoadMoreAsLastItem(true);
            	adapter.notifyDataSetChanged();
        } else {
            setupListAdapter(lst);
        }
    }
    
    private void setupListAdapter(ArrayList<Moment> items) {  	
        	ImageResizer imageResizer = new ImageResizer(getActivity(), DensityUtil.dip2px(getActivity(), 100));
        	adapter = new MomentAdapter(getActivity(),
                    getActivity(),
                    items,
                    false,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
                    false,
                    imageResizer,
                    this,
                    null,
                    new MessageBox(getActivity()));
            adapter.setShowLoadMoreAsLastItem(!items.isEmpty());
            adapter.setLoadDelegate(this);
            setListAdapter(adapter);
 
    }

    public void checkNewMoments() {
        AsyncTaskExecutor.executeShortNetworkTask(new RefreshMomentsTask(), Long.valueOf(0));
    }

    public void insertMoment(Moment moment, int index) {
        if (adapter != null) {
            adapter.insert(moment, index);
            Log.d("---insertMoment----:","insertMomen插入新活动");
//            adapter.notifyLoadingCompleted();
            //把下面两句放在此，主要是为了达到真正的新建动态刷新，不影响局部刷新和位置
            //还可以解决新建动态后头像标记不刷新问题
            setupListAdapter(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag),-1));
            checkNewMoments();
            
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        setupListHeaderView();
        
        PullToRefreshListView listView = getPullToRefreshListView();
        if (listView != null) {
            listView.setOnRefreshListener(this);
        }
        
        checkNewReviews();

        Database.addDBTableChangeListener(Database.TBL_MOMENT,momentReviewObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);

    }

    @Override
    public void onPause() {
        super.onPause();
      //重新返回到所有好友页面的动态，再次显示新动态提醒
        TimelineFragment.newReviewFlag = true;
        Database.removeDBTableChangeListener(momentReviewObserver);
    }
    
    
    @Override
    public void onDestroy() {
    	if (adapter != null) {
    		adapter.stopMedia();//消除掉位关闭的媒体文件
    	}
    	super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("---fragment");
        if (requestCode == REQ_COMMENT && resultCode == Activity.RESULT_OK) {
            Log.d("---REQ_COMMENT:","刷新view");
            // refresh views
            boolean handled = false;
            if (data != null) {
                String changedMomentId = data.getStringExtra(MomentDetailActivity.EXTRA_CHANGED_MOMENT_ID);
                if (changedMomentId != null) {
//                    Database dbHelper = new Database(getActivity());
                    for (int i = 0; i < adapter.getCount(); ++i) {
                        Moment m = adapter.getItem(i);
                        if (TextUtils.equals(changedMomentId, m.id)) {
                            Moment changedMoment = dbHelper.fetchMoment(changedMomentId);
                            adapter.remove(m);
                            adapter.insert(changedMoment, i);
                            adapter.notifyDataSetChanged();
                            handled = true;
                            break;
                        }
                    }
                } else {
                    String deletedMomentId = data.getStringExtra(MomentDetailActivity.EXTRA_DELETED_MOMENT_ID);
                    if (deletedMomentId != null) {
                        for (int i = 0; i < adapter.getCount(); ++i) {
                            Moment m = adapter.getItem(i);
                            if (TextUtils.equals(deletedMomentId, m.id)) {
                                adapter.remove(m);
                                adapter.notifyDataSetChanged();
                                handled = true;
                                break;
                            }
                        }
                    }
                }

            }

            if (!handled) {
                // refresh list
                AsyncTaskExecutor.executeShortNetworkTask(new RefreshMomentsTask(){
                    @Override
                    protected void onPostExecute(Integer errno) {
                        super.onPostExecute(errno);
                    }
                }, Long.valueOf(0));
            }
        }
    }

    /**
     * 删除自己的动态
     * 备注：该方法必须返回int型，可以避免数组角标跨界
     * @date 2015/5/7
     * @param deletedMomentId
     */
    public int refreshDeleteAdapter(String deletedMomentId){
        if (deletedMomentId != null) {
            for (int i = 0; i < adapter.getCount(); ++i) {
                Moment m = adapter.getItem(i);
                if (TextUtils.equals(deletedMomentId, m.id)) {
                    adapter.remove(m);
                    adapter.notifyDataSetChanged();
                    return 0;
                }
            }
        }
        return 0;
    }


//    public int refreshDeleteReview(String changedMomentId) {
//        //Database dbHelper = new Database(getActivity());
//
//        if (changedMomentId != null) {
//            for (int i = 0; i < adapter.getCount(); ++i) {
//                Moment m = adapter.getItem(i);
//                if (TextUtils.equals(changedMomentId, m.id)) {
//                    Moment changedMoment = dbHelper.fetchMoment(changedMomentId);
//                    adapter.remove(m);
//                    adapter.insert(changedMoment, i);
//                    adapter.notifyDataSetChanged();
//                    return 0;
//                }
//            }
//        }
//        return 0;
//
//    }

    
    /**
     * 点赞和评论的局部刷新
     */
    @Override
    public void replyToMoment(int position, final Moment moment, Review replyTo, boolean like) {
        if (like) {
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Integer>() {
                Review r = new Review(); // 添加或删除的赞

                @Override
                protected Integer doInBackground(String... params) {
                    MomentWebServerIF web = MomentWebServerIF.getInstance(getActivity());  
                    if (!moment.likedByMe) { // 点赞
                        return web.fReviewMoment(params[0], Review.TYPE_LIKE, null, null, r);
                    } else { // 撤销赞
                        Review likeReview = null;
                        String mMyUid = PrefUtil.getInstance(getActivity()).getUid();
                        for (Review aReview : moment.reviews) {
                            if (Review.TYPE_LIKE == aReview.type && aReview.uid.equals(mMyUid)) {
                                likeReview = aReview;
                                break;
                            }
                        }
                        if (null != likeReview) {
                            r = likeReview;
                            return web.fDeleteMomentReview(moment.id, likeReview);
                        } else {
                            return ErrorCode.OPERATION_FAILED;
                        }
                    }
                }

                @Override
                protected void onPostExecute(Integer errcode) {
                    if (errcode == ErrorCode.OK) {
                        moment.likedByMe = !moment.likedByMe;
                        if (moment.likedByMe) {
                            moment.reviews.add(r);
                        } else {
                            moment.reviews.remove(r);
                        }
//                        Database dbHelper = new Database(getActivity());
                        dbHelper.storeMoment(moment, moment.id);
//                        adapter.notifyDataSetChanged();
                    }
                }
            }, moment.id);
        } else {//评论

            //评论按钮进入详情页刷新
//        	Intent intent = new Intent(this.getActivity(), MomentDetailActivity.class).putExtra("moment", moment);
//        	intent.putExtra("button_reply", true);//通过评论按钮进入详情页，自动弹起输入软键盘，有标志值且为true
//        	String mMyUid = PrefUtil.getInstance(this.getActivity()).getUid();
//            if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && moment.owner.userID.equals(mMyUid)) {
//                intent.putExtra("isowner", 1);//给自己多传一个标志值
//            }
//
//            startActivityForResult(intent,REQ_COMMENT);

            //使得当前item的moment和外moment保持一致
            this.moment =  moment;

            if (mMenu == null)
                mMenu = new BottomButtonBoard(getActivity(), getActivity().findViewById(android.R.id.content));
            else
                mMenu.clearView();

            if (replyTo == null) {
                TimelineActivity.replyToMoment_helper(-1, moment.id, replyTo, getActivity(),
                        this, this, getLikeBtnClickListener(moment.id));
            } else {
                TimelineActivity.doWithReview(-1, moment.id, replyTo, mMenu, getActivity(), this,
                        this, onMomentReviewDeleteListener, getLikeBtnClickListener(moment.id));
            }

            //点击评论按钮，进入详情页自动弹起输入软键盘,而不是item
//            if (mInputMgr != null) {
//
//                mInputMgr.mTxtContent.setFocusable(true);
//                mInputMgr.mTxtContent.setFocusableInTouchMode(true);
//                mInputMgr.mTxtContent.requestFocus();
//
//                Handler hanlder = new Handler();
//                hanlder.postDelayed(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        InputMethodManager imm = (InputMethodManager) mInputMgr.mTxtContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                        imm.showSoftInput(mInputMgr.mTxtContent, 0);
//                    }
//                }, 200);
//            }


        }
    }


    /**
     * 点击非评论按钮进入详情页刷新
     * @date 2015/1/8
     */
    public void onMomentClicked(int position, Moment moment) {

        Activity context = getActivity();

        Intent intent = new Intent(this.getActivity(), MomentDetailActivity.class)
                .putExtra("moment", moment);
        intent.putExtra("button_reply", false);
        String mMyUid = PrefUtil.getInstance(context).getUid();
        if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && moment.owner.userID.equals(mMyUid)) {
            intent.putExtra("isowner", 1);//给自己多传一个标志值
        }

        // 用户可能在详情页点赞或评论，或删除动态，总之可那需要在 onActivityResult 中刷新UI
        startActivityForResult(intent, REQ_COMMENT);
    }


    private View.OnClickListener getLikeBtnClickListener(final String momentId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Moment m;
                if (TextUtils.equals(moment.id, momentId)) {
                    m = moment;
                } else {
                    m = dbHelper.fetchMoment(momentId);
                }
                if (null != m) {
                    doLikeMoment_async(m);
                }
            }
        };
    }


    /**
     * Possible error:
     * java.lang.IllegalStateException: Cannot add header view to list -- setAdapter has already been called.
     */
    protected abstract void setupListHeaderView();

    @Override
    public void onSenderChanged(int index) {

    	if (index == 0) {//全部
    		countType = -1;
    	} else if (index == 1) {//官方账号 0
    		countType = 0;
    	} else if (index == 2) {//老师账号2
    		countType = 2;
    	} else if (index == 3) {//学生账号1
    		countType = 1;
    	}
    	//重新定位到全部内容
    	selectedTag = 0;
    	fillListView(loadLocalMoments (0, tagIdxFromUiToDb(selectedTag),countType), false);
    	
    	
    }

    /**
     * 输入法中点赞的刷新
     * @param moment
     */


    private void doLikeMoment_async(final Moment moment) {
        if (moment == null)
            return;
        if(moment.likedByMe) {
            Review likeReview=null;
            String mMyUid = mPrefUtil.getUid();
            for(Review aReview : moment.reviews) {
                if(Review.TYPE_LIKE == aReview.type && aReview.uid.equals(mMyUid)) {
                    likeReview=aReview;
                    break;
                }
            }

            if(null != likeReview) {
                TimelineActivity.deleteMomentReview(getActivity(),moment.id,likeReview,onMomentReviewDeleteListener);
                moment.reviews.remove(likeReview);
                dbHelper.storeMoment(moment, moment.id);
//                setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));

            } else {
                Log.e("delete like review null");
            }
            return;
        }
        final String momentId = moment.id;
        final Review r = new Review();
        r.id= "0";
        r.type = Review.TYPE_LIKE;
        r.uid = mPrefUtil.getUid();
        r.nickname = mPrefUtil.getMyNickName();
        r.read = true;

        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mMomentWeb.fReviewMoment(momentId, Review.TYPE_LIKE, null, null, r);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
                    moment.likedByMe = true;
//                    moment.reviews.add(r);执行这句会外面评论导致多一个点赞
                    addReviewToList(r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setLayoutForTimelineMoment(moment, getLikeBtnClickListener(momentId));
                        mInputMgr.setInputText("");
                    }

                    dbHelper.storeMoment(moment, moment.id);
                    //实现点赞异步刷新
//                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, momentId));
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        });
    }

    /**
     * 输入法中对动态的评论
     * @param moment
     * @param replyToReviewId
     * @param strComment
     */

    private void doReviewMoment_async(final Moment moment,
                                      final String replyToReviewId,
                                      final String strComment) {
        if (moment == null) return;
        final String moment_id = moment.id;
        final Review r = new Review();
        r.id = "0";
        r.uid = mPrefUtil.getUid();
        r.nickname = mPrefUtil.getMyNickName();
        r.type = Review.TYPE_TEXT;
        r.text = strComment;
        r.replyToReviewId = replyToReviewId;
        r.read = true;

        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mMomentWeb.fReviewMoment(moment_id, Review.TYPE_TEXT,
                            strComment, replyToReviewId, r);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    addReviewToList(r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setInputText("");
                    }

//                    moment.reviews.add(r);//执行这句会外面评论导致多一条的评论
                    dbHelper.storeMoment(moment, moment.id);
//                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));//外面无需返回刷新
                }
            }
        });
    }

    private void addReviewToList(Review r) {
        moment.reviews.add(moment.reviews.size(), r);
    }
    

    @Override
    public void onTagChanged(int index) {
        selectedTag = index;
        countType = -1;//-1为全部
        fillListView(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag),countType), false);
        //Toast.makeText(getActivity(), "tag: " + index, Toast.LENGTH_SHORT).show();
    }

    /**
     * 如果使用了 {@link com.handmark.pulltorefresh.widget.PullToRefreshListView}，请重写此方法，以便在这里设置事件
     * 处理过程。
     * @return
     */
    protected PullToRefreshListView getPullToRefreshListView() {
        return null;
    }

    public int getMomentTag() {
        return selectedTag;
    }

    /**
     * UI 中的 tag index 以 0 代表不限，
     * DB 中的 tag index 以 -1 代表不限。
     * @param uiTagIdx
     * @return
     */
    private String tagIdxFromUiToDb(int uiTagIdx) {
        // tag与数据库对应
        String tag = "-1";
        switch (uiTagIdx) {
        case 0:
            // 全部
            tag = "-1";
            break;
        case 1:
            // 通知
            tag = Moment.SERVER_MOMENT_TAG_FOR_NOTICE;
            break;
        case 2:
            // 问答
            tag = Moment.SERVER_MOMENT_TAG_FOR_QA;
            break;
        case 3:
            // 学习
            tag = Moment.SERVER_MOMENT_TAG_FOR_STUDY;
            break;
        case 4:
            // 生活
            tag = Moment.SERVER_MOMENT_TAG_FOR_LIFE;
            break;
        case 5:
            // 投票
            // 此处有两个值，数据库中处理
            tag = Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE;
            break;
        case 6:
            // 视频
            tag = Moment.SERVER_MOMENT_TAG_FOR_VIDEO;
            break;
        default:
            break;
        }
        return tag;
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
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
    public void toastCannotSendMsg() {

    }

    @Override
    public void onHeightChanged(int height) {

    }

    @Override
    public void onTextInputted(String text) {
        if (mInputMgr == null)
            return;
        if (TextUtils.isEmpty(text.trim())) {
            mMsgBox.toast(R.string.comment_content_cannot_empty);
            return;
        }
        if (text.length() > TimelineActivity.COMMENT_MOST_WORDS) {
            mMsgBox.toast(String.format(getString(R.string.moments_comment_oom), TimelineActivity.COMMENT_MOST_WORDS));
            return;
        }
        mInputMgr.setSoftKeyboardVisibility(false);

        String mid = mInputMgr.extra().getString(EXTRA_REPLY_TO_MOMENT_ID);
        Review replyTo = mInputMgr.extra().getParcelable(EXTRA_REPLY_TO_REVIEW);
        if (mid == null)
            return;

        Moment m;
        if (TextUtils.equals(mid, moment.id)) {
            m = moment;
        } else {
            m = dbHelper.fetchMoment(mid);
        }
        if (m == null)
            return;
        doReviewMoment_async(m,
                replyTo == null ? null : replyTo.id,
                text);
        mInputMgr.hide();//评论完后自动关闭输入框
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
    public void onHybirdInputted(String text, String imagePath, String imageThumbPath, String voicePath, int voiceDuration) {
        mMsgBox.show(null, getString(R.string.moments_hybird_not_supported_in_review));
    }

    @Override
    public void onHybirdRequested() {
        mMsgBox.show(null, getString(R.string.moments_hybird_not_supported_in_review));
    }

    @Override
    public void onVideoInputted(String path, String thumbPath) {
        mMsgBox.show(null, getString(R.string.moments_video_not_supported_in_review));
    }

    @Override
    public void willRecordAudio() {

    }

    @Override
    public void onLocationInputted(double latitude, double longitude, String address) {
        mMsgBox.show(null, getString(R.string.moments_loc_not_supported_in_review));
    }

    @Override
    public void onCallRequested() {

    }

    @Override
    public void onVideoChatRequested() {

    }


    /**
     * Params[0] should be max timestamp.
     */
    private class RefreshMomentsTask extends AsyncTask<Long, Void, Integer> {
        long maxTimestamp;

        @Override
        protected Integer doInBackground(Long... params) {
            maxTimestamp = params[0];
            return loadRemoteMoments(maxTimestamp);
        }

        @Override
        protected void onPostExecute(Integer errno) {
            mNoMore = true;
            if (errno == ErrorCode.OK) {
                ArrayList<Moment> lst = loadLocalMoments(maxTimestamp, tagIdxFromUiToDb(selectedTag),countType);
                if (lst != null && !lst.isEmpty()) {
                    fillListView(lst, maxTimestamp > 0);

                    // 如果是刷新，滚到列表顶部
                    if (maxTimestamp == 0 && viewCreated) {
                        if (getPullToRefreshListView() != null) {
                            getPullToRefreshListView().getRefreshableView().setSelection(0);
                        } else {
                            getListView().setSelection(0);
                        }
                    }
                    mNoMore = false;
                }
            } else {
                Toast.makeText(getActivity(), R.string.moments_check_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRefresh(final PullToRefreshBase refreshView) {
        AsyncTaskExecutor.executeShortNetworkTask(new RefreshMomentsTask(){
            @Override
            protected void onPostExecute(Integer errno) {
                super.onPostExecute(errno);
                refreshView.onRefreshComplete();
            }
        }, Long.valueOf(0));
    }

    @Override
    public boolean onLoadMore() {
        AsyncTaskExecutor.executeShortNetworkTask(new RefreshMomentsTask(){
            @Override
            protected void onPostExecute(Integer errno) {
                super.onPostExecute(errno);
                adapter.notifyLoadingCompleted();
            }
        }, maxTimestamp);
        return true;
    }

    @Override
    public boolean noMore() {
        return mNoMore;
    }

    private IDBTableChangeListener momentReviewObserver = new IDBTableChangeListener() {

        public void onDBTableChanged(String tableName) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkNewReviews();
                }
            });
        }
    };

    private void checkNewReviews() {
        Moment dummy = new Moment(null);
        Database mDb = new Database(getActivity());
        int newReviewsCount = 0;
        if (TimelineFragment.newReviewFlag) {//自己
        	newReviewsCount = mDb.fetchNewReviews(dummy);
        }

        if (moment != null) { //删除自己的评论后自动刷新

            fillListView(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag),countType), false);
        }


        adapter.setNewReviewCount(newReviewsCount);
        adapter.notifyDataSetChanged();
    }

    /**
     * Handle back press event.
     * @return true if the event is consumed.
     */
    public boolean handleBackPress() {
        return false;
    }
}
