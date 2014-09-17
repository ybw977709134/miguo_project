package com.wowtech.DraggableListView;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-12
 * Time: 上午11:20
 * To change this template use File | Settings | File Templates.
 */
public abstract class AutoDragActivity extends Activity implements PullDownView.OnPullDownListener, AdapterView.OnItemClickListener {
    private ListView mListView;

    private PullDownView mPullDownView;
    private LinearLayout emptyContentLayout;

    private View mHeaderLoadingView;
    private Animation refreshingAnim;
    private Animation refreshingFinishAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pulldown);

        initView();
    }

    private void initView() {
        /*
         * 1.使用PullDownView
         * 2.设置OnPullDownListener
         * 3.从mPullDownView里面获取ListView
         */
        mPullDownView = (PullDownView) findViewById(R.id.pull_down_view);
        mPullDownView.setOnPullDownListener(this);

        emptyContentLayout = (LinearLayout)findViewById(R.id.content_empty_item_indicator_layout);

        mListView = mPullDownView.getListView();

        mListView.setOnItemClickListener(this);

        mHeaderLoadingView = findViewById(R.id.pulldown_header_loading);

        refreshingAnim = AnimationUtils.loadAnimation(this, R.anim.content_refresh);
        refreshingAnim.setInterpolator(new LinearInterpolator());
        mHeaderLoadingView.startAnimation(refreshingAnim);

        refreshingFinishAnim=AnimationUtils.loadAnimation(this, R.anim.content_refresh_finish);
        refreshingFinishAnim.setInterpolator(new LinearInterpolator());
        refreshingFinishAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mHeaderLoadingView.clearAnimation();
                mHeaderLoadingView.setVisibility(View.GONE);

                mPullDownView.notifyDidRefresh();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void updateContentStatus() {
        if(mListView.getAdapter().getCount() > 0) {
            emptyContentLayout.setVisibility(View.GONE);
        } else {
            emptyContentLayout.setVisibility(View.VISIBLE);
        }
    }

    public void triggerRefresh() {
        mPullDownView.triggerRefresh(1);
    }

    public void setEmptyContentView(View view) {
        emptyContentLayout.addView(view);
    }

    public void setContentListViewAttr(Drawable dividerDrawable,int dividerHeight) {
        if(null != dividerDrawable) {
            mListView.setDivider(dividerDrawable);
        }

        if(dividerHeight > 0) {
            mListView.setDividerHeight(dividerHeight);
        }
    }

    public void setContentListViewAdapter(ListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    public void triggerAutoLoadMore() {
        if(null == mListView.getAdapter()) {
            Log.e("AutoDragActivity","trigger auto load more should be called after list adapter set");
        } else {
            mPullDownView.enableAutoFetchMore(true, 1);
        }
    }

    public void notifyLoadDataFinish() {
        mHeaderLoadingView.clearAnimation();
        mHeaderLoadingView.setVisibility(View.GONE);
        updateContentStatus();

        mPullDownView.notifyDidLoad();
    }

    public void notifyRefreshFinish() {
        mHeaderLoadingView.clearAnimation();
        mHeaderLoadingView.setVisibility(View.VISIBLE);
        mHeaderLoadingView.startAnimation(refreshingFinishAnim);

        updateContentStatus();
    }

    public void notifyLoadMoreFinish() {
        mPullDownView.notifyDidMore();

        updateContentStatus();
    }

    @Override
    public void onRefresh() {
        mHeaderLoadingView.clearAnimation();
        mHeaderLoadingView.setVisibility(View.VISIBLE);
        mHeaderLoadingView.startAnimation(refreshingAnim);

        onRefreshHandle();
    }

    @Override
    public void onMore() {
        onMoreHandle();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onItemClickedHandle(parent,view,position,id);
    }

    public abstract void onRefreshHandle();
    public abstract void onMoreHandle();
    public abstract void onItemClickedHandle(AdapterView<?> parent, View view, int position, long id);
}
