package co.onemeter.oneapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/14/13
 * Time: 9:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class YQResizeLayout extends RelativeLayout {

    private OnResizeListener mListener;

    public interface OnResizeListener {
        void onResize(int w, int h, int oldw, int oldh);
    }

    public YQResizeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnResizeListener(OnResizeListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mListener != null) {
            mListener.onResize(w, h, oldw, oldh);
        }
    }
}
