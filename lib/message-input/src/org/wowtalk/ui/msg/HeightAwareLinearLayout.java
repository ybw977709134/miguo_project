package org.wowtalk.ui.msg;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * This is a RelativeLayout with the ability to notify height change,
 * usually caused by toggling of soft keyboard. So it can be useful
 * for detecting keyboard state.
 *
 * <p>
 * View.addOnLayoutChangeListener() should be utilized to detect height
 * change, problem is that it requires API LEVEL 11.
 * </p>
 */
public class HeightAwareLinearLayout extends LinearLayout
        implements IHeightAwareView {

    private int mMaxHeight = 0;
    private int mRequestHeight = 0;
    private IHeightAwareView.OnHeightChangedListener mL = null;

    public HeightAwareLinearLayout(Context context) {
        super(context, null, 0);
    }

    public HeightAwareLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public HeightAwareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        Log.e(String.format("HeightAwareRelativeLayout.onLayout %s %d, %d, %d, %d",
//                changed, left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);

        if(mRequestHeight != bottom - top && mL != null) {
            mL.onHeightChanged(bottom - top,
                    (mMaxHeight < bottom - top ? bottom - top : mMaxHeight));
        }

        mRequestHeight = bottom - top;
        if(mRequestHeight > mMaxHeight) {
            mMaxHeight = mRequestHeight;
        }
    }

    @Override
    public void setOnHeightChangedListener(IHeightAwareView.OnHeightChangedListener l) {
        mL = l;
    }

    public int maxHeight() {
        return mMaxHeight;
    }

    public int requestHeight() {
        return mRequestHeight;
    }
}
