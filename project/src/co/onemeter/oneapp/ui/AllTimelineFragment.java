package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.onemeter.oneapp.R;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.Moment;
import org.wowtalk.api.WowMomentWebServerIF;

import java.util.ArrayList;

/**
 * <p>浏览所有人的动态。</p>
 * Created by pzy on 10/13/14.
 */
public class AllTimelineFragment extends TimelineFragment implements MenuBar.OnDropdownMenuItemClickListener {

    private View dialogBackground;
    private View headerView;
    private int originalHeaderViewsCount = 0;
    private MenuBar timelineDropdownFilter;
    private PullToRefreshListView ptrListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);
        ptrListView = (PullToRefreshListView) view.findViewById(R.id.ptr_list);
        dialogBackground = view.findViewById(R.id.dialog_container);
        dialogBackground.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected ArrayList<Moment> loadLocalMoments(String tag) {
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
        if (timelineDropdownFilter.isShowingDialog()) {
            timelineDropdownFilter.tryDismissAll();
        }
    }

    @Override
    protected void setupListHeaderView() {
        if (headerView == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount) {
            originalHeaderViewsCount = getListView().getHeaderViewsCount();
            timelineDropdownFilter =
                    new MenuBar(getActivity(),
                            R.layout.timeline_filter,
                            new int[]{R.id.btn_sender, R.id.btn_cat},
                            dialogBackground) {
                        @Override
                        protected String[] getSubItems(int subMenuResId) {
                            switch (subMenuResId) {
                                case R.id.btn_sender:
                                    return getResources().getStringArray(R.array.timeline_senders);
                                case R.id.btn_cat:
                                    return getResources().getStringArray(R.array.timeline_categories);
                            }
                            return new String[0];
                        }
                    };
            headerView = timelineDropdownFilter.getView();
            getListView().addHeaderView(headerView);
            timelineDropdownFilter.setOnFilterChangedListener(this);
        }
    }

    @Override
    protected PullToRefreshListView getPullToRefreshListView() {
        return ptrListView;
    }

    @Override
    public void onDropdownMenuShow(int subMenuResId) {
    }

    @Override
    public void onDropdownMenuItemClick(int subMenuResId, int itemIdx) {
        switch (subMenuResId) {
            case R.id.btn_sender:
                onSenderChanged(itemIdx);
                break;
            case R.id.btn_cat:
                onTagChanged(itemIdx);
                break;
        }
    }
}
