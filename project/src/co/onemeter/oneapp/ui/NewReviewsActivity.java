package co.onemeter.oneapp.ui;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.NewReviewAdapter;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-8
 * Time: AM10:32
 * To change this template use File | Settings | File Templates.
 */
public class NewReviewsActivity extends ListActivity implements View.OnClickListener {
    public static final String EXTRA_KEY_HOSTTYPE = "host-type";
    public static final int EXTRA_VALUE_HOSTTYPE_MOMENT = 0;
    public static final int EXTRA_VALUE_HOSTTYPE_EVENT = 1;

    private int mHostType = EXTRA_VALUE_HOSTTYPE_MOMENT;
    private NewReviewAdapter mAdapter;
    private Database mDb;

    private TextView tvDel;
    private MessageBox mMsgBox;
    private int reviewConfirmReadCount;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reviews);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        findViewById(R.id.title_back).setOnClickListener(this);

        mHostType = getIntent().getIntExtra(EXTRA_KEY_HOSTTYPE, EXTRA_VALUE_HOSTTYPE_MOMENT);

        mDb = Database.open(this);

        mMsgBox = new MessageBox(this);

        tvDel=(TextView) findViewById(R.id.title_del);
        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMsgBox.showWait();

                setAllReviewReaded();
            }
        });

        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
    }

    private void setAllReviewReaded() {
        if(null != mAdapter) {
            //set all reviews readed
            int count=mAdapter.getCount();
            reviewConfirmReadCount=count;

            for(int i=0; i<count; ++i) {
                Review r = mAdapter.getItem(i);
                if (r == null)
                    return;

                Moment m = mDb.fetchMoment(r.hostId);

                setMomentReviewsReaded(m,r.id);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Database.removeDBTableChangeListener(momentReviewObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupNewReviewList();

        setResult(RESULT_OK);
    }

    private IDBTableChangeListener momentReviewObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupNewReviewList();
                }
            });
        }
    };

    private void setupNewReviewList() {
        List<Review> reviews = null;

        // query unread reviews
        if (mHostType == EXTRA_VALUE_HOSTTYPE_MOMENT) {
            Moment dummy = new Moment(null);
            if (mDb.fetchNewReviews(dummy) > 0) {
                reviews = dummy.reviews;
            }
        } else if (mHostType == EXTRA_VALUE_HOSTTYPE_EVENT) {
            WEvent dummy = new WEvent();
            if (mDb.fetchNewReviews(dummy) > 0) {
                reviews = dummy.reviews;
            }
        }

        if(null == reviews || reviews.size() == 0) {
            tvDel.setVisibility(View.INVISIBLE);
            mMsgBox.dismissWait();
        } else {
            tvDel.setVisibility(View.VISIBLE);
        }

        // refresh list view
        if (mAdapter != null) {
            // update adapter
            mAdapter.clear();
            if (reviews != null && !reviews.isEmpty()) {
                for(Review r : reviews)
                    mAdapter.add(r);
            }
            mAdapter.notifyDataSetChanged();
        } else if (reviews != null && !reviews.isEmpty()) {
            // setup adapter
            mAdapter = new NewReviewAdapter(this, reviews);
            setListAdapter(mAdapter);
        }
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Review r = mAdapter.getItem(position);
        if (r == null)
            return;

        Moment m = mDb.fetchMoment(r.hostId);

        //anyway set this review readed even if moment is not exist
        setMomentReviewsReaded(m,r.id);

        if (m == null)
            return;

        MomentDetailActivity.launch(this, m);
//        TimelineFragment.launch(this, m);
//        TimelineFragment.launch(this, m);


    }

    private void setMomentReviewsReaded(Moment m,final String reviewId) {
        if(null == m) {
            Log.w("set moment reviews readed,moment null,set specific review readed");
            mDb.setSpecificReviewReaded(reviewId);

            new AsyncTask<Moment, Void, Void>() {
                @Override
                protected Void doInBackground(Moment... moments) {
//                MomentActivity.requestCheckNewReviews();
                    MomentWebServerIF.getInstance(NewReviewsActivity.this).fSetReviewRead(reviewId);
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    --reviewConfirmReadCount;
                    if(reviewConfirmReadCount <= 0) {
                        mMsgBox.dismissWait();
                    }
                }
            }.execute(m);
        } else {
            Log.i("set all moment reviews readed");
            mDb.setReviewsRead(m);

            new AsyncTask<Moment, Void, Void>() {
                @Override
                protected Void doInBackground(Moment... moments) {
//                MomentActivity.requestCheckNewReviews();
                    MomentWebServerIF.getInstance(NewReviewsActivity.this).fSetReviewRead(moments[0]);
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    --reviewConfirmReadCount;
                    if(reviewConfirmReadCount <= 0) {
                        mMsgBox.dismissWait();
                    }
                }
            }.execute(m);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
        }
    }
}