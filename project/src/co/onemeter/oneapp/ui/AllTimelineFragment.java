package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.onemeter.oneapp.R;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.Database;
import org.wowtalk.api.Moment;
import org.wowtalk.api.MomentWebServerIF;

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
    protected ArrayList<Moment> loadLocalMoments(long maxTimestamp, String tag,int countType) {
        return new Database(getActivity()).fetchMomentsOfAllBuddies(maxTimestamp, PAGE_SIZE, tag,countType);
    }

    @Override
    protected int loadRemoteMoments(long maxTimestamp) {
        MomentWebServerIF web = MomentWebServerIF.getInstance(getActivity());
        return web.fGetMomentsOfAll(maxTimestamp, PAGE_SIZE, true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timelineDropdownFilter != null && timelineDropdownFilter.isShowingDialog()) {
            timelineDropdownFilter.tryDismissAll();
        }
    }

    @Override
    protected void setupListHeaderView() {

        Log.i("---运行1:","外层");
//        if ((headerView == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount)
//                && getListAdapter() == null) {

        if (headerView == null ) {

            Log.i("---运行2:","内层");
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

//            headerView.setVisibility(View.GONE);

//            getListView().addHeaderView(headerView);

            if (TimelineActivity.FLAG_ISPUBLIC) {
                headerView.setVisibility(View.GONE);
            } else {
                getListView().addHeaderView(headerView);
            }
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

                if (itemIdx == 0) {//全部
                    MenuBar.getSender().setText("全部");
                } else if (itemIdx == 1) {//官方账号 0
                    MenuBar.getSender().setText("官方账号");
                } else if (itemIdx == 2) {//老师账号2
                    MenuBar.getSender().setText("老师账号");
                } else if (itemIdx == 3) {//学生账号1
                    MenuBar.getSender().setText("学生账号");
                }

                onSenderChanged(itemIdx);

                break;

            case R.id.btn_cat:
            	//0全部，1通知，2问答，3学习，4生活，5，投票，6视频
            	//由于新需求的变化，无论是老师账号还是学生账号都去除掉通知和问答的栏目
            	//改变后：0全部，1学习，2生活，3，投票，4视频
            	if (itemIdx >= 1) {
            		itemIdx += 2;
            	}

                if (itemIdx == 0) {//
                    MenuBar.getCat().setText("全部");
                } else if (itemIdx == 1) {
                    MenuBar.getCat().setText("通知");
                } else if (itemIdx == 2) {
                    MenuBar.getCat().setText("问答");
                } else if (itemIdx == 3) {
                    MenuBar.getCat().setText("学习");
                }else if (itemIdx == 4) {
                    MenuBar.getCat().setText("生活");
                }else if (itemIdx == 5) {
                    MenuBar.getCat().setText("投票");
                } else if (itemIdx == 6) {
                    MenuBar.getCat().setText("视频");
                }
            	
                onTagChanged(itemIdx);
                break;
        }
    }

    @Override
    public boolean handleBackPress() {
        if (timelineDropdownFilter != null && timelineDropdownFilter.isShowingDialog()) {
            timelineDropdownFilter.tryDismissAll();
            return true;
        }
        return false;
    }
}
