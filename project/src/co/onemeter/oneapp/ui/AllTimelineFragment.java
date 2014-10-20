package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.Moment;
import org.wowtalk.api.WowMomentWebServerIF;

import java.util.ArrayList;

/**
 * <p>浏览所有人的动态。</p>
 * Created by pzy on 10/13/14.
 */
public class AllTimelineFragment extends TimelineFragment {

    private View dialogBackground;
    private View headerView;
    private int originalHeaderViewsCount = 0;
    private TimelineFilterOnClickListener timelineFilterOnClickListener;

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
    }

    @Override
    protected ArrayList<Moment> loadLocalMoments(int tag) {
        return dbHelper.fetchMomentsOfAllBuddies(0, 20, tag);
    }

    @Override
    protected int loadRemoteMoments() {
        WowMomentWebServerIF web = WowMomentWebServerIF.getInstance(getActivity());
        return web.fGetMomentsOfAll(0, 20, true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timelineFilterOnClickListener.isShowingDialog()) {
            timelineFilterOnClickListener.tryDismissAll();
        }
    }

    @Override
    protected void setupListHeaderView() {
        if (headerView == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount) {
            originalHeaderViewsCount = getListView().getHeaderViewsCount();
            headerView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.timeline_filter, null);
            getListView().addHeaderView(headerView);
            AQuery q = new AQuery(headerView);
            timelineFilterOnClickListener = new TimelineFilterOnClickListener(
                    dialogBackground,
                    headerView,
                    headerView.findViewById(R.id.btn_sender),
                    headerView.findViewById(R.id.btn_cat)
            );
            timelineFilterOnClickListener.setOnFilterChangedListener(this);
            q.find(R.id.btn_sender).clicked(timelineFilterOnClickListener);
            q.find(R.id.btn_cat).clicked(timelineFilterOnClickListener);
        }
    }
}
