package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * <p>浏览动态。</p>
 * Created by pzy on 10/13/14.
 */
public abstract class TimelineFragment extends ListFragment
        implements MomentAdapter.MomentActionHandler, OnTimelineFilterChangedListener,
        PullToRefreshListView.OnRefreshListener, MomentAdapter.LoadDelegate  {
    protected static final int PAGE_SIZE = 10;
    private static final int REQ_COMMENT = 123;
    protected Database dbHelper;
    private MomentAdapter adapter;

    
    // selected tag index on UI
    private int selectedTag = 0;
    // record max timestamp for convenience of loading more
    private long maxTimestamp = 0;
    
    //用于区分账号的类型
    private int countType = -1;//默认全部
    private boolean viewCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        dbHelper = new Database(getActivity());

        if (savedInstanceState != null) {
            selectedTag = savedInstanceState.getInt("selectedTag");
        }

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
        new RefreshMomentsTask().execute(Long.valueOf(0));
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
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        Database.removeDBTableChangeListener(momentReviewObserver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_COMMENT && resultCode == Activity.RESULT_OK) {
            // refresh views
            boolean handled = false;
            if (data != null) {
                String changedMomentId = data.getStringExtra(MomentDetailActivity.EXTRA_CHANGED_MOMENT_ID);
                if (changedMomentId != null) {
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
                new RefreshMomentsTask(){
                    @Override
                    protected void onPostExecute(Integer errno) {
                        super.onPostExecute(errno);
                    }
                }.execute(Long.valueOf(0));
            }
        }
    }
    
    /**
     * 点赞和评论的局部刷新
     */
    @Override
    public void replyToMoment(int position, final Moment moment, Review replyTo, boolean like) {
        if (like) {
            new AsyncTask<String, Void, Integer>() {
                Review r = new Review(); // 添加或删除的赞
                @Override
                protected Integer doInBackground(String... params) {
                    MomentWebServerIF web = MomentWebServerIF.getInstance(getActivity());
                    if (!moment.likedByMe) { // 点赞
                        return web.fReviewMoment(params[0], Review.TYPE_LIKE, null, null, r);
                    } else { // 撤销赞
                        Review likeReview=null;
                        String mMyUid = PrefUtil.getInstance(getActivity()).getUid();
                        for(Review aReview : moment.reviews) {
                            if(Review.TYPE_LIKE == aReview.type && aReview.uid.equals(mMyUid)) {
                                likeReview=aReview;
                                break;
                            }
                        }
                        if(null != likeReview) {
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
                        dbHelper.storeMoment(moment, moment.id);
                        adapter.notifyDataSetChanged();
                    }
                }
            }.execute(moment.id);
        } else {
        	
//        	String mMyUid = PrefUtil.getInstance(this.getActivity()).getUid();
//            if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && moment.owner.userID.equals(mMyUid)) {//跳转到自己的详情页
//            	TimelineFragment.launchForOwnerComment(getActivity(), moment);
//                
//            } else if (!moment.owner.userID.equals(mMyUid)) {//跳转到好友的详情页
//            	TimelineFragment.launchComment(getActivity(), moment);
//            	
//            }
            
            startActivityForResult(
                    new Intent(this.getActivity(), MomentDetailActivity.class)
                            .putExtra("moment", moment),
                    REQ_COMMENT
            );
        }
    }
    
//    /**
//     * 跳转到好友
//     * @param context
//     * @param moment
//     */
//    public static void launch(Context context, Moment moment) {
//        Intent intent = new Intent(context, MomentDetailActivity.class);
//        intent.putExtra("moment", moment);
//        ((Activity) context).startActivityForResult(intent, REQ_COMMENT);
//    }
//    
//    /**
//     * 跳转到自己
//     * @param context
//     * @param moment
//     */
//    public static void launchForOwner(Context context, Moment moment) {
//        Intent intent = new Intent(context, MomentDetailActivity.class);
//        intent.putExtra("moment", moment);
//        intent.putExtra("isowner", 1);//给自己多传一个标志值   
//        ((Activity) context).startActivityForResult(intent, REQ_COMMENT);
//    }
    
//    /**
//     * 点击评论按钮跳转到好友
//     * @param context
//     * @param moment
//     */
//    public static void launchComment(Context context, Moment moment) {
//        Intent intent = new Intent(context, MomentDetailActivity.class);
//        intent.putExtra("moment", moment);
//        intent.putExtra("comment", 1);//这是点击评论按钮跳转到详情页
//        ((Activity) context).startActivityForResult(intent, REQ_COMMENT);
//    }
//    
//    /**
//     * 点击评论按钮跳转到自己
//     * @param context
//     * @param moment
//     */
//    public static void launchForOwnerComment(Context context, Moment moment) {
//        Intent intent = new Intent(context, MomentDetailActivity.class);
//        intent.putExtra("moment", moment);
//        intent.putExtra("isowner", 1);//给自己多传一个标志值
//        intent.putExtra("comment", 1);//这是点击评论按钮跳转到详情页
//        ((Activity) context).startActivityForResult(intent, REQ_COMMENT);
//    }

    public void onMomentClicked(int position, Moment moment) {

        Activity context = getActivity();

        Intent intent = new Intent(this.getActivity(), MomentDetailActivity.class)
                .putExtra("moment", moment);

        String mMyUid = PrefUtil.getInstance(context).getUid();
        if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && moment.owner.userID.equals(mMyUid)) {
            intent.putExtra("isowner", 1);//给自己多传一个标志值
        }

        // 用户可能在详情页点赞或评论，或删除动态，总之可那需要在 onActivityResult 中刷新UI
        startActivityForResult(intent, REQ_COMMENT);
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
                }
            } else {
                Toast.makeText(getActivity(), R.string.moments_check_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRefresh(final PullToRefreshBase refreshView) {
        new RefreshMomentsTask(){
            @Override
            protected void onPostExecute(Integer errno) {
                super.onPostExecute(errno);
                refreshView.onRefreshComplete();
            }
        }.execute(Long.valueOf(0));
    }

    @Override
    public boolean onLoadMore() {
        new RefreshMomentsTask(){
            @Override
            protected void onPostExecute(Integer errno) {
                super.onPostExecute(errno);
                adapter.notifyLoadingCompleted();
            }
        }.execute(maxTimestamp);
        return true;
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
        int newReviewsCount = mDb.fetchNewReviews(dummy);
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
