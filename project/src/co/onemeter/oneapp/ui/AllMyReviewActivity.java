package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.NewReviewAdapter;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-8-5
 * Time: 上午9:42
 * To change this template use File | Settings | File Templates.
 */
public class AllMyReviewActivity extends Activity implements View.OnClickListener{
    private MessageBox mMsgBox;
    private Database mDb;
    private ListView lvReviewList;

    private int lastItemIndex;
    private int topOffset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        setContentView(R.layout.all_my_review_layout);

        mDb = Database.open(this);
        mMsgBox = new MessageBox(this);

        findViewById(R.id.title_back).setOnClickListener(this);
        setupReviewList();

        Database.addDBTableChangeListener(Database.TBL_MOMENT,momentObserver);
    }

    private IDBTableChangeListener momentObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupReviewList();
                }
            });
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        Database.removeDBTableChangeListener(momentObserver);
    }

    private void setupReviewList() {
        lvReviewList=(ListView) findViewById(R.id.review_list);
        lvReviewList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Review r = ((NewReviewAdapter)lvReviewList.getAdapter()).getItem(position);
                MomentDetailActivity.launch(AllMyReviewActivity.this, mDb.fetchMoment(r.hostId));
//                TimelineFragment.launch(AllMyReviewActivity.this, mDb.fetchMoment(r.hostId));
            }
        });

        lvReviewList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                lastItemIndex=lvReviewList.getFirstVisiblePosition();
                View v = lvReviewList.getChildAt(0);
                topOffset = (v == null) ? 0 : v.getTop();
            }
        });

        //load reviews
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, ArrayList<Moment>>() {
            @Override
            protected ArrayList<Moment> doInBackground(Void... params) {
                return mDb.fetchMomentsOfSingleBuddy(
                        PrefUtil.getInstance(AllMyReviewActivity.this).getUid(),
                        -1, -1, -1);
            }

            @Override
            protected void onPostExecute(ArrayList<Moment> momentArrayList) {
                mMsgBox.dismissWait();

                List<Review> allReviewList = new LinkedList<Review>();
                for (Moment aMoment : momentArrayList) {
                    allReviewList.addAll(aMoment.reviews);
                }

                if (0 == allReviewList.size()) {
                    findViewById(R.id.no_reviews_indicator).setVisibility(View.VISIBLE);
                    lvReviewList.setVisibility(View.GONE);
                } else {
                    findViewById(R.id.no_reviews_indicator).setVisibility(View.GONE);
                    lvReviewList.setVisibility(View.VISIBLE);

                    NewReviewAdapter mAdapter = new NewReviewAdapter(AllMyReviewActivity.this, allReviewList);

                    lvReviewList.setAdapter(mAdapter);

                    lvReviewList.setSelectionFromTop(lastItemIndex, topOffset);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.title_back:
                finish();
                break;
        }
    }
}
