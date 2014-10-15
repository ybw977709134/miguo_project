package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import co.onemeter.oneapp.R;

/**
 * Created by pzy on 10/15/14.
 */
public class TimelineFilterOnClickListener implements View.OnClickListener {
    ViewGroup dialogContainer;
    View anchorView;
    Context context;
    int btnSenderResId;
    int btnCategoryResId;
    PopupWindow dlgSender;
    PopupWindow dlgCat;

    public TimelineFilterOnClickListener(
            ViewGroup dialogContainer,
            View anchorView,
            int btnSenderResId, int btnCategoryResId) {
        this.dialogContainer = dialogContainer;
        this.anchorView = anchorView;
        context = anchorView.getContext();
        this.btnSenderResId = btnSenderResId;
        this.btnCategoryResId = btnCategoryResId;
    }
    @Override
    public void onClick(View v) {
        int vid = v.getId();
        PopupWindow dlgToShow = null;
        if (vid == btnSenderResId) {
            // toggle dialog
            if (dlgSender != null && dlgSender.isShowing()) {
                dlgSender.dismiss();
                dialogContainer.setVisibility(View.GONE);
            } else {
                if (dlgCat != null && dlgCat.isShowing()) {
                    dlgCat.dismiss();
                }
                if (dlgSender == null) {
                    dlgSender = createSenderDialog();
                }
                dlgToShow = dlgSender;
            }
        } else if (vid == btnCategoryResId) {
            // toggle dialog
            if (dlgCat != null && dlgCat.isShowing()) {
                dlgCat.dismiss();
                dialogContainer.setVisibility(View.GONE);
            } else {
                if (dlgSender != null && dlgSender.isShowing()) {
                    dlgSender.dismiss();
                }
                if (dlgCat == null) {
                    dlgCat = createCategoryDialog();
                }
                dlgToShow = dlgCat;
            }
        }

        if (dlgToShow != null) {
            Rect rect = locateView(anchorView);
            if (rect != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dialogContainer.getLayoutParams();
                lp.topMargin = rect.bottom;
                dialogContainer.setLayoutParams(lp);
            }
            dlgToShow.showAtLocation(dialogContainer, Gravity.TOP, 0, rect != null ? rect.bottom : 0);
            dialogContainer.setVisibility(View.VISIBLE);
        }
    }

    private PopupWindow createSenderDialog() {
        View dlgView = View.inflate(context, R.layout.timeline_filter_category_list, null);
        ListView lv = ((ListView)dlgView.findViewById(android.R.id.list));
        lv.setAdapter(
                new ArrayAdapter<String>(context,
                        R.layout.timeline_filter_category_list_item,
                        android.R.id.text1,
                        context.getResources().getStringArray(R.array.timeline_senders))
        );
        return new PopupWindow(dlgView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private PopupWindow createCategoryDialog() {
        View dlgView = View.inflate(context, R.layout.timeline_filter_category_list, null);
        ListView lv = ((ListView)dlgView.findViewById(android.R.id.list));
        lv.setAdapter(
                new ArrayAdapter<String>(context,
                        R.layout.timeline_filter_category_list_item,
                        android.R.id.text1,
                        context.getResources().getStringArray(R.array.timeline_categories))
        );
        return new PopupWindow(dlgView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

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
