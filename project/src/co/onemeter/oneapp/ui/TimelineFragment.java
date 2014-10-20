package co.onemeter.oneapp.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.Moment;
import org.wowtalk.api.Review;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * <p>浏览我发布的动态。</p>
 * Created by pzy on 10/13/14.
 */
public abstract class TimelineFragment extends ListFragment implements MomentAdapter.ReplyDelegate, TimelineFilterOnClickListener.OnFilterChangedListener {
    protected Database dbHelper;
    private MomentAdapter adapter;
    private int selectedTag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new Database(getActivity());

        // load moments
        setupListAdapter(loadLocalMoments());
        checkNewMoments();
    }

    protected abstract ArrayList<Moment> loadLocalMoments();

    /**
     * Load moments from web server.
     * @return {@link org.wowtalk.api.ErrorCode}
     */
    protected abstract int loadRemoteMoments();

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
        setListAdapter(adapter);
    }

    private void checkNewMoments() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return loadRemoteMoments();
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    ArrayList<Moment> lst = loadLocalMoments();
                    if (adapter != null) {
                        adapter.clear();
                        adapter.addAll(lst);
                        adapter.notifyDataSetChanged();
                    } else {
                        setupListAdapter(lst);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.moments_check_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute((Void)null);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListHeaderView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void replyToMoment(int position, String momentId, Review replyTo) {
        new MessageBox(getActivity()).show(null, getString(R.string.not_implemented));
    }

    protected abstract void setupListHeaderView();

    @Override
    public void onSenderChanged(int index) {

    }

    @Override
    public void onCategoryChanged(int index) {
        selectedTag = index;
        Toast.makeText(getActivity(), "tag: " + index, Toast.LENGTH_SHORT).show();
    }
}