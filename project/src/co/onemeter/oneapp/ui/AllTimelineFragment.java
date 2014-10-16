package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import com.androidquery.AQuery;
import org.wowtalk.api.Database;
import org.wowtalk.api.Moment;
import org.wowtalk.api.Review;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * <p>浏览所有人的动态。</p>
 * Created by pzy on 10/13/14.
 */
public class AllTimelineFragment extends ListFragment implements MomentAdapter.ReplyDelegate {

    View dialogBackground;
    MomentAdapter adapter;
    Database dbHelper;
    private ArrayList<Moment> moments;
    private View headerView;
    private int originalHeaderViewsCount = 0;

    private TimelineFilterOnClickListener.OnFilterChangedListener onMomentSenderChangedListener =
            new TimelineFilterOnClickListener.OnFilterChangedListener() {
                @Override
                public void onSenderChanged(int index) {
                    Toast.makeText(AllTimelineFragment.this.getActivity(),
                            "sender: " + index, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCategoryChanged(int index) {
                    Toast.makeText(AllTimelineFragment.this.getActivity(),
                            "category: " + index, Toast.LENGTH_SHORT).show();
                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_timeline, container, false);
        dialogBackground = view.findViewById(R.id.dialog_container);
        dialogBackground.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new Database(getActivity());
        moments = dbHelper.fetchMomentsOfAllBuddies(-1, 20);
        ImageResizer imageResizer = new ImageResizer(getActivity(), DensityUtil.dip2px(getActivity(), 100));
        adapter = new MomentAdapter(getActivity(),
                getActivity(),
                moments,
                false,
                false,
                imageResizer,
                this,
                null,
                new MessageBox(getActivity()));
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup list header view
        if (headerView == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount) {
            originalHeaderViewsCount = getListView().getHeaderViewsCount();
            headerView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.timeline_filter, null);
            getListView().addHeaderView(headerView);
            AQuery q = new AQuery(headerView);
            TimelineFilterOnClickListener clickListener = new TimelineFilterOnClickListener(
                    dialogBackground,
                    headerView,
                    headerView.findViewById(R.id.btn_sender),
                    headerView.findViewById(R.id.btn_cat)
            );
            clickListener.setOnFilterChangedListener(onMomentSenderChangedListener);
            q.find(R.id.btn_sender).clicked(clickListener);
            q.find(R.id.btn_cat).clicked(clickListener);
        }
    }

    @Override
    public void replyToMoment(int position, String momentId, Review replyTo) {
        new MessageBox(getActivity()).show(null, getString(R.string.not_implemented));
    }

}
