package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ListView;
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
        implements MomentAdapter.ReplyDelegate, OnTimelineFilterChangedListener,
        PullToRefreshListView.OnRefreshListener, MomentAdapter.LoadDelegate {
    protected static final int PAGE_SIZE = 10;
    private static final int REQ_COMMENT = 123;
    protected Database dbHelper;
    private MomentAdapter adapter;
    // selected tag index on UI
    private int selectedTag = 0;
    // record max timestamp for convenience of loading more
    private long maxTimestamp = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new Database(getActivity());

        if (savedInstanceState != null) {
            selectedTag = savedInstanceState.getInt("selectedTag");
        }

        // load moments
        setupListAdapter(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag)));
        checkNewMoments();
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
    protected abstract ArrayList<Moment> loadLocalMoments(long maxTimestmap, String tag);

    /**
     * Load moments from web server.
     * @param maxTimestamp 0 means now.
     * @return {@link org.wowtalk.api.ErrorCode}
     */
    protected abstract int loadRemoteMoments(long maxTimestamp);

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
            adapter.setShowLoadMoreAsLastItem(!append || lst.size() >= PAGE_SIZE);
            adapter.notifyDataSetChanged();
        } else {
            setupListAdapter(lst);
        }
    }

    private void setupListAdapter(ArrayList<Moment> items) {
        ImageResizer imageResizer = new ImageResizer(getActivity(), DensityUtil.dip2px(getActivity(), 100));
        //获取上下文的listView
//        ListView listView = getListView();
        
        adapter = new MomentAdapter(getActivity(),
                getActivity(),
                items,
                false,
                false,
                imageResizer,
                this,
                null,
                new MessageBox(getActivity()));/////////// 传入listview
        adapter.setShowLoadMoreAsLastItem(!items.isEmpty());
        adapter.setLoadDelegate(this);
        setListAdapter(adapter);
    }

    private void checkNewMoments() {
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
            // TODO refresh views
        }
    }

    @Override
    public void replyToMoment(int position, final Moment moment, Review replyTo, boolean like) {
        if (like) {
            new AsyncTask<String, Void, Integer>() {
                Review r = new Review();
                @Override
                protected Integer doInBackground(String... params) {
                    WowMomentWebServerIF web = WowMomentWebServerIF.getInstance(getActivity());
                    return web.fReviewMoment(params[0], Review.TYPE_LIKE, null, null, r);
                }

                @Override
                protected void onPostExecute(Integer errcode) {
                    if (errcode == ErrorCode.OK) {
                        moment.likedByMe = true;
                        adapter.notifyDataSetChanged();
                    }
                }
            }.execute(moment.id);
        } else {
            startActivityForResult(
                    new Intent(this.getActivity(), MomentDetailActivity.class)
                            .putExtra("moment", moment),
                    REQ_COMMENT
            );
        }
    }

    /**
     * Possible error:
     * java.lang.IllegalStateException: Cannot add header view to list -- setAdapter has already been called.
     */
    protected abstract void setupListHeaderView();

    @Override
    public void onSenderChanged(int index) {
//        Toast.makeText(getActivity(),
//                "sender: " + index, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTagChanged(int index) {
        selectedTag = index;
        fillListView(loadLocalMoments(0, tagIdxFromUiToDb(selectedTag)), false);
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
                ArrayList<Moment> lst = loadLocalMoments(maxTimestamp, tagIdxFromUiToDb(selectedTag));
                fillListView(lst, maxTimestamp > 0);
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