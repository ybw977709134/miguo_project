package co.onemeter.oneapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-12-2
 * Time: 上午9:21
 * To change this template use File | Settings | File Templates.
 */
public class LinearLayoutAsListView extends LinearLayout {
    private LinearLayoutAsListAdapter listAdapter;

    public LinearLayoutAsListView(Context context) {
        super(context);
    }

    public LinearLayoutAsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearLayoutAsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setListAdapter(LinearLayoutAsListAdapter adapter) {
        listAdapter=adapter;
        if(null != listAdapter) {
            listAdapter.setDataChangeListener(new LinearLayoutAsListDataChangeListener() {
                @Override
                public void onDataChanged() {
                    updateContent();
                }
            });
        }
        updateContent();
    }

    private void updateContent() {
        if(null == listAdapter) {
            return;
        }
        this.removeAllViews();
        for(int i=0; i<listAdapter.getCount(); ++i) {
            this.addView(listAdapter.getView(i,null,this));
        }
    }
}
