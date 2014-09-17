package com.wowtech.DraggableListView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-12
 * Time: 上午9:34
 * To change this template use File | Settings | File Templates.
 */
public class ScrollOverListView extends ListView {

    private int mLastY;
    private int mTopPosition;
    private int mBottomPosition;

    private ListAdapter contentAdapter;

    public ScrollOverListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ScrollOverListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScrollOverListView(Context context) {
        super(context);
        init();
    }

    private void init(){
        mTopPosition = 0;
        mBottomPosition = 0;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);

        contentAdapter=adapter;
    }

    public ListAdapter getContentAdapter() {
        return contentAdapter;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int y = (int) ev.getRawY();

        switch(action){
            case MotionEvent.ACTION_DOWN:{
                mLastY = y;
                final boolean isHandled = mOnScrollOverListener.onMotionDown(ev);
                if (isHandled) {
                    mLastY = y;
                    return isHandled;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE:{
                final int childCount = getChildCount();
                final int deltaY = y - mLastY;

                if(0 == childCount) {
                    mOnScrollOverListener.onMotionMove(ev, deltaY);
                    if(deltaY > 0) {
                        boolean handled;
                        handled = mOnScrollOverListener.onListViewTopAndPullDown(deltaY);
                        if(handled){
                            mLastY = y;
                            return true;
                        }
                    }
                } else {
                    final int itemCount = getAdapter().getCount() - mBottomPosition;
                    //DLog.d("lastY=%d y=%d", mLastY, y);

                    final int firstTop = getChildAt(0).getTop();
                    final int listPadding = getListPaddingTop();

                    final int lastBottom = getChildAt(childCount - 1).getBottom();
                    final int end = getHeight() - getPaddingBottom();

                    final int firstVisiblePosition = getFirstVisiblePosition();

                    final boolean isHandleMotionMove = mOnScrollOverListener.onMotionMove(ev, deltaY);

                    if(isHandleMotionMove){
                        mLastY = y;
                        return true;
                    }

                    //DLog.d("firstVisiblePosition=%d firstTop=%d listPaddingTop=%d deltaY=%d", firstVisiblePosition, firstTop, listPadding, deltaY);
                    if (firstVisiblePosition <= mTopPosition && firstTop >= listPadding && deltaY > 0) {
                        final boolean isHandleOnListViewTopAndPullDown;
                        isHandleOnListViewTopAndPullDown = mOnScrollOverListener.onListViewTopAndPullDown(deltaY);
                        if(isHandleOnListViewTopAndPullDown){
                            mLastY = y;
                            return true;
                        }
                    }

                    // DLog.d("lastBottom=%d end=%d deltaY=%d", lastBottom, end, deltaY);
                    if (firstVisiblePosition + childCount >= itemCount && lastBottom <= end && deltaY < 0) {
                        final boolean isHandleOnListViewBottomAndPullDown;
                        isHandleOnListViewBottomAndPullDown = mOnScrollOverListener.onListViewBottomAndPullUp(deltaY);
                        if(isHandleOnListViewBottomAndPullDown){
                            mLastY = y;
                            return true;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:{
                final boolean isHandlerMotionUp = mOnScrollOverListener.onMotionUp(ev);
                if (isHandlerMotionUp) {
                    mLastY = y;
                    return true;
                }
                break;
            }
        }

        mLastY = y;
        return super.onTouchEvent(ev);
    }

    /**空的*/
    private OnScrollOverListener mOnScrollOverListener = new OnScrollOverListener(){

        @Override
        public boolean onListViewTopAndPullDown(int delta) {
            return false;
        }

        @Override
        public boolean onListViewBottomAndPullUp(int delta) {
            return false;
        }

        @Override
        public boolean onMotionDown(MotionEvent ev) {
            return false;
        }

        @Override
        public boolean onMotionMove(MotionEvent ev, int delta) {
            return false;
        }

        @Override
        public boolean onMotionUp(MotionEvent ev) {
            return false;
        }

    };

    // =============================== public method ===============================

    /**
     * 可以自定义其中一个条目为头部，头部触发的事件将以这个为准，默认为第一个
     *
     * @param index 正数第几个，必须在条目数范围之内
     */
    public void setTopPosition(int index){
        if(getAdapter() == null || index < 0) {
            Log.e("ScrollOverListView","setTopPosition fail "+index);
        } else {
            mTopPosition = index;
        }
    }

    /**
     * 可以自定义其中一个条目为尾部，尾部触发的事件将以这个为准，默认为最后一个
     *
     * @param index 倒数第几个，必须在条目数范围之内
     */
    public void setBottomPosition(int index){
        if(getAdapter() == null || index < 0) {
            Log.e("ScrollOverListView","setBottomPosition fail "+index);
        } else {
            mBottomPosition = index;
        }
    }

    /**
     * 设置这个Listener可以监听是否到达顶端，或者是否到达低端等事件</br>
     *
     * @see OnScrollOverListener
     */
    public void setOnScrollOverListener(OnScrollOverListener onScrollOverListener){
        mOnScrollOverListener = onScrollOverListener;
    }

    /**
     * 滚动监听接口</br>
     * @see ScrollOverListView#setOnScrollOverListener(OnScrollOverListener)
     *
     */
    public interface OnScrollOverListener {

        /**
         * 到达最顶部触发
         *
         * @param delta 手指点击移动产生的偏移量
         * @return
         */
        boolean onListViewTopAndPullDown(int delta);

        /**
         * 到达最底部触发
         *
         * @param delta 手指点击移动产生的偏移量
         * @return
         */
        boolean onListViewBottomAndPullUp(int delta);

        /**
         * 手指触摸按下触发，相当于{@link android.view.MotionEvent#ACTION_DOWN}
         *
         * @return 返回true表示自己处理
         * @ see View-onTouchEvent(MotionEvent)
         */
        boolean onMotionDown(MotionEvent ev);

        /**
         * 手指触摸移动触发，相当于{@link android.view.MotionEvent#ACTION_MOVE}
         *
         * @return 返回true表示自己处理
         * @ see View onTouchEvent(MotionEvent)
         */
        boolean onMotionMove(MotionEvent ev, int delta);

        /**
         * 手指触摸后提起触发，相当于{@link android.view.MotionEvent#ACTION_UP}
         *
         * @return 返回true表示自己处理
         * @ see View#onTouchEvent(MotionEvent)
         */
        boolean onMotionUp(MotionEvent ev);

    }

}
