package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;

import java.util.HashMap;

/**
 * <p>下拉列表，适用于“时间线”和“乐趣活动”页面，作为过滤器。</p>
 * Created by pzy on 10/15/14.
 */
public abstract class DropdownMenu {

    public interface OnDropdownMenuItemClickListener {
        public void onDropdownMenuItemClick(int subMenuResId, int itemIdx);
    }

    private final int[] menuItemResIds;
    private int selectedMenuItemResId;
    // menuItemResId => item index
    HashMap<Integer, Integer> selectedItemIdx = new HashMap<Integer, Integer>();

    private final View view;
    Context context;
    View dialogBackground;
    View anchorView;
    PopupWindow subMenuDlg;
    OnDropdownMenuItemClickListener onFilterChangedListener;

    /**
     * @param dialogBackground 作为对话框下方的屏幕背景，一般为半透明的黑色。
     */
    public DropdownMenu(
            Context context,
            int layoutResId,
            int[] menuItemResIds,
            View dialogBackground) {
        this.context = context;
        view = LayoutInflater.from(context).inflate(layoutResId, null);
        this.menuItemResIds = menuItemResIds;
        this.dialogBackground = dialogBackground;
        this.anchorView = view;

        for (int r : menuItemResIds) {
            selectedItemIdx.put(r, 0);
        }

        AQuery q = new AQuery(view);
        for (int r : menuItemResIds) {
            q.find(r).clicked(clickListener);
        }
    }

    protected abstract String[] getSubItems(int itemId);

    public View getView() {
        return view;
    }

    public void setOnFilterChangedListener(OnDropdownMenuItemClickListener l) {
        onFilterChangedListener = l;
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

    public void tryDismissAll() {
        if (subMenuDlg != null) {
            subMenuDlg.dismiss();
        }

        dialogBackground.setVisibility(View.GONE);

        AQuery q = new AQuery(view);
        for (int r : menuItemResIds) {
            setCollapsedButtonSyle(q.find(r).getTextView());
        }
    }

    public boolean isShowingDialog() {
        return (subMenuDlg != null && subMenuDlg.isShowing());
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

    private PopupWindow createSubMenu(int menuItemResId) {
        selectedMenuItemResId = menuItemResId;
        return createListDialog(context,
                getSubItems(menuItemResId),
                selectedItemIdx.get(menuItemResId),
                onCatListItemClickListener);
    }

    private AdapterView.OnItemClickListener onCatListItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedItemIdx.put(selectedMenuItemResId, position);
                    tryDismissAll();
                    if (onFilterChangedListener != null) {
                        onFilterChangedListener.onDropdownMenuItemClick(selectedMenuItemResId, position);
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

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isShowingDialog()) {
                tryDismissAll();
            } else {
                subMenuDlg = createSubMenu(v.getId());
                showDialog(subMenuDlg, v instanceof TextView ? (TextView) v : null);
            }
        }
    };
}
