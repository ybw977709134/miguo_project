package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;

/**
 * <p>响应时间线页面（{@link co.onemeter.oneapp.ui.AllTimelineFragment}）上的过滤器的点击事件。</p>
 * Created by pzy on 10/15/14.
 */
public class TimelineFilterOnClickListener implements View.OnClickListener {
    View dialogBackground;
    View anchorView;
    Context context;
    View btnSender;
    View btnCategory;
    int selectedSenderIdx = 0;
    int selectedCatIdx = 0;
    PopupWindow dlgSender;
    PopupWindow dlgCat;
    OnTimelineFilterChangedListener onFilterChangedListener;

    /**
     * @param dialogBackground 作为对话框下方的屏幕背景，一般为半透明的黑色。
     * @param anchorView 对话现在在它的下方。
     * @param btnSender 筛选发送者的按钮。
     * @param btnCategory 筛选类型的按钮。
     */
    public TimelineFilterOnClickListener(
            View dialogBackground,
            View anchorView,
            View btnSender, View btnCategory) {
        this.dialogBackground = dialogBackground;
        this.anchorView = anchorView;
        context = anchorView.getContext();
        this.btnSender = btnSender;
        this.btnCategory = btnCategory;
    }

    public void setOnFilterChangedListener(OnTimelineFilterChangedListener l) {
        onFilterChangedListener = l;
    }

    @Override
    public void onClick(View v) {
        PopupWindow dlgToShow = null;
        if (v == btnSender) {
            // toggle dialog
            if (dlgSender != null && dlgSender.isShowing()) {
                dismissSenderDialog();
            } else {
                if (dlgCat != null && dlgCat.isShowing()) {
                    dismissCatDialog();
                }
                if (dlgSender == null) {
                    dlgSender = createSenderDialog();
                }
                dlgToShow = dlgSender;
            }
        } else if (v == btnCategory) {
            // toggle dialog
            if (dlgCat != null && dlgCat.isShowing()) {
                dismissCatDialog();
            } else {
                if (dlgSender != null && dlgSender.isShowing()) {
                    dismissSenderDialog();
                }
                if (dlgCat == null) {
                    dlgCat = createCategoryDialog();
                }
                dlgToShow = dlgCat;
            }
        }

        if (dlgToShow != null) {
            showDialog(dlgToShow, v instanceof TextView ? (TextView) v : null);
        }
    }

    private void setRightDrawable(TextView textView, int drawableResId) {
        Drawable d = context.getResources().getDrawable(drawableResId);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        textView.setCompoundDrawables(null, null, d, null);
    }

    private void setCollapsedButtonSyle(TextView button) {
        button.setTextColor(context.getResources().getColor(R.color.text_gray1));
        setRightDrawable(button, R.drawable.timeline_dropdown_mark_collapsed);
    }

    private void setExpandedButtonSyle(TextView button) {
        button.setTextColor(context.getResources().getColor(R.color.logo_green));
        setRightDrawable(button, R.drawable.timeline_dropdown_mark_expanded);
    }

    private void showDialog(PopupWindow dlgToShow, TextView button) {
        Rect rect = locateView(anchorView);
        if (rect != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dialogBackground.getLayoutParams();
            lp.topMargin = rect.height();
            dialogBackground.setLayoutParams(lp);
        }
        dlgToShow.showAtLocation(dialogBackground, Gravity.TOP, 0, rect != null ? rect.bottom : 0);
        dialogBackground.setVisibility(View.VISIBLE);
        dialogBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDismissAll();
            }
        });
        if (button != null) {
            setExpandedButtonSyle(button);
        }
    }

    private void dismissCatDialog() {
        dlgCat.dismiss();
        dlgCat = null;
        dialogBackground.setVisibility(View.GONE);
        if (btnCategory instanceof TextView) {
            setCollapsedButtonSyle((TextView) btnCategory);
        }
    }

    private void dismissSenderDialog() {
        dlgSender.dismiss();
        dlgSender = null;
        dialogBackground.setVisibility(View.GONE);
        if (btnSender instanceof TextView) {
            setCollapsedButtonSyle((TextView) btnSender);
        }
    }

    public void tryDismissAll() {
        if (dlgSender != null) {
            dlgSender.dismiss();
        }
        if (dlgCat != null) {
            dlgCat.dismiss();
        }

        dialogBackground.setVisibility(View.GONE);

        if (btnCategory instanceof TextView) {
            setCollapsedButtonSyle((TextView) btnCategory);
        }
        if (btnSender instanceof TextView) {
            setCollapsedButtonSyle((TextView) btnSender);
        }
    }

    public boolean isShowingDialog() {
        return (dlgSender != null && dlgSender.isShowing())
                || (dlgCat != null && dlgCat.isShowing());
    }

    private static PopupWindow createListDialog(
            Context context,
            String[] items, final int selectedIdx,
            AdapterView.OnItemClickListener onItemClickListener) {
        View dlgView = View.inflate(context, R.layout.timeline_filter_category_list, null);
        ListView lv = ((ListView)dlgView.findViewById(android.R.id.list));
        lv.setAdapter(
                new ArrayAdapter<String>(context,
                        R.layout.timeline_filter_category_list_item,
                        android.R.id.text1,
                        items) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView tv = (TextView) v.findViewById(android.R.id.text1);
                        Drawable rightDrawable = selectedIdx == position
                                ? getContext().getResources().getDrawable(R.drawable.timeline_filter_list_checkmark)
                                : null;
                        if (rightDrawable != null) {
                            rightDrawable.setBounds(0, 0, rightDrawable.getIntrinsicWidth(), rightDrawable.getIntrinsicHeight());
                        }
                        tv.setCompoundDrawables(null, null,  rightDrawable, null);
                        return v;
                    }
                }
        );
        lv.setOnItemClickListener(onItemClickListener);
        return new PopupWindow(dlgView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private PopupWindow createSenderDialog() {
        return createListDialog(context,
                context.getResources().getStringArray(R.array.timeline_senders),
                selectedSenderIdx,
                onSenderListItemClickListener);
    }

    private PopupWindow createCategoryDialog() {
        return createListDialog(context,
                context.getResources().getStringArray(R.array.timeline_categories),
                selectedCatIdx,
                onCatListItemClickListener);
    }

    private AdapterView.OnItemClickListener onSenderListItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedSenderIdx = position;
                    dismissSenderDialog();
                    if (onFilterChangedListener != null) {
                        onFilterChangedListener.onSenderChanged(position);
                    }
                }
            };

    private AdapterView.OnItemClickListener onCatListItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedCatIdx = position;
                    dismissCatDialog();
                    if (onFilterChangedListener != null) {
                        onFilterChangedListener.onTagChanged(position);
                    }
                }
            };

    private static Rect locateView(View v)
    {
        int[] loc_int = new int[2];
        if (v == null) return null;
        try
        {
            v.getLocationOnScreen(loc_int);
        } catch (NullPointerException npe)
        {
            //Happens when the view doesn't exist on screen anymore.
            return null;
        }
        Rect location = new Rect();
        location.left = loc_int[0];
        location.top = loc_int[1];
        location.right = location.left + v.getWidth();
        location.bottom = location.top + v.getHeight();
        return location;
    }
}
