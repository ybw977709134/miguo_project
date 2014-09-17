package org.wowtalk.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import org.wowtalk.api.Utils;
import co.onemeter.oneapp.R;


/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 4/16/13
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class BottomButtonBoard {
    public static final int BUTTON_BLUE = 0;
    public static final int BUTTON_RED = 1;

    private Context mContext;

    private PopupWindow popupWindow;
    private View view;
    private View parent;
    private LinearLayout layout;
    private RelativeLayout mMainLayout;

    /**
     * 除"cancel"之外的其他的菜单按钮，主要用于更改菜单的背景图片
     */
    private List<Button> mBtns = new ArrayList<Button>();

    public BottomButtonBoard(Context context, View parentView) {
        mContext = context;
        parent = parentView;
        view = LayoutInflater.from(mContext).inflate(R.layout.bottom_button_board, null);
        mMainLayout = (RelativeLayout) view.findViewById(R.id.relativeLayout);
        layout = (LinearLayout) view.findViewById(R.id.layout);
//        popupWindow = new PopupWindow(view, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
        popupWindow = Utils.getFixedPopupWindow(view, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
        ColorDrawable cd = new ColorDrawable(Color.parseColor("#be38313a"));
        popupWindow.setBackgroundDrawable(cd);
        popupWindow.setAnimationStyle(android.R.style.Animation_Toast);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
    }

//    public void setTitle(String title, int visibility) {
//        txtTitle.setVisibility(visibility);
//        txtTitle.setText(title);
//    }

    /**
     * Add button.
     * @param text
     * @param btnStyle BottomButtonBoard.BUTTON_* constants
     * @param listener
     */
    public void add(String text, int btnStyle, View.OnClickListener listener) {
        Resources res = mContext.getResources();
        Button button = new Button(mContext);
        switch (btnStyle) {
        case BUTTON_BLUE:
            button.setTextColor(res.getColor(R.color.menu_color_blue));
            break;
        case BUTTON_RED:
            button.setTextColor(res.getColor(R.color.red));
            break;
        default:
            break;
        }
        button.setText(text);
        button.setSingleLine();
        button.setEllipsize(TruncateAt.MIDDLE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        button.setOnClickListener(listener);
        layout.addView(button);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)button.getLayoutParams();
        params.leftMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_margin);
        params.rightMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_margin);
        params.topMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_divider);
        params.height = res.getDimensionPixelSize(R.dimen.bottom_board_btn_height);
        button.setLayoutParams(params);
        mBtns.add(button);
    }

    public BottomButtonBoard add(String[] texts, int[] btnStyle, View.OnClickListener[] listeners){
        for (int i = 0; i < listeners.length; i++) {
            add(texts[i], btnStyle[i], listeners[i]);
        }
        return this;
    }

    public void addCancelBtn(String text) {
        Resources res = mContext.getResources();
        Button button = new Button(mContext);
        button.setBackgroundResource(R.drawable.menu_view_full_selector);
        button.setTextColor(res.getColor(R.color.menu_color_blue));
        button.setText(text);
        button.setSingleLine();
        button.setEllipsize(TruncateAt.MIDDLE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        layout.addView(button);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)button.getLayoutParams();
        params.leftMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_margin);
        params.rightMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_margin);
        params.topMargin = res.getDimensionPixelSize(R.dimen.bottom_board_btn_top_margin);
        params.height = res.getDimensionPixelSize(R.dimen.bottom_board_btn_height);
        button.setLayoutParams(params);
    }

    public void clearView() {
        layout.removeAllViews();
        mBtns.clear();
    }

    public void show() {
        if (popupWindow.isShowing())
            return;

        // 调整菜单按钮的样式
        updateBtnStyles();
        mBtns.clear();

        final int[] totalHeight = new int[1];
        final int[] viewHeight = new int[1];
        mMainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                totalHeight[0] = mMainLayout.getMeasuredHeight();
                viewHeight[0] = layout.getMeasuredHeight();
            }
        });
        popupWindow.showAtLocation(parent, Gravity.BOTTOM, 0, 0);
        popupWindow.update();
        mMainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getY() < (totalHeight[0] - viewHeight[0])) {
                    dismiss();
                }
                return false;
            }
        });
    }

    /**
     * 调整除"cancel"之外的其他菜单的背景图片
     * 没有菜单时，不做处理；
     * 只有一个菜单时，使用圆角背景图；
     * 只有两个菜单时，第一个使用顶部圆角背景图，第二个使用底部圆角背景图；
     * 其他情况，第一个使用顶部圆角背景图，最后一个使用底部圆角背景图，其他使用没有圆角的背景图
     */
    private void updateBtnStyles() {
        if (mBtns.isEmpty()) {
            return;
        }

        switch (mBtns.size()) {
        case 1:
            mBtns.get(0).setBackgroundResource(R.drawable.menu_view_full_selector);
            break;
        case 2:
            mBtns.get(0).setBackgroundResource(R.drawable.menu_view_top_selector);
            mBtns.get(1).setBackgroundResource(R.drawable.menu_view_bottom_selector);
            break;
        default:
            mBtns.get(0).setBackgroundResource(R.drawable.menu_view_top_selector);
            for (int i = 1; i < mBtns.size() - 1; i++) {
                mBtns.get(i).setBackgroundResource(R.drawable.menu_view_middle_selector);
            }
            mBtns.get(mBtns.size() - 1).setBackgroundResource(R.drawable.menu_view_bottom_selector);
            break;
        }
    }

    public void dismiss() {
        if (!popupWindow.isShowing())
            return;
        popupWindow.dismiss();
    }
}
